package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.FieldSerialiser;

/**
 * 
 * @author rstein
 *
 */
public class IoBufferFieldSerialiser extends FieldSerialiser {
    protected final IoSerialiser ioSerialiser;

    /**
     * 
     * @param ioSerialiser reference to the IoBuffer to be used. The reseting/rewinding has to be done in the user-level code
     * @param reader consumer executed when reading from the back-end serialiser implementation
     * @param writer consumer executed when writing to the back-end serialiser implementation
     * @param classPrototype applicable class/interface prototype reference for which the consumers are applicable (e.g.
     *        example 1: 'List.class' for List&lt;String&gt; or example 2: 'Map.class' for Map&lt;Integer, String&gt;)
     * @param classGenericArguments applicable generics definition (e.g. 'String.class' for List&lt;String&gt; or
     *        'Integer.class, String.class' resp.)
     */
    public IoBufferFieldSerialiser(final IoSerialiser ioSerialiser, final FieldSerialiserFunction reader,
            final FieldSerialiserFunction writer, final Class<?> classPrototype,
            final Class<?>... classGenericArguments) {
        super(reader, writer, classPrototype, classGenericArguments);
        if (ioSerialiser == null) {
            throw new IllegalArgumentException("ioSerialiser must not be null");
        }
        this.ioSerialiser = ioSerialiser;
    }

    /**
     * 
     * @return the IoSerialiser object used by this serialiser
     */
    public IoSerialiser getBuffer() {
        return ioSerialiser;
    }
}
