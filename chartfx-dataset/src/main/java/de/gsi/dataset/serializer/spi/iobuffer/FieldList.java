package de.gsi.dataset.serializer.spi.iobuffer;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;

public class FieldList extends AbstractIoBufferFieldSerialiser {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldList.class);

    public FieldList(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
        super(buffer, classPrototype, classGenericArguments);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(buffer).addArgument(classPrototype).addArgument(classGenericArguments)
                    .log("initialised({}, {}, {}");
        }
    }

    @Override
    public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        Collection<?> origCollection = (Collection<?>) field.getField().get(obj);
        origCollection.clear();

        final Collection<?> setVal = BinarySerialiser.getCollection(ioBuffer, origCollection);

        field.getField().set(obj, setVal);
    }

    @Override
    public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        final Collection<?> retVal = (Collection<?>) field.getField().get(obj);

        BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal);
    }

}
