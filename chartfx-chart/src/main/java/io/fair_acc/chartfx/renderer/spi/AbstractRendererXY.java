package io.fair_acc.chartfx.renderer.spi;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.utils.ProcessingProfiler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.canvas.GraphicsContext;

import java.security.InvalidParameterException;

/**
 * Renderer that requires an X and a Y axis
 *
 * @author ennerf
 */
public abstract class AbstractRendererXY<R extends AbstractRendererXY<R>> extends AbstractRenderer<R> {

    public AbstractRendererXY() {
        chartProperty().addListener((observable, oldValue, chart) -> {
            if (chart != null && !(chart instanceof XYChart)) {
                throw new InvalidParameterException("must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
            }
        });
    }

    @Override
    public XYChart getChart() {
        return (XYChart) super.getChart();
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int unusedOffset) {
        // Nothing to do
        if (getDatasets().isEmpty()) {
            return;
        }

        final long start = ProcessingProfiler.getTimeStamp();

        updateCachedVariables();

        // N.B. importance of reverse order: start with last index, so that
        // most(-like) important DataSet is drawn on top of the others
        for (int i = getDatasetNodes().size() - 1; i >= 0; i--) {
            var dataSetNode = getDatasetNodes().get(i);
            if (dataSetNode.isVisible()) {
                render(getChart().getCanvas().getGraphicsContext2D(), dataSetNode.getDataSet(), dataSetNode);
            }
        }

        ProcessingProfiler.getTimeDiff(start, "render");

    }

    protected abstract void render(GraphicsContext gc, DataSet dataSet, DataSetNode style);

    @Override
    public void updateAxes() {
        // Default to explicitly set axes
        xAxis = getFirstAxis(Orientation.HORIZONTAL);
        yAxis = getFirstAxis(Orientation.VERTICAL);

        // Get or create one in the chart if needed
        var chart = AssertUtils.notNull("chart", getChart());
        if (xAxis == null) {
            xAxis = chart.getFirstAxis(Orientation.HORIZONTAL);
        }
        if (yAxis == null) {
            yAxis = chart.getFirstAxis(Orientation.VERTICAL);
        }
    }

    protected void updateCachedVariables() {
        xMin = xAxis.getValueForDisplay(xAxis.isInvertedAxis() ? xAxis.getLength() : 0.0);
        xMax = xAxis.getValueForDisplay(xAxis.isInvertedAxis() ? 0.0 : xAxis.getLength());
    }

    protected double xMin, xMax;
    protected Axis xAxis;
    protected Axis yAxis;

}
