package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;

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
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj,
                                     ioBuffer.getBooleanArray((boolean[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (boolean[]) field.getField().get(obj)), // writer
                boolean[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getByteArray((byte[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (byte[]) field.getField().get(obj)), // writer
                byte[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getShortArray((short[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (short[]) field.getField().get(obj)), // writer
                short[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getIntArray((int[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), (int[]) field.getField().get(obj)), // writer
                int[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getLongArray((long[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (long[]) field.getField().get(obj)), // writer
                long[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getFloatArray((float[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (float[]) field.getField().get(obj)), // writer
                float[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj,
                                     ioBuffer.getDoubleArray((double[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (double[]) field.getField().get(obj)), // writer
                double[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj,
                                     ioBuffer.getStringArray((String[]) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                                     (String[]) field.getField().get(obj)), // writer
                String[].class));
    }
}
