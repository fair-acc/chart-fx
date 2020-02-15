package de.gsi.math.spectra.dst;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jtransforms.dst.DoubleDST_1D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author rstein
 */
public class DoubleDST1DTests {
    public static final double FFT_NUMERIC_LIMITS = 1e-3f;

    @Test
    public void basicTests() {
        assertThrows(IllegalArgumentException.class, () -> new DoubleDST_1D(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 31, 32, 33, 34, 1023, 1024, 1025, 4095, 4096, 4097, (2 << 16) - 100, 2 << 16,
            (2 << 16) + 100 })
    public void identityDSTTests(final int nSamples) {
        DoubleDST_1D fft = new DoubleDST_1D(nSamples);
        double[] testSignal1Ref = generateDelta(2 * nSamples);
        double[] testSignal1 = generateDelta(2 * nSamples);
        double[] testSignal2Ref = generateRamp(2 * nSamples, nSamples);
        double[] testSignal2 = generateRamp(2 * nSamples, nSamples);

        // basic identity tests
        fft.forward(testSignal1, true);
        fft.inverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.forward(testSignal2, true);
        fft.inverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");
    }

    private static double[] generateDelta(final int nSamples) {
        final double[] retVal = new double[nSamples];
        retVal[0] = 1.0f;
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
