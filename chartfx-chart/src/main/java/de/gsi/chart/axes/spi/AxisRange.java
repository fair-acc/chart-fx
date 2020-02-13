package de.gsi.chart.axes.spi;

import java.security.InvalidParameterException;
import java.util.Objects;

import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.DataRange;
import javafx.scene.chart.ValueAxis;

/**
 * Holds the range of the axis along with {@link ValueAxis#getScale() scale} and tick numbers format to be used.
 *
 * @author rstein
 */
public class AxisRange extends DataRange {

    protected double axisLength;
    protected double scale = 1;
    protected double tickUnit;

    public AxisRange() {
        this(Double.NaN, Double.NaN, 1.0, 1.0, 0.1);
    }

    public AxisRange(final AxisRange other) {
        this(other.getMin(), other.getMax(), other.getAxisLength(), other.getScale(), other.getTickUnit());
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

    /**
     * Add the specified data range to this range.
     *
     * @param range range to be added
     */
    public boolean add(final AxisRange range) {
        boolean retVal = false;
        if (add(range.min)) {
            retVal = true;
        }
        if (add(range.max)) {
            retVal = true;
        }
        return retVal;
    }

    public AxisRange copy() {
        return new AxisRange(this.getLowerBound(), this.getUpperBound(), this.getAxisLength(), this.getScale(),
                this.getTickUnit());
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AxisRange)) {
            return false;
        }

        return ((AxisRange) obj).hashCode() == this.hashCode();
    }

    /**
     * @return the axis length in display (ie. pixel) units
     */
    public double getAxisLength() {
        return axisLength;
    }

    /**
     * @return the lower bound of the axis
     */
    public double getLowerBound() {
        return min;
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
     * @return the upper bound of the axis
     */
    public double getUpperBound() {
        return max;
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

    @Override
    public String toString() {
        return String.format("AxisRange [min=%f, max=%f, axisLength=%f, scale=%f, tickUnit=%f]", min, max, axisLength,
                scale, tickUnit);
    }

}
