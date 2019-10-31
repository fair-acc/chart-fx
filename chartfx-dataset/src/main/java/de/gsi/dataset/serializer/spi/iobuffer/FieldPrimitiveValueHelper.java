package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;

public class FieldPrimitiveValueHelper {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldPrimitiveValueHelper.class);
    protected static final FieldPrimitiveValueHelper SELF = new FieldPrimitiveValueHelper();

    private FieldPrimitiveValueHelper() {
        // utility class
    }
    
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {
        serialiser.addClassDefinition(SELF.new FieldPrimitiveBoolean(ioBuffer, boolean.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveByte(ioBuffer, byte.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveShort(ioBuffer, short.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveInteger(ioBuffer, int.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveLong(ioBuffer, long.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveFloat(ioBuffer, float.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveDouble(ioBuffer, double.class));
        serialiser.addClassDefinition(SELF.new FieldPrimitiveString(ioBuffer, String.class));

        // alt implementation option:
//        serialiser.addClassDefinition((obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), \\
//                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getBoolean(obj)), \\
//                (obj, field) -> field.getField().setBoolean(obj, ioBuffer.getBoolean()), boolean.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(serialiser).addArgument(ioBuffer).log("initialised({}, {}");
        }

    }

    public class FieldPrimitiveBoolean extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveBoolean(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            final boolean retVal = ioBuffer.getBoolean();
            field.getField().setBoolean(obj, retVal);
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getBoolean(obj));
        }
    }

    public class FieldPrimitiveByte extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveByte(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().setByte(obj, ioBuffer.getByte());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getByte(obj));
        }
    }

    public class FieldPrimitiveDouble extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveDouble(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getDouble());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getDouble(obj));
        }
    }

    public class FieldPrimitiveFloat extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveFloat(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getFloat());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getFloat(obj));
        }
    }

    public class FieldPrimitiveInteger extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveInteger(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getInt());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getInt(obj));
        }
    }

    public class FieldPrimitiveLong extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveLong(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getLong());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getLong(obj));
        }
    }

    public class FieldPrimitiveShort extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveShort(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getShort());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), field.getField().getShort(obj));
        }
    }

    public class FieldPrimitiveString extends AbstractIoBufferFieldSerialiser {
        public FieldPrimitiveString(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getString());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (String) field.getField().get(obj));
        }
    }

}
