package de.gsi.microservice.utils;

import static java.util.Map.Entry;

import java.util.Properties;
import java.util.Set;

public class SystemProperties { // NOPMD -- nomen est omen
    private static final Properties SYSTEM_PROPERTIES = System.getProperties();

    public static String getProperty(final String key) {
        return SYSTEM_PROPERTIES.getProperty(key);
    }

    public static String getPropertyIgnoreCase(String key, String defaultValue) {
        String value = SYSTEM_PROPERTIES.getProperty(key);
        if (null != value)
            return value;

        // Not matching with the actual key then
        Set<Entry<Object, Object>> systemProperties = SYSTEM_PROPERTIES.entrySet();
        for (final Entry<Object, Object> entry : systemProperties) {
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                return (String) entry.getValue();
            }
        }
        return defaultValue;
    }

    public static String getPropertyIgnoreCase(String key) {
        return getPropertyIgnoreCase(key, null);
    }

    public static double getValue(String key, double defaultValue) {
        final String value = getProperty(key);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    public static int getValue(String key, int defaultValue) {
        final String value = getProperty(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static double getValueIgnoreCase(String key, double defaultValue) {
        final String value = getPropertyIgnoreCase(key);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    public static int getValueIgnoreCase(String key, int defaultValue) {
        final String value = getPropertyIgnoreCase(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static Object put(final Object key, final Object value) {
        return SYSTEM_PROPERTIES.put(key, value);
    }
}