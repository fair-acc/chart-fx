package de.gsi.dataset.serializer.spi.iobuffer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser.HeaderInfo;
import de.gsi.dataset.serializer.spi.ClassDescriptions;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;
import de.gsi.dataset.serializer.spi.FieldHeader;
import de.gsi.dataset.serializer.spi.FieldSerialiser;
import de.gsi.dataset.serializer.spi.FieldSerialiser.FieldSerialiserFunction;

/**
 * reference implementation for streaming arbitrary object to and from a IoBuffer-based byte-buffer
 *
 * @author rstein
 */
public class IoBufferSerialiser extends AbstractSerialiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(IoBufferSerialiser.class);
    private final Map<Integer, WeakHashMap<String, ClassFieldDescription>> fieldToClassFieldDescription = new HashMap<>();
    private final IoBuffer ioBuffer;

    /**
     * Initialises new IoBuffer-backed object serialiser
     *
     * @param buffer the backing IoBuffer (see e.g. {@link de.gsi.dataset.serializer.spi.FastByteBuffer} or
     *        {@link de.gsi.dataset.serializer.spi.ByteBuffer}
     */
    public IoBufferSerialiser(final IoBuffer buffer) {
        super();
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        ioBuffer = buffer;
        startMarkerFunction = subClassName -> BinarySerialiser.putStartMarker(ioBuffer, subClassName);
        endMarkerFunction = subClassName -> BinarySerialiser.putEndMarker(ioBuffer, subClassName);

        // register primitive and boxed data type handlers
        FieldPrimitiveValueHelper.register(this, ioBuffer);
        FieldPrimitveValueArrayHelper.register(this, ioBuffer);
        FieldBoxedValueHelper.register(this, ioBuffer);
        FieldBoxedValueArrayHelper.register(this, ioBuffer);

        // Collection serialiser mapper to IoBuffer
        final FieldSerialiserFunction collectionReader = (obj, field) -> {
            final Collection<?> origCollection = (Collection<?>) field.getField().get(obj);
            origCollection.clear();

            final Collection<?> setVal = BinarySerialiser.getCollection(ioBuffer, origCollection);
            field.getField().set(obj, setVal);
        }; // reader
        final FieldSerialiserFunction collectionWriter = (obj, field) -> {
            final Collection<?> retVal = (Collection<?>) field.getField().get(obj);
            BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal); // writer
        };
        addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, collectionReader, collectionWriter, Collection.class));
        addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, collectionReader, collectionWriter, List.class));
        addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, collectionReader, collectionWriter, Queue.class));
        addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, collectionReader, collectionWriter, Set.class));

        // Enum serialiser mapper to IoBuffer
        addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, BinarySerialiser.getEnum(ioBuffer, (Enum<?>) field.getField().get(obj))), // reader
                (obj, field) -> BinarySerialiser.put(ioBuffer, field.getFieldName(), (Enum<?>) field.getField().get(obj)), // writer
                Enum.class));

        // Map serialiser mapper to IoBuffer
        addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> { // reader
                    final Map<?, ?> origMap = (Map<?, ?>) field.getField().get(obj);
                    origMap.clear();
                    final Map<?, ?> setVal = BinarySerialiser.getMap(ioBuffer, origMap);

                    field.getField().set(obj, setVal);
                }, // writer
                (obj, field) -> {
                    final Map<?, ?> retVal = (Map<?, ?>) field.getField().get(obj);
                    BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal);
                },
                Map.class));

        FieldDataSetHelper.register(this, ioBuffer);

        // addClassDefinition(null, StringHashMapList.class);
    }

    protected void deserialise(final Object obj, final FieldHeader fieldRoot, final ClassFieldDescription classFieldDescription, final int recursionDepth) throws IllegalAccessException {
        final String ioName = fieldRoot.getFieldName();

        if (!ioName.equals(classFieldDescription.getFieldName())) {
            // did not find matching (sub-)field in class
            if (fieldRoot.getChildren().isEmpty()) {
                return;
            }
            // check for potential inner fields
            for (final FieldHeader fieldHeader : fieldRoot.getChildren()) {
                final String fieldName = fieldHeader.getFieldName();
                Map<String, ClassFieldDescription> rMap = fieldToClassFieldDescription.computeIfAbsent(recursionDepth, depth -> new WeakHashMap<>());
                final ClassFieldDescription subFieldDescription = rMap.computeIfAbsent(fieldName, name -> classFieldDescription.getChildren().stream().filter(e -> e.getFieldName().equals(fieldName)).findFirst().get());

                if (subFieldDescription != null) {
                    deserialise(obj, fieldHeader, subFieldDescription, recursionDepth + 1);
                }
            }
            return;
        }

        final Class<?> fieldClass = classFieldDescription.getType();
        if (classFieldDescription.isFinal() && !fieldClass.isInterface()) {
            // cannot set final variables
            LOGGER.atWarn().addArgument(classFieldDescription.getFieldNameRelative()).log("cannot set final field '{}'");
            return;
        }

        final Optional<FieldSerialiser> serialiser = findFieldSerialiserForKnownClassOrInterface(fieldClass, classFieldDescription.getActualTypeArguments());

        if (!serialiser.isPresent()) {
            final Object ref = classFieldDescription.getField().get(obj);
            final Object subRef;
            if (ref == null) {
                subRef = classFieldDescription.allocateMemberClassField(obj);
            } else {
                subRef = ref;
            }

            // no specific deserialiser present check for potential inner fields
            for (final FieldHeader fieldHeader : fieldRoot.getChildren()) {
                final String fieldName = fieldHeader.getFieldName();
                Map<String, ClassFieldDescription> rMap = fieldToClassFieldDescription.computeIfAbsent(recursionDepth, depth -> new WeakHashMap<>());
                final ClassFieldDescription subFieldDescription = rMap.computeIfAbsent(fieldName, name -> classFieldDescription.getChildren().stream().filter(e -> e.getFieldName().equals(fieldName)).findFirst().get());

                if (subFieldDescription != null) {
                    deserialise(subRef, fieldHeader, subFieldDescription, recursionDepth + 1);
                }
            }
            return;
        }

        ioBuffer.position(fieldRoot.getDataBufferPosition());
        serialiser.get().getReaderFunction().exec(obj, classFieldDescription);
    }

    @Override
    public Object deserialiseObject(final Object obj) throws IllegalAccessException {
        if (obj == null) {
            throw new IllegalArgumentException("obj must not be null (yet)");
        }

        final long startPosition = ioBuffer.position();
        final HeaderInfo bufferHeader = BinarySerialiser.checkHeaderInfo(ioBuffer);

        // match field header with class field description
        final ClassFieldDescription classFieldDescription = ClassDescriptions.get(obj.getClass());

        ioBuffer.position(startPosition);
        final FieldHeader fieldRoot = BinarySerialiser.parseIoStream(ioBuffer);
        // deserialise into object
        for (final FieldHeader child : fieldRoot.getChildren()) {
            deserialise(obj, child, classFieldDescription, 0);
        }

        return obj;
    }

    public IoBuffer getBuffer() {
        return ioBuffer;
    }

    @Override
    public void serialiseObject(final Object obj) throws IllegalAccessException {
        BinarySerialiser.putHeaderInfo(ioBuffer);

        super.serialiseObject(obj);

        BinarySerialiser.putEndMarker(ioBuffer, "OBJ_ROOT_END");
    }
}
