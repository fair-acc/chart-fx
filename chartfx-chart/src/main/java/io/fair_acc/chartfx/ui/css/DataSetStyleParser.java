package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Parser for styles used in the ErrorDataSetRenderer
 *
 * @author ennerf
 */
public class DataSetStyleParser extends AbstractStyleParser {

    public static DataSetStyleParser newInstance() {
        return new DataSetStyleParser();
    }

    protected DataSetStyleParser() {
    }

    @Override
    protected boolean parseEntry(String key, String value) {
        switch (key) {

            case DataSetStyleBuilder.VISIBILITY:
                return isValid(visible = parse(value, str -> {
                    switch (str) {
                        case "visible":
                            return true;
                        case "hidden":
                            return false;
                    }
                    return null;
                }));

            case DataSetStyleBuilder.MARKER_TYPE:
                return isValid(marker = parse(value, DefaultMarker::get));

            case DataSetStyleBuilder.FILL_COLOR:
                return isValid(fillColor = parseColor(value));

            case DataSetStyleBuilder.MARKER_COLOR:
                return isValid(markerColor = parseColor(value));

            case DataSetStyleBuilder.STROKE_COLOR:
                return isValid(strokeColor = parseColor(value));

            case DataSetStyleBuilder.MARKER_SIZE:
                return isValid(markerSize = parseDouble(value));

            case DataSetStyleBuilder.INTENSITY:
                return isValid(intensity = parseDouble(value));

            case DataSetStyleBuilder.STROKE_WIDTH:
                return isValid(lineWidth = parseDouble(value));

            case DataSetStyleBuilder.STROKE_DASH_PATTERN:
                return isValid(lineDashPattern = parseDoubleArray(value));

            case DataSetStyleBuilder.FONT:
                return isValid(font = parse(value, Font::font));

            case DataSetStyleBuilder.FONT_WEIGHT:
                return isValid(fontWeight = parse(value, FontWeight::findByName));

            case DataSetStyleBuilder.FONT_SIZE:
                return isValid(fontSize = parseDouble(value));

            case DataSetStyleBuilder.FONT_STYLE:
                    return isValid(fontStyle = parse(value, FontPosture::findByName));

            default:
                return false;
        }
    }

    public Optional<Boolean> getVisible() {
        return optional(visible);
    }

    public Optional<Marker> getMarker() {
        return optional(marker);
    }

    public OptionalDouble getMarkerSize() {
        return optional(markerSize);
    }

    public OptionalDouble getLineWidth() {
        return optional(lineWidth);
    }
    public OptionalDouble getIntensity() {
        return optional(intensity);
    }

    public Optional<double[]> getLineDashPattern() {
        return optional(lineDashPattern);
    }

    public Optional<Paint> getMarkerColor() {
        return optional(markerColor);
    }

    public Optional<Paint> getFillColor() {
        return optional(fillColor);
    }

    public Optional<Paint> getStrokeColor() {
        return optional(strokeColor);
    }

    public Optional<Paint> getLineColor() {
        return getStrokeColor();
    }

    public Optional<Font> getFont() {
        return optional(font);
    }

    public Optional<FontPosture> getFontStyle() {
        return optional(fontStyle);
    }

    public Optional<FontWeight> getFontWeight() {
        return optional(fontWeight);
    }

    protected void clear() {
        marker = null;
        visible = null;
        markerSize = Double.NaN;
        intensity = Double.NaN;
        lineWidth = Double.NaN;
        lineDashPattern = null;
        markerColor = null;
        fillColor = null;
        strokeColor = null;
        font = null;
        fontWeight = null;
        fontStyle = null;
        fontSize = Double.NaN;
    }

    private Marker marker;
    private Boolean visible;
    private double markerSize;
    private double intensity;
    private double lineWidth;
    private double[] lineDashPattern;
    private Paint markerColor;
    private Paint fillColor;
    private Paint strokeColor;
    private Font font;
    private FontWeight fontWeight;
    private FontPosture fontStyle;
    private double fontSize;

}
