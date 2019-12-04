package de.gsi.chart;

import static de.gsi.dataset.DataSet.DIM_Z;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.PolarTickStep;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.GridRenderer;
import de.gsi.chart.renderer.spi.LabelledMarkerRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.utils.AssertUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;

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
    protected BooleanProperty polarPlot = new SimpleBooleanProperty(this, "polarPlot", false);
    private final ObjectProperty<PolarTickStep> polarStepSize = new SimpleObjectProperty<>(PolarTickStep.THIRTY);
    private final GridRenderer gridRenderer = new GridRenderer(this);
    private long lastCanvasUpdate;
    private boolean callCanvasUpdateLater;
    private final ChangeListener<Side> axisSideChangeListener = this::axisSideChanged;

    public XYChart() {
        this(null, null);
    }

    /**
     * Construct a new XYChart with the given axes.
     *
     * @param xAxis the axis to use as primary x-Axis
     * @param yAxis the axis to use as primary y-Axis
     */
    public XYChart(final Axis xAxis, final Axis yAxis) {
        super();

        if (xAxis != null) {
            if (xAxis.getSide() == null || !xAxis.getSide().isHorizontal()) {
                xAxis.setSide(Side.BOTTOM);
            }
            getAxes().add(xAxis);
        }

        if (yAxis != null) {
            if (yAxis.getSide() == null || !yAxis.getSide().isVertical()) {
                yAxis.setSide(Side.LEFT);
            }
            getAxes().add(yAxis);
        }

        this.setAnimated(false);
        getRenderers().addListener(this::rendererChanged);

        getRenderers().add(new ErrorDataSetRenderer());
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
            change.getRemoved().forEach(set -> {
                AssertUtils.notNull("to be removed axis is null", set);
                // check if axis is associated with an existing renderer, if yes
                // -&gt; throw an exception
                // remove from axis.side property side listener
                set.sideProperty().removeListener(axisSideChangeListener);
            });
            for (final Axis set : change.getAddedSubList()) {
                // check if axis is associated with an existing renderer,
                // if yes -&gt; throw an exception
                AssertUtils.notNull("to be added axis is null", set);

                final Side side = set.getSide();
                if (side == null) {
                    throw new InvalidParameterException(new StringBuilder().append("axis '").append(set.getName())
                            .append("' has 'null' as side being set").toString());
                }
                if (!getAxesPane(set.getSide()).getChildren().contains((Node) set)) {
                    getAxesPane(set.getSide()).getChildren().add((Node) set);
                }

                set.sideProperty().addListener(axisSideChangeListener);

            }
        }

        requestLayout();
    }

    protected void axisSideChanged(final ObservableValue<? extends Side> change, final Side oldValue,
            final Side newValue) {
        if (newValue != null && newValue.equals(oldValue)) {
            return;
        }
        // loop through all registered axis
        for (final Axis axis : axesList) {

            if (axis.getSide() == null) {
                // remove axis from all axis panes
                removeFromAllAxesPanes(axis);
            }

            // check if axis is in correct pane
            if (getAxesPane(axis.getSide()).getChildren().contains((Node) axis)) {
                // yes, it is continue with next axis
                continue;
            }
            // axis needs to be moved to new pane location
            // first: remove axis from all axis panes
            removeFromAllAxesPanes(axis);

            // second: add axis to correct axis pane
            getAxesPane(axis.getSide()).getChildren().add((Node) axis);
        }
        requestLayout();
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

            // check if there are assignable axes not yet present in the Chart's list
            getAxes().addAll(renderer.getAxes().subList(0, 2).stream()
                    .filter(a -> (a.getSide() != null && !getAxes().contains(a))).collect(Collectors.toList()));
        }
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
        allDataSets.addAll(getDatasets());
        getRenderers().stream().filter(renderer -> !(renderer instanceof LabelledMarkerRenderer))
                .forEach(renderer -> allDataSets.addAll(renderer.getDatasets()));

        return allDataSets;
    }

    /**
     * @return datasets attached to the chart and datasets attached to all renderers TODO: change to change listener
     *         that add/remove datasets from a global observable list
     */
    public ObservableList<DataSet> getAllShownDatasets() {
        // return allVisibleDataSets;
        final ObservableList<DataSet> ret = FXCollections.observableArrayList();
        ret.addAll(getDatasets());
        getRenderers().stream().filter(Renderer::showInLegend).forEach(renderer -> ret.addAll(renderer.getDatasets()));
        return ret;
    }

    protected List<DataSet> getDataSetForAxis(final Axis axis) {
        final List<DataSet> retVal = new ArrayList<>();
        if (axis == null) {
            return retVal;
        }
        retVal.addAll(getDatasets());
        getRenderers().forEach(renderer -> renderer.getAxes().stream().filter(axis::equals)
                .forEach(rendererAxis -> retVal.addAll(renderer.getDatasets())));
        return retVal;
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
     * Indicates whether horizontal grid lines are visible or not.
     *
     * @return horizontalGridLinesVisible property
     */
    public final BooleanProperty horizontalGridLinesVisibleProperty() {
        return gridRenderer.horizontalGridLinesVisibleProperty();
    }

    private boolean isDataEmpty() {
        return getAllDatasets() == null || getAllDatasets().isEmpty();
    }

    /**
     * Indicates whether horizontal grid lines are visible.
     *
     * @return {@code true} if horizontal grid lines are visible else {@code false}.
     */
    public final boolean isHorizontalGridLinesVisible() {
        return horizontalGridLinesVisibleProperty().get();
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
     * Indicates whether vertical grid lines are visible.
     *
     * @return {@code true} if vertical grid lines are visible else {@code false}.
     */
    public final boolean isVerticalGridLinesVisible() {
        return verticalGridLinesVisibleProperty().get();
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

    @Override
    protected void redrawCanvas() {
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - pre");
        }
        setAutoNotification(false);
        FXUtils.assertJavaFxThread();
        final long now = System.nanoTime();
        final double diffMillisSinceLastUpdate = TimeUnit.NANOSECONDS.toMillis(now - lastCanvasUpdate);
        if (diffMillisSinceLastUpdate < XYChart.BURST_LIMIT_MS) {
            if (!callCanvasUpdateLater) {
                callCanvasUpdateLater = true;
                // repaint 20 ms later in case this was just a burst operation
                final KeyFrame kf1 = new KeyFrame(Duration.millis(20), e -> requestLayout());

                final Timeline timeline = new Timeline(kf1);
                Platform.runLater(timeline::play);
            }

            return;
        }
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - executing");
            LOGGER.debug("   xychart redrawCanvas() - canvas size = {}",
                    String.format("%fx%f", canvas.getWidth(), canvas.getHeight()));
        }

        lastCanvasUpdate = now;
        callCanvasUpdateLater = false;

        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (!gridRenderer.isDrawOnTop()) {
            gridRenderer.render(gc, this, 0, null);
        }

        int dataSetOffset = 0;
        for (final Renderer renderer : getRenderers()) {
            // check for and add required axes
            checkRendererForRequiredAxes(renderer);

            renderer.render(gc, this, dataSetOffset, getDatasets());
            dataSetOffset += getDatasets().size() + renderer.getDatasets().size();
        }

        if (gridRenderer.isDrawOnTop()) {
            gridRenderer.render(gc, this, 0, null);
        }
        setAutoNotification(true);
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - done");
        }
    }

    protected void rendererChanged(final ListChangeListener.Change<? extends Renderer> change) {
        FXUtils.assertJavaFxThread();
        super.rendererChanged(change);

        //        while (change.next()) {
        //            // handle added renderer
        //            for (final Renderer renderer : change.getAddedSubList()) {
        //                // checkRendererForRequiredAxes(renderer);
        //            }
        //
        //            // handle removed renderer
        //            change.getRemoved().forEach(renderer -> renderer.getDatasets().removeListener(datasetChangeListener));
        //        }
        requestLayout();
        updateLegend(getDatasets(), getRenderers());
    }

    /**
     * Sets the value of the {@link #verticalGridLinesVisibleProperty()}.
     *
     * @param value {@code true} to make vertical lines visible
     */
    public final void setHorizontalGridLinesVisible(final boolean value) {
        horizontalGridLinesVisibleProperty().set(value);
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

    /**
     * Sets the value of the {@link #verticalGridLinesVisibleProperty()}.
     *
     * @param value {@code true} to make vertical lines visible
     */
    public final void setVerticalGridLinesVisible(final boolean value) {
        verticalGridLinesVisibleProperty().set(value);
    }

    @Override
    public void updateAxisRange() {
        if (isDataEmpty()) {
            return;
        }

        // lock datasets to prevent writes while updating the axes
        ObservableList<DataSet> dataSets = this.getAllDatasets();
        // check that all registered data sets have proper ranges defined
        dataSets.parallelStream().forEach(dataset -> dataset.getAxisDescriptions().parallelStream()
                .filter(axisD -> !axisD.isDefined()).forEach(axisDescription -> {
                    dataset.lock().writeLockGuard(() -> {
                        dataset.recomputeLimits(dataset.getAxisDescriptions().indexOf(axisDescription));
                    });
                }));

        // N.B. possible race condition on this line -> for the future to solve
        // recomputeLimits holds a writeLock the following sections need a read lock (for allowing parallel axis)
        // there isn't an easy way to down-grade the established write locks into read locks (yet)
        // Experimental version:
        // dataSets.forEach(dataset -> {
        // dataset.lock().writeLock();
        // dataset.getAxisDescriptions().parallelStream().filter(axisD -> !axisD.isDefined())
        // .forEach(axisDescription -> {
        // dataset.setAutoNotification(false);
        // dataset.recomputeLimits(dataset.getAxisDescriptions().indexOf(axisDescription));
        // });
        // DefaultDataSetLock<DataSet> myLock = (DefaultDataSetLock<DataSet>) dataset.lock();
        // myLock.downGradeWriteLock();
        // });

        dataSets.forEach(ds -> ds.lock().readLock());
        try {
            getAxes().forEach(chartAxis -> {
                final List<DataSet> dataSetForAxis = getDataSetForAxis(chartAxis);
                updateNumericAxis(chartAxis, dataSetForAxis);
                // chartAxis.requestAxisLayout();
            });
        } finally {
            dataSets.forEach(ds -> ds.lock().readUnLock());
        }

        // unlock datasets again
    }

    /**
     * Indicates whether vertical grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty verticalGridLinesVisibleProperty() {
        return gridRenderer.verticalGridLinesVisibleProperty();
    }

    protected static void updateNumericAxis(final Axis axis, final List<DataSet> dataSets) {
        if (dataSets == null || dataSets.isEmpty()) {
            return;
        }
        final boolean oldAutoState = axis.autoNotification().getAndSet(false);
        final double oldMin = axis.getMin();
        final double oldMax = axis.getMax();
        final double oldLength = axis.getLength();

        // TODO: add new auto-ranging here
        final boolean isHorizontal = axis.getSide().isHorizontal();
        final Side side = axis.getSide();
        axis.getAutoRange().clear();
        dataSets.forEach(dataset -> {
            if (dataset instanceof DataSet3D && (side == Side.RIGHT || side == Side.TOP)) {
                final DataSet3D mDataSet = (DataSet3D) dataset;
                axis.getAutoRange().add(mDataSet.getAxisDescription(2).getMin());
                axis.getAutoRange().add(mDataSet.getAxisDescription(2).getMax());
            } else {
                axis.getAutoRange().add(dataset.getAxisDescription(isHorizontal ? 0 : 1).getMin());
                axis.getAutoRange().add(dataset.getAxisDescription(isHorizontal ? 0 : 1).getMax());
            }
        });
        axis.getAutoRange().setAxisLength(axis.getLength() == 0 ? 1 : axis.getLength(), side);

        // handling of numeric axis and auto-range or auto-grow setting only
        if (!axis.isAutoRanging() && !axis.isAutoGrowRanging()) {
            if (oldMin != axis.getMin() || oldMax != axis.getMax() || oldLength != axis.getLength()) {
                axis.forceRedraw();
            }
            axis.autoNotification().set(oldAutoState);
            return;
        }

        final List<Number> dataMinMax = new ArrayList<>();
        dataSets.forEach(dataset -> dataset.lock().readLockGuardOptimistic(() -> {
            // for (AxisDescription axisDescription : dataset.getAxisDescriptions()) {
            // if (!axisDescription.isDefined()) {
            // dataset.recomputeLimits(dataset.getAxisDescriptions().indexOf(axisDescription));
            // }
            // }
            if (dataset instanceof DataSet3D && (side == Side.RIGHT || side == Side.TOP)) {
                final DataSet3D mDataSet = (DataSet3D) dataset;
                dataMinMax.add(mDataSet.getAxisDescription(DIM_Z).getMin());
                dataMinMax.add(mDataSet.getAxisDescription(DIM_Z).getMax());
            } else {
                dataMinMax.add(dataset.getAxisDescription(isHorizontal ? 0 : 1).getMin());
                dataMinMax.add(dataset.getAxisDescription(isHorizontal ? 0 : 1).getMax());
            }
        }));

        if (axis.isAutoGrowRanging()) {
            dataMinMax.add(axis.getMin());
            dataMinMax.add(axis.getMax());
        }

        // work-around since we cannot overwrite the method 'autorange(min,max)' in ValueAxis
        if (axis.isAutoGrowRanging()) {
            double min = +Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (final Number val : dataMinMax) {
                min = Math.min(min, val.doubleValue());
                max = Math.max(max, val.doubleValue());
            }
            axis.set(min, max);
        } else {
            axis.invalidateRange(dataMinMax);
        }
        if (oldMin != axis.getMin() || oldMax != axis.getMax() || oldLength != axis.getLength()) {
            axis.forceRedraw();
        }
        axis.autoNotification().set(oldAutoState);
    }

}
