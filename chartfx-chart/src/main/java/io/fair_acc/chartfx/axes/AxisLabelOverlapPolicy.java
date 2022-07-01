package io.fair_acc.chartfx.axes;

public enum AxisLabelOverlapPolicy {
    /** omen-est-omen allow overlap */
    DO_NOTHING,
    /** make every other label invisible (ie. gain a factor 2 margin */
    SKIP_ALT,
    /** narrow font where possible */
    NARROW_FONT,
    /**
     * shift every second label by one label height/width (N.B. especially useful for category axes
     */
    SHIFT_ALT,
    /**
     * shift every second label by one label height/width (N.B. especially useful for category axes
     */
    FORCED_SHIFT_ALT
}
