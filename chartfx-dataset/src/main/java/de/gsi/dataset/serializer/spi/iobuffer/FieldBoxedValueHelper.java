package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoClassSerialiser;

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
     */
    public static void register(final IoClassSerialiser serialiser) {
        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getBoolean()), // reader
                (io, obj, field) -> (Boolean) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putBoolean((Boolean) field.getField().get(obj)), // writer
                Boolean.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getByte()), // reader
                (io, obj, field) -> (Byte) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putByte((Byte) field.getField().get(obj)), // writer
                Byte.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getShort()), // reader
                (io, obj, field) -> (Short) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putShort((Short) field.getField().get(obj)), // writer
                Short.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getInt()), // reader
                (io, obj, field) -> (Integer) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putInt((Integer) field.getField().get(obj)), // writer
                Integer.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getLong()), // reader
                (io, obj, field) -> (Long) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putLong((Long) field.getField().get(obj)), // writer
                Long.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getFloat()), // reader
                (io, obj, field) -> (Float) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putFloat((Float) field.getField().get(obj)), // writer
                Float.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getDouble()), // reader
                (io, obj, field) -> (Double) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putDouble((Double) field.getField().get(obj)), // writer
                Double.class));
    }
}
