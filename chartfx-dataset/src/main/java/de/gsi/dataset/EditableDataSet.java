package de.gsi.dataset;

/**
 * TODO: bulk methods?
 * @author rstein
 *
 */
public interface EditableDataSet extends DataSet {
    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param newValue new data point coordinate
     * @return itself (fluent design)
     */
    EditableDataSet add(final int index, final double... newValue);

    /**
     * 
     * @return edit constraints for data set
     */
    EditConstraints getEditConstraints();

    /**
     * remove point from data set
     *
     * @param index data point which should be removed
     * @return itself (fluent design)
     */
    EditableDataSet remove(final int index);

    /**
     * modify point in the the data set
     *
     * @param index data point index at which the new data point should be added
     * @param newValue new data point coordinate
     * @return itself (fluent design)
     */
    EditableDataSet set(final int index, final double... newValue);

    /**
     * 
     * @param constraints new edit constraints
     * @return itself (fluent design)
     */
    EditableDataSet setEditConstraints(final EditConstraints constraints);

    /**
     * @param name the new data set name
     * @return itself (fluent design)
     */
    EditableDataSet setName(final String name);
}
