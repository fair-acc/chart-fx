package de.gsi.math.functions;

import de.gsi.math.TMath;

/**
 * class implementing the trigonometric Sine function
 *
 * @author rstein
 */
public class TrigSineFunction extends AbstractFunction1D implements Function1D {
    public TrigSineFunction(final String name) {
        this(name, null);
    }

    // @formatter:off
    /**
     * initialise Sine function y = [0]*sin(2*pi*[[1]*t+[2]])
     * 
     * parameter order: parameter[0] = amplitude (default: 1.0) parameter[1] = frequency (default: 1.0) parameter[2] =
     * phase (default: 1.0)
     *
     * @param name function name
     * @param parameter function parameter
     */
    // @formatter:on
    public TrigSineFunction(final String name, final double[] parameter) {
        super(name, new double[3]);
        setParameterName(0, "amplitude");
        setParameterValue(0, 1.0);
        setParameterName(1, "frequency");
        setParameterValue(1, 1.0);
        setParameterName(2, "phase");
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
        return fparameter[0] * TMath.Sin(TMath.TwoPi() * (fparameter[1] * x + fparameter[2]));
    }

}
