package de.gsi.dataset.event;

/**
 * RemovedDataEvent class that is passed along the notification performed by the
 * {@code EventSource} class. The class is intended to be further extended by
 * named derivatives to allow for context-based event filters.
 * 
 * @see EventSource for details
 * 
 * @author rstein
 *
 */
public class RemovedDataEvent extends UpdatedDataEvent {
    private static final long serialVersionUID = 6539189762419952854L;

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     */
    public RemovedDataEvent(final EventSource source) {
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
    public RemovedDataEvent(final EventSource source, final String msg) {
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
    public RemovedDataEvent(final EventSource source, final String msg, final Object payload) {
        super(source, msg, payload);
    }

}