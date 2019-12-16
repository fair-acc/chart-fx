package de.gsi.chart.viewer.event;

import de.gsi.dataset.event.EventSource;

/**
 * Event issued after DataViewWindow has been detached
 *
 * @see de.gsi.chart.viewer.DataViewWindow
 * @see de.gsi.chart.viewer.event.WindowUpdateEvent
 * @author rstein
 */
public class WindowDetachedEvent extends WindowUpdateEvent {
    private static final long serialVersionUID = 2846294413532027952L;

    public WindowDetachedEvent(final EventSource evtSource) {
        super(evtSource, Type.WINDOW_DETACHED);
    }

    public WindowDetachedEvent(final EventSource evtSource, final String msg) {
        super(evtSource, msg, Type.WINDOW_DETACHED);
    }

    public WindowDetachedEvent(final EventSource evtSource, final String msg, final Object obj) {
        super(evtSource, msg, obj, Type.WINDOW_DETACHED);
    }
}
