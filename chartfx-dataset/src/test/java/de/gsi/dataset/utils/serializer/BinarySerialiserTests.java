package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.utils.serializer.BinarySerialiser.FieldHeader;


/**
 *
 * @author rstein
 */
public class BinarySerialiserTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiserTests.class);


    @Test
    public void testBinarySerialiserSimple() {
        FastByteBuffer buffer = new FastByteBuffer();
        
        BinarySerialiser.put(buffer, "boolean", true);
        BinarySerialiser.put(buffer, "byte", (byte)42);
        BinarySerialiser.put(buffer, "short", (short)43);
        BinarySerialiser.put(buffer, "int", 44);
        BinarySerialiser.put(buffer, "long", (long)45);
        
        buffer.reset();
        FieldHeader header;
        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("boolean"), "byte type retrieval");
        assertTrue(buffer.getBoolean(), "byte retrieval");
        
        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("byte"), "byte type retrieval");
        assertTrue(buffer.getByte() == 42, "byte retrieval");


        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testBinarySerialiserSimple(..)");
        }
    }
}

