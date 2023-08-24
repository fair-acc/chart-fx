package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import javafx.scene.paint.Color;
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


            case DataSetStyleBuilder.INTENSITY:
                return isValid(intensity = parseDouble(value));


            case DataSetStyleBuilder.MARKER_TYPE:
                return isValid(markerType = parse(value, DefaultMarker::get));
            case DataSetStyleBuilder.MARKER_LINE_WIDTH:
                return isValid(markerLineWidth = parseDouble(value));
            case DataSetStyleBuilder.MARKER_SIZE:
                return isValid(markerSize = parseDouble(value));
            case DataSetStyleBuilder.MARKER_COLOR:
                return isValid(markerColor = parseColor(value));
            case DataSetStyleBuilder.MARKER_LINE_DASHES:
                return isValid(markerLineDashes = parseDoubleArray(value));


            case DataSetStyleBuilder.LINE_WIDTH:
                return isValid(lineWidth = parseDouble(value));
            case DataSetStyleBuilder.LINE_COLOR:
                return isValid(lineColor = parseColor(value));
            case DataSetStyleBuilder.LINE_DASHES:
                return isValid(lineDashes = parseDoubleArray(value));


            case DataSetStyleBuilder.FILL_COLOR:
                return isValid(fillColor = parseColor(value));
            case DataSetStyleBuilder.STROKE_COLOR:
                return isValid(strokeColor = parseColor(value));
            case DataSetStyleBuilder.STROKE_WIDTH:
                return isValid(strokeWidth = parseDouble(value));
            case DataSetStyleBuilder.STROKE_DASH_PATTERN:
                return isValid(strokeDashPattern = parseDoubleArray(value));


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


    // Generic
    public Optional<Boolean> getVisible() {
        return optional(visible);
    }

    public OptionalDouble getIntensity() {
        return optional(intensity);
    }

    // Marker
    public Optional<Marker> getMarkerType() {
        return optional(markerType);
    }

    public OptionalDouble getMarkerLineWidth() {
        return optional(markerLineWidth);
    }

    public OptionalDouble getMarkerSize() {
        return optional(markerSize);
    }

    public Optional<Paint> getMarkerColor() {
        return optional(markerColor);
    }

    public Optional<double[]> getMarkerLineDashes() {
        return optional(markerLineDashes);
    }

    // Line
    public OptionalDouble getLineWidth() {
        return optional(lineWidth);
    }

    public Optional<Paint> getLineColor() {
        return optional(lineColor);
    }

    public Optional<double[]> getLineDashes() {
        return optional(lineDashes);
    }

    // Shape
    public Optional<Paint> getFillColor() {
        return optional(fillColor);
    }

    public Optional<Paint> getStrokeColor() {
        return optional(strokeColor);
    }

    public OptionalDouble getStrokeWidth() {
        return optional(strokeWidth);
    }

    public Optional<double[]> getStrokeDashes() {
        return optional(strokeDashPattern);
    }

    // Text
    public Optional<Font> getFont() {
        return optional(font);
    }

    public Optional<FontWeight> getFontWeight() {
        return optional(fontWeight);
    }

    public OptionalDouble getFontSize() {
        return optional(fontSize);
    }

    public Optional<FontPosture> getFontStyle() {
        return optional(fontStyle);
    }

    protected void clear() {
        // Generic
        visible = null;
        intensity = Double.NaN;

        // Marker
        markerType = null;
        markerLineWidth = Double.NaN;
        markerSize = Double.NaN;
        markerColor = null;
        markerLineDashes = null;

        // Line
        lineWidth = Double.NaN;
        lineColor = null;
        lineDashes = null;

        // Shape
        fillColor = null;
        strokeColor = null;
        strokeWidth = Double.NaN;
        strokeDashPattern = null;

        // Text
        font = null;
        fontWeight = null;
        fontSize = Double.NaN;
        fontStyle = null;
    }

    // Generic
    private Boolean visible;
    private double intensity;

    // Marker
    private Marker markerType;
    private double markerLineWidth;
    private double markerSize;
    private Paint markerColor;
    private double[] markerLineDashes;

    // Line
    private double lineWidth;
    private Color lineColor;
    private double[] lineDashes;

    // Shape
    private Paint fillColor;
    private Paint strokeColor;
    private double strokeWidth;
    private double[] strokeDashPattern;


    // Text
    private Font font;
    private FontWeight fontWeight;
    private double fontSize;
    private FontPosture fontStyle;

}
