package de.gsi.serializer.spi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.serializer.DataType;
import de.gsi.serializer.FieldDescription;
import de.gsi.serializer.FieldSerialiser;
import de.gsi.serializer.annotations.Description;
import de.gsi.serializer.annotations.Direction;
import de.gsi.serializer.annotations.Groups;
import de.gsi.serializer.annotations.MetaInfo;
import de.gsi.serializer.annotations.Unit;
import de.gsi.serializer.utils.ClassUtils;

import sun.misc.Unsafe;

/**
 * @author rstein
 */
public class ClassFieldDescription implements FieldDescription {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFieldDescription.class);
    private final int hierarchyDepth;
    private final FieldAccess fieldAccess;
    private final String fieldName;
    private final int fieldNameHashCode;
    private final String fieldNameRelative;
    private final String fieldUnit;
    private final String fieldDescription;
    private final String fieldDirection;
    private final List<String> fieldGroups;
    private final boolean annotationPresent;
    private final ClassFieldDescription parent;
    private final List<FieldDescription> children = new ArrayList<>();
    private final Class<?> classType;
    private final DataType dataType;
    private final String typeName;
    private final int modifierID;
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
    private String toStringName; // computed on demand and cached
    private Type genericType; // computed on demand and cached
    private List<Type> genericTypeList; // computed on demand and cached
    private List<String> genericTypeNameList; // computed on demand and cached
    private String genericTypeNames; // computed on demand and cached
    private String genericTypeNamesSimple; // computed on demand and cached
    private String modifierStr; // computed on demand and cached
    // serialiser info
    private FieldSerialiser<?> fieldSerialiser;

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

    protected ClassFieldDescription(final Class<?> referenceClass, final Field field, final ClassFieldDescription parent, final int recursionLevel) {
        super();
        hierarchyDepth = recursionLevel;
        this.parent = parent;

        if (referenceClass == null) {
            if (field == null) {
                throw new IllegalArgumentException("field must not be null");
            }
            fieldAccess = new FieldAccess(field);
            classType = field.getType();
            fieldNameHashCode = field.getName().hashCode();
            fieldName = field.getName().intern();
            fieldNameRelative = this.parent == null ? fieldName : (this.parent.getFieldNameRelative() + "." + fieldName).intern();

            modifierID = field.getModifiers();
            dataType = DataType.fromClassType(classType);
        } else {
            fieldAccess = null; // it's a root, no field definition available
            classType = referenceClass;
            fieldNameHashCode = classType.getName().hashCode();
            fieldName = classType.getName().intern();
            fieldNameRelative = fieldName;

            modifierID = classType.getModifiers();
            dataType = DataType.START_MARKER;
        }

        // read annotation values
        AnnotatedElement annotatedElement = field == null ? referenceClass : field;
        fieldUnit = getFieldUnit(annotatedElement);
        fieldDescription = getFieldDescription(annotatedElement);
        fieldDirection = getFieldDirection(annotatedElement);
        fieldGroups = getFieldGroups(annotatedElement);

        annotationPresent = fieldUnit != null || fieldDescription != null || fieldDirection != null || !fieldGroups.isEmpty();

        typeName = ClassUtils.translateClassName(classType.getTypeName()).intern();

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
        if (this.parent != null && (serializable || fullScan)) {
            this.parent.getChildren().add(this);
        }
    }

    public Object allocateMemberClassField(final Object fieldParent) {
        try {
            // need to allocate new class object
            Class<?> fieldParentClass = ClassUtils.getRawType(getParent(this, 1).getType());
            final Object newFieldObj;
            if (fieldParentClass.getDeclaringClass() == null) {
                final Constructor<?> constr = fieldParentClass.getDeclaredConstructor();
                newFieldObj = constr.newInstance();
            } else {
                final Constructor<?> constr = fieldParentClass.getDeclaredConstructor(fieldParent.getClass());
                newFieldObj = constr.newInstance(fieldParent);
            }
            this.getField().set(fieldParent, newFieldObj);

            return newFieldObj;
        } catch (InstantiationException | InvocationTargetException | SecurityException | NoSuchMethodException | IllegalAccessException e) {
            LOGGER.atError().setCause(e).log("error initialising inner class object");
        }
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldDescription)) {
            return false;
        }
        final FieldDescription other = (FieldDescription) obj;
        if (this.getFieldNameHashCode() != other.getFieldNameHashCode()) {
            return false;
        }

        if (this.getDataType() != other.getDataType()) {
            return false;
        }

        return this.getFieldName().equals(other.getFieldName());
    }

    @Override
    public FieldDescription findChildField(final String fieldName) {
        return findChildField(fieldName.hashCode(), fieldName);
    }

    @Override
    public FieldDescription findChildField(final int fieldNameHashCode, final String fieldName) {
        for (int i = 0; i < children.size(); i++) {
            final FieldDescription child = children.get(i);
            final String name = child.getFieldName();
            if (name == fieldName) { //NOSONAR //NOPMD early return if the same String object reference
                return child;
            }
            if (child.getFieldNameHashCode() == fieldNameHashCode && name.equals(fieldName)) {
                return child;
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
            genericTypeNameList = getActualTypeArguments().stream().map(Type::getTypeName).collect(Collectors.toList());
        }

        return genericTypeNameList;
    }

    /**
     * @return generic type argument objects of the class (e.g. for List&lt;String&gt; this would return 'String.class')
     */
    public List<Type> getActualTypeArguments() {
        if (genericTypeList == null) {
            genericTypeList = new ArrayList<>();
            if ((fieldAccess == null) || (getGenericType() == null) || !(getGenericType() instanceof ParameterizedType)) {
                return genericTypeList;
            }
            genericTypeList.addAll(Arrays.asList(((ParameterizedType) getGenericType()).getActualTypeArguments()));
        }

        return genericTypeList;
    }

    /**
     * @return the children (if any) from the super classes
     */
    @Override
    public List<FieldDescription> getChildren() {
        return children;
    }

    @Override
    public int getDataSize() {
        return 0;
    }

    @Override
    public int getDataStartOffset() {
        return 0;
    }

    @Override
    public int getDataStartPosition() {
        return 0;
    }

    /**
     * @return the DataType (if known) for the detected Field, {@link DataType#OTHER} in all other cases
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @return the underlying Field type or {@code null} if it's a root node
     */
    public FieldAccess getField() {
        return fieldAccess;
    }

    @Override
    public String getFieldDescription() {
        return fieldDescription;
    }

    @Override
    public String getFieldDirection() {
        return fieldDirection;
    }

    @Override
    public List<String> getFieldGroups() {
        return fieldGroups;
    }

    /**
     * @return the underlying field name
     */
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int getFieldNameHashCode() {
        return fieldNameHashCode;
    }

    /**
     * @return relative field name within class hierarchy (ie. field_level0.field_level1.variable_0)
     */
    public String getFieldNameRelative() {
        return fieldNameRelative;
    }

    public FieldSerialiser getFieldSerialiser() {
        return fieldSerialiser;
    }

    @Override
    public int getFieldStart() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getFieldUnit() {
        return fieldUnit;
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
                genericTypeNames = getActualTypeArgumentNames().stream().collect(Collectors.joining(", ", "<", ">")).intern();
            }
        }
        return genericTypeNames;
    }

    /**
     * @return field type strings (e.g. for the class Map&lt;Integer,String&gt; this returns
     *         '&lt;java.lang.Integer,java.lang.String&gt;'
     */
    public String getGenericFieldTypeStringSimple() {
        if (genericTypeNamesSimple == null) {
            if (getActualTypeArgumentNames().isEmpty()) {
                genericTypeNamesSimple = "";
            } else {
                genericTypeNamesSimple = getActualTypeArguments().stream() //
                                                 .map(t -> ClassUtils.translateClassName(t.getTypeName()))
                                                 .collect(Collectors.joining(", ", "<", ">"))
                                                 .intern();
            }
        }
        return genericTypeNamesSimple;
    }

    public Type getGenericType() {
        if (genericType == null) {
            genericType = fieldAccess == null ? new Type() {
                @Override
                public String getTypeName() {
                    return "unknown type";
                }
            } : fieldAccess.field.getGenericType();
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
            modifierStr = Modifier.toString(modifierID).intern();
        }
        return modifierStr;
    }

    /**
     * @return the parent
     */
    @Override
    public FieldDescription getParent() {
        return parent;
    }

    /**
     * @param field class Field description for which
     * @param hierarchyLevel the recursion level of the parent (e.g. '1' yields the immediate parent, '2' the parent of
     *        the parent etc.)
     * @return the parent field reference description for the provided field
     */
    public FieldDescription getParent(final FieldDescription field, final int hierarchyLevel) {
        if (field == null) {
            throw new IllegalArgumentException("field is null at hierarchyLevel = " + hierarchyLevel);
        }
        if ((hierarchyLevel == 0) || field.getParent() == null) {
            return field;
        }
        return getParent(field.getParent(), hierarchyLevel - 1);
    }

    /**
     * @return field class type
     */
    @Override
    public Type getType() {
        return classType;
    }

    /**
     * @return field class type name
     */
    public String getTypeName() {
        return typeName;
    }

    @Override
    public int hashCode() {
        return fieldNameHashCode;
    }

    /**
     * @return the isAbstract
     */
    public boolean isAbstract() {
        return modAbstract;
    }

    @Override
    public boolean isAnnotationPresent() {
        return annotationPresent;
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
    public void printFieldStructure() {
        printClassStructure(this, true, 0);
    }

    public void setFieldSerialiser(final FieldSerialiser<?> fieldSerialiser) {
        this.fieldSerialiser = fieldSerialiser;
    }

    @Override
    public String toString() {
        if (toStringName == null) {
            toStringName = (ClassFieldDescription.class.getSimpleName() + " for: " + getModifierString() + " "
                            + getTypeName() + getGenericFieldTypeStringSimple() + " " + getFieldName() + " (hierarchyDepth = " + getHierarchyDepth() + ")")
                                   .intern();
        }
        return toStringName;
    }

    protected static void exploreClass(final Class<?> classType, final ClassFieldDescription parent, final int recursionLevel, final boolean fullScan) {
        if (recursionLevel > ClassUtils.getMaxRecursionDepth()) {
            throw new IllegalStateException("recursion error while scanning object structure: recursionLevel = '"
                                            + recursionLevel + "' > " + ClassFieldDescription.class.getSimpleName() + ".maxRecursionLevel ='"
                                            + ClassUtils.getMaxRecursionDepth() + "'");
        }

        // call super types
        if ((classType.getSuperclass() != null) && !classType.getSuperclass().equals(Object.class) && !classType.getSuperclass().equals(Enum.class)) {
            // dive into parent hierarchy w/o parsing Object.class, -> meaningless and causes infinite recursion
            exploreClass(classType.getSuperclass(), parent, recursionLevel + 1, fullScan);
        }

        // loop over member fields and inner classes
        for (final Field pfield : classType.getDeclaredFields()) {
            final FieldDescription localParent = parent.getParent();
            if ((localParent != null && pfield.getType().equals(localParent.getType()) && recursionLevel >= ClassUtils.getMaxRecursionDepth()) || pfield.getName().startsWith("this$")) {
                // inner classes contain parent as part of declared fields
                continue;
            }
            final ClassFieldDescription field = new ClassFieldDescription(pfield, parent, recursionLevel + 1, fullScan); // NOPMD
            // N.B. unavoidable in-loop object generation

            // N.B. omitting field.isSerializable() (static or transient modifier) is essential
            // as they often indicate class dependencies that are prone to infinite dependency loops
            // (e.g. for classes with static references to themselves or maps-of-maps-of-maps-....)
            final boolean isClassAndNotObjectOrEnmum = field.isClass() && (!field.getType().equals(Object.class) || !field.getType().equals(Enum.class));
            if (field.isSerializable() && (isClassAndNotObjectOrEnmum || field.isInterface()) && field.getDataType().equals(DataType.OTHER)) {
                // object is a (technically) Serializable, unknown (ie 'OTHER) compound object
                // or interface than can be further parsed
                exploreClass(ClassUtils.getRawType(field.getType()), field, recursionLevel + 1, fullScan);
            }
        }
    }

    protected static void printClassStructure(final ClassFieldDescription field, final boolean fullView, final int recursionLevel) {
        final String enumOrClass = field.isEnum() ? "Enum " : "class ";
        final String typeCategorgy = (field.isInterface() ? "interface " : (field.isPrimitive() ? "" : enumOrClass)); //NOSONAR //NOPMD
        final String typeName = field.getTypeName() + field.getGenericFieldTypeString();
        final String mspace = spaces(recursionLevel * ClassUtils.getIndentationNumberOfSpace());
        final boolean isSerialisable = field.isSerializable();

        if (isSerialisable || fullView) {
            LOGGER.atInfo().addArgument(mspace).addArgument(isSerialisable ? "  " : "//") //
                    .addArgument(field.getModifierString())
                    .addArgument(typeCategorgy)
                    .addArgument(typeName)
                    .addArgument(field.getFieldName())
                    .log("{} {} {} {}{} {}");
            if (field.isAnnotationPresent()) {
                LOGGER.atInfo().addArgument(mspace).addArgument(isSerialisable ? "  " : "//") //
                        .addArgument(field.getFieldUnit())
                        .addArgument(field.getFieldDescription())
                        .addArgument(field.getFieldDirection())
                        .addArgument(field.getFieldGroups())
                        .log("{} {}         <meta-info: unit:'{}' description:'{}' direction:'{}' groups:'{}'>");
            }

            field.getChildren().forEach(f -> printClassStructure((ClassFieldDescription) f, fullView, recursionLevel + 1));
        }
    }

    private static String getFieldDescription(final AnnotatedElement annotatedElement) {
        final MetaInfo[] annotationMeta = annotatedElement.getAnnotationsByType(MetaInfo.class);
        if (annotationMeta != null && annotationMeta.length > 0) {
            return annotationMeta[0].description().intern();
        }
        final Description[] annotationDescription = annotatedElement.getAnnotationsByType(Description.class);
        if (annotationDescription != null && annotationDescription.length > 0) {
            return annotationDescription[0].value().intern();
        }
        return null;
    }

    private static String getFieldDirection(final AnnotatedElement annotatedElement) {
        final MetaInfo[] annotationMeta = annotatedElement.getAnnotationsByType(MetaInfo.class);
        if (annotationMeta != null && annotationMeta.length > 0) {
            return annotationMeta[0].direction().intern();
        }
        final Direction[] annotationDirection = annotatedElement.getAnnotationsByType(Direction.class);
        if (annotationDirection != null && annotationDirection.length > 0) {
            return annotationDirection[0].value().intern();
        }
        return null;
    }

    private static List<String> getFieldGroups(final AnnotatedElement annotatedElement) {
        final MetaInfo[] annotationMeta = annotatedElement.getAnnotationsByType(MetaInfo.class);
        if (annotationMeta != null && annotationMeta.length > 0) {
            return Arrays.asList(annotationMeta[0].groups());
        }
        final Groups[] annotationGroups = annotatedElement.getAnnotationsByType(Groups.class);
        if (annotationGroups != null && annotationGroups.length > 0) {
            final List<String> ret = new ArrayList<>(annotationGroups[0].value().length);
            for (int i = 0; i < annotationGroups[0].value().length; i++) {
                ret.add(annotationGroups[0].value()[i].intern());
            }
            return ret;
        }
        return Collections.emptyList();
    }

    private static String getFieldUnit(final AnnotatedElement annotatedElement) {
        final MetaInfo[] annotationMeta = annotatedElement.getAnnotationsByType(MetaInfo.class);
        if (annotationMeta != null && annotationMeta.length > 0) {
            return annotationMeta[0].unit().intern();
        }
        final Unit[] annotationUnit = annotatedElement.getAnnotationsByType(Unit.class);
        if (annotationUnit != null && annotationUnit.length > 0) {
            return annotationUnit[0].value().intern();
        }
        return null;
    }

    private static String spaces(final int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }

    public static class FieldAccess {
        private static final Unsafe unsafe; // NOPMD

        static {
            // get an instance of the otherwise private 'Unsafe' class
            try {
                final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true); //NOSONAR
                unsafe = (Unsafe) field.get(null);
            } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
                throw new SecurityException(e); // NOPMD
            }
        }

        private final Field field;
        private final long fieldByteOffset;

        private FieldAccess(final Field field) {
            this.field = field;
            field.setAccessible(true); //NOSONAR

            long offset = -1;
            try {
                offset = unsafe.objectFieldOffset(field);
            } catch (IllegalArgumentException e) {
                // fails for private static final fields
            }
            this.fieldByteOffset = offset;
        }

        public Object get(final Object classReference) {
            return unsafe.getObject(classReference, fieldByteOffset);
        }

        public boolean getBoolean(final Object classReference) {
            return unsafe.getBoolean(classReference, fieldByteOffset);
        }

        public byte getByte(final Object classReference) {
            return unsafe.getByte(classReference, fieldByteOffset);
        }

        public char getChar(final Object classReference) {
            return unsafe.getChar(classReference, fieldByteOffset);
        }

        public double getDouble(final Object classReference) {
            return unsafe.getDouble(classReference, fieldByteOffset);
        }

        public Field getField() {
            return field;
        }
        public float getFloat(final Object classReference) {
            return unsafe.getFloat(classReference, fieldByteOffset);
        }
        public int getInt(final Object classReference) {
            return unsafe.getInt(classReference, fieldByteOffset);
        }
        public long getLong(final Object classReference) {
            return unsafe.getLong(classReference, fieldByteOffset);
        }
        public short getShort(final Object classReference) { // NOPMD
            return unsafe.getShort(classReference, fieldByteOffset);
        }
        public void set(final Object classReference, final Object obj) {
            unsafe.putObject(classReference, fieldByteOffset, obj);
        }
        public void setBoolean(final Object classReference, final boolean value) {
            unsafe.putBoolean(classReference, fieldByteOffset, value);
        }
        public void setByte(final Object classReference, final byte value) {
            unsafe.putByte(classReference, fieldByteOffset, value);
        }
        public void setChar(final Object classReference, final char value) {
            unsafe.putChar(classReference, fieldByteOffset, value);
        }

        public void setDouble(final Object classReference, final double value) {
            unsafe.putDouble(classReference, fieldByteOffset, value);
        }

        public void setFloat(final Object classReference, final float value) {
            unsafe.putFloat(classReference, fieldByteOffset, value);
        }

        public void setInt(final Object classReference, final int value) {
            unsafe.putInt(classReference, fieldByteOffset, value);
        }

        public void setLong(final Object classReference, final long value) {
            unsafe.putLong(classReference, fieldByteOffset, value);
        }

        public void setShort(final Object classReference, final short value) { // NOPMD
            unsafe.putShort(classReference, fieldByteOffset, value);
        }
    }
}
