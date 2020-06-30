package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;

/**
 * helper class to register default serialiser for primitive array types (ie. boolean[], byte[], short[], ..., double[])
 * and String[]
 * 
 * @author rstein
 */
public final class FieldPrimitveValueArrayHelper {
    private FieldPrimitveValueArrayHelper() {
        // utility class
    }

    /**
     * registers default serialiser for array primitive types (ie. boolean[], byte[], short[], ..., double[]) and
     * String[]
     * 
     * @param serialiser for which the field serialisers should be registered
     * @param ioBuffer reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoSerialiser ioBuffer) {
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getBooleanArray((boolean[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (boolean[]) field.getField().get(obj)), // writer
                boolean[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getByteArray((byte[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (byte[]) field.getField().get(obj)), // writer
                byte[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getCharArray((char[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (char[]) field.getField().get(obj)), // writer
                char[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getShortArray((short[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (short[]) field.getField().get(obj)), // writer
                short[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getIntArray((int[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (int[]) field.getField().get(obj)), // writer
                int[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getLongArray((long[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (long[]) field.getField().get(obj)), // writer
                long[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getFloatArray((float[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (float[]) field.getField().get(obj)), // writer
                float[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getDoubleArray((double[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (double[]) field.getField().get(obj)), // writer
                double[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBuffer().getStringArray((String[]) field.getField().get(obj))), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), (String[]) field.getField().get(obj)), // writer
                String[].class));
    }
}
