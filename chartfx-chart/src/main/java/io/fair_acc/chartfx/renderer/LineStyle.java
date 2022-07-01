package io.fair_acc.chartfx.renderer;

public enum LineStyle {
    NONE,
    NORMAL,
    AREA,
    ZERO_ORDER_HOLDER,
    STAIR_CASE, // aka. ZERO-ORDER_HOLDER
    HISTOGRAM,
    HISTOGRAM_FILLED, // similar to area but enclosing histogram style-type bars
    BEZIER_CURVE // smooth Bezier-type curve - beware this can be slow for many data points
}
