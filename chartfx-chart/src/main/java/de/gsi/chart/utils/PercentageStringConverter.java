/*****************************************************************************
 * *
 * BI Common - convert Number <-> String *
 * *
 * modified: 2017-03-07 Harald Braeuning *
 * *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.text.DecimalFormat;

import javafx.util.StringConverter;

/**
 * @author braeun
 */
public class PercentageStringConverter extends StringConverter<Number> {

    private int precision = 1;
    private boolean appendPercentSign = true;
    private final DecimalFormat format = new DecimalFormat();

    public PercentageStringConverter() {
        buildFormat(precision);
    }

    public PercentageStringConverter(final int precision) {
        this.precision = precision;
        buildFormat(precision);
    }

    public PercentageStringConverter(final int precision, final boolean appendPercentSign) {
        this.precision = precision;
        this.appendPercentSign = appendPercentSign;
        buildFormat(precision);
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(final int precision) {
        this.precision = precision;
        buildFormat(precision);
    }

    public boolean isAppendPercentSign() {
        return appendPercentSign;
    }

    public void setAppendPercentSign(final boolean appendPercentSign) {
        this.appendPercentSign = appendPercentSign;
    }

    @Override
    public String toString(final Number object) {
        return format.format(object.doubleValue() * 100.0) + (appendPercentSign ? "%" : "");
    }

    @Override
    public Number fromString(final String input) {
        String s = input;
        final int p = s.indexOf('%');
        if (p > 0) {
            s = s.substring(0, p);
        }
        return Double.parseDouble(s);
    }

    private void buildFormat(final int precision) {
        final StringBuilder sb = new StringBuilder(32);
        sb.append('0');
        if (precision > 0) {
            sb.append('.');
            for (int i = 0; i < precision; i++) {
                sb.append('0');
            }
        }
        format.applyPattern(sb.toString());
    }

}
