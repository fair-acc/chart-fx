package de.gsi.math;

/**
 * Simple quick-and-dirty math lookup table
 * TODO: evaluate whether this should be replaced by the better implementations in:
 * https://github.com/jeffhain/jafama
 * (N.B. main concern: dependency on yet another external library)
 *
 * @author rstein
 */
public class FastMath {
    private static final int DEFAULT_TRIG_RESOLUTION = 3600; // gradations full circle (2*\pi, 360 deg resp)
    private static int precision = DEFAULT_TRIG_RESOLUTION;
    private static int modulusRad;
    private static int modulusRadQuater;
    private static int modulusDeg;
    private static float[] sinRadLookup; // sine lookup table [rad]
    private static float[] sinDegLockup; // sine lookup table [deg]
    static {
        init();
    }

    private FastMath() {
        // static helper class
    }

    public static double cos(double a) {
        return sinLocalLookUp((int) (a * precision + modulusRadQuater + 0.5));
    }

    public static float cos(float a) {
        return sinLocalLookUp((int) (a * precision + modulusRadQuater + 0.5f));
    }

    public static double cosDeg(double a) {
        return sinLocalLookUpDegree((int) ((a + 90f) * precision + 0.5f));
    }

    public static float cosDeg(float a) {
        return sinLocalLookUpDegree((int) ((a + 90f) * precision + 0.5f));
    }

    public static int getPrecision() {
        return precision;
    }

    public static void setPrecision(int precision) {
        if (precision <= 0) {
            throw new IllegalArgumentException("precision '" + precision + "'must be positive");
        }
        FastMath.precision = precision;
        init();
    }

    public static double sin(double a) {
        return sinLocalLookUp((int) (a * precision + 0.5));
    }

    public static float sin(float a) {
        return sinLocalLookUp((int) (a * precision + 0.5f));
    }

    public static double sinDeg(double a) {
        return sinLocalLookUpDegree((int) (a * precision + 0.5));
    }

    public static float sinDeg(float a) {
        return sinLocalLookUpDegree((int) (a * precision + 0.5f));
    }

    private static void init() {
        modulusRad = (int) (2.0 * Math.PI * precision);
        modulusRadQuater = modulusRad >> 2;
        modulusDeg = 360 * precision;
        sinRadLookup = new float[modulusRad]; // sine lookup table [rad]
        sinDegLockup = new float[modulusDeg]; // sine lookup table [deg]

        for (int i = 0; i < modulusRad; i++) {
            sinRadLookup[i] = (float) Math.sin((double) i / precision);
        }
        for (int i = 0; i < modulusDeg; i++) {
            sinDegLockup[i] = (float) Math.sin(Math.toRadians((double) i / precision));
        }
    }

    private static float sinLocalLookUp(int a) {
        return a >= 0 ? sinRadLookup[a % (modulusRad)] : -sinRadLookup[-a % (modulusRad)];
    }

    private static float sinLocalLookUpDegree(int a) {
        return a >= 0 ? sinDegLockup[a % (modulusDeg)] : -sinDegLockup[-a % (modulusDeg)];
    }
}
