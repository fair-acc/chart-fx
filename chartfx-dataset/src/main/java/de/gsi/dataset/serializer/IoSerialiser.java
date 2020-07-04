package de.gsi.dataset.serializer;

import java.util.*;

import de.gsi.dataset.serializer.spi.ProtocolInfo;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;

public interface IoSerialiser {
    ProtocolInfo checkHeaderInfo();

    boolean getBoolean();

    boolean[] getBooleanArray();

    IoBuffer getBuffer();

    void setBuffer(final IoBuffer buffer);

    byte getByte();

    byte[] getByteArray();

    char[] getCharArray();

    char getCharacter();

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

    void put(boolean value);

    void put(boolean[] arrayValue);

    void put(boolean[] arrayValue, int[] dims);

    void put(byte value);

    void put(byte[] arrayValue);

    void put(byte[] arrayValue, int[] dims);

    void put(char value);

    void put(char[] arrayValue);

    void put(char[] arrayValue, int[] dims);

    <E> void put(Collection<E> collection);

    void put(double value);

    void put(double[] arrayValue);

    void put(double[] arrayValue, int[] dims);

    void put(Enum<?> enumeration);

    void put(float value);

    void put(float[] arrayValue);

    void put(float[] arrayValue, int[] dims);

    void put(int value);

    void put(int[] arrayValue);

    void put(int[] arrayValue, int[] dims);

    void put(long value);

    void put(long[] arrayValue);

    void put(long[] arrayValue, int[] dims);

    <K, V> void put(Map<K, V> map);

    void put(short value);

    void put(short[] arrayValue);

    void put(short[] arrayValue, // NOPMD
            int[] dims);

    void put(String value);

    void put(String[] arrayValue);

    void put(String[] arrayValue, int[] dims);

    void putEndMarker(String markerName);

    void putFieldHeader(final FieldDescription fieldDescription);

    void putFieldHeader(final String fieldName, DataType dataType);

    void putFieldHeader(final String fieldName, DataType dataType, int additionalSize);

    /**
     * Adds header and version information
     */
    void putHeaderInfo();

    void putStartMarker(String markerName);

    void updateDataEndMarker();
}
