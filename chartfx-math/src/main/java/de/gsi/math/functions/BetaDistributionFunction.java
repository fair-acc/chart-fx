package de.gsi.math.functions;

import de.gsi.math.TMath;

public class BetaDistributionFunction extends AbstractFunction1D implements Function1D {

    /**
     * initialise the Beta distribution function parameter order: parameter[0] = p (default: 5.0) parameter[1] = q
     * (default: 1.0)
     *
     * @param name function name
     */
    public BetaDistributionFunction(final String name) {
        this(name, null);
    }

    /**
     * initialise the Beta distribution function parameter order: parameter[0] = p (default: 5.0) parameter[1] = q
     * (default: 1.0)
     *
     * @param name function name
     * @param parameter 0:p 1:q
     */
    public BetaDistributionFunction(final String name, final double[] parameter) {
        super(name, new double[2]);
        setParameterName(0, "p");
        setParameterValue(0, 5.0);
        setParameterName(1, "q");
        setParameterValue(1, 1.0);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 2); i++) {
            setParameterValue(i, parameter[0]);
        }
    }

    @Override
    public double getValue(final double x) {
        return TMath.BetaDist(x, fparameter[0], fparameter[1]);
    }
}
