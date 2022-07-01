package io.fair_acc.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for TRandom
 *
 * @author Alexander Krimm
 */
public class TRandomTests {
    private static final long SEED = -12346;
    private static final long WRAPPING_SEED = 2088216195;
    private static final int N_SAMPLES = 1_000_000;

    @Test
    public void testUtils() {
        TRandom rnd = new TRandom(0);
        rnd.SetSeed(47);
        assertEquals("TRandom(47)", rnd.toString());
    }

    @Test
    public void testRndm() {
        TRandom rnd = new TRandom(WRAPPING_SEED);
        // uniform on ]0,1]
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Rndm();
            assertTrue(r > 0 && r <= 1);
        }
    }

    @Test
    public void testBinomial() {
        TRandom rnd = new TRandom(SEED);
        final int ntot = 10;
        final double prob = 0.5;
        double sum = 0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Binomial(ntot, prob);
            assertTrue(r >= 0 && r <= ntot); // test range
            sum += r;
        }
        assertEquals(ntot * prob, sum / N_SAMPLES, 1e-2); // test mean

        // Test special cases
        assertEquals(0.0, rnd.Binomial(ntot, -0.2));
        assertEquals(0.0, rnd.Binomial(ntot, 1.2));
    }

    @Test
    public void testBreitWigner() {
        TRandom rnd = new TRandom(SEED);
        final double mean = 5.0;
        final double gamma = 2.0;
        double sum = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.BreitWigner(mean, gamma);
            sum += r;
        }
        assertEquals(mean, sum / N_SAMPLES, 1); // test mean
    }

    @Test
    public void testCircle() {
        TRandom rnd = new TRandom(SEED);
        final double radius = 2.0;
        final double[] result = new double[2];
        for (int i = 0; i < N_SAMPLES; i++) {
            rnd.Circle(result, radius);
            assertEquals(radius, Math.sqrt(result[0] * result[0] + result[1] * result[1]), 1e-10);
        }
    }

    @Test
    public void testExp() {
        TRandom rnd = new TRandom(SEED);
        final double tau = 1.0;
        double sum = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Exp(tau);
            sum += r;
        }
        assertEquals(1.0 / tau, sum / N_SAMPLES, 1e-2); // test mean
    }

    @Test
    public void testGaus() {
        TRandom rnd = new TRandom(SEED);
        final double mean = 2.0;
        final double sigma = 0.5;
        double sum = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Gaus(mean, sigma);
            sum += r;
        }
        assertEquals(mean, sum / N_SAMPLES, 1e-3); // test mean
    }

    @Test
    public void testInteger() {
        TRandom rnd = new TRandom(SEED);
        final long imax = Long.MAX_VALUE - 1234378439L;
        for (int i = 0; i < N_SAMPLES; i++) {
            long r = rnd.Integer(imax);
            assertTrue(r > 0 && r < imax);
        }
    }

    @Test
    public void testLandau() {
        TRandom rnd = new TRandom(SEED);
        final double mpv = 2.0;
        final double sigma = 0.5;
        for (int i = 0; i < N_SAMPLES / 100; i++) {
            double r = rnd.Landau(mpv, sigma);
            assertTrue(r > 0);
        }
        // special cases
        assertEquals(0.0, rnd.Landau(mpv, -0.1));
    }

    @Test
    public void testPoisson() {
        TRandom rnd = new TRandom(SEED);
        // mean < 25
        final double mean = 2.0;
        double sum = 0.0;
        double sum2 = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Poisson(mean);
            double r2 = rnd.PoissonD(mean);
            sum += r;
            sum2 += r2;
        }
        assertEquals(mean, sum / N_SAMPLES, 1e-2); // test mean
        assertEquals(mean, sum2 / N_SAMPLES, 1e-2); // test mean
        // mean < 1e9
        final double mean2 = 101;
        sum = 0.0;
        sum2 = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Poisson(mean2);
            double r2 = rnd.PoissonD(mean2);
            sum += r;
            sum2 += r2;
        }
        assertEquals(mean2, sum / N_SAMPLES, 1); // test mean
        assertEquals(mean2, sum2 / N_SAMPLES, 1); // test mean
        // mean < 1e9
        final double mean3 = 1e10;
        sum = 0.0;
        sum2 = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            double r = rnd.Poisson(mean3);
            double r2 = rnd.PoissonD(mean3);
            sum += r;
            sum2 += r2;
        }
        assertEquals(mean3, sum / N_SAMPLES, 8e9); // test mean
        assertEquals(mean3, sum2 / N_SAMPLES, 8e9); // test mean

        // special cases
        assertEquals(0.0, rnd.Poisson(-1.0));
        assertEquals(0.0, rnd.PoissonD(-1.0));
    }

    @Test
    public void testRannor() {
        TRandom rnd = new TRandom(SEED);
        final double mean = 0.0;
        final double[] resultDouble = new double[2];
        final float[] resultFloat = new float[2];
        double sumi = 0.0;
        double sumr = 0.0;
        double sumi2 = 0.0;
        double sumr2 = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            rnd.Rannor(resultDouble);
            rnd.Rannor(resultFloat);
            sumi += resultDouble[0];
            sumr += resultDouble[1];
            sumi2 += resultFloat[0];
            sumr2 += resultFloat[1];
        }
        assertEquals(mean, sumi / N_SAMPLES, 1e-2);
        assertEquals(mean, sumr / N_SAMPLES, 1e-2);
        assertEquals(mean, sumi2 / N_SAMPLES, 1e-2);
        assertEquals(mean, sumr2 / N_SAMPLES, 1e-2);
    }

    @Test
    public void testRndmArray() {
        TRandom rnd = new TRandom(WRAPPING_SEED);
        final int n = 1_000;
        final float[] result = new float[n];
        rnd.RndmArray(n, result);
        for (int i = 0; i < n; i++) {
            double r = rnd.Rndm();
            assertTrue(r > 0 && r <= 1);
        }
    }

    @Test
    public void testSphere() {
        TRandom rnd = new TRandom(SEED);
        final double radius = 2.0;
        final double[] result = new double[3];
        for (int i = 0; i < N_SAMPLES; i++) {
            rnd.Sphere(result, radius);
            assertEquals(radius, Math.sqrt(result[0] * result[0] + result[1] * result[1] + result[2] * result[2]),
                    1e-10);
        }
    }

    @Test
    public void testUniform() {
        TRandom rnd = new TRandom(SEED);
        final double x1 = 1.337;
        final double x2 = 42.23;
        double sum = 0.0;
        double sum2 = 0.0;
        for (int i = 0; i < N_SAMPLES; i++) {
            final double r = rnd.Uniform(x1, x2);
            final double r2 = rnd.Uniform(x2);
            sum += r;
            sum2 += r2;
            assertTrue(r > x1 && r <= x2);
            assertTrue(r2 > 0 && r2 <= x2);
        }
        assertEquals(0.5 * (x1 + x2), sum / N_SAMPLES, 1e-2); // test mean
        assertEquals(0.5 * x2, sum2 / N_SAMPLES, 1e-1); // test mean
    }
}
