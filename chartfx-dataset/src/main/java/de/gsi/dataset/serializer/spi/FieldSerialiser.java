package de.gsi.dataset.serializer.spi;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * default field serialiser implementation. The user needs to provide the reader and writer consumer lambdas to connect
 * to the given serialiser back-end implementation.
 * 
 * @author rstein
 */
public class FieldSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldSerialiser.class);
    private final Class<?> classPrototype;
    private final List<Class<?>> classGenericArguments;
    private final String name;
    private final String canonicalName;
    private final String simpleName;
    private final int cachedHashCode;
    protected FieldSerialiserFunction readerFunction;
    protected FieldSerialiserFunction writerFunction;

    /**
     * 
     * @param reader consumer executed when reading from the back-end serialiser implementation
     * @param writer consumer executed when writing to the back-end serialiser implementation
     * @param classPrototype applicable class/interface prototype reference for which the consumers are applicable (e.g.
     *        example 1: 'List.class' for List&lt;String&gt; or example 2: 'Map.class' for Map&lt;Integer, String&gt;)
     * @param classGenericArguments applicable generics definition (e.g. 'String.class' for List&lt;String&gt; or
     *        'Integer.class, String.class' resp.)
     */
    public FieldSerialiser(final FieldSerialiserFunction reader, final FieldSerialiserFunction writer,
            final Class<?> classPrototype, Class<?>... classGenericArguments) {
        if ((reader == null || writer == null) && LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(reader).addArgument(writer).log("caution: reader {} or writer {} is null");
        }
        if (classPrototype == null) {
            throw new IllegalArgumentException("classPrototype must not be null");
        }
        this.readerFunction = reader;
        this.writerFunction = writer;
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

    /**
     * 
     * @return canonical name of the class/interface description
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * 
     * @return class reference
     */
    public Class<?> getClassPrototype() {
        return classPrototype;
    }

    /**
     * 
     * @return class reference to generics arguments
     */
    public List<Class<?>> getGenericsPrototypes() {
        return classGenericArguments;
    }

    /**
     * 
     * @return consumer that is being executed for reading from the back-end serialiser implementation
     */
    public FieldSerialiserFunction getReaderFunction() {
        return readerFunction;
    }

    /**
     * 
     * @return simple name name of the class/interface description
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * 
     * @return consumer that is being executed for writing to the back-end serialiser implementation
     */
    public FieldSerialiserFunction getWriterFunction() {
        return writerFunction;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * used as lambda expression for user-level code to read/write data into the given serialiser back-end
     * implementation
     * 
     * @author rstein
     */
    public interface FieldSerialiserFunction {
        /**
         * Performs this operation on the given arguments.
         *
         * @param t the specific object reference for the given field
         * @param u the description for the given class member
         * @throws IllegalAccessException in case a forbidden field is being accessed
         */
        public void exec(Object t, ClassFieldDescription u) throws IllegalAccessException;
    }
}
