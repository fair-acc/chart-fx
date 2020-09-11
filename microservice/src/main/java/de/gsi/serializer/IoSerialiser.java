package de.gsi.serializer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import de.gsi.serializer.spi.ProtocolInfo;
import de.gsi.serializer.spi.WireDataFieldDescription;

public interface IoSerialiser {
    /**
     * Reads and checks protocol header information.
     * @return ProtocolInfo info Object (extends FieldHeader)
     * @throws IllegalStateException in case the format is incompatible with this serialiser
     */
    ProtocolInfo checkHeaderInfo();

    void setQueryFieldName(String fieldName, final int dataStartPosition);

    int[] getArraySizeDescriptor();

    boolean getBoolean(); // NOPMD by rstein

    default boolean[] getBooleanArray() {
        return getBooleanArray(null, 0);
    }

    default boolean[] getBooleanArray(final boolean[] dst) {
        return getBooleanArray(dst, dst == null ? -1 : dst.length);
    }

    boolean[] getBooleanArray(final boolean[] dst, final int length);

    IoBuffer getBuffer();

    void setBuffer(IoBuffer buffer);

    byte getByte();

    default byte[] getByteArray() {
        return getByteArray(null, 0);
    }

    default byte[] getByteArray(final byte[] dst) {
        return getByteArray(dst, dst == null ? -1 : dst.length);
    }

    byte[] getByteArray(final byte[] dst, final int length);

    char getChar();

    default char[] getCharArray() {
        return getCharArray(null, 0);
    }

    default char[] getCharArray(final char[] dst) {
        return getCharArray(dst, dst == null ? -1 : dst.length);
    }

    char[] getCharArray(final char[] dst, final int length);

    <E> Collection<E> getCollection(Collection<E> collection);

    <E> E getCustomData(FieldSerialiser<E> serialiser);

    double getDouble();

    default double[] getDoubleArray() {
        return getDoubleArray(null, 0);
    }

    default double[] getDoubleArray(final double[] dst) {
        return getDoubleArray(dst, dst == null ? -1 : dst.length);
    }

    double[] getDoubleArray(final double[] dst, final int length);

    <E extends Enum<E>> Enum<E> getEnum(Enum<E> enumeration);

    String getEnumTypeList();

    WireDataFieldDescription getFieldHeader();

    float getFloat();

    default float[] getFloatArray() {
        return getFloatArray(null, 0);
    }

    default float[] getFloatArray(final float[] dst) {
        return getFloatArray(dst, dst == null ? -1 : dst.length);
    }

    float[] getFloatArray(final float[] dst, final int length);

    int getInt();

    default int[] getIntArray() {
        return getIntArray(null, 0);
    }

    default int[] getIntArray(final int[] dst) {
        return getIntArray(dst, dst == null ? -1 : dst.length);
    }

    int[] getIntArray(final int[] dst, final int length);

    <E> List<E> getList(List<E> collection);

    long getLong();

    default long[] getLongArray() {
        return getLongArray(null, 0);
    }

    default long[] getLongArray(final long[] dst) {
        return getLongArray(dst, dst == null ? -1 : dst.length);
    }

    long[] getLongArray(final long[] dst, final int length);

    <K, V, E> Map<K, V> getMap(Map<K, V> map);

    <E> Queue<E> getQueue(Queue<E> collection);

    <E> Set<E> getSet(Set<E> collection);

    short getShort(); // NOPMD by rstein

    default short[] getShortArray() { // NOPMD by rstein
        return getShortArray(null, 0);
    }

    default short[] getShortArray(final short[] dst) { // NOPMD by rstein
        return getShortArray(dst, dst == null ? -1 : dst.length);
    }

    short[] getShortArray(final short[] dst, final int length); // NOPMD by rstein

    String getString();

    default String[] getStringArray() {
        return getStringArray(null, 0);
    }

    default String[] getStringArray(final String[] dst) {
        return getStringArray(dst, dst == null ? -1 : dst.length);
    }

    String[] getStringArray(final String[] dst, final int length);

    String getStringISO8859();

    boolean isPutFieldMetaData();

    void setPutFieldMetaData(boolean putFieldMetaData);

    WireDataFieldDescription parseIoStream(boolean readHeader);

    <E> void put(FieldDescription fieldDescription, Collection<E> collection, Type valueType);

    void put(FieldDescription fieldDescription, Enum<?> enumeration);

    <K, V, E> void put(FieldDescription fieldDescription, Map<K, V> map, Type keyType, Type valueType);

    <E> void put(String fieldName, Collection<E> collection, Type valueType);

    void put(String fieldName, Enum<?> enumeration);

    <K, V, E> void put(String fieldName, Map<K, V> map, Type keyType, Type valueType);

    default void put(FieldDescription fieldDescription, final boolean[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final byte[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final char[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final double[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final float[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final int[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final long[] src) {
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final short[] src) { // NOPMD
        put(fieldDescription, src, -1);
    }

    default void put(FieldDescription fieldDescription, final String[] src) {
        put(fieldDescription, src, -1);
    }

    void put(FieldDescription fieldDescription, boolean value);

    void put(FieldDescription fieldDescription, boolean[] values, int n);

    void put(FieldDescription fieldDescription, boolean[] values, int[] dims);

    void put(FieldDescription fieldDescription, byte value);

    void put(FieldDescription fieldDescription, byte[] values, int n);

    void put(FieldDescription fieldDescription, byte[] values, int[] dims);

    void put(FieldDescription fieldDescription, char value);

    void put(FieldDescription fieldDescription, char[] values, int n);

    void put(FieldDescription fieldDescription, char[] values, int[] dims);

    void put(FieldDescription fieldDescription, double value);

    void put(FieldDescription fieldDescription, double[] values, int n);

    void put(FieldDescription fieldDescription, double[] values, int[] dims);

    void put(FieldDescription fieldDescription, float value);

    void put(FieldDescription fieldDescription, float[] values, int n);

    void put(FieldDescription fieldDescription, float[] values, int[] dims);

    void put(FieldDescription fieldDescription, int value);

    void put(FieldDescription fieldDescription, int[] values, int n);

    void put(FieldDescription fieldDescription, int[] values, int[] dims);

    void put(FieldDescription fieldDescription, long value);

    void put(FieldDescription fieldDescription, long[] values, int n);

    void put(FieldDescription fieldDescription, long[] values, int[] dims);

    void put(FieldDescription fieldDescription, short value);

    void put(FieldDescription fieldDescription, short[] values, int n);

    void put(FieldDescription fieldDescription, short[] values, int[] dims);

    void put(FieldDescription fieldDescription, String string);

    void put(FieldDescription fieldDescription, String[] values, int n);

    void put(FieldDescription fieldDescription, String[] values, int[] dims);

    default void put(String fieldName, final boolean[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final byte[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final char[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final double[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final float[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final int[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final long[] src) {
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final short[] src) { // NOPMD
        put(fieldName, src, -1);
    }

    default void put(String fieldName, final String[] src) {
        put(fieldName, src, -1);
    }

    void put(String fieldName, boolean value);

    void put(String fieldName, boolean[] values, int n);

    void put(String fieldName, boolean[] values, int[] dims);

    void put(String fieldName, byte value);

    void put(String fieldName, byte[] values, int n);

    void put(String fieldName, byte[] values, int[] dims);

    void put(String fieldName, char value);

    void put(String fieldName, char[] values, int n);

    void put(String fieldName, char[] values, int[] dims);

    void put(String fieldName, double value);

    void put(String fieldName, double[] values, int n);

    void put(String fieldName, double[] values, int[] dims);

    void put(String fieldName, float value);

    void put(String fieldName, float[] values, int n);

    void put(String fieldName, float[] values, int[] dims);

    void put(String fieldName, int value);

    void put(String fieldName, int[] values, int n);

    void put(String fieldName, int[] values, int[] dims);

    void put(String fieldName, long value);

    void put(String fieldName, long[] values, int n);

    void put(String fieldName, long[] values, int[] dims);

    void put(String fieldName, short value);

    void put(String fieldName, short[] values, int n);

    void put(String fieldName, short[] values, int[] dims);

    void put(String fieldName, String string);

    void put(String fieldName, String[] values, int n);

    void put(String fieldName, String[] values, int[] dims);

    int putArraySizeDescriptor(int n);

    int putArraySizeDescriptor(int[] dims);

    <E> WireDataFieldDescription putCustomData(FieldDescription fieldDescription, E obj, Class<? extends E> type, FieldSerialiser<E> serialiser);

    void putEndMarker(FieldDescription fieldDescription);

    WireDataFieldDescription putFieldHeader(FieldDescription fieldDescription);

    WireDataFieldDescription putFieldHeader(String fieldName, DataType dataType);

    /**
     * Adds header and version information
     * @param field optional FieldDescription (ie. to allow to attach MetaData to the start/stop marker)
     */
    void putHeaderInfo(FieldDescription... field);

    void putStartMarker(FieldDescription fieldDescription);

    void updateDataEndMarker(WireDataFieldDescription fieldHeader);

    void setFieldSerialiserLookupFunction(BiFunction<Type, Type[], FieldSerialiser<Object>> serialiserLookupFunction);

    BiFunction<Type, Type[], FieldSerialiser<Object>> getSerialiserLookupFunction();
}
