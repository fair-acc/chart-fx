package de.gsi.math.functions;

import de.gsi.math.TMath;

/**
 * class implementing the lognormal function see http://en.wikipedia.org/wiki/Log-normal_distribution Howe
 *
 * @author rstein
 */
public class LogNormalFunction extends AbstractFunction1D implements Function1D {

    /**
     * LogNormal function default parameter: parameter[0] = theta = 0.0 parameter[1] = sigma = 1.0 parameter[2] = scale
     * = 1.0
     *
     * @param name function name
     */
    public LogNormalFunction(final String name) {
        this(name, null);
    }

    /**
     * LogNormal function parameter order: parameter[0] = theta (default: 0.0) parameter[1] = sigma (default: 1.0)
     * parameter[2] = scale (default: 1.0)
     *
     * @param name function name
     * @param parameter function parameter
     */
    public LogNormalFunction(final String name, final double[] parameter) {
        super(name, new double[3]);
        setParameterName(0, "theta");
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
        return TMath.LogNormal(x, fparameter[0], fparameter[1], fparameter[2]);
    }

}
