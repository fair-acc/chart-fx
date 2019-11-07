package de.gsi.math.spectra.wavelet;

/**
 * <p>
 * Daubechies D4 wavelet transform (D4 denotes four coefficients)
 * </p>
 * <p>
 * I have to confess up front that the comment here does not even come close to describing wavelet algorithms and the
 * Daubechies D4 algorithm in particular. I don't think that it can be described in anything less than a journal article
 * or perhaps a book. I even have to apologize for the notation I use to describe the algorithm, which is barely
 * adequate. But explaining the correct notation would take a fair amount of space as well. This comment really
 * represents some notes that I wrote up as I implemented the code. If you are unfamiliar with wavelets I suggest that
 * you look at the bearcave.com web pages and at the wavelet literature. I have yet to see a really good reference on
 * wavelets for the software developer. The best book I can recommend is <i>Ripples in Mathematics</i> by Jensen and
 * Cour-Harbo.
 * </p>
 * <p>
 * All wavelet algorithms have two components, a wavelet function and a scaling function. These are sometime also
 * referred to as high pass and low pass filters respectively.
 * </p>
 * <p>
 * The wavelet function is passed two or more samples and calculates a wavelet coefficient. In the case of the Haar
 * wavelet this is
 * </p>
 * 
 * 
 * coef<sub>i</sub> = odd<sub>i</sub> - even<sub>i</sub><br>
 * or<br>
 * coef<sub>i</sub> = 0.5 * (odd<sub>i</sub> - even<sub>i</sub>)<br>
 * 
 * <p>
 * depending on the version of the Haar algorithm used.
 * </p>
 * <p>
 * The scaling function produces a smoother version of the original data. In the case of the Haar wavelet algorithm this
 * is an average of two adjacent elements.
 * </p>
 * <p>
 * The Daubechies D4 wavelet algorithm also has a wavelet and a scaling function. The coefficients for the scaling
 * function are denoted as h<sub>i</sub> and the wavelet coefficients are g<sub>i</sub>.
 * </p>
 * <p>
 * Mathematicians like to talk about wavelets in terms of a wavelet algorithm applied to an infinite data set. In this
 * case one step of the forward transform can be expressed as the infinite matrix of wavelet coefficients represented
 * below multiplied by the infinite signal vector.
 * </p>
 * 
 * <p>
 * a<sub>i</sub> = ...h0,h1,h2,h3, 0, 0, 0, 0, 0, 0, 0, ... s<sub>i</sub><br>
 * c<sub>i</sub> = ...g0,g1,g2,g3, 0, 0, 0, 0, 0, 0, 0, ... s<sub>i+1</sub><br>
 * a<sub>i+1</sub> = ...0, 0, h0,h1,h2,h3, 0, 0, 0, 0, 0, ... s<sub>i+2</sub><br>
 * c<sub>i+1</sub> = ...0, 0, g0,g1,g2,g3, 0, 0, 0, 0, 0, ... s<sub>i+3</sub><br>
 * a<sub>i+2</sub> = ...0, 0, 0, 0, h0,h1,h2,h3, 0, 0, 0, ... s<sub>i+4</sub><br>
 * c<sub>i+2</sub> = ...0, 0, 0, 0, g0,g1,g2,g3, 0, 0, 0, ... s<sub>i+5</sub><br>
 * a<sub>i+3</sub> = ...0, 0, 0, 0, 0, 0, h0,h1,h2,h3, 0, ... s<sub>i+6</sub><br>
 * c<sub>i+3</sub> = ...0, 0, 0, 0, 0, 0, g0,g1,g2,g3, 0, ... s<sub>i+7</sub><br>
 * 
 * <p>
 * The dot product (inner product) of the infinite vector and a row of the matrix produces either a smoother version of
 * the signal (a<sub>i</sub>) or a wavelet coefficient (c<sub>i</sub>).
 * </p>
 * <p>
 * In an ordered wavelet transform, the smoothed (a<sub>i</sub>) are stored in the first half of an <i>n</i> element
 * array region. The wavelet coefficients (c<sub>i</sub>) are stored in the second half the <i>n</i> element region. The
 * algorithm is recursive. The smoothed values become the input to the next step.
 * </p>
 * <p>
 * The transpose of the forward transform matrix above is used to calculate an inverse transform step. Here the dot
 * product is formed from the result of the forward transform and an inverse transform matrix row.
 * </p>
 * 
 *
 * s<sub>i</sub> = ...h2,g2,h0,g0, 0, 0, 0, 0, 0, 0, 0, ... a<sub>i</sub><br>
 * s<sub>i+1</sub> = ...h3,g3,h1,g1, 0, 0, 0, 0, 0, 0, 0, ... c<sub>i</sub><br>
 * s<sub>i+2</sub> = ...0, 0, h2,g2,h0,g0, 0, 0, 0, 0, 0, ... a<sub>i+1</sub><br>
 * s<sub>i+3</sub> = ...0, 0, h3,g3,h1,g1, 0, 0, 0, 0, 0, ... c<sub>i+1</sub><br>
 * s<sub>i+4</sub> = ...0, 0, 0, 0, h2,g2,h0,g0, 0, 0, 0, ... a<sub>i+2</sub><br>
 * s<sub>i+5</sub> = ...0, 0, 0, 0, h3,g3,h1,g1, 0, 0, 0, ... c<sub>i+2</sub><br>
 * s<sub>i+6</sub> = ...0, 0, 0, 0, 0, 0, h2,g2,h0,g0, 0, ... a<sub>i+3</sub><br>
 * s<sub>i+7</sub> = ...0, 0, 0, 0, 0, 0, h3,g3,h1,g1, 0, ... c<sub>i+3</sub><br>
 * 
 * <p>
 * Using a standard dot product is grossly inefficient since most of the operands are zero. In practice the wavelet
 * coefficient values are moved along the signal vector and a four element dot product is calculated. Expressed in terms
 * of arrays, for the forward transform this would be:
 * </p>
 * 
 * <p>
 * a<sub>i</sub> = s[i]*h0 + s[i+1]*h1 + s[i+2]*h2 + s[i+3]*h3<br>
 * c<sub>i</sub> = s[i]*g0 + s[i+1]*g1 + s[i+2]*g2 + s[i+3]*g3<br>
 * 
 * <p>
 * This works fine if we have an infinite data set, since we don't have to worry about shifting the coefficients "off
 * the end" of the signal.
 * </p>
 * <p>
 * I sometimes joke that I left my infinite data set in my other bear suit. The only problem with the algorithm
 * described so far is that we don't have an infinite signal. The signal is finite. In fact not only must the signal be
 * finite, but it must have a power of two number of elements.
 * </p>
 * <p>
 * If i=N-1, the i+2 and i+3 elements will be beyond the end of the array. There are a number of methods for handling
 * the wavelet edge problem. This version of the algorithm acts like the data is periodic, where the data at the start
 * of the signal wraps around to the end.
 * </p>
 * <p>
 * This algorithm uses a temporary array. A Lifting Scheme version of the Daubechies D4 algorithm does not require a
 * temporary. The matrix discussion above is based on material from <i>Ripples in Mathematics</i>, by Jensen and
 * Cour-Harbo. Any error are mine.
 * </p>
 * <p>
 * <b>Author</b>: Ian Kaplan<br>
 * <b>Use</b>: You may use this software for any purpose as long as I cannot be held liable for the result. Please
 * credit me with authorship if use use this source code.
 * </p>
 */
public class DaubechiesWavelet {

    protected final double sqrt_3 = Math.sqrt(3);
    protected final double denom = 4 * Math.sqrt(2);
    //
    // forward transform scaling (smoothing) coefficients
    //
    protected final double h0 = (1 + sqrt_3) / denom;
    protected final double h1 = (3 + sqrt_3) / denom;
    protected final double h2 = (3 - sqrt_3) / denom;
    protected final double h3 = (1 - sqrt_3) / denom;

    //
    // forward transform wavelet coefficients
    //
    protected final double g0 = h3;
    protected final double g1 = -h2;
    protected final double g2 = h1;
    protected final double g3 = -h0;

    //
    // Inverse transform coefficients for smoothed values
    //
    protected final double Ih0 = h2;
    protected final double Ih1 = g2; // h1
    protected final double Ih2 = h0;
    protected final double Ih3 = g0; // h3
    //
    // Inverse transform for wavelet values
    //
    protected final double Ig0 = h3;
    protected final double Ig1 = g3; // -h0
    protected final double Ig2 = h1;
    protected final double Ig3 = g1; // -h2

    /**
     * Forward Daubechies D4 transform
     * 
     * @param s input vector (result replaces original)
     */
    public void daubTrans(final double s[]) {
        final int N = s.length;
        int n;
        for (n = N; n >= 4; n >>= 1) {
            transform(s, n);
        }
    }

    /**
     * Inverse Daubechies D4 transform
     * 
     * @param coef input vector (result replaces original)
     */
    public void invDaubTrans(final double coef[]) {
        final int N = coef.length;
        int n;
        for (n = 4; n <= N; n <<= 1) {
            invTransform(coef, n);
        }
    }

    protected void invTransform(final double a[], final int n) {
        if (n >= 4) {
            int i, j;
            final int half = n >> 1;
            final int halfPls1 = half + 1;

            final double tmp[] = new double[n];

            // last smooth val last coef. first smooth first coef
            tmp[0] = a[half - 1] * Ih0 + a[n - 1] * Ih1 + a[0] * Ih2 + a[half] * Ih3;
            tmp[1] = a[half - 1] * Ig0 + a[n - 1] * Ig1 + a[0] * Ig2 + a[half] * Ig3;
            j = 2;
            for (i = 0; i < half - 1; i++) {
                // smooth val coef. val smooth val coef. val
                tmp[j++] = a[i] * Ih0 + a[i + half] * Ih1 + a[i + 1] * Ih2 + a[i + halfPls1] * Ih3;
                tmp[j++] = a[i] * Ig0 + a[i + half] * Ig1 + a[i + 1] * Ig2 + a[i + halfPls1] * Ig3;
            }
            for (i = 0; i < n; i++) {
                a[i] = tmp[i];
            }
        }
    }

    /**
     * <p>
     * Forward wavelet transform.
     * </p>
     * <p>
     * Note that at the end of the computation the calculation wraps around to the beginning of the signal.
     * </p>
     * 
     * @param a input vector
     * @param n size of input vector
     */
    protected void transform(final double a[], final int n) {
        if (n >= 4) {
            int i, j;
            final int half = n >> 1;

            final double tmp[] = new double[n];
            System.err.printf("h0 = %f h1 = %f h2 = %f h3= %f\n", h0, h1, h2, h3);
            i = 0;
            for (j = 0; j < n - 3; j = j + 2) {
                tmp[i] = a[j] * h0 + a[j + 1] * h1 + a[j + 2] * h2 + a[j + 3] * h3;
                tmp[i + half] = a[j] * g0 + a[j + 1] * g1 + a[j + 2] * g2 + a[j + 3] * g3;
                i++;
            }

            tmp[i] = a[n - 2] * h0 + a[n - 1] * h1 + a[0] * h2 + a[1] * h3;
            tmp[i + half] = a[n - 2] * g0 + a[n - 1] * g1 + a[0] * g2 + a[1] * g3;

            for (i = 0; i < n; i++) {
                a[i] = tmp[i];
            }
        }
    } // transform

} // daub
