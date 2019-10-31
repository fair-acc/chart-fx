package de.gsi.dataset.serializer;

import java.util.List;

import de.gsi.dataset.serializer.spi.ClassFieldDescription;

/**
 * @author rstein
 */
public interface FieldSerialiser {

    String getCanonicalName();

    Class<?> getClassPrototype();

    List<Class<?>> getGenericsPrototypes();

    String getSimpleName();

    void readFrom(final Object obj, final ClassFieldDescription field) throws IllegalAccessException;

    void writeTo(final Object obj, final ClassFieldDescription field) throws IllegalAccessException;
}
