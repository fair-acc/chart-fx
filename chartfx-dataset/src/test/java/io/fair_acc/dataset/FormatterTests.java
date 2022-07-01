package io.fair_acc.dataset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static io.fair_acc.dataset.DefaultNumberFormatter.FormatMode;
import static io.fair_acc.dataset.DefaultNumberFormatter.SignConvention;

import java.text.ParsePosition;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FormatterTests {
    @Test
    void basicTests() {
        Formatter<Number> testFormatter = new Formatter<>() {
            /**
             * Use this overwrite if the default Number format isn't enough and something more specific is needed.
             *
             * @param pattern the pattern for this message format
             * @param args optional arguments that are filled by the calling argument
             * @return the formatted string
             */
            @Override
            public @NotNull String format(@NotNull String pattern, Object... args) {
                if (args.length <= 3) {
                    return Formatter.super.format(pattern, args);
                } else {
                    // override with custom format definition
                    return "too many arguments: " + args.length;
                    // alt: return Arrays.toString(args) ....
                }
            }

            @Override
            public @NotNull Number fromString(final @NotNull String string) {
                return Double.valueOf(string);
                //alt: throw new NumberFormatException("not implemented");
            }

            @Override
            public Class<Number> getClassInstance() {
                return Number.class;
            }

            @Override
            public @NotNull String toString(@NotNull final Number number) {
                return number.toString();
            }
        };
        final String defaultPattern = "{1} with {2}, {0} and {0, number, #.0}";

        Assertions.assertDoesNotThrow(() -> testFormatter.format(defaultPattern, 3.141, "testing", 10));
        assertNotNull(testFormatter.getClassInstance());
        assertEquals(3.141, testFormatter.fromString("3.141"));
        assertEquals(3.141, testFormatter.fromString("3.141", new ParsePosition(0)));
        assertEquals(3.141, testFormatter.fromString("3.141 otherItem", new ParsePosition(0)));

        // System.out.println("A: " + testFormatter.format(defaultPattern, 3.141, "testing", 10))
        // System.out.println("B: " + testFormatter.format(defaultPattern, 1, 2, 3, 4))
    }

    @Test
    void toStringFixedWidth() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.FIXED_WIDTH_ONLY));
        assertEquals(FormatMode.FIXED_WIDTH_ONLY, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setNumberOfCharacters(6));
        assertEquals(6, formatter.getNumberOfCharacters());

        assertEquals(" 1.234", formatter.format("{0}", 1.234567890));
        assertEquals(" 1.234", formatter.toString(1.234567890123456789));
        assertEquals(" 1234567890", formatter.toString(1234567890));
        assertEquals(" 12345678901", formatter.toString(12345678901L));
        assertEquals("   123", formatter.toString(123));
        assertEquals("     0", formatter.toString(0.0));
        assertEquals("     0", formatter.toString(0));
        assertEquals("   0.1", formatter.toString(0.1));
        assertEquals("  0.01", formatter.toString(0.01));
        assertEquals("    -∞", formatter.toString(Double.NEGATIVE_INFINITY));
        assertEquals("    +∞", formatter.toString(Double.POSITIVE_INFINITY));
        assertEquals("   NaN", formatter.toString(Double.NaN));
        assertEquals("    -∞", formatter.toString(Float.NEGATIVE_INFINITY));
        assertEquals("    +∞", formatter.toString(Float.POSITIVE_INFINITY));
        assertEquals("   NaN", formatter.toString(Float.NaN));
        assertEquals(" 0.000", formatter.toString(0.000000000123456789));
    }

    @Test
    void toStringFixedWidthExponentialOnly() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.FIXED_WIDTH_EXP));
        assertEquals(FormatMode.FIXED_WIDTH_EXP, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setNumberOfCharacters(5));
        assertEquals(5, formatter.getNumberOfCharacters());
        assertDoesNotThrow(() -> formatter.setSignConvention(SignConvention.NONE));
        assertEquals(SignConvention.NONE, formatter.getSignConvention());

        for (int i = 0; i < 15; i++) {
            final int width = i;
            assertDoesNotThrow(() -> formatter.setNumberOfCharacters(width));
            assertEquals(width, formatter.getNumberOfCharacters());

            final double testValue = 1.234567890123456789e9;
            final var string = formatter.toString(testValue);
            // System.err.printf("%2d out = '%s'%n", i, string)
            assertEquals((i < 6) ? 4 : width, string.length(), "length mismatch: string = '" + string + "' requested width = " + width);
            final double parsedValue = formatter.fromString(string).doubleValue();
            final double delta = (i < 6) ? 1 : Math.pow(10, -i + 5);
            assertEquals(testValue, parsedValue, delta * testValue, "failed for test string: " + string + "' requested width = " + width + " value: " + testValue + " vs. parsed: " + parsedValue + " delta: " + delta);
        }
    }

    @Test
    void toStringByteFormat() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.BYTE_PREFIX));
        assertEquals(FormatMode.BYTE_PREFIX, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setFixedPrecision(2));
        assertEquals(2, formatter.getFixedPrecision());

        for (int i = 0; i < 7; i++) {
            final double testValue = 1.12 * Math.pow(1024, i);
            final var string = formatter.toString(testValue);
            // System.err.printf("%2d out = '%s'%n", i, string)
            final double parsedValue = formatter.fromString(string).doubleValue();
            assertEquals(testValue, parsedValue, 0.01 * testValue, "failed for test string: " + string + " value: " + testValue + " vs. parsed: " + parsedValue);
        }
    }

    @Test
    void toStringJdkDefault() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.JDK));
        assertEquals(FormatMode.JDK, formatter.getFormatMode());

        assertEquals("1.2345678901234567", formatter.toString(1.234567890123456789)); // main different to optimal
        assertEquals("1234567890", formatter.toString(1234567890));
        assertEquals("12345678901", formatter.toString(12345678901L));
        assertEquals("123", formatter.toString(123));
        assertEquals("0", formatter.toString(0));
        assertEquals("0.1", formatter.toString(0.1));
        assertEquals("0.01", formatter.toString(0.01));
        assertEquals("-∞", formatter.toString(Double.NEGATIVE_INFINITY));
        assertEquals("+∞", formatter.toString(Double.POSITIVE_INFINITY));
        assertEquals("NaN", formatter.toString(Double.NaN));
        assertEquals("-∞", formatter.toString(Float.NEGATIVE_INFINITY));
        assertEquals("+∞", formatter.toString(Float.POSITIVE_INFINITY));
        assertEquals("NaN", formatter.toString(Float.NaN));
        assertEquals("1.23456789E-10", formatter.toString(0.000000000123456789)); // main different to optimal
    }

    @Test
    void toStringOptimalWidth() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.OPTIMAL_WIDTH));
        assertEquals(FormatMode.OPTIMAL_WIDTH, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setFixedPrecision(2));
        assertEquals(2, formatter.getFixedPrecision());

        assertEquals("1.23", formatter.format("{0}", 1.234567890));
        assertEquals("1.23", formatter.toString(1.234567890123456789));
        assertEquals("1.23E+9", formatter.toString(1234567890));
        assertEquals("1.23E+10", formatter.toString(12345678901L));
        assertEquals("123", formatter.toString(123));
        assertEquals("0", formatter.toString(0));
        assertEquals("0.1", formatter.toString(0.1));
        assertEquals("0.01", formatter.toString(0.01));
        assertEquals("-∞", formatter.toString(Double.NEGATIVE_INFINITY));
        assertEquals("+∞", formatter.toString(Double.POSITIVE_INFINITY));
        assertEquals("NaN", formatter.toString(Double.NaN));
        assertEquals("-∞", formatter.toString(Float.NEGATIVE_INFINITY));
        assertEquals("+∞", formatter.toString(Float.POSITIVE_INFINITY));
        assertEquals("NaN", formatter.toString(Float.NaN));
        assertEquals("1.23E-10", formatter.toString(0.000000000123456789));
    }

    @Test
    void toStringFixedWidthAndExponential() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.FIXED_WIDTH_AND_EXP));
        assertEquals(FormatMode.FIXED_WIDTH_AND_EXP, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setNumberOfCharacters(8));
        assertEquals(8, formatter.getNumberOfCharacters());
        assertDoesNotThrow(() -> formatter.setSignConvention(SignConvention.EMPTY_SIGN));
        assertEquals(SignConvention.EMPTY_SIGN, formatter.getSignConvention());
        assertDoesNotThrow(() -> formatter.setSignConventionExp(SignConvention.FORCE_SIGN));
        assertEquals(SignConvention.FORCE_SIGN, formatter.getSignConventionExp());

        assertEquals(" 1.23456", formatter.format("{0}", 1.234567890));
        assertEquals(" 1.23456", formatter.toString(1.234567890123456789));
        assertEquals(" 1.23E+9", formatter.toString(1234567890));
        assertEquals(" 1.2E+10", formatter.toString(12345678901L));
        assertEquals("     123", formatter.toString(123));
        assertEquals("       0", formatter.toString(0.0));
        assertEquals("       0", formatter.toString(0));
        assertEquals("     0.1", formatter.toString(0.1));
        assertEquals("    0.01", formatter.toString(0.01));
        assertEquals("      -∞", formatter.toString(Double.NEGATIVE_INFINITY));
        assertEquals("      +∞", formatter.toString(Double.POSITIVE_INFINITY));
        assertEquals("     NaN", formatter.toString(Double.NaN));
        assertEquals("      -∞", formatter.toString(Float.NEGATIVE_INFINITY));
        assertEquals("      +∞", formatter.toString(Float.POSITIVE_INFINITY));
        assertEquals("     NaN", formatter.toString(Float.NaN));
        assertEquals(" 1.2E-10", formatter.toString(0.000000000123456789));

        assertEquals("-1.23456", formatter.format("{0}", -1.234567890));
        assertEquals("-1.23456", formatter.toString(-1.234567890123456789));
        assertEquals("-1.23E+9", formatter.toString(-1234567890));
        assertEquals("-1.2E+10", formatter.toString(-12345678901L));
        assertEquals("    -123", formatter.toString(-123));
        assertEquals("      -0", formatter.toString(-0.0));
        assertEquals("       0", formatter.toString(-0)); // negative zero does not exist for integers (2-complement)
        assertEquals("    -0.1", formatter.toString(-0.1));
        assertEquals("   -0.01", formatter.toString(-0.01));
        assertEquals("-1.2E-10", formatter.toString(-0.000000000123456789));
    }

    @Test
    void toStringMetricPrefix() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.METRIC_PREFIX));
        assertEquals(FormatMode.METRIC_PREFIX, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setFixedPrecision(2));
        assertEquals(2, formatter.getFixedPrecision());

        assertEquals("1.23", formatter.format("{0}", 1.234567890));
        assertEquals("1.23", formatter.toString(1.234567890123456789));
        assertEquals("1.23G", formatter.toString(1234567890));
        assertEquals("12.35G", formatter.toString(12345678901L));
        assertEquals("123.00", formatter.toString(123));
        assertEquals("0.00", formatter.toString(0));
        assertEquals("100.00m", formatter.toString(0.1));
        assertEquals("10.00m", formatter.toString(0.01));
        assertEquals("-∞", formatter.toString(Double.NEGATIVE_INFINITY));
        assertEquals("+∞", formatter.toString(Double.POSITIVE_INFINITY));
        assertEquals("NaN", formatter.toString(Double.NaN));
        assertEquals("-∞", formatter.toString(Float.NEGATIVE_INFINITY));
        assertEquals("+∞", formatter.toString(Float.POSITIVE_INFINITY));
        assertEquals("NaN", formatter.toString(Float.NaN));
        assertEquals("123.46p", formatter.toString(0.000000000123456789));

        assertDoesNotThrow(() -> formatter.setFixedPrecision(0));
        assertEquals(0, formatter.getFixedPrecision());
        assertEquals("1y", formatter.toString(1e-24));
        assertEquals("1z", formatter.toString(1e-21));
        assertEquals("1a", formatter.toString(1e-18));
        assertEquals("1f", formatter.toString(1e-15));
        assertEquals("1p", formatter.toString(1e-12));
        assertEquals("1n", formatter.toString(1e-9));
        assertEquals("1µ", formatter.toString(1e-6));
        assertEquals("1m", formatter.toString(1e-3));
        assertEquals("1", formatter.toString(1e0));
        assertEquals("1k", formatter.toString(1e3));
        assertEquals("1M", formatter.toString(1e6));
        assertEquals("1G", formatter.toString(1e9));
        assertEquals("1T", formatter.toString(1e12));
        assertEquals("1P", formatter.toString(1e15));
        assertEquals("1E", formatter.toString(1e18));
        assertEquals("1Z", formatter.toString(1e21));
        assertEquals("1Y", formatter.toString(1e24));
    }

    @Test
    void toAndFromStringMetricPrefix() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.METRIC_PREFIX));
        assertEquals(FormatMode.METRIC_PREFIX, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setFixedPrecision(2));
        assertEquals(2, formatter.getFixedPrecision());

        for (int i = -28; i < 28; i++) {
            final double testValue = 1.23 * Math.pow(10, i);
            final var string = formatter.toString(testValue);
            final double parsedValue = formatter.fromString(string).doubleValue();
            assertEquals(testValue, parsedValue, 0.01 * testValue, "failed for test string: " + string + " value: " + testValue + " vs. parsed: " + parsedValue);
        }
    }

    @Test
    void toAndFromStringDefault() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.FIXED_WIDTH_AND_EXP));
        assertEquals(FormatMode.FIXED_WIDTH_AND_EXP, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setNumberOfCharacters(10));

        for (int i = -6; i < 6; i++) {
            final double testValue = 1.23 * Math.pow(10, i);
            final var string = formatter.toString(testValue);
            final double parsedValue = formatter.fromString(string).doubleValue();
            assertEquals(testValue, parsedValue, 0.03 * testValue, "failed for test string: " + string + " value: " + testValue + " vs. parsed: " + parsedValue);
        }
    }

    @Test
    void toStringFixedWidthExponentialWidthScan() {
        final var formatter = new DefaultNumberFormatter();
        assertDoesNotThrow(() -> formatter.setFormatMode(FormatMode.FIXED_WIDTH_AND_EXP));
        assertEquals(FormatMode.FIXED_WIDTH_AND_EXP, formatter.getFormatMode());
        assertDoesNotThrow(() -> formatter.setSignConvention(SignConvention.NONE));
        assertEquals(SignConvention.NONE, formatter.getSignConvention());

        for (int i = 0; i < 15; i++) {
            final int width = i;
            assertDoesNotThrow(() -> formatter.setNumberOfCharacters(width));
            assertEquals(width, formatter.getNumberOfCharacters());

            final var string = formatter.toString(1.234567890123456789e9);
            // System.err.printf("%2d out = '%s'%n", i, string)
            assertEquals((i < 6) ? 4 : width, string.length(), "length mismatch: string = '" + string + "' requested width = " + width);
        }

        assertDoesNotThrow(() -> formatter.setSignConventionExp(SignConvention.FORCE_SIGN));
        assertEquals(SignConvention.FORCE_SIGN, formatter.getSignConventionExp());

        for (int i = 0; i < 15; i++) {
            final int width = i;
            assertDoesNotThrow(() -> formatter.setNumberOfCharacters(width));
            assertEquals(width, formatter.getNumberOfCharacters());

            final var string = formatter.toString(1.234567890123456789e9);
            // System.err.printf("%2d out = '%s'%n", i, string)
            assertEquals((i < 6) ? 4 : width, string.length(), "length mismatch: string = '" + string + "' requested width = " + width);
        }
    }
}