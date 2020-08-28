package de.gsi.dataset.spi.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author akrimm
 */
class MultiArrayTest {
    //// Double
    @Test
    void testMultiArrayDoubleExceptions() {
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0, new int[] { 1, 2, 3 })); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0)); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of(new double[] { 3, 2 }, 0, new int[] { 1, 2, 3 })); // elements smaller than dimensions
    }

    @Test
    void testMultiArrayDouble() {
        final MultiArray<double[]> array = MultiArray.of(new double[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 11, 22, 33 }, 3, new int[] { 2, 3, 2 });
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
        assertArrayEquals(new double[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 1.337, 22, 33 }, arrayDouble.elements());
        assertArrayEquals(new int[] { 1, 2, 0 }, array.getIndices(array.getIndex(new int[] { 1, 2, 0 })));
        assertArrayEquals(new int[] { 0, 0, 0 }, array.getIndices(3));
        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, 2, 2 }));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, -1, 0 }));
    }

    @Test
    void testMultiArrayDouble2D() {
        final MultiArray<double[]> array = MultiArray.of(new double[] { 0, 0, 0, 0, 9, 8, 7, 6, 5, 4 }, 4, new int[] { 2, 3 });
        assertTrue(array instanceof MultiArrayDouble.MultiArray2DDouble);
        final MultiArrayDouble.MultiArray2DDouble array2d = (MultiArrayDouble.MultiArray2DDouble) array;
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
    void testMultiArrayDouble1D() {
        final MultiArray<double[]> array = MultiArray.of(new double[] { 100, 99, 9, 8, 7, 6, 5, 4 }, 2);
        assertTrue(array instanceof MultiArrayDouble.MultiArray1DDouble);
        final MultiArrayDouble.MultiArray1DDouble array1d = (MultiArrayDouble.MultiArray1DDouble) array;
        assertEquals(2, array.getOffset());
        assertEquals(7, array1d.get(2));
        assertEquals(4, array1d.get(5));
        assertEquals(8, ((MultiArrayDouble) array).get(new int[] { 1 }));
        array1d.set(5, 1.337);
        assertEquals(1.337, array1d.get(5));
    }

    //// Int

    @Test
    void testMultiArrayInt() {
        final MultiArray<int[]> array = MultiArray.of(new int[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 11, 22, 33 }, 3, new int[] { 2, 3, 2 });
        assertTrue(array instanceof MultiArrayInt);
        assertEquals(3, array.getOffset());
        final MultiArrayInt arrayInt = (MultiArrayInt) array;
        assertEquals(12, arrayInt.getElementsCount());
        assertArrayEquals(new int[] { 2, 3, 2 }, arrayInt.getDimensions());
        assertEquals(33, arrayInt.get(new int[] { 1, 2, 1 }));
        assertEquals(7, arrayInt.get(new int[] { 0, 1, 0 }));
        assertEquals(8, arrayInt.get(new int[] { 1, 0, 0 }));
        arrayInt.set(new int[] { 1, 1, 1 }, 1337);
        assertEquals(1337, arrayInt.get(new int[] { 1, 1, 1 }));
        assertArrayEquals(new int[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 1337, 22, 33 }, arrayInt.elements());
        assertArrayEquals(new int[] { 1, 2, 0 }, array.getIndices(array.getIndex(new int[] { 1, 2, 0 })));
        assertArrayEquals(new int[] { 0, 0, 0 }, array.getIndices(3));
        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, 2, 2 }));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, -1, 0 }));
    }

    @Test
    void testMultiArrayInt2D() {
        final MultiArray<int[]> array = MultiArray.of(new int[] { 0, 0, 0, 0, 9, 8, 7, 6, 5, 4 }, 4, new int[] { 2, 3 });
        assertTrue(array instanceof MultiArrayInt.MultiArray2DInt);
        final MultiArrayInt.MultiArray2DInt array2d = (MultiArrayInt.MultiArray2DInt) array;
        assertArrayEquals(new int[] { 7, 6 }, array2d.getRow(1));
        assertEquals(4, array.getOffset());
        assertEquals(8, ((MultiArrayInt) array).get(new int[] { 1, 0 }));
        assertEquals(8, array2d.get(1, 0));
        assertEquals(4, ((MultiArrayInt) array).get(new int[] { 1, 2 }));
        assertEquals(4, array2d.get(1, 2));
        array2d.set(1, 2, 1337);
        assertEquals(1337, array2d.get(1, 2));
    }

    @Test
    void testMultiArrayInt1D() {
        final MultiArray<int[]> array = MultiArray.of(new int[] { 100, 99, 9, 8, 7, 6, 5, 4 }, 2);
        assertTrue(array instanceof MultiArrayInt.MultiArray1DInt);
        final MultiArrayInt.MultiArray1DInt array1d = (MultiArrayInt.MultiArray1DInt) array;
        assertEquals(2, array.getOffset());
        assertEquals(7, array1d.get(2));
        assertEquals(4, array1d.get(5));
        assertEquals(8, ((MultiArrayInt) array).get(new int[] { 1 }));
        array1d.set(5, 1337);
        assertEquals(1337, array1d.get(5));
    }

    //// Float
    @Test
    void testMultiArrayFloatExceptions() {
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0, new int[] { 1, 2, 3 })); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0)); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of(new float[] { 3, 2 }, 0, new int[] { 1, 2, 3 })); // elements smaller than dimensions
    }

    @Test
    void testMultiArrayFloat() {
        final MultiArray<float[]> array = MultiArray.of(new float[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 11, 22, 33 }, 3, new int[] { 2, 3, 2 });
        assertTrue(array instanceof MultiArrayFloat);
        assertEquals(3, array.getOffset());
        final MultiArrayFloat arrayFloat = (MultiArrayFloat) array;
        assertEquals(12, arrayFloat.getElementsCount());
        assertArrayEquals(new int[] { 2, 3, 2 }, arrayFloat.getDimensions());
        assertEquals(33, arrayFloat.get(new int[] { 1, 2, 1 }));
        assertEquals(7, arrayFloat.get(new int[] { 0, 1, 0 }));
        assertEquals(8, arrayFloat.get(new int[] { 1, 0, 0 }));
        arrayFloat.set(new int[] { 1, 1, 1 }, 1.337f);
        assertEquals(1.337f, arrayFloat.get(new int[] { 1, 1, 1 }));
        assertArrayEquals(new float[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 1.337f, 22, 33 }, arrayFloat.elements());
        assertArrayEquals(new int[] { 1, 2, 0 }, array.getIndices(array.getIndex(new int[] { 1, 2, 0 })));
        assertArrayEquals(new int[] { 0, 0, 0 }, array.getIndices(3));
        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, 2, 2 }));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, -1, 0 }));
    }

    @Test
    void testMultiArrayFloat2D() {
        final MultiArray<float[]> array = MultiArray.of(new float[] { 0, 0, 0, 0, 9, 8, 7, 6, 5, 4 }, 4, new int[] { 2, 3 });
        assertTrue(array instanceof MultiArrayFloat.MultiArray2DFloat);
        final MultiArrayFloat.MultiArray2DFloat array2d = (MultiArrayFloat.MultiArray2DFloat) array;
        assertArrayEquals(new float[] { 7, 6 }, array2d.getRow(1));
        assertEquals(4, array.getOffset());
        assertEquals(8, ((MultiArrayFloat) array).get(new int[] { 1, 0 }));
        assertEquals(8, array2d.get(1, 0));
        assertEquals(4, ((MultiArrayFloat) array).get(new int[] { 1, 2 }));
        assertEquals(4, array2d.get(1, 2));
        array2d.set(1, 2, 1.337f);
        assertEquals(1.337f, array2d.get(1, 2));
    }

    @Test
    void testMultiArrayFloat1D() {
        final MultiArray<float[]> array = MultiArray.of(new float[] { 100, 99, 9, 8, 7, 6, 5, 4 }, 2);
        assertTrue(array instanceof MultiArrayFloat.MultiArray1DFloat);
        final MultiArrayFloat.MultiArray1DFloat array1d = (MultiArrayFloat.MultiArray1DFloat) array;
        assertEquals(2, array.getOffset());
        assertEquals(7, array1d.get(2));
        assertEquals(4, array1d.get(5));
        assertEquals(8, ((MultiArrayFloat) array).get(new int[] { 1 }));
        array1d.set(5, 1.337f);
        assertEquals(1.337f, array1d.get(5));
    }

    //// Long
    @Test
    void testMultiArrayLongExceptions() {
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0, new int[] { 1, 2, 3 })); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0)); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of(new long[] { 3, 2 }, 0, new int[] { 1, 2, 3 })); // elements smaller than dimensions
    }

    @Test
    void testMultiArrayLong() {
        final MultiArray<long[]> array = MultiArray.of(new long[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 11, 22, 33 }, 3, new int[] { 2, 3, 2 });
        assertTrue(array instanceof MultiArrayLong);
        assertEquals(3, array.getOffset());
        final MultiArrayLong arrayLong = (MultiArrayLong) array;
        assertEquals(12, arrayLong.getElementsCount());
        assertArrayEquals(new int[] { 2, 3, 2 }, arrayLong.getDimensions());
        assertEquals(33, arrayLong.get(new int[] { 1, 2, 1 }));
        assertEquals(7, arrayLong.get(new int[] { 0, 1, 0 }));
        assertEquals(8, arrayLong.get(new int[] { 1, 0, 0 }));
        arrayLong.set(new int[] { 1, 1, 1 }, 1337);
        assertEquals(1337, arrayLong.get(new int[] { 1, 1, 1 }));
        assertArrayEquals(new long[] { 123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 1337, 22, 33 }, arrayLong.elements());
        assertArrayEquals(new int[] { 1, 2, 0 }, array.getIndices(array.getIndex(new int[] { 1, 2, 0 })));
        assertArrayEquals(new int[] { 0, 0, 0 }, array.getIndices(3));
        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, 2, 2 }));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, -1, 0 }));
    }

    @Test
    void testMultiArrayLong2D() {
        final MultiArray<long[]> array = MultiArray.of(new long[] { 0, 0, 0, 0, 9, 8, 7, 6, 5, 4 }, 4, new int[] { 2, 3 });
        assertTrue(array instanceof MultiArrayLong.MultiArray2DLong);
        final MultiArrayLong.MultiArray2DLong array2d = (MultiArrayLong.MultiArray2DLong) array;
        assertArrayEquals(new long[] { 7, 6 }, array2d.getRow(1));
        assertEquals(4, array.getOffset());
        assertEquals(8, ((MultiArrayLong) array).get(new int[] { 1, 0 }));
        assertEquals(8, array2d.get(1, 0));
        assertEquals(4, ((MultiArrayLong) array).get(new int[] { 1, 2 }));
        assertEquals(4, array2d.get(1, 2));
        array2d.set(1, 2, 1337);
        assertEquals(1337, array2d.get(1, 2));
    }

    @Test
    void testMultiArrayLong1D() {
        final MultiArray<long[]> array = MultiArray.of(new long[] { 100, 99, 9, 8, 7, 6, 5, 4 }, 2);
        assertTrue(array instanceof MultiArrayLong.MultiArray1DLong);
        final MultiArrayLong.MultiArray1DLong array1d = (MultiArrayLong.MultiArray1DLong) array;
        assertEquals(2, array.getOffset());
        assertEquals(7, array1d.get(2));
        assertEquals(4, array1d.get(5));
        assertEquals(8, ((MultiArrayLong) array).get(new int[] { 1 }));
        array1d.set(5, 1337);
        assertEquals(1337, array1d.get(5));
    }

    //// Object
    @Test
    void testMultiArrayObjectExceptions() {
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0, new int[] { 1, 2, 3 })); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0)); // elements no array
        assertThrows(IllegalArgumentException.class, () -> MultiArray.of(new String[] { "a", "b" }, 0, new int[] { 1, 2, 3 })); // elements smaller than dimensions
    }

    @Test
    void testMultiArrayObject() {
        final MultiArray<String[]> array = MultiArray.of(new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o" }, 3, new int[] { 2, 3, 2 });
        assertTrue(array instanceof MultiArrayObject);
        assertEquals(3, array.getOffset());
        final MultiArrayObject<String> arrayObject = (MultiArrayObject<String>) array;
        assertEquals(12, arrayObject.getElementsCount());
        assertArrayEquals(new int[] { 2, 3, 2 }, arrayObject.getDimensions());
        assertEquals("o", arrayObject.get(new int[] { 1, 2, 1 }));
        assertEquals("f", arrayObject.get(new int[] { 0, 1, 0 }));
        assertEquals("e", arrayObject.get(new int[] { 1, 0, 0 }));
        arrayObject.set(new int[] { 1, 1, 1 }, "foobar");
        assertEquals("foobar", arrayObject.get(new int[] { 1, 1, 1 }));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "foobar", "n", "o" }, arrayObject.elements());
        assertArrayEquals(new int[] { 1, 2, 0 }, array.getIndices(array.getIndex(new int[] { 1, 2, 0 })));
        assertArrayEquals(new int[] { 0, 0, 0 }, array.getIndices(3));
        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, 2, 2 }));
        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[] { 0, -1, 0 }));
    }

    @Test
    void testMultiArrayObject2D() {
        final MultiArray<String[]> array = MultiArray.of(new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }, 4, new int[] { 2, 3 });
        assertTrue(array instanceof MultiArrayObject.MultiArray2DObject);
        final MultiArrayObject.MultiArray2DObject array2d = (MultiArrayObject.MultiArray2DObject) array;
        assertArrayEquals(new String[] { "g", "h" }, array2d.getRow(1));
        assertEquals(4, array.getOffset());
        assertEquals("f", ((MultiArrayObject) array).get(new int[] { 1, 0 }));
        assertEquals("f", array2d.get(1, 0));
        assertEquals("j", ((MultiArrayObject) array).get(new int[] { 1, 2 }));
        assertEquals("j", array2d.get(1, 2));
        array2d.set(1, 2, "foobar");
        assertEquals("foobar", array2d.get(1, 2));
    }

    @Test
    void testMultiArrayObject1D() {
        final MultiArray<String[]> array = MultiArray.of(new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" }, 2);
        assertTrue(array instanceof MultiArrayObject.MultiArray1DObject);
        final MultiArrayObject.MultiArray1DObject array1d = (MultiArrayObject.MultiArray1DObject) array;
        assertEquals(2, array.getOffset());
        assertEquals("e", array1d.get(2));
        assertEquals("h", array1d.get(5));
        assertEquals("d", ((MultiArrayObject) array).get(new int[] { 1 }));
        array1d.set(5, "foobar");
        assertEquals("foobar", array1d.get(5));
    }
    //// Boolean
    //    @Test
    //    void testMultiArrayBooleanExceptions() {
    //        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", new int[]{1, 2, 3}, 0)); // elements no array
    //        assertThrows(IllegalArgumentException.class, () -> MultiArray.of("Foo", 0)); // elements no array
    //        assertThrows(IllegalArgumentException.class, () -> MultiArray.of(new boolean[]{true, false}, new int[]{1, 2, 3}, 0)); // elements smaller than dimensions
    //    }
    //
    //    @Test
    //    void testMultiArrayBoolean() {
    //        final MultiArray<boolean[]> array = MultiArray.of(new boolean[]{true, false, false, false, true, true, false, false, true, false, false, false, false, false, true}, new int[]{2, 3, 2}, 3);
    //        assertTrue(array instanceof MultiArrayBoolean);
    //        assertEquals(3, array.getOffset());
    //        final MultiArrayBoolean arrayBoolean = (MultiArrayBoolean) array;
    //        assertEquals(12, arrayBoolean.getElementsCount());
    //        assertArrayEquals(new int[]{2, 3, 2}, arrayBoolean.getDimensions());
    //        assertEquals(true, arrayBoolean.get(new int[]{1, 2, 1}));
    //        assertEquals(true, arrayBoolean.get(new int[]{0, 1, 0}));
    //        assertEquals(true, arrayBoolean.get(new int[]{1, 0, 0}));
    //        arrayBoolean.set(new int[]{1, 1, 1}, 1.337);
    //        assertEquals(1.337, arrayBoolean.get(new int[]{1, 1, 1}));
    //        assertArrayEquals(new boolean[]{123, 321, 213, 9, 8, 7, 6, 5, 4, 1, 2, 3, 1.337, 22, 33}, arrayBoolean.getStridedArray());
    //        assertArrayEquals(new int[]{1, 2, 0}, array.getIndices(array.getIndex(new int[]{1, 2, 0})));
    //        assertArrayEquals(new int[]{0, 0, 0}, array.getIndices(3));
    //        assertEquals(8 + 3, array.getIndex(array.getIndices(8 + 3)));
    //        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(15));
    //        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndices(2));
    //        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[]{0, 2, 2}));
    //        assertThrows(IndexOutOfBoundsException.class, () -> array.getIndex(new int[]{0, -1, 0}));
    //    }
    //
    //    @Test
    //    void testMultiArrayBoolean2D() {
    //        final MultiArray<boolean[]> array = MultiArray.of(new boolean[]{0, 0, 0, 0, 9, 8, 7, 6, 5, 4}, new int[]{2, 3}, 4);
    //        assertTrue(array instanceof MultiArrayBoolean.MultiArray2DBoolean);
    //        final MultiArrayBoolean.MultiArray2DBoolean array2d = (MultiArrayBoolean.MultiArray2DBoolean) array;
    //        assertArrayEquals(new boolean[]{7, 6}, array2d.getRow(1));
    //        assertEquals(4, array.getOffset());
    //        assertEquals(8, ((MultiArrayBoolean) array).get(new int[]{1, 0}));
    //        assertEquals(8, array2d.get(1, 0));
    //        assertEquals(4, ((MultiArrayBoolean) array).get(new int[]{1, 2}));
    //        assertEquals(4, array2d.get(1, 2));
    //        array2d.set(1, 2, 1.337);
    //        assertEquals(1.337, array2d.get(1, 2));
    //    }
    //
    //    @Test
    //    void testMultiArrayBoolean1D() {
    //        final MultiArray<boolean[]> array = MultiArray.of(new boolean[]{100, 99, 9, 8, 7, 6, 5, 4}, 2);
    //        assertTrue(array instanceof MultiArrayBoolean.MultiArray1DBoolean);
    //        final MultiArrayBoolean.MultiArray1DBoolean array1d = (MultiArrayBoolean.MultiArray1DBoolean) array;
    //        assertEquals(2, array.getOffset());
    //        assertEquals(7, array1d.get(2));
    //        assertEquals(4, array1d.get(5));
    //        assertEquals(8, ((MultiArrayBoolean) array).get(new int[]{1}));
    //        array1d.set(1.337, 5);
    //        assertEquals(1.337, array1d.get(5));
    //    }
}
