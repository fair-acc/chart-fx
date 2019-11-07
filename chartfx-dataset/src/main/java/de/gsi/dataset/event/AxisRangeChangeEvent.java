package de.gsi.dataset.event;

/**
 * Event issued when only the axis range changed
 * 
 * @see de.gsi.dataset.AxisDescription
 * 
 * @author rstein
 */
public class AxisRangeChangeEvent extends AxisChangeEvent {
    private static final long serialVersionUID = -7285890268185312226L;

    public AxisRangeChangeEvent(EventSource evtSource) {
        super(evtSource);
    }

    public AxisRangeChangeEvent(EventSource evtSource, int dimension) {
        super(evtSource, dimension);
    }

    public AxisRangeChangeEvent(EventSource evtSource, String msg, int dimension) {
        super(evtSource, msg, dimension);
    }

    public AxisRangeChangeEvent(EventSource evtSource, String msg, Object obj, int dimension) {
        super(evtSource, msg, obj, dimension);
    }
}
