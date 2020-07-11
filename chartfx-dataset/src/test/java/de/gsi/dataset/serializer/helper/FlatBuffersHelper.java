package de.gsi.dataset.serializer.helper;

import java.nio.ByteBuffer;

import com.google.flatbuffers.ArrayReadWriteBuf;
import com.google.flatbuffers.FlexBuffers;
import com.google.flatbuffers.FlexBuffersBuilder;

@SuppressWarnings("PMD") // complexity is part of the very large use-case surface that is being tested
public class FlatBuffersHelper {
    public static ByteBuffer serialiseCustom(FlexBuffersBuilder builder, final TestDataClass pojo) {
        return serialiseCustom(builder, pojo, true);
    }

    public static ByteBuffer serialiseCustom(FlexBuffersBuilder builder, final TestDataClass pojo, final boolean header) {
        final int map = builder.startMap();
        builder.putBoolean("bool1", pojo.bool1);
        builder.putBoolean("bool2", pojo.bool2);

        builder.putInt("byte1", pojo.byte1);
        builder.putInt("byte2", pojo.byte2);
        builder.putInt("short1", pojo.short1);
        builder.putInt("short2", pojo.short2);
        builder.putInt("char1", pojo.char1);
        builder.putInt("char2", pojo.char2);
        builder.putInt("int1", pojo.int1);
        builder.putInt("int2", pojo.int2);
        builder.putInt("long1", pojo.long1);
        builder.putInt("long2", pojo.long2);

        builder.putFloat("float1", pojo.float1);
        builder.putFloat("float2", pojo.float2);
        builder.putFloat("double1", pojo.double1);
        builder.putFloat("double2", pojo.double2);
        builder.putString("string1", pojo.string1);
        builder.putString("string2", pojo.string2);

        // 1D-arrays
        final boolean typed = false;
        final boolean fixed = false;
        int svec = builder.startVector();
        for (int i = 0; i < pojo.boolArray.length; i++) {
            builder.putBoolean(pojo.boolArray[i]);
        }
        builder.endVector("boolArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.byteArray.length; i++) {
            builder.putInt(pojo.byteArray[i]);
        }
        builder.endVector("byteArray", svec, typed, fixed);

        //        builder.putFieldHeader("charArray", DataType.CHAR_ARRAY);
        //        builder.put(pojo.charArray);

        svec = builder.startVector();
        for (int i = 0; i < pojo.shortArray.length; i++) {
            builder.putInt(pojo.shortArray[i]);
        }
        builder.endVector("shortArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.intArray.length; i++) {
            builder.putInt(pojo.intArray[i]);
        }
        builder.endVector("intArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.longArray.length; i++) {
            builder.putInt(pojo.longArray[i]);
        }
        builder.endVector("longArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.floatArray.length; i++) {
            builder.putFloat(pojo.floatArray[i]);
        }
        builder.endVector("floatArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.doubleArray.length; i++) {
            builder.putFloat(pojo.doubleArray[i]);
        }
        builder.endVector("doubleArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.stringArray.length; i++) {
            builder.putString(pojo.stringArray[i]);
        }
        builder.endVector("stringArray", svec, typed, fixed);

        // multi-dim case
        svec = builder.startVector();
        for (int i = 0; i < pojo.nDimensions.length; i++) {
            builder.putInt(pojo.nDimensions[i]);
        }
        builder.endVector("nDimensions", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.boolNdimArray.length; i++) {
            builder.putBoolean(pojo.boolNdimArray[i]);
        }
        builder.endVector("boolNdimArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.byteNdimArray.length; i++) {
            builder.putInt(pojo.byteNdimArray[i]);
        }
        builder.endVector("byteNdimArray", svec, typed, fixed);

        //        builder.putFieldHeader("charArray", DataType.CHAR_ARRAY);
        //        builder.put(pojo.charArray);

        svec = builder.startVector();
        for (int i = 0; i < pojo.shortNdimArray.length; i++) {
            builder.putInt(pojo.shortNdimArray[i]);
        }
        builder.endVector("shortNdimArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.intNdimArray.length; i++) {
            builder.putInt(pojo.intNdimArray[i]);
        }
        builder.endVector("intNdimArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.longNdimArray.length; i++) {
            builder.putInt(pojo.longNdimArray[i]);
        }
        builder.endVector("longNdimArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.floatNdimArray.length; i++) {
            builder.putFloat(pojo.floatNdimArray[i]);
        }
        builder.endVector("floatNdimArray", svec, typed, fixed);

        svec = builder.startVector();
        for (int i = 0; i < pojo.doubleNdimArray.length; i++) {
            builder.putFloat(pojo.doubleNdimArray[i]);
        }
        builder.endVector("doubleNdimArray", svec, typed, fixed);
        builder.endMap(null, map);

        if (pojo.nestedData != null) {
            //            final int nestedMap = builder.startMap();
            serialiseCustom(builder, pojo.nestedData, false);
            //            builder.endMap("nestedData", map);
        }

        if (header) {
            return builder.finish();
        }
        return null;
    }

    public static void deserialiseCustom(ByteBuffer buffer, final TestDataClass pojo) {
        FlexBuffers.Map map = FlexBuffers.getRoot(new ArrayReadWriteBuf(buffer.array(), buffer.limit())).asMap();
        deserialiseCustom(map, pojo, true);
    }

    public static void deserialiseCustom(FlexBuffers.Map map, final TestDataClass pojo, boolean header) {
        pojo.bool1 = map.get("bool1").asBoolean();
        pojo.bool2 = map.get("bool2").asBoolean();
        pojo.byte1 = (byte) map.get("byte1").asInt();
        pojo.byte2 = (byte) map.get("byte2").asInt();
        pojo.char1 = (char) map.get("char1").asInt();
        pojo.char2 = (char) map.get("char2").asInt();
        pojo.short1 = (short) map.get("short1").asInt();
        pojo.short2 = (short) map.get("short2").asInt();
        pojo.int1 = map.get("int1").asInt();
        pojo.int2 = map.get("int2").asInt();
        pojo.long1 = map.get("long1").asLong();
        pojo.long2 = map.get("long2").asLong();
        pojo.float1 = (float) map.get("float1").asFloat();
        pojo.float2 = (float) map.get("float2").asFloat();
        pojo.double1 = map.get("double1").asFloat();
        pojo.double2 = map.get("double2").asFloat();
        pojo.string1 = map.get("string1").asString();
        pojo.string2 = map.get("string2").asString();

        // 1-dim arrays
        FlexBuffers.Vector vector;

        vector = map.get("boolArray").asVector();
        pojo.boolArray = new boolean[vector.size()];
        for (int i = 0; i < pojo.boolArray.length; i++) {
            pojo.boolArray[i] = vector.get(i).asBoolean();
        }

        vector = map.get("byteArray").asVector();
        pojo.byteArray = new byte[vector.size()];
        for (int i = 0; i < pojo.byteArray.length; i++) {
            pojo.byteArray[i] = (byte) vector.get(i).asInt();
        }

        vector = map.get("shortArray").asVector();
        pojo.shortArray = new short[vector.size()];
        for (int i = 0; i < pojo.shortArray.length; i++) {
            pojo.shortArray[i] = (short) vector.get(i).asInt();
        }

        //        vector = map.get("charArray").asVector();
        //        pojo.charArray = new int[vector.size()];
        //        for (int i = 0; i < pojo.boolArray.length; i++) {
        //            pojo.charArray[i] = (char)vector.get(i).asInt();
        //        }

        vector = map.get("intArray").asVector();
        pojo.intArray = new int[vector.size()];
        for (int i = 0; i < pojo.intArray.length; i++) {
            pojo.intArray[i] = vector.get(i).asInt();
        }

        vector = map.get("longArray").asVector();
        pojo.longArray = new long[vector.size()];
        for (int i = 0; i < pojo.longArray.length; i++) {
            pojo.longArray[i] = vector.get(i).asLong();
        }

        vector = map.get("floatArray").asVector();
        pojo.floatArray = new float[vector.size()];
        for (int i = 0; i < pojo.floatArray.length; i++) {
            pojo.floatArray[i] = (float) vector.get(i).asFloat();
        }

        vector = map.get("doubleArray").asVector();
        pojo.doubleArray = new double[vector.size()];
        for (int i = 0; i < pojo.doubleArray.length; i++) {
            pojo.doubleArray[i] = vector.get(i).asFloat();
        }

        vector = map.get("stringArray").asVector();
        pojo.stringArray = new String[vector.size()];
        for (int i = 0; i < pojo.stringArray.length; i++) {
            pojo.stringArray[i] = vector.get(i).asString();
        }

        // multidim case
        vector = map.get("nDimensions").asVector();
        pojo.nDimensions = new int[vector.size()];
        for (int i = 0; i < pojo.nDimensions.length; i++) {
            pojo.nDimensions[i] = vector.get(i).asInt();
        }

        vector = map.get("boolNdimArray").asVector();
        pojo.boolNdimArray = new boolean[vector.size()];
        for (int i = 0; i < pojo.boolNdimArray.length; i++) {
            pojo.boolNdimArray[i] = vector.get(i).asBoolean();
        }

        vector = map.get("byteNdimArray").asVector();
        pojo.byteNdimArray = new byte[vector.size()];
        for (int i = 0; i < pojo.byteNdimArray.length; i++) {
            pojo.byteNdimArray[i] = (byte) vector.get(i).asInt();
        }

        vector = map.get("shortNdimArray").asVector();
        pojo.shortNdimArray = new short[vector.size()];
        for (int i = 0; i < pojo.shortNdimArray.length; i++) {
            pojo.shortNdimArray[i] = (short) vector.get(i).asInt();
        }

        //        vector = map.get("charNdimArray").asVector();
        //        pojo.charNdimArray = new int[vector.size()];
        //        for (int i = 0; i < pojo.charNdimArray.length; i++) {
        //            pojo.charNdimArray[i] = (char)vector.get(i).asInt();
        //        }

        vector = map.get("intNdimArray").asVector();
        pojo.intNdimArray = new int[vector.size()];
        for (int i = 0; i < pojo.intNdimArray.length; i++) {
            pojo.intNdimArray[i] = vector.get(i).asInt();
        }

        vector = map.get("longNdimArray").asVector();
        pojo.longNdimArray = new long[vector.size()];
        for (int i = 0; i < pojo.longNdimArray.length; i++) {
            pojo.longNdimArray[i] = vector.get(i).asLong();
        }

        vector = map.get("floatNdimArray").asVector();
        for (int i = 0; i < pojo.floatNdimArray.length; i++) {
            pojo.floatNdimArray[i] = (float) vector.get(i).asFloat();
        }

        vector = map.get("doubleNdimArray").asVector();
        pojo.doubleNdimArray = new double[vector.size()];
        for (int i = 0; i < pojo.doubleNdimArray.length; i++) {
            pojo.doubleNdimArray[i] = vector.get(i).asFloat();
        }

        final FlexBuffers.Map nestedMap = map.get("nestedData").asMap();

        if (nestedMap != null && nestedMap.size() != 0) {
            deserialiseCustom(map.get("nestedData").asMap(), pojo.nestedData, false);
        }
    }
}
