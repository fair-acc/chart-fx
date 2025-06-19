/*
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */
package io.fair_acc.chartfx.plugins;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.spi.utils.Tuple;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Direct adopt and modified from {@link DataPointTooltip}
 *
 * @author lacgit
 */
public class OhlcvTooltip extends AbstractDataFormattingPlugin {
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

    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance", OhlcvTooltip.DEFAULT_PICKING_DISTANCE) {
        @Override
        protected void invalidated() {
            if (get() <= 0) {
                throw new IllegalArgumentException("The " + getName() + " must be a positive value");
            }
        }
    };

    private final EventHandler<MouseEvent> mouseMoveHandler = this::updateToolTip;

    /**
     * Creates a new instance of DataPointTooltip class with {{@link #pickingDistanceProperty() picking distance}
     * initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     */
    public OhlcvTooltip() {
        label.getStyleClass().add(OhlcvTooltip.STYLE_CLASS_LABEL);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setManaged(false);
        setXValueFormatter(new StringConverter<Number>() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
            @Override
            public String toString(Number number) {
                if (number == null) return "";
                return dateFormat.format(new Date(number.longValue()*1000));
            }

            @Override
            public Number fromString(String s) {
                return null;
            }
        });
        setYValueFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number number) {
                return String.format("%,d%n", number.intValue());
            }

            @Override
            public Number fromString(String s) {
                return null;
            }
        });
        registerInputEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler);
    }

    /**
     * Creates a new instance of DataPointTooltip class.
     *
     * @param pickingDistance the initial value for the {@link #pickingDistanceProperty() pickingDistance} property
     */
    public OhlcvTooltip(final double pickingDistance) {
        this();
        setPickingDistance(pickingDistance);
    }

    protected Optional<DataPoint> findDataPoint(final MouseEvent event, final Bounds plotAreaBounds) {
        if (!plotAreaBounds.contains(event.getX(), event.getY())) {
            return Optional.empty();
        }

        final Point2D mouseLocation = getLocationInPlotArea(event);

        return findNearestDataPointWithinPickingDistance(mouseLocation);
    }

    protected Optional<DataPoint> findNearestDataPointWithinPickingDistance(final Point2D mouseLocation) {
        final Chart chart = getChart();
        if (!(chart instanceof XYChart)) {
            return Optional.empty();
        }

        final XYChart xyChart = (XYChart) chart;
        final ObservableList<DataSet> xyChartDatasets = xyChart.getDatasets();
        return xyChart.getRenderers().stream() // for all renderers
                .flatMap(renderer -> Stream.of(renderer.getDatasets(), xyChartDatasets) //
                                             .flatMap(List::stream) // combine global and renderer specific Datasets
                                             .flatMap(dataset -> getPointsCloseToCursor(dataset, renderer, mouseLocation))) // get points in range of cursor
                .reduce((p1, p2) -> p1.distanceFromMouse <= p2.distanceFromMouse ? p1 : p2); // find closest point, tie-breaking in favor of earlier data sets to match rendering order
    }

    protected Stream<DataPoint> getPointsCloseToCursor(final DataSet dataset, final Renderer renderer, final Point2D mouseLocation) {
        // Get Axes for the Renderer
        final Axis xAxis = findXAxis(renderer);
        final Axis yAxis = findYAxis(renderer);
        if (xAxis == null || yAxis == null) {
            return Stream.empty(); // ignore this renderer because there are no valid axes available
        }

        if (dataset instanceof GridDataSet) {
            return Stream.empty(); // TODO: correct impl for grid data sets
        }

        return dataset.lock().readLockGuard(() -> {
            int minIdx = 0;
            int maxIdx = dataset.getDataCount();

            if (isDataSorted(renderer)) {
                // get the screen x coordinates and dataset indices between which points can be in picking distance
                final double xMin = xAxis.getValueForDisplay(mouseLocation.getX() - getPickingDistance());
                final double xMax = xAxis.getValueForDisplay(mouseLocation.getX() + getPickingDistance());

                minIdx = Math.max(0, dataset.getIndex(DataSet.DIM_X, xMin) - 1);
                maxIdx = Math.min(dataset.getDataCount(), dataset.getIndex(DataSet.DIM_X, xMax) + 1);
            }

            return IntStream.range(minIdx, maxIdx) // loop over all candidate points
                    .mapToObj(i -> getDataPointFromDataSet(renderer, dataset, xAxis, yAxis, mouseLocation, i)) // get points with distance to mouse
                    .filter(p -> p.distanceFromMouse <= getPickingDistance()) // filter out points which are too far away
                    .map(dataPoint -> dataPoint.withFormattedLabel(formatLabel(dataPoint)))
                    .collect(Collectors.toList()) // Realize list so that calculations are done within the data set lock
                    .stream();
        });
    }

    private boolean isDataSorted(final Renderer renderer) {
        return renderer instanceof ErrorDataSetRenderer && ((ErrorDataSetRenderer) renderer).isAssumeSortedData();
    }

    private Axis findYAxis(final Renderer renderer) {
        return renderer.getAxes().stream().filter(ax -> ax.getSide().isVertical()).findFirst().orElse(null);
    }

    private Axis findXAxis(final Renderer renderer) {
        return renderer.getAxes().stream().filter(ax -> ax.getSide().isHorizontal()).findFirst().orElse(null);
    }

    protected DataPoint getDataPointFromDataSet(final Renderer renderer, final DataSet dataset, final Axis xAxis, final Axis yAxis, final Point2D mouseLocation, final int index) {
        final double xValue = dataset.get(DataSet.DIM_X, index);
        final double yValue = dataset.get(DataSet.DIM_Y, index);

        final double displayPositionX = xAxis.getDisplayPosition(xValue);
        final double displayPositionY = yAxis.getDisplayPosition(yValue);
        final double distanceFromMouseLocation = new Point2D(displayPositionX, displayPositionY).distance(mouseLocation);

        final String dataLabelSafe = getDataLabelSafe(dataset, index);

        return new DataPoint( //
                renderer, //
                xValue, //
                yValue, //
                dataLabelSafe, //
                distanceFromMouseLocation);
    }

    protected String formatDataPoint(final DataPoint dataPoint) {
        return formatData(dataPoint.renderer, new Tuple<>(dataPoint.x, dataPoint.y));
    }

    protected String formatLabel(DataPoint dataPoint) {
        return String.format("'%s'%n%s", dataPoint.label, formatDataPoint(dataPoint));
    }

    protected String getDataLabelSafe(final DataSet dataSet, final int index) {
        String labelString = dataSet.getDataLabel(index);
        if (labelString == null) {
            return String.format("%s [%d]", dataSet.getName(), index);
        }
        return labelString;
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
     * Distance of the mouse cursor from the data point (expressed in display units) that should trigger showing the
     * tool tip. By default initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
    }

    /**
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance the new picking distance
     */
    public final void setPickingDistance(final double distance) {
        pickingDistanceProperty().set(distance);
    }

    protected void updateLabel(final MouseEvent event, final Bounds plotAreaBounds, final DataPoint dataPoint) {
        label.setText(dataPoint.formattedLabel);
        final double mouseX = event.getX();
        final double spaceLeft = mouseX - plotAreaBounds.getMinX();
        final double spaceRight = plotAreaBounds.getWidth() - spaceLeft;
        double width = label.prefWidth(-1);
        boolean atSide = true; // set to false if we cannot print the tooltip beside the point

        double xLocation;
        if (spaceRight >= width + LABEL_X_OFFSET) { // place to right if enough space
            xLocation = mouseX + OhlcvTooltip.LABEL_X_OFFSET;
        } else if (spaceLeft >= width + LABEL_X_OFFSET) { // place left if enough space
            xLocation = mouseX - OhlcvTooltip.LABEL_X_OFFSET - width;
        } else if (width < plotAreaBounds.getWidth()) {
            xLocation = spaceLeft > spaceRight ? plotAreaBounds.getMaxX() - width : plotAreaBounds.getMinX();
            atSide = false;
        } else {
            width = plotAreaBounds.getWidth();
            xLocation = plotAreaBounds.getMinX();
            atSide = false;
        }

        final double mouseY = event.getY();
        final double spaceTop = mouseY - plotAreaBounds.getMinY();
        final double spaceBottom = plotAreaBounds.getHeight() - spaceTop;
        double height = label.prefHeight(width);

        double yLocation;
        if (height < spaceBottom) {
            yLocation = mouseY + OhlcvTooltip.LABEL_Y_OFFSET;
        } else if (height < spaceTop) {
            yLocation = mouseY - OhlcvTooltip.LABEL_Y_OFFSET - height;
        } else if (atSide && height < plotAreaBounds.getHeight()) {
            yLocation = spaceTop < spaceBottom ? plotAreaBounds.getMaxY() - height : plotAreaBounds.getMinY();
        } else if (atSide) {
            yLocation = plotAreaBounds.getMinY();
            height = plotAreaBounds.getHeight();
        } else if (spaceBottom > spaceTop) {
            yLocation = mouseY + OhlcvTooltip.LABEL_Y_OFFSET;
            height = spaceBottom - LABEL_Y_OFFSET;
        } else {
            yLocation = plotAreaBounds.getMinY();
            height = spaceTop - LABEL_Y_OFFSET;
        }
        label.resizeRelocate(xLocation, yLocation, width, height);
    }

    private void updateToolTip(final MouseEvent event) {
        final Bounds plotAreaBounds = getChart().getPlotArea().getBoundsInLocal();
        final Optional<DataPoint> dataPoint = findDataPoint(event, plotAreaBounds);

        if (dataPoint.isEmpty()) {
            getChartChildren().remove(label);
            return;
        }
        updateLabel(event, plotAreaBounds, dataPoint.get());
        if (!getChartChildren().contains(label)) {
            getChartChildren().add(label);
            label.requestLayout();
        }
    }

    public static class DataPoint {
        public final Renderer renderer;
        public final double x;
        public final double y;
        public final String label;
        public final String formattedLabel; // may be empty
        public final double distanceFromMouse;

        public DataPoint(Renderer renderer, double x, double y, String label, double distanceFromMouse, String formattedLabel) {
            this.renderer = renderer;
            this.x = x;
            this.y = y;
            this.label = label;
            this.distanceFromMouse = distanceFromMouse;
            this.formattedLabel = formattedLabel;
        }

        public DataPoint(Renderer renderer, double x, double y, String label, double distanceFromMouse) {
            this(renderer, x, y, label, distanceFromMouse, "");
        }

        public DataPoint withFormattedLabel(String formattedLabel) {
            return new DataPoint(renderer, x, y, formattedLabel, distanceFromMouse, formattedLabel);
        }
    }
}
