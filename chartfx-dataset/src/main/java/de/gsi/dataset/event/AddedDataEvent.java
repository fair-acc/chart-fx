package de.gsi.dataset.event;

/**
 * AddedDataEvent class that is passed along the notification performed by the
 * {@code EventSource} class. The class is intended to be further extended by
 * named derivatives to allow for context-based event filters.
 * 
 * @see EventSource for details
 * 
 * @author rstein
 *
 */
public class AddedDataEvent extends UpdatedDataEvent {
    private static final long serialVersionUID = 1893124043479588248L;

    /**
     * generates new update event
     * @param source the class issuing the event
     */
    public AddedDataEvent(final EventSource source) {
        super(source, null, null);
    }
    
    /**
     * generates new update event
     * @param source the class issuing the event
     * @param msg a customised message to be passed along (e.g. for debugging)    
     */
    public AddedDataEvent(final EventSource source, final String msg) {
        super(source, msg, null);
    }

    /**
     * generates new update event
     * @param source the class issuing the event
     * @param msg a customised message to be passed along (e.g. for debugging)
     * @param payload a customised user pay-load to be passed to the listener
     */
    public AddedDataEvent(final EventSource source, final String msg, final Object payload) {
        super(source, msg, payload);
    }
}
