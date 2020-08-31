package de.gsi.dataset.serializer.spi.iobuffer;

import de.gsi.dataset.serializer.*;
import de.gsi.dataset.serializer.spi.WireDataFieldDescription;
import de.gsi.dataset.spi.utils.*;

/**
 * helper class to register default serialiser for MultiArray types
 *
 * @author Alexander Krimm
 */
public class FieldMultiArrayHelper {
    private FieldMultiArrayHelper() {
        // utility class
    }

    public static <E> MultiArray<E> getMultiArray(final IoSerialiser serialiser, final MultiArray<E> dst, final DataType type) {
        final int[] dims = serialiser.getArraySizeDescriptor();
        int n = 1;
        for (int ni : dims) {
            n *= ni;
        }
        switch (type) {
            case BOOL_ARRAY:
                return (MultiArray<E>) MultiArrayBoolean.wrap(serialiser.getBuffer().getBooleanArray(dst == null ? null : (boolean[]) dst.elements(), n), dims);
            case BYTE_ARRAY:
                return (MultiArray<E>) MultiArrayByte.wrap(serialiser.getBuffer().getByteArray(dst == null ? null : (byte[]) dst.elements(), n), dims);
            case SHORT_ARRAY:
                return (MultiArray<E>) MultiArrayShort.wrap(serialiser.getBuffer().getShortArray(dst == null ? null : (short[]) dst.elements(), n), dims);
            case INT_ARRAY:
                return (MultiArray<E>) MultiArrayInt.wrap(serialiser.getBuffer().getIntArray(dst == null ? null : (int[]) dst.elements(), n), dims);
            case LONG_ARRAY:
                return (MultiArray<E>) MultiArrayLong.wrap(serialiser.getBuffer().getLongArray(dst == null ? null : (long[]) dst.elements(), n), dims);
            case FLOAT_ARRAY:
                return (MultiArray<E>) MultiArrayFloat.wrap(serialiser.getBuffer().getFloatArray(dst == null ? null : (float[]) dst.elements(), n), dims);
            case DOUBLE_ARRAY:
                return (MultiArray<E>) MultiArrayDouble.wrap(serialiser.getBuffer().getDoubleArray(dst == null ? null : (double[]) dst.elements(), n), dims);
            case CHAR_ARRAY:
                return (MultiArray<E>) MultiArrayChar.wrap(serialiser.getBuffer().getCharArray(dst == null ? null : (char[]) dst.elements(), n), dims);
            case STRING_ARRAY:
                return (MultiArray<E>) MultiArrayObject.wrap(serialiser.getBuffer().getStringArray(dst == null ? null : (String[]) dst.elements(), n), dims);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    public static <E> void put(final IoSerialiser serialiser, final String fieldName, final MultiArray<E> value) {
        if (value instanceof MultiArrayDouble) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.DOUBLE_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putDoubleArray(((MultiArrayDouble) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayFloat) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.FLOAT_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putFloatArray(((MultiArrayFloat) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayInt) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.INT_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putIntArray(((MultiArrayInt) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayLong) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.LONG_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putLongArray(((MultiArrayLong) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayShort) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.SHORT_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putShortArray(((MultiArrayShort) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayChar) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.CHAR_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putCharArray(((MultiArrayChar) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayByte) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.BYTE_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putByteArray(((MultiArrayByte) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else if (value instanceof MultiArrayObject) {
            final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldName, DataType.STRING_ARRAY);
            final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
            serialiser.getBuffer().putStringArray(((MultiArrayObject<String>) value).elements(), nElements);
            serialiser.updateDataEndMarker(fieldHeader);
        } else {
            throw new IllegalArgumentException("Illegal DataType for MultiArray");
        }
    }

    public static <E> void put(final IoSerialiser serialiser, final FieldDescription fieldDescription, final MultiArray<E> value) {
        final WireDataFieldDescription fieldHeader = serialiser.putFieldHeader(fieldDescription);
        final int nElements = serialiser.putArraySizeDescriptor(value.getDimensions());
        switch (fieldDescription.getDataType()) {
            case BOOL_ARRAY:
                serialiser.getBuffer().putBooleanArray(((MultiArrayBoolean) value).elements(), nElements);
                break;
            case BYTE_ARRAY:
                serialiser.getBuffer().putByteArray(((MultiArrayByte) value).elements(), nElements);
                break;
            case SHORT_ARRAY:
                serialiser.getBuffer().putShortArray(((MultiArrayShort) value).elements(), nElements);
                break;
            case INT_ARRAY:
                serialiser.getBuffer().putIntArray(((MultiArrayInt) value).elements(), nElements);
                break;
            case LONG_ARRAY:
                serialiser.getBuffer().putLongArray(((MultiArrayLong) value).elements(), nElements);
                break;
            case FLOAT_ARRAY:
                serialiser.getBuffer().putFloatArray(((MultiArrayFloat) value).elements(), nElements);
                break;
            case DOUBLE_ARRAY:
                serialiser.getBuffer().putDoubleArray(((MultiArrayDouble) value).elements(), nElements);
                break;
            case CHAR_ARRAY:
                serialiser.getBuffer().putCharArray(((MultiArrayChar) value).elements(), nElements);
                break;
            case STRING_ARRAY:
                serialiser.getBuffer().putStringArray(((MultiArrayObject<String>) value).elements(), nElements);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + fieldDescription.getDataType());
        }
        serialiser.updateDataEndMarker(fieldHeader);
    }

    /**
     * Registers default serialiser for MultiArray
     *
     * @param serialiser for which the field serialisers should be registered
     */
    public static void register(final IoClassSerialiser serialiser) {
        serialiser.addClassDefinition(new FieldSerialiser<>( //
                (io, obj, field) -> field.getField().set(obj, getMultiArray(io, (MultiArray<?>) field.getField().get(obj), field.getDataType())), // reader
                (io, obj, field) -> getMultiArray(io, (MultiArray<?>) ((field == null) ? obj : field.getField().get(obj)), DataType.DOUBLE_ARRAY), // return
                (io, obj, field) -> put(io, field, (MultiArray<?>) field.getField().get(obj)), // writer
                MultiArrayDouble.class));
    }
}
