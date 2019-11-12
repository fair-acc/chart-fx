package de.gsi.dataset.utils.serializer.helper;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * @author rstein
 */
public class MyGenericClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyGenericClass.class);
    private static boolean verboseLogging = false;
    private static boolean extendedTestCase = true;
    private static final String MODIFIED = "Modified";
    // supported data types
    protected boolean dummyBoolean;
    protected byte dummyByte;
    protected short dummyShort;
    protected int dummyInt;
    protected long dummyLong;
    protected float dummyFloat;
    protected double dummyDouble;
    protected String dummyString = "Test";

    protected TestEnum enumState = TestEnum.TEST_CASE_1;

    protected ArrayList<Integer> arrayListInteger = new ArrayList<>();
    protected ArrayList<String> arrayListString = new ArrayList<>();

    protected BoxedPrimitivesSubClass boxedPrimitivesNull;
    public BoxedPrimitivesSubClass boxedPrimitives = new BoxedPrimitivesSubClass();

    public ArraySubClass arrays = new ArraySubClass();
    public BoxedObjectArraySubClass objArrays = new BoxedObjectArraySubClass();

    public MyGenericClass() {
        arrayListInteger.add(1);
        arrayListInteger.add(2);
        arrayListInteger.add(3);
        arrayListString.add("String#1");
        arrayListString.add("String#2");
        arrayListString.add("String#3");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MyGenericClass)) {
            return false;
        }
        // normally this value is immediately returned for 'false',
        // here: state is latched to detect potential other violations
        boolean state = true;
        MyGenericClass other = (MyGenericClass) obj;
        if (this.hashCode() != other.hashCode()) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(this.hashCode())
                    .addArgument(other.hashCode()).log("{} - hashCode is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyBoolean != other.dummyBoolean) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoolean)
                    .addArgument(other.dummyBoolean).log("{} - dummyBoolean is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyByte != other.dummyByte) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyByte)
                    .addArgument(other.dummyByte).log("{} - dummyByte is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyShort != other.dummyShort) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyShort)
                    .addArgument(other.dummyShort).log("{} - dummyShort is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyInt != other.dummyInt) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyInt).addArgument(other.dummyInt)
                    .log("{} - dummyInt is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyLong != other.dummyLong) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyLong)
                    .addArgument(other.dummyLong).log("{} - dummyLong is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyFloat != other.dummyFloat) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyFloat)
                    .addArgument(other.dummyFloat).log("{} - dummyFloat is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyDouble != other.dummyDouble) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyDouble)
                    .addArgument(other.dummyDouble).log("{} - dummyDouble is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (dummyString == null || !dummyString.contentEquals(other.dummyString)) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyString)
                    .addArgument(other.dummyString).addArgument(dummyString).addArgument(other.dummyString)
                    .log("{} - dummyString is not equal {} vs {}'");
            state = false;
        }

        if (!boxedPrimitives.equals(other.boxedPrimitives)) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(boxedPrimitives)
                    .addArgument(other.boxedPrimitives)
                    .log("{} - boxedPrimitives are not equal: this '{}' vs. other '{}'");
            state = false;
        }

        if (!extendedTestCase) {
            // abort equals for more complex/extended data structures
            return state;
        }

        if (!enumState.equals(other.enumState)) {
            state = false;
        }

        if (!arrayListInteger.equals(other.arrayListInteger)) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(arrayListInteger)
                    .addArgument(other.arrayListInteger)
                    .log("{} - arrayListInteger is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (!arrayListString.equals(other.arrayListString)) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(arrayListString)
                    .addArgument(other.arrayListString)
                    .log("{} - arrayListString is not equal: this '{}' vs. other '{}'");
            state = false;
        }
        if (!arrays.equals(other.arrays)) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).log("{} - arrays are not equal");
            state = false;
        }
        if (!objArrays.equals(other.objArrays)) {
            logBackEnd().addArgument(this.getClass().getSimpleName()).log("{} - objArrays is not equal");
            state = false;
        }

        return state;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (extendedTestCase) {
            result = prime * result + ((enumState == null) ? 0 : enumState.hashCode());
            result = prime * result + ((arrayListInteger == null) ? 0 : arrayListInteger.hashCode());
            result = prime * result + ((arrayListString == null) ? 0 : arrayListString.hashCode());
            result = prime * result + ((arrays == null) ? 0 : arrays.hashCode());
            result = prime * result + ((boxedPrimitives == null) ? 0 : boxedPrimitives.hashCode());
            result = prime * result + ((boxedPrimitivesNull == null) ? 0 : boxedPrimitivesNull.hashCode());
        }
        result = prime * result + (dummyBoolean ? 1231 : 1237);
        result = prime * result + dummyByte;
        long temp;
        temp = Double.doubleToLongBits(dummyDouble);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + Float.floatToIntBits(dummyFloat);
        result = prime * result + dummyInt;
        result = prime * result + (int) (dummyLong ^ (dummyLong >>> 32));
        result = prime * result + dummyShort;
        result = prime * result + ((dummyString == null) ? 0 : dummyString.hashCode());
        // result = prime * result + ((objArrays == null) ? 0 : objArrays.hashCode());
        return result;
    }

    public void modifyValues() {
        dummyBoolean = !dummyBoolean;
        dummyByte = (byte) (dummyByte + 1);
        dummyShort = (short) (dummyShort + 2);
        dummyInt = dummyInt + 1;
        dummyLong = dummyLong + 1;
        dummyFloat = dummyFloat + 0.5f;
        dummyDouble = dummyDouble + 1.5;
        dummyString = MODIFIED + dummyString;
        arrayListInteger.set(0, arrayListInteger.get(0) + 1);
        arrayListString.set(0, MODIFIED + arrayListString.get(0));
        enumState = TestEnum.values()[(enumState.ordinal() + 1) % TestEnum.values().length];
    }

    @Override
    public String toString() {
        return new StringBuilder()//
                .append("[dummyBoolean=").append(dummyBoolean)//
                .append(", dummyByte=").append(dummyByte)//
                .append(", dummyShort=").append(dummyShort)//
                .append(", dummyInt=").append(dummyInt)//
                .append(", dummyFloat=").append(dummyFloat)//
                .append(", dummyDouble=").append(dummyDouble)//
                .append(", dummyString=").append(dummyString)//
                .append(", arrayListInteger=").append(arrayListInteger)//
                .append(", arrayListString=").append(arrayListString)//
                .append(", hash(boxedPrimitives)=").append(boxedPrimitives.hashCode())//
                .append(", hash(arrays)=").append(arrays.hashCode())//
                .append(", hash(objArrays)=").append(objArrays.hashCode())//
                .append(']').toString();
    }

    public static boolean isExtendedTestCase() {
        return extendedTestCase;
    }

    public static boolean isVerboseChecks() {
        return verboseLogging;
    }

    private static LoggingEventBuilder logBackEnd() {
        return verboseLogging ? LOGGER.atError() : LOGGER.atDebug();
    }

    public static void setEnableExtendedTestCase(final boolean state) {
        extendedTestCase = state;
    }

    public static void setVerboseChecks(final boolean state) {
        verboseLogging = state;
    }

    public class ArraySubClass {
        protected boolean[] dummyBooleanArray = new boolean[2];
        protected byte[] dummyByteArray = new byte[2];
        protected short[] dummyShortArray = new short[2];
        protected int[] dummyIntArray = new int[2];
        protected long[] dummyLongArray = new long[2];
        protected float[] dummyFloatArray = new float[2];
        protected double[] dummyDoubleArray = new double[2];
        protected String[] dummyStringArray = { "Test 1", "Test 2" };

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ArraySubClass)) {
                return false;
            }
            // normally this value is immediately returned for 'false',
            // here: state is latched to detect potential other violations
            boolean state = true;
            ArraySubClass other = (ArraySubClass) obj;
            if (this.hashCode() != other.hashCode()) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(this.hashCode())
                        .addArgument(other.hashCode()).log("{} - hashCode is not equal: this '{}' vs. other '{}'");
                state = false;
            }

            if (!Arrays.equals(dummyBooleanArray, other.dummyBooleanArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBooleanArray)
                        .addArgument(other.dummyBooleanArray)
                        .log("{} - dummyBooleanArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyByteArray, other.dummyByteArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyByteArray)
                        .addArgument(other.dummyByteArray)
                        .log("{} - dummyByteArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyShortArray, other.dummyShortArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyShortArray)
                        .addArgument(other.dummyShortArray)
                        .log("{} - dummyShortArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyIntArray, other.dummyIntArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyIntArray)
                        .addArgument(other.dummyIntArray)
                        .log("{} - dummyIntArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyLongArray, other.dummyLongArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyLongArray)
                        .addArgument(other.dummyLongArray)
                        .log("{} - dummyLongArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyFloatArray, other.dummyFloatArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyFloatArray)
                        .addArgument(other.dummyFloatArray)
                        .log("{} - dummyFloatArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyDoubleArray, other.dummyDoubleArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyDoubleArray)
                        .addArgument(other.dummyDoubleArray)
                        .log("{} - dummyDoubleArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyStringArray, other.dummyStringArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyStringArray)
                        .addArgument(other.dummyStringArray)
                        .log("{} - dummyStringArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }

            return state;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(dummyBooleanArray);
            result = prime * result + Arrays.hashCode(dummyByteArray);
            result = prime * result + Arrays.hashCode(dummyDoubleArray);
            result = prime * result + Arrays.hashCode(dummyFloatArray);
            result = prime * result + Arrays.hashCode(dummyIntArray);
            result = prime * result + Arrays.hashCode(dummyLongArray);
            result = prime * result + Arrays.hashCode(dummyShortArray);
            result = prime * result + Arrays.hashCode(dummyStringArray);
            return result;
        }

        public void modifyValues() {
            dummyBooleanArray[0] = !dummyBooleanArray[0];
            dummyByteArray[0] = (byte) (dummyByteArray[0] + (byte) 1);
            dummyShortArray[0] = (short) (dummyShortArray[0] + (short) 1);
            dummyIntArray[0] = dummyIntArray[0] + 1;
            dummyLongArray[0] = dummyLongArray[0] + 1l;
            dummyFloatArray[0] = dummyFloatArray[0] + 0.5f;
            dummyDoubleArray[0] = dummyDoubleArray[0] + 1.5;
            dummyStringArray[0] = MODIFIED + dummyStringArray[0];
        }
    }

    public class BoxedObjectArraySubClass {
        protected Boolean[] dummyBoxedBooleanArray = { false, false };
        protected Byte[] dummyBoxedByteArray = { 0, 0 };
        protected Short[] dummyBoxedShortArray = { 0, 0 };
        protected Integer[] dummyBoxedIntArray = { 0, 0 };
        protected Long[] dummyBoxedLongArray = { 0l, 0l };
        protected Float[] dummyBoxedFloatArray = { 0.0f, 0.0f };
        protected Double[] dummyBoxedDoubleArray = { 0.0, 0.0 };
        protected String[] dummyBoxedStringArray = { "TestString#2", "TestString#2" };

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BoxedObjectArraySubClass)) {
                return false;
            }
            // normally this value is immediately returned for 'false',
            // here: state is latched to detect potential other violations
            boolean state = true;
            BoxedObjectArraySubClass other = (BoxedObjectArraySubClass) obj;
            if (this.hashCode() != other.hashCode()) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(this.hashCode())
                        .addArgument(other.hashCode()).log("{} - hashCode is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedBooleanArray, other.dummyBoxedBooleanArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedBooleanArray)
                        .addArgument(other.dummyBoxedBooleanArray)
                        .log("{} - dummyBoxedBooleanArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedByteArray, other.dummyBoxedByteArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedByteArray)
                        .addArgument(other.dummyBoxedByteArray)
                        .log("{} - dummyBoxedByteArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedDoubleArray, other.dummyBoxedDoubleArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedDoubleArray)
                        .addArgument(other.dummyBoxedDoubleArray)
                        .log("{} - dummyBoxedDoubleArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedFloatArray, other.dummyBoxedFloatArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedFloatArray)
                        .addArgument(other.dummyBoxedFloatArray)
                        .log("{} - dummyBoxedFloatArray is not equal: this '{}' vs. other '{}'");
                state = false;

            }
            if (!Arrays.equals(dummyBoxedIntArray, other.dummyBoxedIntArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedIntArray)
                        .addArgument(other.dummyBoxedIntArray)
                        .log("{} - dummyBoxedIntArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedLongArray, other.dummyBoxedLongArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedLongArray)
                        .addArgument(other.dummyBoxedLongArray)
                        .log("{} - dummyBoxedLongArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedShortArray, other.dummyBoxedShortArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedShortArray)
                        .addArgument(other.dummyBoxedShortArray)
                        .log("{} - dummyBoxedShortArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!Arrays.equals(dummyBoxedStringArray, other.dummyBoxedStringArray)) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedStringArray)
                        .addArgument(other.dummyBoxedStringArray)
                        .log("{} - dummyBoxedStringArray is not equal: this '{}' vs. other '{}'");
                state = false;
            }

            return state;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(dummyBoxedBooleanArray);
            result = prime * result + Arrays.hashCode(dummyBoxedByteArray);
            result = prime * result + Arrays.hashCode(dummyBoxedDoubleArray);
            result = prime * result + Arrays.hashCode(dummyBoxedFloatArray);
            result = prime * result + Arrays.hashCode(dummyBoxedIntArray);
            result = prime * result + Arrays.hashCode(dummyBoxedLongArray);
            result = prime * result + Arrays.hashCode(dummyBoxedShortArray);
            result = prime * result + Arrays.hashCode(dummyBoxedStringArray);
            return result;
        }

        public void modifyValues() {
            dummyBoxedBooleanArray[0] = !dummyBoxedBooleanArray[0];
            dummyBoxedByteArray[0] = (byte) (dummyBoxedByteArray[0] + (byte) 1);
            dummyBoxedShortArray[0] = (short) (dummyBoxedShortArray[0] + (short) 1);
            dummyBoxedIntArray[0] = dummyBoxedIntArray[0] + 1;
            dummyBoxedLongArray[0] = dummyBoxedLongArray[0] + 1l;
            dummyBoxedFloatArray[0] = dummyBoxedFloatArray[0] + 0.5f;
            dummyBoxedDoubleArray[0] = dummyBoxedDoubleArray[0] + 1.5;
            dummyBoxedStringArray[0] = MODIFIED + dummyBoxedStringArray[0];
        }
    }

    public class BoxedPrimitivesSubClass {
        protected Boolean dummyBoxedBoolean = Boolean.FALSE;
        protected Byte dummyBoxedByte = Byte.valueOf((byte) 0);
        protected Short dummyBoxedShort = Short.valueOf((short) 0);
        protected Integer dummyBoxedInt = Integer.valueOf(0);
        protected Long dummyBoxedLong = Long.valueOf(0l);
        protected Float dummyBoxedFloat = Float.valueOf(0f);
        protected Double dummyBoxedDouble = Double.valueOf(0.0);
        protected String dummyBoxedString = "Test";

        protected BoxedPrimitivesSubSubClass boxedPrimitivesSubSubClass = new BoxedPrimitivesSubSubClass();
        protected BoxedPrimitivesSubSubClass boxedPrimitivesSubSubClassNull; // to check instantiation

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BoxedPrimitivesSubClass)) {
                return false;
            }
            // normally this value is immediately returned for 'false',
            // here: state is latched to detect potential other violations
            boolean state = true;
            BoxedPrimitivesSubClass other = (BoxedPrimitivesSubClass) obj;
            if (this.hashCode() != other.hashCode()) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(this.hashCode())
                        .addArgument(other.hashCode()).log("{} - hashCode is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedBoolean == null && other.dummyBoxedBoolean == null) && (dummyBoxedBoolean == null
                    || other.dummyBoxedBoolean == null || !dummyBoxedBoolean.equals(other.dummyBoxedBoolean))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedBoolean)
                        .addArgument(other.dummyBoxedBoolean)
                        .log("{} - dummyBoxedBoolean is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedByte == null && other.dummyBoxedByte == null) && (dummyBoxedByte == null
                    || other.dummyBoxedByte == null || !dummyBoxedByte.equals(other.dummyBoxedByte))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedByte)
                        .addArgument(other.dummyBoxedByte)
                        .log("{} - dummyBoxedByte is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedShort == null && other.dummyBoxedShort == null) && (dummyBoxedShort == null
                    || other.dummyBoxedShort == null || !dummyBoxedShort.equals(other.dummyBoxedShort))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedShort)
                        .addArgument(other.dummyBoxedShort)
                        .log("{} - dummyBoxedShort is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedInt == null && other.dummyBoxedInt == null) && (dummyBoxedInt == null
                    || other.dummyBoxedInt == null || !dummyBoxedInt.equals(other.dummyBoxedInt))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedInt)
                        .addArgument(other.dummyBoxedInt)
                        .log("{} - dummyBoxedInt is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedLong == null && other.dummyBoxedLong == null) && (dummyBoxedLong == null
                    || other.dummyBoxedLong == null || !dummyBoxedLong.equals(other.dummyBoxedLong))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedLong)
                        .addArgument(other.dummyBoxedLong)
                        .log("{} - dummyBoxedLong is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedFloat == null && other.dummyBoxedFloat == null) && (dummyBoxedFloat == null
                    || other.dummyBoxedFloat == null || !dummyBoxedFloat.equals(other.dummyBoxedFloat))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedFloat)
                        .addArgument(other.dummyBoxedFloat)
                        .log("{} - dummyBoxedFloat is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedDouble == null && other.dummyBoxedDouble == null) && (dummyBoxedDouble == null
                    || other.dummyBoxedDouble == null || !dummyBoxedDouble.equals(other.dummyBoxedDouble))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedDouble)
                        .addArgument(other.dummyBoxedDouble)
                        .log("{} - dummyBoxedDouble is not equal: this '{}' vs. other '{}'");
                state = false;
            }
            if (!(dummyBoxedString == null && other.dummyBoxedString == null) && (dummyBoxedString == null
                    || other.dummyBoxedString == null || !dummyBoxedString.equals(other.dummyBoxedString))) {
                logBackEnd().addArgument(this.getClass().getSimpleName()).addArgument(dummyBoxedString)
                        .addArgument(other.dummyBoxedString)
                        .log("{} - dummyBoxedString is not equal: this '{}' vs. other '{}'");
                state = false;
            }

            return state;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((dummyBoxedBoolean == null) ? 0 : dummyBoxedBoolean.hashCode());
            result = prime * result + ((dummyBoxedByte == null) ? 0 : dummyBoxedByte.hashCode());
            result = prime * result + ((dummyBoxedDouble == null) ? 0 : dummyBoxedDouble.hashCode());
            result = prime * result + ((dummyBoxedFloat == null) ? 0 : dummyBoxedFloat.hashCode());
            result = prime * result + ((dummyBoxedInt == null) ? 0 : dummyBoxedInt.hashCode());
            result = prime * result + ((dummyBoxedLong == null) ? 0 : dummyBoxedLong.hashCode());
            result = prime * result + ((dummyBoxedShort == null) ? 0 : dummyBoxedShort.hashCode());
            result = prime * result + ((dummyBoxedString == null) ? 0 : dummyBoxedString.hashCode());
            return result;
        }

        public void modifyValues() {
            dummyBoxedBoolean = !dummyBoxedBoolean;
            dummyBoxedByte = (byte) (dummyBoxedByte + 1);
            dummyBoxedShort = (short) (dummyBoxedShort + 2);
            dummyBoxedInt = dummyBoxedInt + 1;
            dummyBoxedLong = dummyBoxedLong + 1;
            dummyBoxedFloat = dummyBoxedFloat + 0.5f;
            dummyBoxedDouble = dummyBoxedDouble + 1.5;
            dummyBoxedString = MODIFIED + dummyBoxedString;
            arrayListInteger.set(0, arrayListInteger.get(0) + 1);
            arrayListString.set(0, MODIFIED + arrayListString.get(0));
        }

        @Override
        public String toString() {
            return new StringBuilder()//
                    .append("[dummyBoxedBoolean=").append(dummyBoxedBoolean)//
                    .append(", dummyBoxedByte=").append(dummyBoxedByte)//
                    .append(", dummyBoxedShort=").append(dummyBoxedShort)//
                    .append(", dummyBoxedInt=").append(dummyBoxedInt)//
                    .append(", dummyBoxedFloat=").append(dummyBoxedFloat)//
                    .append(", dummyBoxedDouble=").append(dummyBoxedDouble)//
                    .append(", dummyBoxedString=").append(dummyBoxedString)//
                    .append(']').toString();
        }

        @SuppressWarnings("hiding")
        public class BoxedPrimitivesSubSubClass {
            protected Boolean dummyBoxedBooleanL2 = true;
            protected Byte dummyBoxedByteL2 = Byte.valueOf((byte) 0);
            protected Short dummyBoxedShortL2 = Short.valueOf((short) 0);
            protected Integer dummyBoxedIntL2 = Integer.valueOf(0);
            protected Long dummyBoxedLongL2 = Long.valueOf(0l);
            protected Float dummyBoxedFloatL2 = Float.valueOf(0f);
            protected Double dummyBoxedDoubleL2 = Double.valueOf(0.0);
            protected String dummyBoxedStringL2 = "Test";

            public BoxedPrimitivesSubSubSubClass boxedPrimitivesSubSubSubClass = new BoxedPrimitivesSubSubSubClass();
            public BoxedPrimitivesSubSubSubClass boxedPrimitivesSubSubSubClassNull; // to check instantiation

            @SuppressWarnings("hiding")
            public class BoxedPrimitivesSubSubSubClass {
                protected Boolean dummyBoxedBooleanL3 = false;
                protected Byte dummyBoxedByteL3 = Byte.valueOf((byte) 0);
                protected Short dummyBoxedShortL3 = Short.valueOf((short) 0);
                protected Integer dummyBoxedIntL3 = Integer.valueOf(0);
                protected Long dummyBoxedLongL3 = Long.valueOf(0l);
                protected Float dummyBoxedFloatL3 = Float.valueOf(0f);
                protected Double dummyBoxedDoubleL3 = Double.valueOf(0.0);
                protected String dummyBoxedStringL3 = "Test";
            }
        }

    }

    public enum TestEnum {
        TEST_CASE_1, TEST_CASE_2, TEST_CASE_3, TEST_CASE_4;
    }
}
