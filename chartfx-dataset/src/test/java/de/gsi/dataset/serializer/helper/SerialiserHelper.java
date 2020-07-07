package de.gsi.dataset.serializer.helper;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoSerialiser;
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
        ioSerialiser.getBuffer().putBoolean(pojo.bool1);
        ioSerialiser.putFieldHeader("bool2", DataType.BOOL);
        ioSerialiser.getBuffer().putBoolean(pojo.bool2);
        ioSerialiser.putFieldHeader("byte1", DataType.BYTE);
        ioSerialiser.getBuffer().putByte(pojo.byte1);
        ioSerialiser.putFieldHeader("byte2", DataType.BYTE);
        ioSerialiser.getBuffer().putByte(pojo.byte2);
        ioSerialiser.putFieldHeader("char1", DataType.CHAR);
        ioSerialiser.getBuffer().putChar(pojo.char1);
        ioSerialiser.putFieldHeader("char2", DataType.CHAR);
        ioSerialiser.getBuffer().putChar(pojo.char2);
        ioSerialiser.putFieldHeader("short1", DataType.SHORT);
        ioSerialiser.getBuffer().putShort(pojo.short1);
        ioSerialiser.putFieldHeader("short2", DataType.SHORT);
        ioSerialiser.getBuffer().putShort(pojo.short2);
        ioSerialiser.putFieldHeader("int1", DataType.INT);
        ioSerialiser.getBuffer().putInt(pojo.int1);
        ioSerialiser.putFieldHeader("int2", DataType.INT);
        ioSerialiser.getBuffer().putInt(pojo.int2);
        ioSerialiser.putFieldHeader("long1", DataType.LONG);
        ioSerialiser.getBuffer().putLong(pojo.long1);
        ioSerialiser.putFieldHeader("long2", DataType.LONG);
        ioSerialiser.getBuffer().putLong(pojo.long2);
        ioSerialiser.putFieldHeader("float1", DataType.FLOAT);
        ioSerialiser.getBuffer().putFloat(pojo.float1);
        ioSerialiser.putFieldHeader("float2", DataType.FLOAT);
        ioSerialiser.getBuffer().putFloat(pojo.float2);
        ioSerialiser.putFieldHeader("double1", DataType.DOUBLE);
        ioSerialiser.getBuffer().putDouble(pojo.double1);
        ioSerialiser.putFieldHeader("double2", DataType.DOUBLE);
        ioSerialiser.getBuffer().putDouble(pojo.double2);
        ioSerialiser.putFieldHeader("string1", DataType.STRING);
        ioSerialiser.getBuffer().putString(pojo.string1);
        ioSerialiser.putFieldHeader("string2", DataType.STRING);
        ioSerialiser.getBuffer().putString(pojo.string2);

        // 1D-arrays
        ioSerialiser.putFieldHeader("boolArray", DataType.BOOL_ARRAY);
        ioSerialiser.getBuffer().putBooleanArray(pojo.boolArray, 0, pojo.boolArray.length);
        ioSerialiser.putFieldHeader("byteArray", DataType.BYTE_ARRAY);
        ioSerialiser.getBuffer().putByteArray(pojo.byteArray, 0, pojo.byteArray.length);
        //ioSerialiser.putFieldHeader("charArray", DataType.CHAR_ARRAY);
        //ioSerialiser.getBuffer().putCharArray(pojo.charArray, 0, pojo.charArray.lenght);
        ioSerialiser.putFieldHeader("shortArray", DataType.SHORT_ARRAY);
        ioSerialiser.getBuffer().putShortArray(pojo.shortArray, 0, pojo.shortArray.length);
        ioSerialiser.putFieldHeader("intArray", DataType.INT_ARRAY);
        ioSerialiser.getBuffer().putIntArray(pojo.intArray, 0, pojo.intArray.length);
        ioSerialiser.putFieldHeader("longArray", DataType.LONG_ARRAY);
        ioSerialiser.getBuffer().putLongArray(pojo.longArray, 0, pojo.longArray.length);
        ioSerialiser.putFieldHeader("floatArray", DataType.FLOAT_ARRAY);
        ioSerialiser.getBuffer().putFloatArray(pojo.floatArray, 0, pojo.floatArray.length);
        ioSerialiser.putFieldHeader("doubleArray", DataType.DOUBLE_ARRAY);
        ioSerialiser.getBuffer().putDoubleArray(pojo.doubleArray, 0, pojo.doubleArray.length);
        ioSerialiser.putFieldHeader("stringArray", DataType.STRING_ARRAY);
        ioSerialiser.getBuffer().putStringArray(pojo.stringArray, 0, pojo.stringArray.length);

        // multi-dim case
        ioSerialiser.putFieldHeader("nDimensions", DataType.INT_ARRAY);
        ioSerialiser.getBuffer().putIntArray(pojo.nDimensions, 0, pojo.nDimensions.length);
        ioSerialiser.putFieldHeader("boolNdimArray", DataType.BOOL_ARRAY);
        ioSerialiser.getBuffer().putBooleanArray(pojo.boolNdimArray, 0, pojo.nDimensions);
        ioSerialiser.putFieldHeader("byteNdimArray", DataType.BYTE_ARRAY);
        ioSerialiser.getBuffer().putByteArray(pojo.byteNdimArray, 0, pojo.nDimensions);
        //ioSerialiser.putFieldHeader("charNdimArray", DataType.CHAR_ARRAY);
        //ioSerialiser.put(pojo.charNdimArray, pojo.nDimensions);
        ioSerialiser.putFieldHeader("shortNdimArray", DataType.SHORT_ARRAY);
        ioSerialiser.getBuffer().putShortArray(pojo.shortNdimArray, 0, pojo.nDimensions);
        ioSerialiser.putFieldHeader("intNdimArray", DataType.INT_ARRAY);
        ioSerialiser.getBuffer().putIntArray(pojo.intNdimArray, 0, pojo.nDimensions);
        ioSerialiser.putFieldHeader("longNdimArray", DataType.LONG_ARRAY);
        ioSerialiser.getBuffer().putLongArray(pojo.longNdimArray, 0, pojo.nDimensions);
        ioSerialiser.putFieldHeader("floatNdimArray", DataType.FLOAT_ARRAY);
        ioSerialiser.getBuffer().putFloatArray(pojo.floatNdimArray, 0, pojo.nDimensions);
        ioSerialiser.putFieldHeader("doubleNdimArray", DataType.DOUBLE_ARRAY);
        ioSerialiser.getBuffer().putDoubleArray(pojo.doubleNdimArray, 0, pojo.nDimensions);

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

        getFieldHeader(ioSerialiser);
        pojo.bool1 = ioSerialiser.getBuffer().getBoolean();
        getFieldHeader(ioSerialiser);
        pojo.bool2 = ioSerialiser.getBuffer().getBoolean();

        getFieldHeader(ioSerialiser);
        pojo.byte1 = ioSerialiser.getBuffer().getByte();
        getFieldHeader(ioSerialiser);
        pojo.byte2 = ioSerialiser.getBuffer().getByte();

        getFieldHeader(ioSerialiser);
        pojo.char1 = ioSerialiser.getBuffer().getChar();
        getFieldHeader(ioSerialiser);
        pojo.char2 = ioSerialiser.getBuffer().getChar();

        getFieldHeader(ioSerialiser);
        pojo.short1 = ioSerialiser.getBuffer().getShort();
        getFieldHeader(ioSerialiser);
        pojo.short2 = ioSerialiser.getBuffer().getShort();

        getFieldHeader(ioSerialiser);
        pojo.int1 = ioSerialiser.getBuffer().getInt();
        getFieldHeader(ioSerialiser);
        pojo.int2 = ioSerialiser.getBuffer().getInt();

        getFieldHeader(ioSerialiser);
        pojo.long1 = ioSerialiser.getBuffer().getLong();
        getFieldHeader(ioSerialiser);
        pojo.long2 = ioSerialiser.getBuffer().getLong();

        getFieldHeader(ioSerialiser);
        pojo.float1 = ioSerialiser.getBuffer().getFloat();
        getFieldHeader(ioSerialiser);
        pojo.float2 = ioSerialiser.getBuffer().getFloat();

        getFieldHeader(ioSerialiser);
        pojo.double1 = ioSerialiser.getBuffer().getDouble();
        getFieldHeader(ioSerialiser);
        pojo.double2 = ioSerialiser.getBuffer().getDouble();

        getFieldHeader(ioSerialiser);
        pojo.string1 = ioSerialiser.getBuffer().getString();
        getFieldHeader(ioSerialiser);
        pojo.string2 = ioSerialiser.getBuffer().getString();

        // 1-dim arrays
        getFieldHeader(ioSerialiser);
        pojo.boolArray = ioSerialiser.getBuffer().getBooleanArray();
        getFieldHeader(ioSerialiser);
        pojo.byteArray = ioSerialiser.getBuffer().getByteArray();
        //getFieldHeader(ioSerialiser);
        //pojo.charArray = ioSerialiser.getBuffer().getCharArray(ioSerialiser);
        getFieldHeader(ioSerialiser);
        pojo.shortArray = ioSerialiser.getBuffer().getShortArray();
        getFieldHeader(ioSerialiser);
        pojo.intArray = ioSerialiser.getBuffer().getIntArray();
        getFieldHeader(ioSerialiser);
        pojo.longArray = ioSerialiser.getBuffer().getLongArray();
        getFieldHeader(ioSerialiser);
        pojo.floatArray = ioSerialiser.getBuffer().getFloatArray();
        getFieldHeader(ioSerialiser);
        pojo.doubleArray = ioSerialiser.getBuffer().getDoubleArray();
        getFieldHeader(ioSerialiser);
        pojo.stringArray = ioSerialiser.getBuffer().getStringArray();

        // multidim case
        getFieldHeader(ioSerialiser);
        pojo.nDimensions = ioSerialiser.getBuffer().getIntArray();
        getFieldHeader(ioSerialiser);
        pojo.boolNdimArray = ioSerialiser.getBuffer().getBooleanArray();
        getFieldHeader(ioSerialiser);
        pojo.byteNdimArray = ioSerialiser.getBuffer().getByteArray();
        getFieldHeader(ioSerialiser);
        pojo.shortNdimArray = ioSerialiser.getBuffer().getShortArray();
        getFieldHeader(ioSerialiser);
        pojo.intNdimArray = ioSerialiser.getBuffer().getIntArray();
        getFieldHeader(ioSerialiser);
        pojo.longNdimArray = ioSerialiser.getBuffer().getLongArray();
        getFieldHeader(ioSerialiser);
        pojo.floatNdimArray = ioSerialiser.getBuffer().getFloatArray();
        getFieldHeader(ioSerialiser);
        pojo.doubleNdimArray = ioSerialiser.getBuffer().getDoubleArray();

        final WireDataFieldDescription field = getFieldHeader(ioSerialiser);
        if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER)) {
            if (pojo.nestedData == null) {
                pojo.nestedData = new TestDataClass();
            }
            deserialiseCustom(ioSerialiser, pojo.nestedData, false);

        } else if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER)) {
            throw new IllegalStateException("format error/unexpected tag with data type = " + field.getDataType() + " and field name = " + field.getFieldName());
        }
    }

    private static WireDataFieldDescription getFieldHeader(IoSerialiser ioSerialiser) {
        WireDataFieldDescription field = ioSerialiser.getFieldHeader();
        ioSerialiser.getBuffer().position(field.getDataStartPosition());
        return field;
    }

    public static WireDataFieldDescription deserialiseMap(IoSerialiser ioSerialiser) {
        return ioSerialiser.parseIoStream(true);
    }
}
