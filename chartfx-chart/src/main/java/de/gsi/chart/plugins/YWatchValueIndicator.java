/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.event.EventSource;
import javafx.geometry.*;
import javafx.scene.input.MouseEvent;

/**
 * A horizontal line drawn on the plot area, indicating specified Y value, with an optional {@link #textProperty() text
 * label} describing the value.
 * <p>
 * Style Classes (from least to most specific):
 * <ul>
 * <li><b>Label:</b> {@code value-indicator-label, y-value-indicator-label, y-value-indicator-label[index]}</li>
 * <li><b>Line:</b> {@code value-indicator-line, y-value-indicator-line, y-value-indicator-line[index]}</li>
 * </ul>
 * where {@code [index]} corresponds to the index (zero based) of this indicator instance added to the
 * {@code XYChartPane}. For example class {@code y-value-indicator-label1} can be used to style label of the second
 * instance of this indicator added to the chart pane.
 *
 * @author mhrabia
 */
public class YWatchValueIndicator extends AbstractSingleValueIndicator implements EventSource, ValueIndicator {

    protected static final String STYLE_CLASS_LABEL = "value-watch-indicator-label";
    protected static final String STYLE_CLASS_LINE = "value-watch-indicator-line";
    protected static final String STYLE_CLASS_MARKER = "value-watch-indicator-marker";

    /**
     * Creates a new instance indicating given Y value belonging to the specified {@code yAxis}.
     *
     * @param axis the axis this indicator is associated with
     * @param value a value to be marked
     */
    public YWatchValueIndicator(final Axis axis, final double value) {
        this(axis, value, null);
    }

    /**
     * Creates a new instance indicating given Y value belonging to the specified {@code yAxis}, with the specified
     * {@link #textProperty() label}.
     *
     * @param axis the axis this indicator is associated with
     * @param value a value to be marked
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     */
    public YWatchValueIndicator(final Axis axis, final double value, final String text) {
        super(axis, value, text);
        setLabelHorizontalAnchor(HPos.RIGHT);
        setLabelPosition(0.975);

        /*
        pickLine.setOnMouseDragged(this::handleDragMouseEvent);
        triangle.setOnMouseDragged(this::handleDragMouseEvent);
        label.setOnMouseDragged(this::handleDragMouseEvent);
         */
    }

    protected void handleDragMouseEvent(final MouseEvent mouseEvent) {
        Point2D c = getChart().getPlotArea().sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        final double yPosData = getAxis().getValueForDisplay(c.getY() - dragDelta.y);

        if (getAxis().isValueOnAxis(yPosData)) {
            valueProperty().set(yPosData);
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
        if (getAxis().getSide().equals(Side.RIGHT)) {
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(0, 0)).getX() + 2;
            triangle.getPoints().setAll(0.0, 0.0, 10.0, 10.0, 50.0, 10.0, 50.0, -10.0, 10.0, -10.0);
        } else {
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(getAxis().getWidth(), 0)).getX() - 2;
            //todo for LEFT side too
            triangle.getPoints().setAll(0.0, 0.0, -8.0, 8.0, -8.0, -8.0);
        }
        final double yPosGlobal = getChart().getPlotForeground().sceneToLocal(getChart().getCanvas().localToScene(0, yPos)).getY();

        if (yPos < minY || yPos > maxY) {
            getChartChildren().clear();
        } else {
            layoutLine(minX, yPos, maxX, yPos);
            layoutMarker(axisPos, yPosGlobal, minX, yPos);
            layoutWatchLabel(new BoundingBox(minX, yPos, maxX - minX, 0), getLabelPosition(),
                    AbstractSingleValueIndicator.MIDDLE_POSITION, axisPos, yPosGlobal);
        }
    }

    protected void layoutWatchLabel(final Bounds bounds, final double hPos, final double vPos, double axisPos, double yPosGlobal) {
        if (label.getText() == null || label.getText().isEmpty()) {
            getChartChildren().remove(label);
            return;
        }

        double xPos = bounds.getMinX();
        double yPos = bounds.getMinY();

        final double width = label.prefWidth(-1);
        final double height = label.prefHeight(width);
        final double baseLine = label.getBaselineOffset();

        label.resizeRelocate(xPos + axisPos, yPos + baseLine, width, height);
        label.toFront();

        if (!getChart().getPlotForeground().getChildren().contains(label)) {
            getChart().getPlotForeground().getChildren().add(label);
        }
    }

    @Override
    public void updateStyleClass() {
        setStyleClasses(label, "y-", STYLE_CLASS_LABEL);
        setStyleClasses(line, "y-", STYLE_CLASS_LINE);
        setStyleClasses(triangle, "x-", STYLE_CLASS_MARKER);
    }
}
