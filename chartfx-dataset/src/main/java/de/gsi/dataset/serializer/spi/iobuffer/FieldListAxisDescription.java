package de.gsi.dataset.serializer.spi.iobuffer;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;
import de.gsi.dataset.spi.DefaultAxisDescription;

/**
 * FieldSerialiser implementation for List&lt;AxisDescription&gt; to
 * IoBuffer-backed byte-buffer
 * 
 * @author rstein
 */
public class FieldListAxisDescription extends IoBufferFieldSerialiser {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FieldListAxisDescription.class);

    /**
     * FieldSerialiser implementation for List&lt;AxisDescription&gt; to
     * IoBuffer-backed byte-buffer
     * 
     * @param buffer the backing IoBuffer
     * 
     */
    public FieldListAxisDescription(IoBuffer buffer) {
        super(buffer, (obj, field) -> {
        }, (obj, field) -> {
        }, List.class, AxisDescription.class);
        readerFunction = this::execFieldReader;
        writerFunction = this::execFieldWriter;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(buffer).addArgument(getClassPrototype()).addArgument(getGenericsPrototypes())
                    .log("initialised({}, {}, {}");
        }
    }

    protected final void execFieldReader(final Object obj, ClassFieldDescription field) throws IllegalAccessException {
        Collection<AxisDescription> setVal = (Collection<AxisDescription>) field.getField().get(obj); // NOPMD
        // N.B. cast should fail at runtime (points to lib inconsistency)
        setVal.clear();
        final int nElements = ioBuffer.getInt(); // number of elements
        for (int i = 0; i < nElements; i++) {
            // read start marker
            BinarySerialiser.getFieldHeader(ioBuffer);
            final byte startMarker = ioBuffer.getByte();
            if (startMarker != DataType.START_MARKER.getAsByte()) {
                throw new IllegalStateException("corrupt start marker, value is " + startMarker + " vs. should "
                        + DataType.START_MARKER.getAsByte());
            }

            BinarySerialiser.getFieldHeader(ioBuffer);
            String axisName = ioBuffer.getString();
            BinarySerialiser.getFieldHeader(ioBuffer);
            String axisUnit = ioBuffer.getString();
            BinarySerialiser.getFieldHeader(ioBuffer);
            double min = ioBuffer.getDouble();
            BinarySerialiser.getFieldHeader(ioBuffer);
            double max = ioBuffer.getDouble();

            DefaultAxisDescription ad = new DefaultAxisDescription(null, axisName, axisUnit, min, max); // NOPMD
            // N.B. PMD - unavoidable in-loop instantiation

            BinarySerialiser.getFieldHeader(ioBuffer);
            final byte endMarker = ioBuffer.getByte();
            if (endMarker != DataType.END_MARKER.getAsByte()) {
                throw new IllegalStateException(
                        "corrupt end marker, value is " + endMarker + " vs. should " + DataType.END_MARKER.getAsByte());
            }

            setVal.add(ad);
        }

        field.getField().set(obj, setVal);
    }

    protected void execFieldWriter(Object obj, ClassFieldDescription field) throws IllegalAccessException {
        final String fieldName = field.getFieldName();
        final List<AxisDescription> axisDescriptions = (List<AxisDescription>) field.getField().get(obj); // NOPMD
        // N.B. cast should fail at runtime (points to lib inconsistency)

        final int nElements = axisDescriptions.size();
        final int entrySize = 50; // as an initial estimate

        final long sizeMarkerStart = BinarySerialiser.putArrayHeader(ioBuffer, fieldName, DataType.LIST,
                new int[] { nElements }, (nElements * entrySize) + 9);
        ioBuffer.putInt(nElements); // number of elements
        for (AxisDescription axis : axisDescriptions) {
            BinarySerialiser.putStartMarker(ioBuffer, axis.getName());
            BinarySerialiser.put(ioBuffer, fieldName, axis.getName());
            BinarySerialiser.put(ioBuffer, fieldName, axis.getUnit());
            BinarySerialiser.put(ioBuffer, fieldName, axis.getMin());
            BinarySerialiser.put(ioBuffer, fieldName, axis.getMax());
            BinarySerialiser.putEndMarker(ioBuffer, axis.getName());
        }
        BinarySerialiser.adjustDataByteSizeBlock(ioBuffer, sizeMarkerStart);
    }
}