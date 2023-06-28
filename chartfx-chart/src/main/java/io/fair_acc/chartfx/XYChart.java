package io.fair_acc.chartfx;

import java.security.InvalidParameterException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.PolarTickStep;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
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
    protected final ChangeListener<? super Boolean> gridLineVisibilitychange = (ob, o, n) -> requestLayout();
    private long lastCanvasUpdate;
    private boolean callCanvasUpdateLater;

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

        gridRenderer.horizontalGridLinesVisibleProperty().addListener(gridLineVisibilitychange);
        gridRenderer.verticalGridLinesVisibleProperty().addListener(gridLineVisibilitychange);
        gridRenderer.getHorizontalMinorGrid().visibleProperty().addListener(gridLineVisibilitychange);
        gridRenderer.getVerticalMinorGrid().visibleProperty().addListener(gridLineVisibilitychange);
        gridRenderer.drawOnTopProperty().addListener(gridLineVisibilitychange);

        this.setAnimated(false);
        getRenderers().addListener(this::rendererChanged);

        getRenderers().add(new ErrorDataSetRenderer());
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
        getRenderers().stream().filter(renderer -> !(renderer instanceof LabelledMarkerRenderer)).forEach(renderer -> allDataSets.addAll(renderer.getDatasets()));

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
     * Indicates whether horizontal grid lines are visible or not.
     *
     * @return horizontalGridLinesVisible property
     */
    public final BooleanProperty horizontalGridLinesVisibleProperty() {
        return gridRenderer.horizontalGridLinesVisibleProperty();
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
        dataSets.parallelStream()
                .forEach(dataset -> dataset.getAxisDescriptions().parallelStream().filter(axisD -> !axisD.isDefined()).forEach(axisDescription -> dataset.lock().writeLockGuard(() -> dataset.recomputeLimits(axisDescription.getDimIndex()))));

        final ArrayDeque<DataSet> lockQueue = new ArrayDeque<>(dataSets);
        recursiveLockGuard(lockQueue, () -> getAxes().forEach(chartAxis -> {
            final List<DataSet> dataSetForAxis = getDataSetForAxis(chartAxis);
            updateNumericAxis(chartAxis, dataSetForAxis);
            // chartAxis.requestAxisLayout()
        }));
    }

    protected void recursiveLockGuard(final Deque<DataSet> queue, final Runnable runnable) { // NOPMD
        if (queue.isEmpty()) {
            runnable.run();
        } else {
            queue.pop().lock().readLockGuard(() -> recursiveLockGuard(queue, runnable));
        }
    }

    /**
     * Indicates whether vertical grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty verticalGridLinesVisibleProperty() {
        return gridRenderer.verticalGridLinesVisibleProperty();
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
                // TODO:
                //  check if axis is associated with an existing renderer, if yes
                //  -&gt; throw an exception
                if (axis instanceof Node) {
                    axesAndCanvasPane.remove((Node) axis);
                }
            });

            change.getAddedSubList().forEach(axis -> {
                // check if axis is associated with an existing renderer,
                // if yes -&gt; throw an exception
                AssertUtils.notNull("to be added axis is null", axis);

                final Side side = axis.getSide();
                if (side == null) {
                    throw new InvalidParameterException("axis '" + axis.getName() + "' has 'null' as side being set");
                }
                if (axis instanceof Node) {
                    axesAndCanvasPane.addSide(side, (Node) axis);
                }
            });
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
        }
        // check if there are assignable axes not yet present in the Chart's list
        getAxes().addAll(renderer.getAxes().stream().limit(2).filter(a -> (a.getSide() != null && !getAxes().contains(a))).collect(Collectors.toList()));
    }

    protected List<DataSet> getDataSetForAxis(final Axis axis) {
        final List<DataSet> retVal = new ArrayList<>();
        if (axis == null) {
            return retVal;
        }
        retVal.addAll(getDatasets());
        getRenderers().forEach(renderer -> renderer.getAxes().stream().filter(axis::equals).forEach(rendererAxis -> retVal.addAll(renderer.getDatasets())));
        return retVal;
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
            LOGGER.debug("   xychart redrawCanvas() - canvas size = {}", String.format("%fx%f", canvas.getWidth(), canvas.getHeight()));
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

            final List<DataSet> drawnDataSets = renderer.render(gc, this, dataSetOffset, getDatasets());
            dataSetOffset += drawnDataSets == null ? 0 : drawnDataSets.size();
        }

        if (gridRenderer.isDrawOnTop()) {
            gridRenderer.render(gc, this, 0, null);
        }
        setAutoNotification(true);
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("   xychart redrawCanvas() - done");
        }
    }

    protected static void updateNumericAxis(final Axis axis, final List<DataSet> dataSets) {
        if (dataSets == null || dataSets.isEmpty()) {
            return;
        }
        final boolean oldAutoState = axis.autoNotification().getAndSet(false);
        final double oldMin = axis.getAutoRange().getMin();
        final double oldMax = axis.getAutoRange().getMax();
        final double oldLength = axis.getLength();

        final boolean isHorizontal = axis.getSide().isHorizontal();
        final Side side = axis.getSide();
        axis.getAutoRange().clear();
        dataSets.stream().filter(DataSet::isVisible).forEach(dataset -> dataset.lock().readLockGuard(() -> {
            if (dataset.getDimension() > 2 && (side == Side.RIGHT || side == Side.TOP)) {
                if (!dataset.getAxisDescription(DataSet.DIM_Z).isDefined()) {
                    dataset.recomputeLimits(DataSet.DIM_Z);
                }
                axis.getAutoRange().add(dataset.getAxisDescription(DataSet.DIM_Z).getMin());
                axis.getAutoRange().add(dataset.getAxisDescription(DataSet.DIM_Z).getMax());
            } else {
                final int nDim = isHorizontal ? DataSet.DIM_X : DataSet.DIM_Y;
                if (!dataset.getAxisDescription(nDim).isDefined()) {
                    dataset.recomputeLimits(nDim);
                }
                axis.getAutoRange().add(dataset.getAxisDescription(nDim).getMin());
                axis.getAutoRange().add(dataset.getAxisDescription(nDim).getMax());
            }
        }));

        // handling of numeric axis and auto-range or auto-grow setting only
        if (!axis.isAutoRanging() && !axis.isAutoGrowRanging()) {
            if (oldMin != axis.getMin() || oldMax != axis.getMax() || oldLength != axis.getLength()) {
                axis.requestAxisLayout();
            }
            axis.autoNotification().set(oldAutoState);
            return;
        }

        if (axis.isAutoGrowRanging()) {
            axis.getAutoRange().add(oldMin);
            axis.getAutoRange().add(oldMax);
        }

        axis.getAutoRange().setAxisLength(axis.getLength() == 0 ? 1 : axis.getLength(), side);
        axis.getUserRange().setAxisLength(axis.getLength() == 0 ? 1 : axis.getLength(), side);
        axis.invalidateRange(null);

        if (oldMin != axis.getMin() || oldMax != axis.getMax() || oldLength != axis.getLength()) {
            axis.requestAxisLayout();
        }
        axis.autoNotification().set(oldAutoState);
    }
}
