package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public class FieldDoubleArrayList extends AbstractIoBufferFieldSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldDoubleArrayList.class);

    public FieldDoubleArrayList(IoBuffer buffer, Class<?> classPrototype, Class<?>... classGenericArguments) {
        super(buffer, classPrototype, classGenericArguments);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(buffer).addArgument(classPrototype).addArgument(classGenericArguments)
                    .log("initialised({}, {}, {}");
        }
    }

    @Override
    public void readFrom(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        // DoubleArrayList origField = (DoubleArrayList) field.getField().get(obj);

        field.getField().set(obj, DoubleArrayList.wrap(BinarySerialiser.getDoubleArray(ioBuffer)));
    }

    @Override
    public void writeTo(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        final DoubleArrayList retVal = (DoubleArrayList) field.getField().get(obj);

        BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal.elements(), new int[] { retVal.size() });
    }

}
