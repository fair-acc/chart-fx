/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.event.EventSource;

/**
 * A horizontal line drawn on the plot area, indicating specified Y value with the {@link #textProperty() text
 * label} describing the value inside the Y-Axis marker.
 * <p>
 * Style Classes (from least to most specific):
 * <ul>
 * <li><b>Marker:</b> {@code value-watch-indicator-marker, {id}-value-watch-indicator-marker, {id}-value-watch-indicator-marker[index]}</li>
 * <li><b>Label:</b> {@code value-watch-indicator-label, {id}-value-watch-indicator-label, {id}-value-watch-indicator-label[index]}</li>
 * <li><b>Line:</b> {@code value-watch-indicator-line, {id}-value-watch-indicator-line, {id}-value-watch-indicator-line[index]}</li>
 * </ul>
 * where {@code [index]} corresponds to the index (zero based) of this indicator instance added to the
 * {@code XYChartPane}. For example class {@code y-value-indicator-label1} can be used to style label of the second
 * instance of this indicator added to the chart pane.
 *
 * @author afischer (modified from mhrabia)
 */
public class YWatchValueIndicator extends AbstractSingleValueIndicator implements EventSource, ValueIndicator {
    protected static final String STYLE_CLASS_LABEL = "value-watch-indicator-label";
    protected static final String STYLE_CLASS_LINE = "value-watch-indicator-line";
    protected static final String STYLE_CLASS_MARKER = "value-watch-indicator-marker";

    protected final String valueFormat;
    protected String id;

    /**
     * Creates a new instance indicating given Y value belonging to the specified {@code yAxis}.
     *
     * @param axis the axis this indicator is associated with
     * @param valueFormat a value string format for marker visualization
     * @param value a value to be marked
     */
    public YWatchValueIndicator(final Axis axis, final String valueFormat, final double value) {
        super(axis, value, String.format(valueFormat, value));
        this.valueFormat = valueFormat;

        // marker is visible always for this indicator
        triangle.visibleProperty().unbind();
        triangle.visibleProperty().set(true);

        pickLine.setOnMouseDragged(this::handleDragMouseEvent);
        triangle.setOnMouseDragged(this::handleDragMouseEvent);
        label.setOnMouseDragged(this::handleDragMouseEvent);
    }

    /**
     * Creates a new instance for the specified {@code yAxis}.
     * The Y value is updated by listeners.
     *
     * @param axis the axis this indicator is associated with
     * @param valueFormat a value string format for marker visualization
     */
    public YWatchValueIndicator(final Axis axis, final String valueFormat) {
        this(axis, valueFormat, 0.0);
    }

    /**
     * Set the text and value for this indicator marker.
     *
     * @param value Update marker label and its Y Axis position by this double value.
     */
    public void setMarkerValue(final double value) {
        setText(String.format(valueFormat, value));
        setValue(value);
    }

    /**
     * Set visibility of the horizontal line
     *
     * @param lineVisible line visibility boolean
     */
    public void setLineVisible(final boolean lineVisible) {
        line.setVisible(lineVisible);
        pickLine.setVisible(lineVisible);
    }

    /**
     * Unique identification of the indicator
     *
     * @return id unique ID
     */
    public String getId() {
        return id;
    }

    /**
     * Unique identification of the indicator
     *
     * @param id unique ID
     */
    public void setId(String id) {
        this.id = id;
    }

    protected void handleDragMouseEvent(final MouseEvent mouseEvent) {
        Point2D c = getChart().getPlotArea().sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        final double yPosData = getAxis().getValueForDisplay(c.getY() - dragDelta.y);

        if (getAxis().isValueOnAxis(yPosData)) {
            setMarkerValue(yPosData);
        }

        mouseEvent.consume();
    }

    @Override
    public void layoutChildren() {
        if (getChart() == null) {
            return;
        }
        final Bounds plotAreaBounds = getChart().getCanvas().getBoundsInLocal();
        final double minX = plotAreaBounds.getMinX();
        final double maxX = plotAreaBounds.getMaxX();
        final double minY = plotAreaBounds.getMinY();
        final double maxY = plotAreaBounds.getMaxY();

        final double yPos = minY + getAxis().getDisplayPosition(getValue());
        final double axisPos;
        final boolean isRightSide = getAxis().getSide().equals(Side.RIGHT);
        if (isRightSide) {
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(0, 0)).getX() + 2;
            triangle.getPoints().setAll(0.0, 0.0, 10.0, 10.0, 50.0, 10.0, 50.0, -10.0, 10.0, -10.0);
        } else {
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(getAxis().getWidth(), 0)).getX() - 2;
            triangle.getPoints().setAll(0.0, 0.0, -10.0, 10.0, -50.0, 10.0, -50.0, -10.0, -10.0, -10.0);
        }
        final double yPosGlobal = getChart().getPlotForeground().sceneToLocal(getChart().getCanvas().localToScene(0, yPos)).getY();

        if (yPos < minY || yPos > maxY) {
            getChart().getPlotForeground().getChildren().remove(triangle);
            getChart().getPlotForeground().getChildren().remove(label);
            getChartChildren().remove(line);
            getChartChildren().remove(pickLine);
        } else {
            layoutLine(minX, yPos, maxX, yPos);
            layoutMarker(axisPos, yPosGlobal, minX, yPos);
            layoutWatchLabel(new BoundingBox(minX, yPos, maxX - minX, 0), axisPos, isRightSide);
        }
    }

    @Override
    protected void layoutLine(double startX, double startY, double endX, double endY) {
        if (!line.isVisible()) {
            return;
        }
        super.layoutLine(startX, startY, endX, endY);
    }

    protected void layoutWatchLabel(final Bounds bounds, double axisPos, boolean isRightSide) {
        if (label.getText() == null || label.getText().isEmpty()) {
            getChartChildren().remove(label);
            return;
        }

        double xPos = bounds.getMinX();
        double yPos = bounds.getMinY();

        final double width = label.prefWidth(-1);
        final double height = label.prefHeight(width);
        final double baseLine = label.getBaselineOffset();

        double padding = isRightSide ? 0 : width + label.getPadding().getRight();
        label.resizeRelocate(xPos + axisPos - padding, yPos + baseLine, width, height);
        label.toFront();

        if (!getChart().getPlotForeground().getChildren().contains(label)) {
            getChart().getPlotForeground().getChildren().add(label);
        }
    }

    @Override
    public void updateStyleClass() {
        setStyleClasses(label, getId() + "-", STYLE_CLASS_LABEL);
        setStyleClasses(line, getId() + "-", STYLE_CLASS_LINE);
        setStyleClasses(triangle, getId() + "-", STYLE_CLASS_MARKER);
    }
}
