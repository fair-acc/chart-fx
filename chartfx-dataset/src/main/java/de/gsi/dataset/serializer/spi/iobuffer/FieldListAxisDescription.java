package de.gsi.dataset.serializer.spi.iobuffer;

import java.util.Collection;
import java.util.List;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.ClassFieldDescription;
import de.gsi.dataset.spi.DefaultAxisDescription;

/**
 * FieldSerialiser implementation for List&lt;AxisDescription&gt; to IoBuffer-backed byte-buffer
 * 
 * @author rstein
 */
public class FieldListAxisDescription extends IoBufferFieldSerialiser {
    /**
     * FieldSerialiser implementation for List&lt;AxisDescription&gt; to IoBuffer-backed byte-buffer
     * 
     * @param buffer the backing IoBuffer
     * 
     */
    public FieldListAxisDescription(IoSerialiser buffer) {
        super(buffer, (obj, field) -> {}, (obj, field) -> {}, List.class, AxisDescription.class);
        readerFunction = this::execFieldReader;
        writerFunction = this::execFieldWriter;
    }

    protected final void execFieldReader(final Object obj, ClassFieldDescription field) throws IllegalAccessException {
        Collection<AxisDescription> setVal = (Collection<AxisDescription>) field.getField().get(obj); // NOPMD
        // N.B. cast should fail at runtime (points to lib inconsistency)
        setVal.clear();
        final int nElements = ioBuffer.getBuffer().getInt(); // number of elements
        for (int i = 0; i < nElements; i++) {
            // read start marker
            ioBuffer.getFieldHeader();
            final byte startMarker = ioBuffer.getByte();
            // TODO: refactor
            // if (startMarker != DataType.START_MARKER.getAsByte()) {
            //     throw new IllegalStateException("corrupt start marker, value is " + startMarker + " vs. should " + DataType.START_MARKER.getAsByte());
            // }

            ioBuffer.getFieldHeader();
            String axisName = ioBuffer.getString();
            ioBuffer.getFieldHeader();
            String axisUnit = ioBuffer.getString();
            ioBuffer.getFieldHeader();
            double min = ioBuffer.getDouble();
            ioBuffer.getFieldHeader();
            double max = ioBuffer.getDouble();

            DefaultAxisDescription ad = new DefaultAxisDescription(i, axisName, axisUnit, min, max); // NOPMD
            // N.B. PMD - unavoidable in-loop instantiation

            ioBuffer.getFieldHeader();
            final byte endMarker = ioBuffer.getByte();
            //TODO: refactor
            // if (endMarker != DataType.END_MARKER.getAsByte()) {
            //      throw new IllegalStateException("corrupt end marker, value is " + endMarker + " vs. should " + DataType.END_MARKER.getAsByte());
            // }

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

        final long sizeMarkerStart = ioBuffer.putArrayHeader(fieldName, DataType.LIST,
                new int[] { nElements }, (nElements * entrySize) + 9);
        ioBuffer.getBuffer().putInt(nElements); // number of elements
        for (AxisDescription axis : axisDescriptions) {
            ioBuffer.putStartMarker(axis.getName());
            ioBuffer.put(fieldName, axis.getName());
            ioBuffer.put(fieldName, axis.getUnit());
            ioBuffer.put(fieldName, axis.getMin());
            ioBuffer.put(fieldName, axis.getMax());
            ioBuffer.putEndMarker(axis.getName());
        }
        ioBuffer.adjustDataByteSizeBlock(sizeMarkerStart);
    }
}