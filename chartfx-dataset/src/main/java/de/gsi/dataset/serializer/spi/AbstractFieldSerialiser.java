package de.gsi.dataset.serializer.spi;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.FieldSerialiser;

/**
 * @author rstein
 */
public abstract class AbstractFieldSerialiser implements FieldSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFieldSerialiser.class);
    private final Class<?> classPrototype;
    private final List<Class<?>> classGenericArguments;
    private final String name;
    private final String canonicalName;
    private final String simpleName;
    private final int cachedHashCode;

    public AbstractFieldSerialiser(final Class<?> classPrototype, Class<?>... classGenericArguments) {
        if (classPrototype == null) {
            throw new IllegalArgumentException("classPrototype must not be null");
        }
        this.classPrototype = classPrototype;
        this.classGenericArguments = Arrays.asList(classGenericArguments);
        cachedHashCode = AbstractSerialiser.computeHashCode(classPrototype, this.classGenericArguments);

        String genericFieldString = this.classGenericArguments.isEmpty() ? ""
                : this.classGenericArguments.stream().map(Class::getName).collect(Collectors.joining(", ", "<", ">"));

        canonicalName = classPrototype.getCanonicalName() + genericFieldString;
        simpleName = classPrototype.getSimpleName()
                + AbstractSerialiser.getGenericFieldSimpleTypeString(this.classGenericArguments);
        name = "Serialiser for " + canonicalName;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(classPrototype.getName()).addArgument(genericFieldString)
                    .log("init class {}{}");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public Class<?> getClassPrototype() {
        return classPrototype;
    }

    @Override
    public List<Class<?>> getGenericsPrototypes() {
        return classGenericArguments;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return name;
    }
}
