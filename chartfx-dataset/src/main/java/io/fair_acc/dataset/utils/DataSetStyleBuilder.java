package io.fair_acc.dataset.utils;

/**
 * Utility class for generating Chart CSS without a JavaFX dependency.
 *
 * @author ennerf
 */
public class DataSetStyleBuilder<T extends DataSetStyleBuilder<T>> extends StyleBuilder<T> {

    public static DataSetStyleBuilder<?> getInstance() {
        return instance.get().reset();
    }

    private static final ThreadLocal<DataSetStyleBuilder<?>> instance = ThreadLocal.withInitial(DataSetStyleBuilder::new);

    public static final String COLOR_INDEX = "-fx-color-index";

    public T setColorIndex(int value) {
        return setIntegerProp(COLOR_INDEX, value);
    }

    public static final String INTENSITY = "-fx-intensity";

    public T setIntensity(double value) {
        return setDoubleProp(INTENSITY, value);
    }

    public static final String SHOW_IN_LEGEND = "-fx-show-in-legend";

    public T setShowInLegend(boolean value) {
        return setBooleanProp(SHOW_IN_LEGEND, value);
    }

    public static final String FILL_COLOR = "-fx-fill";

    public T setFill(String color) {
        return setStringProp(FILL_COLOR, color);
    }

    public T setFill(int r, int g, int b, double a) {
        return setColorProp(FILL_COLOR, r, g, b, a);
    }

    public static final String STROKE_COLOR = "-fx-stroke";

    public T setStroke(String color) {
        return setStringProp(STROKE_COLOR, color);
    }

    public T setStroke(int r, int g, int b, double a) {
        return setColorProp(STROKE_COLOR, r, g, b, a);
    }

    public static final String STROKE_WIDTH = "-fx-stroke-width";

    public T setStrokeWidth(double value) {
        return setDoubleProp(STROKE_WIDTH, value);
    }

    public static final String MARKER_COLOR = "-fx-marker-color"; // TODO: not used yet

    public T setMarkerColor(String color) {
        return setStringProp(MARKER_COLOR, color);
    }

    public T setMarkerColor(int r, int g, int b, double a) {
        return setColorProp(MARKER_COLOR, r, g, b, a);
    }

    public static final String MARKER_SIZE = "-fx-marker-size";

    public T setMarkerSize(double value) {
        return setDoubleProp(MARKER_SIZE, value);
    }

    public static final String MARKER_TYPE = "-fx-marker-type";

    /**
     * @param value DefaultMarker value, e.g., "rectangle", "circle2" etc.
     * @return this
     */
    public T setMarkerType(String value) {
        return setStringProp(MARKER_TYPE, value);
    }

    public static final String STROKE_DASH_PATTERN = "-fx-stroke-dash-array";

    public T setStrokeDashPattern(double... pattern) {
        return setDoubleArray(STROKE_DASH_PATTERN, pattern);
    }

    public static final String VISIBILITY = "visibility";
    public T setVisible(boolean value) {
        return setStringProp(VISIBILITY, value ? "visible" : "hidden");
    }

    public static final String FONT_FAMILY = "-fx-font-family";
    public T setFontFamily(String value) {
        return setStringProp(FONT_FAMILY, value);
    }

    public static final String FONT_WEIGHT = "-fx-font-weight";
    public T setFontWeight(String value) {
        return setStringProp(FONT_WEIGHT, value);
    }

    public static final String FONT_SIZE = "-fx-font-size";

    public T setFontSize(String size) {
        return setStringProp(FONT_SIZE, size);
    }

    public T setFontSizePx(double value) {
        return setFontSize(value + "px");
    }

    public T setFontSizeEm(double value) {
        return setFontSize(value + "em");
    }

}
