package de.gsi.math.spectra.fft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 *
 * @author rstein
 */
public class FloatFFT3DTests {
    public static final float FFT_NUMERIC_LIMITS = 1e-3f;

    @ParameterizedTest
    @CsvSource({ "2, 2", "2, 4", "4, 2", "4, 4", "4, 8", "8, 4", "32, 32", "32, 64", "64, 32" })
    public void basicComplex2dIdentityTests(final int nRows, final int nCols) {
        FloatFFT_3D fft = new FloatFFT_3D(nRows, nCols, nCols);

        final int nSamples = 4 * nRows * nCols;
        float[][][] testSignal1Ref = generateDelta(nRows, nCols, 2 * nCols);
        float[][][] testSignal1 = generateDelta(nRows, nCols, 2 * nCols);

        // basic identity tests
        fft.complexForward(testSignal1);
        fft.complexInverse(testSignal1, true);
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                assertArrayEquals(testSignal1Ref[i][j], testSignal1[i][j], nSamples * FFT_NUMERIC_LIMITS,
                        "delta identity " + i + " x " + j);
            }
        }

    }

    @ParameterizedTest
    @CsvSource({ "2, 2", "2, 4", "4, 2", "4, 4", "4, 8", "8, 4", "32, 32", "32, 64", "64, 32" })
    public void basicComplexIdentityTests(final int nRows, final int nCols) {
        FloatFFT_3D fft = new FloatFFT_3D(nRows, nCols, nCols);

        final int nSamples = 2 * nRows * nCols * nCols;
        float[] testSignal1Ref = generateDelta(nSamples);
        float[] testSignal1 = generateDelta(nSamples);
        float[] testSignal2Ref = generateRamp(nSamples, nSamples);
        float[] testSignal2 = generateRamp(nSamples, nSamples);

        // basic identity tests
        fft.complexForward(testSignal1);
        fft.complexInverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.complexForward(testSignal2);
        fft.complexInverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");

    }

    @ParameterizedTest
    @CsvSource({ "2, 2", "2, 4", "4, 2", "4, 4", "4, 8", "8, 4", "32, 32", "32, 64", "64, 32" })
    public void basicReal2dIdentityTests(final int nRows, final int nCols) {
        FloatFFT_3D fft = new FloatFFT_3D(nRows, nCols, nCols);

        final int nSamples = nRows * nCols * nCols;
        float[][][] testSignal1Ref = generateDelta(nRows, nCols, nCols);
        float[][][] testSignal1 = generateDelta(nRows, nCols, nCols);

        // basic identity tests
        fft.realForward(testSignal1);
        fft.realInverse(testSignal1, true);
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                assertArrayEquals(testSignal1Ref[i][j], testSignal1[i][j], nSamples * FFT_NUMERIC_LIMITS,
                        "delta identity " + i + " x " + j);
            }
        }

    }

    @ParameterizedTest
    @CsvSource({ "2, 2", "2, 4", "4, 2", "4, 4", "4, 8", "8, 4", "32, 32", "32, 64", "64, 32" })
    public void basicRealIdentityTests(final int nRows, final int nCols) {
        FloatFFT_3D fft = new FloatFFT_3D(nRows, nCols, nCols);

        final int nSamples = nRows * nCols * nCols;
        float[] testSignal1Ref = generateDelta(nSamples);
        float[] testSignal1 = generateDelta(nSamples);
        float[] testSignal2Ref = generateRamp(nSamples, nSamples);
        float[] testSignal2 = generateRamp(nSamples, nSamples);

        // basic identity tests
        fft.realForward(testSignal1);
        fft.realInverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.realForward(testSignal2);
        fft.realInverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");

    }

    @ParameterizedTest
    @CsvSource({ "2, true", "2, false", "4, true", "4, false", "6, true", "6, false" })
    public void identityFullRealFFTTests(final int nSamples, final boolean forward) {
        FloatFFT_3D fft = new FloatFFT_3D(nSamples, nSamples, nSamples);

        float[] testSignal1a = generateDelta(2 * nSamples * nSamples * nSamples);
        float[] testSignal1b = generateDelta(2 * nSamples * nSamples * nSamples);

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
    @CsvSource({ "2, true", "2, false", "4, true", "4, false", "6, true", "6, false" })
    public void identityFullRealFFTTests2(final int nSamples, final boolean forward) {
        FloatFFT_3D fft = new FloatFFT_3D(nSamples, nSamples, nSamples);

        float[][][] testSignal1a = generateDelta(nSamples, nSamples, 2 * nSamples);
        float[][][] testSignal1b = generateDelta(nSamples, nSamples, 2 * nSamples);

        // basic identity tests
        if (forward) {
            fft.realForwardFull(testSignal1a);
            fft.complexInverse(testSignal1a, true);
        } else {
            fft.realInverseFull(testSignal1a, true);
            fft.complexInverse(testSignal1b, true);
        }
        for (int i = 0; i < nSamples; i++) {
            for (int j = 0; j < nSamples; j++) {
                assertArrayEquals(testSignal1a[i][j], testSignal1a[i][j], nSamples * FFT_NUMERIC_LIMITS,
                        "delta identity " + i + " x " + j);
            }
        }
    }

    private static float[] generateDelta(final int nSamples) {
        final float[] retVal = new float[nSamples];
        retVal[0] = 1.0f;
        return retVal;
    }

    private static float[][][] generateDelta(final int nRows, final int nCols, final int nCols2) {
        final float[][][] retVal = new float[nRows][nCols][nCols2];
        retVal[0][0][0] = 1.0f;
        return retVal;
    }

    private static float[] generateRamp(final int nSamples, final int nRamp) {
        final float[] retVal = new float[nSamples];
        for (int i = 0; i < nRamp; i++) {
            retVal[i] = i;
        }
        return retVal;
    }

}
