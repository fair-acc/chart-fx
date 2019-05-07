package de.gsi.chart.data.testdata.spi;

import de.gsi.chart.data.DataSetError;

/**
 * abstract error data set for graphical testing purposes
 * this implementation generates a random walk (Brownian noise) function.
 *
 * @author rstein
 */
public class RandomWalkFunction extends AbstractTestFunction<RandomWalkFunction> implements DataSetError {

    public RandomWalkFunction(final String name, final int count) {
        super(name, count);
    }

    @Override
    public double[] generateY(final int count) {
        return RandomDataGenerator.generateDoubleArray(0, 0.01, count);
    }
}