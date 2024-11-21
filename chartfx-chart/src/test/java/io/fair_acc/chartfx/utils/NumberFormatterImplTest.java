package io.fair_acc.chartfx.utils;

import static org.junit.jupiter.api.Assertions.*;

import static io.fair_acc.chartfx.utils.NumberFormatterImpl.*;

import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleFunction;

import org.junit.jupiter.api.Test;

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
        assertEquals("0,000000000123456789", formatter.apply(123.456789E-12));
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
        assertEquals("0.000000000123456789", formatter.apply(123.456789E-12));
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
    void exponentialFormatParsing() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatterImpl(true, ALL_DIGITS, Optional.empty());
        assertEquals(0., formatter.fromString("0E0"));
        assertEquals(2.1,formatter.fromString("2.1E0"));
        assertEquals(123.456789E-12, formatter.fromString("1.23456789E-10"));
    }

    @Test
    void plain5Decimals() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(false, 5);
        assertEquals("10.00000", formatter.apply(9.999999999999998));
        assertEquals("0.00000", formatter.apply(0));
        assertEquals("2.00000", formatter.apply(2.0));
        assertEquals("2.10000", formatter.apply(2.1));
        assertEquals("10.00000", formatter.apply(10));
        assertEquals("123.45679", formatter.apply(123.456789));
        assertEquals("1.23457E14", formatter.apply(123.456789E12));
        assertEquals("0.00000", formatter.apply(123.456789E-12));
    }

    @Test
    void exponential5Decimals() {
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

    @Test
    void testPlainRounding() {
        // inputs copied from actual ticks
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(false, 1);
        assertEquals("0.1", formatter.apply(0.14999999999999994));
        assertEquals("0.2", formatter.apply(0.19999999999999996));
        assertEquals("0.2", formatter.apply(0.24999999999999994));
        assertEquals("0.3", formatter.apply(0.29999999999999993));
        assertEquals("0.3", formatter.apply(0.3499999999999999));
        assertEquals("0.1", formatter.apply(0.09999999999999994));
        assertEquals("0.0", formatter.apply(0.04999999999999993));
        assertEquals("-0.0", formatter.apply(-6.938893903907228E-17));
        assertEquals("-0.1", formatter.apply(-0.05000000000000007));
        assertEquals("0.0", formatter.apply(0.04999999999999999));

        formatter = createFormatter(false, 2);
        assertEquals("-0.02", formatter.apply(-0.019999999999999997));
        assertEquals("-0.01", formatter.apply(-0.009999999999999997));
        assertEquals("0.01", formatter.apply(0.010000000000000004));
        assertEquals("0.06", formatter.apply(0.06000000000000001));

        assertEquals("-0.02", formatter.apply(-0.019));
        assertEquals("-0.01", formatter.apply(-0.009));
        assertEquals("0.01", formatter.apply(0.010000000000000004));
        assertEquals("0.00", formatter.apply(0.001000000000000004));
    }
    
    @Test
    void exponentialSeparator() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatterImpl(true, 5, Optional.of('\u202F'));
        assertEquals("2.10000\u202FE0", formatter.toString(2.1));
        assertEquals("1.00000\u202FE1", formatter.toString(10));
        assertEquals("1.23457\u202FE14", formatter.toString(123.456789E12));
        assertEquals("1.23457\u202FE-10", formatter.toString(123.456789E-12));
    }
    
    @Test
    void exponentialSeparatorParsing() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatterImpl(true, 5, Optional.of('\u202F'));
        assertEquals(2.1, formatter.fromString("2.10000\u202FE0"));
        assertEquals(10., formatter.fromString("1.00000\u202FE1"));
        assertEquals(1.23457E14, formatter.fromString("1.23457\u202FE14"));
        assertEquals(1.23457E-10, formatter.fromString("1.23457\u202FE-10"));
    }
    
    private static NumberFormatterImpl createFormatterImpl(
            boolean exponentialForm,
            int decimalPlaces,
            Optional<Character> exponentialSeparator) {
        var formatter = new NumberFormatterImpl();
        formatter.setExponentialForm(exponentialForm);
        formatter.setDecimalPlaces(decimalPlaces);
        exponentialSeparator.ifPresent(sep -> formatter.setExponentialSeparator(sep));
        return formatter;
    }

    private static DoubleFunction<String> createFormatter(boolean exponentialForm, int decimalPlaces) {
        var formatter = createFormatterImpl(exponentialForm, decimalPlaces, Optional.empty());
        return formatter::toString;
    }
}
