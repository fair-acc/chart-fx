package de.gsi.dataset.serializer.spi.iobuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.IoClassSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ByteBuffer;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.MultiDimDoubleDataSet;
import de.gsi.dataset.testdata.spi.TriangleFunction;

/**
 * @author Alexander Krimm
 * @author rstein
 */
class DataSetSerialiserTests {
    private static final int BUFFER_SIZE = 10000;
    private static final String[] DEFAULT_AXES_NAME = { "x", "y", "z" };
    private static final double DELTA = 1e-3;

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testDataSet(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE);

        boolean asFloat32 = false;
        final DoubleDataSet original = new DoubleDataSet(new TriangleFunction("test", 1009));
        addMetaData(original, true);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));

        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        buffer.reset(); // reset to read position (==0)
        final DataSet restored = ioSerialiser.readDataSetFromByteArray();

        assertEquals(original, restored);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testDataSetErrorSymmetric(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        boolean asFloat32 = false;

        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", new double[] { 1, 2, 3 },
                new double[] { 6, 7, 8 }, new double[] { 7, 8, 9 }, new double[] { 7, 8, 9 }, 3, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public ErrorType getErrorType(int dimIndex) {
                if (dimIndex == 1) {
                    return ErrorType.SYMMETRIC;
                }
                return super.getErrorType(dimIndex);
            }
        };
        addMetaData(original, true);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));
        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        buffer.reset(); // reset to read position (==0)
        final DefaultErrorDataSet restored = (DefaultErrorDataSet) ioSerialiser.readDataSetFromByteArray();

        assertEquals(new DefaultErrorDataSet(original), new DefaultErrorDataSet(restored));
    }

    @DisplayName("test getDoubleArray([boolean[], byte[], ..., String[]) helper method")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testGetDoubleArrayHelper(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer);

        putGenericTestArrays(ioSerialiser);

        buffer.reset();

        // test conversion to double array
        ioSerialiser.checkHeaderInfo();
        assertThrows(IllegalArgumentException.class, () -> DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.OTHER));
        assertArrayEquals(new double[] { 1.0, 0.0, 1.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.BOOL_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.BYTE_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.CHAR_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.SHORT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.INT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.LONG_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.FLOAT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.DOUBLE_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, DataSetSerialiser.getDoubleArray(ioSerialiser, DataType.STRING_ARRAY));
    }

    private static void putGenericTestArrays(final BinarySerialiser ioSerialiser) {
        ioSerialiser.putHeaderInfo();
        ioSerialiser.putGenericArrayAsPrimitive(DataType.BOOL, new Boolean[] { true, false, true }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.BYTE, new Byte[] { (byte) 1, (byte) 0, (byte) 2 }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.CHAR, new Character[] { (char) 1, (char) 0, (char) 2 }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.SHORT, new Short[] { (short) 1, (short) 0, (short) 2 }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.INT, new Integer[] { 1, 0, 2 }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.LONG, new Long[] { 1L, 0L, 2L }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.FLOAT, new Float[] { (float) 1, (float) 0, (float) 2 }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.DOUBLE, new Double[] { (double) 1, (double) 0, (double) 2 }, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.STRING, new String[] { "1.0", "0.0", "2.0" }, 3);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testDataSetFloatError(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE);

        boolean asFloat32 = true;
        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", new double[] { 1f, 2f, 3f },
                new double[] { 6f, 7f, 8f }, new double[] { 7f, 8f, 9f }, new double[] { 7f, 8f, 9f }, 3, false);
        addMetaData(original, true);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));

        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        buffer.reset(); // reset to read position (==0)
        final DataSet restored = ioSerialiser.readDataSetFromByteArray();

        assertEquals(original, restored);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testDataSetFloatErrorSymmetric(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE);
        boolean asFloat32 = true;

        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", new double[] { 1f, 2f, 3f },
                new double[] { 6f, 7f, 8f }, new double[] { 7f, 8f, 9f }, new double[] { 7f, 8f, 9f }, 3, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public ErrorType getErrorType(int dimIndex) {
                if (dimIndex == 1) {
                    return ErrorType.SYMMETRIC;
                }
                return super.getErrorType(dimIndex);
            }
        };
        addMetaData(original, true);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));

        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        buffer.reset(); // reset to read position (==0)
        final DefaultErrorDataSet restored = (DefaultErrorDataSet) ioSerialiser.readDataSetFromByteArray();

        assertEquals(new DefaultErrorDataSet(original), new DefaultErrorDataSet(restored));
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testErrorDataSet(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(10 * BUFFER_SIZE);

        boolean asFloat32 = false;
        final DoubleErrorDataSet original = new DoubleErrorDataSet(new TriangleFunction("test", 1009));
        addMetaData(original, true);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));

        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        buffer.reset(); // reset to read position (==0)
        final DataSet restored = ioSerialiser.readDataSetFromByteArray();

        assertEquals(original, restored);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testGenericSerialiserIdentity(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE);

        IoClassSerialiser serialiser = new IoClassSerialiser(buffer, BinarySerialiser.class);

        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", //
                new double[] { 1f, 2f, 3f }, new double[] { 6f, 7f, 8f }, //
                new double[] { 0.7f, 0.8f, 0.9f }, new double[] { 7f, 8f, 9f }, 3, false);
        addMetaData(original, true);
        DataSetWrapper dsOrig = new DataSetWrapper();
        dsOrig.source = original;
        DataSetWrapper cpOrig = new DataSetWrapper();

        // serialise-deserialise DataSet
        buffer.reset(); // '0' writing at start of buffer
        serialiser.serialiseObject(dsOrig);

        // buffer.reset(); // reset to read position (==0)
        // final WireDataFieldDescription root = serialiser.getIoSerialiser().parseIoStream(true);
        // root.printFieldStructure();

        buffer.reset(); // reset to read position (==0)
        serialiser.deserialiseObject(cpOrig);

        // check DataSet for equality
        if (!(cpOrig.source instanceof DataSetError)) {
            throw new IllegalStateException("DataSet '" + cpOrig.source + "' is not not instanceof DataSetError");
        }
        DataSetError test = (DataSetError) (cpOrig.source);

        testIdentityCore(original, test);
        testIdentityLabelsAndStyles(original, test, true);
        if (test instanceof DataSetMetaData) {
            testIdentityMetaData(original, (DataSetMetaData) test, true);
        }
        assertEquals(dsOrig.source, test);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testMultiDimDataSet(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE);

        boolean asFloat32 = false;
        final MultiDimDoubleDataSet original = new MultiDimDoubleDataSet("test", false,
                new double[][] { { 1, 2, 3 }, { 10, 20 }, { 0.5, 1, 1.5, 2, 2.5, 3 } });
        // Labels and styles are not correctly handled by multi dim data set because it is not really defined on which
        // dimension the label index is defined
        addMetaData(original, false);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));

        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        buffer.reset(); // reset to read position (==0)
        final DataSet restored = ioSerialiser.readDataSetFromByteArray();

        assertEquals(original, restored);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testMultiDimDataSetFloatNoMetaDataAndLabels(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE);

        boolean asFloat32 = true;
        final MultiDimDoubleDataSet original = new MultiDimDoubleDataSet("test", false,
                new double[][] { { 1, 2, 3 }, { 10, 20 }, { 0.5, 1, 1.5, 2, 2.5, 3 } });
        addMetaData(original, false);

        final DataSetSerialiser ioSerialiser = new DataSetSerialiser(new BinarySerialiser(buffer));

        ioSerialiser.setDataLablesSerialised(false);
        ioSerialiser.setMetaDataSerialised(false);
        ioSerialiser.writeDataSetToByteArray(original, asFloat32);
        ioSerialiser.setDataLablesSerialised(true);
        ioSerialiser.setMetaDataSerialised(true);
        buffer.reset(); // reset to read position (==0)
        final DataSet restored = ioSerialiser.readDataSetFromByteArray();

        MultiDimDoubleDataSet originalNoMetaData = new MultiDimDoubleDataSet(original);
        original.getMetaInfo().clear();
        original.getErrorList().clear();
        original.getWarningList().clear();
        original.getInfoList().clear();

        assertEquals(originalNoMetaData, restored);
    }

    @Test
    void testMiscellaneous() {
        assertEquals(0, DataSetSerialiser.getDimIndex("axis0", "axis"));
        assertDoesNotThrow(() -> DataSetSerialiser.getDimIndex("axi0", "axis"));
        assertEquals(-1, DataSetSerialiser.getDimIndex("axi0", "axis"));
        assertDoesNotThrow(() -> DataSetSerialiser.getDimIndex("axis0.1", "axis"));
        assertEquals(-1, DataSetSerialiser.getDimIndex("axis0.1", "axis"));
    }

    private static void addMetaData(final AbstractDataSet<?> dataSet, final boolean addLabelsStyles) {
        if (addLabelsStyles) {
            dataSet.addDataLabel(1, "test");
            dataSet.addDataStyle(2, "color: red");
        }
        dataSet.getMetaInfo().put("Test", "Value");
        dataSet.getErrorList().add("TestError");
        dataSet.getWarningList().add("TestWarning");
        dataSet.getInfoList().add("TestInfo");
    }

    private static String encodingBinary(final boolean isBinaryEncoding) {
        return isBinaryEncoding ? "binary-based" : "string-based";
    }

    private static boolean floatInequality(double a, double b) {
        // 32-bit float uses 23-bit for the mantissa
        return Math.abs((float) a - (float) b) > 2 / Math.pow(2, 23);
    }

    private static void testIdentityCore(final DataSetError original, final DataSetError test) {
        // some checks
        assertEquals(original.getName(), test.getName(), "name");
        assertEquals(original.getDimension(), test.getDimension(), "dimension");

        assertEquals(original.getDataCount(), test.getDataCount(), "getDataCount()");

        // check for numeric value
        final int dataCount = original.getDataCount();
        for (int dim = 0; dim < original.getDimension(); dim++) {
            final String dStr = dim < DEFAULT_AXES_NAME.length ? DEFAULT_AXES_NAME[dim] : "dim" + (dim + 1) + "-Axis";

            assertEquals(original.getErrorType(dim), test.getErrorType(dim), dStr + " error Type");
            assertArrayEquals(Arrays.copyOfRange(original.getValues(dim), 0, dataCount), Arrays.copyOfRange(test.getValues(dim), 0, dataCount), DELTA, dStr + "-Values");
            assertArrayEquals(Arrays.copyOfRange(original.getErrorsPositive(dim), 0, dataCount), Arrays.copyOfRange(test.getErrorsPositive(dim), 0, dataCount), DELTA, dStr + "-Errors positive");
            assertArrayEquals(Arrays.copyOfRange(original.getErrorsNegative(dim), 0, dataCount), Arrays.copyOfRange(test.getErrorsNegative(dim), 0, dataCount), DELTA, dStr + "-Errors negative");
        }
    }

    private static void testIdentityLabelsAndStyles(final DataSet2D originalDS, final DataSet testDS, final boolean binary) {
        // check for labels & styles
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            if (originalDS.getDataLabel(i) == null && testDS.getDataLabel(i) == null) {
                // cannot compare null vs null
                continue;
            }
            if (!originalDS.getDataLabel(i).equals(testDS.getDataLabel(i))) {
                String msg = String.format("data set label do not match (%s): original(%d) ='%s' vs. copy(%d) ='%s' %n",
                        encodingBinary(binary), i, originalDS.getDataLabel(i), i, testDS.getDataLabel(i));
                throw new IllegalStateException(msg);
            }
        }
        for (int i = 0; i < originalDS.getDataCount(); i++) {
            if (originalDS.getStyle(i) == null && testDS.getStyle(i) == null) {
                // cannot compare null vs null
                continue;
            }
            if (!originalDS.getStyle(i).equals(testDS.getStyle(i))) {
                String msg = String.format("data set style do not match (%s): original(%d) ='%s' vs. copy(%d) ='%s' %n",
                        encodingBinary(binary), i, originalDS.getStyle(i), i, testDS.getStyle(i));
                throw new IllegalStateException(msg);
            }
        }
    }

    private static void testIdentityMetaData(final DataSetMetaData originalDS, final DataSetMetaData testDS, final boolean binary) {
        // check for meta data and meta messages
        if (!originalDS.getInfoList().equals(testDS.getInfoList())) {
            String msg = String.format("data set info lists do not match (%s): original ='%s' vs. copy ='%s' %n",
                    encodingBinary(binary), originalDS.getInfoList(), testDS.getInfoList());
            throw new IllegalStateException(msg);
        }
    }

    private static class DataSetWrapper {
        public DataSet source;
    }
}
