package de.gsi.serializer.spi.iobuffer;

import de.gsi.dataset.utils.GenericsHelper;
import de.gsi.serializer.FieldSerialiser;
import de.gsi.serializer.IoClassSerialiser;

/**
 * helper class to register default serialiser for boxed array primitive types (ie. Boolean[], Byte[], Short[], ...,
 * double[]) w/o String[] (already part of the {@link FieldPrimitiveValueHelper}
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
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getBooleanArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toBoolPrimitive((Boolean[]) field.getField().get(obj))), // writer
                Boolean[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getByteArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toBytePrimitive((Byte[]) field.getField().get(obj))), // writer
                Byte[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getCharArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toCharPrimitive((Character[]) field.getField().get(obj))), // writer
                Character[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getShortArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toShortPrimitive((Short[]) field.getField().get(obj))), // writer
                Short[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getIntArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toIntegerPrimitive((Integer[]) field.getField().get(obj))), // writer
                Integer[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getLongArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toLongPrimitive((Long[]) field.getField().get(obj))), // writer
                Long[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getFloatArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toFloatPrimitive((Float[]) field.getField().get(obj))), // writer
                Float[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, GenericsHelper.toObject(io.getDoubleArray())), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, GenericsHelper.toDoublePrimitive((Double[]) field.getField().get(obj))), // writer
                Double[].class));
    }
}
