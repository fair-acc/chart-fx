package de.gsi.chart.plugins;

import java.util.Arrays;
import java.util.Comparator;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
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
 * @author mhrabia
 * @author afischer (modified)
 */
public class YWatchValueIndicator extends AbstractSingleValueIndicator implements EventSource, ValueIndicator {
    protected static final String STYLE_CLASS_LABEL = "value-watch-indicator-label";
    protected static final String STYLE_CLASS_LINE = "value-watch-indicator-line";
    protected static final String STYLE_CLASS_MARKER = "value-watch-indicator-marker";
    public static final String WATCH_INDICATOR_PREVENT_OCCLUSION = "WatchIndicatorPreventOcclusion";

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
        setText(getAxis().getTickMarkLabel(value));
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

        final double halfHeight = label.getHeight() / 2;
        final double width = label.getWidth() + 10;
        final double yPos = minY + getAxis().getDisplayPosition(getValue());
        final double axisPos;
        final boolean isRightSide = getAxis().getSide().equals(Side.RIGHT);
        if (isRightSide) {
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(0, 0)).getX() + 2;
        } else {
            axisPos = getChart().getPlotForeground().sceneToLocal(getAxis().getCanvas().localToScene(getAxis().getWidth(), 0)).getX() - 2;
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
            layoutWatchLabel(axisPos, yPosGlobal, isRightSide, halfHeight, width);
        }
    }

    @Override
    protected void layoutLine(double startX, double startY, double endX, double endY) {
        if (!line.isVisible()) {
            return;
        }
        super.layoutLine(startX, startY, endX, endY);
    }

    protected void layoutWatchLabel(final double x, double y, boolean isRightSide, final Double halfHeight, final Double width) {
        if (label.getText() == null || label.getText().isEmpty()) {
            getChartChildren().remove(label);
            return;
        }

        final double labelWidth = label.prefWidth(-1);
        final double labelHeight = label.prefHeight(labelWidth);
        final double padding = 2.0;
        if (isPreventOcclusion()) { // iterate over all indicators and move them so they don't overlap
            final YWatchValueIndicator[] indicators = getChart().getPlugins().filtered(p -> p instanceof YWatchValueIndicator && ((YWatchValueIndicator) p).getAxis() == getAxis()).toArray(new YWatchValueIndicator[0]);
            Arrays.sort(indicators, Comparator.comparingDouble(AbstractSingleValueIndicator::getValue).reversed());
            final double[] movedPosition = Arrays.stream(indicators).mapToDouble(ind -> ind.triangle.getTranslateY()).toArray();
            for (int i = 0; i < movedPosition.length - 1; i++) { // calculate new positions
                final double diff = movedPosition[i + 1] - movedPosition[i];
                if (labelHeight + padding > diff) {
                    final double offset = halfHeight - 0.5 * diff + 0.5 * padding; // amount to move indicators up/down
                    movedPosition[i + 1] += offset;
                    movedPosition[i] -= offset;
                    for (int j = i - 1; j >= 0; j--) { // check if lower markers have to be moved
                        final double diff2 = movedPosition[j + 1] - movedPosition[j];
                        if (labelHeight + padding > diff2) {
                            movedPosition[j] -= 2 * halfHeight - diff2;
                        }
                    }
                }
            }
            for (int i = 0; i < movedPosition.length; i++) { // layout markers and labels to new positions
                final double offset = movedPosition[i] - indicators[i].triangle.getTranslateY();
                final double halfHeightPos = offset + halfHeight;
                final double halfHeightNeg = offset - halfHeight;
                if (isRightSide) {
                    indicators[i].triangle.getPoints().setAll(0.0, 0.0, 10.0, halfHeightPos, width, halfHeightPos, width, halfHeightNeg, 10.0, halfHeightNeg);
                } else {
                    indicators[i].triangle.getPoints().setAll(0.0, 0.0, -10.0, halfHeightPos, -width, halfHeightPos, -width, halfHeightNeg, -10.0, -halfHeightNeg);
                }
                indicators[i].label.resizeRelocate(isRightSide ? x : x - labelWidth, indicators[i].triangle.getTranslateY() - 0.5 * labelHeight + offset, labelWidth, labelHeight);
            }
        } else { // just draw the labels at their position
            if (isRightSide) {
                triangle.getPoints().setAll(0.0, 0.0, 10.0, halfHeight, width, halfHeight, width, -halfHeight, 10.0, -halfHeight);
            } else {
                triangle.getPoints().setAll(0.0, 0.0, -10.0, halfHeight, -width, halfHeight, -width, -halfHeight, -10.0, -halfHeight);
            }
            label.resizeRelocate(isRightSide ? x : x - labelWidth, y - 0.5 * labelHeight, labelWidth, labelHeight);
        }
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

    /**
     * @return {@code true} if occlusion detection is disabled for this indicator's axis
     */
    public boolean isPreventOcclusion() {
        return (getAxis() instanceof Node) && ((Node) getAxis()).getProperties().get(WATCH_INDICATOR_PREVENT_OCCLUSION) == Boolean.TRUE;
    }

    /**
     * @param state true: prevent occlusion of indicator labels for this indicator's axis
     */
    public void setPreventOcclusion(final boolean state) {
        if (state) {
            ((Node) getAxis()).getProperties().put(WATCH_INDICATOR_PREVENT_OCCLUSION, true);
        } else {
            ((Node) getAxis()).getProperties().remove(WATCH_INDICATOR_PREVENT_OCCLUSION);
        }
    }
}
