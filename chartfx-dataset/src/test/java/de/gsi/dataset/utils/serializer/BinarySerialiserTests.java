package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoSerialiser;
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
        IoSerialiser ioSerialiser = new BinarySerialiser(buffer);

        ioSerialiser.put("boolean", true);
        ioSerialiser.put("byte", (byte) 42);
        ioSerialiser.put("short", (short) 43);
        ioSerialiser.put("int", 44);
        ioSerialiser.put("long", (long) 45);

        buffer.reset();
        FieldHeader header;
        header = ioSerialiser.getFieldHeader();
        assertEquals("boolean", header.getFieldName(), "byte type retrieval");
        assertTrue(buffer.getBoolean(), "byte retrieval");

        header = ioSerialiser.getFieldHeader();
        assertEquals("byte", header.getFieldName(), "byte type retrieval");
        assertEquals((byte) 42, buffer.getByte(), "byte retrieval");

        header = ioSerialiser.getFieldHeader();
        assertEquals("short", header.getFieldName(), "byte type retrieval");
        assertEquals((short) 43, buffer.getShort(), "byte retrieval");

        header = ioSerialiser.getFieldHeader();
        assertEquals("int", header.getFieldName(), "byte type retrieval");
        assertEquals(44, buffer.getInt(), "byte retrieval");

        header = ioSerialiser.getFieldHeader();
        assertEquals("long", header.getFieldName(), "byte type retrieval");
        assertEquals(45L, buffer.getLong(), "byte retrieval");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testBinarySerialiserSimple(..)");
        }
    }
}
