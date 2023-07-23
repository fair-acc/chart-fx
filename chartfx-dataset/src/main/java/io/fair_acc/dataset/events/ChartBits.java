package io.fair_acc.dataset.events;

import java.util.function.IntSupplier;

/**
 * @author ennerf
 */
public enum ChartBits implements IntSupplier {
    AxisLayout, // size needs to be evaluated (e.g. labels may be larger)
    AxisCanvas, // needs to be drawn
    AxisRange, // anything related to min/max, tick marks, etc.
    AxisTickFormatter, // tick label formatting
    AxisLabelText; // display name or units

    public static final int KnownMask = BitState.mask(ChartBits.values());
    public static final int AxisMask = BitState.mask(AxisLayout, AxisCanvas, AxisRange, AxisTickFormatter, AxisLabelText);

    public static StateListener printer() {
        return PRINTER;
    }

    public static StateListener printerWithStackTrace() {
        return STACK_TRACE_PRINTER;
    }

    private static final StateListener PRINTER = BitState.createDebugPrinter(false, values());
    private static final StateListener STACK_TRACE_PRINTER = BitState.createDebugPrinter(true, values());

    @Override
    public int getAsInt() {
        return bit;
    }

    public boolean isSet(int mask) {
        return BitState.isSet(mask, bit);
    }

    final int bit = 1 << ordinal();

}
