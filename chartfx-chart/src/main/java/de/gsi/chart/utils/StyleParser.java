package de.gsi.chart.utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChartCss;

/**
 * Some helper routines to parse CSS-style formatting attributes
 *
 * @author rstein
 */
public final class StyleParser { // NOPMD
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleParser.class);
    private static final int DEFAULT_FONT_SIZE = 18;
    private static final String DEFAULT_FONT = "Helvetia";
    private static final Pattern AT_LEAST_ONE_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern QUOTES_PATTERN = Pattern.compile("[\"\']");
    private static final Pattern STYLE_ASSIGNMENT_PATTERN = Pattern.compile("[=:]");

    private StyleParser() {
    }

    public static Boolean getBooleanPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);
        final String value = map.get(key.toLowerCase(Locale.UK));
        if (value == null) {
            return null;
        }

        try {
            return Boolean.parseBoolean(value);
        } catch (final NumberFormatException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER.error(
                        "could not parse boolean description for '" + key + "'='" + value + "' returning null", ex);
            }
            return null;
        }
    }

    public static Color getColorPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);
        final String value = map.get(key.toLowerCase(Locale.UK));
        if (value == null) {
            return null;
        }

        try {
            return Color.web(value);
        } catch (final IllegalArgumentException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER.error(
                        "could not parse color description for '" + key + "'='" + value + "' returning null", ex);
            }
            return null;
        }
    }

    public static double[] getFloatingDecimalArrayPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);
        final String value = map.get(key.toLowerCase(Locale.UK));
        if (value == null) {
            return null;
        }

        try {
            final String[] splitValues = value.split(",");
            if (splitValues == null || splitValues.length == 0) {
                return null;
            }
            final double[] retArray = new double[splitValues.length];
            for (int i = 0; i < splitValues.length; i++) {
                retArray[i] = Double.parseDouble(splitValues[i]);
            }
            return retArray;
        } catch (final NumberFormatException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER.error(
                        "could not parse integer description for '" + key + "'='" + value + "' returning null", ex);
            }
            return null;
        }
    }

    public static Double getFloatingDecimalPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);
        final String value = map.get(key.toLowerCase(Locale.UK));
        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER.error(
                        "could not parse integer description for '" + key + "'='" + value + "' returning null", ex);
            }
            return null;
        }
    }

    public static Font getFontPropertyValue(final String style) {
        if (style == null) {
            return Font.font(StyleParser.DEFAULT_FONT, StyleParser.DEFAULT_FONT_SIZE);
        }

        try {
            double fontSize = StyleParser.DEFAULT_FONT_SIZE;
            final Double fontSizeObj = StyleParser.getFloatingDecimalPropertyValue(style, XYChartCss.FONT_SIZE);
            if (fontSizeObj != null) {
                fontSize = fontSizeObj;
            }

            FontWeight fontWeight = null;
            final String fontW = StyleParser.getPropertyValue(style, XYChartCss.FONT_WEIGHT);
            if (fontW != null) {
                fontWeight = FontWeight.findByName(fontW);
            }

            FontPosture fontPosture = null;
            final String fontP = StyleParser.getPropertyValue(style, XYChartCss.FONT_POSTURE);
            if (fontP != null) {
                fontPosture = FontPosture.findByName(fontP);
            }

            final String font = StyleParser.getPropertyValue(style, XYChartCss.FONT);
            if (font == null) {
                return Font.font(StyleParser.DEFAULT_FONT, fontWeight, fontPosture, fontSize);
            }

            return Font.font(font, fontWeight, fontPosture, fontSize);

        } catch (final NumberFormatException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER
                        .error("could not parse font description style='" + style + "' returning default font", ex);
            }
            return Font.font(StyleParser.DEFAULT_FONT, StyleParser.DEFAULT_FONT_SIZE);
        }
    }

    public static Integer getIntegerPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);
        final String value = map.get(key.toLowerCase(Locale.UK));
        if (value == null) {
            return null;
        }

        try {
            return Integer.decode(value);
        } catch (final NumberFormatException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER.error(
                        "could not parse integer description for '" + key + "'='" + value + "' returning null", ex);
            }
            return null;
        }
    }

    public static String getPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);

        return map.get(key.toLowerCase(Locale.UK));
    }

    public static double[] getStrokeDashPropertyValue(final String style, final String key) {
        if (style == null || key == null) {
            return null;
        }

        final Map<String, String> map = StyleParser.splitIntoMap(style);
        final String value = map.get(key.toLowerCase(Locale.UK));
        if (value == null) {
            return null;
        }

        try {
            return Arrays.asList(value.split(",\\s*")).stream().map(String::trim).mapToDouble(Double::parseDouble).toArray();
        } catch (final IllegalArgumentException ex) {
            if (LOGGER.isErrorEnabled()) {
                StyleParser.LOGGER.error(
                        "could not parse color description for '" + key + "'='" + value + "' returning null", ex);
            }
            return null;
        }
    }

    public static String mapToString(final Map<String, String> map) {
        String ret = "";
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            ret = ret.concat(key).concat("=").concat(value).concat(";");
        }
        return ret;
    }

    /**
     * spits input string, converts keys and values to lower case, and replaces '"' and ''' if any
     *
     * @param style the input style string
     * @return the sanitised map
     */
    public static Map<String, String> splitIntoMap(final String style) {
        final ConcurrentHashMap<String, String> retVal = new ConcurrentHashMap<>();
        if (style == null) {
            return retVal;
        }

        final String[] keyVals = AT_LEAST_ONE_WHITESPACE_PATTERN.matcher(style.toLowerCase(Locale.UK)).replaceAll("").split(";");
        for (final String keyVal : keyVals) {
            final String[] parts = STYLE_ASSIGNMENT_PATTERN.split(keyVal, 2);
            if (parts == null || parts[0] == null || parts.length <= 1) {
                continue;
            }

            retVal.put(parts[0], QUOTES_PATTERN.matcher(parts[1]).replaceAll(""));
        }

        return retVal;
    }
}
