package de.gsi.math.spectra.fft;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.MultiDimDoubleDataSet;
import de.gsi.math.spectra.Apodization;
import de.gsi.math.spectra.fft.ShortTimeFourierTransform.Padding;

/**
 * Test the short-time fourier transform.
 * For now just tests against trigeometric functions and verifies some points of the spectrum.
 * 
 * @author Alexander Krimm
 */
class STFTTest {
    @Test
    public void testRealSTFT() {
        final int N_FREQ = 1024;
        final int N_T = 30;
        final int N_SAMPLES = 8090;
        final double DT = 1e-3;
        final double F_SINE = 50.0;
        DoubleErrorDataSet sine = new DoubleErrorDataSet("sine", N_SAMPLES);
        for (int i = 0; i < N_SAMPLES; i++) {
            double t = i * DT;
            double y = i > 0.2 * N_SAMPLES ? Math.sin(F_SINE * Math.PI * 2 * t) : 0.0;
            sine.set(i, t, y);
        }
        sine.getAxisDescription(DIM_X).set("Time", "s");
        sine.getAxisDescription(DIM_Y).set("Voltage", "V");

        DataSet3D sineSpectrogram = ShortTimeFourierTransform.getSpectrogram(sine, N_FREQ, N_T);

        // check dimensions
        assertEquals(N_T, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(N_FREQ / 2, sineSpectrogram.getDataCount(DIM_Y));
        assertEquals(N_FREQ / 2 * N_T, sineSpectrogram.getDataCount(DIM_Z));

        // check metadata
        assertEquals("STFT(" + sine.getName() + ")", sineSpectrogram.getName());
        assertEquals(N_FREQ, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("STFT-nFFT")));
        assertEquals(N_T, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("STFT-nT")));

        // check axes
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_X).getMin());
        assertEquals(DT * N_SAMPLES, sineSpectrogram.getAxisDescription(DIM_X).getMax(), N_FREQ);
        assertEquals(sine.getAxisDescription(DIM_X).getName(), sineSpectrogram.getAxisDescription(DIM_X).getName());
        assertEquals(sine.getAxisDescription(DIM_X).getUnit(), sineSpectrogram.getAxisDescription(DIM_X).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Y).getMin());
        assertEquals(0.5 / DT, sineSpectrogram.getAxisDescription(DIM_Y).getMax(), 10.0);
        assertEquals("Frequency", sineSpectrogram.getAxisDescription(DIM_Y).getName());
        assertEquals("Hz", sineSpectrogram.getAxisDescription(DIM_Y).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Z).getMin());
        assertEquals(0.5, sineSpectrogram.getAxisDescription(DIM_Z).getMax(), 0.1);
        assertEquals("Magnitude", sineSpectrogram.getAxisDescription(DIM_Z).getName());
        assertEquals(sine.getAxisDescription(DIM_Y).getUnit(), sineSpectrogram.getAxisDescription(DIM_Z).getUnit());

        // check spectrogram
        assertEquals(0.0, sineSpectrogram.getZ(2, N_FREQ / 4));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.6 < //
                   sineSpectrogram.getZ(15, sineSpectrogram.getYIndex(F_SINE)));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.2 > sineSpectrogram.getZ(15,
                           sineSpectrogram.getYIndex(F_SINE) + 2));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.2 > sineSpectrogram.getZ(15,
                           sineSpectrogram.getYIndex(F_SINE) - 2));
    }

    @Test
    public void testComplexSTFT() {
        final int N_FREQ = 128;
        final int N_T = 30;
        final int N_SAMPLES = 8090;
        final double DT = 1e-3;
        final double F_SINE = 50.0;
        MultiDimDoubleDataSet sine = new MultiDimDoubleDataSet("sine", 3, N_SAMPLES);
        for (int i = 0; i < N_SAMPLES; i++) {
            double t = i * DT;
            double y_re = i > 0.2 * N_SAMPLES ? Math.sin(F_SINE * Math.PI * 2 * t) : 0.0;
            double y_im = i > 0.2 * N_SAMPLES ? Math.cos(F_SINE * Math.PI * 2 * t) : 0.0;
            sine.set(i, t, y_re, y_im);
        }
        sine.getAxisDescription(DIM_X).set("Time", "s");
        sine.getAxisDescription(DIM_Y).set("Voltage", "V");

        DataSet3D sineSpectrogram = ShortTimeFourierTransform.getSpectrogramComplex(sine, N_FREQ, N_T,
                Apodization.Hann, Padding.ZERO);

        // check dimensions
        assertEquals(N_T, sineSpectrogram.getDataCount(DIM_X));
        assertEquals(N_FREQ, sineSpectrogram.getDataCount(DIM_Y));
        assertEquals(N_FREQ * N_T, sineSpectrogram.getDataCount(DIM_Z));

        // check metadata
        assertEquals("STFT(" + sine.getName() + ")", sineSpectrogram.getName());
        assertEquals(N_FREQ, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("STFT-nFFT")));
        assertEquals(N_T, Integer.valueOf(((DoubleDataSet3D) sineSpectrogram).getMetaInfo().get("STFT-nT")));

        // check axes
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_X).getMin());
        assertEquals(7.696, sineSpectrogram.getAxisDescription(DIM_X).getMax());
        assertEquals(sine.getAxisDescription(DIM_X).getName(), sineSpectrogram.getAxisDescription(DIM_X).getName());
        assertEquals(sine.getAxisDescription(DIM_X).getUnit(), sineSpectrogram.getAxisDescription(DIM_X).getUnit());
        assertEquals(-0.5 / DT, sineSpectrogram.getAxisDescription(DIM_Y).getMin(), 0.1);
        assertEquals(0.5 / DT, sineSpectrogram.getAxisDescription(DIM_Y).getMax(), 10);
        assertEquals("Frequency", sineSpectrogram.getAxisDescription(DIM_Y).getName());
        assertEquals("Hz", sineSpectrogram.getAxisDescription(DIM_Y).getUnit());
        assertEquals(0, sineSpectrogram.getAxisDescription(DIM_Z).getMin());
        assertEquals(0.5, sineSpectrogram.getAxisDescription(DIM_Z).getMax(), 0.1);
        assertEquals("Magnitude", sineSpectrogram.getAxisDescription(DIM_Z).getName());
        assertEquals(sine.getAxisDescription(DIM_Y).getUnit(), sineSpectrogram.getAxisDescription(DIM_Z).getUnit());

        // check spectrogram
        assertEquals(0.0, sineSpectrogram.getZ(2, N_FREQ / 4));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.6 < //
                   sineSpectrogram.getZ(15, sineSpectrogram.getYIndex(-F_SINE)));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.2 > //
                   sineSpectrogram.getZ(15, sineSpectrogram.getYIndex(F_SINE)));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.2 > sineSpectrogram.getZ(15,
                           sineSpectrogram.getYIndex(F_SINE) + 2));
        assertTrue(sineSpectrogram.getAxisDescription(DIM_Z).getMax() * 0.2 > sineSpectrogram.getZ(15,
                           sineSpectrogram.getYIndex(F_SINE) - 2));
    }
}
