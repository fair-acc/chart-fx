package io.fair_acc.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.math.MathDataSet.DataSetValueFunction;

/**
 * Basic tests for DataSetMath class
 *
 * @author rstein
 */
public class MathDataSetTests {
    private final DataSetValueFunction identityValueFunction = (input, output, len) -> System.arraycopy(input, 0, output, 0, len);
    @Test
    public void errorDataSetTests() {
        final int nBins = 512;
        final DoubleErrorDataSet rawDataSetRef = new DoubleErrorDataSet(generateSineWaveData(nBins));
        final double[] yErrorNeg = rawDataSetRef.getErrorsNegative(DataSet.DIM_Y);
        final double[] yErrorPos = rawDataSetRef.getErrorsPositive(DataSet.DIM_Y);
        for (int i = 0; i < nBins; i++) {
            yErrorNeg[i] = i * nBins;
            yErrorPos[i] = i * nBins + 0.5;
        }

        MathDataSet identityDataSet = new MathDataSet("N", null, null, (input, output, length) -> {
            // identity function
            System.arraycopy(input, 0, output, 0, length);
        }, -1, rawDataSetRef);
        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetData);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());
        assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_X), identityDataSet.getValues(DataSet.DIM_X));
        assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_Y), identityDataSet.getValues(DataSet.DIM_Y));
        assertArrayEquals(yErrorNeg, identityDataSet.getErrorsNegative(DataSet.DIM_Y));
        assertArrayEquals(yErrorPos, identityDataSet.getErrorsPositive(DataSet.DIM_Y));
    }

    @Test
    public void testConstructorParameterAssertions() {
        final int nBins = 512;
        final DoubleDataSet dsRef1 = generateSineWaveData(nBins);
        final DoubleDataSet dsRef2 = generateSineWaveData(nBins);

        assertThrows(IllegalArgumentException.class, () -> new MathDataSet("I", null, null, null, 20));

        assertThrows(IllegalArgumentException.class, () -> new MathDataSet("I", null, null, identityValueFunction, 20, dsRef1, dsRef2));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, 20, dsRef1));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, -1, dsRef1));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, 20, dsRef1));

        // test specific constructors

        // DataSet -> DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, dsRef1));
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, 20, dsRef1));

        // List<DataSet> -> DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", (inputs, out) -> out.set(inputs.get(0)), dsRef1, dsRef2));
        assertDoesNotThrow(() -> new MathDataSet("I", (inputs, out) -> out.set(inputs.get(0)), 20, dsRef1, dsRef2));

        // modify only yValues in DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", identityValueFunction, dsRef1));
        assertDoesNotThrow(() -> new MathDataSet("I", identityValueFunction, 20, dsRef1));
    }

    @Test
    public void testDataSetFunction() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        final DoubleDataSet magDataSetRef = generateSineWaveSpectrumData(nBins);
        assertEquals(nBins, rawDataSetRef.getDataCount());

        final MathDataSet magDataSet = new MathDataSet("magI", dataSets -> {
            assertEquals(nBins, dataSets.getDataCount());
            return DataSetMath.magnitudeSpectrumDecibel(dataSets);
        }, rawDataSetRef);
        magDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetData);
        Awaitility.await().until(() -> magDataSet.getBitState().isDirty());
        assertArrayEquals(magDataSetRef.getValues(DataSet.DIM_Y), magDataSet.getValues(DataSet.DIM_Y));

        final MathDataSet magDataSet2 = new MathDataSet(null, dataSets -> {
            assertEquals(nBins, dataSets.getDataCount());
            return DataSetMath.magnitudeSpectrumDecibel(dataSets);
        }, rawDataSetRef);
        magDataSet2.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetData);
        Awaitility.await().until(() -> magDataSet2.getBitState().isDirty());
        assertArrayEquals(magDataSetRef.getValues(DataSet.DIM_Y), magDataSet2.getValues(DataSet.DIM_Y));
    }

    @Test
    public void testIdentity() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        assertEquals(nBins, rawDataSetRef.getDataCount());

        final MathDataSet identityDataSet = new MathDataSet("I", (input, output, length) -> {
            assertEquals(nBins, input.length);
            assertEquals(nBins, length);
            assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_Y), input, "yValue input equality with source");

            // identity function
            System.arraycopy(input, 0, output, 0, length);
        }, rawDataSetRef);
        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetData);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());
        assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_Y), identityDataSet.getValues(DataSet.DIM_Y));

        final MathDataSet identityDataSet2 = new MathDataSet(null, (input, output, length) -> {
            assertEquals(nBins, input.length);
            assertEquals(nBins, length);
            assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_Y), input, "yValue input equality with source");

            // identity function
            System.arraycopy(input, 0, output, 0, length);
        }, rawDataSetRef);
        identityDataSet2.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetData);
        Awaitility.await().until(() -> identityDataSet2.getBitState().isDirty());
        assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_Y), identityDataSet2.getValues(DataSet.DIM_Y));
    }

    @Test
    public void testNotifies() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        final AtomicInteger counter1 = new AtomicInteger();
        final AtomicInteger counter2 = new AtomicInteger();

        MathDataSet identityDataSet = new MathDataSet("N", null, null, (input, output, length) -> {
            counter1.incrementAndGet();
            // identity function
            System.arraycopy(input, 0, output, 0, length);
        }, -1, rawDataSetRef);
        identityDataSet.getBitState().addInvalidateListener(ChartBits.DataSetData,
                (src, bits) -> counter2.incrementAndGet());

        // wrong event does not invoke update
        rawDataSetRef.fireInvalidated(ChartBits.ChartLegend);

        // AddedDataEvent does invoke update
        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetDataAdded);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());
        assertArrayEquals(rawDataSetRef.getValues(DataSet.DIM_Y), identityDataSet.getValues(DataSet.DIM_Y));

        // RemovedDataEvent does invoke update
        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetDataRemoved);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());

        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetDataRemoved);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());

        // UpdatedDataEvent does invoke update
        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetData);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());

        identityDataSet.getBitState().clear();
        rawDataSetRef.fireInvalidated(ChartBits.DataSetDataAdded);
        Awaitility.await().until(() -> identityDataSet.getBitState().isDirty());

    }

    protected static DoubleDataSet generateSineWaveData(final int nData) {
        DoubleDataSet function = new DoubleDataSet("composite sine", nData);
        for (int i = 0; i < nData; i++) {
            final double t = i;
            double y = 0;
            final double centreFrequency = 0.25;
            final double diffFrequency = 0.05;
            for (int j = 0; j < 8; j++) {
                final double a = 2.0 * Math.pow(10, -j);
                final double diff = j == 0 ? 0 : (j % 2 - 0.5) * j * diffFrequency;
                y += a * Math.sin(2.0 * Math.PI * (centreFrequency + diff) * t);
            }

            function.add(t, y);
        }
        return function;
    }

    protected static DoubleDataSet generateSineWaveSpectrumData(final int nData) {
        return new DoubleDataSet(DataSetMath.magnitudeSpectrumDecibel(generateSineWaveData(nData)));
    }
}
