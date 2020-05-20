/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import de.gsi.chart.axes.Axis;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventSource;
import de.gsi.dataset.event.UpdateEvent;

/**
 * Plugin indicating a specific X or Y value as a line drawn on the plot area, with an optional {@link #textProperty()
 * text label} describing the value.
 *
 * @author mhrabia
 */
public abstract class AbstractSingleValueIndicator extends AbstractValueIndicator
        implements EventSource, ValueIndicator {
    /**
     * The default distance between the data point coordinates and mouse cursor that triggers shifting the line.
     */
    protected static final int DEFAULT_PICKING_DISTANCE = 30;
    protected static final double MIDDLE_POSITION = 0.5;
    protected static final String STYLE_CLASS_LABEL = "value-indicator-label";
    protected static final String STYLE_CLASS_LINE = "value-indicator-line";
    protected static final String STYLE_CLASS_MARKER = "value-indicator-marker";
    protected static double triangleHalfWidth = 5.0;
    private final transient AtomicBoolean autoNotification = new AtomicBoolean(true);
    private final transient List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());
    private boolean autoRemove = false;

    /**
     * Line indicating the value.
     */
    protected final Line line = new Line();
    protected final Line pickLine = new Line();

    /**
     * small triangle marker as handler to shift the line marker
     */
    protected final Polygon triangle = new Polygon();
    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance",
            DEFAULT_PICKING_DISTANCE) {
        @Override
        protected void invalidated() {
            if (get() <= 0) {
                throw new IllegalArgumentException("The " + getName() + " must be a positive value");
            }
        }
    };

    private final DoubleProperty value = new SimpleDoubleProperty(this, "value") {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    private final DoubleProperty labelPosition = new SimpleDoubleProperty(this, "labelPosition", 0.5) {
        @Override
        protected void invalidated() {
            if (get() < 0 || get() > 1) {
                throw new IllegalArgumentException("labelPosition must be in rage [0,1]");
            }
            layoutChildren();
        }
    };

    /**
     * Creates a new instance of AbstractSingleValueIndicator.
     * 
     * @param axis reference axis
     * @param value a X value to be indicated
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     */
    protected AbstractSingleValueIndicator(Axis axis, final double value, final String text) {
        super(axis, text);
        setValue(value);

        initLine();

        initTriangle();

        editableIndicatorProperty().addListener((ch, o, n) -> updateMouseListener(n));
        updateMouseListener(isEditable());

        // Need to add them so that at initialization of the stage the CCS is
        // applied and we can calculate label's
        // width and height
        getChartChildren().addAll(line, triangle, label);
        this.value.addListener(
                (ch, o, n) -> invokeListener(new UpdateEvent(this, "value changed to " + n + " for axis " + axis)));
    }

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
    }

    /**
     * Returns the value of the {@link #labelPositionProperty()}.
     *
     * @return the relative position of the {@link #textProperty() text label}
     */
    public final double getLabelPosition() {
        return labelPositionProperty().get();
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
     * Returns the indicated value.
     *
     * @return indicated value
     */
    @Override
    public final double getValue() {
        return valueProperty().get();
    }

    private void initLine() {
        // mouse transparent if not editable
        line.setMouseTransparent(true);
        pickLine.setPickOnBounds(true);
        pickLine.setStroke(Color.TRANSPARENT);
        pickLine.setStrokeWidth(getPickingDistance());
        pickLine.mouseTransparentProperty().bind(editableIndicatorProperty().not());
        pickLine.setOnMousePressed(mouseEvent -> {
            /*
             * Record a delta distance for the drag and drop operation. Because layoutLine() sets the start/end points
             * we have to use these here. It is enough to use the start point. For X indicators, start x and end x are
             * identical and for Y indicators start y and end y are identical.
             */
            dragDelta.x = pickLine.getStartX() - mouseEvent.getX();
            dragDelta.y = pickLine.getStartY() - mouseEvent.getY();
            pickLine.setCursor(Cursor.MOVE);
            mouseEvent.consume();
        });
    }

    private void initTriangle() {
        triangle.visibleProperty().bind(editableIndicatorProperty());
        triangle.mouseTransparentProperty().bind(editableIndicatorProperty().not());
        triangle.setPickOnBounds(true);
        final double a = AbstractSingleValueIndicator.triangleHalfWidth;
        triangle.getPoints().setAll(-a, -a, -a, +a, +a, +a, +a, -a);
        triangle.setOnMousePressed(mouseEvent -> {
            /*
             * Record a delta distance for the drag and drop operation. Because the whole node is translated in
             * layoutMarker we use the layout position here.
             */
            dragDelta.x = triangle.getLayoutX() - mouseEvent.getX();
            dragDelta.y = triangle.getLayoutY() - mouseEvent.getY();
            triangle.setCursor(Cursor.MOVE);
            mouseEvent.consume();
        });
    }

    /**
     * @return {@code true} indicator should be removed if there is no listener attached to it
     */
    public boolean isAutoRemove() {
        return autoRemove;
    }

    /**
     * Relative position, between 0.0 (left, bottom) and 1.0 (right, top) of the description {@link #textProperty()
     * label} in the plot area.
     * <p>
     * <b>Default value: 0.5</b>
     * </p>
     *
     * @return labelPosition property
     */
    public final DoubleProperty labelPositionProperty() {
        return labelPosition;
    }

    /**
     * Sets the line coordinates.
     * 
     * @param startX start x coordinate
     * @param startY start y coordinate
     * @param endX stop x coordinate
     * @param endY stop y coordinate
     */
    protected void layoutLine(final double startX, final double startY, final double endX, final double endY) {
        line.setStartX(startX);
        line.setStartY(startY);
        line.setEndX(endX);
        line.setEndY(endY);
        pickLine.setStartX(startX);
        pickLine.setStartY(startY);
        pickLine.setEndX(endX);
        pickLine.setEndY(endY);

        addChildNodeIfNotPresent(line);
        addChildNodeIfNotPresent(pickLine);
        // pickLine.toBack();
    }

    /**
     * Sets the marker coordinates.
     * 
     * @param startX start x coordinate
     * @param startY start y coordinate
     * @param endX stop x coordinate
     * @param endY stop y coordinate
     */
    protected void layoutMarker(final double startX, final double startY, final double endX, final double endY) {
        if (!triangle.isVisible()) {
            return;
        }

        triangle.setTranslateX(startX);
        triangle.setTranslateY(startY);
        addChildNodeIfNotPresent(triangle);
    }

    /**
     * Distance of the mouse cursor from the line (in pixel) that should trigger the moving of the line. By default
     * initialized to {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
    }

    /**
     * @param autoRemove {@code true} indicator should be removed if there is no listener attached to it
     */
    public void setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }

    /**
     * Sets the new value of the {@link #labelPositionProperty()}.
     *
     * @param value the label position, between 0.0 and 1.0 (both inclusive)
     */
    public final void setLabelPosition(final double value) {
        labelPositionProperty().set(value);
    }

    /**
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance the new picking distance
     */
    public final void setPickingDistance(final double distance) {
        pickingDistanceProperty().set(distance);
    }

    /**
     * Sets the value that should be indicated.
     *
     * @param newValue value to be indicated
     */
    @Override
    public final void setValue(final double newValue) {
        valueProperty().set(newValue);
    }

    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    private void updateMouseListener(final boolean state) {
        if (state) {
            pickLine.setOnMouseReleased(mouseEvent -> pickLine.setCursor(Cursor.HAND));
            pickLine.setOnMouseEntered(mouseEvent -> pickLine.setCursor(Cursor.HAND));
            triangle.setOnMouseReleased(mouseEvent -> triangle.setCursor(Cursor.HAND));
            triangle.setOnMouseEntered(mouseEvent -> triangle.setCursor(Cursor.HAND));
            label.setOnMouseReleased(mouseEvent -> label.setCursor(Cursor.HAND));
            label.setOnMouseEntered(mouseEvent -> label.setCursor(Cursor.HAND));
        } else {
            pickLine.setOnMouseReleased(null);
            pickLine.setOnMouseEntered(null);
            triangle.setOnMouseReleased(null);
            triangle.setOnMouseEntered(null);
            label.setOnMouseReleased(null);
            label.setOnMouseEntered(null);
        }
    }

    /**
     * Value indicated by this plugin.
     *
     * @return value property
     */
    @Override
    public final DoubleProperty valueProperty() {
        return value;
    }
}
