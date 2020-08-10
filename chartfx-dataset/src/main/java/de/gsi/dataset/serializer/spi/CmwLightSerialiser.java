package de.gsi.dataset.serializer.spi;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.FieldDescription;
import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.IoSerialiser;

/**
 * Light-weight open-source implementation of a (de-)serialiser that is binary-compatible to the serialiser used by CMW,
 * a proprietary closed-source middle-ware used in some accelerator laboratories.
 *
 * N.B. this implementation is intended only for performance/functionality comparison and to enable a backward compatible
 * transition to the {@link de.gsi.dataset.serializer.spi.BinarySerialiser} implementation which is a bit more flexible,
 * has some additional (optional) features, and a better IO performance. See the corresponding benchmarks for details;
 *
 * @author rstein
 */
public class CmwLightSerialiser implements IoSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmwLightSerialiser.class);
    public static final String NOT_IMPLEMENTED = "not implemented";
    private static final int ADDITIONAL_HEADER_INFO_SIZE = 1000;
    private static final DataType[] byteToDataType = new DataType[256];
    private static final Byte[] dataTypeToByte = new Byte[256];

    static {
        // static mapping of protocol bytes -- needed to be compatible with other wire protocols
        // N.B. CmwLightSerialiser does not implement mappings for:
        // * discreteFunction
        // * discreteFunction_list
        // and does not distinguish between 1D, 2D, or multi-dim arrays which share the same wire-encoding anyway
        // CMW's 'Data' object is mapped to START_MARKER also used for nested data structures

        byteToDataType[0] = DataType.BOOL;
        byteToDataType[1] = DataType.BYTE;
        byteToDataType[2] = DataType.SHORT;
        byteToDataType[3] = DataType.INT;
        byteToDataType[4] = DataType.LONG;
        byteToDataType[5] = DataType.FLOAT;
        byteToDataType[6] = DataType.DOUBLE;
        byteToDataType[201] = DataType.CHAR; // not actually implemented by CMW
        byteToDataType[7] = DataType.STRING;
        byteToDataType[8] = DataType.START_MARKER; // mapped to CMW 'Data' type

        // needs to be defined last
        byteToDataType[9] = DataType.BOOL_ARRAY;
        byteToDataType[10] = DataType.BYTE_ARRAY;
        byteToDataType[11] = DataType.SHORT_ARRAY;
        byteToDataType[12] = DataType.INT_ARRAY;
        byteToDataType[13] = DataType.LONG_ARRAY;
        byteToDataType[14] = DataType.FLOAT_ARRAY;
        byteToDataType[15] = DataType.DOUBLE_ARRAY;
        byteToDataType[202] = DataType.CHAR_ARRAY; // not actually implemented by CMW
        byteToDataType[16] = DataType.STRING_ARRAY;

        // CMW 2D arrays -- also mapped internally to byte arrays
        byteToDataType[17] = DataType.BOOL_ARRAY;
        byteToDataType[18] = DataType.BYTE_ARRAY;
        byteToDataType[19] = DataType.SHORT_ARRAY;
        byteToDataType[20] = DataType.INT_ARRAY;
        byteToDataType[21] = DataType.LONG_ARRAY;
        byteToDataType[22] = DataType.FLOAT_ARRAY;
        byteToDataType[23] = DataType.DOUBLE_ARRAY;
        byteToDataType[203] = DataType.CHAR_ARRAY; // not actually implemented by CMW
        byteToDataType[24] = DataType.STRING_ARRAY;

        // CMW multi-dim arrays -- also mapped internally to byte arrays
        byteToDataType[25] = DataType.BOOL_ARRAY;
        byteToDataType[26] = DataType.BYTE_ARRAY;
        byteToDataType[27] = DataType.SHORT_ARRAY;
        byteToDataType[28] = DataType.INT_ARRAY;
        byteToDataType[29] = DataType.LONG_ARRAY;
        byteToDataType[30] = DataType.FLOAT_ARRAY;
        byteToDataType[31] = DataType.DOUBLE_ARRAY;
        byteToDataType[204] = DataType.CHAR_ARRAY; // not actually implemented by CMW
        byteToDataType[32] = DataType.STRING_ARRAY;

        for (int i = byteToDataType.length - 1; i >= 0; i--) {
            if (byteToDataType[i] == null) {
                continue;
            }
            final int id = byteToDataType[i].getID();
            dataTypeToByte[id] = (byte) i;
        }
    }

    private int bufferIncrements = ADDITIONAL_HEADER_INFO_SIZE;
    private IoBuffer buffer;
    private WireDataFieldDescription parent;
    private Deque<Integer> parentChildCount = new ArrayDeque<>();
    private WireDataFieldDescription lastFieldHeader;

    public CmwLightSerialiser(final IoBuffer buffer) {
        super();
        this.buffer = buffer;
        this.buffer.setCallBackFunction(null);
    }

    @Override
    public ProtocolInfo checkHeaderInfo() {
        final String fieldName = "";
        final int dataSize = FastByteBuffer.SIZE_OF_INT;
        final WireDataFieldDescription headerStartField = new WireDataFieldDescription(parent, fieldName.hashCode(), fieldName, DataType.START_MARKER, buffer.position(), buffer.position(), dataSize);
        final int nEntries = buffer.getInt();
        if (nEntries <= 0) {
            throw new IllegalStateException("nEntries = " + nEntries + " <= 0!");
        }
        parent = lastFieldHeader = headerStartField;
        return new ProtocolInfo(headerStartField, CmwLightSerialiser.class.getCanonicalName(), (byte) 1, (byte) 0, (byte) 1);
    }

    public IoBuffer getBuffer() {
        return buffer;
    }

    public int getBufferIncrements() {
        return bufferIncrements;
    }

    @Override
    public <E> Collection<E> getCollection(final Collection<E> collection, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public <E> E getCustomData(final FieldSerialiser<E> serialiser) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public <E extends Enum<E>> Enum<E> getEnum(final Enum<E> enumeration) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public String getEnumTypeList() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public WireDataFieldDescription getFieldHeader() {
        // process CMW-like wire-format
        final int headerStart = buffer.position();

        final String fieldName = buffer.getStringISO8859();
        final byte dataTypeByte = buffer.getByte();
        final DataType dataType = getDataType(dataTypeByte);
        // process CMW-like wire-format - done

        final int dataStartOffset = buffer.position() - headerStart;
        final int dataStartPosition = headerStart + dataStartOffset;
        final int dataSize;
        if (dataType == DataType.START_MARKER) {
            dataSize = FastByteBuffer.SIZE_OF_INT;

        } else if (dataType.isScalar()) {
            dataSize = dataType.getPrimitiveSize();
        } else if (dataType == DataType.STRING) {
            dataSize = FastByteBuffer.SIZE_OF_INT + buffer.getInt(); // <(>string size -1> + <string byte data>
        } else if (dataType.isArray() && dataType != DataType.STRING_ARRAY) {
            // read array descriptor
            final int[] dims = buffer.getArraySizeDescriptor();
            final int arraySize = buffer.getInt(); // strided array size
            dataSize = FastByteBuffer.SIZE_OF_INT * (dims.length + 2) + arraySize * dataType.getPrimitiveSize(); // <array description> + <nElments * primitive size>
        } else if (dataType == DataType.STRING_ARRAY) {
            // read array descriptor -- this case has a high-penalty since the size of all Strings needs to be read
            final int[] dims = buffer.getArraySizeDescriptor();
            final int arraySize = buffer.getInt(); // strided array size
            // String parsing, need to follow every single element
            int totalSize = FastByteBuffer.SIZE_OF_INT * arraySize;
            for (int i = 0; i < arraySize; i++) {
                final int stringSize = buffer.getInt(); // <(>string size -1> + <string byte data>
                totalSize += stringSize;
                buffer.position(buffer.position() + stringSize);
            }
            dataSize = FastByteBuffer.SIZE_OF_INT * (dims.length + 2) + totalSize;
        } else {
            throw new IllegalStateException("should not reach here -- format is incompatible with CMW");
        }

        final int fieldNameHashCode = fieldName.hashCode(); //TODO: verify same hashcode function

        lastFieldHeader = new WireDataFieldDescription(parent, fieldNameHashCode, fieldName, dataType, headerStart, dataStartOffset, dataSize);
        buffer.position(dataStartPosition);

        if (dataType == DataType.START_MARKER) {
            parent = lastFieldHeader;
            buffer.position(dataStartPosition);
            final int childCount = buffer.getInt();
            parentChildCount.add(childCount);
            buffer.position(dataStartPosition + dataSize);
        }

        if (dataSize < 0) {
            throw new IllegalStateException("should not reach here -- format is incompatible with CMW");
        }
        return lastFieldHeader;
    }

    @Override
    public <E> List<E> getList(final List<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public <K, V, E> Map<K, V> getMap(final Map<K, V> map, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public WireDataFieldDescription getParent() {
        return parent;
    }

    @Override
    public <E> Queue<E> getQueue(final Queue<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public <E> Set<E> getSet(final Set<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * @return {@code true} the ISO-8859-1 character encoding is being enforced for data fields (better performance), otherwise UTF-8 is being used (more generic encoding)
     */
    public boolean isEnforceSimpleStringEncoding() {
        return buffer.isEnforceSimpleStringEncoding();
    }

    @Override
    public boolean isPutFieldMetaData() {
        return false;
    }

    @Override
    public WireDataFieldDescription parseIoStream(final boolean readHeader) {
        final WireDataFieldDescription fieldRoot = getRootElement();
        parent = fieldRoot;
        final WireDataFieldDescription headerRoot = readHeader ? checkHeaderInfo().getFieldHeader() : getFieldHeader();
        buffer.position(headerRoot.getDataStartPosition() + headerRoot.getDataSize());
        parseIoStream(headerRoot, 0);
        return fieldRoot;
    }

    public void parseIoStream(final WireDataFieldDescription fieldRoot, final int recursionDepth) {
        if (fieldRoot == null || fieldRoot.getDataType() != DataType.START_MARKER) {
            throw new IllegalStateException("fieldRoot not a START_MARKER but: " + fieldRoot);
        }
        buffer.position(fieldRoot.getDataStartPosition());
        final int nEntries = buffer.getInt();
        if (nEntries <= 0) {
            throw new IllegalStateException("nEntries = " + nEntries + " <= 0!");
        }
        parent = lastFieldHeader = fieldRoot;
        for (int i = 0; i < nEntries; i++) {
            final WireDataFieldDescription field = getFieldHeader();
            final int dataSize = field.getDataSize();
            final int skipPosition = field.getDataStartPosition() + dataSize;

            if (field.getDataType() == DataType.START_MARKER) {
                // detected sub-class start marker
                parent = lastFieldHeader = field;
                parseIoStream(field, recursionDepth + 1);
                parent = lastFieldHeader = fieldRoot;
                continue;
            }

            if (dataSize < 0) {
                LOGGER.atWarn().addArgument(field.getFieldName()).addArgument(field.getDataType()).addArgument(dataSize).log("WireDataFieldDescription for '{}' type '{}' has bytesToSkip '{} <= 0'");
                // fall-back option in case of undefined dataSetSize -- usually indicated an internal serialiser error
                throw new IllegalStateException();
            }

            if (skipPosition < buffer.capacity()) {
                buffer.position(skipPosition);
            } else {
                // reached end of buffer
                if (skipPosition == buffer.capacity()) {
                    return;
                }
                throw new IllegalStateException("reached beyond end of buffer at " + skipPosition + " vs. capacity" + buffer.capacity() + " " + field);
            }
        }
    }

    @Override
    public <E> void put(final Collection<E> collection, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void put(final Enum<?> enumeration) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public <K, V, E> void put(final Map<K, V> map, Type keyType, Type valueType, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public <E> WireDataFieldDescription putCustomData(final FieldDescription fieldDescription, final E rootObject, Class<? extends E> type, final FieldSerialiser<E> serialiser) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void putEndMarker(final String markerName) {
        if (parent.getParent() != null) {
            parent = (WireDataFieldDescription) parent.getParent();
        }
    }

    @Override
    public WireDataFieldDescription putFieldHeader(final FieldDescription fieldDescription) {
        return putFieldHeader(fieldDescription, fieldDescription.getDataType());
    }

    public WireDataFieldDescription putFieldHeader(final FieldDescription fieldDescription, DataType customDataType) {
        final boolean isScalar = customDataType.isScalar();
        buffer.setCallBackFunction(null);

        final int headerStart = buffer.position();
        final String fieldName = fieldDescription.getFieldName();
        buffer.putStringISO8859(fieldName); // full field name
        buffer.putByte(getDataType(customDataType)); // data type ID

        final int dataStart = buffer.position();
        final int dataStartOffset = dataStart - headerStart;
        final int dataSize;
        if (isScalar) {
            dataSize = customDataType.getPrimitiveSize();
        } else if (customDataType == DataType.START_MARKER) {
            dataSize = FastByteBuffer.SIZE_OF_INT;
            buffer.ensureAdditionalCapacity(dataSize);
        } else {
            dataSize = -1;
            // from hereon there are data specific structures
            buffer.ensureAdditionalCapacity(16); // allocate 16+ bytes to account for potential array header (safe-bet)
        }

        lastFieldHeader = new WireDataFieldDescription(parent, fieldDescription.getFieldNameHashCode(), fieldDescription.getFieldName(), customDataType, headerStart, dataStartOffset, dataSize);
        updateDataEntryCount();

        return lastFieldHeader;
    }

    @Override
    public WireDataFieldDescription putFieldHeader(final String fieldName, final DataType dataType) {
        final boolean isScalar = dataType.isScalar();

        final int headerStart = buffer.position();
        buffer.putStringISO8859(fieldName); // full field name
        buffer.putByte(getDataType(dataType)); // data type ID

        final int dataStart = buffer.position();
        final int dataStartOffset = dataStart - headerStart;
        final int dataSize;
        if (isScalar) {
            dataSize = dataType.getPrimitiveSize();
        } else if (dataType == DataType.START_MARKER) {
            dataSize = FastByteBuffer.SIZE_OF_INT;
            buffer.ensureAdditionalCapacity(dataSize);
        } else {
            dataSize = -1;
            // from hereon there are data specific structures
            buffer.ensureAdditionalCapacity(16); // allocate 16+ bytes to account for potential array header (safe-bet)
        }

        final int fieldNameHashCode = fieldName.hashCode(); // TODO: check hashCode function
        lastFieldHeader = new WireDataFieldDescription(parent, fieldNameHashCode, fieldName, dataType, headerStart, dataStartOffset, dataSize);
        updateDataEntryCount();

        return lastFieldHeader;
    }

    @Override
    public void putHeaderInfo(final FieldDescription... field) {
        parent = lastFieldHeader = getRootElement();
        final String fieldName = "";
        final int dataSize = FastByteBuffer.SIZE_OF_INT;
        lastFieldHeader = new WireDataFieldDescription(parent, fieldName.hashCode(), fieldName, DataType.START_MARKER, buffer.position(), buffer.position(), dataSize);
        buffer.putInt(0);
        updateDataEntryCount();
        parent = lastFieldHeader;
    }

    @Override
    public void putStartMarker(final String markerName) {
        putFieldHeader(markerName, DataType.START_MARKER);
        buffer.putInt(0);
        buffer.putStartMarker(markerName);
        updateDataEndMarker(lastFieldHeader);
        parent = lastFieldHeader;
    }

    @Override
    public void putStartMarker(final FieldDescription fieldDescription) {
        putFieldHeader(fieldDescription);
        buffer.putInt(0);
        buffer.putStartMarker(fieldDescription.getFieldName());
        updateDataEndMarker(lastFieldHeader);
        parent = lastFieldHeader;
    }

    public void setBuffer(final IoBuffer buffer) {
        this.buffer = buffer;
        this.buffer.setCallBackFunction(null);
    }

    /**
     *
     * @param state {@code true} the ISO-8859-1 character encoding is being enforced for data fields (better performance), otherwise UTF-8 is being used (more generic encoding)
     */
    public void setEnforceSimpleStringEncoding(final boolean state) {
        buffer.setEnforceSimpleStringEncoding(state);
    }

    @Override
    public void setPutFieldMetaData(final boolean putFieldMetaData) {
        // do nothing -- not implemented for this serialiser
    }

    @Override
    public void updateDataEndMarker(final WireDataFieldDescription fieldHeader) {
        final int sizeMarkerEnd = buffer.position();
        final int headerStart = fieldHeader.getFieldStart();
        final int dataStart = headerStart + fieldHeader.getDataStartOffset();
        final int dataSize = sizeMarkerEnd - dataStart;
        if (fieldHeader.getDataSize() != dataSize) {
            fieldHeader.setDataSize(dataSize);
        }
    }

    private WireDataFieldDescription getRootElement() {
        return new WireDataFieldDescription(null, "ROOT".hashCode(), "ROOT", DataType.OTHER, buffer.position(), -1, -1);
    }

    private void updateDataEntryCount() {
        // increment parent child count
        if (parent == null) {
            throw new IllegalStateException("no parent");
        }

        final int parentDataStart = parent.getDataStartPosition();
        if (parentDataStart >= 0) { // N.B. needs to be '>=' since CMW header is an incomplete field header containing only an 'nEntries<int>' data field
            buffer.position(parentDataStart);
            final int nEntries = buffer.getInt();
            buffer.position(parentDataStart);
            buffer.putInt(nEntries + 1);
            buffer.position(lastFieldHeader.getDataStartPosition());
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
        final int id = byteValue & 0xFF;
        if (byteToDataType[id] != null) {
            return byteToDataType[id];
        }

        throw new IllegalArgumentException("DataType byteValue=" + byteValue + " rawByteValue=" + (byteValue & 0xFF) + " not mapped");
    }
}
