package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;

public class FieldPrimitveValueArrayHelper {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldPrimitveValueArrayHelper.class);
    protected static final FieldPrimitveValueArrayHelper SELF = new FieldPrimitveValueArrayHelper();

    private FieldPrimitveValueArrayHelper() {
        // utility class
    }

    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {
        serialiser.addClassDefinition(SELF.new FieldPrimitiveBooleanArray(ioBuffer, boolean[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveByteArray(ioBuffer, byte[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveShortArray(ioBuffer, short[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveIntegerArray(ioBuffer, int[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveLongArray(ioBuffer, long[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveFloatArray(ioBuffer, float[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveDoubleArray(ioBuffer, double[].class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveStringArray(ioBuffer, String[].class));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(serialiser).addArgument(ioBuffer).log("initialised({}, {}");
        }
    }

    public class FieldPrimitiveBooleanArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveBooleanArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            boolean[] retVal = ioBuffer.getBooleanArray((boolean[]) field.getField().get(obj));
            field.getField().set(obj, retVal);
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (boolean[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveByteArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveByteArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getByteArray((byte[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (byte[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveDoubleArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveDoubleArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getDoubleArray((double[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (double[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveFloatArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveFloatArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getFloatArray((float[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (float[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveIntegerArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveIntegerArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getIntArray((int[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (int[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveLongArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveLongArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getLongArray((long[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (long[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveShortArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveShortArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getShortArray((short[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (short[]) field.getField().get(obj));
        }
    }

    public class FieldPrimitiveStringArray extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveStringArray(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getStringArray((String[]) field.getField().get(obj)));
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (String[]) field.getField().get(obj));
        }
    }

}
