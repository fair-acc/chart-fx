package io.fair_acc.chartfx.renderer.spi;

import java.security.InvalidParameterException;

import javafx.geometry.Orientation;
import javafx.scene.canvas.GraphicsContext;

import io.fair_acc.bench.DurationMeasure;
import io.fair_acc.bench.Measurable;
import io.fair_acc.bench.MeasurementRecorder;
import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.AssertUtils;

/**
 * Renderer that requires an X and a Y axis
 *
 * @author ennerf
 */
public abstract class AbstractRendererXY<R extends AbstractRendererXY<R>> extends AbstractRenderer<R> implements Measurable {
    public AbstractRendererXY() {
        chartProperty().addListener((obs, old, chart) -> requireChartXY(chart));
    }

    @Override
    public void setChart(Chart chart) {
        // throw early to provide a better stacktrace without lots of listeners
        super.setChart(requireChartXY(chart));
    }

    private XYChart requireChartXY(Chart chart) {
        if (chart == null || chart instanceof XYChart) {
            return (XYChart) chart;
        }
        throw new InvalidParameterException("must be derivative of XYChart for renderer - "
                                            + this.getClass().getSimpleName());
    }

    @Override
    public XYChart getChart() {
        return (XYChart) super.getChart();
    }

    @Override
    public void render() {
        // Nothing to do
        if (getDatasets().isEmpty()) {
            return;
        }

        benchDrawAll.start();
        updateCachedVariables();

        // N.B. importance of reverse order: start with last index, so that
        // most(-like) important DataSet is drawn on top of the others
        for (int i = getDatasetNodes().size() - 1; i >= 0; i--) {
            var dataSetNode = getDatasetNodes().get(i);
            if (dataSetNode.isVisible()) {
                benchDrawOne.start();
                render(getChart().getCanvas().getGraphicsContext2D(), dataSetNode.getDataSet(), dataSetNode);
                benchDrawOne.stop();
            }
        }

        benchDrawAll.stop();
    }

    protected abstract void render(GraphicsContext gc, DataSet dataSet, DataSetNode style);

    @Override
    public void updateAxes() {
        // Default to explicitly set axes
        xAxis = ensureAxisInChart(getFirstAxis(Orientation.HORIZONTAL));
        yAxis = ensureAxisInChart(getFirstAxis(Orientation.VERTICAL));

        // Get or create one in the chart if needed
        var chart = AssertUtils.notNull("chart", getChart());
        if (xAxis == null) {
            xAxis = chart.getXAxis();
        }
        if (yAxis == null) {
            yAxis = chart.getYAxis();
        }

        // For backwards compatibility: A CategoryAxis without explicitly set
        // categories copies the labels of the first dataset that is using it.
        if (xAxis instanceof CategoryAxis axis && !getDatasets().isEmpty()) {
            axis.updateCategories(getDatasets().get(0));
        }
        if (yAxis instanceof CategoryAxis axis && !getDatasets().isEmpty()) {
            axis.updateCategories(getDatasets().get(0));
        }
    }

    protected Axis ensureAxisInChart(Axis axis) {
        if (axis != null && !getChart().getAxes().contains(axis)) {
            getChart().getAxes().add(axis);
        }
        return axis;
    }

    @Override
    public void updateAxisRange(Axis axis, AxisRange range) {
        if (axis == xAxis) {
            updateAxisRange(range, DataSet.DIM_X);
        } else if (axis == yAxis) {
            updateAxisRange(range, DataSet.DIM_Y);
        }
    }

    protected void updateAxisRange(AxisRange range, int dim) {
        for (DataSetNode node : getDatasetNodes()) {
            if (node.isVisible()) {
                updateAxisRange(node.getDataSet(), range, dim);
            }
        }
    }

    protected void updateAxisRange(DataSet dataSet, AxisRange range, int dim) {
        if (dataSet.getDataCount() == 0) return;
        var dsRange = dataSet.getAxisDescription(dim);
        if (!dsRange.isDefined()) {
            dataSet.recomputeLimits(dim);
        }
        range.add(dsRange.getMin());
        range.add(dsRange.getMax());
    }

    protected void updateCachedVariables() {
        xMin = xAxis.getValueForDisplay(xAxis.isInvertedAxis() ? xAxis.getLength() : 0.0);
        xMax = xAxis.getValueForDisplay(xAxis.isInvertedAxis() ? 0.0 : xAxis.getLength());
    }

    protected double xMin, xMax;
    protected Axis xAxis;
    protected Axis yAxis;

    @Override
    public void setRecorder(MeasurementRecorder recorder) {
        benchDrawAll = recorder.newDuration("xy-drawAll");
        benchDrawOne = recorder.newTraceDuration("xy-drawOne");
    }

    private DurationMeasure benchDrawAll = DurationMeasure.DISABLED;
    private DurationMeasure benchDrawOne = DurationMeasure.DISABLED;
}
