package de.gsi.math.functions;

import de.gsi.math.TMathConstants;

public class SigmoidFunction extends AbstractFunction1D implements Function1D {

    /**
     * initialise the sigmoid function y = 1.0/(1.0*exp(-slope*(x-location)))) parameter order: parameter[0] = location
     * (default: 0.0) parameter[1] = slope (default: 1.0) parameter[2] = scaling (default: 1.0 (fixed))
     *
     * @param name function name
     */
    public SigmoidFunction(final String name) {
        this(name, null);
    }

    /**
     * initialise the sigmoid function y = 1.0/(1.0*exp(-slope*(x-location)))) parameter order: parameter[0] = location
     * (default: 0.0) parameter[1] = slope (default: 1.0) parameter[2] = scaling (default: 1.0 (fixed))
     *
     * @param name function name
     * @param parameter function parameter
     */
    public SigmoidFunction(final String name, final double[] parameter) {
        super(name, new double[3]);
        setParameterName(0, "location");
        setParameterValue(0, 0.0);
        setParameterName(1, "slope");
        setParameterValue(1, 1.0);
        setParameterName(2, "scaling");
        setParameterValue(2, 1.0);
        fixParameter(2, true);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 3); i++) {
            setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(final double x) {
        return fparameter[2] / (1.0 + TMathConstants.Exp(-fparameter[1] * (x - fparameter[0])));
    }
}
