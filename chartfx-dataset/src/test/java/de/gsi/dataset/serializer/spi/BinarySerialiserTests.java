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
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser.HeaderInfo;
import de.gsi.dataset.serializer.spi.helper.MyGenericClass;
import de.gsi.dataset.utils.AssertUtils;

/**
 *
 * @author rstein
 */
@SuppressWarnings("PMD.ExcessiveMethodLength")
public class BinarySerialiserTests {
    private static final int BUFFER_SIZE = 1000;
    private static final int ARRAY_DIM_1D = 1; // array dimension

    @DisplayName("basic primitive array writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testBasicInterfacePrimitiveArrays(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        Deque<Long> positionBefore = new LinkedList<>();
        Deque<Long> positionAfter = new LinkedList<>();

        // add primitive array types
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "boolean", new boolean[] { true, false });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "byte", new byte[] { (byte) 42, (byte) 42 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "short", new short[] { (short) 43, (short) 43 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "int", new int[] { 44, 44 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "long", new long[] { (long) 45, (long) 45 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "float", new float[] { 1.0f, 1.0f });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "double", new double[] { 3.0, 3.0 });
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "string", new String[] { "test", "test" });
        positionAfter.add(buffer.position());

        FieldHeader header;
        int positionAfterFieldHeader;
        int skipNBytes;
        int[] dims;
        // check primitive types
        buffer.reset();
        assertEquals(0, buffer.position(), "initial buffer position");

        // boolean
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final boolean[] booleanArray = BinarySerialiser.getBooleanArray(buffer);
        assertArrayEquals(new boolean[] { true, false }, booleanArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // byte
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("byte", header.getFieldName(), "byte field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final byte[] byteArray = BinarySerialiser.getByteArray(buffer);
        assertArrayEquals(new byte[] { (byte) 42, (byte) 42 }, byteArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // short
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("short", header.getFieldName(), "short field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final short[] shortArray = BinarySerialiser.getShortArray(buffer);
        assertArrayEquals(new short[] { (short) 43, (short) 43 }, shortArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // int
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("int", header.getFieldName(), "int field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final int[] intArray = BinarySerialiser.getIntArray(buffer);
        assertNotNull(intArray);
        assertArrayEquals(new int[] { 44, 44 }, intArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // long
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("long", header.getFieldName(), "long field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final long[] longArray = BinarySerialiser.getLongArray(buffer);
        assertNotNull(longArray);
        assertArrayEquals(new long[] { 45, 45 }, longArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // float
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("float", header.getFieldName(), "float field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final float[] floatArray = BinarySerialiser.getFloatArray(buffer);
        assertNotNull(floatArray);
        assertArrayEquals(new float[] { 1.0f, 1.0f }, floatArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // double
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("double", header.getFieldName(), "double field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final double[] doubleArray = BinarySerialiser.getDoubleArray(buffer);
        assertNotNull(doubleArray);
        assertArrayEquals(new double[] { 3.0, 3.0 }, doubleArray);
        assertEquals(skipNBytes, buffer.position() - positionAfterFieldHeader, "actual numer of bytes skipped");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // string
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        dims = BinarySerialiser.getArrayDimensions(buffer);
        assertEquals(ARRAY_DIM_1D, dims.length, "dimension");
        assertEquals(2, dims[0], "array size");
        final String[] stringArray = BinarySerialiser.getStringArray(buffer);
        assertNotNull(stringArray);
        assertArrayEquals(new String[] { "test", "test" }, stringArray);
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

        Deque<Long> positionBefore = new LinkedList<>();
        Deque<Long> positionAfter = new LinkedList<>();

        // add primitive types
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "boolean", true);
        positionAfter.add(buffer.position());
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "boolean", false);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "byte", (byte) 42);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "short", (short) 43);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "int", 44);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "long", (long) 45);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "float", 1.0f);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "double", 3.0);
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "string", "test");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "string", "");
        positionAfter.add(buffer.position());

        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "string", (String) null);
        positionAfter.add(buffer.position());

        FieldHeader header;
        // check primitive types
        buffer.reset();
        assertEquals(0, buffer.position(), "initial buffer position");

        // boolean
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        assertTrue(BinarySerialiser.getBoolean(buffer), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("boolean", header.getFieldName(), "boolean field name retrieval");
        assertFalse(BinarySerialiser.getBoolean(buffer), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // byte
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("byte", header.getFieldName(), "boolean field name retrieval");
        assertEquals(42, BinarySerialiser.getByte(buffer), "byte retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // short
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("short", header.getFieldName(), "short field name retrieval");
        assertEquals(43, BinarySerialiser.getShort(buffer), "short retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // int
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("int", header.getFieldName(), "int field name retrieval");
        assertEquals(44, BinarySerialiser.getInteger(buffer), "int retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // long
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("long", header.getFieldName(), "long field name retrieval");
        assertEquals(45, BinarySerialiser.getLong(buffer), "long retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // float
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("float", header.getFieldName(), "float field name retrieval");
        assertEquals(1.0f, BinarySerialiser.getFloat(buffer), "float retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // double
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("double", header.getFieldName(), "double field name retrieval");
        assertEquals(3.0, BinarySerialiser.getDouble(buffer), "double retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // string
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("test", BinarySerialiser.getString(buffer), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("", BinarySerialiser.getString(buffer), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("string", header.getFieldName(), "string field name retrieval");
        assertEquals("", BinarySerialiser.getString(buffer), "string retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());
    }

    @DisplayName("basic tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testHeaderAndSpecialItems(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        Deque<Long> positionBefore = new LinkedList<>();
        Deque<Long> positionAfter = new LinkedList<>();

        // add start marker
        positionBefore.add(buffer.position());
        BinarySerialiser.putStartMarker(buffer, "StartMarker");
        positionAfter.add(buffer.position());

        // add header info
        positionBefore.add(buffer.position());
        BinarySerialiser.putHeaderInfo(buffer);
        positionAfter.add(buffer.position());

        // add Collection - List<E>
        final List<Integer> list = Arrays.asList(1, 2, 3);
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "collection", list);
        positionAfter.add(buffer.position());

        // add Collection - Set<E>
        final Set<Integer> set = Set.of(1, 2, 3);
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "set", set);
        positionAfter.add(buffer.position());

        // add Collection - Queue<E>
        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "queue", queue);
        positionAfter.add(buffer.position());

        // add Map
        final Map<Integer, String> map = new HashMap<>();
        list.stream().forEach(item -> map.put(item, "Item#" + item.toString()));
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "map", map);
        positionAfter.add(buffer.position());

        // add Enum
        positionBefore.add(buffer.position());
        BinarySerialiser.put(buffer, "enum", DataType.ENUM);
        positionAfter.add(buffer.position());

        // add end marker
        positionBefore.add(buffer.position());
        BinarySerialiser.putEndMarker(buffer, "EndMarker");
        positionAfter.add(buffer.position());

        buffer.reset();

        FieldHeader header;
        int positionAfterFieldHeader;
        int skipNBytes;
        // check types
        assertEquals(0, buffer.position(), "initial buffer position");

        // start marker
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("StartMarker", header.getFieldName(), "StartMarker type retrieval");
        assertEquals(DataType.START_MARKER.getAsByte(), buffer.getByte(), "StartMarker retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // header info
        assertEquals(positionBefore.removeFirst(), buffer.position());
        HeaderInfo headerInfo = BinarySerialiser.checkHeaderInfo(buffer);
        assertEquals(BinarySerialiser.THIS_HEADER, headerInfo);
        assertNotNull(BinarySerialiser.THIS_HEADER.toString());
        assertEquals(BinarySerialiser.THIS_HEADER.hashCode(), headerInfo.hashCode());
        assertFalse(headerInfo.equals(new Object())); // silly comparison for coverage reasons
        assertNotNull(headerInfo);
        assertEquals(BinarySerialiser.class.getCanonicalName(), headerInfo.getProducerName());
        assertEquals(BinarySerialiser.VERSION_MAJOR, headerInfo.getVersionMajor());
        assertEquals(BinarySerialiser.VERSION_MINOR, headerInfo.getVersionMinor());
        assertEquals(BinarySerialiser.VERSION_MICRO, headerInfo.getVersionMicro());
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - List
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("collection", header.getFieldName(), "Collection<E> field name");
        assertEquals(DataType.LIST, header.getDataType(), "Collection<E> - type ID");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(1, header.getDataDimension(), "List Dimension");
        assertEquals(3, header.getDataDimensions()[0], "List size");
        assertEquals(ARRAY_DIM_1D, BinarySerialiser.getInteger(buffer), "dimension");
        assertEquals(3, BinarySerialiser.getInteger(buffer), "array size");
        final long readPosition = buffer.position();
        Collection<Integer> retrievedCollection = BinarySerialiser.getCollection(buffer, (Collection<Integer>) null);
        assertNotNull(retrievedCollection, "retrieved collection not null");
        assertEquals(list, retrievedCollection);
        // check for specific List interface
        buffer.position(readPosition);
        List<Integer> retrievedList = BinarySerialiser.getList(buffer, (List<Integer>) null);
        assertNotNull(retrievedList, "retrieved collection List not null");
        assertEquals(list, retrievedList);
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - Set
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("set", header.getFieldName(), "Set<E> field name");
        assertEquals(DataType.SET, header.getDataType(), "Set<E> - type ID");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(1, header.getDataDimension(), "Set<E> Dimension");
        assertEquals(3, header.getDataDimensions()[0], "Set<E> size");
        assertEquals(ARRAY_DIM_1D, BinarySerialiser.getInteger(buffer), "dimension");
        assertEquals(3, BinarySerialiser.getInteger(buffer), "array size");
        Collection<Integer> retrievedSet = BinarySerialiser.getSet(buffer, (Set<Integer>) null);
        assertNotNull(retrievedSet, "retrieved set not null");
        assertEquals(set, retrievedSet);
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // Collections - Queue
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("queue", header.getFieldName(), "Queue<E> field name");
        assertEquals(DataType.QUEUE, header.getDataType(), "Queue<E> - type ID");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(1, header.getDataDimension(), "Queue<E> Dimension");
        assertEquals(3, header.getDataDimensions()[0], "Queue<E> size");
        assertEquals(ARRAY_DIM_1D, BinarySerialiser.getInteger(buffer), "dimension");
        assertEquals(3, BinarySerialiser.getInteger(buffer), "array size");
        Queue<Integer> retrievedQueue = BinarySerialiser.getQueue(buffer, (Queue<Integer>) null);
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
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("map", header.getFieldName(), "Map<K,V> field name");
        assertEquals(DataType.MAP, header.getDataType(), "Map<K,V> - type ID");
        positionAfterFieldHeader = (int) buffer.position(); // actual buffer position after having read the field header
        skipNBytes = BinarySerialiser.getInteger(buffer); // number of bytes to be skipped till end of this data chunk
        assertEquals(positionAfter.peekFirst() - positionAfterFieldHeader, skipNBytes, "buffer skip address");
        assertFalse(header.getDataType().isScalar());
        assertEquals(1, header.getDataDimension(), "Map<K,V> Dimension");
        assertEquals(3, header.getDataDimensions()[0], "Map<K,V> size");
        assertEquals(ARRAY_DIM_1D, BinarySerialiser.getInteger(buffer), "dimension");
        assertEquals(3, BinarySerialiser.getInteger(buffer), "array size");
        Map<Integer, String> retrievedMap = BinarySerialiser.getMap(buffer, (Map<Integer, String>) null);
        assertNotNull(retrievedMap, "retrieved set not null");
        assertEquals(map, retrievedMap); // N.B. no direct comparison possible -> only partial Queue interface overlapp
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // enum
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("enum", header.getFieldName(), "enum type retrieval");
        buffer.position(header.getDataBufferPosition());
        assertDoesNotThrow(() -> BinarySerialiser.getEnumTypeList(buffer)); //skips enum info
        buffer.position(header.getDataBufferPosition());
        assertEquals(DataType.ENUM, BinarySerialiser.getEnum(buffer, DataType.OTHER), "enum retrieval");
        assertEquals(positionAfter.removeFirst(), buffer.position());

        // end marker
        assertEquals(positionBefore.removeFirst(), buffer.position());
        header = BinarySerialiser.getFieldHeader(buffer);
        assertEquals("EndMarker", header.getFieldName(), "EndMarker type retrieval");
        assertEquals(DataType.END_MARKER.getAsByte(), buffer.getByte(), "EndMarker retrieval");
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

        // check out-of-bound handling for primitives
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getBoolean(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getByte(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getShort(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getInteger(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getLong(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getFloat(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getDouble(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getString(buffer));

        // check out-of-bound handling for primitive arrays
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getBooleanArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getByteArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getShortArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getIntArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getLongArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getFloatArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getDoubleArray(buffer));
        assertThrows(IndexOutOfBoundsException.class, () -> BinarySerialiser.getStringArray(buffer));
    }

    @DisplayName("basic primitive array writer tests")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testParseIoStream(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once

        BinarySerialiser.putHeaderInfo(buffer); // add header info

        // add some primitives
        BinarySerialiser.put(buffer, "boolean", true);
        BinarySerialiser.put(buffer, "byte", (byte) 42);
        BinarySerialiser.put(buffer, "short", (short) 42);
        BinarySerialiser.put(buffer, "int", (int) 42);
        BinarySerialiser.put(buffer, "long", (long) 42);
        BinarySerialiser.put(buffer, "float", (float) 42);
        BinarySerialiser.put(buffer, "double", (double) 42);
        BinarySerialiser.put(buffer, "string", "string");

        BinarySerialiser.put(buffer, "boolean[]", new boolean[] { true });
        BinarySerialiser.put(buffer, "byte[]", new byte[] { (byte) 42 });
        BinarySerialiser.put(buffer, "short[]", new short[] { (short) 42 });
        BinarySerialiser.put(buffer, "int[]", new int[] { (int) 42 });
        BinarySerialiser.put(buffer, "long[]", new long[] { (long) 42 });
        BinarySerialiser.put(buffer, "float[]", new float[] { (float) 42 });
        BinarySerialiser.put(buffer, "double[]", new double[] { (double) 42 });
        BinarySerialiser.put(buffer, "string[]", new String[] { "string" });

        final Collection<Integer> collection = Arrays.asList(1, 2, 3);
        BinarySerialiser.put(buffer, "collection", collection); // add Collection - List<E>

        final List<Integer> list = Arrays.asList(1, 2, 3);
        BinarySerialiser.put(buffer, "list", list); // add Collection - List<E>

        final Set<Integer> set = Set.of(1, 2, 3);
        BinarySerialiser.put(buffer, "set", set); // add Collection - Set<E>

        final Queue<Integer> queue = new LinkedList<>(Arrays.asList(1, 2, 3));
        BinarySerialiser.put(buffer, "queue", queue); // add Collection - Queue<E>

        final Map<Integer, String> map = new HashMap<>();
        list.stream().forEach(item -> map.put(item, "Item#" + item.toString()));
        BinarySerialiser.put(buffer, "map", map); // add Map

        BinarySerialiser.put(buffer, "enum", DataType.ENUM); // add Enum

        // start nested data
        BinarySerialiser.putStartMarker(buffer, "nested context"); // add end marker
        BinarySerialiser.put(buffer, "boolean", new boolean[] { true });
        BinarySerialiser.put(buffer, "byte", new byte[] { (byte) 0x42 });

        BinarySerialiser.putEndMarker(buffer, "nested context"); // add end marker
        // end nested data

        BinarySerialiser.putEndMarker(buffer, "Life is good!"); // add end marker

        buffer.reset();

        // and read back streamed items
        final HeaderInfo bufferHeader = BinarySerialiser.checkHeaderInfo(buffer);
        assertNotNull(bufferHeader);
        final FieldHeader objectRoot = BinarySerialiser.parseIoStream(buffer);
        assertNotNull(objectRoot);

        buffer.reset();
        // check agnostic parsing of buffer
        final HeaderInfo bufferHeader2 = BinarySerialiser.checkHeaderInfo(buffer);
        assertNotNull(bufferHeader2);
        for (FieldHeader field : objectRoot.getChildren()) {
            buffer.position(field.getDataBufferPosition());
            BinarySerialiser.swallowRest(buffer, field);
        }
    }

    @Test
    public void testGenericHelper() {
        assertArrayEquals(new double[] { 1.0, 0.0, 1.0 }, BinarySerialiser.toDoubles(new boolean[] { true, false, true }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new byte[] { (byte) 1, (byte) 0, (byte) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new char[] { (char) 1, (char) 0, (char) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new short[] { (short) 1, (short) 0, (short) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new int[] { (int) 1, (int) 0, (int) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new long[] { (long) 1, (long) 0, (long) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new float[] { (float) 1, (float) 0, (float) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.toDoubles(new String[] { "1.0", "0.0", "2.0" }));
    }

    @DisplayName("test getDoubleArray([boolean[], byte[], ..., String[]) helper method")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testGetDoubleArrayHelper(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once

        putGenericTestArrays(buffer);

        buffer.reset();

        // test conversion to double array
        assertThrows(IllegalArgumentException.class, () -> BinarySerialiser.getDoubleArray(buffer, DataType.OTHER));
        assertArrayEquals(new double[] { 1.0, 0.0, 1.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.BOOL_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.BYTE_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.CHAR_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.SHORT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.INT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.LONG_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.FLOAT_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.DOUBLE_ARRAY));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getDoubleArray(buffer, DataType.STRING_ARRAY));
    }

    @DisplayName("test getGenericArrayAsBoxedPrimitive(...) helper method")
    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void testgetGenericArrayAsBoxedPrimitiveHelper(final Class<? extends IoBuffer> bufferClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(2 * BUFFER_SIZE); // a bit larger buffer since we test more cases at once

        putGenericTestArrays(buffer);

        buffer.reset();

        // test conversion to double array
        assertThrows(IllegalArgumentException.class, () -> BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.OTHER));

        assertArrayEquals(new Boolean[] { true, false, true }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.BOOL));
        assertArrayEquals(new Byte[] { (byte) 1.0, (byte) 0.0, (byte) 2.0 }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.BYTE));
        assertArrayEquals(new Character[] { (char) 1.0, (char) 0.0, (char) 2.0 }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.CHAR));
        assertArrayEquals(new Short[] { (short) 1.0, (short) 0.0, (short) 2.0 }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.SHORT));
        assertArrayEquals(new Integer[] { (int) 1.0, (int) 0.0, (int) 2.0 }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.INT));
        assertArrayEquals(new Long[] { (long) 1.0, (long) 0.0, (long) 2.0 }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.LONG));
        assertArrayEquals(new Float[] { 1.0f, 0.0f, 2.0f }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.FLOAT));
        assertArrayEquals(new Double[] { 1.0, 0.0, 2.0 }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.DOUBLE));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, BinarySerialiser.getGenericArrayAsBoxedPrimitive(buffer, DataType.STRING));
    }

    private void putGenericTestArrays(final IoBuffer buffer) {
        assertThrows(IllegalArgumentException.class, () -> BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.OTHER, new Object[] { new Object() }, 1));

        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.BOOL, new Boolean[] { true, false, true }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.BYTE, new Byte[] { (byte) 1, (byte) 0, (byte) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.CHAR, new Character[] { (char) 1, (char) 0, (char) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.SHORT, new Short[] { (short) 1, (short) 0, (short) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.INT, new Integer[] { (int) 1, (int) 0, (int) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.LONG, new Long[] { (long) 1, (long) 0, (long) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.FLOAT, new Float[] { (float) 1, (float) 0, (float) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.DOUBLE, new Double[] { (double) 1, (double) 0, (double) 2 }, 3);
        BinarySerialiser.putGenericArrayAsPrimitive(buffer, DataType.STRING, new String[] { "1.0", "0.0", "2.0" }, 3);
    }

    @Test
    public void testMisc() {
        final int bufferIncrements = BinarySerialiser.getBufferIncrements();
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        BinarySerialiser.setBufferIncrements(bufferIncrements + 1);
        assertEquals(bufferIncrements + 1, BinarySerialiser.getBufferIncrements());

        assertDoesNotThrow(() -> BinarySerialiser.getNumberOfElements(new int[] { 1, 2, 3 }));
        assertEquals(1000, BinarySerialiser.getNumberOfElements(new int[] { 10, 10, 10 }));
    }
}
