/*****************************************************************************
 *                                                                           *
 * BI Common - convert Number <-> String                                     *
 *                                                                           *
 * modified: 2017-04-25 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package io.fair_acc.chartfx.utils;

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

    private void buildFormat(int precision) {
        if (precision == 0) {
            format.applyPattern("#0");
        } else {
            final StringBuilder sb = new StringBuilder(32);
            sb.append("0.");
            sb.append("0".repeat(Math.max(0, precision)));
            format.applyPattern(sb.toString());
        }
    }

    @Override
    public Number fromString(String string) {
        return Double.parseDouble(string);
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.utils.NumberFormatter#getPrecision()
     */
    @Override
    public int getPrecision() {
        return precision;
    }

    @Override
    public boolean isExponentialForm() {
        return false;
    }

    @Override
    public NumberFormatter setExponentialForm(boolean state) {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.utils.NumberFormatter#setPrecision(int)
     */
    @Override
    public NumberFormatter setPrecision(int precision) {
        this.precision = precision;
        buildFormat(precision);
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
}
