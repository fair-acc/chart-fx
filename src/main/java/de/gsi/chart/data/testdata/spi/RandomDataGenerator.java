/*
 * $Id: RandomDataGenerator.java,v 1.5 2008-12-11 13:46:35 emccrory Exp $
 *
 * $Date: 2008-12-11 13:46:35 $ $Revision: 1.5 $ $Author: emccrory $
 *
 * Copyright CERN, All Rights Reserved.
 */
package de.gsi.chart.data.testdata.spi;

import java.util.Random;
import java.util.SplittableRandom;

/**
 * Generates arrays with random data.
 *
 * @version $Id: RandomDataGenerator.java,v 1.5 2008-12-11 13:46:35 emccrory Exp $
 */
public class RandomDataGenerator {
    final static Random rndDeprecated = new Random(System.currentTimeMillis());
    final static SplittableRandom rnd = new SplittableRandom(System.currentTimeMillis());

    public static double[] generateZeroOneArray(final double firstValue, final int size) {
        final double[] data = new double[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            data[i] = rnd.nextBoolean() ? 0 : 1;
        }

        return data;
    }

    public static double[] generateDoubleArray(final double firstValue, final double variance, final int size) {
        final double[] data = new double[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            final int sign = rnd.nextBoolean() ? 1 : -1;
            final double val = rnd.nextInt() % 1000 / 1000.0;
            data[i] = data[i - 1] + variance * val * sign;
        }

        return data;
    }

    public static float[] generateFloatArray(final float firstValue, final float variance, final int size) {
        final float[] data = new float[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            final int sign = rnd.nextBoolean() ? 1 : -1;
            final float val = (float) (rnd.nextInt() % 1000 / 1000.0);
            data[i] = data[i - 1] + variance * val * sign;
        }

        return data;
    }

    public static int[] generateIntArray(final int firstValue, final int variance, final int size) {
        final int[] data = new int[size];

        data[0] = firstValue;
        for (int i = 1; i < data.length; i++) {
            final int sign = rnd.nextBoolean() ? 1 : -1;
            data[i] = data[i - 1] + (int) (variance * rnd.nextDouble()) * sign;
        }

        return data;
    }

    public static byte[] generateByteArray(final int size) {
        final byte[] data = new byte[size];

        rndDeprecated.nextBytes(data);

        return data;
    }

    private static double nextNextGaussian;
    private static boolean haveNextNextGaussian = false;

    synchronized static public double nextGaussian() {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }

        double v1, v2, s;
        do {
            v1 = 2 * rnd.nextDouble() - 1; // between -1 and 1
            v2 = 2 * rnd.nextDouble() - 1; // between -1 and 1
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        final double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;
    }

    static public double random() {
        return rnd.nextDouble();
    }

    //
    // ---------- For configuring the Gaussian data generator
    // ------------------------------------------------------------
    //

    public static double myRandom(final double low, final double high) {
        assert high > low;
        return rnd.nextDouble() * (high - low) + low;
    }

    final static int numberOfPoints = 1000;
    final static double xMax = 100.0; // Assuming that xMin is zero.
    final static double peak = 10.0;
    final static double back = RandomDataGenerator.peak / 10.0;
    final static double yMax = (RandomDataGenerator.peak + RandomDataGenerator.back) * 1.1;
    final static double sarurationLevel = RandomDataGenerator.peak * 1.5;
    private static double noiseLevel = RandomDataGenerator.back / 2.0;
    private static double slope;
    private static double width;
    private static double center;
    private static double background;
    private static double amplitude;
    private static double[] xValues;
    private static double[] yValues;

    public static synchronized void getNew1DGaussian() {
        RandomDataGenerator.slope = RandomDataGenerator.myRandom(-0.003, 0.003);
        RandomDataGenerator.width = RandomDataGenerator.xMax * RandomDataGenerator.myRandom(0.04, 0.07);
        RandomDataGenerator.center = RandomDataGenerator.xMax * RandomDataGenerator.myRandom(-0.02, 0.02);
        RandomDataGenerator.background = RandomDataGenerator.back * RandomDataGenerator.myRandom(0.5, 1.5);
        RandomDataGenerator.amplitude = RandomDataGenerator.peak * RandomDataGenerator.myRandom(0.9, 1.1);
        RandomDataGenerator.noiseLevel = 0.3;

        if (RandomDataGenerator.xValues == null) {
            RandomDataGenerator.xValues = new double[RandomDataGenerator.numberOfPoints];
            RandomDataGenerator.yValues = new double[RandomDataGenerator.numberOfPoints];
            for (int i = 0; i < RandomDataGenerator.numberOfPoints; i++) {
                RandomDataGenerator.xValues[i] = 2 * RandomDataGenerator.xMax * (0.5
                        - (double) (RandomDataGenerator.numberOfPoints - i) / RandomDataGenerator.numberOfPoints);
            }
        }

        for (int i = 0; i < RandomDataGenerator.numberOfPoints; i++) {
            RandomDataGenerator.yValues[i] = RandomDataGenerator.myRandom(0, RandomDataGenerator.noiseLevel)
                    + RandomDataGenerator.background
                    + RandomDataGenerator.slope * (RandomDataGenerator.xValues[i] - RandomDataGenerator.center)
                    + RandomDataGenerator.amplitude
                            * Math.exp(-(RandomDataGenerator.xValues[i] - RandomDataGenerator.center)
                                    * (RandomDataGenerator.xValues[i] - RandomDataGenerator.center)
                                    / RandomDataGenerator.width / RandomDataGenerator.width / 2);
        }

        for (int i = 0; i < RandomDataGenerator.numberOfPoints; i++) {
            RandomDataGenerator.yValues[i] = RandomDataGenerator.yValues[i] > RandomDataGenerator.sarurationLevel
                    ? RandomDataGenerator.sarurationLevel : RandomDataGenerator.yValues[i];
        }
    }

    public static double[] getGaussianY() {
        if (RandomDataGenerator.yValues == null) {
            RandomDataGenerator.getNew1DGaussian();
        }

        return RandomDataGenerator.yValues;
    }

    public static double[] getGaussianX() {
        if (RandomDataGenerator.xValues == null) {
            RandomDataGenerator.getNew1DGaussian();
        }

        return RandomDataGenerator.xValues;
    }

    public static void main(final String[] args) {
    }
}