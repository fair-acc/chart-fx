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
            try {
                dataset.lock();

                // update categories in case of category axes for the first
                // (index == '0') indexed data set
                if (index == 0) {
                    if (xyChart.getXAxis() instanceof CategoryAxis) {
                        final CategoryAxis axis = (CategoryAxis) xyChart.getXAxis();
                        axis.updateCategories(dataset);
                    }

                    if (xyChart.getYAxis() instanceof CategoryAxis) {
                        final CategoryAxis axis = (CategoryAxis) xyChart.getYAxis();
                        axis.updateCategories(dataset);
                    }
                }

                if (dataset.getDataCount() > 0) {
                    gc.setStroke(LineRenderer.COLORS[index++ % 4]);
                    int i = dataset.getXIndex(xmin);
                    if (i < 0) {
                        i = 0;
                    }
                    double x0 = xAxis.getDisplayPosition(dataset.getX(i));
                    double y0 = yAxis.getDisplayPosition(dataset.getY(i));
                    i++;
                    final int maxIndex = Math.min(dataset.getXIndex(xmax) + 1, dataset.getDataCount());
                    for (; i < maxIndex; i++) {
                        final double x1 = xAxis.getDisplayPosition(dataset.getX(i));
                        final double y1 = yAxis.getDisplayPosition(dataset.getY(i));
                        gc.strokeLine(x0, y0, x1, y1);
                        // System.out.println(x0+" "+y0+" -> "+x1+" "+y1);
                        x0 = x1;
                        y0 = y1;
                    }
                }
            } finally {
                dataset.unlock();
            }
        }
        ProcessingProfiler.getTimeDiff(start);
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // not implemented for this class
        return null;
    }
}
