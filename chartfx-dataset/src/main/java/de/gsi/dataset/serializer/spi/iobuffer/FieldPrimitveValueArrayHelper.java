package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoClassSerialiser;

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
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getBooleanArray((boolean[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (boolean[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putBooleanArray((boolean[]) field.getField().get(obj)), // writer
                boolean[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getByteArray((byte[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (byte[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putByteArray((byte[]) field.getField().get(obj)), // writer
                byte[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getCharArray((char[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (char[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putCharArray((char[]) field.getField().get(obj)), // writer
                char[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getShortArray((short[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (short[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putShortArray((short[]) field.getField().get(obj)), // writer
                short[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getIntArray((int[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (int[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putIntArray((int[]) field.getField().get(obj)), // writer
                int[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getLongArray((long[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (long[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putLongArray((long[]) field.getField().get(obj)), // writer
                long[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getFloatArray((float[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (float[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putFloatArray((float[]) field.getField().get(obj)), // writer
                float[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getDoubleArray((double[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (double[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putDoubleArray((double[]) field.getField().get(obj)), // writer
                double[].class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, io.getBuffer().getStringArray((String[]) field.getField().get(obj))), // reader
                (io, obj, field) -> (String[]) field.getField().get(obj), // return
                (io, obj, field) -> io.getBuffer().putStringArray((String[]) field.getField().get(obj)), // writer
                String[].class));
    }
}
