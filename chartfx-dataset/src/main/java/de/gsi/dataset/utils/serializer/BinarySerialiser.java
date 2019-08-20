package de.gsi.dataset.utils.serializer;

import static de.gsi.dataset.utils.serializer.FastByteBuffer.SIZE_OF_BYTE;
import static de.gsi.dataset.utils.serializer.FastByteBuffer.SIZE_OF_INT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gsi.dataset.utils.AssertUtils;

public class BinarySerialiser { // NOPMD - omen est omen
    private static final String READ_POSITION_AT_BUFFER_END = "read position at buffer end";
    public static final byte VERSION_MAJOR = 1;
    public static final byte VERSION_MINOR = 0;
    public static final byte VERSION_MICRO = 0;
    protected static final BinarySerialiser SELF = new BinarySerialiser();
    protected static HeaderInfo headerThis = SELF.new HeaderInfo(BinarySerialiser.class.getSimpleName(), VERSION_MAJOR,
            VERSION_MINOR, VERSION_MICRO);
    private static int bufferIncrements;

    protected BinarySerialiser() {
    }

    public static int getBufferIncrements() {
        return bufferIncrements;
    }

    public static void setBufferIncrements(final int bufferIncrements) {
        AssertUtils.gtEqThanZero("bufferIncrements", bufferIncrements);
        BinarySerialiser.bufferIncrements = bufferIncrements;
    }

    //
    // -- WRITE OPERATIONS -------------------------------------------
    //

    /**
     * Adds header and version information
     * 
     * @param buffer to use for serialisation
     */
    public static void putHeaderInfo(FastByteBuffer buffer) {
        AssertUtils.notNull("buffer", buffer);
        buffer.putString("#file producer : ");
        buffer.putString(BinarySerialiser.class.getCanonicalName());
        buffer.putString("\n");
        buffer.putByte(VERSION_MAJOR);
        buffer.putByte(VERSION_MINOR);
        buffer.putByte(VERSION_MICRO);
    }

    protected static void putFieldHeader(final FastByteBuffer buffer, final String fieldName, final DataType dataType) {
        putFieldHeader(buffer, fieldName, dataType, 0);
    }

    protected static void putFieldHeader(final FastByteBuffer buffer, final String fieldName, final DataType dataType,
            final int additionalSize) {
        AssertUtils.notNull("buffer", buffer);
        AssertUtils.notNull("fieldName", fieldName);
        buffer.ensureAdditionalCapacity(fieldName.length() + 1L + 2 * SIZE_OF_BYTE + bufferIncrements
                + dataType.getPrimitiveSize() + additionalSize);
        buffer.putString(fieldName);
        buffer.putByte(dataType.getAsByte());
    }

    public static void putEndMarker(final FastByteBuffer buffer) {
        putFieldHeader(buffer, DataType.END_MARKER.getAsString(), DataType.END_MARKER);
        buffer.putByte(DataType.END_MARKER.getAsByte());
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final boolean value) {
        putFieldHeader(buffer, fieldName, DataType.BOOL);
        buffer.putBoolean(value);
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final byte value) {
        putFieldHeader(buffer, fieldName, DataType.BYTE);
        buffer.putByte(value);
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final short value) { // NOPMD
        putFieldHeader(buffer, fieldName, DataType.SHORT);
        buffer.putShort(value);
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final int value) {
        putFieldHeader(buffer, fieldName, DataType.INT);
        buffer.putInt(value);
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final long value) {
        putFieldHeader(buffer, fieldName, DataType.LONG);
        buffer.putLong(value);
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final float value) {
        putFieldHeader(buffer, fieldName, DataType.FLOAT);
        buffer.putFloat(value);
    }

    public static void put(FastByteBuffer buffer, final String fieldName, final double value) {
        putFieldHeader(buffer, fieldName, DataType.DOUBLE);
        buffer.putDouble(value);
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final String value) {
        putFieldHeader(buffer, fieldName, DataType.STRING, (value == null ? 1 : value.length()) + 1);
        buffer.putString(value == null ? "" : value);
    }

    private static int getNumberOfElements(final int[] dimensions) {
        AssertUtils.notNull("dimensions", dimensions);
        int ret = 1;
        for (int i = 0; i < dimensions.length; i++) {
            final int dim = dimensions[i];
            AssertUtils.gtThanZero("dimensions[" + i + "]", dim);
            ret *= dim;
        }
        return ret;
    }

    protected static int putArrayHeader(final FastByteBuffer buffer, final String fieldName, final DataType dataType,
            final int[] dims) {
        AssertUtils.notNull("dims", dims);
        final int nElements = getNumberOfElements(dims);
        final int addBufferSize = dims.length * (int) SIZE_OF_INT + nElements * (int) dataType.getPrimitiveSize() + 5;
        putFieldHeader(buffer, fieldName, dataType, addBufferSize);

        buffer.putInt(dims.length); // number of dimensions
        for (int i = 0; i < dims.length; ++i) {
            buffer.putInt(dims[i]); // vector size for each dimension
        }
        return nElements;
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final boolean[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final boolean[] arrayValue,
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.BOOL_ARRAY, dims);
        buffer.putBooleanArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final byte[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final byte[] arrayValue,
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.BYTE_ARRAY, dims);
        buffer.putByteArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final short[] arrayValue) { // NOPMD
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final short[] arrayValue, // NOPMD
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.SHORT_ARRAY, dims);
        buffer.putShortArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final int[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final int[] arrayValue,
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.INT_ARRAY, dims);
        buffer.putIntArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final long[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final long[] arrayValue,
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.LONG_ARRAY, dims);
        buffer.putLongArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final float[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final float[] arrayValue,
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.FLOAT_ARRAY, dims);
        buffer.putFloatArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final double[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final double[] arrayValue,
            final int[] dims) {
        final int nElements = putArrayHeader(buffer, fieldName, DataType.DOUBLE_ARRAY, dims);
        buffer.putDoubleArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final String[] arrayValue) {
        put(buffer, fieldName, arrayValue, new int[] { arrayValue.length });
    }

    public static void put(final FastByteBuffer buffer, final String fieldName, final String[] arrayValue,
            final int[] dims) {
        if (arrayValue == null || arrayValue.length == 0) {
            return;
        }
        final int nElements = putArrayHeader(buffer, fieldName, DataType.STRING_ARRAY, dims);
        buffer.putStringArray(arrayValue, Math.min(nElements, arrayValue.length));
    }

    protected static void putGenericArrayAsPrimitive(FastByteBuffer buffer, DataType dataType, Object[] data,
            int nToCopy) {
        // @formatter:off
        switch (dataType) {
        case BOOL:   buffer.putBooleanArray(GenericsHelper.toBoolPrimitive((Boolean[]) data), nToCopy); break;
        case BYTE:   buffer.putByteArray(GenericsHelper.toBytePrimitive(data), nToCopy); break;
        case SHORT:  buffer.putShortArray(GenericsHelper.toShortPrimitive(data), nToCopy); break;
        case INT:    buffer.putIntArray(GenericsHelper.toIntegerPrimitive(data), nToCopy); break;
        case LONG:   buffer.putLongArray(GenericsHelper.toLongPrimitive(data), nToCopy); break;
        case FLOAT:  buffer.putFloatArray(GenericsHelper.toFloatPrimitive(data), nToCopy); break;
        case DOUBLE: buffer.putDoubleArray(GenericsHelper.toDoublePrimitive(data), nToCopy); break;
        case STRING: buffer.putStringArray(GenericsHelper.toStringPrimitive(data), nToCopy); break;
        // @formatter:on
        default:
            throw new IllegalArgumentException("type not implemented - " + data[0].getClass().getSimpleName());
        }
    }

    public static <K, V> void put(final FastByteBuffer buffer, final String fieldName, final Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        Object[] keySet = map.keySet().toArray();
        Object[] valueSet = map.values().toArray();
        final int nElements = keySet.length;
        DataType keyDataType = DataType.fromClassType(keySet[0].getClass());
        DataType valueDataType = DataType.fromClassType(valueSet[0].getClass());
        // convert into two linear arrays one of K and the other for V
        // streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)>
        // <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        final int entrySize = 16; // as an initial estimate

        // write field header
        putFieldHeader(buffer, fieldName, DataType.MAP, nElements * entrySize + 5);
        buffer.putInt(1); // write dimension '1'
        buffer.putInt(nElements); // write number of map elements
        buffer.putByte(keyDataType.getAsByte()); // write key element type
        buffer.putByte(valueDataType.getAsByte()); // write value element type
        putGenericArrayAsPrimitive(buffer, keyDataType, keySet, nElements);
        putGenericArrayAsPrimitive(buffer, valueDataType, valueSet, nElements);
    }

    //
    // -- READ OPERATIONS --------------------------------------------
    //

    public static HeaderInfo checkHeaderInfo(final FastByteBuffer readBuffer) {
        AssertUtils.notNull("readBuffer", readBuffer);
        readBuffer.getString(); // should read "#file producer : "
        // -- but not explicitly checked
        String producer = readBuffer.getString();
        readBuffer.getString(); // not explicitly checked
        byte major = readBuffer.getByte();
        byte minor = readBuffer.getByte();
        byte micro = readBuffer.getByte();

        HeaderInfo header = SELF.new HeaderInfo(producer, major, minor, micro);

        if (!header.isCompatible()) {
            String msg = String.format("byte buffer version incompatible: reveived '%s' vs. this '%s'",
                    header.toString(), headerThis.toString());
            throw new IllegalStateException(msg);
        }

        return header;
    }

    public static FieldHeader getFieldHeader(final FastByteBuffer readBuffer) {
        final String fieldName = readBuffer.getString();
        final byte dataTypeByte = readBuffer.getByte();
        final DataType dataType = DataType.fromByte(dataTypeByte);
        if (dataType.equals(DataType.END_MARKER)) {
            return null;
        }

        if (dataType.isScalar()) {
            return SELF.new FieldHeader(fieldName, dataType, new int[] { 1 });
        }

        // multi-dimensional array or map
        final int arrayDims = readBuffer.getInt();
        final int[] dims = new int[arrayDims];
        for (int i = 0; i < arrayDims; ++i) {
            dims[i] = readBuffer.getInt();
        }
        return SELF.new FieldHeader(fieldName, dataType, dims);
    }

    public static boolean getBoolean(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getBoolean();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static byte getByte(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getByte();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static short getShort(final FastByteBuffer readBuffer) { // NOPMD
        if (readBuffer.verifySize()) {
            return readBuffer.getShort();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static int getInteger(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getInt();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static long getLong(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getLong();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static float getFloat(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getFloat();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static double getDouble(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getDouble();
        }
        throw new IndexOutOfBoundsException(READ_POSITION_AT_BUFFER_END);
    }

    public static String getString(final FastByteBuffer readBuffer) {
        if (readBuffer.verifySize()) {
            return readBuffer.getString();
        }
        return null;
    }

    public static int[] getArrayDimensions(final FastByteBuffer readBuffer) {
        final int arrayDims = readBuffer.getInt(); // array dimensions
        final int[] dims = new int[arrayDims];
        for (int i = 0; i < arrayDims; ++i) {
            dims[i] = readBuffer.getInt();
        }
        return dims;
    }

    public static boolean[] getBooleanArray(final FastByteBuffer readBuffer) {
        return readBuffer.getBooleanArray();
    }

    public static byte[] getByteArray(final FastByteBuffer readBuffer) {
        return readBuffer.getByteArray();
    }

    public static short[] getShortArray(final FastByteBuffer readBuffer) { // NOPMD
        return readBuffer.getShortArray();
    }

    public static int[] getIntArray(final FastByteBuffer readBuffer) {
        return readBuffer.getIntArray();
    }

    public static long[] getLongArray(final FastByteBuffer readBuffer) {
        return readBuffer.getLongArray();
    }

    public static float[] getFloatArray(final FastByteBuffer readBuffer) {
        return readBuffer.getFloatArray();
    }

    public static double[] getDoubleArray(final FastByteBuffer readBuffer) {
        return readBuffer.getDoubleArray();
    }

    public static String[] getStringArray(final FastByteBuffer readBuffer) {
        return readBuffer.getStringArray();
    }

    protected static Object[] getGenericArrayAsPrimitive(FastByteBuffer readBuffer, DataType dataType) {
        Object[] retVal;
        // @formatter:off
        switch (dataType) {
        case BOOL:   retVal = GenericsHelper.toObject(readBuffer.getBooleanArray()); break;
        case BYTE:   retVal = GenericsHelper.toObject(readBuffer.getByteArray()); break;
        case SHORT:  retVal = GenericsHelper.toObject(readBuffer.getShortArray()); break;
        case INT:    retVal = GenericsHelper.toObject(readBuffer.getIntArray()); break;
        case LONG:   retVal = GenericsHelper.toObject(readBuffer.getLongArray()); break;
        case FLOAT:  retVal = GenericsHelper.toObject(readBuffer.getFloatArray()); break;
        case DOUBLE: retVal = GenericsHelper.toObject(readBuffer.getDoubleArray()); break;
        case STRING: retVal = readBuffer.getStringArray(); break;
        // @formatter:on
        default:
            throw new IllegalArgumentException("type not implemented - " + dataType);
        }
        return retVal;
    }

    public static <K, V> Map<K, V> getMap(final FastByteBuffer readBuffer, Map<K, V> map) {
        // convert into two linear arrays one of K and the other for V
        // streamer encoding as
        // <1 (int)><n_map (int)><type K (byte)<type V (byte)>
        // <Length, [K_0,...,K_length]> <Length, [V_0, ..., V_length]>
        DataType keyDataType = DataType.fromByte(readBuffer.getByte());
        DataType valueDataType = DataType.fromByte(readBuffer.getByte());

        // read key and value vector
        Object[] keys = getGenericArrayAsPrimitive(readBuffer, keyDataType);
        Object[] values = getGenericArrayAsPrimitive(readBuffer, valueDataType);
        if (map == null) {
            map = new ConcurrentHashMap<>();
        }
        for (int i = 0; i < keys.length; i++) {
            map.put((K) keys[i], (V) values[i]);
        }

        return map;
    }

    //
    // -- HELPER CLASSES ---------------------------------------------
    //

    public class FieldHeader {
        private final String fieldName;
        private final DataType dataType;
        private final int[] dimensions;

        private FieldHeader(String fieldName, DataType dataType, final int[] dims) {
            this.fieldName = fieldName;
            this.dataType = dataType;
            this.dimensions = dims;
        }

        public String getFieldName() {
            return fieldName;
        }

        public DataType getDataType() {
            return dataType;
        }

        public int getDataDimension() {
            return dimensions.length;
        }

        public int[] getDataDimensions() {
            return dimensions;
        }

        @Override
        public String toString() {
            if (dimensions.length == 1 && dimensions[0] == 1) {
                return String.format("[fieldName=%s, fieldType=%s]", fieldName, dataType.getAsString());
            }

            StringBuilder builder = new StringBuilder(27);
            builder.append("[fieldName=").append(fieldName).append(", fieldType=").append(dataType.getAsString())
                    .append('[');
            for (int i = 0; i < dimensions.length; i++) {
                builder.append(dimensions[i]);
                if (i < dimensions.length - 1) {
                    builder.append(',');
                }
            }
            builder.append("]]");
            return builder.toString();
        }
    }

    public class HeaderInfo {
        private final String producerName;
        private final byte versionMajor;
        private final byte versionMinor;
        private final byte versionMicro;

        private HeaderInfo(final String producer, final byte major, final byte minor, final byte micro) {
            this.producerName = producer;
            this.versionMajor = major;
            this.versionMinor = minor;
            this.versionMicro = micro;
        }

        public String getProducerName() {
            return producerName;
        }

        public byte getVersionMajor() {
            return versionMajor;
        }

        public byte getVersionMinor() {
            return versionMinor;
        }

        public byte getVersionMicro() {
            return versionMicro;
        }

        public boolean isCompatible() {
            return getVersionMajor() <= VERSION_MAJOR;
        }

        public String toString() {
            return String.format("%s-v%d.%d.%d", getProducerName(), getVersionMajor(), getVersionMinor(),
                    getVersionMicro());
        }
    }
}
