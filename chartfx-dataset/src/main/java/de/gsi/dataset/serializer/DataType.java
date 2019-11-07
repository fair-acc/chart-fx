package de.gsi.dataset.serializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Enum defining the data primitives that can be serialised. Arrays are always handled as multi-dimensional arrays.
 *
 * @author rstein
 */
public enum DataType {
    // @formatter:off
    // start marker
    START_MARKER(0, "bool", "boolean", 1, Cat.SINGLE_VALUE),
    // primitive types
    BOOL(1, "bool", "boolean", 1, Cat.SINGLE_VALUE, boolean.class, Boolean.class),
    BYTE(2, "byte", "byte", 1, Cat.SINGLE_VALUE, byte.class, Byte.class),
    SHORT(3, "short", "short", 2, Cat.SINGLE_VALUE, short.class, Short.class),
    INT(4, "int", "int", 4, Cat.SINGLE_VALUE, int.class, Integer.class),
    LONG(5, "long", "long", 8, Cat.SINGLE_VALUE, long.class, Long.class),
    FLOAT(6, "float", "float", 4, Cat.SINGLE_VALUE, float.class, Float.class),
    DOUBLE(7, "double", "double", 8, Cat.SINGLE_VALUE, double.class, Double.class),
    CHAR(8, "char", "char", 2, Cat.SINGLE_VALUE, char.class, Character.class),
    STRING(9, "string", "java.lang.String", 1, Cat.SINGLE_VALUE, String.class),

    // array of primitive types
    BOOL_ARRAY(101, "bool_array", "[Z", 1, Cat.ARRAY, boolean[].class, Boolean[].class),
    BYTE_ARRAY(102, "byte_array", "[B", 1, Cat.ARRAY, byte[].class, Byte[].class),
    SHORT_ARRAY(103, "short_array", "[S", 2, Cat.ARRAY, short[].class, Short[].class),
    INT_ARRAY(104, "int_array", "[I", 4, Cat.ARRAY, int[].class, Integer[].class),
    LONG_ARRAY(105, "long_array", "[J", 8, Cat.ARRAY, long[].class, Long[].class),
    FLOAT_ARRAY(106, "float_array", "[F", 4, Cat.ARRAY, float[].class, Float[].class),
    DOUBLE_ARRAY(107, "double_array", "[D", 8, Cat.ARRAY, double[].class, Double[].class),
    CHAR_ARRAY(108, "char_array", "[C", 2, Cat.ARRAY, char[].class, Character[].class),
    STRING_ARRAY(109, "string_array", "[java.lang.String", 1, Cat.ARRAY, String[].class),

    // complex objects
    COLLECTION(200, "collection", "", 1, Cat.ARRAY, Collection.class),
    ENUM(201, "enum", "java.lang.Enum", 4, Cat.ARRAY, Enum.class), LIST(202, "list", "", 1, Cat.ARRAY, List.class),
    MAP(203, "map", "", 1, Cat.ARRAY, Map.class), QUEUE(204, "queue", "", 1, Cat.ARRAY, Queue.class),
    SET(205, "set", "", 1, Cat.ARRAY, Set.class),

    // default for future extensions (e.g. moving to short
    OTHER(0xFD, "other", "", 1, Cat.COMPLEX_OBJECT, Object.class),
    // end marker
    END_MARKER(0xFE, "end_marker", "", 1, Cat.SINGLE_VALUE);
    // @formatter:on

    private final byte byteValue;
    private final long primitiveSize;
    private final String stringValue;
    private final String javaName;
    private final List<Class<?>> classTypes;
    private final boolean scalar;
    private final boolean array;
    private final boolean object;

    DataType(final int byteValue, final String stringValue, final String javaName, final long primitiveSize,
            final Cat type, final Class<?>... classType) {
        this.byteValue = (byte) byteValue;
        this.stringValue = stringValue;
        this.javaName = javaName;
        this.primitiveSize = primitiveSize;
        classTypes = Arrays.asList(classType);
        scalar = type.equals(Cat.SINGLE_VALUE);
        array = type.equals(Cat.ARRAY);
        object = type.equals(Cat.COMPLEX_OBJECT);
    }

    /**
     * Returns the byte representation of the data type.
     *
     * @return the byte representation
     */
    public byte getAsByte() {
        return byteValue;
    }

    /**
     * Returns the string representation of the data type.
     *
     * @return the string representation
     */
    public String getAsString() {
        return stringValue;
    }

    /**
     * Returns the corresponding java class type matching the given data type
     *
     * @return the matching java class type
     */
    public List<Class<?>> getClassTypes() {
        return classTypes;
    }

    /**
     * Returns the string representation of the java class type.
     *
     * @return the string representation of the class
     */
    public String getJavaName() {
        return javaName;
    }

    public long getPrimitiveSize() {
        return primitiveSize;
    }

    public boolean isArray() {
        return array;
    }

    public boolean isObject() {
        return object;
    }

    public boolean isScalar() {
        return scalar;
    }

    /**
     * Returns the data type matching the given byte representation, if any.
     *
     * @param value the value to be searched
     * @return the matching data type
     */
    public static DataType fromByte(final byte value) {
        for (final DataType type : DataType.values()) {
            if (type.byteValue == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Entry type '" + value + "' is not supported");
    }

    /**
     * Returns the data type matching the given java class type, if any.
     *
     * @param classType the value to be searched
     * @return the matching data type
     */
    public static DataType fromClassType(final Class<?> classType) {
        for (final DataType type : DataType.values()) {
            if (type.getClassTypes().contains(classType)) {
                return type;
            }
        }

        throw new IllegalArgumentException("data type not implemented " + classType.getSimpleName());
    }

    /**
     * Returns the data type matching the given java string representation, if any.
     *
     * @param str the string to be searched
     * @return the matching data type
     */
    public static DataType fromJavaTypeString(final String str) {
        for (final DataType type : DataType.values()) {
            if (type.stringValue.equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Entry type '" + str + "' is not supported");
    }

    /**
     * Returns the data type matching the given string representation, if any.
     *
     * @param str the string to be searched
     * @return the matching data type
     */
    public static DataType fromString(final String str) {
        for (final DataType type : DataType.values()) {
            if (type.stringValue.equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Entry type '" + str + "' is not supported");
    }
}
