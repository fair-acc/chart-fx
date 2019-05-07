package de.gsi.chart;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.DataSet3D;
import de.gsi.chart.renderer.PolarTickStep;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.GridRenderer;
import de.gsi.chart.renderer.spi.LabelledMarkerRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.AssertUtils;
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
 * GPLv3 which -- in the spirit of 'Ship of Theseus' -- makes it de-facto a new development. Contributions, bug-fixes,
 * and modifications are welcome. Hope you find this library useful and enjoy!
 *
 * @author braeun
 * @author rstein
 */
public class XYChart extends Chart {

    protected static final int BURST_LIMIT_MS = 15;
    protected BooleanProperty polarPlot = new SimpleBooleanProperty(this, "polarPlot", false);
    private final ObjectProperty<PolarTickStep> polarStepSize = new SimpleObjectProperty<>(PolarTickStep.THIRTY);
    private final GridRenderer gridRenderer = new GridRenderer(this);
    private long lastCanvasUpdate = 0;
    private boolean callCanvasUpdateLater = false;
    final ChangeListener<Side> axisSideChangeListener = this::axisSideChanged;

    public XYChart() {
        this(new DefaultNumericAxis(), new DefaultNumericAxis());
    }

    /**
     * Construct a new XYChart with the given axes.
     *
     * @param xAxis the axis to use as primary x-Axis
     * @param yAxis the axis to use as primary y-Axis
     */
    public XYChart(final Axis xAxis, final Axis yAxis) {
        Objects.requireNonNull(xAxis, "X axis is required");
        Objects.requireNonNull(yAxis, "Y axis is required");

        if (xAxis.getSide() == null || !xAxis.getSide().isHorizontal()) {
            xAxis.setSide(Side.BOTTOM);
        }

        if (yAxis.getSide() == null || !yAxis.getSide().isVertical()) {
            yAxis.setSide(Side.LEFT);
        }

        // add XYChart specific axis handling (ie. placement around charts, add
        // new DefaultNumericAxis if one is
        // missing, etc.)

        getAxes().addAll(xAxis, yAxis);

        // getDatasets().addListener(datasetChangeListener);

        // initChartDataSetListener();

        getRenderers().add(new ErrorDataSetRenderer());

        setAnimated(false);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
    }

    /**
     * whether renderer should use polar coordinates (x -> interpreted as phi, y as radial coordinate)
     *
     * @return true if renderer is plotting in polar coordinates
     */
    public final boolean isPolarPlot() {
        return polarPlotProperty().get();
    }

    /**
     * Sets whether renderer should use polar coordinates (x -> interpreted as phi, y as radial coordinate)
     *
     * @param state true if renderer is parallelising sub-functionalities
     * @return itself (fluent design)
     */
    public final XYChart setPolarPlot(final boolean state) {
        polarPlotProperty().set(state);
        return this;
    }

    public ObjectProperty<PolarTickStep> polarStepSizeProperty() {
        return polarStepSize;
    }

    public void setPolarStepSize(final PolarTickStep step) {
        polarStepSizeProperty().set(step);
    }

    public PolarTickStep getPolarStepSize() {
        return polarStepSizeProperty().get();
    }

    /**
     * Sets whether renderer should use polar coordinates (x -> interpreted as phi, y as radial coordinate)
     *
     * @return true if renderer is plotting in polar coordinates
     */
    public final BooleanProperty polarPlotProperty() {
        return polarPlot;
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

    /**
     * Indicates whether vertical grid lines are visible or not.
     *
     * @return verticalGridLinesVisible property
     */
    public final BooleanProperty verticalGridLinesVisibleProperty() {
        return gridRenderer.verticalGridLinesVisibleProperty();
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
     * Sets the value of the {@link #verticalGridLinesVisibleProperty()}.
     *
     * @param value {@code true} to make vertical lines visible
     */
    public final void setVerticalGridLinesVisible(final boolean value) {
        verticalGridLinesVisibleProperty().set(value);
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
     * @return nomen est omen
     */
    public GridRenderer getGridRenderer() {
        return gridRenderer;
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
     * Sets the value of the {@link #verticalGridLinesVisibleProperty()}.
     *
     * @param value {@code true} to make vertical lines visible
     */
    public final void setHorizontalGridLinesVisible(final boolean value) {
        horizontalGridLinesVisibleProperty().set(value);
    }

    private boolean isDataEmpty() {
        return getAllDatasets() == null || getAllDatasets().isEmpty();
    }

    @Override
    public void updateAxisRange() {
        if (isDataEmpty()) {
            return;
        }

        getAxes().forEach(chartAxis -> {
            final List<DataSet> dataSets = getDataSetForAxis(chartAxis);
            updateNumericAxis(chartAxis, dataSets);
            // chartAxis.requestAxisLayout();
        });

        return;
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

    protected void updateNumericAxis(final Axis axis, final List<DataSet> dataSets) {
        if (dataSets != null && dataSets.isEmpty()) {
            return;
        }
        final boolean oldFlag = axis.isAutoNotification();
        axis.setAutoNotifaction(false);

        // TODO: add new auto-ranging here
        final boolean isHorizontal = axis.getSide().isHorizontal();
        final Side side = axis.getSide();
        axis.getAutoRange().empty();
        dataSets.forEach(dataset -> {
            if (dataset instanceof DataSet3D && (side == Side.RIGHT || side == Side.TOP)) {
                final DataSet3D mDataSet = (DataSet3D) dataset;
                axis.getAutoRange().add(mDataSet.getZRange().getMin());
                axis.getAutoRange().add(mDataSet.getZRange().getMax());
            } else {
                axis.getAutoRange().add(isHorizontal ? dataset.getXMin() : dataset.getYMin());
                axis.getAutoRange().add(isHorizontal ? dataset.getXMax() : dataset.getYMax());
            }
        });
        axis.getAutoRange().setAxisLength(axis.getLength() == 0 ? 1 : axis.getLength(), side);

        // handling of numeric axis and auto-range or auto-grow setting only
        if (!axis.isAutoRanging() && !axis.isAutoGrowRanging()) {
            axis.setAutoNotifaction(oldFlag);
            return;
        }

        final List<Number> dataMinMax = new ArrayList<>();
        dataSets.forEach(dataset -> {
            if (dataset instanceof DataSet3D && (side == Side.RIGHT || side == Side.TOP)) {
                final DataSet3D mDataSet = (DataSet3D) dataset;
                dataMinMax.add(mDataSet.getZRange().getMin());
                dataMinMax.add(mDataSet.getZRange().getMax());
            } else {
                dataMinMax.add(isHorizontal ? dataset.getXMin() : dataset.getYMin());
                dataMinMax.add(isHorizontal ? dataset.getXMax() : dataset.getYMax());
            }
        });

        if (axis.isAutoGrowRanging()) {
            dataMinMax.add(axis.getLowerBound());
            dataMinMax.add(axis.getUpperBound());
        }

        // work-around since we cannot overwrite the method 'autorange(min,max)'
        // in ValueAxis
        if (axis.isAutoGrowRanging()) {
            double min = +Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (final Number val : dataMinMax) {
                min = Math.min(min, val.doubleValue());
                max = Math.max(max, val.doubleValue());
            }
            axis.setLowerBound(min);
            axis.setUpperBound(max);
        } else {
            axis.invalidateRange(dataMinMax);
        }
        axis.setAutoNotifaction(oldFlag);
        return;
    }

    @Override
    protected void redrawCanvas() {
        if (DEBUG) {
            System.err.println("   xychart redrawCanvas() - pre");
        }
        setAutoNotifaction(false);

        final long now = System.nanoTime();
        final double diffMillisSinceLastUpdate = TimeUnit.NANOSECONDS.toMillis(now - lastCanvasUpdate);
        if (diffMillisSinceLastUpdate < XYChart.BURST_LIMIT_MS) {
            if (!callCanvasUpdateLater) {
                callCanvasUpdateLater = true;
                // repaint 20 ms later in case this was just a burst operation
                // final KeyFrame kf1 = new KeyFrame(Duration.millis(20), e ->
                // redrawCanvas());
                // final KeyFrame kf1 = new KeyFrame(Duration.millis(20), e ->
                // layoutChildren());
                final KeyFrame kf1 = new KeyFrame(Duration.millis(20), e -> requestLayout());

                final Timeline timeline = new Timeline(kf1);
                Platform.runLater(timeline::play);
            }
            // System.err.println("XYChart burst diffMillisSinceLastUpdate = " +
            // diffMillisSinceLastUpdate);
            return;
        }
        if (DEBUG) {
            System.err.println("   xychart redrawCanvas() - executing");
            System.err.println("   xychart redrawCanvas() - canvas size = "
                    + String.format("%fx%f", canvas.getWidth(), canvas.getHeight()));
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
            renderer.render(gc, this, dataSetOffset, getDatasets());
            dataSetOffset += getDatasets().size() + renderer.getDatasets().size();
        }

        if (gridRenderer.isDrawOnTop()) {
            gridRenderer.render(gc, this, 0, null);
        }
        setAutoNotifaction(true);
        if (DEBUG) {
            System.err.println("   xychart redrawCanvas() - done");
        }
    }

    protected void axisSideChanged(final ObservableValue<? extends Side> change, final Side oldValue,
            final Side newValue) {
        // loop through all registered axis
        for (final Axis axis : axesList) {

            if (axis.getSide() == null) {
                // remove axis from all axis panes
                removeFromAllAxesPanes(axis);
            }

            // check if axis is in correct pane
            if (getAxesPane(axis.getSide()).getChildren().contains(axis)) {
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
                // -> throw an exception
                // remove from axis.side property side listener
                set.sideProperty().removeListener(axisSideChangeListener);
            });
            for (final Axis set : change.getAddedSubList()) {
                // check if axis is associated with an existing renderer,
                // if yes -> throw an exception
                AssertUtils.notNull("to be added axis is null", set);

                if (!(set instanceof Axis) && !(set instanceof CategoryAxis)) {
                    throw new InvalidParameterException(
                            new StringBuilder().append("do not know what to do with axis ").append(set.getLabel())
                                    .append(" type ").append(set.getClass().toGenericString()).toString());
                }
                final Side side = set.getSide();
                if (side == null) {
                    throw new InvalidParameterException(new StringBuilder().append("axis '").append(set.getLabel())
                            .append("' has 'null' as side being set").toString());
                }
                if (!getAxesPane(set.getSide()).getChildren().contains(set)) {
                    getAxesPane(set.getSide()).getChildren().add((Node) set);
                } else {
                    // axis already added to correct axis pane
                }

                set.sideProperty().addListener(axisSideChangeListener);

            }
        }

        requestLayout();
    }

}
