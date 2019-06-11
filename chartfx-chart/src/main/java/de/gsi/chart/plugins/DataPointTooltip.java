/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import java.util.LinkedList;
import java.util.List;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

/**
 * A tool tip label appearing next to the mouse cursor when placed over a data point's symbol. If symbols are not
 * created/shown for given plot, the tool tip is shown for the closest data point that is within the
 * {@link #pickingDistanceProperty()} from the mouse cursor.
 * <p>
 * CSS style class name: {@value #STYLE_CLASS_LABEL}
 *
 * @author Grzegorz Kruk TODO: extend so that label = new Label(); is a generic object and can also be overwritten with
 *         another implementation (&lt;-&gt; advanced interactor) additional add/remove listener are needed to edit/update the
 *         custom object based on DataPoint (for the time being private class)
 */
public class DataPointTooltip extends AbstractDataFormattingPlugin {

    /**
     * Name of the CSS class of the tool tip label.
     */
    public static final String STYLE_CLASS_LABEL = "chart-datapoint-tooltip-label";

    /**
     * The default distance between the data point coordinates and mouse cursor that triggers showing the tool tip
     * label.
     */
    public static final int DEFAULT_PICKING_DISTANCE = 5;

    private static final int LABEL_X_OFFSET = 15;
    private static final int LABEL_Y_OFFSET = 5;

    private final Label label = new Label();

    /**
     * Creates a new instance of DataPointTooltip class with {{@link #pickingDistanceProperty() picking distance}
     * initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     */
    public DataPointTooltip() {
        label.getStyleClass().add(DataPointTooltip.STYLE_CLASS_LABEL);
        registerInputEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler);
    }

    /**
     * Creates a new instance of DataPointTooltip class.
     *
     * @param pickingDistance the initial value for the {@link #pickingDistanceProperty() pickingDistance} property
     */
    public DataPointTooltip(final double pickingDistance) {
        this();
        setPickingDistance(pickingDistance);
    }

    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance",
            DataPointTooltip.DEFAULT_PICKING_DISTANCE) {

        @Override
        protected void invalidated() {
            if (get() <= 0) {
                throw new IllegalArgumentException("The " + getName() + " must be a positive value");
            }
        }
    };

    /**
     * Distance of the mouse cursor from the data point (expressed in display units) that should trigger showing the
     * tool tip. By default initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
    }

    /**
     * Returns the value of the {@link #pickingDistanceProperty()}.
     *
     * @return the current picking distance
     */
    public final double getPickingDistance() {
        return pickingDistanceProperty().get();
    }

    /**
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance the new picking distance
     */
    public final void setPickingDistance(final double distance) {
        pickingDistanceProperty().set(distance);
    }

    private final EventHandler<MouseEvent> mouseMoveHandler = this::updateToolTip;

    private void updateToolTip(final MouseEvent event) {
        final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();
        // final Bounds plotAreaBounds = getChartPane().getPlotAreaBounds();
        final DataPoint dataPoint = findDataPoint(event, plotAreaBounds);

        if (dataPoint == null) {
            getChartChildren().remove(label);
            return;
        }
        updateLabel(event, plotAreaBounds, dataPoint);
        if (!getChartChildren().contains(label)) {
            getChartChildren().add(label);
            label.requestLayout();
        }
    }

    private DataPoint findDataPoint(final MouseEvent event, final Bounds plotAreaBounds) {
        if (!plotAreaBounds.contains(event.getX(), event.getY())) {
            return null;
        }

        final Point2D mouseLocation = getLocationInPlotArea(event);
        DataPoint nearestDataPoint = null;

        Chart chart = getChart();
        final DataPoint point = findNearestDataPointWithinPickingDistance(chart, mouseLocation);
        if (nearestDataPoint == null || point != null && point.distanceFromMouse < nearestDataPoint.distanceFromMouse) {
            nearestDataPoint = point;
        }

        return nearestDataPoint;
    }

    private DataPoint findNearestDataPointWithinPickingDistance(final Chart chart, final Point2D mouseLocation) {
        DataPoint nearestDataPoint = null;
        if (!(chart instanceof XYChart)) {
            return null;
        }
        final XYChart xyChart = (XYChart) chart;
        // final double xValue = toDataPoint(xyChart.getYAxis(),
        // mouseLocation).getXValue().doubleValue();
        // TODO: iterate through all axes, renderer and datasets
        final double xValue = xyChart.getXAxis().getValueForDisplay(mouseLocation.getX());

        for (final DataPoint dataPoint : findNeighborPoints(xyChart, xValue)) {
            // Point2D displayPoint = toDisplayPoint(chart.getYAxis(),
            // (X)dataPoint.x , dataPoint.y);
            if (getChart().getFirstAxis(Orientation.HORIZONTAL) instanceof Axis) {
                final double x = xyChart.getXAxis().getDisplayPosition(dataPoint.x);
                final double y = xyChart.getYAxis().getDisplayPosition(dataPoint.y);
                final Point2D displayPoint = new Point2D(x, y);
                dataPoint.distanceFromMouse = displayPoint.distance(mouseLocation);
                if (displayPoint.distance(mouseLocation) <= getPickingDistance() && (nearestDataPoint == null
                        || dataPoint.distanceFromMouse < nearestDataPoint.distanceFromMouse)) {
                    nearestDataPoint = dataPoint;
                }
            }
        }
        return nearestDataPoint;
    }

    private List<DataPoint> findNeighborPoints(final XYChart chart, final double searchedX) {
        final List<DataPoint> points = new LinkedList<>();
        for (final DataSet dataSet : chart.getAllDatasets()) {
            final Pair<DataPoint, DataPoint> neighborPoints = findNeighborPoints(dataSet, searchedX);
            if (neighborPoints.getKey() != null) {
                points.add(neighborPoints.getKey());
            }
            if (neighborPoints.getValue() != null) {
                points.add(neighborPoints.getValue());
            }
        }
        return points;
    }

    /**
     * Handles series that have data sorted or not sorted with respect to X coordinate.
     * @param dataSet data set
     * @param searchedX x coordinate
     * @return return neighouring data points
     */
    private Pair<DataPoint, DataPoint> findNeighborPoints(final DataSet dataSet, final double searchedX) {
        int prevIndex = -1;
        int nextIndex = -1;
        double prevX = Double.MIN_VALUE;
        double nextX = Double.MAX_VALUE;

        final int nDataCount = (dataSet instanceof DataSet3D) ? ((DataSet3D) dataSet).getXDataCount()
                : dataSet.getDataCount();
        for (int i = 0, size = nDataCount; i < size; i++) {
            final double currentX = dataSet.getX(i);

            if (currentX < searchedX) {
                if (prevX < currentX) {
                    prevIndex = i;
                    prevX = currentX;
                }
            } else if (nextX > currentX) {
                nextIndex = i;
                nextX = currentX;
            }
        }
        final DataPoint prevPoint = prevIndex == -1 ? null
                : new DataPoint(getChart(), dataSet.getX(prevIndex), dataSet.getY(prevIndex),
                        dataSet.getDataLabel(prevIndex));
        final DataPoint nextPoint = nextIndex == -1 || nextIndex == prevIndex ? null
                : new DataPoint(getChart(), dataSet.getX(nextIndex), dataSet.getY(nextIndex),
                        dataSet.getDataLabel(nextIndex));

        return new Pair<>(prevPoint, nextPoint);
    }

    // @SuppressWarnings({ "rawtypes", "unchecked" })
    // private List<Data<? extends Number, Y>> castXToNumber(final Series
    // series) {
    // return series.getData();
    // }

    private void updateLabel(final MouseEvent event, final Bounds plotAreaBounds, final DataPoint dataPoint) {
        label.setText(formatLabel(dataPoint));
        // TODO continue here (new formatting etc.)
        final double mouseX = event.getX();
        final double mouseY = event.getY();
        final double width = label.prefWidth(-1);
        final double height = label.prefHeight(width);

        double xLocation = mouseX + DataPointTooltip.LABEL_X_OFFSET;
        double yLocation = mouseY - DataPointTooltip.LABEL_Y_OFFSET - height;

        if (xLocation + width > plotAreaBounds.getMaxX()) {
            xLocation = mouseX - DataPointTooltip.LABEL_X_OFFSET - width;
        }
        if (yLocation < plotAreaBounds.getMinY()) {
            yLocation = mouseY + DataPointTooltip.LABEL_Y_OFFSET;
        }
        label.resizeRelocate(xLocation, yLocation, width, height);
    }

    private String formatDataPoint(final DataPoint dataPoint) {
        return String.format("DataPoint@(%.3f,%.3f)", dataPoint.x, dataPoint.y);
        // return formatData(dataPoint.chart.getYAxis(), dataPoint.x,
        // dataPoint.y);
    }

    protected String formatLabel(DataPoint dataPoint) {
        return String.format("'%s'\n%s", dataPoint.label, formatDataPoint(dataPoint));
    }

    protected class DataPoint {

        final Chart chart;
        final double x;
        final double y;
        final String label;
        double distanceFromMouse;

        DataPoint(final Chart chart, final double x, final double y, final String label) {
            this.chart = chart;
            this.x = x;
            this.y = y;
            this.label = label;
        }

        public Chart getChart() {
            return chart;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public String getLabel() {
            return label;
        }

        public double getDistanceFromMouse() {
            return distanceFromMouse;
        }

    }
}
