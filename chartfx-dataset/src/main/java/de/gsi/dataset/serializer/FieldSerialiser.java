package de.gsi.dataset.serializer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.spi.ClassFieldDescription;

/**
 * default field serialiser implementation. The user needs to provide the reader and writer consumer lambdas to connect
 * to the given serialiser back-end implementation.
 * @param <R> function return type
 * @author rstein
 */
public class FieldSerialiser<R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldSerialiser.class);
    private final Class<?> classPrototype;
    private final List<Type> classGenericArguments;
    private final String name;
    private final String canonicalName;
    private final String simpleName;
    private final int cachedHashCode;
    protected TriConsumer readerFunction;
    protected TriConsumer writerFunction;
    protected TriFunction<R> returnFunction;

    /**
     *
     * @param reader consumer executed when reading from the back-end serialiser implementation
     * @param returnFunction function that is being executed for returning a new object to the back-end serialiser implementation
     * @param writer consumer executed when writing to the back-end serialiser implementation
     * @param classPrototype applicable class/interface prototype reference for which the consumers are applicable (e.g.
     *        example 1: 'List.class' for List&lt;String&gt; or example 2: 'Map.class' for Map&lt;Integer, String&gt;)
     * @param classGenericArguments applicable generics definition (e.g. 'String.class' for List&lt;String&gt; or
     *        'Integer.class, String.class' resp.)
     */
    public FieldSerialiser(final TriConsumer reader, final TriFunction<R> returnFunction, final TriConsumer writer, final Class<?> classPrototype, Class<?>... classGenericArguments) {
        if ((reader == null || returnFunction == null || writer == null)) {
            LOGGER.atWarn().addArgument(reader).addArgument(writer).log("caution: reader {}, return {} or writer {} is null");
        }
        if (classPrototype == null) {
            throw new IllegalArgumentException("classPrototype must not be null");
        }
        this.readerFunction = reader;
        this.returnFunction = returnFunction;
        this.writerFunction = writer;
        this.classPrototype = classPrototype;
        this.classGenericArguments = Arrays.asList(classGenericArguments);
        cachedHashCode = IoClassSerialiser.computeHashCode(classPrototype, this.classGenericArguments);

        String genericFieldString = this.classGenericArguments.isEmpty() ? "" : this.classGenericArguments.stream().map(Type::getTypeName).collect(Collectors.joining(", ", "<", ">"));

        canonicalName = classPrototype.getCanonicalName() + genericFieldString;
        simpleName = classPrototype.getSimpleName() + IoClassSerialiser.getGenericFieldSimpleTypeString(this.classGenericArguments);
        name = "Serialiser for " + canonicalName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
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
    public List<Type> getGenericsPrototypes() {
        return classGenericArguments;
    }

    /**
     * 
     * @return consumer that is being executed for reading from the back-end serialiser implementation
     */
    public TriConsumer getReaderFunction() {
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
    public TriConsumer getWriterFunction() {
        return writerFunction;
    }

    /**
     *
     * @return function that is being executed for returning a new object to the back-end serialiser implementation
     */
    public TriFunction<R> getReturnObjectFunction() {
        return returnFunction;
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
     * used as lambda expression for user-level code to read/write data into the given serialiser back-end implementation
     *
     * @author rstein
     */
    public interface TriConsumer {
        /**
         * Performs this operation on the given arguments.
         *
         * @param ioSerialiser the reference to the calling IoSerialiser
         * @param rootObj the specific root object reference the given field is part of
         * @param field the description for the given class member, if null then rootObj is written/read directly
         */
        void accept(IoSerialiser ioSerialiser, Object rootObj, ClassFieldDescription field);
    }

    /**
     * used as lambda expression for user-level code to return new object data (read-case) from the given serialiser back-end implementation
     *
     * @author rstein
     * @param <R> generic return type
     */
    public interface TriFunction<R> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param ioSerialiser the reference to the calling IoSerialiser
         * @param rootObj the specific root object reference the given field is part of
         * @param field the description for the given class member, if null then rootObj is written/read directly
         * @return The value of the field which is either taken from rootObj if present or compatible or newly allocated otherwise
         */
        R apply(IoSerialiser ioSerialiser, Object rootObj, ClassFieldDescription field);
    }
}
