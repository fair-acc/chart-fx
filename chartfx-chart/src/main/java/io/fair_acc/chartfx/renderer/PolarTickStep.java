package io.fair_acc.chartfx.renderer;

public enum PolarTickStep {
    FIVE(5),
    FIFTEEN(15),
    THIRTY(30),
    FOURTY_FIVE(45),
    SIXTY(60),
    NINETY(90);

    private final double value;

    PolarTickStep(final double value) {
        this.value = value;
    }

    public double get() {
        return value;
    }
}
