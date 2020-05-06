package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.utils.MathUtils;

/**
 * Tests for the {@link de.gsi.math.MultiDimDataSetMath} -- Reduces 3D data to
 * 2D DataSet either via slicing, min, mean, max or integration
 *
 * @author rstein
 */
public class MultiDimDatasetMathTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiDimDatasetMathTests.class);

    @Test
    public void testIntegralOptions() {
        LOGGER.atDebug().log("testIntegralOptions");
        DataSet testData = new DataSetBuilder("test") //
                .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) // x-array
                .setValuesNoCopy(DIM_Y, new double[] { 6, 7, 8 }) // y-array
                .setValues(DIM_Z, new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } }) //
                .build();
        DoubleErrorDataSet sliceDataSetX = new DoubleErrorDataSet("test_X");
        DoubleErrorDataSet sliceDataSetY = new DoubleErrorDataSet("test_Y");
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeIntegral(null, sliceDataSetX, DIM_X, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeIntegral(testData, null, DIM_X, 0.0, 4.0));

        // integral over full array
        final double[] integralX = new double[3];
        final double[] integralY = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                integralX[j] += testData.get(DIM_Z, j + i * 3);
                integralY[i] += testData.get(DIM_Z, j + i * 3);
            }
        }

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                assertTrue(testData.getDataCount(DIM_X) != sliceDataSetX.getDataCount(DIM_X));
                assertTrue(testData.getDataCount(DIM_Y) != sliceDataSetY.getDataCount(DIM_X));
            } else {
                // N.B. during the second iteration the sliceDataSets should have the same
                // dimension as the source
                assertEquals(testData.getDataCount(DIM_X), sliceDataSetX.getDataCount(DIM_X));
                assertEquals(testData.getDataCount(DIM_Y), sliceDataSetY.getDataCount(DIM_X));
            }
            MultiDimDataSetMath.computeIntegral(testData, sliceDataSetX, DIM_X, 0, 9);
            MultiDimDataSetMath.computeIntegral(testData, sliceDataSetY, DIM_Y, 0, 4);
            assertArrayEquals(integralX, sliceDataSetX.getValues(DIM_Y), "x-integral");
            assertArrayEquals(integralY, sliceDataSetY.getValues(DIM_Y), "y-integral");
        }

        LOGGER.atDebug().log("testIntegralOptions - done");
    }

    @Test
    public void testMaxOptions() {
        LOGGER.atDebug().log("testMaxOptions");
        DataSet testData = new DataSetBuilder("test") //
                .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) // x-array
                .setValuesNoCopy(DIM_Y, new double[] { 6, 7, 8 }) // y-array
                .setValues(DIM_Z, new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } }) //
                .build();

        DoubleErrorDataSet sliceDataSetX = new DoubleErrorDataSet("test_X");
        DoubleErrorDataSet sliceDataSetY = new DoubleErrorDataSet("test_Y");
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeMax(null, sliceDataSetX, DIM_X, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeMax(testData, null, DIM_X, 0.0, 4.0));

        // max over full array
        final double[] maxY = new double[3];
        final double[] maxX = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                maxX[j] = Math.max(testData.get(DIM_Z, j + i * 3), maxX[j]);
                maxY[i] = Math.max(testData.get(DIM_Z, j + i * 3), maxY[i]);
            }
        }

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                assertTrue(testData.getDataCount(DIM_X) != sliceDataSetX.getDataCount(DIM_X));
                assertTrue(testData.getDataCount(DIM_Y) != sliceDataSetY.getDataCount(DIM_X));
            } else {
                // N.B. during the second iteration the sliceDataSets should have the same
                // dimension as the source
                assertEquals(testData.getDataCount(DIM_X), sliceDataSetX.getDataCount(DIM_X));
                assertEquals(testData.getDataCount(DIM_Y), sliceDataSetY.getDataCount(DIM_X));
            }
            MultiDimDataSetMath.computeMax(testData, sliceDataSetX, DIM_X, 0, 10);
            MultiDimDataSetMath.computeMax(testData, sliceDataSetY, DIM_Y, 0, 4);
            assertArrayEquals(maxX, sliceDataSetX.getValues(DIM_Y), "x-max");
            assertArrayEquals(maxY, sliceDataSetY.getValues(DIM_Y), "y-max");
        }

        LOGGER.atDebug().log("testMaxOptions - done");
    }

    @Test
    public void testMeanOptions() {
        LOGGER.atDebug().log("testMeanOptions");
        DataSet testData = new DataSetBuilder("test") //
                .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) // x-array
                .setValuesNoCopy(DIM_Y, new double[] { 6, 7, 8 }) // y-array
                .setValues(DIM_Z, new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } }) //
                .build();

        DoubleErrorDataSet sliceDataSetX = new DoubleErrorDataSet("test_X");
        DoubleErrorDataSet sliceDataSetY = new DoubleErrorDataSet("test_Y");
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeMean(null, sliceDataSetX, DIM_X, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeMean(testData, null, DIM_X, 0.0, 4.0));

        // mean over full array
        final double[] meanX = new double[3];
        final double[] meanY = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                meanX[j] += testData.get(DIM_Z, j + i * 3) / 3.0;
                meanY[i] += testData.get(DIM_Z, j + i * 3) / 3.0;
            }
        }

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                assertTrue(testData.getDataCount(DIM_X) != sliceDataSetX.getDataCount(DIM_X));
                assertTrue(testData.getDataCount(DIM_Y) != sliceDataSetY.getDataCount(DIM_X));
            } else {
                // N.B. during the second iteration the sliceDataSets should have the same
                // dimension as the source
                assertEquals(testData.getDataCount(DIM_X), sliceDataSetX.getDataCount(DIM_X));
                assertEquals(testData.getDataCount(DIM_Y), sliceDataSetY.getDataCount(DIM_X));
            }
            MultiDimDataSetMath.computeMean(testData, sliceDataSetX, DIM_X, 0, 10);
            MultiDimDataSetMath.computeMean(testData, sliceDataSetY, DIM_Y, 0, 4);
            for (int j = 0; j < 3; j++) {
                assertTrue(MathUtils.nearlyEqual(meanX[j], sliceDataSetX.getValues(DIM_Y)[j]), "x-integral");
                assertTrue(MathUtils.nearlyEqual(meanY[j], sliceDataSetY.getValues(DIM_Y)[j]), "y-integral");
            }
        }

        LOGGER.atDebug().log("testMeanOptions - done");
    }

    @Test
    public void testMinOptions() {
        LOGGER.atDebug().log("testMinOptions");
        DataSet testData = new DataSetBuilder("test") //
                .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) // x-array
                .setValuesNoCopy(DIM_Y, new double[] { 6, 7, 8 }) // y-array
                .setValues(DIM_Z, new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } }) //
                .build();

        DoubleErrorDataSet sliceDataSetX = new DoubleErrorDataSet("test_X");
        DoubleErrorDataSet sliceDataSetY = new DoubleErrorDataSet("test_Y");
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeMin(null, sliceDataSetX, DIM_X, 0.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeMin(testData, null, DIM_X, 0.0, 4.0));

        // min over full array
        final double[] minY = new double[] { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        final double[] minX = new double[] { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                minX[j] = Math.min(testData.get(DIM_Z, j + i * 3), minX[j]);
                minY[i] = Math.min(testData.get(DIM_Z, j + i * 3), minY[i]);
            }
        }

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                assertTrue(testData.getDataCount(DIM_X) != sliceDataSetX.getDataCount(DIM_X));
                assertTrue(testData.getDataCount(DIM_Y) != sliceDataSetY.getDataCount(DIM_X));
            } else {
                // N.B. during the second iteration the sliceDataSets should have the same
                // dimension as the source
                assertEquals(testData.getDataCount(DIM_X), sliceDataSetX.getDataCount(DIM_X));
                assertEquals(testData.getDataCount(DIM_Y), sliceDataSetY.getDataCount(DIM_X));
            }
            MultiDimDataSetMath.computeMin(testData, sliceDataSetX, DIM_X, 0, 10);
            MultiDimDataSetMath.computeMin(testData, sliceDataSetY, DIM_Y, 0, 4);
            assertArrayEquals(minX, sliceDataSetX.getValues(DIM_Y), "x-min");
            assertArrayEquals(minY, sliceDataSetY.getValues(DIM_Y), "y-min");
        }

        LOGGER.atDebug().log("testMinOptions - done");
    }

    @Test
    public void testSliceOptions() {
        LOGGER.atDebug().log("testSliceOptions");
        DataSet testData = new DataSetBuilder("test") //
                .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) // x-array
                .setValuesNoCopy(DIM_Y, new double[] { 6, 7, 8 }) // y-array
                .setValues(DIM_Z, new double[][] { // z-array
                        new double[] { 1, 2, 3 }, //
                        new double[] { 6, 5, 4 }, //
                        new double[] { 9, 8, 7 } }) //
                .build();

        DoubleErrorDataSet sliceDataSetX = new DoubleErrorDataSet("test_X");
        DoubleErrorDataSet sliceDataSetY = new DoubleErrorDataSet("test_Y");
        MultiDimDataSetMath.computeSlice(testData, sliceDataSetX, DIM_X, 0.0);
        MultiDimDataSetMath.computeSlice(testData, sliceDataSetY, DIM_Y, 0.0);

        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeSlice(null, sliceDataSetX, DIM_X, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> MultiDimDataSetMath.computeSlice(testData, null, DIM_X, 0.0));

        assertArrayEquals(testData.getValues(DIM_X), sliceDataSetX.getValues(DIM_X));
        assertArrayEquals(testData.getValues(DIM_Y), sliceDataSetY.getValues(DIM_X));

        assertArrayEquals(Arrays.copyOf(testData.getValues(DIM_Z), 3), sliceDataSetX.getValues(DIM_Y), "first row match");
        assertArrayEquals(testData.getValues(DIM_Y), sliceDataSetY.getValues(DIM_X));

        MultiDimDataSetMath.computeSlice(testData, sliceDataSetX, DIM_X, 7.0);

        assertArrayEquals(Arrays.copyOfRange(testData.getValues(DIM_Z), 3, 6), sliceDataSetX.getValues(DIM_Y), "second row match");
        assertArrayEquals(testData.getValues(DIM_Y), sliceDataSetY.getValues(DIM_X));

        assertArrayEquals(new double[] { 1, 6, 9 }, sliceDataSetY.getValues(DIM_Y), "first column match");
        MultiDimDataSetMath.computeSlice(testData, sliceDataSetY, DIM_Y, 2.0);
        assertArrayEquals(new double[] { 2, 5, 8 }, sliceDataSetY.getValues(DIM_Y), "second column match");

        LOGGER.atDebug().log("testSliceOptions - done");
    }
}
