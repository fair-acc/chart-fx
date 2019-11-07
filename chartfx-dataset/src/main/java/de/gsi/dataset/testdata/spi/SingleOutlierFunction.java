package de.gsi.dataset.testdata.spi;

import java.util.SplittableRandom;

/**
 * abstract error data set for graphical testing purposes this implementation generates a function with a single random
 * outlier
 *
 * @author rstein
 */
public class SingleOutlierFunction extends AbstractTestFunction<SingleOutlierFunction> {
    private static final long serialVersionUID = -7456808511068971349L;
    protected static SplittableRandom rnd = new SplittableRandom(System.currentTimeMillis());

    /**
     * 
     * @param name data set name
     * @param count number of samples
     */
    public SingleOutlierFunction(final String name, final int count) {
        super(name, count);
    }

    @Override
    public double[] generateY(final int count) {
        final long step = SingleOutlierFunction.rnd.nextInt(count);
        final double[] retVal = new double[count];
        for (int i = 0; i < count; i++) {
            retVal[i] = i == step ? 1.0 : 0.0;
        }
        return retVal;
    }

}
