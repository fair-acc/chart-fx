package io.fair_acc.chartfx.utils;

public interface NumberFormatter {

    int getDecimalPlaces();

    boolean isExponentialForm();

    NumberFormatter setExponentialForm(boolean state);

    NumberFormatter setDecimalPlaces(int precision);

    String toString(double val);

}