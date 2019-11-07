package de.gsi.chart.utils;

public interface NumberFormatter {

    int getPrecision();

    boolean isExponentialForm();

    NumberFormatter setExponentialForm(boolean state);

    NumberFormatter setPrecision(int precision);

    String toString(double val);

}