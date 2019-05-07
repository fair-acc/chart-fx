package de.gsi.chart.data.testdata.spi;

import de.gsi.chart.data.DataSetError;

/**
 * abstract error data set for graphical testing purposes
 * this implementation generates a sine function
 *
 * @author rstein
 */
public class SineFunction  extends AbstractTestFunction<SineFunction> implements DataSetError {
	private final boolean useSystemTimeOffset;

    public SineFunction(final String name, final int count) {
        super(name, count);
        useSystemTimeOffset = false;
    }

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
