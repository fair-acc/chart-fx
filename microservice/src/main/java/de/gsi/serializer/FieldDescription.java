package de.gsi.serializer;

import java.lang.reflect.Type;
import java.util.List;

import de.gsi.serializer.utils.ClassUtils;

public interface FieldDescription {
    boolean isAnnotationPresent();

    FieldDescription findChildField(String fieldName);

    FieldDescription findChildField(final int fieldNameHashCode, final String fieldName);

    List<FieldDescription> getChildren();

    /**
     * @return the data size in bytes stored after the field header
     */
    int getDataSize();

    /**
     * @return the offset in bytes from the field start position until the first data object can be read.
     * (N.B. equals to 'getFieldstart() + getDataOffset()', the data ends at 'getDataStartOffset() + getDataSize()'
     */
    int getDataStartOffset();

    /**
     * @return the buffer byte position from where the first data object can be read
     */
    int getDataStartPosition();

    /**
     * @return the stored data type, see {@link DataType} for details
     */
    DataType getDataType();

    /**
     * @return optional meta data tag describing the purpose of this data field (N.B. can be empty String)
     */
    String getFieldDescription();

    /**
     * Return optional meta data tag describing the 'direction' of this data field.
     * The information encodes the source servicedevelopers intend to the receiving user whether the field can be, for example,
     * modified (get/set), set-only, or read-only, or attach any other similar information. Encoding/interpretation is
     * left ad-lib to the source service developer.
     *
     * @return optional meta data (N.B. can be empty String).
     */
    String getFieldDirection();

    /**
     * @return optional meta data tag describing the group/set this data field belongs to (N.B. can be empty String, corresponding to 'all')
     */
    List<String> getFieldGroups();

    /**
     * @return the data field's name
     */
    String getFieldName();

    /**
     * @return the data field name's hashcode (N.B. used for faster identification of the field)
     */
    int getFieldNameHashCode();

    /**
     *
     * @return buffer position in byte where the data field header starts
     */
    int getFieldStart();

    /**
     * @return optional meta data tag describing the field's SI unit or similar (N.B. can be empty String)
     */
    String getFieldUnit();

    /**
     * @return for a hierarchical/nested data structure refers to the parent this field belongs to (N.B. can be null if there isn't a parent, e.g. for a root element)
     */
    FieldDescription getParent();

    Type getType();

    /**
     * Prints the class field structure to the logging output for diagnostics purposes starting from this element as a root.

     * N.B. regarding formatting/parsing
     * The indentation depth is controlled via {@link ClassUtils#setIndentationNumberOfSpace}.
     * The max recursion depth during the class structure parsing is controlled via {@link ClassUtils#setMaxRecursionDepth}.
     */
    void printFieldStructure();
}
