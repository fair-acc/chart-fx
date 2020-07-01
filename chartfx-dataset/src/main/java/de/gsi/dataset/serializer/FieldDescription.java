package de.gsi.dataset.serializer;

import java.util.List;
import java.util.Optional;

public interface FieldDescription {
    int indentationNumerOfSpace = 4;

    Optional<FieldDescription> findChildField(String fieldName);

    List<FieldDescription> getChildren();

    long getDataBufferPosition();

    int getDataDimension();

    int[] getDataDimensions();

    DataType getDataType();

    long getExpectedNumberOfDataBytes();

    String getFieldName();

    String getFieldNameRelative();

    Optional<FieldDescription> getParent();

    void printFieldStructure();

    Class<?> getType();
}
