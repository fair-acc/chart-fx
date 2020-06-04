package de.gsi.dataset.serializer.spi;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;

/**
 * Field header descriptor
 * 
 * @author rstein
 */
public class FieldHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FieldHeader.class);
    public static int indentationNumerOfSpace = 4;

    private final String fieldName;
    private final DataType dataType;
    private final int[] dimensions;
    private final long positionBuffer;
    private final long expectedNumberOfBytes;

    private Optional<FieldHeader> parent; // N.B. was 'final' TODO: investigate to keep last root once BinarySerialiser is refactored to a non-static class

    private final List<FieldHeader> children = new ArrayList<>();

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
    public FieldHeader(final FieldHeader parent, final String fieldName, final DataType dataType, final int[] dims, final long positionBuffer, final long expectedNumberOfBytes) {
        this.parent = parent == null ? Optional.empty() : Optional.of(parent);
        this.fieldName = fieldName;
        this.dataType = dataType;
        dimensions = Arrays.copyOf(dims, dims.length);
        this.positionBuffer = positionBuffer;
        this.expectedNumberOfBytes = expectedNumberOfBytes;
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
    public FieldHeader(final String fieldName, final DataType dataType, final int[] dims, final long positionBuffer, final long expectedNumberOfBytes) {
        this(null, fieldName, dataType, dims, positionBuffer, expectedNumberOfBytes);
    }

    public List<FieldHeader> getChildren() {
        return children;
    }

    public long getDataBufferPosition() {
        return positionBuffer;
    }

    public int getDataDimension() {
        return dimensions.length;
    }

    public int[] getDataDimensions() {
        return dimensions;
    }

    public DataType getDataType() {
        return dataType;
    }

    public long getExpectedNumberOfDataBytes() {
        return expectedNumberOfBytes;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Optional<FieldHeader> getParent() {
        return parent;
    }

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

    void setParent(final FieldHeader parent) { // NOPMD - explicitly package private -> TODO: will be refactored
        this.parent = Optional.of(parent);
    }

    public static Optional<FieldHeader> findHeaderFor(final List<FieldHeader> fieldHeaderList, final String fieldName) {
        return fieldHeaderList.stream().filter(p -> p.getFieldName().equals(fieldName)).findFirst();
    }

    protected static void printFieldStructure(final FieldHeader field, final int recursionLevel) {
        final String mspace = spaces((recursionLevel) *indentationNumerOfSpace);
        LOGGER.atInfo().addArgument(mspace).addArgument(field.toString()).log("{}{}");
        field.getChildren().stream().forEach(f -> printFieldStructure(f, recursionLevel + 1));
    }

    private static String spaces(final int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }
}