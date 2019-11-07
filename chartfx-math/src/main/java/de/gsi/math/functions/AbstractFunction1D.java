package de.gsi.math.functions;

public abstract class AbstractFunction1D extends AbstractFunction implements Function1D {
    private boolean computeErrorEstimate = false;

    /**
     * @param name function name
     * @param parameter parameter array
     */
    public AbstractFunction1D(final String name, final double[] parameter) {
        this(name, parameter, new String[parameter.length]);
    }

    /**
     * @param name function name
     * @param parameters parameter array
     * @param parameterNames paramter names
     */
    public AbstractFunction1D(final String name, final double[] parameters, final String[] parameterNames) {
        super(name, parameters, parameterNames);
    }

    /**
     * @param name function name
     * @param nparm number of free parameter
     */
    public AbstractFunction1D(final String name, final int nparm) {
        this(name, new double[nparm], new String[nparm]);
    }

    @Override
    public int getInputDimension() {
        return 1;
    }

    @Override
    public int getOutputDimension() {
        return 1;
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
     * @param state true: compute error estimate
     */
    public void setErrorEstimateComputation(final boolean state) {
        computeErrorEstimate = state;
    }
}
