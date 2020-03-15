package de.gsi.chart.plugins.measurements;

import static de.gsi.chart.axes.AxisMode.X;
import static de.gsi.chart.axes.AxisMode.Y;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.ACC;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.HORIZONTAL;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.INDICATOR;
import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.VERTICAL;

import java.util.Optional;

import javafx.geometry.Orientation;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.MetricPrefix;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.utils.DragResizerUtil;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.math.SimpleDataSetEstimators;

/**
 * Simple DataSet parameter measurements N.B. this contains only algorithms w/o
 * external library dependencies (ie. fitting routines, etc.)
 *
 * @author rstein
 */
public class SimpleMeasurements extends AbstractChartMeasurement {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMeasurements.class);
    private static final double DEFAULT_MIN = Double.NEGATIVE_INFINITY;
    private static final double DEFAULT_MAX = Double.POSITIVE_INFINITY;
    private final MeasurementType measType;

    public SimpleMeasurements(final ParameterMeasurements plugin, final MeasurementType measType) {
        super(plugin, measType.toString(), measType.isVertical ? X : Y);
        this.measType = measType;

        setTitle(measType.toString());
        getValueField().setMinRange(SimpleMeasurements.DEFAULT_MIN).setMaxRange(SimpleMeasurements.DEFAULT_MAX);

        final Label minValueLabel = new Label(" " + getValueField().getMinRange());
        getValueField().minRangeProperty().addListener((ch, o, n) -> minValueLabel.setText(" " + n.toString()));
        final Label maxValueLabel = new Label(" " + getValueField().getMaxRange());
        getValueField().maxRangeProperty().addListener((ch, o, n) -> maxValueLabel.setText(" " + n.toString()));
        getDialogContentBox().getChildren().addAll(new HBox(new Label("Min. Range: "), getValueField().getMinRangeTextField(), minValueLabel),
                new HBox(new Label("Max. Range: "), getValueField().getMaxRangeTextField(), maxValueLabel));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(SimpleMeasurements.class.getSimpleName()).addArgument(measType.getName()).log("{} type: '{}'- initialised");
        }
    }

    public MeasurementType getMeasType() {
        return measType;
    }

    @Override
    public void handle(final UpdateEvent event) {
        if (getValueIndicators().isEmpty() || getValueIndicatorsUser().size() < 2) {
            // not yet initialised
            return;
        }

        final DataSet selectedDataSet = getDataSet();
        final double newValueMarker1 = getValueIndicatorsUser().get(0).getValue();
        final double newValueMarker2 = getValueIndicatorsUser().get(1).getValue();

        final int index0 = selectedDataSet.getIndex(DataSet.DIM_X, newValueMarker1);
        final int index1 = selectedDataSet.getIndex(DataSet.DIM_X, newValueMarker2);
        final int indexMin = Math.min(index0, index1);
        final int indexMax = Math.max(index0, index1);

        DataSet ds = selectedDataSet;

        double val = Double.NaN;
        switch (measType) {
        // indicators
        case VALUE_HOR:
            val = selectedDataSet.get(DataSet.DIM_X, indexMin);
            break;
        case VALUE_VER:
            val = selectedDataSet.get(DataSet.DIM_Y, indexMin);
            break;
        case DISTANCE_HOR:
            val = SimpleDataSetEstimators.getDistance(ds, indexMin, indexMax, true);
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
            val = selectedDataSet.get(DataSet.DIM_X, SimpleDataSetEstimators.getLocationMaximum(ds, indexMin, indexMax));
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

        final Chart chart = getMeasurementPlugin().getChart();
        final Axis axis = chart.getFirstAxis(measType.isVerticalMeasurement() ? Orientation.VERTICAL : Orientation.HORIZONTAL);

        final String axisUnit = axis.getUnit();
        final String unit = axisUnit == null ? "a.u." : axis.getUnit();

        // update label valueTextField
        String valueLabel;
        if (axis instanceof DefaultNumericAxis && axisUnit != null) {
            final double unitScale = ((DefaultNumericAxis) axis).getUnitScaling();
            final String axisPrefix = MetricPrefix.getShortPrefix(unitScale);
            // convert value according to scale factor
            final double scaledValue = val / unitScale;

            getValueField().setUnit(new StringBuilder().append(axisPrefix).append(unit).toString());
            final AxisLabelFormatter axisFormatter = ((DefaultNumericAxis) axis).getAxisLabelFormatter();
            valueLabel = axisFormatter.toString(scaledValue);
        } else {
            if (Math.abs(Math.log10(Math.abs(val))) < SMALL_FORMAT_THRESHOLD) {
                valueLabel = formatterSmall.format(val);
            } else {
                valueLabel = formatterLarge.format(val);
            }

            getValueField().setUnit(unit);
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(result).log("config dialogue finished with result {}");
            LOGGER.atDebug().addArgument(getValueIndicators()).log("detected getValueIndicators() = {}");
            LOGGER.atDebug().addArgument(getValueIndicatorsUser()).log("detected getValueIndicatorsUser() = {}");
        }

        // initial update
        handle(null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("initialised and called initial handle(null)");
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

        private String name;

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
        VALUE_HOR(false, INDICATOR, "hor. value"),
        VALUE_VER(true, INDICATOR, "ver. value"),
        DISTANCE_HOR(false, INDICATOR, "hor. distance"),
        DISTANCE_VER(true, INDICATOR, "ver. distance"),

        // vertical-type measurements
        MINIMUM(true, VERTICAL, "Minimum"),
        MAXIMUM(true, VERTICAL, "Maximum"),
        RANGE(true, VERTICAL, "Range"),
        MEAN(true, VERTICAL, "Mean"),
        RMS(true, VERTICAL, "R.M.S."),
        MEDIAN(true, VERTICAL, "Median"),
        INTEGRAL(true, VERTICAL, "Integral"),
        TRANSMISSION_ABS(true, ACC, "Abs. Transmission"),
        TRANSMISSION_REL(true, ACC, "Rel. Transmission"),

        // horizontal-type measurements
        EDGE_DETECT(false, HORIZONTAL, "Edge-Detect"),
        RISETIME_10_90(false, HORIZONTAL, "10%-90% Rise-/Fall-Time\n (simple)"),
        RISETIME_20_80(false, HORIZONTAL, "20%-80% Rise-/Fall-Time\n (simple)"),
        FWHM(false, HORIZONTAL, "FWHM"),
        FWHM_INTERPOLATED(false, HORIZONTAL, "FWHM (interp.)"),
        LOCATION_MAXIMUM(false, HORIZONTAL, "Loc. Maximum"),
        LOCATION_MAXIMUM_GAUSS(false, HORIZONTAL, "Loc. Maximum\n(Gauss-interp.)"),
        DUTY_CYCLE(false, HORIZONTAL, "Duty Cycle\n(10% hysteresis)"),
        PERIOD(true, HORIZONTAL, "Period"),
        FREQUENCY(false, HORIZONTAL, "Frequency");

        private String name;
        private MeasurementCategory category;
        private boolean isVertical;

        MeasurementType(final boolean isVerticalMeasurement, final MeasurementCategory measurementCategory, final String description) {
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

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
