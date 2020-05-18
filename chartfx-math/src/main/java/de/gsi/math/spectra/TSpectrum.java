package de.gsi.math.spectra;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.spi.utils.DoublePoint;
import de.gsi.dataset.utils.ArrayCache;
import de.gsi.math.ArrayMath;
import de.gsi.math.ArrayUtils;
import de.gsi.math.TMath;

/**************************************************************************
 * Original Author: Miroslav Morhac 27/05/99 clean up/java port: Ralph Steinhagen 18/04/18 THIS CLASS CONTAINS ADVANCED
 * SPECTRA PROCESSING FUNCTIONS. ONE-DIMENSIONAL BACKGROUND ESTIMATION FUNCTIONS ONE-DIMENSIONAL SMOOTHING FUNCTIONS
 * ONE-DIMENSIONAL DECONVOLUTION FUNCTIONS ONE-DIMENSIONAL PEAK SEARCH FUNCTIONS These functions were written by:
 * Miroslav Morhac Institute of Physics Slovak Academy of Sciences Dubravska cesta 9, 842 28 BRATISLAVA SLOVAKIA
 * email:fyzimiro@savba.sk, fax:+421 7 54772479 The original code in C has been repackaged as a C++ class by R.Brun The
 * algorithms in this class have been published in the following references: [1] M.Morhac et al.: Background elimination
 * methods for multidimensional coincidence gamma-ray spectra. Nuclear Instruments and Methods in Physics Research A 401
 * (1997) 113-132. [2] M.Morhac et al.: Efficient one- and two-dimensional Gold deconvolution and its application to
 * gamma-ray spectra decomposition. Nuclear Instruments and Methods in Physics Research A 401 (1997) 385-408. [3]
 * M.Morhac et al.: Identification of peaks in multidimensional coincidence gamma-ray spectra. Nuclear Instruments and
 * Methods in Research Physics A 443(2000), 108-125. These NIM papers are also available as doc or ps files from:
 * ftp://root.cern.ch/root/Spectrum.doc
 * <p>
 * ftp://root.cern.ch/root/SpectrumDec.ps.gz ftp://root.cern.ch/root/SpectrumSrc.ps.gz
 * ftp://root.cern.ch/root/SpectrumBck.ps.gz The original source documentation as well as examples can be found at
 * http://root.cern.ch/
 */

public class TSpectrum {
    private static final Logger LOGGER = LoggerFactory.getLogger(TSpectrum.class);
    private static final String CACHED_ARRAY_BACKGROUND = "TSpectrum::background:workingSpace";
    private static final String CACHED_ARRAY_SMOOTH_MARKOV = "TSpectrum::smoothMarkov:workingSpace";
    private static final String CACHED_ARRAY_SEARCH = "TSpectrum::search:workingSpace";
    private static final String CACHED_ARRAY_DECONVOLUTION = "TSpectrum::deconvolution:workingSpace";
    private static final String CACHED_ARRAY_DECONVOLUTION_RL = "TSpectrum::deconvolutionLR:workingSpace";
    private static final String CACHED_ARRAY_UNFOLDING = "TSpectrum::unfolding:workingSpace";
    private static final int PEAK_WINDOW = 1024;
    protected int fgAverageWindow = 3; // Average window of searched peaks
    protected int fgIterations = 3; // Maximum number of decon iterations (default=3)

    protected int fMaxPeaks; // Maximum number of peaks to be found
    protected double fResolution; // resolution of the neighbouring peaks

    /**
     * The TSpectrum() default constructor
     */
    public TSpectrum() {
        final int n = 100;
        fMaxPeaks = n;
        fResolution = 1;
    }

    /**
     * @param maxpositions maximum number of peaks
     * @param resolution determines resolution of the neighboring peaks default value is 1 correspond to 3 sigma
     *        distance between peaks. Higher values allow higher resolution (smaller distance between peaks. May be set
     *        later through SetResolution.)
     */
    public TSpectrum(int maxpositions, double resolution) {
        int n = maxpositions;
        if (n <= 0) {
            n = 1;
        }
        fMaxPeaks = n;
        setResolution(resolution);
    }

    /**
     * @param w average window of searched peaks
     * @see #search
     */
    public void setAverageWindow(int w) {
        fgAverageWindow = w;
    }

    /**
     * @param n max number of decon iterations in deconvolution operation
     * @see #search
     */
    public void setDeconIterations(int n) {
        fgIterations = n;
    }

    /**
     * @param resolution determines resolution of the neighboring peaks default value is 1 correspond to 3 sigma
     *        distance between peaks. Higher values allow higher resolution (smaller distance between peaks. May be set
     *        later through SetResolution.
     */
    public void setResolution(double resolution) {
        if (resolution > 1) {
            fResolution = resolution;
        } else {
            fResolution = 1;
        }
    }

    /**
     * ONE-DIMENSIONAL BACKGROUND ESTIMATION FUNCTION - GENERAL FUNCTION This function calculates background spectrum
     * from source spectrum. The result is placed in the vector pointed by spectrum pointer.
     *
     * @param source vector of source spectrum
     * @param destination where to copy the filtered background
     * @param length length of the spectrum vector
     * @param numberIterations maximal width of clipping window,
     * @param direction direction of change of clipping window - possible values=kBackIncreasingWindow
     *        kBackDecreasingWindow
     * @param filterOrder order of clipping filter, -possible values=kBackOrder2 kBackOrder4 kBackOrder6 kBackOrder8
     * @param smoothing logical variable whether the smoothing operation in the estimation of background will be
     *        included resp. smoothing window
     * @param compton logical variable whether the estimation of Compton edge will be included - possible values=kFALSE
     *        kTRUE
     * @return filtered array, N.B. if destination is null or has insufficient length a new array is being allocated,
     *         otherwise calculations are done in-place.
     */
    public static double[] background(final double[] source, final double[] destination, int length,
            int numberIterations, Direction direction, FilterOrder filterOrder, SmoothWindow smoothing,
            boolean compton) {
        if (source == null || length <= 0 || source.length < length) {
            throw new InvalidParameterException("input spectrum null or invalid vector size");
        }
        if (numberIterations < 1) {
            throw new InvalidParameterException("width of clipping window must be positive, is: " + numberIterations);
        }
        if (length < 2 * numberIterations + 1) {
            throw new InvalidParameterException("clipping window is too large (length < 2 * numberIterations + 1) -> "
                                                + length + "< " + (2 * numberIterations + 1));
        }
        if (filterOrder == null) {
            throw new InvalidParameterException("filterOrder must not be null");
        }
        if (smoothing == null) {
            throw new InvalidParameterException("smoothing must not be null");
        }

        final double[] workingSpace = ArrayCache.getCachedDoubleArray(CACHED_ARRAY_BACKGROUND, 2 * length);
        System.arraycopy(source, 0, workingSpace, 0, length);
        System.arraycopy(source, 0, workingSpace, length, length);

        switch (filterOrder) {
        case ORDER_2:
            filterBackgroundOrder2(workingSpace, length, numberIterations, direction, smoothing);
            break;
        case ORDER_4: {
            filterBackgroundOrder4(workingSpace, length, numberIterations, direction, smoothing);
        } break;
        case ORDER_6: {
            filterBackgroundOrder6(workingSpace, length, numberIterations, direction, smoothing);
        } break;
        case ORDER_8:
        default: {
            filterBackgroundOrder8(workingSpace, length, numberIterations, direction, smoothing);
        } break;
        } /* switch (filterOrder) {...} */

        if (compton) {
            for (int i = 0; i < length; i++) {
                double a = workingSpace[i];
                double b = source[i];
                if (Math.abs(a - b) >= 1) {
                    int b1 = i - 1;
                    if (b1 < 0) {
                        b1 = 0;
                    }
                    double yb1 = workingSpace[b1];
                    double c = 0;
                    int priz = 0;
                    int b2;
                    for (b2 = b1 + 1; priz == 0 && b2 < length; b2++) {
                        a = workingSpace[b2];
                        b = source[b2];
                        c = c + b - yb1;
                        if (Math.abs(a - b) < 1) {
                            priz = 1;
                        }
                    }
                    if (b2 == length) {
                        b2 -= 1;
                    }
                    double yb2 = workingSpace[b2];
                    if (yb1 <= yb2) {
                        c = 0;
                        for (int j = b1; j <= b2; j++) {
                            b = source[j];
                            c = c + b - yb1;
                        }
                        if (c > 1) {
                            c = (yb2 - yb1) / c;
                            double d = 0;
                            for (int j = b1; j <= b2 && j < length; j++) {
                                b = source[j];
                                d = d + b - yb1;
                                a = c * d + yb1;
                                workingSpace[length + j] = a;
                            }
                        }
                    } else {
                        c = 0;
                        for (int j = b2; j >= b1; j--) {
                            b = source[j];
                            c = c + b - yb2;
                        }
                        if (c > 1) {
                            c = (yb1 - yb2) / c;
                            double d = 0;
                            //for (int j = b2; j >= b1 && j >= 0; j--) { // LGTM fix
                            for (int j = b2; j >= b1; j--) { // LGTM fix
                                b = source[j];
                                d = d + b - yb2;
                                a = c * d + yb2;
                                workingSpace[length + j] = a;
                            }
                        }
                    }
                    i = b2;
                }
            }
        } /* if (compton) {..} */

        final double[] returnVector = destination == null || destination.length < length ? new double[length]
                                                                                         : destination;
        System.arraycopy(workingSpace, 0, returnVector, 0, length);
        ArrayCache.release(CACHED_ARRAY_BACKGROUND, workingSpace);

        return returnVector;
    }

    /**
     * ONE-DIMENSIONAL DECONVOLUTION FUNCTION This function calculates deconvolution from source spectrum according to
     * response spectrum using Gold algorithm The result is placed in the vector pointed by destination pointer.
     *
     * @param source vector of source spectrum
     * @param response vector of response spectrum
     * @param destination vector of response spectrum
     * @param length length of source and response spectra
     * @param numberIterations for details we refer to the reference given below
     * @param numberRepetitions for repeated boosted deconvolution @boost, boosting coefficient M. Morhac, J. Kliman, V.
     *        Matousek, M. Veselsk, I. Turzo.: Efficient one- and two-dimensional Gold deconvolution and its application
     *        to gamma-ray spectra decomposition. NIM, A401 (1997) 385-408.
     * @param boost ???
     * @return filtered array, N.B. if destination is null or has insufficient length a new array is being allocated,
     *         otherwise calculations are done in-place.
     */
    public static double[] deconvolution(double[] source, double[] response, double[] destination, int length,
            int numberIterations, int numberRepetitions, double boost) {
        if (length <= 0) {
            throw new IllegalArgumentException("length '" + length + "'is <= 0");
        }
        if (numberRepetitions <= 0) {
            throw new IllegalArgumentException("numberRepetitions '" + length + "'is <= 0");
        }

        // working_space-pointer to the working vector (its size must be 4*length of source spectrum)
        final double[] workingSpace = ArrayCache.getCachedDoubleArray(CACHED_ARRAY_DECONVOLUTION, 4 * length);
        int i;
        int j;
        int k;
        int lindex;
        int posit;
        int lhGold;
        int l;
        int repet;
        double lda;
        double ldb;
        double ldc;
        double area;
        double maximum;
        area = 0;
        lhGold = -1;
        posit = 0;
        maximum = 0;

        // read response vector
        for (i = 0; i < length; i++) {
            lda = response[i];
            if (lda != 0) {
                lhGold = i + 1;
            }
            workingSpace[i] = lda;
            area += lda;
            if (lda > maximum) {
                maximum = lda;
                posit = i;
            }
        }
        if (lhGold == -1) {
            throw new IllegalArgumentException("ZERO RESPONSE VECTOR");
        }

        // read source vector
        for (i = 0; i < length; i++) {
            workingSpace[2 * length + i] = source[i];
        }

        // create matrix at*a and vector at*y
        for (i = 0; i < length; i++) {
            lda = 0;
            for (j = 0; j < length; j++) {
                ldb = workingSpace[j];
                k = i + j;
                if (k < length) {
                    ldc = workingSpace[k];
                    lda = lda + ldb * ldc;
                }
            }
            workingSpace[length + i] = lda;
            lda = 0;
            for (k = 0; k < length; k++) {
                l = k - i;
                if (l >= 0) {
                    ldb = workingSpace[l];
                    ldc = workingSpace[2 * length + k];
                    lda = lda + ldb * ldc;
                }
            }
            workingSpace[3 * length + i] = lda;
        }

        // move vector at*y
        for (i = 0; i < length; i++) {
            workingSpace[2 * length + i] = workingSpace[3 * length + i];
        }

        // initialization of resulting vector
        for (i = 0; i < length; i++) {
            workingSpace[i] = 1;
        }

        // **START OF ITERATIONS**
        for (repet = 0; repet < numberRepetitions; repet++) {
            if (repet != 0) {
                for (i = 0; i < length; i++) {
                    workingSpace[i] = Math.pow(workingSpace[i], boost);
                }
            }
            for (lindex = 0; lindex < numberIterations; lindex++) {
                for (i = 0; i < length; i++) {
                    if (workingSpace[2 * length + i] > 0.000001 && workingSpace[i] > 0.000001) {
                        lda = 0;
                        for (j = 0; j < lhGold; j++) {
                            ldb = workingSpace[j + length];
                            if (j == 0) {
                                ldc = workingSpace[i];
                            } else {
                                k = i + j;
                                ldc = 0;
                                if (k < length) {
                                    ldc = workingSpace[k];
                                }
                                k = i - j;
                                if (k >= 0) {
                                    ldc += workingSpace[k];
                                }
                            }

                            lda = lda + ldb * ldc;
                        }
                        ldb = workingSpace[2 * length + i];

                        if (lda != 0) {
                            lda = ldb / lda;
                        }

                        ldb = workingSpace[i];
                        lda *= ldb;
                        workingSpace[3 * length + i] = lda;
                    }
                }
                for (i = 0; i < length; i++) {
                    workingSpace[i] = workingSpace[3 * length + i];
                }
            }
        }

        // shift resulting spectrum
        for (i = 0; i < length; i++) {
            lda = workingSpace[i];
            j = i + posit;
            j = j % length;
            workingSpace[length + j] = lda;
        }

        final double[] returnVector = destination == null || destination.length < length ? new double[length]
                                                                                         : destination;
        System.arraycopy(workingSpace, 0, returnVector, 0, length);
        ArrayMath.multiplyInPlace(returnVector, area);
        ArrayCache.release(CACHED_ARRAY_DECONVOLUTION, workingSpace);
        return returnVector;
    }

    /**
     * ONE-DIMENSIONAL DECONVOLUTION FUNCTION This function calculates deconvolution from source spectrum according to
     * response spectrum using Richardson-Lucy algorithm The result is placed in the vector pointed by destination pointer.
     *
     * @param source vector of source spectrum
     * @param response vector of response spectrum
     * @param destination vector
     * @param length length of source and response spectra
     * @param numberIterations for details we refer to the reference given above
     * @param numberRepetitions for repeated boosted deconvolution
     * @param boost boosting coefficient
     * @return filtered array, N.B. if destination is null or has insufficient length a new array is being allocated,
     *         otherwise calculations are done in-place.
     */
    public static double[] deconvolutionRL(double[] source, double[] response, double[] destination, int length,
            int numberIterations, int numberRepetitions, double boost) {
        if (length <= 0) {
            throw new IllegalArgumentException("length '" + length + "'is <= 0");
        }
        if (numberRepetitions <= 0) {
            throw new IllegalArgumentException("numberRepetitions '" + length + "'is <= 0");
        }

        // working_space-pointer to the working vector
        // (its size must be 4*length of source spectrum)
        final double[] workingSpace = ArrayCache.getCachedDoubleArray(CACHED_ARRAY_DECONVOLUTION_RL, 4 * length);
        int i;
        int j;
        int k;
        int lindex;
        int posit;
        int lhGold;
        int repet;
        int kmin;
        int kmax;
        double lda;
        double ldb;
        double ldc;
        double maximum;
        lhGold = -1;
        posit = 0;
        maximum = 0;

        // read response vector
        for (i = 0; i < length; i++) {
            lda = response[i];
            if (lda != 0) {
                lhGold = i + 1;
            }
            workingSpace[length + i] = lda;
            if (lda > maximum) {
                maximum = lda;
                posit = i;
            }
        }

        if (lhGold == -1) {
            throw new IllegalArgumentException("ZERO RESPONSE VECTOR");
        }

        // read source vector
        for (i = 0; i < length; i++) {
            workingSpace[2 * length + i] = source[i];
        }

        // initialization of resulting vector
        for (i = 0; i < length; i++) {
            if (i <= length - lhGold) {
                workingSpace[i] = 1;
            } else {
                workingSpace[i] = 0;
            }
        }

        // **START OF ITERATIONS**
        for (repet = 0; repet < numberRepetitions; repet++) {
            if (repet != 0) {
                for (i = 0; i < length; i++) {
                    workingSpace[i] = Math.pow(workingSpace[i], boost);
                }
            }
            for (lindex = 0; lindex < numberIterations; lindex++) {
                for (i = 0; i <= length - lhGold; i++) {
                    lda = 0;
                    if (workingSpace[i] > 0) { // x[i]
                        for (j = i; j < i + lhGold; j++) {
                            ldb = workingSpace[2 * length + j]; // y[j]
                            if (j < length) {
                                if (ldb > 0) { // y[j]
                                    kmax = j;
                                    if (kmax > lhGold - 1) {
                                        kmax = lhGold - 1;
                                    }
                                    kmin = j + lhGold - length;
                                    if (kmin < 0) {
                                        kmin = 0;
                                    }
                                    ldc = 0;
                                    for (k = kmax; k >= kmin; k--) {
                                        ldc += workingSpace[length + k] * workingSpace[j - k]; // h[k]*x[j-k]
                                    }
                                    if (ldc > 0) {
                                        ldb /= ldc;
                                    } else {
                                        ldb = 0;
                                    }
                                }
                                ldb *= workingSpace[length + j - i]; // y[j]*h[j-i]/suma(h[j][k]x[k])
                            }
                            lda += ldb;
                        }
                        lda *= workingSpace[i];
                    }
                    workingSpace[3 * length + i] = lda;
                }
                for (i = 0; i < length; i++) {
                    workingSpace[i] = workingSpace[3 * length + i];
                }
            }
        }

        // shift resulting spectrum
        for (i = 0; i < length; i++) {
            lda = workingSpace[i];
            j = i + posit;
            j = j % length;
            workingSpace[length + j] = lda;
        }

        // write back resulting spectrum
        for (i = 0; i < length; i++) {
            source[i] = workingSpace[length + i];
        }
        final double[] returnVector = destination == null || destination.length < length ? new double[length]
                                                                                         : destination;
        System.arraycopy(workingSpace, 0, returnVector, 0, length);

        ArrayCache.release(CACHED_ARRAY_DECONVOLUTION_RL, workingSpace);
        return returnVector;
    }

    protected static void filterBackgroundOrder2(final double[] workingSpace, int length, int numberIterations,
            Direction direction, SmoothWindow smoothing) {
        double av;
        double men;
        int startIndex;
        if (direction == Direction.INCREASING) {
            startIndex = 1;
        } else {
            startIndex = numberIterations;
        }
        final int bw = (smoothing.getValue() - 1) / 2;
        do {
            for (int j = startIndex; j < length - startIndex; j++) {
                if (smoothing.isSmoothing()) {
                    // smoothing
                    double a = workingSpace[length + j];
                    av = 0;
                    men = 0;
                    for (int w = j - bw; w <= j + bw; w++) {
                        if (w >= 0 && w < length) {
                            av += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    av /= men;
                    double b = 0;
                    men = 0;
                    for (int w = j - startIndex - bw; w <= j - startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            b += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b /= men;
                    double c = 0;
                    men = 0;
                    for (int w = j + startIndex - bw; w <= j + startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            c += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c /= men;
                    b = (b + c) / 2;
                    if (b < a) {
                        av = b;
                    }
                    workingSpace[j] = av;
                } else {
                    // not smoothing
                    double a = workingSpace[length + j];
                    double b = (workingSpace[length + j - startIndex] + workingSpace[length + j + startIndex]) / 2.0;
                    if (b < a) {
                        a = b;
                    }
                    workingSpace[j] = a;
                }
            }
            for (int j = startIndex; j < length - startIndex; j++) {
                workingSpace[length + j] = workingSpace[j];
            }
            if (direction == Direction.INCREASING) {
                startIndex += 1;
            } else {
                startIndex -= 1;
            }
        } while (direction == Direction.INCREASING && startIndex <= numberIterations
                 || direction == Direction.DECREASING && startIndex >= 1);
    }

    protected static void filterBackgroundOrder4(final double[] workingSpace, final int length,
            final int numberIterations, final Direction direction, final SmoothWindow smoothing) {
        int startIndex;
        if (direction == Direction.INCREASING) {
            startIndex = 1;
        } else {
            startIndex = numberIterations;
        }
        final int bw = (smoothing.getValue() - 1) / 2;
        do {
            for (int j = startIndex; j < length - startIndex; j++) {
                if (smoothing.isSmoothing()) {
                    // smoothing
                    double a = workingSpace[length + j];
                    double av = 0;
                    double men = 0;
                    for (int w = j - bw; w <= j + bw; w++) {
                        if (w >= 0 && w < length) {
                            av += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    av /= men;
                    double b = 0;
                    men = 0;
                    for (int w = j - startIndex - bw; w <= j - startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            b += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b /= men;
                    double c = 0;
                    men = 0;
                    for (int w = j + startIndex - bw; w <= j + startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            c += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c /= men;
                    b = (b + c) / 2;
                    double ai = startIndex / 2.0;
                    double b4 = 0;
                    men = 0;
                    for (int w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            b4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b4 /= men;
                    double c4 = 0;
                    men = 0;
                    for (int w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            c4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c4 /= men;
                    double d4 = 0;
                    men = 0;
                    for (int w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            d4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    d4 /= men;
                    double e4 = 0;
                    men = 0;
                    for (int w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            e4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    e4 /= men;
                    b4 = (-b4 + 4 * c4 + 4 * d4 - e4) / 6;
                    if (b < b4) {
                        b = b4;
                    }
                    if (b < a) {
                        av = b;
                    }
                    workingSpace[j] = av;
                } else {
                    // not smoothing
                    double a = workingSpace[length + j];
                    double b = (workingSpace[length + j - startIndex] + workingSpace[length + j + startIndex]) / 2.0;
                    double c = 0;
                    double ai = startIndex / 2.0;
                    c -= workingSpace[length + j - (int) (2 * ai)] / 6;
                    c += 4 * workingSpace[length + j - (int) ai] / 6;
                    c += 4 * workingSpace[length + j + (int) ai] / 6;
                    c -= workingSpace[length + j + (int) (2 * ai)] / 6;
                    if (b < c) {
                        b = c;
                    }
                    if (b < a) {
                        a = b;
                    }
                    workingSpace[j] = a;
                }
            }
            for (int j = startIndex; j < length - startIndex; j++) {
                workingSpace[length + j] = workingSpace[j];
            }
            if (direction == Direction.INCREASING) {
                startIndex += 1;
            } else {
                startIndex -= 1;
            }
        } while (direction == Direction.INCREASING && startIndex <= numberIterations
                 || direction == Direction.DECREASING && startIndex >= 1);
    }

    protected static void filterBackgroundOrder6(final double[] workingSpace, int length, int numberIterations,
            Direction direction, SmoothWindow smoothing) {
        int startIndex;
        if (direction == Direction.INCREASING) {
            startIndex = 1;
        } else {
            startIndex = numberIterations;
        }
        final int bw = (smoothing.getValue() - 1) / 2;
        do {
            for (int j = startIndex; j < length - startIndex; j++) {
                if (smoothing.isSmoothing()) {
                    // smoothing
                    double a = workingSpace[length + j];
                    double av = 0;
                    double men = 0;
                    for (int w = j - bw; w <= j + bw; w++) {
                        if (w >= 0 && w < length) {
                            av += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    av /= men;
                    double b = 0;
                    men = 0;
                    for (int w = j - startIndex - bw; w <= j - startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            b += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b /= men;
                    double c = 0;
                    men = 0;
                    for (int w = j + startIndex - bw; w <= j + startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            c += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c /= men;
                    b = (b + c) / 2;
                    double ai = startIndex / 2.0;
                    double b4 = 0;
                    men = 0;
                    for (int w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            b4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b4 /= men;
                    double c4 = 0;
                    men = 0; // XX
                    for (int w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            c4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c4 /= men;
                    double d4 = 0;
                    men = 0; // XX
                    for (int w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            d4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    d4 /= men;
                    double e4 = 0;
                    men = 0; // XX
                    for (int w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            e4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    e4 /= men;
                    b4 = (-b4 + 4 * c4 + 4 * d4 - e4) / 6;
                    ai = startIndex / 3.0;
                    double b6 = 0;
                    men = 0; // XX
                    for (int w = j - (int) (3 * ai) - bw; w <= j - (int) (3 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            b6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b6 /= men;
                    double c6 = 0;
                    men = 0; // XX
                    for (int w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            c6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c6 /= men;
                    double d6 = 0;
                    men = 0; // XX
                    for (int w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            d6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    d6 /= men;
                    double e6 = 0;
                    men = 0; // XX
                    for (int w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            e6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    e6 /= men;
                    double f6 = 0;
                    men = 0;
                    for (int w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            f6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    f6 /= men;
                    double g6 = 0;
                    men = 0;
                    for (int w = j + (int) (3 * ai) - bw; w <= j + (int) (3 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            g6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    g6 /= men;
                    b6 = (b6 - 6 * c6 + 15 * d6 + 15 * e6 - 6 * f6 + g6) / 20;
                    if (b < b6) {
                        b = b6;
                    }
                    if (b < b4) {
                        b = b4;
                    }
                    if (b < a) {
                        av = b;
                    }
                    workingSpace[j] = av;
                } else {
                    // not smoothing
                    double a = workingSpace[length + j];
                    double b = (workingSpace[length + j - startIndex] + workingSpace[length + j + startIndex]) / 2.0;
                    double c = 0;
                    double ai = startIndex / 2.0;
                    c -= workingSpace[length + j - (int) (2 * ai)] / 6;
                    c += 4 * workingSpace[length + j - (int) ai] / 6;
                    c += 4 * workingSpace[length + j + (int) ai] / 6;
                    c -= workingSpace[length + j + (int) (2 * ai)] / 6;
                    double d = 0;
                    ai = startIndex / 3.0;
                    d += workingSpace[length + j - (int) (3 * ai)] / 20;
                    d -= 6 * workingSpace[length + j - (int) (2 * ai)] / 20;
                    d += 15 * workingSpace[length + j - (int) ai] / 20;
                    d += 15 * workingSpace[length + j + (int) ai] / 20;
                    d -= 6 * workingSpace[length + j + (int) (2 * ai)] / 20;
                    d += workingSpace[length + j + (int) (3 * ai)] / 20;
                    if (b < d) {
                        b = d;
                    }
                    if (b < c) {
                        b = c;
                    }
                    if (b < a) {
                        a = b;
                    }
                    workingSpace[j] = a;
                }
            }
            for (int j = startIndex; j < length - startIndex; j++) {
                workingSpace[length + j] = workingSpace[j];
            }

            if (direction == Direction.INCREASING) {
                startIndex += 1;
            } else {
                startIndex -= 1;
            }
        } while (direction == Direction.INCREASING && startIndex <= numberIterations
                 || direction == Direction.DECREASING && startIndex >= 1);
    }

    protected static void filterBackgroundOrder8(final double[] workingSpace, int length, int numberIterations,
            Direction direction, SmoothWindow smoothing) {
        int startIndex;
        if (direction == Direction.INCREASING) {
            startIndex = 1;
        } else {
            startIndex = numberIterations;
        }
        final int bw = (smoothing.getValue() - 1) / 2;
        do {
            for (int j = startIndex; j < length - startIndex; j++) {
                if (smoothing.isSmoothing()) {
                    // smoothing
                    double a = workingSpace[length + j];
                    double av = 0;
                    double men = 0;
                    for (int w = j - bw; w <= j + bw; w++) {
                        if (w >= 0 && w < length) {
                            av += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    av /= men;
                    double b = 0;
                    men = 0;
                    for (int w = j - startIndex - bw; w <= j - startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            b += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b /= men;
                    double c = 0;
                    men = 0;
                    for (int w = j + startIndex - bw; w <= j + startIndex + bw; w++) {
                        if (w >= 0 && w < length) {
                            c += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c /= men;
                    b = (b + c) / 2;
                    double ai = startIndex / 2.0;
                    double b4 = 0;
                    men = 0;
                    for (int w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            b4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b4 /= men;
                    double c4 = 0;
                    men = 0;
                    for (int w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            c4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c4 /= men;
                    double d4 = 0;
                    men = 0;
                    for (int w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            d4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    d4 /= men;
                    double e4 = 0;
                    men = 0;
                    for (int w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            e4 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    e4 /= men;
                    b4 = (-b4 + 4 * c4 + 4 * d4 - e4) / 6;
                    ai = startIndex / 3.0;
                    double b6 = 0;
                    men = 0;
                    for (int w = j - (int) (3 * ai) - bw; w <= j - (int) (3 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            b6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b6 /= men;
                    double c6 = 0;
                    men = 0;
                    for (int w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            c6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c6 /= men;
                    double d6 = 0;
                    men = 0;
                    for (int w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            d6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    d6 /= men;
                    double e6 = 0;
                    men = 0;
                    for (int w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            e6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    e6 /= men;
                    double f6 = 0;
                    men = 0;
                    for (int w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            f6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    f6 /= men;
                    double g6 = 0;
                    men = 0;
                    for (int w = j + (int) (3 * ai) - bw; w <= j + (int) (3 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            g6 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    g6 /= men;
                    b6 = (b6 - 6 * c6 + 15 * d6 + 15 * e6 - 6 * f6 + g6) / 20;
                    ai = startIndex / 4.0;
                    double b8 = 0;
                    men = 0;
                    for (int w = j - (int) (4 * ai) - bw; w <= j - (int) (4 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            b8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    b8 /= men;
                    double c8 = 0;
                    men = 0;
                    for (int w = j - (int) (3 * ai) - bw; w <= j - (int) (3 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            c8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    c8 /= men;
                    double d8 = 0;
                    men = 0;
                    for (int w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            d8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    d8 /= men;
                    double e8 = 0;
                    men = 0;
                    for (int w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            e8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    e8 /= men;
                    double f8 = 0;
                    men = 0;
                    for (int w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                        if (w >= 0 && w < length) {
                            f8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    f8 /= men;
                    double g8 = 0;
                    men = 0;
                    for (int w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            g8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    g8 /= men;
                    double h8 = 0;
                    men = 0;
                    for (int w = j + (int) (3 * ai) - bw; w <= j + (int) (3 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            h8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    h8 /= men;
                    double i8 = 0;
                    men = 0;
                    for (int w = j + (int) (4 * ai) - bw; w <= j + (int) (4 * ai) + bw; w++) {
                        if (w >= 0 && w < length) {
                            i8 += workingSpace[length + w];
                            men += 1;
                        }
                    }
                    i8 /= men;
                    b8 = (-b8 + 8 * c8 - 28 * d8 + 56 * e8 - 56 * f8 - 28 * g8 + 8 * h8 - i8) / 70;
                    if (b < b8) {
                        b = b8;
                    }
                    if (b < b6) {
                        b = b6;
                    }
                    if (b < b4) {
                        b = b4;
                    }
                    if (b < a) {
                        av = b;
                    }
                    workingSpace[j] = av;
                } else {
                    // not smoothing
                    double a = workingSpace[length + j];
                    double b = (workingSpace[length + j - startIndex] + workingSpace[length + j + startIndex]) / 2.0;
                    double c = 0;
                    double ai = startIndex / 2.0;
                    c -= workingSpace[length + j - (int) (2 * ai)] / 6;
                    c += 4 * workingSpace[length + j - (int) ai] / 6;
                    c += 4 * workingSpace[length + j + (int) ai] / 6;
                    c -= workingSpace[length + j + (int) (2 * ai)] / 6;
                    double d = 0;
                    ai = startIndex / 3.0;
                    d += workingSpace[length + j - (int) (3 * ai)] / 20;
                    d -= 6 * workingSpace[length + j - (int) (2 * ai)] / 20;
                    d += 15 * workingSpace[length + j - (int) ai] / 20;
                    d += 15 * workingSpace[length + j + (int) ai] / 20;
                    d -= 6 * workingSpace[length + j + (int) (2 * ai)] / 20;
                    d += workingSpace[length + j + (int) (3 * ai)] / 20;
                    double e = 0;
                    ai = startIndex / 4.0;
                    e -= workingSpace[length + j - (int) (4 * ai)] / 70;
                    e += 8 * workingSpace[length + j - (int) (3 * ai)] / 70;
                    e -= 28 * workingSpace[length + j - (int) (2 * ai)] / 70;
                    e += 56 * workingSpace[length + j - (int) ai] / 70;
                    e += 56 * workingSpace[length + j + (int) ai] / 70;
                    e -= 28 * workingSpace[length + j + (int) (2 * ai)] / 70;
                    e += 8 * workingSpace[length + j + (int) (3 * ai)] / 70;
                    e -= workingSpace[length + j + (int) (4 * ai)] / 70;
                    if (b < e) {
                        b = e;
                    }
                    if (b < d) {
                        b = d;
                    }
                    if (b < c) {
                        b = c;
                    }
                    if (b < a) {
                        a = b;
                    }
                    workingSpace[j] = a;
                }
            }
            for (int j = startIndex; j < length - startIndex; j++) {
                workingSpace[length + j] = workingSpace[j];
            }

            if (direction == Direction.INCREASING) {
                startIndex += 1;
            } else {
                startIndex -= 1;
            }
        } while (direction == Direction.INCREASING && startIndex <= numberIterations
                 || direction == Direction.DECREASING && startIndex >= 1);
    }

    // STATIC functions (called by TH1)

    /**
     * ONE-DIMENSIONAL HIGH-RESOLUTION PEAK SEARCH FUNCTION This function searches for peaks in source spectrum It is
     * based on deconvolution method. First the background is removed (if desired), then Markov spectrum is calculated
     * (if desired), then the response function is generated according to given sigma and deconvolution is carried out.
     *
     * @param sourceX vector of source spectrum
     * @param sourceY vector of source spectrum
     * @param destVector vector of resulting deconvolved spectrum
     * @param length length of source spectrum
     * @param nMaxPeaks maximum number of peaks to search for (upper bound)
     * @param sigma sigma of searched peaks, for details we refer to manual
     * @param threshold threshold value in % for selected peaks, peaks with amplitude less than
     *        threshold*highest_peak/100 are ignored, see manual
     * @param backgroundRemove logical variable, set if the removal of background before deconvolution is desired
     * @param deconIterations number of iterations in deconvolution operation
     * @param markov logical variable, if it is true, first the source spectrum is replaced by new spectrum calculated
     *        using Markov chains method.
     * @param averWindow averaging window of searched peaks, for details we refer to manual (applies only for Markov
     *        method)
     * @return list with identified peaks
     */
    public static List<DoublePoint> search(final double[] sourceX, final double[] sourceY, final double[] destVector,
            final int length, final int nMaxPeaks, final double sigma, final double threshold,
            final boolean backgroundRemove, final int deconIterations, final boolean markov, final int averWindow) {
        if (sourceX == null || sourceY == null) {
            throw new IllegalArgumentException("neither sourceX '" + (sourceX == null ? "null" : "OK") + "' nor sourceY '" + (sourceY == null ? "null" : "OK") + "' must be null");
        } else if (sourceX.length < length) {
            throw new IllegalArgumentException(
                    "sourceX.length too short is '" + sourceX.length + "' vs. should '" + length + "'");
        } else if (sourceY.length < length) {
            throw new IllegalArgumentException(
                    "sourceY.length too short is '" + sourceY.length + "' vs. should '" + length + "'");
        }

        if ((destVector == null || destVector.length < length) && LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(destVector == null ? Double.NaN : destVector.length).addArgument(length).log("destination vector has insufficient length {} vs. {} needed, omitting copying background spectrum");
        }

        if (sigma < 1) {
            throw new IllegalArgumentException("Invalid sigma '" + sigma + "', must be greater than or equal to 1");
        }

        if (threshold <= 0 || threshold >= 100) {
            throw new IllegalArgumentException(
                    "invalid threshold '" + threshold + "', must be within ]0,100[");
        }

        if ((int) (5.0 * sigma + 0.5) >= PEAK_WINDOW / 2) {
            throw new IllegalArgumentException("too large sigma");
        }

        if (markov && averWindow <= 0) {
            throw new IllegalArgumentException("averanging window must be positive");
        }

        final int numberIterations = (int) (7 * sigma + 0.5);
        if (backgroundRemove && length < 2 * numberIterations + 1) {
            throw new IllegalArgumentException("too large clipping window");
        }
        final int sizeExt = length + 2 * numberIterations;
        final int shift = numberIterations;

        final int searchK = (int) (2 * sigma + 0.5);
        double l1low = 0;
        if (searchK >= 2) {
            double m0low = 0;
            double m1low = 0;
            double m2low = 0;
            double l0low = 0;
            for (int i = 0; i < searchK; i++) {
                final int a = i;
                double b = sourceY[i];
                m0low += 1;
                m1low += a;
                m2low += a * a;
                l0low += b;
                l1low += a * b;
            }
            final double detlow = m0low * m2low - m1low * m1low;
            if (detlow == 0) {
                l1low = 0;
            } else {
                l1low = (-l0low * m1low + l1low * m0low) / detlow;
            }
            if (l1low > 0) {
                l1low = 0;
            }
        } else {
            l1low = 0;
        }

        int nWidthSigma = (int) (7 * sigma + 0.5) * 2;
        final double[] workingSpace = ArrayCache.getCachedDoubleArray(CACHED_ARRAY_SEARCH, 7 * (length + nWidthSigma));
        ArrayUtils.fillArray(workingSpace, 0.0);

        for (int i = 0; i < sizeExt; i++) {
            if (i < shift) {
                final int a = i - shift;
                workingSpace[i + sizeExt] = sourceY[0] + l1low * a;
                if (workingSpace[i + sizeExt] < 0) {
                    workingSpace[i + sizeExt] = 0;
                }
            } else if (i >= length + shift) {
                workingSpace[i + sizeExt] = sourceY[length - 1];
                if (workingSpace[i + sizeExt] < 0) {
                    workingSpace[i + sizeExt] = 0;
                }
            } else {
                workingSpace[i + sizeExt] = sourceY[i - shift];
            }
        }

        if (backgroundRemove) {
            for (int i = 1; i <= numberIterations; i++) {
                for (int j = i; j < sizeExt - i; j++) {
                    if (markov) {
                        final double a = workingSpace[sizeExt + j];
                        double av = 0;
                        double men = 0;
                        final int bw = Math.min(averWindow / 2, 2);
                        for (int w = j - bw; w <= j + bw; w++) {
                            if (w >= 0 && w < sizeExt) {
                                av += workingSpace[sizeExt + w];
                                men += 1;
                            }
                        }
                        av /= men;
                        double b = 0;
                        men = 0;
                        for (int w = j - i - bw; w <= j - i + bw; w++) {
                            if (w >= 0 && w < sizeExt) {
                                b += workingSpace[sizeExt + w];
                                men += 1;
                            }
                        }
                        b /= men;
                        double c = 0;
                        men = 0;
                        for (int w = j + i - bw; w <= j + i + bw; w++) {
                            if (w >= 0 && w < sizeExt) {
                                c += workingSpace[sizeExt + w];
                                men += 1;
                            }
                        }
                        c /= men;
                        b = (b + c) / 2;
                        if (b < a) {
                            av = b;
                        }
                        workingSpace[j] = av;
                    } else {
                        double a = workingSpace[sizeExt + j];
                        final double b = (workingSpace[sizeExt + j - i] + workingSpace[sizeExt + j + i]) / 2.0;
                        if (b < a) {
                            a = b;
                        }

                        workingSpace[j] = a;
                    }
                }
                for (int j = i; j < sizeExt - i; j++) {
                    workingSpace[sizeExt + j] = workingSpace[j];
                }
            }
            for (int j = 0; j < sizeExt; j++) {
                if (j < shift) {
                    final int a = j - shift;
                    double b = sourceY[0] + l1low * a;
                    if (b < 0) {
                        b = 0;
                    }
                    workingSpace[sizeExt + j] = b - workingSpace[sizeExt + j];
                }

                else if (j >= length + shift) {
                    double b = sourceY[length - 1];
                    if (b < 0) {
                        b = 0;
                    }
                    workingSpace[sizeExt + j] = b - workingSpace[sizeExt + j];
                }

                else {
                    workingSpace[sizeExt + j] = sourceY[j - shift] - workingSpace[sizeExt + j];
                }
            }
            for (int j = 0; j < sizeExt; j++) {
                if (workingSpace[sizeExt + j] < 0) {
                    workingSpace[sizeExt + j] = 0;
                }
            }
        }

        for (int i = 0; i < sizeExt; i++) {
            workingSpace[i + 6 * sizeExt] = workingSpace[i + sizeExt];
        }

        if (markov) {
            for (int j = 0; j < sizeExt; j++) {
                workingSpace[2 * sizeExt + j] = workingSpace[sizeExt + j];
            }

            double signalMax = 0;
            double plocha = 0.0;
            for (int i = 0; i < sizeExt; i++) {
                workingSpace[i] = 0;
                if (signalMax < workingSpace[2 * sizeExt + i]) {
                    signalMax = workingSpace[2 * sizeExt + i];
                }
                plocha += workingSpace[2 * sizeExt + i];
            }
            if (signalMax == 0) {
                ArrayCache.release(CACHED_ARRAY_SEARCH, workingSpace);
                return Collections.emptyList();
            }

            double nom = 1;
            final int xmin = 0;
            final int xmax = sizeExt - 1;
            workingSpace[xmin] = 1;
            for (int i = xmin; i < xmax; i++) {
                double nip = workingSpace[2 * sizeExt + i] / signalMax;
                double nim = workingSpace[2 * sizeExt + i + 1] / signalMax;
                double sp = 0;
                double sm = 0;
                double a;
                for (int l = 1; l <= averWindow; l++) {
                    if ((i + l) > xmax) {
                        a = workingSpace[2 * sizeExt + xmax] / signalMax;
                    } else {
                        a = workingSpace[2 * sizeExt + i + l] / signalMax;
                    }

                    double b = a - nip;
                    if (a + nip <= 0) {
                        a = 1;
                    } else {
                        a = Math.sqrt(a + nip);
                    }

                    b /= a;
                    b = Math.exp(b);
                    sp += b;
                    if ((i - l + 1) < xmin) {
                        a = workingSpace[2 * sizeExt + xmin] / signalMax;
                    } else {
                        a = workingSpace[2 * sizeExt + i - l + 1] / signalMax;
                    }

                    b = a - nim;
                    if (a + nim <= 0) {
                        a = 1;
                    } else {
                        a = Math.sqrt(a + nim);
                    }

                    b /= a;
                    b = Math.exp(b);
                    sm += b;
                }
                a = sp / sm;
                a = workingSpace[i + 1] = workingSpace[i] * a;
                nom += a;
            }
            for (int i = xmin; i <= xmax; i++) {
                workingSpace[i] = workingSpace[i] / nom;
            }
            for (int j = 0; j < sizeExt; j++) {
                workingSpace[sizeExt + j] = workingSpace[j] * plocha;
            }
            for (int j = 0; j < sizeExt; j++) {
                workingSpace[2 * sizeExt + j] = workingSpace[sizeExt + j];
            }
            if (backgroundRemove) {
                for (int i = 1; i <= numberIterations; i++) {
                    for (int j = i; j < sizeExt - i; j++) {
                        double a = workingSpace[sizeExt + j];
                        double b = (workingSpace[sizeExt + j - i] + workingSpace[sizeExt + j + i]) / 2.0;
                        if (b < a) {
                            a = b;
                        }
                        workingSpace[j] = a;
                    }
                    for (int j = i; j < sizeExt - i; j++) {
                        workingSpace[sizeExt + j] = workingSpace[j];
                    }
                }
                for (int j = 0; j < sizeExt; j++) {
                    workingSpace[sizeExt + j] = workingSpace[2 * sizeExt + j] - workingSpace[sizeExt + j];
                }
            }
        }

        // deconvolution starts
        double area = 0;
        int lhGold = -1;
        int posit = 0;
        double maximum = 0;

        // generate response vector
        for (int i = 0; i < sizeExt; i++) {
            double lda = i - 3 * sigma;
            lda = lda * lda / (2 * sigma * sigma);
            lda = (int) (1000 * Math.exp(-lda));
            if (lda != 0) {
                lhGold = i + 1;
            }

            workingSpace[i] = lda;
            area += lda;
            if (lda > maximum) {
                maximum = lda;
                posit = i;
            }
        }

        // read source vector
        for (int i = 0; i < sizeExt; i++) {
            workingSpace[2 * sizeExt + i] = Math.abs(workingSpace[sizeExt + i]);
        }

        {
            // create matrix at*a(vector b)
            int startIndex = lhGold - 1;
            if (startIndex > sizeExt) {
                startIndex = sizeExt;
            }

            for (int i = -startIndex; i <= startIndex; i++) {
                double lda = 0;
                int jmin = 0;
                if (i < 0) {
                    jmin = -i;
                }
                int jmax = lhGold - 1 - i;
                if (jmax > (lhGold - 1)) {
                    jmax = lhGold - 1;
                }

                for (int j = jmin; j <= jmax; j++) {
                    final double ldb = workingSpace[j];
                    final double ldc = workingSpace[i + j];
                    lda = lda + ldb * ldc;
                }
                workingSpace[sizeExt + i + startIndex] = lda;
            }
        }

        {
            // create vector p
            int startIndex = lhGold - 1;
            for (int i = -startIndex; i <= sizeExt + startIndex - 1; i++) {
                double lda = 0;
                for (int j = 0; j <= (lhGold - 1); j++) {
                    final double ldb = workingSpace[j];
                    final int k = i + j;
                    if (k >= 0 && k < sizeExt) {
                        final double ldc = workingSpace[2 * sizeExt + k];
                        lda = lda + ldb * ldc;
                    }
                }
                workingSpace[4 * sizeExt + i + startIndex] = lda;
            }

            // move vector p
            for (int i = -startIndex; i <= sizeExt + startIndex - 1; i++) {
                workingSpace[2 * sizeExt + i + startIndex] = workingSpace[4 * sizeExt + i + startIndex];
            }
        }

        // initialization of resulting vector
        for (int i = 0; i < sizeExt; i++) {
            workingSpace[i] = 1;
        }

        // START OF ITERATIONS
        for (int lindex = 0; lindex < deconIterations; lindex++) {
            for (int i = 0; i < sizeExt; i++) {
                if (Math.abs(workingSpace[2 * sizeExt + i]) > 0.00001 && Math.abs(workingSpace[i]) > 0.00001) {
                    double lda = 0;
                    int jmin = lhGold - 1;
                    if (jmin > i) {
                        jmin = i;
                    }

                    jmin = -jmin;
                    int jmax = lhGold - 1;
                    if (jmax > (sizeExt - 1 - i)) {
                        jmax = sizeExt - 1 - i;
                    }

                    for (int j = jmin; j <= jmax; j++) {
                        final double ldb = workingSpace[j + lhGold - 1 + sizeExt];
                        final double ldc = workingSpace[i + j];
                        lda = lda + ldb * ldc;
                    }
                    double ldb = workingSpace[2 * sizeExt + i];
                    if (lda != 0) {
                        lda = ldb / lda;
                    }

                    ldb = workingSpace[i];
                    lda *= ldb;
                    workingSpace[3 * sizeExt + i] = lda;
                }
            }
            for (int i = 0; i < sizeExt; i++) {
                workingSpace[i] = workingSpace[3 * sizeExt + i];
            }
        }

        // shift resulting spectrum
        for (int i = 0; i < sizeExt; i++) {
            final int j = (i + posit) % sizeExt;
            workingSpace[sizeExt + j] = workingSpace[i];
        }

        // write back resulting spectrum
        maximum = 0;
        double maximumDecon = 0;
        final int stop = lhGold - 1;
        for (int i = 0; i < sizeExt - stop; i++) {
            if (i >= shift && i < length + shift) {
                workingSpace[i] = area * workingSpace[sizeExt + i + stop];
                if (maximumDecon < workingSpace[i]) {
                    maximumDecon = workingSpace[i];
                }
                if (maximum < workingSpace[6 * sizeExt + i]) {
                    maximum = workingSpace[6 * sizeExt + i];
                }
            } else {
                workingSpace[i] = 0;
            }
        }
        double lda = 1;
        if (lda > threshold) {
            lda = threshold;
        }
        lda /= 100;

        // searching for peaks in de-convolved spectrum
        List<Integer> peakList = new ArrayList<>();
        int peakIndex = 0;
        for (int i = 1; i < sizeExt - 1; i++) {
            final boolean condition2 = workingSpace[i] > workingSpace[i - 1] && workingSpace[i] > workingSpace[i + 1]
                                       && i >= shift && i < length + shift && workingSpace[i] > lda * maximumDecon
                                       && workingSpace[6 * sizeExt + i] > threshold * maximum / 100.0;
            if (condition2) {
                double a = 0;
                double b = 0;
                for (int j = i - 1; j <= i + 1; j++) {
                    a += (j - shift) * workingSpace[j];
                    b += workingSpace[j];
                }
                a /= b;
                if (a < 0) {
                    a = 0;
                }

                if (a >= length) {
                    a = length - 1;
                }
                if (peakIndex == 0) {
                    peakList.add((int) a);
                    peakIndex = 1;
                } else {
                    int priz = 0;
                    int searchIndex;
                    for (searchIndex = 0; searchIndex < peakIndex && priz == 0; searchIndex++) {
                        if (workingSpace[6 * sizeExt + shift + (int) a] > workingSpace[6 * sizeExt + shift + peakList.get(searchIndex)]) {
                            priz = 1;
                        }
                    }
                    if (priz == 0) {
                        if (searchIndex < nMaxPeaks) {
                            if (peakList.size() > searchIndex) {
                                peakList.set(searchIndex, (int) a);
                            } else {
                                peakList.add((int) a);
                            }
                        }
                    } else {
                        for (int k = peakIndex; k >= searchIndex; k--) {
                            if (k < nMaxPeaks) {
                                final Integer prevPoint = peakList.get(k - 1);
                                if (peakList.size() > k) {
                                    peakList.set(k, prevPoint);
                                } else {
                                    peakList.add(prevPoint);
                                }
                            }
                        }
                        peakList.set(searchIndex - 1, (int) a); // NOPMD
                    }
                    if (peakIndex < nMaxPeaks) {
                        peakIndex += 1;
                    }
                }
            }
        }

        if (destVector != null && destVector.length >= length) {
            System.arraycopy(workingSpace, shift, destVector, 0, length);
        }

        ArrayCache.release(CACHED_ARRAY_SEARCH, workingSpace);
        if (peakIndex == nMaxPeaks && LOGGER.isWarnEnabled()) {
            LOGGER.atWarn().addArgument(nMaxPeaks).log("maximum specified number of peaks limit reached {}");
        }

        return peakList.stream().map(p -> new DoublePoint(sourceX[p], sourceY[p])).collect(Collectors.toList());
    }

    /**
     * ONE-DIMENSIONAL MARKOV SPECTRUM SMOOTHING FUNCTION This function calculates smoothed spectrum from source
     * spectrum based on Markov chain method. The result is placed in the array pointed by source pointer.
     *
     * @param source array of source spectrum
     * @param destination array where to store the filtered background
     * @param length length of source array
     * @param averWindow width of averaging smoothing window
     * @return filtered array, N.B. if destination is null or has insufficient length a new array is being allocated,
     *         otherwise calculations are done in-place.
     */
    public static double[] smoothMarkov(final double[] source, final double[] destination, final int length,
            final int averWindow) {
        if (source == null || source.length < length) {
            throw new IllegalArgumentException("source must not be null or length smaller than '" + length + "'");
        }
        if (averWindow <= 0) {
            throw new IllegalArgumentException("averaging window must be positive");
        }

        final double[] workingSpace = ArrayCache.getCachedDoubleArray(CACHED_ARRAY_SMOOTH_MARKOV, length);
        ArrayUtils.fillArray(workingSpace, 0.0);

        final double sourceMax = TMath.Maximum(source, length);
        if (sourceMax == -Double.MAX_VALUE) {
            throw new IllegalArgumentException("source vector is not finite, could not find maximum");
        }
        final double sourceMin = TMath.Minimum(source, length);
        if (sourceMin == Double.MAX_VALUE) {
            throw new IllegalArgumentException("source vector is not finite, could not find minimum");
        }

        final int xmin = 0;
        final int xmax = length - 1;
        double nom = 1;
        workingSpace[xmin] = 1;
        for (int i = xmin; i < xmax; i++) {
            double nip = source[i] / sourceMax;
            double nim = source[i + 1] / sourceMax;
            double sp = 0;
            double sm = 0;
            double a;
            for (int l = 1; l <= averWindow; l++) {
                double b;
                if ((i + l) > xmax) {
                    a = source[xmax] / sourceMax;
                } else {
                    a = source[i + l] / sourceMax;
                }
                b = a - nip;

                if (a + nip <= 0) {
                    a = 1;
                } else {
                    a = Math.sqrt(a + nip);
                }

                b /= a;
                b = Math.exp(b);
                sp += b;
                if ((i - l + 1) < xmin) {
                    a = source[xmin] / sourceMax;
                } else {
                    a = source[i - l + 1] / sourceMax;
                }

                b = a - nim;
                if (a + nim <= 0) {
                    a = 1;
                } else {
                    a = Math.sqrt(a + nim);
                }

                b /= a;
                b = Math.exp(b);
                sm += b;
            }
            a = sp / sm;
            a = workingSpace[i + 1] = workingSpace[i] * a;
            nom += a;
        }

        double area = 0.0;
        for (int i = 0; i < length; i++) {
            area += source[i];
        }
        ArrayMath.multiplyInPlace(workingSpace, area / nom);

        ArrayCache.release(CACHED_ARRAY_SMOOTH_MARKOV, workingSpace);
        final double[] returnVector = destination == null || destination.length < length ? new double[length]
                                                                                         : destination;
        System.arraycopy(workingSpace, 0, returnVector, 0, length);

        return returnVector;
    }

    /**
     * ONE-DIMENSIONAL UNFOLDING FUNCTION This function unfolds source spectrum according to response matrix columns.
     * The result is placed in the vector pointed by source pointer.
     *
     * @param source vector of source spectrum
     * @param respMatrix matrix of response spectra
     * @param destination vector of unfolded source spectrum
     * @param lengthx length of source spectrum and # of columns of response matrix
     * @param lengthy length of destination spectrum and # of rows of response matrix
     * @param numberIterations Note!!! lengthx must be &gt;= lengthy
     * @param numberRepetitions repetition count
     * @param boost boosting coefficient
     * @return filtered array, N.B. if destination is null or has insufficient length a new array is being allocated,
     *         otherwise calculations are done in-place.
     */
    public static double[] unfolding(double[] source, double[][] respMatrix, final double[] destination, int lengthx,
            int lengthy, int numberIterations, int numberRepetitions, double boost) {
        int lindex;
        int lhx = 0;
        int repet;
        double lda;
        double ldb;
        double ldc;
        double area;
        if (lengthx <= 0 || lengthy <= 0) {
            throw new IllegalArgumentException("lengthx:" + lengthx + " lengthy" + lengthx + "<=0");
        }
        if (lengthx < lengthy) {
            throw new IllegalArgumentException(
                    "lengthx:" + lengthx + " must be greater than lengthy" + lengthx + "<=0");
        }
        if (numberIterations <= 0) {
            throw new IllegalArgumentException("Number of iterations must be positive: is" + numberIterations);
        }

        final int workSpaceSize = lengthx * lengthy + 2 * lengthy * lengthy + 4 * lengthx;
        final double[] workingSpace = ArrayCache.getCachedDoubleArray(CACHED_ARRAY_UNFOLDING, workSpaceSize);

        /* read response matrix */
        for (int j = 0; j < lengthy && lhx != -1; j++) {
            area = 0;
            lhx = -1;
            for (int i = 0; i < lengthx; i++) {
                lda = respMatrix[j][i];
                if (lda != 0) {
                    lhx = i + 1;
                }
                workingSpace[j * lengthx + i] = lda;
                area += lda;
            }
            if (lhx != -1) {
                for (int i = 0; i < lengthx; i++) {
                    workingSpace[j * lengthx + i] /= area;
                }
            }
        }

        if (lhx == -1) {
            throw new IllegalArgumentException("ZERO COLUMN IN RESPONSE MATRIX");
        }

        /* read source vector */
        for (int i = 0; i < lengthx; i++) {
            workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 2 * lengthx + i] = source[i];
        }

        /* create matrix at*a + at*y */
        for (int i = 0; i < lengthy; i++) {
            for (int j = 0; j < lengthy; j++) {
                lda = 0;
                for (int k = 0; k < lengthx; k++) {
                    ldb = workingSpace[lengthx * i + k];
                    ldc = workingSpace[lengthx * j + k];
                    lda = lda + ldb * ldc;
                }
                workingSpace[lengthx * lengthy + lengthy * i + j] = lda;
            }
            lda = 0;
            for (int k = 0; k < lengthx; k++) {
                ldb = workingSpace[lengthx * i + k];
                ldc = workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 2 * lengthx + k];
                lda = lda + ldb * ldc;
            }
            workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 3 * lengthx + i] = lda;
        }

        /* move vector at*y */
        for (int i = 0; i < lengthy; i++) {
            workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 2 * lengthx + i] = workingSpace[lengthx * lengthy
                                                                                                     + 2 * lengthy * lengthy + 3 * lengthx + i];
        }

        /* create matrix at*a*at*a + vector at*a*at*y */
        for (int i = 0; i < lengthy; i++) {
            for (int j = 0; j < lengthy; j++) {
                lda = 0;
                for (int k = 0; k < lengthy; k++) {
                    ldb = workingSpace[lengthx * lengthy + lengthy * i + k];
                    ldc = workingSpace[lengthx * lengthy + lengthy * j + k];
                    lda = lda + ldb * ldc;
                }
                workingSpace[lengthx * lengthy + lengthy * lengthy + lengthy * i + j] = lda;
            }
            lda = 0;
            for (int k = 0; k < lengthy; k++) {
                ldb = workingSpace[lengthx * lengthy + lengthy * i + k];
                ldc = workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 2 * lengthx + k];
                lda = lda + ldb * ldc;
            }
            workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 3 * lengthx + i] = lda;
        }

        /* move at*a*at*y */
        for (int i = 0; i < lengthy; i++) {
            workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 2 * lengthx + i] = workingSpace[lengthx * lengthy
                                                                                                     + 2 * lengthy * lengthy + 3 * lengthx + i];
        }

        /* initialisation in resulting vector */
        for (int i = 0; i < lengthy; i++) {
            workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + i] = 1;
        }

        /*** START OF ITERATIONS ***/
        for (repet = 0; repet < numberRepetitions; repet++) {
            if (repet != 0) {
                for (int i = 0; i < lengthy; i++) {
                    workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + i] = Math
                                                                                          .pow(workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + i], boost);
                }
            }
            for (lindex = 0; lindex < numberIterations; lindex++) {
                for (int i = 0; i < lengthy; i++) {
                    lda = 0;
                    for (int j = 0; j < lengthy; j++) {
                        ldb = workingSpace[lengthx * lengthy + lengthy * lengthy + lengthy * i + j];
                        ldc = workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + j];
                        lda = lda + ldb * ldc;
                    }
                    ldb = workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 2 * lengthx + i];
                    if (lda != 0) {
                        lda = ldb / lda;
                    }
                    ldb = workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + i];
                    lda *= ldb;
                    workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + 3 * lengthx + i] = lda;
                }
                for (int i = 0; i < lengthy; i++) {
                    workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + i] = workingSpace[lengthx * lengthy
                                                                                               + 2 * lengthy * lengthy + 3 * lengthx + i];
                }
            }
        }

        final double[] returnVector = destination == null || destination.length < source.length
                                              ? new double[source.length]
                                              : destination;
        /* write back resulting spectrum */
        for (int i = 0; i < lengthx; i++) {
            if (i < lengthy) {
                returnVector[i] = workingSpace[lengthx * lengthy + 2 * lengthy * lengthy + i];
            } else {
                returnVector[i] = 0;
            }
        }
        ArrayCache.release(CACHED_ARRAY_UNFOLDING, workingSpace);

        return returnVector;
    }

    public enum Direction {
        INCREASING,
        DECREASING;
    }

    public enum FilterOrder {
        ORDER_2,
        ORDER_4,
        ORDER_6,
        ORDER_8;
    }

    public enum SmoothWindow {
        NO_SMOOTHING(1),
        SMOOTHING_WIDTH3(3),
        SMOOTHING_WIDTH5(5),
        SMOOTHING_WIDTH7(7),
        SMOOTHING_WIDTH9(9),
        SMOOTHING_WIDTH11(11),
        SMOOTHING_WIDTH13(13),
        SMOOTHING_WIDTH15(15);

        private final int value;

        SmoothWindow(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public boolean isSmoothing() {
            return !this.equals(NO_SMOOTHING);
        }
    }
}
