package de.gsi.math.spectra;

import java.util.Arrays;

import org.jtransforms.fft.DoubleFFT_1D;

import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.fitter.NonLinearRegressionFitter;
import de.gsi.math.functions.CombFunction;

/**
 * Class implements frequency interpolation of spectral peaks. The main idea behind these algorithm is: The resolution
 * of frequency estimates that are solely based on the value of the highest bin of an FFT or DTFT spectrum is limited by
 * the binning of the frequency spectrum, which assuming an FFT spectrum is determined by the number of samples N that
 * have been used to compute the spectra (resolution = 1/N). The functions in this class provide algorithms that fit
 * this frequency by taking the value not only of the highest but also adjacent bins into account. While the peak shape
 * strongly depends on the underlying process, the gaussian interpolation gives typically the best results in most
 * cases.
 *
 * @see #interpolateGaussian reference: R.J. Steinhagen, "Tune and Chromaticity Diagnostics", in: Proceedings of the
 *      CERN Accelerator School, Dourdan, France, 2005
 * @author rstein
 */
public class SpectrumTools {
    /**
     * compute equidistant frequency axis based on the length of the magnitude spectrum
     *
     * @param nMag requested length of output vector
     * @return computed [0.0, 0.5] frequency scale
     */
    public static double[] computeFrequencyScale(final int nMag) {
        final double[] ret = new double[nMag];
        final double scale = 0.5 / nMag;
        for (int i = 0; i < nMag; i++) {
            ret[i] = i * scale;
        }
        return ret;
    }

    /**
     * compute equidistant frequency axis based on the length of the magnitude spectrum
     *
     * @param nMag requested length of output vector
     * @return computed [0.0, 0.5] frequency scale
     */
    public static float[] computeFrequencyScaleFloat(final int nMag) {
        final float[] ret = new float[nMag];
        final float scale = 0.5f / nMag;
        for (int i = 0; i < nMag; i++) {
            ret[i] = i * scale;
        }
        return ret;
    }

    /**
     * compute magnitude power spectra
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data Since due to intrinsic uncertainties the DC and Nyquist frequency components are less
     *        representative for the given spectrum, their values are set to their adjacent frequency bins.
     * @return computed magnitude spectrum
     */
    public static double[] computeMagnitudeSpectrum(final double[] data) {
        return computeMagnitudeSpectrum(data, true);
    }

    /**
     * compute magnitude power spectra
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data
     * @param truncateDCNyq true: whether to smooth spectra and to ZOH the DC and Nyquist frequencies
     * @return computed magnitude spectrum
     */
    public static double[] computeMagnitudeSpectrum(final double[] data, final boolean truncateDCNyq) {
        final double[] ret = new double[data.length / 2];
        for (int i = 0; i < ret.length; i++) {
            final int i2 = i << 1;
            final double Re = data[i2];
            final double Im = data[i2 + 1];

            ret[i] = TMathConstants.Sqrt(TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im)) / ret.length;
        }
        if (truncateDCNyq) {
            // smooth spectra on both ends to minimise DC/Nyquist frequency
            // artefacts
            ret[0] = ret[1];
            ret[ret.length - 1] = ret[ret.length - 2];
        } else {
            // full DC/Nyquist frequency treatment
            ret[0] = data[0] / ret.length;
            ret[ret.length - 1] = data[1] / ret.length;
        }

        return ret;
    }

    /**
     * compute magnitude power spectra
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data Since due to intrinsic uncertainties the DC and Nyquist frequency components are less
     *        representative for the given spectrum, their values are set to their adjacent frequency bins.
     * @return computed magnitude spectrum
     */
    public static float[] computeMagnitudeSpectrum(final float[] data) {
        return computeMagnitudeSpectrum(data, true);
    }

    /**
     * compute magnitude power spectra
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data
     * @param truncateDCNyq true: whether to smooth spectra and to ZOH the DC and Nyquist frequencies
     * @return computed magnitude spectrum
     */
    public static float[] computeMagnitudeSpectrum(final float[] data, final boolean truncateDCNyq) {
        final float[] ret = new float[data.length / 2];
        for (int i = 0; i < ret.length; i++) {
            final int i2 = i << 1;
            final double Re = data[i2];
            final double Im = data[i2 + 1];

            ret[i] = (float) (TMathConstants.Sqrt(TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im)) / ret.length);
        }
        if (truncateDCNyq) {
            // smooth spectra on both ends to minimise DC/Nyquist frequency
            // artefacts
            ret[0] = ret[1];
            ret[ret.length - 1] = ret[ret.length - 2];
        } else {
            // full DC/Nyquist frequency treatment
            ret[0] = data[0] / ret.length;
            ret[ret.length - 1] = data[1] / ret.length;
        }

        return ret;
    }

    /**
     * compute magnitude power spectra in decibel
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data
     * @param truncateDCNyq true: whether to smooth spectra and to ZOH the DC and Nyquist frequencies
     * @return computed magnitude spectrum in [dB]
     */
    public static double[] computeMagnitudeSpectrum_dB(final double[] data, final boolean truncateDCNyq) {
        final int n2 = data.length / 2;
        final double[] ret = new double[n2];
        for (int i = 0; i < ret.length; i++) {
            final int i2 = i << 1;
            final double Re = data[i2];
            final double Im = data[i2 + 1];

            ret[i] = 10 * TMathConstants.Log10((TMathConstants.Sqr(Re / n2) + TMathConstants.Sqr(Im / n2)));
        }

        if (truncateDCNyq) {
            // smooth spectra on both ends to minimise DC/Nyquist frequency
            // artifacts
            ret[0] = ret[1];
            ret[n2 - 1] = ret[n2 - 2];
        } else {
            // full DC/Nyquist frequency treatment
            ret[0] = data[0];
            ret[ret.length - 1] = data[1];
        }

        return ret;
    }

    /**
     * compute magnitude power spectra in decibel
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data
     * @param truncateDCNyq true: whether to smooth spectra and to ZOH the DC and Nyquist frequencies
     * @return computed magnitude spectrum in [dB]
     */
    public static float[] computeMagnitudeSpectrum_dB(final float[] data, final boolean truncateDCNyq) {
        final int n2 = data.length / 2;
        final float[] ret = new float[data.length / 2];
        for (int i = 0; i < ret.length; i++) {
            final int i2 = i << 1;
            final double Re = data[i2];
            final double Im = data[i2 + 1];

            ret[i] = (float) (10
                              * TMathConstants.Log10((TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im)) / ret.length));
        }

        if (truncateDCNyq) {
            // smooth spectra on both ends to minimise DC/Nyquist frequency
            // artifacts
            ret[0] = ret[1];
            ret[n2 - 1] = ret[n2 - 2];
        } else {
            // full DC/Nyquist frequency treatment
            ret[0] = data[0];
            ret[ret.length - 1] = data[1];
        }

        return ret;
    }

    /**
     * compute maxima
     *
     * @param data input data
     * @return maxima locations
     */
    public static double[][] computeMaxima(final double[] data) {
        final int n = data.length;
        final double[][] ret = new double[2][2];
        final double[] x = new double[n];
        final double[] y = new double[n];

        x[0] = 0;
        y[0] = data[1];
        int npeaks = 1;
        for (int i = 1; i < n - 1; i++) {
            if (data[i - 1] <= data[i] & data[i] >= data[i + 1]) {
                x[npeaks] = i;
                y[npeaks] = data[i];
                npeaks++;
            }
        }

        x[npeaks] = n - 1;
        y[npeaks] = data[n - 1];

        if (npeaks >= 3) {
            final double slope1 = (y[1] - y[2]) / (x[1] - x[2]);
            final double tmp1 = slope1 * (x[0] - x[1]) + y[1];
            if (tmp1 > y[0]) {
                y[0] = tmp1;
            }

            final double slope2 = (y[npeaks - 1] - y[npeaks - 2]) / (x[npeaks - 1] - x[npeaks - 2]);
            final double tmp2 = slope2 * (x[npeaks] - x[npeaks - 1]) + y[npeaks - 1];
            if (tmp2 > y[npeaks]) {
                y[npeaks] = tmp2;
            }
        }
        npeaks++;

        ret[0] = Arrays.copyOf(x, npeaks);
        ret[1] = Arrays.copyOf(y, npeaks);
        return ret;
    }

    /**
     * compute minima
     *
     * @param data input data
     * @return computed minima
     */
    public static double[][] computeMinima(final double[] data) {
        final int n = data.length;
        final double[][] ret = new double[2][2];
        final double[] x = new double[n];
        final double[] y = new double[n];

        x[0] = 0;
        y[0] = data[0];
        int npeaks = 1;
        for (int i = 2; i < n - 1; i++) {
            if (data[i - 1] >= data[i] && data[i] <= data[i + 1]) {
                x[npeaks] = i;
                y[npeaks] = data[i];
                npeaks++;
            }
        }

        x[npeaks] = n - 1;
        y[npeaks] = data[n - 1];

        if (npeaks >= 3) {
            final double slope1 = (y[1] - y[2]) / (x[1] - x[2]);
            final double tmp1 = slope1 * (x[0] - x[1]) + y[1];
            if (tmp1 < y[0]) {
                y[0] = tmp1;
            }

            final double slope2 = (y[npeaks - 1] - y[npeaks - 2]) / (x[npeaks - 1] - x[npeaks - 2]);
            final double tmp2 = slope2 * (x[npeaks] - x[npeaks - 1]) + y[npeaks - 1];

            if (tmp2 < y[npeaks]) {
                y[npeaks] = tmp2;
            }
        }
        npeaks++;

        ret[0] = Arrays.copyOf(x, npeaks);
        ret[1] = Arrays.copyOf(y, npeaks);
        return ret;
    }

    /**
     * compute phase spectra ([-PI,+PI])
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data
     * @return computed phase spectrum
     */
    public static double[] computePhaseSpectrum(final double[] data) {
        final double[] ret = new double[data.length / 2];
        for (int i = 0; i < ret.length; i++) {
            final int i2 = i << 1;
            final double Re = data[i2];
            final double Im = data[i2 + 1];

            ret[i] = TMathConstants.ATan2(Im, Re);
        }

        // smooth spectra on both ends to minimise DC/Nyquist frequency
        // artifacts
        ret[0] = ret[1];
        ret[ret.length - 1] = ret[ret.length - 2];

        return ret;
    }

    /**
     * compute phase spectra ([-PI,+PI])
     *
     * @see DoubleFFT_1D for the expected spectra layout
     * @param data the input data
     * @return phase spectrum
     */
    public static float[] computePhaseSpectrum(final float[] data) {
        final float[] ret = new float[data.length / 2];
        for (int i = 0; i < ret.length; i++) {
            final int i2 = i << 1;
            final double Re = data[i2];
            final double Im = data[i2 + 1];

            ret[i] = (float) TMathConstants.ATan2(Im, Re);
        }

        // smooth spectra on both ends to minimise DC/Nyquist frequency
        // artefacts
        ret[0] = ret[1];
        ret[ret.length - 1] = ret[ret.length - 2];

        return ret;
    }

    public static double[][] filterPeaksHarmonics(final double[][] peaks, final double[] magnitude, double estimate,
            final boolean useRealAmplitudes) {
        final double[] testDataX = new double[magnitude.length];
        final double[] testDataY = new double[magnitude.length];
        final double[][] ret = new double[2][];
        ret[0] = Arrays.copyOf(peaks[0], peaks[0].length);
        ret[1] = Arrays.copyOf(peaks[1], peaks[1].length);

        for (int i = 0; i < magnitude.length; i++) {
            testDataX[i] = i;
        }

        for (int i = 0; i < peaks[0].length; i++) {
            final int index = (int) peaks[0][i];
            if (index > 0) {
                if (useRealAmplitudes) {
                    testDataY[index - 1] = 0.5 * magnitude[index];
                    testDataY[index + 0] = 1.0 * magnitude[index];
                    testDataY[index + 1] = 0.5 * magnitude[index];
                } else {
                    testDataY[index - 1] = 0.5;
                    testDataY[index + 0] = 1.0;
                    testDataY[index + 1] = 0.5;
                }
            }
        }

        final CombFunction combFunction = new CombFunction("myCombFunction",
                new double[] { estimate, 1.0, 1.2 / (2 * magnitude.length) });
        combFunction.fixParameter(1, true);
        combFunction.fixParameter(2, true);

        final NonLinearRegressionFitter fitter = new NonLinearRegressionFitter(testDataX, testDataY);
        final double[] start = { estimate, 1.0, 1.5 / (2 * magnitude.length) };
        final double[] step = { 0.00001, 0.1, 1e-5 };
        fitter.simplex(combFunction, start, step);

        final double[] parameter = fitter.getBestEstimates();
        for (int i = 0; i < parameter.length; i++) {
            System.out.printf("parameter %d = %f\n", i, parameter[i]);
        }
        estimate = parameter[0];
        System.err.println("set estimate to " + estimate);
        // combFunction.setParameterValue(0, estimate);
        int count = 0;
        for (int i = 0; i < peaks[0].length; i++) {
            final double comb = combFunction.getValue(peaks[0][i]);
            // System.err.println("peak = " + peaks[0][i] + " -> " + comb);
            if (comb > 0) {
                ret[0][count] = peaks[0][i];
                ret[1][count] = peaks[1][i];
                count++;
            }
        }

        System.err.println("filtered " + count);
        ret[0] = Arrays.copyOf(ret[0], count);
        ret[1] = Arrays.copyOf(ret[1], count);
        return ret;
    }

    public static double[][] filterPeaksSignalToNoise(final double[][] peaks, final double snRatio,
            final boolean dBScale) {
        final double[][] ret = new double[2][peaks[0].length];
        final double[] x = peaks[0];
        final double[] y = peaks[1];
        int count = 0;
        final double max = TMath.Maximum(y);
        if (!dBScale) {
            for (int i = 0; i < peaks[0].length; i++) {
                if (y[i] > max / snRatio) {
                    ret[0][count] = x[i];
                    ret[1][count] = y[i];
                    count++;
                }
            }
        } else {
            for (int i = 0; i < peaks[0].length; i++) {
                if (y[i] > max - snRatio) {
                    ret[0][count] = x[i];
                    ret[1][count] = y[i];
                    count++;
                }
            }
        }
        ret[0] = Arrays.copyOf(ret[0], count);
        ret[1] = Arrays.copyOf(ret[1], count);
        return ret;
    }

    /**
     * interpolation using a bary-centre approach
     *
     * @param data data array
     * @param index 0&lt; index &lt; data.length, location of the to be interpolated peak
     * @return interpolated bary centre
     */
    public static double interpolateBaryCentre(final double[] data, final int index) {
        final double tresolution = 1.0 / (2.0 * data.length);

        if (index > 0 && index < data.length - 1) {
            double sum = Math.pow(data[index - 1], 1);
            sum += Math.pow(data[index - 0], 1);
            sum += Math.pow(data[index + 1], 1);

            double val = data[index - 1] * (index - 1);
            val += data[index + 0] * (index + 0);
            val += data[index + 1] * (index + 1);
            val /= sum;
            return val * tresolution;
        } else {
            return index * tresolution;
        }
    }

    /**
     * interpolation using a Gaussian interpolation
     *
     * @param data data array
     * @param index 0&lt; index &lt; data.length, location of the to be interpolated peak
     * @return interpolated gauss index
     */
    public static double interpolateGaussian(final double[] data, final int index) {
        final double tresolution = 1.0 / (2 * data.length);

        if (index > 0 && index < data.length - 1) {
            final double left = Math.pow(data[index - 1], 1);
            final double center = Math.pow(data[index - 0], 1);
            final double right = Math.pow(data[index + 1], 1);

            double val = index;
            val += 0.5 * Math.log(right / left) / Math.log(Math.pow(center, 2) / (left * right));
            return val * tresolution;
        } else {
            return index * tresolution;
        }
    }

    /**
     * interpolation using a NAFF/SUSSIX based approach
     *
     * @param data data array
     * @param index 0&lt; index &lt; data.length, location of the to be interpolated peak
     * @return NAFF-interpolated peak position
     */
    public static double interpolateNAFF(final double[] data, final int index) {
        final double val = index / (double) (2 * data.length);
        if (index > 0 && index < data.length - 1) {
            final double pin = TMathConstants.Pi() / data.length;
            final double left = Math.pow(data[index - 1], 1);
            final double center = Math.pow(data[index - 0], 1);
            final double right = Math.pow(data[index + 1], 1);

            if (left < right) {
                return val + TMathConstants.ATan2(right * TMathConstants.Sin(pin), center + right * TMathConstants.Cos(pin)) / TMathConstants.Pi();
            } else {
                return val
                        - TMathConstants.ATan2(left * TMathConstants.Sin(pin), center + left * TMathConstants.Cos(pin))
                                  / TMathConstants.Pi();
            }
        } else {
            return val;
        }
    }

    /**
     * interpolation using a parabolic interpolation
     *
     * @param data data array
     * @param index 0&lt; index &lt; data.length, location of the to be interpolated peak
     * @return parabolic-interpolated peak position
     */
    public static double interpolateParabolic(final double[] data, final int index) {
        final double tresolution = 1.0 / (2.0 * data.length);

        if (index > 0 && index < data.length - 1) {
            final double left = Math.pow(data[index - 1], 1);
            final double center = Math.pow(data[index - 0], 1);
            final double right = Math.pow(data[index + 1], 1);

            return (index + 0.5 * (right - left) / (2 * center - left - right)) * tresolution;
        } else {
            return data[index] * tresolution;
        }
    }

    public static synchronized double[] interpolateSpectrum(final double[] data, final int noversampling) {
        final double[] val1 = Arrays.copyOf(data, data.length);

        DoubleFFT_1D fft1D = new DoubleFFT_1D(data.length);
        fft1D.realInverse(val1, true);

        final double[] val2 = new double[noversampling * val1.length];
        System.arraycopy(val1, 0, val2, 0, val1.length - 2);

        final int fftLength = noversampling * data.length;
        fft1D = new DoubleFFT_1D(fftLength);
        fft1D.realForward(val2);

        for (int i = 0; i < val2.length; i++) {
            val2[i] *= noversampling;
        }

        return val2;
    }

    public static void main(final String[] args) {
        final double[] data = new double[1024];
        final double mean = 128.123456789123456789123456789; // [bins]
        final double sigma = 1.2; // [bins]

        for (int i = 0; i < 1024; i++) {
            final double x = i; // [bins]
            // Gaussian Peak
            data[i] = TMath.Gauss(x, mean, sigma, true);

            // Sinc Peak
            // data[i] = TMath.Abs(TMath.Sinc(TMath.Pi()*(x-mean)/sigma, true));

            // Cauchy-Lorentz or Breit-Wigner distribution (typical for
            // resonances)
            // data[i] = TMath.BreitWigner(x, mean, sigma);
        }

        final int lmax = (int) TMath.LocationMaximum(data, data.length);
        System.out.println("found highest peak at bin = " + lmax);
        final double interGauss = SpectrumTools.interpolateGaussian(data, lmax) * 2 * data.length;
        final double interBary = SpectrumTools.interpolateBaryCentre(data, lmax) * 2 * data.length;
        final double interPara = SpectrumTools.interpolateParabolic(data, lmax) * 2 * data.length;
        final double interNAFF = SpectrumTools.interpolateNAFF(data, lmax) * 2 * data.length;

        System.out.println(" ");
        System.out.printf("no interpolation                 f=%f [bins], abs. error = %e[bins]\n", (double) lmax,
                Math.abs(lmax - mean));
        System.out.printf("Gaussian peak frequency estimate f=%f [bins], abs. error = %e[bins]\n", interGauss,
                Math.abs(interGauss - mean));
        System.out.printf("Bary-centre frequency estimate   f=%f [bins], abs. error = %e[bins]\n", interBary,
                Math.abs(interBary - mean));
        System.out.printf("Parabolic frequency estimate     f=%f [bins], abs. error = %e[bins]\n", interPara,
                Math.abs(interPara - mean));
        System.out.printf("'NAFF'/'SUSSIX'-type estimate    f=%f [bins], abs. error = %e[bins]\n", interNAFF,
                Math.abs(interNAFF - mean));
        System.out.println(" ");
    }
}
