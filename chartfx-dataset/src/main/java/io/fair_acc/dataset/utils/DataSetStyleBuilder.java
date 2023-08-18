package io.fair_acc.dataset.utils;

/**
 * Utility class for generating Chart CSS without a JavaFX dependency.
 *
 * @author ennerf
 */
public class DataSetStyleBuilder extends StyleBuilder<DataSetStyleBuilder> {

    public static DataSetStyleBuilder instance() {
        return instance.get().reset();
    }

    public static DataSetStyleBuilder newInstance() {
        return new DataSetStyleBuilder();
    }

    protected DataSetStyleBuilder() {
    }

    private static final ThreadLocal<DataSetStyleBuilder> instance = ThreadLocal.withInitial(DataSetStyleBuilder::new);

    public static final String COLOR_INDEX = "-fx-color-index";

    public DataSetStyleBuilder setColorIndex(int value) {
        return setIntegerProp(COLOR_INDEX, value);
    }

    public static final String INTENSITY = "-fx-intensity";

    public DataSetStyleBuilder setIntensity(double value) {
        return setDoubleProp(INTENSITY, value);
    }

    public static final String SHOW_IN_LEGEND = "-fx-show-in-legend";

    public DataSetStyleBuilder setShowInLegend(boolean value) {
        return setBooleanProp(SHOW_IN_LEGEND, value);
    }

    public static final String FILL_COLOR = "-fx-fill";

    public DataSetStyleBuilder setFill(String color) {
        return setStringProp(FILL_COLOR, color);
    }

    public DataSetStyleBuilder setFill(int r, int g, int b, double a) {
        return setColorProp(FILL_COLOR, r, g, b, a);
    }

    public static final String STROKE_COLOR = "-fx-stroke";

    public DataSetStyleBuilder setStroke(String color) {
        return setStringProp(STROKE_COLOR, color);
    }

    public DataSetStyleBuilder setStroke(int r, int g, int b, double a) {
        return setColorProp(STROKE_COLOR, r, g, b, a);
    }

    public static final String STROKE_WIDTH = "-fx-stroke-width";

    public DataSetStyleBuilder setStrokeWidth(double value) {
        return setDoubleProp(STROKE_WIDTH, value);
    }

    public static final String MARKER_COLOR = "-fx-marker-color"; // TODO: not used yet

    public DataSetStyleBuilder setMarkerColor(String color) {
        return setStringProp(MARKER_COLOR, color);
    }

    public DataSetStyleBuilder setMarkerColor(int r, int g, int b, double a) {
        return setColorProp(MARKER_COLOR, r, g, b, a);
    }

    public static final String MARKER_SIZE = "-fx-marker-size";

    public DataSetStyleBuilder setMarkerSize(double value) {
        return setDoubleProp(MARKER_SIZE, value);
    }

    public static final String MARKER_TYPE = "-fx-marker-type";

    /**
     * @param value DefaultMarker value, e.g., "rectangle", "circle2" etc.
     * @return this
     */
    public DataSetStyleBuilder setMarkerType(String value) {
        return setStringProp(MARKER_TYPE, value);
    }

    public static final String STROKE_DASH_PATTERN = "-fx-stroke-dash-array";

    public DataSetStyleBuilder setStrokeDashPattern(double... pattern) {
        return setDoubleArray(STROKE_DASH_PATTERN, pattern);
    }

    public static final String VISIBILITY = "visibility";
    public DataSetStyleBuilder setVisible(boolean value) {
        return setStringProp(VISIBILITY, value ? "visible" : "hidden");
    }

    public static final String FONT = "-fx-font";
    public DataSetStyleBuilder setFont(String value) {
        return setStringProp(FONT, value);
    }

    public static final String FONT_FAMILY = "-fx-font-family";
    public DataSetStyleBuilder setFontFamily(String value) {
        return setStringProp(FONT_FAMILY, value);
    }

    public static final String FONT_WEIGHT = "-fx-font-weight";
    public DataSetStyleBuilder setFontWeight(String value) {
        return setStringProp(FONT_WEIGHT, value);
    }

    public static final String FONT_SIZE = "-fx-font-size";

    public DataSetStyleBuilder setFontSize(String size) {
        return setStringProp(FONT_SIZE, size);
    }

    public DataSetStyleBuilder setFontSize(double value) {
        return setDoubleProp(FONT_SIZE, value); // TODO: 'px' currently not supported
    }

    public static final String FONT_STYLE = "-fx-font-style";

    public DataSetStyleBuilder setFontItalic(boolean italic) {
        return setStringProp(FONT_STYLE, italic ? "italic" : "regular");
    }

}
