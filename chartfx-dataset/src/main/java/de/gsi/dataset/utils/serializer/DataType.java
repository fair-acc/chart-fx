package de.gsi.dataset.utils.serializer;

import java.util.Map;

/**
 * Enum defining the data primitives that can be serialised. Arrays are always
 * handled as multi-dimensional arrays.
 * 
 * @author rstein
 */
public enum DataType {
    // @formatter:off
    // primitive types
    BOOL                     (0     ,"bool"                    , 1, Boolean.class, true),
    BYTE                     (1     ,"byte"                    , 1, Byte.class, true),
    SHORT                    (2     ,"short"                   , 2, Short.class, true),
    INT                      (3     ,"int"                     , 4, Integer.class, true),
    LONG                     (4     ,"long"                    , 8, Long.class, true),
    FLOAT                    (5     ,"float"                   , 4, Float.class, true),
    DOUBLE                   (6     ,"double"                  , 8, Double.class, true),
    CHAR                     (7     ,"char"                    , 2, Character.class, true),
    STRING                   (8     ,"string"                  , 1, String.class, true),

    // array of primitive types
    BOOL_ARRAY               (100   ,"bool_array"              , 1, Boolean[].class, false),
    BYTE_ARRAY               (101   ,"byte_array"              , 1, Byte[].class, false),
    SHORT_ARRAY              (102   ,"short_array"             , 2, Short[].class, false),
    INT_ARRAY                (103   ,"int_array"               , 4, Integer[].class, false),
    LONG_ARRAY               (104   ,"long_array"              , 8, Long[].class, false),
    FLOAT_ARRAY              (105   ,"float_array"             , 4, Float[].class, false),
    DOUBLE_ARRAY             (106   ,"double_array"            , 8, Double[].class, false),
    CHAR_ARRAY               (107   ,"char"                    , 2, Character[].class, true),
    STRING_ARRAY             (108   ,"string_array"            , 1, String[].class, false),

    // complex objects
    MAP                      (200   ,"map"                     , 1, Map.class, false),

    END_MARKER               (0xFE  ,"end_marker"              , 1, Byte.class, false);
    // @formatter:on

    private final byte byteValue;
    private final long primitiveSize;
    private final String stringValue;
    private final Class<?> classType;
    private final boolean scalar;

    DataType(final int byteValue, final String stringValue, final long primitiveSize, Class<?> classType, boolean isScalar) {
        this.byteValue = (byte) byteValue;
        this.stringValue = stringValue;
        this.primitiveSize = primitiveSize;
        this.classType = classType;
        this.scalar = isScalar;
    }

    /**
     * Returns the byte representation of the data type.
     * 
     * @return the byte representation
     */
    public byte getAsByte() {
        return this.byteValue;
    }

    public long getPrimitiveSize() {
        return primitiveSize;
    }
    

    /**
     * Returns the corresponding java class type matching the given data type
     * 
     * @return the matching java class type
     */
    public Class<?> getClassType() {
        return classType;
    }

    public boolean isScalar() {
        return scalar;
    }

    /**
     * Returns the string representation of the data type.
     * 
     * @return the string representation
     */
    public String getAsString() {
        return this.stringValue;
    }

    @Override
    public String toString() {
        return this.stringValue;
    }

    /**
     * Returns the data type matching the given string representation, if any.
     * 
     * @param str the string to be searched
     * 
     * @return the matching data type
     */
    public static DataType fromString(String str) {
        for (DataType type : DataType.values()) {
            if (type.stringValue.equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Entry type '" + str + "' is not supported");
    }

    /**
     * Returns the data type matching the given byte representation, if any.
     * 
     * @param value the value to be searched
     * @return the matching data type
     */
    public static DataType fromByte(byte value) {
        for (DataType type : DataType.values()) {
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
    public static DataType fromClassType(Class<?> classType) {
        for (DataType type : DataType.values()) {
            if (type.getClassType() == classType) {
                return type;
            }
        }

        throw new IllegalArgumentException("data type not implemented " + classType.getSimpleName());
    }

}
