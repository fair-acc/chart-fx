package de.gsi.dataset.serializer.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.gsi.dataset.serializer.DataType;

/**
 * Field header descriptor
 * 
 * @author rstein
 */
public class FieldHeader {
	private final String fieldName;
	private final DataType dataType;
	private final int[] dimensions;
	private final long positionBuffer;
	private final long expectedNumberOfBytes;

	private final Optional<FieldHeader> parent;

	private final List<FieldHeader> children = new ArrayList<>();

	/**
	 * Constructs new serializer field header
	 * 
	 * @param parent                the optional parent field header (for cascaded
	 *                              objects)
	 * @param fieldName             the clear text field name description
	 * @param dataType              the data type of that field
	 * @param dims                  array with length indicating the
	 *                              number-of-dimensions and indices the length of
	 *                              each dimension
	 * @param positionBuffer        the position from which the actual data can be
	 *                              parsed onwards
	 * @param expectedNumberOfBytes the expected number of bytes to skip the data
	 *                              block
	 */
	public FieldHeader(final FieldHeader parent, final String fieldName, final DataType dataType, final int[] dims,
			final long positionBuffer, final long expectedNumberOfBytes) {
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
	 * @param fieldName             the clear text field name description
	 * @param dataType              the data type of that field
	 * @param dims                  array with length indicating the
	 *                              number-of-dimensions and indices the length of
	 *                              each dimension
	 * @param positionBuffer        the position from which the actual data can be
	 *                              parsed onwards
	 * @param expectedNumberOfBytes the expected number of bytes to skip the data
	 *                              block
	 */
	public FieldHeader(final String fieldName, final DataType dataType, final int[] dims, final long positionBuffer,
			final long expectedNumberOfBytes) {
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
	
	@Override
	public String toString() {
		if ((dimensions.length == 1) && (dimensions[0] == 1)) {
			return String.format("[fieldName=%s, fieldType=%s]", fieldName, dataType.getAsString());
		}

		final StringBuilder builder = new StringBuilder(27);
		builder.append("[fieldName=").append(fieldName).append(", fieldType=").append(dataType.getAsString())
				.append('[');
		for (int i = 0; i < dimensions.length; i++) {
			builder.append(dimensions[i]);
			if (i < (dimensions.length - 1)) {
				builder.append(',');
			}
		}
		builder.append("]]");
		return builder.toString();
	}

	public static Optional<FieldHeader> findHeaderFor(final List<FieldHeader> fieldHeaderList, final String fieldName) {
		return fieldHeaderList.stream().filter(p -> p.getFieldName().equals(fieldName)).findFirst();
	}
}