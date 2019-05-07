package de.gsi.chart.axes.spi.transforms;

import de.gsi.chart.axes.Axis;

/**
 * Identity axis transform
 *
 * @author rstein
 */
public class DefaultAxisTransform extends AbstractAxisTransform {

    public DefaultAxisTransform(final Axis axis) {
        super(axis);
    }

    @Override
    public double forward(final double val) {
        return val;
    }

    @Override
    public double backward(final double val) {
        return val;
    }

    @Override
    public double getRoundedMinimumRange(final double min) {
        return Math.ceil(min);
    }

    @Override
    public double getRoundedMaximumRange(final double max) {
        return Math.floor(max);
    }

}
