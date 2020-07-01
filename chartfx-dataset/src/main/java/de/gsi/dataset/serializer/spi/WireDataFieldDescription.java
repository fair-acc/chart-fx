package de.gsi.dataset.serializer.spi;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.FieldDescription;

/**
 * Field header descriptor
 * 
 * @author rstein
 */
public class WireDataFieldDescription implements FieldDescription {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireDataFieldDescription.class);
    private final int hashCode;
    private final String fieldName;
    private final String fieldNameRelative;
    private final DataType dataType;
    private final int[] dimensions;
    private final long positionBuffer;
    private final long expectedNumberOfBytes;

    private Optional<FieldDescription> parent; // N.B. was 'final' TODO: investigate to keep last root once BinarySerialiser is refactored to a non-static class

    private final List<FieldDescription> children = new ArrayList<>();

    /**
     * Constructs new serializer field header
     * 
     * @param parent the optional parent field header (for cascaded objects)
     * @param fieldName the clear text field name description
     * @param dataType the data type of that field
     * @param dims array with length indicating the number-of-dimensions and indices the length of each dimension
     * @param positionBuffer the position from which the actual data can be parsed onwards
     * @param expectedNumberOfBytes the expected number of bytes to skip the data block
     */
    public WireDataFieldDescription(final WireDataFieldDescription parent, final String fieldName, final DataType dataType, final int[] dims, final long positionBuffer, final long expectedNumberOfBytes) {
        this.parent = parent == null ? Optional.empty() : Optional.of(parent);
        this.hashCode = fieldName.hashCode();
        this.fieldName = fieldName;
        this.dataType = dataType;
        dimensions = Arrays.copyOf(dims, dims.length);
        this.positionBuffer = positionBuffer;
        this.expectedNumberOfBytes = expectedNumberOfBytes;

        fieldNameRelative = this.parent.map(fieldDescription -> fieldDescription.getFieldNameRelative() + "." + fieldName).orElse(fieldName);
    }

    /**
     * Constructs new serializer field header
     * 
     * @param fieldName the clear text field name description
     * @param dataType the data type of that field
     * @param dims array with length indicating the number-of-dimensions and indices the length of each dimension
     * @param positionBuffer the position from which the actual data can be parsed onwards
     * @param expectedNumberOfBytes the expected number of bytes to skip the data block
     */
    public WireDataFieldDescription(final String fieldName, final DataType dataType, final int[] dims, final long positionBuffer, final long expectedNumberOfBytes) {
        this(null, fieldName, dataType, dims, positionBuffer, expectedNumberOfBytes);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldDescription)) {
            return false;
        }
        if (this.hashCode() != obj.hashCode()) {
            return false;
        }

        return this.getFieldName().equals(((FieldDescription) obj).getFieldName());
    }

    @Override
    public Optional<FieldDescription> findChildField(String fieldName) {
        final int hashCode = fieldName.hashCode();
        //return fieldDescriptionList.stream().filter(p -> p.hashCode() == hashCode && p.getFieldName().equals(fieldName)).findFirst();
        for (int i = 0; i < children.size(); i++) {
            final FieldDescription field = children.get(i);
            final String name = field.getFieldName();
            if (name == fieldName) { // NOPMD early return if the same String object reference
                return Optional.of(field);
            }
            if (field.hashCode() == hashCode && name.equals(fieldName)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<FieldDescription> getChildren() {
        return children;
    }

    @Override
    public long getDataBufferPosition() {
        return positionBuffer;
    }

    @Override
    public int getDataDimension() {
        return dimensions.length;
    }

    @Override
    public int[] getDataDimensions() {
        return dimensions;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public long getExpectedNumberOfDataBytes() {
        return expectedNumberOfBytes;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldNameRelative() {
        return fieldNameRelative;
    }

    @Override
    public Optional<FieldDescription> getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public void printFieldStructure() {
        if (parent.isPresent()) {
            LOGGER.atInfo().addArgument(parent.get()).log("FielHeader structure (parent: {}):");
            printFieldStructure(this, 0);
        } else {
            LOGGER.atInfo().log("FielHeader structure (no parent):");
        }
        printFieldStructure(this, 0);
    }

    @Override
    public String toString() {
        if ((dimensions.length == 1) && (dimensions[0] == 1)) {
            return String.format("[fieldName=%s, fieldType=%s]", fieldName, dataType.getAsString());
        }

        final StringBuilder builder = new StringBuilder(27);
        builder.append("[fieldName=").append(fieldName).append(", fieldType=").append(dataType.getAsString()).append('[');
        for (int i = 0; i < dimensions.length; i++) {
            builder.append(dimensions[i]);
            if (i < (dimensions.length - 1)) {
                builder.append(',');
            }
        }
        builder.append("]]");
        return builder.toString();
    }

    @Override
    public Class<?> getType() {
        return dataType.getClassTypes().get(0);
    }

    void setParent(final WireDataFieldDescription parent) { // NOPMD - explicitly package private -> TODO: will be refactored
        this.parent = Optional.of(parent);
    }

    protected static void printFieldStructure(final FieldDescription field, final int recursionLevel) {
        final String mspace = spaces((recursionLevel) *indentationNumerOfSpace);
        LOGGER.atInfo().addArgument(mspace).addArgument(field.toString()).log("{}{}");
        field.getChildren().forEach(f -> printFieldStructure(f, recursionLevel + 1));
    }

    private static String spaces(final int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }
}