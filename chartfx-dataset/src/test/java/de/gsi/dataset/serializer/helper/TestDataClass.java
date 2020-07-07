package de.gsi.dataset.serializer.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD") // complexity is part of the very large use-case surface that is being tested
public class TestDataClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataClass.class);

    public boolean bool1;
    public boolean bool2;
    public byte byte1;
    public byte byte2;
    public char char1;
    public char char2;
    public short short1;
    public short short2;
    public int int1;
    public int int2;
    public long long1;
    public long long2;
    public float float1;
    public float float2;
    public double double1;
    public double double2;
    public String string1;
    public String string2;

    // 1-dim arrays
    public boolean[] boolArray;
    public byte[] byteArray;
    //    public char[] charArray;
    public short[] shortArray;
    public int[] intArray;
    public long[] longArray;
    public float[] floatArray;
    public double[] doubleArray;
    public String[] stringArray;

    // generic n-dim arrays - N.B. striding-arrays: low-level format is the same except of 'nDimension' descriptor
    public int[] nDimensions;
    public boolean[] boolNdimArray;
    public byte[] byteNdimArray;
    // public char[] charNdimArray;
    public short[] shortNdimArray;
    public int[] intNdimArray;
    public long[] longNdimArray;
    public float[] floatNdimArray;
    public double[] doubleNdimArray;

    public TestDataClass nestedData;

    public TestDataClass() {
        this(-1, -1, -1);
    }

    /**
     * @param nSizePrimitives size of primitive arrays (smaller 0: do not initialise fields/allocate arrays)
     * @param nSizeString size of String[] array (smaller 0: do not initialise fields/allocate arrays)
     * @param nestedClassRecursion how many nested sub-classes should be allocated
     */
    public TestDataClass(final int nSizePrimitives, final int nSizeString, final int nestedClassRecursion) {
        if (nestedClassRecursion > 0) {
            nestedData = new TestDataClass(nSizePrimitives, nSizeString, nestedClassRecursion - 1);
            nestedData.init(nSizePrimitives + 1, nSizeString + 1); //N.B. '+1' to have different sizes for nested classes
        }

        init(nSizePrimitives, nSizeString);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TestDataClass)) {
            LOGGER.atError().addArgument(obj).log("incompatible object type of obj = '{}'");
            return false;
        }
        final TestDataClass other = (TestDataClass) obj;
        boolean returnState = true;
        if (this.bool1 != other.bool1) {
            LOGGER.atError().addArgument("bool1").addArgument(this.bool1).addArgument(other.bool1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.bool2 != other.bool2) {
            LOGGER.atError().addArgument("bool2").addArgument(this.bool2).addArgument(other.bool2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.byte1 != other.byte1) {
            LOGGER.atError().addArgument("byte1").addArgument(this.byte1).addArgument(other.byte1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.byte2 != other.byte2) {
            LOGGER.atError().addArgument("byte2").addArgument(this.byte2).addArgument(other.byte2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.char1 != other.char1) {
            LOGGER.atError().addArgument("char1").addArgument(this.char1).addArgument(other.char1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.char2 != other.char2) {
            LOGGER.atError().addArgument("char2").addArgument(this.char2).addArgument(other.char2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.short1 != other.short1) {
            LOGGER.atError().addArgument("short1").addArgument(this.short1).addArgument(other.short1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.short2 != other.short2) {
            LOGGER.atError().addArgument("short2").addArgument(this.short2).addArgument(other.short2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.int1 != other.int1) {
            LOGGER.atError().addArgument("int1").addArgument(this.int1).addArgument(other.int1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.int2 != other.int2) {
            LOGGER.atError().addArgument("int2").addArgument(this.int2).addArgument(other.int2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.float1 != other.float1) {
            LOGGER.atError().addArgument("float1").addArgument(this.float1).addArgument(other.float1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.float2 != other.float2) {
            LOGGER.atError().addArgument("float2").addArgument(this.float2).addArgument(other.float2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.double1 != other.double1) {
            LOGGER.atError().addArgument("double1").addArgument(this.double1).addArgument(other.double1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (this.double2 != other.double2) {
            LOGGER.atError().addArgument("double2").addArgument(this.double2).addArgument(other.double2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (string1 != other.string1 && (string1 == null || !string1.equals(other.string1))) { //NOSONAR //NOPMD null-compatible String check
            LOGGER.atError().addArgument("string1").addArgument(this.string1).addArgument(other.string1) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }
        if (string2 != other.string2 && (string2 == null || !string2.equals(other.string2))) { //NOSONAR //NOPMD null-compatible String check
            LOGGER.atError().addArgument("string2").addArgument(this.string2).addArgument(other.string2) //
                    .log("field '{}' does not match '{}' vs '{}'");
            returnState = false;
        }

        // test 1D-arrays
        try {
            assertArrayEquals(this.boolArray, other.boolArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("boolArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.byteArray, other.byteArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("byteArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        //try {
        //    assertArrayEquals(this.charArray, other.charArray);
        //} catch(AssertionFailedError e) {
        //    LOGGER.atError().addArgument("charArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
        //    returnState = false;
        //}
        try {
            assertArrayEquals(this.shortArray, other.shortArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("shortArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.intArray, other.intArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("intArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.longArray, other.longArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("longArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.floatArray, other.floatArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("floatArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.doubleArray, other.doubleArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("doubleArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.stringArray, other.stringArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("doubleArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }

        // test n-dimensional -arrays
        try {
            assertArrayEquals(this.nDimensions, other.nDimensions);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("nDimensions").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.boolNdimArray, other.boolNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("boolNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.byteNdimArray, other.byteNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("byteNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        //try {
        //    assertArrayEquals(this.charNdimArray, other.charNdimArray);
        //} catch(AssertionFailedError e) {
        //    LOGGER.atError().addArgument("charNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
        //    returnState = false;
        //}
        try {
            assertArrayEquals(this.shortNdimArray, other.shortNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("shortNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.intNdimArray, other.intNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("intNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.longNdimArray, other.longNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("longNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.floatNdimArray, other.floatNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("floatNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }
        try {
            assertArrayEquals(this.doubleNdimArray, other.doubleNdimArray);
        } catch (AssertionFailedError e) {
            LOGGER.atError().addArgument("doubleNdimArray").addArgument(e.getMessage()).log("field '{}' does not match '{}'");
            returnState = false;
        }

        // check for nested data content
        if (this.nestedData != null) {
            returnState = returnState & this.nestedData.equals(other.nestedData);
        } else if (this.nestedData == null && other.nestedData != null) {
            LOGGER.atError().addArgument("nestedData").addArgument(this.nestedData).addArgument(other.nestedData).log("field '{}' error:  this.nestedData == null ({}) && other.nestedData != null ({})");
            returnState = false;
        }

        return returnState;
    }

    public final void clear() {
        bool1 = false;
        bool2 = false;
        byte1 = 0;
        byte2 = 0;
        char1 = 0;
        char2 = 0;
        short1 = 0;
        short2 = 0;
        int1 = 0;
        int2 = 0;
        long1 = 0;
        long2 = 0;
        float1 = 0;
        float2 = 0;
        double1 = 0;
        double2 = 0;

        string1 = null;
        string2 = null;

        // reset 1-dim arrays
        boolArray = null;
        byteArray = null;
        // charArray = null;
        shortArray = null;
        intArray = null;
        longArray = null;
        floatArray = null;
        doubleArray = null;
        stringArray = null;

        // reset n-dim arrays
        nDimensions = null;
        boolNdimArray = null;
        byteNdimArray = null;
        //            charNdimArray = null;
        shortNdimArray = null;
        intNdimArray = null;
        longNdimArray = null;
        floatNdimArray = null;
        doubleNdimArray = null;
    }

    public final void init(final int nSizePrimitives, final int nSizeString) {
        if (nSizePrimitives >= 0) {
            bool1 = true;
            bool2 = false;
            byte1 = 10;
            byte2 = -100;
            char1 = 'a';
            char2 = 'Z';
            short1 = 20;
            short2 = -200;
            int1 = 30;
            int2 = -300;
            long1 = 40;
            long2 = -400;
            float1 = 50.5f;
            float2 = -500.5f;
            double1 = 60.6;
            double2 = -600.6;

            string1 = "Hello World!";
            string2 = "Γειά σου Κόσμε!";

            // allocate 1-dim arrays
            boolArray = getBooleanEnumeration(0, nSizePrimitives);
            byteArray = getByteEnumeration(1, nSizePrimitives + 1);
            //            charArray = getCharEnumeration(2, nSizePrimitives + 2);
            shortArray = getShortEnumeration(3, nSizePrimitives + 3);
            intArray = getIntEnumeration(4, nSizePrimitives + 4);
            longArray = getLongEnumeration(5, nSizePrimitives + 5);
            floatArray = getFloatEnumeration(6, nSizePrimitives + 6);
            doubleArray = getDoubleEnumeration(7, nSizePrimitives + 7);

            // allocate n-dim arrays -- N.B. for simplicity the dimension/low-level backing size is const

            nDimensions = new int[] { 2, 3, 2 };
            final int nMultiDim = nDimensions[0] * nDimensions[1] * nDimensions[2];
            boolNdimArray = getBooleanEnumeration(0, nMultiDim);
            byteNdimArray = getByteEnumeration(1, nMultiDim + 1);
            //            charNdimArray = getCharEnumeration(2, nMultiDim + 2);
            shortNdimArray = getShortEnumeration(3, nMultiDim + 3);
            intNdimArray = getIntEnumeration(4, nMultiDim + 4);
            longNdimArray = getLongEnumeration(5, nMultiDim + 5);
            floatNdimArray = getFloatEnumeration(6, nMultiDim + 6);
            doubleNdimArray = getDoubleEnumeration(7, nMultiDim + 7);
        }

        if (nSizeString >= 0) {
            stringArray = new String[nSizeString];
            for (int i = 0; i < nSizeString; ++i) {
                stringArray[i] = string1;
            }
        }
    }

    private static boolean[] getBooleanEnumeration(final int from, final int to) {
        final boolean[] ret = new boolean[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = i % 2 == 0;
        }
        return ret;
    }

    private static byte[] getByteEnumeration(final int from, final int to) {
        final byte[] ret = new byte[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = (byte) i;
        }
        return ret;
    }

    private static char[] getCharEnumeration(final int from, final int to) {
        final char[] ret = new char[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = (char) i;
        }
        return ret;
    }

    private static double[] getDoubleEnumeration(final int from, final int to) {
        final double[] ret = new double[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = i / 10f;
        }
        return ret;
    }

    private static float[] getFloatEnumeration(final int from, final int to) {
        final float[] ret = new float[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = i / 10f;
        }
        return ret;
    }

    private static int[] getIntEnumeration(final int from, final int to) {
        final int[] ret = new int[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = i;
        }
        return ret;
    }

    private static long[] getLongEnumeration(final int from, final int to) {
        final long[] ret = new long[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = i;
        }
        return ret;
    }

    private static short[] getShortEnumeration(final int from, final int to) {
        final short[] ret = new short[to - from];
        for (int i = from; i < to; i++) {
            ret[i - from] = (short) i;
        }
        return ret;
    }
}
