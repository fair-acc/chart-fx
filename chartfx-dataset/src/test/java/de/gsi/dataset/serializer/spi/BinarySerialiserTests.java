package de.gsi.dataset.serializer.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import de.gsi.dataset.serializer.FieldDescription;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.helper.MyGenericClass;
import de.gsi.dataset.utils.AssertUtils;

/**
 *
 * @author rstein
 */
@SuppressWarnings("PMD.ExcessiveMethodLength")
public class BinarySerialiserTests {
    private static final int BUFFER_SIZE = 2000;
    private static final int ARRAY_DIM_1D = 1; // array dimension

    @DisplayName("basic primitive array writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testBasicInterfacePrimitiveArrays(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        Deque<Long> positionBefore = new LinkedList<>();
        Deque<Long> positionAfter = new LinkedList<>();

        // add primitive array types
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL_ARRAY);
        ioSerialiser.put(new boolean[] { true, false });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("byte", DataType.BYTE_ARRAY);
        ioSerialiser.put(new byte[] { (byte) 42, (byte) 42 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("short", DataType.SHORT_ARRAY);
        ioSerialiser.put(new short[] { (short) 43, (short) 43 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("int", DataType.INT_ARRAY);
        ioSerialiser.put(new int[] { 44, 44 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("long", DataType.LONG_ARRAY);
        ioSerialiser.put(new long[] { (long) 45, (long) 45 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("float", DataType.FLOAT_ARRAY);
        ioSerialiser.put(new float[] { 1.0f, 1.0f });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("double", DataType.DOUBLE_ARRAY);
        ioSerialiser.put(new double[] { 3.0, 3.0 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING_ARRAY);
        ioSerialiser.put(new String[] { "test", "test" });
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
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final boolean[] booleanArray = ioSerialiser.getBooleanArray();
        assertArrayEquals(new boolean[] { true, false }, booleanArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // byte
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("byte", header.getFieldName(), "byte field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final byte[] byteArray = ioSerialiser.getByteArray();
        assertArrayEquals(new byte[] { (byte) 42, (byte) 42 }, byteArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // short
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("short", header.getFieldName(), "short field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final short[] shortArray = ioSerialiser.getShortArray();
        assertArrayEquals(new short[] { (short) 43, (short) 43 }, shortArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // int
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("int", header.getFieldName(), "int field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final int[] intArray = ioSerialiser.getIntArray();
        assertNotNull(intArray);
        assertArrayEquals(new int[] { 44, 44 }, intArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // long
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("long", header.getFieldName(), "long field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final long[] longArray = ioSerialiser.getLongArray();
        assertNotNull(longArray);
        assertArrayEquals(new long[] { 45, 45 }, longArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // float
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("float", header.getFieldName(), "float field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final float[] floatArray = ioSerialiser.getFloatArray();
        assertNotNull(floatArray);
        assertArrayEquals(new float[] { 1.0f, 1.0f }, floatArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // double
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("double", header.getFieldName(), "double field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final double[] doubleArray = ioSerialiser.getDoubleArray();
        assertNotNull(doubleArray);
        assertArrayEquals(new double[] { 3.0, 3.0 }, doubleArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // string
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        assertEquals(positionAfterFieldHeader, header.getDataStartOffset(), "data start position");
        assertEquals(positionAfter.peekFirst(), header.getDataStartOffset() + header.getDataSize(), "data end skip address");
        dims = ioSerialiser.getBuffer().getArraySizeDescriptor();
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        buffer.position(header.getDataStartOffset()); // return to original data start
        final String[] stringArray = ioSerialiser.getStringArray();
        assertNotNull(stringArray);
        assertArrayEquals(new String[] { "test", "test" }, stringArray);
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @DisplayName("basic primitive writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testBasicInterfacePrimitives(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        Deque<Long> positionBefore = new LinkedList<>();
        Deque<Long> positionAfter = new LinkedList<>();

        // add primitive types
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.put(true);
        positionAfter.add(buffer.position());
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.put(false);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("byte", DataType.BYTE);
        ioSerialiser.put((byte) 42);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("short", DataType.SHORT);
        ioSerialiser.put((short) 43);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("int", DataType.INT);
        ioSerialiser.put(44);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("long", DataType.LONG);
        ioSerialiser.put((long) 45);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("float", DataType.FLOAT);
        ioSerialiser.put(1.0f);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("double", DataType.DOUBLE);
        ioSerialiser.put(3.0);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.put("test");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.put("");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.put((String) null);
        positionAfter.add(buffer.position());

        WireDataFieldDescription header;
        // check primitive types
        buffer.reset();
        assertEquals(0, buffer.position(), "initial buffer position");

        // boolean
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        assertTrue(ioSerialiser.getBoolean(), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        assertFalse(ioSerialiser.getBoolean(), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // byte
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("byte", header.getFieldName(), "boolean field name retrieval");
        assertEquals(42, ioSerialiser.getByte(), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // short
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("short", header.getFieldName(), "short field name retrieval");
        assertEquals(43, ioSerialiser.getShort(), "short retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // int
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("int", header.getFieldName(), "int field name retrieval");
        assertEquals(44, ioSerialiser.getInteger(), "int retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // long
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("long", header.getFieldName(), "long field name retrieval");
        assertEquals(45, ioSerialiser.getLong(), "long retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // float
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("float", header.getFieldName(), "float field name retrieval");
        assertEquals(1.0f, ioSerialiser.getFloat(), "float retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // double
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("double", header.getFieldName(), "double field name retrieval");
        assertEquals(3.0, ioSerialiser.getDouble(), "double retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // string
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("test", ioSerialiser.getString(), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("", ioSerialiser.getString(), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("", ioSerialiser.getString(), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @DisplayName("basic tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testHeaderAndSpecialItems(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        Deque<Long> positionBefore = new LinkedList<>();
        Deque<Long> positionAfter = new LinkedList<>();

        // add start marker
        positionBefore.add(buffer.position());
        ioSerialiser.putStartMarker("StartMarker");
        positionAfter.add(buffer.position());

        // add header info
        positionBefore.add(buffer.position());
        ioSerialiser.putHeaderInfo();
        positionAfter.add(buffer.position());

        // add Collection - List<E>
        final List<Integer> list = Arrays.asList(1, 2, 3);
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("collection", DataType.COLLECTION);
        ioSerialiser.put(list);
        positionAfter.add(buffer.position());

        // add Collection - Set<E>
        final Set<Integer> set = Set.of(1, 2, 3);
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("set", DataType.SET);
        ioSerialiser.put(set);
        positionAfter.add(buffer.position());

        // add Collection - Queue<E>
        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("queue", DataType.QUEUE);
        ioSerialiser.put(queue);
        positionAfter.add(buffer.position());

        // add Map
        final Map<Integer, String> map = new HashMap<>();
        list.forEach(item -> map.put(item, "Item#" + item.toString()));
        positionBefore.add(buffer.position());
        ioSerialiser.putFieldHeader("map", DataType.MAP);
        ioSerialiser.put(map);
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

        // start marker
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("StartMarker", header.getFieldName(), "StartMarker type retrieval");
        assertEquals(BinarySerialiser.getDataType(DataType.START_MARKER), buffer.getByte(), "StartMarker retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // header info
        assertEquals(positionBefore.removeFirst(), buffer.position());
        ProtocolInfo headerInfo = ioSerialiser.checkHeaderInfo();
        assertEquals(ioSerialiser.headerInfo, headerInfo);
        assertNotNull(ioSerialiser.headerInfo.toString());
        assertEquals(ioSerialiser.headerInfo.hashCode(), headerInfo.hashCode());
        assertNotEquals(headerInfo, new Object()); // silly comparison for coverage reasons
        assertNotNull(headerInfo);
        assertEquals(BinarySerialiser.class.getCanonicalName(), headerInfo.getProducerName());
        assertEquals(BinarySerialiser.VERSION_MAJOR, headerInfo.getVersionMajor());
        assertEquals(BinarySerialiser.VERSION_MINOR, headerInfo.getVersionMinor());
        assertEquals(BinarySerialiser.VERSION_MICRO, headerInfo.getVersionMicro());
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - List
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("collection", header.getFieldName(), "Collection<E> field name");
        assertEquals(DataType.COLLECTION, header.getDataType(), "Collection<E> - type ID");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getInteger(), "dimension");
        assertEquals(3, ioSerialiser.getInteger(), "array size");
        buffer.position(header.getDataStartOffset());
        final long readPosition = buffer.position();
        Collection<Integer> retrievedCollection = ioSerialiser.getCollection(null);
        assertNotNull(retrievedCollection, "retrieved collection not null");
        assertEquals(list, retrievedCollection);
        assertEquals(buffer.position(), header.getDataStartOffset() + header.getDataSize(), "buffer position data end");
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
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getInteger(), "dimension");
        assertEquals(3, ioSerialiser.getInteger(), "array size");
        buffer.position(header.getDataStartOffset());
        Collection<Integer> retrievedSet = ioSerialiser.getSet(null);
        assertNotNull(retrievedSet, "retrieved set not null");
        assertEquals(set, retrievedSet);
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - Queue
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("queue", header.getFieldName(), "Queue<E> field name");
        assertEquals(DataType.QUEUE, header.getDataType(), "Queue<E> - type ID");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getInteger(), "dimension");
        assertEquals(3, ioSerialiser.getInteger(), "array size");
        buffer.position(header.getDataStartOffset());
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
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = header.getDataSize(); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(ARRAY_DIM_1D, ioSerialiser.getInteger(), "dimension");
        assertEquals(3, ioSerialiser.getInteger(), "array size");
        buffer.position(header.getDataStartOffset());
        Map<Integer, String> retrievedMap = ioSerialiser.getMap(null);
        assertNotNull(retrievedMap, "retrieved set not null");
        assertEquals(map, retrievedMap); // N.B. no direct comparison possible -> only partial Queue interface overlapp
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // enum
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("enum", header.getFieldName(), "enum type retrieval");
        buffer.position(header.getDataStartOffset());
        assertDoesNotThrow(ioSerialiser::getEnumTypeList); //skips enum info
        buffer.position(header.getDataStartOffset());
        assertEquals(DataType.ENUM, ioSerialiser.getEnum(DataType.OTHER), "enum retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // end marker
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = ioSerialiser.getFieldHeader();
        assertEquals("EndMarker", header.getFieldName(), "EndMarker type retrieval");
        assertEquals(BinarySerialiser.getDataType(DataType.END_MARKER), buffer.getByte(), "EndMarker retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @Test
    public void testIdentityGenericObject() {
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
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testIndexOutOfBoundsGuards(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(0);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        // check out-of-bound handling for primitives
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getBoolean);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getByte);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getShort);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getInteger);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getLong);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getFloat);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getDouble);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getString);

        // check out-of-bound handling for primitive arrays
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getBooleanArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getByteArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getShortArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getIntArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getLongArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getFloatArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getDoubleArray);
        assertThrows(IndexOutOfBoundsException.class, ioSerialiser::getStringArray);
    }

    @DisplayName("basic primitive array writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testParseIoStream(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        ioSerialiser.putHeaderInfo(); // add header info

        // add some primitives
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.put(true);
        ioSerialiser.putFieldHeader("byte", DataType.BYTE);
        ioSerialiser.put((byte) 42);
        ioSerialiser.putFieldHeader("short", DataType.SHORT);
        ioSerialiser.put((short) 42);
        ioSerialiser.putFieldHeader("int", DataType.INT);
        ioSerialiser.put(42);
        ioSerialiser.putFieldHeader("long", DataType.LONG);
        ioSerialiser.put(42L);
        ioSerialiser.putFieldHeader("float", DataType.FLOAT);
        ioSerialiser.put((float) 42);
        ioSerialiser.putFieldHeader("double", DataType.DOUBLE);
        ioSerialiser.put((double) 42);
        ioSerialiser.putFieldHeader("string", DataType.STRING);
        ioSerialiser.put("string");

        ioSerialiser.putFieldHeader("boolean[]", DataType.BOOL_ARRAY);
        ioSerialiser.put(new boolean[] { true });
        ioSerialiser.putFieldHeader("byte[]", DataType.BYTE_ARRAY);
        ioSerialiser.put(new byte[] { (byte) 42 });
        ioSerialiser.putFieldHeader("short[]", DataType.SHORT_ARRAY);
        ioSerialiser.put(new short[] { (short) 42 });
        ioSerialiser.putFieldHeader("int[]", DataType.INT_ARRAY);
        ioSerialiser.put(new int[] { 42 });
        ioSerialiser.putFieldHeader("long[]", DataType.LONG_ARRAY);
        ioSerialiser.put(new long[] { 42L });
        ioSerialiser.putFieldHeader("float[]", DataType.FLOAT_ARRAY);
        ioSerialiser.put(new float[] { (float) 42 });
        ioSerialiser.putFieldHeader("double[]", DataType.DOUBLE_ARRAY);
        ioSerialiser.put(new double[] { (double) 42 });
        ioSerialiser.putFieldHeader("string[]", DataType.STRING_ARRAY);
        ioSerialiser.put(new String[] { "string" });

        final Collection<Integer> collection = Arrays.asList(1, 2, 3);
        ioSerialiser.putFieldHeader("collection", DataType.COLLECTION);
        ioSerialiser.put(collection); // add Collection - List<E>

        final List<Integer> list = Arrays.asList(1, 2, 3);
        ioSerialiser.putFieldHeader("list", DataType.LIST);
        ioSerialiser.put(list); // add Collection - List<E>

        final Set<Integer> set = Set.of(1, 2, 3);
        ioSerialiser.putFieldHeader("set", DataType.SET);
        ioSerialiser.put(set); // add Collection - Set<E>

        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        ioSerialiser.putFieldHeader("queue", DataType.QUEUE);
        ioSerialiser.put(queue); // add Collection - Queue<E>

        final Map<Integer, String> map = new HashMap<>();
        list.forEach(item -> map.put(item, "Item#" + item.toString()));
        ioSerialiser.putFieldHeader("map", DataType.MAP);
        ioSerialiser.put(map); // add Map

        ioSerialiser.putFieldHeader("enum", DataType.ENUM);
        ioSerialiser.put(DataType.ENUM); // add Enum

        // start nested data
        ioSerialiser.putStartMarker("nested context"); // add end marker
        ioSerialiser.putFieldHeader("boolean", DataType.BOOL_ARRAY);
        ioSerialiser.put(new boolean[] { true });
        ioSerialiser.putFieldHeader("byte", DataType.BYTE_ARRAY);
        ioSerialiser.put(new byte[] { (byte) 0x42 });

        ioSerialiser.putEndMarker("nested context"); // add end marker
        // end nested data

        ioSerialiser.putEndMarker("Life is good!"); // add end marker

        buffer.reset();

        // and read back streamed items
        final WireDataFieldDescription objectRoot = ioSerialiser.parseIoStream();
        assertNotNull(objectRoot);

        buffer.reset();
        // check agnostic parsing of buffer
        final ProtocolInfo bufferHeader2 = ioSerialiser.checkHeaderInfo();
        assertNotNull(bufferHeader2);
        for (FieldDescription field : objectRoot.getChildren()) {
            buffer.position(field.getDataStartOffset());
            ioSerialiser.swallowRest(field);
        }
    }

    @DisplayName("test getDoubleArray([boolean[], byte[], ..., String[]) helper method")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testGetDoubleArrayHelper(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        putGenericTestArrays(ioSerialiser);

        buffer.reset();

        // test conversion to double array
        assertThrows(IllegalArgumentException.class, () -> ioSerialiser.getDoubleArray(DataType.OTHER));
        assertArrayEquals(new double[] { 1.0, 0.0, 1.0 }, ioSerialiser.getDoubleArray(DataType.BOOL_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.BYTE_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.CHAR_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.SHORT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.INT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.LONG_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.FLOAT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.DOUBLE_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getDoubleArray(DataType.STRING_ARRAY));
    }

    @DisplayName("test getGenericArrayAsBoxedPrimitive(...) helper method")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testgetGenericArrayAsBoxedPrimitiveHelper(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        putGenericTestArrays(ioSerialiser);

        buffer.reset();

        // test conversion to double array
        //TODO: follow-up w.r.t. serialiser lib: assertThrows(IllegalArgumentException.class, () -> ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.OTHER));

        assertArrayEquals(new Boolean[] { true, false, true }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.BOOL));
        assertArrayEquals(new Byte[] { (byte) 1.0, (byte) 0.0, (byte) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.BYTE));
        assertArrayEquals(new Character[] { (char) 1.0, (char) 0.0, (char) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.CHAR));
        assertArrayEquals(new Short[] { (short) 1.0, (short) 0.0, (short) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.SHORT));
        assertArrayEquals(new Integer[] { (int) 1.0, (int) 0.0, (int) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.INT));
        assertArrayEquals(new Long[] { (long) 1.0, (long) 0.0, (long) 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.LONG));
        assertArrayEquals(new Float[] { 1.0f, 0.0f, 2.0f }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.FLOAT));
        assertArrayEquals(new Double[] { 1.0, 0.0, 2.0 }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.DOUBLE));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, ioSerialiser.getGenericArrayAsBoxedPrimitive(DataType.STRING));
    }

    private void putGenericTestArrays(final BinarySerialiser ioSerialiser) {
        //TODO: follow-up w.r.t. serialiser lib: assertThrows(IllegalArgumentException.class, () -> ioSerialiser.putGenericArrayAsPrimitive(DataType.OTHER, new Object[] { new Object() }, 1));

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

    @Test
    public void testMisc() {
        final BinarySerialiser ioSerialiser = new BinarySerialiser(new FastByteBuffer(1000)); // TODO: generalise to IoBuffer
        final int bufferIncrements = ioSerialiser.getBufferIncrements();
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        ioSerialiser.setBufferIncrements(bufferIncrements + 1);
        assertEquals(bufferIncrements + 1, ioSerialiser.getBufferIncrements());

        assertDoesNotThrow(() -> BinarySerialiser.getNumberOfElements(new int[] { 1, 2, 3 }));
        assertEquals(1000, BinarySerialiser.getNumberOfElements(new int[] { 10, 10, 10 }));
    }
}
