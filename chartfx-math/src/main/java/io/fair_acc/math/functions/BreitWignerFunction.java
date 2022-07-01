package io.fair_acc.math.functions;

import io.fair_acc.math.Math;

public class BreitWignerFunction extends AbstractFunction1D implements Function1D {
    /**
     * initialise the Breit-Wigner distribution function parameter order: parameter[0] = mean (default: 0.0)
     * parameter[1] = gamma (default: 1.0)
     *
     * @param name function name
     */
    public BreitWignerFunction(final String name) {
        this(name, null);
    }

    /**
     * initialise the Breit-Wigner distribution function parameter order: parameter[0] = mean (default: 0.0)
     * parameter[1] = gamma (default: 1.0)
     *
     * @param name function name
     * @param parameter parameter of function 0: mean: 1: gamma
     */
    public BreitWignerFunction(final String name, final double[] parameter) {
        super(name, new double[2]);
        setParameterName(0, "mean");
        setParameterValue(0, 0);
        setParameterName(1, "gamma");
        setParameterValue(1, 1.0);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 2); i++) {
            setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(final double x) {
        return Math.breitWigner(x, fparameter[0], fparameter[1]);
    }
}
