package io.fair_acc.chartfx.plugins;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.ui.geometry.Side;

/**
 * A vertical line drawn on the plot area, indicating specified X value, with an optional {@link #textProperty() text
 * label} describing the value.
 * <p>
 * Style Classes (from least to most specific):
 * <ul>
 * <li><b>Label:</b> {@code value-indicator-label, x-value-indicator-label, x-value-indicator-label[index]}</li>
 * <li><b>Line:</b> {@code value-indicator-line, x-value-indicator-line, x-value-indicator-line[index]}</li>
 * </ul>
 * where {@code [index]} corresponds to the index (zero based) of this indicator instance added to the
 * {@code XYChartPane}. For example class {@code x-value-indicator-label1} can be used to style label of the second
 * instance of this indicator added to the chart pane.
 *
 * @author mhrabia
 */
public class XValueIndicator extends AbstractSingleValueIndicator implements ValueIndicator {
    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param value a X value to be indicated
     */
    public XValueIndicator(final Axis axis, final double value) {
        this(axis, value, null);
    }

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param value a X value to be indicated
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     */
    public XValueIndicator(final Axis axis, final double value, final String text) {
        super(axis, value, text);
        setLabelPosition(0.04);

        pickLine.setOnMouseDragged(this::handleDragMouseEvent);
        triangle.setOnMouseDragged(this::handleDragMouseEvent);
        label.setOnMouseDragged(this::handleDragMouseEvent);
    }

    protected void handleDragMouseEvent(final MouseEvent mouseEvent) {
        if (!isEditable()) {
            return;
        }
        Point2D c = getChart().getPlotArea().sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        final double xPosData = getAxis().getValueForDisplay(c.getX() + dragDelta.x);
        if (getAxis().isValueOnAxis(xPosData)) {
            valueProperty().set(xPosData);
        }

        mouseEvent.consume();
        runPostLayout();
    }

    @Override
    public void runPostLayout() {
        if (getChart() == null) {
            return;
        }

        final Bounds plotAreaBounds = getChart().getCanvas().getBoundsInLocal();
        final double minX = plotAreaBounds.getMinX();
        final double maxX = plotAreaBounds.getMaxX();
        final double minY = plotAreaBounds.getMinY();
        final double maxY = plotAreaBounds.getMaxY();
        final double xPos = minX + getAxis().getDisplayPosition(getValue());
        final double axisPos;
        if (getAxis().getSide().equals(Side.BOTTOM)) {
            triangle.getPoints().setAll(0.0, -8.0, -8.0, 0.0, 8.0, 0.0);
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(0, 0)).getY() + 6;
        } else {
            triangle.getPoints().setAll(0.0, 0.0, -8.0, -8.0, 8.0, -8.0);
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(0, getAxis().getHeight())).getY() - 6;
        }
        final double xPosGlobal = getChart().getPlotForeground().sceneToLocal(getChart().getCanvas().localToScene(xPos, 0)).getX();

        if (xPos < minX || xPos > maxX) {
            getChartChildren().clear();
        } else {
            layoutLine(xPos, minY, xPos, maxY);
            layoutMarker(xPosGlobal, axisPos + 4, xPos, maxY);

            layoutLabel(new BoundingBox(xPos, minY, 0, maxY - minY), AbstractSingleValueIndicator.MIDDLE_POSITION,
                    getLabelPosition());
        }
    }

    @Override
    public void updateStyleClass() {
        setStyleClasses(label, "x-", AbstractSingleValueIndicator.STYLE_CLASS_LABEL);
        setStyleClasses(line, "x-", AbstractSingleValueIndicator.STYLE_CLASS_LINE);
        setStyleClasses(triangle, "x-", AbstractSingleValueIndicator.STYLE_CLASS_MARKER);
    }
}
