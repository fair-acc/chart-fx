package de.gsi.chart.data.testdata.spi;

import java.util.SplittableRandom;

import de.gsi.chart.data.DataSetError;

/**
 * abstract error data set for graphical testing purposes this implementation
 * generates a random step function
 *
 * @author rstein
 */
public class RandomStepFunction extends AbstractTestFunction<RandomStepFunction> implements DataSetError {
    protected static SplittableRandom rnd = new SplittableRandom(System.currentTimeMillis());

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
