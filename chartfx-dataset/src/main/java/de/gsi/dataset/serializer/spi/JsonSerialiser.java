package de.gsi.dataset.serializer.spi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import com.jsoniter.JsonIterator;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.any.Any;
import com.jsoniter.extra.PreciseFloatSupport;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsonException;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.FieldDescription;
import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.utils.ByteBufferOutputStream;

public class JsonSerialiser implements IoSerialiser {
    public static final String NOT_A_JSON_COMPATIBLE_PROTOCOL = "Not a JSON compatible protocol";
    public static final String JSON_ROOT = "JSON_ROOT";
    private static final int DEFAULT_INITIAL_CAPACITY = 10000;
    private static final int DEFAULT_INDENTATION = 2;
    private static final char BRACKET_OPEN = '{';
    private static final char BRACKET_CLOSE = '}';
    private static final String LINE_BREAK = System.getProperty("line.separator");
    private final StringBuilder builder = new StringBuilder(DEFAULT_INITIAL_CAPACITY);
    private IoBuffer buffer;
    private boolean putFieldMetaData = true;
    private Any root = null;
    private Any tempRoot = null;
    private WireDataFieldDescription parent;
    private WireDataFieldDescription lastFieldHeader;
    private String queryFieldName = null;
    private boolean hasFieldBefore = false;
    private String indentation = "";

    /**
     * @param buffer the backing IoBuffer (see e.g. {@link de.gsi.dataset.serializer.spi.FastByteBuffer} or{@link de.gsi.dataset.serializer.spi.ByteBuffer}
     */
    public JsonSerialiser(final IoBuffer buffer) {
        super();
        this.buffer = buffer;

        // JsonStream.setIndentionStep(DEFAULT_INDENTATION)
        // JsonStream.setMode(EncodingMode.REFLECTION_MODE) -- enable as a fall back
        // JsonIterator.setMode(DecodingMode.REFLECTION_MODE) -- enable as a fall back
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);

        try {
            PreciseFloatSupport.enable();
        } catch (JsonException e) {
            // swallow subsequent enabling exceptions (function is guarded and supposed to be called only once)
        }
    }

    @Override
    public ProtocolInfo checkHeaderInfo() {
        // make coarse check (ie. check if first non-null character is a '{' bracket
        int count = buffer.position();
        while (buffer.getByte(count) != BRACKET_OPEN && (buffer.getByte(count) == 0 || buffer.getByte(count) == ' ' || buffer.getByte(count) == '\t' || buffer.getByte(count) == '\n')) {
            count++;
        }
        if (buffer.getByte(count) != BRACKET_OPEN) {
            throw new IllegalStateException(NOT_A_JSON_COMPATIBLE_PROTOCOL);
        }
        JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
        iter.reset(buffer.elements(), 0, buffer.limit());

        try {
            tempRoot = root = iter.readAny();
        } catch (IOException e) {
            throw new IllegalStateException(NOT_A_JSON_COMPATIBLE_PROTOCOL, e);
        }

        final WireDataFieldDescription headerStartField = new WireDataFieldDescription(this, null, JSON_ROOT.hashCode(), JSON_ROOT, DataType.OTHER, buffer.position(), count - 1, -1);
        final ProtocolInfo header = new ProtocolInfo(this, headerStartField, JsonSerialiser.class.getCanonicalName(), (byte) 1, (byte) 0, (byte) 0);
        parent = lastFieldHeader = headerStartField;
        queryFieldName = JSON_ROOT;
        return header;
    }

    public <T> T deserialiseObject(final T obj) {
        final JsonIterator iter = JsonIterator.parse(buffer.elements(), 0, buffer.limit());
        try {
            return iter.read(obj);
        } catch (IOException | JsonException e) {
            throw new IllegalStateException(NOT_A_JSON_COMPATIBLE_PROTOCOL, e);
        }
    }

    @Override
    public int[] getArraySizeDescriptor() {
        return new int[0];
    }

    @Override
    public boolean getBoolean() {
        return tempRoot.get(queryFieldName).toBoolean();
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(boolean[].class);
    }

    @Override
    public IoBuffer getBuffer() {
        return buffer;
    }

    @Override
    public byte getByte() {
        return (byte) tempRoot.get(queryFieldName).toInt();
    }

    @Override
    public byte[] getByteArray(final byte[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(byte[].class);
    }

    @Override
    public char getChar() {
        return (char) tempRoot.get(queryFieldName).toInt();
    }

    @Override
    public char[] getCharArray(final char[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(char[].class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Collection<E> getCollection(final Collection<E> collection, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        return tempRoot.get(queryFieldName).as(ArrayList.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> E getCustomData(final FieldSerialiser<E> serialiser) {
        return (E) tempRoot.get(queryFieldName);
    }

    @Override
    public double getDouble() {
        return tempRoot.get(queryFieldName).toDouble();
    }

    @Override
    public double[] getDoubleArray(final double[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(double[].class);
    }

    @Override
    public <E extends Enum<E>> Enum<E> getEnum(final Enum<E> enumeration) {
        return null;
    }

    @Override
    public String getEnumTypeList() {
        return null;
    }

    @Override
    public WireDataFieldDescription getFieldHeader() {
        return null;
    }

    @Override
    public float getFloat() {
        return tempRoot.get(queryFieldName).toFloat();
    }

    @Override
    public float[] getFloatArray(final float[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(float[].class);
    }

    @Override
    public int getInt() {
        return tempRoot.get(queryFieldName).toInt();
    }

    @Override
    public int[] getIntArray(final int[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(int[].class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> List<E> getList(final List<E> collection, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        return tempRoot.get(queryFieldName).as(List.class);
    }

    @Override
    public long getLong() {
        return tempRoot.get(queryFieldName).toLong();
    }

    @Override
    public long[] getLongArray(final long[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(long[].class);
    }

    @Override
    public <K, V, E> Map<K, V> getMap(final Map<K, V> map, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        return null;
    }

    public WireDataFieldDescription getParent() {
        return parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Queue<E> getQueue(final Queue<E> collection, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        return tempRoot.get(queryFieldName).as(ArrayDeque.class);
    }

    public String getSerialisedString() {
        return buffer.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> Set<E> getSet(final Set<E> collection, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        return tempRoot.get(queryFieldName).as(HashSet.class);
    }

    @Override
    public short getShort() {
        return (short) tempRoot.get(queryFieldName).toLong();
    }

    @Override
    public short[] getShortArray(final short[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(short[].class);
    }

    @Override
    public String getString() {
        return tempRoot.get(queryFieldName).toString();
    }

    @Override
    public String[] getStringArray(final String[] dst, final int length) {
        return tempRoot.get(queryFieldName).as(String[].class);
    }

    @Override
    public String getStringISO8859() {
        return tempRoot.get(queryFieldName).toString();
    }

    @Override
    public boolean isPutFieldMetaData() {
        return putFieldMetaData;
    }

    @Override
    public WireDataFieldDescription parseIoStream(final boolean readHeader) {
        JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
        iter.reset(buffer.elements(), 0, buffer.limit());

        try {
            tempRoot = root = iter.readAny();
        } catch (IOException e) {
            throw new IllegalStateException(NOT_A_JSON_COMPATIBLE_PROTOCOL, e);
        }
        final WireDataFieldDescription fieldRoot = new WireDataFieldDescription(this, null, "ROOT".hashCode(), "ROOT", DataType.OTHER, buffer.position(), -1, -1);
        parseIoStream(fieldRoot, tempRoot, "");

        return fieldRoot;
    }

    @Override
    public <E> void put(final FieldDescription fieldDescription, final Collection<E> collection, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        put(fieldDescription.getFieldName(), collection, valueType, serialiserLookup);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final Enum<?> enumeration) {
        put(fieldDescription.getFieldName(), enumeration);
    }

    @Override
    public <K, V, E> void put(final FieldDescription fieldDescription, final Map<K, V> map, final Type keyType, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        put(fieldDescription.getFieldName(), map, keyType, valueType, serialiserLookup);
    }

    @Override
    public <E> void put(final String fieldName, final Collection<E> collection, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\", ").append("[");
        if (collection == null || collection.isEmpty()) {
            builder.append(']');
            return;
        }
        final Iterator<E> iter = collection.iterator();
        builder.append(iter.next().toString());
        E element;
        while ((element = iter.next()) != null) {
            builder.append(", ").append(element.toString());
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final Enum<?> enumeration) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(enumeration);
        hasFieldBefore = true;
    }

    @Override
    public <K, V, E> void put(final String fieldName, final Map<K, V> map, final Type keyType, final Type valueType, final BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\", ").append('{');
        if (map == null || map.isEmpty()) {
            builder.append('}');
            return;
        }
        final Set<Map.Entry<K, V>> entrySet = map.entrySet();
        boolean isFirst = true;
        for (Map.Entry<K, V> entry : entrySet) {
            final V value = entry.getValue();
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(", ");
            }
            builder.append('\"').append(entry.getKey()).append('\"').append(':');

            switch (DataType.fromClassType((Class<?>) value)) {
            case CHAR:
                builder.append((int) value);
                break;
            case STRING:
                builder.append('\"').append(entry.getValue()).append('\"');
                break;
            default:
                builder.append(value);
                break;
            }
        }

        builder.append('}');
        hasFieldBefore = true;
    }

    @Override
    public void put(final FieldDescription fieldDescription, final boolean value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final boolean[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final boolean[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final byte value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final byte[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final byte[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final char value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final char[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final char[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final double value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final double[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final double[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final float value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final float[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final float[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final int value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final int[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final int[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final long value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final long[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final long[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final short value) {
        put(fieldDescription.getFieldName(), value);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final short[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final short[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final String string) {
        put(fieldDescription.getFieldName(), string);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final String[] values, final int n) {
        put(fieldDescription.getFieldName(), values, n);
    }

    @Override
    public void put(final FieldDescription fieldDescription, final String[] values, final int[] dims) {
        put(fieldDescription.getFieldName(), values, dims);
    }

    @Override
    public void put(final String fieldName, final boolean value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final boolean[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final boolean[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final byte value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final byte[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final byte[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final char value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append((int) value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final char[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append((int) values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append((int) values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final char[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final double value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final double[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final double[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final float value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final float[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final float[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final int value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final int[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final int[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final long value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final long[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final long[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final short value) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append(value);
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final short[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append(values[0]);
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", ").append(values[i]);
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final short[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public void put(final String fieldName, final String string) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": \"").append(string).append('\"');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final String[] values, final int n) {
        lineBreak();
        builder.append('\"').append(fieldName).append("\": ").append("[");
        if (values == null || values.length <= 0) {
            builder.append(']');
            return;
        }
        builder.append('\"').append(values[0]).append('\"');
        final int valuesSize = values.length;
        final int nElements = n >= 0 ? Math.min(n, valuesSize) : valuesSize;
        for (int i = 1; i < nElements; i++) {
            builder.append(", \"").append(values[i]).append('\"');
        }
        builder.append(']');
        hasFieldBefore = true;
    }

    @Override
    public void put(final String fieldName, final String[] values, final int[] dims) {
        put(fieldName, values, getNumberElements(dims));
    }

    @Override
    public int putArraySizeDescriptor(final int n) {
        return 0;
    }

    @Override
    public int putArraySizeDescriptor(final int[] dims) {
        return 0;
    }

    @Override
    public <E> WireDataFieldDescription putCustomData(final FieldDescription fieldDescription, final E obj, final Class<? extends E> type, final FieldSerialiser<E> serialiser) {
        return null;
    }

    @Override
    public void putEndMarker(final FieldDescription fieldDescription) {
        indentation = indentation.substring(0, Math.max(indentation.length() - DEFAULT_INDENTATION, 0));
        builder.append(LINE_BREAK).append(indentation).append(BRACKET_CLOSE).append(LINE_BREAK);
        hasFieldBefore = true;
        final byte[] outputStrBytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(outputStrBytes, 0, buffer.elements(), buffer.position(), outputStrBytes.length);
        buffer.position(buffer.position() + outputStrBytes.length);
        builder.setLength(0);
    }

    @Override
    public WireDataFieldDescription putFieldHeader(final String fieldName, final DataType dataType) {
        lastFieldHeader = new WireDataFieldDescription(this, parent, fieldName.hashCode(), fieldName, dataType, -1, 1, -1);
        queryFieldName = fieldName;
        return lastFieldHeader;
    }

    @Override
    public void putHeaderInfo(final FieldDescription... field) {
        hasFieldBefore = false;
        indentation = "";
        builder.setLength(0);
        putStartMarker(null);
    }

    @Override
    public void putStartMarker(final FieldDescription fieldDescription) {
        lineBreak();
        if (fieldDescription != null) {
            builder.append('\"').append(fieldDescription.getFieldName()).append("\": ");
        }
        builder.append(BRACKET_OPEN);
        indentation = indentation + " ".repeat(DEFAULT_INDENTATION);
        builder.append(LINE_BREAK);
        builder.append(indentation);
        hasFieldBefore = false;
    }

    public void serialiseObject(final Object obj) {
        if (obj == null) {
            // serialise null object
            builder.setLength(0);
            builder.append(BRACKET_OPEN).append(BRACKET_CLOSE);
            byte[] bytes = getSerialisedString().getBytes(Charset.defaultCharset());
            System.arraycopy(bytes, 0, buffer.elements(), buffer.position(), bytes.length);
            buffer.position(bytes.length);
            return;
        }
        try (ByteBufferOutputStream byteOutputStream = new ByteBufferOutputStream(java.nio.ByteBuffer.wrap(buffer.elements()), false)) {
            byteOutputStream.position(buffer.position());
            JsonStream.serialize(obj, byteOutputStream);
            buffer.position(byteOutputStream.position());
        } catch (IOException e) {
            throw new IllegalStateException(NOT_A_JSON_COMPATIBLE_PROTOCOL, e);
        }
    }

    @Override
    public void setBuffer(final IoBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void setPutFieldMetaData(final boolean putFieldMetaData) {
        this.putFieldMetaData = putFieldMetaData;
    }

    @Override
    public void setQueryFieldName(final String fieldName, final int dataStartPosition) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName must not be null or blank: " + fieldName);
        }
        if (root == null) {
            throw new IllegalArgumentException("JSON Any root hasn't been analysed/parsed yet");
        }
        this.queryFieldName = fieldName;
        // buffer.position(dataStartPosition); // N.B. not needed at this time
    }

    @Override
    public void updateDataEndMarker(final WireDataFieldDescription fieldHeader) {
        // not needed
    }

    private int getNumberElements(final int[] dims) {
        int n = 1;
        for (final int dim : dims) {
            n *= dim;
        }
        return n;
    }

    private void lineBreak() {
        if (hasFieldBefore) {
            builder.append(',');
            builder.append(LINE_BREAK);
            builder.append(indentation);
        }
    }

    private void parseIoStream(final WireDataFieldDescription fieldRoot, final Any any, final String fieldName) {
        if (!(any.object() instanceof Map) || any.size() == 0) {
            return;
        }

        final Map<String, Any> map = any.asMap();
        final WireDataFieldDescription putStartMarker = new WireDataFieldDescription(this, fieldRoot, fieldName.hashCode(), fieldName, DataType.START_MARKER, 0, -1, -1);
        for (Map.Entry<String, Any> child : map.entrySet()) {
            final String childName = child.getKey();
            final Any childAny = map.get(childName);
            final Object data = childAny.object();
            if (data instanceof Map) {
                parseIoStream(putStartMarker, childAny, childName);
            } else if (data != null) {
                new WireDataFieldDescription(this, putStartMarker, childName.hashCode(), childName, DataType.fromClassType(data.getClass()), 0, -1, -1);
            }
        }
        //add if necessary new WireDataFieldDescription(this, fieldRoot, fieldName.hashCode(), fieldName, DataType.END_MARKER, 0, -1, -1)
    }
}
