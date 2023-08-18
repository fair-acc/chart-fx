package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.dataset.utils.DataSetStyleBuilder;
import javafx.scene.paint.Paint;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Parser for styles used in the ErrorDataSetRenderer
 *
 * @author ennerf
 */
public class DataSetStyleParser extends AbstractStyleParser {

    @Override
    protected boolean parseEntry(String key, String value) {
        switch (key) {

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

            case DataSetStyleBuilder.STROKE_WIDTH:
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
