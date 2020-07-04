package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.IoSerialiser;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public final class FieldDataSetHelper {
    private FieldDataSetHelper() {
        // utility class
    }

    /**
     * registers default DataSet interface and related helper methods
     * 
     * @param serialiser for which the field serialisers should be registered
     * @param ioBuffer reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoSerialiser ioBuffer) {
        // DoubleArrayList serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj, DoubleArrayList.wrap(ioBuffer.getDoubleArray())), // reader
                (obj, field) -> {
                    final DoubleArrayList retVal = (DoubleArrayList) field.getField().get(obj);
                    ioBuffer.put(retVal.elements(), new int[] { retVal.size() });
                }, // writer
                DoubleArrayList.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> {
                    final String dataSetType = ioBuffer.getString();
                    if (!DataSet.class.getName().equals(dataSetType)) {
                        throw new IllegalArgumentException("unknown DataSet type = " + dataSetType);
                    }

                    field.getField().set(obj, new DataSetSerialiser(ioBuffer).readDataSetFromByteArray());
                }, // reader
                (obj, field) -> {
                    final DataSet retVal = (DataSet) (field.getField() == null ? obj : field.getField().get(obj));

                    ioBuffer.getBuffer().putString(DataSet.class.getName());
                    new DataSetSerialiser(ioBuffer).writeDataSetToByteArray(retVal, false);
                    ioBuffer.updateDataEndMarker();
                }, // writer
                DataSet.class));

        // List<AxisDescription> serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new FieldListAxisDescription(ioBuffer));
    }
}
