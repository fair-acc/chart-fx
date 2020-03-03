package de.gsi.math.spectra;

import java.util.Map;
import java.util.WeakHashMap;

import de.gsi.dataset.utils.AssertUtils;
import de.gsi.math.TMathConstants;

/**
 * implementation of frequenty used apodization (aka. windowing) functions reference:
 * http://en.wikipedia.org/wiki/Apodization_function - feel free to populate this with the other (however also less
 * frequently used) windows
 * The array for the windows are cached. By default a WeakHashMap is used, but the cache can be replaced e.g by
 * {@link de.gsi.dataset.utils.Cache} if different caching behaviour is wanted.
 *
 * @author rstein
 * @author akrimm
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

    private static Map<ApodizationArrayDescription, double[]> windowCache = new WeakHashMap<>();

    private String fname;

    private Apodization(final String name) {
        fname = name;
    }

    /**
     * computes and returns the value of the apodization function for a given window index
     *
     * @param i index within window
     * @param n length of window
     * @return value of the apodization function
     */
    public double getIndex(final int i, final int n) {
        return getWindow(n)[i];
    }

    /**
     * computes and returns the value of the apodization function for a given window index
     *
     * @param i index within window
     * @param n length of window
     * @param m additional window (typ. bandwidth, power, etc)
     * @return value of the apodization function
     */
    public double getIndex(final int i, final int n, final double m) {
        switch (this) {
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
        case Rectangular:
            return getIndex(i, n);
        }
    }

    /**
     * Directly computes a single window value. If the whole window is required multiple times
     * it will be faster to use the cached version {@link #getIndex(int, int)} provides.
     * 
     * @param i index within the window
     * @param n length of the window
     * @return value of the apodization function
     */
    public double getIndexUncached(final int i, final int n) {
        switch (this) {
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
        case Rectangular:
            return 1.0;
        }
    }

    public String getName() {
        return fname;
    }

    /**
     * Returns the window for a
     * 
     * @param n window size
     * @return a cached array containing the requested window
     */
    public double[] getWindow(final int n) {
        return windowCache.computeIfAbsent(new ApodizationArrayDescription(this, n), desc -> {
            final double[] window = new double[desc.length];
            for (int i = 0; i < desc.length; i++) {
                window[i] = getIndexUncached(i, desc.length);
            }
            return window;
        });
    }

    /**
     * Applies the given apodization window to the given array (in-place)
     * 
     * @param data input data
     */
    public void apodize(final double[] data) {
        final double[] window = getWindow(data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] * window[i];
        }
    }

    /**
     * Applies the given apodization window to the given array (in-place)
     * 
     * @param data input data
     * @param function the apodization function to use
     */
    public static void apodize(final double[] data, final Apodization function) {
        function.apodize(data);
    }

    /**
     * @return the global cache for the apodization windows
     */
    public static Map<ApodizationArrayDescription, double[]> getWindowCache() {
        return windowCache;
    }

    /**
     * Changes the map used to cache the apodization windows.
     * The map should implement some sort of caching behaviour e.g
     * <ul>
     * <li>{@link java.util.WeakHashMap} {@code<ApodizationArrayDescription, double[]>} invalidates under memory
     * pressure (The default)
     * <li>{@link de.gsi.dataset.utils.Cache} limits retention time and number of cached entries
     * <li>custom implementation
     * </ul>
     * 
     * @param windowCache A map to use as a cache for the apodization windows
     */
    public static void setWindowCache(Map<ApodizationArrayDescription, double[]> windowCache) {
        Apodization.windowCache = windowCache;
    }

    /**
     * private class identifying a specific buffer array, used as a key for the array cache
     */
    protected static class ApodizationArrayDescription {
        final protected Apodization apodization;
        final protected int length;

        /**
         * @param apodization
         * @param length
         */
        public ApodizationArrayDescription(Apodization apodization, int length) {
            AssertUtils.notNull("apodization", apodization);
            this.apodization = apodization;
            this.length = length;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ApodizationArrayDescription)) {
                return false;
            }
            ApodizationArrayDescription other = (ApodizationArrayDescription) obj;
            return (apodization == other.apodization && length == other.length);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + apodization.hashCode();
            result = prime * result + length;
            return result;
        }
    }
}
