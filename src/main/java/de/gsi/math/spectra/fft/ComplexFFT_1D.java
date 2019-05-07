package de.gsi.math.spectra.fft;

import org.apache.commons.math3.complex.Complex;

/**
 * classic FFT implementation based on the Complex data object
 *
 * @author rstein
 */
public class ComplexFFT_1D {
    private static Complex[][] coef; // FFT coefficients (calculated once
    private static int coef_order = 0; // power of two till the coefficients are computed

    // compute the FFT of x[], assuming its length is a power of 2
    public static Complex[] fft(final Complex[] x) {
        final int N = x.length;

        // base case
        if (N == 1) {
            return new Complex[] { x[0] };
        }

        // radix 2 Cooley-Tukey FFT
        if (N % 2 != 0) {
            throw new RuntimeException("N is not a power of 2");
        }

        // pre-calculate FFT coefficients - once
        final int base = (int) (Math.log(N / 2) / Math.log(2)) + 1;
        boolean precalculate = true;

        if (base > 17) {
            precalculate = false;
        }

        if ((coef == null || base > coef_order) && precalculate) {
            // System.out.printf("compute FFT coefficients\n");
            // compute usually coefficients only required up to 2^16 point FFT
            int order = 0;
            if (base < 16) {
                order = 16;
            } else {
                order = base + 1;
            }
            coef = new Complex[order][];
            for (int i = 0; i < order; i++) {
                coef[i] = new Complex[(int) Math.pow(2, i)];
                for (int j = 0; j < (int) Math.pow(2, i); j++) {
                    final double kth = -2 * j * Math.PI / Math.pow(2, i);
                    coef[i][j] = new Complex(Math.cos(kth), Math.sin(kth));
                }
            }
            coef_order = order;
            // System.out.printf("compute FFT coefficients - done\n");
        }

        // fft of even terms
        final Complex[] even = new Complex[N / 2];
        for (int k = 0; k < N / 2; k++) {
            even[k] = x[2 * k];
        }
        final Complex[] q = fft(even);

        // fft of odd terms
        final Complex[] odd = even; // reuse the array
        for (int k = 0; k < N / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        final Complex[] r = fft(odd);

        // combine
        final Complex[] y = new Complex[N];

        for (int k = 0; k < N / 2; k++) {
            if (precalculate) {
                y[k] = q[k].add(coef[base][k].multiply(r[k]));
                y[k + N / 2] = q[k].subtract(coef[base][k].multiply(r[k]));
            } else {
                final double kth = -2 * k * Math.PI / N;
                final Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
                y[k] = q[k].add(wk.multiply(r[k]));
                y[k + N / 2] = q[k].subtract(wk.multiply(r[k]));
            }
        }

        return y;
    }

    public static Complex[] fft_orig(final Complex[] x) {
        final int N = x.length;

        // base case
        if (N == 1) {
            return new Complex[] { x[0] };
        }

        // radix 2 Cooley-Tukey FFT
        if (N % 2 != 0) {
            throw new RuntimeException("N is not a power of 2");
        }

        // fft of even terms
        final Complex[] even = new Complex[N / 2];
        for (int k = 0; k < N / 2; k++) {
            even[k] = x[2 * k];
        }
        final Complex[] q = fft(even);

        // fft of odd terms
        final Complex[] odd = even; // reuse the array
        for (int k = 0; k < N / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        final Complex[] r = fft(odd);

        // combine
        final Complex[] y = new Complex[N];
        for (int k = 0; k < N / 2; k++) {
            final double kth = -2 * k * Math.PI / N;
            final Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].add(wk.multiply(r[k]));
            y[k + N / 2] = q[k].subtract(wk.multiply(r[k]));
        }
        return y;
    }

    // compute the inverse FFT of x[], assuming its length is a power of 2
    public static Complex[] ifft(final Complex[] x) {
        final int N = x.length;
        Complex[] y = new Complex[N];

        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < N; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by N
        for (int i = 0; i < N; i++) {
            y[i] = y[i].multiply(1.0 / N);
        }

        return y;

    }

    // compute the circular convolution of x and y
    public static Complex[] cconvolve(final Complex[] x, final Complex[] y) {

        // should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.length != y.length) {
            throw new RuntimeException("Dimensions don't agree");
        }

        final int N = x.length;

        // compute FFT of each sequence
        final Complex[] a = fft(x);
        final Complex[] b = fft(y);

        // point-wise multiply
        final Complex[] c = new Complex[N];
        for (int i = 0; i < N; i++) {
            c[i] = a[i].multiply(b[i]);
        }

        // compute inverse FFT
        return ifft(c);
    }

    // compute the linear convolution of x and y
    public static Complex[] convolve(final Complex[] x, final Complex[] y) {
        final Complex ZERO = new Complex(0, 0);

        final Complex[] a = new Complex[2 * x.length];
        for (int i = 0; i < x.length; i++) {
            a[i] = x[i];
        }
        for (int i = x.length; i < 2 * x.length; i++) {
            a[i] = ZERO;
        }

        final Complex[] b = new Complex[2 * y.length];
        for (int i = 0; i < y.length; i++) {
            b[i] = y[i];
        }
        for (int i = y.length; i < 2 * y.length; i++) {
            b[i] = ZERO;
        }

        return cconvolve(a, b);
    }

    // test client
    public static void main(final String[] args) {
        // int N = Integer.parseInt(args[0]);
        final int N = 1024;
        final Complex[] x = new Complex[N];

        // original data
        for (int i = 0; i < N; i++) {
            x[i] = new Complex(i, 0);
            x[i] = new Complex(-2 * Math.random() + 1, 0);
        }
        System.out.println("x");
        System.out.println("-------------------");
        for (int i = 0; i < N; i++) {
            System.out.println(x[i]);
        }
        System.out.println();

        // FFT of original data
        final Complex[] y = fft(x);
        System.out.println("y = fft(x)");
        System.out.println("-------------------");
        for (int i = 0; i < N; i++) {
            System.out.println(y[i]);
        }
        System.out.println();

        // take inverse FFT
        final Complex[] z = ifft(y);
        System.out.println("z = ifft(y)");
        System.out.println("-------------------");
        for (int i = 0; i < N; i++) {
            System.out.println(z[i]);
        }
        System.out.println();

        // circular convolution of x with itself
        final Complex[] c = cconvolve(x, x);
        System.out.println("c = cconvolve(x, x)");
        System.out.println("-------------------");
        for (int i = 0; i < N; i++) {
            System.out.println(c[i]);
        }
        System.out.println();

        // linear convolution of x with itself
        final Complex[] d = convolve(x, x);
        System.out.println("d = convolve(x, x)");
        System.out.println("-------------------");
        for (int i = 0; i < d.length; i++) {
            System.out.println(d[i]);
        }
        System.out.println();

    }

}