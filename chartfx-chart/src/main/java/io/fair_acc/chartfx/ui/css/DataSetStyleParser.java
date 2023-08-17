package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.XYChartCss;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.marker.Marker;
import io.fair_acc.chartfx.utils.StyleParser;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Parser utility for overwriting dataset styles.
 * Fields that were not styled explicitly are set to null.
 *
 * @author ennerf
 */
public class DataSetStyleParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetStyleParser.class);

    public DataSetStyleParser parse(String dataSetStyle) {
        clear();
        if (dataSetStyle == null || dataSetStyle.isBlank()) {
            return this;
        }

        // parse style:
        final Map<String, String> map = StyleParser.splitIntoMap(dataSetStyle);

        final String markerType = map.get(XYChartCss.MARKER_TYPE.toLowerCase(Locale.UK));
        if (markerType != null) {
            try {
                marker = DefaultMarker.get(markerType);
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("could not parse marker type description for '" + XYChartCss.MARKER_TYPE + "'='" + markerType + "'", ex);
            }
        }

        final String markerSize = map.get(XYChartCss.MARKER_SIZE.toLowerCase(Locale.UK));
        if (markerSize != null) {
            try {
                this.markerSize = Double.parseDouble(markerSize);
            } catch (final NumberFormatException ex) {
                LOGGER.error("could not parse marker size description for '" + XYChartCss.MARKER_SIZE + "'='" + markerSize + "'", ex);
            }
        }

        final String markerColor = map.get(XYChartCss.MARKER_COLOR.toLowerCase(Locale.UK));
        if (markerColor != null) {
            try {
                this.markerColor = Color.web(markerColor);
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("could not parse marker color description for '" + XYChartCss.MARKER_COLOR + "'='" + markerColor + "'", ex);
            }
        }

        final String fillColor = map.get(XYChartCss.FILL_COLOR.toLowerCase(Locale.UK));
        if (markerColor != null) {
            try {
                this.fillColor = Color.web(markerColor);
            } catch (final IllegalArgumentException ex) {
                LOGGER.error("could not parse fill color description for '" + XYChartCss.FILL_COLOR + "'='" + markerColor + "'", ex);
            }
        }


        return this;
    }

    public Optional<Marker> getMarker() {
        return Optional.ofNullable(marker);
    }

    public OptionalDouble getMarkerSize() {
        return Double.isFinite(markerSize) ? OptionalDouble.of(markerSize) : OptionalDouble.empty();
    }

    public Optional<Paint> getMarkerColor() {
        return Optional.ofNullable(markerColor);
    }

    public Optional<Paint> getFillColor() {
        return Optional.ofNullable(fillColor);
    }

    public void clear() {
        marker = null;
        markerSize = Double.NaN;
        markerColor = null;
        fillColor = null;
    }

    private Marker marker;
    private double markerSize;
    private Paint markerColor;
    private Paint fillColor;

}
