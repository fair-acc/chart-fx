package io.fair_acc.math.spectra.fft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jtransforms.fft.FloatFFT_1D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.fair_acc.math.utils.ConcurrencyUtils;

/**
 * @author rstein
 */
public class FloatFFT1DTests {
    public static final float FFT_NUMERIC_LIMITS = 1e-3f;

    @Test
    public void basicTests() {
        assertThrows(IllegalArgumentException.class, () -> new FloatFFT_1D(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 1023, 1024, 1025, 4095, 4096, 4097, (2 << 16) - 100, 2 << 16,
                         (2 << 16) + 100 })
    public void
    identityComplexlFFTTests(final int nSamples) {
        FloatFFT_1D fft = new FloatFFT_1D(nSamples);
        float[] testSignal1Ref = generateDelta(2 * nSamples);
        float[] testSignal1 = generateDelta(2 * nSamples);
        float[] testSignal2Ref = generateRamp(2 * nSamples, nSamples);
        float[] testSignal2 = generateRamp(2 * nSamples, nSamples);

        // basic identity tests
        fft.complexForward(testSignal1);
        fft.complexInverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.complexForward(testSignal2);
        fft.complexInverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");
    }

    @ParameterizedTest
    @CsvSource({ "1, true", "1, false", "2, true", "2, false", "3, true", "3, false", "4, true", "4, false", "5, true",
            "5, false", "6, true", "6, false", "1023, true", "1023, false", "1024, true", "1024, false", "1025, true",
            "1025, false", "66000, true", "66000, false" })
    public void
    identityFullRealFFTTests(final int nSamples, final boolean forward) {
        FloatFFT_1D fft = new FloatFFT_1D(nSamples);

        float[] testSignal1a = generateDelta(2 * nSamples);
        float[] testSignal1b = new float[2 * nSamples];
        float[] testSignal2a = generateRamp(2 * nSamples, nSamples);
        float[] testSignal2b = new float[2 * nSamples];

        for (int i = 0; i < nSamples; i++) {
            testSignal1b[2 * i] = testSignal1a[i];
            testSignal2b[2 * i] = testSignal2a[i];
        }

        // basic identity tests
        if (forward) {
            fft.realForwardFull(testSignal1a);
            fft.complexForward(testSignal1b);
        } else {
            fft.realInverseFull(testSignal1a, true);
            fft.complexInverse(testSignal1b, true);
        }
        assertArrayEquals(testSignal1a, testSignal1b, nSamples * FFT_NUMERIC_LIMITS,
                "full real vs complex forward delta");

        if (forward) {
            fft.realForwardFull(testSignal2a);
            fft.complexForward(testSignal2b);
        } else {
            fft.realInverseFull(testSignal2a, true);
            fft.complexInverse(testSignal2b, true);
        }
        assertArrayEquals(testSignal2a, testSignal2b, (nSamples < 2000 ? 1.0f : 10f) * nSamples * FFT_NUMERIC_LIMITS,
                "full real vs complex forward ramp");
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 1023, 1024, 1025, 4095, 4096, 4097, (2 << 10) - 100, 2 << 10,
                         (2 << 10) + 100 })
    public void
    identityRealFFTTests(final int nSamples) {
        FloatFFT_1D fft = new FloatFFT_1D(nSamples);

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
    @ValueSource(ints = { 1, 2, 3, 4, 5, 1023, 1024, 1025, 4095, 4096, 4097, (2 << 10) - 100, 2 << 10,
                         (2 << 10) + 100 })
    public void
    threadFFTTests(final int nSamples) {
        FloatFFT_1D fft = new FloatFFT_1D(nSamples);

        float[] testSignal1Ref = generateDelta(nSamples);
        float[] testSignal1 = generateDelta(nSamples);
        float[] testSignal2Ref = generateDelta(nSamples);
        float[] testSignal2 = generateDelta(nSamples);

        // basic identity tests
        fft.realForward(testSignal1);
        fft.realInverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        ConcurrencyUtils.setForceThreads(true);
        ConcurrencyUtils.setNumberOfThreads(8);
        fft.realForward(testSignal2);
        fft.realInverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "delta identity");
        ConcurrencyUtils.setForceThreads(false);
    }

    private static float[] generateDelta(final int nSamples) {
        final float[] retVal = new float[nSamples];
        retVal[0] = 1.0f;
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
