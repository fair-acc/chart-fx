package io.fair_acc.chartfx.axes;

/**
 * Small interface for defining axis transforms (e.g. logarithmic-base 10, 2, other transforms etc.
 *
 * @author rstein
 */
public interface AxisTransform {

    /**
     * default backward transform for affine functions: backward(forward(val)) = forward(backward(val)) = val;
     *
     * @param val input value
     * @return output value
     */
    double backward(double val);

    /**
     * default forward transform for affine functions: backward(forward(val)) = forward(backward(val)) = val;
     *
     * @param val input value
     * @return output value
     */
    double forward(double val);

    double getMaximumRange();

    double getMinimumRange();

    double getRoundedMaximumRange(double val);

    double getRoundedMinimumRange(double val);

    void setMaximumRange(double val);

    void setMinimumRange(double val);
}
