package io.fair_acc.math.functions;

/**
 * generic n-dimensional function interface
 *
 * @author rstein
 */
public interface FunctionND extends Function {
    /**
     * @param x input parameter array
     * @return array of function values
     */
    double[] getValue(double[] x);

    /**
     * @param x input parameter array
     * @param i output index
     * @return value of function
     */
    double getValue(double[] x, int i);
}
