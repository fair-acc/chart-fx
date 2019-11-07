package de.gsi.chart.axes.spi;

/**
 * Metric prefixes and conversions according to ISO/IEC 80000 N.B. the 'deca' and 'deci' are omitted/deprecated
 *
 * @author rstein
 */
public enum MetricPrefix {
    YOTTA("yotta", "Y", 1e24), ZETTA("zetta", "Z", 1e21), EXA("exa", "E", 1e18), PETA("peta", "P", 1e15),
    TERA("tera", "T", 1e12), GIGA("giga", "G", 1e9), MEGA("mega", "M", 1e6), KILO("kilo", "k", 1e3),
    HECTO("hecto", "h", 1e2),
    // DECA("deca", "da", 1e1),
    NONE("", "", 1e0),
    // DECI("deci", "d", 1e-1),
    CENTI("centi", "c", 1e-2), MILLI("milli", "m", 1e-3), MICRO("micro", "\u03BC", 1e-6), NANO("nano", "n", 1e-9),
    PICO("pico", "p", 1e-12), FEMTO("femto", "f", 1e-15), ATTO("atto", "a", 1e-18), ZEPTO("zepto", "z", 1e-21),
    YOCTO("yocto", "y", 1e-24);

    final String longPrefix;
    final String shortPrefix;
    final double power;

    MetricPrefix(final String longPrefix, final String shortPrefix, final double power) {
        this.longPrefix = longPrefix;
        this.shortPrefix = shortPrefix;
        this.power = power;
    }

    public String getLongPrefix() {
        return longPrefix;
    }

    public double getPower() {
        return power;
    }

    public String getShortPrefix() {
        return shortPrefix;
    }

    public static String getLongPrefix(final double scaling) {
        for (final MetricPrefix e : MetricPrefix.values()) {
            if (scaling == e.getPower()) {
                return e.getLongPrefix();
            }
        }
        return scaling + "*";
    }

    public static MetricPrefix getNearestMatch(final double scaling) {
        for (final MetricPrefix e : MetricPrefix.values()) {
            if (scaling >= e.getPower()) {
                return e;
            }
        }
        return NONE;
    }

    public static String getShortPrefix(final double scaling) {
        for (final MetricPrefix e : MetricPrefix.values()) {
            if (scaling == e.getPower()) {
                return e.getShortPrefix();
            }
        }
        return scaling + "*";
    }
}
