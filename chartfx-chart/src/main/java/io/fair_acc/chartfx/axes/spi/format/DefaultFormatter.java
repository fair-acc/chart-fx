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
            // TODO: The previous implementation used the DecimalFormat for parsing. Did that do anything special?
            return formatter.fromString(valueString);
        } catch (final Throwable exc) {
            // Matches the previous contract
            throw new IllegalArgumentException(exc);
        }
    }

    @Override
    protected void rangeUpdated() {
        if (majorTickMarksCopy != null && majorTickMarksCopy.size() > 0) {
            final boolean prevExponentialForm = formatter.isExponentialForm();
            final int prevPrecision = formatter.getPrecision();
            configureFormatter(majorTickMarksCopy);

            // Clear the cache if the formatting changed
            if (formatter.isExponentialForm() != prevExponentialForm
                    || formatter.getPrecision() != prevPrecision
                    || oldRangeIndex != rangeIndex) {
                labelCache.clear();

                // TODO: what is the rangeIndex? it does not get set anywhere. Maybe that's a subclass thing?
                oldRangeIndex = rangeIndex;
            }
        }
    }

    void configureFormatter(List<Double> tickMarks) {
        // Prepare enough cacheable objects
        final int n = tickMarks.size();
        while (decompositions.size() < n) {
            decompositions.add(new Schubfach.DecomposedDouble());
        }

        // Decompose the double values into significand and exponents
        for (int i = 0; i < n; i++) {
            double value = tickMarks.get(i) / unitScaling;
            Schubfach.decomposeDouble(value, decompositions.get(i));
        }

        // Special case if we only render a single tick
        if (n == 1) {
            var decomp = decompositions.get(0);
            formatter.setExponentialForm(decomp.getExponent() < -3 || decomp.getExponent() > 4);
            formatter.setPrecision(-1);
            return;
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
        boolean useExponentialForm = minExp < -3 || maxExp > 4;
        formatter.setExponentialForm(useExponentialForm);

        // Non-exponential forms are rendered with fixed separators, so
        // we need to shift to the largest exponent before determining the
        // significant digits.
        if (!useExponentialForm && (minExp != maxExp)) {
            for (int i = 0; i < n; i++) {
                decompositions.get(i).shiftExponentTo(maxExp);
            }
        }

        // Find the smallest difference between all significands
        long minDiff = 10000000000000000L; // max 17 digits
        for (int i = 0; i < n - 1; i++) {
            long f0 = decompositions.get(i).getSignificand();
            long f1 = decompositions.get(i + 1).getSignificand();
            long absDiff = f0 < f1 ? f1 - f0 : f0 - f1;
            minDiff = Math.min(minDiff, absDiff);
        }

        // In the exponential form cases the significands are often the same,
        // e.g., 1E0, 1E1, 1E2 etc., so we just render all significant digits
        // to be consistent. With the length check we would otherwise get 17
        //  after comma digits because there are no significant differences.
        if (minDiff == 0) {
            formatter.setPrecision(-1);
            return;
        }

        // Check at which digit the smallest difference occurs. We also
        // need to check rounding to avoid tiny floating point errors
        // that produce 9,999 (4 digits) instead of 10,000 (5 digits).
        int decimalLength = Schubfach.getDecimalLength(minDiff);
        int maxSigDigits = Schubfach.H_DOUBLE - decimalLength + 1;
        int roundedDecimalLength = Schubfach.getDecimalLength(minDiff + Schubfach.getRoundingOffset(maxSigDigits));
        if (roundedDecimalLength > decimalLength) {
            maxSigDigits = Schubfach.H_DOUBLE - roundedDecimalLength + 1;
        }

        // The precision is interpreted as the number of digits in exponential form,
        // i.e., the number of after-comma digits plus one. This allows us to pass
        // a fixed comma point for the non-exponential form.
        // TODO: rename precision to afterCommaDigits and break backwards compatibility?
        if (useExponentialForm) {
            // 5 = x.yyyy
            formatter.setPrecision(maxSigDigits);
        } else {
            // 5 = (xx)x.yyyy
            int afterCommaDigits = Math.max(maxSigDigits - maxExp, 0);
            formatter.setPrecision(Math.min(maxSigDigits, afterCommaDigits + 1));
        }

        // TODO: remove debug print
        /*System.out.println();
        System.out.println("minExp = " + minExp);
        System.out.println("maxExp = " + maxExp);
        System.out.println("maxSigDigits = " + maxSigDigits);
        System.out.println("afterCommaDigits = " + (formatter.getPrecision() - 1));*/
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
