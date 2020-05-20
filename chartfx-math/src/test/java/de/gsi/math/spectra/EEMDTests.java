package de.gsi.math.spectra;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EEMDTests {
    @Test
    public void extremeTest() {
        double[][] spmax = new double[2][10];
        double[][] spmin = new double[2][10];

        assertEquals(-1, EEMD.extrema(new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, spmax, spmin));
        assertArrayEquals(new double[] { 9, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, spmax[0]);
        assertArrayEquals(new double[] { 10, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, spmax[1]);
        assertArrayEquals(new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, spmin[0]);
        assertArrayEquals(new double[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, spmin[1]);

        assertEquals(1, EEMD.extrema(new double[] { 1, 2, 3, 2, 1, 5 }, spmax, spmin));
        assertArrayEquals(new double[] { 2, 5, 0, 0, 0, 0, 0, 0, 0, 0 }, spmax[0]);
        assertArrayEquals(new double[] { 3, 5, 0, 0, 0, 0, 0, 0, 0, 0 }, spmax[1]);
        assertArrayEquals(new double[] { 0, 4, 0, 0, 0, 0, 0, 0, 0, 0 }, spmin[0]);
        assertArrayEquals(new double[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, spmin[1]);
    }
}
