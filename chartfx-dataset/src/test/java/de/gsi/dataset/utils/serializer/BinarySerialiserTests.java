package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;

/**
 * @author rstein
 */
public class BinarySerialiserTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySerialiserTests.class);

    @Test
    public void testBinarySerialiserSimple() {
        FastByteBuffer buffer = new FastByteBuffer();
        IoSerialiser ioSerialiser = new BinarySerialiser(buffer);

        ioSerialiser.putFieldHeader("boolean", DataType.BOOL);
        ioSerialiser.put(true);
        ioSerialiser.putFieldHeader("byte", DataType.BYTE);
        ioSerialiser.put((byte) 42);
        ioSerialiser.putFieldHeader("short", DataType.SHORT);
        ioSerialiser.put((short) 43);
        ioSerialiser.putFieldHeader("int", DataType.INT);
        ioSerialiser.put(44);
        ioSerialiser.putFieldHeader("long", DataType.LONG);
        ioSerialiser.put((long) 45);

        buffer.reset();
        WireDataFieldDescription header;
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
