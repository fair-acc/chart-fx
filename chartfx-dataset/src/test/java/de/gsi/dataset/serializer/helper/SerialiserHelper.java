package de.gsi.dataset.serializer.helper;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.FieldHeader;

@SuppressWarnings("PMD") // complexity is part of the very large use-case surface that is being tested 
public final class SerialiserHelper {
    public static void serialiseCustom(IoBuffer bytebuffer, final TestDataClass pojo) {
        serialiseCustom(bytebuffer, pojo, true);
    }

    public static void serialiseCustom(final IoBuffer bytebuffer, final TestDataClass pojo, final boolean header) {
        if (header) {
            BinarySerialiser.putHeaderInfo(bytebuffer);
        }

        BinarySerialiser.put(bytebuffer, "bool1", pojo.bool1);
        BinarySerialiser.put(bytebuffer, "bool2", pojo.bool2);
        BinarySerialiser.put(bytebuffer, "byte1", pojo.byte1);
        BinarySerialiser.put(bytebuffer, "byte2", pojo.byte2);
        BinarySerialiser.put(bytebuffer, "char1", pojo.char1);
        BinarySerialiser.put(bytebuffer, "char2", pojo.char2);
        BinarySerialiser.put(bytebuffer, "short1", pojo.short1);
        BinarySerialiser.put(bytebuffer, "short2", pojo.short2);
        BinarySerialiser.put(bytebuffer, "int1", pojo.int1);
        BinarySerialiser.put(bytebuffer, "int2", pojo.int2);
        BinarySerialiser.put(bytebuffer, "long1", pojo.long1);
        BinarySerialiser.put(bytebuffer, "long2", pojo.long2);
        BinarySerialiser.put(bytebuffer, "float1", pojo.float1);
        BinarySerialiser.put(bytebuffer, "float2", pojo.float2);
        BinarySerialiser.put(bytebuffer, "double1", pojo.double1);
        BinarySerialiser.put(bytebuffer, "double2", pojo.double2);
        BinarySerialiser.put(bytebuffer, "string1", pojo.string1);
        BinarySerialiser.put(bytebuffer, "string2", pojo.string2);

        // 1D-arrays
        BinarySerialiser.put(bytebuffer, "boolArray", pojo.boolArray);
        BinarySerialiser.put(bytebuffer, "byteArray", pojo.byteArray);
        //        BinarySerialiser.put(bytebuffer, "charArray", pojo.charArray);
        BinarySerialiser.put(bytebuffer, "shortArray", pojo.shortArray);
        BinarySerialiser.put(bytebuffer, "intArray", pojo.intArray);
        BinarySerialiser.put(bytebuffer, "longArray", pojo.longArray);
        BinarySerialiser.put(bytebuffer, "floatArray", pojo.floatArray);
        BinarySerialiser.put(bytebuffer, "doubleArray", pojo.doubleArray);
        BinarySerialiser.put(bytebuffer, "stringArray", pojo.stringArray);

        // multi-dim case
        BinarySerialiser.put(bytebuffer, "nDimensions", pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "boolNdimArray", pojo.boolNdimArray, pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "byteNdimArray", pojo.byteNdimArray, pojo.nDimensions);
        //BinarySerialiser.put(bytebuffer, "charNdimArray", pojo.charNdimArray, pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "shortNdimArray", pojo.shortNdimArray, pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "intNdimArray", pojo.intNdimArray, pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "longNdimArray", pojo.longNdimArray, pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "floatNdimArray", pojo.floatNdimArray, pojo.nDimensions);
        BinarySerialiser.put(bytebuffer, "doubleNdimArray", pojo.doubleNdimArray, pojo.nDimensions);

        if (pojo.nestedData != null) {
            BinarySerialiser.putStartMarker(bytebuffer, "nestedData");
            serialiseCustom(bytebuffer, pojo.nestedData, false);
            BinarySerialiser.putEndMarker(bytebuffer, "nestedData");
        }

        if (header) {
            BinarySerialiser.putEndMarker(bytebuffer, "OBJ_ROOT_END");
        }
    }

    public static void deserialiseCustom(IoBuffer bytebuffer, final TestDataClass pojo) {
        deserialiseCustom(bytebuffer, pojo, true);
    }

    public static void deserialiseCustom(IoBuffer bytebuffer, final TestDataClass pojo, boolean header) {
        if (header) {
            BinarySerialiser.checkHeaderInfo(bytebuffer);
        }
        FieldHeader field;

        field = getFieldHeader(bytebuffer);
        pojo.bool1 = BinarySerialiser.getBoolean(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.bool2 = BinarySerialiser.getBoolean(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.byte1 = BinarySerialiser.getByte(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.byte2 = BinarySerialiser.getByte(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.char1 = BinarySerialiser.getCharacter(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.char2 = BinarySerialiser.getCharacter(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.short1 = BinarySerialiser.getShort(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.short2 = BinarySerialiser.getShort(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.int1 = BinarySerialiser.getInteger(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.int2 = BinarySerialiser.getInteger(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.long1 = BinarySerialiser.getLong(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.long2 = BinarySerialiser.getLong(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.float1 = BinarySerialiser.getFloat(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.float2 = BinarySerialiser.getFloat(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.double1 = BinarySerialiser.getDouble(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.double2 = BinarySerialiser.getDouble(bytebuffer);

        field = getFieldHeader(bytebuffer);
        pojo.string1 = BinarySerialiser.getString(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.string2 = BinarySerialiser.getString(bytebuffer);

        // 1-dim arrays
        field = getFieldHeader(bytebuffer);
        pojo.boolArray = BinarySerialiser.getBooleanArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.byteArray = BinarySerialiser.getByteArray(bytebuffer);
        //field = getFieldHeader(bytebuffer);
        //pojo.charArray = BinarySerialiser.getCharArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.shortArray = BinarySerialiser.getShortArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.intArray = BinarySerialiser.getIntArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.longArray = BinarySerialiser.getLongArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.floatArray = BinarySerialiser.getFloatArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.doubleArray = BinarySerialiser.getDoubleArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.stringArray = BinarySerialiser.getStringArray(bytebuffer);

        // multidim case
        field = getFieldHeader(bytebuffer);
        pojo.nDimensions = BinarySerialiser.getIntArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.boolNdimArray = BinarySerialiser.getBooleanArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.byteNdimArray = BinarySerialiser.getByteArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.shortNdimArray = BinarySerialiser.getShortArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.intNdimArray = BinarySerialiser.getIntArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.longNdimArray = BinarySerialiser.getLongArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.floatNdimArray = BinarySerialiser.getFloatArray(bytebuffer);
        field = getFieldHeader(bytebuffer);
        pojo.doubleNdimArray = BinarySerialiser.getDoubleArray(bytebuffer);

        field = getFieldHeader(bytebuffer);
        if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER)) {
            final byte byteMarker = BinarySerialiser.getByte(bytebuffer);
            if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER) || DataType.START_MARKER.getAsByte() != byteMarker) {
                throw new IllegalStateException("format error tag with START_MARKER data type = " + field.getDataType() + " markerByte = " + byteMarker + " field name = " + field.getFieldName());
            }

            if (pojo.nestedData == null) {
                pojo.nestedData = new TestDataClass();
            }
            deserialiseCustom(bytebuffer, pojo.nestedData, false);

        } else if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER)) {
            final byte byteMarker = BinarySerialiser.getByte(bytebuffer);
            if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER) || DataType.END_MARKER.getAsByte() != byteMarker) {
                throw new IllegalStateException("format error tag with END_MARKER data type = " + field.getDataType() + " markerByte = " + byteMarker + " field name = " + field.getFieldName());
            }
            return;
        } else {
            throw new IllegalStateException("format error/unexpected tag with data type = " + field.getDataType() + " and field name = " + field.getFieldName());
        }
    }

    private static FieldHeader getFieldHeader(IoBuffer bytebuffer) {
        FieldHeader field = BinarySerialiser.getFieldHeader(bytebuffer);
        bytebuffer.position(field.getDataBufferPosition());
        return field;
    }

    public static FieldHeader deserialiseMap(IoBuffer bytebuffer) {
        return BinarySerialiser.parseIoStream(bytebuffer);
    }
}
