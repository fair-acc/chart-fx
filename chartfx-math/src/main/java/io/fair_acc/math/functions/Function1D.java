package io.fair_acc.math.functions;

import java.security.InvalidParameterException;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;

/**
 * generic one-dimensional function interface
 *
 * @author rstein
 */
public interface Function1D extends Function {
    /**
     * @param xmin min. x range
     * @param xmax max x range
     * @param nsamples number of sample points
     * @return DataSet representation of the function
     */
    default DataSet getDataSetEstimate(final double xmin, final double xmax, final int nsamples) {
        if (xmin > xmax || nsamples <= 0) {
            throw new InvalidParameterException("AbstractFunciton1D::getDataSetEstimate(" + xmin + "," + xmax + ","
                                                + nsamples + ") - "
                                                + "invalid range");
        }
        final double[] xValues = new double[nsamples];
        final double step = (xmax - xmin) / nsamples;
        for (int i = 0; i < nsamples; i++) {
            xValues[i] = xmin + i * step;
        }
        return getDataSetEstimate(xValues);
    }

    /**
     * @return DataSet representation of the function
     * @param xValues X coordinate for which the function should be evaluated
     */
    default DataSet getDataSetEstimate(final double[] xValues) {
        return new DefaultErrorDataSet(getName(), xValues, getValues(xValues), new double[xValues.length],
                new double[xValues.length], xValues.length, true);
    }

    double getValue(final double x);

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
}
