package de.gsi.math;

import static de.gsi.dataset.event.EventRateLimiter.UpdateStrategy.INSTANTANEOUS_RATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventRateLimiter;
import de.gsi.dataset.event.EventRateLimiter.UpdateStrategy;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.DoubleErrorDataSet;

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
    private final transient EventListener eventListener;
    private final transient List<DataSet> sourceDataSets;
    private final transient DataSetFunction dataSetFunction;
    private final transient DataSetsFunction dataSetsFunction;
    private final transient DataSetValueFunction dataSetValueFunction;
    private final transient long minUpdatePeriod; // NOPMD
    private final transient UpdateStrategy updateStrategy; // NOPMD
    private final transient String transformName;

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetFunction} for details
     * @param source reference source DataSet
     * N.B. a default minUpdatePeriod of 40 milliseconds and {@link UpdateStrategy#INSTANTANEOUS_RATE} is assumed
     */
    public MathDataSet(final String transformName, DataSetFunction dataSetFunction, final DataSet source) {
        this(transformName, dataSetFunction, null, null, DEFAULT_UPDATE_LIMIT, INSTANTANEOUS_RATE, source);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetFunction} for details
     * @param minUpdatePeriod the minimum time in milliseconds. With {@link UpdateStrategy#INSTANTANEOUS_RATE} this implies
     *            a minimum update time-out
     * @param updateStrategy if null defaults to {@link UpdateStrategy#INSTANTANEOUS_RATE}, see {@link UpdateStrategy} for
     *            details
     * @param source reference source DataSet
     */
    public MathDataSet(final String transformName, final DataSetFunction dataSetFunction, final long minUpdatePeriod, final UpdateStrategy updateStrategy, final DataSet source) {
        this(transformName, dataSetFunction, null, null, minUpdatePeriod, updateStrategy, source);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetsFunction} for details
     * @param sources reference source DataSet array
     * N.B. a default minUpdatePeriod of 40 milliseconds and {@link UpdateStrategy#INSTANTANEOUS_RATE} is assumed
     */
    public MathDataSet(final String transformName, final DataSetsFunction dataSetFunction, final DataSet... sources) {
        this(transformName, null, dataSetFunction, null, DEFAULT_UPDATE_LIMIT, INSTANTANEOUS_RATE, sources);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetsFunction} for details
     * @param minUpdatePeriod the minimum time in milliseconds. With {@link UpdateStrategy#INSTANTANEOUS_RATE} this implies
     *            a minimum update time-out
     * @param updateStrategy if null defaults to {@link UpdateStrategy#INSTANTANEOUS_RATE}, see {@link UpdateStrategy} for
     *            details
     * @param sources reference source DataSet array
     */
    public MathDataSet(final String transformName, final DataSetsFunction dataSetFunction, final long minUpdatePeriod, final UpdateStrategy updateStrategy,
            final DataSet... sources) {
        this(transformName, null, dataSetFunction, null, minUpdatePeriod, updateStrategy, sources);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetValueFunction} for details
     * @param source reference source DataSet
     * N.B. a default minUpdatePeriod of 40 milliseconds and {@link UpdateStrategy#INSTANTANEOUS_RATE} is assumed
     */
    public MathDataSet(final String transformName, DataSetValueFunction dataSetFunction, final DataSet source) {
        this(transformName, null, null, dataSetFunction, DEFAULT_UPDATE_LIMIT, INSTANTANEOUS_RATE, source);
    }

    /**
     * @param transformName String defining the prefix of the name of the calculated DataSet
     * @param dataSetFunction the DataSet in-to-out transform. see {@link DataSetValueFunction} for details
     * @param minUpdatePeriod the minimum time in milliseconds. With {@link UpdateStrategy#INSTANTANEOUS_RATE} this implies
     *            a minimum update time-out
     * @param updateStrategy if null defaults to {@link UpdateStrategy#INSTANTANEOUS_RATE}, see {@link UpdateStrategy} for
     *            details
     * @param source reference source DataSet
     */
    public MathDataSet(final String transformName, final DataSetValueFunction dataSetFunction, final long minUpdatePeriod, final UpdateStrategy updateStrategy,
            final DataSet source) {
        this(transformName, null, null, dataSetFunction, minUpdatePeriod, updateStrategy, source);
    }

    protected MathDataSet(final String transformName, DataSetFunction dataSetFunction, DataSetsFunction dataSetsFunction, DataSetValueFunction dataSetValueFunction,
            final long minUpdatePeriod, UpdateStrategy updateStrategy, final DataSet... sources) {
        super(getCompositeDataSetName(transformName, sources));
        this.sourceDataSets = new ArrayList<>(Arrays.asList(sources));
        this.minUpdatePeriod = minUpdatePeriod;
        this.updateStrategy = updateStrategy == null ? INSTANTANEOUS_RATE : updateStrategy;
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

        if (minUpdatePeriod > 0) {
            eventListener = new EventRateLimiter(this::handle, this.minUpdatePeriod, this.updateStrategy);
        } else {
            eventListener = this::handle;
        }
        registerListener(); // NOPMD

        // exceptionally call handler during DataSet creation
        handle(new UpdatedDataEvent(this, MathDataSet.class.getSimpleName() + " - initial constructor update"));
    }

    public final void deregisterListener() {
        sourceDataSets.forEach(srcDataSet -> srcDataSet.removeListener(eventListener));
    }

    public final List<DataSet> getSourceDataSets() {
        return sourceDataSets;
    }

    public final void registerListener() {
        sourceDataSets.forEach(srcDataSet -> srcDataSet.addListener(eventListener));
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

    protected void handle(UpdateEvent event) {
        boolean isKnownEvent = event instanceof AddedDataEvent || event instanceof RemovedDataEvent || event instanceof UpdatedDataEvent;
        if (event == null || !isKnownEvent) {
            return;
        }
        this.lock().writeLockGuard(() -> {
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
        });
        fireInvalidated(new UpdatedDataEvent(this, "propagated update from source " + this.getName()));
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
