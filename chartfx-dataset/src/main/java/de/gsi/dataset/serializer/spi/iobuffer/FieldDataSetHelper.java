package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.FieldSerialiser;
import de.gsi.dataset.serializer.IoClassSerialiser;

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
                    io.put(field, retVal.elements(), 0, retVal.size());
                }, // writer
                DoubleArrayList.class));

        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) ->
                // short form: FieldSerialiser.this.getReturnObjectFunction().andThen(io, obj, field) -- not possible inside a constructor
                field.getField().set(obj, new DataSetSerialiser(io).readDataSetFromByteArray()), // reader
                (io, obj, field) -> new DataSetSerialiser(io).readDataSetFromByteArray(), // return object function
                (io, obj, field) -> {
                    final DataSet retVal = (DataSet) (field == null || field.getField() == null ? obj : field.getField().get(obj));
                    new DataSetSerialiser(io).writeDataSetToByteArray(retVal, false);
                }, // writer
                DataSet.class));

        // List<AxisDescription> serialiser mapper to IoBuffer
        serialiser.addClassDefinition(new FieldListAxisDescription());
    }
}
