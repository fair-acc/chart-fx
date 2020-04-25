package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ByteBufferOutputStreamTests {
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testExpandByteBufferOnPositionIncrease(final boolean directBuffer) throws Exception {
        final ByteBuffer initialBuffer = directBuffer ? ByteBuffer.allocateDirect(16) : ByteBuffer.allocate(16);
        ByteBufferOutputStream output = new ByteBufferOutputStream(initialBuffer, true);
        output.write("hello".getBytes());
        output.position(32);
        assertEquals(32, output.position());
        assertEquals(5, initialBuffer.position());

        ByteBuffer buffer = output.buffer();
        assertEquals(32, buffer.limit());
        buffer.position(0);
        buffer.limit(5);
        byte[] bytes = new byte[5];
        buffer.get(bytes);
        assertArrayEquals("hello".getBytes(), bytes);
        output.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testExpandByteBufferOnWrite(final boolean directBuffer) throws Exception {
        final ByteBuffer initialBuffer = directBuffer ? ByteBuffer.allocateDirect(16) : ByteBuffer.allocate(16);
        ByteBufferOutputStream output = new ByteBufferOutputStream(initialBuffer, true);
        output.write("Hello".getBytes());
        output.write(new byte[27]);
        assertEquals(32, output.position());
        assertEquals(5, initialBuffer.position());

        ByteBuffer buffer = output.buffer();
        assertEquals(32, buffer.limit());
        buffer.position(0);
        buffer.limit(5);
        byte[] bytes = new byte[5];
        buffer.get(bytes);
        assertArrayEquals("Hello".getBytes(), bytes);

        buffer.rewind();
        output.close();
    }

    @Test
    public void testWriteByte() throws IOException {
        ByteBufferOutputStream output = new ByteBufferOutputStream(ByteBuffer.allocate(1), true);
        final byte[] secondMsg = "Hello World!".getBytes();
        for (int byt : secondMsg) {
            output.write(byt);
        }
        output.buffer().position(0);
        output.buffer().limit(12);

        // read result
        byte[] bytes = new byte[12];
        output.buffer().get(bytes);
        assertArrayEquals(secondMsg, bytes);
        output.buffer().rewind();

        output.write(secondMsg, 0, secondMsg.length);
        output.write(secondMsg, 0, secondMsg.length);

        output.buffer().position(0);
        output.buffer().limit(24);

        // read result
        output.buffer().get(bytes);
        assertArrayEquals(secondMsg, bytes);
        output.buffer().get(bytes);
        assertArrayEquals(secondMsg, bytes);

        output.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testWriteByteBuffer(final boolean directBuffer) throws Exception {
        final ByteBuffer initialBuffer = directBuffer ? ByteBuffer.allocateDirect(16) : ByteBuffer.allocate(16);
        long value = 234239230L;
        initialBuffer.putLong(value);
        initialBuffer.flip();

        ByteBufferOutputStream output = new ByteBufferOutputStream(ByteBuffer.allocate(32));
        output.write(initialBuffer);
        assertEquals(8, initialBuffer.position());
        assertEquals(8, output.position());
        assertEquals(value, output.buffer().getLong(0));
        output.close();
    }
}