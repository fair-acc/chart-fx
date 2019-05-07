package de.gsi.chart.utils;

public interface NumberFormatter {

	int getPrecision();

	NumberFormatter setPrecision(int precision);

	boolean isExponentialForm();

	NumberFormatter setExponentialForm(boolean state);

	String toString(double val);


}