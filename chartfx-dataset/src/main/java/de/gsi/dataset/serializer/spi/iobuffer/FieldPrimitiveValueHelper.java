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
     * @param ioSerialiser reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoSerialiser ioSerialiser) {
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setBoolean(obj, ioSerialiser.getBoolean()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getBoolean(obj)), // writer
                boolean.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setByte(obj, ioSerialiser.getByte()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getByte(obj)), // writer
                byte.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setChar(obj, ioSerialiser.getCharacter()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getChar(obj)), // writer
                char.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setShort(obj, ioSerialiser.getShort()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getShort(obj)), // writer
                short.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setInt(obj, ioSerialiser.getInteger()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getInt(obj)), // writer
                int.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setLong(obj, ioSerialiser.getLong()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getLong(obj)), // writer
                long.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setFloat(obj, ioSerialiser.getFloat()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getFloat(obj)), // writer
                float.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().setDouble(obj, ioSerialiser.getDouble()), // reader
                (obj, field) -> ioSerialiser.put(field.getField().getDouble(obj)), // writer
                double.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioSerialiser, //
                (obj, field) -> field.getField().set(obj, ioSerialiser.getString()), // reader
                (obj, field) -> ioSerialiser.put((String) field.getField().get(obj)), // writer
                String.class));
    }
}
