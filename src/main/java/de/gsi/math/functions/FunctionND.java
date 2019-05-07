package de.gsi.math.functions;

/**
 * generic n-dimensional function interface
 * 
 * @author rstein
 * @version
 */
public interface FunctionND extends Function {

    /**
     * @param param parameter list
     * @param x input parameter array
     * @param i output index
     * @return value of function
     */
    public double getValue(double x[], int i);

    /**
     * @param param parameter list
     * @param x input parameter array
     * @param i output index
     * @return array of function values
     */
    public double[] getValue(double x[]);

}
