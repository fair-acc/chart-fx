package de.gsi.dataset;

/**
 * @author rstein
 *
 */
public interface EditableDataSet extends DataSet {

    /**
     * @param name the new data set name
     */
    public void setName(String name);

    /**
     * modify point in the the data set
     *
     * @param index
     *            data point index at which the new data point should be added
     * @param x
     *            horizontal coordinate of the new data point
     * @param y
     *            vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    public EditableDataSet set(final int index, final double x, final double y);

    /**
     * add point to the data set
     *
     * @param index
     *            data point index at which the new data point should be added
     * @param x
     *            horizontal coordinate of the new data point
     * @param y
     *            vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    public EditableDataSet add(final int index, final double x, final double y);

    /**
     * remove point from data set
     *
     * @param index
     *            data point which should be removed
     * @return itself (fluent design)
     */
    public EditableDataSet remove(final int index);

    /**
     * 
     * @return edit constraints for data set
     */
    public EditConstraints getEditConstraints();
    
    /**
     * 
     * @param constraints new edit constraints
     * @return itself (fluent design)
     */
    public EditableDataSet setEditConstraints(EditConstraints constraints);
}
