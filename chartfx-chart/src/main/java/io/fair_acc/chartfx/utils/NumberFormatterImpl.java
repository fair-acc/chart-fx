package io.fair_acc.chartfx.utils;


import javafx.util.StringConverter;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;

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
        return afterCommaDigits + 1;
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

    @Override
    public NumberFormatter setPrecision(final int precision) { // TODO: replace with setAfterCommaDigits?
        this.afterCommaDigits = Math.max(ALL_DIGITS, precision - 1);
        return this;
    }

    @Override
    public String toString(final double val) {
        switch (Schubfach.encodeDouble(val, this::encodeDouble)) {
            case Schubfach.NON_SPECIAL:
                return bytesToString();
            case Schubfach.PLUS_ZERO:
            case Schubfach.MINUS_ZERO:
                encodeZero();
                return length == 1 ? "0" : bytesToString();
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
        String result = toString(object.doubleValue());
        // TODO: remove debug print
        // System.out.println(object + " => " + result + " (afterCommaDigits = " + afterCommaDigits + ")");
        return result;
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

        // Round to the desired number of digits
        final boolean useExponentialForm = isExponentialForm || e > MAX_PLAIN_EXP;
        if (afterCommaDigits >= 0) {
            final int significantDigits = afterCommaDigits + (useExponentialForm ? 1 : e);
            f += Schubfach.getRoundingOffset(significantDigits);
            if (f >= DIGITS_18) {
                f /= 10;
                e += 1;
            }
        }

        // extract digits
        long hm = multiplyHigh(f, 193428131138340668L) >>> 20;
        int h = (int) (hm * 1441151881L >>> 57); // first digit
        int m = (int) (hm - 100000000 * h); // next 8 digits
        int l = (int) (f - 100000000L * hm); // lowest 8 digits

        // TODO: remove debug statements
        /*System.out.println();
        System.out.println("h = " + h);
        System.out.println("m = " + m);
        System.out.println("l = " + l);
        System.out.println("e = " + e);*/

        if (useExponentialForm) {
            toExponentialFormat(h, m, l, e);
        } else {
            if (e > 0) {
                toPlainFormat(h, m, l, e);
            } else {
                toPlainFormatWithLeadingZeros(h, m, l, e);
            }
        }

    }

    private void toExponentialFormat(int h, int m, int l, int e) {
        appendDigit(h);
        if (afterCommaDigits > 0) {
            append(DOT);
            appendNDigits(m, l, afterCommaDigits);
        } else if (afterCommaDigits == ALL_DIGITS) {
            append(DOT);
            append8Digits(m);
            lowDigits(l);
        }
        exponent(e - 1);
    }

    private void toPlainFormat(int h, int m, int l, int e) {
        appendDigit(h);
        int y = y(m);
        int t;
        int i = 1;
        for (; i < e; ++i) {
            t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
        if (afterCommaDigits == 0) {
            return;
        }
        append(DOT);
        if (afterCommaDigits == ALL_DIGITS) {
            for (; i <= 8; ++i) {
                t = 10 * y;
                appendDigit(t >>> 28);
                y = t & MASK_28;
            }
            lowDigits(l);
        } else {
            int remainingDigits = afterCommaDigits;
            for (; i <= 8 && remainingDigits > 0; ++i) {
                t = 10 * y;
                appendDigit(t >>> 28);
                y = t & MASK_28;
                remainingDigits--;
            }
            appendNDigits(l, remainingDigits);
        }
    }

    private void toPlainFormatWithLeadingZeros(int h, int m, int l, int e) {
        append(ZERO);
        if (afterCommaDigits == 0) {
            return;
        }
        append(DOT);
        int spaceLeft = bytes.length - length;
        if (afterCommaDigits == ALL_DIGITS) {
            for (; e < 0 && spaceLeft > 0; ++e) {
                append(ZERO);
                spaceLeft--;
            }
            if (spaceLeft > 0) {
                appendDigit(h);
                appendNDigits(m, l, Math.min(spaceLeft - 1, 16));
            }
            removeTrailingZeroes();
        } else {
            int remainingDigits = Math.min(afterCommaDigits, spaceLeft);
            for (; e < 0 && remainingDigits > 0; ++e) {
                append(ZERO);
                remainingDigits--;
            }
            if (remainingDigits > 0) {
                appendDigit(h);
                appendNDigits(m, l, remainingDigits - 1);
            }
        }
    }

    private void encodeZero() {
        length = 0;
        append(ZERO);
        if (afterCommaDigits > 0) {
            append(DOT);
            for (int i = 0; i < afterCommaDigits; i++) {
                append(ZERO);
            }
        }
        if (isExponentialForm) {
            append(EXP);
            append(ZERO);
        }
    }

    private void append(int c) {
        bytes[length++] = (byte) c;
    }

    private void append(byte c) {
        bytes[length++] = c;
    }

    private void append(byte[] bytes) {
        for (byte b : bytes) {
            append(b);
        }
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

    private void lowDigits(int l) {
        if (l != 0) {
            append8Digits(l);
        }
        removeTrailingZeroes();
    }

    private int appendNDigits(int m, int l, int digits) {
        if (digits > 0) {
            digits -= appendNDigits(m, digits);
        }
        if (digits > 0) {
            digits -= appendNDigits(l, digits);
        }
        // Should always end at zero, but it's possible that users
        // manually request >17 digits.
        return digits;
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

    static final int ALL_DIGITS = -1;
    private int afterCommaDigits = ALL_DIGITS;

    /*
    Room for the longer of the forms
    -ddddd.dddddddddddd         H + 2 characters
    -0.00ddddddddddddddddd      H + 5 characters
    -d.ddddddddddddddddE-eee    H + 7 characters
    where there are H digits d
    */
    private static final int MAX_EXP_LENGTH = 5;
    private static final int MAX_CHARS_DOUBLE = Schubfach.H_DOUBLE + 7 + MAX_EXP_LENGTH;

    /**
     * eventually the plain format starts going beyond the byte array limits,
     * so we just render the exponential form instead. This is mostly to
     * produce something sensible for manual calls as this condition should
     * never be met in charting code.
     */
    private static final int MAX_PLAIN_EXP = 7;
    private static final long DIGITS_18 = 100000000000000000L;
    byte[] bytes = new byte[MAX_CHARS_DOUBLE];
    int length = 0;

    boolean isExponentialForm = false;

    // Used for left-to-tight digit extraction.
    private static final int MASK_28 = (1 << 28) - 1;

    public NumberFormatterImpl setDecimalFormatSymbols(DecimalFormatSymbols symbols) {
        String exp = symbols.getExponentSeparator();
        if (exp.length() > MAX_EXP_LENGTH) {
            throw new IllegalArgumentException("Exponent separator can't be longer than " + MAX_EXP_LENGTH);
        }
        this.EXP = "E".equals(exp) ? DEFAULT_EXP : exp.getBytes(StandardCharsets.ISO_8859_1);
        this.DOT = (byte) symbols.getDecimalSeparator();
        return this;
    }

    byte DOT = '.';
    byte[] EXP = DEFAULT_EXP;
    private static final byte[] DEFAULT_EXP = new byte[]{'E'};
    private static final byte ZERO = (byte) '0';
    private static final byte MINUS = (byte) '-';

}
