/*
 * $Id: RandomDataGenerator.java,v 1.5 2008-12-11 13:46:35 emccrory Exp $
 *
 * $Date: 2008-12-11 13:46:35 $ $Revision: 1.5 $ $Author: emccrory $
 *
 * Copyright CERN, All Rights Reserved.
 */
package de.gsi.dataset.testdata.spi;

import java.util.Random;
import java.util.SplittableRandom;

/**
 * Generates arrays with random data.
 *
 * @version $Id: RandomDataGenerator.java,v 1.5 2008-12-11 13:46:35 emccrory Exp $
 */
@SuppressWarnings("PMD.VariableNamingConventions")
public final class RandomDataGenerator { // NOPMD nomen est omen
    private static final Random RND_DEPRECATED = new Random(System.currentTimeMillis());
    private static final SplittableRandom RND = new SplittableRandom(System.currentTimeMillis());
    private static final int NUMBER_OF_POINTS = 1000;
    private static final double X_MAX = 100.0; // Assuming that xMin is zero.
    private static final double PEAK = 10.0;
    private static final double BACK = RandomDataGenerator.PEAK / 10.0;
    private static final double Y_MAX = (RandomDataGenerator.PEAK + RandomDataGenerator.BACK) * 1.1;
    private static final double SATURATION_LEVEL = RandomDataGenerator.PEAK * 1.5;
    private static double noiseLevel = RandomDataGenerator.BACK / 2.0;
    private static double slope;
    private static double width;
    private static double center;
    private static double background;
    private static double amplitude;
    private static double[] xValues;
    private static double[] yValues;
    private static double nextNextGaussian;
    private static boolean haveNextNextGaussian = false;

    public static byte[] generateByteArray(final int size) {
        final byte[] data = new byte[size];

        RND_DEPRECATED.nextBytes(data);

        return data;
    }

    public static double[] generateDoubleArray(final double firstValue, final double variance, final int size) {
        final double[] data = new double[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            final int sign = RND.nextBoolean() ? 1 : -1;
            final double val = RND.nextInt() % 1000 / 1000.0;
            data[i] = data[i - 1] + variance * val * sign;
        }

        return data;
    }

    public static float[] generateFloatArray(final float firstValue, final float variance, final int size) {
        final float[] data = new float[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            final int sign = RND.nextBoolean() ? 1 : -1;
            final float val = (float) (RND.nextInt() % 1000 / 1000.0);
            data[i] = data[i - 1] + variance * val * sign;
        }

        return data;
    }

    public static int[] generateIntArray(final int firstValue, final int variance, final int size) {
        final int[] data = new int[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            final int sign = RND.nextBoolean() ? 1 : -1;
            data[i] = data[i - 1] + (int) (variance * RND.nextDouble()) * sign;
        }

        return data;
    }

    public static double[] generateZeroOneArray(final double firstValue, final int size) {
        final double[] data = new double[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            data[i] = RND.nextBoolean() ? 0 : 1;
        }

        return data;
    }

    public static synchronized double[] getGaussianX() {
        if (RandomDataGenerator.xValues == null) {
            RandomDataGenerator.getNew1DGaussian();
        }

        return RandomDataGenerator.xValues;
    }

    public static synchronized double[] getGaussianY() {
        if (RandomDataGenerator.yValues == null) {
            RandomDataGenerator.getNew1DGaussian();
        }

        return RandomDataGenerator.yValues;
    }

    //
    // ---------- For configuring the Gaussian data generator
    // ------------------------------------------------------------
    //

    public static synchronized void getNew1DGaussian() {
        RandomDataGenerator.slope = RandomDataGenerator.myRandom(-0.003, 0.003);
        RandomDataGenerator.width = RandomDataGenerator.X_MAX * RandomDataGenerator.myRandom(0.04, 0.07);
        RandomDataGenerator.center = RandomDataGenerator.X_MAX * RandomDataGenerator.myRandom(-0.02, 0.02);
        RandomDataGenerator.background = RandomDataGenerator.BACK * RandomDataGenerator.myRandom(0.5, 1.5);
        RandomDataGenerator.amplitude = RandomDataGenerator.PEAK * RandomDataGenerator.myRandom(0.9, 1.1);
        RandomDataGenerator.noiseLevel = 0.3;

        if (RandomDataGenerator.xValues == null) {
            RandomDataGenerator.xValues = new double[RandomDataGenerator.NUMBER_OF_POINTS];
            RandomDataGenerator.yValues = new double[RandomDataGenerator.NUMBER_OF_POINTS];
            for (int i = 0; i < RandomDataGenerator.NUMBER_OF_POINTS; i++) {
                RandomDataGenerator.xValues[i] = 2 * RandomDataGenerator.X_MAX * (0.5
                        - (double) (RandomDataGenerator.NUMBER_OF_POINTS - i) / RandomDataGenerator.NUMBER_OF_POINTS);
            }
        }

        for (int i = 0; i < RandomDataGenerator.NUMBER_OF_POINTS; i++) {
            RandomDataGenerator.yValues[i] = RandomDataGenerator.myRandom(0, RandomDataGenerator.noiseLevel)
                    + RandomDataGenerator.background
                    + RandomDataGenerator.slope * (RandomDataGenerator.xValues[i] - RandomDataGenerator.center)
                    + RandomDataGenerator.amplitude
                            * Math.exp(-(RandomDataGenerator.xValues[i] - RandomDataGenerator.center)
                                    * (RandomDataGenerator.xValues[i] - RandomDataGenerator.center)
                                    / RandomDataGenerator.width / RandomDataGenerator.width / 2);
        }

        for (int i = 0; i < RandomDataGenerator.NUMBER_OF_POINTS; i++) {
            RandomDataGenerator.yValues[i] = RandomDataGenerator.yValues[i] > RandomDataGenerator.SATURATION_LEVEL
                    ? RandomDataGenerator.SATURATION_LEVEL
                    : RandomDataGenerator.yValues[i];
        }
    }

    public static double myRandom(final double low, final double high) {
        assert high > low;
        return RND.nextDouble() * (high - low) + low;
    }

    synchronized static public double nextGaussian() {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }

        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * RND.nextDouble() - 1; // between -1 and 1
            v2 = 2 * RND.nextDouble() - 1; // between -1 and 1
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        final double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;
    }

    static public double random() {
        return RND.nextDouble();
    }
}