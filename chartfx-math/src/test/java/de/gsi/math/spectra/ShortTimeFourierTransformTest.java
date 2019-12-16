package de.gsi.math.spectra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.jtransforms.fft.DoubleFFT_1D;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.MultiDimDoubleDataSet;
import de.gsi.math.spectra.ShortTimeFourierTransform.Padding;

/**
 * Test the short-time Fourier transform.
 * For now just tests against trigonometric functions and verifies some points of the spectrum.
 * 
 * @author Alexander Krimm
 */
class ShortTimeFourierTransformTest {
    @Test
    public void testRealSTFT() {
        final int nFft = 128;
        final int step = 64;
        final int nSamples = (128 * 3) - 32;
        final double dt = 1e-3;
        final double fSine = 25.0;
        DoubleErrorDataSet sine = new DoubleErrorDataSet("sine", nSamples);
        for (int i = 0; i < nSamples; i++) {
            double t = i * dt;
            double y = i > nFft ? Math.sin(fSine * Math.PI * 2 * t) : 0.0;
            sine.set(i, t, y);
        }
        sine.getAxisDescription(DIM_X).set("Time", "s");
        sine.getAxisDescription(DIM_Y).set("Voltage", "V");

        final DataSet sineSpectrogram = ShortTimeFourierTransform.real(sine, null, nFft, step, Apodization.Hann,
                Padding.ZERO, false, true);
        // check dimensions
        assertEquals(6, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(nFft / 2, sineSpectrogram.getDataCount(DIM_Y));
        assertEquals(nFft / 2 * 6, sineSpectrogram.getDataCount(DIM_Z));

        // check metadata
        assertEquals("STFT(" + sine.getName() + ")", sineSpectrogram.getName());
        assertEquals(nFft, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("RealSTFT-nFFT")));
        assertEquals(step, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("RealSTFT-step")));

        // check axes
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_X).getMin());
        assertEquals(dt * nSamples, sineSpectrogram.getAxisDescription(DIM_X).getMax(), nFft);
        assertEquals(sine.getAxisDescription(DIM_X).getName(), sineSpectrogram.getAxisDescription(DIM_X).getName());
        assertEquals(sine.getAxisDescription(DIM_X).getUnit(), sineSpectrogram.getAxisDescription(DIM_X).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Y).getMin());
        assertEquals(0.5 / dt, sineSpectrogram.getAxisDescription(DIM_Y).getMax(), 10.0);
        assertEquals("Frequency", sineSpectrogram.getAxisDescription(DIM_Y).getName());
        assertEquals("Hz", sineSpectrogram.getAxisDescription(DIM_Y).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Z).getMin());
        // assertEquals(0.5, sineSpectrogram.getAxisDescription(DIM_Z).getMax(), 0.1);
        assertEquals("Magnitude", sineSpectrogram.getAxisDescription(DIM_Z).getName());
        assertEquals(sine.getAxisDescription(DIM_Y).getUnit(), sineSpectrogram.getAxisDescription(DIM_Z).getUnit());

        // check spectrogram
        assertArrayEquals(new double[] { 0, 1 * step * dt, 2 * step * dt, 3 * step * dt, 4 * step * dt, 5 * step * dt },
                sineSpectrogram.getValues(DIM_X), dt);
        // get spectrogram raw data
        final double[][] zValues = transpose(((DoubleDataSet3D) sineSpectrogram).getZValues());
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFft);
        // compare to transform t = 0
        final double[] vals1ref = Arrays.copyOfRange(sine.getYValues(), 0, nFft);
        Apodization.Hann.apodize(vals1ref);
        fastFourierTrafo.realForward(vals1ref);
        final double[] vals1refMag = SpectrumTools.computeMagnitudeSpectrum(vals1ref, true);
        assertArrayEquals(vals1refMag, zValues[0]);
        // compare to transform t = 2 * step * dt
        final double[] vals2ref = Arrays.copyOfRange(sine.getYValues(), 2 * step, 2 * step + nFft);
        Apodization.Hann.apodize(vals2ref);
        fastFourierTrafo.realForward(vals2ref);
        final double[] vals2refMag = SpectrumTools.computeMagnitudeSpectrum(vals2ref, true);
        assertArrayEquals(vals2refMag, zValues[2]);
        // compare to transform t = 5*step*dt
        final double[] vals3ref = new double[nFft];
        System.arraycopy(sine.getYValues(), 5 * step, vals3ref, 0, nFft - 32 - step);
        Apodization.Hann.apodize(vals3ref);
        fastFourierTrafo.realForward(vals3ref);
        final double[] vals3refMag = SpectrumTools.computeMagnitudeSpectrum(vals3ref, true);
        assertArrayEquals(vals3refMag, zValues[5]);

        // compare to transform t = 5*step*dt with ZOH
        ShortTimeFourierTransform.real(sine, sineSpectrogram, nFft, step, Apodization.Hamming, Padding.ZOH, false,
                true);
        final double[][] zValuesZOH = transpose(((DoubleDataSet3D) sineSpectrogram).getZValues());
        final double[] vals3refZOH = new double[nFft];
        System.arraycopy(sine.getYValues(), 5 * step, vals3refZOH, 0, nFft - 32 - step);
        Arrays.fill(vals3refZOH, nFft - 32 - step, nFft, vals3refZOH[nFft - 32 - step - 1]);
        Apodization.Hamming.apodize(vals3refZOH);
        fastFourierTrafo.realForward(vals3refZOH);
        final double[] vals3refMagZOH = SpectrumTools.computeMagnitudeSpectrum(vals3refZOH, true);
        assertArrayEquals(zValuesZOH[5], vals3refMagZOH);

        // compare to transform t = 5*step*dt with ZOH
        final DataSet sineSpectrogramMirror = ShortTimeFourierTransform.real(sine, null, nFft, step,
                Apodization.Rectangular, Padding.MIRROR, true, false);
        final double[][] zValuesMirror = transpose(((DoubleDataSet3D) sineSpectrogramMirror).getZValues());
        final double[] vals3refMirror = new double[nFft];
        System.arraycopy(sine.getYValues(), 5 * step, vals3refMirror, 0, nFft - 32 - step);
        for (int i = 0; i < vals3refMirror.length - nFft + 32 + step; i++) {
            vals3refMirror[nFft - 32 - step + i] = sine.getYValues()[5 * step + nFft - 32 - step - i - 1];
        }
        Apodization.Rectangular.apodize(vals2ref);
        fastFourierTrafo.realForward(vals3refMirror);
        final double[] vals3refMagMirror = SpectrumTools.computeMagnitudeSpectrum_dB(vals3refMirror, false);
        assertArrayEquals(zValuesMirror[5], vals3refMagMirror);

        // perform different sized Transforms
        ShortTimeFourierTransform.real(sine, sineSpectrogram, nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR,
                true, false);
        assertEquals(3, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(nFft / 2, sineSpectrogram.getDataCount(DIM_Y));

        ShortTimeFourierTransform.real(sine, sineSpectrogram, 2 * nFft, 2 * step, Apodization.Rectangular,
                Padding.MIRROR, true, false);
        assertEquals(3, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(nFft, sineSpectrogram.getDataCount(DIM_Y));

        // test empty input dataSet (inplace and non-inplace)
        ShortTimeFourierTransform.real(new DoubleDataSet("empty"), sineSpectrogram, 2 * nFft, 2 * step,
                Apodization.Rectangular, Padding.MIRROR, true, false);
        assertEquals(0, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(0, sineSpectrogram.getDataCount(DIM_Y));
        final DataSet emptySpectrogram = ShortTimeFourierTransform.real(new DoubleDataSet("empty"), null, 2 * nFft,
                2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertEquals(0, emptySpectrogram.getDataCount(DIM_X));
        assertEquals(0, emptySpectrogram.getDataCount(DIM_Y));

        // test unconforming dataSets
        assertThrows(IllegalArgumentException.class,
                () -> ShortTimeFourierTransform.real(
                        new DoubleDataSet3D("illegal data set", new double[] { 1, 2 }, new double[] { 1, 2, 3 },
                                new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } }),
                        null, 8, 8, Apodization.Hann, Padding.ZERO, true, false));
    }

    /**
     * Utility Function to transpose a 2D array
     * 
     * @param input a 2D array
     * @return the transposed array
     */
    private static double[][] transpose(final double[][] input) {
        if (input == null || input.length == 0) {
            return new double[0][0];
        }
        final double[][] output = new double[input[0].length][input.length];
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                output[j][i] = input[i][j];
            }
        }
        return output;
    }

    @Test
    public void testComplexSTFT() {
        final int nFreq = 128;
        final int step = 64;
        final int nSamples = (128 * 3) - 32;
        final double dt = 1e-3;
        final double fSine = 25.0;
        final MultiDimDoubleDataSet sine = new MultiDimDoubleDataSet("sine", 3, nSamples);
        for (int i = 0; i < nSamples; i++) {
            double t = i * dt;
            double y_re = i > nFreq ? Math.sin(fSine * Math.PI * 2 * t) : 0.0;
            double y_im = i > nFreq ? Math.cos(fSine * Math.PI * 2 * t) : 0.0;
            sine.set(i, t, y_re, y_im);
        }
        sine.getAxisDescription(DIM_X).set("Time", "s");
        sine.getAxisDescription(DIM_Y).set("Voltage", "V");

        final DataSet sineSpectrogram = ShortTimeFourierTransform.complex(sine, null, nFreq, step, Apodization.Hann,
                Padding.ZERO, false, true);
        // check dimensions
        assertEquals(6, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(nFreq, sineSpectrogram.getDataCount(DIM_Y));
        assertEquals(nFreq * 6, sineSpectrogram.getDataCount(DIM_Z));

        // check metadata
        assertEquals("STFT(" + sine.getName() + ")", sineSpectrogram.getName());
        assertEquals(nFreq,
                Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("ComplexSTFT-nFFT")));
        assertEquals(step, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("ComplexSTFT-step")));

        // check axes
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_X).getMin());
        assertEquals(dt * nSamples, sineSpectrogram.getAxisDescription(DIM_X).getMax(), nFreq);
        assertEquals(sine.getAxisDescription(DIM_X).getName(), sineSpectrogram.getAxisDescription(DIM_X).getName());
        assertEquals(sine.getAxisDescription(DIM_X).getUnit(), sineSpectrogram.getAxisDescription(DIM_X).getUnit());
        assertEquals(-0.5 / dt, sineSpectrogram.getAxisDescription(DIM_Y).getMin(), 10.0);
        assertEquals(0.5 / dt, sineSpectrogram.getAxisDescription(DIM_Y).getMax(), 10.0);
        assertEquals("Frequency", sineSpectrogram.getAxisDescription(DIM_Y).getName());
        assertEquals("Hz", sineSpectrogram.getAxisDescription(DIM_Y).getUnit());
        assertEquals("Magnitude", sineSpectrogram.getAxisDescription(DIM_Z).getName());
        assertEquals(sine.getAxisDescription(DIM_Y).getUnit(), sineSpectrogram.getAxisDescription(DIM_Z).getUnit());

        // check spectrogram
        assertArrayEquals(new double[] { 0, 1 * step * dt, 2 * step * dt, 3 * step * dt, 4 * step * dt, 5 * step * dt },
                sineSpectrogram.getValues(DIM_X), dt);
        // get spectrogram raw data
        final double[][] zValues = transpose(((DoubleDataSet3D) sineSpectrogram).getZValues());
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFreq);
        // compare to transform t = 0
        final double[] vals1ref = getStridedComplex(sine.getValues(DIM_Y), sine.getValues(DIM_Z), 0, nFreq);
        ShortTimeFourierTransform.apodizeComplex(vals1ref, Apodization.Hann);
        fastFourierTrafo.complexForward(vals1ref);
        final double[] vals1refMag = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals1ref, true));
        assertArrayEquals(vals1refMag, zValues[0]);
        // compare to transform t = 2 * step * dt
        final double[] vals2ref = getStridedComplex(sine.getValues(DIM_Y), sine.getValues(DIM_Z), 2 * step,
                2 * step + nFreq);
        ShortTimeFourierTransform.apodizeComplex(vals2ref, Apodization.Hann);
        fastFourierTrafo.complexForward(vals2ref);
        final double[] vals2refMag = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals2ref, true));
        assertArrayEquals(vals2refMag, zValues[2]);
        // compare to transform t = 5*step*dt
        final double[] vals3refstart = getStridedComplex(sine.getValues(DIM_Y), sine.getValues(DIM_Z), 5 * step,
                sine.getDataCount());
        final double[] vals3ref = new double[nFreq * 2];
        System.arraycopy(vals3refstart, 0, vals3ref, 0, vals3refstart.length);
        ShortTimeFourierTransform.apodizeComplex(vals3ref, Apodization.Hann);
        fastFourierTrafo.complexForward(vals3ref);
        final double[] vals3refMag = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals3ref, true));
        assertArrayEquals(vals3refMag, zValues[5]);

        // compare to transform t = 5*step*dt with ZOH
        ShortTimeFourierTransform.complex(sine, sineSpectrogram, nFreq, step, Apodization.Hamming, Padding.ZOH, false,
                true);
        final double[][] zValuesZOH = transpose(((DoubleDataSet3D) sineSpectrogram).getZValues());

        final double[] vals3refZOH = new double[nFreq * 2];
        System.arraycopy(vals3refstart, 0, vals3refZOH, 0, vals3refstart.length);
        final double realZOH = sine.get(DIM_Y, sine.getDataCount() - 1);
        final double imagZOH = sine.get(DIM_Z, sine.getDataCount() - 1);
        for (int i = 0; i < nFreq - vals3refstart.length / 2; i++) {
            vals3refZOH[vals3refstart.length + 2 * i] = realZOH;
            vals3refZOH[vals3refstart.length + 2 * i + 1] = imagZOH;
        }
        ShortTimeFourierTransform.apodizeComplex(vals3refZOH, Apodization.Hamming);
        fastFourierTrafo.complexForward(vals3refZOH);
        final double[] vals3refMagZOH = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals3refZOH, true));
        assertArrayEquals(zValuesZOH[5], vals3refMagZOH);

        // compare to transform t = 5*step*dt with ZOH
        final DataSet sineSpectrogramMirror = ShortTimeFourierTransform.complex(sine, null, nFreq, step,
                Apodization.Rectangular, Padding.MIRROR, true, false);
        final double[][] zValuesMirror = transpose(((DoubleDataSet3D) sineSpectrogramMirror).getZValues());
        final double[] vals3refMirror = new double[nFreq * 2];
        System.arraycopy(vals3refstart, 0, vals3refMirror, 0, vals3refstart.length);
        for (int i = 0; i < nFreq - vals3refstart.length / 2; i++) {
            vals3refMirror[vals3refstart.length + 2 * i] = sine.get(DIM_Y, sine.getDataCount() - i - 1);
            vals3refMirror[vals3refstart.length + 2 * i + 1] = sine.get(DIM_Z, sine.getDataCount() - i - 1);
        }
        ShortTimeFourierTransform.apodizeComplex(vals3refMirror, Apodization.Rectangular);
        fastFourierTrafo.complexForward(vals3refMirror);
        final double[] vals3refMagMirror = alignSpectrum(
                SpectrumTools.computeMagnitudeSpectrum_dB(vals3refMirror, false));
        assertArrayEquals(zValuesMirror[5], vals3refMagMirror);

        // perform different sized Transforms
        ShortTimeFourierTransform.complex(sine, sineSpectrogram, nFreq, 2 * step, Apodization.Rectangular, Padding.MIRROR,
                true, false);
        assertEquals(3, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(nFreq, sineSpectrogram.getDataCount(DIM_Y));

        ShortTimeFourierTransform.complex(sine, sineSpectrogram, 2 * nFreq, 2 * step, Apodization.Rectangular,
                Padding.MIRROR, true, false);
        assertEquals(3, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(nFreq*2, sineSpectrogram.getDataCount(DIM_Y));

        // test empty input dataSet (inplace and non-inplace)
        ShortTimeFourierTransform.complex(new MultiDimDoubleDataSet("empty",3), sineSpectrogram, 2 * nFreq, 2 * step,
                Apodization.Rectangular, Padding.MIRROR, true, false);
        assertEquals(0, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(0, sineSpectrogram.getDataCount(DIM_Y));
        final DataSet emptySpectrogram = ShortTimeFourierTransform.complex(new MultiDimDoubleDataSet("empty",3), null, 2 * nFreq,
                2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertEquals(0, emptySpectrogram.getDataCount(DIM_X));
        assertEquals(0, emptySpectrogram.getDataCount(DIM_Y));

        // test non-conforming dataSets
        assertThrows(IllegalArgumentException.class,
                () -> ShortTimeFourierTransform.complex(
                        new DoubleDataSet3D("illegal data set", new double[] { 1, 2 }, new double[] { 1, 2, 3 },
                                new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } }),
                        null, 8, 8, Apodization.Hann, Padding.ZERO, true, false));
    }

    /**
     * Realigns a spectrum by switching upper and lower half [0,..., fs/2, -fs/2, ..., -1] -&gt; [-fs/2, ..., fs/2]
     */
    private static double[] alignSpectrum(double[] input) {
        final double[] result = new double[input.length];
        System.arraycopy(input, 0, result, input.length / 2, input.length / 2);
        System.arraycopy(input, input.length / 2, result, 0, input.length / 2);
        return result;
    }

    private static double[] getStridedComplex(final double[] real, final double[] imag, final int start,
            final int end) {
        final int len = end - start;
        final double[] res = new double[len * 2];
        for (int i = 0; i < len; i++) {
            res[2 * i] = real[start + i];
            res[2 * i + 1] = imag[start + i];
        }
        return res;
    }
}
