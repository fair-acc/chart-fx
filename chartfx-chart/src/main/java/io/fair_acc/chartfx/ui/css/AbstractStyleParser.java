package io.fair_acc.chartfx.ui.css;

import io.fair_acc.dataset.utils.StyleBuilder;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
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
        usedAtLeastOneKey = false;
        StyleBuilder.forEachProperty(style, this::onEntry);
        return usedAtLeastOneKey;
    }

    private void onEntry(String key, String value) {
        usedAtLeastOneKey |= parseEntry(key, value);
    }

    boolean usedAtLeastOneKey = false;

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

    protected double[] parseDoubleArray(String value) {
        return parse(value, str -> Arrays.stream(value.split("\\s+"))
                .mapToDouble(Double::parseDouble)
                .toArray());
    }

    protected Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (final IllegalArgumentException ex) {
            LOGGER.error("could not parse color value of '" + currentKey + "'='" + value + "'", ex);
            return null;
        }
    }

    protected boolean parseColor(String value, Consumer<Color> onSuccess) {
        var color = parseColor(value);
        if (isValid(color)) {
            onSuccess.accept(color);
            return true;
        }
        return false;
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
