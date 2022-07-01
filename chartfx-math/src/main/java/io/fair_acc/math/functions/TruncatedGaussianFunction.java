package io.fair_acc.math.functions;

import io.fair_acc.math.Math;

/**
 * class implementing the Gaussian function (/normal distribution)
 *
 * @author rstein
 */
public class TruncatedGaussianFunction extends AbstractFunction1D implements Function1D {
    // @formatter:off
    /**
     * initialise Gaussian function (/normal distribution) y = scale/(sigma*sqrt(2.pi)) * exp( -0.5 [(x - mean)/sigma]^2
     * ) parameter order: parameter[0] = mean (default: 0.0) parameter[1] = sigma (default: 1.0) parameter[2] = scale
     * (default: 1.0) parameter[3] = truncation (default: 3.0 sigma)
     *
     * @param name function name
     */
    // @formatter:on
    public TruncatedGaussianFunction(final String name) {
        this(name, null);
    }

    // @formatter:off
    /**
     * initialise Gaussian function (/normal distribution) y = scale/(sigma*sqrt(2.pi)) * exp( -0.5 [(x - mean)/sigma]^2
     * ) parameter order: parameter[0] = mean (default: 0.0) parameter[1] = sigma (default: 1.0) parameter[2] = scale
     * (default: 1.0) parameter[3] = truncation (default: 2.0 sigma)
     *
     * @param name function name
     * @param parameter function parameter
     */
    // @formatter:on
    public TruncatedGaussianFunction(final String name, final double[] parameter) {
        super(name, new double[4]);
        setParameterName(0, "mean");
        setParameterValue(0, 0);
        setParameterName(1, "sigma");
        setParameterValue(1, 1.0);
        setParameterName(2, "scaling");
        setParameterValue(2, 1.0);
        setParameterName(3, "truncation");
        setParameterValue(3, 2.0);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 4); i++) {
            setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(final double x) {
        if (Math.abs(x - fparameter[0]) < fparameter[3] * fparameter[1]) {
            return fparameter[2] * Math.gauss(x, fparameter[0], fparameter[1], true);
        }
        return 0.0;
    }
}
