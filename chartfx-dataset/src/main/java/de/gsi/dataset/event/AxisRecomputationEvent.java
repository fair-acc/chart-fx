package de.gsi.dataset.event;

/**
 * Event issued when only if the Axis is undefined and needs to be recomputed.
 *
 * @see de.gsi.dataset.AxisDescription
 *
 * @author rstein
 */
public class AxisRecomputationEvent extends AxisChangeEvent {
    private static final long serialVersionUID = -7285890268185312226L;

    public AxisRecomputationEvent(EventSource evtSource) {
        super(evtSource);
    }

    public AxisRecomputationEvent(EventSource evtSource, int dimension) {
        super(evtSource, dimension);
    }

    public AxisRecomputationEvent(EventSource evtSource, String msg, int dimension) {
        super(evtSource, msg, dimension);
    }

    public AxisRecomputationEvent(EventSource evtSource, String msg, Object obj, int dimension) {
        super(evtSource, msg, obj, dimension);
    }
}
