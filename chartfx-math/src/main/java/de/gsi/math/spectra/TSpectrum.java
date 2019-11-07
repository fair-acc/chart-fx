package de.gsi.math.spectra;

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

    protected static int fgAverageWindow = 3; // Average window of searched peaks
    protected static int fgIterations = 3; // Maximum number of decon iterations (default=3)
    private static int PEAK_WINDOW = 1024;
    private static int kBackOrder2 = 0;
    private static int kBackOrder4 = 1;
    private static int kBackOrder6 = 2;
    private static int kBackOrder8 = 3;
    private static int kBackIncreasingWindow = 0;

    private static int kBackDecreasingWindow = 1;

    private static int kBackSmoothing3 = 3;
    private static int kBackSmoothing5 = 5;
    private static int kBackSmoothing7 = 7;
    private static int kBackSmoothing9 = 9;
    private static int kBackSmoothing11 = 11;
    private static int kBackSmoothing13 = 13;
    private static int kBackSmoothing15 = 15;
    protected int fMaxPeaks; // Maximum number of peaks to be found
    protected int fNPeaks; // number of peaks found
    protected double[] fPosition; // [fNPeaks] array of current peak positions
    protected double[] fPositionX; // [fNPeaks] X position of peaks
    protected double[] fPositionY; // [fNPeaks] Y position of peaks
    protected double fResolution; // resolution of the neighboring peaks

    /**
     * The TSpectrum() default constructor
     */
    public TSpectrum() {
        int n = 100;
        fMaxPeaks = n;
        fPosition = new double[n];
        fPositionX = new double[n];
        fPositionY = new double[n];
        fResolution = 1;
        fNPeaks = 0;
    }

    /**
     * @param maxpositions maximum number of peaks
     * @param resolution determines resolution of the neighboring peaks default value is 1 correspond to 3 sigma
     *        distance between peaks. Higher values allow higher resolution (smaller distance between peaks. May be set
     *        later through SetResolution.)
     */
    public TSpectrum(int maxpositions, double resolution) {
        int n = maxpositions;
        if (n <= 0)
            n = 1;
        fMaxPeaks = n;
        fPosition = new double[n];
        fPositionX = new double[n];
        fPositionY = new double[n];
        fNPeaks = 0;
        SetResolution(resolution);
    }

    /**
     * ONE-DIMENSIONAL BACKGROUND ESTIMATION FUNCTION - GENERAL FUNCTION This function calculates background spectrum
     * from source spectrum. The result is placed in the vector pointed by spe1945ctrum pointer. Function parameters:
     * 
     * @param spectrum vector of source spectrum
     * @param ssize length of the spectrum vector
     * @param numberIterations maximal width of clipping window,
     * @param direction direction of change of clipping window - possible values=kBackIncreasingWindow
     *        kBackDecreasingWindow
     * @param filterOrder order of clipping filter, -possible values=kBackOrder2 kBackOrder4 kBackOrder6 kBackOrder8
     * @param smoothing logical variable whether the smoothing operation in the estimation of background will be
     *        included - possible values=kFALSE kTRUE
     * @param smoothWindow width of smoothing window, -possible values=kBackSmoothing3 kBackSmoothing5 kBackSmoothing7
     *        kBackSmoothing9 kBackSmoothing11 kBackSmoothing13 kBackSmoothing15
     * @param compton logical variable whether the estimation of Compton edge will be included - possible values=kFALSE
     *        kTRUE
     * @return message of algorithm
     */
    String Background(double[] spectrum, int ssize, int numberIterations, int direction, int filterOrder,
            boolean smoothing, int smoothWindow, boolean compton) {
        int i, j, w, bw, b1, b2, priz;
        double a, b, c, d, e, yb1, yb2, ai, av, men, b4, c4, d4, e4, b6, c6, d6, e6, f6, g6, b8, c8, d8, e8, f8, g8, h8,
                i8;

        if (ssize <= 0)
            return "Wrong Parameters";
        if (numberIterations < 1)
            return "Width of Clipping Window Must Be Positive";
        if (ssize < 2 * numberIterations + 1)
            return "Too Large Clipping Window";
        if (smoothing == true && smoothWindow != kBackSmoothing3 && smoothWindow != kBackSmoothing5
                && smoothWindow != kBackSmoothing7 && smoothWindow != kBackSmoothing9
                && smoothWindow != kBackSmoothing11 && smoothWindow != kBackSmoothing13
                && smoothWindow != kBackSmoothing15)
            return "Incorrect width of smoothing window";

        double[] working_space = new double[2 * ssize];
        for (i = 0; i < ssize; i++) {
            working_space[i] = spectrum[i];
            working_space[i + ssize] = spectrum[i];
        }

        bw = (smoothWindow - 1) / 2;
        if (direction == kBackIncreasingWindow)
            i = 1;
        else if (direction == kBackDecreasingWindow)
            i = numberIterations;

        if (filterOrder == kBackOrder2) {
            do {
                for (j = i; j < ssize - i; j++) {
                    if (smoothing == false) {
                        a = working_space[ssize + j];
                        b = (working_space[ssize + j - i] + working_space[ssize + j + i]) / 2.0;
                        if (b < a)
                            a = b;
                        working_space[j] = a;
                    }

                    else if (smoothing == true) {
                        a = working_space[ssize + j];
                        av = 0;
                        men = 0;
                        for (w = j - bw; w <= j + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                av += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        av = av / men;
                        b = 0;
                        men = 0;
                        for (w = j - i - bw; w <= j - i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b = b / men;
                        c = 0;
                        men = 0;
                        for (w = j + i - bw; w <= j + i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c = c / men;
                        b = (b + c) / 2;
                        if (b < a)
                            av = b;
                        working_space[j] = av;
                    }
                }
                for (j = i; j < ssize - i; j++)
                    working_space[ssize + j] = working_space[j];
                if (direction == kBackIncreasingWindow)
                    i += 1;
                else if (direction == kBackDecreasingWindow)
                    i -= 1;
            } while (direction == kBackIncreasingWindow && i <= numberIterations
                    || direction == kBackDecreasingWindow && i >= 1);
        }

        else if (filterOrder == kBackOrder4) {
            do {
                for (j = i; j < ssize - i; j++) {
                    if (smoothing == false) {
                        a = working_space[ssize + j];
                        b = (working_space[ssize + j - i] + working_space[ssize + j + i]) / 2.0;
                        c = 0;
                        ai = i / 2;
                        c -= working_space[ssize + j - (int) (2 * ai)] / 6;
                        c += 4 * working_space[ssize + j - (int) ai] / 6;
                        c += 4 * working_space[ssize + j + (int) ai] / 6;
                        c -= working_space[ssize + j + (int) (2 * ai)] / 6;
                        if (b < c)
                            b = c;
                        if (b < a)
                            a = b;
                        working_space[j] = a;
                    }

                    else if (smoothing == true) {
                        a = working_space[ssize + j];
                        av = 0;
                        men = 0;
                        for (w = j - bw; w <= j + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                av += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        av = av / men;
                        b = 0;
                        men = 0;
                        for (w = j - i - bw; w <= j - i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b = b / men;
                        c = 0;
                        men = 0;
                        for (w = j + i - bw; w <= j + i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c = c / men;
                        b = (b + c) / 2;
                        ai = i / 2;
                        b4 = 0;
                        men = 0;
                        for (w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b4 = b4 / men;
                        c4 = 0;
                        men = 0;
                        for (w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c4 = c4 / men;
                        d4 = 0;
                        men = 0;
                        for (w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                d4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        d4 = d4 / men;
                        e4 = 0;
                        men = 0;
                        for (w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                e4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        e4 = e4 / men;
                        b4 = (-b4 + 4 * c4 + 4 * d4 - e4) / 6;
                        if (b < b4)
                            b = b4;
                        if (b < a)
                            av = b;
                        working_space[j] = av;
                    }
                }
                for (j = i; j < ssize - i; j++)
                    working_space[ssize + j] = working_space[j];
                if (direction == kBackIncreasingWindow)
                    i += 1;
                else if (direction == kBackDecreasingWindow)
                    i -= 1;
            } while (direction == kBackIncreasingWindow && i <= numberIterations
                    || direction == kBackDecreasingWindow && i >= 1);
        }

        else if (filterOrder == kBackOrder6) {
            do {
                for (j = i; j < ssize - i; j++) {
                    if (smoothing == true) {
                        a = working_space[ssize + j];
                        b = (working_space[ssize + j - i] + working_space[ssize + j + i]) / 2.0;
                        c = 0;
                        ai = i / 2;
                        c -= working_space[ssize + j - (int) (2 * ai)] / 6;
                        c += 4 * working_space[ssize + j - (int) ai] / 6;
                        c += 4 * working_space[ssize + j + (int) ai] / 6;
                        c -= working_space[ssize + j + (int) (2 * ai)] / 6;
                        d = 0;
                        ai = i / 3;
                        d += working_space[ssize + j - (int) (3 * ai)] / 20;
                        d -= 6 * working_space[ssize + j - (int) (2 * ai)] / 20;
                        d += 15 * working_space[ssize + j - (int) ai] / 20;
                        d += 15 * working_space[ssize + j + (int) ai] / 20;
                        d -= 6 * working_space[ssize + j + (int) (2 * ai)] / 20;
                        d += working_space[ssize + j + (int) (3 * ai)] / 20;
                        if (b < d)
                            b = d;
                        if (b < c)
                            b = c;
                        if (b < a)
                            a = b;
                        working_space[j] = a;
                    }

                    else if (smoothing == true) {
                        a = working_space[ssize + j];
                        av = 0;
                        men = 0;
                        for (w = j - bw; w <= j + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                av += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        av = av / men;
                        b = 0;
                        men = 0;
                        for (w = j - i - bw; w <= j - i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b = b / men;
                        c = 0;
                        men = 0;
                        for (w = j + i - bw; w <= j + i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c = c / men;
                        b = (b + c) / 2;
                        ai = i / 2;
                        b4 = 0;
                        men = 0;
                        for (w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b4 = b4 / men;
                        c4 = 0;
                        men = 0; // XX
                        for (w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c4 = c4 / men;
                        d4 = 0;
                        men = 0; // XX
                        for (w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                d4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        d4 = d4 / men;
                        e4 = 0;
                        men = 0; // XX
                        for (w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                e4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        e4 = e4 / men;
                        b4 = (-b4 + 4 * c4 + 4 * d4 - e4) / 6;
                        ai = i / 3;
                        b6 = 0;
                        men = 0; // XX
                        for (w = j - (int) (3 * ai) - bw; w <= j - (int) (3 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b6 = b6 / men;
                        c6 = 0;
                        men = 0; // XX
                        for (w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c6 = c6 / men;
                        d6 = 0;
                        men = 0; // XX
                        for (w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                d6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        d6 = d6 / men;
                        e6 = 0;
                        men = 0; // XX
                        for (w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                e6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        e6 = e6 / men;
                        f6 = 0;
                        men = 0;
                        for (w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                f6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        f6 = f6 / men;
                        g6 = 0;
                        men = 0;
                        for (w = j + (int) (3 * ai) - bw; w <= j + (int) (3 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                g6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        g6 = g6 / men;
                        b6 = (b6 - 6 * c6 + 15 * d6 + 15 * e6 - 6 * f6 + g6) / 20;
                        if (b < b6)
                            b = b6;
                        if (b < b4)
                            b = b4;
                        if (b < a)
                            av = b;
                        working_space[j] = av;
                    }
                }
                for (j = i; j < ssize - i; j++)
                    working_space[ssize + j] = working_space[j];

                if (direction == kBackIncreasingWindow)
                    i += 1;
                else if (direction == kBackDecreasingWindow)
                    i -= 1;
            } while (direction == kBackIncreasingWindow && i <= numberIterations
                    || direction == kBackDecreasingWindow && i >= 1);
        }

        else if (filterOrder == kBackOrder8) {
            do {
                for (j = i; j < ssize - i; j++) {
                    if (smoothing == false) {
                        a = working_space[ssize + j];
                        b = (working_space[ssize + j - i] + working_space[ssize + j + i]) / 2.0;
                        c = 0;
                        ai = i / 2;
                        c -= working_space[ssize + j - (int) (2 * ai)] / 6;
                        c += 4 * working_space[ssize + j - (int) ai] / 6;
                        c += 4 * working_space[ssize + j + (int) ai] / 6;
                        c -= working_space[ssize + j + (int) (2 * ai)] / 6;
                        d = 0;
                        ai = i / 3;
                        d += working_space[ssize + j - (int) (3 * ai)] / 20;
                        d -= 6 * working_space[ssize + j - (int) (2 * ai)] / 20;
                        d += 15 * working_space[ssize + j - (int) ai] / 20;
                        d += 15 * working_space[ssize + j + (int) ai] / 20;
                        d -= 6 * working_space[ssize + j + (int) (2 * ai)] / 20;
                        d += working_space[ssize + j + (int) (3 * ai)] / 20;
                        e = 0;
                        ai = i / 4;
                        e -= working_space[ssize + j - (int) (4 * ai)] / 70;
                        e += 8 * working_space[ssize + j - (int) (3 * ai)] / 70;
                        e -= 28 * working_space[ssize + j - (int) (2 * ai)] / 70;
                        e += 56 * working_space[ssize + j - (int) ai] / 70;
                        e += 56 * working_space[ssize + j + (int) ai] / 70;
                        e -= 28 * working_space[ssize + j + (int) (2 * ai)] / 70;
                        e += 8 * working_space[ssize + j + (int) (3 * ai)] / 70;
                        e -= working_space[ssize + j + (int) (4 * ai)] / 70;
                        if (b < e)
                            b = e;
                        if (b < d)
                            b = d;
                        if (b < c)
                            b = c;
                        if (b < a)
                            a = b;
                        working_space[j] = a;
                    }

                    else if (smoothing == true) {
                        a = working_space[ssize + j];
                        av = 0;
                        men = 0;
                        for (w = j - bw; w <= j + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                av += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        av = av / men;
                        b = 0;
                        men = 0;
                        for (w = j - i - bw; w <= j - i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b = b / men;
                        c = 0;
                        men = 0;
                        for (w = j + i - bw; w <= j + i + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c = c / men;
                        b = (b + c) / 2;
                        ai = i / 2;
                        b4 = 0;
                        men = 0;
                        for (w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b4 = b4 / men;
                        c4 = 0;
                        men = 0;
                        for (w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c4 = c4 / men;
                        d4 = 0;
                        men = 0;
                        for (w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                d4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        d4 = d4 / men;
                        e4 = 0;
                        men = 0;
                        for (w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                e4 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        e4 = e4 / men;
                        b4 = (-b4 + 4 * c4 + 4 * d4 - e4) / 6;
                        ai = i / 3;
                        b6 = 0;
                        men = 0;
                        for (w = j - (int) (3 * ai) - bw; w <= j - (int) (3 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b6 = b6 / men;
                        c6 = 0;
                        men = 0;
                        for (w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c6 = c6 / men;
                        d6 = 0;
                        men = 0;
                        for (w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                d6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        d6 = d6 / men;
                        e6 = 0;
                        men = 0;
                        for (w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                e6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        e6 = e6 / men;
                        f6 = 0;
                        men = 0;
                        for (w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                f6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        f6 = f6 / men;
                        g6 = 0;
                        men = 0;
                        for (w = j + (int) (3 * ai) - bw; w <= j + (int) (3 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                g6 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        g6 = g6 / men;
                        b6 = (b6 - 6 * c6 + 15 * d6 + 15 * e6 - 6 * f6 + g6) / 20;
                        ai = i / 4;
                        b8 = 0;
                        men = 0;
                        for (w = j - (int) (4 * ai) - bw; w <= j - (int) (4 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                b8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        b8 = b8 / men;
                        c8 = 0;
                        men = 0;
                        for (w = j - (int) (3 * ai) - bw; w <= j - (int) (3 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                c8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        c8 = c8 / men;
                        d8 = 0;
                        men = 0;
                        for (w = j - (int) (2 * ai) - bw; w <= j - (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                d8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        d8 = d8 / men;
                        e8 = 0;
                        men = 0;
                        for (w = j - (int) ai - bw; w <= j - (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                e8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        e8 = e8 / men;
                        f8 = 0;
                        men = 0;
                        for (w = j + (int) ai - bw; w <= j + (int) ai + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                f8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        f8 = f8 / men;
                        g8 = 0;
                        men = 0;
                        for (w = j + (int) (2 * ai) - bw; w <= j + (int) (2 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                g8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        g8 = g8 / men;
                        h8 = 0;
                        men = 0;
                        for (w = j + (int) (3 * ai) - bw; w <= j + (int) (3 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                h8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        h8 = h8 / men;
                        i8 = 0;
                        men = 0;
                        for (w = j + (int) (4 * ai) - bw; w <= j + (int) (4 * ai) + bw; w++) {
                            if (w >= 0 && w < ssize) {
                                i8 += working_space[ssize + w];
                                men += 1;
                            }
                        }
                        i8 = i8 / men;
                        b8 = (-b8 + 8 * c8 - 28 * d8 + 56 * e8 - 56 * f8 - 28 * g8 + 8 * h8 - i8) / 70;
                        if (b < b8)
                            b = b8;
                        if (b < b6)
                            b = b6;
                        if (b < b4)
                            b = b4;
                        if (b < a)
                            av = b;
                        working_space[j] = av;
                    }
                }
                for (j = i; j < ssize - i; j++)
                    working_space[ssize + j] = working_space[j];

                if (direction == kBackIncreasingWindow)
                    i += 1;
                else if (direction == kBackDecreasingWindow)
                    i -= 1;
            } while (direction == kBackIncreasingWindow && i <= numberIterations
                    || direction == kBackDecreasingWindow && i >= 1);
        }

        if (compton == true) {
            for (i = 0, b2 = 0; i < ssize; i++) {
                b1 = b2;
                a = working_space[i];
                b = spectrum[i];
                j = i;
                if (Math.abs(a - b) >= 1) {
                    b1 = i - 1;
                    if (b1 < 0)
                        b1 = 0;
                    yb1 = working_space[b1];
                    for (b2 = b1 + 1, c = 0, priz = 0; priz == 0 && b2 < ssize; b2++) {
                        a = working_space[b2];
                        b = spectrum[b2];
                        c = c + b - yb1;
                        if (Math.abs(a - b) < 1) {
                            priz = 1;
                            yb2 = b;
                        }
                    }
                    if (b2 == ssize)
                        b2 -= 1;
                    yb2 = working_space[b2];
                    if (yb1 <= yb2) {
                        for (j = b1, c = 0; j <= b2; j++) {
                            b = spectrum[j];
                            c = c + b - yb1;
                        }
                        if (c > 1) {
                            c = (yb2 - yb1) / c;
                            for (j = b1, d = 0; j <= b2 && j < ssize; j++) {
                                b = spectrum[j];
                                d = d + b - yb1;
                                a = c * d + yb1;
                                working_space[ssize + j] = a;
                            }
                        }
                    }

                    else {
                        for (j = b2, c = 0; j >= b1; j--) {
                            b = spectrum[j];
                            c = c + b - yb2;
                        }
                        if (c > 1) {
                            c = (yb1 - yb2) / c;
                            for (j = b2, d = 0; j >= b1 && j >= 0; j--) {
                                b = spectrum[j];
                                d = d + b - yb2;
                                a = c * d + yb2;
                                working_space[ssize + j] = a;
                            }
                        }
                    }
                    i = b2;
                }
            }
        }

        for (j = 0; j < ssize; j++)
            spectrum[j] = working_space[ssize + j];

        working_space = null;
        return null;
    }

    /**
     * ONE-DIMENSIONAL BACKGROUND ESTIMATION FUNCTION This function calculates the background spectrum in the input
     * histogram h. The background is returned as a histogram. Function parameters:
     * 
     * @param h input 1-d histogram
     * @param numberIterations (default value = 20) Increasing numberIterations make the result smoother and lower.
     * @param option may contain one of the following options - to set the direction parameter "BackIncreasingWindow".
     *        By default the direction is BackDecreasingWindow - filterOrder-order of clipping filter, (default
     *        "BackOrder2" -possible values= "BackOrder4" "BackOrder6" "BackOrder8" - "nosmoothing"- if selected, the
     *        background is not smoothed By default the background is smoothed. - smoothWindow-width of smoothing
     *        window, (default is "BackSmoothing3") -possible values= "BackSmoothing5" "BackSmoothing7" "BackSmoothing9"
     *        "BackSmoothing11" "BackSmoothing13" "BackSmoothing15" - "Compton" if selected the estimation of Compton
     *        edge will be included. NOTE that the background is only evaluated in the current range of h. ie, if h has
     *        a bin range (set via h->GetXaxis()->SetRange(binmin,binmax), the returned histogram will be created with
     *        the same number of bins as the input histogram h, but only bins from binmin to binmax will be filled with
     *        the estimated background.
     * @return data set
     */
    double[] Background(double[] h, int numberIterations, String option) {

        if (h == null) {
            System.out.println("TSpectrum::Background() - dataset is null");
            return null;
        }
        // TODO implement 2-D histograms
        // if (!(h instanceOf(DefaultDataSet))) {
        // System.out.println("Search: Only implemented for 1-d histograms");
        // return null;
        // }
        String opt = option.toLowerCase();

        // set options

        int direction = kBackDecreasingWindow;
        if (opt.indexOf("backincreasingwindow") >= 0)
            direction = kBackIncreasingWindow;
        int filterOrder = kBackOrder2;
        if (opt.indexOf("backorder4") >= 0)
            filterOrder = kBackOrder4;
        if (opt.indexOf("backorder6") >= 0)
            filterOrder = kBackOrder6;
        if (opt.indexOf("backorder8") >= 0)
            filterOrder = kBackOrder8;
        boolean smoothing = true;
        if (opt.indexOf("nosmoothing") >= 0)
            smoothing = false;
        int smoothWindow = kBackSmoothing3;
        if (opt.indexOf("backsmoothing5") >= 0)
            smoothWindow = kBackSmoothing5;
        if (opt.indexOf("backsmoothing7") >= 0)
            smoothWindow = kBackSmoothing7;
        if (opt.indexOf("backsmoothing9") >= 0)
            smoothWindow = kBackSmoothing9;
        if (opt.indexOf("backsmoothing11") >= 0)
            smoothWindow = kBackSmoothing11;
        if (opt.indexOf("backsmoothing13") >= 0)
            smoothWindow = kBackSmoothing13;
        if (opt.indexOf("backsmoothing15") >= 0)
            smoothWindow = kBackSmoothing15;
        boolean compton = false;
        if (opt.indexOf("compton") >= 0)
            compton = true;

        // Assume use all bins for background computation
        // TODO: implement range selection
        int first = 0;
        int last = h.length - 1;
        int size = last - first + 1;
        double[] source = new double[size];
        for (int i = 0; i < size; i++)
            source[i] = h[i + first];

        // find background (source is input and in output contains the background
        Background(source, size, numberIterations, direction, filterOrder, smoothing, smoothWindow, compton);

        return source;
    }

    /**
     * ONE-DIMENSIONAL DECONVOLUTION FUNCTION This function calculates deconvolution from source spectrum according to
     * response spectrum using Gold algorithm The result is placed in the vector pointed by source pointer.
     *
     * @param source vector of source spectrum
     * @param response vector of response spectrum
     * @param ssize length of source and response spectra
     * @param numberIterations for details we refer to the reference given below
     * @param numberRepetitions for repeated boosted deconvolution @boost, boosting coefficient M. Morhac, J. Kliman, V.
     *        Matousek, M. Veselsk�, I. Turzo.: Efficient one- and two-dimensional Gold deconvolution and its
     *        application to gamma-ray spectra decomposition. NIM, A401 (1997) 385-408.
     * @param boost ???
     * @return algorithm return error message
     */
    String Deconvolution(double[] source, double[] response, int ssize, int numberIterations, int numberRepetitions,
            double boost) {

        if (ssize <= 0)
            return "Wrong Parameters";
        if (numberRepetitions <= 0)
            return "Wrong Parameters";

        // working_space-pointer to the working vector
        // (its size must be 4*ssize of source spectrum)
        double[] working_space = new double[4 * ssize];
        int i, j, k, lindex, posit, lh_gold, l, repet;
        double lda, ldb, ldc, area, maximum;
        area = 0;
        lh_gold = -1;
        posit = 0;
        maximum = 0;

        // read response vector
        for (i = 0; i < ssize; i++) {
            lda = response[i];
            if (lda != 0)
                lh_gold = i + 1;
            working_space[i] = lda;
            area += lda;
            if (lda > maximum) {
                maximum = lda;
                posit = i;
            }
        }
        if (lh_gold == -1)
            return "ZERO RESPONSE VECTOR";

        // read source vector
        for (i = 0; i < ssize; i++)
            working_space[2 * ssize + i] = source[i];

        // create matrix at*a and vector at*y
        for (i = 0; i < ssize; i++) {
            lda = 0;
            for (j = 0; j < ssize; j++) {
                ldb = working_space[j];
                k = i + j;
                if (k < ssize) {
                    ldc = working_space[k];
                    lda = lda + ldb * ldc;
                }
            }
            working_space[ssize + i] = lda;
            lda = 0;
            for (k = 0; k < ssize; k++) {
                l = k - i;
                if (l >= 0) {
                    ldb = working_space[l];
                    ldc = working_space[2 * ssize + k];
                    lda = lda + ldb * ldc;
                }
            }
            working_space[3 * ssize + i] = lda;
        }

        // move vector at*y
        for (i = 0; i < ssize; i++) {
            working_space[2 * ssize + i] = working_space[3 * ssize + i];
        }

        // initialization of resulting vector
        for (i = 0; i < ssize; i++)
            working_space[i] = 1;

        // **START OF ITERATIONS**
        for (repet = 0; repet < numberRepetitions; repet++) {
            if (repet != 0) {
                for (i = 0; i < ssize; i++)
                    working_space[i] = Math.pow(working_space[i], boost);
            }
            for (lindex = 0; lindex < numberIterations; lindex++) {
                for (i = 0; i < ssize; i++) {
                    if (working_space[2 * ssize + i] > 0.000001 && working_space[i] > 0.000001) {
                        lda = 0;
                        for (j = 0; j < lh_gold; j++) {
                            ldb = working_space[j + ssize];
                            if (j != 0) {
                                k = i + j;
                                ldc = 0;
                                if (k < ssize)
                                    ldc = working_space[k];
                                k = i - j;
                                if (k >= 0)
                                    ldc += working_space[k];
                            } else
                                ldc = working_space[i];

                            lda = lda + ldb * ldc;
                        }
                        ldb = working_space[2 * ssize + i];

                        if (lda != 0)
                            lda = ldb / lda;
                        else
                            lda = 0;

                        ldb = working_space[i];
                        lda = lda * ldb;
                        working_space[3 * ssize + i] = lda;
                    }
                }
                for (i = 0; i < ssize; i++)
                    working_space[i] = working_space[3 * ssize + i];
            }
        }

        // shift resulting spectrum
        for (i = 0; i < ssize; i++) {
            lda = working_space[i];
            j = i + posit;
            j = j % ssize;
            working_space[ssize + j] = lda;
        }

        // write back resulting spectrum
        for (i = 0; i < ssize; i++)
            source[i] = area * working_space[ssize + i];
        working_space = null;
        return null;
    }

    /**
     * ONE-DIMENSIONAL DECONVOLUTION FUNCTION This function calculates deconvolution from source spectrum according to
     * response spectrum using Richardson-Lucy algorithm The result is placed in the vector pointed by source pointer.
     *
     * @param source vector of source spectrum
     * @param response vector of response spectrum
     * @param ssize length of source and response spectra
     * @param numberIterations for details we refer to the reference given above
     * @param numberRepetitions for repeated boosted deconvolution
     * @param boost boosting coefficient
     * @return algorithm return error message
     */
    String DeconvolutionRL(double[] source, double[] response, int ssize, int numberIterations, int numberRepetitions,
            double boost) {

        if (ssize <= 0)
            return "Wrong Parameters";
        if (numberRepetitions <= 0)
            return "Wrong Parameters";

        // working_space-pointer to the working vector
        // (its size must be 4*ssize of source spectrum)
        double[] working_space = new double[4 * ssize];
        int i, j, k, lindex, posit, lh_gold, repet, kmin, kmax;
        double lda, ldb, ldc, maximum;
        lh_gold = -1;
        posit = 0;
        maximum = 0;

        // read response vector
        for (i = 0; i < ssize; i++) {
            lda = response[i];
            if (lda != 0)
                lh_gold = i + 1;
            working_space[ssize + i] = lda;
            if (lda > maximum) {
                maximum = lda;
                posit = i;
            }
        }

        if (lh_gold == -1)
            return "ZERO RESPONSE VECTOR";

        // read source vector
        for (i = 0; i < ssize; i++)
            working_space[2 * ssize + i] = source[i];

        // initialization of resulting vector
        for (i = 0; i < ssize; i++) {
            if (i <= ssize - lh_gold)
                working_space[i] = 1;
            else
                working_space[i] = 0;
        }

        // **START OF ITERATIONS**
        for (repet = 0; repet < numberRepetitions; repet++) {
            if (repet != 0) {
                for (i = 0; i < ssize; i++)
                    working_space[i] = Math.pow(working_space[i], boost);
            }
            for (lindex = 0; lindex < numberIterations; lindex++) {
                for (i = 0; i <= ssize - lh_gold; i++) {
                    lda = 0;
                    if (working_space[i] > 0) {// x[i]
                        for (j = i; j < i + lh_gold; j++) {
                            ldb = working_space[2 * ssize + j];// y[j]
                            if (j < ssize) {
                                if (ldb > 0) {// y[j]
                                    kmax = j;
                                    if (kmax > lh_gold - 1)
                                        kmax = lh_gold - 1;
                                    kmin = j + lh_gold - ssize;
                                    if (kmin < 0)
                                        kmin = 0;
                                    ldc = 0;
                                    for (k = kmax; k >= kmin; k--) {
                                        ldc += working_space[ssize + k] * working_space[j - k];// h[k]*x[j-k]
                                    }
                                    if (ldc > 0)
                                        ldb = ldb / ldc;

                                    else
                                        ldb = 0;
                                }
                                ldb = ldb * working_space[ssize + j - i];// y[j]*h[j-i]/suma(h[j][k]x[k])
                            }
                            lda += ldb;
                        }
                        lda = lda * working_space[i];
                    }
                    working_space[3 * ssize + i] = lda;
                }
                for (i = 0; i < ssize; i++)
                    working_space[i] = working_space[3 * ssize + i];
            }
        }

        // shift resulting spectrum
        for (i = 0; i < ssize; i++) {
            lda = working_space[i];
            j = i + posit;
            j = j % ssize;
            working_space[ssize + j] = lda;
        }

        // write back resulting spectrum
        for (i = 0; i < ssize; i++)
            source[i] = working_space[ssize + i];

        working_space = null;
        return null;
    }

    /**
     * TSpectrum desctructor
     */
    @Override
    public void finalize() {
        fPosition = null;
        fPositionX = null;
        fPositionY = null;
    }

    /**
     * @return number of peaks found
     */
    public int getNPeaks() {
        return fNPeaks;
    }

    /**
     * @return X coordinates of found peaks
     */
    public double[] getPeakX() {
        double[] peakDouble = new double[fNPeaks];
        for (int i = 0; i < fNPeaks; i++)
            peakDouble[i] = fPositionX[i];
        return peakDouble;
    }

    /**
     * @return Y coordinates of found peaks
     */
    public double[] getPeakY() {
        double[] peakDouble = new double[fNPeaks];
        for (int i = 0; i < fNPeaks; i++)
            peakDouble[i] = fPositionY[i];
        return peakDouble;
    }

    /**
     * Print the array of positions
     */
    void Print() {
        System.out.println("Number of positions = " + fNPeaks);
        for (int i = 0; i < fNPeaks; i++) {
            System.out.println(" x[" + i + "] = " + fPositionX[i] + ", y[" + i + "] = " + fPositionY[i]);
        }
    }

    /**
     * ONE-DIMENSIONAL PEAK SEARCH FUNCTION This function searches for peaks in source spectrum in hin The number of
     * found peaks and their positions are written into the members fNpeaks and fPositionX. The search is performed in
     * the current histogram range. Function parameters:
     * 
     * @param hinX histogram x of source spectrum
     * @param hinY histogram Y of source spectrum
     * @param houtX out histogram of source spectrum
     * @param houtY out histogram of source spectrum
     * @param sigma sigma of searched peaks, for details we refer to manual
     * @param option string options passed to the algorithms
     * @param threshold (default=0.05) peaks with amplitude less than threshold*highest_peak are discarded. 0 &lt;
     *        threshold &lt;1 By default, the background is removed before deconvolution. Specify the option
     *        "nobackground" to not remove the background. By default the "Markov" chain algorithm is used. Specify the
     *        option "noMarkov" to disable this algorithm Note that by default the source spectrum is replaced by a new
     *        spectrum
     * @return number of found peaks
     */
    public int Search(double[] hinX, double[] hinY, double[] houtX, double[] houtY, double sigma, String option,
            double threshold) {

        if (hinX == null || hinY == null)
            return 0;
        int dimension = 1;
        // int dimension = hin.GetDimension();
        // if (dimension > 2) {
        // System.out.println("Search: Only implemented for 1-d and 2-d histograms");
        // return 0;
        // }
        if (threshold <= 0 || threshold >= 1) {
            System.out.println("Search: threshold must 0<threshold<1, threshol=0.05 assumed");
            threshold = 0.05;
        }
        String opt = option.toLowerCase();

        boolean background = true;
        if (opt.indexOf("nobackground") >= 0) {
            background = false;
            opt = opt.replaceAll("nobackground", "");
        }
        boolean markov = true;
        if (opt.indexOf("nomarkov") >= 0) {
            markov = false;
            opt = opt.replaceAll("nomarkov", "");
        }
        // TODO: update first last selection
        // int first = hin.GetXaxis().GetFirst();
        // int last = hin.GetXaxis().GetLast();
        int first = 0;
        int last = hinX.length - 1;
        int size = last - first + 1;
        double[] source = new double[size];
        double[] dest = new double[size];
        for (int i = 0; i < size - 1; i++) {
            source[i] = hinY[i + first];
        }
        if (sigma <= 1) {
            sigma = size / fMaxPeaks;
            if (sigma < 1)
                sigma = 1;
            if (sigma > 8)
                sigma = 8;
        }

        int npeaks = SearchHighRes(source, dest, size, sigma, 100 * threshold, background, fgIterations, markov,
                fgAverageWindow);

        System.out.println("N peaks is: " + npeaks);

        // hout.clear();
        houtX = new double[fPositionX.length];
        houtY = new double[fPositionY.length];
        for (int i = 0; i < npeaks; i++) {
            int bin = first + (int) (fPositionX[i] + 0.5);
            bin = bin - 1;
            fPositionX[i] = hinX[bin];
            fPositionY[i] = hinY[bin];
            houtX[i] = fPositionX[i];
            houtY[i] = fPositionY[i];
        }

        source = null;
        dest = null;

        if (opt.indexOf("goff") >= 0)
            return npeaks;

        if (npeaks == 0)
            return 0;

        return npeaks;
    }

    /**
     * ONE-DIMENSIONAL HIGH-RESOLUTION PEAK SEARCH FUNCTION This function searches for peaks in source spectrum It is
     * based on deconvolution method. First the background is removed (if desired), then Markov spectrum is calculated
     * (if desired), then the response function is generated according to given sigma and deconvolution is carried out.
     * 
     * @param source vector of source spectrum
     * @param destVector vector of resulting deconvolved spectrum
     * @param ssize length of source spectrum
     * @param sigma sigma of searched peaks, for details we refer to manual
     * @param threshold threshold value in % for selected peaks, peaks with amplitude less than
     *        threshold*highest_peak/100 are ignored, see manual
     * @param backgroundRemove logical variable, set if the removal of background before deconvolution is desired
     * @param deconIterations number of iterations in deconvolution operation
     * @param markov logical variable, if it is true, first the source spectrum is replaced by new spectrum calculated
     *        using Markov chains method.
     * @param averWindow averaging window of searched peaks, for details we refer to manual (applies only for Markov
     *        method)
     * @return number of found peaks
     */
    public int SearchHighRes(double[] source, double[] destVector, int ssize, double sigma, double threshold,
            boolean backgroundRemove, int deconIterations, boolean markov, int averWindow) {

        int i, j, numberIterations = (int) (7 * sigma + 0.5);
        double a, b, c;
        int k, lindex, posit, imin, imax, jmin, jmax, lh_gold, priz;
        double lda, ldb, ldc, area, maximum, maximum_decon;
        int xmin, xmax, l, peak_index = 0, size_ext = ssize + 2 * numberIterations, shift = numberIterations, bw = 2, w;
        double maxch;
        double nom, nip, nim, sp, sm, plocha = 0;
        double m0low = 0, m1low = 0, m2low = 0, l0low = 0, l1low = 0, detlow, av, men;

        if (sigma < 1) {
            System.out.println("SearchHighRes" + " Invalid sigma, must be greater than or equal to 1");
            return 0;
        }

        if (threshold <= 0 || threshold >= 100) {
            System.out.println("SearchHighRes: Invalid threshold, must be positive and less than 100");
            return 0;
        }

        j = (int) (5.0 * sigma + 0.5);
        if (j >= PEAK_WINDOW / 2) {
            System.out.println("SearchHighRes: Too large sigma");
            return 0;
        }

        if (markov == true) {
            if (averWindow <= 0) {
                System.out.println("SearchHighRes: Averanging window must be positive");
                return 0;
            }
        }

        if (backgroundRemove == true) {
            if (ssize < 2 * numberIterations + 1) {
                System.out.println("SearchHighRes: Too large clipping window");
                return 0;
            }
        }

        k = (int) (2 * sigma + 0.5);
        if (k >= 2) {
            for (i = 0; i < k; i++) {
                a = i;
                b = source[i];
                m0low += 1;
                m1low += a;
                m2low += a * a;
                l0low += b;
                l1low += a * b;
            }
            detlow = m0low * m2low - m1low * m1low;
            if (detlow != 0)
                l1low = (-l0low * m1low + l1low * m0low) / detlow;

            else
                l1low = 0;
            if (l1low > 0)
                l1low = 0;
        }

        else {
            l1low = 0;
        }

        i = (int) (7 * sigma + 0.5);
        i = 2 * i;
        double[] working_space = new double[7 * (ssize + i)];
        for (j = 0; j < 7 * (ssize + i); j++)
            working_space[j] = 0;
        for (i = 0; i < size_ext; i++) {
            if (i < shift) {
                a = i - shift;
                working_space[i + size_ext] = source[0] + l1low * a;
                if (working_space[i + size_ext] < 0)
                    working_space[i + size_ext] = 0;
            }

            else if (i >= ssize + shift) {
                a = i - (ssize - 1 + shift);
                working_space[i + size_ext] = source[ssize - 1];
                if (working_space[i + size_ext] < 0)
                    working_space[i + size_ext] = 0;
            }

            else
                working_space[i + size_ext] = source[i - shift];
        }

        if (backgroundRemove == true) {
            for (i = 1; i <= numberIterations; i++) {
                for (j = i; j < size_ext - i; j++) {
                    if (markov == false) {
                        a = working_space[size_ext + j];
                        b = (working_space[size_ext + j - i] + working_space[size_ext + j + i]) / 2.0;
                        if (b < a)
                            a = b;

                        working_space[j] = a;
                    }

                    else {
                        a = working_space[size_ext + j];
                        av = 0;
                        men = 0;
                        for (w = j - bw; w <= j + bw; w++) {
                            if (w >= 0 && w < size_ext) {
                                av += working_space[size_ext + w];
                                men += 1;
                            }
                        }
                        av = av / men;
                        b = 0;
                        men = 0;
                        for (w = j - i - bw; w <= j - i + bw; w++) {
                            if (w >= 0 && w < size_ext) {
                                b += working_space[size_ext + w];
                                men += 1;
                            }
                        }
                        b = b / men;
                        c = 0;
                        men = 0;
                        for (w = j + i - bw; w <= j + i + bw; w++) {
                            if (w >= 0 && w < size_ext) {
                                c += working_space[size_ext + w];
                                men += 1;
                            }
                        }
                        c = c / men;
                        b = (b + c) / 2;
                        if (b < a)
                            av = b;
                        working_space[j] = av;
                    }
                }
                for (j = i; j < size_ext - i; j++)
                    working_space[size_ext + j] = working_space[j];
            }
            for (j = 0; j < size_ext; j++) {
                if (j < shift) {
                    a = j - shift;
                    b = source[0] + l1low * a;
                    if (b < 0)
                        b = 0;
                    working_space[size_ext + j] = b - working_space[size_ext + j];
                }

                else if (j >= ssize + shift) {
                    a = j - (ssize - 1 + shift);
                    b = source[ssize - 1];
                    if (b < 0)
                        b = 0;
                    working_space[size_ext + j] = b - working_space[size_ext + j];
                }

                else {
                    working_space[size_ext + j] = source[j - shift] - working_space[size_ext + j];
                }
            }
            for (j = 0; j < size_ext; j++) {
                if (working_space[size_ext + j] < 0)
                    working_space[size_ext + j] = 0;
            }
        }

        for (i = 0; i < size_ext; i++) {
            working_space[i + 6 * size_ext] = working_space[i + size_ext];
        }

        if (markov == true) {
            for (j = 0; j < size_ext; j++)
                working_space[2 * size_ext + j] = working_space[size_ext + j];
            xmin = 0;
            xmax = size_ext - 1;
            for (i = 0, maxch = 0; i < size_ext; i++) {
                working_space[i] = 0;
                if (maxch < working_space[2 * size_ext + i])
                    maxch = working_space[2 * size_ext + i];
                plocha += working_space[2 * size_ext + i];
            }
            if (maxch == 0) {
                working_space = null;
                return 0;
            }

            nom = 1;
            working_space[xmin] = 1;
            for (i = xmin; i < xmax; i++) {
                nip = working_space[2 * size_ext + i] / maxch;
                nim = working_space[2 * size_ext + i + 1] / maxch;
                sp = 0;
                sm = 0;
                for (l = 1; l <= averWindow; l++) {
                    if ((i + l) > xmax)
                        a = working_space[2 * size_ext + xmax] / maxch;

                    else
                        a = working_space[2 * size_ext + i + l] / maxch;

                    b = a - nip;
                    if (a + nip <= 0)
                        a = 1;

                    else
                        a = Math.sqrt(a + nip);

                    b = b / a;
                    b = Math.exp(b);
                    sp = sp + b;
                    if ((i - l + 1) < xmin)
                        a = working_space[2 * size_ext + xmin] / maxch;

                    else
                        a = working_space[2 * size_ext + i - l + 1] / maxch;

                    b = a - nim;
                    if (a + nim <= 0)
                        a = 1;

                    else
                        a = Math.sqrt(a + nim);

                    b = b / a;
                    b = Math.exp(b);
                    sm = sm + b;
                }
                a = sp / sm;
                a = working_space[i + 1] = working_space[i] * a;
                nom = nom + a;
            }
            for (i = xmin; i <= xmax; i++) {
                working_space[i] = working_space[i] / nom;
            }
            for (j = 0; j < size_ext; j++)
                working_space[size_ext + j] = working_space[j] * plocha;
            for (j = 0; j < size_ext; j++) {
                working_space[2 * size_ext + j] = working_space[size_ext + j];
            }
            if (backgroundRemove == true) {
                for (i = 1; i <= numberIterations; i++) {
                    for (j = i; j < size_ext - i; j++) {
                        a = working_space[size_ext + j];
                        b = (working_space[size_ext + j - i] + working_space[size_ext + j + i]) / 2.0;
                        if (b < a)
                            a = b;
                        working_space[j] = a;
                    }
                    for (j = i; j < size_ext - i; j++)
                        working_space[size_ext + j] = working_space[j];
                }
                for (j = 0; j < size_ext; j++) {
                    working_space[size_ext + j] = working_space[2 * size_ext + j] - working_space[size_ext + j];
                }
            }
        }

        // deconvolution starts
        area = 0;
        lh_gold = -1;
        posit = 0;
        maximum = 0;

        // generate response vector
        for (i = 0; i < size_ext; i++) {
            lda = i - 3 * sigma;
            lda = lda * lda / (2 * sigma * sigma);
            j = (int) (1000 * Math.exp(-lda));
            lda = j;
            if (lda != 0)
                lh_gold = i + 1;

            working_space[i] = lda;
            area = area + lda;
            if (lda > maximum) {
                maximum = lda;
                posit = i;
            }
        }

        // read source vector
        for (i = 0; i < size_ext; i++)
            working_space[2 * size_ext + i] = Math.abs(working_space[size_ext + i]);

        // create matrix at*a(vector b)
        i = lh_gold - 1;
        if (i > size_ext)
            i = size_ext;

        imin = -i;
        imax = i;
        for (i = imin; i <= imax; i++) {
            lda = 0;
            jmin = 0;
            if (i < 0)
                jmin = -i;
            jmax = lh_gold - 1 - i;
            if (jmax > (lh_gold - 1))
                jmax = lh_gold - 1;

            for (j = jmin; j <= jmax; j++) {
                ldb = working_space[j];
                ldc = working_space[i + j];
                lda = lda + ldb * ldc;
            }
            working_space[size_ext + i - imin] = lda;
        }

        // create vector p
        i = lh_gold - 1;
        imin = -i;
        imax = size_ext + i - 1;
        for (i = imin; i <= imax; i++) {
            lda = 0;
            for (j = 0; j <= (lh_gold - 1); j++) {
                ldb = working_space[j];
                k = i + j;
                if (k >= 0 && k < size_ext) {
                    ldc = working_space[2 * size_ext + k];
                    lda = lda + ldb * ldc;
                }

            }
            working_space[4 * size_ext + i - imin] = lda;
        }

        // move vector p
        for (i = imin; i <= imax; i++)
            working_space[2 * size_ext + i - imin] = working_space[4 * size_ext + i - imin];

        // initialization of resulting vector
        for (i = 0; i < size_ext; i++)
            working_space[i] = 1;

        // START OF ITERATIONS
        for (lindex = 0; lindex < deconIterations; lindex++) {
            for (i = 0; i < size_ext; i++) {
                if (Math.abs(working_space[2 * size_ext + i]) > 0.00001 && Math.abs(working_space[i]) > 0.00001) {
                    lda = 0;
                    jmin = lh_gold - 1;
                    if (jmin > i)
                        jmin = i;

                    jmin = -jmin;
                    jmax = lh_gold - 1;
                    if (jmax > (size_ext - 1 - i))
                        jmax = size_ext - 1 - i;

                    for (j = jmin; j <= jmax; j++) {
                        ldb = working_space[j + lh_gold - 1 + size_ext];
                        ldc = working_space[i + j];
                        lda = lda + ldb * ldc;
                    }
                    ldb = working_space[2 * size_ext + i];
                    if (lda != 0)
                        lda = ldb / lda;

                    else
                        lda = 0;

                    ldb = working_space[i];
                    lda = lda * ldb;
                    working_space[3 * size_ext + i] = lda;
                }
            }
            for (i = 0; i < size_ext; i++) {
                working_space[i] = working_space[3 * size_ext + i];
            }
        }

        // shift resulting spectrum
        for (i = 0; i < size_ext; i++) {
            lda = working_space[i];
            j = i + posit;
            j = j % size_ext;
            working_space[size_ext + j] = lda;
        }

        // write back resulting spectrum
        maximum = 0;
        maximum_decon = 0;
        j = lh_gold - 1;
        for (i = 0; i < size_ext - j; i++) {
            if (i >= shift && i < ssize + shift) {
                working_space[i] = area * working_space[size_ext + i + j];
                if (maximum_decon < working_space[i])
                    maximum_decon = working_space[i];
                if (maximum < working_space[6 * size_ext + i])
                    maximum = working_space[6 * size_ext + i];
            }

            else
                working_space[i] = 0;
        }
        lda = 1;
        if (lda > threshold)
            lda = threshold;
        lda = lda / 100;

        // searching for peaks in deconvolved spectrum
        for (i = 1; i < size_ext - 1; i++) {
            if (working_space[i] > working_space[i - 1] && working_space[i] > working_space[i + 1]) {
                if (i >= shift && i < ssize + shift) {
                    if (working_space[i] > lda * maximum_decon
                            && working_space[6 * size_ext + i] > threshold * maximum / 100.0) {
                        for (j = i - 1, a = 0, b = 0; j <= i + 1; j++) {
                            a += (j - shift) * working_space[j];
                            b += working_space[j];
                        }
                        a = a / b;
                        if (a < 0)
                            a = 0;

                        if (a >= ssize)
                            a = ssize - 1;
                        if (peak_index == 0) {
                            fPositionX[0] = a;
                            peak_index = 1;
                        }

                        else {
                            for (j = 0, priz = 0; j < peak_index && priz == 0; j++) {
                                if (working_space[6 * size_ext + shift + (int) a] > working_space[6 * size_ext + shift
                                        + (int) fPositionX[j]])
                                    priz = 1;
                            }
                            if (priz == 0) {
                                if (j < fMaxPeaks) {
                                    fPositionX[j] = a;
                                }
                            }

                            else {
                                for (k = peak_index; k >= j; k--) {
                                    if (k < fMaxPeaks) {
                                        fPositionX[k] = fPositionX[k - 1];
                                    }
                                }
                                fPositionX[j - 1] = a;
                            }
                            if (peak_index < fMaxPeaks)
                                peak_index += 1;
                        }
                    }
                }
            }
        }

        for (i = 0; i < ssize; i++)
            destVector[i] = working_space[i + shift];

        working_space = null;
        fNPeaks = peak_index;
        if (peak_index == fMaxPeaks)
            System.out.println("SearchHighRes: Peak buffer full");
        return fNPeaks;
    }

    /**
     * @param w average window of searched peaks
     * @see #SearchHighRes
     */
    public void SetAverageWindow(int w) {
        fgAverageWindow = w;
    }

    /**
     * @param n max number of decon iterations in deconvolution operation
     * @see #SearchHighRes
     */
    void SetDeconIterations(int n) {
        fgIterations = n;
    }

    // STATIC functions (called by TH1)

    /**
     * @param resolution determines resolution of the neighboring peaks default value is 1 correspond to 3 sigma
     *        distance between peaks. Higher values allow higher resolution (smaller distance between peaks. May be set
     *        later through SetResolution.
     */
    public void SetResolution(double resolution) {

        if (resolution > 1)
            fResolution = resolution;
        else
            fResolution = 1;
    }

    /**
     * ONE-DIMENSIONAL MARKOV SPECTRUM SMOOTHING FUNCTION This function calculates smoothed spectrum from source
     * spectrum based on Markov chain method. The result is placed in the array pointed by source pointer.
     *
     * @param source array of source spectrum
     * @param ssize length of source array
     * @param averWindow width of averaging smoothing window
     * @return algorithm return error message
     */
    String SmoothMarkov(double[] source, int ssize, int averWindow) {
        int xmin, xmax, i, l;
        double a, b, maxch;
        double nom, nip, nim, sp, sm, area = 0;
        if (averWindow <= 0)
            return "Averaging Window must be positive";
        double[] working_space = new double[ssize];
        xmin = 0;
        xmax = ssize - 1;

        for (i = 0, maxch = 0; i < ssize; i++) {
            working_space[i] = 0;
            if (maxch < source[i])
                maxch = source[i];

            area += source[i];
        }

        if (maxch == 0)
            return null;

        nom = 1;
        working_space[xmin] = 1;
        for (i = xmin; i < xmax; i++) {
            nip = source[i] / maxch;
            nim = source[i + 1] / maxch;
            sp = 0;
            sm = 0;
            for (l = 1; l <= averWindow; l++) {

                if ((i + l) > xmax)
                    a = source[xmax] / maxch;
                else
                    a = source[i + l] / maxch;
                b = a - nip;

                if (a + nip <= 0)
                    a = 1;
                else
                    a = Math.sqrt(a + nip);

                b = b / a;
                b = Math.exp(b);
                sp = sp + b;
                if ((i - l + 1) < xmin)
                    a = source[xmin] / maxch;
                else
                    a = source[i - l + 1] / maxch;

                b = a - nim;
                if (a + nim <= 0)
                    a = 1;
                else
                    a = Math.sqrt(a + nim);

                b = b / a;
                b = Math.exp(b);
                sm = sm + b;
            }
            a = sp / sm;
            a = working_space[i + 1] = working_space[i] * a;
            nom = nom + a;
        }
        for (i = xmin; i <= xmax; i++) {
            working_space[i] = working_space[i] / nom;
        }
        for (i = 0; i < ssize; i++)
            source[i] = working_space[i] * area;

        working_space = null;
        return null;
    }

    /**
     * static function, interface to TSpectrum::Background
     * 
     * @param hist input histogram
     * @param niter number of search iterations
     * @param option passed to the underlying algorithm
     * @return array of background estimate
     */
    double[] StaticBackground(double[] hist, int niter, String option) {
        TSpectrum s = new TSpectrum();
        return s.Background(hist, niter, option);
    }

    /**
     * static function, interface to TSpectrum::Search
     * 
     * @param histX histogram x of source spectrum
     * @param histY histogram Y of source spectrum
     * @param houtX out histogram of source spectrum
     * @param houtY out histogram of source spectrum
     * @param sigma sigma of searched peaks, for details we refer to manual
     * @param option string options passed to the algorithms
     * @param threshold (default=0.05) peaks with amplitude less than threshold*highest_peak are discarded.
     *        0<threshold<1 By default, the background is removed before deconvolution. Specify the option
     *        "nobackground" to not remove the background. By default the "Markov" chain algorithm is used. Specify the
     *        option "noMarkov" to disable this algorithm Note that by default the source spectrum is replaced by a new
     *        spectrum
     * @return number of found peaks
     */
    int StaticSearch(double[] histX, double[] histY, double[] houtX, double[] houtY, double sigma, String option,
            double threshold) {
        TSpectrum s = new TSpectrum();
        return s.Search(histX, histY, houtX, houtY, sigma, option, threshold);
    }

    /**
     * ONE-DIMENSIONAL UNFOLDING FUNCTION This function unfolds source spectrum according to response matrix columns.
     * The result is placed in the vector pointed by source pointer.
     * 
     * @param source vector of source spectrum
     * @param respMatrix matrix of response spectra
     * @param ssizex length of source spectrum and # of columns of response matrix
     * @param ssizey length of destination spectrum and # of rows of response matrix
     * @param numberIterations Note!!! ssizex must be >= ssizey
     * @param numberRepetitions repitition count
     * @param boost boosting coefficient
     * @return algorithm return error message
     */
    String Unfolding(double[] source, double[][] respMatrix, int ssizex, int ssizey, int numberIterations,
            int numberRepetitions, double boost) {
        int i, j, k, lindex, lhx = 0, repet;
        double lda, ldb, ldc, area;
        if (ssizex <= 0 || ssizey <= 0)
            return "Wrong Parameters";
        if (ssizex < ssizey)
            return "Sizex must be greater than sizey)";
        if (numberIterations <= 0)
            return "Number of iterations must be positive";
        double[] working_space = new double[ssizex * ssizey + 2 * ssizey * ssizey + 4 * ssizex];

        /* read response matrix */
        for (j = 0; j < ssizey && lhx != -1; j++) {
            area = 0;
            lhx = -1;
            for (i = 0; i < ssizex; i++) {
                lda = respMatrix[j][i];
                if (lda != 0) {
                    lhx = i + 1;
                }
                working_space[j * ssizex + i] = lda;
                area = area + lda;
            }
            if (lhx != -1) {
                for (i = 0; i < ssizex; i++)
                    working_space[j * ssizex + i] /= area;
            }
        }

        if (lhx == -1)
            return ("ZERO COLUMN IN RESPONSE MATRIX");

        /* read source vector */
        for (i = 0; i < ssizex; i++)
            working_space[ssizex * ssizey + 2 * ssizey * ssizey + 2 * ssizex + i] = source[i];

        /* create matrix at*a + at*y */
        for (i = 0; i < ssizey; i++) {
            for (j = 0; j < ssizey; j++) {
                lda = 0;
                for (k = 0; k < ssizex; k++) {
                    ldb = working_space[ssizex * i + k];
                    ldc = working_space[ssizex * j + k];
                    lda = lda + ldb * ldc;
                }
                working_space[ssizex * ssizey + ssizey * i + j] = lda;
            }
            lda = 0;
            for (k = 0; k < ssizex; k++) {
                ldb = working_space[ssizex * i + k];
                ldc = working_space[ssizex * ssizey + 2 * ssizey * ssizey + 2 * ssizex + k];
                lda = lda + ldb * ldc;
            }
            working_space[ssizex * ssizey + 2 * ssizey * ssizey + 3 * ssizex + i] = lda;
        }

        /* move vector at*y */
        for (i = 0; i < ssizey; i++)
            working_space[ssizex * ssizey + 2 * ssizey * ssizey + 2 * ssizex + i] = working_space[ssizex * ssizey
                    + 2 * ssizey * ssizey + 3 * ssizex + i];

        /* create matrix at*a*at*a + vector at*a*at*y */
        for (i = 0; i < ssizey; i++) {
            for (j = 0; j < ssizey; j++) {
                lda = 0;
                for (k = 0; k < ssizey; k++) {
                    ldb = working_space[ssizex * ssizey + ssizey * i + k];
                    ldc = working_space[ssizex * ssizey + ssizey * j + k];
                    lda = lda + ldb * ldc;
                }
                working_space[ssizex * ssizey + ssizey * ssizey + ssizey * i + j] = lda;
            }
            lda = 0;
            for (k = 0; k < ssizey; k++) {
                ldb = working_space[ssizex * ssizey + ssizey * i + k];
                ldc = working_space[ssizex * ssizey + 2 * ssizey * ssizey + 2 * ssizex + k];
                lda = lda + ldb * ldc;
            }
            working_space[ssizex * ssizey + 2 * ssizey * ssizey + 3 * ssizex + i] = lda;
        }

        /* move at*a*at*y */
        for (i = 0; i < ssizey; i++)
            working_space[ssizex * ssizey + 2 * ssizey * ssizey + 2 * ssizex + i] = working_space[ssizex * ssizey
                    + 2 * ssizey * ssizey + 3 * ssizex + i];

        /* initialization in resulting vector */
        for (i = 0; i < ssizey; i++)
            working_space[ssizex * ssizey + 2 * ssizey * ssizey + i] = 1;

        /*** START OF ITERATIONS ***/
        for (repet = 0; repet < numberRepetitions; repet++) {
            if (repet != 0) {
                for (i = 0; i < ssizey; i++)
                    working_space[ssizex * ssizey + 2 * ssizey * ssizey + i] = Math
                            .pow(working_space[ssizex * ssizey + 2 * ssizey * ssizey + i], boost);
            }
            for (lindex = 0; lindex < numberIterations; lindex++) {
                for (i = 0; i < ssizey; i++) {
                    lda = 0;
                    for (j = 0; j < ssizey; j++) {
                        ldb = working_space[ssizex * ssizey + ssizey * ssizey + ssizey * i + j];
                        ldc = working_space[ssizex * ssizey + 2 * ssizey * ssizey + j];
                        lda = lda + ldb * ldc;
                    }
                    ldb = working_space[ssizex * ssizey + 2 * ssizey * ssizey + 2 * ssizex + i];
                    if (lda != 0) {
                        lda = ldb / lda;
                    }

                    else
                        lda = 0;
                    ldb = working_space[ssizex * ssizey + 2 * ssizey * ssizey + i];
                    lda = lda * ldb;
                    working_space[ssizex * ssizey + 2 * ssizey * ssizey + 3 * ssizex + i] = lda;
                }
                for (i = 0; i < ssizey; i++)
                    working_space[ssizex * ssizey + 2 * ssizey * ssizey + i] = working_space[ssizex * ssizey
                            + 2 * ssizey * ssizey + 3 * ssizex + i];
            }
        }

        /* write back resulting spectrum */
        for (i = 0; i < ssizex; i++) {
            if (i < ssizey)
                source[i] = working_space[ssizex * ssizey + 2 * ssizey * ssizey + i];

            else
                source[i] = 0;
        }
        return null;
    }

    public static void main(String[] argv) {
        // TODO: some simple test script to illustrate the function of TSpectrum
        // maybe two gaussian peaks plus some noise ??
    }

}
