package de.gsi.math.functions;

import java.util.Random;

public class RandomWalkFunction extends AbstractFunction1D implements Function1D {
    private double fstate = 0.0;
    private double fstep = 0.5;
    private static final Random frandom = new Random(System.currentTimeMillis());

    public RandomWalkFunction(final String name, final double step) {
        super(name, 0);
        fstep = step;
    }

    @Override
    public double getValue(final double x) {
        fstate += fstep * frandom.nextGaussian();
        return fstate;
    }

    /**
     * quick check function
     *
     * @param args
     */
    public static void main(final String[] args) {
        final RandomWalkFunction func = new RandomWalkFunction("rand1", 0.1);

        for (int i = 0; i <= 6; i++) {
            final double x = i;
            System.out.printf("%+2d: rand(%+4.2f) = %f\n", i, x, func.getValue(0));
        }

        func.printParameters(true);
    }

}
