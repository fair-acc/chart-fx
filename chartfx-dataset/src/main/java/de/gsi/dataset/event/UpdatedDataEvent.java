package de.gsi.dataset.event;

/**
 * UpdatedDataEvent class that is passed along the notification performed by the
 * {@code EventSource} class. The class is intended to be further extended by
 * named derivatives to allow for context-based event filters.
 * 
 * @see EventSource for details
 * 
 * @author rstein
 *
 */
public class UpdatedDataEvent extends UpdateEvent {
    private static final long serialVersionUID = 2906468013676213645L;

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     */
    public UpdatedDataEvent(final EventSource source) {
        super(source, null, null);
    }

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     * @param msg
     *            a customised message to be passed along (e.g. for debugging)
     */
    public UpdatedDataEvent(final EventSource source, final String msg) {
        super(source, msg, null);
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
     */
    public UpdatedDataEvent(final EventSource source, final String msg, final Object payload) {
        super(source, msg, payload);
    }
}
