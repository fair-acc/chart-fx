package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.utils.StyleParser;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * Helps with parsing CSS limited to what is supported
 * by the renderer.
 *
 * @author ennerf
 */
public abstract class AbstractStyleParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStyleParser.class);

    /**
     * @param style string
     * @return true if the style contained relevant styles
     */
    public boolean tryParse(String style) {
        if (style == null || style.isEmpty()) {
            return false;
        }
        return parse(style);
    }

    protected boolean parse(String style) {
        clear();
        final Map<String, String> map = StyleParser.splitIntoMap(style);
        boolean usedAtLeastOneKey = false;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                usedAtLeastOneKey |= parseEntry(currentKey = entry.getKey(), value);
            }
        }
        return usedAtLeastOneKey;
    }

    protected abstract void clear();

    protected abstract boolean parseEntry(String key, String value);

    protected double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException ex) {
            LOGGER.error("could not parse double value of '" + currentKey + "'='" + value + "'", ex);
            return Double.NaN;
        }
    }

    protected Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (final IllegalArgumentException ex) {
            LOGGER.error("could not parse color value of '" + currentKey + "'='" + value + "'", ex);
            return null;
        }
    }

    protected <T> T parse(String value, Function<String, T> func) {
        try {
            return func.apply(value);
        } catch (RuntimeException ex) {
            LOGGER.error("could not parse value of '" + currentKey + "'='" + value + "'", ex);
            return null;
        }
    }

    protected boolean isValid(Object value) {
        return value != null;
    }

    protected boolean isValid(double value) {
        return !Double.isNaN(value);
    }

    protected <T> Optional<T> optional(T value) {
        return Optional.ofNullable(value);
    }

    protected OptionalDouble optional(double value) {
        return isValid(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
    }

    private String currentKey;

}
