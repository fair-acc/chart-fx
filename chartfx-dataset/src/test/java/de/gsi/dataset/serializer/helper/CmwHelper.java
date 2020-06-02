package de.gsi.dataset.serializer.helper;

//import cern.cmw.data.Data;
//import cern.cmw.data.DataFactory;
//import cern.cmw.data.Entry;

public final class CmwHelper {
    /*
    public static Data getCmwData(final TestDataClass pojo) {
        Data data = DataFactory.createData();

        data.append("bool1", pojo.bool1);
        data.append("bool2", pojo.bool2);
        data.append("byte1", pojo.byte1);
        data.append("byte2", pojo.byte2);
        data.append("char1", pojo.char1);
        data.append("char2", pojo.char2);
        data.append("short1", pojo.short1);
        data.append("short2", pojo.short2);
        data.append("int1", pojo.int1);
        data.append("int2", pojo.int2);
        data.append("long1", pojo.long1);
        data.append("long2", pojo.long2);
        data.append("float1", pojo.float1);
        data.append("float2", pojo.float2);
        data.append("double1", pojo.double1);
        data.append("double2", pojo.double2);
        data.append("string1", pojo.string1);
        // data.append("string2", string2); // N.B. CMW handles only ASCII characters

        // 1-dim array
        data.appendArray("boolArray", pojo.boolArray);
        data.appendArray("byteArray", pojo.byteArray);
        //data.appendArray("charArray", pojo.charArray); // not supported by CMW
        data.appendArray("shortArray", pojo.shortArray);
        data.appendArray("intArray", pojo.intArray);
        data.appendArray("longArray", pojo.longArray);
        data.appendArray("floatArray", pojo.floatArray);
        data.appendArray("doubleArray", pojo.doubleArray);
        data.appendArray("stringArray", pojo.stringArray);

        // multidim arrays
        data.appendMultiArray("boolNdimArray", pojo.boolNdimArray, pojo.nDimensions);
        data.appendMultiArray("byteNdimArray", pojo.byteNdimArray, pojo.nDimensions);
        //data.appendMultiArray("charNdimArray", pojo.charNdimArray, pojo.nDimensions); // not supported by CMW
        data.appendMultiArray("shortNdimArray", pojo.shortNdimArray, pojo.nDimensions);
        data.appendMultiArray("intNdimArray", pojo.intNdimArray, pojo.nDimensions);
        data.appendMultiArray("longNdimArray", pojo.longNdimArray, pojo.nDimensions);
        data.appendMultiArray("floatNdimArray", pojo.floatNdimArray, pojo.nDimensions);
        data.appendMultiArray("doubleNdimArray", pojo.doubleNdimArray, pojo.nDimensions);

        if (pojo.nestedData != null) {
            data.append("nestedData", getCmwData(pojo.nestedData));
        }

        return data;
    }

    public static void applyCmwData(final Data data, final TestDataClass pojo) {
        pojo.bool1 = data.getBool("bool1");
        pojo.bool2 = data.getBool("bool2");
        pojo.byte1 = data.getByte("byte1");
        pojo.byte2 = data.getByte("byte2");
        //pojo.char1 = data.getCharacter("char1"); // not supported by CMW
        //pojo.char2 = data.getCharacter("char2"); // not supported by CMW
        pojo.short1 = data.getShort("short1");
        pojo.short2 = data.getShort("short2");
        pojo.int1 = data.getInt("int1");
        pojo.int2 = data.getInt("int2");
        pojo.long1 = data.getLong("long1");
        pojo.long2 = data.getLong("long2");
        pojo.float1 = data.getFloat("float1");
        pojo.float2 = data.getFloat("float2");
        pojo.double1 = data.getDouble("double1");
        pojo.double2 = data.getDouble("double2");
        pojo.string1 = data.getString("string1");
        //pojo.string2 = data.getString("string2"); // N.B. handles only ASCII characters

        // 1-dim array
        pojo.boolArray = data.getBoolArray("boolArray");
        pojo.byteArray = data.getByteArray("byteArray");
        //pojo.charArray = data.getCharacterArray("byteArray"); // not supported by CMW
        pojo.shortArray = data.getShortArray("shortArray");
        pojo.intArray = data.getIntArray("intArray");
        pojo.longArray = data.getLongArray("longArray");
        pojo.floatArray = data.getFloatArray("floatArray");
        pojo.doubleArray = data.getDoubleArray("doubleArray");
        pojo.stringArray = data.getStringArray("stringArray");

        // multi-dim arrays
        pojo.boolNdimArray = data.getBoolMultiArray("boolNdimArray").getElements();
        pojo.byteNdimArray = data.getByteMultiArray("byteNdimArray").getElements();
        //pojo.charNdimArray = data.getCharMultiArray("byteArray"); // not supported by CMW
        pojo.shortNdimArray = data.getShortMultiArray("shortNdimArray").getElements();
        pojo.intNdimArray = data.getIntMultiArray("intNdimArray").getElements();
        pojo.longNdimArray = data.getLongMultiArray("longNdimArray").getElements();
        pojo.floatNdimArray = data.getFloatMultiArray("floatNdimArray").getElements();
        pojo.doubleNdimArray = data.getDoubleMultiArray("doubleNdimArray").getElements();

        final Entry nestedEntry = data.getEntry("nestedData");
        if (nestedEntry != null) {
            if (pojo.nestedData == null) {
                pojo.nestedData = new TestDataClass(-1, -1, -1);
            }
            applyCmwData(nestedEntry.getData(), pojo.nestedData);
        }
    }
    */
}
