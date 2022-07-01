package io.fair_acc.math.functions;

import java.util.Random;

public class RandomFunction extends AbstractFunction1D implements Function1D {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private double amplitude = 0.5;

    public RandomFunction(final String name, final double amplitude) {
        super(name, 0);
        this.amplitude = amplitude;
    }

    @Override
    public double getValue(final double x) {
        return amplitude * RANDOM.nextGaussian();
    }
}
