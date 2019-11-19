package de.gsi.math.functions;

import de.gsi.math.TMath;

public class PoissonFunction extends AbstractFunction1D implements Function1D {

    /**
     * initialise Poisson function parameter order: parameter[0] = par (default: 1.0)
     * 
     * @param name function name
     */
    public PoissonFunction(final String name) {
        this(name, null);
    }

    /**
     * initialise Poisson function parameter order: parameter[0] = par (default: 1.0)
     * 
     * @param name function name
     * @param parameter function parameter
     */
    public PoissonFunction(final String name, final double[] parameter) {
        super(name, new double[1]);
        setParameterName(0, "par");
        setParameterValue(0, 1);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 1); i++) {
            setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(final double x) {
        return TMath.PoissonI(x, fparameter[0]);
    }
}
