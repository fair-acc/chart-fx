package io.fair_acc.chartfx.plugins.measurements;

import static io.fair_acc.chartfx.axes.AxisMode.X;
import static io.fair_acc.chartfx.axes.AxisMode.Y;
import static io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements.MeasurementCategory.ACC;
import static io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements.MeasurementCategory.HORIZONTAL;
import static io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements.MeasurementCategory.INDICATOR;
import static io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements.MeasurementCategory.VERTICAL;

import java.util.Optional;

import javafx.scene.control.ButtonType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisLabelFormatter;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.axes.spi.MetricPrefix;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.utils.DragResizerUtil;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.UpdateEvent;
import io.fair_acc.math.SimpleDataSetEstimators;

/**
 * Simple DataSet parameter measurements N.B. this contains only algorithms w/o
 * external library dependencies (ie. fitting routines, etc.)
 *
 * @author rstein
 */
public class SimpleMeasurements extends AbstractChartMeasurement {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMeasurements.class);
    private final MeasurementType measType;

    public SimpleMeasurements(final ParameterMeasurements plugin, final MeasurementType measType) {
        super(plugin, measType.toString(), measType.isVertical ? X : Y, measType.getRequiredSelectors(), 1);
        this.measType = measType;

        setTitle(measType.toString());
        getValueField().setMinRange(DEFAULT_MIN).setMaxRange(DEFAULT_MAX);

        // needs to be added here to be aesthetically the last fields in the GridPane
        addMinMaxRangeFields();
    }

    public MeasurementType getMeasType() {
        return measType;
    }

    @Override
    public void handle(final UpdateEvent event) {
        final DataSet ds = getDataSet();
        if (getValueIndicatorsUser().size() < measType.getRequiredSelectors() || ds == null) {
            // not yet initialised
            return;
        }

        final double newValueMarker1 = requiredNumberOfIndicators >= 1 && !getValueIndicatorsUser().isEmpty() ? getValueIndicatorsUser().get(0).getValue() : DEFAULT_MIN;
        final double newValueMarker2 = requiredNumberOfIndicators >= 2 && getValueIndicatorsUser().size() >= 2 ? getValueIndicatorsUser().get(1).getValue() : DEFAULT_MAX;

        ds.lock().readLockGuard(() -> {
            if (!ds.getAxisDescription(DataSet.DIM_X).isDefined()) {
                ds.recomputeLimits(DataSet.DIM_X);
            }
            final int index0 = ds.getIndex(DataSet.DIM_X, newValueMarker1);
            final int index1 = ds.getIndex(DataSet.DIM_X, newValueMarker2);
            final int indexMin = requiredNumberOfIndicators == 1 ? index0 : Math.min(index0, index1);
            final int indexMax = Math.max(index0, index1);
            final Chart chart = getMeasurementPlugin().getChart();
            Axis axis = getFirstAxisForDataSet(chart, ds, !measType.isVerticalMeasurement());

            double val = Double.NaN;
            switch (measType) {
            // simple marker w/o computations
            case MARKER_HOR:
                val = newValueMarker1;
                axis = getFirstAxisForDataSet(chart, ds, true);
                break;
            case MARKER_DISTANCE_HOR:
                val = newValueMarker2 - newValueMarker1;
                axis = getFirstAxisForDataSet(chart, ds, true);
                break;
            case MARKER_VER:
                val = newValueMarker1;
                axis = getFirstAxisForDataSet(chart, ds, false);
                break;
            case MARKER_DISTANCE_VER:
                val = newValueMarker2 - newValueMarker1;
                axis = getFirstAxisForDataSet(chart, ds, false);
                break;
            // indicators
            case VALUE_HOR:
                val = SimpleDataSetEstimators.getZeroCrossing(ds, newValueMarker1);
                break;
            case VALUE_VER:
                val = ds.get(DataSet.DIM_Y, indexMin);
                break;
            case DISTANCE_HOR:
                val = SimpleDataSetEstimators.getZeroCrossing(ds, newValueMarker2) - SimpleDataSetEstimators.getZeroCrossing(ds, newValueMarker1);
                break;
            case DISTANCE_VER:
                val = SimpleDataSetEstimators.getDistance(ds, indexMin, indexMax, false);
                break;
            // vertical measurements
            case MINIMUM:
                val = SimpleDataSetEstimators.getMinimum(ds, indexMin, indexMax);
                break;
            case MAXIMUM:
                val = SimpleDataSetEstimators.getMaximum(ds, indexMin, indexMax);
                break;
            case RANGE:
                val = SimpleDataSetEstimators.getRange(ds, indexMin, indexMax);
                break;
            case MEAN:
                val = SimpleDataSetEstimators.getMean(ds, indexMin, indexMax);
                break;
            case RMS:
                val = SimpleDataSetEstimators.getRms(ds, indexMin, indexMax);
                break;
            case MEDIAN:
                val = SimpleDataSetEstimators.getMedian(ds, indexMin, indexMax);
                break;
            case INTEGRAL:
                // N.B. use of non-sanitised indices index[0,1]
                val = SimpleDataSetEstimators.getIntegral(ds, index0, index1);
                break;
            case INTEGRAL_FULL:
                val = SimpleDataSetEstimators.getIntegral(ds, 0, ds.getDataCount());
                break;
            case TRANSMISSION_ABS:
                // N.B. use of non-sanitised indices index[0,1]
                val = SimpleDataSetEstimators.getTransmission(ds, index0, index1, true);
                break;
            case TRANSMISSION_REL:
                // N.B. use of non-sanitised indices index[0,1]
                val = SimpleDataSetEstimators.getTransmission(ds, index0, index1, false);
                break;

            // horizontal measurements
            case EDGE_DETECT:
                val = SimpleDataSetEstimators.getEdgeDetect(ds, indexMin, indexMax);
                break;
            case RISETIME_10_90:
                val = SimpleDataSetEstimators.getSimpleRiseTime1090(ds, indexMin, indexMax);
                break;
            case RISETIME_20_80:
                val = SimpleDataSetEstimators.getSimpleRiseTime2080(ds, indexMin, indexMax);
                break;
            case FWHM:
                val = SimpleDataSetEstimators.getFullWidthHalfMaximum(ds, indexMin, indexMax, false);
                break;
            case FWHM_INTERPOLATED:
                val = SimpleDataSetEstimators.getFullWidthHalfMaximum(ds, indexMin, indexMax, true);
                break;
            case LOCATION_MAXIMUM:
                val = ds.get(DataSet.DIM_X, SimpleDataSetEstimators.getLocationMaximum(ds, indexMin, indexMax));
                break;
            case LOCATION_MAXIMUM_GAUSS:
                val = SimpleDataSetEstimators.getLocationMaximumGaussInterpolated(ds, indexMin, indexMax);
                break;
            case DUTY_CYCLE:
                val = SimpleDataSetEstimators.getDutyCycle(ds, indexMin, indexMax);
                break;
            case PERIOD:
                val = 1.0 / SimpleDataSetEstimators.getFrequencyEstimate(ds, indexMin, indexMax);
                break;
            case FREQUENCY:
                val = SimpleDataSetEstimators.getFrequencyEstimate(ds, indexMin, indexMax);
                break;
            default:
                break;
            }

            final String axisUnit = axis.getUnit();
            final String unit = axisUnit == null ? "a.u." : axisUnit;

            // update label valueTextField
            String valueLabel;
            if (axis instanceof DefaultNumericAxis && axisUnit != null) {
                final double unitScale = axis.getUnitScaling();
                final String axisPrefix = MetricPrefix.getShortPrefix(unitScale);
                // convert value according to scale factor
                final double scaledValue = val / unitScale;

                FXUtils.runFX(() -> getValueField().setUnit(axisPrefix + unit));
                final AxisLabelFormatter axisFormatter = ((DefaultNumericAxis) axis).getAxisLabelFormatter();
                valueLabel = axisFormatter.toString(scaledValue);
            } else {
                if (Math.abs(Math.log10(Math.abs(val))) < SMALL_FORMAT_THRESHOLD) {
                    valueLabel = formatterSmall.format(val);
                } else {
                    valueLabel = formatterLarge.format(val);
                }

                FXUtils.runFX(() -> getValueField().setUnit(unit));
            }

            final double tempVal = val;
            FXUtils.runFX(() -> getValueField().setValue(tempVal, valueLabel));

            switch (measType) {
            case TRANSMISSION_ABS:
            case TRANSMISSION_REL:
                FXUtils.runFX(() -> getValueField().setUnit("%"));
                break;
            case INTEGRAL:
            default:
                break;
            }
        });

        if (event != null) {
            // republish updateEvent
            invokeListener(event);
        }
    }

    @Override
    public void initialize() {
        getDataViewWindow().setContent(getValueField());
        DragResizerUtil.makeResizable(getValueField());

        Optional<ButtonType> result = super.showConfigDialogue();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.atTrace().addArgument(result).log("config dialogue finished with result {}");
            LOGGER.atTrace().addArgument(getValueIndicators()).log("detected getValueIndicators() = {}");
            LOGGER.atTrace().addArgument(getValueIndicatorsUser()).log("detected getValueIndicatorsUser() = {}");
        }

        // initial update
        handle(null);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.atTrace().log("initialised and called initial handle(null)");
        }
    }

    @Override
    protected void removeAction() {
        super.removeAction();
        getMeasurementPlugin().getChart().requestLayout();
    }

    public enum MeasurementCategory {
        INDICATOR("Indicators"),
        VERTICAL("Vertical Measurements"),
        HORIZONTAL("Horizontal Measurements"),
        ACC("Accelerator Misc.");

        private final String name;

        MeasurementCategory(final String description) {
            name = description;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum MeasurementType {
        // indicators
        // simple non-computing marker
        MARKER_HOR(true, INDICATOR, "Marker X", 1),
        MARKER_DISTANCE_HOR(true, INDICATOR, "Marker ∆X"),
        MARKER_VER(false, INDICATOR, "Marker Y", 1),
        MARKER_DISTANCE_VER(false, INDICATOR, "Marker ∆Y"),

        /** 
         * horizontal value at indicator 
         */
        VALUE_HOR(false, INDICATOR, "hor. value", 1),
        DISTANCE_HOR(false, INDICATOR, "hor. distance"),
        /** 
         * vertical value at indicator 
         */
        VALUE_VER(true, INDICATOR, "ver. value", 1),
        DISTANCE_VER(true, INDICATOR, "ver. distance"),

        // vertical-type measurements
        MINIMUM(true, VERTICAL, "Minimum"),
        MAXIMUM(true, VERTICAL, "Maximum"),
        RANGE(true, VERTICAL, "Range"),
        MEAN(true, VERTICAL, "Mean"),
        RMS(true, VERTICAL, "R.M.S."),
        MEDIAN(true, VERTICAL, "Median"),
        INTEGRAL(true, VERTICAL, "Integral"),
        INTEGRAL_FULL(true, VERTICAL, "Integral - full range", 0),
        TRANSMISSION_ABS(true, ACC, "Abs. Transmission"),
        TRANSMISSION_REL(true, ACC, "Rel. Transmission"),

        // horizontal-type measurements
        EDGE_DETECT(true, HORIZONTAL, "Edge-Detect"),
        RISETIME_10_90(true, HORIZONTAL, "10%-90% Rise-/Fall-Time\n (simple)"),
        RISETIME_20_80(true, HORIZONTAL, "20%-80% Rise-/Fall-Time\n (simple)"),
        FWHM(true, HORIZONTAL, "FWHM"),
        FWHM_INTERPOLATED(true, HORIZONTAL, "FWHM (interp.)"),
        LOCATION_MAXIMUM(true, HORIZONTAL, "Loc. Maximum"),
        LOCATION_MAXIMUM_GAUSS(true, HORIZONTAL, "Loc. Maximum\n(Gauss-interp.)"),
        DUTY_CYCLE(true, HORIZONTAL, "Duty Cycle\n(10% hysteresis)"),
        PERIOD(true, HORIZONTAL, "Period"),
        FREQUENCY(true, HORIZONTAL, "Frequency");

        private final String name;
        private final MeasurementCategory category;
        private final boolean isVertical;
        private final int requiredSelectors;
        private final int requiredDataSets;

        MeasurementType(final boolean isVerticalMeasurement, final MeasurementCategory measurementCategory, final String description) {
            this(isVerticalMeasurement, measurementCategory, description, 2);
        }

        MeasurementType(final boolean isVerticalMeasurement, final MeasurementCategory measurementCategory, final String description, final int requiredSelectors) {
            isVertical = isVerticalMeasurement;
            category = measurementCategory;
            name = description;
            this.requiredSelectors = requiredSelectors;
            this.requiredDataSets = 1;
        }

        public MeasurementCategory getCategory() {
            return category;
        }

        public boolean isVerticalMeasurement() {
            return isVertical;
        }

        public String getName() {
            return name;
        }

        public int getRequiredDataSets() {
            return requiredDataSets;
        }

        public int getRequiredSelectors() {
            return requiredSelectors;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
