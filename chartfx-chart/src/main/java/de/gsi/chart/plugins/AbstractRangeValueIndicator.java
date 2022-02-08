package de.gsi.chart.plugins;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.shape.Rectangle;

import de.gsi.chart.axes.Axis;

/**
 * Plugin indicating a value range as a rectangle drawn on the plot area, with an optional {@link #textProperty() text
 * label} describing the range.
 *
 * @author mhrabia
 */
public abstract class AbstractRangeValueIndicator extends AbstractValueIndicator {
    protected static final String STYLE_CLASS_LABEL = "range-indicator-label";
    protected static final String STYLE_CLASS_RECT = "range-indicator-rect";

    /**
     * Rectangle indicating the range.
     */
    protected final Rectangle rectangle = new Rectangle(0, 0, 0, 0);

    private final DoubleProperty lowerBound = new SimpleDoubleProperty(this, "lowerBound") {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    private final DoubleProperty upperBound = new SimpleDoubleProperty(this, "upperBound") {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    private final DoubleProperty labelHorizontalPosition = new SimpleDoubleProperty(this, "labelHorizontalPosition",
            0.5) {
        @Override
        protected void invalidated() {
            if (get() < 0 || get() > 1) {
                throw new IllegalArgumentException("labelHorizontalPosition must be in rage [0,1]");
            }
            layoutChildren();
        }
    };

    private final DoubleProperty labelVerticalPosition = new SimpleDoubleProperty(this, "labelVerticalPosition", 0.5) {
        @Override
        protected void invalidated() {
            if (get() < 0 || get() > 1) {
                throw new IllegalArgumentException("labelVerticalPosition must be in rage [0,1]");
            }
            layoutChildren();
        }
    };

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param lowerBound lower bound (min value) of the range
     * @param upperBound upper bound (max value) of the range
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     */
    protected AbstractRangeValueIndicator(Axis axis, final double lowerBound, final double upperBound,
            final String text) {
        super(axis, text);
        setLowerBound(lowerBound);
        setUpperBound(upperBound);

        rectangle.setMouseTransparent(true);
        rectangle.setManaged(false);
        getChartChildren().addAll(rectangle, label);
    }

    /**
     * Returns the value of the {@link #labelHorizontalPositionProperty()}.
     *
     * @return relative horizontal position of the {@link #textProperty() text label}
     */
    public final double getLabelHorizontalPosition() {
        return labelHorizontalPositionProperty().get();
    }

    /**
     * Returns the value of the {@link #labelVerticalPositionProperty()}.
     *
     * @return relative vertical position of the {@link #textProperty() text label}
     */
    public final double getLabelVerticalPosition() {
        return labelVerticalPositionProperty().get();
    }

    /**
     * Returns the value of the {@link #lowerBoundProperty()}.
     *
     * @return lower bound of the range to be indicated
     */
    public final double getLowerBound() {
        return lowerBoundProperty().get();
    }

    /**
     * Returns the value of the {@link #upperBoundProperty()}.
     *
     * @return upper bound (max value) of the range to be indicated
     */
    public final double getUpperBound() {
        return upperBoundProperty().get();
    }

    /**
     * Relative horizontal position of the {@link #textProperty() text label} on the plot area, with value between 0.0
     * (left) and 1.0 (right). Value 0.5 will position the label in the middle of the plot area.
     *
     * @return labelHorizontalPosition property
     */
    public final DoubleProperty labelHorizontalPositionProperty() {
        return labelHorizontalPosition;
    }

    /**
     * Relative vertical position of the {@link #textProperty() text label} on the plot area, with value between 0.0
     * (bottom) and 1.0 (top). Value 0.5 will position the label in the middle of the plot area.
     *
     * @return labelVerticalPosition property
     */
    public final DoubleProperty labelVerticalPositionProperty() {
        return labelVerticalPosition;
    }

    /**
     * Layouts the rectangle and label within given bounds.
     *
     * @param bounds our own bounds in relation to the plot area bounds
     */
    protected void layout(final Bounds bounds) {
        if (bounds.intersects(getChart().getCanvas().getBoundsInLocal())) {
            layoutLabel(bounds, getLabelHorizontalPosition(), getLabelVerticalPosition());
            rectangle.setX(bounds.getMinX());
            rectangle.setY(bounds.getMinY());
            rectangle.setWidth(bounds.getWidth());
            rectangle.setHeight(bounds.getHeight());
            addChildNodeIfNotPresent(rectangle);

        } else {
            getChartChildren().clear();
        }
    }

    /**
     * Lower bound (min value) of the range to be indicated.
     *
     * @return lowerBound property
     */
    public final DoubleProperty lowerBoundProperty() {
        return lowerBound;
    }

    /**
     * Sets the new value of the {@link #labelHorizontalPositionProperty()}.
     *
     * @param value the new horizontal position, between 0.0 and 1.0 (both inclusive)
     */
    public final void setLabelHorizontalPosition(final double value) {
        labelHorizontalPositionProperty().set(value);
    }

    /**
     * Sets the new value of the {@link #labelVerticalPositionProperty()}.
     *
     * @param value the new vertical position, between 0.0 and 1.0 (both inclusive)
     */
    public final void setLabelVerticalPosition(final double value) {
        labelVerticalPositionProperty().set(value);
    }

    /**
     * Sets the value of the {@link #lowerBoundProperty()}
     *
     * @param value the value for the lower bound of the indicator
     */
    public final void setLowerBound(final double value) {
        lowerBoundProperty().set(value);
    }

    /**
     * Sets the value of {@link #upperBoundProperty()}
     *
     * @param value upper bound (max value) of the range to be indicated
     */
    public final void setUpperBound(final double value) {
        upperBoundProperty().set(value);
    }

    /**
     * Upper bound (max value) of the range to be indicated.
     *
     * @return upperBound property
     */
    public final DoubleProperty upperBoundProperty() {
        return upperBound;
    }
}
