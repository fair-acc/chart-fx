package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;
import de.gsi.dataset.serializer.spi.GenericsHelper;

public class FieldBoxedValueArrayHelper {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldBoxedValueArrayHelper.class);
    protected static final FieldBoxedValueArrayHelper SELF = new FieldBoxedValueArrayHelper();

    private FieldBoxedValueArrayHelper() {
        // utility class
    }
    
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {
        serialiser.addClassDefinition(SELF.new FieldBoxedBooleanArray(ioBuffer, Boolean[].class));
        serialiser.addClassDefinition(SELF.new FieldBoxedByteArray(ioBuffer, Byte[].class));
        serialiser.addClassDefinition(SELF.new FieldBoxedShortArray(ioBuffer, Short[].class));
        serialiser.addClassDefinition(SELF.new FieldBoxedIntegerArray(ioBuffer, Integer[].class));
        serialiser.addClassDefinition(SELF.new FieldBoxedLongArray(ioBuffer, Long[].class));
        serialiser.addClassDefinition(SELF.new FieldBoxedFloatArray(ioBuffer, Float[].class));
        serialiser.addClassDefinition(SELF.new FieldBoxedDoubleArray(ioBuffer, Double[].class));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(serialiser).addArgument(ioBuffer).log("initialised({}, {}");
        }
    }

    public class FieldBoxedBooleanArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedBooleanArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getBooleanArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toBoolPrimitive((Boolean[]) field.getField().get(obj)));
        }
    }

    public class FieldBoxedByteArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedByteArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getByteArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toBytePrimitive((Byte[]) field.getField().get(obj)));
        }
    }

    public class FieldBoxedDoubleArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedDoubleArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getDoubleArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toDoublePrimitive((Double[]) field.getField().get(obj)));
        }
    }

    public class FieldBoxedFloatArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedFloatArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getFloatArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toFloatPrimitive((Float[]) field.getField().get(obj)));
        }
    }

    public class FieldBoxedIntegerArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedIntegerArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getIntArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toIntegerPrimitive((Integer[]) field.getField().get(obj)));
        }
    }

    public class FieldBoxedLongArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedLongArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getLongArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toLongPrimitive((Long[]) field.getField().get(obj)));
        }
    }

    public class FieldBoxedShortArray extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedShortArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, GenericsHelper.toObject(ioBuffer.getShortArray()));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(),
                    GenericsHelper.toShortPrimitive((Short[]) field.getField().get(obj)));
        }
    }
}
