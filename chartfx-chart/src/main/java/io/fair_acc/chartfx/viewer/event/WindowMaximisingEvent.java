package io.fair_acc.chartfx.viewer.event;

import io.fair_acc.dataset.event.EventSource;

/**
 * Event issued before DataViewWindow is being maximised
 *
 * @see io.fair_acc.chartfx.viewer.DataViewWindow
 * @see io.fair_acc.chartfx.viewer.event.WindowUpdateEvent
 * @author rstein
 */
public class WindowMaximisingEvent extends WindowUpdateEvent {
    private static final long serialVersionUID = 2846294413532027952L;

    public WindowMaximisingEvent(final EventSource evtSource) {
        super(evtSource, Type.WINDOW_MAXIMISING);
    }

    public WindowMaximisingEvent(final EventSource evtSource, final String msg) {
        super(evtSource, msg, Type.WINDOW_MAXIMISING);
    }

    public WindowMaximisingEvent(final EventSource evtSource, final String msg, final Object obj) {
        super(evtSource, msg, obj, Type.WINDOW_MAXIMISING);
    }
}
