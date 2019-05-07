/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import de.gsi.chart.axes.Axis;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

/**
 * Plugin indicating a specific X or Y value as a line drawn on the plot area,
 * with an optional {@link #textProperty() text label} describing the value.
 *
 * @author mhrabia
 */
public abstract class AbstractSingleValueIndicator extends AbstractValueIndicator {
    /**
     * The default distance between the data point coordinates and mouse cursor
     * that triggers shifting the line.
     */
    public static final int DEFAULT_PICKING_DISTANCE = 30;
    static final double MIDDLE_POSITION = 0.5;
    static final String STYLE_CLASS_LABEL = "value-indicator-label";
    static final String STYLE_CLASS_LINE = "value-indicator-line";
    static final String STYLE_CLASS_MARKER = "value-indicator-marker";
    protected static double TRIANGLE_HALF_WIDTH = 5.0;

    /**
     * Line indicating the value.
     */
    protected final Line line = new Line();
    protected final Line pickLine = new Line();

    /**
     * small triangle marker as handler to shift the line marker
     */
    protected final Polygon triangle = new Polygon();

    /**
     * Creates a new instance of AbstractSingleValueIndicator.
     *
     * @param value
     *            a X value to be indicated
     * @param text
     *            the text to be shown by the label. Value of
     *            {@link #textProperty()}.
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
    }

    private final DoubleProperty pickingDistance = new SimpleDoubleProperty(this, "pickingDistance",
            DEFAULT_PICKING_DISTANCE) {
        @Override
        protected void invalidated() {
            if (get() <= 0) {
                throw new IllegalArgumentException("The " + getName() + " must be a positive value");
            }
        }
    };

    /**
     * Distance of the mouse cursor from the line (in pixel) that should trigger
     * the moving of the line. By default initialized to
     * {@value #DEFAULT_PICKING_DISTANCE}.
     *
     * @return the picking distance property
     */
    public final DoubleProperty pickingDistanceProperty() {
        return pickingDistance;
    }

    /**
     * Sets the value of {@link #pickingDistanceProperty()}.
     *
     * @param distance
     *            the new picking distance
     */
    public final void setPickingDistance(final double distance) {
        pickingDistanceProperty().set(distance);
    }

    /**
     * Returns the value of the {@link #pickingDistanceProperty()}.
     *
     * @return the current picking distance
     */
    public final double getPickingDistance() {
        return pickingDistanceProperty().get();
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
             * Record a delta distance for the drag and drop operation. Because
             * layoutLine() sets the start/end points we have to use these here.
             * It is enough to use the start point. For X indicators, start x
             * and end x are identical and for Y indicators start y and end y
             * are identical.
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
        final double a = AbstractSingleValueIndicator.TRIANGLE_HALF_WIDTH;
        triangle.getPoints().setAll(-a, -a, -a, +a, +a, +a, +a, -a);
        triangle.setOnMousePressed(mouseEvent -> {
            /*
             * Record a delta distance for the drag and drop operation. Because
             * the whole node is translated in layoutMarker we use the layout
             * position here.
             */
            dragDelta.x = triangle.getLayoutX() - mouseEvent.getX();
            dragDelta.y = triangle.getLayoutY() - mouseEvent.getY();
            triangle.setCursor(Cursor.MOVE);
            mouseEvent.consume();
        });
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

    private final DoubleProperty value = new SimpleDoubleProperty(this, "value") {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    /**
     * Value indicated by this plugin.
     *
     * @return value property
     */
    public final DoubleProperty valueProperty() {
        return value;
    }

    /**
     * Returns the indicated value.
     *
     * @return indicated value
     */
    public final double getValue() {
        return valueProperty().get();
    }

    /**
     * Sets the value that should be indicated.
     *
     * @param newValue
     *            value to be indicated
     */
    public final void setValue(final double newValue) {
        valueProperty().set(newValue);
    }

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
     * Relative position, between 0.0 (left, bottom) and 1.0 (right, top) of the
     * description {@link #textProperty() label} in the plot area.
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
     * Returns the value of the {@link #labelPositionProperty()}.
     *
     * @return the relative position of the {@link #textProperty() text label}
     */
    public final double getLabelPosition() {
        return labelPositionProperty().get();
    }

    /**
     * Sets the new value of the {@link #labelPositionProperty()}.
     *
     * @param value
     *            the label position, between 0.0 and 1.0 (both inclusive)
     */
    public final void setLabelPosition(final double value) {
        labelPositionProperty().set(value);
    }

    /**
     * Sets the line coordinates.
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
        pickLine.toBack();
    }

    /**
     * Sets the marker coordinates.
     */
    protected void layoutMarker(final double startX, final double startY, final double endX, final double endY) {
        if (!triangle.isVisible()) {
            return;
        }

        triangle.setTranslateX(startX);
        triangle.setTranslateY(startY);
        addChildNodeIfNotPresent(triangle);
    }

}
