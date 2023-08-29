package io.fair_acc.math;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;

/**
 * DataSet that automatically transforms source DataSet accordance to
 * DataSetFunction or DataSetValueFunction definition. An optional rate limit is
 * available to limit the number of redundant (GUI) updates if desired.
 *
 * @author rstein
 */
public class MathDataSet extends DoubleErrorDataSet {
    private static final long serialVersionUID = -4978160822533565009L;
    private static final long DEFAULT_UPDATE_LIMIT = 40;
    private final transient List<DataSet> sourceDataSets;
    private final transient BitState dataSourceState = BitState.initDirtyMultiThreaded(this, ChartBits.DataSetMask);
    private final transient DataSetFunction dataSetFunction;
    private final transient DataSetsFunction dataSetsFunction;
    private final transient DataSetValueFunction dataSetValueFunction;
    private final transient long minUpdatePeriod; // NOPMD
    //private final transient UpdateStrategy updateStrategy; // NOPMD
    private final transient String transformName;

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetFunction} for details
     * @param source reference source DataSet
     */
    public MathDataSet(final String transformName, DataSetFunction dataSetFunction, final DataSet source) {
        this(transformName, dataSetFunction, null, null, DEFAULT_UPDATE_LIMIT, source);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetFunction} for details
     *            a minimum update time-out
     * @param source reference source DataSet
     */
    public MathDataSet(final String transformName, final DataSetFunction dataSetFunction, final long minUpdatePeriod, final DataSet source) {
        this(transformName, dataSetFunction, null, null, minUpdatePeriod, source);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetsFunction} for details
     * @param sources reference source DataSet array
     */
    public MathDataSet(final String transformName, final DataSetsFunction dataSetFunction, final DataSet... sources) {
        this(transformName, null, dataSetFunction, null, DEFAULT_UPDATE_LIMIT, sources);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetsFunction} for details
     * @param sources reference source DataSet array
     */
    public MathDataSet(final String transformName, final DataSetsFunction dataSetFunction, final long minUpdatePeriod, final DataSet... sources) {
        this(transformName, null, dataSetFunction, null, minUpdatePeriod, sources);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetValueFunction} for details
     * @param source reference source DataSet
     */
    public MathDataSet(final String transformName, DataSetValueFunction dataSetFunction, final DataSet source) {
        this(transformName, null, null, dataSetFunction, DEFAULT_UPDATE_LIMIT, source);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetValueFunction} for details
     * @param source reference source DataSet
     */
    public MathDataSet(final String transformName, final DataSetValueFunction dataSetFunction, final long minUpdatePeriod, final DataSet source) {
        this(transformName, null, null, dataSetFunction, minUpdatePeriod, source);
    }

    protected MathDataSet(final String transformName, DataSetFunction dataSetFunction, DataSetsFunction dataSetsFunction, DataSetValueFunction dataSetValueFunction,
            final long minUpdatePeriod, final DataSet... sources) {
        super(getCompositeDataSetName(transformName, sources));
        this.sourceDataSets = new ArrayList<>(Arrays.asList(sources));
        this.minUpdatePeriod = minUpdatePeriod;
        this.dataSetFunction = dataSetFunction;
        this.dataSetsFunction = dataSetsFunction;
        this.dataSetValueFunction = dataSetValueFunction;
        this.transformName = transformName;

        if (dataSetFunction == null && dataSetsFunction == null && dataSetValueFunction == null) {
            throw new IllegalArgumentException("dataSetFunction, dataSetsFunction and dataSetValueFunction cannot all be null");
        }

        if (dataSetValueFunction != null && sourceDataSets.size() > 1) {
            throw new IllegalArgumentException(
                    "sources list may not be larger than one if the 'dataSetValueFunction' interface is used"
                    + " -> try to use 'DataSetFunction' instead");
            // N.B. rationale is that if one combines data from more than one DataSet
            // that it's very likely that they have different x vectors/sampling.
            // This usually requires a more sophisticated approach better handled through
            // the 'DataSetFunction' interface
        }

        // TODO: the updates currently get computed on the change listener thread. When should this happen concurrently?
        // TODO: maybe trigger the update from the chart preLayout?
        dataSourceState.addChangeListener((obj, bits) -> update());

        // exceptionally call handler during DataSet creation
        registerListener(); // NOPMD

        // call handler for initial constructor update
        update();
    }

    public final void deregisterListener() {
        sourceDataSets.forEach(srcDataSet -> srcDataSet.getBitState().removeInvalidateListener(dataSourceState));
    }

    public final List<DataSet> getSourceDataSets() {
        return sourceDataSets;
    }

    public final void registerListener() {
        sourceDataSets.forEach(srcDataSet -> srcDataSet.getBitState().addInvalidateListener(dataSourceState));
    }

    private void handleDataSetValueFunctionInterface() {
        final DataSet dataSet = sourceDataSets.get(0);
        final int length = dataSet.getDataCount();
        final double[] xSourceVector = dataSet.getValues(DIM_X);
        final double[] ySourceVector = dataSet.getValues(DIM_Y);
        final double[] ySourceErrorPos;
        final double[] ySourceErrorNeg;
        if (dataSet instanceof DataSetError) {
            DataSetError dsError = (DataSetError) dataSet;
            ySourceErrorPos = dsError.getErrorsPositive(DIM_Y);
            ySourceErrorNeg = dsError.getErrorsNegative(DIM_Y);
        } else {
            ySourceErrorPos = new double[length];
            ySourceErrorNeg = ySourceErrorPos;
        }
        if (this.getCapacity() < length) {
            final int amount = length - this.getCapacity();
            this.increaseCapacity(amount);
        }
        final double[] xDestVector = this.getValues(DIM_X);
        final double[] yDestVector = this.getValues(DIM_Y);

        // copy x-array values
        System.arraycopy(xSourceVector, 0, xDestVector, 0, length);
        // operation is in place using the y-array values of 'this'
        dataSetValueFunction.transform(ySourceVector, yDestVector, length);
        this.set(xDestVector, yDestVector, ySourceErrorNeg, ySourceErrorPos, length, false); // N.B zero copy re-use of
                // existing array
    }

    protected void update() {
        this.lock().writeLockGuard(() -> {
            if (dataSourceState.isClean()) {
                return;
            }
            dataSourceState.clear();
            if (dataSetFunction != null) {
                set(dataSetFunction.transform(sourceDataSets.get(0)));
            } else if (dataSetsFunction != null) {
                dataSetsFunction.transform(sourceDataSets, this);
            } else {
                if (sourceDataSets.isEmpty()) {
                    return;
                }
                handleDataSetValueFunctionInterface();
            }

            this.setName(getCompositeDataSetName(transformName, sourceDataSets.toArray(new DataSet[0])));
            // Note: the data bit is already invalidated at the storing data set level
        });
    }

    protected static String getCompositeDataSetName(final String transformName, final DataSet... sources) {
        final List<DataSet> dataSets = Arrays.asList(sources);
        final String sanitizedFunctionName = transformName == null ? "" : transformName;
        return dataSets.stream().map(DataSet::getName).collect(Collectors.joining(",", sanitizedFunctionName + "(", ")"));
    }

    /**
     * simple DataSet transform function definition for single input DataSets
     *
     * @author rstein
     */
    public interface DataSetFunction {
        DataSet transform(final DataSet input);
    }

    /**
     * simple DataSet transform function definition for multiple input DataSets
     *
     * @author rstein
     */
    public interface DataSetsFunction {
        void transform(final List<DataSet> inputDataSet, final MathDataSet outputDataSet);
    }

    /**
     * simple DataSet transform function definition, only the y value is being
     * transformed, the x-axis is taken from the source DataSet
     *
     * @author rstein
     */
    public interface DataSetValueFunction {
        void transform(final double[] inputY, final double[] outputY, final int length);
    }
}
