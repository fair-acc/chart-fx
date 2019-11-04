package de.gsi.dataset.serializer.spi.iobuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.AbstractSerialiser;
import de.gsi.dataset.serializer.spi.BinarySerialiser;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public final class FieldDataSetHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldDataSetHelper.class);

    private FieldDataSetHelper() {
        // utility class
    }

    /**
     * registers default DataSet interface and related helper methods
     * 
     * @param serialiser for which the field serialisers should be registered
     * @param ioBuffer   reference to the IoBuffer back-ends
     */
    public static void register(final AbstractSerialiser serialiser, final IoBuffer ioBuffer) {

        // DoubleArrayList serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> field.getField().set(obj,
                        DoubleArrayList.wrap(BinarySerialiser.getDoubleArray(ioBuffer))), // reader
                (obj, field) -> {
                    final DoubleArrayList retVal = (DoubleArrayList) field.getField().get(obj);
                    BinarySerialiser.put(ioBuffer, field.getFieldName(), retVal.elements(),
                            new int[] { retVal.size() });
                }, // writer
                DoubleArrayList.class));

        serialiser.addClassDefinition(new IoBufferFieldSerialiser(ioBuffer, //
                (obj, field) -> {
                    final String dataSetType = ioBuffer.getString();
                    if (!DataSet.class.getName().equals(dataSetType)) {
                        throw new IllegalArgumentException("unknown DataSet type = " + dataSetType);
                    }
                    
                    field.getField().set(obj, DataSetSerialiser.readDataSetFromByteArray(ioBuffer));
                }, // reader
                (obj, field) -> {
                    final DataSet retVal = (DataSet) (field.getField() == null ? obj : field.getField().get(obj));
                    final long sizeMarkerStart = BinarySerialiser.putArrayHeader(ioBuffer, field.getFieldName(),
                            DataType.OTHER, new int[] { 1 }, 1);

                    ioBuffer.putString(DataSet.class.getName());
                    DataSetSerialiser.writeDataSetToByteArray(retVal, ioBuffer, false);
                    BinarySerialiser.adjustDataByteSizeBlock(ioBuffer, sizeMarkerStart);
                }, // writer
                DataSet.class));

        // List<AxisDescription> serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new FieldListAxisDescription(ioBuffer));

    }
}
