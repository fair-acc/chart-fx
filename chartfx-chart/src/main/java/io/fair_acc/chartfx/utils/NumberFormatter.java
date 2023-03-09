package io.fair_acc.chartfx.utils;

public interface NumberFormatter {

    int getPrecision();

    boolean isExponentialForm();

    NumberFormatter setExponentialForm(boolean state);

    /**
     * @param precision numer of after-comma-digits plus one
     */
    NumberFormatter setPrecision(int precision);

    String toString(double val);

}