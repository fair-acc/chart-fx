package de.gsi.serializer.spi.iobuffer;

import de.gsi.dataset.DataSet;
import de.gsi.serializer.FieldSerialiser;
import de.gsi.serializer.IoClassSerialiser;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public final class FieldDataSetHelper {
    private FieldDataSetHelper() {
        // utility class
    }

    /**
     * registers default DataSet interface and related helper methods
     * 
     * @param serialiser for which the field serialisers should be registered
     */
    public static void register(final IoClassSerialiser serialiser) {
        // DoubleArrayList serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, DoubleArrayList.wrap(io.getDoubleArray())), // reader
                (io, obj, field) -> DoubleArrayList.wrap(io.getDoubleArray()), // return
                (io, obj, field) -> {
                    final DoubleArrayList retVal = (DoubleArrayList) field.getField().get(obj);
                    io.put(field, retVal.elements(), retVal.size());
                }, // writer
                DoubleArrayList.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> {
                    // short form: FieldSerialiser.this.getReturnObjectFunction().andThen(io, obj, field) -- not possible inside a constructor
                    final DataSet origDataSet = (DataSet) field.getField().get(obj);
                    field.getField().set(obj, DataSetSerialiser.withIoSerialiser(io).read(origDataSet));
                }, // reader
                (io, obj, field) -> DataSetSerialiser.withIoSerialiser(io).read(), // return object function
                (io, obj, field) -> {
                    final DataSet origDataSet = (DataSet) (field == null || field.getField() == null ? obj : field.getField().get(obj));
                    DataSetSerialiser.withIoSerialiser(io).write(origDataSet, false);
                }, // writer
                DataSet.class));

        // List<AxisDescription> serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new FieldListAxisDescription());
    }
}
