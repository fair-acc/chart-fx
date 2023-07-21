package io.fair_acc.dataset.events;

import java.util.function.IntSupplier;

/**
 * @author ennerf
 */
public enum ChartBits implements IntSupplier {
    AxisCanvas,
    AxisLayout,
    AxisTransform,
    AxisTickLocation,
    AxisTickFormatter,
    AxisLabelText;

    @Override
    public int getAsInt() {
        return bit;
    }

    public boolean isSet(int mask) {
        return (bit & mask) != 0;
    }

    final int bit = 1 << ordinal();

    public static final int ANY = BitState.mask(ChartBits.values());

}
