package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.EventRateLimiter.UpdateStrategy;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.math.MathDataSet.DataSetValueFunction;

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
        }, -1, null, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getXValues(), identityDataSet.getXValues());
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());
        assertArrayEquals(yErrorNeg, identityDataSet.getErrorsNegative(DataSet.DIM_Y));
        assertArrayEquals(yErrorPos, identityDataSet.getErrorsPositive(DataSet.DIM_Y));
    }

    @Test
    public void testConstructorParameterAssertions() {
        final int nBins = 512;
        final DoubleDataSet dsRef1 = generateSineWaveData(nBins);
        final DoubleDataSet dsRef2 = generateSineWaveData(nBins);

        assertThrows(IllegalArgumentException.class, () -> new MathDataSet("I", null, null, null, 20, UpdateStrategy.INSTANTANEOUS_RATE));

        assertThrows(IllegalArgumentException.class, () -> new MathDataSet("I", null, null, identityValueFunction, 20, UpdateStrategy.INSTANTANEOUS_RATE, dsRef1, dsRef2));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, 20, UpdateStrategy.INSTANTANEOUS_RATE, dsRef1));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, -1, UpdateStrategy.INSTANTANEOUS_RATE, dsRef1));

        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, null, null, 20, null, dsRef1));

        // test specific constructors

        // DataSet -> DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, dsRef1));
        assertDoesNotThrow(() -> new MathDataSet("I", ds -> ds, 20, null, dsRef1));

        // List<DataSet> -> DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", (inputs, out) -> out.set(inputs.get(0)), dsRef1, dsRef2));
        assertDoesNotThrow(() -> new MathDataSet("I", (inputs, out) -> out.set(inputs.get(0)), 20, null, dsRef1, dsRef2));

        // modify only yValues in DataSet
        assertDoesNotThrow(() -> new MathDataSet("I", identityValueFunction, dsRef1));
        assertDoesNotThrow(() -> new MathDataSet("I", identityValueFunction, 20, null, dsRef1));
    }

    @Test
    public void testDataSetFunction() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        final DoubleDataSet magDataSetRef = generateSineWaveSpectrumData(nBins);
        assertEquals(nBins, rawDataSetRef.getDataCount());

        MathDataSet magDataSet = new MathDataSet("magI", dataSets -> {
            assertEquals(nBins, dataSets.getDataCount());
            return DataSetMath.magnitudeSpectrumDecibel(dataSets);
        }, rawDataSetRef);
        assertArrayEquals(magDataSetRef.getYValues(), magDataSet.getYValues());

        magDataSet = new MathDataSet(null, dataSets -> {
            assertEquals(nBins, dataSets.getDataCount());
            return DataSetMath.magnitudeSpectrumDecibel(dataSets);
        }, rawDataSetRef);
        assertArrayEquals(magDataSetRef.getYValues(), magDataSet.getYValues());
    }

    @Test
    public void testIdentity() {
        final int nBins = 512;
        final DoubleDataSet rawDataSetRef = generateSineWaveData(nBins);
        assertEquals(nBins, rawDataSetRef.getDataCount());

        MathDataSet identityDataSet = new MathDataSet("I", (input, output, length) -> {
            assertEquals(nBins, input.length);
            assertEquals(nBins, length);
            assertArrayEquals(rawDataSetRef.getYValues(), input, "yValue input equality with source");

            // identity function
            System.arraycopy(input, 0, output, 0, length);
        }, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());

        identityDataSet = new MathDataSet(null, (input, output, length) -> {
            assertEquals(nBins, input.length);
            assertEquals(nBins, length);
            assertArrayEquals(rawDataSetRef.getYValues(), input, "yValue input equality with source");

            // identity function
            System.arraycopy(input, 0, output, 0, length);
        }, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());
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
        }, -1, null, rawDataSetRef);
        assertArrayEquals(rawDataSetRef.getYValues(), identityDataSet.getYValues());
        identityDataSet.addListener(evt -> counter2.incrementAndGet());

        // has been initialised once during construction
        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());
        counter1.set(0);

        // null does not invoke update
        rawDataSetRef.invokeListener(null, false);
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // null does not invoke update
        rawDataSetRef.invokeListener(new UpdateEvent(rawDataSetRef, "wrong event"), false);
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // null does not invoke update
        rawDataSetRef.invokeListener(new UpdateEvent(identityDataSet, "wrong reference", false));
        assertEquals(0, counter1.get());
        assertEquals(0, counter2.get());

        // AddedDataEvent does invoke update
        rawDataSetRef.invokeListener(new AddedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());

        // RemovedDataEvent does invoke update
        rawDataSetRef.invokeListener(new RemovedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(2, counter1.get());
        assertEquals(2, counter2.get());
        rawDataSetRef.invokeListener(new RemovedDataEvent(identityDataSet, "wrong reference", false));
        assertEquals(3, counter1.get());
        assertEquals(3, counter2.get());

        // UpdatedDataEvent does invoke update
        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(4, counter1.get());
        assertEquals(4, counter2.get());

        assertEquals(1, rawDataSetRef.updateEventListener().size());
        identityDataSet.deregisterListener();
        assertEquals(0, rawDataSetRef.updateEventListener().size());
        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(4, counter1.get());
        assertEquals(4, counter2.get());

        identityDataSet.registerListener();
        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(5, counter1.get());
        assertEquals(5, counter2.get());

        assertEquals(1, identityDataSet.getSourceDataSets().size());
        identityDataSet.getSourceDataSets().clear();
        assertEquals(0, identityDataSet.getSourceDataSets().size());

        rawDataSetRef.invokeListener(new UpdatedDataEvent(rawDataSetRef, "OK reference", false));
        assertEquals(5, counter1.get());
        assertEquals(6, counter2.get());
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
