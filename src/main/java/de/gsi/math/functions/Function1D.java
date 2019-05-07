package de.gsi.math.functions;

import java.security.InvalidParameterException;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.spi.DoubleErrorDataSet;

/**
 * generic one-dimensional function interface
 *
 * @author rstein
 */
public interface Function1D extends Function {

    double getValue(double x);

    default double[] getValues(final double[] x) {
        if (x == null) {
            throw new IllegalArgumentException("x array argument is null");
        }
        final double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            y[i] = getValue(x[i]);
        }
        return y;
    }

    /**
     * @return DataSet representation of the function
     * @param xValues
     *            X coordinate for which the function should be evaluated
     */
    default DataSet getDataSetEstimate(final double[] xValues) {
        return new DoubleErrorDataSet(getName(), xValues, getValues(xValues));
    }

    /**
     * @param xmin
     *            min. x range
     * @param xmax
     *            max x range
     * @param nsamples
     *            number of sample points
     * @return DataSet representation of the function
     */
    default DataSet getDataSetEstimate(final double xmin, final double xmax, final int nsamples) {
        if (xmin > xmax || nsamples <= 0) {
            throw new InvalidParameterException("AbstractFunciton1D::getDataSetEstimate(" + xmin + "," + xmax + ","
                    + nsamples + ") - " + "invalid range");
        }
        final double[] xValues = new double[nsamples];
        final double step = (xmax - xmin) / nsamples;
        for (int i = 0; i < nsamples; i++) {
            xValues[i] = xmin + i * step;
        }
        return getDataSetEstimate(xValues);
    }

}
