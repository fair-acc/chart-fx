package de.gsi.dataset.serializer.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.IoClassSerialiser;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.benchmark.SerialiserBenchmark;
import de.gsi.dataset.serializer.spi.CmwLightSerialiser;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.ProtocolInfo;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;

public class CmwLightHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialiserBenchmark.class); // N.B. SerialiserBenchmark reference on purpose
    private static final IoBuffer byteBuffer = new FastByteBuffer(100000);
    // private static final IoBuffer byteBuffer = new ByteBuffer(20000);
    private static final CmwLightSerialiser cmwLightSerialiser = new CmwLightSerialiser(byteBuffer);
    private static final IoClassSerialiser ioSerialiser = new IoClassSerialiser(cmwLightSerialiser);
    private static int nEntries = -1;
    /*
    public static void checkCmwLightVsCmwIdentityBackward(final TestDataClass inputObject, TestDataClass outputObject) {
        final DataSerializer cmwSerializer = DataFactory.createDataSerializer();
        TestDataClass.setCmwCompatibilityMode(true);

        outputObject.clear();
        byteBuffer.reset();
        CmwLightHelper.serialiseCustom(cmwLightSerialiser, inputObject);
        final int nBytesCmwLight = byteBuffer.position();
        LOGGER.atInfo().addArgument(nBytesCmwLight).log("backward compatibility check: CmwLight serialiser nBytes = {}");

        // keep: checks serialised data structure
        // wrapCmwBuffer.reset();
        // final WireDataFieldDescription fieldRoot = CmwLightHelper.deserialiseMap(cmwLightSerialiser);
        // fieldRoot.printFieldStructure();

        // N.B. cannot use custom deserialiser since entry order seems to be arbitrary in CMW Data object
        byteBuffer.reset();
        final Data retrievedData = cmwSerializer.deserializeFromBinary(((FastByteBuffer) byteBuffer).elements());
        CmwHelper.applyCmwData(retrievedData, outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);
        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");

        TestDataClass.setCmwCompatibilityMode(false);
        cmwLightSerialiser.setBuffer(byteBuffer);
    }

    public static void checkCmwLightVsCmwIdentityForward(final TestDataClass inputObject, TestDataClass outputObject) {
        final DataSerializer cmwSerializer = DataFactory.createDataSerializer();
        TestDataClass.setCmwCompatibilityMode(true);

        outputObject.clear();
        byteBuffer.reset();
        CmwLightHelper.serialiseCustom(cmwLightSerialiser, inputObject);
        final int nBytesCmwLight = byteBuffer.position();

        final Data cmwData = CmwHelper.getCmwData(inputObject);
        final byte[] cmwBuffer = cmwSerializer.serializeToBinary(cmwData);
        FastByteBuffer wrapCmwBuffer = FastByteBuffer.wrap(cmwBuffer);
        LOGGER.atInfo().addArgument(cmwBuffer.length).addArgument(nBytesCmwLight).log("forward compatibility check: CMW nBytes = {} vs. CmwLight serialiser nBytes = {}");
        if (cmwBuffer.length != nBytesCmwLight) {
            throw new IllegalStateException("CMW byte buffer length = " + cmwBuffer.length + " vs. CmwLight byte buffer length = " + nBytesCmwLight);
        }

        wrapCmwBuffer.reset();
        cmwLightSerialiser.setBuffer(wrapCmwBuffer);
        final Data retrievedData = cmwSerializer.deserializeFromBinary(cmwBuffer);
        CmwHelper.applyCmwData(retrievedData, outputObject);

        // keep: checks serialised data structure
        // wrapCmwBuffer.reset();
        // final WireDataFieldDescription fieldRoot = CmwLightHelper.deserialiseMap(cmwLightSerialiser);
        // fieldRoot.printFieldStructure();

        // N.B. cannot use custom deserialiser since entry order seems to be arbitrary in CMW Data object
        wrapCmwBuffer.reset();
        outputObject = (TestDataClass) ioSerialiser.deserialiseObject(outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);
        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");

        TestDataClass.setCmwCompatibilityMode(false);
        cmwLightSerialiser.setBuffer(byteBuffer);
    }
*/
    public static void checkCustomSerialiserIdentity(final TestDataClass inputObject, TestDataClass outputObject) {
        outputObject.clear();
        byteBuffer.reset();
        CmwLightHelper.serialiseCustom(cmwLightSerialiser, inputObject);
        final int nBytesCmwLight = byteBuffer.position();
        LOGGER.atInfo().addArgument(nBytesCmwLight).log("CmwLight serialiser nBytes = {}");

        // keep: checks serialised data structure
        // byteBuffer.reset();
        // final WireDataFieldDescription fieldRoot = CmwLightHelper.deserialiseMap(cmwLightSerialiser);
        // fieldRoot.printFieldStructure();

        byteBuffer.reset();
        CmwLightHelper.deserialiseCustom(cmwLightSerialiser, outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    public static void checkSerialiserIdentity(final TestDataClass inputObject, TestDataClass outputObject) {
        outputObject.clear();
        byteBuffer.reset();

        ioSerialiser.serialiseObject(inputObject);

        // CmwLightHelper.serialiseCustom(cmwLightSerialiser, inputObject);
        final int nBytes = byteBuffer.position();
        LOGGER.atInfo().addArgument(nBytes).log("CmwLight serialiser nBytes = {}");

        // keep: checks serialised data structure
        // byteBuffer.reset();
        // final WireDataFieldDescription fieldRoot = CmwLightHelper.deserialiseMap(cmwLightSerialiser);
        // fieldRoot.printFieldStructure();

        byteBuffer.reset();
        outputObject = (TestDataClass) ioSerialiser.deserialiseObject(outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    public static void deserialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo) {
        deserialiseCustom(ioSerialiser, pojo, true);
    }

    public static void deserialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo, boolean header) {
        if (header) {
            final ProtocolInfo headerField = ioSerialiser.checkHeaderInfo();
            byteBuffer.position(headerField.getDataStartPosition());
        }
        // read 'nEntries' chunk
        nEntries = byteBuffer.getInt();
        if (nEntries <= 0) {
            throw new IllegalStateException("nEntries = " + nEntries + " <= 0!");
        }
        getFieldHeader(ioSerialiser);
        pojo.bool1 = ioSerialiser.getBuffer().getBoolean();
        getFieldHeader(ioSerialiser);
        pojo.bool2 = ioSerialiser.getBuffer().getBoolean();

        getFieldHeader(ioSerialiser);
        pojo.byte1 = ioSerialiser.getBuffer().getByte();
        getFieldHeader(ioSerialiser);
        pojo.byte2 = ioSerialiser.getBuffer().getByte();

        if (!TestDataClass.isCmwCompatibilityMode()) { // disabled since reference CMW impl does not support char
            getFieldHeader(ioSerialiser);
            pojo.char1 = ioSerialiser.getBuffer().getChar();
            getFieldHeader(ioSerialiser);
            pojo.char2 = ioSerialiser.getBuffer().getChar();
        } else {
            // CMW compatibility mode
            getFieldHeader(ioSerialiser);
            pojo.char1 = (char) ioSerialiser.getBuffer().getShort();
            getFieldHeader(ioSerialiser);
            pojo.char2 = (char) ioSerialiser.getBuffer().getShort();
        }

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
        if (!TestDataClass.isCmwCompatibilityMode()) { // disabled since reference CMW impl does not support UTF-8
            getFieldHeader(ioSerialiser);
            pojo.string2 = ioSerialiser.getBuffer().getString();
        }

        // 1-dim arrays
        getFieldHeader(ioSerialiser);
        pojo.boolArray = ioSerialiser.getBuffer().getBooleanArray();
        getFieldHeader(ioSerialiser);
        pojo.byteArray = ioSerialiser.getBuffer().getByteArray();
        // getFieldHeader(ioSerialiser);
        // pojo.charArray = ioSerialiser.getBuffer().getCharArray(ioSerialiser);
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

        if (!TestDataClass.isCmwCompatibilityMode()) { // disabled since reference temporarily since CmwLight does not distinguish betwenn 1D, 2D, or nDim arrays (CMW has the same wire-format but different DataType ids for it)
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
        }

        final WireDataFieldDescription field = getFieldHeader(ioSerialiser);
        if (field == null) {
            // reached the end
            return;
        }

        if (field.getDataType().equals(de.gsi.dataset.serializer.DataType.START_MARKER)) {
            if (pojo.nestedData == null) {
                pojo.nestedData = new TestDataClass();
            }
            deserialiseCustom(ioSerialiser, pojo.nestedData, false);
        }
    }

    public static WireDataFieldDescription deserialiseMap(IoSerialiser ioSerialiser) {
        return ioSerialiser.parseIoStream(true);
    }

    public static CmwLightSerialiser getCmwLightSerialiser() {
        return cmwLightSerialiser;
    }

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
        if (!TestDataClass.isCmwCompatibilityMode()) { // disabled since reference CMW impl does not support char
            ioSerialiser.putFieldHeader("char1", DataType.CHAR);
            ioSerialiser.getBuffer().putChar(pojo.char1);
            ioSerialiser.putFieldHeader("char2", DataType.CHAR);
            ioSerialiser.getBuffer().putChar(pojo.char2);
        } else {
            // CMW compatibility mode
            ioSerialiser.putFieldHeader("char1", DataType.SHORT);
            ioSerialiser.getBuffer().putShort((short) pojo.char1);
            ioSerialiser.putFieldHeader("char2", DataType.SHORT);
            ioSerialiser.getBuffer().putShort((short) pojo.char2);
        }
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
        if (!TestDataClass.isCmwCompatibilityMode()) { // disabled since reference CMW impl does not support UTF-8
            ioSerialiser.putFieldHeader("string2", DataType.STRING);
            ioSerialiser.getBuffer().putString(pojo.string2);
        }

        // 1D-arrays
        ioSerialiser.putFieldHeader("boolArray", DataType.BOOL_ARRAY);
        ioSerialiser.getBuffer().putBooleanArray(pojo.boolArray, 0, pojo.boolArray.length);
        ioSerialiser.putFieldHeader("byteArray", DataType.BYTE_ARRAY);
        ioSerialiser.getBuffer().putByteArray(pojo.byteArray, 0, pojo.byteArray.length);
        //ioSerialiser.putFieldHeader("charArray", DataType.CHAR_ARRAY); // not supported by CMW
        //ioSerialiser.getBuffer().putCharArray(pojo.charArray, 0, pojo.charArray.length); // not supported by CMW
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

        if (!TestDataClass.isCmwCompatibilityMode()) { // disabled since reference temporarily since CmwLight does not distinguish betwenn 1D, 2D, or nDim arrays (CMW has the same wire-format but different DataType ids for it)
            // multi-dim case
            ioSerialiser.putFieldHeader("nDimensions", DataType.INT_ARRAY);
            ioSerialiser.getBuffer().putIntArray(pojo.nDimensions, 0, pojo.nDimensions.length);
            ioSerialiser.putFieldHeader("boolNdimArray", DataType.BOOL_ARRAY);
            ioSerialiser.getBuffer().putBooleanArray(pojo.boolNdimArray, 0, pojo.nDimensions);
            ioSerialiser.putFieldHeader("byteNdimArray", DataType.BYTE_ARRAY);
            ioSerialiser.getBuffer().putByteArray(pojo.byteNdimArray, 0, pojo.nDimensions);
            //ioSerialiser.putFieldHeader("charNdimArray", DataType.CHAR_ARRAY); // not supported by CMW
            //ioSerialiser.put(pojo.charNdimArray, pojo.nDimensions); // not supported by CMW
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
        }

        if (pojo.nestedData != null) {
            ioSerialiser.putStartMarker("nestedData");
            serialiseCustom(ioSerialiser, pojo.nestedData, false);
            ioSerialiser.putEndMarker("nestedData");
        }

        if (header) {
            ioSerialiser.putEndMarker("OBJ_ROOT_END");
        }
    }

    public static void testCustomSerialiserPerformance(final int iterations, final TestDataClass inputObject, final TestDataClass outputObject) {
        final long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset();
            CmwLightHelper.serialiseCustom(cmwLightSerialiser, inputObject);

            byteBuffer.reset();
            CmwLightHelper.deserialiseCustom(cmwLightSerialiser, outputObject);

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }
        if (iterations <= 1) {
            // JMH use-case
            return;
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserBenchmark.humanReadableByteCount(byteBuffer.position(), true)) //
                .addArgument(diffMillis) //
                .log("CmwLight Serializer (custom) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static void testPerformancePojo(final int iterations, final TestDataClass inputObject, TestDataClass outputObject) {
        cmwLightSerialiser.setPutFieldMetaData(true);
        final long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            if (i == 1) {
                // only stream meta-data the first iteration
                cmwLightSerialiser.setPutFieldMetaData(false);
            }
            byteBuffer.reset();
            ioSerialiser.serialiseObject(inputObject);

            byteBuffer.reset();

            outputObject = (TestDataClass) ioSerialiser.deserialiseObject(outputObject);

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }
        if (iterations <= 1) {
            // JMH use-case
            return;
        }
        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserBenchmark.humanReadableByteCount(byteBuffer.position(), true)) //
                .addArgument(diffMillis) //
                .log("CmwLight Serializer (POJO) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static WireDataFieldDescription testSerialiserPerformanceMap(final int iterations, final TestDataClass inputObject) {
        final long startTime = System.nanoTime();

        WireDataFieldDescription ret = null;
        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset();
            CmwLightHelper.serialiseCustom(cmwLightSerialiser, inputObject);
            byteBuffer.reset();
            ret = CmwLightHelper.deserialiseMap(cmwLightSerialiser);

            if (ret.getDataSize() == 0) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }
        if (iterations <= 1) {
            // JMH use-case
            return ret;
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserBenchmark.humanReadableByteCount(byteBuffer.position(), true)) //
                .addArgument(diffMillis) //
                .log("CmwLight Serializer (Map only)  throughput = {}/s for {} per test run (took {} ms)");
        return ret;
    }

    private static WireDataFieldDescription getFieldHeader(IoSerialiser ioSerialiser) {
        if (nEntries == 0) {
            return null;
        } else if (nEntries <= 0) {
            throw new IllegalStateException("nEntries = " + nEntries + " <= 0!");
        }
        WireDataFieldDescription field = ioSerialiser.getFieldHeader();
        ioSerialiser.getBuffer().position(field.getDataStartPosition());
        nEntries--;
        return field;
    }
}
