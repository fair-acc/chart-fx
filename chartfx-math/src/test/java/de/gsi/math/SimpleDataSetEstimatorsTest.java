package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.testdata.spi.TriangleFunction;

/**
 * Test SimpleDataSetEstimators Utility class
 * 
 * @author Alexander Krimm
 */
class SimpleDataSetEstimatorsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataSetEstimatorsTest.class);
    private static final int N_SAMPLES = 1024;

    static DataSet triangle;
    static DataSet emptyDataSet;

    @BeforeAll
    public static void setup() {
        triangle = new TriangleFunction("Triangle Function", N_SAMPLES, 0);
        emptyDataSet = new DefaultDataSet("EmptyDataSet");
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
    }

    @Test
    public void getMedianTest() {
        assertEquals(0.5, SimpleDataSetEstimators.getMedian(triangle, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getMinimumTest() {
        assertEquals(0.0, SimpleDataSetEstimators.getMinimum(triangle, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getRangeTest() {
        assertEquals(1.0, SimpleDataSetEstimators.getRange(triangle, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getRmsTest() {
        // unbiased RMS -> signal amplitude = 0.5
        assertEquals(0.5 / Math.sqrt(3), SimpleDataSetEstimators.getRms(triangle, 0, N_SAMPLES), 1e-2);
    }

    @Test
    public void getSimpleRiseTimeTest() {
        assertEquals(0.6 * 0.5 * N_SAMPLES, SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1),
                2.01);
        assertEquals(0.8 * 0.5 * N_SAMPLES, SimpleDataSetEstimators.getSimpleRiseTime1090(triangle, 0, N_SAMPLES - 1),
                2.01);
        assertEquals(0.9 * 0.5 * N_SAMPLES,
                SimpleDataSetEstimators.getSimpleRiseTime(triangle, 0, N_SAMPLES - 1, 0.05, 0.95), 2.01);
    }

    // TODO:
    // @Test
    // public void getTransmissionTest() {
    //     fail("Not yet implemented");
    // }
    //
    // @Test
    // public void interpolateGaussianTest() {
    //        fail("Not yet implemented");
    // }
    //
    // @Test
    // public void linearInterpolateTest() {
    //     fail("Not yet implemented");
    // }
    //
    //  @Test
    //  public void getDutyCycleTest() {
    //      fail("Not yet implemented");
    //  }
    //
    //  @Test
    //  public void getEdgeDetectTest() {
    //      fail("Not yet implemented");
    //  }
    //
    //  @Test
    //  public void getFrequencyEstimateTest() {
    //      fail("Not yet implemented");
    //  }
}
