package de.gsi.dataset.spi.utils;

/**
 * small helper class from:
 * https://floating-point-gui.de/errors/NearlyEqualsTest.java
 * 
 * @author Michael Borgwardt
 */
public class MathUtils {
    public static boolean nearlyEqual(double a, double b) {
        return nearlyEqual(a, b, 1e-14);
    }

    public static boolean nearlyEqual(double a, double b, double epsilon) {
        final double absA = Math.abs(a);
        final double absB = Math.abs(b);
        final double diff = Math.abs(a - b);

        if (a == b) {
            // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || absA + absB < Double.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            return diff < (epsilon * Double.MIN_NORMAL);
        } else {
            // use relative error
            return diff / Math.min((absA + absB), Double.MAX_VALUE) < epsilon;
        }
    }

    public static boolean nearlyEqual(float a, float b) {
        return nearlyEqual(a, b, 0.00001f);
    }
    
    public static boolean nearlyEqual(float a, float b, float epsilon) {
        final float absA = Math.abs(a);
        final float absB = Math.abs(b);
        final float diff = Math.abs(a - b);

        if (a == b) { // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || (absA + absB < Float.MIN_NORMAL)) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            return diff < (epsilon * Float.MIN_NORMAL);
        } else { // use relative error
            return diff / Math.min((absA + absB), Float.MAX_VALUE) < epsilon;
        }
    }

    // http://stackoverflow.com/questions/3728246/what-should-be-the-
    // epsilon-value-when-performing-double-value-equal-comparison
    // ULP = Unit in Last Place
    public static double relativeEpsilon(double a, double b) {
        return Math.max(Math.ulp(a), Math.ulp(b));
    }

    public static float relativeEpsilon(float a, float b) {
        return Math.max(Math.ulp(a), Math.ulp(b));
    }
}
