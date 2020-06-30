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
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.utils.AssertUtils;
import de.gsi.dataset.utils.GenericsHelper;

/**
 * Generic binary serialiser aimed at efficiently transferring data between server/client and in particular between
 * Java/C++/web-based programs.
 * 
 * <p>
 * There are two default backing buffer implementations ({@link de.gsi.dataset.serializer.spi.FastByteBuffer FastByteBuffer} and {@link de.gsi.dataset.serializer.spi.ByteBuffer ByteBuffer}), 
 * but can be extended/replaced with any other buffer is also possible provided it implements the {@link de.gsi.dataset.serializer.IoBuffer IoBuffer} interface.
 * 
 * <p>
 * The default serialisable data types are defined in {@link de.gsi.dataset.serializer.DataType DataType} and include definitions for 
 * <ul>
 * <li> primitives (byte, short, ..., float, double, and String), and 
 * <li> arrays thereof (ie. byte[], short[], ..., float[], double[], and String[]), as well as 
 * <li> complex objects implementing Collections (ie. Set, List, Queues), Enums or Maps.
 * </ul> 
 * Any other complex data objects can be stored/extended using the {@link DataType#OTHER OTHER} sub-type.
 * 
 * N.B. Multi-dimensional arrays are handled through one-dimensional striding arrays with the additional
 * infos on number of dimensions and size for each individual dimension.
 * 
 * <p>
 * <b>raw-byte level protocol</b>: above data items are stored as follows:
 * <pre><code>
 * * header info:   [ start marker ] + 
 *                      [ "#file producer : ": String ] + 
 *                      [ clear text serialiser name: String ] + // e.g. "de.gsi.dataset.serializer.spi.BinarySerialiser"
 *                      [ "\n":String ] + 
 *                      [ 1 byte - major protocol version ] +
 *                      [ 1 byte - minor protocol version ] +
 *                      [ 1 byte - micro protocol version ] // micro: non API-changing bug fixes in implementation 
 * * start marker:  [ String - field name ][ 1 byte - uniqueType (0x00) ]
 * * String:        [ 4 bytes (int) - length (including termination) ][ n bytes based on latin1 encoding ]
 * * field header:  [ String - field name ] + [ 1 byte - uniqueType ]
 * * primitives:    [ field header  ] + [ 1-8 bytes depending on DataType ]
 * * prim. arrays:  [ array header  ] + [   ]=1-8 bytes x N_i or more - array data depending on variable DataType ]
 * * boxed arrays:  as above but each element cast to corresponding primitive type
 * * array header:  [ field header  ] + 
 *                      [4 bytes - number of bytes to skip until data end (-1: uninitialised) ] +
 *                      [4 bytes - number of dimensions N_d ] + 
 *                      [4 bytes x N_d - vector sizes for each dimension N_i ]  
 * * Collection[E]:
 * * List[]:
 * * Queue[E]:
 * * Set[E]:        [ array header (uniqueType= one of the Collection type IDs) ] + 
 *                      [ 1 byte - uniqueType of E ] + [  n bytes - array of E cast to primitive type and/or string ]
 * * Map[K,V]:      [ array header (uniqueType=0xCB) ] + [ 1 byte - uniqueType of K ] +  [ 1 byte - uniqueType of V ] +
 *                      [ n bytes - array of K cast to primitive type and/or string ] + 
 *                      [ n bytes - array of V cast to primitive type and/or string ]
 * * OTHER          [ field header - uniqueByte = 0xFD ] + 
 *                      [ 4 bytes - number of bytes to skip until data end (-1: uninitialised) ] +
 *                      [ 1 byte - uniqueType -- custom definition ]
 *                      [ n bytes - custom serialisation definition ]
 * * end marker:    [ String - field name ][ 1 byte - uniqueType (0xFE) ]
 * 
 * * nesting or sub-structures (ie. POJOs with sub-classes) can be achieved via:
 * [  start marker - field name == nesting context1 ] 
 *   [  start marker - field name == nesting context2 ]
 *    ... 
 *   [  end marker - field name == nesting context2 (optional name) ]
 * [  end marker - field name == nesting context1 (optional name) ]
 * 
 * with
 * T: being a generic list parameter outlined in {@link de.gsi.dataset.serializer.DataType DataType}
 * K: being a generic key parameter outlined in {@link de.gsi.dataset.serializer.DataType DataType}
 * V: being a generic value parameter outlined in {@link de.gsi.dataset.serializer.DataType DataType}
 * </code></pre>
 * 
 * @author rstein
 */
@SuppressWarnings({ "PMD.CommentSize", "PMD.ExcessivePublicCount", "PMD.PrematureDeclaration", "unused" }) // variables need to be read from stream
public class BinarySerialiser implements IoSerialiser { // NOPMD - omen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiser.class);
    private static final String READ_POSITION_AT_BUFFER_END = "read position at buffer end";
    public static final byte VERSION_MAJOR = 1;
    public static final byte VERSION_MINOR = 0;
    public static final byte VERSION_MICRO = 0;
    private static final DataType[] byteToDataType = new DataType[256];
    private static final Byte[] dataTypeToByte = new Byte[256];
    static {
        byteToDataType[0] = DataType.START_MARKER;

        byteToDataType[1] = DataType.BOOL;
        byteToDataType[2] = DataType.BYTE;
        byteToDataType[3] = DataType.SHORT;
        byteToDataType[4] = DataType.INT;
        byteToDataType[5] = DataType.LONG;
        byteToDataType[6] = DataType.FLOAT;
        byteToDataType[7] = DataType.DOUBLE;
        byteToDataType[8] = DataType.CHAR;
        byteToDataType[9] = DataType.STRING;

        byteToDataType[101] = DataType.BOOL_ARRAY;
        byteToDataType[102] = DataType.BYTE_ARRAY;
        byteToDataType[103] = DataType.SHORT_ARRAY;
        byteToDataType[104] = DataType.INT_ARRAY;
        byteToDataType[105] = DataType.LONG_ARRAY;
        byteToDataType[106] = DataType.FLOAT_ARRAY;
        byteToDataType[107] = DataType.DOUBLE_ARRAY;
        byteToDataType[108] = DataType.CHAR_ARRAY;
        byteToDataType[109] = DataType.STRING_ARRAY;

        byteToDataType[200] = DataType.COLLECTION;
        byteToDataType[201] = DataType.ENUM;
        byteToDataType[202] = DataType.LIST;
        byteToDataType[203] = DataType.MAP;
        byteToDataType[204] = DataType.QUEUE;
        byteToDataType[205] = DataType.SET;

        byteToDataType[0xFD] = DataType.OTHER;
        byteToDataType[0xFE] = DataType.END_MARKER;

        int count = 0;
        for (int i = 0; i < byteToDataType.length; i++) {
            if (byteToDataType[i] == null) {
                continue;
            }
            final int id = byteToDataType[i].getID();
            dataTypeToByte[id] = (byte) i;
        }
    }

    protected final HeaderInfo headerInfo = new HeaderInfo(BinarySerialiser.class.getCanonicalName(), VERSION_MAJOR, VERSION_MINOR, VERSION_MICRO);
    private static int bufferIncrements;
    private IoBuffer buffer;

    /**
     * @param buffer the backing IoBuffer (see e.g. {@link de.gsi.dataset.serializer.spi.FastByteBuffer} or{@link de.gsi.dataset.serializer.spi.ByteBuffer}
     */
    public BinarySerialiser(final IoBuffer buffer) {
        super();
        this.buffer = buffer;
    }

    public IoBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(final IoBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void adjustDataByteSizeBlock(final long sizeMarkerStart) {
        final long sizeMarkerEnd = buffer.position();

        // go back and re-adjust the actual size info
        buffer.position(sizeMarkerStart);
        final long expectedNumberOfBytes = sizeMarkerEnd - sizeMarkerStart;
        buffer.putInt((int) expectedNumberOfBytes); // write actual byte size

        // go back to the new write position
        buffer.position(sizeMarkerEnd);
    }

    @Override
    public HeaderInfo checkHeaderInfo() {
        AssertUtils.notNull("buffer", buffer);
        final FieldHeader headerStartField = getFieldHeader();
        final byte startMarker = buffer.getByte();
        if (startMarker != getDataType(DataType.START_MARKER)) {
            // TODO: replace with (new to be written) custom SerializerFormatException(..)
            throw new InvalidParameterException("header does not start with a START_MARKER('" + getDataType(DataType.START_MARKER) + "') DataType but " + startMarker + " fieldName = " + headerStartField.getFieldName());
        }
        buffer.getString(); // should read "#file producer : "
        // -- but not explicitly checked
        final String producer = buffer.getString();
        buffer.getString(); // not explicitly checked
        final byte major = buffer.getByte();
        final byte minor = buffer.getByte();
        final byte micro = buffer.getByte();

        final HeaderInfo header = new HeaderInfo(headerStartField, producer, major, minor, micro);

        if (!header.isCompatible()) {
            final String msg = String.format("byte buffer version incompatible: received '%s' vs. this '%s'", header.toString(), headerInfo.toString());
            throw new IllegalStateException(msg);
        }
        return header;
    }

    public int[] getArrayDimensions() {
        final int arrayDims = buffer.getInt(); // array dimensions
        final int[] dims = new int[arrayDims];
        for (int i = 0; i < arrayDims; ++i) {
            dims[i] = buffer.getInt();
        }
        return dims;
    }

    @Override
    public boolean getBoolean() {
        if (buffer.hasRemaining()) {
            return buffer.getBoolean();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public boolean[] getBooleanArray() {
        if (buffer.hasRemaining()) {
            return buffer.getBooleanArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public int getBufferIncrements() {
        return bufferIncrements;
    }

    @Override
    public byte getByte() {
        if (buffer.hasRemaining()) {
            return buffer.getByte();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public byte[] getByteArray() {
        if (buffer.hasRemaining()) {
            return buffer.getByteArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public char getCharacter() {
        if (buffer.hasRemaining()) {
            return buffer.getChar();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public char[] getCharArray() {
        return buffer.getCharArray();
    }

    @Override
    public <E> Collection<E> getCollection(final Collection<E> collection) {
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        final int nElements = values.length;
        final Collection<E> retCollection = collection == null ? new ArrayList<>(nElements) : collection;
        for (final Object value : values) {
            retCollection.add((E) value);
        }

        return retCollection;
    }

    @Override
    public double getDouble() {
        if (buffer.hasRemaining()) {
            return buffer.getDouble();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public double[] getDoubleArray() {
        if (buffer.hasRemaining()) {
            return buffer.getDoubleArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public double[] getDoubleArray(final DataType dataType) {
        switch (dataType) {
        case BOOL_ARRAY:
            return GenericsHelper.toDoublePrimitive(getBooleanArray());
        case BYTE_ARRAY:
            return GenericsHelper.toDoublePrimitive(getByteArray());
        case SHORT_ARRAY:
            return GenericsHelper.toDoublePrimitive(getShortArray());
        case INT_ARRAY:
            return GenericsHelper.toDoublePrimitive(getIntArray());
        case LONG_ARRAY:
            return GenericsHelper.toDoublePrimitive(getLongArray());
        case FLOAT_ARRAY:
            return GenericsHelper.toDoublePrimitive(getFloatArray());
        case DOUBLE_ARRAY:
            return getDoubleArray();
        case CHAR_ARRAY:
            return GenericsHelper.toDoublePrimitive(getCharArray());
        case STRING_ARRAY:
            return GenericsHelper.toDoublePrimitive(getStringArray());
        default:
            throw new IllegalArgumentException("dataType '" + dataType + "' is not an array");
        }
    }

    @Override
    public <E extends Enum<E>> Enum<E> getEnum(final Enum<E> enumeration) {
        // read value vector
        final String enumSimpleName = getString();
        final String enumName = getString();
        final String enumTypeList = getString();
        final String enumState = getString();
        final int enumOrdinal = getInteger();
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
            return (Enum<E>) valueOf.invoke(null, enumState);
        } catch (final ReflectiveOperationException e) {
            LOGGER.atError().setCause(e).addArgument(enumClass).log("could not match 'valueOf(String)' function for class/(supposedly) enum of {}");
        }

        return null;
    }

    @Override
    public String getEnumTypeList() {
        // read value vector
        final String enumSimpleName = getString();
        final String enumName = getString();
        final String enumTypeList = getString();
        final String enumState = getString();
        final int enumOrdinal = getInteger();

        return enumTypeList;
    }

    @Override
    public FieldHeader getFieldHeader() {
        final String fieldName = buffer.getString();
        final byte dataTypeByte = buffer.getByte();
        final DataType dataType = getDataType(dataTypeByte);

        if (dataType.isScalar()) {
            final long pos = buffer.position();
            final long nBytesToRead = dataType == DataType.STRING ? buffer.getInt() + 4
                                                                  : dataType.getPrimitiveSize();
            buffer.position(pos);

            return new FieldHeader(fieldName, dataType, new int[] { 1 }, buffer.position(), nBytesToRead);
        }

        // multi-dimensional array or other complex data type (Collection, List, Map, Set...)
        final long temp = buffer.position();
        final int expectedNumberOfBytes = buffer.getInt();

        final int[] dims = getArrayDimensions();
        final long readDataPosition = buffer.position();
        buffer.position(temp);

        return new FieldHeader(fieldName, dataType, dims, readDataPosition, expectedNumberOfBytes);
    }

    @Override
    public float getFloat() {
        if (buffer.hasRemaining()) {
            return buffer.getFloat();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public float[] getFloatArray() {
        if (buffer.hasRemaining()) {
            return buffer.getFloatArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    protected Object[] getGenericArrayAsBoxedPrimitive(final DataType dataType) {
        final Object[] retVal;
        // @formatter:off
        switch (dataType) {
        case BOOL:
            retVal = GenericsHelper.toObject(buffer.getBooleanArray());
            break;
        case BYTE:
            retVal = GenericsHelper.toObject(buffer.getByteArray());
            break;
        case CHAR:
            retVal = GenericsHelper.toObject(buffer.getCharArray());
            break;
        case SHORT:
            retVal = GenericsHelper.toObject(buffer.getShortArray());
            break;
        case INT:
            retVal = GenericsHelper.toObject(buffer.getIntArray());
            break;
        case LONG:
            retVal = GenericsHelper.toObject(buffer.getLongArray());
            break;
        case FLOAT:
            retVal = GenericsHelper.toObject(buffer.getFloatArray());
            break;
        case DOUBLE:
            retVal = GenericsHelper.toObject(buffer.getDoubleArray());
            break;
        case STRING:
            retVal = buffer.getStringArray();
            break;
        // @formatter:on
        default:
            throw new IllegalArgumentException("type not implemented - " + dataType);
        }
        return retVal;
    }

    @Override
    public int[] getIntArray() {
        if (buffer.hasRemaining()) {
            return buffer.getIntArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public int getInteger() {
        if (buffer.hasRemaining()) {
            return buffer.getInt();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public <E> List<E> getList(final List<E> collection) {
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        final int nElements = values.length;
        final List<E> retCollection = collection == null ? new ArrayList<>(nElements) : collection;
        for (final Object value : values) {
            retCollection.add((E) value);
        }

        return retCollection;
    }

    @Override
    public long getLong() {
        if (buffer.hasRemaining()) {
            return buffer.getLong();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public long[] getLongArray() {
        if (buffer.hasRemaining()) {
            return buffer.getLongArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public <K, V> Map<K, V> getMap(final Map<K, V> map) {
        // convert into two linear arrays one of K and the other for V streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)> <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        final DataType keyDataType = getDataType(buffer.getByte());
        final DataType valueDataType = getDataType(buffer.getByte());

        // read key and value vector
        final Object[] keys = getGenericArrayAsBoxedPrimitive(keyDataType);
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        final Map<K, V> retMap = map == null ? new ConcurrentHashMap<>() : map;
        for (int i = 0; i < keys.length; i++) {
            retMap.put((K) keys[i], (V) values[i]);
        }

        return retMap;
    }

    protected static int getNumberOfElements(final int[] dimensions) {
        AssertUtils.notNull("dimensions", dimensions);
        int ret = 1;
        for (int dim : dimensions) {
            ret *= dim;
        }
        return ret;
    }

    @Override
    public <E> Queue<E> getQueue(final Queue<E> collection) {
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        final int nElements = values.length;
        final Queue<E> retCollection = collection == null ? new PriorityQueue<>(nElements) : collection;
        for (final Object value : values) {
            retCollection.add((E) value);
        }

        return retCollection;
    }

    @Override
    public <E> Set<E> getSet(final Set<E> collection) {
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        final int nElements = values.length;
        final Set<E> retCollection = collection == null ? new HashSet<>(nElements) : collection;
        for (final Object value : values) {
            retCollection.add((E) value);
        }

        return retCollection;
    }

    @Override
    public short getShort() { // NOPMD
        if (buffer.hasRemaining()) {
            return buffer.getShort();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public short[] getShortArray() { // NOPMD
        if (buffer.hasRemaining()) {
            return buffer.getShortArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public String getString() {
        if (buffer.hasRemaining()) {
            return buffer.getString();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public String[] getStringArray() {
        if (buffer.hasRemaining()) {
            return buffer.getStringArray();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    @Override
    public FieldHeader parseIoStream() {
        final FieldHeader fieldRoot = new FieldHeader("ROOT", DataType.OTHER, new int[] { 0 }, buffer.position(), 100);
        final HeaderInfo headerRoot = checkHeaderInfo();
        headerRoot.setParent(fieldRoot);
        fieldRoot.getChildren().add(headerRoot);

        parseIoStream(headerRoot, 0);
        return fieldRoot;
    }

    protected void parseIoStream(final FieldHeader fieldRoot, final int recursionDepth) {
        FieldHeader fieldHeader;
        while ((fieldHeader = getFieldHeader()) != null) {
            final long bytesToSkip = fieldHeader.getExpectedNumberOfDataBytes();
            final long skipPosition = buffer.position() + bytesToSkip;
            fieldHeader.setParent(fieldRoot);

            // reached end of (sub-)class - check marker value and close nested hierarchy
            if (fieldHeader.getDataType() == DataType.END_MARKER) {
                // reached end of (sub-)class - check marker value and close nested hierarchy
                final byte markerValue = buffer.getByte();
                final Optional<FieldHeader> superParent = fieldRoot.getParent();
                if (getDataType(DataType.END_MARKER) != markerValue) {
                    throw new IllegalStateException("reached end marker, mismatched value '" + markerValue + "' vs. should '" + getDataType(DataType.END_MARKER) + "'");
                }

                if (superParent.isEmpty()) {
                    fieldRoot.getChildren().add(fieldHeader);
                } else {
                    superParent.get().getChildren().add(fieldHeader);
                }
                break;
            }

            fieldRoot.getChildren().add(fieldHeader);

            if (bytesToSkip < 0) {
                LOGGER.atWarn().addArgument(fieldHeader.getFieldName()).addArgument(fieldHeader.getDataType()).addArgument(bytesToSkip).log("FieldHeader for '{}' type '{}' has bytesToSkip '{} <= 0'");

                // fall-back option in case of
                swallowRest(fieldHeader);
            } else {
                buffer.position(skipPosition);
            }

            // detected sub-class start marker
            // check marker value
            if (fieldHeader.getDataType() == DataType.START_MARKER) {
                buffer.position(fieldHeader.getDataBufferPosition());
                // detected sub-class start marker
                // check marker value
                final byte markerValue = buffer.getByte();
                if (getDataType(DataType.START_MARKER) != markerValue) {
                    throw new IllegalStateException("reached start marker, mismatched value '" + markerValue + "' vs. should '" + getDataType(DataType.START_MARKER) + "'");
                }

                parseIoStream(fieldHeader, recursionDepth + 1);
            }
        }
    }

    @Override
    public void put(final String fieldName, final boolean value) {
        putFieldHeader(fieldName, DataType.BOOL);
        buffer.putBoolean(value);
    }

    @Override
    public void put(final String fieldName, final boolean[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final boolean[] arrayValue,
            final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.BOOL_ARRAY, dims, nElements);
        buffer.putBooleanArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final byte value) {
        putFieldHeader(fieldName, DataType.BYTE);
        buffer.putByte(value);
    }

    @Override
    public void put(final String fieldName, final byte[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final byte[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.BYTE_ARRAY, dims, nElements);
        buffer.putByteArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final char value) {
        putFieldHeader(fieldName, DataType.CHAR);
        buffer.putChar(value);
    }

    @Override
    public void put(final String fieldName, final char[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final char[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.CHAR_ARRAY, dims, nElements);
        buffer.putCharArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public <E> void put(final String fieldName, final Collection<E> collection) {
        if (collection == null || collection.isEmpty()) {
            return;
        }
        final Object[] values = collection.toArray();
        final int nElements = collection.size();
        final DataType valueDataType = DataType.fromClassType(values[0].getClass());
        final int entrySize = 17; // as an initial estimate
        DataType dataType = DataType.COLLECTION;
        if (collection instanceof Queue) {
            dataType = DataType.QUEUE;
        } else if (collection instanceof List) {
            dataType = DataType.LIST;
        } else if (collection instanceof Set) {
            dataType = DataType.SET;
        }

        final long sizeMarkerStart = putArrayHeader(fieldName, dataType, new int[] { nElements },
                (nElements * entrySize) + 9);

        buffer.putByte(getDataType(valueDataType)); // write value element type
        putGenericArrayAsPrimitive(valueDataType, values, nElements);

        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final double value) {
        putFieldHeader(fieldName, DataType.DOUBLE);
        buffer.putDouble(value);
    }

    @Override
    public void put(final String fieldName, final double[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final double[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.DOUBLE_ARRAY, dims, nElements);
        buffer.putDoubleArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final Enum<?> enumeration) {
        if (enumeration == null) {
            return;
        }
        final Class<? extends Enum<?>> clazz = (Class<? extends Enum<?>>) enumeration.getClass();
        if (clazz == null) {
            return;
        }
        final Enum<?>[] enumConsts = clazz.getEnumConstants();
        if (enumConsts == null) {
            return;
        }

        final int nElements = 1;
        final int entrySize = 17; // as an initial estimate

        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.ENUM, new int[] { 1 },
                (nElements * entrySize) + 9);
        final String typeList = Arrays.stream(clazz.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
        buffer.putString(clazz.getSimpleName());
        buffer.putString(enumeration.getClass().getName());
        buffer.putString(typeList);
        buffer.putString(enumeration.name());
        buffer.putInt(enumeration.ordinal());

        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final float value) {
        putFieldHeader(fieldName, DataType.FLOAT);
        buffer.putFloat(value);
    }

    @Override
    public void put(final String fieldName, final float[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final float[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.FLOAT_ARRAY, dims, nElements);
        buffer.putFloatArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final int value) {
        putFieldHeader(fieldName, DataType.INT);
        buffer.putInt(value);
    }

    @Override
    public void put(final String fieldName, final int[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final int[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.INT_ARRAY, dims, nElements);
        buffer.putIntArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final long value) {
        putFieldHeader(fieldName, DataType.LONG);
        buffer.putLong(value);
    }

    @Override
    public void put(final String fieldName, final long[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final long[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.LONG_ARRAY, dims, nElements);
        buffer.putLongArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public <K, V> void put(final String fieldName, final Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        final Object[] keySet = map.keySet().toArray();
        final Object[] valueSet = map.values().toArray();
        final int nElements = keySet.length;
        final DataType keyDataType = DataType.fromClassType(keySet[0].getClass());
        final DataType valueDataType = DataType.fromClassType(valueSet[0].getClass());
        // convert into two linear arrays one of K and the other for V streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)> <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        final int entrySize = 17; // as an initial estimate

        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.MAP, new int[] { nElements },
                (nElements * entrySize) + 9);

        buffer.putByte(getDataType(keyDataType)); // write key element type
        buffer.putByte(getDataType(valueDataType)); // write value element type
        putGenericArrayAsPrimitive(keyDataType, keySet, nElements);
        putGenericArrayAsPrimitive(valueDataType, valueSet, nElements);

        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final short value) { // NOPMD
        putFieldHeader(fieldName, DataType.SHORT);
        buffer.putShort(value);
    }

    @Override
    public void put(final String fieldName, final short[] arrayValue) { // NOPMD
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final short[] arrayValue, // NOPMD
            final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.SHORT_ARRAY, dims, nElements);
        buffer.putShortArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public void put(final String fieldName, final String value) {
        putFieldHeader(fieldName, DataType.STRING, (value == null ? 1 : value.length()) + 1);
        buffer.putString(value == null ? "" : value);
    }

    @Override
    public void put(final String fieldName, final String[] arrayValue) {
        put(fieldName, arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String fieldName, final String[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long addCapacity = Arrays.stream(arrayValue).map(s -> s.length() + 1).reduce(0, Integer::sum);
        buffer.ensureAdditionalCapacity(addCapacity);
        final long sizeMarkerStart = putArrayHeader(fieldName, DataType.STRING_ARRAY, dims, nElements);
        buffer.putStringArray(arrayValue, nElements);
        adjustDataByteSizeBlock(sizeMarkerStart);
    }

    @Override
    public long putArrayHeader(final String fieldName, final DataType dataType,
            final int[] dims, final int nElements) {
        AssertUtils.notNull("dims", dims);
        final int arrayByteSize = nElements * (int) dataType.getPrimitiveSize();
        final int addBufferSize = ((dims.length + 6) * (int) SIZE_OF_INT) + arrayByteSize;
        putFieldHeader(fieldName, dataType, addBufferSize);

        final long sizeMarkerStart = buffer.position();
        buffer.putInt(-1); // default size

        // add array specific header info
        buffer.putInt(dims.length); // number of dimensions
        for (final int dim : dims) {
            buffer.putInt(dim); // vector size for each dimension
        }

        return sizeMarkerStart;
    }

    @Override
    public void putEndMarker(final String markerName) {
        putFieldHeader(markerName, DataType.END_MARKER);
        buffer.putByte(getDataType(DataType.END_MARKER));
    }

    @Override
    public void putFieldHeader(final String fieldName, final DataType dataType) {
        putFieldHeader(fieldName, dataType, 0);
    }

    @Override
    public void putFieldHeader(final String fieldName, final DataType dataType, final int additionalSize) {
        AssertUtils.notNull("buffer", buffer);
        AssertUtils.notNull("fieldName", fieldName);
        // fieldName.lengthxbyte + 1 byte (\0 termination) + 4 bytes length string + 1 byte type
        final long addCapacity = ((fieldName.length() + 1 + 4 + 1) * SIZE_OF_BYTE) + bufferIncrements
                                 + dataType.getPrimitiveSize() + additionalSize;
        buffer.ensureAdditionalCapacity(addCapacity);
        buffer.putString(fieldName);
        buffer.putByte(getDataType(dataType));
    }

    public void putGenericArrayAsPrimitive(final DataType dataType, final Object[] data,
            final int nToCopy) {
        switch (dataType) {
        case BOOL:
            buffer.putBooleanArray(GenericsHelper.toBoolPrimitive(data), nToCopy);
            break;
        case BYTE:
            buffer.putByteArray(GenericsHelper.toBytePrimitive(data), nToCopy);
            break;
        case CHAR:
            buffer.putCharArray(GenericsHelper.toCharPrimitive(data), nToCopy);
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

    @Override
    public void putHeaderInfo() {
        AssertUtils.notNull("buffer", buffer);
        final long addCapacity = 20 + "OBJ_ROOT_START".length() + "#file producer : ".length() + BinarySerialiser.class.getCanonicalName().length();
        buffer.ensureAdditionalCapacity(addCapacity);
        putStartMarker("OBJ_ROOT_START");
        buffer.putString("#file producer : ");
        buffer.putString(BinarySerialiser.class.getCanonicalName());
        buffer.putString("\n");
        buffer.putByte(VERSION_MAJOR);
        buffer.putByte(VERSION_MINOR);
        buffer.putByte(VERSION_MICRO);
    }

    @Override
    public void putStartMarker(final String markerName) {
        putFieldHeader(markerName, DataType.START_MARKER);
        buffer.putByte(getDataType(DataType.START_MARKER));
    }

    public void setBufferIncrements(final int bufferIncrements) {
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        BinarySerialiser.bufferIncrements = bufferIncrements;
    }

    @SuppressWarnings("PMD.NcssCount")
    protected void swallowRest(final FieldHeader fieldHeader) {
        // parse whatever is left
        // N.B. this is/should be the only place where 'Object' is used since the JVM will perform boxing of primitive types
        // automatically. Boxing and later un-boxing is a significant high-performance bottleneck for any serialiser
        Object leftOver;
        int size = -1;
        switch (fieldHeader.getDataType()) {
        case BOOL:
            leftOver = getBoolean();
            break;
        case BYTE:
            leftOver = getByte();
            break;
        case SHORT:
            leftOver = getShort();
            break;
        case INT:
            leftOver = getInteger();
            break;
        case LONG:
            leftOver = getLong();
            break;
        case FLOAT:
            leftOver = getFloat();
            break;
        case DOUBLE:
            leftOver = getDouble();
            break;
        case STRING:
            leftOver = getString();
            break;
        case BOOL_ARRAY:
            leftOver = getBooleanArray();
            break;
        case BYTE_ARRAY:
            leftOver = getByteArray();
            break;
        case SHORT_ARRAY:
            leftOver = getShortArray();
            break;
        case INT_ARRAY:
            leftOver = getIntArray();
            break;
        case LONG_ARRAY:
            leftOver = getLongArray();
            break;
        case FLOAT_ARRAY:
            leftOver = getFloatArray();
            break;
        case DOUBLE_ARRAY:
            leftOver = getDoubleArray();
            break;
        case STRING_ARRAY:
            leftOver = getStringArray();
            break;
        case COLLECTION:
            leftOver = getCollection(new ArrayList<>());
            break;
        case LIST:
            leftOver = getList(new ArrayList<>());
            break;
        case SET:
            leftOver = getSet(new HashSet<>());
            break;
        case QUEUE:
            leftOver = getQueue(new PriorityQueue<>());
            break;
        case MAP:
            leftOver = getMap(new ConcurrentHashMap<>());
            break;
        case ENUM:
            leftOver = getEnumTypeList();
            break;
        case START_MARKER:
        case END_MARKER:
            size = 1;
            leftOver = getByte();
            break;
        default:
            throw new IllegalArgumentException("encountered unknown format for " + fieldHeader.toString());
        }

        if (buffer.position() >= buffer.capacity()) {
            throw new IllegalStateException("read beyond buffer capacity, position = " + buffer.position() + " vs capacity = " + buffer.capacity());
        }

        LOGGER.atTrace().addArgument(fieldHeader).addArgument(leftOver).addArgument(size).log("swallowed unused element '{}'='{}' size = {}");
    }

    public static class HeaderInfo extends FieldHeader {
        private final String producerName;
        private final byte versionMajor;
        private final byte versionMinor;
        private final byte versionMicro;

        private HeaderInfo(final String producer, final byte major, final byte minor, final byte micro) {
            super(producer, DataType.START_MARKER, new int[] {}, -1, -1);
            producerName = producer;
            versionMajor = major;
            versionMinor = minor;
            versionMicro = micro;
        }

        private HeaderInfo(FieldHeader fieldHeader, final String producer, final byte major, final byte minor, final byte micro) {
            super(fieldHeader.getFieldName(), fieldHeader.getDataType(), fieldHeader.getDataDimensions(), fieldHeader.getDataBufferPosition(), fieldHeader.getExpectedNumberOfDataBytes());
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
            // N.B. no API changes within the same 'major.minor'- version
            // micro.version tracks possible benin additions & internal bug-fixes
            return getVersionMajor() <= VERSION_MAJOR && getVersionMinor() <= VERSION_MINOR;
        }

        @Override
        public String toString() {
            return super.toString() + String.format(" serialiser: %s-v%d.%d.%d", getProducerName(), getVersionMajor(), getVersionMinor(), getVersionMicro());
        }
    }

    public static byte getDataType(final DataType dataType) {
        final int id = dataType.getID();
        if (dataTypeToByte[id] != null) {
            return dataTypeToByte[id];
        }

        throw new IllegalArgumentException("DataType " + dataType + " not mapped to specific byte");
    }

    public static DataType getDataType(final byte byteValue) {
        if (dataTypeToByte[byteValue & 0xFF] != null) {
            return byteToDataType[byteValue & 0xFF];
        }

        throw new IllegalArgumentException("DataType byteValue=" + byteValue + " rawByteValue=" + (byteValue & 0xFF) + "not mapped");
    }
}
