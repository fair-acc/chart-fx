package de.gsi.chart.viewer.event;

import de.gsi.dataset.event.EventSource;

/**
 * Event issued after DataViewWindow has been maximised
 *
 * @see de.gsi.chart.viewer.DataViewWindow
 * @see de.gsi.chart.viewer.event.WindowUpdateEvent
 * @author rstein
 */
public class WindowMaximisedEvent extends WindowUpdateEvent {
    private static final long serialVersionUID = 2846294413532027952L;

    public WindowMaximisedEvent(final EventSource evtSource) {
        super(evtSource, Type.WINDOW_MAXIMISED);
    }

    public WindowMaximisedEvent(final EventSource evtSource, final String msg) {
        super(evtSource, msg, Type.WINDOW_MAXIMISED);
    }

    public WindowMaximisedEvent(final EventSource evtSource, final String msg, final Object obj) {
        super(evtSource, msg, obj, Type.WINDOW_MAXIMISED);
    }
}
