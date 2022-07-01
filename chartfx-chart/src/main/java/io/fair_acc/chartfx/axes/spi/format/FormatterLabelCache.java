package io.fair_acc.chartfx.axes.spi.format;

import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.StringConverter;

/**
 * Simple cache to keep recurring results of String formatters
 *
 * @author rstein
 */
public class FormatterLabelCache extends WeakHashMap<Number, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormatterLabelCache.class);

    public FormatterLabelCache() {
        super();
    }

    public String get(final StringConverter<Number> formatter, final Number value) {
        return computeIfAbsent(value, formatter::toString);
    }

    // @Override
    // public String get(Object key) {
    // String ret = super.get(key);
    // if (ret == null) {
    // System.out.println("cache missed");
    // } else {
    // System.out.println("cache hit");
    // }
    // return ret;
    // }
}
