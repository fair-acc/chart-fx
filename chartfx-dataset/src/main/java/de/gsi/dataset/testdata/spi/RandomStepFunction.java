package de.gsi.dataset.testdata.spi;

import java.util.SplittableRandom;

/**
 * abstract error data set for graphical testing purposes this implementation generates a random step function
 *
 * @author rstein
 */
public class RandomStepFunction extends AbstractTestFunction<RandomStepFunction> {
    private static final long serialVersionUID = -5338037806123143152L;
    protected static SplittableRandom rnd = new SplittableRandom(System.currentTimeMillis());

    /**
     * 
     * @param name data set name
     * @param count number of samples
     */
    public RandomStepFunction(final String name, final int count) {
        super(name, count);
    }

    @Override
    public double[] generateY(final int count) {
        final double[] retVal = new double[count];
        final long step = RandomStepFunction.rnd.nextInt(count);
        for (int i = 0; i < count; i++) {
            retVal[i] = i < step ? 0.0 : 1.0;
        }
        return retVal;
    }

}
