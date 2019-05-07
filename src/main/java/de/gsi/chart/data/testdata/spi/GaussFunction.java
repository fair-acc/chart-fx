package de.gsi.chart.data.testdata.spi;

import de.gsi.chart.data.DataSetError;

/**
 * abstract error data set for graphical testing purposes
 * this implementation generates a Gaussian function
 *
 * @author rstein
 */
public class GaussFunction extends AbstractTestFunction<GaussFunction> implements DataSetError {

    public GaussFunction(final String name, final int count) {
        super(name, count);
    }

    public static double gauss(double x) {
        return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }

    public static double gauss(double x, double mu, double sigma) {
        return GaussFunction.gauss((x - mu) / sigma) / sigma;
    }

    @Override
    public double[] generateY(final int count) {
    	final double[] retVal = new double[count];
    	final double centre = 0.5*count;
    	final double sigma = count/10.0;
        for (int i = 0; i < count; i++) {
        	final double x = i;
            retVal[i] = GaussFunction.gauss(x, centre, sigma)*sigma;
        }
        return retVal;
    }

}
