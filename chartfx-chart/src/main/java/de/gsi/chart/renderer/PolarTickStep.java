package de.gsi.chart.renderer;

public enum PolarTickStep {
    FIVE(5), FIFTEEN(15), THIRTY(30), FOURTY_FIVE(45), SIXTY(60), NINETY(90);

    private final double VALUE;

    PolarTickStep(final double VALUE) {
        this.VALUE = VALUE;
    }

    public double get() { return VALUE; }
}