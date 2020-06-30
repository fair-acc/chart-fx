package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;

/**
 * helper class to register default serialiser for primitive types (ie. boolean, byte, short, ..., double) and String
 * 
 * @author rstein
 */
public final class FieldPrimitiveValueHelper {
    private FieldPrimitiveValueHelper() {
        // utility class
    }

    /**
     * registers default serialiser for primitive types (ie. boolean, byte, short, ..., double) and String
     * 
     * @param serialiser for which the field serialisers should be registered
     * @param ioBuffer reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoSerialiser ioBuffer) {
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setBoolean(obj, ioBuffer.getBoolean()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getBoolean(obj)), // writer
                boolean.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setByte(obj, ioBuffer.getByte()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getByte(obj)), // writer
                byte.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setChar(obj, ioBuffer.getCharacter()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getChar(obj)), // writer
                char.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setShort(obj, ioBuffer.getShort()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getShort(obj)), // writer
                short.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setInt(obj, ioBuffer.getInteger()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getInt(obj)), // writer
                int.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setLong(obj, ioBuffer.getLong()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getLong(obj)), // writer
                long.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setFloat(obj, ioBuffer.getFloat()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getFloat(obj)), // writer
                float.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().setDouble(obj, ioBuffer.getDouble()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(), field.getField().getDouble(obj)), // writer
                double.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getString()), // reader
                (obj, field) -> ioBuffer.put(field.getFieldName(),
                                     (String) field.getField().get(obj)), // writer
                String.class));
    }
}
