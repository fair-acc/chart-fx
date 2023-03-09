package io.fair_acc.chartfx.utils;

import com.ibm.icu.text.DecimalFormat;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.function.DoubleFunction;

import static io.fair_acc.chartfx.utils.NumberFormatterImpl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases based on removed com.ibm.icu.text.DecimalFormat
 *
 * @author Florian Enner
 * @since 07 March 2023
 */
class NumberFormatterImplTest {

    @Test
    void plainFormatGerman() {
        Locale.setDefault(Locale.GERMAN);
        var formatter = createFormatter(false, ALL_DIGITS);
        assertEquals("0", formatter.apply(0));
        assertEquals("2", formatter.apply(2.0));
        assertEquals("2,1", formatter.apply(2.1));
        assertEquals("10", formatter.apply(10));
        assertEquals("123,456789", formatter.apply(123.456789));
        assertEquals("1,23456789E14", formatter.apply(123.456789E12));
        assertEquals("1,23456789E-10", formatter.apply(123.456789E-12));
    }

    @Test
    void plainFormatEnglish() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(false, ALL_DIGITS);
        assertEquals("0", formatter.apply(0));
        assertEquals("2", formatter.apply(2.0));
        assertEquals("2.1", formatter.apply(2.1));
        assertEquals("10", formatter.apply(10));
        assertEquals("123.456789", formatter.apply(123.456789));
        assertEquals("1.23456789E14", formatter.apply(123.456789E12));
        assertEquals("1.23456789E-10", formatter.apply(123.456789E-12));
    }

    @Test
    void exponentialFormat() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(true, ALL_DIGITS);
        assertEquals("0E0", formatter.apply(0));
        assertEquals("2E0", formatter.apply(2.0));
        assertEquals("2.1E0", formatter.apply(2.1));
        assertEquals("1E1", formatter.apply(10));
        assertEquals("1.23456789E2", formatter.apply(123.456789));
        assertEquals("1.23456789E14", formatter.apply(123.456789E12));
        assertEquals("1.23456789E-10", formatter.apply(123.456789E-12));
    }

    @Test
    void afterCommaDigits5plain() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(false, 5);
        assertEquals("10.00000", formatter.apply(9.999999999999998));
        assertEquals("0.00000", formatter.apply(0));
        assertEquals("2.00000", formatter.apply(2.0));
        assertEquals("2.10000", formatter.apply(2.1));
        assertEquals("10.00000", formatter.apply(10));
        assertEquals("123.45679", formatter.apply(123.456789));
        assertEquals("1.23457E14", formatter.apply(123.456789E12));
        assertEquals("1.23457E-10", formatter.apply(123.456789E-12));
    }

    @Test
    void afterCommaDigits5Exp() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(true, 5);
        assertEquals("0.00000E0", formatter.apply(0));
        assertEquals("2.00000E0", formatter.apply(2.0));
        assertEquals("2.10000E0", formatter.apply(2.1));
        assertEquals("1.00000E1", formatter.apply(10));
        assertEquals("1.23457E2", formatter.apply(123.456789));
        assertEquals("1.23457E14", formatter.apply(123.456789E12));
        assertEquals("1.23457E-10", formatter.apply(123.456789E-12));
    }

    private static DoubleFunction<String> createFormatter(boolean exponentialForm, int afterCommaDigits) {
        var formatter = new NumberFormatterImpl();
        formatter.setExponentialForm(exponentialForm);
        formatter.setPrecision(afterCommaDigits + 1);
        return formatter::toString;
    }

    private static DoubleFunction<String> createBaselineIbmFormatter(boolean exponentialForm, int afterCommaDigits) {
        // TODO: remove
        //  this formatter was previously used for exponential formatting. It served as a baseline,
        //  but the behavior later changed to something more specialized for charting.
        var formatter = new DecimalFormat();
        formatter.setGroupingSize(0);
        formatter.setScientificNotation(exponentialForm);
        if (afterCommaDigits >= 0) {
            formatter.setSignificantDigitsUsed(true);
            formatter.setMinimumSignificantDigits(afterCommaDigits + 1);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
        }
        return formatter::format;
    }


}