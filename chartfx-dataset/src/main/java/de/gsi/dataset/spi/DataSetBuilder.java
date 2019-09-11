package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.AssertUtils;

public class DataSetBuilder {
    protected String name;
    protected double[] xValues;
    protected double[] yValues;
    protected double[] xErrorsPos;
    protected double[] xErrorsNeg;
    protected double[] yErrorsPos;
    protected double[] yErrorsNeg;
    protected List<String> infoList = new ArrayList<>();
    protected List<String> warningList = new ArrayList<>();
    protected List<String> errorList = new ArrayList<>();
    protected Map<String, String> metaInfoMap = new HashMap<>();
    protected Map<Integer, String> dataLabels = new HashMap<>();
    protected Map<Integer, String> dataStyles = new HashMap<>();
    protected List<AxisDescription> axisDescription = new ArrayList<>();
    protected String xAxisName;
    protected String yAxisName;
    protected String xAxisUnit;
    protected String yAxisUnit;
    protected double xMin = Double.NaN;
    protected double xMax = Double.NaN;
    protected double yMin = Double.NaN;
    protected double yMax = Double.NaN;
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

    public final DataSetBuilder setXPosErrorNoCopy(final double[] xErrorValuesPos) { // NOPMD
        // direct storage is on purpose
        this.xErrorsPos = xErrorValuesPos;
        return this;
    }

    public final DataSetBuilder setXPosError(final double[] xErrorValuesPos) {
        final int size = initialCapacity < 0 ? xErrorValuesPos.length
                : Math.min(initialCapacity, xErrorValuesPos.length);
        this.xErrorsPos = new double[size];
        System.arraycopy(xErrorValuesPos, 0, this.xErrorsPos, 0, size);
        return this;
    }

    public DataSetBuilder setXNegErrorNoCopy(final double[] xErrorValuesNeg) { // NOPMD
        // direct storage is on purpose
        this.yErrorsNeg = xErrorValuesNeg;
        return this;
    }

    public DataSetBuilder setXNegError(final double[] xErrorValuesNeg) {
        final int size = initialCapacity < 0 ? xErrorValuesNeg.length
                : Math.min(initialCapacity, xErrorValuesNeg.length);
        this.xErrorsNeg = new double[size];
        System.arraycopy(xErrorValuesNeg, 0, this.xErrorsNeg, 0, size);
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

    public DataSetBuilder setMetaInfoList(final String[] infos) {
        this.infoList.addAll(Arrays.asList(infos));
        return this;
    }

    public DataSetBuilder setMetaWarningList(final String[] warning) {
        this.warningList.addAll(Arrays.asList(warning));
        return this;
    }

    public DataSetBuilder setMetaErrorList(final String[] errors) {
        this.errorList.addAll(Arrays.asList(errors));
        return this;
    }

    public DataSetBuilder setMetaInfoMap(final Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return this;
        }
        metaInfoMap.putAll(map);
        return this;
    }

    public DataSetBuilder setDataLabelMap(final Map<Integer, String> map) {
        if (map == null || map.isEmpty()) {
            return this;
        }
        dataLabels.putAll(map);
        return this;
    }

    public DataSetBuilder setDataStyleMap(final Map<Integer, String> map) {
        if (map == null || map.isEmpty()) {
            return this;
        }
        dataStyles.putAll(map);
        return this;
    }

    public DataSetBuilder setXMin(final double value) {
        xMin = value;
        return this;
    }

    public DataSetBuilder setXMax(final double value) {
        xMax = value;
        return this;
    }

    public DataSetBuilder setYMin(final double value) {
        yMin = value;
        return this;
    }

    public DataSetBuilder setYMax(final double value) {
        yMax = value;
        return this;
    }

    //
    // -- BUILD OPERATIONS -------------------------------------------
    //

    protected DefaultDataSet buildWithYArrayOnly(final String dsName) {
        final int size = initialCapacity < 0 ? yValues.length : Math.min(yValues.length, initialCapacity);
        final double[] dsX = new double[size];
        for (int i = 0; i < size; i++) {
            dsX[i] = i;
        }
        return new DefaultDataSet(dsName, dsX, yValues, size, false);
    }

    protected DefaultErrorDataSet buildWithYErrors(final String dsName, final int size) {
        // at least one error array has been provided
        final double[] dsYep = yErrorsPos == null ? yErrorsNeg : yErrorsPos;
        final double[] dsYen = yErrorsNeg == null ? yErrorsPos : yErrorsNeg;
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, size);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, size);

        return new DefaultErrorDataSet(dsName, xValues, yValues, dsYen, dsYep, size, false);
    }

    protected DataSet buildRawDataSet(final String dsName) {
        if (xValues == null && yValues == null) {
            // no X/Y arrays provided
            return new DefaultDataSet(dsName, Math.max(initialCapacity, 0));
        }

        if (xValues == null) {
            // no X array provided
            return buildWithYArrayOnly(dsName);
        }

        if (yValues == null) {
            throw new IllegalStateException("yValues is null");
        }

        final int minArrays = Math.min(xValues.length, yValues.length);
        final int size = initialCapacity < 0 ? minArrays : Math.min(minArrays, initialCapacity);
        DataSet dataSet;
        if (yErrorsNeg == null && yErrorsPos == null) {
            // no error arrays -> build non-error data set
            dataSet = new DefaultDataSet(dsName, xValues, yValues, size, false);
        } else {
            // at least one error array has been provided
            dataSet = buildWithYErrors(dsName, size);
        }

        return dataSet;
    }

    protected void addMetaData(final DataSet dataSet) {
        if (!(dataSet instanceof AbstractDataSet)) {
            return;
        }
        AbstractDataSet<?> ds = (AbstractDataSet<?>) dataSet;
        ds.getInfoList().addAll(infoList);
        ds.getWarningList().addAll(warningList);
        ds.getErrorList().addAll(errorList);
        ds.getMetaInfo().putAll(metaInfoMap);
    }

    protected void addDataRanges(final DataSet dataSet) {
        if (!(dataSet instanceof AbstractDataSet)) {
            return;
        }
        AbstractDataSet<?> ds = (AbstractDataSet<?>) dataSet;
        ds.getAxisDescription(0).set(xAxisName, xAxisUnit, xMin, xMax);
        ds.getAxisDescription(1).set(yAxisName, yAxisUnit, yMin, yMax);

        for (AxisDescription axis : ds.getAxisDescriptions()) {
            if (!axis.isDefined()) {
                ds.recomputeLimits(ds.getAxisDescriptions().indexOf(axis));
            }
        }
    }

    protected void addDataLabelStyleMap(final DataSet dataSet) {
        if (!(dataSet instanceof AbstractDataSet)) {
            return;
        }
        AbstractDataSet<?> ds = (AbstractDataSet<?>) dataSet;
        if (!dataLabels.isEmpty()) {
            dataLabels.forEach(ds::addDataLabel);
        }
        if (!dataStyles.isEmpty()) {
            dataStyles.forEach(ds::addDataStyle);
        }
    }

    public DataSet build() {
        final String dsName = name == null ? ("DataSet@" + System.currentTimeMillis()) : name;
        DataSet dataSet = buildRawDataSet(dsName);

        // add meta data
        addMetaData(dataSet);

        addDataRanges(dataSet);

        addDataLabelStyleMap(dataSet);

        return dataSet;
    }
}
