package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;

/**
 * helper class to register default serialiser for boxed primitive types (ie. Boolean, Byte, Short, ..., double) w/o
 * String (already part of the {@link de.gsi.dataset.serializer.spi.iobuffer.FieldPrimitiveValueHelper}
 * 
 * @author rstein
 */
public final class FieldBoxedValueHelper {
    private FieldBoxedValueHelper() {
        // utility class
    }

    /**
     * registers default serialiser for primitive array types (ie. boolean[], byte[], short[], ..., double[]) and
     * String[]
     * 
     * @param serialiser for which the field serialisers should be registered
     * @param ioBuffer reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoSerialiser ioBuffer) {
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBoolean()), // reader
                (obj, field) -> ioBuffer.put((Boolean) field.getField().get(obj)), // writer
                Boolean.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getByte()), // reader
                (obj, field) -> ioBuffer.put((Byte) field.getField().get(obj)), // writer
                Byte.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getShort()), // reader
                (obj, field) -> ioBuffer.put((Short) field.getField().get(obj)), // writer
                Short.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getInteger()), // reader
                (obj, field) -> ioBuffer.put((Integer) field.getField().get(obj)), // writer
                Integer.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getLong()), // reader
                (obj, field) -> ioBuffer.put((Long) field.getField().get(obj)), // writer
                Long.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getFloat()), // reader
                (obj, field) -> ioBuffer.put((Float) field.getField().get(obj)), // writer
                Float.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getDouble()), // reader
                (obj, field) -> ioBuffer.put((Double) field.getField().get(obj)), // writer
                Double.class));

        //        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
        //                (obj, field) -> field.getField().set(obj, ioBuffer.getString()), // reader
        //                (obj, field) -> BinarySerialiser.put((String) field.getField().get(obj)), // writer
        //                String.class));
    }
}
