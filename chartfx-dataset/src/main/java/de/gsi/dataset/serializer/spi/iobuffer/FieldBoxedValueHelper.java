package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;

/**
 * helper class to register default serialiser for boxed primitive types (ie. Boolean, Byte, Short, ..., double) w/o
 * String (already part of the {@link de.gsi.dataset.serializer.spi.iobuffer.FieldPrimitiveValueHelper}
 * 
 * @author rstein
 */
public final class FieldBoxedValueHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldBoxedValueHelper.class);

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
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getBoolean()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        (Boolean) field.getField().get(obj)), // writer
                Boolean.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getByte()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), (Byte) field.getField().get(obj)), // writer
                Byte.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getShort()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), (Short) field.getField().get(obj)), // writer
                Short.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getInt()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        (Integer) field.getField().get(obj)), // writer
                Integer.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getLong()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), (Long) field.getField().get(obj)), // writer
                Long.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getFloat()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), (Float) field.getField().get(obj)), // writer
                Float.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, ioBuffer.getDouble()), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
                        (Double) field.getField().get(obj)), // writer
                Double.class));

//        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
//                (obj, field) -> field.getField().set(obj, ioBuffer.getString()), // reader
//                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(),
//                        (String) field.getField().get(obj)), // writer
//                String.class));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(serialiser).addArgument(ioBuffer).log("initialised({}, {}");
        }
    }
}
