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
    void defaultFormatGerman() {
        Locale.setDefault(Locale.GERMAN);
        var formatter = createFormatter(false, DEFAULT_PRECISION);
        assertEquals("2", formatter.apply(2.0));
        assertEquals("2,1", formatter.apply(2.1));
        assertEquals("123,457", formatter.apply(123.456789));
        assertEquals("123456789000000", formatter.apply(123.456789E12));
        assertEquals("0", formatter.apply(123.456789E-12));
    }

    @Test
    void defaultFormatEnglish() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(false, DEFAULT_PRECISION);
        assertEquals("2", formatter.apply(2.0));
        assertEquals("2.1", formatter.apply(2.1));
        assertEquals("123.457", formatter.apply(123.456789));
        assertEquals("123456789000000", formatter.apply(123.456789E12));
        assertEquals("0", formatter.apply(123.456789E-12));
    }

    @Test
    void exponentialForm() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(true, DEFAULT_PRECISION);
        assertEquals("2E0", formatter.apply(2.0));
        assertEquals("2.1E0", formatter.apply(2.1));
        assertEquals("1.235E2", formatter.apply(123.456789));
        assertEquals("1.235E14", formatter.apply(123.456789E12));
        assertEquals("1.235E-10", formatter.apply(123.456789E-12));
    }

    @Test
    void precision6() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(false, 6);
        assertEquals("2.00000", formatter.apply(2.0));
        assertEquals("2.10000", formatter.apply(2.1));
        assertEquals("123.457", formatter.apply(123.456789));
        assertEquals("123457000000000", formatter.apply(123.456789E12));
        assertEquals("0.000000000123457", formatter.apply(123.456789E-12));
    }

    @Test
    void precision6Exp() {
        Locale.setDefault(Locale.US);
        var formatter = createFormatter(true, 6);
        assertEquals("2.00000E0", formatter.apply(2.0));
        assertEquals("2.10000E0", formatter.apply(2.1));
        assertEquals("1.23457E2", formatter.apply(123.456789));
        assertEquals("1.23457E14", formatter.apply(123.456789E12));
        assertEquals("1.23457E-10", formatter.apply(123.456789E-12));
    }

    private static DoubleFunction<String> createFormatter(boolean exponentialForm, int precision) {
        if (CREATE_BASELINE) {
            return createBaselineIbmFormatter(exponentialForm, precision);
        }
        var formatter = new NumberFormatterImpl();
        formatter.setExponentialForm(exponentialForm);
        if (precision != DEFAULT_PRECISION) {
            formatter.setPrecision(precision);
        }
        return formatter::toString;
    }

    private static DoubleFunction<String> createBaselineIbmFormatter(boolean exponentialForm, int precision) {
        var formatter = new DecimalFormat();
        formatter.setGroupingSize(0);
        formatter.setScientificNotation(exponentialForm);
        if (precision != DEFAULT_PRECISION) {
            formatter.setSignificantDigitsUsed(true);
            formatter.setMinimumSignificantDigits(precision);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
        }
        return formatter::format;
    }

    private static boolean CREATE_BASELINE = false;

}