package de.gsi.dataset.serializer;

import java.util.List;
import java.util.Optional;

public interface FieldDescription {
    int INDENTATION_NUMER_OF_SPACE = 4;

    Optional<FieldDescription> findChildField(String fieldName);

    List<FieldDescription> getChildren();

    long getDataSize();

    long getDataStartOffset();

    DataType getDataType();

    String getFieldName();

    String getFieldNameRelative();

    FieldDescription getParent();

    Class<?> getType();

    void printFieldStructure();
}
