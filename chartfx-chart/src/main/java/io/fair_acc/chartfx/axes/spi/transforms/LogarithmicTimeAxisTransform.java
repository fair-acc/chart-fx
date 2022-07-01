package io.fair_acc.chartfx.axes.spi.transforms;

import io.fair_acc.chartfx.axes.Axis;

/**
 * @author rstein
 */
public class LogarithmicTimeAxisTransform extends LogarithmicAxisTransform {

    public LogarithmicTimeAxisTransform(final Axis axis) {
        super(axis);
    }

    @Override
    public double backward(final double val) {
        // return pow(val) + rangeMax;
        return pow(val);
    }

    @Override
    public double forward(final double val) {
        // return log(rangeMax - val);
        return log(val);
    }
}
