package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.GenericsHelper;

/**
 * helper class to register default serialiser for boxed array primitive types
 * (ie. Boolean[], Byte[], Short[], ..., double[]) w/o String[] (already part of
 * the {@link de.gsi.dataset.serializer.spi.iobuffer.FieldPrimitiveValueHelper}
 * 
 * @author rstein
 */
public final class FieldBoxedValueArrayHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldBoxedValueArrayHelper.class);

    private FieldBoxedValueArrayHelper() {
        // utility class
    }

    /**
     * registers default serialiser for boxed array primitive types (ie. Boolean[],
     * Byte[], Short[], ..., Double[])
     * 
     * @param serialiser for which the field serialisers should be registered
     * @param ioBuffer   reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getBooleanArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toBoolPrimitive((Boolean[]) field.getField().get(obj))), // writer
                Boolean[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getByteArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toBytePrimitive((Byte[]) field.getField().get(obj))), // writer
                Byte[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getShortArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toShortPrimitive((Short[]) field.getField().get(obj))), // writer
                Short[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getIntArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toIntegerPrimitive((Integer[]) field.getField().get(obj))), // writer
                Integer[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getLongArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toLongPrimitive((Long[]) field.getField().get(obj))), // writer
                Long[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getFloatArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toFloatPrimitive((Float[]) field.getField().get(obj))), // writer
                Float[].class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getDoubleArray())), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        GenericsHelper.toDoublePrimitive((Double[]) field.getField().get(obj))), // writer
                Double[].class));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(serialiser).addArgument(ioBuffer).log("initialised({}, {}");
        }
    }
}
