package de.gsi.dataset.serializer.spi;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.DataType;
import de.gsi.dataset.serializer.FieldDescription;
import de.gsi.dataset.serializer.utils.ClassUtils;

/**
 * Field header descriptor
 * 
 * @author rstein
 */
public class WireDataFieldDescription implements FieldDescription {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireDataFieldDescription.class);
    private final String fieldName;
    private final int fieldNameHashCode;
    private final DataType dataType;
    private final List<FieldDescription> children = new ArrayList<>();
    private final FieldDescription parent;
    private String fieldUnit;
    private String fieldDescription;
    private String fieldDirection;
    private List<String> fieldGroups;
    private final int fieldStart;
    private final int fieldDataStart;
    private final int dataStartOffset;
    private int dataSize;

    /**
     * Constructs new serializer field header
     * 
     * @param parent the optional parent field header (for cascaded objects)
     * @param fieldNameHashCode the fairly-unique hash-code of the field name,
     *                         N.B. checked during 1st iteration against fieldName, if no collisions are present then
     *                         this check is being suppressed
     * @param fieldName the clear text field name description
     * @param dataType the data type of that field
     * @param fieldStart the absolute buffer position from which the field header can be parsed
     * @param dataStartOffset the position from which the actual data can be parsed onwards
     * @param dataSize the expected number of bytes to skip the data block
     */
    public WireDataFieldDescription(final FieldDescription parent, final int fieldNameHashCode, final String fieldName, final DataType dataType, //
            final int fieldStart, final int dataStartOffset, final int dataSize) {
        this.parent = parent;
        this.fieldNameHashCode = fieldNameHashCode;
        this.fieldName = fieldName;
        this.dataType = dataType;
        this.fieldStart = fieldStart;
        this.dataStartOffset = dataStartOffset;
        this.dataSize = dataSize;
        this.fieldDataStart = fieldStart + dataStartOffset;

        if (this.parent != null /*&& !this.parent.getChildren().contains(this)*/) {
            this.parent.getChildren().add(this);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldDescription)) {
            return false;
        }
        FieldDescription other = (FieldDescription) obj;
        if (this.getFieldNameHashCode() != other.getFieldNameHashCode()) {
            return false;
        }

        if (this.getDataType() != other.getDataType()) {
            return false;
        }

        return this.getFieldName().equals(other.getFieldName());
    }

    @Override
    public boolean isAnnotationPresent() {
        return fieldUnit != null || fieldDescription != null || fieldDirection != null || (fieldGroups != null && !fieldGroups.isEmpty());
    }

    @Override
    public FieldDescription findChildField(final int fieldNameHashCode, final String fieldName) {
        for (int i = 0; i < children.size(); i++) { //NOSONAR
            final FieldDescription field = children.get(i);
            final String name = field.getFieldName();
            if (name == fieldName) { //NOSONAR //NOPMD early return if the same String object reference
                return field;
            }
            if (field.hashCode() == fieldNameHashCode && name.equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    @Override
    public List<FieldDescription> getChildren() {
        return children;
    }

    @Override
    public int getDataSize() {
        return dataSize;
    }

    @Override
    public int getDataStartOffset() {
        return dataStartOffset;
    }

    @Override
    public int getDataStartPosition() {
        return fieldDataStart;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String getFieldDescription() {
        return fieldDescription;
    }

    @Override
    public String getFieldDirection() {
        return fieldDirection;
    }

    @Override
    public List<String> getFieldGroups() {
        return fieldGroups;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int getFieldNameHashCode() {
        return fieldNameHashCode;
    }

    @Override
    public int getFieldStart() {
        return fieldStart;
    }

    @Override
    public String getFieldUnit() {
        return fieldUnit;
    }

    @Override
    public FieldDescription getParent() {
        return parent;
    }

    @Override
    public Class<?> getType() {
        return dataType.getClassTypes().get(0);
    }

    @Override
    public int hashCode() {
        return fieldNameHashCode;
    }

    @Override
    public void printFieldStructure() {
        if (parent != null) {
            LOGGER.atInfo().addArgument(parent).log("FielHeader structure (parent: {}):");
            printFieldStructure(this, 0);
        } else {
            LOGGER.atInfo().log("FielHeader structure (no parent):");
        }
        printFieldStructure(this, 0);
    }

    public void setDataSize(final int size) {
        dataSize = size;
    }

    public void setFieldDescription(final String fieldDescription) {
        this.fieldDescription = fieldDescription;
    }

    public void setFieldDirection(final String fieldDirection) {
        this.fieldDirection = fieldDirection;
    }

    public void setFieldGroups(final List<String> fieldGroups) {
        this.fieldGroups = fieldGroups;
    }

    public void setFieldUnit(final String fieldUnit) {
        this.fieldUnit = fieldUnit;
    }

    @Override
    public String toString() {
        return String.format("[fieldName=%s, fieldType=%s]", fieldName, dataType.getAsString());
    }

    protected static void printFieldStructure(final FieldDescription field, final int recursionLevel) {
        final String mspace = spaces((recursionLevel) *ClassUtils.getIndentationNumberOfSpace());
        LOGGER.atInfo().addArgument(mspace).addArgument(field.toString()).log("{}{}");
        if (field.isAnnotationPresent()) {
            LOGGER.atInfo().addArgument(mspace) //
                    .addArgument(field.getFieldUnit())
                    .addArgument(field.getFieldDescription())
                    .addArgument(field.getFieldDirection())
                    .addArgument(field.getFieldGroups())
                    .log("{}     <meta-info: unit:'{}' description:'{}' direction:'{}' groups:'{}'>");
        }
        field.getChildren().forEach(f -> printFieldStructure(f, recursionLevel + 1));
    }

    private static String spaces(final int spaces) {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }
}