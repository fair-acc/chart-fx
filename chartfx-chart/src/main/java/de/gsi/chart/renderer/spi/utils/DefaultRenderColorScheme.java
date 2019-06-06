package de.gsi.chart.renderer.spi.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import de.gsi.chart.XYChartCss;
import de.gsi.dataset.utils.AssertUtils;
import de.gsi.chart.utils.StyleParser;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

public final class DefaultRenderColorScheme {

    private static final DefaultRenderColorScheme SELF = new DefaultRenderColorScheme();
    public static final String DEFAULT_FONT = "Helvetia";
    public static final int DEFAULT_FONT_SIZE = 18;

    public static final Color[] MISC = { Color.valueOf("#5DA5DA"), // (blue)
            Color.valueOf("#F15854"), // (red)
            Color.valueOf("#FAA43A"), // (orange)
            Color.valueOf("#60BD68"), // (green)
            Color.valueOf("#F17CB0"), // (pink)
            Color.valueOf("#B2912F"), // (brown)
            Color.valueOf("#B276B2"), // (purple)
            Color.valueOf("#DECF3F"), // (yellow)
            Color.valueOf("#4D4D4D"), // (gray)
    };

    public static final Color[] ADOBE = { Color.valueOf("#00a4e4"), // blue
            Color.valueOf("#ff0000"), // red
            Color.valueOf("#fbb034"), // orange
            Color.valueOf("#ffdd00"), // yellow
            Color.valueOf("#c1d82f"), // green
            Color.valueOf("#8a7967"), // brown
            Color.valueOf("#6a737b") // darkbrown/black
    };

    public static final Color[] TUNEVIEWER = { // old legacy colour scheme from
            // an earlier project
            Color.valueOf("#0000c8"), // dark blue
            Color.valueOf("#c80000"), // dark red
            Color.valueOf("#00c800"), // dark green
            Color.ORANGE, // orange
            Color.MAGENTA, // magenta
            Color.CYAN, // cyan
            Color.DARKGRAY, // dark grey
            Color.PINK, // pink
            Color.BLACK // black
    };

    // https://brandcolors.net/
    // check-out
    // AIESEC
    // DELL
    // Duolingo (not bad)

    private static final Color[] COLORS = DefaultRenderColorScheme.TUNEVIEWER;

    private static Paint[] fillStyles;
    private static double defaultLineWidth = 0.5;
    private static double lineWidth = 1.5;
    private static double hatchShiftByIndex = 1.5;

    private DefaultRenderColorScheme() {

    }

    private static synchronized void init() {
        if (DefaultRenderColorScheme.fillStyles != null) {
            return;
        }
        DefaultRenderColorScheme.fillStyles = new Paint[DefaultRenderColorScheme.COLORS.length];
        for (int i = 0; i < DefaultRenderColorScheme.COLORS.length; i++) {
            DefaultRenderColorScheme.fillStyles[i] = FillPatternStyle.getDefaultHatch(
                    DefaultRenderColorScheme.COLORS[i].brighter(), i * DefaultRenderColorScheme.hatchShiftByIndex);
        }
    }

    private static Color getColorModifier(final Map<String, List<String>> parameterMap, final Color orignalColor) {
        Color color = orignalColor;

        final List<String> intensityModifier = parameterMap.get(XYChartCss.DATASET_INTENSITY.toLowerCase(Locale.UK));
        if (color != null && intensityModifier != null && !intensityModifier.isEmpty()) {            
            try {
                final double intensity = Double.parseDouble(intensityModifier.get(0));
                color = color.deriveColor(0, intensity / 100, 1.0, intensity / 100);
            } catch (final NumberFormatException e) {
                // re-use unmodified original color              
            }            
        }

        return color;
    }

    public static Color getColor(final int index) {
        AssertUtils.gtEqThanZero("color index", index);
        DefaultRenderColorScheme.init();
        return DefaultRenderColorScheme.COLORS[index % DefaultRenderColorScheme.COLORS.length];
    }

    public static Paint getFill(final int index) {
        AssertUtils.gtEqThanZero("fillStyles index", index);
        DefaultRenderColorScheme.init();
        return DefaultRenderColorScheme.fillStyles[index % DefaultRenderColorScheme.fillStyles.length];
    }

    public static void setGraphicsContextAttributes(final GraphicsContext gc, final String style) {
        if (gc == null || style == null) {
            return;
        }

        final Color strokeColor = StyleParser.getColorPropertyValue(style, XYChartCss.STROKE_COLOR);
        if (strokeColor != null) {
            gc.setStroke(strokeColor);
        }

        final Color fillColor = StyleParser.getColorPropertyValue(style, XYChartCss.FILL_COLOR);
        if (fillColor != null) {
            gc.setFill(fillColor);
        }

        final Double strokeWidth = StyleParser.getFloatingDecimalPropertyValue(style, XYChartCss.STROKE_WIDTH);
        if (strokeWidth != null) {
            gc.setLineWidth(strokeWidth);
        }

        final Font font = StyleParser.getFontPropertyValue(style);
        if (font != null) {
            gc.setFont(font);
        }

        final double[] dashPattern = StyleParser.getFloatingDecimalArrayPropertyValue(style,
                XYChartCss.STROKE_DASH_PATTERN);
        if (dashPattern != null) {
            gc.setLineDashes(dashPattern);
        }
    }

    public static void setLineScheme(final GraphicsContext gc, final String defaultStyle, final int dsIndex) {
        AssertUtils.gtEqThanZero("setLineScheme dsIndex", dsIndex);
        DefaultRenderColorScheme.init();
        final Map<String, List<String>> map = DefaultRenderColorScheme.splitQuery(defaultStyle);
        
        final Color lineColor = StyleParser.getColorPropertyValue(defaultStyle, XYChartCss.DATASET_STROKE_COLOR);
        final Color rawColor = lineColor == null ? DefaultRenderColorScheme.getColor(dsIndex) : lineColor;

        gc.setLineWidth(DefaultRenderColorScheme.lineWidth);
        gc.setFill(DefaultRenderColorScheme.getFill(dsIndex));
        gc.setStroke(DefaultRenderColorScheme.getColorModifier(map, rawColor));
    }

    public static void setFillScheme(final GraphicsContext gc, final String defaultStyle, final int dsIndex) {
        AssertUtils.gtEqThanZero("setFillScheme dsIndex", dsIndex);
        final Map<String, List<String>> map = DefaultRenderColorScheme.splitQuery(defaultStyle);
        
        final Color fillColor = StyleParser.getColorPropertyValue(defaultStyle, XYChartCss.FILL_COLOR);
        final Color rawColor = fillColor == null ? DefaultRenderColorScheme.getColor(dsIndex) : fillColor;

        final Color color = DefaultRenderColorScheme.getColorModifier(map, rawColor);
        if (color == null) {
            return;
        }

        final ImagePattern hatch = FillPatternStyle.getDefaultHatch(color.brighter(),
                dsIndex * DefaultRenderColorScheme.hatchShiftByIndex);

        gc.setFill(hatch);
    }

    public static void setMarkerScheme(final GraphicsContext gc, final String defaultStyle, final int dsIndex) {
        AssertUtils.gtEqThanZero("setMarkerScheme dsIndex", dsIndex);
        final Map<String, List<String>> map = DefaultRenderColorScheme.splitQuery(defaultStyle);

        final Color color = DefaultRenderColorScheme.getColorModifier(map, DefaultRenderColorScheme.getColor(dsIndex));

        gc.setLineWidth(DefaultRenderColorScheme.defaultLineWidth);
        gc.setStroke(color);
        gc.setFill(color);
    }

    private static Map<String, List<String>> splitQuery(final String styleString) {
        if (styleString == null || styleString.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return Arrays.stream(styleString.split(";")).map(DefaultRenderColorScheme.SELF::splitQueryParameter)
                .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private SimpleImmutableEntry<String, String> splitQueryParameter(final String it) {
        final int idx = it.indexOf('=');
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, value);
    }
}
