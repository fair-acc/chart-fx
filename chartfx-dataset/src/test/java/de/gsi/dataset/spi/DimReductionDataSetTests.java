package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.spi.DimReductionDataSet.Option;
import de.gsi.dataset.spi.utils.MathUtils;

/**
 * Tests for the DimReductionDataSet
 * -- Reduces 3D data to 2D DataSet either via slicing, min, mean, max or integration
 *
 * @author rstein
 */
public class DimReductionDataSetTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetEqualityTests.class);
    private int nEvent = 0;

    @Test
    public void testGetterSetterConsistency() {
        LOGGER.atDebug().log("testGetterSetterConsistency");
        DoubleDataSet3D testData = new DoubleDataSet3D("test", //
                new double[] { 1, 2, 3 }, // x-array
                new double[] { 6, 7, 8 }, // y-array
                new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } });

        DimReductionDataSet reducedDataSetX = new DimReductionDataSet(testData, DIM_X, Option.INTEGRAL);
        DimReductionDataSet reducedDataSetY = new DimReductionDataSet(testData, DIM_Y, Option.INTEGRAL);

        reducedDataSetX.setMinValue(0.0);
        assertEquals(0.0, reducedDataSetX.getMinValue());
        reducedDataSetX.setMinValue(2.0);
        assertEquals(2.0, reducedDataSetX.getMinValue());

        reducedDataSetX.setMaxValue(0.0);
        assertEquals(0.0, reducedDataSetX.getMaxValue());
        reducedDataSetX.setMaxValue(2.0);
        assertEquals(2.0, reducedDataSetX.getMaxValue());

        reducedDataSetX.setRange(1.5, 2.5);
        assertEquals(1.5, reducedDataSetX.getMinValue());
        assertEquals(2.5, reducedDataSetX.getMaxValue());

        reducedDataSetY.setMinValue(0.0);
        assertEquals(0.0, reducedDataSetY.getMinValue());
        reducedDataSetY.setMinValue(2.0);
        assertEquals(2.0, reducedDataSetY.getMinValue());

        reducedDataSetY.setMaxValue(0.0);
        assertEquals(0.0, reducedDataSetY.getMaxValue());
        reducedDataSetY.setMaxValue(2.0);
        assertEquals(2.0, reducedDataSetY.getMaxValue());

        reducedDataSetY.setRange(1.5, 2.5);
        assertEquals(1.5, reducedDataSetY.getMinValue());
        assertEquals(2.5, reducedDataSetY.getMaxValue());
    }

    @Test
    public void testIntegralOptions() {
        LOGGER.atDebug().log("testIntegralOptions");
        DoubleDataSet3D testData = new DoubleDataSet3D("test", //
                new double[] { 1, 2, 3 }, // x-array
                new double[] { 6, 7, 8 }, // y-array
                new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } });

        DimReductionDataSet sliceDataSetX = new DimReductionDataSet(testData, DIM_X, Option.INTEGRAL);
        DimReductionDataSet sliceDataSetY = new DimReductionDataSet(testData, DIM_Y, Option.INTEGRAL);

        assertEquals(testData, sliceDataSetX.getSourceDataSet(), "equal source dataSet");
        assertEquals(testData, sliceDataSetY.getSourceDataSet(), "equal source dataSet");
        assertEquals(Option.INTEGRAL, sliceDataSetY.getReductionOption(), "reduction option");

        sliceDataSetY.setMinValue(0);
        assertEquals(0, sliceDataSetY.getMinIndex());
        sliceDataSetY.setMinValue(2);
        assertEquals(1, sliceDataSetY.getMinIndex());
        sliceDataSetY.setMinValue(0);
        assertEquals(0, sliceDataSetY.getMinIndex());

        sliceDataSetY.setMaxValue(2);
        assertEquals(1, sliceDataSetY.getMaxIndex());
        sliceDataSetY.setMaxValue(3);
        assertEquals(2, sliceDataSetY.getMaxIndex());

        sliceDataSetX.setMinValue(5);
        assertEquals(0, sliceDataSetX.getMinIndex());
        sliceDataSetX.setMinValue(6);
        assertEquals(0, sliceDataSetX.getMinIndex());
        sliceDataSetX.setMinValue(7);
        assertEquals(1, sliceDataSetX.getMinIndex());
        sliceDataSetX.setMinValue(0);

        sliceDataSetX.setMaxValue(7);
        assertEquals(1, sliceDataSetX.getMaxIndex());
        sliceDataSetX.setMaxValue(8);
        assertEquals(2, sliceDataSetX.getMaxIndex());

        // integral over full array
        final double[] integralX = new double[3];
        final double[] integralY = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                integralX[j] += testData.getZ(j, i);
                integralY[i] += testData.getZ(j, i);
            }
        }
        sliceDataSetY.setMinValue(0);
        sliceDataSetY.setMaxValue(4);
        sliceDataSetX.setMinValue(0);
        sliceDataSetX.setMaxValue(9);

        assertArrayEquals(integralX, sliceDataSetX.getValues(DIM_Y), "x-integral");
        assertArrayEquals(integralY, sliceDataSetY.getValues(DIM_Y), "y-integral");

        LOGGER.atDebug().log("testIntegralOptions - done");
    }

    @Test
    public void testMaxOptions() {
        LOGGER.atDebug().log("testMaxOptions");
        DoubleDataSet3D testData = new DoubleDataSet3D("test", //
                new double[] { 1, 2, 3 }, // x-array
                new double[] { 6, 7, 8 }, // y-array
                new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } });

        DimReductionDataSet sliceDataSetX = new DimReductionDataSet(testData, DIM_X, Option.MAX);
        DimReductionDataSet sliceDataSetY = new DimReductionDataSet(testData, DIM_Y, Option.MAX);

        assertEquals(testData, sliceDataSetX.getSourceDataSet(), "equal source dataSet");
        assertEquals(testData, sliceDataSetY.getSourceDataSet(), "equal source dataSet");
        assertEquals(Option.MAX, sliceDataSetX.getReductionOption(), "reduction option");

        // max over full array
        final double[] maxY = new double[3];
        final double[] maxX = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                maxX[j] = Math.max(testData.getZ(j, i), maxX[j]);
                maxY[i] = Math.max(testData.getZ(j, i), maxY[i]);
            }
        }

        sliceDataSetX.setMinValue(0);
        sliceDataSetX.setMaxValue(10);
        sliceDataSetY.setMinValue(0);
        sliceDataSetY.setMaxValue(4);

        assertArrayEquals(maxX, sliceDataSetX.getValues(DIM_Y), "x-max");
        assertArrayEquals(maxY, sliceDataSetY.getValues(DIM_Y), "y-max");

        LOGGER.atDebug().log("testMaxOptions - done");
    }

    @Test
    public void testMeanOptions() {
        LOGGER.atDebug().log("testMeanOptions");
        DoubleDataSet3D testData = new DoubleDataSet3D("test", //
                new double[] { 1, 2, 3 }, // x-array
                new double[] { 6, 7, 8 }, // y-array
                new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } });

        DimReductionDataSet sliceDataSetX = new DimReductionDataSet(testData, DIM_X, Option.MEAN);
        DimReductionDataSet sliceDataSetY = new DimReductionDataSet(testData, DIM_Y, Option.MEAN);

        assertEquals(testData, sliceDataSetX.getSourceDataSet(), "equal source dataSet");
        assertEquals(testData, sliceDataSetY.getSourceDataSet(), "equal source dataSet");
        assertEquals(Option.MEAN, sliceDataSetX.getReductionOption(), "reduction option");

        // mean over full array
        final double[] meanX = new double[3];
        final double[] meanY = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                meanX[j] += testData.getZ(j, i) / 3.0;
                meanY[i] += testData.getZ(j, i) / 3.0;
            }
        }

        sliceDataSetX.setMinValue(0);
        sliceDataSetX.setMaxValue(10);
        sliceDataSetY.setMinValue(0);
        sliceDataSetY.setMaxValue(4);

        for (int i = 0; i < 3; i++) {
            assertTrue(MathUtils.nearlyEqual(meanX[i], sliceDataSetX.getValues(DIM_Y)[i]), "x-integral");
            assertTrue(MathUtils.nearlyEqual(meanY[i], sliceDataSetY.getValues(DIM_Y)[i]), "y-integral");
        }

        LOGGER.atDebug().log("testMeanOptions - done");
    }

    @Test
    public void testMinOptions() {
        LOGGER.atDebug().log("testMinOptions");
        DoubleDataSet3D testData = new DoubleDataSet3D("test", //
                new double[] { 1, 2, 3 }, // x-array
                new double[] { 6, 7, 8 }, // y-array
                new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } });

        DimReductionDataSet sliceDataSetX = new DimReductionDataSet(testData, DIM_X, Option.MIN);
        DimReductionDataSet sliceDataSetY = new DimReductionDataSet(testData, DIM_Y, Option.MIN);

        assertEquals(testData, sliceDataSetX.getSourceDataSet(), "equal source dataSet");
        assertEquals(testData, sliceDataSetY.getSourceDataSet(), "equal source dataSet");
        assertEquals(Option.MIN, sliceDataSetX.getReductionOption(), "reduction option");

        // min over full array
        final double[] minY = new double[] { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        final double[] minX = new double[] { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                minX[j] = Math.min(testData.getZ(j, i), minX[j]);
                minY[i] = Math.min(testData.getZ(j, i), minY[i]);
            }
        }

        sliceDataSetX.setMinValue(0);
        sliceDataSetX.setMaxValue(10);
        sliceDataSetY.setMinValue(0);
        sliceDataSetY.setMaxValue(4);

        assertArrayEquals(minX, sliceDataSetX.getValues(DIM_Y), "x-min");
        assertArrayEquals(minY, sliceDataSetY.getValues(DIM_Y), "y-min");

        LOGGER.atDebug().log("testMinOptions - done");
    }

    @Test
    public void testSliceOptions() {
        LOGGER.atDebug().log("testSliceOptions");
        DoubleDataSet3D testData = new DoubleDataSet3D("test", //
                new double[] { 1, 2, 3 }, // x-array
                new double[] { 6, 7, 8 }, // y-array
                new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } });

        DimReductionDataSet sliceDataSetX = new DimReductionDataSet(testData, DIM_X, Option.SLICE);
        DimReductionDataSet sliceDataSetY = new DimReductionDataSet(testData, DIM_Y, Option.SLICE);

        assertEquals(testData, sliceDataSetX.getSourceDataSet(), "equal source dataSet");
        assertEquals(testData, sliceDataSetY.getSourceDataSet(), "equal source dataSet");
        assertEquals(Option.SLICE, sliceDataSetX.getReductionOption(), "reduction option");

        nEvent = 0;
        sliceDataSetX.addListener(evt -> {
            nEvent++;
        });
        testData.invokeListener(new UpdateEvent(testData, "testX"), true);
        assertEquals(1, nEvent, "DataSet3D event propagated");

        assertArrayEquals(testData.getValues(DIM_X), sliceDataSetX.getValues(DIM_X));
        assertArrayEquals(testData.getValues(DIM_Y), sliceDataSetY.getValues(DIM_X));

        assertArrayEquals(testData.getZValues()[0], sliceDataSetX.getValues(DIM_Y), "first row match");
        assertArrayEquals(testData.getValues(DIM_Y), sliceDataSetY.getValues(DIM_X));

        sliceDataSetX.setMinValue(7.0);
        assertEquals(2, nEvent, "DataSet3D event propagated");

        assertArrayEquals(testData.getZValues()[1], sliceDataSetX.getValues(DIM_Y), "second row match");
        assertArrayEquals(testData.getValues(DIM_Y), sliceDataSetY.getValues(DIM_X));

        assertArrayEquals(new double[] { 1, 6, 9 }, sliceDataSetY.getValues(DIM_Y), "first column match");
        sliceDataSetY.setMinValue(2.0);
        assertArrayEquals(new double[] { 2, 5, 8 }, sliceDataSetY.getValues(DIM_Y), "second column match");

        LOGGER.atDebug().log("testSliceOptions - done");
    }
}
