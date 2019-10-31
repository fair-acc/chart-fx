package de.gsi.dataset.serializer.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;

/**
 * @author rstein
 */
public class ClassFieldDescription implements Iterable<ClassFieldDescription> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFieldDescription.class);
    private static final int WILDCARD_EXTENDS_LENGTH = "? extends ".length();
    public static int maxRecursionLevel = 10;
    private String toStringName; // computed on demand and cached
    private final int hierarchyDepth;
    private final Field field; // since we cannot derive from 'final class Field'
    private final String fieldName;
    private final String fieldNameRelative;
    private final Optional<ClassFieldDescription> parent;
    private final List<ClassFieldDescription> children = new ArrayList<>();
    private final Map<String, ClassFieldDescription> hashMap = new ConcurrentHashMap<>();

    private final Class<?> classType;
    private final DataType dataType;
    private final String typeName;
    private Type genericType; // computed on demand and cached
    private List<Class<?>> genericTypeList; // computed on demand and cached

    private List<String> genericTypeNameList; // computed on demand and cached
    private String genericTypeNames; // computed on demand and cached
    private final int modifierID;
    private String modifierStr; // computed on demand and cached

    private final boolean modPublic;
    private final boolean modProtected;

    private final boolean modPrivate;
    // field modifier in canonical order
    private final boolean modAbstract;
    private final boolean modStatic;
    private final boolean modFinal;
    private final boolean modTransient;
    private final boolean modVolatile;
    private final boolean modSynchronized;
    private final boolean modNative;
    private final boolean modStrict;
    private final boolean modInterface;
    // additional qualities
    private final boolean isprimitive;
    private final boolean isclass;

    private final boolean serializable;
    // access counter (N.B. to check if data has been modified)
    private final AtomicInteger readCount = new AtomicInteger();
    private final AtomicInteger writeCount = new AtomicInteger();

    /**
     * This should be called only once with the root class as an argument
     *
     * @param referenceClass the root node containing further Field children
     * @param fullScan       {@code true} if the class field should be serialised
     *                       according to {@link java.io.Serializable} (ie. object's
     *                       non-static and non-transient fields); {@code false}
     *                       otherwise.
     */
    public ClassFieldDescription(final Class<?> referenceClass, final boolean fullScan) {
        super();
        if (referenceClass == null) {
            throw new IllegalArgumentException("object must not be null");
        }
        hierarchyDepth = 0;
        field = null; // it's a root, no field definition available
        classType = referenceClass;
        fieldName = classType.getName();
        parent = Optional.empty();
        fieldNameRelative = "";

        genericType = classType.getGenericSuperclass();
        dataType = dataTypeFomClassType(classType);
        typeName = ClassDescriptions.translateClassName(classType.getTypeName());
        modifierID = classType.getModifiers();

        modPublic = Modifier.isPublic(modifierID);
        modProtected = Modifier.isProtected(modifierID);
        modPrivate = Modifier.isPrivate(modifierID);

        modAbstract = Modifier.isAbstract(modifierID);
        modStatic = Modifier.isStatic(modifierID);
        modFinal = Modifier.isFinal(modifierID);
        modTransient = Modifier.isTransient(modifierID);
        modVolatile = Modifier.isVolatile(modifierID);
        modSynchronized = Modifier.isSynchronized(modifierID);
        modNative = Modifier.isNative(modifierID);
        modStrict = Modifier.isStrict(modifierID);
        modInterface = classType.isInterface();

        // additional fields
        isprimitive = classType.isPrimitive();
        isclass = !isprimitive && !modInterface;
        serializable = !modTransient && !modStatic;

        // parse object
        exploreClass(classType, this, 0, fullScan);
    }

    public ClassFieldDescription(final Field field, final ClassFieldDescription parent, final int recursionLevel,
            final boolean fullScan) {
        super();
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        hierarchyDepth = recursionLevel;
        this.field = field;
        fieldName = field.getName();
        this.parent = parent == null ? Optional.empty() : Optional.of(parent);
        fieldNameRelative = (this.parent.isPresent()
                ? this.parent.get().isRoot() ? "" : (this.parent.get().getFieldNameRelative() + ".")
                : "") + fieldName;

        classType = field.getType();
        dataType = dataTypeFomClassType(classType);
        typeName = ClassDescriptions.translateClassName(classType.getTypeName());
        modifierID = field.getModifiers();

        modPublic = Modifier.isPublic(modifierID);
        modProtected = Modifier.isProtected(modifierID);
        modPrivate = Modifier.isPrivate(modifierID);

        modAbstract = Modifier.isAbstract(modifierID);
        modStatic = Modifier.isStatic(modifierID);
        modFinal = Modifier.isFinal(modifierID);
        modTransient = Modifier.isTransient(modifierID);
        modVolatile = Modifier.isVolatile(modifierID);
        modSynchronized = Modifier.isSynchronized(modifierID);
        modNative = Modifier.isNative(modifierID);
        modStrict = Modifier.isStrict(modifierID);
        modInterface = classType.isInterface();

        // additional fields
        isprimitive = classType.isPrimitive();
        isclass = !isprimitive && !modInterface;
        serializable = !modTransient && !modStatic;

        // enable access by default (saves performance later on)
        field.setAccessible(true);

        // add child to parent if it serializable or if a full scan is requested
        if (this.parent.isPresent() && (serializable || fullScan)) {
            this.parent.get().getChildren().add(this);
            this.parent.get().getFieldMap().put(fieldName, this);
        }

    }

    protected Object allocateMemberClassField(final Object fieldParent, final ClassFieldDescription localParent)
            throws IllegalAccessException {
        try {
            // need to allocate new object
            Constructor<?> constr = getParent(this, 1).getType().getDeclaredConstructor(fieldParent.getClass());
            final Object newFieldObj = constr.newInstance(fieldParent);
            localParent.getField().set(fieldParent, newFieldObj);

            return newFieldObj;
        } catch (InstantiationException | InvocationTargetException | SecurityException | NoSuchMethodException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("error initialising inner class object");
            }
        }
        return null;
    }

    public List<String> getActualTypeArgumentNames() {
        if (genericTypeNameList == null) {
            genericTypeNameList = getActualTypeArguments().stream()
                    .map(t -> ClassDescriptions.translateClassName(t.getTypeName())).collect(Collectors.toList());
        }

        return genericTypeNameList;
    }

    public List<Class<?>> getActualTypeArguments() {
        if (genericTypeList == null) {
            genericTypeList = new ArrayList<>();
            if ((field == null) || (getGenericType() == null) || !(getGenericType() instanceof ParameterizedType)) {
                return genericTypeList;
            }
            Type[] typeArguments = ((ParameterizedType) getGenericType()).getActualTypeArguments();
            for (Type type : typeArguments) {
                final String tName = type.getTypeName(); // may contain wildcards
                final String tCleanName = tName.charAt(0) == '?' ? tName.substring(WILDCARD_EXTENDS_LENGTH) : tName;
                // TODO: if name does not contain a dot '.' then likely it's a generic class
                // description, e.g. ConcurrentHashMap<K.V>
                Class<?> clazz = ClassDescriptions.getClassByNameNonVerboseError(tCleanName);
                genericTypeList.add(clazz);
            }
        }

        return genericTypeList;
    }

    /**
     * @return the children (if any) from the super classes
     */
    public List<ClassFieldDescription> getChildren() {
        return children;
    }

    /**
     * @return the DataType (if known) for the detected Field,
     *         {@link de.gsi.dataset.serializer.DataType#OTHER} in all other cases
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @return the underlying Field type or {@code null} if it's a root node
     */
    public Field getField() {
        return field;
    }

    /**
     * @return the hashMap containing the child references by field name
     */
    public Map<String, ClassFieldDescription> getFieldMap() {
        return hashMap;
    }

    /**
     * @return the underlying field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return relative field name within class hierarchy (ie.
     *         field_level0.field_level1.variable_0)
     */
    public String getFieldNameRelative() {
        return fieldNameRelative;
    }

    public String getGenericFieldTypeString() {
        if (genericTypeNames == null) {
            if (getActualTypeArgumentNames().isEmpty()) {
                genericTypeNames = "";
            } else {
                genericTypeNames = getActualTypeArgumentNames().stream().collect(Collectors.joining(", ", "<", ">"));
            }
        }
        return genericTypeNames;
    }

    public Type getGenericType() {
        if (genericType == null) {
            genericType = field == null ? new Type() {
                @Override
                public String getTypeName() {
                    return "unknown type";
                }
            } : field.getGenericType();
        }
        return genericType;
    }

    public int getHierarchyDepth() {
        return hierarchyDepth;
    }

    public Object getMemberClassObject(final Object rootObject) {
        if (rootObject == null) {
            throw new IllegalArgumentException("rootObject is null");
        }
        if (this.isRoot()) {
            return rootObject;
        }
        final int depth = this.getHierarchyDepth() - 1;
        Object temp = rootObject;
        Object parent1 = rootObject;
        for (int i = 0; i < depth; i++) {
            ClassFieldDescription localParent = getParent(this, depth - i);

            try {
//                System.err.println("rootObject = " + rootObject.toString());
//                System.err.println("localParent = " + localParent.getFieldName());
//                System.err.println("parent1 = " + parent1);
//                System.err.println("localParent.getField() = " + localParent.getField());
                if (localParent.getField() == null) {
                    return rootObject;
                }
                temp = localParent.getField().get(parent1);
                if (temp == null) {
                    temp = allocateMemberClassField(parent1, localParent);
                }
                if ((temp = localParent.getField().get(parent1)) == null) {
                    throw new IllegalStateException(
                            "could not allocate inner class object field = " + field.toString());
                }

                parent1 = temp;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().setCause(e)
                            .log("could not retrieve inner loop object for field '" + field.toString());
                }
            }
        }
        return temp;
    }

    /**
     * @return the modifierID
     */
    public int getModifierID() {
        return modifierID;
    }

    /**
     * @return the full modifier string (cached)
     */
    public String getModifierString() {
        if (modifierStr == null) {
            // initialise only on a need to basis
            // for performance reasons
            modifierStr = Modifier.toString(modifierID);
        }
        return modifierStr;
    }

    /**
     * @return the parent
     */
    public Optional<ClassFieldDescription> getParent() {
        return parent;
    }

    public ClassFieldDescription getParent(final ClassFieldDescription field, final int hierarchyLevel) {
        if (field == null) {
            throw new IllegalArgumentException("field is null at hierarchyLevel = " + hierarchyLevel);
        }
        if (hierarchyLevel == 0 || field.getParent().isEmpty()) {
            return field;
        }
        final Optional<ClassFieldDescription> localParent = field.getParent();
        return getParent(localParent.isPresent() ? localParent.get() : null, hierarchyLevel - 1);
    }

    /**
     * @return field class type
     */
    public Class<?> getType() {
        return classType;
    }

    /**
     * @return field class type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return the isAbstract
     */
    public boolean isAbstract() {
        return modAbstract;
    }

    /**
     * @return the isClass
     */
    public boolean isClass() {
        return isclass;
    }

    /**
     * @return {@code true} if the class field includes the {@code final} modifier;
     *         {@code false} otherwise.
     */
    public boolean isFinal() {
        return modFinal;
    }

    /**
     * @return {@code true} if the class field is an interface
     */
    public boolean isInterface() {
        return modInterface;
    }

    /**
     * @return the isNative
     */
    public boolean isNative() {
        return modNative;
    }

    /**
     * @return {@code true} if the class field is a primitive type (ie. boolean,
     *         byte, ..., int, float, double)
     */
    public boolean isPrimitive() {
        return isprimitive;
    }

    /**
     * @return {@code true} if the class field includes the {@code private}
     *         modifier; {@code false} otherwise.
     */
    public boolean isPrivate() {
        return modPrivate;
    }

    /**
     * @return the isProtected
     */
    public boolean isProtected() {
        return modProtected;
    }

    /**
     * @return {@code true} if the class field includes the {@code public} modifier;
     *         {@code false} otherwise.
     */
    public boolean isPublic() {
        return modPublic;
    }

    /**
     * @return the isRoot
     */
    public boolean isRoot() {
        return hierarchyDepth == 0;
    }

    /**
     * @return {@code true} if the class field should be serialised according to
     *         {@link java.io.Serializable} (ie. object's non-static and
     *         non-transient fields); {@code false} otherwise.
     */
    public boolean isSerializable() {
        return serializable;
    }

    /**
     * @return {@code true} if the class field includes the {@code static} modifier;
     *         {@code false} otherwise.
     */
    public boolean isStatic() {
        return modStatic;
    }

    /**
     * @return {@code true} if the class field includes the {@code strictfp}
     *         modifier; {@code false} otherwise.
     */
    public boolean isStrict() {
        return modStrict;
    }

    /**
     * @return {@code true} if the class field includes the {@code synchronized}
     *         modifier; {@code false} otherwise.
     */
    public boolean isSynchronized() {
        return modSynchronized;
    }

    /**
     * @return {@code true} if the class field includes the {@code transient}
     *         modifier; {@code false} otherwise.
     */
    public boolean isTransient() {
        return modTransient;
    }

    /**
     * @return {@code true} if the class field includes the {@code volatile}
     *         modifier; {@code false} otherwise.
     */
    public boolean isVolatile() {
        return modVolatile;
    }

    @Override
    public Iterator<ClassFieldDescription> iterator() {
        return new ClassFieldDescriptionIterator<>(this);
    }

    /**
     * @return the readCount
     */
    public AtomicInteger readCount() {
        return readCount;
    }

    public void reset() {
        resetReadCount();
        resetWriteCount();
    }

    public void resetReadCount() {
        readCount.set(0);
        getChildren().stream().forEach(ClassFieldDescription::resetReadCount);
    }

    public void resetWriteCount() {
        writeCount.set(0);
        getChildren().stream().forEach(ClassFieldDescription::resetWriteCount);
    }

    @Override
    public String toString() {
        if (toStringName == null) {
            toStringName = ClassFieldDescription.class.getSimpleName() + " for: " + this.getModifierString() + " "
                    + this.getTypeName() + " " + this.getFieldNameRelative() + " (hierarchyDepth = "
                    + this.getHierarchyDepth() + ")";
        }
        return toStringName;
    }

    /**
     * @return the writeCount
     */
    public AtomicInteger writeCount() {
        return writeCount;
    }

    /**
     * Returns the data type matching the given java class type, if any.
     *
     * @param classType the value to be searched
     * @return the matching data type
     */
    protected static DataType dataTypeFomClassType(final Class<?> classType) {
        for (final DataType type : DataType.values()) {
            if (type.getClassTypes().contains(classType)) {
                return type;
            }
        }

        // unknown data type returning generic 'OTHER'
        return DataType.OTHER;
    }

    protected static void exploreClass(final Class<? extends Object> classType, final ClassFieldDescription parent,
            final int recursionLevel, final boolean fullScan) {
        if (classType == null) {
            throw new IllegalArgumentException("classType must not be null");
        }
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }

        if (classType.equals(Object.class)) {
            // don't further parse Object.class, meaningless and cause infinite recursion
            return;
        }

        if (recursionLevel > maxRecursionLevel) {
            throw new IllegalStateException("recursion error while scanning object structure: recursionLevel = '"
                    + recursionLevel + "' > " + ClassFieldDescription.class.getSimpleName() + ".maxRecursionLevel ='"
                    + ClassFieldDescription.maxRecursionLevel + "'");
        }

        // call super types
        if (classType.getSuperclass() != null && !classType.getSuperclass().equals(Object.class)) {
            // dive into parent hierarchy
            exploreClass(classType.getSuperclass(), parent, recursionLevel + 1, fullScan);
        }

        // loop over member fields and inner classes
        for (final Field pfield : classType.getDeclaredFields()) {
            final Optional<ClassFieldDescription> localParent = parent.getParent();
            if (localParent.isPresent() && pfield.getType().equals(localParent.get().getType())) {
                // inner classes contain parent as part of declared fields
                continue;
            }
            final ClassFieldDescription field = new ClassFieldDescription(pfield, parent, recursionLevel + 1, fullScan);

            // N.B. omitting field.isSerializable() (static or transient modifier) is
            // essential
            // as they often indicate class dependencies that are prone to infinite
            // dependency loops
            // (e.g. for classes with static references to themselves or
            // maps-of-maps-of-maps-....)
            final boolean isClassAndNotObject = field.isClass() && !field.getType().equals(Object.class);
            if (field.isSerializable() && (isClassAndNotObject || field.isInterface())
                    && field.getDataType().equals(DataType.OTHER)) {
                // object is a (technically) Serializable, unknown (ie 'OTHER) compound object
                // or interface than can be further parsed
                exploreClass(field.getType(), field, recursionLevel + 1, fullScan);
            }
        }
    }
}
