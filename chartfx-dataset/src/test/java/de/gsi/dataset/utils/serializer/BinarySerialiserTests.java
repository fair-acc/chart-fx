package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.FieldHeader;

/**
 * @author rstein
 */
public class BinarySerialiserTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiserTests.class);

    @Test
    public void testBinarySerialiserSimple() {
        FastByteBuffer buffer = new FastByteBuffer();

        BinarySerialiser.put(buffer, "boolean", true);
        BinarySerialiser.put(buffer, "byte", (byte) 42);
        BinarySerialiser.put(buffer, "short", (short) 43);
        BinarySerialiser.put(buffer, "int", 44);
        BinarySerialiser.put(buffer, "long", (long) 45);

        buffer.reset();
        FieldHeader header;
        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("boolean"), "byte type retrieval");
        assertTrue(buffer.getBoolean(), "byte retrieval");

        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("byte"), "byte type retrieval");
        assertEquals((byte) 42, buffer.getByte(), "byte retrieval");

        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("short"), "byte type retrieval");
        assertEquals((short) 43, buffer.getShort(), "byte retrieval");

        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("int"), "byte type retrieval");
        assertEquals(44, buffer.getInt(), "byte retrieval");

        header = BinarySerialiser.getFieldHeader(buffer);
        assertTrue(header.getFieldName().equals("long"), "byte type retrieval");
        assertEquals(45l, buffer.getLong(), "byte retrieval");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testBinarySerialiserSimple(..)");
        }
    }
}
