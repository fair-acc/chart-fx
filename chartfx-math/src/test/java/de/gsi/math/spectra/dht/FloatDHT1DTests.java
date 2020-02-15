package de.gsi.math.spectra.dht;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jtransforms.dht.FloatDHT_1D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author rstein
 */
public class FloatDHT1DTests {
    public static final float FFT_NUMERIC_LIMITS = 1e-3f;

    @Test
    public void basicTests() {
        assertThrows(IllegalArgumentException.class, () -> new FloatDHT_1D(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 31, 32, 33, 34, 1023, 1024, 1025, 4095, 4096, 4097, (2 << 16) - 100, 2 << 16,
            (2 << 16) + 100 })
    public void identityDHTTests(final int nSamples) {
        FloatDHT_1D fft = new FloatDHT_1D(nSamples);
        float[] testSignal1Ref = generateDelta(2 * nSamples);
        float[] testSignal1 = generateDelta(2 * nSamples);
        float[] testSignal2Ref = generateRamp(2 * nSamples, nSamples);
        float[] testSignal2 = generateRamp(2 * nSamples, nSamples);

        // basic identity tests
        fft.forward(testSignal1);
        fft.inverse(testSignal1, true);
        assertArrayEquals(testSignal1Ref, testSignal1, nSamples * FFT_NUMERIC_LIMITS, "delta identity");

        fft.forward(testSignal2);
        fft.inverse(testSignal2, true);
        assertArrayEquals(testSignal2Ref, testSignal2, nSamples * FFT_NUMERIC_LIMITS, "ramp identity");
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
