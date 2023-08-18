package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import io.fair_acc.dataset.event.UpdatedMetaDataEvent;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;

/**
 * Generic DataSet interface tests that (nearly) all DataSets should fulfill.
 *
 * @author rstein
 */
class GenericDataSetTests {
    private static final int DEFAULT_COUNT_MAX = 100;
    private static final String DEFAULT_DATASET_NAME1 = "testDataSet1";
    private static final String DEFAULT_DATASET_NAME2 = "testDataSet1";

    static Stream<Class<? extends DataSet>> dataSetClassProvider() {
        return Stream.of(
                AveragingDataSet.class,
                CircularDoubleErrorDataSet.class,
                DefaultDataSet.class, DefaultErrorDataSet.class, DoubleDataSet.class, DoubleErrorDataSet.class,
                FifoDoubleErrorDataSet.class, FloatDataSet.class, FragmentedDataSet.class,
                LimitedIndexedTreeDataSet.class,
                MultiDimDoubleDataSet.class,
                // RollingDataSet.class,
                WrappedDataSet.class);
    }

    @ParameterizedTest
    @MethodSource("dataSetClassProvider")
    @Timeout(1)
    void testEquality(final Class<DataSet> clazz) throws AssertionError {
        DataSet dataSet1 = getDefaultTestDataSet(clazz, DEFAULT_DATASET_NAME1, DEFAULT_COUNT_MAX + 2);
        assertNotNull(dataSet1, "test data set: " + clazz.getName());
        assertEquals(DEFAULT_DATASET_NAME1, dataSet1.getName());
        DataSet dataSet2 = getDefaultTestDataSet(clazz, DEFAULT_DATASET_NAME2, DEFAULT_COUNT_MAX + 3);
        assertNotNull(dataSet2, "test data set: " + clazz.getName());
        assertEquals(DEFAULT_DATASET_NAME2, dataSet2.getName());

        assertEquals(dataSet1, dataSet2);
    }

    @ParameterizedTest
    @MethodSource("dataSetClassProvider")
    @Timeout(1)
    void testSetDataSet(final Class<DataSet> clazz) throws AssertionError {
        DataSet dataSet = getDefaultTestDataSet(clazz, DEFAULT_DATASET_NAME1, DEFAULT_COUNT_MAX);
        assertNotNull(dataSet, "test data set: " + clazz.getName());

        DoubleDataSet testDataSet = new DoubleDataSet("DataSet");
        DoubleErrorDataSet testDataSetError = new DoubleErrorDataSet("DataSetError");
        final double error = (dataSet instanceof DataSetError) ? 0.1 : 0.0;
        for (int i = 0; i < 10; i++) {
            testDataSet.add(i, i);
            testDataSetError.add(10 + i, 5 + i, error, error);
        }

        for (int plane = 0; plane < 2; plane++) {
            testDataSet.recomputeLimits(plane);
            testDataSetError.recomputeLimits(plane);
        }


        final AtomicInteger notifyCounter = new AtomicInteger();
        final int bit = BitState.mask(ChartBits.DataSetDataAdded);
        dataSet.getBitState().addChangeListener(bit, (src, bits) -> notifyCounter.getAndIncrement());

        dataSet.getBitState().clear(bit);
        assertDoesNotThrow(() -> dataSet.set(testDataSet));
        assertSameDataRanges(testDataSet, dataSet);
        dataSet.recomputeLimits(DIM_X);
        dataSet.recomputeLimits(DIM_Y);
        assertSameDataRanges(testDataSet, dataSet);

        assertEquals(1, notifyCounter.get());
        dataSet.getBitState().clear(bit);
        assertDoesNotThrow(() -> dataSet.set(testDataSet, true));
        assertEquals(2, notifyCounter.get());
        dataSet.getBitState().clear(bit);
        assertDoesNotThrow(() -> dataSet.set(testDataSet, false));
        assertEquals(3, notifyCounter.get());
        dataSet.getBitState().clear(bit);

        notifyCounter.set(0);
        assertDoesNotThrow(() -> dataSet.set(testDataSetError));
        assertSameDataRanges(testDataSetError, dataSet);
        dataSet.recomputeLimits(DIM_X);
        dataSet.recomputeLimits(DIM_Y);
        assertSameDataRanges(testDataSetError, dataSet);

        assertEquals(1, notifyCounter.get());
        dataSet.getBitState().clear(bit);
        assertDoesNotThrow(() -> dataSet.set(testDataSetError, true));
        assertEquals(2, notifyCounter.get());
        dataSet.getBitState().clear(bit);
        assertDoesNotThrow(() -> dataSet.set(testDataSetError, false));
        assertEquals(3, notifyCounter.get());
        dataSet.getBitState().clear(bit);
    }

    public static void assertSameDataRanges(final DataSet reference, final DataSet test) throws AssertionError {
        assertEquals(reference.getAxisDescription(DIM_X).getMin(), test.getAxisDescription(DIM_X).getMin());
        assertEquals(reference.getAxisDescription(DIM_X).getMax(), test.getAxisDescription(DIM_X).getMax());
        assertEquals(reference.getAxisDescription(DIM_Y).getMin(), test.getAxisDescription(DIM_Y).getMin());
        assertEquals(reference.getAxisDescription(DIM_Y).getMax(), test.getAxisDescription(DIM_Y).getMax());
    }

    public static DataSet getDefaultTestDataSet(final Class<? extends DataSet> clazz, final String dataSetName, final int initialCapacity) throws AssertionError {
        try {
            return Objects.requireNonNull(clazz, "clazz must not be null").getDeclaredConstructor(String.class).newInstance(dataSetName);
        } catch (Exception t) { // NOPMD NOSONAR
            // re-try with alternate initialisation
            try {
                return Objects.requireNonNull(clazz, "clazz must not be null").getDeclaredConstructor(String.class, int.class).newInstance(dataSetName, initialCapacity);
            } catch (Exception e) { // NOPMD NOSONAR
                // aggregate different types and sterilise to 'Exception'
                throw new AssertionError("exception while constructing", e);
            }
        }
    }
}
