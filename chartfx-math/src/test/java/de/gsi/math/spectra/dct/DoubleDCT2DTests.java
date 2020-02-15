package de.gsi.math.spectra.dct;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.jtransforms.dct.DoubleDCT_2D;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author rstein
 */
public class DoubleDCT2DTests {
    public static final float FFT_NUMERIC_LIMITS = 1e-3f;

    @ParameterizedTest
    @CsvSource({ "2,2", "2,4", "4,2", "4,4", "4,8", "8,4", "1024,1024", "1024,512", "512, 1024" })
    public void basicReal2dIdentityTests(final int nRows, final int nCols) {
        DoubleDCT_2D fft = new DoubleDCT_2D(nRows, nCols);

        final int nSamples = nRows * nCols;
        double[][] testSignal1Ref = generateDelta(nRows, nCols);
        double[][] testSignal1 = generateDelta(nRows, nCols);

        // basic identity tests
        fft.forward(testSignal1, true);
        fft.inverse(testSignal1, true);
        for (int i = 0; i < nRows; i++) {
            assertArrayEquals(testSignal1Ref[i], testSignal1[i], nSamples * FFT_NUMERIC_LIMITS, "delta identity " + i);
        }

    }

    @ParameterizedTest
    @CsvSource({ "2,2", "2,4", "4,2", "4,4", "4,8", "8,4", "1024,1024", "1024,512", "512, 1024" })
    public void basicRealIdentityTests(final int nRows, final int nCols) {
        DoubleDCT_2D fft = new DoubleDCT_2D(nRows, nCols);

        final int nSamples = nRows * nCols;
        double[] testSignal1Ref = generateDelta(nSamples);
        double[] testSignal1 = generateDelta(nSamples);
        double[] testSignal2Ref = generateRamp(nSamples, nSamples);
        double[] testSignal2 = generateRamp(nSamples, nSamples);

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
