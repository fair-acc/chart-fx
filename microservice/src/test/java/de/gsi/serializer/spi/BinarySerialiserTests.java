package de.gsi.serializer.spi;

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

import de.gsi.dataset.utils.AssertUtils;
import de.gsi.serializer.DataType;
import de.gsi.serializer.IoBuffer;
import de.gsi.serializer.spi.helper.MyGenericClass;

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
        ioSerialiser.put("boolean", new boolean[] { true, false });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("byte", new byte[] { (byte) 42, (byte) 42 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("short", new short[] { (short) 43, (short) 43 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("int", new int[] { 44, 44 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("long", new long[] { (long) 45, (long) 45 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("float", new float[] { 1.0f, 1.0f });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("double", new double[] { 3.0, 3.0 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("string", new String[] { "test", "test" });
        positionAfter.add(buffer.position());

        WireDataFieldDescription header;
        int positionAfterFieldHeader;
        long skipNBytes;
        int[] dims;
        // check primitive types
        buffer.flip();
        assertEquals(0, buffer.position(), "initial buffer position");

        // boolean
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartPosition(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartPosition() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new boolean[] { true, false }, ioSerialiser.getBuffer().getBooleanArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new boolean[] { true, false }, ioSerialiser.getBooleanArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new byte[] { (byte) 42, (byte) 42 }, ioSerialiser.getBuffer().getByteArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new byte[] { (byte) 42, (byte) 42 }, ioSerialiser.getByteArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new short[] { (short) 43, (short) 43 }, ioSerialiser.getBuffer().getShortArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new short[] { (short) 43, (short) 43 }, ioSerialiser.getShortArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new int[] { 44, 44 }, ioSerialiser.getBuffer().getIntArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 44, 44 }, ioSerialiser.getIntArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new long[] { 45, 45 }, ioSerialiser.getBuffer().getLongArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new long[] { 45, 45 }, ioSerialiser.getLongArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new float[] { 1.0f, 1.0f }, ioSerialiser.getBuffer().getFloatArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new float[] { 1.0f, 1.0f }, ioSerialiser.getFloatArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new double[] { 3.0, 3.0 }, ioSerialiser.getBuffer().getDoubleArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new double[] { 3.0, 3.0 }, ioSerialiser.getDoubleArray()); // get data from IoSerialiser
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
        dims = ioSerialiser.getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new int[] { 2 }, ioSerialiser.getArraySizeDescriptor()); // manual dimension check
        assertArrayEquals(new String[] { "test", "test" }, ioSerialiser.getBuffer().getStringArray()); // get data from IoBuffer
        buffer.position(header.getDataStartPosition()); // return to original data start
        assertArrayEquals(new String[] { "test", "test" }, ioSerialiser.getStringArray()); // get data from IoSerialiser
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
        ioSerialiser.put("boolean", true);
        positionAfter.add(buffer.position());
        positionBefore.add(buffer.position());
        ioSerialiser.put("boolean", false);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("byte", (byte) 42);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("short", (short) 43);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("int", 44);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("long", 45L);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("float", 1.0f);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("double", 3.0);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("string", "test");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("string", "");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.put("string", (String) null);
        positionAfter.add(buffer.position());

        WireDataFieldDescription header;
        // check primitive types
        buffer.flip();
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
        final String dataStartMarkerName = "StartMarker";
        final WireDataFieldDescription dataStartMarker = new WireDataFieldDescription(ioSerialiser, null, dataStartMarkerName.hashCode(), dataStartMarkerName, DataType.START_MARKER, -1, -1, -1);
        ioSerialiser.putStartMarker(dataStartMarker);
        positionAfter.add(buffer.position());

        // add Collection - List<E>
        final List<Integer> list = Arrays.asList(1, 2, 3);
        positionBefore.add(buffer.position());
        ioSerialiser.put("collection", list, Integer.class);
        positionAfter.add(buffer.position());

        // add Collection - Set<E>
        final Set<Integer> set = Set.of(1, 2, 3);
        positionBefore.add(buffer.position());
        ioSerialiser.put("set", set, Integer.class);
        positionAfter.add(buffer.position());

        // add Collection - Queue<E>
        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        positionBefore.add(buffer.position());
        ioSerialiser.put("queue", queue, Integer.class);
        positionAfter.add(buffer.position());

        // add Map
        final Map<Integer, String> map = new HashMap<>();
        list.forEach(item -> map.put(item, "Item#" + item.toString()));
        positionBefore.add(buffer.position());
        ioSerialiser.put("map", map, Integer.class, String.class);
        positionAfter.add(buffer.position());

        // add Enum
        positionBefore.add(buffer.position());
        ioSerialiser.put("enum", DataType.ENUM);
        positionAfter.add(buffer.position());

        // add end marker
        positionBefore.add(buffer.position());
        final String dataEndMarkerName = "EndMarker";
        final WireDataFieldDescription dataEndMarker = new WireDataFieldDescription(ioSerialiser, null, dataEndMarkerName.hashCode(), dataEndMarkerName, DataType.START_MARKER, -1, -1, -1);
        ioSerialiser.putEndMarker(dataEndMarker);
        positionAfter.add(buffer.position());

        buffer.flip();

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
        assertEquals(BinarySerialiser.PROTOCOL_NAME, headerInfo.getProducerName());
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
        assertEquals("collection", header.getFieldName(), "List<E> field name");
        assertEquals(DataType.LIST, header.getDataType(), "List<E> - type ID");
        positionAfterFieldHeader = buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertNotNull(positionAfter.peekFirst());
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getBuffer().getInt(), "dimension");
        assertEquals(3, ioSerialiser.getBuffer().getInt(), "array size");
        buffer.position(header.getDataStartPosition());
        final int readPosition = buffer.position();
        Collection<Integer> retrievedCollection = ioSerialiser.getCollection(null);
        assertNotNull(retrievedCollection, "retrieved collection not null");
        assertEquals(list, retrievedCollection);
        assertEquals(buffer.position(), header.getDataStartPosition() + header.getDataSize(), "buffer position data end");
        // check for specific List interface
        buffer.position(readPosition);
        List<Integer> retrievedList = ioSerialiser.getList(null);
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
        Collection<Integer> retrievedSet = ioSerialiser.getSet(null);
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
        Queue<Integer> retrievedQueue = ioSerialiser.getQueue(null);
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
        Map<Integer, String> retrievedMap = ioSerialiser.getMap(null);
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
        ioSerialiser.put("boolean", true);
        ioSerialiser.put("byte", (byte) 42);
        ioSerialiser.put("short", (short) 42);
        ioSerialiser.put("int", 42);
        ioSerialiser.put("long", 42L);
        ioSerialiser.put("float", 42f);
        ioSerialiser.put("double", 42);
        ioSerialiser.put("string", "string");

        ioSerialiser.put("boolean[]", new boolean[] { true }, 1);
        ioSerialiser.put("byte[]", new byte[] { (byte) 42 }, 1);
        ioSerialiser.put("short[]", new short[] { (short) 42 }, 1);
        ioSerialiser.put("int[]", new int[] { 42 }, 1);
        ioSerialiser.put("long[]", new long[] { 42L }, 1);
        ioSerialiser.put("float[]", new float[] { (float) 42 }, 1);
        ioSerialiser.put("double[]", new double[] { (double) 42 }, 1);
        ioSerialiser.put("string[]", new String[] { "string" }, 1);

        final Collection<Integer> collection = Arrays.asList(1, 2, 3);
        ioSerialiser.put("collection", collection, Integer.class); // add Collection - List<E>

        final List<Integer> list = Arrays.asList(1, 2, 3);
        ioSerialiser.put("list", list, Integer.class); // add Collection - List<E>

        final Set<Integer> set = Set.of(1, 2, 3);
        ioSerialiser.put("set", set, Integer.class); // add Collection - Set<E>

        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        ioSerialiser.put("queue", queue, Integer.class); // add Collection - Queue<E>

        final Map<Integer, String> map = new HashMap<>();
        list.forEach(item -> map.put(item, "Item#" + item.toString()));
        ioSerialiser.put("map", map, Integer.class, String.class); // add Map

        ioSerialiser.put("enum", DataType.ENUM); // add Enum

        // start nested data
        final String nestedContextName = "nested context";
        final WireDataFieldDescription nestedContextMarker = new WireDataFieldDescription(ioSerialiser, null, nestedContextName.hashCode(), nestedContextName, DataType.START_MARKER, -1, -1, -1);
        ioSerialiser.putStartMarker(nestedContextMarker); // add start marker
        ioSerialiser.put("booleanArray", new boolean[] { true }, 1);
        ioSerialiser.put("byteArray", new byte[] { (byte) 0x42 }, 1);

        ioSerialiser.putEndMarker(nestedContextMarker); // add end marker
        // end nested data

        final String dataEndMarkerName = "Life is good!";
        final WireDataFieldDescription dataEndMarker = new WireDataFieldDescription(ioSerialiser, null, dataEndMarkerName.hashCode(), dataEndMarkerName, DataType.START_MARKER, -1, -1, -1);
        ioSerialiser.putEndMarker(dataEndMarker); // add end marker

        buffer.flip();

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

        buffer.flip();

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
