package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.AssertUtils;

public class DataSetBuilder {
    protected String name;
    protected double[] xValues;
    protected double[] yValues;
    protected double[] yErrorsPos;
    protected double[] yErrorsNeg;
    protected int initialCapacity = -1;

    /**
     * default DataSet factory
     */
    public DataSetBuilder() {
        this("default data set");
    }

    /**
     * default DataSet factory
     * 
     * @param dataSetName data set name
     */
    public DataSetBuilder(final String dataSetName) {
        super();
        this.setName(dataSetName);
    }

    public final DataSetBuilder setName(final String name) {
        this.name = name;
        return this;
    }

    public final DataSetBuilder setXValuesNoCopy(final double[] xValues) { // NOPMD
        // direct storage is on purpose
        this.xValues = xValues;
        return this;
    }

    public final DataSetBuilder setXValues(final double[] xValues) {
        final int size = initialCapacity < 0 ? xValues.length : Math.min(initialCapacity, xValues.length);
        this.xValues = new double[size];
        System.arraycopy(xValues, 0, this.xValues, 0, size);
        return this;
    }

    public final DataSetBuilder setYValuesNoCopy(final double[] yValues) { // NOPMD
        // direct storage is on purpose
        this.yValues = yValues;
        return this;
    }

    public final DataSetBuilder setYValues(final double[] yValues) {
        final int size = initialCapacity < 0 ? yValues.length : Math.min(initialCapacity, yValues.length);
        this.yValues = new double[size];
        System.arraycopy(yValues, 0, this.yValues, 0, size);
        return this;
    }

    public final DataSetBuilder setYPosErrorNoCopy(final double[] yErrorValuesPos) { // NOPMD
        // direct storage is on purpose
        this.yErrorsPos = yErrorValuesPos;
        return this;
    }

    public final DataSetBuilder setYPosError(final double[] yErrorValuesPos) {
        final int size = initialCapacity < 0 ? yErrorValuesPos.length
                : Math.min(initialCapacity, yErrorValuesPos.length);
        this.yErrorsPos = new double[size];
        System.arraycopy(yErrorValuesPos, 0, this.yErrorsPos, 0, size);
        return this;
    }

    public DataSetBuilder setYNegErrorNoCopy(final double[] yErrorValuesNeg) { // NOPMD
        // direct storage is on purpose
        this.yErrorsNeg = yErrorValuesNeg;
        return this;
    }

    public DataSetBuilder setYNegError(final double[] yErrorValuesNeg) {
        final int size = initialCapacity < 0 ? yErrorValuesNeg.length
                : Math.min(initialCapacity, yErrorValuesNeg.length);
        this.yErrorsNeg = new double[size];
        System.arraycopy(yErrorValuesNeg, 0, this.yErrorsNeg, 0, size);
        return this;
    }

    public DataSet build() {
        final String dsName = name == null ? ("DataSet@" + System.currentTimeMillis()) : name;
        if (xValues == null && yValues == null) {
            // no X/Y arrays provided
            return new DefaultDataSet(dsName, Math.max(initialCapacity, 0));
        }
        // either at least X or Y array provided
        final double[] dsX = xValues == null ? new double[yValues.length] : xValues;
        final double[] dsY = yValues == null ? new double[xValues.length] : yValues;
        final int minArrays = Math.min(dsX.length, dsY.length);
        final int size = initialCapacity < 0 ? minArrays : Math.min(minArrays, initialCapacity);

        if (yErrorsNeg == null && yErrorsPos == null) {
            // no error arrays -> build non-error data set
            return new DefaultDataSet(dsName, dsX, dsY, size, false);
        }
        // at least one error array has been provided
        final double[] dsYep = yErrorsPos == null ? yErrorsNeg : yErrorsPos;
        final double[] dsYen = yErrorsNeg == null ? yErrorsPos : yErrorsNeg;
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, size);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, size);

        return new DefaultErrorDataSet(dsName, dsX, dsY, dsYen, dsYep, size, false);
    }
}
