package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.DefaultDataSet;
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
    static private DataSet emptyDataSet;

    @BeforeAll
    public static void setup() {
        triangle = new TriangleFunction("Triangle Function", N_SAMPLES, 0);
        emptyDataSet = new DefaultDataSet("EmptyDataSet");
    }

    @Test
    public void getCenterOfMassTest() {
        // DataSet is symmetrical, so CoM should be in the center
        assertEquals(N_SAMPLES / 2, SimpleDataSetEstimators.computeCentreOfMass(triangle), 1);
        // first half of dataset is rectangular triangle CoM at 0.5*2/3
        assertEquals(N_SAMPLES / 3, SimpleDataSetEstimators.computeCentreOfMass(triangle, 0, N_SAMPLES / 2), 1);
        // same, but do not use indices but x-values
        assertEquals(N_SAMPLES / 3, SimpleDataSetEstimators.computeCentreOfMass(triangle, 0.0, N_SAMPLES / 2.0), 1);
        // Assert with empty DataSet, expect NaN
        assertEquals(Double.NaN, SimpleDataSetEstimators.computeCentreOfMass(emptyDataSet));
    }

    @Test
    public void getFullWidthHalfMaximumTest() {
        // Test with empty DataSet
        assertEquals(Double.NaN, SimpleDataSetEstimators.getFullWidthHalfMaximum(emptyDataSet, 0, 0, false));
        // test without interpolation. Delta can be up to 2.0
        assertEquals(N_SAMPLES / 2.0, SimpleDataSetEstimators.getFullWidthHalfMaximum(triangle, 0, N_SAMPLES, false),
                2.01);
        // test with interpolation
        assertEquals(N_SAMPLES / 2.0, SimpleDataSetEstimators.getFullWidthHalfMaximum(triangle, 0, N_SAMPLES, true),
                0.001);
    }

    @Test
    public void getIntegralTest() {
        assertEquals(0.5 * N_SAMPLES, SimpleDataSetEstimators.getIntegral(triangle, 0, N_SAMPLES), 2.0);
    }

    @Test
    public void getLocationMaximumTest() {
        assertEquals(0.5 * N_SAMPLES, SimpleDataSetEstimators.getLocationMaximum(triangle, 0, N_SAMPLES), 2.01);
    }

    @Test
    public void getMaximumTest() {
        assertEquals(1.0, SimpleDataSetEstimators.getMaximum(triangle, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getMeanTest() {
        assertEquals(0.5, SimpleDataSetEstimators.getMean(triangle, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getMean(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getMedianTest() {
        assertEquals(0.5, SimpleDataSetEstimators.getMedian(triangle, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getMedian(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getMinimumTest() {
        assertEquals(0.0, SimpleDataSetEstimators.getMinimum(triangle, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getRangeTest() {
        assertEquals(1.0, SimpleDataSetEstimators.getRange(triangle, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRange(emptyDataSet, 0, emptyDataSet.getDataCount()));
        // DataSet with infinite range
        assertEquals(Double.POSITIVE_INFINITY,
                SimpleDataSetEstimators.getRange(new DataSetBuilder()
                        .setYValues(
                                new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, 0, 4 })
                        .build(), 0, 5));
        assertEquals(Double.POSITIVE_INFINITY, SimpleDataSetEstimators.getRange(
                new DataSetBuilder().setYValues(new double[] { Double.NEGATIVE_INFINITY, 5, Double.NaN, 0, 4 }).build(),
                0, 5));
        assertEquals(Double.POSITIVE_INFINITY, SimpleDataSetEstimators.getRange(
                new DataSetBuilder().setYValues(new double[] { Double.POSITIVE_INFINITY, 5, Double.NaN, 0, 4 }).build(),
                0, 5));
        // DataSet only containing NaNs
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRange(
                new DataSetBuilder().setYValues(new double[] { Double.NaN, Double.NaN, Double.NaN }).build(), 0, 3));
    }

    @Test
    public void getRmsTest() {
        // unbiased RMS -> signal amplitude = 0.5
        assertEquals(0.5 / Math.sqrt(3), SimpleDataSetEstimators.getRms(triangle, 0, N_SAMPLES), 1e-2);
        assertEquals(Double.NaN, SimpleDataSetEstimators.getRms(emptyDataSet, 0, emptyDataSet.getDataCount()));
    }

    @Test
    public void getSimpleRiseTimeTest() {
        assertEquals(0.6 * 0.5 * N_SAMPLES, SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1),
                2.01);
        assertEquals(0.8 * 0.5 * N_SAMPLES, SimpleDataSetEstimators.getSimpleRiseTime1090(triangle, 0, N_SAMPLES - 1),
                2.01);
        assertEquals(0.9 * 0.5 * N_SAMPLES,
                SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.05, 0.95), 2.01);
        // inverted, evaluates falling time
        assertEquals(0.9 * 0.5 * N_SAMPLES,
                SimpleDataSetEstimators.getSimpleRiseTime(triangle, N_SAMPLES / 2, N_SAMPLES - 1, 0.05, 0.95), 2.01);

    }

    // TODO:
    @Test
    public void getTransmissionTest() {
        DataSet dataset = new DataSetBuilder().setYValues(new double[] { 9, 9, 6, 5, 2 }).build();
        assertEquals(2 / 9.0 * 100,
                SimpleDataSetEstimators.getTransmission(dataset, 0, dataset.getDataCount() - 1, true));
        assertEquals((2 - 9) / 9.0 * 100,
                SimpleDataSetEstimators.getTransmission(dataset, 0, dataset.getDataCount() - 1, false));
    }

    @Test
    public void interpolateGaussianTest() {
        GaussFunction dataSet = new GaussFunction("testGauss", N_SAMPLES);
        assertEquals(N_SAMPLES / 2,
                SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(dataSet, 0, N_SAMPLES - 1));
    }

    @Test
    public void getDutyCycleTest() {
        DataSet dataset = new DataSetBuilder().setYValues(new double[] { 0,0,0,1,1,0,0,0,1,1 }).build();
        assertEquals(0.4,
                SimpleDataSetEstimators.getDutyCycle(dataset, 0, dataset.getDataCount()));
        dataset = new DataSetBuilder().setYValues(new double[] { 0,0.4,0.5,0.6,0.7,1.0,0,0.3,0.5,Double.NaN }).build();
        assertEquals(0.5,
                SimpleDataSetEstimators.getDutyCycle(dataset, 0, dataset.getDataCount()));
    }

// Not tested because the function does not do what the comments say it does...
// comments talk about 20-80%, but it does something like first time signal
// is more than half way to maximum minus the start time
//    @Test
//    public void getEdgeDetectTest() {
//        DataSet dataset = new DataSetBuilder().setYValues(new double[] { 0,0,0,1,1,0,0,0,1,1 }).build();
//        assertEquals(0.4,
//                SimpleDataSetEstimators.getEdgeDetect(dataset, 0, dataset.getDataCount()));
//    }

    @Test
    public void getFrequencyEstimateTest() {
        SineFunction dataSet = new SineFunction("testSine", N_SAMPLES);
        assertEquals(10.0 / N_SAMPLES, SimpleDataSetEstimators.getFrequencyEstimate(dataSet, 0, N_SAMPLES - 1), 1e-3);
    }
}
