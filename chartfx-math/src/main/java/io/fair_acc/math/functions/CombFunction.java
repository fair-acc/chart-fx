package io.fair_acc.math.functions;

import io.fair_acc.math.MathBase;

public class CombFunction extends AbstractFunction1D {
    public CombFunction(final String name, final double[] parameter) {
        super(name, new double[3]);
        // declare parameter names
        setParameterName(0, "fundamental");
        setParameterName(1, "scale");
        setParameterName(2, "width");

        if (parameter == null) {
            return;
        }

        // assign default values
        final int maxIndex = MathBase.min(parameter.length, getParameterCount());
        for (int i = 0; i < maxIndex; i++) {
            setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(final double x) {
        double y = 0.0;
        final double fundamental = fparameter[0];
        if (fundamental == 0) {
            return 0;
        }
        for (int i = 1; i < 1024; i++) {
            // y += TMath.Gauss(x, i*fundamental, fparameter[2], false);
            if (Math.abs(i * fundamental - x) < fparameter[2]) {
                y += 1.0;
            }
        }

        return fparameter[1] * y;
    }
}
