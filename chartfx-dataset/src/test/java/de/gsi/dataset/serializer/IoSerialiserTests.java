package de.gsi.dataset.serializer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static de.gsi.dataset.DataSet.DIM_X;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.helper.SerialiserHelper;
import de.gsi.dataset.serializer.helper.TestDataClass;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ByteBuffer;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
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
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer);

        final IoClassSerialiser serialiser = new IoClassSerialiser(ioSerialiser);
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

        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer);
        final IoClassSerialiser serialiser = new IoClassSerialiser(ioSerialiser);
        assertEquals(bufferClass, buffer.getClass());
        assertEquals(bufferClass, ioSerialiser.getBuffer().getClass());

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
        final BinarySerialiser ioSerialiser = new BinarySerialiser(buffer); // TODO: generalise to IoBuffer

        final IoClassSerialiser ioClassSerialiser = new IoClassSerialiser(ioSerialiser);
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
}
