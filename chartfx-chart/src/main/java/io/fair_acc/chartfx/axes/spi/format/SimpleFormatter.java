package io.fair_acc.chartfx.axes.spi.format;

import java.text.DecimalFormat;
import java.text.ParseException;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.TickUnitSupplier;
import javafx.util.StringConverter;

/**
 * @author rstein
 */
public class SimpleFormatter extends AbstractFormatter {

    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();
    private final DecimalFormat formatter = new DecimalFormat("0.######");
    private String prefix;
    private String suffix;

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     */
    public SimpleFormatter() {
        super();
        setTickUnitSupplier(SimpleFormatter.DEFAULT_TICK_UNIT_SUPPLIER);
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     *
     * @param axis The axis to format tick marks for
     */
    public SimpleFormatter(final Axis axis) {
        super(axis);
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis with a prefix and/or suffix.
     *
     * @param axis The axis to format tick marks for
     * @param prefix The prefix to append to the start of formatted number, can be null if not needed
     * @param suffix The suffix to append to the end of formatted number, can be null if not needed
     */
    public SimpleFormatter(final Axis axis, final String prefix, final String suffix) {
        this(axis);
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Converts the string provided into a Number defined by the this converter. Format of the string and type of the
     * resulting object is defined by this converter.
     *
     * @return a Number representation of the string passed in.
     * @see StringConverter#toString
     */
    @Override
    public Number fromString(final String string) {
        try {
            final int prefixLength = prefix == null ? 0 : prefix.length();
            final int suffixLength = suffix == null ? 0 : suffix.length();
            return formatter.parse(string.substring(prefixLength, string.length() - suffixLength));
        } catch (final ParseException exc) {
            throw new IllegalArgumentException(exc);
        }
    }

    @Override
    protected void rangeUpdated() {
        // normally set formatter based on range, this doesn't because it's the
        // 'simple' implementation
    }

    // private String toString(final Number object, final String numFormatter) {
    // if (numFormatter == null || numFormatter.isEmpty()) {
    // return toString(object, formatter);
    // }
    // return toString(object, new DecimalFormat(numFormatter));
    // }

    /**
     * Converts the object provided into its string form. Format of the returned string is defined by this converter.
     *
     * @return a string representation of the object passed in.
     * @see StringConverter#toString
     */
    @Override
    public String toString(final Number object) {
        return toString(object, formatter);
    }

    private String toString(final Number object, final DecimalFormat numFormatter) {
        final String pref = prefix == null ? "" : prefix;
        final String suff = suffix == null ? "" : suffix;
        return pref + numFormatter.format(object) + suff;
    }
}
