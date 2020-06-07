package de.gsi.math.functions;

import de.gsi.math.Math;

/**
 * class implementing the Gaussian function (/normal distribution)
 *
 * @author rstein
 */
public class GaussianFunction extends AbstractFunction1D implements Function1D {
    // @formatter:off
    /**
     * initialise Gaussian function (/normal distribution) y = scale/(sigma*sqrt(2.pi)) * exp( -0.5 [(x - mean)/sigma]^2
     * ) parameter order: parameter[0] = mean (default: 0.0) parameter[1] = sigma (default: 1.0) parameter[2] = scale
     * (default: 1.0)
     *
     * @param name function name
     */
    // @formatter:on
    public GaussianFunction(final String name) {
        this(name, null);
    }

    // @formatter:off
    /**
     * initialise Gaussian function (/normal distribution) y = scale/(sigma*sqrt(2.pi)) * exp( -0.5 [(x - mean)/sigma]^2
     * ) parameter order: parameter[0] = mean (default: 0.0) parameter[1] = sigma (default: 1.0) parameter[2] = scale
     * (default: 1.0)
     *
     * @param name function name
     * @param parameter function parameter
     */
    // @formatter:on
    public GaussianFunction(final String name, final double[] parameter) {
        super(name, new double[3]);
        setParameterName(0, "mean");
        setParameterValue(0, 0);
        setParameterName(1, "sigma");
        setParameterValue(1, 1.0);
        setParameterName(2, "scaling");
        setParameterValue(2, 1.0);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 3); i++) {
            setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(final double x) {
        return fparameter[2] * Math.gauss(x, fparameter[0], fparameter[1], true);
    }
}
