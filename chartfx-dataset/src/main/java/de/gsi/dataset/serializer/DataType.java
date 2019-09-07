package de.gsi.dataset.serializer;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Enum defining the data primitives that can be serialised. Arrays are always
 * handled as multi-dimensional arrays.
 *
 * @author rstein
 */
public enum DataType {
    // @formatter:off
    // start marker
    START_MARKER             (0,   "bool",          "boolean",             1, Boolean.class, Cat.PRIMITIVE),
    // primitive types
    BOOL                     (1,   "bool",          "boolean",             1, Boolean.class, Cat.PRIMITIVE),
    BYTE                     (2,   "byte" ,         "byte",                1, Byte.class, Cat.PRIMITIVE),
    SHORT                    (3,   "short",         "short",               2, Short.class, Cat.PRIMITIVE),
    INT                      (4,   "int",           "int",                 4, Integer.class, Cat.PRIMITIVE),
    LONG                     (5,   "long",          "long",                8, Long.class, Cat.PRIMITIVE),
    FLOAT                    (6,   "float" ,        "float",               4, Float.class, Cat.PRIMITIVE),
    DOUBLE                   (7,   "double",        "double",              8, Double.class, Cat.PRIMITIVE),
    CHAR                     (8,   "char",          "char",                2, Character.class, Cat.PRIMITIVE),
    STRING                   (9,   "string",        "string",              1, String.class, Cat.PRIMITIVE),

    // array of primitive types
    BOOL_ARRAY               (101, "bool_array",    "[Z",                  1, Boolean[].class, Cat.ARRAY),
    BYTE_ARRAY               (102, "byte_array",    "[B",                  1, Byte[].class, Cat.ARRAY),
    SHORT_ARRAY              (103, "short_array",   "[S",                  2, Short[].class, Cat.ARRAY),
    INT_ARRAY                (104, "int_array",     "[I",                  4, Integer[].class, Cat.ARRAY),
    LONG_ARRAY               (105, "long_array",    "[J",                  8, Long[].class, Cat.ARRAY),
    FLOAT_ARRAY              (106, "float_array",   "[F",                  4, Float[].class, Cat.ARRAY),
    DOUBLE_ARRAY             (107, "double_array",  "[D",                  8, Double[].class, Cat.ARRAY),
    CHAR_ARRAY               (108, "char_array",    "[C",                  2, Character[].class, Cat.ARRAY),
    STRING_ARRAY             (109, "string_array",  "[java.lang.String",   1, String[].class, Cat.ARRAY),

    // complex objects
    COLLECTION               (200, "collection",     "",                    1, Collection.class, Cat.OBJECT),
    ENUMERATION              (201, "enumeration",    "",                    1, Enumeration.class, Cat.OBJECT),
    LIST                     (202, "list",           "",                    1, List.class, Cat.OBJECT),
    MAP                      (203, "map",            "",                    1, Map.class, Cat.OBJECT),
    QUEUE                    (204, "queue",          "",                    1, Queue.class, Cat.OBJECT),


    // default for future extensions (e.g. moving to short
    OTHER                    (0xFD, "other",         "",                    1, Object.class, Cat.PRIMITIVE),
    // end marker
    END_MARKER               (0xFE, "end_marker",    "",                    1, Byte.class, Cat.PRIMITIVE);
    // @formatter:on

    private final byte byteValue;
    private final long primitiveSize;
    private final String stringValue;
    private final String javaName;
    private final Class<?> classType;
    private final boolean scalar;
    private final boolean array;
    private final boolean object;

    DataType(final int byteValue, final String stringValue, final String javaName, final long primitiveSize,
            final Class<?> classType, final Cat type) {
        this.byteValue = (byte) byteValue;
        this.stringValue = stringValue;
        this.javaName = javaName;
        this.primitiveSize = primitiveSize;
        this.classType = classType;
        scalar = type.equals(Cat.PRIMITIVE);
        array = type.equals(Cat.ARRAY);
        object = type.equals(Cat.OBJECT);
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
            if (type.getClassType() == classType) {
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
    public Class<?> getClassType() {
        return classType;
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

    public boolean isScalar() {
        return scalar;
    }

    public boolean isArray() {
        return array;
    }

    public boolean isObject() {
        return object;
    }
}
