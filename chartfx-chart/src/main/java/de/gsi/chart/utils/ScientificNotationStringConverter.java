package de.gsi.chart.utils;

/*****************************************************************************
 * *
 * BI Common - convert Number <-> String *
 * *
 * modified: 2017-04-25 Harald Braeuning *
 * *
 ****************************************************************************/

import java.text.DecimalFormat;

import javafx.util.StringConverter;

/**
 * @author braeun
 */
public class ScientificNotationStringConverter extends StringConverter<Number> implements NumberFormatter {

	private int precision = 2;
	private final DecimalFormat format = new DecimalFormat();

	public ScientificNotationStringConverter() {
		buildFormat(precision);
	}

	public ScientificNotationStringConverter(final int precision) {
		this.precision = precision;
		buildFormat(precision);
	}

	@Override
	public int getPrecision() {
		return precision;
	}

	@Override
	public NumberFormatter setPrecision(final int precision) {
		this.precision = precision;
		buildFormat(precision);
		return this;
	}

	@Override
	public boolean isExponentialForm() {
		return true;
	}

	@Override
	public NumberFormatter setExponentialForm(boolean state) {
		return this;
	}

	@Override
	public String toString(double val) {
		return toString(Double.valueOf(val));
	}

	@Override
	public String toString(final Number object) {
		return format.format(object);
	}

	@Override
	public Number fromString(final String string) {
		return Double.parseDouble(string);
	}

	private void buildFormat(final int precision) {
		final StringBuilder sb = new StringBuilder(32);
		sb.append("0.");
		for (int i = 0; i < precision; i++) {
			sb.append('0');
		}
		sb.append("E0");
		format.applyPattern(sb.toString());
	}

}
