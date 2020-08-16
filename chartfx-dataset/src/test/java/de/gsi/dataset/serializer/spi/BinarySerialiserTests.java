package de.gsi.dataset.serializer.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.helper.MyGenericClass;
import de.gsi.dataset.utils.AssertUtils;

/**
 *
 * @author rstein
 */
@SuppressWarnings("PMD.ExcessiveMethodLength")
class BinarySerialiserTests {
    private static final int BUFFER_SIZE = 2000;
    private static final int ARRAY_DIM_1D = 1; // array dimension

    private void putGenericTestArrays(final BinarySerialiser ioSerialiser) {
        ioSerialiser.putHeaderInfo();
        ioSerialiser.putGenericArrayAsPrimitive(DataType.BOOL, new Boolean[] { true, false, true }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.BYTE, new Byte[] { (byte) 1, (byte) 0, (byte) 2 }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.CHAR, new Character[] { (char) 1, (char) 0, (char) 2 }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.SHORT, new Short[] { (short) 1, (short) 0, (short) 2 }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.INT, new Integer[] { 1, 0, 2 }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.LONG, new Long[] { 1L, 0L, 2L }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.FLOAT, new Float[] { (float) 1, (float) 0, (float) 2 }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.DOUBLE, new Double[] { (double) 1, (double) 0, (double) 2 }, 0, 3);
        ioSerialiser.putGenericArrayAsPrimitive(DataType.STRING, new String[] { "1.0", "0.0", "2.0" }, 0, 3);
    }

    @DisplayName("basic primitive array writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testBasicInterfacePrimitiveArrays(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        Deque<Integer> positionBefore = new LinkedList<>();
        Deque<Integer> positionAfter = new LinkedList<>();

        // add primitive array types
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL_ARRAY);
        ioSerialiser.getBuffer().putBooleanArray(new boolean[] { true, false });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("byte", DataType.BYTE_ARRAY);
        ioSerialiser.getBuffer().putByteArray(new byte[] { (byte) 42, (byte) 42 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("short", DataType.SHORT_ARRAY);
        ioSerialiser.getBuffer().putShortArray(new short[] { (short) 43, (short) 43 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("int", DataType.INT_ARRAY);
        ioSerialiser.getBuffer().putIntArray(new int[] { 44, 44 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("long", DataType.LONG_ARRAY);
        ioSerialiser.getBuffer().putLongArray(new long[] { (long) 45, (long) 45 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("float", DataType.FLOAT_ARRAY);
        ioSerialiser.getBuffer().putFloatArray(new float[] { 1.0f, 1.0f });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("double", DataType.DOUBLE_ARRAY);
        ioSerialiser.getBuffer().putDoubleArray(new double[] { 3.0, 3.0 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING_ARRAY);
        ioSerialiser.getBuffer().putStringArray(new String[] { "test", "test" });
        positionAfter.add(buffer.position());

        WireDataFieldDescription header;
        int positionAfterFieldHeader;
        long skipNBytes;
        int[] dims;
        // check primitive types
        buffer.reset();
        assertEquals(0, buffer.position(), "initial buffer position");

        // boolean
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final boolean[] booleanArray = ioSerialiser.getBuffer().getBooleanArray();
        assertArrayEquals(new boolean[] { true, false }, booleanArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // byte
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("byte", header.getFieldName(), "byte field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final byte[] byteArray = ioSerialiser.getBuffer().getByteArray();
        assertArrayEquals(new byte[] { (byte) 42, (byte) 42 }, byteArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // short
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("short", header.getFieldName(), "short field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final short[] shortArray = ioSerialiser.getBuffer().getShortArray();
        assertArrayEquals(new short[] { (short) 43, (short) 43 }, shortArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // int
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("int", header.getFieldName(), "int field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final int[] intArray = ioSerialiser.getBuffer().getIntArray();
        assertNotNull(intArray);
        assertArrayEquals(new int[] { 44, 44 }, intArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // long
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("long", header.getFieldName(), "long field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final long[] longArray = ioSerialiser.getBuffer().getLongArray();
        assertNotNull(longArray);
        assertArrayEquals(new long[] { 45, 45 }, longArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // float
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("float", header.getFieldName(), "float field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final float[] floatArray = ioSerialiser.getBuffer().getFloatArray();
        assertNotNull(floatArray);
        assertArrayEquals(new float[] { 1.0f, 1.0f }, floatArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // double
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("double", header.getFieldName(), "double field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final double[] doubleArray = ioSerialiser.getBuffer().getDoubleArray();
        assertNotNull(doubleArray);
        assertArrayEquals(new double[] { 3.0, 3.0 }, doubleArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // string
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        final String[] stringArray = ioSerialiser.getBuffer().getStringArray();
        assertNotNull(stringArray);
        assertArrayEquals(new String[] { "test", "test" }, stringArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual number of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @DisplayName("basic primitive writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testBasicInterfacePrimitives(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        Deque<Integer> positionBefore = new LinkedList<>();
        Deque<Integer> positionAfter = new LinkedList<>();

        // add primitive types
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.getBuffer().putBoolean(true);
        positionAfter.add(buffer.position());
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.getBuffer().putBoolean(false);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("byte", DataType.BYTE);
        ioSerialiser.getBuffer().putByte((byte) 42);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("short", DataType.SHORT);
        ioSerialiser.getBuffer().putShort((short) 43);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("int", DataType.INT);
        ioSerialiser.getBuffer().putInt(44);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("long", DataType.LONG);
        ioSerialiser.getBuffer().putLong(45L);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("float", DataType.FLOAT);
        ioSerialiser.getBuffer().putFloat(1.0f);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("double", DataType.DOUBLE);
        ioSerialiser.getBuffer().putDouble(3.0);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.getBuffer().putString("test");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.getBuffer().putString("");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.getBuffer().putString(null);
        positionAfter.add(buffer.position());

        WireDataFieldDescription header;
        // check primitive types
        buffer.reset();
        assertEquals(0, buffer.position(), "initial buffer position");

        // boolean
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        assertTrue(ioSerialiser.getBuffer().getBoolean(), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        assertFalse(ioSerialiser.getBuffer().getBoolean(), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // byte
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("byte", header.getFieldName(), "boolean field name retrieval");
        assertEquals(42, ioSerialiser.getBuffer().getByte(), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // short
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("short", header.getFieldName(), "short field name retrieval");
        assertEquals(43, ioSerialiser.getBuffer().getShort(), "short retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // int
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("int", header.getFieldName(), "int field name retrieval");
        assertEquals(44, ioSerialiser.getBuffer().getInt(), "int retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // long
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("long", header.getFieldName(), "long field name retrieval");
        assertEquals(45, ioSerialiser.getBuffer().getLong(), "long retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // float
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("float", header.getFieldName(), "float field name retrieval");
        assertEquals(1.0f, ioSerialiser.getBuffer().getFloat(), "float retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // double
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("double", header.getFieldName(), "double field name retrieval");
        assertEquals(3.0, ioSerialiser.getBuffer().getDouble(), "double retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // string
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("test", ioSerialiser.getBuffer().getString(), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("", ioSerialiser.getBuffer().getString(), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("", ioSerialiser.getBuffer().getString(), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @DisplayName("basic tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testHeaderAndSpecialItems(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer);

        Deque<Integer> positionBefore = new LinkedList<>();
        Deque<Integer> positionAfter = new LinkedList<>();

        // add header info
        positionBefore.add(buffer.position());
        ioSerialiser.putHeaderInfo();
        positionAfter.add(buffer.position());

        // add start marker
        positionBefore.add(buffer.position());
        ioSerialiser.putStartMarker("StartMarker");
        positionAfter.add(buffer.position());

        // add Collection - List<E>
        final List<Integer> list = Arrays.asList(1, 2, 3);
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("collection", DataType.COLLECTION);
        ioSerialiser.put(list, Integer.class, null);
        positionAfter.add(buffer.position());

        // add Collection - Set<E>
        final Set<Integer> set = Set.of(1, 2, 3);
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("set", DataType.SET);
        ioSerialiser.put(set, Integer.class, null);
        positionAfter.add(buffer.position());

        // add Collection - Queue<E>
        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("queue", DataType.QUEUE);
        ioSerialiser.put(queue, Integer.class, null);
        positionAfter.add(buffer.position());

        // add Map
        final Map<Integer, String> map = new HashMap<>();
        list.forEach(item -> map.put(item, "Item#" + item.toString()));
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("map", DataType.MAP);
        ioSerialiser.put(map, Integer.class, String.class, null);
        positionAfter.add(buffer.position());

        // add Enum
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("enum", DataType.ENUM);
        ioSerialiser.put(DataType.ENUM);
        positionAfter.add(buffer.position());

        // add end marker
        positionBefore.add(buffer.position());
        ioSerialiser.putEndMarker("EndMarker");
        positionAfter.add(buffer.position());

        buffer.reset();

        WireDataFieldDescription header;
        int positionAfterFieldHeader;
        long skipNBytes;
        // check types
        assertEquals(0, buffer.position(), "initial buffer position");

        // header info
        assertEquals(positionBefore.removeFirst(), buffer.position());
        ProtocolInfo headerInfo = ioSerialiser.checkHeaderInfo();
        assertNotEquals(headerInfo, new Object()); // silly comparison for coverage reasons
        assertNotNull(headerInfo);
        assertEquals(BinarySerialiser.class.getCanonicalName(), headerInfo.getProducerName());
        assertEquals(BinarySerialiser.VERSION_MAJOR, headerInfo.getVersionMajor());
        assertEquals(BinarySerialiser.VERSION_MINOR, headerInfo.getVersionMinor());
        assertEquals(BinarySerialiser.VERSION_MICRO, headerInfo.getVersionMicro());
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // start marker
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("StartMarker", header.getFieldName(), "StartMarker type retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - List
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("collection", header.getFieldName(), "Collection<E> field name");
        assertEquals(DataType.COLLECTION, header.getDataType(), "Collection<E> - type ID");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertNotNull(positionAfter.peekFirst());
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getBuffer().getInt(), "dimension");
        assertEquals(3, ioSerialiser.getBuffer().getInt(), "array size");
        buffer.position(header.getDataStartPosition());
        final int readPosition = buffer.position();
        Collection<Integer> retrievedCollection = ioSerialiser.getCollection(null, null);
        assertNotNull(retrievedCollection, "retrieved collection not null");
        assertEquals(list, retrievedCollection);
        assertEquals(buffer.position(), header.getDataStartPosition() + header.getDataSize(), "buffer position data end");
        // check for specific List interface
        buffer.position(readPosition);
        List<Integer> retrievedList = ioSerialiser.getList(null, null);
        assertNotNull(retrievedList, "retrieved collection List not null");
        assertEquals(list, retrievedList);
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - Set
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("set", header.getFieldName(), "Set<E> field name");
        assertEquals(DataType.SET, header.getDataType(), "Set<E> - type ID");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertNotNull(positionAfter.peekFirst());
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getBuffer().getInt(), "dimension");
        assertEquals(3, ioSerialiser.getBuffer().getInt(), "array size");
        buffer.position(header.getDataStartPosition());
        Collection<Integer> retrievedSet = ioSerialiser.getSet(null, null);
        assertNotNull(retrievedSet, "retrieved set not null");
        assertEquals(set, retrievedSet);
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - Queue
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("queue", header.getFieldName(), "Queue<E> field name");
        assertEquals(DataType.QUEUE, header.getDataType(), "Queue<E> - type ID");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertNotNull(positionAfter.peekFirst());
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getBuffer().getInt(), "dimension");
        assertEquals(3, ioSerialiser.getBuffer().getInt(), "array size");
        buffer.position(header.getDataStartPosition());
        Queue<Integer> retrievedQueue = ioSerialiser.getQueue(null, null);
        assertNotNull(retrievedQueue, "retrieved set not null");
        // assertEquals(queue, retrievedQueue); // N.B. no direct comparison possible -> only partial Queue interface overlapp
        while (!queue.isEmpty() && !retrievedQueue.isEmpty()) {
            assertEquals(queue.poll(), retrievedQueue.poll());
        }
        assertEquals(0, queue.size(), "reference queue empty");
        assertEquals(0, retrievedQueue.size(), "retrieved queue empty");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // retrieve Map
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("map", header.getFieldName(), "Map<K,V> field name");
        assertEquals(DataType.MAP, header.getDataType(), "Map<K,V> - type ID");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertNotNull(positionAfter.peekFirst());
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getBuffer().getInt(), "dimension");
        assertEquals(3, ioSerialiser.getBuffer().getInt(), "array size");
        buffer.position(header.getDataStartPosition());
        Map<Integer, String> retrievedMap = ioSerialiser.getMap(null, null);
        assertNotNull(retrievedMap, "retrieved set not null");
        assertEquals(map, retrievedMap); // N.B. no direct comparison possible -> only partial Queue interface overlapp
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // enum
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("enum", header.getFieldName(), "enum type retrieval");
        buffer.position(header.getDataStartPosition());
        assertDoesNotThrow(ioSerialiser::getEnumTypeList); //skips enum info
        buffer.position(header.getDataStartPosition());
        assertEquals(DataType.ENUM, ioSerialiser.getEnum(DataType.OTHER), "enum retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // end marker
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("EndMarker", header.getFieldName(), "EndMarker type retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @Test
    void testIdentityGenericObject() {
        // Simple tests to verify that the equals and hashCode functions of 'MyGenericClass' work as expected
        final MyGenericClass rootObject1 = new MyGenericClass();
        final MyGenericClass rootObject2 = new MyGenericClass();
        MyGenericClass.setVerboseChecks(false);

        assertNotNull(rootObject1.toString());
        assertNotNull(rootObject1.boxedPrimitives.toString());

        assertEquals(rootObject1, rootObject2);

        rootObject1.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.modifyValues();
        assertEquals(rootObject1, rootObject2);

        rootObject1.boxedPrimitives.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.boxedPrimitives.modifyValues();
        assertEquals(rootObject1, rootObject2);

        rootObject1.arrays.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.arrays.modifyValues();
        assertEquals(rootObject1, rootObject2);

        rootObject1.objArrays.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.objArrays.modifyValues();
        assertEquals(rootObject1, rootObject2);
    }

    @DisplayName("basic primitive array writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testParseIoStream(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer);

        ioSerialiser.putHeaderInfo(); // add header info

        // add some primitives
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.getBuffer().putBoolean(true);
        ioSerialiser.putFieldHeader("byte", DataType.BYTE);
        ioSerialiser.getBuffer().putByte((byte) 42);
        ioSerialiser.putFieldHeader("short", DataType.SHORT);
        ioSerialiser.getBuffer().putShort((short) 42);
        ioSerialiser.putFieldHeader("int", DataType.INT);
        ioSerialiser.getBuffer().putInt(42);
        ioSerialiser.putFieldHeader("long", DataType.LONG);
        ioSerialiser.getBuffer().putLong(42L);
        ioSerialiser.putFieldHeader("float", DataType.FLOAT);
        ioSerialiser.getBuffer().putFloat(42f);
        ioSerialiser.putFieldHeader("double", DataType.DOUBLE);
        ioSerialiser.getBuffer().putDouble(42);
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.getBuffer().putString("string");

        ioSerialiser.putFieldHeader("boolean[]", DataType.BOOL_ARRAY);
        ioSerialiser.getBuffer().putBooleanArray(new boolean[] { true }, 0, 1);
        ioSerialiser.putFieldHeader("byte[]", DataType.BYTE_ARRAY);
        ioSerialiser.getBuffer().putByteArray(new byte[] { (byte) 42 }, 0, 1);
        ioSerialiser.putFieldHeader("short[]", DataType.SHORT_ARRAY);
        ioSerialiser.getBuffer().putShortArray(new short[] { (short) 42 }, 0, 1);
        ioSerialiser.putFieldHeader("int[]", DataType.INT_ARRAY);
        ioSerialiser.getBuffer().putIntArray(new int[] { 42 }, 0, 1);
        ioSerialiser.putFieldHeader("long[]", DataType.LONG_ARRAY);
        ioSerialiser.getBuffer().putLongArray(new long[] { 42L }, 0, 1);
        ioSerialiser.putFieldHeader("float[]", DataType.FLOAT_ARRAY);
        ioSerialiser.getBuffer().putFloatArray(new float[] { (float) 42 }, 0, 1);
        ioSerialiser.putFieldHeader("double[]", DataType.DOUBLE_ARRAY);
        ioSerialiser.getBuffer().putDoubleArray(new double[] { (double) 42 }, 0, 1);
        ioSerialiser.putFieldHeader("string[]", DataType.STRING_ARRAY);
        ioSerialiser.getBuffer().putStringArray(new String[] { "string" }, 0, 1);

        final Collection<Integer> collection = Arrays.asList(1, 2, 3);
        ioSerialiser.putFieldHeader("collection", DataType.COLLECTION);
        ioSerialiser.put(collection, Integer.class, null); // add Collection - List<E>

        final List<Integer> list = Arrays.asList(1, 2, 3);
        ioSerialiser.putFieldHeader("list", DataType.LIST);
        ioSerialiser.put(list, Integer.class, null); // add Collection - List<E>

        final Set<Integer> set = Set.of(1, 2, 3);
        ioSerialiser.putFieldHeader("set", DataType.SET);
        ioSerialiser.put(set, Integer.class, null); // add Collection - Set<E>

        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        ioSerialiser.putFieldHeader("queue", DataType.QUEUE);
        ioSerialiser.put(queue, Integer.class, null); // add Collection - Queue<E>

        final Map<Integer, String> map = new HashMap<>();
        list.forEach(item -> map.put(item, "Item#" + item.toString()));
        ioSerialiser.putFieldHeader("map", DataType.MAP);
        ioSerialiser.put(map, Integer.class, String.class, null); // add Map

        ioSerialiser.putFieldHeader("enum", DataType.ENUM);
        ioSerialiser.put(DataType.ENUM); // add Enum

        // start nested data
        ioSerialiser.putStartMarker("nested context"); // add end marker
        ioSerialiser.putFieldHeader("booleanArray", DataType.BOOL_ARRAY);
        ioSerialiser.getBuffer().putBooleanArray(new boolean[] { true }, 0, 1);
        ioSerialiser.putFieldHeader("byteArray", DataType.BYTE_ARRAY);
        ioSerialiser.getBuffer().putByteArray(new byte[] { (byte) 0x42 }, 0, 1);

        ioSerialiser.putEndMarker("nested context"); // add end marker
        // end nested data

        ioSerialiser.putEndMarker("Life is good!"); // add end marker

        buffer.reset();

        // and read back streamed items
        final WireDataFieldDescription objectRoot = ioSerialiser.parseIoStream(true);
        assertNotNull(objectRoot);
        // objectRoot.printFieldStructure();
    }

    @DisplayName("test getGenericArrayAsBoxedPrimitive(...) helper method")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testGetGenericArrayAsBoxedPrimitiveHelper(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer);

        putGenericTestArrays(ioSerialiser);

        buffer.reset();

        // test conversion to double array
        ioSerialiser.checkHeaderInfo();
        assertArrayEquals(new Boolean[] { true, false, true }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.BOOL));
        assertArrayEquals(new Byte[] { (byte) 1.0, (byte) 0.0, (byte) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.BYTE));
        assertArrayEquals(new Character[] { (char) 1.0, (char) 0.0, (char) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.CHAR));
        assertArrayEquals(new Short[] { (short) 1.0, (short) 0.0, (short) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.SHORT));
        assertArrayEquals(new Integer[] { 1, 0, 2 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.INT));
        assertArrayEquals(new Long[] { (long) 1.0, (long) 0.0, (long) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.LONG));
        assertArrayEquals(new Float[] { 1.0f, 0.0f, 2.0f }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.FLOAT));
        assertArrayEquals(new Double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.DOUBLE));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.STRING));
    }

    @Test
    void testMisc() {
        final BinarySerialiser ioSerialiser = new BinarySerialiser(new FastByteBuffer(1000)); // TODO: generalise to IoBuffer
        final int bufferIncrements = ioSerialiser.getBufferIncrements();
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        ioSerialiser.setBufferIncrements(bufferIncrements + 1);
        assertEquals(bufferIncrements + 1, ioSerialiser.getBufferIncrements());
    }
}
