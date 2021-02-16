/*****************************************************************************
 *                                                                           *
 * BI Common - convert Number <-> String                                     *
 *                                                                           *
 * modified: 2017-03-07 Harald Braeuning                                     *
 *                                                                           *
 ****************************************************************************/

package de.gsi.chart.utils;

import java.text.DecimalFormat;

import javafx.util.StringConverter;

/**
 *
 * @author braeun
 */
public class MemorySizeStringConverter extends StringConverter<Number> {
    private int precision = 1;

    private Unit unit = Unit.BEST;
    private final DecimalFormat format = new DecimalFormat();

    public MemorySizeStringConverter() {
        buildFormat(precision);
    }

    public MemorySizeStringConverter(int precision) {
        this.precision = precision;
        buildFormat(precision);
    }

    public MemorySizeStringConverter(Unit unit) {
        this.unit = unit;
        buildFormat(precision);
    }

    public MemorySizeStringConverter(Unit unit, int precision) {
        this.unit = unit;
        this.precision = precision;
        buildFormat(precision);
    }

    private void buildFormat(int precision) {
        final StringBuilder sb = new StringBuilder(32);
        sb.append("0.");
        sb.append("0".repeat(Math.max(0, precision)));
        format.applyPattern(sb.toString());
    }

    @Override
    public Number fromString(String string) {
        throw new UnsupportedOperationException("Parsing memory size string not implemented yet");
        //    return Double.parseDouble(string);
    }

    public int getPrecision() {
        return precision;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
        buildFormat(precision);
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    @Override
    public String toString(Number object) {
        String u;
        double v = object.doubleValue();
        switch (unit) {
        case BEST:
            if (v < 1000) {
                u = "B";
            } else if (v < 1000000) {
                v /= 1000.0;
                u = "kB";
            } else if (v < 1000000000) {
                v /= 1000000.0;
                u = "MB";
            } else {
                v /= 1000000000.0;
                u = "GB";
            }
            break;
        case KILOBYTE:
            v /= 1000.0;
            u = "kB";
            break;
        case MEGABYTE:
            v /= 1000000.0;
            u = "MB";
            break;
        case GIGABYTE:
            v /= 1000000000.0;
            u = "GB";
            break;
        default:
        case BYTE:
            u = "B";
            break;
        }
        return format.format(v) + u;
    }

    public enum Unit {
        BEST,
        BYTE,
        KILOBYTE,
        MEGABYTE,
        GIGABYTE
    }
}
