package de.gsi.chart.plugins;

import java.util.Objects;
import java.util.function.Predicate;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.Axes;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.ui.geometry.Side;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;

/**
 * Allows dragging the visible plot area along X and/or Y axis, changing the visible axis range.
 * <p>
 * Reacts on {@link MouseEvent#DRAG_DETECTED} event accepted by {@link #getMouseFilter() mouse filter}.
 * <p>
 * {@code Panner} works properly only if both X and Y axis are instances of {@link DefaultNumericAxis}.
 *
 * @author Grzegorz Kruk
 */
public class Panner extends ChartPlugin {
    /**
     * Default pan mouse filter passing on left mouse button with {@link MouseEvent#isControlDown() control key down}.
     */
    public static final Predicate<MouseEvent> DEFAULT_MOUSE_FILTER = event ->
    // MouseEvents.isOnlyPrimaryButtonDown(event) &&
    // MouseEvents.isOnlyCtrlModifierDown(event) ||
    MouseEventsHelper.isOnlyMiddleButtonDown(event);

    private Predicate<MouseEvent> mouseFilter = Panner.DEFAULT_MOUSE_FILTER;
    private Point2D previousMouseLocation = null;

    private final ObjectProperty<AxisMode> axisMode = new SimpleObjectProperty<AxisMode>(this, "axisMode",
            AxisMode.XY) {
        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(), "The " + getName() + " must not be null");
        }
    };

    private Cursor originalCursor;

    private final ObjectProperty<Cursor> dragCursor = new SimpleObjectProperty<>(this, "dragCursor");

    private final EventHandler<MouseEvent> panStartHandler = event -> {
        if (mouseFilter == null || mouseFilter.test(event)) {
            panStarted(event);
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> panDragHandler = event -> {
        if (panOngoing()) {
            panDragged(event);
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> panEndHandler = event -> {
        if (panOngoing()) {
            panEnded();
            event.consume();
        }
    };

    /**
     * Creates a new instance of Panner class with {@link AxisMode#XY XY} {@link #axisModeProperty() axisMode}.
     */
    public Panner() {
        this(AxisMode.XY);
    }

    /**
     * Creates a new instance of Panner class.
     *
     * @param panMode initial value for the {@link #axisModeProperty() axisMode} property
     */
    public Panner(final AxisMode panMode) {
        setAxisMode(panMode);
        setDragCursor(Cursor.CLOSED_HAND);
        registerMouseHandlers();
    }

    /**
     * The mode defining axis along which the pan operation is allowed. By default initialized to {@link AxisMode#XY}.
     *
     * @return the axis mode property
     */
    public final ObjectProperty<AxisMode> axisModeProperty() {
        return axisMode;
    }

    /**
     * Mouse cursor to be used during drag operation.
     *
     * @return the mouse cursor property
     */
    public final ObjectProperty<Cursor> dragCursorProperty() {
        return dragCursor;
    }

    /**
     * Returns the value of the {@link #axisModeProperty()}.
     *
     * @return current mode
     */
    public final AxisMode getAxisMode() {
        return axisModeProperty().get();
    }

    /**
     * Returns the value of the {@link #dragCursorProperty()}
     *
     * @return the current cursor
     */
    public final Cursor getDragCursor() {
        return dragCursorProperty().get();
    }

    /**
     * Returns MouseEvent filter triggering pan operation.
     *
     * @return filter used to test whether given MouseEvent should start panning operation
     * @see #setMouseFilter(Predicate)
     */
    public Predicate<MouseEvent> getMouseFilter() {
        return mouseFilter;
    }

    private void installCursor() {
        originalCursor = getChart().getCursor();
        if (getDragCursor() != null) {
            getChart().setCursor(getDragCursor());
        }
    }

    private void panChart(final Chart chart, final Point2D mouseLocation) {
        if (!(chart instanceof XYChart)) {
            return;
        }
        for (final Axis axis : chart.getAxes()) {
            if (axis.getSide() == null) {
                continue;
            }
            final Side side = axis.getSide();

            final double prevData = axis.getValueForDisplay(
                    side.isHorizontal() ? previousMouseLocation.getX() : previousMouseLocation.getY());
            final double newData = axis
                    .getValueForDisplay(side.isHorizontal() ? mouseLocation.getX() : mouseLocation.getY());
            final double offset = prevData - newData;

            final boolean allowsShift = side.isHorizontal() ? getAxisMode().allowsX() : getAxisMode().allowsY();
            if (!Axes.hasBoundedRange(axis) && allowsShift) {
                axis.setAutoRanging(false);
                shiftBounds(axis, offset);
            }
        }
    }

    private void panDragged(final MouseEvent event) {
        final Point2D mouseLocation = getLocationInPlotArea(event);
        panChart(getChart(), mouseLocation);
        previousMouseLocation = mouseLocation;
    }

    private void panEnded() {
        previousMouseLocation = null;
        uninstallCursor();
    }

    private boolean panOngoing() {
        return previousMouseLocation != null;
    }

    private void panStarted(final MouseEvent event) {
        previousMouseLocation = getLocationInPlotArea(event);
        installCursor();
    }

    private void registerMouseHandlers() {
        registerInputEventHandler(MouseEvent.MOUSE_PRESSED, panStartHandler);
        registerInputEventHandler(MouseEvent.MOUSE_DRAGGED, panDragHandler);
        registerInputEventHandler(MouseEvent.MOUSE_RELEASED, panEndHandler);
    }

    /**
     * Sets the value of the {@link #axisModeProperty()}.
     *
     * @param mode the mode to be used
     */
    public final void setAxisMode(final AxisMode mode) {
        axisModeProperty().set(mode);
    }

    /**
     * Sets value of the {@link #dragCursorProperty()}.
     *
     * @param cursor the cursor to be used by the plugin
     */
    public final void setDragCursor(final Cursor cursor) {
        dragCursorProperty().set(cursor);
    }

    /**
     * Sets the filter determining whether given MouseEvent triggered on {@link MouseEvent#DRAG_DETECTED event type}
     * should start the panning operation.
     * <p>
     * By default it is initialized to {@link #DEFAULT_MOUSE_FILTER}.
     *
     * @param mouseFilter the mouse filter to be used. Can be set to {@code null} to start panning on any
     *        {@link MouseEvent#DRAG_DETECTED DRAG_DETECTED} event.
     */
    public void setMouseFilter(final Predicate<MouseEvent> mouseFilter) {
        this.mouseFilter = mouseFilter;
    }

    /**
     * Depending if the offset is positive or negative, change first upper or lower bound to not provoke lowerBound
     * &gt;= upperBound when offset &gt;= upperBound - lowerBound.
     * 
     * @param axis reference axis
     * @param offset panning distance
     */
    private void shiftBounds(final Axis axis, final double offset) {
        if (offset < 0) {
            axis.setMin(axis.getMin() + offset);
            axis.setMax(axis.getMax() + offset);
        } else {
            axis.setMax(axis.getMax() + offset);
            axis.setMin(axis.getMin() + offset);
        }
    }

    private void uninstallCursor() {
        getChart().setCursor(originalCursor);
    }
}
