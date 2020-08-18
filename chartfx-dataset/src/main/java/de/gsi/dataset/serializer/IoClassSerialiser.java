package de.gsi.dataset.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.spi.ClassFieldDescription;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;
import de.gsi.dataset.serializer.spi.iobuffer.FieldBoxedValueArrayHelper;
import de.gsi.dataset.serializer.spi.iobuffer.FieldBoxedValueHelper;
import de.gsi.dataset.serializer.spi.iobuffer.FieldCollectionsHelper;
import de.gsi.dataset.serializer.spi.iobuffer.FieldDataSetHelper;
import de.gsi.dataset.serializer.spi.iobuffer.FieldMapHelper;
import de.gsi.dataset.serializer.spi.iobuffer.FieldPrimitiveValueHelper;
import de.gsi.dataset.serializer.spi.iobuffer.FieldPrimitveValueArrayHelper;
import de.gsi.dataset.serializer.utils.ClassUtils;

/**
 * reference implementation for streaming arbitrary object classes to and from a IoSerialiser- and IoBuffer-based buffers
 *
 * @author rstein
 */
public class IoClassSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoClassSerialiser.class);
    private static final Map<String, Constructor<Object>> CLASS_CONSTRUCTOR_MAP = new ConcurrentHashMap<>();
    protected final IoSerialiser ioSerialiser;
    private final Map<Type, List<FieldSerialiser<?>>> classMap = new HashMap<>();
    private final Map<FieldSerialiserKey, FieldSerialiserValue> cachedFieldMatch = new HashMap<>();
    protected Consumer<FieldDescription> startMarkerFunction;
    protected Consumer<FieldDescription> endMarkerFunction;

    /**
     * Initialises new IoBuffer-backed object serialiser
     *
     * @param ioSerialiser the backing IoSerialiser (see e.g. {@link de.gsi.dataset.serializer.IoSerialiser}
     * @see de.gsi.dataset.serializer.spi.BinarySerialiser
     */
    public IoClassSerialiser(final IoSerialiser ioSerialiser) {
        if (ioSerialiser == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        this.ioSerialiser = ioSerialiser;
        startMarkerFunction = ioSerialiser::putStartMarker;
        endMarkerFunction = ioSerialiser::putEndMarker;

        // register primitive and boxed data type handlers
        FieldPrimitiveValueHelper.register(this);
        FieldPrimitveValueArrayHelper.register(this);
        FieldBoxedValueHelper.register(this);
        FieldBoxedValueArrayHelper.register(this);
        FieldCollectionsHelper.register(this);

        // Enum serialiser mapper to IoBuffer
        addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getEnum((Enum<?>) field.getField().get(obj))), // reader
                (io, obj, field) -> io.getEnum((Enum<?>) (field == null ? obj : field.getField().get(obj))), // return
                (io, obj, field) -> io.put(field, (Enum<?>) field.getField().get(obj)), // writer
                Enum.class));

        FieldMapHelper.register(this);
        FieldDataSetHelper.register(this);
    }

    public void addClassDefinition(FieldSerialiser<?> serialiser) {
        if (serialiser == null) {
            throw new IllegalArgumentException("serialiser must not be null");
        }
        if (serialiser.getClassPrototype() == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (serialiser.getGenericsPrototypes() == null) {
            throw new IllegalArgumentException("types must not be null");
        }
        synchronized (knownClasses()) {
            final List<FieldSerialiser<?>> list = knownClasses().computeIfAbsent(serialiser.getClassPrototype(), key -> new ArrayList<>());

            if (list.isEmpty() || !list.contains(serialiser)) {
                list.add(serialiser);
            }
        }
    }

    public <E> FieldSerialiser<E> cacheFindFieldSerialiser(Type clazz, List<Type> classGenericArguments) {
        // odd construction is needed since 'computeIfAbsent' cannot place 'null' element into the Map and since 'null' has a double interpretation of
        // a) a non-initialiser map value
        // b) a class for which no custom serialiser exist
        return (FieldSerialiser<E>) cachedFieldMatch.computeIfAbsent(new FieldSerialiserKey(clazz, classGenericArguments), key -> new FieldSerialiserValue(findFieldSerialiser(clazz, classGenericArguments))).get();
    }

    public Object deserialiseObject(final Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("obj must not be null (yet)");
        }

        final int startPosition = ioSerialiser.getBuffer().position();

        // match field header with class field description
        final ClassFieldDescription clazz = ClassUtils.getFieldDescription(obj.getClass());
        final FieldSerialiser<?> existingSerialiser = clazz.getFieldSerialiser();
        final FieldSerialiser<?> fieldSerialiser = existingSerialiser == null ? cacheFindFieldSerialiser(clazz.getType(), clazz.getActualTypeArguments()) : existingSerialiser;

        if (clazz.getFieldSerialiser() == null && fieldSerialiser != null) {
            clazz.setFieldSerialiser(fieldSerialiser);
        }

        ioSerialiser.getBuffer().position(startPosition);
        final FieldDescription fieldRoot = ioSerialiser.parseIoStream(true);

        if (fieldSerialiser != null) {
            // return new object
            final FieldDescription rawObjectFieldDescription = fieldRoot.getChildren().get(0).getChildren().get(0);
            ioSerialiser.getBuffer().position(rawObjectFieldDescription.getDataStartPosition());
            if (rawObjectFieldDescription.getDataType() == DataType.OTHER) {
                return ioSerialiser.getCustomData(fieldSerialiser);
            }
            return fieldSerialiser.getReturnObjectFunction().apply(ioSerialiser, obj, clazz);
        }
        // deserialise into object
        if (!fieldRoot.getChildren().isEmpty() && !fieldRoot.getChildren().get(0).getFieldName().isEmpty()) {
            for (final FieldDescription child : fieldRoot.getChildren()) {
                deserialise(obj, child, clazz, 0);
            }
            return obj;
        }

        // class reference is not known by name (ie. was empty) parse directly dependent children
        final List<FieldDescription> fieldRootChildren = fieldRoot.getChildren().get(0).getChildren();
        for (final FieldDescription fieldDescription : fieldRootChildren) {
            final ClassFieldDescription subFieldDescription = (ClassFieldDescription) clazz.findChildField(fieldDescription.getFieldNameHashCode(), fieldDescription.getFieldName());

            if (subFieldDescription != null) {
                deserialise(obj, fieldDescription, subFieldDescription, 1);
            }
        }

        return obj;
    }

    /**
     * find FieldSerialiser for known class, interface and corresponding generics
     * @param type the class or interface
     * @param classGenericArguments optional generics arguments
     * @return FieldSerialiser matching the base class/interface and generics arguments
     */
    public <E> FieldSerialiser<E> findFieldSerialiser(Type type, List<Type> classGenericArguments) {
        final Class<?> clazz = ClassUtils.getRawType(type);
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        final List<FieldSerialiser<?>> directClassMatchList = classMap.get(type);
        if (directClassMatchList != null && !directClassMatchList.isEmpty()) {
            if (directClassMatchList.size() == 1 || classGenericArguments == null || classGenericArguments.isEmpty()) {
                return (FieldSerialiser<E>) directClassMatchList.get(0);
            }
            // more than one possible serialiser implementation
            for (final FieldSerialiser<?> entry : directClassMatchList) {
                if (checkClassCompatibility(classGenericArguments, entry.getGenericsPrototypes())) {
                    return (FieldSerialiser<E>) entry;
                }
            }
            // found FieldSerialiser entry but not matching required generic types
        }

        // did not find FieldSerialiser entry by specific class -> search for assignable interface definitions

        final List<Class<?>> potentialMatchingKeys = new ArrayList<>(10);
        for (Type key : knownClasses().keySet()) {
            final Class<?> testClass = ClassUtils.getRawType(key);
            if (testClass.isAssignableFrom(clazz)) {
                potentialMatchingKeys.add(testClass);
            }
        }
        if (potentialMatchingKeys.isEmpty()) {
            // did not find any matching clazz/interface FieldSerialiser entries
            return null;
        }

        final List<FieldSerialiser<?>> interfaceMatchList = new ArrayList<>(10);
        for (Class<?> testClass : potentialMatchingKeys) {
            final List<FieldSerialiser<?>> fieldSerialisers = knownClasses().get(testClass);
            if (fieldSerialisers.isEmpty()) {
                continue;
            }
            interfaceMatchList.addAll(fieldSerialisers);
        }
        if (interfaceMatchList.size() == 1 || classGenericArguments == null || classGenericArguments.isEmpty()) {
            // found single match FieldSerialiser entry type w/o specific generics requirements
            return (FieldSerialiser<E>) interfaceMatchList.get(0);
        }

        // more than one possible serialiser implementation
        for (final FieldSerialiser<?> entry : interfaceMatchList) {
            if (checkClassCompatibility(classGenericArguments, entry.getGenericsPrototypes())) {
                // found generics matching or assignable entry
                return (FieldSerialiser<E>) entry;
            }
        }
        // could not match with generics arguments

        // find generic serialiser entry w/o generics parameter requirements
        return (FieldSerialiser<E>) interfaceMatchList.stream().filter(entry -> entry.getGenericsPrototypes().isEmpty()).findFirst().orElse(null);
    }

    public IoSerialiser getIoSerialiser() {
        return ioSerialiser;
    }

    public final <E> BiFunction<Type, Type[], FieldSerialiser<E>> getSerialiserLookupFunction() {
        return (primaryType, secondaryType) -> {
            if (primaryType == null) {
                throw new IllegalArgumentException("no serialiser implementation found for classType = " + primaryType);
            }
            return (FieldSerialiser<E>) cacheFindFieldSerialiser(ClassUtils.getRawType(primaryType), secondaryType == null ? Collections.emptyList() : Arrays.asList(secondaryType));
        };
    }

    public Map<Type, List<FieldSerialiser<?>>> knownClasses() {
        return classMap;
    }

    public void serialiseObject(final Object rootObj, final ClassFieldDescription classField, final int recursionDepth) {
        final FieldSerialiser<?> existingSerialiser = classField.getFieldSerialiser();
        final FieldSerialiser fieldSerialiser = existingSerialiser == null ? cacheFindFieldSerialiser(classField.getType(), classField.getActualTypeArguments()) : existingSerialiser;

        if (fieldSerialiser != null && recursionDepth != 0) {
            if (existingSerialiser == null) {
                classField.setFieldSerialiser(fieldSerialiser);
            }
            // write field header
            if (classField.getDataType() == DataType.OTHER) {
                final WireDataFieldDescription header = ioSerialiser.putFieldHeader(classField.getFieldName(), classField.getDataType());
                fieldSerialiser.getWriterFunction().accept(ioSerialiser, rootObj, classField);
                ioSerialiser.updateDataEndMarker(header);
            } else {
                fieldSerialiser.getWriterFunction().accept(ioSerialiser, rootObj, classField);
            }
            return;
        }
        // cannot serialise field check whether this is a container class and contains serialisable children

        if (classField.getChildren().isEmpty()) {
            // no further children
            return;
        }

        // dive into it's children
        if (recursionDepth != 0 && startMarkerFunction != null) {
            startMarkerFunction.accept(classField);
        }

        final Object newRoot = classField.getField() == null ? rootObj : classField.getField().get(rootObj);
        for (final FieldDescription fieldDescription : classField.getChildren()) {
            ClassFieldDescription field = (ClassFieldDescription) fieldDescription;

            if (!field.isPrimitive()) {
                final Object reference = field.getField().get(newRoot);
                if (!field.isPrimitive() && reference == null) {
                    // only follow and serialise non-null references of sub-classes
                    continue;
                }
            }
            serialiseObject(newRoot, field, recursionDepth + 1);
        }

        if (recursionDepth != 0 && endMarkerFunction != null) {
            endMarkerFunction.accept(classField);
        }
    }

    public void serialiseObject(final Object obj) {
        if (obj == null) {
            // serialise null object
            ioSerialiser.putHeaderInfo();
            final String dataEndMarkerName = "OBJ_ROOT_END";
            final WireDataFieldDescription dataEndMarker = new WireDataFieldDescription(null, dataEndMarkerName.hashCode(), dataEndMarkerName, DataType.START_MARKER, -1, -1, -1);
            ioSerialiser.putEndMarker(dataEndMarker);
            return;
        }

        final ClassFieldDescription classField = ClassUtils.getFieldDescription(obj.getClass());
        final FieldSerialiser<?> existingSerialiser = classField.getFieldSerialiser();
        final FieldSerialiser fieldSerialiser = existingSerialiser == null ? cacheFindFieldSerialiser(classField.getType(), classField.getActualTypeArguments()) : existingSerialiser;

        if (fieldSerialiser == null) {
            ioSerialiser.putHeaderInfo(classField);
            serialiseObject(obj, classField, 0);
            ioSerialiser.putEndMarker(classField);
        } else {
            if (existingSerialiser == null) {
                classField.setFieldSerialiser(fieldSerialiser);
            }
            ioSerialiser.putHeaderInfo();
            ioSerialiser.putCustomData(classField, obj, obj.getClass(), fieldSerialiser);
            final String dataEndMarkerName = "OBJ_ROOT_END";
            final WireDataFieldDescription dataEndMarker = new WireDataFieldDescription(null, dataEndMarkerName.hashCode(), dataEndMarkerName, DataType.START_MARKER, -1, -1, -1);
            ioSerialiser.putEndMarker(dataEndMarker);
        }
    }

    protected boolean checkClassCompatibility(final List<Type> ref1, final List<Type> ref2) {
        if (ref1.size() != ref2.size()) {
            return false;
        }
        if (ref1.isEmpty() && ref2.isEmpty()) {
            return true;
        }

        for (int i = 0; i < ref1.size(); i++) {
            final Class<?> class1 = ClassUtils.getRawType(ref1.get(i));
            final Class<?> class2 = ClassUtils.getRawType(ref2.get(i));
            if (!class1.equals(class2) && !(class2.isAssignableFrom(class1))) {
                return false;
            }
        }

        return true;
    }

    protected void deserialise(final Object obj, final FieldDescription fieldRoot, final ClassFieldDescription classField, final int recursionDepth) {
        final FieldSerialiser existingSerialiser = classField.getFieldSerialiser();
        final FieldSerialiser<?> fieldSerialiser = existingSerialiser == null ? cacheFindFieldSerialiser(classField.getType(), classField.getActualTypeArguments()) : existingSerialiser;

        if (fieldSerialiser != null) {
            if (existingSerialiser == null) {
                classField.setFieldSerialiser(fieldSerialiser);
            }
            ioSerialiser.getBuffer().position(fieldRoot.getDataStartPosition());
            classField.getFieldSerialiser().getReaderFunction().accept(ioSerialiser, obj, classField);
            return;
        }

        if (fieldRoot.getFieldNameHashCode() != classField.getFieldNameHashCode() /*|| !fieldRoot.getFieldName().equals(classField.getFieldName())*/) {
            // did not find matching (sub-)field in class
            if (fieldRoot.getChildren().isEmpty()) {
                return;
            }
            // check for potential inner fields
            for (final FieldDescription fieldDescription : fieldRoot.getChildren()) {
                final ClassFieldDescription subFieldDescription = (ClassFieldDescription) classField.findChildField(fieldDescription.getFieldNameHashCode(), fieldDescription.getFieldName());

                if (subFieldDescription != null) {
                    deserialise(obj, fieldDescription, subFieldDescription, recursionDepth + 1);
                }
            }
            return;
        }

        final Class<?> fieldClass = ClassUtils.getRawType(classField.getType());
        if (classField.isFinal() && !fieldClass.isInterface()) {
            // cannot set final variables
            LOGGER.atWarn().addArgument(classField.getParent()).addArgument(classField.getFieldName()).log("cannot (read: better should not) set final field '{}-{}'");
            return;
        }

        final Object ref = classField.getField() == null ? obj : classField.getField().get(obj);
        final Object subRef;
        if (ref == null) {
            subRef = classField.allocateMemberClassField(obj);
        } else {
            subRef = ref;
        }

        // no specific deserialiser present check for potential inner fields
        for (final FieldDescription fieldDescription : fieldRoot.getChildren()) {
            final ClassFieldDescription subFieldDescription = (ClassFieldDescription) classField.findChildField(fieldDescription.getFieldNameHashCode(), fieldDescription.getFieldName());

            if (subFieldDescription != null) {
                deserialise(subRef, fieldDescription, subFieldDescription, recursionDepth + 1);
            }
        }
    }

    public static int computeHashCode(final Class<?> classPrototype, List<Type> classGenericArguments) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classPrototype == null) ? 0 : classPrototype.getName().hashCode());
        if (classGenericArguments == null || classGenericArguments.isEmpty()) {
            return result;
        }
        for (final Type arg : classGenericArguments) {
            result = prime * result + ((arg == null) ? 0 : arg.getTypeName().hashCode());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static Constructor<Object> getClassConstructorByName(final String name, Class<?>... parameterTypes) {
        return CLASS_CONSTRUCTOR_MAP.computeIfAbsent(name, key -> {
            try {
                return (Constructor<Object>) ClassUtils.getClassByName(key)
                        .getDeclaredConstructor(parameterTypes);
            } catch (SecurityException | NoSuchMethodException e) {
                LOGGER.atError().setCause(e).addArgument(Arrays.toString(parameterTypes)).addArgument(name).log("exception while getting constructor{} for class {}");
                return null;
            }
        });
    }

    public static String[] getClassNames(List<Class<?>> classGenericArguments) {
        if (classGenericArguments == null) {
            return new String[0];
        }
        final String[] argStrings = new String[classGenericArguments.size()];
        for (int i = 0; i < argStrings.length; i++) {
            argStrings[i] = classGenericArguments.get(i).getName();
        }
        return argStrings;
    }

    public static String getGenericFieldSimpleTypeString(List<Type> classArguments) {
        if (classArguments == null || classArguments.isEmpty()) {
            return "";
        }
        return classArguments.stream().map(Type::getTypeName).collect(Collectors.joining(", ", "<", ">"));
    }

    private static class FieldSerialiserKey {
        private final Type clazz;
        private final List<Type> classGenericArguments;

        private FieldSerialiserKey(Type clazz, List<Type> classGenericArguments) {
            this.clazz = clazz;
            this.classGenericArguments = classGenericArguments;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final FieldSerialiserKey that = (FieldSerialiserKey) o;
            return clazz.equals(that.clazz) && classGenericArguments.equals(that.classGenericArguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, classGenericArguments);
        }

        @Override
        public String toString() {
            return "FieldSerialiserKey{"
                    + "clazz=" + clazz + ", classGenericArguments=" + classGenericArguments + '}';
        }
    }

    private static class FieldSerialiserValue {
        private final FieldSerialiser<?> fieldSerialiser;

        private FieldSerialiserValue(FieldSerialiser<?> fieldSerialiser) {
            this.fieldSerialiser = fieldSerialiser;
        }

        private FieldSerialiser<?> get() {
            return fieldSerialiser;
        }
    }
}
