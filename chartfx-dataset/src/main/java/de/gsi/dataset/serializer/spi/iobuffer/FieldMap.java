package de.gsi.dataset.serializer.spi.iobuffer;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;

public class FieldMap extends AbstractIoBufferFieldSerialiser {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldMap.class);

    public FieldMap(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
        super(buffer, classPrototype, classGenericArguments);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(buffer).addArgument(classPrototype).addArgument(classGenericArguments)
                    .log("initialised({}, {}, {}");
        }
    }

    @Override
    public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        Map<?, ?> origMap = (Map<?, ?>) field.getField().get(obj);
        origMap.clear();
        final Map<?, ?> setVal = BinarySerialiser.getMap(ioBuffer, origMap);

        field.getField().set(obj, setVal);
    }

    @Override
    public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        final Map<?, ?> retVal = (Map<?, ?>) field.getField().get(obj);

        BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal);
    }

}