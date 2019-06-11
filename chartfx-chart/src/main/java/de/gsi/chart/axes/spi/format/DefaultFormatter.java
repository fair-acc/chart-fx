package de.gsi.chart.axes.spi.format;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.WeakHashMap;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.TickUnitSupplier;
import de.gsi.chart.utils.DigitNumberArithmetic;
import de.gsi.chart.utils.NumberFormatterImpl;
import de.gsi.dataset.spi.utils.Tuple;
import javafx.util.StringConverter;

/**
 * Default number formatter for NumberAxis, this stays in sync with auto-ranging and formats values appropriately. You
 * can wrap this formatter to add prefixes or suffixes;
 */
public class DefaultFormatter extends AbstractFormatter {

    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();
    private static final String FORMAT_SMALL_SCALE = "0.00";
    private static final String FORMAT_LARGE_SCALE = "#.##E0";
    public static final int DEFAULT_SMALL_AXIS = 6; // [orders of magnitude],
                                                    // e.g. '4' <-> [1,10000]
    private final DecimalFormat formatterSmall = new DecimalFormat(DefaultFormatter.FORMAT_SMALL_SCALE);
    private final DecimalFormat formatterLarge = new DecimalFormat(DefaultFormatter.FORMAT_LARGE_SCALE);

    private String formatterPattern = "%f";
    private boolean isExponentialForm = false;

    private final WeakHashMap<Integer, WeakHashMap<Number, String>> numberFormatCache = new WeakHashMap<>();

    protected NumberFormatterImpl myFormatter = new NumberFormatterImpl();
    protected int rangeIndex = 0;
    protected int oldRangeIndex = 0;
    private String prefix;
    private String suffix;

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     *
     * @param axis The axis to format tick marks for
     */
    public DefaultFormatter(final Axis axis) {
        super(axis);
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     */
    public DefaultFormatter() {
        super();
        setTickUnitSupplier(DefaultFormatter.DEFAULT_TICK_UNIT_SUPPLIER);
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis with a prefix and/or suffix.
     *
     * @param axis The axis to format tick marks for
     * @param prefix The prefix to append to the start of formatted number, can be null if not needed
     * @param suffix The suffix to append to the end of formatted number, can be null if not needed
     */
    public DefaultFormatter(final Axis axis, final String prefix, final String suffix) {
        this(axis);
        this.prefix = prefix;
        this.suffix = suffix;

    }

    @Override
    protected void rangeUpdated() {

        final double range = getRange();
        isExponentialForm = range < 1e-3 || range > 1e4;
        myFormatter.setExponentialForm(isExponentialForm);

        int maxSigDigits = 0;
        int maxDigits = 0; /** number of digits before separator */
        if (majorTickMarksCopy != null) {
            for (int i = 0; i < majorTickMarksCopy.size(); i++) {
                final double val = majorTickMarksCopy.get(i).doubleValue() / unitScaling;
                final int nDigits = (int) Math.log10(Math.abs(val)) + 1;
                maxDigits = Math.max(nDigits, maxDigits);
            }

            int maxExp = 0;
            int maxFrac = 0;
            for (int i = 0; i < majorTickMarksCopy.size() - 1; i++) {
                final double lower = majorTickMarksCopy.get(i).doubleValue() / unitScaling;
                final double upper = majorTickMarksCopy.get(i + 1).doubleValue() / unitScaling;
                final int significantDifferentDigits = DigitNumberArithmetic
                        .numberDigitsUntilFirstSignificantDigit(lower, upper);
                maxSigDigits = Math.max(maxSigDigits, significantDifferentDigits);

                // first tuple index is exp, second is number of fraction digits
                final Tuple<Double, Double> formatTuple = DigitNumberArithmetic.formatStringForSignificantDigits(lower,
                        upper);
                maxExp = (int) Math.max(formatTuple.getXValue(), maxExp);
                // rstein: added default +1 as temporary mitigation to increase significant number of digits in axis
                // labels TODO: find a more holistic solution -> May 2019
                maxFrac = (int) Math.max(formatTuple.getYValue() + 1, maxFrac);
            }

            final StringBuilder sb = new StringBuilder("%");
            sb.append(maxExp + maxFrac + 1);
            if (maxFrac > 0) {
                sb.append(".");
                sb.append(maxFrac);
            } else if (maxExp == 0 && maxFrac == 0) {
                sb.append(".1");
            } else if (maxExp >= 0 && maxFrac == 0) {
                sb.append(".0");
            }
            formatterPattern = sb.append("f").toString();

            // System.err.println("myFormatter::formatterPattern = " +
            // formatterPattern);
            // System.err.println("myFormatter::setPrecision(...) = " +
            // maxSigDigits);
            // System.err.println("myFormatter::maxDigitsBeforeSeparator(...) ="
            // + maxDigits);
            // System.err.println(String.format("max %d:%d\n", maxExp,
            // maxFrac));

            myFormatter.setPrecision(maxSigDigits);
        }

        // System.out.println(range+" -> "+rangeIndex+":
        // "+formatter.toPattern());

        if (oldRangeIndex != rangeIndex)

        {
            labelCache.clear();
            oldRangeIndex = rangeIndex;
        }
    }

    /**
     * Converts the object provided into its string form. Format of the returned string is defined by this converter.
     *
     * @return a string representation of the object passed in.
     * @see StringConverter#toString
     */
    @Override
    public String toString(final Number object) {
        // TODO: just for testing need to clean-up w.r.t. use of cache etc.
        // return labelCache.get(formatter, object.doubleValue());
        // return labelCache.get(formatter, object.doubleValue());

        if (isExponentialForm) {
            return labelCache.get(myFormatter, object.doubleValue());
        }
        final WeakHashMap<Number, String> hash = numberFormatCache.get(formatterPattern.hashCode());
        if (hash != null) {
            final String label = hash.get(object);
            if (label != null) {
                return label;
            }
        }
        // couldn't find label in cache
        final String retVal = String.format(formatterPattern, object.doubleValue());
        // add retVal to cache
        if (hash == null) {
            final WeakHashMap<Number, String> temp = new WeakHashMap<>();
            temp.put(object, retVal);
            numberFormatCache.put(Integer.valueOf(formatterPattern.hashCode()), temp);
            // checkCache = new WeakHashMap<>();
        } else {
            hash.put(object, retVal);
        }

        return retVal;
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
        final int prefixLength = prefix == null ? 0 : prefix.length();
        final int suffixLength = suffix == null ? 0 : suffix.length();
        try {
            return formatterSmall.parse(string.substring(prefixLength, string.length() - suffixLength));
        } catch (final ParseException exc) {
            try {
                return formatterLarge.parse(string.substring(prefixLength, string.length() - suffixLength));
            } catch (final ParseException ex) {
                ex.addSuppressed(exc);
                throw new IllegalArgumentException(ex);
            }
        }
    }
}
