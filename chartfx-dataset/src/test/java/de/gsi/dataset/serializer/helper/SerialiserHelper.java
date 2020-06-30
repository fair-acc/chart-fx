package de.gsi.dataset.serializer.helper;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.FieldHeader;

@SuppressWarnings("PMD") // complexity is part of the very large use-case surface that is being tested
public final class SerialiserHelper {
    public static void serialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo) {
        serialiseCustom(ioSerialiser, pojo, true);
    }

    public static void serialiseCustom(final IoSerialiser ioSerialiser, final TestDataClass pojo, final boolean header) {
        if (header) {
            ioSerialiser.putHeaderInfo();
        }

        ioSerialiser.put("bool1", pojo.bool1);
        ioSerialiser.put("bool2", pojo.bool2);
        ioSerialiser.put("byte1", pojo.byte1);
        ioSerialiser.put("byte2", pojo.byte2);
        ioSerialiser.put("char1", pojo.char1);
        ioSerialiser.put("char2", pojo.char2);
        ioSerialiser.put("short1", pojo.short1);
        ioSerialiser.put("short2", pojo.short2);
        ioSerialiser.put("int1", pojo.int1);
        ioSerialiser.put("int2", pojo.int2);
        ioSerialiser.put("long1", pojo.long1);
        ioSerialiser.put("long2", pojo.long2);
        ioSerialiser.put("float1", pojo.float1);
        ioSerialiser.put("float2", pojo.float2);
        ioSerialiser.put("double1", pojo.double1);
        ioSerialiser.put("double2", pojo.double2);
        ioSerialiser.put("string1", pojo.string1);
        ioSerialiser.put("string2", pojo.string2);

        // 1D-arrays
        ioSerialiser.put("boolArray", pojo.boolArray);
        ioSerialiser.put("byteArray", pojo.byteArray);
        //        ioSerialiser.put( "charArray", pojo.charArray);
        ioSerialiser.put("shortArray", pojo.shortArray);
        ioSerialiser.put("intArray", pojo.intArray);
        ioSerialiser.put("longArray", pojo.longArray);
        ioSerialiser.put("floatArray", pojo.floatArray);
        ioSerialiser.put("doubleArray", pojo.doubleArray);
        ioSerialiser.put("stringArray", pojo.stringArray);

        // multi-dim case
        ioSerialiser.put("nDimensions", pojo.nDimensions);
        ioSerialiser.put("boolNdimArray", pojo.boolNdimArray, pojo.nDimensions);
        ioSerialiser.put("byteNdimArray", pojo.byteNdimArray, pojo.nDimensions);
        //ioSerialiser.put( "charNdimArray", pojo.charNdimArray, pojo.nDimensions);
        ioSerialiser.put("shortNdimArray", pojo.shortNdimArray, pojo.nDimensions);
        ioSerialiser.put("intNdimArray", pojo.intNdimArray, pojo.nDimensions);
        ioSerialiser.put("longNdimArray", pojo.longNdimArray, pojo.nDimensions);
        ioSerialiser.put("floatNdimArray", pojo.floatNdimArray, pojo.nDimensions);
        ioSerialiser.put("doubleNdimArray", pojo.doubleNdimArray, pojo.nDimensions);

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
        FieldHeader field;

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
            if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER) || DataType.START_MARKER.getAsByte() != byteMarker) {
                throw new IllegalStateException("format error tag with START_MARKER data type = " + field.getDataType() + " markerByte = " + byteMarker + " field name = " + field.getFieldName());
            }

            if (pojo.nestedData == null) {
                pojo.nestedData = new TestDataClass();
            }
            deserialiseCustom(ioSerialiser, pojo.nestedData, false);

        } else if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER)) {
            final byte byteMarker = ioSerialiser.getByte();
            if (!field.getDataType().equals(de.gsi.dataset.serializer.DataType.END_MARKER) || DataType.END_MARKER.getAsByte() != byteMarker) {
                throw new IllegalStateException("format error tag with END_MARKER data type = " + field.getDataType() + " markerByte = " + byteMarker + " field name = " + field.getFieldName());
            }
        } else {
            throw new IllegalStateException("format error/unexpected tag with data type = " + field.getDataType() + " and field name = " + field.getFieldName());
        }
    }

    private static FieldHeader getFieldHeader(IoSerialiser ioSerialiser) {
        FieldHeader field = ioSerialiser.getFieldHeader();
        ioSerialiser.getBuffer().position(field.getDataBufferPosition());
        return field;
    }

    public static FieldHeader deserialiseMap(IoSerialiser ioSerialiser) {
        return ioSerialiser.parseIoStream();
    }
}
