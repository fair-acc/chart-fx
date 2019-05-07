package de.gsi.chart.axes;

/**
 * Small interface for defining axis transforms (e.g. logarithmic-base 10, 2, other transforms etc.
 *
 * @author rstein
 */
public interface AxisTransform {

    /**
     * default forward transform
     * for affine functions: backward(forward(val)) = forward(backward(val)) = val;
     *
     * @param val input value
     * @return output value
     */
    double forward(double val);

    /**
     * default backward transform
     * for affine functions: backward(forward(val)) = forward(backward(val)) = val;
     *
     * @param val input value
     * @return output value
     */
    double backward(double val);

    void setMinimumRange(double val);

    double getMinimumRange();

    double getRoundedMinimumRange(double val);

    void setMaximumRange(double val);

    double getMaximumRange();

    double getRoundedMaximumRange(double val);
}
