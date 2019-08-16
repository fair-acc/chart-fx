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
    /**
     * maximum recursion depth that is being explored default is 10: anything beyond that is typically an indication if
     * infinite recursion
     */
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
    private final boolean isEnum;

    private final boolean serializable;
    // access counter (N.B. to check if data has been modified)
    private final AtomicInteger readCount = new AtomicInteger();
    private final AtomicInteger writeCount = new AtomicInteger();

    /**
     * This should be called only once with the root class as an argument
     *
     * @param referenceClass the root node containing further Field children
     * @param fullScan {@code true} if the class field should be serialised according to {@link java.io.Serializable}
     *        (ie. object's non-static and non-transient fields); {@code false} otherwise.
     */
    public ClassFieldDescription(final Class<?> referenceClass, final boolean fullScan) {
        this(referenceClass, null, null, 0);
        if (referenceClass == null) {
            throw new IllegalArgumentException("object must not be null");
        }

        genericType = classType.getGenericSuperclass();

        // parse object
        exploreClass(classType, this, 0, fullScan);
    }

    protected ClassFieldDescription(final Class<?> referenceClass, final Field field,
            final ClassFieldDescription parent, final int recursionLevel) {
        super();
        hierarchyDepth = recursionLevel;
        this.parent = parent == null ? Optional.empty() : Optional.of(parent);

        if (referenceClass == null) {
            if (field == null) {
                throw new IllegalArgumentException("field must not be null");
            }
            this.field = field;
            classType = field.getType();
            fieldName = field.getName();
            if (this.parent.isPresent()) {
                final String relativeName = this.parent.get().isRoot() ? ""
                        : (this.parent.get().getFieldNameRelative() + ".");
                fieldNameRelative = relativeName + fieldName;
            } else {
                fieldNameRelative = fieldName;
            }

            modifierID = field.getModifiers();
        } else {
            this.field = null; // it's a root, no field definition available
            classType = referenceClass;
            fieldName = classType.getName();
            fieldNameRelative = "";

            modifierID = classType.getModifiers();
        }

        dataType = dataTypeFomClassType(classType);
        typeName = ClassDescriptions.translateClassName(classType.getTypeName());

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
        isEnum = Enum.class.isAssignableFrom(classType);
        serializable = !modTransient && !modStatic;

    }

    /**
     * This should be called for individual class field members
     *
     * @param field Field reference for the given class member
     * @param parent pointer to the root/parent reference class field description
     * @param recursionLevel hierarchy level (i.e. '0' being the root class, '1' the sub-class etc.
     * @param fullScan {@code true} if the class field should be serialised according to {@link java.io.Serializable}
     *        (ie. object's non-static and non-transient fields); {@code false} otherwise.
     */
    public ClassFieldDescription(final Field field, final ClassFieldDescription parent, final int recursionLevel,
            final boolean fullScan) {
        this(null, field, parent, recursionLevel);
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }

        if (serializable) {
            // enable access by default (saves performance later on)
            field.setAccessible(true);
        }

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
            final Constructor<?> constr = getParent(this, 1).getType().getDeclaredConstructor(fieldParent.getClass());
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

    /**
     * @return generic type argument name of the class (e.g. for List&lt;String&gt; this would return
     *         'java.lang.String')
     */
    public List<String> getActualTypeArgumentNames() {
        if (genericTypeNameList == null) {
            genericTypeNameList = getActualTypeArguments().stream()
                    .map(t -> ClassDescriptions.translateClassName(t.getTypeName())).collect(Collectors.toList());
        }

        return genericTypeNameList;
    }

    /**
     * @return generic type argument objects of the class (e.g. for List&lt;String&gt; this would return 'String.class')
     */
    public List<Class<?>> getActualTypeArguments() {
        if (genericTypeList == null) {
            genericTypeList = new ArrayList<>();
            if ((field == null) || (getGenericType() == null) || !(getGenericType() instanceof ParameterizedType)) {
                return genericTypeList;
            }
            final Type[] typeArguments = ((ParameterizedType) getGenericType()).getActualTypeArguments();
            for (final Type type : typeArguments) {
                final String tName = type.getTypeName(); // may contain wildcards
                final String tCleanName = tName.charAt(0) == '?' ? tName.substring(WILDCARD_EXTENDS_LENGTH) : tName;
                // if name does not contain a dot '.' then likely it's a generic class
                // description, e.g. ConcurrentHashMap<K.V>
                final Class<?> clazz = ClassDescriptions.getClassByNameNonVerboseError(tCleanName);
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
     * @return the DataType (if known) for the detected Field, {@link de.gsi.dataset.serializer.DataType#OTHER} in all
     *         other cases
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
     * @return relative field name within class hierarchy (ie. field_level0.field_level1.variable_0)
     */
    public String getFieldNameRelative() {
        return fieldNameRelative;
    }

    /**
     * @return field type strings (e.g. for the class Map&lt;Integer,String&gt; this returns
     *         '&lt;java.lang.Integer,java.lang.String&gt;'
     */
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

    /**
     * @return hierarchy level depth w.r.t. root object (ie. '0' being a variable in the root object)
     */
    public int getHierarchyDepth() {
        return hierarchyDepth;
    }

    /**
     * @param rootObject reference to the root object
     * @return the specific object link corresponding to this field
     */
    public Object getMemberClassObject(final Object rootObject) {
        if (rootObject == null) {
            throw new IllegalArgumentException("rootObject is null");
        }
        if (isRoot()) {
            return rootObject;
        }
        final int depth = getHierarchyDepth() - 1;
        Object temp = rootObject;
        Object parent1 = rootObject;
        for (int i = 0; i < depth; i++) {
            final ClassFieldDescription localParent = getParent(this, depth - i);

            try {
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

    /**
     * @param field class Field description for which
     * @param hierarchyLevel the recursion level of the parent (e.g. '1' yields the immediate parent, '2' the parent of
     *        the parent etc.)
     * @return the parent field reference description for the provided field
     */
    public ClassFieldDescription getParent(final ClassFieldDescription field, final int hierarchyLevel) {
        if (field == null) {
            throw new IllegalArgumentException("field is null at hierarchyLevel = " + hierarchyLevel);
        }
        if ((hierarchyLevel == 0) || !field.getParent().isPresent()) {
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
     * @return whether class is an Enum type
     */
    public boolean isEnum() {
        return isEnum;
    }

    /**
     * @return {@code true} if the class field includes the {@code final} modifier; {@code false} otherwise.
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
     * @return {@code true} if the class field is a primitive type (ie. boolean, byte, ..., int, float, double)
     */
    public boolean isPrimitive() {
        return isprimitive;
    }

    /**
     * @return {@code true} if the class field includes the {@code private} modifier; {@code false} otherwise.
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
     * @return {@code true} if the class field includes the {@code public} modifier; {@code false} otherwise.
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
     * @return {@code true} if the class field should be serialised according to {@link java.io.Serializable} (ie.
     *         object's non-static and non-transient fields); {@code false} otherwise.
     */
    public boolean isSerializable() {
        return serializable;
    }

    /**
     * @return {@code true} if the class field includes the {@code static} modifier; {@code false} otherwise.
     */
    public boolean isStatic() {
        return modStatic;
    }

    /**
     * @return {@code true} if the class field includes the {@code strictfp} modifier; {@code false} otherwise.
     */
    public boolean isStrict() {
        return modStrict;
    }

    /**
     * @return {@code true} if the class field includes the {@code synchronized} modifier; {@code false} otherwise.
     */
    public boolean isSynchronized() {
        return modSynchronized;
    }

    /**
     * @return {@code true} if the class field includes the {@code transient} modifier; {@code false} otherwise.
     */
    public boolean isTransient() {
        return modTransient;
    }

    /**
     * @return {@code true} if the class field includes the {@code volatile} modifier; {@code false} otherwise.
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

    /**
     * resets read and write count
     */
    public void reset() {
        resetReadCount();
        resetWriteCount();
    }

    /**
     * resets the read counter
     */
    public void resetReadCount() {
        readCount.set(0);
        getChildren().stream().forEach(ClassFieldDescription::resetReadCount);
    }

    /**
     * resets the write counter
     */
    public void resetWriteCount() {
        writeCount.set(0);
        getChildren().stream().forEach(ClassFieldDescription::resetWriteCount);
    }

    @Override
    public String toString() {
        if (toStringName == null) {
            toStringName = ClassFieldDescription.class.getSimpleName() + " for: " + getModifierString() + " "
                    + getTypeName() + " " + getFieldNameRelative() + " (hierarchyDepth = " + getHierarchyDepth() + ")";
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

        if (recursionLevel > maxRecursionLevel) {
            throw new IllegalStateException("recursion error while scanning object structure: recursionLevel = '"
                    + recursionLevel + "' > " + ClassFieldDescription.class.getSimpleName() + ".maxRecursionLevel ='"
                    + ClassFieldDescription.maxRecursionLevel + "'");
        }

        // call super types
        if ((classType.getSuperclass() != null) && !classType.getSuperclass().equals(Object.class)
                && !classType.getSuperclass().equals(Enum.class)) {
            // dive into parent hierarchy w/o parsing Object.class,
            // -> meaningless and causes infinite recursion
            exploreClass(classType.getSuperclass(), parent, recursionLevel + 1, fullScan);
        }

        // loop over member fields and inner classes
        for (final Field pfield : classType.getDeclaredFields()) {
            final Optional<ClassFieldDescription> localParent = parent.getParent();
            if ((localParent.isPresent() && pfield.getType().equals(localParent.get().getType()))
                    || pfield.getName().startsWith("this$")) {
                // inner classes contain parent as part of declared fields
                continue;
            }
            final ClassFieldDescription field = new ClassFieldDescription(pfield, parent, recursionLevel + 1, fullScan); // NOPMD
            // N.B. unavoidable in-loop object generation

            // N.B. omitting field.isSerializable() (static or transient modifier) is
            // essential
            // as they often indicate class dependencies that are prone to infinite
            // dependency loops
            // (e.g. for classes with static references to themselves or
            // maps-of-maps-of-maps-....)
            final boolean isClassAndNotObjectOrEnmum = field.isClass()
                    && (!field.getType().equals(Object.class) || !field.getType().equals(Enum.class));
            if (field.isSerializable() && (isClassAndNotObjectOrEnmum || field.isInterface())
                    && field.getDataType().equals(DataType.OTHER)) {
                // object is a (technically) Serializable, unknown (ie 'OTHER) compound object
                // or interface than can be further parsed
                exploreClass(field.getType(), field, recursionLevel + 1, fullScan);
            }
        }
    }
}
