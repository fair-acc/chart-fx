package de.gsi.chart.plugins.measurements;

import static de.gsi.chart.axes.AxisMode.X;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.controlsfx.tools.Borders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.AbstractSingleValueIndicator;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.UpdateEvent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * @author rstein
 */
public class ValueIndicator extends AbstractChartMeasurement {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValueIndicator.class);
    protected static final int SMALL_FORMAT_THRESHOLD = 3;
    private static final String FORMAT_SMALL_SCALE = "0.###";
    private static final String FORMAT_LARGE_SCALE = "0.##E0";
    public static final int DEFAULT_SMALL_AXIS = 6; // [orders of magnitude],
                                                    // e.g. '4' <-> [1,10000]
    protected final DecimalFormat formatterSmall = new DecimalFormat(ValueIndicator.FORMAT_SMALL_SCALE);
    protected final DecimalFormat formatterLarge = new DecimalFormat(ValueIndicator.FORMAT_LARGE_SCALE);

    protected AbstractSingleValueIndicator sliderIndicator1;
    protected AbstractSingleValueIndicator sliderIndicator2;
    protected AxisMode axisMode;
    protected NumberFormat formatter;

    public ValueIndicator(final XYChart chart, final AxisMode axisMode) {
        super(chart);
        this.axisMode = axisMode;

        final Axis axis = axisMode == X ? chart.getXAxis() : chart.getYAxis();
        if (!(axis instanceof Axis)) {
            ValueIndicator.LOGGER.warn("axis type " + axis.getClass().getSimpleName()
                    + "not compatible with indicator (needs to derivce from Axis)");
            return;
        }
        final Axis numAxis = axis;
        formatter = formatterSmall;

        getDialogContentBox().getChildren().addAll(
                new HBox(new Label("Min. Range: "), valueField.getMinRangeTextField()),
                new HBox(new Label("Max. Range: "), valueField.getMaxRangeTextField()));

        final double lower = numAxis.getLowerBound();
        final double upper = numAxis.getUpperBound();
        final double middle = 0.5 * Math.abs(upper - lower) + Math.min(lower, upper);
        System.err.println(
                "axis title = " + numAxis.getLabel() + " middle = " + middle + " min = " + numAxis.getUpperBound());
        if (axisMode == X) {
            sliderIndicator1 = new XValueIndicator(numAxis, middle);
            sliderIndicator2 = new XValueIndicator(numAxis, middle);
        } else {
            sliderIndicator1 = new YValueIndicator(numAxis, middle);
            sliderIndicator2 = new YValueIndicator(numAxis, middle);
        }

        sliderIndicator1.setText("Marker#" + AbstractChartMeasurement.markerCount);
        chart.getPlugins().add(sliderIndicator1);
        AbstractChartMeasurement.markerCount++;

        title = "Value@Marker#" + (AbstractChartMeasurement.markerCount - 1);
        chart.requestLayout();
    }

    @Override
    public void initialize() {
        final Node node = Borders.wrap(valueField).lineBorder().title(title).color(Color.BLACK).build().build();
        node.setMouseTransparent(true);
        displayPane.getChildren().add(node);

        sliderIndicator1.valueProperty().addListener((ch, oldValue, newValue) -> {
            if (oldValue != newValue) {
            	if (Platform.isFxApplicationThread()) {
            		handle(null);
            	} else {
            		Platform.runLater(() -> handle(null));
            	}
            }
        });
        // chartPane.addListener(e -> invalidated(null));
        super.showConfigDialogue();
        if (Platform.isFxApplicationThread()) {
    		handle(null);
    	} else {
    		Platform.runLater(() -> handle(null));
    	}
        chart.requestLayout();
    }

    @Override
    protected void nominalAction() {
        super.nominalAction();
    }

    @Override
    protected void defaultAction() {
        super.defaultAction();
        valueField.resetRanges();
    }

    @Override
    protected void removeAction() {
        super.removeAction();
        chart.getPlugins().remove(sliderIndicator1);
        chart.requestLayout();
    }

    @Override
    public void handle(final UpdateEvent observable) {
    	if (Platform.isFxApplicationThread()) {
    		Platform.runLater(() -> handle(observable));
    	    return;
    	}
        double val;
        final DataSet selectedDataSet = getDataSet();
        final Axis axis;
        final double newValue = sliderIndicator1.getValue();
        if (axisMode == X) {
            final int index = selectedDataSet.getXIndex(newValue);
            val = selectedDataSet.getY(index);
            axis = chart.getYAxis();
        } else {
            final int index = selectedDataSet.getYIndex(newValue);
            val = selectedDataSet.getX(index);
            axis = chart.getXAxis();
        }

        // update label unitTextField
        valueField.setUnit(axis.getUnit() == null ? "" : axis.getUnit());

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
