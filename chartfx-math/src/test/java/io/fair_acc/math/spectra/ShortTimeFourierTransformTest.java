package io.fair_acc.math.spectra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static io.fair_acc.dataset.DataSet.DIM_Z;

import java.util.Arrays;

import org.jtransforms.fft.DoubleFFT_1D;
import org.junit.jupiter.api.Test;

import io.fair_acc.dataset.DataSetMetaData;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.spi.DataSetBuilder;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.spi.MultiDimDoubleDataSet;
import io.fair_acc.math.spectra.ShortTimeFourierTransform.Padding;

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

        final GridDataSet sineSpectrogram = ShortTimeFourierTransform.real(sine, null, nFft, step, Apodization.Hann, Padding.ZERO, false, true);
        // check dimensions
        assertArrayEquals(new int[] { nFft / 2, 6 }, sineSpectrogram.getShape());
        assertEquals(nFft / 2 * 6, sineSpectrogram.getDataCount());

        // check metadata
        assertEquals("STFT(" + sine.getName() + ")", sineSpectrogram.getName());
        assertEquals(nFft, Integer.valueOf(((DataSetMetaData) sineSpectrogram).getMetaInfo().get("RealSTFT-nFFT")));
        assertEquals(step, Integer.valueOf(((DataSetMetaData) sineSpectrogram).getMetaInfo().get("RealSTFT-step")));

        // check axes
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Y).getMin());
        assertEquals(dt * nSamples, sineSpectrogram.getAxisDescription(DIM_Y).getMax(), nFft);
        assertEquals(sine.getAxisDescription(DIM_X).getName(), sineSpectrogram.getAxisDescription(DIM_Y).getName());
        assertEquals(sine.getAxisDescription(DIM_X).getUnit(), sineSpectrogram.getAxisDescription(DIM_Y).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_X).getMin());
        assertEquals(0.5 / dt, sineSpectrogram.getAxisDescription(DIM_X).getMax(), 10.0);
        assertEquals("Frequency", sineSpectrogram.getAxisDescription(DIM_X).getName());
        assertEquals("Hz", sineSpectrogram.getAxisDescription(DIM_X).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Z).getMin());
        // assertEquals(0.5, sineSpectrogram.getAxisDescription(DIM_Z).getMax(), 0.1);
        assertEquals("Magnitude", sineSpectrogram.getAxisDescription(DIM_Z).getName());
        assertEquals(sine.getAxisDescription(DIM_Y).getUnit(), sineSpectrogram.getAxisDescription(DIM_Z).getUnit());

        // check spectrogram
        assertArrayEquals(new double[] { 0, 1 * step * dt, 2 * step * dt, 3 * step * dt, 4 * step * dt, 5 * step * dt }, sineSpectrogram.getGridValues(DIM_Y), dt);
        // get spectrogram raw data
        final double[][] zValues = getDataAsNestedArray(sineSpectrogram);
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFft);
        // compare to transform t = 0
        final double[] vals1ref = Arrays.copyOfRange(sine.getValues(DIM_Y), 0, nFft);
        Apodization.Hann.apodize(vals1ref);
        fastFourierTrafo.realForward(vals1ref);
        final double[] vals1refMag = SpectrumTools.computeMagnitudeSpectrum(vals1ref, true);
        assertArrayEquals(vals1refMag, zValues[0]);
        // compare to transform t = 2 * step * dt
        final double[] vals2ref = Arrays.copyOfRange(sine.getValues(DIM_Y), 2 * step, 2 * step + nFft);
        Apodization.Hann.apodize(vals2ref);
        fastFourierTrafo.realForward(vals2ref);
        final double[] vals2refMag = SpectrumTools.computeMagnitudeSpectrum(vals2ref, true);
        assertArrayEquals(vals2refMag, zValues[2]);
        // compare to transform t = 5*step*dt
        final double[] vals3ref = new double[nFft];
        System.arraycopy(sine.getValues(DIM_Y), 5 * step, vals3ref, 0, nFft - 32 - step);
        Apodization.Hann.apodize(vals3ref);
        fastFourierTrafo.realForward(vals3ref);
        final double[] vals3refMag = SpectrumTools.computeMagnitudeSpectrum(vals3ref, true);
        assertArrayEquals(vals3refMag, zValues[5]);

        // compare to transform t = 5*step*dt with ZOH
        ShortTimeFourierTransform.real(sine, sineSpectrogram, nFft, step, Apodization.Hamming, Padding.ZOH, false, true);
        final double[][] zValuesZOH = getDataAsNestedArray(sineSpectrogram);
        final double[] vals3refZOH = new double[nFft];
        System.arraycopy(sine.getValues(DIM_Y), 5 * step, vals3refZOH, 0, nFft - 32 - step);
        Arrays.fill(vals3refZOH, nFft - 32 - step, nFft, vals3refZOH[nFft - 32 - step - 1]);
        Apodization.Hamming.apodize(vals3refZOH);
        fastFourierTrafo.realForward(vals3refZOH);
        final double[] vals3refMagZOH = SpectrumTools.computeMagnitudeSpectrum(vals3refZOH, true);
        assertArrayEquals(zValuesZOH[5], vals3refMagZOH);

        // compare to transform t = 5*step*dt with ZOH
        final GridDataSet sineSpectrogramMirror = ShortTimeFourierTransform.real(sine, null, nFft, step, Apodization.Rectangular, Padding.MIRROR, true, false);
        final double[][] zValuesMirror = getDataAsNestedArray(sineSpectrogramMirror);
        final double[] vals3refMirror = new double[nFft];
        System.arraycopy(sine.getValues(DIM_Y), 5 * step, vals3refMirror, 0, nFft - 32 - step);
        for (int i = 0; i < vals3refMirror.length - nFft + 32 + step; i++) {
            vals3refMirror[nFft - 32 - step + i] = sine.getValues(DIM_Y)[5 * step + nFft - 32 - step - i - 1];
        }
        Apodization.Rectangular.apodize(vals2ref);
        fastFourierTrafo.realForward(vals3refMirror);
        final double[] vals3refMagMirror = SpectrumTools.computeMagnitudeSpectrum_dB(vals3refMirror, false);
        assertArrayEquals(zValuesMirror[5], vals3refMagMirror);

        // perform different sized Transforms
        ShortTimeFourierTransform.real(sine, sineSpectrogram, nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { nFft / 2, 3 }, sineSpectrogram.getShape());

        ShortTimeFourierTransform.real(sine, sineSpectrogram, 2 * nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { nFft, 3 }, sineSpectrogram.getShape());

        // test empty input dataSet (inplace and non-inplace)
        ShortTimeFourierTransform.real(new DoubleDataSet("empty"), sineSpectrogram, 2 * nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { 0, 0 }, sineSpectrogram.getShape());
        final GridDataSet emptySpectrogram = ShortTimeFourierTransform.real(new DoubleDataSet("empty"), null, 2 * nFft, 2 * step, Apodization.Rectangular,
                Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { 0, 0 }, emptySpectrogram.getShape());

        // test unconforming dataSets
        assertThrows(IllegalArgumentException.class,
                () -> ShortTimeFourierTransform.real(new DataSetBuilder("illegal data set").setDimension(3).setInitalCapacity(3, 5, 7).build(), null, 8, 8, Apodization.Hann, Padding.ZERO, true, false));
    }

    /**
     * Utility Function to transpose a DataSet
     *
     * @param input A 3 dimensional DataSet
     * @return the transposed array content
     */
    private static double[][] getDataAsNestedArray(final GridDataSet input) {
        if (input == null || input.getDataCount() == 0) {
            return new double[0][0];
        }
        int nx = input.getShape(DIM_X);
        int ny = input.getShape(DIM_Y);
        final double[][] output = new double[ny][nx];
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                output[j][i] = input.get(DIM_Z, i + j * nx);
            }
        }
        return output;
    }

    @Test
    public void testComplexSTFT() {
        final int nFft = 128;
        final int step = 64;
        final int nSamples = (128 * 3) - 32;
        final Apodization apodization = Apodization.Hann;
        final Padding padding = Padding.ZERO;
        final boolean dbScale = false;
        final boolean truncateDCNy = true;

        final double dt = 1e-3;
        final double fSine = 25.0;
        final MultiDimDoubleDataSet sine = new MultiDimDoubleDataSet("sine", 3, nSamples);
        for (int i = 0; i < nSamples; i++) {
            double t = i * dt;
            double y_re = i > nFft ? Math.sin(fSine * Math.PI * 2 * t) : 0.0;
            double y_im = i > nFft ? Math.cos(fSine * Math.PI * 2 * t) : 0.0;
            sine.set(i, t, y_re, y_im);
        }
        sine.getAxisDescription(DIM_X).set("Time", "s");
        sine.getAxisDescription(DIM_Y).set("Voltage", "V");

        final GridDataSet sineSpectrogram = ShortTimeFourierTransform.complex(sine, null, nFft, step, apodization, padding, false, true);
        // check dimensions
        assertArrayEquals(new int[] { nFft, 6 }, sineSpectrogram.getShape());
        assertEquals(nFft * 6, sineSpectrogram.getDataCount());

        // check metadata
        assertEquals("STFT(" + sine.getName() + ")", sineSpectrogram.getName());
        assertEquals(nFft, Integer.valueOf(((DataSetMetaData) sineSpectrogram).getMetaInfo().get("ComplexSTFT-nFFT")));
        assertEquals(step, Integer.valueOf(((DataSetMetaData) sineSpectrogram).getMetaInfo().get("ComplexSTFT-step")));

        // check axes
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Y).getMin());
        assertEquals(dt * nSamples, sineSpectrogram.getAxisDescription(DIM_Y).getMax(), nFft);
        assertEquals(sine.getAxisDescription(DIM_X).getName(), sineSpectrogram.getAxisDescription(DIM_Y).getName());
        assertEquals(sine.getAxisDescription(DIM_X).getUnit(), sineSpectrogram.getAxisDescription(DIM_Y).getUnit());
        assertEquals(-0.5 / dt, sineSpectrogram.getAxisDescription(DIM_X).getMin(), 10.0);
        assertEquals(0.5 / dt, sineSpectrogram.getAxisDescription(DIM_X).getMax(), 10.0);
        assertEquals("Frequency", sineSpectrogram.getAxisDescription(DIM_X).getName());
        assertEquals("Hz", sineSpectrogram.getAxisDescription(DIM_X).getUnit());
        assertEquals("Magnitude", sineSpectrogram.getAxisDescription(DIM_Z).getName());
        assertEquals(sine.getAxisDescription(DIM_Y).getUnit(), sineSpectrogram.getAxisDescription(DIM_Z).getUnit());

        // check spectrogram
        assertArrayEquals(new double[] { 0, 1 * step * dt, 2 * step * dt, 3 * step * dt, 4 * step * dt, 5 * step * dt }, sineSpectrogram.getGridValues(DIM_Y), dt);
        // get spectrogram raw data
        final double[][] zValues = getDataAsNestedArray(sineSpectrogram);
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFft);
        // compare to transform t = 0
        final double[] vals1ref = getStridedComplex(sine.getValues(DIM_Y), sine.getValues(DIM_Z), 0, nFft);
        ShortTimeFourierTransform.apodizeComplex(vals1ref, Apodization.Hann);
        fastFourierTrafo.complexForward(vals1ref);
        final double[] vals1refMag = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals1ref, true));
        assertArrayEquals(vals1refMag, zValues[0]);
        // compare to transform t = 2 * step * dt
        final double[] vals2ref = getStridedComplex(sine.getValues(DIM_Y), sine.getValues(DIM_Z), 2 * step, 2 * step + nFft);
        ShortTimeFourierTransform.apodizeComplex(vals2ref, Apodization.Hann);
        fastFourierTrafo.complexForward(vals2ref);
        final double[] vals2refMag = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals2ref, true));
        assertArrayEquals(vals2refMag, zValues[2]);
        // compare to transform t = d5 * step * dt
        final double[] vals3refstart = getStridedComplex(sine.getValues(DIM_Y), sine.getValues(DIM_Z), 5 * step, sine.getDataCount());
        final double[] vals3ref = new double[nFft * 2];
        System.arraycopy(vals3refstart, 0, vals3ref, 0, vals3refstart.length);
        ShortTimeFourierTransform.apodizeComplex(vals3ref, Apodization.Hann);
        fastFourierTrafo.complexForward(vals3ref);
        final double[] vals3refMag = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals3ref, true));
        assertArrayEquals(vals3refMag, zValues[5]);

        // compare to transform t = 5*step*dt with ZOH
        ShortTimeFourierTransform.complex(sine, sineSpectrogram, nFft, step, Apodization.Hamming, Padding.ZOH, false, true);
        final double[][] zValuesZOH = getDataAsNestedArray(sineSpectrogram);

        final double[] vals3refZOH = new double[nFft * 2];
        System.arraycopy(vals3refstart, 0, vals3refZOH, 0, vals3refstart.length);
        final double realZOH = sine.get(DIM_Y, sine.getDataCount() - 1);
        final double imagZOH = sine.get(DIM_Z, sine.getDataCount() - 1);
        for (int i = 0; i < nFft - vals3refstart.length / 2; i++) {
            vals3refZOH[vals3refstart.length + 2 * i] = realZOH;
            vals3refZOH[vals3refstart.length + 2 * i + 1] = imagZOH;
        }
        ShortTimeFourierTransform.apodizeComplex(vals3refZOH, Apodization.Hamming);
        fastFourierTrafo.complexForward(vals3refZOH);
        final double[] vals3refMagZOH = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum(vals3refZOH, true));
        assertArrayEquals(zValuesZOH[5], vals3refMagZOH);

        // compare to transform t = 5*step*dt with ZOH
        final GridDataSet sineSpectrogramMirror = ShortTimeFourierTransform.complex(sine, null, nFft, step, Apodization.Rectangular, Padding.MIRROR, true, false);
        final double[][] zValuesMirror = getDataAsNestedArray(sineSpectrogramMirror);
        final double[] vals3refMirror = new double[nFft * 2];
        System.arraycopy(vals3refstart, 0, vals3refMirror, 0, vals3refstart.length);
        for (int i = 0; i < nFft - vals3refstart.length / 2; i++) {
            vals3refMirror[vals3refstart.length + 2 * i] = sine.get(DIM_Y, sine.getDataCount() - i - 1);
            vals3refMirror[vals3refstart.length + 2 * i + 1] = sine.get(DIM_Z, sine.getDataCount() - i - 1);
        }
        ShortTimeFourierTransform.apodizeComplex(vals3refMirror, Apodization.Rectangular);
        fastFourierTrafo.complexForward(vals3refMirror);
        final double[] vals3refMagMirror = alignSpectrum(SpectrumTools.computeMagnitudeSpectrum_dB(vals3refMirror, false));
        assertArrayEquals(zValuesMirror[5], vals3refMagMirror);

        // perform different sized Transforms
        ShortTimeFourierTransform.complex(sine, sineSpectrogram, nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { nFft, 3 }, sineSpectrogram.getShape());

        ShortTimeFourierTransform.complex(sine, sineSpectrogram, 2 * nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { nFft * 2, 3 }, sineSpectrogram.getShape());

        // test empty input dataSet (inplace and non-inplace)
        ShortTimeFourierTransform.complex(new MultiDimDoubleDataSet("empty", 3), sineSpectrogram, 2 * nFft, 2 * step, Apodization.Rectangular, Padding.MIRROR,
                true, false);
        assertArrayEquals(new int[] { 0, 0 }, sineSpectrogram.getShape());
        final GridDataSet emptySpectrogram = ShortTimeFourierTransform.complex(new MultiDimDoubleDataSet("empty", 3), null, 2 * nFft, 2 * step,
                Apodization.Rectangular, Padding.MIRROR, true, false);
        assertArrayEquals(new int[] { 0, 0 }, emptySpectrogram.getShape());

        // test non-conforming dataSets
        assertThrows(IllegalArgumentException.class, () -> ShortTimeFourierTransform.complex( //
                                                                new DataSetBuilder("illegal data set") //
                                                                        .setValues(DIM_X, new double[] { 1, 2 }) //
                                                                        .setValues(DIM_Y, new double[] { 1, 2, 3 }) //
                                                                        .setValues(DIM_Z, new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } }) //
                                                                        .build(), //
                                                                null, 8, 8, Apodization.Hann, Padding.ZERO, true, false));

        // test strided complex low-level method
        double[] real = sine.getValues(DIM_Y);
        double[] imag = sine.getValues(DIM_Z);
        assertArrayEquals(
                ShortTimeFourierTransform.complex(getStridedComplex(real, imag, 0, real.length), null, nFft, step, apodization, Padding.MIRROR, dbScale,
                        truncateDCNy), //
                ShortTimeFourierTransform.complex(real, imag, null, nFft, step, apodization, Padding.MIRROR, dbScale, truncateDCNy));
        assertArrayEquals(
                ShortTimeFourierTransform.complex(getStridedComplex(real, imag, 0, real.length), null, nFft, step, apodization, Padding.ZERO, dbScale,
                        truncateDCNy), //
                ShortTimeFourierTransform.complex(real, imag, null, nFft, step, apodization, Padding.ZERO, dbScale, truncateDCNy));
        assertArrayEquals(
                ShortTimeFourierTransform.complex(getStridedComplex(real, imag, 0, real.length), null, nFft, step, apodization, Padding.ZOH, dbScale,
                        truncateDCNy), //
                ShortTimeFourierTransform.complex(real, imag, null, nFft, step, apodization, Padding.ZOH, dbScale, truncateDCNy));
        assertArrayEquals(
                ShortTimeFourierTransform.complex(getStridedComplex(real, imag, 0, real.length), null, nFft, step, apodization, Padding.ZOH, true,
                        truncateDCNy), //
                ShortTimeFourierTransform.complex(real, imag, null, nFft, step, apodization, Padding.ZOH, true, truncateDCNy));
    }

    /**
     * Realigns a spectrum by switching upper and lower half [0,..., fs/2, -fs/2, ..., -1] -&gt; [-fs/2, ..., fs/2]<
     */
    private static double[] alignSpectrum(double[] input) {
        final double[] result = new double[input.length];
        System.arraycopy(input, 0, result, input.length / 2, input.length / 2);
        System.arraycopy(input, input.length / 2, result, 0, input.length / 2);
        return result;
    }

    private static double[] getStridedComplex(final double[] real, final double[] imag, final int start, final int end) {
        final int len = end - start;
        final double[] res = new double[len * 2];
        for (int i = 0; i < len; i++) {
            res[2 * i] = real[start + i];
            res[2 * i + 1] = imag[start + i];
        }
        return res;
    }
}
