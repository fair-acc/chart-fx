package io.fair_acc.chartfx.renderer.spi.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.chartfx.ui.css.StyleUtil;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.utils.AssertUtils;

@SuppressWarnings("PMD.FieldNamingConventions")
public final class DefaultRenderColorScheme {

    public static PseudoClass PALETTE_MISC = PseudoClass.getPseudoClass("misc");
    public static PseudoClass PALETTE_ADOBE = PseudoClass.getPseudoClass("adobe");
    public static PseudoClass PALETTE_DELL = PseudoClass.getPseudoClass("dell");
    public static PseudoClass PALETTE_EQUIDISTANT = PseudoClass.getPseudoClass("equidistant");
    public static PseudoClass PALETTE_TUNEVIEWER = PseudoClass.getPseudoClass("tuneviewer");
    public static PseudoClass PALETTE_MATLAB = PseudoClass.getPseudoClass("matlab");
    public static PseudoClass PALETTE_MATLAB_DARK = PseudoClass.getPseudoClass("matlab-dark");
    
    private static final String DEFAULT_FONT = "Helvetica";
    private static final int DEFAULT_FONT_SIZE = 18;
    private static final DefaultRenderColorScheme SELF = new DefaultRenderColorScheme();

    public static final ObservableList<Color> MISC = FXCollections.observableList(Arrays.asList( //
            Color.valueOf("#5DA5DA"), // (blue)
            Color.valueOf("#F15854"), // (red)
            Color.valueOf("#FAA43A"), // (orange)
            Color.valueOf("#60BD68"), // (green)
            Color.valueOf("#F17CB0"), // (pink)
            Color.valueOf("#B2912F"), // (brown)
            Color.valueOf("#B276B2"), // (purple)
            Color.valueOf("#DECF3F"), // (yellow)
            Color.valueOf("#4D4D4D") // (gray)
            ));

    public static final ObservableList<Color> ADOBE = FXCollections.observableList(Arrays.asList( //
            Color.valueOf("#00a4e4"), // blue
            Color.valueOf("#ff0000"), // red
            Color.valueOf("#fbb034"), // orange
            Color.valueOf("#ffdd00"), // yellow
            Color.valueOf("#c1d82f"), // green
            Color.valueOf("#8a7967"), // brown
            Color.valueOf("#6a737b") // darkbrown/black
            ));

    public static final ObservableList<Color> DELL = FXCollections.observableList(Arrays.asList( //
            Color.valueOf("#0085c3"), //
            Color.valueOf("#7ab800"), //
            Color.valueOf("#f2af00"), //
            Color.valueOf("#dc5034"), //
            Color.valueOf("#6e2585"), //
            Color.valueOf("#71c6c1"), //
            Color.valueOf("#009bbb"), //
            Color.valueOf("#444444") //
            ));

    public static final ObservableList<Color> EQUIDISTANT = FXCollections.observableList(Arrays.asList( //
            Color.valueOf("#003f5c"), //
            Color.valueOf("#2f4b7c"), //
            Color.valueOf("#665191"), //
            Color.valueOf("#a05195"), //
            Color.valueOf("#d45087"), //
            Color.valueOf("#f95d6a"), //
            Color.valueOf("#ff7c43"), //
            Color.valueOf("#ffa600") //
            ));

    public static final ObservableList<Color> TUNEVIEWER = FXCollections.observableList(Arrays.asList( //
            // old legacy colour scheme from an earlier project
            Color.valueOf("#0000c8"), // dark blue
            Color.valueOf("#c80000"), // dark red
            Color.valueOf("#00c800"), // dark green
            Color.ORANGE, // orange
            Color.MAGENTA, // magenta
            Color.CYAN, // cyan
            Color.DARKGRAY, // dark grey
            Color.PINK, // pink
            Color.BLACK // black
            ));

    private static final ListProperty<Color> strokeColours = new SimpleListProperty<>(SELF, "defaulStrokeColours", FXCollections.observableList(TUNEVIEWER));

    private static final ListProperty<Color> fillColours = new SimpleListProperty<>(SELF, "defaulFillColours", FXCollections.observableList(TUNEVIEWER));
    private static final ListProperty<Paint> fillStyles = new SimpleListProperty<>(SELF, "fillStyles");
    private static final ObjectProperty<Font> defaultFont = new SimpleObjectProperty<>(SELF, "defaultFontSize", Font.font(DEFAULT_FONT, DEFAULT_FONT_SIZE));
    private static final DoubleProperty markerLineWidth = new SimpleDoubleProperty(SELF, "defaultLineWidth", 0.5);
    private static final DoubleProperty lineWidth = new SimpleDoubleProperty(SELF, "lineWidth", 1.5);
    private static final DoubleProperty hatchShiftByIndex = new SimpleDoubleProperty(SELF, "hatchShiftByIndex", 1.5);
    static {
        fillStylesProperty().clear();
        fillStylesProperty().set(getStandardFillStyle());
    }

    private DefaultRenderColorScheme() {
    }

    private SimpleImmutableEntry<String, String> splitQueryParameter(final String it) {
        final int idx = it.indexOf('=');
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = (idx > 0) && (it.length() > (idx + 1)) ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, value);
    }

    public static DoubleProperty defaultStrokeLineWidthProperty() {
        return lineWidth;
    }

    public static ListProperty<Color> fillColorProperty() {
        return fillColours;
    }

    public static ListProperty<Paint> fillStylesProperty() {
        return fillStyles;
    }

    public static ObjectProperty<Font> fontProperty() {
        return defaultFont;
    }

    private static Paint getModifiedColor(Paint color, double intensity) {
        if (!(color instanceof Color)) {
            return color;
        }
        if(intensity < 0 || intensity >= 100) {
            return color;
        }
        final double scale = intensity / 100;
        return ((Color) color).deriveColor(0, scale, 1.0, scale);
    }

    private static Color getColorModifier(final Map<String, List<String>> parameterMap, final Color orignalColor) {
        Color color = orignalColor;

        final List<String> intensityModifier = parameterMap.get(XYChartCss.DATASET_INTENSITY.toLowerCase(Locale.UK));
        if ((color != null) && (intensityModifier != null) && !intensityModifier.isEmpty()) {
            try {
                final double intensity = Double.parseDouble(intensityModifier.get(0));
                color = color.deriveColor(0, intensity / 100, 1.0, intensity / 100);
            } catch (final NumberFormatException e) {
                // re-use unmodified original color
            }
        }

        return color;
    }

    public static Paint getFill(final int index) {
        AssertUtils.gtEqThanZero("fillStyles index", index);
        final int size = fillStylesProperty().size();
        return fillStylesProperty().get(index % size);
    }

    public static Color getFillColor(final int index) {
        AssertUtils.gtEqThanZero("color index", index);
        final int size = fillColorProperty().size();
        return fillColorProperty().get(index % size);
    }

    public static ObservableList<Paint> getStandardFillStyle() {
        final ObservableList<Paint> values = FXCollections.observableArrayList();
        for (Color colour : fillColorProperty().get()) {
            values.add(FillPatternStyleHelper.getDefaultHatch(colour.brighter(), hatchShiftByIndexProperty().get()));
        }
        return values;
    }

    public static Color getStrokeColor(final int index) {
        AssertUtils.gtEqThanZero("color index", index);
        final int size = strokeColorProperty().size();
        return strokeColorProperty().get(index % size);
    }

    public static DoubleProperty hatchShiftByIndexProperty() {
        return hatchShiftByIndex;
    }

    public static DoubleProperty markerLineWidthProperty() {
        return markerLineWidth;
    }

    public static void setFillScheme(final GraphicsContext gc, final DataSetNode dataSetNode) {
        setFillScheme(gc, dataSetNode.getStyle(), dataSetNode.getColorIndex());
    }

    @Deprecated
    public static void setFillScheme(final GraphicsContext gc, final String defaultStyle, final int dsIndex) {
        AssertUtils.gtEqThanZero("setFillScheme dsIndex", dsIndex);
        final Map<String, List<String>> map = splitQuery(defaultStyle);

        final Color fillColor = StyleParser.getColorPropertyValue(defaultStyle, XYChartCss.FILL_COLOR);
        if (fillColor != null) {
            final Color color = getColorModifier(map, fillColor);
            if (color == null) {
                return;
            }

            final ImagePattern hatch = FillPatternStyleHelper.getDefaultHatch(color.brighter(),
                    dsIndex * hatchShiftByIndexProperty().get());

            gc.setFill(hatch);
        } else {
            final int size = fillStylesProperty().size();
            gc.setFill(fillStylesProperty().get(dsIndex % size));
        }
    }

    public static void setGraphicsContextAttributes(final GraphicsContext gc, final DataSetNode style) {
        setGraphicsContextAttributes(gc, style.getStyle());
    }

    @Deprecated
    public static void setGraphicsContextAttributes(final GraphicsContext gc, final String style) {
        if ((gc == null) || (style == null)) {
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

    public static void setLineScheme(final GraphicsContext gc, final DataSetNode dataSet) {
//        setLineScheme(gc, dataSet.getStyle(), dataSet.getColorIndex());
        gc.setLineWidth(dataSet.getStrokeWidth());
        StyleUtil.copyLineDashes(gc, dataSet);
        gc.setFill(dataSet.getFill());
        gc.setStroke(getModifiedColor(dataSet.getStroke(), dataSet.getIntensity()));
    }

    @Deprecated // TODO: replace with css colors
    public static void setLineScheme(final GraphicsContext gc, final String defaultStyle, final int dsIndex) {
        AssertUtils.gtEqThanZero("setLineScheme dsIndex", dsIndex);
        final Map<String, List<String>> map = splitQuery(defaultStyle);

        final Color lineColor = StyleParser.getColorPropertyValue(defaultStyle, XYChartCss.DATASET_STROKE_COLOR);
        final double[] lineDash = StyleParser.getStrokeDashPropertyValue(defaultStyle, XYChartCss.STROKE_DASH_PATTERN);
        final Color rawColor = lineColor == null ? getStrokeColor(dsIndex) : lineColor;

        gc.setLineWidth(defaultStrokeLineWidthProperty().get());
        gc.setLineDashes(lineDash);
        gc.setFill(getFill(dsIndex));
        gc.setStroke(getColorModifier(map, rawColor));
    }

    public static void setMarkerScheme(final GraphicsContext gc, final DataSetNode dataSetNode) {
//        setMarkerScheme(gc, dataSetNode.getStyle(), dataSetNode.getColorIndex());
        var color = getModifiedColor(dataSetNode.getStroke(), dataSetNode.getIntensity());
        gc.setLineWidth(dataSetNode.getMarkerStrokeWidth());
        gc.setStroke(color);
        gc.setFill(color);
    }

    @Deprecated // TODO: replace with CSS colors
    public static void setMarkerScheme(final GraphicsContext gc, final String defaultStyle, final int dsIndex) {
        AssertUtils.gtEqThanZero("setMarkerScheme dsIndex", dsIndex);
        final Map<String, List<String>> map = splitQuery(defaultStyle);

        final Color color = getColorModifier(map, getStrokeColor(dsIndex));

        gc.setLineWidth(markerLineWidthProperty().get());
        gc.setStroke(color);
        gc.setFill(color);
    }

    private static Map<String, List<String>> splitQuery(final String styleString) {
        if ((styleString == null) || styleString.isEmpty()) {
            return Collections.emptyMap();
        }

        return Arrays.stream(styleString.split(";")).map(SELF::splitQueryParameter).collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public static ListProperty<Color> strokeColorProperty() {
        return strokeColours;
    }

    public enum Palette {
        P_TUNEVIEWER(TUNEVIEWER),
        P_MISC(MISC),
        P_ADOBE(ADOBE),
        P_DELL(DELL),
        P_EQUIDISTANT(EQUIDISTANT);

        ObservableList<Color> list;

        Palette(ObservableList<Color> list) {
            this.list = list;
        }

        public ObservableList<Color> getPalette() {
            return list;
        }

        public static Palette getValue(ObservableList<Color> list) {
            for (Palette p : Palette.values()) {
                if (p.getPalette().equals(list)) {
                    return p;
                }
            }
            throw new IllegalArgumentException("unknown palette");
        }
    }
}
