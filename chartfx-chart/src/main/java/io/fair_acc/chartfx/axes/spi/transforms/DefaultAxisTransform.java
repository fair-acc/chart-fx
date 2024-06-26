package io.fair_acc.chartfx.axes.spi.transforms;

import io.fair_acc.chartfx.axes.Axis;

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
    public double backward(final double val) {
        return val;
    }

    @Override
    public double forward(final double val) {
        return val;
    }

    @Override
    public double getRoundedMaximumRange(final double max) {
        return Math.ceil(max);
    }

    @Override
    public double getRoundedMinimumRange(final double min) {
        return Math.floor(min);
    }
}
