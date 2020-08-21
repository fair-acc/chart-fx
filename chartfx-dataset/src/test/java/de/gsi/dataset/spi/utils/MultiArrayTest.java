package de.gsi.dataset.spi.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author akrimm
 */
public class MultiArrayTest {
    @Test
    public void testMultiArrayDoubleExceptions() {
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", new int[] { 1, 2, 3 }, 0)); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0)); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of(new double[] { 3, 2 }, new int[] { 1, 2, 3 }, 0)); // elements smaller than dimensions
    }

    @Test
    public void testMultiArrayDouble2D() {
        final MultiArray<double[]> array = MultiArray.of(new double[] { 0, 0, 0, 0, 9, 8, 7, 6, 5, 4 }, new int[] { 2, 3 }, 4);
        assertTrue(array instanceof MultiArrayDouble.MultiArrayDouble2D);
        final MultiArrayDouble.MultiArrayDouble2D array2d = (MultiArrayDouble.MultiArrayDouble2D) array;
        assertArrayEquals(new double[] { 7, 6 }, array2d.getRow(1));
        assertEquals(4, array.getOffset());
        assertEquals(8, ((MultiArrayDouble) array).get(new int[] { 1, 0 }));
        assertEquals(8, array2d.get(1, 0));
        assertEquals(4, ((MultiArrayDouble) array).get(new int[] { 1, 2 }));
        assertEquals(4, array2d.get(1, 2));
        array2d.set(1, 2, 1.337);
        assertEquals(1.337, array2d.get(1, 2));
    }

    @Test
    public void testMultiArrayDouble1D() {
        final MultiArray<double[]> array = MultiArray.of(new double[] { 100, 99, 9, 8, 7, 6, 5, 4 }, 2);
        assertTrue(array instanceof MultiArrayDouble.MultiArrayDouble1D);
        final MultiArrayDouble.MultiArrayDouble1D array1d = (MultiArrayDouble.MultiArrayDouble1D) array;
        assertEquals(2, array.getOffset());
        assertEquals(7, array1d.get(2));
        assertEquals(4, array1d.get(5));
        assertEquals(8, ((MultiArrayDouble) array).get(new int[] { 1 }));
        array1d.set(1.337, 5);
        assertEquals(1.337, array1d.get(5));
    }

    @Test
    public void testMultiArrayDouble() {
        final MultiArray<double[]> array = MultiArray.of(new double[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 11, 22, 33 }, new int[] { 2, 3, 2 }, 3);
        assertTrue(array instanceof MultiArrayDouble);
        assertEquals(3, array.getOffset());
        final MultiArrayDouble arrayDouble = (MultiArrayDouble) array;
        assertEquals(12, arrayDouble.getElementsCount());
        assertArrayEquals(new int[] { 2, 3, 2 }, arrayDouble.getDimensions());
        assertEquals(33, arrayDouble.get(new int[] { 1, 2, 1 }));
        assertEquals(7, arrayDouble.get(new int[] { 0, 1, 0 }));
        assertEquals(8, arrayDouble.get(new int[] { 1, 0, 0 }));
        arrayDouble.set(new int[] { 1, 1, 1 }, 1.337);
        assertEquals(1.337, arrayDouble.get(new int[] { 1, 1, 1 }));
        assertArrayEquals(new double[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 1.337, 22, 33 }, arrayDouble.getStridedArray());
        assertArrayEquals(new int[] { 1, 2, 0 }, array.getIndices(array.getIndex(new int[] { 1, 2, 0 })));
        assertArrayEquals(new int[] { 0, 0, 0 }, array.getIndices(3));
        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, 2, 2 }));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, -1, 0 }));
    }
}
