package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;

/**
 * 
 * @author rstein
 *
 */
public class FieldBoxedValueHelper {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldBoxedValueHelper.class);
    protected static final FieldBoxedValueHelper SELF = new FieldBoxedValueHelper();

    private FieldBoxedValueHelper() {
        // utility class
    }
    
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {
        serialiser.addClassDefinition(SELF.new FieldBoxedBoolean(ioBuffer, Boolean.class));
        serialiser.addClassDefinition(SELF.new FieldBoxedByte(ioBuffer, Byte.class));
        serialiser.addClassDefinition(SELF.new FieldBoxedShort(ioBuffer, Short.class));
        serialiser.addClassDefinition(SELF.new FieldBoxedInteger(ioBuffer, Integer.class));
        serialiser.addClassDefinition(SELF.new FieldBoxedLong(ioBuffer, Long.class));
        serialiser.addClassDefinition(SELF.new FieldBoxedFloat(ioBuffer, Float.class));
        serialiser.addClassDefinition(SELF.new FieldBoxedDouble(ioBuffer, Double.class));
//        serialiser.addClassDefinition(SELF.new FieldBoxedString(ioBuffer, String.class));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(serialiser).addArgument(ioBuffer).log("initialised({}, {}");
        }
    }

    public class FieldBoxedBoolean extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedBoolean(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            final Boolean setVal = ioBuffer.getBoolean();
            field.getField().set(obj, setVal);
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            Boolean retVal = (Boolean) field.getField().get(obj);
            BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal.booleanValue());
        }
    }

    public class FieldBoxedByte extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedByte(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            final Byte setVal = ioBuffer.getByte();
            field.getField().set(obj, setVal);
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (Byte) field.getField().get(obj));
        }
    }

    public class FieldBoxedDouble extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedDouble(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getDouble());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (Double) field.getField().get(obj));
        }
    }

    public class FieldBoxedFloat extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedFloat(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getFloat());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (Float) field.getField().get(obj));
        }
    }

    public class FieldBoxedInteger extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedInteger(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getInt());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            final Integer retVal = (Integer) field.getField().get(obj);
            BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal);
        }
    }

    public class FieldBoxedLong extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedLong(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getLong());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (Long) field.getField().get(obj));
        }
    }

    public class FieldBoxedShort extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedShort(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
            super(buffer, classPrototype, classGenericArguments);
        }

        @Override
        public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            field.getField().set(obj, ioBuffer.getShort());
        }

        @Override
        public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
            BinarySerialiser.put(ioBuffer, field.getFieldName(), (Short) field.getField().get(obj));
        }
    }

    public class FieldBoxedString extends AbstractIoBufferFieldSerialiser {
        public FieldBoxedString(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
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
