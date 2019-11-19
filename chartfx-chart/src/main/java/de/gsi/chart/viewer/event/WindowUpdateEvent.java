package de.gsi.chart.viewer.event;

import de.gsi.dataset.event.EventSource;
import de.gsi.dataset.event.UpdateEvent;

/**
 * Event emitted when a DataViewWindow state changes
 *
 * @author rstein
 */
public class WindowUpdateEvent extends UpdateEvent {
    private static final long serialVersionUID = -3505688662889761060L;
    private final Type type;

    /**
     * generates new update event
     *
     * @param source the class issuing the event
     */
    public WindowUpdateEvent(final EventSource source) {
        super(source, null, null);
        type = Type.UNSPECIFIED;
    }

    /**
     * generates new update event
     *
     * @param source the class issuing the event
     * @param msg a customised message to be passed along (e.g. for debugging)
     * @param payload a customised user pay-load to be passed to the listener
     * @param type WindowUpdateEvent event type
     */
    public WindowUpdateEvent(final EventSource source, final String msg, final Object payload, final Type type) {
        super(source, msg, payload);
        this.type = type;
    }

    /**
     * generates new update event
     *
     * @param source the class issuing the event
     * @param msg a customised message to be passed along (e.g. for debugging)
     * @param type WindowUpdateEvent event type
     */
    public WindowUpdateEvent(final EventSource source, final String msg, final Type type) {
        super(source, msg, null);
        this.type = type;
    }

    /**
     * generates new update event
     *
     * @param source the class issuing the event
     * @param type WindowUpdateEvent event type
     */
    public WindowUpdateEvent(final EventSource source, final Type type) {
        super(source, null, null);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        UNSPECIFIED,
        WINDOW_MINIMISING,
        WINDOW_MINIMISED,
        WINDOW_RESTORED,
        WINDOW_RESTORING,
        WINDOW_DETACHING,
        WINDOW_DETACHED,
        WINDOW_MAXIMISING,
        WINDOW_MAXIMISED,
        WINDOW_CLOSING,
        WINDOW_CLOSED;
    }
}
