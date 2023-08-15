package io.fair_acc.chartfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.events.ChartBits;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.canvas.GraphicsContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.PolarTickStep;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.GridRenderer;
import io.fair_acc.chartfx.renderer.spi.LabelledMarkerRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(XYChart.class);
    protected static final int BURST_LIMIT_MS = 15;
    protected final BooleanProperty polarPlot = new SimpleBooleanProperty(this, "polarPlot", false);
    private final ObjectProperty<PolarTickStep> polarStepSize = new SimpleObjectProperty<>(PolarTickStep.THIRTY);
    private final GridRenderer gridRenderer = new GridRenderer();

    /**
     * Construct a new XYChart with the given axes.
     *
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
            if(renderer instanceof LabelledMarkerRenderer){
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
        ret.addAll(getDatasets());
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
        if (isDataEmpty()) {
            return;
        }

        // Check that all registered data sets have proper ranges defined. The datasets
        // are already locked, so we can use parallel stream without extra synchronization.
        getRenderers().stream()
                .flatMap(renderer -> renderer.getDatasetNodes().stream())
                .filter(DataSetNode::isVisible)
                .map(DataSetNode::getDataSet)
                .filter(ds -> ds.getBitState().isDirty(ChartBits.DataSetData, ChartBits.DataSetRange))
                .distinct()
                .forEach(dataset -> dataset.getAxisDescriptions().parallelStream()
                        .filter(axisD -> !axisD.isDefined() || axisD.getBitState().isDirty())
                        .forEach(axisDescription -> dataset.recomputeLimits(axisDescription.getDimIndex())));

        // Update each of the axes
        getAxes().forEach(chartAxis -> updateNumericAxis(chartAxis, getDataSetForAxis(chartAxis)));

    }

    private boolean isDataEmpty() {
        return getAllDatasets() == null || getAllDatasets().isEmpty();
    }

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

    /**
     * checks whether renderer has required x and y axes and adds the first x or y from the chart itself if necessary
     * <p>
     * additionally moves axis from Renderer with defined Side that are not yet in the Chart also to the chart's list
     *
     * @param renderer to be checked
     */
    protected void checkRendererForRequiredAxes(final Renderer renderer) {
        if (renderer.getAxes().size() < 2) {
            // not enough axes present in renderer
            Optional<Axis> xAxis = renderer.getAxes().stream().filter(a -> a.getSide().isHorizontal()).findFirst();
            Optional<Axis> yAxis = renderer.getAxes().stream().filter(a -> a.getSide().isVertical()).findFirst();

            // search for horizontal/vertical axes in Chart (which creates one if missing) and add to renderer
            if (xAxis.isEmpty()) {
                renderer.getAxes().add(getFirstAxis(Orientation.HORIZONTAL));
            }
            if (yAxis.isEmpty()) {
                // search for horizontal axis in Chart (which creates one if missing) and add to renderer
                renderer.getAxes().add(getFirstAxis(Orientation.VERTICAL));
            }
        }
        // check if there are assignable axes not yet present in the Chart's list
        getAxes().addAll(renderer.getAxes().stream().limit(2).filter(a -> (a.getSide() != null && !getAxes().contains(a))).collect(Collectors.toList()));
    }

    protected List<DataSetNode> getDataSetForAxis(final Axis axis) {
        final List<DataSetNode> list = new ArrayList<>();
        getRenderers().stream()
                .filter(renderer -> renderer.getAxes().contains(axis))
                .map(Renderer::getDatasetNodes)
                .forEach(list::addAll);
        return list;
    }

    @Override
    protected void runPreLayout() {
        for (Renderer renderer : getRenderers()) {
            // check for and add required axes
            checkRendererForRequiredAxes(renderer);
        }
        super.runPreLayout();
    }

    @Override
    protected void redrawCanvas() {
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - pre");
        }
        FXUtils.assertJavaFxThread();
        final long now = System.nanoTime();
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - executing");
            LOGGER.debug("   xychart redrawCanvas() - canvas size = {}", String.format("%fx%f", canvas.getWidth(), canvas.getHeight()));
        }

        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Bottom grid
        if (!gridRenderer.isDrawOnTop()) {
            gridRenderer.render(gc, this, 0, null);
        }

        // Data
        for (final Renderer renderer : getRenderers()) {
            renderer.render(gc, this, renderer.getIndexOffset(), FXCollections.emptyObservableList());
        }

        // Top grid
        if (gridRenderer.isDrawOnTop()) {
            gridRenderer.render(gc, this, 0, null);
        }

        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - done");
        }
    }

    protected static void updateNumericAxis(final Axis axis, final List<DataSetNode> dataSets) {
        if (dataSets == null || dataSets.isEmpty()) {
            return;
        }

        final Side side = axis.getSide();
        final boolean isHorizontal = side.isHorizontal();

        // Determine the range of all datasets for this axis
        final AxisRange dsRange = new AxisRange();
        dsRange.clear();
        dataSets.stream().filter(DataSetNode::isVisible).map(DataSetNode::getDataSet)
                .forEach(dataset -> {
                    if (dataset.getDimension() > 2 && (side == Side.RIGHT || side == Side.TOP)) {
                if (!dataset.getAxisDescription(DataSet.DIM_Z).isDefined()) {
                    dataset.recomputeLimits(DataSet.DIM_Z);
                }
                dsRange.add(dataset.getAxisDescription(DataSet.DIM_Z).getMin());
                dsRange.add(dataset.getAxisDescription(DataSet.DIM_Z).getMax());
            } else {
                final int nDim = isHorizontal ? DataSet.DIM_X : DataSet.DIM_Y;
                if (!dataset.getAxisDescription(nDim).isDefined()) {
                    dataset.recomputeLimits(nDim);
                }
                dsRange.add(dataset.getAxisDescription(nDim).getMin());
                dsRange.add(dataset.getAxisDescription(nDim).getMax());
            }
        });

        // Update the auto range
        final boolean changed;
        if (axis.isAutoGrowRanging() && axis.getAutoRange().isDefined()) {
            changed = axis.getAutoRange().add(dsRange);
        } else {
            changed = axis.getAutoRange().set(dsRange.getMin(), dsRange.getMax());
        }

        // Trigger a redraw
        if (changed && (axis.isAutoRanging() || axis.isAutoGrowRanging())) {
            axis.invalidateRange();
        }

    }
}
