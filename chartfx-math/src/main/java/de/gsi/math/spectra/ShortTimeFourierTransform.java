package de.gsi.math.spectra;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import java.util.Arrays;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Static utility class providing magnitude spectrograms from complex and real valued input data.
 * Contains high-level functions, which transform whole DataSets and also add metadata/axis descriptions,
 * but also low level array based functions.
 * For complex input values, the data can be provided as two separate arrays as well as in the "interleaved"
 * layout used by JTransforms.
 * 
 * @author Alexander Krimm
 */
public class ShortTimeFourierTransform {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortTimeFourierTransform.class);

    /**
     * Applies the apodization function to data in "interleaved" complex array.
     * 
     * @param data an array containing [re1, im1, re2, im2 ... ]
     * @param apodization the apodization window function to use
     */
    protected static void apodizeComplex(double[] data, Apodization apodization) {
        final double[] window = apodization.getWindow(data.length / 2);
        for (int i = 0; i < data.length / 2; i++) {
            data[2 * i] = data[2 * i] * window[i];
            data[2 * i + 1] = data[2 * i + 1] * window[i];
        }
    }

    /**
     * Does a rounded up division.
     * 
     * @param a an integer
     * @param b another integer
     * @return ceil(a/b)
     * @see <a href="https://stackoverflow.com/a/21830188/12405013">https://stackoverflow.com/a/21830188/12405013</a>
     */
    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    /**
     * Perform a Short term fourier transform on complex input data.
     * The complex data is expected to be supplied as a DataSet with dim() = 3 with
     * time axis data in DIM_X, real part in DIM_Y and imaginary part in DIM_Z.
     * All dimensions should have the same number of samples.
     * 
     * @param input a dataset with equidistantly spaced y(t) = Re(c(t)) and z(t) = data
     * @param output optional output dataset, if not Null, data will be modified in place
     * @param nFFT the number of frequency bins
     * @param step The timestep size in samples
     * @param apodization function, by default Hann window is used
     * @param padding how to pad the slices at the start and end of the time axis: ZERO(default), ZOH or MIRROR
     * @return the spectrogram, a DataSet3D with dimensions [nf = nQuantx x nY = nQuantt]
     */
    public static DataSet complex(final DataSet input, final DataSet output, final int nFFT, final int step,
            final Apodization apodization, final Padding padding, final boolean dbScale, final boolean truncateDCNy) {
        // validate input data
        AssertUtils.notNull("input", input);
        AssertUtils.gtThanZero("nFFT", nFFT);
        AssertUtils.gtThanZero("step", step);
        AssertUtils.notNull("apodization", apodization);
        AssertUtils.notNull("padding", padding);
        AssertUtils.gtOrEqual("input.getDimension()", 3, input.getDimension());
        if (input.getDataCount(DIM_X) != input.getDataCount(DIM_Y)
                || input.getDataCount(DIM_X) != input.getDataCount(DIM_Z)) {
            throw new IllegalArgumentException("The first 3 dimensions must have equal number of points");
        }
        // early returns for trivial cases
        if (input.getDataCount() == 0) {
            if (output instanceof DoubleDataSet3D) {
                ((DoubleDataSet3D) output).set(new double[0], new double[0], new double[0][0]);
                return ((DoubleDataSet3D) output).clearMetaInfo();
            }
            return new DoubleDataSet3D("STFT(" + input.getName() + ")");
        }
        // get data from dataSet
        final int nSamples = input.getDataCount();
        final double dt = (input.get(DIM_X, nSamples - 1) - input.get(DIM_X, 0)) / nSamples;
        final double[] real = input.getValues(DIM_Y);
        final double[] imag = input.getValues(DIM_Z);
        final double[] oldTimeAxis = output == null ? null : output.getValues(DIM_X);
        final double[] timeAxis = getTimeAxis(dt, nSamples, step, oldTimeAxis);
        final double[] oldFrequencyAxis = output == null ? null : output.getValues(DIM_Y);
        final double[] frequencyAxis = getFrequencyAxisComplex(dt, nFFT, oldFrequencyAxis);
        final double[][] oldAmplitudeData = output instanceof DoubleDataSet3D ? ((DoubleDataSet3D) output).getZValues()
                                                                              : null;
        final double[][] amplitudeData = complex(real, imag, oldAmplitudeData, nFFT, step, apodization, padding,
                dbScale, truncateDCNy);

        // initialize result dataset
        DoubleDataSet3D result;
        if (output == null) {
            result = new DoubleDataSet3D("STFT(" + input.getName() + ")", timeAxis, frequencyAxis, amplitudeData);
        } else if (output instanceof DoubleDataSet3D) {
            result = (DoubleDataSet3D) output;
        } else {
            // TODO: find generic way to modify data in place
            result = new DoubleDataSet3D("STFT(" + input.getName() + ")", timeAxis, frequencyAxis, amplitudeData);
        }
        result.lock().writeLockGuard(() -> {
            // only update data arrays if at least one array was newly allocated
            if (oldTimeAxis != timeAxis || oldFrequencyAxis != frequencyAxis || oldAmplitudeData != amplitudeData) {
                result.set(timeAxis, frequencyAxis, amplitudeData);
            }

            result.getMetaInfo().put("ComplexSTFT-nFFT", Integer.toString(nFFT));
            result.getMetaInfo().put("ComplexSTFT-step", Integer.toString(step));

            // Set Axis Labels and Units
            final String timeUnit = input.getAxisDescription(DIM_X).getUnit();
            result.getAxisDescription(DIM_X).set("Time", timeUnit, timeAxis[0], timeAxis[timeAxis.length - 1]);
            final String freqUnit = timeUnit.equals("s") ? "Hz" : "1/" + timeUnit;
            result.getAxisDescription(DIM_Y).set("Frequency", freqUnit, frequencyAxis[0],
                    frequencyAxis[frequencyAxis.length - 1]);
            result.getAxisDescription(DIM_Z).set("Magnitude", input.getAxisDescription(DIM_Y).getUnit());
            result.recomputeLimits(DIM_Z);
        });

        return result;
    }

    public static double[][] complex(final double[] real, final double[] imag, final double[][] output, final int nFFT,
            final int step, final Apodization apodization, final Padding padding, final boolean dbScale,
            final boolean truncateDCNy) {
        AssertUtils.equalDoubleArrays(real, imag); // check for same length
        final int nT = ceilDiv(real.length, step); // number of time steps
        final double[][] amplitudeData = output == null || output.length != nFFT || output[0].length != nT
                                                 ? new double[nFFT][nT]
                                                 : output; // output array
        // calculate spectrogram
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFFT);
        final double[] raw = new double[2 * nFFT]; // array to perform calculations in
        for (int i = 0; i < nT; i++) {
            // obtain input data for FFT
            final int offset = i * step;
            final int validLength = real.length - offset;
        fillraw:
            for (int j = 0; j < nFFT; j++) {
                if (offset + j < real.length) {
                    raw[2 * j] = real[offset + j];
                    raw[2 * j + 1] = imag[offset + j];
                } else { // padding
                    switch (padding) {
                    case MIRROR:
                        raw[2 * j] = real[real.length - j + validLength - 1];
                        raw[2 * j + 1] = imag[imag.length - j + validLength - 1];
                        break;
                    case ZERO:
                        Arrays.fill(raw, 2 * j, raw.length, 0.0);
                        break fillraw; // break out of loop
                    default:
                    case ZOH:
                        raw[2 * j] = real[real.length - 1];
                        raw[2 * j + 1] = imag[imag.length - 1];
                        break;
                    }
                }
            }
            // apply apodization function
            apodizeComplex(raw, apodization);
            // perform Fourier transform
            fastFourierTrafo.complexForward(raw);
            // calculate magnitude spectrum
            final double[] current = dbScale ? SpectrumTools.computeMagnitudeSpectrum_dB(raw, truncateDCNy)
                                             : SpectrumTools.computeMagnitudeSpectrum(raw, truncateDCNy);
            // copy output into result array (layout of spectrum is 0, ..., fmax, 0, ..., fmin)
            // TODO: use more efficient copy, possibly transpose output?
            for (int j = 0; j < nFFT; j++) {
                if (j < nFFT / 2) {
                    amplitudeData[j + nFFT / 2][i] = current[j];
                } else {
                    amplitudeData[j - nFFT / 2][i] = current[j];
                }
            }
        }
        return amplitudeData;
    }

    public static double[][] complex(final double[] complexInput, final double[][] output, final int nFFT,
            final int step, final Apodization apodization, final Padding padding, final boolean dbScale,
            final boolean truncateDCNy) {
        final int nT = ceilDiv(complexInput.length, 2 * step); // number of time steps
        final double[][] amplitudeData = output == null || output.length != nFFT || output[0].length != nT
                                                 ? new double[nFFT][nT]
                                                 : output; // output array
        // calculate spectrogram
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFFT);
        final double[] raw = new double[2 * nFFT]; // array to perform calculations in
        for (int i = 0; i < nT; i++) {
            // obtain input data for FFT
            final int offset = i * 2 * step;
            final int validLength = complexInput.length - offset;
            if (validLength >= 2 * nFFT) {
                System.arraycopy(complexInput, offset, raw, 0, 2 * nFFT);
            } else { // data has to be padded
                System.arraycopy(complexInput, offset, raw, 0, validLength);
                switch (padding) {
                case MIRROR:
                    for (int j = validLength; j + 1 < raw.length; j += 2) {
                        raw[j] = complexInput[complexInput.length - j + validLength - 1];
                        raw[j + 1] = complexInput[complexInput.length - j + validLength];
                    }
                    break;
                case ZERO:
                    Arrays.fill(raw, validLength, raw.length, 0.0);
                    break;
                default:
                case ZOH:
                    for (int j = validLength; j + 1 < raw.length; j += 2) {
                        raw[j] = complexInput[complexInput.length - 2];
                        raw[j + 1] = complexInput[complexInput.length - 1];
                    }
                    break;
                }
            }
            // apply apodization function
            apodizeComplex(raw, apodization);
            // perform Fourier transform
            fastFourierTrafo.complexForward(raw);
            // calculate magnitude spectrum
            final double[] current = dbScale ? SpectrumTools.computeMagnitudeSpectrum_dB(raw, truncateDCNy)
                                             : SpectrumTools.computeMagnitudeSpectrum(raw, truncateDCNy);
            // copy output into result array (layout of spectrum is 0, ..., fmax, 0, ..., fmin)
            // TODO: use more efficient copy, possibly transpose output?
            for (int j = 0; j < nFFT; j++) {
                if (j < nFFT / 2) {
                    amplitudeData[j + nFFT / 2][i] = current[j];
                } else {
                    amplitudeData[j - nFFT / 2][i] = current[j];
                }
            }
        }
        return amplitudeData;
    }

    public static double[] getFrequencyAxisComplex(final double dt, final int nFFT, final double[] output) {
        final double fStep = 1.0 / dt / nFFT;
        final double[] frequencyAxis = output == null || output.length != nFFT ? new double[nFFT] : output;
        for (int i = -nFFT / 2; i < nFFT / 2; i++) {
            frequencyAxis[i + nFFT / 2] = i * fStep;
        }

        return frequencyAxis;
    }

    public static double[] getFrequencyAxisReal(final double dt, final int nFFT, final double[] output) {
        final double fStep = 1.0 / dt / nFFT;
        final double[] frequencyAxis = output == null || output.length != nFFT / 2 ? new double[nFFT / 2] : output;
        for (int i = 0; i < nFFT / 2; i++) {
            frequencyAxis[i] = i * fStep;
        }

        return frequencyAxis;
    }

    public static double[] getTimeAxis(final double dt, final int nSamples, final int step, final double[] output) {
        final int nT = ceilDiv(nSamples, step);
        final double[] timeAxis = output == null || output.length != nT ? new double[nT] : output;
        for (int i = 0; i < timeAxis.length; i++) {
            timeAxis[i] = dt * i * step;
        }
        return timeAxis;
    }

    public static DataSet real(final DataSet input, final DataSet output, final int nFFT, final int step,
            final Apodization apodization, final Padding padding, final boolean dbScale, final boolean truncateDCNy) {
        // validate input data
        AssertUtils.notNull("input", input);
        AssertUtils.gtThanZero("nFFT", nFFT);
        AssertUtils.gtThanZero("step", step);
        AssertUtils.notNull("apodization", apodization);
        AssertUtils.notNull("padding", padding);
        AssertUtils.gtOrEqual("input.getDimension()", 2, input.getDimension());
        if (input.getDataCount(DIM_X) != input.getDataCount(DIM_Y)) {
            throw new IllegalArgumentException("The X and Y dimensions must have equal number of points");
        }
        // early returns for trivial cases
        if (input.getDataCount() == 0) {
            if (output instanceof DoubleDataSet3D) {
                ((DoubleDataSet3D) output).set(new double[0], new double[0], new double[0][0]);
                return ((DoubleDataSet3D) output).clearMetaInfo();
            }
            return new DoubleDataSet3D("STFT(" + input.getName() + ")");
        }
        // get data from dataSet
        final int nSamples = input.getDataCount();
        final double dt = (input.get(DIM_X, nSamples - 1) - input.get(DIM_X, 0)) / nSamples;
        final double[] yData = input.getValues(DIM_Y);
        final double[] oldTimeAxis = output == null ? null : output.getValues(DIM_X);
        final double[] timeAxis = getTimeAxis(dt, nSamples, step, oldTimeAxis);
        final double[] oldFrequencyAxis = output == null ? null : output.getValues(DIM_Y);
        final double[] frequencyAxis = getFrequencyAxisReal(dt, nFFT, oldFrequencyAxis);
        final double[][] oldAmplitudeData = output instanceof DoubleDataSet3D ? ((DoubleDataSet3D) output).getZValues()
                                                                              : null;
        final double[][] amplitudeData = real(yData, oldAmplitudeData, nFFT, step, apodization, padding, dbScale,
                truncateDCNy);

        // initialize result dataset
        DoubleDataSet3D result;
        if (output == null) {
            result = new DoubleDataSet3D("STFT(" + input.getName() + ")", timeAxis, frequencyAxis, amplitudeData);
        } else if (output instanceof DoubleDataSet3D) {
            result = (DoubleDataSet3D) output;
        } else {
            // TODO: find generic way to modify data in place
            result = new DoubleDataSet3D("STFT(" + input.getName() + ")", timeAxis, frequencyAxis, amplitudeData);
        }
        result.lock().writeLockGuard(() -> {
            // only update data arrays if at least one array was newly allocated
            if (oldTimeAxis != timeAxis || oldFrequencyAxis != frequencyAxis || oldAmplitudeData != amplitudeData) {
                result.set(timeAxis, frequencyAxis, amplitudeData);
            }

            result.getMetaInfo().put("RealSTFT-nFFT", Integer.toString(nFFT));
            result.getMetaInfo().put("RealSTFT-step", Integer.toString(step));

            // Set Axis Labels and Units
            final String timeUnit = input.getAxisDescription(DIM_X).getUnit();
            result.getAxisDescription(DIM_X).set("Time", timeUnit, timeAxis[0], timeAxis[timeAxis.length - 1]);
            final String freqUnit = timeUnit.equals("s") ? "Hz" : "1/" + timeUnit;
            result.getAxisDescription(DIM_Y).set("Frequency", freqUnit, frequencyAxis[0],
                    frequencyAxis[frequencyAxis.length - 1]);
            result.getAxisDescription(DIM_Z).set("Magnitude", input.getAxisDescription(DIM_Y).getUnit());
            result.recomputeLimits(DIM_Z);
        });

        return result;
    }

    public static double[][] real(final double[] input, final double[][] output, final int nFFT, final int step,
            final Apodization apodization, final Padding padding, final boolean dbScale, final boolean truncateDCNy) {
        final int nT = ceilDiv(input.length, step); // number of time steps
        final double[][] amplitudeData = output == null || output.length != nFFT / 2 || output[0].length != nT
                                                 ? new double[nFFT / 2][nT]
                                                 : output; // output array
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(nFFT);
        final double[] raw = new double[nFFT]; // array to perform calculations in
        for (int i = 0; i < nT; i++) {
            // obtain input data for FFT
            final int offset = i * step;
            final int validLength = input.length - offset;
            if (validLength >= nFFT) {
                System.arraycopy(input, offset, raw, 0, nFFT);
            } else { // data has to be padded
                System.arraycopy(input, offset, raw, 0, validLength);
                switch (padding) {
                case MIRROR:
                    for (int j = validLength; j < raw.length; j++) {
                        raw[j] = input[input.length - j + validLength - 1];
                    }
                    break;
                case ZERO:
                    Arrays.fill(raw, validLength, raw.length, 0.0);
                    break;
                default:
                case ZOH:
                    Arrays.fill(raw, validLength, raw.length, input[input.length - 1]);
                    break;
                }
            }
            // apply apodization function
            apodization.apodize(raw);
            // perform Fourier transform
            fastFourierTrafo.realForward(raw);
            // calculate magnitude spectrum
            final double[] current = dbScale ? SpectrumTools.computeMagnitudeSpectrum_dB(raw, truncateDCNy)
                                             : SpectrumTools.computeMagnitudeSpectrum(raw, truncateDCNy);
            // copy output into result array (layout of spectrum is 0, ..., fmax, 0, ..., fmin)
            // TODO: use more efficient copy, possibly transpose output?
            for (int j = 0; j < nFFT / 2; j++) {
                amplitudeData[j][i] = current[j];
            }
        }
        return amplitudeData;
    }

    public enum Padding {
        ZERO,
        ZOH,
        MIRROR;
    }
}
