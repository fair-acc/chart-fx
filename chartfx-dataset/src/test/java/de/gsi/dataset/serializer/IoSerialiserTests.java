package de.gsi.dataset.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static de.gsi.dataset.DataSet.DIM_X;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.helper.MyGenericClass;
import de.gsi.dataset.serializer.spi.iobuffer.IoBufferSerialiser;
import de.gsi.dataset.spi.DoubleDataSet;

public class IoSerialiserTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoSerialiserTests.class);

    @Test
    public void simpleStreamerTest() {
        // check reading/writing
        final MyGenericClass inputObject = new MyGenericClass();
        MyGenericClass outputObject1 = new MyGenericClass();
        MyGenericClass.setVerboseChecks(true);

        // first test - check for equal initialisation -- this should be trivial
        assertEquals(inputObject, outputObject1);

        IoBuffer buffer = new FastByteBuffer(1000000); // TODO: check allocation of byte buffer
        IoBufferSerialiser serialiser = new IoBufferSerialiser(buffer);

        try {
            serialiser.serialiseObject(inputObject);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }

        buffer.reset();
        try {
            outputObject1 = (MyGenericClass) serialiser.deserialiseObject(outputObject1);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }

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
        LOGGER.atInfo().log("serialise modified data");
        try {
            serialiser.serialiseObject(inputObject);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }
        LOGGER.atInfo().addArgument(buffer.position()).log("serialise modified data nBytes = {}");
        LOGGER.atInfo().log("deserialise modified data");
        buffer.reset();
        try {
            outputObject2 = (MyGenericClass) serialiser.deserialiseObject(outputObject2);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }
        LOGGER.atInfo().addArgument(buffer.position()).log("deserialise modified data nBytes = {}");

        LOGGER.atInfo().log("check modified streamer");
        // third test - both vectors should have the same modified values
        assertEquals(inputObject, outputObject2);
        LOGGER.atInfo().log("simpleStreamerTest() - done");
    }

    @Test
    public void testIdentityDoubleDataSet() {
        IoBuffer buffer = new FastByteBuffer(); // TODO: check allocation of byte buffer
        IoBufferSerialiser serialiser = new IoBufferSerialiser(buffer);

        final DoubleDataSet inputObject = new DoubleDataSet("inputObject");
        DoubleDataSet outputObject = new DoubleDataSet("outputObject");
        assertNotEquals(inputObject, outputObject);

        try {
            buffer.reset();
            serialiser.serialiseObject(inputObject);
            buffer.reset();
            outputObject = (DoubleDataSet) serialiser.deserialiseObject(outputObject);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }

        assertEquals(inputObject, outputObject);
        LOGGER.atDebug().log("finished test#1 - uninitialised DataSet");

        inputObject.add(0.0, 1.0);
        inputObject.getAxisDescription(DIM_X).set("time", "s");
        try {
            buffer.reset();
            serialiser.serialiseObject(inputObject);
            buffer.reset();
            outputObject = (DoubleDataSet) serialiser.deserialiseObject(outputObject);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }
        assertEquals(inputObject, outputObject);
        LOGGER.atDebug().log("finished test#2 - initialised DataSet w/ single data point");

        inputObject.addDataLabel(0, "MyCustomDataLabel");
        inputObject.addDataStyle(0, "MyCustomDataStyle");
        inputObject.setStyle("myDataSetStyle");
        try {
            buffer.reset();
            serialiser.serialiseObject(inputObject);
            buffer.reset();
            outputObject = (DoubleDataSet) serialiser.deserialiseObject(outputObject);
        } catch (IllegalAccessException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }
        }
        assertEquals(inputObject, outputObject);
        LOGGER.atDebug().log("finished test#3 - initialised DataSet w/ single data point");

        LOGGER.atInfo().addArgument(this.getClass().getSimpleName()).log("{} - testIdentityDoubleDataSet() - completed successfully");
    }
}
