package de.gsi.chart.plugins.measurements;

import static de.gsi.chart.axes.AxisMode.X;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.ACC;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.HORIZONTAL;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.INDICATOR;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.VERTICAL;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType.INTEGRAL;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType.TRANSMISSION_ABS;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType.TRANSMISSION_REL;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType.VALUE_HOR;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType.VALUE_VER;

import org.controlsfx.tools.Borders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.data.DataSet;
import de.gsi.chart.plugins.measurements.utils.SimpleDataSetEstimators;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Simple DataSet parameter measurements N.B. this contains only algorithms w/o
 * external library dependencies (ie. fitting routines, etc.)
 *
 * @author rstein
 */
public class SimpleMeasurements extends ValueIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMeasurements.class);
    private static final double DEFAULT_MIN = Double.NEGATIVE_INFINITY;
    private static final double DEFAULT_MAX = Double.POSITIVE_INFINITY;

    public enum MeasurementCategory {
        INDICATOR("Indicators"), VERTICAL("Vertical Measurements"), HORIZONTAL("Horizontal Measurements"), ACC(
                "Accelerator Misc.");

        private String name;

        MeasurementCategory(final String description) {
            name = description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum MeasurementType {
        // indicators
        VALUE_HOR(false, INDICATOR, "hor. value"), VALUE_VER(true, INDICATOR, "ver. value"), DISTANCE_HOR(false,
                INDICATOR, "hor. distance"), DISTANCE_VER(true, INDICATOR, "ver. distance"),

        // vertical-type measurements
        MINIMUM(true, VERTICAL, "Minimum"), MAXIMUM(true, VERTICAL, "Maximum"), RANGE(true, VERTICAL, "Range"), MEAN(
                true, VERTICAL, "Mean"), RMS(true, VERTICAL, "R.M.S."), MEDIAN(true, VERTICAL, "Median"), INTEGRAL(true,
                        VERTICAL, "Integral"), TRANSMISSION_ABS(true, ACC,
                                "Abs. Transmission"), TRANSMISSION_REL(true, ACC, "Rel. Transmission"),

        // horizontal-type measurements
        EDGE_DETECT(true, HORIZONTAL, "Edge-Detect"), RISETIME(true, HORIZONTAL,
                "20%-80% Rise-/Fall-Time\n (simple)"), FWHM(true, HORIZONTAL, "FWHM"), FWHM_INTERPOLATED(true,
                        HORIZONTAL,
                        "FWHM (interp.)"), LOCATION_MAXIMUM(true, HORIZONTAL, "Loc. Maximum"), LOCATION_MAXIMUM_GAUSS(
                                true, HORIZONTAL, "Loc. Maximum\n(Gauss-interp.)"), DUTY_CYCLE(true, HORIZONTAL,
                                        "Duty Cycle\n(10% hysteresis)"), PERIOD(true, HORIZONTAL,
                                                "Period"), FREQUENCY(true, HORIZONTAL, "Frequency");

        private String name;
        private MeasurementCategory category;
        private boolean isVertical;

        MeasurementType(final boolean isVerticalMeasurement, final MeasurementCategory measurementCategory,
                final String description) {
            isVertical = isVerticalMeasurement;
            category = measurementCategory;
            name = description;
        }

        public MeasurementCategory getCategory() {
            return category;
        }

        public boolean isVerticalMeasurement() {
            return isVertical;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final MeasurementType measType;

    public SimpleMeasurements(final XYChart chart, final MeasurementType measType) {
        super(chart, AxisMode.X);
        this.measType = measType;
        title = measType + " [#" + (AbstractChartMeasurement.markerCount - 1) + ", #"
                + AbstractChartMeasurement.markerCount + "]";
        valueField.setMinRange(SimpleMeasurements.DEFAULT_MIN).setMaxRange(SimpleMeasurements.DEFAULT_MAX);

        final Axis axis = axisMode == X ? chart.getXAxis() : chart.getYAxis();
        if (!(axis instanceof Axis)) {
            SimpleMeasurements.LOGGER.warn("axis type " + axis.getClass().getSimpleName()
                    + "not compatible with indicator (needs to derivce from Axis)");
            return;
        }
        final Axis numAxis = axis;

        final double lower = numAxis.getLowerBound();
        final double upper = numAxis.getUpperBound();
        final double middle = 0.5 * Math.abs(upper - lower);
        final double min = Math.min(lower, upper);
        sliderIndicator1.setValue(min + 0.5 * middle);

        if (measType != VALUE_HOR && measType != VALUE_VER) {
            // need two indicators
            sliderIndicator2.setValue(min + 1.5 * middle);
            chart.getPlugins().add(sliderIndicator2);
            sliderIndicator2.setText("Marker#" + AbstractChartMeasurement.markerCount);
            AbstractChartMeasurement.markerCount++;
        } else {
            sliderIndicator2.setValue(Double.MAX_VALUE);
        }

        valueField.setMinRange(SimpleMeasurements.DEFAULT_MIN).setMaxRange(SimpleMeasurements.DEFAULT_MAX);
    }

    @Override
    public void initialize() {
        // super.initialize();

        final Node node = Borders.wrap(valueField).lineBorder().title(title).color(Color.BLACK).build().build();
        node.setMouseTransparent(true);
        displayPane.setCenter(node);

        sliderIndicator1.valueProperty().addListener((ch, oldValue, newValue) -> {
            if (oldValue != newValue) {
                invalidated(null);
            }
        });

        sliderIndicator2.valueProperty().addListener((ch, oldValue, newValue) -> {
            if (oldValue != newValue) {
                invalidated(null);
            }
        });
        chart.addListener(e -> invalidated(null));
        super.showConfigDialogue();
        invalidated(null);
    }

    @Override
    protected void removeAction() {
        super.removeAction();
        chart.getPlugins().remove(sliderIndicator2);
        chart.requestLayout();
    }

    @Override
    public void invalidated(final Observable observable) {
        if (sliderIndicator2 == null) {
            // not yet initialised
            return;
        }
        final DataSet selectedDataSet = getDataSet();
        final double newValueMarker1 = sliderIndicator1.getValue();
        final double newValueMarker2 = sliderIndicator2.getValue();

        final int index0 = selectedDataSet.getXIndex(newValueMarker1);
        final int index1 = selectedDataSet.getXIndex(newValueMarker2);
        final int indexMin = Math.min(index0, index1);
        final int indexMax = Math.max(index0, index1);

        double val;
        switch (measType) {
        // indicators
        case VALUE_HOR:
            val = SimpleDataSetEstimators.getValue(selectedDataSet, indexMin, true);
            break;
        case VALUE_VER:
            val = SimpleDataSetEstimators.getValue(selectedDataSet, indexMin, false);
            break;
        case DISTANCE_HOR:
            val = SimpleDataSetEstimators.getDistance(selectedDataSet, indexMin, indexMax, true);
            break;
        case DISTANCE_VER:
            val = SimpleDataSetEstimators.getDistance(selectedDataSet, indexMin, indexMax, false);
            break;
        // vertical measurements
        case MINIMUM:
            val = SimpleDataSetEstimators.getMinimum(selectedDataSet, indexMin, indexMax);
            break;
        case MAXIMUM:
            val = SimpleDataSetEstimators.getMaximum(selectedDataSet, indexMin, indexMax);
            break;
        case RANGE:
            val = SimpleDataSetEstimators.getRange(selectedDataSet, indexMin, indexMax);
            break;
        case MEAN:
            val = SimpleDataSetEstimators.getMean(selectedDataSet, indexMin, indexMax);
            break;
        case RMS:
            val = SimpleDataSetEstimators.getRms(selectedDataSet, indexMin, indexMax);
            break;
        case MEDIAN:
            val = SimpleDataSetEstimators.getMedian(selectedDataSet, indexMin, indexMax);
            break;
        case INTEGRAL:
            val = SimpleDataSetEstimators.getIntegral(selectedDataSet, index0, index1); // N.B.
                                                                                        // use
                                                                                        // of
                                                                                        // non-sanitised
                                                                                        // indices
                                                                                        // index[0,1]
            break;
        case TRANSMISSION_ABS:
            val = SimpleDataSetEstimators.getTransmission(selectedDataSet, index0, index1, true); // N.B.
                                                                                                  // use
                                                                                                  // of
                                                                                                  // non-sanitised
                                                                                                  // index[0,1]
            break;
        case TRANSMISSION_REL:
            val = SimpleDataSetEstimators.getTransmission(selectedDataSet, index0, index1, false); // N.B.
                                                                                                   // use
                                                                                                   // of
                                                                                                   // non-sanitised
                                                                                                   // index[0,1]
            break;

        // horizontal measurements
        case EDGE_DETECT:
            val = SimpleDataSetEstimators.getEdgeDetect(selectedDataSet, indexMin, indexMax);
            break;
        case RISETIME:
            val = SimpleDataSetEstimators.getSimpleRiseTime(selectedDataSet, indexMin, indexMax);
            break;
        case FWHM:
            val = SimpleDataSetEstimators.getFullWidthHalfMaximum(selectedDataSet, indexMin, indexMax, false);
            break;
        case FWHM_INTERPOLATED:
            val = SimpleDataSetEstimators.getFullWidthHalfMaximum(selectedDataSet, indexMin, indexMax, true);
            break;
        case LOCATION_MAXIMUM:
            val = selectedDataSet.getX(SimpleDataSetEstimators.getLocationMaximum(selectedDataSet, indexMin, indexMax));
            break;
        case LOCATION_MAXIMUM_GAUSS:
            val = SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(selectedDataSet, indexMin, indexMax);
            break;
        case DUTY_CYCLE:
            val = SimpleDataSetEstimators.getDutyCycle(selectedDataSet, indexMin, indexMax);
            break;
        case PERIOD:
            val = 1.0 / SimpleDataSetEstimators.getFrequencyEstimate(selectedDataSet, indexMin, indexMax);
            break;
        case FREQUENCY:
            val = SimpleDataSetEstimators.getFrequencyEstimate(selectedDataSet, indexMin, indexMax);
            break;

        default:
            val = Double.NaN;
        }

        final Axis axis = measType.isVerticalMeasurement() ? chart.getYAxis() : chart.getXAxis();
        axis.getLabel();
        // final String unit = axisLabel.substring(axisLabel.indexOf('[') + 1,
        // axisLabel.indexOf(']'));
        final String unit = axis.getUnit() == null ? "a.u." : axis.getUnit();
        valueField.setUnit(unit);

        if (measType == TRANSMISSION_ABS || measType == TRANSMISSION_REL) {
            valueField.setUnit("%");
        }

        if (measType == INTEGRAL) {
            final Axis altAxis = measType.isVerticalMeasurement() ? chart.getXAxis() : chart.getYAxis();
            final String altAxisLabel = altAxis.getLabel();
            final String unit2 = altAxisLabel.substring(altAxisLabel.indexOf('[') + 1, altAxisLabel.indexOf(']'));
            valueField.setUnit(unit + "*" + unit2);
        }

        // update label valueTextField
        String valueLabel;
        if (axis instanceof DefaultNumericAxis) {
            final AxisLabelFormatter axisFormatter = ((DefaultNumericAxis) axis).getAxisLabelFormatter();

            valueLabel = axisFormatter.toString(val);
        } else {
            if (Math.abs(Math.log10(Math.abs(val))) < ValueIndicator.SMALL_FORMAT_THRESHOLD) {
                formatter = formatterSmall;
            } else {
                formatter = formatterLarge;
            }
            valueLabel = formatter.format(val);
        }
        valueField.setValue(val, valueLabel);

    }

}
