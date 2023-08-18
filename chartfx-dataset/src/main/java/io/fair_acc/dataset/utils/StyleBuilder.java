package io.fair_acc.dataset.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * A utility class for generating valid CSS that does
 * not require JavaFX to be on the classpath.
 *
 * @author ennerf
 */
public class StyleBuilder<T extends StyleBuilder<T>> {

    public T reset() {
        properties.clear();
        return getThis();
    }

    public T withExisting(String style) {
        forEachProperty(style, properties::put);
        return getThis();
    }

    protected T setIntegerProp(String key, int value) {
        properties.put(key, String.valueOf(value));
        return getThis();
    }

    protected T setDoubleProp(String key, double value) {
        properties.put(key, String.valueOf(value));
        return getThis();
    }

    protected T setDoubleArray(String key, double... values) {
        return setStringProp(key, toDoubleArray(values));
    }

    protected String toDoubleArray(double[] values) {
        if (values.length == 0) {
            return "null";
        }
        builder.setLength(0);
        builder.append(values[0]);
        for (int i = 1; i < values.length; i++) {
            builder.append(" ").append(values[i]);
        }
        return builder.toString();
    }

    protected T setBooleanProp(String key, boolean value) {
        properties.put(key, String.valueOf(value));
        return getThis();
    }

    public T setStringProp(String key, String value) {
        properties.put(key, value);
        return getThis();
    }

    protected T setColorProp(String key, int r, int g, int b, double a) {
        properties.put(key, String.format("rgb(%d,%d,%d,%f)", r & 0xFF, g & 0xFF, b & 0xFF, a));
        return getThis();
    }

    public String build() {
        builder.setLength(0);

        // add all entries
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue()).append(";\n");
            }
        }

        // remove last newline
        if (builder.charAt(builder.length() - 1) == '\n') {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }


    @SuppressWarnings("unchecked")
    protected T getThis() {
        return (T) this;
    }

    private final HashMap<String, String> properties = new HashMap<>();
    private final StringBuilder builder = new StringBuilder();

    public static int forEachProperty(String style, BiConsumer<String,String> consumer) {
        if (style == null || style.isEmpty()) {
            return 0;
        }
        int addedEntries = 0;
        final String[] keyVals = AT_LEAST_ONE_WHITESPACE_PATTERN.matcher(style).replaceAll("").split(";");
        for (final String keyVal : keyVals) {
            final String[] parts = STYLE_ASSIGNMENT_PATTERN.split(keyVal, 2);
            if (parts.length <= 1) {
                continue;
            }
            consumer.accept(parts[0], QUOTES_PATTERN.matcher(parts[1]).replaceAll(""));
            addedEntries++;
        }
        return addedEntries;
    }

    private static final Pattern AT_LEAST_ONE_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern QUOTES_PATTERN = Pattern.compile("[\"']");
    private static final Pattern STYLE_ASSIGNMENT_PATTERN = Pattern.compile("[=:]");

}
