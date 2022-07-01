package io.fair_acc.chartfx.utils;

public interface NumberFormatter {

    int getPrecision();

    boolean isExponentialForm();

    NumberFormatter setExponentialForm(boolean state);

    NumberFormatter setPrecision(int precision);

    String toString(double val);

}