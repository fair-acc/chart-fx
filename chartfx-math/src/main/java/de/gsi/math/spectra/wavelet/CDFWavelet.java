package de.gsi.math.spectra.wavelet;

public class CDFWavelet {
    /**
     * Cohen-Daubechies-Feauveau
     * forward biorthogonal 9/7 wavelet transform (lifting implementation)

     * The first half part of the output signal contains the approximation coefficients.
     * The second half part contains the detail coefficients (aka. wavelets coefficients).
     * 
     * @param x input signal, which will be replaced by its output transform
     * @param n length of the signal, and must be a power of 2
     */
    public void fwt97(final double[] x, final int n) {
        double a;
        int i;

        // Predict 1
        a = -1.586134342;
        for (i = 1; i < n - 2; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[n - 1] += 2 * a * x[n - 2];

        // Update 1
        a = -0.05298011854;
        for (i = 2; i < n; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[0] += 2 * a * x[1];

        // Predict 2
        a = 0.8829110762;
        for (i = 1; i < n - 2; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[n - 1] += 2 * a * x[n - 2];

        // Update 2
        a = 0.4435068522;
        for (i = 2; i < n; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[0] += 2 * a * x[1];

        // Scale
        a = 1 / 1.149604398;
        for (i = 0; i < n; i++) {
            if (i % 2 > 0) {
                x[i] *= a;
            } else {
                x[i] /= a;
            }
        }

        // Pack
        double[] tempbank = null;
        if (tempbank == null) {
            tempbank = new double[n];
        }

        for (i = 0; i < n; i++) {
            if (i % 2 == 0) {
                tempbank[i / 2] = x[i];
            } else {
                tempbank[n / 2 + i / 2] = x[i];
            }
        }
        for (i = 0; i < n; i++) {
            x[i] = tempbank[i];
        }
    }

    /**
     * iwt97 - Inverse biorthogonal 9/7 wavelet transform
     * This is the inverse of fwt97 so that iwt97(fwt97(x,n),n)=x for every signal x of length n.
     * See also fwt97.
     *      
     * @param x input signal, which will be replaced by its output transform
     * @param n length of the signal, and must be a power of 2
     */
    public void iwt97(final double[] x, final int n) {
        double a;
        int i;

        // Unpack
        double[] tempbank = null;
        if (tempbank == null) {
            tempbank = new double[n];
        }

        for (i = 0; i < n / 2; i++) {
            tempbank[i * 2] = x[i];
            tempbank[i * 2 + 1] = x[i + n / 2];
        }
        for (i = 0; i < n; i++) {
            x[i] = tempbank[i];
        }

        // Undo scale
        a = 1.149604398;
        for (i = 0; i < n; i++) {
            if (i % 2 > 0) {
                x[i] *= a;
            } else {
                x[i] /= a;
            }
        }

        // Undo update 2
        a = -0.4435068522;
        for (i = 2; i < n; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[0] += 2 * a * x[1];

        // Undo predict 2
        a = -0.8829110762;
        for (i = 1; i < n - 2; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[n - 1] += 2 * a * x[n - 2];

        // Undo update 1
        a = 0.05298011854;
        for (i = 2; i < n; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[0] += 2 * a * x[1];

        // Undo predict 1
        a = 1.586134342;
        for (i = 1; i < n - 2; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[n - 1] += 2 * a * x[n - 2];
    }

    /**
     * fwt53 - Forward biorthogonal 5/3 wavelet transform (lifting implementation)
     * x is an input signal, which will be replaced by its output transform.
     * n is the length of the signal, and must be a power of 2.
     * The first half part of the output signal contains the approximation coefficients.
     * The second half part contains the detail coefficients (aka. the wavelets coefficients).
     * See also iwt53.
     * 
     * @param x input signal, which will be replaced by its output transform
     * @param n length of the signal, and must be a power of 2
     */
    public void fwt53(final double[] x, final int n) {
        double a;
        int i;

        // Predict 1
        a = -0.5;
        for (i = 1; i < n - 2; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[n - 1] += 2 * a * x[n - 2];

        // Update 1
        a = 0.25;
        for (i = 2; i < n; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[0] += 2 * a * x[1];

        // Scale
        a = Math.sqrt(2.0);
        for (i = 0; i < n; i++) {
            if (i % 2 > 0) {
                x[i] *= a;
            } else {
                x[i] /= a;
            }
        }

        // Pack
        double[] tempbank = null;
        if (tempbank == null) {
            tempbank = new double[n];
        }

        for (i = 0; i < n; i++) {
            if (i % 2 == 0) {
                tempbank[i / 2] = x[i];
            } else {
                tempbank[n / 2 + i / 2] = x[i];
            }
        }
        for (i = 0; i < n; i++) {
            x[i] = tempbank[i];
        }
    }

    /**
     * iwt53 - Inverse biorthogonal 5/3 wavelet transform
     * This is the inverse of fwt53 so that iwt53(fwt53(x,n),n)=x for every signal x of length n.
     * See also fwt53.
     * 
     * @param x input signal, which will be replaced by its output transform
     * @param n length of the signal, and must be a power of 2
     */
    public void iwt53(final double[] x, final int n) {
        double a;
        int i;

        // Unpack
        double[] tempbank = null;
        if (tempbank == null) {
            tempbank = new double[n];
        }
        for (i = 0; i < n / 2; i++) {
            tempbank[i * 2] = x[i];
            tempbank[i * 2 + 1] = x[i + n / 2];
        }
        for (i = 0; i < n; i++) {
            x[i] = tempbank[i];
        }

        // Undo scale
        a = 1 / Math.sqrt(2.0);
        for (i = 0; i < n; i++) {
            if (i % 2 > 0) {
                x[i] *= a;
            } else {
                x[i] /= a;
            }
        }

        // Undo update 1
        a = -0.25;
        for (i = 2; i < n; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[0] += 2 * a * x[1];

        // Undo predict 1
        a = 0.5;
        for (i = 1; i < n - 2; i += 2) {
            x[i] += a * (x[i - 1] + x[i + 1]);
        }
        x[n - 1] += 2 * a * x[n - 2];
    }

    public static void main(final String argv[]) {
        final CDFWavelet wtf = new CDFWavelet();
        final double[] x = new double[32];

        // Makes a fancy cubic signal
        for (int i = 0; i < 32; i++) {
            x[i] = 5 + i + 0.4 * i * i - 0.02 * i * i * i;
        }

        // Prints original signal x
        System.out.printf("Original signal:\n");
        for (int i = 0; i < 32; i++) {
            System.out.printf("x[%d]=%f\n", i, x[i]);
        }
        System.out.printf("\n");

        // Do the forward 9/7 transform
        wtf.fwt97(x, 32);

        // Prints the wavelet coefficients
        System.out.printf("Wavelets coefficients:\n");
        for (int i = 0; i < 32; i++) {
            System.out.printf("wc[%d]=%f\n", i, x[i]);
        }
        System.out.printf("\n");

        // Do the inverse 9/7 transform
        wtf.iwt97(x, 32);

        // Prints the reconstructed signal
        System.out.printf("Reconstructed signal:\n");
        for (int i = 0; i < 32; i++) {
            System.out.printf("xx[%d]=%f\n", i, x[i]);
        }
    }
}
