package de.gsi.dataset;

/**
 * @author rstein
 *
 */
public interface EditableDataSet extends DataSet {

	/**
	 * @param name the new data set name
	 * @return itself (fluent design)
	 */
	EditableDataSet setName(final String name);

	/**
	 * modify point in the the data set
	 *
	 * @param index data point index at which the new data point should be added
	 * @param x     horizontal coordinate of the new data point
	 * @param y     vertical coordinate of the new data point
	 * @return itself (fluent design)
	 */
	EditableDataSet set(final int index, final double x, final double y);

	/**
	 * add point to the data set
	 *
	 * @param index data point index at which the new data point should be added
	 * @param x     horizontal coordinate of the new data point
	 * @param y     vertical coordinate of the new data point
	 * @return itself (fluent design)
	 */
	EditableDataSet add(final int index, final double x, final double y);

	/**
	 * remove point from data set
	 *
	 * @param index data point which should be removed
	 * @return itself (fluent design)
	 */
	EditableDataSet remove(final int index);

	/**
	 * 
	 * @return edit constraints for data set
	 */
	EditConstraints getEditConstraints();

	/**
	 * 
	 * @param constraints new edit constraints
	 * @return itself (fluent design)
	 */
	EditableDataSet setEditConstraints(final EditConstraints constraints);
    
    /**
     * Set the name and optionally the unit for the i-th axis.
     * All axis names and units default to the empty String.
     * @param dim 0: X-Axis, 1: Y-Axis, ...
     * @param name Axis name (shown as axis label)
     * @param unit optional: Axis unit (shown in axis label)
     */
    void setAxisDescription(int dim, String name, String... unit);

}
