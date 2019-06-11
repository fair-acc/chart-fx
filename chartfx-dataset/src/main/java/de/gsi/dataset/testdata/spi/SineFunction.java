package de.gsi.dataset.testdata.spi;

import de.gsi.dataset.DataSetError;

/**
 * abstract error data set for graphical testing purposes
 * this implementation generates a sine function
 *
 * @author rstein
 */
public class SineFunction  extends AbstractTestFunction<SineFunction> implements DataSetError {
	private final boolean useSystemTimeOffset;

	/**
     * 
     * @param name data set name
     * @param count number of samples
     */
    public SineFunction(final String name, final int count) {
        super(name, count);
        useSystemTimeOffset = false;
    }

    /**
     * 
     * @param name data set name
     * @param count number of samples
     * @param useSystemTime true: use system time
     */
    public SineFunction(final String name, final int count, boolean useSystemTime) {
        super(name, count);
        useSystemTimeOffset = useSystemTime;
        update();
    }


    @Override
    public double[] generateY(final int count) {
    	final double[] retVal = new double[count];
    	final double period = count/10.0;
    	final double offset = useSystemTimeOffset ? System.currentTimeMillis()/1000.0 : 0.0;
        for (int i = 0; i < count; i++) {
        	final double t = i/period;
            retVal[i] = Math.sin(2.0 * Math.PI * (t + + 0.1*offset));
        }
        return retVal;
    }

}
