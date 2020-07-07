package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoClassSerialiser;

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
     */
    public static void register(final IoClassSerialiser serialiser) {
        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setBoolean(obj, io.getBuffer().getBoolean()), // reader
                (io, obj, field) -> (Boolean) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putBoolean(field.getField().getBoolean(obj)), // writer
                boolean.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setByte(obj, io.getBuffer().getByte()), // reader
                (io, obj, field) -> (Byte) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putByte(field.getField().getByte(obj)), // writer
                byte.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setChar(obj, io.getBuffer().getChar()), // reader
                (io, obj, field) -> (Character) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putChar(field.getField().getChar(obj)), // writer
                char.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setShort(obj, io.getBuffer().getShort()), // reader
                (io, obj, field) -> (Short) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putShort(field.getField().getShort(obj)), // writer
                short.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setInt(obj, io.getBuffer().getInt()), // reader
                (io, obj, field) -> (Integer) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putInt(field.getField().getInt(obj)), // writer
                int.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setLong(obj, io.getBuffer().getLong()), // reader
                (io, obj, field) -> (Long) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putLong(field.getField().getLong(obj)), // writer
                long.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setFloat(obj, io.getBuffer().getFloat()), // reader
                (io, obj, field) -> (Float) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putFloat(field.getField().getFloat(obj)), // writer
                float.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().setDouble(obj, io.getBuffer().getDouble()), // reader
                (io, obj, field) -> (Double) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putDouble(field.getField().getDouble(obj)), // writer
                double.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getString()), // reader
                (io, obj, field) -> (String) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putString((String) field.getField().get(obj)), // writer
                String.class));
    }
}
