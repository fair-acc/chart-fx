package io.fair_acc.dataset.events;

import java.util.function.IntSupplier;

/**
 * @author ennerf
 */
public enum ChartBits implements IntSupplier {
    AxisCanvas,
    AxisLayout,
    AxisTransform,
    AxisTickFormatter,
    AxisLabelText;

    @Override
    public int getAsInt() {
        return bit;
    }

    public boolean isSet(int mask) {
        return (bit & mask) != 0;
    }

    private int clear(int mask) {
        return mask & ~bit;
    }

    public static StateListener printer() {
        return (source, bits) -> System.out.println(toString(source, bits));
    }

    public static String toString(BitState bitState, int bits) {
        StringBuilder builder = new StringBuilder();
        builder.append(bitState.getSource()).append(" ");
        if(bits == 0) {
            return builder.append("clean").toString();
        }
        builder.append("dirty[");
        for (ChartBits bit : knownBits) {
            if(bit.isSet(bits)) {
                builder.append(bit.name()).append(", ");
                bits = bit.clear(bits);
            }
        }
        if (bits != 0) {
            builder.append("UNKNOWN, ");
        }
        builder.setLength(builder.length() - 2);
        return builder.append("]").toString();
    }

    final int bit = 1 << ordinal();

    private static final ChartBits[] knownBits = ChartBits.values();
    public static final int ANY = BitState.mask(knownBits);

}
