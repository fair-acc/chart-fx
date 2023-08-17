package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import javafx.scene.paint.Paint;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Parser for styles used in the ErrorDataSetRenderer
 *
 * @author ennerf
 */
public class ErrorStyleParser extends AbstractStyleParser {

    @Override
    protected boolean parseEntry(String key, String value) {
        // TODO: account for lowercase and/or switch to valid CSS names
        switch (key) {

            case XYChartCss.MARKER_TYPE:
                return isValid(marker = parse(value, DefaultMarker::get));

            case XYChartCss.FILL_COLOR:
                return isValid(fillColor = parseColor(value));

            case XYChartCss.MARKER_COLOR:
                return isValid(markerColor = parseColor(value));

            case XYChartCss.STROKE_COLOR:
                return isValid(strokeColor = parseColor(value));

            case XYChartCss.MARKER_SIZE:
                return isValid(markerSize = parseDouble(value));

            case XYChartCss.STROKE_WIDTH:
                return isValid(lineWidth = parseDouble(value));

            default:
                return false;
        }
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

    public Optional<Paint> getMarkerColor() {
        return optional(markerColor);
    }

    public Optional<Paint> getFillColor() {
        return optional(fillColor);
    }

    public Optional<Paint> getLineColor() {
        return optional(strokeColor);
    }

    protected void clear() {
        marker = null;
        markerSize = Double.NaN;
        lineWidth = Double.NaN;
        markerColor = null;
        fillColor = null;
        strokeColor = null;
    }

    private Marker marker;
    private double markerSize;
    private double lineWidth;
    private Paint markerColor;
    private Paint fillColor;
    private Paint strokeColor;

}
