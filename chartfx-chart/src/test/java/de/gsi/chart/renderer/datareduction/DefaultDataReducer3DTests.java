package de.gsi.chart.renderer.datareduction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.gsi.math.ArrayUtils;

/**
 * @author rstein
 */
public class DefaultDataReducer3DTests {
    private static final int N_DIM = 4;
    private static final double[] TEST_DATA = { //
        1, 2, 3, 4, //
        5, 6, 7, 8, //
        9, 10, 11, 12, //
        13, 14, 15, 16
    };
    private static final double[] TEST_DATA_RED_X_2_MIN = { 1, 3, 5, 7, 9, 11, 13, 15 };
    private static final double[] TEST_DATA_RED_Y_2_MIN = { 1, 2, 3, 4, 9, 10, 11, 12 };
    private static final double[] TEST_DATA_RED_XY_2_MIN = { 1, 3, 9, 11 };

    private static final double[] TEST_DATA_RED_X_2_MAX = { 2, 4, 6, 8, 10, 12, 14, 16 };
    private static final double[] TEST_DATA_RED_Y_2_MAX = { 5, 6, 7, 8, 13, 14, 15, 16 };
    private static final double[] TEST_DATA_RED_XY_2_MAX = { 6, 8, 14, 16 };

    private static final double[] TEST_DATA_RED_X_2_AVG = { 1.5, 3.5, 5.5, 7.5, 9.5, 11.5, 13.5, 15.5 };
    private static final double[] TEST_DATA_RED_Y_2_AVG = { 3, 4, 5, 6, 11, 12, 13, 14 };
    private static final double[] TEST_DATA_RED_XY_2_AVG = { 3.5, 5.5, 11.5, 13.5 };

    private static final double[] TEST_DATA_RED_X_2_DOWN = { 1, 3, 5, 7, 9, 11, 13, 15 };
    private static final double[] TEST_DATA_RED_Y_2_DOWN = { 1, 2, 3, 4, 9, 10, 11, 12 };
    private static final double[] TEST_DATA_RED_XY_2_DOWN = { 1, 3, 9, 11 };

    @Test
    public void reductionByTwoTests() {
        final double[] inputBuffer = Arrays.copyOf(TEST_DATA, TEST_DATA.length);
        assertEquals(TEST_DATA.length, inputBuffer.length, "data buffer length");
        final double[] tempBuffer = new double[N_DIM * N_DIM];
        ArrayUtils.fillArray(tempBuffer, -1);
        final double[] tempBuffer2 = new double[N_DIM * N_DIM / 2];
        ArrayUtils.fillArray(tempBuffer2, -1);
        final double[] tempBuffer4 = new double[N_DIM * N_DIM / 4];
        ArrayUtils.fillArray(tempBuffer4, -1);

        // min tests
        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer2, N_DIM / 2, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MIN);
        assertArrayEquals(TEST_DATA_RED_X_2_MIN, tempBuffer2, "TEST_DATA_RED_X_2_MIN");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer2, N_DIM, N_DIM / 2, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MIN);
        assertArrayEquals(TEST_DATA_RED_Y_2_MIN, tempBuffer2, "TEST_DATA_RED_Y_2_MIN");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer4, N_DIM / 2, N_DIM / 2, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MIN);
        assertArrayEquals(TEST_DATA_RED_XY_2_MIN, tempBuffer4, "TEST_DATA_RED_XY_2_MIN");
        ArrayUtils.fillArray(tempBuffer4, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer, N_DIM, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MIN);
        assertArrayEquals(TEST_DATA, tempBuffer, "identity for MIN");
        ArrayUtils.fillArray(tempBuffer, -1);

        // max tests
        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer2, N_DIM / 2, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MAX);
        assertArrayEquals(TEST_DATA_RED_X_2_MAX, tempBuffer2, "TEST_DATA_RED_X_2_MAX");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer2, N_DIM, N_DIM / 2, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MAX);
        assertArrayEquals(TEST_DATA_RED_Y_2_MAX, tempBuffer2, "TEST_DATA_RED_Y_2_MAX");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer4, N_DIM / 2, N_DIM / 2, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MAX);
        assertArrayEquals(TEST_DATA_RED_XY_2_MAX, tempBuffer4, "TEST_DATA_RED_XY_2_MAX");
        ArrayUtils.fillArray(tempBuffer4, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer, N_DIM, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.MAX);
        assertArrayEquals(TEST_DATA, tempBuffer, "identity for MAX");
        ArrayUtils.fillArray(tempBuffer, -1);

        // average tests
        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer2, N_DIM / 2, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA_RED_X_2_AVG, tempBuffer2, "TEST_DATA_RED_X_2_AVG");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer2, N_DIM, N_DIM / 2, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA_RED_Y_2_AVG, tempBuffer2, "TEST_DATA_RED_Y_2_AVG");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer4, N_DIM / 2, N_DIM / 2, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA_RED_XY_2_AVG, tempBuffer4, "TEST_DATA_RED_XY_2_AVG");
        ArrayUtils.fillArray(tempBuffer4, -1);

        DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer, N_DIM, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA, tempBuffer, "identity for AVERAGE");
        ArrayUtils.fillArray(tempBuffer, -1);

        // test assertions
        assertThrows(IllegalArgumentException.class, () -> {
            DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer, 1, N_DIM, inputBuffer, N_DIM, N_DIM, 0, 3,
                    ReductionType.AVERAGE);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            DefaultDataReducer3D.scaleDownByFactorTwo(tempBuffer, N_DIM, 1, inputBuffer, N_DIM, N_DIM, 0, 3,
                    ReductionType.AVERAGE);
        });
    }

    @Test
    public void reductionTests() {
        final double[] inputBuffer = Arrays.copyOf(TEST_DATA, TEST_DATA.length);
        assertEquals(TEST_DATA.length, inputBuffer.length, "data buffer length");
        final double[] tempBuffer = new double[N_DIM * N_DIM];
        ArrayUtils.fillArray(tempBuffer, -1);
        final double[] tempBuffer2 = new double[N_DIM * N_DIM / 2];
        ArrayUtils.fillArray(tempBuffer2, -1);
        final double[] tempBuffer4 = new double[N_DIM * N_DIM / 4];
        ArrayUtils.fillArray(tempBuffer4, -1);

        // min tests
        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM / 2, N_DIM, ReductionType.MIN);
        assertArrayEquals(TEST_DATA_RED_X_2_MIN, tempBuffer2, "TEST_DATA_RED_X_2_MIN");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM, N_DIM / 2, ReductionType.MIN);
        assertArrayEquals(TEST_DATA_RED_Y_2_MIN, tempBuffer2, "TEST_DATA_RED_Y_2_MIN");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer4, N_DIM / 2, N_DIM / 2,
                ReductionType.MIN);
        assertArrayEquals(TEST_DATA_RED_XY_2_MIN, tempBuffer4, "TEST_DATA_RED_XY_2_MIN");
        ArrayUtils.fillArray(tempBuffer4, -1);

        // max tests
        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM / 2, N_DIM, ReductionType.MAX);
        assertArrayEquals(TEST_DATA_RED_X_2_MAX, tempBuffer2, "TEST_DATA_RED_X_2_MAX");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM, N_DIM / 2, ReductionType.MAX);
        assertArrayEquals(TEST_DATA_RED_Y_2_MAX, tempBuffer2, "TEST_DATA_RED_Y_2_MAX");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer4, N_DIM / 2, N_DIM / 2,
                ReductionType.MAX);
        assertArrayEquals(TEST_DATA_RED_XY_2_MAX, tempBuffer4, "TEST_DATA_RED_XY_2_MAX");
        ArrayUtils.fillArray(tempBuffer4, -1);

        // average tests
        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM / 2, N_DIM,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA_RED_X_2_AVG, tempBuffer2, "TEST_DATA_RED_X_2_AVG");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM, N_DIM / 2,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA_RED_Y_2_AVG, tempBuffer2, "TEST_DATA_RED_Y_2_AVG");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer4, N_DIM / 2, N_DIM / 2,
                ReductionType.AVERAGE);
        assertArrayEquals(TEST_DATA_RED_XY_2_AVG, tempBuffer4, "TEST_DATA_RED_XY_2_AVG");
        ArrayUtils.fillArray(tempBuffer4, -1);

        // average tests
        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM / 2, N_DIM,
                ReductionType.DOWN_SAMPLE);
        assertArrayEquals(TEST_DATA_RED_X_2_DOWN, tempBuffer2, "TEST_DATA_RED_X_2_DOWN");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer2, N_DIM, N_DIM / 2,
                ReductionType.DOWN_SAMPLE);
        assertArrayEquals(TEST_DATA_RED_Y_2_DOWN, tempBuffer2, "TEST_DATA_RED_Y_2_DOWN");
        ArrayUtils.fillArray(tempBuffer2, -1);

        DefaultDataReducer3D.resample(inputBuffer, N_DIM, N_DIM, tempBuffer4, N_DIM / 2, N_DIM / 2,
                ReductionType.DOWN_SAMPLE);
        assertArrayEquals(TEST_DATA_RED_XY_2_DOWN, tempBuffer4, "TEST_DATA_RED_XY_2_DOWN");
        ArrayUtils.fillArray(tempBuffer4, -1);
    }
}
