package io.fair_acc.chartfx.utils;


import javafx.util.StringConverter;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

import static io.fair_acc.chartfx.utils.Schubfach.*;
import static java.lang.Math.*;


public class NumberFormatterImpl extends StringConverter<Number> implements NumberFormatter {
    public final static char DEFAULT_DECIMAL_SEPARATOR = ' ';

    public NumberFormatterImpl() {
        super();
        setDecimalFormatSymbols(DecimalFormatSymbols.getInstance());
    }

    public NumberFormatterImpl(final int precision, final boolean exponentialForm) {
        this();
        setPrecision(precision);
        setExponentialForm(exponentialForm);
    }

    @Override
    public Number fromString(final String string) {
        return Double.parseDouble(string);
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.utils.NumberFormatter#getPrecision()
     */
    @Override
    public int getPrecision() {
        return minSignificantDigits;
    }

    @Override
    public boolean isExponentialForm() {
        return isExponentialForm;
    }

    @Override
    public NumberFormatter setExponentialForm(final boolean state) {
        this.isExponentialForm = state;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.fair_acc.chartfx.utils.NumberFormatter#setPrecision(int)
     */
    @Override
    public NumberFormatter setPrecision(final int precision) {
        if ((precision != DEFAULT_PRECISION && precision < 1) || precision > 17) {
            throw new IllegalArgumentException("Limited precision must be between 1 and 17");
        }
        this.minSignificantDigits = precision;
        return this;
    }

    @Override
    public String toString(final double val) {
        switch (Schubfach.encodeDouble(val, this::encodeDouble)) {
            case Schubfach.NON_SPECIAL:
                return bytesToString();
            case Schubfach.PLUS_ZERO:
            case Schubfach.MINUS_ZERO:
                return "0"; // TODO: match precision
            case Schubfach.PLUS_INF:
                return "+inf";
            case Schubfach.MINUS_INF:
                return "-inf";
            default:
                return "NaN";
        }
    }

    @Override
    public String toString(final Number object) {
        return toString(object.doubleValue());
    }

    private void encodeDouble(boolean negative, long f, int e) {
        length = 0;
        if (negative) {
            append(MINUS);
        }

        // Normalize significand to H (17) digits
        final int len = getDecimalLength(f);
        f *= getNormalizationScale(H_DOUBLE, len);
        e += len;

        // Round middle point
        final int roundingPrecision = minSignificantDigits != DEFAULT_PRECISION ?
                minSignificantDigits : DEFAULT_MAX_SIGNIFICANT_DIGITS;
        f += ROUNDING_OFFSET[roundingPrecision];

        // extract digits
        long hm = multiplyHigh(f, 193428131138340668L) >>> 20;
        int h = (int) (hm * 1441151881L >>> 57); // first digit
        int m = (int) (hm - 100000000 * h); // next 8 digits
        int l = (int) (f - 100000000L * hm); // lowest 8 digits

        // TODO: remove debug statements
       /* System.out.println();
        System.out.println("h = " + h);
        System.out.println("m = " + m);
        System.out.println("l = " + l);
        System.out.println("e = " + e);*/

        if (isExponentialForm) {
            if (minSignificantDigits == DEFAULT_PRECISION) {
                toExponentialFormDefault(h, m, l, e);
            } else {
                toExponentialFormFixed(h, m, l, e);
            }
        } else {
            if (minSignificantDigits == DEFAULT_PRECISION) {
                toFullFormDefault(h, m, l, e);
            } else {
                toFullFormFixed(h, m, l, e);
            }
        }

    }

    private void toExponentialFormDefault(int h, int m, int l, int e) {
        appendDigit(h);
        append(DOT);
        appendNDigits(m, l, DEFAULT_MAX_SIGNIFICANT_DIGITS - 1);
        removeTrailingZeroes();
        exponent(e - 1);
    }

    private void toExponentialFormFixed(int h, int m, int l, int e) {
        appendDigit(h);
        append(DOT);
        appendNDigits(m, l, minSignificantDigits - 1);
        exponent(e - 1);
    }

    private void toFullFormDefault(int h, int m, int l, int e) {
        if (e < 0) {
            // all significant digits are on the right, e.g., 0.1234
            if (-e > DEFAULT_MAX_SIGNIFICANT_DIGITS) {
                // the number is so small that it's considered zero
                append(ZERO);
                return;
            }

            // the number has at least one significant digit that shows up
            appendDigit(0);
            append(DOT);
            for (; e < 0; ++e) {
                append(ZERO);
            }
            appendDigit(h);
            appendNDigits(m, DEFAULT_MAX_SIGNIFICANT_DIGITS - length);

        } else if (e >= DEFAULT_MAX_SIGNIFICANT_DIGITS) {
            // all significant digits are on the left, e.g., 123400 // TODO: should not be rounded
            appendDigit(h);
            appendNDigits(m, l, DEFAULT_MAX_SIGNIFICANT_DIGITS - 1);
            Arrays.fill(bytes, length, e, ZERO);
            length = e;

        } else {
            // significant digits are on both sides, e.g., 12.34
            // we write the all digits and then insert the dot
            appendDigit(h);
            appendNDigits(m, l, DEFAULT_MAX_SIGNIFICANT_DIGITS - 1);
            System.arraycopy(bytes, e, bytes, e + 1, length - e);
            bytes[e] = DOT;
            length++;
            removeTrailingZeroes();
        }
    }

    private void toFullFormFixed(int h, int m, int l, int e) {
        if (e < 0) {
            // all significant digits are on the right, e.g., 0.0000123456
            append(ZERO);
            append(DOT);
            for (; e < 0; e++) {
                append(ZERO);
            }
            appendDigit(h);
            appendNDigits(m, l, minSignificantDigits - 1);

        } else if (e >= minSignificantDigits) {
            // all significant digits are on the left, e.g., 1234560000 // TODO: ICU4j rounds, but do we want that?
            appendDigit(h);
            appendNDigits(m, l, minSignificantDigits - 1);
            Arrays.fill(bytes, length, e, ZERO);
            length = e;

        } else {
            // significant digits are on both sides, e.g., 123.456
            // we write the all digits and then insert the dot
            appendDigit(h);
            appendNDigits(m, l, minSignificantDigits - 1);
            System.arraycopy(bytes, e, bytes, e + 1, length - e);
            bytes[e] = DOT;
            length++;
        }
    }

    private void append(int c) {
        bytes[length++] = (byte) c;
    }

    private void append(byte c) {
        bytes[length++] = c;
    }

    private void appendDigit(int d) {
        bytes[length++] = (byte) (ZERO + d);
    }

    private void append8Digits(int m) {
        /*
        Left-to-right digits extraction:
        algorithm 1 in [3], with b = 10, k = 8, n = 28.
         */
        int y = y(m);
        for (int i = 0; i < 8; ++i) {
            int t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
    }

    private void appendNDigits(int m, int l, int digits) {
        if (digits > 0) {
            digits -= appendNDigits(m, digits);
        }
        if (digits > 0) {
            digits -= appendNDigits(l, digits);
        }
        if (digits != 0) {
            throw new AssertionError("failed to write all digits");
        }
    }

    /**
     * writes up to 8 digits
     *
     * @param m input value
     * @param n max number of digits
     * @return number of actually written digits
     */
    private int appendNDigits(int m, int n) {
        if (n <= 0) {
            return 0;
        } else if (n > 8) {
            n = 8;
        }
        int y = y(m);
        for (int i = 0; i < n; ++i) {
            int t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
        return n;
    }

    private void removeTrailingZeroes() {
        while (bytes[length - 1] == ZERO) {
            length--;
        }
        // remove trailing comma
        if (bytes[length - 1] == DOT) {
            length--;
        }
    }

    private int y(int a) {
        /*
        Algorithm 1 in [3] needs computation of
            floor((a + 1) 2^n / b^k) - 1
        with a < 10^8, b = 10, k = 8, n = 28.
        Noting that
            (a + 1) 2^n <= 10^8 2^28 < 10^17
        For n = 17, m = 8 the table in section 10 of [1] leads to:
         */
        return (int) (multiplyHigh(
                (long) (a + 1) << 28,
                193_428_131_138_340_668L) >>> 20) - 1;
    }

    private void exponent(int e) {
        append(EXP);
        if (e < 0) {
            append(MINUS);
            e = -e;
        }
        if (e < 10) {
            appendDigit(e);
            return;
        }
        /*
        For n = 2, m = 1 the table in section 10 of [1] shows
            floor(e / 10) = floor(103 e / 2^10)
         */
        int d = e * 103 >>> 10;
        appendDigit(d);
        appendDigit(e - 10 * d);
    }

    private String bytesToString() {
        return new String(bytes, 0, length, StandardCharsets.ISO_8859_1);
    }

    /*
    Room for the longer of the forms
    -ddddd.dddddddddddd         H + 2 characters
    -0.00ddddddddddddddddd      H + 5 characters
    -d.ddddddddddddddddE-eee    H + 7 characters
    where there are H digits d
    */
    private static final int MAX_CHARS_DOUBLE = Schubfach.H_DOUBLE + 7;
    byte[] bytes = new byte[MAX_CHARS_DOUBLE];
    int length = 0;

    static final int DEFAULT_PRECISION = -1;
    static final int DEFAULT_MAX_SIGNIFICANT_DIGITS = 4;
    int minSignificantDigits = DEFAULT_PRECISION;
    boolean isExponentialForm = false;

    // Used for left-to-tight digit extraction.
    private static final int MASK_28 = (1 << 28) - 1;

    /**
     * Offset map for rounding up/down to the desired precision
     */
    private static long[] ROUNDING_OFFSET = new long[]{
            0L,
            5000000000000000L,
            500000000000000L,
            50000000000000L,
            5000000000000L,
            500000000000L,
            50000000000L,
            5000000000L,
            500000000L,
            50000000L,
            5000000L,
            500000L,
            500000L,
            50000L,
            5000L,
            500L,
            500L,
            50L,
            5L,
            0L
    };

    public NumberFormatterImpl setDecimalFormatSymbols(DecimalFormatSymbols symbols) {
        this.DOT = (byte) symbols.getDecimalSeparator();
        return this;
    }

    private byte DOT = (byte) '.';
    private static final byte ZERO = (byte) '0';
    private static final byte MINUS = (byte) '-';
    private static final byte EXP = (byte) 'E';

}
