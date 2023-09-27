package io.fair_acc.chartfx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.canvas.GraphicsContext;

import io.fair_acc.bench.DurationMeasure;
import io.fair_acc.bench.MeasurementRecorder;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.chartfx.renderer.PolarTickStep;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.GridRenderer;
import io.fair_acc.chartfx.renderer.spi.LabelledMarkerRenderer;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.utils.AssertUtils;

/**
 * Chart designed primarily to display data traces using DataSet interfaces which are more flexible and efficient than
 * the observable lists used by XYChart. Brief history: original design inspired by Oracle, extended by CERN (i.e.
 * plugin concept/zoomer), modified to mitigate JavaFX performance issues and extended renderer
 * concept/canvas-concept/interfaces/+more plugins by GSI. Refactored and re-write in 2018 to make it compatible with
 * Apache 2.0 which -- in the spirit of 'Ship of Theseus' -- makes it de-facto a new development. Contributions,
 * bug-fixes, and modifications are welcome. Hope you find this library useful and enjoy!
 *
 * @author braeun
 * @author rstein
 */
public class XYChart extends Chart {
    protected static final int BURST_LIMIT_MS = 15;
    protected final BooleanProperty polarPlot = new SimpleBooleanProperty(this, "polarPlot", false);
    private final ObjectProperty<PolarTickStep> polarStepSize = new SimpleObjectProperty<>(PolarTickStep.THIRTY);
    private final GridRenderer gridRenderer = new GridRenderer(this);

    /**
     * Construct a new XYChart with the given axes.
     */
    public XYChart() {
        this(new Axis[] {}); // NOPMD NOSONAR
        // N.B. this constructor is needed since JavaFX seems to instantiate fxml using reflection to find the corresponding constructor
    }

    /**
     * Construct a new XYChart with the given axes.
     *
     * @param axes All axes to be added to the chart
     */
    public XYChart(final Axis... axes) {
        super(axes);

        for (int dim = 0; dim < axes.length; dim++) {
            final Axis axis = axes[dim];
            if (axis == null) {
                continue;
            }
            switch (dim) {
            case DataSet.DIM_X:
                axis.setSide(Side.BOTTOM);
                break;
            case DataSet.DIM_Y:
                axis.setSide(Side.LEFT);
                break;
            default:
                axis.setSide(Side.RIGHT);
                break;
            }
            getAxes().add(axis);
        }

        styleableNodes.getChildren().add(gridRenderer);
        PropUtil.runOnChange(getBitState().onAction(ChartBits.ChartCanvas),
                gridRenderer.getHorizontalMajorGrid().changeCounterProperty(),
                gridRenderer.getHorizontalMinorGrid().changeCounterProperty(),
                gridRenderer.getVerticalMajorGrid().changeCounterProperty(),
                gridRenderer.getVerticalMinorGrid().changeCounterProperty(),
                gridRenderer.drawOnTopProperty());

        getRenderers().addListener(this::rendererChanged);

        // TODO: get rid of default instance. It's created if anyone wants to use getDatasets()
        getRenderers().add(new ErrorDataSetRenderer());
    }

    /**
     * @return datasets of the first renderer. Creates a renderer if needed.
     */
    public ObservableList<DataSet> getDatasets() {
        if (getRenderers().isEmpty()) {
            getRenderers().add(new ErrorDataSetRenderer());
        }
        return getRenderers().get(0).getDatasets();
    }

    /**
     * @return datasets attached to the chart and datasets attached to all renderers
     */
    @Override
    public ObservableList<DataSet> getAllDatasets() {
        if (getRenderers() == null) {
            return allDataSets;
        }

        allDataSets.clear();
        for (Renderer renderer : getRenderers()) {
            if (renderer instanceof LabelledMarkerRenderer) {
                continue;
            }
            allDataSets.addAll(renderer.getDatasets());
        }
        return allDataSets;
    }

    /**
     * @return datasets attached to the chart and datasets attached to all renderers TODO: change to change listener
     *         that add/remove datasets from a global observable list
     */
    public ObservableList<DataSet> getAllShownDatasets() {
        final ObservableList<DataSet> ret = FXCollections.observableArrayList();
        getRenderers().stream().filter(Renderer::showInLegend).forEach(renderer -> ret.addAll(renderer.getDatasets()));
        return ret;
    }

    /**
     * @return nomen est omen
     */
    public GridRenderer getGridRenderer() {
        return gridRenderer;
    }

    public PolarTickStep getPolarStepSize() {
        return polarStepSizeProperty().get();
    }

    /**
     * Returns the x axis.
     *
     * @return x axis
     */
    public Axis getXAxis() {
        return getFirstAxis(Orientation.HORIZONTAL);
    }

    /**
     * Returns the y axis.
     *
     * @return y axis
     */
    public Axis getYAxis() {
        return getFirstAxis(Orientation.VERTICAL);
    }

    /**
     * whether renderer should use polar coordinates (x -&gt; interpreted as phi, y as radial coordinate)
     *
     * @return true if renderer is plotting in polar coordinates
     */
    public final boolean isPolarPlot() {
        return polarPlotProperty().get();
    }

    /**
     * Sets whether renderer should use polar coordinates (x -&gt; interpreted as phi, y as radial coordinate)
     *
     * @return true if renderer is plotting in polar coordinates
     */
    public final BooleanProperty polarPlotProperty() {
        return polarPlot;
    }

    public ObjectProperty<PolarTickStep> polarStepSizeProperty() {
        return polarStepSize;
    }

    /**
     * Sets whether renderer should use polar coordinates (x -&gt; interpreted as phi, y as radial coordinate)
     *
     * @param state true if renderer is parallelising sub-functionalities
     * @return itself (fluent design)
     */
    public final XYChart setPolarPlot(final boolean state) {
        polarPlotProperty().set(state);
        return this;
    }

    public void setPolarStepSize(final PolarTickStep step) {
        polarStepSizeProperty().set(step);
    }

    @Override
    public void updateAxisRange() {
        // Update the axis definitions of all datasets. We do it here, so we can make better
        // use of multi-threading. The datasets are already locked, so we can use a parallel
        // stream without extra synchronization.
        getRenderers().stream().flatMap(renderer -> renderer.getDatasetNodes().stream()).filter(DataSetNode::isVisible).map(DataSetNode::getDataSet).filter(ds -> ds.getBitState().isDirty(ChartBits.DataSetData, ChartBits.DataSetRange)).distinct().forEach(dataset -> dataset.getAxisDescriptions().parallelStream().filter(axisD -> !axisD.isDefined() || axisD.getBitState().isDirty()).forEach(axisDescription -> dataset.recomputeLimits(axisDescription.getDimIndex())));

        // Update each axis
        for (Axis axis : getAxes()) {
            // Determine the current range
            axisRange.clear();
            for (Renderer renderer : getRenderers()) {
                renderer.updateAxisRange(axis, axisRange);
            }

            // Update the internal auto range
            boolean changed = false;
            if (axis.isAutoGrowRanging() && axis.getAutoRange().isDefined()) {
                if (axisRange.isDefined()) {
                    changed = axis.getAutoRange().add(axisRange);
                }
            } else {
                changed = axis.getAutoRange().set(axisRange.getMin(), axisRange.getMax());
            }

            // Trigger a redraw
            if (changed && (axis.isAutoRanging() || axis.isAutoGrowRanging())) {
                axis.invalidateRange();
            }

            // Feature for backwards compatibility: Category axes that do not have
            // their categories set copy the categories of the first dataset of the
            // first renderer that is using this axis.
            if (axis instanceof CategoryAxis catAxis) {
                for (Renderer renderer : getRenderers()) {
                    if (renderer.isUsingAxis(axis)) {
                        if (!renderer.getDatasets().isEmpty()) {
                            catAxis.updateCategories(renderer.getDatasets().get(0));
                        }
                        break;
                    }
                }
            }
        }
    }

    private final AxisRange axisRange = new AxisRange();

    /**
     * add XYChart specific axis handling (ie. placement around charts, add new DefaultNumericAxis if one is missing,
     * etc.)
     *
     * @param change the new axis change that is being added
     */
    @Override
    protected void axesChanged(final ListChangeListener.Change<? extends Axis> change) {
        while (change.next()) {
            change.getRemoved().forEach(axis -> {
                AssertUtils.notNull("to be removed axis is null", axis);
                // TODO: throw an exception if axis is associated with an existing renderer?
            });

            change.getAddedSubList().forEach(axis -> {
                // check if axis is associated with an existing renderer,
                // if yes -&gt; throw an exception
                AssertUtils.notNull("to be added axis is null", axis);
            });
        }

        invalidate();
    }

    @Override
    protected void redrawCanvas() {
        FXUtils.assertJavaFxThread();

        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Bottom grid
        if (!gridRenderer.isDrawOnTop()) {
            benchDrawGrid.start();
            gridRenderer.render();
            benchDrawGrid.stop();
        }

        // Data
        benchDrawData.start();
        for (final Renderer renderer : getRenderers()) {
            renderer.render();
        }
        benchDrawData.stop();

        // Top grid
        if (gridRenderer.isDrawOnTop()) {
            benchDrawGrid.start();
            gridRenderer.render();
            benchDrawGrid.stop();
        }
    }

    /**
     * @param recorder recorder for this chart and all nested components
     */
    public void setGlobalRecorder(MeasurementRecorder recorder) {
        setRecorder(recorder);
        int i = 0;
        for (var renderer : getRenderers()) {
            var p = recorder.addPrefix("renderer" + i);
            renderer.setRecorder(p);
            int dsIx = 0;
            for (var dataset : renderer.getDatasets()) {
                dataset.setRecorder(p.addPrefix("ds" + dsIx));
                dataset.lock().setRecorder(p.addPrefix("ds" + dsIx));
                dsIx++;
            }
            i++;
        }
        gridRenderer.setRecorder(recorder);
        i = 0;
        for (Axis axis : getAxes()) {
            if (axis == getXAxis()) {
                axis.setRecorder(recorder.addPrefix("x"));
            } else if (axis == getYAxis()) {
                axis.setRecorder(recorder.addPrefix("y"));
            } else {
                axis.setRecorder(recorder.addPrefix("axis" + i));
            }
            i++;
        }
        i = 0;
        for (ChartPlugin plugin : getPlugins()) {
            plugin.setRecorder(recorder.addPrefix("plugin" + i++));
        }
    }

    @Override
    public void setRecorder(MeasurementRecorder recorder) {
        benchDrawData = recorder.newDuration("xychart-drawData");
        benchDrawGrid = recorder.newDuration("xychart-drawGrid");
        super.setRecorder(recorder);
    }

    private DurationMeasure benchDrawData = DurationMeasure.DISABLED;
    private DurationMeasure benchDrawGrid = DurationMeasure.DISABLED;
}
