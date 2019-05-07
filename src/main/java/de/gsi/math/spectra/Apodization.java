package de.gsi.math.spectra;

import de.gsi.math.TMathConstants;

/**
 * implementation of frequenty used apodization (aka. windowing) functions reference:
 * http://en.wikipedia.org/wiki/Apodization_function - feel free to populate this with the other (however also less
 * frequently used) windows
 *
 * @author rstein
 */
public enum Apodization {
    Rectangular("rectangular"),
    Hamming("Hamming"),
    Hann("von Hann"),
    HannExp("exp. von Hann"),
    Blackman("Blackman"),
    Nuttall("Nuttall"),
    BlackmanHarris("Blackman-Harris"),
    BlackmanNuttall("Blackman-Nuttall"),
    FlatTop("Flat-Top"),
    Exponential("exponential");

    private String fname;

    private Apodization(final String name) {
        fname = name;
    }

    public String getName() {
        return fname;
    }

    /**
     * computes and returns the value of the apodization function for a given window index
     *
     * @param j index within window
     * @param n length of window @return, the apodization value
     */
    public double getIndex(final int i, final int n) {
        switch (this) {
        case Rectangular:
            return 1.0;
        case Hamming: {
            // final double a = TMath.TwoPi()/(n-1);
            // the following Hamming window definition is more common
            final double a = TMathConstants.TwoPi() / n;
            return 0.53836 - 0.46164 * TMathConstants.Cos(a * i);
        }
        case Hann: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 0.5 - 0.5 * TMathConstants.Cos(a * i);
        }
        case HannExp: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 1.0 * TMathConstants.Power(TMathConstants.Sin(a * i), 2.0);
        }
        case Blackman: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 0.42 - 0.5 * TMathConstants.Cos(a * i) + 0.08 * TMathConstants.Cos(2 * a * i);
        }
        case Nuttall: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 0.355768 - 0.487396 * TMathConstants.Cos(a * i) + 0.144232 * TMathConstants.Cos(2 * a * i)
                    - 0.012604 * TMathConstants.Cos(3 * a * i);
        }
        case BlackmanHarris: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 0.35875 - 0.48829 * TMathConstants.Cos(a * i) + 0.14128 * TMathConstants.Cos(2 * a * i)
                    - 0.01168 * TMathConstants.Cos(3 * a * i);
        }
        case BlackmanNuttall: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 0.3635819 - 0.4891775 * TMathConstants.Cos(a * i) + 0.1365995 * TMathConstants.Cos(2 * a * i)
                    - 0.0106411 * TMathConstants.Cos(3 * a * i);
        }
        case FlatTop: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 1 - 1.93 * TMathConstants.Cos(a * i) + 1.29 * TMathConstants.Cos(2 * a * i)
                    - 0.388 * TMathConstants.Cos(3 * a * i) + 0.032 * TMathConstants.Cos(4 * a * i);
        }
        case Exponential: {
            final double a = 3 * n, exp0 = TMathConstants.Exp(0);
            return TMathConstants.Exp(i / a) / exp0;
        }
        default:
            return 1.0;
        }
    }

    /**
     * computes and returns the value of the apodization function for a given window index
     *
     * @param j index within window
     * @param n length of window
     * @param m additional window (typ. bandwidth, power, etc) @return, the apodization value
     */
    public double getIndex(final int i, final int n, final double m) {
        switch (this) {
        case Rectangular:
            return getIndex(i, n);
        case Hamming:
            return getIndex(i, n);
        case Hann:
            return getIndex(i, n);
        case HannExp: {
            final double a = TMathConstants.TwoPi() / (n - 1);
            return 1.0 * TMathConstants.Power(TMathConstants.Sin(a * i), m);
        }
        case Nuttall:
            return getIndex(i, n);
        case BlackmanHarris:
            return getIndex(i, n);
        case BlackmanNuttall:
            return getIndex(i, n);
        case FlatTop:
            return getIndex(i, n);
        case Blackman:
            return getIndex(i, n);
        case Exponential:
            return getIndex(i, n);
        default:
            return 1.0;
        }
    }

    public static void apodize(final double[] data, final Apodization function) {
        for (int i = 0; i < data.length; i++) {
            final double val = function.getIndex(i, data.length);
            data[i] = data[i] * val;
        }
    }

    public static void main(final String argv[]) {
        System.out.println(Apodization.Hann.getName() + " index (240,1024) = " + Apodization.Hann.getIndex(240, 1024));
    }
}
