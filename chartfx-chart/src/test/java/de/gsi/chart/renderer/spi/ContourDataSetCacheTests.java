package de.gsi.chart.renderer.spi;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.renderer.datareduction.ReductionType;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.dataset.spi.AbstractDataSet3D;
import de.gsi.dataset.spi.DataRange;
import de.gsi.math.ArrayUtils;
import de.gsi.math.TMath;

/**
 * @author rstein
 */
public class ContourDataSetCacheTests {
    private static final double[] TEST_DATA_X = { 1, 2, 3 };
    private static final double[] TEST_DATA_Y = { 1, 2, 3, 4 };
    private static final double[] TEST_DATA_Z = { //
            1, 2, 3, //
            4, 5, 6, //
            7, 8, 9, //
            10, 11, 12 };
    // test cases for inversion
    private static final double[] TEST_DATA_Z_X_INVERTED = { //
            3, 2, 1, //
            6, 5, 4, //
            9, 8, 7, //
            12, 11, 10 };
    private static final double[] TEST_DATA_Z_Y_INVERTED = { //
            10, 11, 12, //
            7, 8, 9, //
            4, 5, 6, //
            1, 2, 3 };
    private static final double[] TEST_DATA_Z_XY_INVERTED = { //
            12, 11, 10, //
            9, 8, 7, //
            6, 5, 4, //
            3, 2, 1 };
    private static final double[] TEST_DATA_RED_Y_MIN = { //
            1, 2, 3, //
            7, 8, 9 };
    private static final double[] TEST_DATA_RED_Y_MAX = { //
            4, 5, 6, //
            10, 11, 12 };
    private static final double[] TEST_DATA_RED_Y_AVG = { //
            2.5, 3.5, 4.5, //
            8.5, 9.5, 10.5 };
    private static final double[] TEST_DATA_Z_QUANT1 = { //
            0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
    private static final double[] TEST_DATA_Z_QUANT2 = { //
            0.9, 0.8, 0.7, 0.6, 0.5, 0.5, 0.4, 0.3, 0.2, 0.1, 0.0, 0.0 };

    @Test
    public void testDataSet() {
        TestDataSet dataSet = new TestDataSet();

        assertEquals(TEST_DATA_X.length, dataSet.getDataCount(DIM_X));
        assertEquals(TEST_DATA_Y.length, dataSet.getDataCount(DIM_Y));
        assertEquals(TEST_DATA_Z.length, dataSet.getDataCount(DIM_Z));

        for (int i = 0; i < dataSet.getDataCount(DIM_X); i++) {
            assertEquals(TEST_DATA_X[i], dataSet.get(DIM_X, i));
        }
        for (int i = 0; i < dataSet.getDataCount(DIM_Y); i++) {
            assertEquals(TEST_DATA_Y[i], dataSet.get(DIM_Y, i));
        }
        for (int i = 0; i < dataSet.getDataCount(DIM_Z); i++) {
            assertEquals(TEST_DATA_Z[i], dataSet.get(DIM_Z, i));
        }
        final int rowWidth = dataSet.getDataCount(DIM_X);
        for (int i = 0; i < dataSet.getDataCount(DIM_Z); i++) {
            assertEquals(dataSet.get(DIM_Z, i), dataSet.getZ(i % rowWidth, i / rowWidth));
        }
    }

    @Test
    public void testHelperFunctions() {
        assertEquals(0.1, ContourDataSetCache.quantize(0.12, 10));
        assertEquals(0.1, ContourDataSetCache.quantize(0.19, 10));
        assertEquals(0.2, ContourDataSetCache.quantize(0.20, 10));
        assertEquals(0.2, ContourDataSetCache.quantize(0.21, 10));

        assertEquals(2, ContourDataSetCache.roundDownEven(2.1));
        assertEquals(2, ContourDataSetCache.roundDownEven(2.5));
        assertEquals(2, ContourDataSetCache.roundDownEven(3.0));
        assertEquals(2, ContourDataSetCache.roundDownEven(3.9));
        assertEquals(4, ContourDataSetCache.roundDownEven(4));

        DataRange range = ContourDataSetCache.computeLocalRange(TEST_DATA_Z, TEST_DATA_X.length, TEST_DATA_Y.length,
                true);
        assertEquals(TMath.Minimum(TEST_DATA_Z), range.getMin());
        assertEquals(TMath.Maximum(TEST_DATA_Z), range.getMax());
        assertEquals(true, range.isDefined());

        range = ContourDataSetCache.computeLocalRange(TEST_DATA_Z, TEST_DATA_X.length, TEST_DATA_Y.length, false);
        assertEquals(false, range.isDefined());

        final AxisTransform identityTransform = new AxisTransform() {

            @Override
            public double backward(double val) {
                return val;
            }

            @Override
            public double forward(double val) {
                return val;
            }

            @Override
            public double getMaximumRange() {
                // not necessary for this test
                return 0;
            }

            @Override
            public double getMinimumRange() {
                // not necessary for this test
                return 0;
            }

            @Override
            public double getRoundedMaximumRange(double val) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public double getRoundedMinimumRange(double val) {
                // not necessary for this test
                return 0;
            }

            @Override
            public void setMaximumRange(double val) {
                // not necessary for this test
            }

            @Override
            public void setMinimumRange(double val) {
                // not necessary for this test
            }
        };
        final double[] inputData = Arrays.copyOf(TEST_DATA_Z, TEST_DATA_Z.length);
        ContourDataSetCache.quantizeData(inputData, TEST_DATA_X.length, TEST_DATA_Y.length, false, 0, 12,
                identityTransform, 10);
        assertArrayEquals(TEST_DATA_Z_QUANT1, inputData, "quantizeData(..)");

        final double[] inputDataInv = Arrays.copyOf(TEST_DATA_Z, TEST_DATA_Z.length);
        ContourDataSetCache.quantizeData(inputDataInv, TEST_DATA_X.length, TEST_DATA_Y.length, true, 0, 12,
                identityTransform, 10);
        assertArrayEquals(TEST_DATA_Z_QUANT2, inputDataInv, "quantizeData(..) - inverted");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testDataReduction(final boolean parallel) {
        TestDataSet dataSet = new TestDataSet();
        final int srcWidth = dataSet.getDataCount(DIM_X);
        final int srcHeigth = dataSet.getDataCount(DIM_Y);

        final double[] inputBuffer = Arrays.copyOf(TEST_DATA_Z, TEST_DATA_Z.length);
        assertEquals(TEST_DATA_Z.length, inputBuffer.length, "data buffer length");
        final double[] tempBuffer = new double[TEST_DATA_Z.length / 2];
        ArrayUtils.fillArray(tempBuffer, -1);

        ContourDataSetCache.reduceData(//
                inputBuffer, srcWidth, srcHeigth, tempBuffer, srcWidth, srcHeigth / 2, //
                2 /* reductionFactor */, ReductionType.MIN, parallel);
        assertArrayEquals(TEST_DATA_RED_Y_MIN, tempBuffer, "data reduction Y min");
        ArrayUtils.fillArray(tempBuffer, -1);

        ContourDataSetCache.reduceData(//
                inputBuffer, dataSet.getDataCount(DIM_X), dataSet.getDataCount(DIM_Y), //
                tempBuffer, dataSet.getDataCount(DIM_X), dataSet.getDataCount(DIM_Y) / 2, //
                2 /* reductionFactor */, ReductionType.MAX, //
                parallel);
        assertArrayEquals(TEST_DATA_RED_Y_MAX, tempBuffer, "data reduction Y max");
        // assertEquals(TEST_DATA_RED_Y_MAX, tempBuffer, "data reduction Y max");
        ArrayUtils.fillArray(tempBuffer, -1);

        ContourDataSetCache.reduceData(//
                inputBuffer, dataSet.getDataCount(DIM_X), dataSet.getDataCount(DIM_Y), //
                tempBuffer, dataSet.getDataCount(DIM_X), dataSet.getDataCount(DIM_Y) / 2, //
                2 /* reductionFactor */, ReductionType.AVERAGE, //
                parallel);
        assertArrayEquals(TEST_DATA_RED_Y_AVG, tempBuffer, "data reduction Y max");
    }

    @Test
    public void testDataTransform() {
        TestDataSet dataSet = new TestDataSet();

        assertEquals(TEST_DATA_X.length, dataSet.getDataCount(DIM_X), "data vector x length");
        assertEquals(TEST_DATA_Y.length, dataSet.getDataCount(DIM_Y), "data vector x length");
        assertEquals(TEST_DATA_Z.length, dataSet.getDataCount(DIM_Z), "data vector x length");

        final double[] dataBuffer = new double[dataSet.getDataCount(DIM_Z)];
        ArrayUtils.fillArray(dataBuffer, -1);
        assertEquals(TEST_DATA_Z.length, dataBuffer.length, "data buffer length");

        assertDoesNotThrow(() -> {
            ContourDataSetCache.computeCoordinates(dataSet, dataBuffer, dataBuffer.length, //
                    false, 0, 2, //
                    false, 0, 3, 0);
        });
        assertArrayEquals(TEST_DATA_Z, dataBuffer, "data buffer content - normal");
        ArrayUtils.fillArray(dataBuffer, -1);

        assertDoesNotThrow(() -> {
            ContourDataSetCache.computeCoordinates(dataSet, dataBuffer, dataBuffer.length, //
                    true, 0, 2, //
                    false, 0, 3, 0);
        });
        assertArrayEquals(TEST_DATA_Z_X_INVERTED, dataBuffer, "data buffer content - X inverted");
        ArrayUtils.fillArray(dataBuffer, -1);

        assertDoesNotThrow(() -> {
            ContourDataSetCache.computeCoordinates(dataSet, dataBuffer, dataBuffer.length, //
                    false, 0, 2, //
                    true, 0, 3, 0);
        });
        assertArrayEquals(TEST_DATA_Z_Y_INVERTED, dataBuffer, "data buffer content - X inverted");
        ArrayUtils.fillArray(dataBuffer, -1);

        assertDoesNotThrow(() -> {
            ContourDataSetCache.computeCoordinates(dataSet, dataBuffer, dataBuffer.length, //
                    true, 0, 2, //
                    true, 0, 3, 0);
        });
        assertArrayEquals(TEST_DATA_Z_XY_INVERTED, dataBuffer, "data buffer content - X inverted");

        ArrayUtils.fillArray(dataBuffer, -1);
        ContourDataSetCache.copySubFrame(dataSet, dataBuffer, false, false, 0, 2, false, 0, 3);
        assertArrayEquals(TEST_DATA_Z, dataBuffer, "data buffer content - normal copySubFrame");
        ArrayUtils.fillArray(dataBuffer, -1);

        ContourDataSetCache.copySubFrame(dataSet, dataBuffer, true, false, 0, 2, false, 0, 3);
        assertArrayEquals(TEST_DATA_Z, dataBuffer, "data buffer content - parallel copySubFrame");

        assertDoesNotThrow(() -> ContourDataSetCache.convertDataArrayToImage(TEST_DATA_Z, TEST_DATA_X.length,
                TEST_DATA_Y.length, ColorGradient.DEFAULT), "data to colour image conversion");
    }

    private class TestDataSet extends AbstractDataSet3D<TestDataSet> {
        private static final long serialVersionUID = 4176996086927034332L;

        public TestDataSet() {
            super(ContourDataSetCacheTests.class.getSimpleName() + "TestDataSet");
        }

        @Override
        public double get(int dimIndex, int index) {
            switch (dimIndex) {
            case DIM_X:
                return TEST_DATA_X[index];
            case DIM_Y:
                return TEST_DATA_Y[index];
            case DIM_Z:
            default:
                return TEST_DATA_Z[index];
            }
        }

        @Override
        public int getDataCount(int dimIndex) {
            switch (dimIndex) {
            case DIM_X:
                return TEST_DATA_X.length;
            case DIM_Y:
                return TEST_DATA_Y.length;
            case DIM_Z:
            default:
                return TEST_DATA_Z.length;
            }
        }

        @Override
        public double getZ(int xIndex, int yIndex) {
            return get(DIM_Z, yIndex * TEST_DATA_X.length + xIndex);
        }
    }
}
