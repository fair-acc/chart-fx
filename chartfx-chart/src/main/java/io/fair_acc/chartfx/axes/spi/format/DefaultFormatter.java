package io.fair_acc.chartfx.axes.spi.format;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.TickUnitSupplier;
import io.fair_acc.chartfx.utils.NumberFormatterImpl;
import io.fair_acc.chartfx.utils.Schubfach;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Default number formatter for NumberAxis, this stays in sync with auto-ranging and formats values appropriately. You
 * can wrap this formatter to add prefixes or suffixes;
 */
public class DefaultFormatter extends AbstractFormatter {
    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();
    private final WeakHashMap<Integer, WeakHashMap<Number, String>> numberFormatCache = new WeakHashMap<>();

    protected NumberFormatterImpl formatter = new NumberFormatterImpl();
    private final List<Schubfach.DecomposedDouble> decompositions = new ArrayList<>();
    protected int rangeIndex = 0;
    protected int oldRangeIndex = 0;
    private String prefix;
    private String suffix;
    protected DisplayFormat displayFormat = DisplayFormat.Auto;

    public static enum DisplayFormat {
        Auto,
        Scientific,
        Plain
    }

    public DisplayFormat getDisplayFormat() {
        return displayFormat;
    }

    public DefaultFormatter setDisplayFormat(DisplayFormat displayFormat) {
        if (displayFormat == null) {
            throw new NullPointerException("displayFormat");
        }
        this.displayFormat = displayFormat;
        return this;
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     */
    public DefaultFormatter() {
        super();
        setTickUnitSupplier(DefaultFormatter.DEFAULT_TICK_UNIT_SUPPLIER);
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis
     *
     * @param axis The axis to format tick marks for
     */
    public DefaultFormatter(final Axis axis) {
        super(axis);
    }

    /**
     * Construct a DefaultFormatter for the given NumberAxis with a prefix and/or suffix.
     *
     * @param axis   The axis to format tick marks for
     * @param prefix The prefix to append to the start of formatted number, can be null if not needed
     * @param suffix The suffix to append to the end of formatted number, can be null if not needed
     */
    public DefaultFormatter(final Axis axis, final String prefix, final String suffix) {
        this(axis);
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Converts the string provided into a Number defined by this converter. Format of the string and type of the
     * resulting object is defined by this converter.
     *
     * @return a Number representation of the string passed in.
     * @see StringConverter#toString
     */
    @Override
    public Number fromString(final String string) {
        final int prefixLength = prefix == null ? 0 : prefix.length();
        final int suffixLength = suffix == null ? 0 : suffix.length();
        final String valueString = string.substring(prefixLength, string.length() - suffixLength);
        try {
            return formatter.fromString(valueString);
        } catch (final Throwable exc) {
            // Matches the previous contract
            throw new IllegalArgumentException(exc);
        }
    }

    @Override
    protected void rangeUpdated() {
        if (majorTickMarksCopy != null && majorTickMarksCopy.size() > 0) {
            final boolean prevForm = formatter.isExponentialForm();
            final int prevDecimals = formatter.getDecimalPlaces();
            configureFormatter(getRange(), majorTickMarksCopy);

            // Clear the cache if the formatting changed
            if (formatter.isExponentialForm() != prevForm || formatter.getDecimalPlaces() != prevDecimals) {
                labelCache.clear();
            }

            // TODO: Left the same as before refactoring, but this code does not seem to be used.
            if (oldRangeIndex != rangeIndex) {
                labelCache.clear();
                oldRangeIndex = rangeIndex;
            }

        }
    }

    protected boolean shouldUseExponentialForm(double range, int minExp, int maxExp) {
        return minExp < -3 || maxExp > 4;
    }

    void configureFormatter(double range, List<Double> tickMarks) {
        // Prepare enough cacheable objects
        final int n = tickMarks.size();
        while (decompositions.size() < n) {
            decompositions.add(new Schubfach.DecomposedDouble());
        }

        // Decompose the double values into significand and exponents
        for (int i = 0; i < n; i++) {
            double value = tickMarks.get(i) / unitScaling;
            if (Math.abs(value) < 1E-14 && range > 1E-12) {
                // treat rounding errors around zero as zero
                // TODO:
                //  (1) confirm that the threshold is sane
                //  (2) The exp format still renders non-zero because
                //      it gets the raw number. This might be best
                //      to fix in the tick marker generator?
                value = 0;
            }
            Schubfach.decomposeDouble(value, decompositions.get(i));
        }

        // Determine the min and max exponents
        int minExp = decompositions.get(0).getExponent();
        int maxExp = minExp;
        for (int i = 1; i < n; i++) {
            final int exp = decompositions.get(i).getExponent();
            minExp = Math.min(minExp, exp);
            maxExp = Math.max(maxExp, exp);
        }

        // Use the exponential form for very large or very small numbers
        final boolean useExponentialForm;
        if (displayFormat == DisplayFormat.Auto) {
            useExponentialForm = shouldUseExponentialForm(range, minExp, maxExp);
        } else {
            useExponentialForm = (displayFormat == DisplayFormat.Scientific);
        }
        formatter.setExponentialForm(useExponentialForm);

        // Special case if we only render a single tick
        if (n == 1) {
            formatter.setDecimalPlaces(-1);
            return;
        }

        // Non-exponential forms are rendered with fixed separators, so
        // we need to shift to the largest exponent before determining the
        // significant digits.
        if (!useExponentialForm && (minExp != maxExp)) {
            for (int i = 0; i < n; i++) {
                decompositions.get(i).shiftExponentTo(maxExp);
            }
        }

        // Find the difference between all significands. Both
        // min and max should be the same, but floating point
        // errors may cause differences. It's not clear whether
        // we should prefer the min or max, so until we run into
        // issues we use the min and err on the side of displaying
        // too many digits.
        long minDiff = 10000000000000000L; // max 17 digits
        long maxDiff = 0L;
        for (int i = 0; i < n - 1; i++) {
            long f0 = decompositions.get(i).getSignificand();
            long f1 = decompositions.get(i + 1).getSignificand();
            long absDiff = f0 < f1 ? f1 - f0 : f0 - f1;
            minDiff = Math.min(minDiff, absDiff);
            maxDiff = Math.max(maxDiff, absDiff);
        }

        // In the exponential form cases the significands are often the same,
        // e.g., 1E0, 1E1, 1E2 etc., so we just render all significant digits
        // to be consistent. With the length check we would otherwise get 17
        // after comma digits because there are no significant differences.
        if (minDiff == 0) {
            formatter.setDecimalPlaces(-1);
            return;
        }

        // Check at which digit the smallest difference occurs. We also
        // need to check rounding to avoid tiny floating point errors
        // that produce 9,999 (4 digits) instead of 10,000 (5 digits).
        int decimalLength = Schubfach.getDecimalLength(minDiff);
        int significantDigits = Schubfach.H_DOUBLE - decimalLength + 1;
        int roundedDecimalLength = Schubfach.getDecimalLength(minDiff + Schubfach.getRoundingOffset(significantDigits));
        if (roundedDecimalLength > decimalLength) {
            significantDigits--;
        }

        // A single digit is enough for unique labels, but for 0.25 steps it looks
        // a bit odd to show labels rounded to [0.3, 0.5, 0.8] ticks. We extend
        // the display to also include the next digit if it is non-zero.
        long divisor = Schubfach.pow10(Schubfach.H_DOUBLE - significantDigits - 2);
        int frac3 = (int) (minDiff / divisor); // 249992 turns into -> 249
        int frac2 = (frac3 + 5) / 10; // round to 2nd decimal, e.g., 249 -> 25
        int fracDigit1 = frac2 / 10; // top digit
        int fracDigit2 = frac2 - fracDigit1 * 10; // isolated second digit
        if (fracDigit2 != 0) {
            significantDigits++;
        }

        // We fix the number of decimal places for a right-aligned
        // display with a consistent comma point.
        final int decimalPlaces = useExponentialForm
                ? significantDigits - 1 // exponential form:     x.yyyyE0
                : significantDigits - maxExp; // plain form: (xx)x.yyyy
        formatter.setDecimalPlaces(Math.max(decimalPlaces, 0));
    }

    /**
     * Converts the object provided into its string form. Format of the returned string is defined by this converter.
     *
     * @return a string representation of the object passed in.
     * @see StringConverter#toString
     */
    @Override
    public String toString(final Number object) {
        return labelCache.get(formatter, object.doubleValue());
    }

}
