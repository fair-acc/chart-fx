package de.gsi.math.functions;

import java.util.Random;

public class RandomWalkFunction extends AbstractFunction1D implements Function1D {
    private double fstate = 0.0;
    private double fstep = 0.5;
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public RandomWalkFunction(final String name, final double step) {
        super(name, 0);
        fstep = step;
    }

    @Override
    public double getValue(final double x) {
        fstate += fstep * RANDOM.nextGaussian();
        return fstate;
    }

}
