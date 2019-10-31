package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractFieldSerialiser;

/**
 * 
 * @author rstein
 *
 */
public abstract class AbstractIoBufferFieldSerialiser extends AbstractFieldSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIoBufferFieldSerialiser.class);
    protected final IoBuffer ioBuffer;

    public AbstractIoBufferFieldSerialiser(final IoBuffer buffer, final Class<?> classPrototype,
            final Class<?>... classGenericArguments) {
        super(classPrototype, classGenericArguments);
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        ioBuffer = buffer;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(buffer).addArgument(classPrototype).addArgument(classGenericArguments)
                    .log("AbstractIoBufferFieldSerialiser({}, {}, {}) -- initialised");
        }
    }

    public IoBuffer getBuffer() {
        return ioBuffer;
    }
}
