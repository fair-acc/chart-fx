package de.gsi.dataset.serializer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static de.gsi.dataset.DataSet.DIM_X;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.helper.CmwLightHelper;
import de.gsi.dataset.serializer.helper.JsonHelper;
import de.gsi.dataset.serializer.helper.SerialiserHelper;
import de.gsi.dataset.serializer.helper.TestDataClass;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ByteBuffer;
import de.gsi.dataset.serializer.spi.CmwLightSerialiser;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.JsonSerialiser;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;
import de.gsi.dataset.serializer.spi.helper.MyGenericClass;
import de.gsi.dataset.spi.DoubleDataSet;

class IoSerialiserTests {
    private static final int BUFFER_SIZE = 50000;

    @ParameterizedTest
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void simpleStreamerTest(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(1000000);

        // check reading/writing
        final MyGenericClass inputObject = new MyGenericClass();
        MyGenericClass outputObject1 = new MyGenericClass();
        MyGenericClass.setVerboseChecks(true);

        // first test - check for equal initialisation -- this should be trivial
        assertEquals(inputObject, outputObject1);

        //final IoBuffer buffer = new FastByteBuffer(1000000);
        final IoClassSerialiser serialiser = new IoClassSerialiser(buffer, BinarySerialiser.class);
        serialiser.serialiseObject(inputObject);

        buffer.reset();
        outputObject1 = (MyGenericClass) serialiser.deserialiseObject(outputObject1);

        // second test - both vectors should have the same initial values
        // after serialise/deserialise
        assertEquals(inputObject, outputObject1);

        MyGenericClass outputObject2 = new MyGenericClass();
        buffer.reset();
        buffer.clear();
        // modify input object w.r.t. init values
        inputObject.modifyValues();
        inputObject.boxedPrimitives.modifyValues();
        inputObject.arrays.modifyValues();
        inputObject.objArrays.modifyValues();

        serialiser.serialiseObject(inputObject);

        buffer.reset();
        outputObject2 = (MyGenericClass) serialiser.deserialiseObject(outputObject2);

        // third test - both vectors should have the same modified values
        assertEquals(inputObject, outputObject2);
    }

    @DisplayName("basic custom serialisation/deserialisation identity")
    @ParameterizedTest(name = "IoBuffer class - {0} recursion level {1}")
    @ArgumentsSource(IoBufferHierarchyArgumentProvider.class)
    void testCustomSerialiserIdentity(final Class<? extends IoBuffer> bufferClass, final int hierarchyLevel) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        final TestDataClass inputObject = new TestDataClass(10, 100, hierarchyLevel);
        final TestDataClass outputObject = new TestDataClass(-1, -1, 0);

        buffer.reset();
        SerialiserHelper.serialiseCustom(ioSerialiser, inputObject);

        buffer.reset();
        SerialiserHelper.deserialiseCustom(ioSerialiser, outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    @ParameterizedTest
    @DisplayName("basic DataSet serialisation/deserialisation identity")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void testCustomDataSetSerialiserIdentity(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        final IoClassSerialiser serialiser = new IoClassSerialiser(buffer, BinarySerialiser.class);
        assertEquals(bufferClass, buffer.getClass());
        assertEquals(bufferClass, serialiser.getDataBuffer().getClass());

        final DoubleDataSet inputObject = new DoubleDataSet("inputObject");
        DataSet outputObject = new DoubleDataSet("outputObject");
        assertNotEquals(inputObject, outputObject);

        buffer.reset();
        serialiser.serialiseObject(inputObject);
        buffer.reset();
        outputObject = (DataSet) serialiser.deserialiseObject(outputObject);

        assertEquals(inputObject, outputObject);

        inputObject.add(0.0, 1.0);
        inputObject.getAxisDescription(DIM_X).set("time", "s");

        buffer.reset();
        serialiser.serialiseObject(inputObject);
        buffer.reset();
        outputObject = (DataSet) serialiser.deserialiseObject(outputObject);

        assertEquals(inputObject, outputObject);

        inputObject.addDataLabel(0, "MyCustomDataLabel");
        inputObject.addDataStyle(0, "MyCustomDataStyle");
        inputObject.setStyle("myDataSetStyle");

        buffer.reset();
        serialiser.serialiseObject(inputObject);
        buffer.reset();
        outputObject = (DoubleDataSet) serialiser.deserialiseObject(outputObject);

        assertEquals(inputObject, outputObject);
    }

    @DisplayName("basic POJO serialisation/deserialisation identity")
    @ParameterizedTest(name = "IoBuffer class - {0} recursion level {1}")
    @ArgumentsSource(IoBufferHierarchyArgumentProvider.class)
    void testIoBufferSerialiserIdentity(final Class<? extends IoBuffer> bufferClass, final int hierarchyLevel) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        final IoClassSerialiser ioClassSerialiser = new IoClassSerialiser(buffer, BinarySerialiser.class);
        final TestDataClass inputObject = new TestDataClass(10, 100, hierarchyLevel);
        final TestDataClass outputObject = new TestDataClass(-1, -1, 0);

        buffer.reset();
        ioClassSerialiser.serialiseObject(inputObject);

        buffer.reset();
        ioClassSerialiser.deserialiseObject(outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    @DisplayName("basic POJO IoSerialiser identity - scan")
    @ParameterizedTest(name = "IoSerialiser {0} - IoBuffer class {1} - recursion level {2}")
    @ArgumentsSource(IoSerialiserHierarchyArgumentProvider.class)
    void testParsingInterface(final Class<? extends IoSerialiser> ioSerialiserClass, final Class<? extends IoBuffer> bufferClass, final int hierarchyLevel) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        assertNotNull(ioSerialiserClass, "ioSerialiserClass being not null");
        assertNotNull(ioSerialiserClass.getConstructor(IoBuffer.class), "Constructor(IoBuffer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        final IoClassSerialiser ioClassSerialiser = new IoClassSerialiser(buffer, ioSerialiserClass);
        assertEquals(ioClassSerialiser.getMatchedIoSerialiser().getClass(), ioSerialiserClass, "matching class type");

        final TestDataClass inputObject = new TestDataClass(10, 100, hierarchyLevel);
        final TestDataClass outputObject = new TestDataClass(-1, -1, 0);

        buffer.reset();
        ioClassSerialiser.serialiseObject(inputObject);
        //        if (ioSerialiserClass.equals(JsonSerialiser.class)) {
        //            System.err.println("json output:=");
        //            final int pos = buffer.position();
        //            System.err.println("data = " + new String(buffer.elements(), 0, pos));
        //        }

        buffer.reset();
        ioClassSerialiser.deserialiseObject(outputObject);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");

        buffer.reset();
        final WireDataFieldDescription rootField = ioClassSerialiser.parseWireFormat();
        //rootField.printFieldStructure();

        assertEquals("ROOT", rootField.getFieldName());
        final WireDataFieldDescription classFields = (WireDataFieldDescription) (rootField.getChildren().get(0));
        for (FieldDescription field : classFields.getChildren()) {
            final WireDataFieldDescription wireField = (WireDataFieldDescription) field;
            final DataType dataType = wireField.getDataType();
            if ((dataType.isScalar() || dataType.isArray()) && dataType != DataType.START_MARKER && dataType != DataType.END_MARKER) {
                final Object data = wireField.data();
                assertNotNull(data, "field non null for: " + wireField.getFieldName());
                assertEquals(dataType, DataType.fromClassType(data.getClass()), "field object type match for: " + wireField.getFieldName());
            }
        }

        final WireDataFieldDescription boolField = (WireDataFieldDescription) (classFields.findChildField("bool1"));
        assertNotNull(boolField);
        assertEquals(inputObject.bool1, boolField.data(), "bool1 data field content");
        assertEquals(inputObject.bool2, ((WireDataFieldDescription) (classFields.findChildField("bool2"))).data(), "bool2 data field content");
        assertEquals(inputObject.byte1, ((WireDataFieldDescription) (classFields.findChildField("byte1"))).data(DataType.BYTE), "byte1 data field content");
        assertEquals(inputObject.byte2, ((WireDataFieldDescription) (classFields.findChildField("byte2"))).data(DataType.BYTE), "byte2 data field content");

        if (!ioSerialiserClass.equals(JsonSerialiser.class)) {
            assertArrayEquals(inputObject.boolArray, (boolean[]) ((WireDataFieldDescription) (classFields.findChildField("boolArray"))).data(), "intArray data field content");
            assertArrayEquals(inputObject.byteArray, (byte[]) ((WireDataFieldDescription) (classFields.findChildField("byteArray"))).data(), "byteArray data field content");
            assertArrayEquals(inputObject.shortArray, (short[]) ((WireDataFieldDescription) (classFields.findChildField("shortArray"))).data(), "shortArray data field content");
            assertArrayEquals(inputObject.intArray, (int[]) ((WireDataFieldDescription) (classFields.findChildField("intArray"))).data(), "intArray data field content");
            assertArrayEquals(inputObject.longArray, (long[]) ((WireDataFieldDescription) (classFields.findChildField("longArray"))).data(), "longArray data field content");
            assertArrayEquals(inputObject.floatArray, (float[]) ((WireDataFieldDescription) (classFields.findChildField("floatArray"))).data(), "floatArray data field content");
            assertArrayEquals(inputObject.doubleArray, (double[]) ((WireDataFieldDescription) (classFields.findChildField("doubleArray"))).data(), "doubleArray data field content");
            assertArrayEquals(inputObject.stringArray, (String[]) ((WireDataFieldDescription) (classFields.findChildField("stringArray"))).data(), "stringArray data field content");
        }

        assertArrayEquals(inputObject.boolArray, (boolean[]) ((WireDataFieldDescription) (classFields.findChildField("boolArray"))).data(DataType.BOOL_ARRAY), "intArray data field content");
        assertArrayEquals(inputObject.byteArray, (byte[]) ((WireDataFieldDescription) (classFields.findChildField("byteArray"))).data(DataType.BYTE_ARRAY), "byteArray data field content");
        assertArrayEquals(inputObject.shortArray, (short[]) ((WireDataFieldDescription) (classFields.findChildField("shortArray"))).data(DataType.SHORT_ARRAY), "shortArray data field content");
        assertArrayEquals(inputObject.intArray, (int[]) ((WireDataFieldDescription) (classFields.findChildField("intArray"))).data(DataType.INT_ARRAY), "intArray data field content");
        assertArrayEquals(inputObject.longArray, (long[]) ((WireDataFieldDescription) (classFields.findChildField("longArray"))).data(DataType.LONG_ARRAY), "longArray data field content");
        assertArrayEquals(inputObject.floatArray, (float[]) ((WireDataFieldDescription) (classFields.findChildField("floatArray"))).data(DataType.FLOAT_ARRAY), "floatArray data field content");
        assertArrayEquals(inputObject.doubleArray, (double[]) ((WireDataFieldDescription) (classFields.findChildField("doubleArray"))).data(DataType.DOUBLE_ARRAY), "doubleArray data field content");
        assertArrayEquals(inputObject.stringArray, (String[]) ((WireDataFieldDescription) (classFields.findChildField("stringArray"))).data(DataType.STRING_ARRAY), "stringArray data field content");
    }

    @DisplayName("benchmark identity tests")
    @Test
    void benchmarkIdentityTests() {
        final TestDataClass inputObject = new TestDataClass(10, 100, 1);
        final TestDataClass outputObject = new TestDataClass(-1, -1, 0);

        // execute thrice to ensure that buffer flipping/state is cleaned-up properly
        for (int i = 0; i < 2; i++) {
            assertDoesNotThrow(() -> {});
            // assertDoesNotThrow(() -> CmwHelper.checkSerialiserIdentity(inputObject, outputObject));
            assertDoesNotThrow(() -> CmwLightHelper.checkSerialiserIdentity(inputObject, outputObject));
            assertDoesNotThrow(() -> CmwLightHelper.checkCustomSerialiserIdentity(inputObject, outputObject));
            assertDoesNotThrow(() -> JsonHelper.checkSerialiserIdentity(inputObject, outputObject));
            assertDoesNotThrow(() -> JsonHelper.checkCustomSerialiserIdentity(inputObject, outputObject));

            assertDoesNotThrow(() -> SerialiserHelper.checkSerialiserIdentity(inputObject, outputObject));
            assertDoesNotThrow(() -> SerialiserHelper.checkCustomSerialiserIdentity(inputObject, outputObject));
            // assertDoesNotThrow(() -> FlatBuffersHelper.checkCustomSerialiserIdentity(inputObject, outputObject));
        }
    }

    @DisplayName("benchmark performance tests")
    @Test
    void benchmarkPerformanceTests() {
        final TestDataClass inputObject = new TestDataClass(10, 100, 1);
        final TestDataClass outputObject = new TestDataClass(-1, -1, 0);
        final int nIterations = 1;
        // execute thrice to ensure that buffer flipping/state is cleaned-up properly
        for (int i = 0; i < 2; i++) {
            assertDoesNotThrow(() -> {});

            // map-only performance
            assertDoesNotThrow(() -> JsonHelper.testSerialiserPerformanceMap(nIterations, inputObject));
            // assertDoesNotThrow(() -> CmwHelper.testSerialiserPerformanceMap(nIterations, inputObject, outputObject));
            assertDoesNotThrow(() -> CmwLightHelper.testSerialiserPerformanceMap(nIterations, inputObject));
            assertDoesNotThrow(() -> SerialiserHelper.testSerialiserPerformanceMap(nIterations, inputObject));

            // custom serialiser performance
            assertDoesNotThrow(() -> JsonHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject));
            // assertDoesNotThrow(() -> FlatBuffersHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject));
            assertDoesNotThrow(() -> CmwLightHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject));
            assertDoesNotThrow(() -> SerialiserHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject));

            // POJO performance
            assertDoesNotThrow(() -> JsonHelper.testPerformancePojo(nIterations, inputObject, outputObject));
            assertDoesNotThrow(() -> JsonHelper.testPerformancePojoCodeGen(nIterations, inputObject, outputObject));
            // assertDoesNotThrow(() -> CmwHelper.testPerformancePojo(nIterations, inputObject, outputObject));
            assertDoesNotThrow(() -> CmwLightHelper.testPerformancePojo(nIterations, inputObject, outputObject));
            assertDoesNotThrow(() -> SerialiserHelper.testPerformancePojo(nIterations, inputObject, outputObject));
        }
    }

    private static class IoBufferHierarchyArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(ByteBuffer.class, 0),
                    Arguments.of(ByteBuffer.class, 1),
                    Arguments.of(ByteBuffer.class, 2),
                    Arguments.of(ByteBuffer.class, 3),
                    Arguments.of(ByteBuffer.class, 4),
                    Arguments.of(ByteBuffer.class, 5),
                    Arguments.of(FastByteBuffer.class, 0),
                    Arguments.of(FastByteBuffer.class, 1),
                    Arguments.of(FastByteBuffer.class, 2),
                    Arguments.of(FastByteBuffer.class, 3),
                    Arguments.of(FastByteBuffer.class, 4),
                    Arguments.of(FastByteBuffer.class, 5));
        }
    }

    private static class IoSerialiserHierarchyArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(BinarySerialiser.class, ByteBuffer.class, 0),
                    Arguments.of(BinarySerialiser.class, ByteBuffer.class, 1),
                    Arguments.of(BinarySerialiser.class, FastByteBuffer.class, 0),
                    Arguments.of(BinarySerialiser.class, FastByteBuffer.class, 1),

                    Arguments.of(CmwLightSerialiser.class, ByteBuffer.class, 0),
                    Arguments.of(CmwLightSerialiser.class, ByteBuffer.class, 1),
                    Arguments.of(CmwLightSerialiser.class, FastByteBuffer.class, 0),
                    Arguments.of(CmwLightSerialiser.class, FastByteBuffer.class, 1),

                    Arguments.of(JsonSerialiser.class, ByteBuffer.class, 0),
                    Arguments.of(JsonSerialiser.class, ByteBuffer.class, 1),
                    Arguments.of(JsonSerialiser.class, FastByteBuffer.class, 0),
                    Arguments.of(JsonSerialiser.class, FastByteBuffer.class, 1));
        }
    }
}
