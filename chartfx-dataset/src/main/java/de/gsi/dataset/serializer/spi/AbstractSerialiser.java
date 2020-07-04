package de.gsi.dataset.serializer.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.FieldDescription;
import de.gsi.dataset.serializer.IoSerialiser;

/**
 * @author rstein
 */
public abstract class AbstractSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSerialiser.class);
    private static final Map<String, Constructor<Object>> CLASS_CONSTRUCTOR_MAP = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<FieldSerialiser>> classMap = new ConcurrentHashMap<>();
    protected final IoSerialiser ioSerialiser;
    protected Consumer<String> startMarkerFunction;
    protected Consumer<String> endMarkerFunction;

    public AbstractSerialiser(final IoSerialiser ioSerialiser) {
        if (ioSerialiser == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        this.ioSerialiser = ioSerialiser;
    }

    public void addClassDefinition(FieldSerialiser serialiser) {
        if (serialiser == null) {
            throw new IllegalArgumentException("serialiser must not be null");
        }
        if (serialiser.getClassPrototype() == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        if (serialiser.getGenericsPrototypes() == null) {
            throw new IllegalArgumentException("types must not be null");
        }
        final List<FieldSerialiser> list = knownClasses().computeIfAbsent(serialiser.getClassPrototype(),
                key -> new ArrayList<>());

        if (list.isEmpty() || !list.contains(serialiser)) {
            list.add(serialiser);
        }
    }

    protected boolean checkClassCompatibility(List<Class<?>> ref1, List<Class<?>> ref2) {
        if (ref1.size() != ref2.size()) {
            return false;
        }
        if (ref1.isEmpty() && ref2.isEmpty()) {
            return true;
        }

        for (int i = 0; i < ref1.size(); i++) {
            final Class<?> class1 = ref1.get(i);
            final Class<?> class2 = ref2.get(i);
            if (!class1.equals(class2) && !(class2.isAssignableFrom(class1))) {
                return false;
            }
        }

        return true;
    }

    public void checkSerialiserAvailability(final List<String> objList, final ClassFieldDescription field) {
        final boolean classIsKnown = isClassKnown(field.getType(), field.getActualTypeArguments());
        if (field.isSerializable() && !classIsKnown) {
            if (field.getChildren().isEmpty()) {
                final String fieldType = field.getTypeName() + field.getGenericFieldTypeString();
                if (!objList.contains(fieldType)) {
                    objList.add(fieldType);
                }
            } else {
                for (FieldDescription child : field.getChildren()) {
                    checkSerialiserAvailability(objList, (ClassFieldDescription) child);
                }
            }
        }
    }

    public abstract Object deserialiseObject(final Object obj) throws IllegalAccessException;

    public Optional<FieldSerialiser> findFieldSerialiserForKnownClassOrInterface(Class<?> clazz,
            List<Class<?>> classGenericArguments) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        final List<FieldSerialiser> directClassMatchList = classMap.get(clazz);
        if (directClassMatchList != null && !directClassMatchList.isEmpty()) {
            if (directClassMatchList.size() == 1 || classGenericArguments == null || classGenericArguments.isEmpty()) {
                return Optional.of(directClassMatchList.get(0));
            }
            // more than one possible serialiser implementation
            for (final FieldSerialiser entry : directClassMatchList) {
                if (checkClassCompatibility(classGenericArguments, entry.getGenericsPrototypes())) {
                    return Optional.of(entry);
                }
            }
            // found FieldSerialiser entry but not matching required generic types
        }

        // did not find FieldSerialiser entry by specific class -> search for assignable interface definitions

        final List<Class<?>> potentialMatchingKeys = this.knownClasses().keySet().stream().filter(k -> k.isAssignableFrom(clazz)).collect(Collectors.toList());
        if (potentialMatchingKeys.isEmpty()) {
            // did not find any matching clazz/interface FieldSerialiser entries
            return Optional.empty();
        }

        final List<FieldSerialiser> interfaceMatchList = potentialMatchingKeys.stream()
                                                                 .map(key -> this.knownClasses().get(key))
                                                                 .flatMap(List::stream)
                                                                 .collect(Collectors.toList());

        if (interfaceMatchList.size() == 1 || classGenericArguments == null || classGenericArguments.isEmpty()) {
            // found single match FieldSerialiser entry type w/o specific generics requirements
            return Optional.of(interfaceMatchList.get(0));
        }

        // more than one possible serialiser implementation
        for (final FieldSerialiser entry : interfaceMatchList) {
            if (checkClassCompatibility(classGenericArguments, entry.getGenericsPrototypes())) {
                // found generics matching or assignable entry
                return Optional.of(entry);
            }
        }
        // could not match with generics arguments

        // find generic serialiser entry w/o generics parameter requirements
        return interfaceMatchList.stream().filter(entry -> entry.getGenericsPrototypes().isEmpty()).findFirst();
    }

    public boolean isClassKnown(Class<?> clazz, List<Class<?>> classGenericArguments) {
        return findFieldSerialiserForKnownClassOrInterface(clazz, classGenericArguments).isPresent();
    }

    public Map<Class<?>, List<FieldSerialiser>> knownClasses() {
        return classMap;
    }

    public void serialiseObject(final Object obj) throws IllegalAccessException {
        serialiseObject(obj, ClassDescriptions.get(obj.getClass()), 0);
    }

    public void serialiseObject(final Object rootObj, final ClassFieldDescription root, final int recursionDepth)
            throws IllegalAccessException {
        final Class<?> fieldClass = root.getType();
        final List<Class<?>> fieldGenericTypes = root.getActualTypeArguments();

        if (isClassKnown(fieldClass, fieldGenericTypes) && recursionDepth != 0) {
            // serialise known class object
            final Optional<FieldSerialiser> serialiser = findFieldSerialiserForKnownClassOrInterface(fieldClass, root.getActualTypeArguments());
            if (serialiser.isEmpty()) {
                // should not happen (because of 'isClassKnown')
                throw new IllegalStateException("should not happen -- cannot serialise field - " + root.getFieldNameRelative() + " - class type = " + root.getTypeName());
            }

            // write field header
            ioSerialiser.putFieldHeader(root);
            serialiser.get().getWriterFunction().exec(rootObj, root);
            return;
        }
        // cannot serialise field check whether this is a container class and contains serialisable children

        if (root.getChildren().isEmpty()) {
            // no further children
            return;
        }

        // dive into it's children
        final String subClass = root.getFieldName();
        if (recursionDepth != 0 && startMarkerFunction != null) {
            startMarkerFunction.accept(subClass);
        }

        final Object newRoot = root.getField() == null ? rootObj : root.getField().get(rootObj);
        for (final FieldDescription fieldDescription : root.getChildren()) {
            ClassFieldDescription field = (ClassFieldDescription) fieldDescription;
            final Object reference = field.getField().get(newRoot);
            if (reference == null) {
                // only follow and serialise non-null references of sub-classes
                continue;
            }
            serialiseObject(newRoot, field, recursionDepth + 1);
        }

        if (recursionDepth != 0 && endMarkerFunction != null) {
            endMarkerFunction.accept(subClass);
        }
    }

    public static int computeHashCode(final Class<?> classPrototype, List<Class<?>> classGenericArguments) {
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
                return (Constructor<Object>) ClassDescriptions.getClassByName(key)
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

    public static String getGenericFieldSimpleTypeString(List<Class<?>> classArguments) {
        if (classArguments == null || classArguments.isEmpty()) {
            return "";
        }
        return classArguments.stream().map(Class::getSimpleName).collect(Collectors.joining(", ", "<", ">"));
    }
}
