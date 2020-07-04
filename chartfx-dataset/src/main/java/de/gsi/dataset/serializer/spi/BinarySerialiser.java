package de.gsi.dataset.serializer.spi;

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
import de.gsi.dataset.serializer.FieldDescription;
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
    public static final byte VERSION_MAJOR = 1;
    public static final byte VERSION_MINOR = 0;
    public static final byte VERSION_MICRO = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiser.class);
    private static final String READ_POSITION_AT_BUFFER_END = "read position at buffer end";
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

        for (int i = 0; i < byteToDataType.length; i++) {
            if (byteToDataType[i] == null) {
                continue;
            }
            final int id = byteToDataType[i].getID();
            dataTypeToByte[id] = (byte) i;
        }
    }

    protected final ProtocolInfo headerInfo = new ProtocolInfo(BinarySerialiser.class.getCanonicalName(), VERSION_MAJOR, VERSION_MINOR, VERSION_MICRO);
    private int bufferIncrements;
    private IoBuffer buffer;
    private WireDataFieldDescription parent;
    private WireDataFieldDescription lastFieldHeader;
    private long fieldHeaderDataEndMarkerPosition;
    private long fieldHeaderDataStart;

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
    public void updateDataEndMarker() {
        final long sizeMarkerEnd = buffer.position();
        buffer.position(fieldHeaderDataEndMarkerPosition);
        buffer.putInt((int) (sizeMarkerEnd - fieldHeaderDataStart));
        buffer.position(sizeMarkerEnd);
    }

    @Override
    public ProtocolInfo checkHeaderInfo() {
        final WireDataFieldDescription headerStartField = getFieldHeader();
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

        final ProtocolInfo header = new ProtocolInfo(headerStartField, producer, major, minor, micro);

        if (!header.isCompatible()) {
            final String msg = String.format("byte buffer version incompatible: received '%s' vs. this '%s'", header.toString(), headerInfo.toString());
            throw new IllegalStateException(msg);
        }
        return header;
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

    public void setBufferIncrements(final int bufferIncrements) {
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        this.bufferIncrements = bufferIncrements;
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
        buffer.getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType valueDataType = getDataType(buffer.getByte());
        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException("protocol mismatch nElements header = " + nElements + " vs. array = " + values.length);
        }

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
        final String enumSimpleName = buffer.getStringISO8859();
        final String enumName = buffer.getStringISO8859();
        buffer.getStringISO8859(); // enumTypeList
        final String enumState = buffer.getStringISO8859();
        buffer.getInt(); // enumOrdinal
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
        buffer.getStringISO8859(); // enumSimpleName
        buffer.getStringISO8859(); // enumName
        final String enumTypeList = buffer.getStringISO8859();
        buffer.getStringISO8859(); // enumState
        buffer.getInt(); // enumOrdinal

        return enumTypeList;
    }

    @Override
    public WireDataFieldDescription getFieldHeader() {
        final int fieldNameHashCode = buffer.getInt();
        final long dataStartOffset = buffer.position() + buffer.getInt();
        long dataSize = buffer.getInt();
        final byte dataTypeByte = buffer.getByte();
        final String fieldName = buffer.getStringISO8859();
        final DataType dataType = getDataType(dataTypeByte);
        final String fieldUnit;
        if (buffer.position() < dataStartOffset) {
            fieldUnit = buffer.getString();
        } else {
            fieldUnit = null;
        }
        final String fieldDescription;
        if (buffer.position() < dataStartOffset) {
            fieldDescription = buffer.getString();
        } else {
            fieldDescription = null;
        }
        final String fieldDirection;
        if (buffer.position() < dataStartOffset) {
            fieldDirection = buffer.getString();
        } else {
            fieldDirection = null;
        }
        final String fieldGroups;
        if (buffer.position() < dataStartOffset) {
            fieldGroups = buffer.getString();
        } else {
            fieldGroups = null;
        }
        if (buffer.position() != dataStartOffset) {
            final long diff = dataStartOffset - buffer.position();
            throw new IllegalStateException("could not parse FieldHeader: fieldName='" + fieldName + "'" //
                                            + " buffer position = " + buffer.position() + " vs. " + dataStartOffset + " diff = " + diff + " bytes");
        }

        if (dataType.isScalar() && dataSize < 0) {
            if (dataType == DataType.STRING) {
                final long pos = buffer.position();
                dataSize = FastByteBuffer.SIZE_OF_INT + buffer.getInt(); // <(>string size -1> + <string byte data>
                buffer.position(pos);
            } else {
                dataSize = dataType.getPrimitiveSize();
            }
        }

        if (parent == null) {
            parent = lastFieldHeader = new WireDataFieldDescription(null, fieldNameHashCode, fieldName, dataType, dataStartOffset, dataSize);
        } else {
            lastFieldHeader = new WireDataFieldDescription(parent, fieldNameHashCode, fieldName, dataType, dataStartOffset, dataSize);
        }
        return lastFieldHeader;
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
        buffer.getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException("protocol mismatch nElements header = " + nElements + " vs. array = " + values.length);
        }
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
        buffer.getArraySizeDescriptor();
        final int nElements = buffer.getInt();
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

    @Override
    public <E> Queue<E> getQueue(final Queue<E> collection) {
        buffer.getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException("protocol mismatch nElements header = " + nElements + " vs. array = " + values.length);
        }
        final Queue<E> retCollection = collection == null ? new PriorityQueue<>(nElements) : collection;
        for (final Object value : values) {
            retCollection.add((E) value);
        }

        return retCollection;
    }

    @Override
    public <E> Set<E> getSet(final Set<E> collection) {
        buffer.getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType valueDataType = getDataType(buffer.getByte());

        // read value vector
        final Object[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException("protocol mismatch nElements header = " + nElements + " vs. array = " + values.length);
        }
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

    /**
     * @return {@code true} the ISO-8859-1 character encoding is being enforced for data fields (better performance), otherwise UTF-8 is being used (more generic encoding)
     */
    public boolean isEnforceSimpleStringEncoding() {
        return buffer.isEnforceSimpleStringEncoding();
    }

    /**
     *
     * @param state, {@code true} the ISO-8859-1 character encoding is being enforced for data fields (better performance), otherwise UTF-8 is being used (more generic encoding)
     */
    public void setEnforceSimpleStringEncoding(final boolean state) {
        buffer.setEnforceSimpleStringEncoding(state);
    }

    @Override
    public WireDataFieldDescription parseIoStream() {
        final WireDataFieldDescription fieldRoot = new WireDataFieldDescription(null, "ROOT".hashCode(), "ROOT", DataType.OTHER, buffer.position(), -1);
        final ProtocolInfo headerRoot = checkHeaderInfo();
        headerRoot.setParent(fieldRoot);
        fieldRoot.getChildren().add(headerRoot);

        parseIoStream(headerRoot, 0);
        return fieldRoot;
    }

    @Override
    public void put(final boolean value) {
        buffer.putBoolean(value);
    }

    @Override
    public void put(final boolean[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final boolean[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putBooleanArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void put(final byte value) {
        buffer.putByte(value);
    }

    @Override
    public void put(final byte[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final byte[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putByteArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void put(final char value) {
        buffer.putChar(value);
    }

    @Override
    public void put(final char[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final char[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putCharArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public <E> void put(final Collection<E> collection) {
        final Object[] values = collection.toArray();
        final int nElements = collection.size();
        final DataType valueDataType = nElements == 0 ? DataType.OTHER : DataType.fromClassType(values[0].getClass());
        final int entrySize = 17; // as an initial estimate
        final DataType dataType;
        if (collection instanceof Queue) {
            dataType = DataType.QUEUE;
        } else if (collection instanceof List) {
            dataType = DataType.LIST;
        } else if (collection instanceof Set) {
            dataType = DataType.SET;
        } else {
            dataType = DataType.COLLECTION;
        }

        buffer.putArraySizeDescriptor(nElements);
        buffer.ensureAdditionalCapacity((nElements * entrySize) + 9);
        buffer.putByte(getDataType(valueDataType)); // write value element type
        putGenericArrayAsPrimitive(valueDataType, values, 0, nElements);

        updateDataEndMarker();
    }

    @Override
    public void put(final double value) {
        buffer.putDouble(value);
    }

    @Override
    public void put(final double[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final double[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        buffer.putDoubleArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void put(final Enum<?> enumeration) {
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

        buffer.ensureAdditionalCapacity((nElements * entrySize) + 9);
        final String typeList = Arrays.stream(clazz.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
        buffer.putStringISO8859(clazz.getSimpleName());
        buffer.putStringISO8859(enumeration.getClass().getName());
        buffer.putStringISO8859(typeList);
        buffer.putStringISO8859(enumeration.name());
        buffer.putInt(enumeration.ordinal());
        updateDataEndMarker();
    }

    @Override
    public void put(final float value) {
        buffer.putFloat(value);
    }

    @Override
    public void put(final float[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final float[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putFloatArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void put(final int value) {
        buffer.putInt(value);
    }

    @Override
    public void put(final int[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final int[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putIntArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void put(final long value) {
        buffer.putLong(value);
    }

    @Override
    public void put(final long[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final long[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putLongArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public <K, V> void put(final Map<K, V> map) {
        final Object[] keySet = map.keySet().toArray();
        final Object[] valueSet = map.values().toArray();
        final int nElements = keySet.length;
        buffer.putArraySizeDescriptor(nElements);
        final DataType keyDataType = nElements == 0 ? DataType.OTHER : DataType.fromClassType(keySet[0].getClass());
        final DataType valueDataType = nElements == 0 ? DataType.OTHER : DataType.fromClassType(valueSet[0].getClass());
        // convert into two linear arrays one of K and the other for V streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)> <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        final int entrySize = 17; // as an initial estimate

        buffer.ensureAdditionalCapacity((nElements * entrySize) + 9);
        buffer.putByte(getDataType(keyDataType)); // write key element type
        buffer.putByte(getDataType(valueDataType)); // write value element type
        putGenericArrayAsPrimitive(keyDataType, keySet, 0, nElements);
        putGenericArrayAsPrimitive(valueDataType, valueSet, 0, nElements);

        updateDataEndMarker();
    }

    @Override
    public void put(final short value) { // NOPMD
        buffer.putShort(value);
    }

    @Override
    public void put(final short[] arrayValue) { // NOPMD
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final short[] arrayValue, // NOPMD
            final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        buffer.putShortArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void put(final String value) {
        buffer.ensureAdditionalCapacity((value == null ? 1 : value.length()) + 1);
        buffer.putString(value == null ? "" : value);
    }

    @Override
    public void put(final String[] arrayValue) {
        put(arrayValue, new int[] { arrayValue == null ? 0 : arrayValue.length });
    }

    @Override
    public void put(final String[] arrayValue, final int[] dims) {
        if (arrayValue == null) {
            return;
        }
        final int nElements = Math.min(getNumberOfElements(dims), arrayValue.length);
        final long addCapacity = Arrays.stream(arrayValue).map(s -> s.length() + 1).reduce(0, Integer::sum);
        buffer.ensureAdditionalCapacity(addCapacity);
        buffer.putStringArray(arrayValue, 0, dims);
        updateDataEndMarker();
    }

    @Override
    public void putEndMarker(final String markerName) {
        putFieldHeader(markerName, DataType.END_MARKER);
        buffer.putByte(getDataType(DataType.END_MARKER));
    }

    @Override
    public void putFieldHeader(final FieldDescription fieldDescription) {
        final DataType dataType = fieldDescription.getDataType();
        putFieldHeader(fieldDescription.getFieldName(), dataType, dataType.getPrimitiveSize());
    }

    @Override
    public void putFieldHeader(final String fieldName, final DataType dataType) {
        putFieldHeader(fieldName, dataType, 0);
    }

    @Override
    public void putFieldHeader(final String fieldName, final DataType dataType, final int additionalSize) {
        //        final long addCapacity = ((fieldName.length() + 1 + 4 + 1) * SIZE_OF_BYTE) + bufferIncrements
        //                                 + dataType.getPrimitiveSize() + additionalSize;
        //        buffer.ensureAdditionalCapacity(addCapacity);

        // -- offset 0 vs. field start
        buffer.putInt(fieldName.hashCode()); // unique hashCode identifier -- TODO: unify across C++/Java & optimise performance
        final long fieldHeaderStart = buffer.position();
        buffer.putInt(-1); // dataStart -- offset
        fieldHeaderDataEndMarkerPosition = buffer.position();
        buffer.putInt(-1); // dataSize (N.B. 'dataStart + dataSize' == start of next field header
        buffer.putByte(getDataType(dataType)); // data type ID
        buffer.putStringISO8859(fieldName); // full field name
        buffer.putString("a.u."); // field unit -- TODO: change to proper definiton
        buffer.putString("field description"); // field description -- TODO: change to proper definiton
        buffer.putString("field in/out direction"); // field direction -- TODO: change to proper definiton
        buffer.putString("field groups"); // field direction -- TODO: change to proper definiton
        // -- offset dataStart
        fieldHeaderDataStart = buffer.position();
        final long diff = fieldHeaderDataStart - fieldHeaderStart;
        buffer.position(fieldHeaderStart);
        buffer.putInt((int) diff); // write offset to dataStart
        if (dataType.isScalar() && !dataType.equals(DataType.STRING)) {
            final long nBytesToRead = dataType.getPrimitiveSize();
            buffer.putInt((int) (nBytesToRead)); // write offset from dataStart to dataEnd
        }
        buffer.position(fieldHeaderDataStart);
        // from hereon there are data specific structures
    }

    public void putGenericArrayAsPrimitive(final DataType dataType, final Object[] data, final int offset, final int nToCopy) {
        switch (dataType) {
        case BOOL:
            buffer.putBooleanArray(GenericsHelper.toBoolPrimitive(data), offset, nToCopy);
            break;
        case BYTE:
            buffer.putByteArray(GenericsHelper.toBytePrimitive(data), offset, nToCopy);
            break;
        case CHAR:
            buffer.putCharArray(GenericsHelper.toCharPrimitive(data), offset, nToCopy);
            break;
        case SHORT:
            buffer.putShortArray(GenericsHelper.toShortPrimitive(data), offset, nToCopy);
            break;
        case INT:
            buffer.putIntArray(GenericsHelper.toIntegerPrimitive(data), offset, nToCopy);
            break;
        case LONG:
            buffer.putLongArray(GenericsHelper.toLongPrimitive(data), offset, nToCopy);
            break;
        case FLOAT:
            buffer.putFloatArray(GenericsHelper.toFloatPrimitive(data), offset, nToCopy);
            break;
        case DOUBLE:
            buffer.putDoubleArray(GenericsHelper.toDoublePrimitive(data), offset, nToCopy);
            break;
        case STRING:
            buffer.putStringArray(GenericsHelper.toStringPrimitive(data), offset, nToCopy);
            break;
        case OTHER:
            // TODO: write generics implementation: idea: look-up for existing serialiser
            break;
        default:
            throw new IllegalArgumentException("type not implemented - " + data[0].getClass().getSimpleName());
        }
    }

    @Override
    public void putHeaderInfo() {
        final long addCapacity = 20 + "OBJ_ROOT_START".length() + "#file producer : ".length() + BinarySerialiser.class.getCanonicalName().length();
        buffer.ensureAdditionalCapacity(addCapacity);
        putStartMarker("OBJ_ROOT_START");
        buffer.putStringISO8859("#file producer : ");
        buffer.putStringISO8859(BinarySerialiser.class.getCanonicalName());
        buffer.putStringISO8859("\n");
        buffer.putByte(VERSION_MAJOR);
        buffer.putByte(VERSION_MINOR);
        buffer.putByte(VERSION_MICRO);
        updateDataEndMarker();
    }

    @Override
    public void putStartMarker(final String markerName) {
        putFieldHeader(markerName, DataType.START_MARKER);
        buffer.putByte(getDataType(DataType.START_MARKER));
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
        case OTHER:
            // TODO: write generics implementation: idea: look-up for existing serialiser
            retVal = new Object[0];
            break;
        // @formatter:on
        default:
            throw new IllegalArgumentException("type not implemented - " + dataType);
        }
        return retVal;
    }

    protected static int getNumberOfElements(final int[] dimensions) {
        AssertUtils.notNull("dimensions", dimensions);
        int ret = 1;
        for (int dim : dimensions) {
            ret *= dim;
        }
        return ret;
    }

    protected void parseIoStream(final WireDataFieldDescription fieldRoot, final int recursionDepth) {
        WireDataFieldDescription field;
        while ((field = getFieldHeader()) != null) {
            final long bytesToSkip = field.getDataSize();
            final long skipPosition = field.getDataStartOffset() + bytesToSkip;
            field.setParent(fieldRoot);

            // reached end of (sub-)class - check marker value and close nested hierarchy
            if (field.getDataType() == DataType.END_MARKER) {
                // reached end of (sub-)class - check marker value and close nested hierarchy
                final byte markerValue = buffer.getByte();
                final FieldDescription superParent = fieldRoot.getParent();
                if (getDataType(DataType.END_MARKER) != markerValue) {
                    throw new IllegalStateException("reached end marker, mismatched value '" + markerValue + "' vs. should '" + getDataType(DataType.END_MARKER) + "'");
                }

                if (superParent == null) {
                    fieldRoot.getChildren().add(field);
                } else {
                    superParent.getChildren().add(field);
                }
                break;
            }

            fieldRoot.getChildren().add(field);

            if (bytesToSkip < 0) {
                LOGGER.atWarn().addArgument(field.getFieldName()).addArgument(field.getDataType()).addArgument(bytesToSkip).log("WireDataFieldDescription for '{}' type '{}' has bytesToSkip '{} <= 0'");

                // fall-back option in case of
                swallowRest(field);
            } else {
                buffer.position(skipPosition);
            }

            // detected sub-class start marker
            // check marker value
            if (field.getDataType() == DataType.START_MARKER) {
                buffer.position(field.getDataStartOffset());
                // detected sub-class start marker
                // check marker value
                final byte markerValue = buffer.getByte();
                if (getDataType(DataType.START_MARKER) != markerValue) {
                    throw new IllegalStateException("reached start marker, mismatched value '" + markerValue + "' vs. should '" + getDataType(DataType.START_MARKER) + "'");
                }

                parseIoStream(field, recursionDepth + 1);
            }
        }
    }

    @SuppressWarnings("PMD.NcssCount")
    protected void swallowRest(final FieldDescription fieldDescription) {
        // parse whatever is left
        // N.B. this is/should be the only place where 'Object' is used since the JVM will perform boxing of primitive types
        // automatically. Boxing and later un-boxing is a significant high-performance bottleneck for any serialiser
        Object leftOver;
        int size = -1;
        switch (fieldDescription.getDataType()) {
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
            throw new IllegalArgumentException("encountered unknown format for " + fieldDescription.toString());
        }

        if (buffer.position() >= buffer.capacity()) {
            throw new IllegalStateException("read beyond buffer capacity, position = " + buffer.position() + " vs capacity = " + buffer.capacity());
        }

        LOGGER.atTrace().addArgument(fieldDescription).addArgument(leftOver).addArgument(size).log("swallowed unused element '{}'='{}' size = {}");
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

        throw new IllegalArgumentException("DataType byteValue=" + byteValue + " rawByteValue=" + (byteValue & 0xFF) + " not mapped");
    }
}
