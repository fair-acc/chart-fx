package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoClassSerialiser;
import de.gsi.dataset.utils.GenericsHelper;

/**
 * helper class to register default serialiser for boxed array primitive types (ie. Boolean[], Byte[], Short[], ...,
 * double[]) w/o String[] (already part of the {@link de.gsi.dataset.serializer.spi.iobuffer.FieldPrimitiveValueHelper}
 * 
 * @author rstein
 */
public final class FieldBoxedValueArrayHelper {
    private FieldBoxedValueArrayHelper() {
        // utility class
    }

    /**
     * registers default serialiser for boxed array primitive types (ie. Boolean[], Byte[], Short[], ..., Double[])
     * 
     * @param serialiser for which the field serialisers should be registered
     */
    public static void register(final IoClassSerialiser serialiser) {
        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getBooleanArray())), // reader
                (io, obj, field) -> (Boolean[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putBooleanArray(GenericsHelper.toBoolPrimitive((Boolean[]) field.getField().get(obj))), // writer
                Boolean[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getByteArray())), // reader
                (io, obj, field) -> (Byte[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putByteArray(GenericsHelper.toBytePrimitive((Byte[]) field.getField().get(obj))), // writer
                Byte[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getCharArray())), // reader
                (io, obj, field) -> (Character[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putCharArray(GenericsHelper.toCharPrimitive((Character[]) field.getField().get(obj))), // writer
                Character[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getShortArray())), // reader
                (io, obj, field) -> (Short[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putShortArray(GenericsHelper.toShortPrimitive((Short[]) field.getField().get(obj))), // writer
                Short[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getIntArray())), // reader
                (io, obj, field) -> (Integer[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putIntArray(GenericsHelper.toIntegerPrimitive((Integer[]) field.getField().get(obj))), // writer
                Integer[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getLongArray())), // reader
                (io, obj, field) -> (Long[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putLongArray(GenericsHelper.toLongPrimitive((Long[]) field.getField().get(obj))), // writer
                Long[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getFloatArray())), // reader
                (io, obj, field) -> (Float[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putFloatArray(GenericsHelper.toFloatPrimitive((Float[]) field.getField().get(obj))), // writer
                Float[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBuffer().getDoubleArray())), // reader
                (io, obj, field) -> (Double[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putDoubleArray(GenericsHelper.toDoublePrimitive((Double[]) field.getField().get(obj))), // writer
                Double[].class));
    }
}
