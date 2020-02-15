package de.gsi.math.spectra.fft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.jtransforms.fft.DoubleFFT_2D;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author rstein
 */
public class DoubleFFT2DTests {
    public static final float FFT_NUMERIC_LIMITS = 1e-3f;

    @ParameterizedTest
    @CsvSource({ "2, 2", "2, 4", "4, 2", "4, 4", "4, 8", "8, 4", "1024, 1024", "1024, 512", "512, 1024" })
    public void basicComplex2dIdentityTests(final int nRows, final int nCols) {
        DoubleFFT_2D fft = new DoubleFFT_2D(nRows, nCols);

        final int nSamples = 4 * nRows * nCols;
        double[][] testSignal1Ref = generateDelta(2 * nRows, 2 * nCols);
        double[][] testSignal1 = generateDelta(2 * nRows, 2 * nCols);

        // basic identity tests
        fft.complexForward(testSignal1);
        fft.complexInverse(testSignal1, true);
        for (int i = 0; i < nRows; i++) {
            assertArrayEquals(testSignal1Ref[i], testSignal1[i], nSamples * FFT_NUMERIC_LIMITS, "delta identity " + i);
        }

    }

    @ParameterizedTest
    @CsvSource({ "2,2", "2,4", "4,2", "4,4", "4,8", "8,4", "1024,1024", "1024,512", "512, 1024" })
    public void basicComplexIdentityTests(final int nRows, final int nCols) {
        DoubleFFT_2D fft = new DoubleFFT_2D(nRows, nCols);

        final int nSamples = 2 * nRows * nCols;
        double[] testSignal1Ref = generateDelta(nSamples);
        double[] testSignal1 = generateDelta(nSamples);
        double[] testSignal2Ref = generateRamp(nSamples, nSamples);
        double[] testSignal2 = generateRamp(nSamples, nSamples);

        // basic identity tests
        fft.complexForward(testSignal1);
        fft.complexInverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.complexForward(testSignal2);
        fft.complexInverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");

    }

    @ParameterizedTest
    @CsvSource({ "2,2", "2,4", "4,2", "4,4", "4,8", "8,4", "1024,1024", "1024,512", "512, 1024" })
    public void basicReal2dIdentityTests(final int nRows, final int nCols) {
        DoubleFFT_2D fft = new DoubleFFT_2D(nRows, nCols);

        final int nSamples = nRows * nCols;
        double[][] testSignal1Ref = generateDelta(nRows, nCols);
        double[][] testSignal1 = generateDelta(nRows, nCols);

        // basic identity tests
        fft.realForward(testSignal1);
        fft.realInverse(testSignal1, true);
        for (int i = 0; i < nRows; i++) {
            assertArrayEquals(testSignal1Ref[i], testSignal1[i], nSamples * FFT_NUMERIC_LIMITS, "delta identity " + i);
        }

    }

    @ParameterizedTest
    @CsvSource({ "2,2", "2,4", "4,2", "4,4", "4,8", "8,4", "1024,1024", "1024,512", "512, 1024" })
    public void basicRealIdentityTests(final int nRows, final int nCols) {
        DoubleFFT_2D fft = new DoubleFFT_2D(nRows, nCols);

        final int nSamples = nRows * nCols;
        double[] testSignal1Ref = generateDelta(nSamples);
        double[] testSignal1 = generateDelta(nSamples);
        double[] testSignal2Ref = generateRamp(nSamples, nSamples);
        double[] testSignal2 = generateRamp(nSamples, nSamples);

        // basic identity tests
        fft.realForward(testSignal1);
        fft.realInverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.realForward(testSignal2);
        fft.realInverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");

    }

    @ParameterizedTest
    @CsvSource({ "2, true", "2, false", "4, true", "4, false", "6, true", "6, false", "1024, true", "1024, false" })
    public void identityFullRealFFTTests(final int nSamples, final boolean forward) {
        DoubleFFT_2D fft = new DoubleFFT_2D(nSamples, nSamples);

        double[] testSignal1a = generateDelta(2 * nSamples * nSamples);
        double[] testSignal1b = generateDelta(2 * nSamples * nSamples);

        // basic identity tests
        if (forward) {
            fft.realForwardFull(testSignal1a);
            fft.complexInverse(testSignal1a, true);
        } else {
            fft.realInverseFull(testSignal1a, true);
            fft.complexInverse(testSignal1b, true);
        }
        assertArrayEquals(testSignal1a, testSignal1b, nSamples * FFT_NUMERIC_LIMITS,
                "full real vs complex forward delta");
    }

    @ParameterizedTest
    @CsvSource({ "2, true", "2, false", "4, true", "4, false", "6, true", "6, false", "1024, true", "1024, false" })
    public void identityFullRealFFTTests2(final int nSamples, final boolean forward) {
        DoubleFFT_2D fft = new DoubleFFT_2D(nSamples, nSamples);

        double[][] testSignal1a = generateDelta(nSamples, 2 * nSamples);
        double[][] testSignal1b = generateDelta(nSamples, 2 * nSamples);

        // basic identity tests
        if (forward) {
            fft.realForwardFull(testSignal1a);
            fft.complexInverse(testSignal1a, true);
        } else {
            fft.realInverseFull(testSignal1a, true);
            fft.complexInverse(testSignal1b, true);
        }
        for (int i = 0; i < nSamples; i++) {
            assertArrayEquals(testSignal1a[i], testSignal1b[i], nSamples * FFT_NUMERIC_LIMITS, "delta identity " + i);
        }
    }

    private static double[] generateDelta(final int nSamples) {
        final double[] retVal = new double[nSamples];
        retVal[0] = 1.0f;
        return retVal;
    }

    private static double[][] generateDelta(final int nRows, final int nCols) {
        final double[][] retVal = new double[nRows][];
        for (int i = 0; i < nRows; i++) {
            retVal[i] = new double[nCols];
        }
        retVal[0][0] = 1.0;
        return retVal;
    }

    private static double[] generateRamp(final int nSamples, final int nRamp) {
        final double[] retVal = new double[nSamples];
        for (int i = 0; i < nRamp; i++) {
            retVal[i] = i;
        }
        return retVal;
    }

}
