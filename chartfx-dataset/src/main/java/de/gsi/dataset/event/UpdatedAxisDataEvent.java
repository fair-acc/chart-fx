package de.gsi.dataset.event;

/**
 * UpdatedAxisDataEvent class that is passed along the notification performed by the
 * {@code EventSource} class.
 * Sent when the axis name or unit is changed (usually via Dataset.setAxisName(...)).
 * 
 * @see EventSource Event Source for details about DataSet Event implementation
 * @see de.gsi.dataset.EditableDataSet#setAxisDescription EditableDataSet's setAxisDescription for details about Axis Data in the Dataset
 * 
 * @author akrimm
 *
 */
public class UpdatedAxisDataEvent  extends UpdateEvent {
    private static final long serialVersionUID = 233444066954702782L;
    
    private int dim;

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     * @param dim for which dimension the Axis data was changed
     */
    public UpdatedAxisDataEvent(final EventSource source, int dim) {
        super(source, null, null);
        this.dim = dim;
    }

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     * @param msg
     *            a customised message to be passed along (e.g. for debugging)
     * @param dim for which dimension the Axis data was changed
     */
    public UpdatedAxisDataEvent(final EventSource source, final String msg, int dim) {
        super(source, msg, null);
        this.dim = dim;
    }
    
    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     * @param msg
     *            a customised message to be passed along (e.g. for debugging)
     * @param payload
     *            a customised user pay-load to be passed to the listener
     * @param dim for which dimension the Axis data was changed
     */
    public UpdatedAxisDataEvent(final EventSource source, final String msg, final Object payload, int dim) {
        super(source, msg, payload);
        this.dim = dim;
    }

    public int getDimension() {
        return dim;
    }
}