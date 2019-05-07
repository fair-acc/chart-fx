package de.gsi.chart.data.testdata;

import de.gsi.chart.data.DataSet;

/**
 * Standard interface for test data set
 *
 * @author rstein
 */
public interface TestDataSet<D extends TestDataSet<D>> extends DataSet {

    /**
     * generate a new set of numbers
     */
    D update();

    /**
     * generate test data set
     *
     * @param count
     *            number of bins
     * @return the generated array
     */
    double[] generateX(final int count);

    /**
     * generate test data set
     *
     * @param count
     *            number of bins
     * @return the generated array
     */
    double[] generateY(final int count);

    D fireInvalidated();
}
