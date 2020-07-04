package de.gsi.dataset.serializer.helper;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;

@SuppressWarnings("PMD") // complexity is part of the very large use-case surface that is being tested
public final class SerialiserHelper {
    public static void serialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo) {
        serialiseCustom(ioSerialiser, pojo, true);
    }

    public static void serialiseCustom(final IoSerialiser ioSerialiser, final TestDataClass pojo, final boolean header) {
        if (header) {
            ioSerialiser.putHeaderInfo();
        }

        ioSerialiser.putFieldHeader("bool1", DataType.BOOL);
        ioSerialiser.put(pojo.bool1);
        ioSerialiser.putFieldHeader("bool1", DataType.BOOL);
        ioSerialiser.put(pojo.bool2);
        ioSerialiser.putFieldHeader("byte1", DataType.BYTE);
        ioSerialiser.put(pojo.byte1);
        ioSerialiser.putFieldHeader("byte2", DataType.BYTE);
        ioSerialiser.put(pojo.byte2);
        ioSerialiser.putFieldHeader("char1", DataType.CHAR);
        ioSerialiser.put(pojo.char1);
        ioSerialiser.putFieldHeader("char2", DataType.CHAR);
        ioSerialiser.put(pojo.char2);
        ioSerialiser.putFieldHeader("short1", DataType.SHORT);
        ioSerialiser.put(pojo.short1);
        ioSerialiser.putFieldHeader("short2", DataType.SHORT);
        ioSerialiser.put(pojo.short2);
        ioSerialiser.putFieldHeader("int1", DataType.INT);
        ioSerialiser.put(pojo.int1);
        ioSerialiser.putFieldHeader("int2", DataType.INT);
        ioSerialiser.put(pojo.int2);
        ioSerialiser.putFieldHeader("long1", DataType.LONG);
        ioSerialiser.put(pojo.long1);
        ioSerialiser.putFieldHeader("long2", DataType.LONG);
        ioSerialiser.put(pojo.long2);
        ioSerialiser.putFieldHeader("float1", DataType.FLOAT);
        ioSerialiser.put(pojo.float1);
        ioSerialiser.putFieldHeader("float2", DataType.FLOAT);
        ioSerialiser.put(pojo.float2);
        ioSerialiser.putFieldHeader("double1", DataType.DOUBLE);
        ioSerialiser.put(pojo.double1);
        ioSerialiser.putFieldHeader("double2", DataType.DOUBLE);
        ioSerialiser.put(pojo.double2);
        ioSerialiser.putFieldHeader("string1", DataType.STRING);
        ioSerialiser.put(pojo.string1);
        ioSerialiser.putFieldHeader("string2", DataType.STRING);
        ioSerialiser.put(pojo.string2);

        // 1D-arrays
        ioSerialiser.putFieldHeader("boolArray", DataType.BOOL_ARRAY);
        ioSerialiser.put(pojo.boolArray);
        ioSerialiser.putFieldHeader("byteArray", DataType.BYTE_ARRAY);
        ioSerialiser.put(pojo.byteArray);
        //        ioSerialiser.putFieldHeader("charArray", DataType.CHAR_ARRAY);
        //        ioSerialiser.put(pojo.charArray);
        ioSerialiser.putFieldHeader("shortArray", DataType.SHORT_ARRAY);
        ioSerialiser.put(pojo.shortArray);
        ioSerialiser.putFieldHeader("intArray", DataType.INT_ARRAY);
        ioSerialiser.put(pojo.intArray);
        ioSerialiser.putFieldHeader("longArray", DataType.LONG_ARRAY);
        ioSerialiser.put(pojo.longArray);
        ioSerialiser.putFieldHeader("floatArray", DataType.FLOAT_ARRAY);
        ioSerialiser.put(pojo.floatArray);
        ioSerialiser.putFieldHeader("doubleArray", DataType.DOUBLE_ARRAY);
        ioSerialiser.put(pojo.doubleArray);
        ioSerialiser.putFieldHeader("stringArray", DataType.STRING_ARRAY);
        ioSerialiser.put(pojo.stringArray);

        // multi-dim case
        ioSerialiser.putFieldHeader("nDimensions", DataType.INT_ARRAY);
        ioSerialiser.put(pojo.nDimensions);
        ioSerialiser.putFieldHeader("boolNdimArray", DataType.BOOL_ARRAY);
        ioSerialiser.put(pojo.boolNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("byteNdimArray", DataType.BYTE_ARRAY);
        ioSerialiser.put(pojo.byteNdimArray, pojo.nDimensions);
        //ioSerialiser.putFieldHeader("charNdimArray", DataType.CHAR_ARRAY);
        //ioSerialiser.put(pojo.charNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("shortNdimArray", DataType.SHORT_ARRAY);
        ioSerialiser.put(pojo.shortNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("intNdimArray", DataType.INT_ARRAY);
        ioSerialiser.put(pojo.intNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("longNdimArray", DataType.LONG_ARRAY);
        ioSerialiser.put(pojo.longNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("floatNdimArray", DataType.FLOAT_ARRAY);
        ioSerialiser.put(pojo.floatNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("doubleNdimArray", DataType.DOUBLE_ARRAY);
        ioSerialiser.put(pojo.doubleNdimArray, pojo.nDimensions);

        if (pojo.nestedData != null) {
            ioSerialiser.putStartMarker("nestedData");
            serialiseCustom(ioSerialiser, pojo.nestedData, false);
            ioSerialiser.putEndMarker("nestedData");
        }

        if (header) {
            ioSerialiser.putEndMarker("OBJ_ROOT_END");
        }
    }

    public static void deserialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo) {
        deserialiseCustom(ioSerialiser, pojo, true);
    }

    public static void deserialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo, boolean header) {
        if (header) {
            ioSerialiser.checkHeaderInfo();
        }
        WireDataFieldDescription field;

        field = getFieldHeader(ioSerialiser);
        pojo.bool1 = ioSerialiser.getBoolean();
        field = getFieldHeader(ioSerialiser);
        pojo.bool2 = ioSerialiser.getBoolean();

        field = getFieldHeader(ioSerialiser);
        pojo.byte1 = ioSerialiser.getByte();
        field = getFieldHeader(ioSerialiser);
        pojo.byte2 = ioSerialiser.getByte();

        field = getFieldHeader(ioSerialiser);
        pojo.char1 = ioSerialiser.getCharacter();
        field = getFieldHeader(ioSerialiser);
        pojo.char2 = ioSerialiser.getCharacter();

        field = getFieldHeader(ioSerialiser);
        pojo.short1 = ioSerialiser.getShort();
        field = getFieldHeader(ioSerialiser);
        pojo.short2 = ioSerialiser.getShort();

        field = getFieldHeader(ioSerialiser);
        pojo.int1 = ioSerialiser.getInteger();
        field = getFieldHeader(ioSerialiser);
        pojo.int2 = ioSerialiser.getInteger();

        field = getFieldHeader(ioSerialiser);
        pojo.long1 = ioSerialiser.getLong();
        field = getFieldHeader(ioSerialiser);
        pojo.long2 = ioSerialiser.getLong();

        field = getFieldHeader(ioSerialiser);
        pojo.float1 = ioSerialiser.getFloat();
        field = getFieldHeader(ioSerialiser);
        pojo.float2 = ioSerialiser.getFloat();

        field = getFieldHeader(ioSerialiser);
        pojo.double1 = ioSerialiser.getDouble();
        field = getFieldHeader(ioSerialiser);
        pojo.double2 = ioSerialiser.getDouble();

        field = getFieldHeader(ioSerialiser);
        pojo.string1 = ioSerialiser.getString();
        field = getFieldHeader(ioSerialiser);
        pojo.string2 = ioSerialiser.getString();

        // 1-dim arrays
        field = getFieldHeader(ioSerialiser);
        pojo.boolArray = ioSerialiser.getBooleanArray();
        field = getFieldHeader(ioSerialiser);
        pojo.byteArray = ioSerialiser.getByteArray();
        //field = getFieldHeader(ioSerialiser);
        //pojo.charArray = ioSerialiser.getCharArray(ioSerialiser);
        field = getFieldHeader(ioSerialiser);
        pojo.shortArray = ioSerialiser.getShortArray();
        field = getFieldHeader(ioSerialiser);
        pojo.intArray = ioSerialiser.getIntArray();
        field = getFieldHeader(ioSerialiser);
        pojo.longArray = ioSerialiser.getLongArray();
        field = getFieldHeader(ioSerialiser);
        pojo.floatArray = ioSerialiser.getFloatArray();
        field = getFieldHeader(ioSerialiser);
        pojo.doubleArray = ioSerialiser.getDoubleArray();
        field = getFieldHeader(ioSerialiser);
        pojo.stringArray = ioSerialiser.getStringArray();

        // multidim case
        field = getFieldHeader(ioSerialiser);
        pojo.nDimensions = ioSerialiser.getIntArray();
        field = getFieldHeader(ioSerialiser);
        pojo.boolNdimArray = ioSerialiser.getBooleanArray();
        field = getFieldHeader(ioSerialiser);
        pojo.byteNdimArray = ioSerialiser.getByteArray();
        field = getFieldHeader(ioSerialiser);
        pojo.shortNdimArray = ioSerialiser.getShortArray();
        field = getFieldHeader(ioSerialiser);
        pojo.intNdimArray = ioSerialiser.getIntArray();
        field = getFieldHeader(ioSerialiser);
        pojo.longNdimArray = ioSerialiser.getLongArray();
        field = getFieldHeader(ioSerialiser);
        pojo.floatNdimArray = ioSerialiser.getFloatArray();
        field = getFieldHeader(ioSerialiser);
        pojo.doubleNdimArray = ioSerialiser.getDoubleArray();

        field = getFieldHeader(ioSerialiser);
        if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER)) {
            final byte byteMarker = ioSerialiser.getByte();

            if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER) || BinarySerialiser.getDataType(DataType.START_MARKER) != byteMarker) {
                throw new IllegalStateException("format error tag with START_MARKER data type = " + field.getDataType() + " markerByte = " + byteMarker + " field name = " + field.getFieldName());
            }

            if (pojo.nestedData == null) {
                pojo.nestedData = new TestDataClass();
            }
            deserialiseCustom(ioSerialiser, pojo.nestedData, false);

        } else if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER)) {
            final byte byteMarker = ioSerialiser.getByte();
            if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER) || BinarySerialiser.getDataType(DataType.END_MARKER) != byteMarker) {
                throw new IllegalStateException("format error tag with END_MARKER data type = " + field.getDataType() + " markerByte = " + byteMarker + " field name = " + field.getFieldName());
            }
        } else {
            throw new IllegalStateException("format error/unexpected tag with data type = " + field.getDataType() + " and field name = " + field.getFieldName());
        }
    }

    private static WireDataFieldDescription getFieldHeader(IoSerialiser ioSerialiser) {
        WireDataFieldDescription field = ioSerialiser.getFieldHeader();
        ioSerialiser.getBuffer().position(field.getDataStartOffset());
        return field;
    }

    public static WireDataFieldDescription deserialiseMap(IoSerialiser ioSerialiser) {
        return ioSerialiser.parseIoStream();
    }
}
