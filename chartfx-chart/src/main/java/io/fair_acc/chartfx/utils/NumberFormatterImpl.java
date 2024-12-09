package io.fair_acc.chartfx.utils;

import static io.fair_acc.chartfx.utils.Schubfach.H_DOUBLE;
import static io.fair_acc.chartfx.utils.Schubfach.getDecimalLength;
import static io.fair_acc.chartfx.utils.Schubfach.getNormalizationScale;
import static java.lang.Math.multiplyHigh;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import javafx.util.StringConverter;

public class NumberFormatterImpl extends StringConverter<Number> implements NumberFormatter {
    
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    
    private static final Pattern UNICODE_WHITESPACE_PATTERN = Pattern.compile("(?U)\\s");
    
    public NumberFormatterImpl() {
        super();
        setDecimalFormatSymbols(DecimalFormatSymbols.getInstance());
    }

    public NumberFormatterImpl(final int decimalPlaces, final boolean exponentialForm) {
        this();
        setDecimalPlaces(decimalPlaces);
        setExponentialForm(exponentialForm);
    }

    @Override
    public Number fromString(final String string) {
        if (exponentialSeparator.length == 0) {
            return Double.parseDouble(string);
        }
        
        String cleanedString = UNICODE_WHITESPACE_PATTERN.matcher(string).replaceAll("");
        return Double.parseDouble(cleanedString);
    }

    @Override
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    @Override
    public boolean isExponentialForm() {
        return isExponentialForm;
    }
    
    public final char getExponentialSeparator() {
        return CHARSET.decode(ByteBuffer.wrap(exponentialSeparator)).get();
    }

    @Override
    public NumberFormatter setExponentialForm(final boolean state) {
        this.isExponentialForm = state;
        return this;
    }

    @Override
    public NumberFormatter setDecimalPlaces(final int decimalPlaces) {
        this.decimalPlaces = Math.max(ALL_DIGITS, decimalPlaces);
        return this;
    }
    
    /**
     * Sets the separator to use between decimal places and exponential e.g. {@code 1_HERE_E-10}. This <em>can</em> break the
     * functionality of {@link #fromString(String)} if a non-whitespace character is used. Default: none. 
     * 
     * @param separator Character to use
     */
    public final void setExponentialSeparator(char separator) {
        exponentialSeparator = charToBytes(separator);
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

        // Round to the desired number of digits
        final boolean useExponentialForm = isExponentialForm || e > MAX_PLAIN_EXP;
        if (decimalPlaces >= 0) {
            final int significantDigits = decimalPlaces + (useExponentialForm ? 1 : e);
            f += Schubfach.getRoundingOffset(significantDigits);
            if (f >= DIGITS_18) {
                f /= 10;
                e += 1;
            }
        }

        // extract digits
        long hm = multiplyHigh(f, 193428131138340668L) >>> 20;
        int h = (int) ((hm * 1441151881L) >>> 57); // first digit
        int m = (int) (hm - 100000000L * h); // next 8 digits
        int l = (int) (f - 100000000L * hm); // lowest 8 digits

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
        if (decimalPlaces > 0) {
            append(dot);
            appendNDigits(m, l, decimalPlaces);
        } else if (decimalPlaces == ALL_DIGITS) {
            append(dot);
            append8Digits(m);
            lowDigits(l);
        }
        append(exponentialSeparator);
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
        if (decimalPlaces == 0) {
            return;
        }
        append(dot);
        if (decimalPlaces == ALL_DIGITS) {
            for (; i <= 8; ++i) {
                t = 10 * y;
                appendDigit(t >>> 28);
                y = t & MASK_28;
            }
            lowDigits(l);
        } else {
            int remainingDigits = decimalPlaces;
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
        if (decimalPlaces == 0) {
            return;
        }
        append(dot);
        int spaceLeft = bytes.length - length;
        if (decimalPlaces == ALL_DIGITS) {
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
            int remainingDigits = Math.min(decimalPlaces, spaceLeft);
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
        if (decimalPlaces > 0) {
            append(dot);
            for (int i = 0; i < decimalPlaces; i++) {
                append(ZERO);
            }
        }
        if (isExponentialForm) {
            append(exp);
            append(ZERO);
        }
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
        if (length >= dot.length && 
                Arrays.equals(
                        bytes,  (length - dot.length), length - dot.length + 1, 
                        dot, 0, dot.length)) {
            length -= dot.length;
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
                              193_428_131_138_340_668L)
                       >>> 20)
      - 1;
    }

    private void exponent(int e) {
        append(exp);
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
        int d = (e * 103) >>> 10;
        appendDigit(d);
        appendDigit(e - 10 * d);
    }

    private String bytesToString() {
        return new String(bytes, 0, length, CHARSET);
    }
    
    private static byte[] charToBytes(char c) {
        return CHARSET.encode(CharBuffer.wrap(new char[] { c })).array();
    }

    static final int ALL_DIGITS = -1;
    private int decimalPlaces = ALL_DIGITS;

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
    private byte[] exponentialSeparator = {};

    // Used for left-to-tight digit extraction.
    private static final int MASK_28 = (1 << 28) - 1;

    public NumberFormatterImpl setDecimalFormatSymbols(DecimalFormatSymbols symbols) {
        String expString = symbols.getExponentSeparator();
        if (expString.length() > MAX_EXP_LENGTH) {
            throw new IllegalArgumentException("Exponent separator can't be longer than " + MAX_EXP_LENGTH);
        }
        this.exp = Objects.equals(expString, "E") ? DEFAULT_EXP : expString.getBytes(CHARSET);
        this.dot = charToBytes(symbols.getDecimalSeparator());
        return this;
    }

    byte[] dot = charToBytes('.');
    byte[] exp = DEFAULT_EXP;
    private static final byte[] DEFAULT_EXP = charToBytes('E');
    private static final byte ZERO = (byte) '0';
    private static final byte MINUS = (byte) '-';
}
