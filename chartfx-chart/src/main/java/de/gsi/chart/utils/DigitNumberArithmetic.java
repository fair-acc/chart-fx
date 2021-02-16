package de.gsi.chart.utils;

import java.math.BigDecimal;
import java.util.WeakHashMap;

import de.gsi.dataset.spi.utils.Tuple;

public final class DigitNumberArithmetic { //NOPMD nomen est omen

    static WeakHashMap<Double, String> numberDigitsCache = new WeakHashMap<>();

    private DigitNumberArithmetic() {
    }

    public static Tuple<Double, Double> formatStringForSignificantDigits(final double num1, final double num2) {
        // TODO: optimise: this function is a performance hotspot (needed for
        // axis label calculation)
        final int exp1 = (int) Math.log10(Math.abs(num1)) + 1;
        final int exp2 = (int) Math.log10(Math.abs(num2)) + 1;
        if (Math.signum(num1) != Math.signum(num2) || exp1 != exp2) {
            return new Tuple<>((double) Math.max(exp1, exp2), (double) 0);
        }

        final double inum1 = Math.floor(num1);
        final double inum2 = Math.floor(num2);
        final double frac1 = num1 - inum1;
        final double frac2 = num2 - inum2;

        /*
         * Even if the numbers are different before the decimal point, numbers after the decimal point may be
         * significant: 15.0 - 17.5 - 20.0 - 22.5 - ...
         */
        if (inum1 != inum2) {
            return new Tuple<>((double) Math.max(exp1, exp2), (double) 0);
        }

        if (frac1 == 0 && frac2 == 0) {
            // System.err.println("both fractions are zero " + num1 + " - " + num2);
            return new Tuple<>((double) Math.max(exp1, exp2), (double) 0);
        } else {
            // System.err.println("fractions are non-zero " + frac1 + " - " + frac2);
        }
        // check if exponent is the same, if yes, then the export is simple
        final String stringNum1 = String.format("%.25f", frac1).replace(".", "");
        final String stringNum2 = String.format("%.25f", frac2).replace(".", "");
        int count = 1;
        for (int i = 1; i < Math.min(stringNum1.length(), stringNum2.length()); i++) {
            if (stringNum1.charAt(i) != stringNum2.charAt(i)) {
                break;
            }
            count++;
        }
        // System.err.println(String.format("number %s vs %s differ in %d-th digit\n", stringNum1, stringNum2, count));

        /*
         * how many digits do we need? If stepsize is 0.25 for example, we get 025000000 and 050000000. Therefore we
         * need two digits after the decimal point (i.e. up to the first digit from the RIGHT, which is different.
         */
        // for (int i=Math.min(stringNum1.length(),stringNum2.length())-1;i>0;i--) {
        // if (stringNum1.charAt(i) != stringNum2.charAt(i)) {
        // count = i;
        // break;
        // }
        // }
        // System.err.println(String.format("number %s vs %s differ at least in %d-th digit\n", stringNum1, stringNum2,
        // count));

        return new Tuple<>((double) Math.max(exp1, exp2), (double) count);
    }

    public static int getNumberOfSignificantDigits(final double val) {
        BigDecimal input = BigDecimal.valueOf(val);
        input = input.stripTrailingZeros();
        return input.scale() < 0 ? input.precision() - input.scale() : input.precision();
    }

    public static int getPrecision(final double val) {
        BigDecimal input = BigDecimal.valueOf(val);
        input = input.stripTrailingZeros();
        return input.precision();
    }

    public static void main(final String[] args) {
        final double[] test1 = { 1.23, 1.0, 1.00, 1.23e19, 1.23456789e19, 1.23456789e-19 };

        for (final double num : test1) {
            final int digits = DigitNumberArithmetic.getNumberOfSignificantDigits(num);
            final String msg = String.format("%f %E has %d significant digits", num, num, digits);
            System.out.println(msg);
        }
        System.out.println();

        final double[][] test2 = { { 1.23, 1.24 }, { 1000, 1001 }, { 0.001, 0.0011 }, { 5.001, 5.0011 },
            { 10.001, 10.0011 }, { 1, 1.0 + 1e-5 }, { 5.0 / 3.0, 5.0 / 3.0 + 1e-5 },
            { 1.234567e9, 1.234567e9 + 0.001 }, { 1.234567e30, 1.234567e30 + 0.001 } };
        for (final double[] pair : test2) {
            final int digits = DigitNumberArithmetic.numberDigitsUntilFirstSignificantDigit(pair[0], pair[1]);
            final String msg = String.format("%f %f differs in digit %d", pair[0], pair[1], digits);
            System.out.println(msg);
        }
        System.out.println();
        final double val1 = 123456789e9;
        System.out.println("1: val1=" + String.format("%f", val1));
        System.out.println("2: val1=" + String.format("%100.50f", val1));
        final double val2 = 123456789 + 1e-8;
        System.out.println("1: val2=" + String.format("%f", val2));
        System.out.println("2: val2=" + String.format("%100.50f", val2));
        System.out.println("3: val1=" + BigDecimal.valueOf(val2).toPlainString());
        System.out.println("4: val1=" + BigDecimal.valueOf(val2).stripTrailingZeros());
    }

    public static int numberDigitsUntilFirstSignificantDigit(final double num1, final double num2) {
        // TODO: optimise: this function is a performance hotspot (needed for
        // axis label calculation)

        final String stringNum1 = numberDigitsCache.computeIfAbsent(num1,
                k -> String.format("%.25e", k).replace(".", ""));
        final String stringNum2 = numberDigitsCache.computeIfAbsent(num2,
                k -> String.format("%.25e", k).replace(".", ""));
        // final String stringNum1 = String.format("%.25e", num1).replace(".",
        // "");
        // final String stringNum2 = String.format("%.25e", num2).replace(".",
        // "");
        int count = 1;
        for (int i = 0; i < Math.min(stringNum1.length(), stringNum2.length()); i++) {
            if (stringNum1.charAt(i) != stringNum2.charAt(i)) {
                break;
            }
            count++;
        }

        return count;
    }

    public static double roundToFractionalDigits(final double num, final int n) {
        final double power = Math.pow(10, n);
        return Math.round(num * power) / power;
    }

    public static double roundToSignificantFigures(final double num, final int n) {
        if (num == 0) {
            return 0;
        }
        final double d = Math.ceil(Math.log10(num < 0 ? -num : num));
        final int power = n - (int) d;
        final double magnitude = Math.pow(10, power);
        final long shifted = Math.round(num * magnitude);
        return shifted / magnitude;
    }
}
