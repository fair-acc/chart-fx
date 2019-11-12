package de.gsi.dataset.serializer.spi;

import static de.gsi.dataset.serializer.spi.FastByteBuffer.SIZE_OF_BYTE;
import static de.gsi.dataset.serializer.spi.FastByteBuffer.SIZE_OF_INT;

import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Generic binary serialiser aimed at efficiently transferring data between server/client and in particular between
 * Java/C++/web-based programs.
 *
 * @author rstein
 */
public class BinarySerialiser { // NOPMD - omen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiser.class);
    private static final String READ_POSITION_AT_BUFFER_END = "read position at buffer end";
    public static final byte VERSION_MAJOR = 1;
    public static final byte VERSION_MINOR = 0;
    public static final byte VERSION_MICRO = 0;
    protected static final BinarySerialiser SELF = new BinarySerialiser();
    protected static HeaderInfo headerThis = SELF.new HeaderInfo(BinarySerialiser.class.getSimpleName(), VERSION_MAJOR,
            VERSION_MINOR, VERSION_MICRO);
    private static int bufferIncrements;

    protected BinarySerialiser() {
        super();
    }

    public static void adjustDataByteSizeBlock(final IoBuffer buffer, final long sizeMarkerStart) {
        final long sizeMarkerEnd = buffer.position();

        // go back and re-adjust the actual size info
        buffer.position(sizeMarkerStart);
        final long expectedNumberOfBytes = sizeMarkerEnd - sizeMarkerStart;
        buffer.putInt((int) expectedNumberOfBytes); // write actual byte size

        // go back to the new write position
        buffer.position(sizeMarkerEnd);
    }

    public static HeaderInfo checkHeaderInfo(final IoBuffer readBuffer) {
        AssertUtils.notNull("readBuffer", readBuffer);
        final FieldHeader headerStartField = BinarySerialiser.getFieldHeader(readBuffer);
        final byte startMarker = readBuffer.getByte();
        if (startMarker != DataType.START_MARKER.getAsByte()) {
            // TODO: replace with (new to be written) custom SerializerFormatException(..)
            throw new InvalidParameterException(
                    "header does not start with a START_MARKER('" + DataType.START_MARKER.getAsByte()
                            + "') DataType but " + startMarker + " fieldName = " + headerStartField.getFieldName());
        }
        readBuffer.getString(); // should read "#file producer : "
        // -- but not explicitly checked
        final String producer = readBuffer.getString();
        readBuffer.getString(); // not explicitly checked
        final byte major = readBuffer.getByte();
        final byte minor = readBuffer.getByte();
        final byte micro = readBuffer.getByte();

        final HeaderInfo header = SELF.new HeaderInfo(producer, major, minor, micro);

        if (!header.isCompatible()) {
            final String msg = String.format("byte buffer version incompatible: reveived '%s' vs. this '%s'",
                    header.toString(), headerThis.toString());
            throw new IllegalStateException(msg);
        }
        return header;
    }

    public static int[] getArrayDimensions(final IoBuffer readBuffer) {
        final int arrayDims = readBuffer.getInt(); // array dimensions
        final int[] dims = new int[arrayDims];
        for (int i = 0; i < arrayDims; ++i) {
            dims[i] = readBuffer.getInt();
        }
        return dims;
    }

    //
    // -- WRITE OPERATIONS -------------------------------------------
    //

    public static boolean getBoolean(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getBoolean();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static boolean[] getBooleanArray(final IoBuffer readBuffer) {
        return readBuffer.getBooleanArray();
    }

    public static int getBufferIncrements() {
        return bufferIncrements;
    }

    public static byte getByte(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getByte();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static byte[] getByteArray(final IoBuffer readBuffer) {
        return readBuffer.getByteArray();
    }

    public static char[] getCharArray(final IoBuffer readBuffer) {
        return readBuffer.getCharArray();
    }

    public static <E> Collection<E> getCollection(final IoBuffer readBuffer, final Collection<E> collection) {
        final DataType valueDataType = DataType.fromByte(readBuffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsPrimitive(readBuffer, valueDataType);
        final int nElements = values.length;
        final Collection<E> retCollection = collection == null ? new ArrayList<>(nElements) : collection;
        for (int i = 0; i < nElements; i++) {
            retCollection.add((E) values[i]);
        }

        return retCollection;
    }

    public static double getDouble(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getDouble();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static double[] getDoubleArray(final IoBuffer readBuffer) {
        return readBuffer.getDoubleArray();
    }

    public static double[] getDoubleArray(final IoBuffer readBuffer, final DataType dataType) {
        switch (dataType) {
        case BOOL_ARRAY:
            return toDoubles(BinarySerialiser.getBooleanArray(readBuffer));
        case BYTE_ARRAY:
            return toDoubles(BinarySerialiser.getByteArray(readBuffer));
        case SHORT_ARRAY:
            return toDoubles(BinarySerialiser.getShortArray(readBuffer));
        case INT_ARRAY:
            return toDoubles(BinarySerialiser.getIntArray(readBuffer));
        case LONG_ARRAY:
            return toDoubles(BinarySerialiser.getLongArray(readBuffer));
        case FLOAT_ARRAY:
            return toDoubles(BinarySerialiser.getFloatArray(readBuffer));
        case DOUBLE_ARRAY:
            return BinarySerialiser.getDoubleArray(readBuffer);
        case CHAR_ARRAY:
            return toDoubles(BinarySerialiser.getCharArray(readBuffer));
        case STRING_ARRAY:
            return toDoubles(BinarySerialiser.getStringArray(readBuffer));
        default:
            throw new IllegalArgumentException("dataType '" + dataType + "' is not an array");
        }
    }

    @SuppressWarnings({ "unused", "PMD.PrematureDeclaration" }) // variables need to be read from stream
    public static Enum<?> getEnum(final IoBuffer readBuffer, final Enum<?> enumeration) {
        // read value vector
        final String enumSimpleName = getString(readBuffer);
        final String enumName = getString(readBuffer);
        final String enumTypeList = getString(readBuffer);
        final String enumState = getString(readBuffer);
        final int enumOrdinal = getInteger(readBuffer);
        // TODO: implement matching by incomplete name match,
        // N.B. for the time being package name + class name is required
        Class<?> enumClass = ClassDescriptions.getClassByName(enumName);
        if (enumClass == null) {
            enumClass = ClassDescriptions.getClassByName(enumSimpleName);
            if (enumClass == null) {
                throw new IllegalStateException(
                        "could not find enum class description '" + enumName + "' or '" + enumSimpleName + "'");
            }
        }

        try {
            final Method valueOf = enumClass.getMethod("valueOf", String.class);
            return (Enum<?>) valueOf.invoke(null, enumState);
        } catch (final ReflectiveOperationException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).addArgument(enumClass)
                        .log("could not match 'valueOf(String)' function for class/(supposedly) enum of {}");
            }
        }

        return null;
    }

    @SuppressWarnings({ "unused", "PMD.PrematureDeclaration" }) // variables need to be read from stream
    public static String getEnumTypeList(final IoBuffer readBuffer) {
        // read value vector
        final String enumSimpleName = getString(readBuffer);
        final String enumName = getString(readBuffer);
        final String enumTypeList = getString(readBuffer);
        final String enumState = getString(readBuffer);
        final int enumOrdinal = getInteger(readBuffer);

        return enumTypeList;
    }

    @SuppressWarnings({ "unused", "PMD.PrematureDeclaration" }) // variables need to be read from stream
    public static FieldHeader getFieldHeader(final IoBuffer readBuffer) {
        final String fieldName = readBuffer.getString();
        final byte dataTypeByte = readBuffer.getByte();
        final DataType dataType = DataType.fromByte(dataTypeByte);

        // if (dataType.equals(DataType.END_MARKER)) {
        // return null;
        // }

        if (dataType.isScalar()) {
            final long pos = readBuffer.position();
            final long nBytesToRead = dataType.equals(DataType.STRING) ? readBuffer.getInt() + 4
                    : dataType.getPrimitiveSize();
            readBuffer.position(pos);

            return new FieldHeader(fieldName, dataType, new int[] { 1 }, readBuffer.position(), nBytesToRead);
        }

        // multi-dimensional array or other complex data type (Collection, List, Map,
        // Set...)
        final long temp = readBuffer.position();
        final int expectedNumberOfBytes = readBuffer.getInt();

        final int arrayDims = readBuffer.getInt();
        final int[] dims = new int[arrayDims];
        for (int i = 0; i < arrayDims; ++i) {
            dims[i] = readBuffer.getInt();
        }
        final long readDataPosition = readBuffer.position();
        readBuffer.position(temp);

        return new FieldHeader(fieldName, dataType, dims, readDataPosition, expectedNumberOfBytes);
    }

    public static float getFloat(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getFloat();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static float[] getFloatArray(final IoBuffer readBuffer) {
        return readBuffer.getFloatArray();
    }

    protected static Object[] getGenericArrayAsPrimitive(final IoBuffer readBuffer, final DataType dataType) {
        Object[] retVal;
        // @formatter:off
        switch (dataType) {
        case BOOL:
            retVal = GenericsHelper.toObject(readBuffer.getBooleanArray());
            break;
        case BYTE:
            retVal = GenericsHelper.toObject(readBuffer.getByteArray());
            break;
        case SHORT:
            retVal = GenericsHelper.toObject(readBuffer.getShortArray());
            break;
        case INT:
            retVal = GenericsHelper.toObject(readBuffer.getIntArray());
            break;
        case LONG:
            retVal = GenericsHelper.toObject(readBuffer.getLongArray());
            break;
        case FLOAT:
            retVal = GenericsHelper.toObject(readBuffer.getFloatArray());
            break;
        case DOUBLE:
            retVal = GenericsHelper.toObject(readBuffer.getDoubleArray());
            break;
        case STRING:
            retVal = readBuffer.getStringArray();
            break;
        // @formatter:on
        default:
            throw new IllegalArgumentException("type not implemented - " + dataType);
        }
        return retVal;
    }

    public static int[] getIntArray(final IoBuffer readBuffer) {
        return readBuffer.getIntArray();
    }

    public static int getInteger(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getInt();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static <E> List<E> getList(final IoBuffer readBuffer, final List<E> collection) {
        final DataType valueDataType = DataType.fromByte(readBuffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsPrimitive(readBuffer, valueDataType);
        final int nElements = values.length;
        final List<E> retCollection = collection == null ? new ArrayList<>(nElements) : collection;
        for (int i = 0; i < nElements; i++) {
            retCollection.add((E) values[i]);
        }

        return retCollection;
    }

    public static long getLong(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getLong();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static long[] getLongArray(final IoBuffer readBuffer) {
        return readBuffer.getLongArray();
    }

    public static <K, V> Map<K, V> getMap(final IoBuffer readBuffer, final Map<K, V> map) {
        // convert into two linear arrays one of K and the other for V
        // streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)>
        // <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        final DataType keyDataType = DataType.fromByte(readBuffer.getByte());
        final DataType valueDataType = DataType.fromByte(readBuffer.getByte());

        // read key and value vector
        final Object[] keys = getGenericArrayAsPrimitive(readBuffer, keyDataType);
        final Object[] values = getGenericArrayAsPrimitive(readBuffer, valueDataType);
        final Map<K, V> retMap = map == null ? new ConcurrentHashMap<>() : map;
        for (int i = 0; i < keys.length; i++) {
            retMap.put((K) keys[i], (V) values[i]);
        }

        return retMap;
    }

    private static int getNumberOfElements(final int[] dimensions) {
        AssertUtils.notNull("dimensions", dimensions);
        int ret = 1;
        for (int i = 0; i < dimensions.length; i++) {
            final int dim = dimensions[i];
            if (i > 0) {
                AssertUtils.gtThanZero("dimensions[" + i + "]", dim);
            }
            ret *= dim;
        }
        return ret;
    }

    public static <E> Queue<E> getQueue(final IoBuffer readBuffer, final Queue<E> collection) {
        final DataType valueDataType = DataType.fromByte(readBuffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsPrimitive(readBuffer, valueDataType);
        final int nElements = values.length;
        final Queue<E> retCollection = collection == null ? new PriorityQueue<>(nElements) : collection;
        for (int i = 0; i < nElements; i++) {
            retCollection.add((E) values[i]);
        }

        return retCollection;
    }

    public static <E> Set<E> getSet(final IoBuffer readBuffer, final Set<E> collection) {
        final DataType valueDataType = DataType.fromByte(readBuffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsPrimitive(readBuffer, valueDataType);
        final int nElements = values.length;
        final Set<E> retCollection = collection == null ? new HashSet<>(nElements) : collection;
        for (int i = 0; i < nElements; i++) {
            retCollection.add((E) values[i]);
        }

        return retCollection;
    }

    public static short getShort(final IoBuffer readBuffer) { // NOPMD
        if (readBuffer.hasRemaining()) {
            return readBuffer.getShort();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static short[] getShortArray(final IoBuffer readBuffer) { // NOPMD
        return readBuffer.getShortArray();
    }

    public static String getString(final IoBuffer readBuffer) {
        if (readBuffer.hasRemaining()) {
            return readBuffer.getString();
        }
        return null;
    }

    public static String[] getStringArray(final IoBuffer readBuffer) {
        return readBuffer.getStringArray();
    }

    public static FieldHeader parseIoStream(final IoBuffer buffer) {
        final FieldHeader fieldRoot = new FieldHeader("ROOT", DataType.START_MARKER, new int[] { 0 }, buffer.position(),
                100);

        parseIoStream(buffer, fieldRoot, 0);
        return fieldRoot;
    }

    @SuppressWarnings({ "unused", "PMD.PrematureDeclaration" }) // variables need to be read from stream
    protected static void parseIoStream(final IoBuffer buffer, final FieldHeader fieldRoot, final int recursionDepth) {

        FieldHeader fieldHeader;
        for (; ((fieldHeader = BinarySerialiser.getFieldHeader(buffer)) != null);) {
            final long bytesToSkip = fieldHeader.getExpectedNumberOfDataBytes();
            final long skipPosition = buffer.position() + bytesToSkip;
            fieldRoot.getChildren().add(fieldHeader);

            if (fieldHeader.getDataType().equals(DataType.END_MARKER)) {
                // reached end of (sub-)class
                // check marker value
                final byte markerValue = buffer.getByte();
                if (DataType.END_MARKER.getAsByte() != markerValue) {
                    throw new IllegalStateException("reached end marker, mismatched value '" + markerValue
                            + "' vs. should '" + DataType.END_MARKER.getAsByte() + "'");
                }
                break;
            }

            if (bytesToSkip < 0) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.atWarn().addArgument(fieldHeader.getFieldName()).addArgument(fieldHeader.getDataType())
                            .addArgument(bytesToSkip).log("FieldHeader for '{}' type '{}' has bytesToSkip '{} <= 0'");
                }

                // fall-back option in case of
                swallowRest(buffer, fieldHeader);
            } else {
                buffer.position(skipPosition);
            }

            if (fieldHeader.getDataType().equals(DataType.START_MARKER)) {
                buffer.position(fieldHeader.getDataBufferPosition());
                // detected sub-class start marker
                // check marker value
                final byte markerValue = buffer.getByte();
                if (DataType.START_MARKER.getAsByte() != markerValue) {
                    throw new IllegalStateException("reached start marker, mismatched value '" + markerValue
                            + "' vs. should '" + DataType.START_MARKER.getAsByte() + "'");
                }

                parseIoStream(buffer, fieldHeader, recursionDepth + 1);
            }
        }
    }

    public static void put(final IoBuffer buffer, final String fieldName, final boolean value) {
        putFieldHeader(buffer, fieldName, DataType.BOOL);
        buffer.putBoolean(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final boolean[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final boolean[] arrayValue,
            final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.BOOL_ARRAY, dims, nElements);
        buffer.putBooleanArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final byte value) {
        putFieldHeader(buffer, fieldName, DataType.BYTE);
        buffer.putByte(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final byte[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final byte[] arrayValue, final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.BYTE_ARRAY, dims, nElements);
        buffer.putByteArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static <E> void put(final IoBuffer buffer, final String fieldName, final Collection<E> collection) {
        if ((collection == null) || collection.isEmpty()) {
            return;
        }
        final Object[] values = collection.toArray();
        final int nElements = collection.size();
        final DataType valueDataType = DataType.fromClassType(values[0].getClass());
        final int entrySize = 17; // as an initial estimate
        DataType dataType = DataType.COLLECTION;
        if (collection instanceof List) {
            dataType = DataType.LIST;
        } else if (collection instanceof Queue) {
            dataType = DataType.QUEUE;
        } else if (collection instanceof Set) {
            dataType = DataType.SET;
        }

        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, dataType, new int[] { nElements },
                (nElements * entrySize) + 9);

        buffer.putByte(valueDataType.getAsByte()); // write value element type
        putGenericArrayAsPrimitive(buffer, valueDataType, values, nElements);

        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final double value) {
        putFieldHeader(buffer, fieldName, DataType.DOUBLE);
        buffer.putDouble(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final double[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final double[] arrayValue, final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.DOUBLE_ARRAY, dims, nElements);
        buffer.putDoubleArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final Enum<?> enumeration) {
        if (enumeration == null) {
            return;
        }
        final Class<? extends Enum<?>> clazz = (Class<? extends Enum<?>>) enumeration.getClass();
        if (clazz == null) {
            return;
        }
        Enum<?>[] enumConsts = clazz.getEnumConstants();
        if (enumConsts == null) {
            return;
        }

        final int nElements = 1;
        final int entrySize = 17; // as an initial estimate

        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.ENUM, new int[] { 1 },
                (nElements * entrySize) + 9);
        final String typeList = Arrays.asList(clazz.getEnumConstants()).stream()
                .map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
        buffer.putString(clazz.getSimpleName());
        buffer.putString(enumeration.getClass().getName());
        buffer.putString(typeList);
        buffer.putString(enumeration.name());
        buffer.putInt(enumeration.ordinal());

        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final float value) {
        putFieldHeader(buffer, fieldName, DataType.FLOAT);
        buffer.putFloat(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final float[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final float[] arrayValue, final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.FLOAT_ARRAY, dims, nElements);
        buffer.putFloatArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final int value) {
        putFieldHeader(buffer, fieldName, DataType.INT);
        buffer.putInt(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final int[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final int[] arrayValue, final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.INT_ARRAY, dims, nElements);
        buffer.putIntArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final long value) {
        putFieldHeader(buffer, fieldName, DataType.LONG);
        buffer.putLong(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final long[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final long[] arrayValue, final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.LONG_ARRAY, dims, nElements);
        buffer.putLongArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static <K, V> void put(final IoBuffer buffer, final String fieldName, final Map<K, V> map) {
        if ((map == null) || map.isEmpty()) {
            return;
        }
        final Object[] keySet = map.keySet().toArray();
        final Object[] valueSet = map.values().toArray();
        final int nElements = keySet.length;
        final DataType keyDataType = DataType.fromClassType(keySet[0].getClass());
        final DataType valueDataType = DataType.fromClassType(valueSet[0].getClass());
        // convert into two linear arrays one of K and the other for V
        // streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)>
        // <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        final int entrySize = 17; // as an initial estimate

        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.MAP, new int[] { nElements },
                (nElements * entrySize) + 9);

        buffer.putByte(keyDataType.getAsByte()); // write key element type
        buffer.putByte(valueDataType.getAsByte()); // write value element type
        putGenericArrayAsPrimitive(buffer, keyDataType, keySet, nElements);
        putGenericArrayAsPrimitive(buffer, valueDataType, valueSet, nElements);

        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final short value) { // NOPMD
        putFieldHeader(buffer, fieldName, DataType.SHORT);
        buffer.putShort(value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final short[] arrayValue) { // NOPMD
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final short[] arrayValue, // NOPMD
            final int[] dims) {
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.SHORT_ARRAY, dims, nElements);
        buffer.putShortArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final String value) {
        putFieldHeader(buffer, fieldName, DataType.STRING, (value == null ? 1 : value.length()) + 1);
        buffer.putString(value == null ? "" : value);
    }

    public static void put(final IoBuffer buffer, final String fieldName, final String[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final IoBuffer buffer, final String fieldName, final String[] arrayValue, final int[] dims) {
        if ((arrayValue == null) || (arrayValue.length == 0)) {
            return;
        }
        final int nElements = getNumberOfElements(dims);
        final long sizeMarkerStart = putArrayHeader(buffer, fieldName, DataType.STRING_ARRAY, dims, nElements);
        buffer.putStringArray(arrayValue, Math.min(nElements, arrayValue.length));
        adjustDataByteSizeBlock(buffer, sizeMarkerStart);
    }

    public static long putArrayHeader(final IoBuffer buffer, final String fieldName, final DataType dataType,
            final int[] dims, final int nElements) {
        AssertUtils.notNull("dims", dims);
        final int arrayByteSize = nElements * (int) dataType.getPrimitiveSize();
        final int addBufferSize = ((dims.length + 6) * (int) SIZE_OF_INT) + arrayByteSize;
        putFieldHeader(buffer, fieldName, dataType, addBufferSize);

        final long sizeMarkerStart = buffer.position();
        buffer.putInt(-1); // default size

        // add array specific header info
        buffer.putInt(dims.length); // number of dimensions
        for (final int dim : dims) {
            buffer.putInt(dim); // vector size for each dimension
        }

        return sizeMarkerStart;
    }

    public static void putEndMarker(final IoBuffer buffer, final String markerName) {
        putFieldHeader(buffer, markerName, DataType.END_MARKER);
        buffer.putByte(DataType.END_MARKER.getAsByte());
    }

    public static void putFieldHeader(final IoBuffer buffer, final String fieldName, final DataType dataType) {
        putFieldHeader(buffer, fieldName, dataType, 0);
    }

    public static void putFieldHeader(final IoBuffer buffer, final String fieldName, final DataType dataType,
            final int additionalSize) {
        AssertUtils.notNull("buffer", buffer);
        AssertUtils.notNull("fieldName", fieldName);
        // fieldName.lengthxbyte + 1 byte (\0 termination) + 4 bytes length string + 1
        // byte type
        final long addCapacity = ((fieldName.length() + 1 + 4 + 1) * SIZE_OF_BYTE) + bufferIncrements
                + dataType.getPrimitiveSize() + additionalSize;
        buffer.ensureAdditionalCapacity(addCapacity);
        buffer.putString(fieldName);
        buffer.putByte(dataType.getAsByte());
    }

    public static void putGenericArrayAsPrimitive(final IoBuffer buffer, final DataType dataType, final Object[] data,
            final int nToCopy) {

        switch (dataType) {
        case BOOL:
            buffer.putBooleanArray(GenericsHelper.toBoolPrimitive(data), nToCopy);
            break;
        case BYTE:
            buffer.putByteArray(GenericsHelper.toBytePrimitive(data), nToCopy);
            break;
        case SHORT:
            buffer.putShortArray(GenericsHelper.toShortPrimitive(data), nToCopy);
            break;
        case INT:
            buffer.putIntArray(GenericsHelper.toIntegerPrimitive(data), nToCopy);
            break;
        case LONG:
            buffer.putLongArray(GenericsHelper.toLongPrimitive(data), nToCopy);
            break;
        case FLOAT:
            buffer.putFloatArray(GenericsHelper.toFloatPrimitive(data), nToCopy);
            break;
        case DOUBLE:
            buffer.putDoubleArray(GenericsHelper.toDoublePrimitive(data), nToCopy);
            break;
        case STRING:
            buffer.putStringArray(GenericsHelper.toStringPrimitive(data), nToCopy);
            break;
        default:
            throw new IllegalArgumentException("type not implemented - " + data[0].getClass().getSimpleName());
        }
    }

    /**
     * Adds header and version information
     *
     * @param buffer to use for serialisation
     */
    public static void putHeaderInfo(final IoBuffer buffer) {
        AssertUtils.notNull("buffer", buffer);
        final long addCapacity = 20 + "OBJ_ROOT_START".length() + "#file producer : ".length()
                + BinarySerialiser.class.getCanonicalName().length();
        buffer.ensureAdditionalCapacity(addCapacity);
        putStartMarker(buffer, "OBJ_ROOT_START");
        buffer.putString("#file producer : ");
        buffer.putString(BinarySerialiser.class.getCanonicalName());
        buffer.putString("\n");
        buffer.putByte(VERSION_MAJOR);
        buffer.putByte(VERSION_MINOR);
        buffer.putByte(VERSION_MICRO);
    }

    public static void putStartMarker(final IoBuffer buffer, final String markerName) {
        putFieldHeader(buffer, markerName, DataType.START_MARKER);
        buffer.putByte(DataType.START_MARKER.getAsByte());
    }

    public static void setBufferIncrements(final int bufferIncrements) {
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        BinarySerialiser.bufferIncrements = bufferIncrements;
    }

    protected static void swallowRest(final IoBuffer readBuffer, final FieldHeader fieldHeader) {
        // parse whatever is left
        // N.B. this is/should be the only place where 'Object' is used
        // since the JVM will perform boxing of primitive types
        // automatically. Boxing and later un-boxing is a significant
        // high-performance bottleneck for any serialiser
        Object leftOver = null;
        int size = -1;
        switch (fieldHeader.getDataType()) {
        case BOOL:
            leftOver = BinarySerialiser.getBoolean(readBuffer);
            break;
        case BYTE:
            leftOver = BinarySerialiser.getByte(readBuffer);
            break;
        case SHORT:
            leftOver = BinarySerialiser.getShort(readBuffer);
            break;
        case INT:
            leftOver = BinarySerialiser.getInteger(readBuffer);
            break;
        case LONG:
            leftOver = BinarySerialiser.getLong(readBuffer);
            break;
        case FLOAT:
            leftOver = BinarySerialiser.getFloat(readBuffer);
            break;
        case DOUBLE:
            leftOver = BinarySerialiser.getDouble(readBuffer);
            break;
        case STRING:
            leftOver = BinarySerialiser.getString(readBuffer);
            break;
        case BOOL_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getBooleanArray(readBuffer);
            break;
        case BYTE_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getByteArray(readBuffer);
            break;
        case SHORT_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getShortArray(readBuffer);
            break;
        case INT_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getIntArray(readBuffer);
            break;
        case LONG_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getLongArray(readBuffer);
            break;
        case FLOAT_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getFloatArray(readBuffer);
            break;
        case DOUBLE_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getDoubleArray(readBuffer);
            break;
        case STRING_ARRAY:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getStringArray(readBuffer);
            break;
        case MAP:
            size = readBuffer.getInt();
            leftOver = BinarySerialiser.getMap(readBuffer, new ConcurrentHashMap<>());
            break;
        case END_MARKER:
            final byte endMarker = BinarySerialiser.getByte(readBuffer);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(fieldHeader).addArgument(Byte.toString(endMarker))
                        .log("swallowed '{}'='{}'");
            }
            break;
        default:
            throw new IllegalArgumentException("encountered unknown format for " + fieldHeader.toString());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(fieldHeader).addArgument(leftOver).addArgument(size)
                    .log("swallowed unused element '%s{}'='%s{}' size = {}");
        }
    }

    private static double[] toDoubles(final boolean[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i] ? 1.0 : 0.0;
        }
        return doubleArray;
    }

    private static double[] toDoubles(final byte[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    private static double[] toDoubles(final char[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    private static double[] toDoubles(final float[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    private static double[] toDoubles(final int[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    private static double[] toDoubles(final long[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    private static double[] toDoubles(final short[] input) { // NOPMD
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    private static double[] toDoubles(final String[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i] == null ? Double.NaN : Double.parseDouble(input[i]);
        }
        return doubleArray;
    }

    //
    // -- HELPER CLASSES ---------------------------------------------
    //

    public class HeaderInfo {
        private final String producerName;
        private final byte versionMajor;
        private final byte versionMinor;
        private final byte versionMicro;

        private HeaderInfo(final String producer, final byte major, final byte minor, final byte micro) {
            producerName = producer;
            versionMajor = major;
            versionMinor = minor;
            versionMicro = micro;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof HeaderInfo)) {
                return false;
            }
            final HeaderInfo other = (HeaderInfo) obj;
            return other.isCompatible();
        }

        public String getProducerName() {
            return producerName;
        }

        public byte getVersionMajor() {
            return versionMajor;
        }

        public byte getVersionMicro() {
            return versionMicro;
        }

        public byte getVersionMinor() {
            return versionMinor;
        }

        @Override
        public int hashCode() {
            return producerName.hashCode();
        }

        public boolean isCompatible() {
            return getVersionMajor() <= VERSION_MAJOR;
        }

        @Override
        public String toString() {
            return String.format("%s-v%d.%d.%d", getProducerName(), getVersionMajor(), getVersionMinor(),
                    getVersionMicro());
        }
    }
}
