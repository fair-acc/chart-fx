package de.gsi.chart.plugins.measurements;

import static de.gsi.chart.axes.AxisMode.X;

import java.text.NumberFormat;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.AbstractSingleValueIndicator;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.chart.utils.DragResizerUtil;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.EventRateLimiter;
import de.gsi.dataset.event.UpdateEvent;

/**
 * @author rstein
 */
public class ValueIndicator extends AbstractChartMeasurement {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValueIndicator.class);
    protected AbstractSingleValueIndicator sliderIndicator1;
    protected AbstractSingleValueIndicator sliderIndicator2;
    protected NumberFormat formatter;

    public ValueIndicator(final ParameterMeasurements plugin, final AxisMode axisMode) {
        super(plugin, "Marker", axisMode);
        if (plugin.getChart() == null) {
            throw new IllegalArgumentException("chart reference must not be null");
        }

        //TODO: add via chart change Listener
        final Axis axis = plugin.getChart().getFirstAxis(axisMode == X ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        formatter = formatterSmall;

        getDialogContentBox().getChildren().addAll(
                new HBox(new Label("Min. Range: "), getValueField().getMinRangeTextField()),
                new HBox(new Label("Max. Range: "), getValueField().getMaxRangeTextField()));

        final double lower = axis.getMin();
        final double upper = axis.getMax();
        final double middle = 0.5 * Math.abs(upper - lower) + Math.min(lower, upper);

        if (axisMode == X) {
            sliderIndicator1 = new XValueIndicator(axis, middle);
            sliderIndicator2 = new XValueIndicator(axis, middle);
        } else {
            sliderIndicator1 = new YValueIndicator(axis, middle);
            sliderIndicator2 = new YValueIndicator(axis, middle);
        }

        sliderIndicator1.setText("Marker#" + AbstractChartMeasurement.markerCount);
        plugin.getChart().getPlugins().add(sliderIndicator1);
        AbstractChartMeasurement.markerCount++;

        setTitle("Value@Marker#" + (AbstractChartMeasurement.markerCount - 1));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log(ValueIndicator.class.getSimpleName() + " - initialised");
        }
    }

    @Override
    public void handle(final UpdateEvent event) {
        if (!Platform.isFxApplicationThread()) {
            // not running from FX application thread restart via runLater...
            FXUtils.runFX(() -> handle(event));
            return;
        }
        final DataSet selectedDataSet = getDataSet();

        final double newValue = sliderIndicator1.getValue();
        final int index = selectedDataSet.getIndex(axisMode == X ? DataSet.DIM_X : DataSet.DIM_Y, newValue);
        final double val = selectedDataSet.get(axisMode == X ? DataSet.DIM_Y : DataSet.DIM_X, index);
        final Axis axis = getMeasurementPlugin().getChart().getFirstAxis(axisMode == X ? Orientation.VERTICAL : Orientation.HORIZONTAL);

        // update label unitTextField
        getValueField().setUnit(axis.getUnit() == null ? "" : axis.getUnit());

        // update label valueTextField
        String valueLabel;
        if (axis instanceof DefaultNumericAxis) {
            final AxisLabelFormatter axisFormatter = ((DefaultNumericAxis) axis).getAxisLabelFormatter();

            valueLabel = axisFormatter.toString(val);
        } else {
            if (Math.abs(Math.log10(Math.abs(val))) < SMALL_FORMAT_THRESHOLD) {
                formatter = formatterSmall;
            } else {
                formatter = formatterLarge;
            }
            valueLabel = formatter.format(val);
        }

        getValueField().setValue(val, valueLabel);

        if (event != null) {
            // republish updateEvent
            invokeListener(event);
        }
    }

    @Override
    public void initialize() {
        getDataViewWindow().setContent(getValueField());
        DragResizerUtil.makeResizable(getValueField());

        sliderIndicator1.addListener(new EventRateLimiter(this::handle, DEFAULT_UPDATE_RATE_LIMIT));

        super.showConfigDialogue();
        FXUtils.runFX(() -> handle(null));
    }

    @Override
    protected void removeAction() {
        super.removeAction();
        getMeasurementPlugin().getChart().getPlugins().remove(sliderIndicator1);
        getMeasurementPlugin().getChart().requestLayout();
    }
}
