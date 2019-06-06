package de.gsi.dataset.testdata.spi;

import de.gsi.dataset.DataSetError;

/**
 * abstract error data set for graphical testing purposes
 * this implementation generates a random walk (Brownian noise) function.
 *
 * @author rstein
 */
public class RandomWalkFunction extends AbstractTestFunction<RandomWalkFunction> implements DataSetError {

    /**
     * 
     * @param name data set name
     * @param count number of samples
     */
    public RandomWalkFunction(final String name, final int count) {
        super(name, count);
    }

    @Override
    public double[] generateY(final int count) {
        return RandomDataGenerator.generateDoubleArray(0, 0.01, count);
    }
}