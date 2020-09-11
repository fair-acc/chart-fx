package de.gsi.serializer.spi.iobuffer;

import de.gsi.serializer.FieldSerialiser;
import de.gsi.serializer.IoClassSerialiser;

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
     */
    public static void register(final IoClassSerialiser serialiser) {
        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBooleanArray((boolean[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (boolean[]) field.getField().get(obj)), // writer
                boolean[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getByteArray((byte[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (byte[]) field.getField().get(obj)), // writer
                byte[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getCharArray((char[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (char[]) field.getField().get(obj)), // writer
                char[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getShortArray((short[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (short[]) field.getField().get(obj)), // writer
                short[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getIntArray((int[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (int[]) field.getField().get(obj)), // writer
                int[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getLongArray((long[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (long[]) field.getField().get(obj)), // writer
                long[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getFloatArray((float[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (float[]) field.getField().get(obj)), // writer
                float[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getDoubleArray((double[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (double[]) field.getField().get(obj)), // writer
                double[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getStringArray((String[]) field.getField().get(obj))), // reader
                (io, obj, field) -> { throw new UnsupportedOperationException("return function not supported for primitive types"); }, // return
                (io, obj, field) -> io.put(field, (String[]) field.getField().get(obj)), // writer
                String[].class));
    }
}
