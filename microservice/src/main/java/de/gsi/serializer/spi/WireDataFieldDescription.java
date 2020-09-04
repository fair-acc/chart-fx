package de.gsi.serializer.spi;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.serializer.DataType;
import de.gsi.serializer.FieldDescription;
import de.gsi.serializer.IoSerialiser;
import de.gsi.serializer.utils.ClassUtils;

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
    private final int fieldStart;
    private final int fieldDataStart;
    private final int dataStartOffset;
    // local references to source buffer needed for parsing
    private final IoSerialiser ioSerialiser;
    private String fieldUnit;
    private String fieldDescription;
    private String fieldDirection;
    private List<String> fieldGroups;
    private int dataSize;

    /**
     * Constructs new serializer field header
     *
     * @param source the referenced IoBuffer (if any)
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
    public WireDataFieldDescription(final IoSerialiser source, final FieldDescription parent, final int fieldNameHashCode, final String fieldName, final DataType dataType, //
            final int fieldStart, final int dataStartOffset, final int dataSize) {
        ioSerialiser = source;
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
    public FieldDescription findChildField(final String fieldName) {
        return findChildField(fieldName.hashCode(), fieldName);
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

    public void setDataSize(final int size) {
        dataSize = size;
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

    public void setFieldDescription(final String fieldDescription) {
        this.fieldDescription = fieldDescription;
    }

    @Override
    public String getFieldDirection() {
        return fieldDirection;
    }

    public void setFieldDirection(final String fieldDirection) {
        this.fieldDirection = fieldDirection;
    }

    @Override
    public List<String> getFieldGroups() {
        return fieldGroups;
    }

    public void setFieldGroups(final List<String> fieldGroups) {
        this.fieldGroups = fieldGroups;
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

    public void setFieldUnit(final String fieldUnit) {
        this.fieldUnit = fieldUnit;
    }

    /**
     * @return raw ioSerialiser reference this field was retrieved with the position in the underlying IoBuffer at the to be read field
     * N.B. this is a safe convenience method and not performance optimised
     */
    public Object data(DataType... overwriteType) {
        ioSerialiser.setQueryFieldName(fieldName, fieldDataStart);
        switch (overwriteType.length == 0 ? this.dataType : overwriteType[0]) {
        case START_MARKER:
        case END_MARKER:
            return null;
        case BOOL:
            return ioSerialiser.getBoolean();
        case BYTE:
            return ioSerialiser.getByte();
        case SHORT:
            return ioSerialiser.getShort();
        case INT:
            return ioSerialiser.getInt();
        case LONG:
            return ioSerialiser.getLong();
        case FLOAT:
            return ioSerialiser.getFloat();
        case DOUBLE:
            return ioSerialiser.getDouble();
        case CHAR:
            return ioSerialiser.getChar();
        case STRING:
            return ioSerialiser.getString();
        case BOOL_ARRAY:
            return ioSerialiser.getBooleanArray();
        case BYTE_ARRAY:
            return ioSerialiser.getByteArray();
        case SHORT_ARRAY:
            return ioSerialiser.getShortArray();
        case INT_ARRAY:
            return ioSerialiser.getIntArray();
        case LONG_ARRAY:
            return ioSerialiser.getLongArray();
        case FLOAT_ARRAY:
            return ioSerialiser.getFloatArray();
        case DOUBLE_ARRAY:
            return ioSerialiser.getDoubleArray();
        case CHAR_ARRAY:
            return ioSerialiser.getCharArray();
        case STRING_ARRAY:
            return ioSerialiser.getStringArray();
        case ENUM:
            return ioSerialiser.getEnum(null);
        case LIST:
            return ioSerialiser.getList(null, null);
        case MAP:
            return ioSerialiser.getMap(null, null);
        case QUEUE:
            return ioSerialiser.getQueue(null, null);
        case SET:
            return ioSerialiser.getSet(null, null);
        case COLLECTION:
            return ioSerialiser.getCollection(null, null);
        case OTHER:
            return ioSerialiser.getCustomData(null);
        default:
            throw new IllegalStateException("unknown dataType = " + dataType);
        }
    }

    /**
     * @return raw ioSerialiser reference this field was retrieved from w/o changing the position in the underlying IoBuffer
     */
    public IoSerialiser getIoSerialiser() {
        return ioSerialiser;
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
    public boolean isAnnotationPresent() {
        return fieldUnit != null || fieldDescription != null || fieldDirection != null || (fieldGroups != null && !fieldGroups.isEmpty());
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