package de.gsi.chart.axes.spi;

import java.security.InvalidParameterException;
import java.util.Objects;

import de.gsi.chart.data.spi.DataRange;
import de.gsi.chart.ui.geometry.Side;
import javafx.scene.chart.ValueAxis;

/**
 * Holds the range of the axis along with {@link ValueAxis#getScale() scale} and tick numbers format to be used.
 */
public class AxisRange extends DataRange {

    protected double axisLength;
    protected double scale = 1;
    protected double tickUnit;

    public AxisRange() {
        this(0.0, 1.0, 1.0, 1.0, 0.1);
    }

    public AxisRange(final double lowerBound, final double upperBound, final double axisLength, final double scale,
            final double tickUnit) {
        super(lowerBound, upperBound);
        if (scale == 0) {
            throw new InvalidParameterException("scale should not be '0'");
        }
        if (tickUnit <= 0) {
            throw new InvalidParameterException("tickUnit should not be <='0'");
        }
        this.axisLength = axisLength;
        this.scale = scale;
        this.tickUnit = tickUnit;
    }

    @Override
    public String toString() {
        return String.format("AxisRange [min=%f, max=%f, axisLength=%f, scale=%f, tickUnit=%f]", min, max, axisLength,
                scale, tickUnit);
    }

    /**
     * @return the lower bound of the axis
     */
    public double getLowerBound() {
        return min;
    }

    /**
     * @return the upper bound of the axis
     */
    public double getUpperBound() {
        return max;
    }

    /**
     * @return the axis length in display (ie. pixel) units
     */
    public double getAxisLength() {
        return axisLength;
    }

    /**
     * @return the calculated {@link ValueAxis#getScale() scale}
     */
    public double getScale() {
        return scale;
    }

    public double getTickUnit() {
        return tickUnit;
    }

    /**
     * Calculate a new scale for this axis. This should not effect any state(properties) of this axis.
     *
     * @param length The display length of the axis
     * @param side axis side
     */
    public void setAxisLength(final double length, final Side side) {
        if (length == 0) {
            throw new InvalidParameterException("length should not be '0'");
        }
        axisLength = length;
        double newScale = 1;
        final double diff = max - min;

        if (side.isVertical()) {
            newScale = diff == 0 ? -length : -(length / diff);
        } else { // HORIZONTAL
            newScale = diff == 0 ? length : length / diff;
        }
        this.scale = newScale;
        this.tickUnit = diff / 10;
    }

    /**
     * Add the specified data range to this range.
     *
     * @param range
     */
    public void add(final AxisRange range) {
        add(range.min);
        add(range.max);
    }

    /**
     * Substracts the specified data range from this range. TODO: check algorithm definiton
     * 
     * @param range
     * @return this axis with reduced limits
     */
    public AxisRange substract(final AxisRange range) {
        if (range.min > max || range.max < min) {
            return this;
        }
        this.min = Math.max(min, range.min);
        this.max = Math.min(max, range.max);

        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(min, max, axisLength, scale, tickUnit);

        // if performance is an issue, use
        // return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof AxisRange)) {
            return false;
        }

        return ((AxisRange) obj).hashCode() == this.hashCode();
    }

    public AxisRange copy() {
        return new AxisRange(this.getLowerBound(), this.getUpperBound(), this.getAxisLength(), this.getScale(),
                this.getTickUnit());
    }

}
