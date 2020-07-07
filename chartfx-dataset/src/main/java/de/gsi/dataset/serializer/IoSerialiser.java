package de.gsi.dataset.serializer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import de.gsi.dataset.serializer.spi.ProtocolInfo;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;

public interface IoSerialiser {
    /**
     * Reads and checks protocol header information.
     * @return ProtocolInfo info Object (extends FieldHeader)
     * @throws IllegalStateException in case the format is incompatible with this serialiser
     */
    ProtocolInfo checkHeaderInfo();

    IoBuffer getBuffer();

    <E> Collection<E> getCollection(Collection<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    <E> E getCustomData(FieldSerialiser<E> serialiser);

    <E extends Enum<E>> Enum<E> getEnum(Enum<E> enumeration);

    String getEnumTypeList();

    WireDataFieldDescription getFieldHeader();

    <E> List<E> getList(List<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    <K, V, E> Map<K, V> getMap(Map<K, V> map, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    WireDataFieldDescription getParent();

    <E> Queue<E> getQueue(Queue<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    <E> Set<E> getSet(Set<E> collection, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    boolean isPutFieldMetaData();

    WireDataFieldDescription parseIoStream(boolean readHeader);

    <E> void put(Collection<E> collection, Type valueType, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    void put(Enum<?> enumeration);

    <K, V, E> void put(Map<K, V> map, Type keyType, Type valueType, BiFunction<Type, Type[], FieldSerialiser<E>> serialiserLookup);

    <E> WireDataFieldDescription putCustomData(FieldDescription fieldDescription, E obj, Class<? extends E> type, FieldSerialiser<E> serialiser);

    void putEndMarker(String markerName);

    WireDataFieldDescription putFieldHeader(FieldDescription fieldDescription);

    WireDataFieldDescription putFieldHeader(String fieldName, DataType dataType);

    /**
     * Adds header and version information
     * @param field optional FieldDescription (ie. to allow to attach MetaData to the start/stop marker)
     */
    void putHeaderInfo(FieldDescription... field);

    void putStartMarker(String markerName);

    void putStartMarker(FieldDescription fieldDescription);

    void setBuffer(IoBuffer buffer);

    void setPutFieldMetaData(boolean putFieldMetaData);

    void updateDataEndMarker(WireDataFieldDescription fieldHeader);
}
