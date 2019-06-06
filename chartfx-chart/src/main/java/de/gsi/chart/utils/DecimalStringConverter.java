/*****************************************************************************
 *                                                                           *
 * BI Common - convert Number <-> String                                     *
 *                                                                           *
 * modified: 2017-04-25 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.text.DecimalFormat;

import javafx.util.StringConverter;

/**
 *
 * @author braeun
 */
public class DecimalStringConverter extends StringConverter<Number> implements NumberFormatter {

	private int precision = 6;
	private final DecimalFormat format = new DecimalFormat();

	public DecimalStringConverter() {
		buildFormat(precision);
	}

	public DecimalStringConverter(int precision) {
		this.precision = precision;
		buildFormat(precision);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.utils.NumberFormatter#getPrecision()
	 */
	@Override
	public int getPrecision() {
		return precision;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.utils.NumberFormatter#setPrecision(int)
	 */
	@Override
	public NumberFormatter setPrecision(int precision) {
		this.precision = precision;
		buildFormat(precision);
		return this;
	}

	@Override
	public boolean isExponentialForm() {
		return false;
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
	public String toString(Number object) {
		return format.format(object);
	}

	@Override
	public Number fromString(String string) {
		return Double.parseDouble(string);
	}

	private void buildFormat(int precision) {
		if (precision == 0) {
			format.applyPattern("#0");
		} else {
			final StringBuilder sb = new StringBuilder(32);
			sb.append("0.");
			for (int i = 0; i < precision; i++) {
				sb.append('0');
			}
			format.applyPattern(sb.toString());
		}
	}

}
