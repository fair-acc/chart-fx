package de.gsi.dataset.serializer;

import java.util.*;

import de.gsi.dataset.serializer.spi.ProtocolInfo;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;

public interface IoSerialiser {
    // TODO: needs to be modified/simplified
    void adjustDataByteSizeBlock(final long sizeMarkerStart);

    IoBuffer getBuffer();

    void setBuffer(final IoBuffer buffer);

    ProtocolInfo checkHeaderInfo();

    boolean getBoolean();

    boolean[] getBooleanArray();

    byte getByte();

    byte[] getByteArray();

    char getCharacter();

    char[] getCharArray();

    <E> Collection<E> getCollection(Collection<E> collection);

    double getDouble();

    double[] getDoubleArray();

    double[] getDoubleArray(DataType dataType);

    <E extends Enum<E>> Enum<E> getEnum(Enum<E> enumeration);

    String getEnumTypeList();

    WireDataFieldDescription getFieldHeader();

    float getFloat();

    float[] getFloatArray();

    int[] getIntArray();

    int getInteger();

    <E> List<E> getList(List<E> collection);

    long getLong();

    long[] getLongArray();

    <K, V> Map<K, V> getMap(Map<K, V> map);

    <E> Queue<E> getQueue(Queue<E> collection);

    <E> Set<E> getSet(Set<E> collection);

    short getShort();

    short[] getShortArray();

    String getString();

    String[] getStringArray();

    WireDataFieldDescription parseIoStream();

    void put(String fieldName, boolean value);

    void put(String fieldName, boolean[] arrayValue);

    void put(String fieldName, boolean[] arrayValue,
            int[] dims);

    void put(String fieldName, byte value);

    void put(String fieldName, byte[] arrayValue);

    void put(String fieldName, byte[] arrayValue, int[] dims);

    void put(String fieldName, char value);

    void put(String fieldName, char[] arrayValue);

    void put(String fieldName, char[] arrayValue, int[] dims);

    <E> void put(String fieldName, Collection<E> collection);

    void put(String fieldName, double value);

    void put(String fieldName, double[] arrayValue);

    void put(String fieldName, double[] arrayValue, int[] dims);

    void put(String fieldName, Enum<?> enumeration);

    void put(String fieldName, float value);

    void put(String fieldName, float[] arrayValue);

    void put(String fieldName, float[] arrayValue, int[] dims);

    void put(String fieldName, int value);

    void put(String fieldName, int[] arrayValue);

    void put(String fieldName, int[] arrayValue, int[] dims);

    void put(String fieldName, long value);

    void put(String fieldName, long[] arrayValue);

    void put(String fieldName, long[] arrayValue, int[] dims);

    <K, V> void put(String fieldName, Map<K, V> map);

    void put(String fieldName, short value);

    void put(String fieldName, short[] arrayValue);

    void put(String fieldName, short[] arrayValue, // NOPMD
            int[] dims);

    void put(String fieldName, String value);

    void put(String fieldName, String[] arrayValue);

    void put(String fieldName, String[] arrayValue, int[] dims);

    long putArrayHeader(String fieldName, DataType dataType,
            int[] dims, int nElements);

    void putEndMarker(String markerName);

    void putFieldHeader(String fieldName, DataType dataType);

    void putFieldHeader(String fieldName, DataType dataType, int additionalSize);

    /**
     * Adds header and version information
     */
    void putHeaderInfo();

    void putStartMarker(String markerName);
}
