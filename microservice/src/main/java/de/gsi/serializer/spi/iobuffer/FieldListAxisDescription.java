package de.gsi.serializer.spi.iobuffer;

import java.util.ArrayList;
import java.util.List;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.spi.DefaultAxisDescription;
import de.gsi.serializer.FieldSerialiser;
import de.gsi.serializer.IoSerialiser;
import de.gsi.serializer.spi.ClassFieldDescription;

/**
 * FieldSerialiser implementation for List&lt;AxisDescription&gt; to IoBuffer-backed byte-buffer
 * 
 * @author rstein
 */
public class FieldListAxisDescription extends FieldSerialiser<List<AxisDescription>> {
    /**
     * FieldSerialiser implementation for List&lt;AxisDescription&gt; to IoBuffer-backed byte-ioSerialiser
     * 
     */
    public FieldListAxisDescription() {
        super((io, obj, field) -> {}, (io, obj, field) -> null, (io, obj, field) -> {}, List.class, AxisDescription.class);
        readerFunction = this::execFieldReader;
        returnFunction = this::execFieldReturn;
        writerFunction = this::execFieldWriter;
    }

    protected void execFieldReader(final IoSerialiser ioSerialiser, final Object obj, ClassFieldDescription field) {
        field.getField().set(obj, execFieldReturn(ioSerialiser, obj, field));
    }

    protected List<AxisDescription> execFieldReturn(final IoSerialiser ioSerialiser, Object obj, ClassFieldDescription field) {
        final Object oldObject = field.getField().get(obj);
        final boolean isListPresent = oldObject instanceof List;

        final int nElements = ioSerialiser.getBuffer().getInt(); // number of elements
        // N.B. cast should fail at runtime (points to lib inconsistency)
        List<AxisDescription> setVal = isListPresent ? (List<AxisDescription>) field.getField().get(obj) : new ArrayList<>(nElements); // NOPMD
        if (isListPresent) {
            setVal.clear();
        }

        for (int i = 0; i < nElements; i++) {
            String axisName = ioSerialiser.getBuffer().getString();
            String axisUnit = ioSerialiser.getBuffer().getString();
            double min = ioSerialiser.getBuffer().getDouble();
            double max = ioSerialiser.getBuffer().getDouble();
            DefaultAxisDescription ad = new DefaultAxisDescription(i, axisName, axisUnit, min, max); // NOPMD
            // N.B. PMD - unavoidable in-loop instantiation
            setVal.add(ad);
        }

        return setVal;
    }

    protected void execFieldWriter(final IoSerialiser ioSerialiser, Object obj, ClassFieldDescription field) {
        final List<AxisDescription> axisDescriptions = (List<AxisDescription>) field.getField().get(obj); // NOPMD
        // N.B. cast should fail at runtime (points to lib inconsistency)

        final int nElements = axisDescriptions.size();
        final int entrySize = 50; // as an initial estimate

        ioSerialiser.getBuffer().ensureAdditionalCapacity((nElements * entrySize) + 9);
        ioSerialiser.getBuffer().putInt(nElements); // number of elements
        for (AxisDescription axis : axisDescriptions) {
            ioSerialiser.getBuffer().putString(axis.getName());
            ioSerialiser.getBuffer().putString(axis.getUnit());
            ioSerialiser.getBuffer().putDouble(axis.getMin());
            ioSerialiser.getBuffer().putDouble(axis.getMax());
        }
    }
}