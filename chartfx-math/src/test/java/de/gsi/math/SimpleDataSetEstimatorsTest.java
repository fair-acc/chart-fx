package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.SineFunction;
import de.gsi.dataset.testdata.spi.TriangleFunction;

/**
 * Test SimpleDataSetEstimators Utility class
 * 
 * @author Alexander Krimm
 */
class SimpleDataSetEstimatorsTest {
    private static final int N_SAMPLES = 1024;

    static private DataSet triangle;
    static private DataSet triangleWithNaN;
    static private DataSet emptyDataSet;
    static private GaussFunction testGauss;

    @Test
    public void computeFWHM() {
        // zero size input array
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeFWHM(new double[] {}, 0, 0));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] {}, 0, 0));
        // regular case
        assertEquals(4, SimpleDataSetEstimators.computeFWHM(new double[] { 1, 2, 3, 2, 1 }, 5, 2));
        assertEquals(3, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] { 1, 2, 3, 2, 1 }, 5, 2));
        // index to close at array bounds
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeFWHM(new double[] { 1, 2, 3, 2 }, 4, 2));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeFWHM(new double[] { 1, 2, 3, 2, 1 }, 5, 0));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeFWHM(new double[] { 1, 2, 3, 2, 1 }, 5, 5));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] { 1, 2, 3, 2 }, 4, 2));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] { 1, 2, 3, 2, 1 }, 5, 0));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] { 1, 2, 3, 2, 1 }, 5, 5));
        // FWHM not inside data range
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeFWHM(new double[] { 1, 2, 3, 4, 3 }, 5, 3));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeFWHM(new double[] { 3, 4, 3, 2, 1 }, 5, 1));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] { 1, 2, 3, 4, 3 }, 5, 3));
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeInterpolatedFWHM(new double[] { 3, 4, 3, 2, 1 }, 5, 1));
    }

    @Test
    public void getCenterOfMassTest() {
        // DataSet is symmetrical, so CoM should be in the center
        assertEquals(N_SAMPLES / 2.0, SimpleDataSetEstimators.computeCentreOfMass(triangle), 1);
        // check if NaN is ignored
        assertEquals(N_SAMPLES / 2.0, SimpleDataSetEstimators.computeCentreOfMass(triangleWithNaN), 1);
        // first half of dataset is rectangular triangle CoM at 0.5*2/3
        assertEquals(N_SAMPLES / 3.0, SimpleDataSetEstimators.computeCentreOfMass(triangle, 0, N_SAMPLES / 2), 1);
        // same, but do not use indices but x-values
        assertEquals(N_SAMPLES / 3.0, SimpleDataSetEstimators.computeCentreOfMass(triangle, 0.0, N_SAMPLES / 2.0), 1);
        // Assert with empty DataSet, expect NaN
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeCentreOfMass(emptyDataSet));
    }

    @Test
    public void getDistanceTest() {
        assertEquals(N_SAMPLES, SimpleDataSetEstimators.getDistance(triangle, 0, N_SAMPLES, true));
        assertEquals(triangle.getValue(DIM_Y, N_SAMPLES / 2),
                SimpleDataSetEstimators.getDistance(triangle, 0, N_SAMPLES / 2, false));
    }

    @Test
    public void getDutyCycleTest() {
        // simple case
        DataSet dataset = new DataSetBuilder().setValues(DIM_Y, new double[] { 0, 0, 0, 1, 1, 0, 0, 0, 1, 1 }).build();
        assertEquals(0.4, SimpleDataSetEstimators.getDutyCycle(dataset, 0, dataset.getDataCount()));
        //test hysteresis
        dataset = new DataSetBuilder().setValues(DIM_Y, new double[] { 0, 0.4, 0.5, 0.6, 0.7, 1.0, 0, 0.3, 0.5, Double.NaN }).build();
        assertEquals(3.0 / (4.0 + 3.0), SimpleDataSetEstimators.getDutyCycle(dataset, 0, dataset.getDataCount()));
        // empty dataSet
        assertEquals(Double.NaN, SimpleDataSetEstimators.getDutyCycle(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getEdgeDetectTest() {
        final DoubleDataSet dataset = (DoubleDataSet) new DataSetBuilder() //
                                              .setValues(DIM_X, new double[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 12 }) //
                                              .setValues(DIM_Y, new double[] { 2, 0, Double.NEGATIVE_INFINITY, 3, 4.9, 5.1, 4, 6, 9, 10 }) //
                                              .build();
        assertEquals(5.0, SimpleDataSetEstimators.getEdgeDetect(dataset, 0, dataset.getDataCount()));
        final DoubleDataSet datasetConstant = (DoubleDataSet) new DataSetBuilder() //
                                                      .setValues(DIM_X, new double[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }) //
                                                      .setValues(DIM_Y, new double[] { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 }) //
                                                      .build();
        assertEquals(Double.NaN,
                SimpleDataSetEstimators.getEdgeDetect(datasetConstant, 0, datasetConstant.getDataCount()));

        assertEquals(Double.NaN, SimpleDataSetEstimators.getEdgeDetect(emptyDataSet, 0, 0));
        final DataSet datasetFalling = new DataSetBuilder() //
                                               .setValues(DIM_X, new double[] { 2, 3, 4, 5, 6, 7, 8 }) //
                                               .setValues(DIM_Y, new double[] { 9, 10, Double.NaN, 4, 5, 3, 1 }) //
                                               .build();
        assertEquals(3.0, SimpleDataSetEstimators.getEdgeDetect(datasetFalling, 0, datasetFalling.getDataCount()));
    }

    @Test
    public void getFrequencyEstimateTest() {
        // simple sine wave
        final SineFunction dataSet = new SineFunction("testSine", N_SAMPLES);
        assertEquals(10.0 / N_SAMPLES, SimpleDataSetEstimators.getFrequencyEstimate(dataSet, 0, N_SAMPLES - 1), 1e-3);
        // sine wave with NaN Values
        final DoubleDataSet dataSetNaN = new DoubleDataSet(dataSet);
        dataSetNaN.set(N_SAMPLES / 4, N_SAMPLES / 4, Double.NaN);
        assertEquals(10.0 / N_SAMPLES, SimpleDataSetEstimators.getFrequencyEstimate(dataSetNaN, 0, N_SAMPLES - 1),
                1e-3);
        // empty dataSet
        assertEquals(Double.NaN,
                SimpleDataSetEstimators.getFrequencyEstimate(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getFullWidthHalfMaximumTest() {
        // Test with empty DataSet
        assertEquals(Double.NaN, SimpleDataSetEstimators.getFullWidthHalfMaximum(emptyDataSet, 0, 0, false));
        // test without interpolation. Delta can be up to 2.0
        assertEquals(N_SAMPLES / 2.0, SimpleDataSetEstimators.getFullWidthHalfMaximum(triangle, 0, N_SAMPLES, false),
                2.01);
        // corner cases, maxium at the edge
        assertEquals(Double.NaN,
                SimpleDataSetEstimators.getFullWidthHalfMaximum(triangle, N_SAMPLES / 2, N_SAMPLES, false));
        assertEquals(Double.NaN, SimpleDataSetEstimators.getFullWidthHalfMaximum(triangle, 0, N_SAMPLES / 2, false));
        // test with data containing NaN
        assertEquals(N_SAMPLES / 2.0,
                SimpleDataSetEstimators.getFullWidthHalfMaximum(triangleWithNaN, 0, N_SAMPLES, false), 2.01);
        // test with interpolation
        assertEquals(N_SAMPLES / 2.0, SimpleDataSetEstimators.getFullWidthHalfMaximum(triangle, 0, N_SAMPLES, true),
                0.001);
    }

    @Test
    public void getIntegralTest() {
        // integral on triangle
        assertEquals(0.5 * N_SAMPLES, SimpleDataSetEstimators.getIntegral(triangle, 0, N_SAMPLES), 2.0);
        // reverse direction to change sign
        assertEquals(-0.5 * N_SAMPLES, SimpleDataSetEstimators.getIntegral(triangle, N_SAMPLES, 0), 2.0);
        // integrate over zero samples should be 0.0
        assertEquals(0.0 * N_SAMPLES, SimpleDataSetEstimators.getIntegral(triangle, N_SAMPLES / 4, N_SAMPLES / 4));
        // integrate over empty data set should be 0.0
        assertEquals(0.0, SimpleDataSetEstimators.getIntegral(emptyDataSet, 0, emptyDataSet.getDataCount()));
        // dataset with discontinuities and jumps
        final DataSet dataset = new DataSetBuilder() //
                                        .setValues(DIM_X, new double[] { 0, 1, 2, 2, 3, 4, 5, Double.NaN })
                                        .setValues(DIM_Y, new double[] { 1, 1, 0, 1, 1, Double.NaN, 1, 2 })
                                        .build();
        assertEquals(2.5, SimpleDataSetEstimators.getIntegral(dataset, 0, dataset.getDataCount()));
    }

    @Test
    public void getLocationMaximumGaussInterpolatedTest() {
        assertEquals(N_SAMPLES / 2,
                SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(testGauss, 0, N_SAMPLES - 1));
        assertEquals(Double.NaN, SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(emptyDataSet, 0, 0));
        assertEquals(Double.NaN, SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(
                                         new DataSetBuilder().setValues(DIM_Y, new double[] { 1, 2, 3, 4, 5 }).build(), 0, 4));
        assertEquals(Double.NaN, SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(
                                         new DataSetBuilder().setValues(DIM_Y, new double[] { 5, 4, 3, 2, 1 }).build(), 0, 4));
    }

    @Test
    public void getLocationMaximumTest() {
        assertEquals(0.5 * N_SAMPLES, SimpleDataSetEstimators.getLocationMaximum(triangleWithNaN, 0, N_SAMPLES), 2.01);
    }

    @Test
    public void getMaximumTest() {
        assertEquals(1.0, SimpleDataSetEstimators.getMaximum(triangleWithNaN, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getMeanTest() {
        assertEquals(0.5, SimpleDataSetEstimators.getMean(triangleWithNaN, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getMean(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getMedianTest() {
        assertEquals(0.5, SimpleDataSetEstimators.getMedian(triangleWithNaN, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getMedian(emptyDataSet, 0, emptyDataSet.getDataCount()));
        // test odd number of elements
        assertEquals(2, SimpleDataSetEstimators
                                .getMedian(new DataSetBuilder().setValues(DIM_Y, new double[] { 1.5, 1.5, 5, 2, 6, 7, -4 }).build(), 0, 7));
    }

    @Test
    public void getMinimumTest() {
        assertEquals(0.0, SimpleDataSetEstimators.getMinimum(triangleWithNaN, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getRangeTest() {
        assertEquals(1.0, SimpleDataSetEstimators.getRange(triangleWithNaN, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRange(emptyDataSet, 0, emptyDataSet.getDataCount()));
        // DataSet with infinite range
        assertEquals(Double.POSITIVE_INFINITY,
                SimpleDataSetEstimators.getRange(new DataSetBuilder()
                                                         .setValues(DIM_Y,
                                                                 new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, 0, 4 })
                                                         .build(),
                        0, 5));
        assertEquals(Double.POSITIVE_INFINITY, SimpleDataSetEstimators.getRange(
                                                       new DataSetBuilder().setValues(DIM_Y, new double[] { Double.NEGATIVE_INFINITY, 5, Double.NaN, 0, 4 }).build(),
                                                       0, 5));
        assertEquals(Double.POSITIVE_INFINITY, SimpleDataSetEstimators.getRange(
                                                       new DataSetBuilder().setValues(DIM_Y, new double[] { Double.POSITIVE_INFINITY, 5, Double.NaN, 0, 4 }).build(),
                                                       0, 5));
        // DataSet only containing NaNs
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRange(
                                         new DataSetBuilder().setValues(DIM_Y, new double[] { Double.NaN, Double.NaN, Double.NaN }).build(), 0, 3));
    }

    @Test
    public void getRmsTest() {
        // unbiased RMS -> signal amplitude = 0.5
        assertEquals(0.5 / Math.sqrt(3), SimpleDataSetEstimators.getRms(triangle, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRms(triangleWithNaN, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRms(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getSimpleRiseTimeTest() {
        assertEquals(0.6 * 0.5 * N_SAMPLES,
                SimpleDataSetEstimators.getSimpleRiseTime(triangleWithNaN, 0, N_SAMPLES - 1), 2.01);
        assertEquals(0.8 * 0.5 * N_SAMPLES, SimpleDataSetEstimators.getSimpleRiseTime1090(triangle, 0, N_SAMPLES - 1),
                2.01);
        assertEquals(0.9 * 0.5 * N_SAMPLES,
                SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.05, 0.95), 2.01);
        // inverted, evaluates falling time
        assertEquals(0.9 * 0.5 * N_SAMPLES,
                SimpleDataSetEstimators.getSimpleRiseTime(triangleWithNaN, N_SAMPLES / 2, N_SAMPLES - 1, 0.05, 0.95),
                2.01);
        // Error Cases
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, Double.NaN, 0.9));
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, -0.1, 0.9));
        assertThrows(IllegalArgumentException.class, () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.1, 1.01));
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.9, 0.8));
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 1.1, 0.8));
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.1, -0.1));
    }

    @Test
    public void getTransmissionTest() {
        DataSet dataset = new DataSetBuilder().setValues(DIM_Y, new double[] { 9, 9, 6, 5, 2 }).build();
        assertEquals(2 / 9.0 * 100,
                SimpleDataSetEstimators.getTransmission(dataset, 0, dataset.getDataCount() - 1, true));
        assertEquals((2 - 9) / 9.0 * 100,
                SimpleDataSetEstimators.getTransmission(dataset, 0, dataset.getDataCount() - 1, false));
    }

    // Tests for helper functions

    @Test
    public void getZeroCrossingTest() {
        assertEquals(Double.NaN, SimpleDataSetEstimators.getZeroCrossing(emptyDataSet, 0.5));
        assertEquals(N_SAMPLES / 4, SimpleDataSetEstimators.getZeroCrossing(triangleWithNaN, 0.5), 1.0);
        assertEquals(0.0, SimpleDataSetEstimators.getZeroCrossing(triangleWithNaN, 0.0));
        assertEquals(Double.NaN, SimpleDataSetEstimators.getZeroCrossing(triangleWithNaN, -0.5), 1.0);
        assertEquals(N_SAMPLES / 4, SimpleDataSetEstimators.getZeroCrossing(DataSetMath.multiplyFunction(triangleWithNaN, -1), -0.5), 1.0);
    }

    @Test
    public void interpolateGaussianTest() {
        final double[] array = new double[] { 5, 7, 8, 7.3, 6 };
        assertEquals(2.09321, SimpleDataSetEstimators.interpolateGaussian(array, array.length, 2), 1e-3);
        assertEquals(0, SimpleDataSetEstimators.interpolateGaussian(array, array.length, 0), 1e-3);
        assertEquals(4, SimpleDataSetEstimators.interpolateGaussian(array, array.length, 4), 1e-3);
        assertEquals(-5, SimpleDataSetEstimators.interpolateGaussian(array, array.length, -5), 1e-3);
    }

    @Test
    public void rootMeanSquareTest() {
        final double[] array = triangle.getValues(DIM_Y);
        assertEquals(0.5 / Math.sqrt(3), SimpleDataSetEstimators.rootMeanSquare(array, array.length), 1e-3);
        assertEquals(Double.NaN, SimpleDataSetEstimators.rootMeanSquare(new double[] {}, 0));
        assertEquals(Double.NaN, SimpleDataSetEstimators.rootMeanSquare(array, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> SimpleDataSetEstimators.rootMeanSquare(array, array.length + 3));
    }

    @Test
    public void sortTest() {
        final double[] toSort = new double[] { 3, 5, 1, Double.POSITIVE_INFINITY, -5, 7, 1.33 };
        final double[] sorted = new double[] { -5, 1, 1.33, 3, 5, 7, Double.POSITIVE_INFINITY };
        final double[] sortedFirstFive = new double[] { -5, 1, 3, 5, Double.POSITIVE_INFINITY };
        final double[] sortedReverse = new double[] { Double.POSITIVE_INFINITY, 7, 5, 3, 1.33, 1, -5 };
        assertArrayEquals(sorted, SimpleDataSetEstimators.sort(toSort, toSort.length, false));
        assertArrayEquals(sortedReverse, SimpleDataSetEstimators.sort(toSort, toSort.length, true));
        assertArrayEquals(new double[] {}, SimpleDataSetEstimators.sort(null, 0, false));
        assertArrayEquals(new double[] {}, SimpleDataSetEstimators.sort(toSort, 0, false));
        assertArrayEquals(new double[] {}, SimpleDataSetEstimators.sort(new double[] {}, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> SimpleDataSetEstimators.sort(toSort, toSort.length + 3, false));
        assertArrayEquals(sortedFirstFive, SimpleDataSetEstimators.sort(toSort, 5, false));
    }

    @BeforeAll
    public static void setUp() {
        triangle = new TriangleFunction("Triangle Function", N_SAMPLES, 0);
        triangleWithNaN = new DoubleDataSet(triangle);
        ((DoubleDataSet) triangleWithNaN).set(N_SAMPLES / 4, N_SAMPLES / 4, Double.NaN);
        ((DoubleDataSet) triangleWithNaN).set(N_SAMPLES / 4 * 3, Double.NaN, Double.NaN);
        emptyDataSet = new DefaultDataSet("EmptyDataSet");
        testGauss = new GaussFunction("testGauss", N_SAMPLES);
    }
}
