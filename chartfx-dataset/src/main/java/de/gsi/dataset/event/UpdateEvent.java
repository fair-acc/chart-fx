package de.gsi.dataset.event;

import java.util.EventObject;

/**
 * Event class that is passed along the notification performed by the
 * {@code EventSource} class. The class is intended to be further extended by
 * named derivatives to allow for context-based event filters.
 * 
 * @see EventSource for details
 * 
 * @author rstein
 *
 */
public class UpdateEvent extends EventObject {
    private static final long serialVersionUID = -3097725478448868303L;
    private final String msg;
    private transient Object payload;

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     */
    public UpdateEvent(final EventSource source) {
        this(source, null, null);
    }

    /**
     * generates new update event
     * 
     * @param source
     *            the class issuing the event
     * @param msg
     *            a customised message to be passed along (e.g. for debugging)
     */
    public UpdateEvent(final EventSource source, final String msg) {
        this(source, msg, null);
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
    public UpdateEvent(final EventSource source, final String msg, final Object payload) {
        super(source);
        this.msg = msg;
        this.payload = payload;
    }

    /**
     * 
     * @return a customised message to be passed along (e.g. for debugging)
     */
    public String getMessage() {
        return msg;
    }

    /**
     * 
     * @return a customised user pay-load to be passed to the listener
     */
    public Object getPayLoad() {
        return payload;
    }

}
