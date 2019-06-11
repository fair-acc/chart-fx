package de.gsi.math.functions;

import java.util.Random;

public class RandomFunction extends AbstractFunction1D implements Function1D {
    private double amplitude = 0.5;
    private static final Random frandom = new Random(System.currentTimeMillis());

    public RandomFunction(final String name, final double amplitude) {
        super(name, 0);
        this.amplitude = amplitude;
    }

    @Override
    public double getValue(final double x) {
        return amplitude * frandom.nextGaussian();
    }
}
