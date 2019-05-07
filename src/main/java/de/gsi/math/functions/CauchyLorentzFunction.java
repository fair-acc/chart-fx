package de.gsi.math.functions;

import de.gsi.math.TMath;

public class CauchyLorentzFunction extends AbstractFunction1D implements Function1D {

    /**
     * initialise the Cauchy-Lorentz distribution function
     * parameter order:
     * parameter[0] = location (default: 0.0)
     * parameter[1] = scale (default: 1.0)
     *
     * @param name function name
     * @param parameter parameter
     */
    public CauchyLorentzFunction(final String name, final double[] parameter) {
        super(name, new double[2]);
        setParameterName(0, "location");
        setParameterValue(0, 0);
        setParameterName(1, "scale");
        setParameterValue(1, 1.0);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 2); i++) {
            setParameterValue(i, parameter[0]);
        }
    }

    /**
     * initialise the Cauchy-Lorentz distribution function
     * parameter order:
     * parameter[0] = location (default: 0.0)
     * parameter[1] = scale (default: 1.0)
     *
     * @param name
     * @param parameter
     */
    public CauchyLorentzFunction(final String name) {
        this(name, null);
    }

    @Override
    public double getValue(final double x) {
        return TMath.CauchyDist(x, fparameter[0], fparameter[1]);
    }
}
