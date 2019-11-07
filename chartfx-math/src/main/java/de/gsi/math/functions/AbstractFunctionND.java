package de.gsi.math.functions;

import java.security.InvalidParameterException;

public abstract class AbstractFunctionND extends AbstractFunction implements FunctionND {
    private boolean computeErrorEstimate = false;

    /**
     * @param name function name
     * @param parameter parameter array
     */
    public AbstractFunctionND(final String name, final double[] parameter) {
        this(name, parameter, new String[parameter.length]);
    }

    /**
     * @param name function name
     * @param parameters parameter array
     * @param parameterNames parameter name array
     */
    public AbstractFunctionND(final String name, final double[] parameters, final String[] parameterNames) {
        super(name, parameters, parameterNames);
    }

    /**
     * @param name function name
     * @param nparm number of free parameter
     */
    public AbstractFunctionND(final String name, final int nparm) {
        this(name, new double[nparm], new String[nparm]);
    }

    @Override
    public double[] getValue(final double[] x) {

        if (x == null) {
            throw new InvalidParameterException("getValue(double[], null, int) " + "- input vector is null");
        }

        if (x.length == getInputDimension()) {
            throw new InvalidParameterException("getValue(double[], double[][], int) "
                    + "- input vector dimension mismatch " + x.length + " vs. " + getInputDimension());
        }

        final int dim = getOutputDimension();
        final double[] ret = new double[dim];

        for (int k = 0; k < dim; k++) {
            ret[k] = this.getValue(x, k);
        }

        return ret;
    }

    /**
     * @return true: error estimated is included in exports/estimates etc.
     */
    public boolean isErrorEstimateComputed() {
        return computeErrorEstimate;
    }

    /**
     * sets whether error estimates is included into exports/estimates etc.
     *
     * @param state true: compute error estimates
     */
    public void setErrorEstimateComputation(final boolean state) {
        computeErrorEstimate = true;
    }

}
