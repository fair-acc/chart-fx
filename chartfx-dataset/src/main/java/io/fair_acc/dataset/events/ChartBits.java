package io.fair_acc.dataset.events;

import java.util.function.IntSupplier;

/**
 * @author ennerf
 */
public enum ChartBits implements IntSupplier {
    Layout,
    Canvas,
    AxisSide,
    AxisRangeChanged;

    public int getBit() {
        return bit;
    }

    @Override
    public int getAsInt() {
        return getBit();
    }

    final int bit = 1 << ordinal();

    public static final int ANY = BitState.mask(ChartBits.values());

}
