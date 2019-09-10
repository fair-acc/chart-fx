package de.gsi.dataset.testdata.spi;

import de.gsi.dataset.DataSetError;

/**
 * abstract error data set for graphical testing purposes this implementation
 * generates a sinc function
 *
 * @author rstein
 */
public class SincFunction extends AbstractTestFunction<SincFunction> implements DataSetError {
    private static final long serialVersionUID = 8401471155758562066L;

    /**
     * 
     * @param name
     *            data set name
     * @param count
     *            number of samples
     */
    public SincFunction(final String name, final int count) {
        super(name, count);
    }

    @Override
    public double[] generateY(final int count) {
        final double[] retVal = new double[count];
        for (int i = 0; i < count; i++) {
            final double x = i / (0.05 * count);

            if (x == 0) {
                retVal[i] = 1.0;
            } else {
                retVal[i] = Math.sin(x) / x;
            }

        }
        return retVal;
    }

}
