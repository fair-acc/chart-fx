package de.gsi.dataset.event;

/**
 * UpdatedMetaDataEvent class that is passed along the notification performed by the {@code EventSource} class. The
 * class is intended to be further extended by named derivatives to allow for context-based event filters.
 * 
 * @see EventSource for details
 * 
 * @author rstein
 *
 */
public class UpdatedMetaDataEvent extends UpdateEvent {
    private static final long serialVersionUID = 233444066954702782L;

    /**
     * generates new update event
     * 
     * @param source the class issuing the event
     */
    public UpdatedMetaDataEvent(final EventSource source) {
        super(source, null, null);
    }

    /**
     * generates new update event
     * 
     * @param source the class issuing the event
     * @param msg a customised message to be passed along (e.g. for debugging)
     */
    public UpdatedMetaDataEvent(final EventSource source, final String msg) {
        super(source, msg, null);
    }

    /**
     * generates new update event
     * 
     * @param source the class issuing the event
     * @param msg a customised message to be passed along (e.g. for debugging)
     * @param payload a customised user pay-load to be passed to the listener
     */
    public UpdatedMetaDataEvent(final EventSource source, final String msg, final Object payload) {
        super(source, msg, payload);
    }
}