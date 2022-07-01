package io.fair_acc.math.spectra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.WeakHashMap;

import org.junit.jupiter.api.Test;

import io.fair_acc.math.spectra.Apodization.ApodizationArrayDescription;

/**
 * Test the apodization class
 * 
 * @author Alexander Krimm
 */
class ApodizationTests {
    private static final double[] RECTANGULAR8 = new double[] { 1, 1, 1, 1, 1, 1, 1, 1 };
    private static final double[] BLACKMAN8 = new double[] { -1.3877787807814457E-17, 0.09045342435412804,
        0.45918295754596355, 0.9203636180999081, 0.9203636180999083, 0.45918295754596383, 0.09045342435412812,
        -1.3877787807814457E-17 };
    private static final double[] BLACKMANHARRIS8 = new double[] { 6.0000000000001025E-5, 0.03339172347815117,
        0.332833504298565, 0.8893697722232837, 0.8893697722232838, 0.3328335042985652, 0.03339172347815122,
        6.0000000000001025E-5 };
    private static final double[] BLACKMANNUTTAL8 = new double[] { 3.628000000000381E-4, 0.03777576895352025,
        0.34272761996881945, 0.8918518610776603, 0.8918518610776603, 0.3427276199688196, 0.0377757689535203,
        3.628000000000381E-4 };
    private static final double[] EXPONENTIAL8 = new double[] { 1.0, 1.0425469051899914, 1.086904049521229,
        1.1331484530668263, 1.1813604128656459, 1.2316236423470497, 1.2840254166877414, 1.338656724353094 };
    private static final double[] FLATTOP8 = new double[] { 0.004000000000000087, -0.16964240541774014,
        0.04525319347985671, 3.622389211937882, 3.6223892119378833, 0.04525319347985735, -0.16964240541774012,
        0.004000000000000087 };
    private static final double[] HAMMING8 = new double[] { 0.07671999999999995, 0.2119312255330421, 0.53836,
        0.8647887744669578, 1.0, 0.8647887744669578, 0.5383600000000001, 0.21193122553304222 };
    private static final double[] HANN8 = new double[] { 0.0, 0.1882550990706332, 0.6112604669781572,
        0.9504844339512095, 0.9504844339512095, 0.6112604669781573, 0.1882550990706333, 0.0 };
    private static final double[] HANNEXP8 = new double[] { 0.0, 0.6112604669781572, 0.9504844339512096,
        0.18825509907063334, 0.18825509907063315, 0.9504844339512096, 0.6112604669781574, 5.99903913064743E-32 };
    private static final double[] NUTTAL8 = new double[] { -2.42861286636753E-17, 0.031142736797915613,
        0.3264168059086425, 0.8876284572934416, 0.8876284572934416, 0.32641680590864275, 0.031142736797915654,
        -2.42861286636753E-17 };

    @Test()
    public void testName() {
        assertEquals("rectangular", Apodization.Rectangular.getName());
    }

    @Test
    public void testApodization() {
        // check all windows for unwanted changes
        assertArrayEquals(RECTANGULAR8, Apodization.Rectangular.getWindow(8));
        assertArrayEquals(BLACKMAN8, Apodization.Blackman.getWindow(8));
        assertArrayEquals(BLACKMANHARRIS8, Apodization.BlackmanHarris.getWindow(8));
        assertArrayEquals(BLACKMANNUTTAL8, Apodization.BlackmanNuttall.getWindow(8));
        assertArrayEquals(EXPONENTIAL8, Apodization.Exponential.getWindow(8));
        assertArrayEquals(FLATTOP8, Apodization.FlatTop.getWindow(8));
        assertArrayEquals(HAMMING8, Apodization.Hamming.getWindow(8));
        assertArrayEquals(HANN8, Apodization.Hann.getWindow(8));
        assertArrayEquals(HANNEXP8, Apodization.HannExp.getWindow(8));
        assertArrayEquals(NUTTAL8, Apodization.Nuttall.getWindow(8));
        // check single values
        assertEquals(RECTANGULAR8[0], Apodization.Rectangular.getIndex(0, 8));
        // check single values with additional argument
        assertEquals(RECTANGULAR8[4], Apodization.Rectangular.getIndex(4, 8, 2.0));
        assertEquals(BLACKMAN8[4], Apodization.Blackman.getIndex(4, 8, 2.0));
        assertEquals(BLACKMANHARRIS8[4], Apodization.BlackmanHarris.getIndex(4, 8, 2.0));
        assertEquals(BLACKMANNUTTAL8[4], Apodization.BlackmanNuttall.getIndex(4, 8, 2.0));
        assertEquals(EXPONENTIAL8[4], Apodization.Exponential.getIndex(4, 8, 1.0));
        assertEquals(1.1813604128656459, Apodization.Exponential.getIndex(4, 8, 2.0));
        assertEquals(FLATTOP8[4], Apodization.FlatTop.getIndex(4, 8, 2.0));
        assertEquals(HAMMING8[4], Apodization.Hamming.getIndex(4, 8, 2.0));
        assertEquals(HANN8[4], Apodization.Hann.getIndex(4, 8, 2.0));
        assertEquals(HANNEXP8[4], Apodization.HannExp.getIndex(4, 8, 2.0));
        assertEquals(NUTTAL8[4], Apodization.Nuttall.getIndex(4, 8, 2.0));

        // check applied apodization
        double[] testdata = new double[] { 1.4, 1.3, 1.1, 4.0, 2.1, 0.1, -0.2, -0.4 };
        Apodization.apodize(testdata, Apodization.FlatTop);
        assertArrayEquals(
                new double[] { 0.005600000000000121, -0.2205351270430622, 0.04977851282784238, 14.489556847751528,
                        7.607017345069555, 0.004525319347985736, 0.033928481083548026, -0.0016000000000000348 },
                testdata);
    }

    @Test
    public void testZeroLength() {
        assertArrayEquals(new double[0], Apodization.Rectangular.getWindow(0));
        assertArrayEquals(new double[0], Apodization.Blackman.getWindow(0));
        assertArrayEquals(new double[0], Apodization.BlackmanHarris.getWindow(0));
        assertArrayEquals(new double[0], Apodization.BlackmanNuttall.getWindow(0));
        assertArrayEquals(new double[0], Apodization.Exponential.getWindow(0));
        assertArrayEquals(new double[0], Apodization.FlatTop.getWindow(0));
        assertArrayEquals(new double[0], Apodization.Hamming.getWindow(0));
        assertArrayEquals(new double[0], Apodization.Hann.getWindow(0));
        assertArrayEquals(new double[0], Apodization.HannExp.getWindow(0));
        assertArrayEquals(new double[0], Apodization.Nuttall.getWindow(0));
    }

    @Test
    public void testCaching() {
        // intial equality test
        final double[] test = Apodization.Blackman.getWindow(12);
        assertEquals(test, Apodization.Blackman.getWindow(12));
        // check that different size returns different array
        assertNotEquals(test, Apodization.Blackman.getWindow(13));
        assertNotEquals(test, Apodization.Nuttall.getWindow(12));
    }

    @Test
    public void testHelperClass() {
        final ApodizationArrayDescription test = new Apodization.ApodizationArrayDescription(Apodization.Blackman, 8);
        final ApodizationArrayDescription test2 = new Apodization.ApodizationArrayDescription(Apodization.Rectangular,
                8);
        final ApodizationArrayDescription test3 = new Apodization.ApodizationArrayDescription(Apodization.Blackman, 12);
        final ApodizationArrayDescription test4 = new Apodization.ApodizationArrayDescription(Apodization.Blackman, 8);
        assertEquals(test, test);
        assertNotEquals(test, test2);
        assertNotEquals(test, test3);
        assertNotEquals(test, new double[3]);
        assertNotEquals(test, null);
        assertEquals(test, test4);
    }

    @Test
    public void testChangingCache() {
        Map<ApodizationArrayDescription, double[]> oldCache = Apodization.getWindowCache();
        WeakHashMap<Apodization.ApodizationArrayDescription, double[]> newCache = new WeakHashMap<>();
        Apodization.setWindowCache(newCache);
        assertEquals(newCache, Apodization.getWindowCache());
        assertNotEquals(oldCache, Apodization.getWindowCache());
    }
}
