package de.gsi.serializer.spi;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.utils.AssertUtils;
import de.gsi.dataset.utils.GenericsHelper;
import de.gsi.serializer.DataType;
import de.gsi.serializer.FieldDescription;
import de.gsi.serializer.FieldSerialiser;
import de.gsi.serializer.IoBuffer;
import de.gsi.serializer.IoSerialiser;
import de.gsi.serializer.utils.ClassUtils;

/**
 * YaS -- Yet another Serialiser implementation
 *
 * Generic binary serialiser aimed at efficiently transferring data between server/client and in particular between
 * Java/C++/web-based programs. For rationale see IoSerialiser.md description.
 * 
 * <p>
 * There are two default backing buffer implementations ({@link FastByteBuffer FastByteBuffer} and {@link ByteBuffer ByteBuffer}),
 * but can be extended/replaced with any other buffer is also possible provided it implements the {@link IoBuffer IoBuffer} interface.
 * 
 * <p>
 * The default serialisable data types are defined in {@link DataType DataType} and include definitions for
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
 * * header info:   [ 4 bytes (int) = 0x0000002A] // magic number used as coarse protocol identifier - precise protocol refined by further fields below
 *                  [ clear text serialiser name: String ] + // ie. "YaS" for 'Yet another Serialiser'
 *                  [ 1 byte - major protocol version ] +
 *                  [ 1 byte - minor protocol version ] +
 *                  [ 1 byte - micro protocol version ] // micro: non API-changing bug fixes in implementation
 *                  [ field header for 'start marker' ] [ 1 byte - uniqueType (0x00) ]
 * * String:        [ 4 bytes (int) - length (including termination) ][ n bytes based on ISO-8859 or UTF-8 encoding ]
 * * field header:  # start field header 'p0'
 *                  [ 1 byte - uniqueType ]
 *                  [ 4 bytes - field name hash code] // enables faster field matching
 *                  [ 4 bytes - dataStart = n bytes until data start] // counted w.r.t. field header start
 *                  [ 4 bytes - dataSize = n bytes for data size]
 *                  [ String (ISO-8859) - field name ]             // optional, if there are no field name hash code collisions
 *                  N.B. following fields are optional (detectable if buffer position smaller than 'p0' + dataStart)
 *                  [ String (UTF-8)    - field unit ]
 *                  [ String (UTF-8)    - field in/out direction ]
 *                  [ String (UTF-8)    - field groups ]
 *                  # start data = 'p0' + dataStart
 *                  ... type specific and/or custom data serialisation
 *                  # end data = 'p0' + dataStart + dataSize
 * * primitives:    [ field header for 'primitive type ID'] + [ 1-8 bytes depending on DataType ]
 * * prim. arrays:  [ array header for 'prim. type array ID'] + [   ]=1-8 bytes x N_i or more - array data depending on variable DataType ]
 * * boxed arrays:  as above but each element cast to corresponding primitive type
 * * array header:  [ field header (as above) ] +
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
 *                      [ 1 byte - uniqueType -- custom class type definition ]
 *                      [ String (ISO-8859) - class type name ]
 *                      [ n bytes - custom serialisation definition ]
 * * start marker:  [ field header for '0x00' ] // dataSize == # bytes until the corresponding end-marker start
 * * end marker:    [ field header for '0xFE' ]
 * 
 * * nesting or sub-structures (ie. POJOs with sub-classes) can be achieved via:
 * [  start marker - field name == nesting context1 ] 
 *   [  start marker - field name == nesting context2 ]
 *    ... 
 *   [  end marker - field name == nesting context2 (optional name) ]
 * [  end marker - field name == nesting context1 (optional name) ]
 * 
 * with
 * T: being a generic list parameter outlined in {@link DataType DataType}
 * K: being a generic key parameter outlined in {@link DataType DataType}
 * V: being a generic value parameter outlined in {@link DataType DataType}
 * </code></pre>
 * 
 * @author rstein
 */
@SuppressWarnings({ "PMD.CommentSize", "PMD.ExcessivePublicCount", "PMD.PrematureDeclaration" }) // variables need to be read from stream
public class BinarySerialiser implements IoSerialiser {
    public static final int VERSION_MAGIC_NUMBER = 0x00000000; // '0' since CmwLight cannot (usually) start with 0 length fields
    public static final String PROTOCOL_NAME = "YaS"; // Yet another Serialiser implementation
    public static final byte VERSION_MAJOR = 1;
    public static final byte VERSION_MINOR = 0;
    public static final byte VERSION_MICRO = 0;
    public static final String PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL = "protocol error: serialiser lookup must not be null for DataType == OTHER";
    public static final String PROTOCOL_MISMATCH_N_ELEMENTS_HEADER = "protocol mismatch nElements header = ";
    public static final String NO_SERIALISER_IMP_FOUND = "no serialiser implementation found for classType = ";
    public static final String VS_ARRAY = " vs. array = ";
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiser.class);
    private static final int ADDITIONAL_HEADER_INFO_SIZE = 1000;
    private static final DataType[] byteToDataType = new DataType[256];
    private static final Byte[] dataTypeToByte = new Byte[256];
    public static final String VS_SHOULD_BE = "' vs. should be '";

    static {
        // static mapping of protocol bytes -- needed to be compatible with other wire protocols
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

    private int bufferIncrements = ADDITIONAL_HEADER_INFO_SIZE;
    private IoBuffer buffer;
    private boolean putFieldMetaData = true;
    private WireDataFieldDescription parent;
    private WireDataFieldDescription lastFieldHeader;

    /**
     * @param buffer the backing IoBuffer (see e.g. {@link FastByteBuffer} or{@link ByteBuffer}
     */
    public BinarySerialiser(final IoBuffer buffer) {
        super();
        this.buffer = buffer;
    }

    @Override
    public ProtocolInfo checkHeaderInfo() {
        final int magicNumber = buffer.getInt();
        if (magicNumber != VERSION_MAGIC_NUMBER) {
            throw new IllegalStateException("byte buffer version magic byte incompatible: received '" + magicNumber + VS_SHOULD_BE + VERSION_MAGIC_NUMBER + "'");
        }
        final String producer = buffer.getStringISO8859();
        if (!PROTOCOL_NAME.equals(producer)) {
            throw new IllegalStateException("byte buffer producer name incompatible: received '" + producer + VS_SHOULD_BE + PROTOCOL_NAME + "'");
        }
        final byte major = buffer.getByte();
        final byte minor = buffer.getByte();
        final byte micro = buffer.getByte();

        final WireDataFieldDescription headerStartField = getFieldHeader();
        final ProtocolInfo header = new ProtocolInfo(this, headerStartField, producer, major, minor, micro);

        if (!header.isCompatible()) {
            final String thisHeader = String.format(" serialiser: %s-v%d.%d.%d", PROTOCOL_NAME, VERSION_MAJOR, VERSION_MINOR, VERSION_MICRO);
            throw new IllegalStateException("byte buffer version incompatible: received '" + header.toString() + VS_SHOULD_BE + thisHeader + "'");
        }
        return header;
    }

    @Override
    public void setQueryFieldName(final String fieldName, final int dataStartPosition) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName must not be null or blank: " + fieldName);
        }
        buffer.position(dataStartPosition);
    }

    @Override
    public int[] getArraySizeDescriptor() {
        final int nDims = buffer.getInt(); // number of dimensions
        final int[] ret = new int[nDims];
        for (int i = 0; i < nDims; i++) {
            ret[i] = buffer.getInt(); // vector size for each dimension
        }
        return ret;
    }

    @Override
    public boolean getBoolean() {
        return buffer.getBoolean();
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getBooleanArray(dst, length);
    }

    public IoBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(final IoBuffer buffer) {
        this.buffer = buffer;
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
        return buffer.getByte();
    }

    @Override
    public byte[] getByteArray(final byte[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getByteArray(dst, length);
    }

    @Override
    public char getChar() {
        return buffer.getChar();
    }

    @Override
    public char[] getCharArray(final char[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getCharArray(dst, length);
    }

    @Override
    public <E> Collection<E> getCollection(final Collection<E> collection, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType collectionType = getDataType(buffer.getByte());
        final DataType valueDataType = getDataType(buffer.getByte());

        final Collection<E> retCollection;
        if (collection != null) {
            retCollection = collection;
            retCollection.clear();
        } else {
            switch (collectionType) {
            case SET:
                retCollection = new HashSet<>(nElements);
                break;
            case QUEUE:
                retCollection = new ArrayDeque<>(nElements);
                break;
            case LIST:
            case COLLECTION:
            default:
                retCollection = new ArrayList<>(nElements);
                break;
            }
        }

        if (DataType.OTHER.equals(valueDataType)) {
            final String classTypeName = buffer.getStringISO8859();
            final String secondaryTypeName = buffer.getStringISO8859();
            final Type classType = ClassUtils.getClassByName(classTypeName);
            final Type[] secondaryType = secondaryTypeName.isEmpty() ? new Type[0] : new Type[] { ClassUtils.getClassByName(secondaryTypeName) };
            if (serialiserLookup == null) {
                throw new IllegalArgumentException(PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL);
            }
            final FieldSerialiser<E> serialiser = serialiserLookup.apply(classType, secondaryType);

            if (serialiser == null) {
                throw new IllegalArgumentException(NO_SERIALISER_IMP_FOUND + classTypeName);
            }
            for (int i = 0; i < nElements; i++) {
                retCollection.add(serialiser.getReturnObjectFunction().apply(this, null, null));
            }

            return retCollection;
        }

        // read primitive or String value vector
        final E[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException(PROTOCOL_MISMATCH_N_ELEMENTS_HEADER + nElements + VS_ARRAY + values.length);
        }
        retCollection.addAll(Arrays.asList(values));

        return retCollection;
    }

    @Override
    public <E> E getCustomData(final FieldSerialiser<E> serialiser) {
        String classType = null;
        String classSecondaryType = null;
        try {
            classType = buffer.getStringISO8859();
            classSecondaryType = buffer.getStringISO8859();
            return serialiser.getReturnObjectFunction().apply(this, null, null);
        } catch (Exception e) { // NOPMD
            LOGGER.atError().setCause(e).addArgument(classType).addArgument(classSecondaryType).log("problems with generic classType: {} classSecondaryType: {}");
            throw e;
        }
    }

    @Override
    public double getDouble() {
        return buffer.getDouble();
    }

    @Override
    public double[] getDoubleArray(final double[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getDoubleArray(dst, length);
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
        Class<?> enumClass = ClassUtils.getClassByName(enumName);
        if (enumClass == null) {
            enumClass = ClassUtils.getClassByName(enumSimpleName);
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
        final int headerStart = buffer.position();
        final byte dataTypeByte = buffer.getByte();
        final int fieldNameHashCode = buffer.getInt();
        final int dataStartOffset = buffer.getInt();
        final int dataStartPosition = headerStart + dataStartOffset;
        int dataSize = buffer.getInt();
        final String fieldName;
        if (buffer.position() < dataStartPosition) {
            fieldName = buffer.getStringISO8859();
        } else {
            fieldName = null;
        }

        final DataType dataType = getDataType(dataTypeByte);
        if (dataType == DataType.END_MARKER) {
            parent = (WireDataFieldDescription) parent.getParent();
        }
        lastFieldHeader = new WireDataFieldDescription(this, parent, fieldNameHashCode, fieldName, dataType, headerStart, dataStartOffset, dataSize);
        if (dataType == DataType.START_MARKER) {
            parent = lastFieldHeader;
        }

        if (this.isPutFieldMetaData()) {
            // optional meta data
            if (buffer.position() < dataStartPosition) {
                lastFieldHeader.setFieldUnit(buffer.getString());
            }
            if (buffer.position() < dataStartPosition) {
                lastFieldHeader.setFieldDescription(buffer.getString());
            }
            if (buffer.position() < dataStartPosition) {
                lastFieldHeader.setFieldDirection(buffer.getString());
            }
            if (buffer.position() < dataStartPosition) {
                final String[] fieldGroups = buffer.getStringArray();
                lastFieldHeader.setFieldGroups(fieldGroups == null ? Collections.emptyList() : Arrays.asList(fieldGroups));
            }
        } else {
            buffer.position(dataStartPosition);
        }

        // check for header-dataStart offset consistency
        if (buffer.position() != dataStartPosition) {
            final int diff = dataStartPosition - buffer.position();
            throw new IllegalStateException("could not parse FieldHeader: fieldName='" + dataType + ":" + fieldName + "' dataOffset = " + dataStartOffset + " bytes (read) -- " //
                                            + " buffer position is " + buffer.position() + " vs. calculated " + dataStartPosition + " diff = " + diff);
        }

        if (dataSize >= 0) {
            return lastFieldHeader;
        }

        // last-minute check in case dataSize hasn't been set correctly
        if (dataType.isScalar()) {
            dataSize = dataType.getPrimitiveSize();
        } else if (dataType == DataType.STRING) {
            // sneak-peak look-ahead to get actual string size
            // N.B. regarding jump size: <(>string size -1> + <string byte data>
            dataSize = buffer.getInt(buffer.position() + FastByteBuffer.SIZE_OF_INT) + FastByteBuffer.SIZE_OF_INT;
        }
        lastFieldHeader.setDataSize(dataSize);

        return lastFieldHeader;
    }

    @Override
    public float getFloat() {
        return buffer.getFloat();
    }

    @Override
    public float[] getFloatArray(final float[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getFloatArray(dst, length);
    }

    @Override
    public int getInt() {
        return buffer.getInt();
    }

    @Override
    public int[] getIntArray(final int[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getIntArray(dst, length);
    }

    @Override
    public <E> List<E> getList(final List<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType listDataType = getDataType(buffer.getByte());
        final DataType valueDataType = getDataType(buffer.getByte());
        if (!listDataType.equals(DataType.LIST) && !listDataType.equals(DataType.COLLECTION)) {
            throw new IllegalArgumentException("dataType incompatible with List = " + listDataType);
        }
        final List<E> retCollection;
        if (collection == null) {
            retCollection = new ArrayList<>();
        } else {
            retCollection = collection;
            retCollection.clear();
        }

        if (DataType.OTHER.equals(valueDataType)) {
            final String classTypeName = buffer.getStringISO8859();
            final String secondaryTypeName = buffer.getStringISO8859();
            final Type classType = ClassUtils.getClassByName(classTypeName);
            final Type[] secondaryType = secondaryTypeName.isEmpty() ? new Type[0] : new Type[] { ClassUtils.getClassByName(secondaryTypeName) };
            if (serialiserLookup == null) {
                throw new IllegalArgumentException(PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL);
            }
            final FieldSerialiser<E> serialiser = serialiserLookup.apply(classType, secondaryType);

            if (serialiser == null) {
                throw new IllegalArgumentException(NO_SERIALISER_IMP_FOUND + classTypeName);
            }
            for (int i = 0; i < nElements; i++) {
                retCollection.add(serialiser.getReturnObjectFunction().apply(this, null, null));
            }

            return retCollection;
        }

        // read primitive or String value vector
        final E[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException(PROTOCOL_MISMATCH_N_ELEMENTS_HEADER + nElements + VS_ARRAY + values.length);
        }
        retCollection.addAll(Arrays.asList(values));

        return retCollection;
    }

    @Override
    public long getLong() {
        return buffer.getLong();
    }

    @Override
    public long[] getLongArray(final long[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getLongArray(dst, length);
    }

    @Override
    public <K, V, E> Map<K, V> getMap(final Map<K, V> map, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        // convert into two linear arrays one of K and the other for V streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)> <Length, [K_0,...,K_length]> <Length, [V_..., V_length]>

        // read key type and key value vector
        final K[] keys;
        final DataType keyDataType = getDataType(buffer.getByte());
        if (keyDataType != DataType.OTHER) {
            keys = getGenericArrayAsBoxedPrimitive(keyDataType);
        } else {
            final String classTypeName = buffer.getStringISO8859();
            final String secondaryTypeName = buffer.getStringISO8859();
            final Type classType = ClassUtils.getClassByName(classTypeName);
            final Type[] secondaryType = secondaryTypeName.isEmpty() ? new Type[0] : new Type[] { ClassUtils.getClassByName(secondaryTypeName) };
            if (serialiserLookup == null) {
                throw new IllegalArgumentException(PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL);
            }
            final FieldSerialiser serialiser = serialiserLookup.apply(classType, secondaryType);
            if (serialiser == null) {
                throw new IllegalArgumentException(NO_SERIALISER_IMP_FOUND + classTypeName);
            }
            keys = (K[]) new Object[nElements];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = (K) serialiser.getReturnObjectFunction().apply(this, null, null);
            }
        }
        // read value type and value vector
        final V[] values;
        final DataType valueDataType = getDataType(buffer.getByte());
        if (valueDataType != DataType.OTHER) {
            values = getGenericArrayAsBoxedPrimitive(valueDataType);
        } else {
            final String classTypeName = buffer.getStringISO8859();
            final String secondaryTypeName = buffer.getStringISO8859();
            final Type classType = ClassUtils.getClassByName(classTypeName);
            final Type[] secondaryType = secondaryTypeName.isEmpty() ? new Type[0] : new Type[] { ClassUtils.getClassByName(secondaryTypeName) };
            if (serialiserLookup == null) {
                throw new IllegalArgumentException(PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL);
            }
            final FieldSerialiser serialiser = serialiserLookup.apply(classType, secondaryType);

            if (serialiser == null) {
                throw new IllegalArgumentException(NO_SERIALISER_IMP_FOUND + classTypeName);
            }
            values = (V[]) new Object[nElements];
            for (int i = 0; i < values.length; i++) {
                values[i] = (V) serialiser.getReturnObjectFunction().apply(this, null, null);
            }
        }

        // generate new/write into existing Map
        final Map<K, V> retMap = map == null ? new ConcurrentHashMap<>() : map;
        if (map != null) {
            map.clear();
        }
        for (int i = 0; i < keys.length; i++) {
            retMap.put(keys[i], values[i]);
        }

        return retMap;
    }

    public WireDataFieldDescription getParent() {
        return parent;
    }

    @Override
    public <E> Queue<E> getQueue(final Queue<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType listDataType = getDataType(buffer.getByte());
        final DataType valueDataType = getDataType(buffer.getByte());
        if (!listDataType.equals(DataType.QUEUE) && !listDataType.equals(DataType.COLLECTION)) {
            throw new IllegalArgumentException("dataType incompatible with Queue = " + listDataType);
        }
        final Queue<E> retCollection;
        if (collection == null) {
            retCollection = new ArrayDeque<>();
        } else {
            retCollection = collection;
            retCollection.clear();
        }

        if (DataType.OTHER.equals(valueDataType)) {
            final String classTypeName = buffer.getStringISO8859();
            final String secondaryTypeName = buffer.getStringISO8859();
            final Type classType = ClassUtils.getClassByName(classTypeName);
            final Type[] secondaryType = secondaryTypeName.isEmpty() ? new Type[0] : new Type[] { ClassUtils.getClassByName(secondaryTypeName) };
            if (serialiserLookup == null) {
                throw new IllegalArgumentException(PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL);
            }
            final FieldSerialiser<E> serialiser = serialiserLookup.apply(classType, secondaryType);

            if (serialiser == null) {
                throw new IllegalArgumentException(NO_SERIALISER_IMP_FOUND + classTypeName);
            }
            for (int i = 0; i < nElements; i++) {
                retCollection.add(serialiser.getReturnObjectFunction().apply(this, null, null));
            }

            return retCollection;
        }

        // read primitive or String value vector
        final E[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException(PROTOCOL_MISMATCH_N_ELEMENTS_HEADER + nElements + VS_ARRAY + values.length);
        }
        retCollection.addAll(Arrays.asList(values));

        return retCollection;
    }

    @Override
    public <E> Set<E> getSet(final Set<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        getArraySizeDescriptor();
        final int nElements = buffer.getInt();
        final DataType listDataType = getDataType(buffer.getByte());
        final DataType valueDataType = getDataType(buffer.getByte());
        if (!listDataType.equals(DataType.SET) && !listDataType.equals(DataType.COLLECTION)) {
            throw new IllegalArgumentException("dataType incompatible with Set = " + listDataType);
        }
        final Set<E> retCollection;
        if (collection == null) {
            retCollection = new HashSet<>();
        } else {
            retCollection = collection;
            retCollection.clear();
        }

        if (DataType.OTHER.equals(valueDataType)) {
            final String classTypeName = buffer.getStringISO8859();
            final String secondaryTypeName = buffer.getStringISO8859();
            final Type classType = ClassUtils.getClassByName(classTypeName);
            final Type[] secondaryType = secondaryTypeName.isEmpty() ? new Type[0] : new Type[] { ClassUtils.getClassByName(secondaryTypeName) };
            if (serialiserLookup == null) {
                throw new IllegalArgumentException(PROTOCOL_ERROR_SERIALISER_LOOKUP_MUST_NOT_BE_NULL);
            }
            final FieldSerialiser<E> serialiser = serialiserLookup.apply(classType, secondaryType);

            if (serialiser == null) {
                throw new IllegalArgumentException(NO_SERIALISER_IMP_FOUND + classTypeName);
            }
            for (int i = 0; i < nElements; i++) {
                retCollection.add(serialiser.getReturnObjectFunction().apply(this, null, null));
            }

            return retCollection;
        }

        // read primitive or String value vector
        final E[] values = getGenericArrayAsBoxedPrimitive(valueDataType);
        if (nElements != values.length) {
            throw new IllegalStateException(PROTOCOL_MISMATCH_N_ELEMENTS_HEADER + nElements + VS_ARRAY + values.length);
        }
        retCollection.addAll(Arrays.asList(values));

        return retCollection;
    }

    @Override
    public short getShort() {
        return buffer.getShort();
    }

    @Override
    public short[] getShortArray(final short[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getShortArray(dst, length);
    }

    @Override
    public String getString() {
        return buffer.getString();
    }

    @Override
    public String[] getStringArray(final String[] dst, final int length) {
        getArraySizeDescriptor();
        return buffer.getStringArray(dst, length);
    }

    @Override
    public String getStringISO8859() {
        return buffer.getStringISO8859();
    }

    /**
     * @return {@code true} the ISO-8859-1 character encoding is being enforced for data fields (better performance), otherwise UTF-8 is being used (more generic encoding)
     */
    public boolean isEnforceSimpleStringEncoding() {
        return buffer.isEnforceSimpleStringEncoding();
    }

    /**
     *
     * @param state {@code true} the ISO-8859-1 character encoding is being enforced for data fields (better performance), otherwise UTF-8 is being used (more generic encoding)
     */
    public void setEnforceSimpleStringEncoding(final boolean state) {
        buffer.setEnforceSimpleStringEncoding(state);
    }

    @Override
    public boolean isPutFieldMetaData() {
        return putFieldMetaData;
    }

    @Override
    public void setPutFieldMetaData(final boolean putFieldMetaData) {
        this.putFieldMetaData = putFieldMetaData;
    }

    @Override
    public WireDataFieldDescription parseIoStream(final boolean readHeader) {
        final WireDataFieldDescription fieldRoot = getRootElement();
        parent = fieldRoot;
        final WireDataFieldDescription headerRoot = readHeader ? checkHeaderInfo().getFieldHeader() : getFieldHeader();
        buffer.position(headerRoot.getDataStartPosition());
        parseIoStream(headerRoot, 0);
        //updateDataEndMarker(fieldRoot);
        return fieldRoot;
    }

    public void parseIoStream(final WireDataFieldDescription fieldRoot, final int recursionDepth) {
        if (fieldRoot.getParent() == null) {
            parent = lastFieldHeader = fieldRoot;
        }
        WireDataFieldDescription field;
        while ((field = getFieldHeader()) != null) {
            final DataType dataType = field.getDataType();
            if (dataType == DataType.END_MARKER) {
                // reached end of (sub-)class - close nested hierarchy
                break;
            }

            if (dataType == DataType.START_MARKER) {
                // detected sub-class start marker
                parseIoStream(field, recursionDepth + 1);
                continue;
            }

            final int dataSize = field.getDataSize();
            if (dataSize < 0) {
                throw new IllegalStateException("FieldDescription for '" + field.getFieldName() + "' type '" + dataType + "' has negative dataSize = " + dataSize);
            }
            final int skipPosition = field.getDataStartPosition() + dataSize;
            buffer.position(skipPosition);
        }
    }

    @Override
    public <E> void put(final FieldDescription fieldDescription, final Collection<E> collection, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final Object[] values = collection.toArray();
        final int nElements = collection.size();
        final Class<?> cleanedType = ClassUtils.getRawType(valueType);
        final DataType valueDataType = DataType.fromClassType(cleanedType);
        final int entrySize = 17; // as an initial estimate
        putArraySizeDescriptor(nElements);
        buffer.putInt(nElements);

        if (collection instanceof Queue) {
            buffer.putByte(getDataType(DataType.QUEUE));
        } else if (collection instanceof Set) {
            buffer.putByte(getDataType(DataType.SET));
        } else if (collection instanceof List) {
            buffer.putByte(getDataType(DataType.LIST));
        } else {
            buffer.putByte(getDataType(DataType.COLLECTION));
        }

        if (ClassUtils.isPrimitiveWrapperOrString(cleanedType) || serialiserLookup == null) {
            buffer.ensureAdditionalCapacity((nElements * entrySize) + 9);
            buffer.putByte(getDataType(valueDataType)); // write value element type
            putGenericArrayAsPrimitive(valueDataType, values, nElements);
        } else {
            buffer.putByte(getDataType(DataType.OTHER)); // write value element type
            final Type[] secondaryType = ClassUtils.getSecondaryType(valueType);
            final FieldSerialiser<E> serialiser = serialiserLookup.apply(valueType, secondaryType);
            if (serialiser == null) {
                throw new IllegalArgumentException("could not find serialiser for class type " + valueType);
            }
            buffer.putStringISO8859(serialiser.getClassPrototype().getCanonicalName()); // primary type
            buffer.putStringISO8859(serialiser.getGenericsPrototypes().isEmpty() ? "" : serialiser.getGenericsPrototypes().get(0).getTypeName()); // secondary type if any

            final FieldSerialiser.TriConsumer writerFunction = serialiser.getWriterFunction();
            for (final Object value : values) {
                writerFunction.accept(this, value, null);
            }
        }

        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final Enum<?> enumeration) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
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

        updateDataEndMarker(fieldHeader);
    }

    @Override
    public <K, V, E> void put(final FieldDescription fieldDescription, final Map<K, V> map, Type keyType, Type valueType, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final Object[] keySet = map.keySet().toArray();
        final int nElements = keySet.length;
        putArraySizeDescriptor(nElements);
        buffer.putInt(nElements);

        // convert into two linear arrays one of K and the other for V streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)> <Length, [K_0,...,K_length]> <Length, [V_..., V_length]>

        final Class<?> cleanedKeyType = ClassUtils.getRawType(keyType);
        final DataType keyDataType = DataType.fromClassType(cleanedKeyType);
        if (serialiserLookup == null || ClassUtils.isPrimitiveWrapperOrString(cleanedKeyType)) {
            final int entrySize = 17; // as an initial estimate
            buffer.ensureAdditionalCapacity((nElements * entrySize) + 9);
            buffer.putByte(getDataType(keyDataType)); // write key element type
            putGenericArrayAsPrimitive(keyDataType, keySet, nElements);
        } else {
            // write key type
            buffer.putByte(getDataType(DataType.OTHER)); // write key element type
            final Type[] secondaryKeyType = ClassUtils.getSecondaryType(keyType);
            final FieldSerialiser<E> serialiserKey = serialiserLookup.apply(keyType, secondaryKeyType);
            if (serialiserKey == null) {
                throw new IllegalArgumentException("could not find serialiser for key class type " + keyType);
            }
            buffer.putStringISO8859(serialiserKey.getClassPrototype().getCanonicalName()); // primary type
            buffer.putStringISO8859(serialiserKey.getGenericsPrototypes().isEmpty() ? "" : serialiserKey.getGenericsPrototypes().get(0).getTypeName()); // secondary key type if any
            // write key data
            final FieldSerialiser.TriConsumer writerFunctionKey = serialiserKey.getWriterFunction();
            for (final Object key : keySet) {
                writerFunctionKey.accept(this, key, null);
            }
        }

        final Class<?> cleanedValueType = ClassUtils.getRawType(valueType);
        final Object[] valueSet = map.values().toArray();
        final DataType valueDataType = DataType.fromClassType(cleanedValueType);
        if (serialiserLookup == null || ClassUtils.isPrimitiveWrapperOrString(cleanedValueType)) {
            final int entrySize = 17; // as an initial estimate
            buffer.ensureAdditionalCapacity((nElements * entrySize) + 9);
            buffer.putByte(getDataType(valueDataType)); // write value element type
            putGenericArrayAsPrimitive(valueDataType, valueSet, nElements);
        } else {
            // write value type
            buffer.putByte(getDataType(DataType.OTHER)); // write key element type
            final Type[] secondaryValueType = ClassUtils.getSecondaryType(valueType);
            final FieldSerialiser<E> serialiserValue = serialiserLookup.apply(valueType, secondaryValueType);
            if (serialiserValue == null) {
                throw new IllegalArgumentException("could not find serialiser for value class type " + valueType);
            }
            buffer.putStringISO8859(serialiserValue.getClassPrototype().getCanonicalName()); // primary type
            buffer.putStringISO8859(serialiserValue.getGenericsPrototypes().isEmpty() ? "" : serialiserValue.getGenericsPrototypes().get(0).getTypeName()); // secondary key type if any

            // write key data
            final FieldSerialiser.TriConsumer writerFunctionValue = serialiserValue.getWriterFunction();
            for (final Object value : valueSet) {
                writerFunctionValue.accept(this, value, null);
            }
        }
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public <E> void put(final String fieldName, final Collection<E> collection, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        final DataType dataType;
        if (collection instanceof Queue) {
            dataType = DataType.QUEUE;
        } else if (collection instanceof Set) {
            dataType = DataType.SET;
        } else if (collection instanceof List) {
            dataType = DataType.LIST;
        } else {
            dataType = DataType.COLLECTION;
        }

        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, dataType);
        this.put((FieldDescription) null, collection, valueType, serialiserLookup);
        this.updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final Enum<?> enumeration) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.ENUM);
        this.put((FieldDescription) null, enumeration);
        this.updateDataEndMarker(fieldHeader);
    }

    @Override
    public <K, V, E> void put(final String fieldName, final Map<K, V> map, final Type keyType, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.MAP);
        this.put((FieldDescription) null, map, keyType, valueType, serialiserLookup);
        this.updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final boolean value) {
        this.putFieldHeader(fieldDescription);
        buffer.putBoolean(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final boolean[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putBooleanArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final boolean[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putBooleanArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final byte value) {
        this.putFieldHeader(fieldDescription);
        buffer.putByte(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final byte[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putByteArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final byte[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putByteArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final char value) {
        this.putFieldHeader(fieldDescription);
        buffer.putChar(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final char[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putCharArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final char[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putCharArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final double value) {
        this.putFieldHeader(fieldDescription);
        buffer.putDouble(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final double[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putDoubleArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final double[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putDoubleArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final float value) {
        this.putFieldHeader(fieldDescription);
        buffer.putFloat(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final float[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putFloatArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final float[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putFloatArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final int value) {
        this.putFieldHeader(fieldDescription);
        buffer.putInt(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final int[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putIntArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final int[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putIntArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final long value) {
        this.putFieldHeader(fieldDescription);
        buffer.putLong(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final long[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putLongArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final long[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putLongArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final short value) { // NOPMD by rstein
        this.putFieldHeader(fieldDescription);
        buffer.putShort(value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final short[] values, final int n) { // NOPMD by rstein
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putShortArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final short[] values, final int[] dims) { // NOPMD by rstein
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putShortArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final String string) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        buffer.putString(string);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final String[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int valuesSize = values == null ? 0 : values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        putArraySizeDescriptor(nElements);
        buffer.putStringArray(values, nElements);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final String[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldDescription);
        final int nElements = putArraySizeDescriptor(dims);
        putArraySizeDescriptor(nElements);
        buffer.putStringArray(values, nElements);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final boolean value) {
        this.putFieldHeader(fieldName, DataType.BOOL);
        buffer.putBoolean(value);
    }

    @Override
    public void put(final String fieldName, final boolean[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.BOOL_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putBooleanArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final boolean[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.BOOL_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putBooleanArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final byte value) {
        this.putFieldHeader(fieldName, DataType.BYTE);
        buffer.putByte(value);
    }

    @Override
    public void put(final String fieldName, final byte[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.BYTE_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putByteArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final byte[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.BYTE_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putByteArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final char value) {
        this.putFieldHeader(fieldName, DataType.CHAR);
        buffer.putChar(value);
    }

    @Override
    public void put(final String fieldName, final char[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.CHAR_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putCharArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final char[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.CHAR_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putCharArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final double value) {
        this.putFieldHeader(fieldName, DataType.DOUBLE);
        buffer.putDouble(value);
    }

    @Override
    public void put(final String fieldName, final double[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.DOUBLE_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putDoubleArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final double[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.DOUBLE_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putDoubleArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final float value) {
        this.putFieldHeader(fieldName, DataType.FLOAT);
        buffer.putFloat(value);
    }

    @Override
    public void put(final String fieldName, final float[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.FLOAT_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putFloatArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final float[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.FLOAT_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putFloatArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final int value) {
        this.putFieldHeader(fieldName, DataType.INT);
        buffer.putInt(value);
    }

    @Override
    public void put(final String fieldName, final int[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.INT_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putIntArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final int[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.INT_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putIntArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final long value) {
        this.putFieldHeader(fieldName, DataType.LONG);
        buffer.putLong(value);
    }

    @Override
    public void put(final String fieldName, final long[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.LONG_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putLongArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final long[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.LONG_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putLongArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final short value) { // NOPMD by rstein
        this.putFieldHeader(fieldName, DataType.SHORT);
        buffer.putShort(value);
    }

    @Override
    public void put(final String fieldName, final short[] values, final int n) { // NOPMD by rstein
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.SHORT_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int bytesToCopy = putArraySizeDescriptor(n >= 0 ? Math.min(n, valuesSize) : valuesSize);
        buffer.putShortArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final short[] values, final int[] dims) { // NOPMD by rstein
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.SHORT_ARRAY);
        final int bytesToCopy = putArraySizeDescriptor(dims);
        buffer.putShortArray(values, bytesToCopy);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final String string) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.STRING);
        buffer.putString(string);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final String[] values, final int n) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.STRING_ARRAY);
        final int valuesSize = values == null ? 0 : values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        putArraySizeDescriptor(nElements);
        buffer.putStringArray(values, nElements);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public void put(final String fieldName, final String[] values, final int[] dims) {
        final WireDataFieldDescription fieldHeader = putFieldHeader(fieldName, DataType.STRING_ARRAY);
        final int nElements = putArraySizeDescriptor(dims);
        putArraySizeDescriptor(nElements);
        buffer.putStringArray(values, nElements);
        updateDataEndMarker(fieldHeader);
    }

    @Override
    public int putArraySizeDescriptor(final int n) {
        buffer.putInt(1); // number of dimensions
        buffer.putInt(n); // vector size for each dimension
        return n;
    }

    @Override
    public int putArraySizeDescriptor(final int[] dims) {
        buffer.putInt(dims.length); // number of dimensions
        int nElements = 1;
        for (final int dim : dims) {
            nElements *= dim;
            buffer.putInt(dim); // vector size for each dimension
        }
        return nElements;
    }

    @Override
    public <E> WireDataFieldDescription putCustomData(final FieldDescription fieldDescription, final E rootObject, Class<? extends E> type, final FieldSerialiser<E> serialiser) {
        if (parent == null) {
            parent = lastFieldHeader = getRootElement();
        }
        final WireDataFieldDescription oldParent = parent;
        final WireDataFieldDescription ret = putFieldHeader(fieldDescription);
        buffer.putByte(ret.getFieldStart(), getDataType(DataType.OTHER));
        parent = lastFieldHeader;
        // write generic class description and type arguments (if any) to aid reconstruction
        buffer.putStringISO8859(serialiser.getClassPrototype().getCanonicalName()); // primary type
        buffer.putStringISO8859(serialiser.getGenericsPrototypes().isEmpty() ? "" : serialiser.getGenericsPrototypes().get(0).getTypeName()); // secondary type if any
        serialiser.getWriterFunction().accept(this, rootObject, fieldDescription instanceof ClassFieldDescription ? (ClassFieldDescription) fieldDescription : null);
        putEndMarker(fieldDescription);
        parent = oldParent;
        return ret;
    }

    @Override
    public void putEndMarker(final FieldDescription fieldDescription) {
        updateDataEndMarker(parent);
        updateDataEndMarker(lastFieldHeader);
        if (parent.getParent() != null) {
            parent = (WireDataFieldDescription) parent.getParent();
        }

        putFieldHeader(fieldDescription);
        buffer.putByte(lastFieldHeader.getFieldStart(), getDataType(DataType.END_MARKER));
    }

    @Override
    public WireDataFieldDescription putFieldHeader(final FieldDescription fieldDescription) {
        if (fieldDescription == null) {
            // early return
            return null;
        }
        final DataType dataType = fieldDescription.getDataType();
        if (isPutFieldMetaData()) {
            buffer.ensureAdditionalCapacity(bufferIncrements);
        }
        final boolean isScalar = dataType.isScalar();

        // -- offset 0 vs. field start
        final int headerStart = buffer.position();
        buffer.putByte(getDataType(dataType)); // data type ID
        buffer.putInt(fieldDescription.getFieldNameHashCode());
        buffer.putInt(-1); // dataStart offset
        final int dataSize = isScalar ? dataType.getPrimitiveSize() : -1;
        buffer.putInt(dataSize); // dataSize (N.B. 'headerStart' + 'dataStart + dataSize' == start of next field header
        buffer.putStringISO8859(fieldDescription.getFieldName()); // full field name

        if (isPutFieldMetaData() && fieldDescription.isAnnotationPresent() && dataType != DataType.END_MARKER) {
            buffer.putString(fieldDescription.getFieldUnit());
            buffer.putString(fieldDescription.getFieldDescription());
            buffer.putString(fieldDescription.getFieldDirection());
            final String[] groups = fieldDescription.getFieldGroups().toArray(new String[0]);
            buffer.putStringArray(groups, groups.length);
        }

        // -- offset dataStart calculations
        final int dataStartOffset = buffer.position() - headerStart;
        buffer.putInt(headerStart + 5, dataStartOffset); // write offset to dataStart

        // from hereon there are data specific structures
        buffer.ensureAdditionalCapacity(16); // allocate 16 bytes to account for potential array header (safe-bet)

        lastFieldHeader = new WireDataFieldDescription(this, parent, fieldDescription.getFieldNameHashCode(), fieldDescription.getFieldName(), dataType, headerStart, dataStartOffset, dataSize);
        if (isPutFieldMetaData() && fieldDescription.isAnnotationPresent()) {
            lastFieldHeader.setFieldUnit(fieldDescription.getFieldUnit());
            lastFieldHeader.setFieldDescription(fieldDescription.getFieldDescription());
            lastFieldHeader.setFieldDirection(fieldDescription.getFieldDirection());
            lastFieldHeader.setFieldGroups(fieldDescription.getFieldGroups());
        }
        return lastFieldHeader;
    }

    @Override
    public WireDataFieldDescription putFieldHeader(final String fieldName, final DataType dataType) {
        final int addCapacity = ((fieldName.length() + 18) * FastByteBuffer.SIZE_OF_BYTE) + bufferIncrements + dataType.getPrimitiveSize();
        buffer.ensureAdditionalCapacity(addCapacity);
        final boolean isScalar = dataType.isScalar();

        // -- offset 0 vs. field start
        final int headerStart = buffer.position();
        buffer.putByte(getDataType(dataType)); // data type ID
        buffer.putInt(fieldName.hashCode()); // unique hashCode identifier -- TODO: unify across C++/Java & optimise performance
        buffer.putInt(-1); // dataStart offset
        final int dataSize = isScalar ? dataType.getPrimitiveSize() : -1;
        buffer.putInt(dataSize); // dataSize (N.B. 'headerStart' + 'dataStart + dataSize' == start of next field header
        buffer.putStringISO8859(fieldName); // full field name

        // this putField method cannot add meta-data use 'putFieldHeader(final FieldDescription fieldDescription)' instead

        // -- offset dataStart calculations
        final int fieldHeaderDataStart = buffer.position();
        final int dataStartOffset = (fieldHeaderDataStart - headerStart);
        buffer.putInt(headerStart + 5, dataStartOffset); // write offset to dataStart

        // from hereon there are data specific structures
        buffer.ensureAdditionalCapacity(16); // allocate 16 bytes to account for potential array header (safe-bet)

        lastFieldHeader = new WireDataFieldDescription(this, parent, fieldName.hashCode(), fieldName, dataType, headerStart, dataStartOffset, dataSize);
        return lastFieldHeader;
    }

    public void putGenericArrayAsPrimitive(final DataType dataType, final Object[] data, final int nToCopy) {
        putArraySizeDescriptor(nToCopy);
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
        case OTHER:
            break;
        default:
            throw new IllegalArgumentException("type not implemented - " + data[0].getClass().getSimpleName());
        }
    }

    @Override
    public void putHeaderInfo(final FieldDescription... field) {
        parent = lastFieldHeader = getRootElement();

        buffer.ensureAdditionalCapacity(ADDITIONAL_HEADER_INFO_SIZE);
        buffer.putInt(VERSION_MAGIC_NUMBER);
        buffer.putStringISO8859(PROTOCOL_NAME);
        buffer.putByte(VERSION_MAJOR);
        buffer.putByte(VERSION_MINOR);
        buffer.putByte(VERSION_MICRO);
        if (field.length == 0 || field[0] == null) {
            putStartMarker(new WireDataFieldDescription(this, null, "OBJ_ROOT_START".hashCode(), "OBJ_ROOT_START", DataType.START_MARKER, -1, -1, -1));
        } else {
            putStartMarker(field[0]);
        }
    }

    @Override
    public void putStartMarker(final FieldDescription fieldDescription) {
        putFieldHeader(fieldDescription);
        buffer.putByte(lastFieldHeader.getFieldStart(), getDataType(DataType.START_MARKER));

        parent = lastFieldHeader;
    }

    @Override
    public void updateDataEndMarker(final WireDataFieldDescription fieldHeader) {
        if (fieldHeader == null) {
            // N.B. early return in case field header hasn't been written
            return;
        }
        final int sizeMarkerEnd = buffer.position();
        if (isPutFieldMetaData() && sizeMarkerEnd >= buffer.capacity()) {
            throw new IllegalStateException("buffer position " + sizeMarkerEnd + " is beyond buffer capacity " + buffer.capacity());
        }

        final int dataSize = sizeMarkerEnd - fieldHeader.getDataStartPosition();
        if (fieldHeader.getDataSize() != dataSize) {
            final int headerStart = fieldHeader.getFieldStart();
            fieldHeader.setDataSize(dataSize);
            buffer.putInt(headerStart + 9, dataSize); // 9 bytes = 1 byte for dataType, 4 bytes for fieldNameHashCode, 4 bytes for dataOffset
        }
    }

    protected <E> E[] getGenericArrayAsBoxedPrimitive(final DataType dataType) {
        final Object[] retVal;
        getArraySizeDescriptor();
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
        default:
            throw new IllegalArgumentException("type not implemented - " + dataType);
        }
        return (E[]) retVal;
    }

    private WireDataFieldDescription getRootElement() {
        final int headerOffset = 1 + PROTOCOL_NAME.length() + 3; // unique byte + protocol length + 3 x byte for version
        return new WireDataFieldDescription(this, null, "ROOT".hashCode(), "ROOT", DataType.OTHER, buffer.position() + headerOffset, -1, -1);
    }

    public static byte getDataType(final DataType dataType) {
        final int id = dataType.getID();
        if (dataTypeToByte[id] != null) {
            return dataTypeToByte[id];
        }

        throw new IllegalArgumentException("DataType " + dataType + " not mapped to specific byte");
    }

    public static DataType getDataType(final byte byteValue) {
        final int id = byteValue & 0xFF;
        if (dataTypeToByte[id] != null) {
            return byteToDataType[id];
        }

        throw new IllegalArgumentException("DataType byteValue=" + byteValue + " rawByteValue=" + (byteValue & 0xFF) + " not mapped");
    }
}
