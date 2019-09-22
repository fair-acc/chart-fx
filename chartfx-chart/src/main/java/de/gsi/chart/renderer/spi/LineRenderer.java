package de.gsi.chart.renderer.spi;

import java.security.InvalidParameterException;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.renderer.Renderer;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @author braeun
 */
@Deprecated
public class LineRenderer extends AbstractDataSetManagement<LineRenderer> implements Renderer {

    static private final Color[] COLORS = { Color.BLACK, Color.BLUE, Color.GREEN, Color.RED };

    /**
     * @return the instance of this LineRenderer.
     */
    @Override
    protected LineRenderer getThis() {
        return this;
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

        final long start = ProcessingProfiler.getTimeStamp();
        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();

        final double xAxisWidth = xAxis.getWidth();
        final double xmin = xAxis.getValueForDisplay(0);
        final double xmax = xAxis.getValueForDisplay(xAxisWidth);
        int index = 0;
        for (final DataSet dataset : datasets) {
            final int lindex = index;
            index = dataset.lock().readLockGuard(() -> {
                // update categories in case of category axes for the first
                // (index == '0') indexed data set
                if (lindex == 0) {
                    if (xyChart.getXAxis() instanceof CategoryAxis) {
                        final CategoryAxis axis = (CategoryAxis) xyChart.getXAxis();
                        axis.updateCategories(dataset);
                    }

                    if (xyChart.getYAxis() instanceof CategoryAxis) {
                        final CategoryAxis axis = (CategoryAxis) xyChart.getYAxis();
                        axis.updateCategories(dataset);
                    }
                }

                if (dataset.getDataCount(DataSet.DIM_X) > 0) {
                    gc.setStroke(LineRenderer.COLORS[(lindex + 1) % 4]);
                    int i = dataset.getIndex(DataSet.DIM_X, xmin);
                    if (i < 0) {
                        i = 0;
                    }
                    double x0 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));
                    double y0 = yAxis.getDisplayPosition(dataset.get(DataSet.DIM_Y, i));
                    i++;
                    final int maxIndex = Math.min(dataset.getIndex(DataSet.DIM_X, xmax) + 1, dataset.getDataCount(DataSet.DIM_X));
                    for (; i < maxIndex; i++) {
                        final double x1 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));
                        final double y1 = yAxis.getDisplayPosition(dataset.get(DataSet.DIM_Y, i));
                        gc.strokeLine(x0, y0, x1, y1);
                        x0 = x1;
                        y0 = y1;
                    }
                }
                return lindex + 1;
            });
        }
        ProcessingProfiler.getTimeDiff(start);
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // not implemented for this class
        return null;
    }
}
