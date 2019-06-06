/*****************************************************************************
 * * Chart Common - simple reducing line renderer * * modified: 2019-02-01 Harald Braeuning * *
 ****************************************************************************/

package de.gsi.chart.renderer.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.utils.DefaultRenderColorScheme;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/**
 * Simple, uncomplicated reducing line renderer
 * 
 * @author braeun
 */
public class ReducingLineRenderer extends AbstractDataSetManagement<ReducingLineRenderer> implements Renderer {

    private int maxPoints;

    //    static private final Color[] COLORS = { Color.BLACK, Color.BLUE, Color.GREEN, Color.RED };

    public ReducingLineRenderer() {
        maxPoints = 300;
    }

    public ReducingLineRenderer(final int maxPoints) {
        this.maxPoints = maxPoints;
    }

    /**
     * @return the instance of this ReducingLineRenderer.
     */
    @Override
    protected ReducingLineRenderer getThis() {
        return this;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(final int maxPoints) {
        this.maxPoints = maxPoints;
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        localDataSetList.addAll(super.getDatasets());

        final long start = ProcessingProfiler.getTimeStamp();
        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();

        final double xAxisWidth = xAxis.getWidth();
        final double xmin = xAxis.getValueForDisplay(0);
        final double xmax = xAxis.getValueForDisplay(xAxisWidth);
        int index = 0;
        for (final DataSet dataset : localDataSetList) {
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

                gc.save();
                DefaultRenderColorScheme.setLineScheme(gc, dataset.getStyle(), index);
                DefaultRenderColorScheme.setGraphicsContextAttributes(gc, dataset.getStyle());
                if (dataset.getDataCount() > 0) {
                    final int n = dataset.getDataCount(xmin, xmax);
                    final int d = n / maxPoints;
                    if (d <= 1) {
                        int i = dataset.getXIndex(xmin);
                        if (i < 0) {
                            i = 0;
                        }
                        double x0 = xAxis.getDisplayPosition(dataset.getX(i));
                        double y0 = yAxis.getDisplayPosition(dataset.getY(i));
                        i++;
                        for (; i < Math.min(dataset.getXIndex(xmax) + 1, dataset.getDataCount()); i++) {
                            final double x1 = xAxis.getDisplayPosition(dataset.getX(i));
                            final double y1 = yAxis.getDisplayPosition(dataset.getY(i));
                            gc.strokeLine(x0, y0, x1, y1);
                            x0 = x1;
                            y0 = y1;
                        }
                    } else {
                        int i = dataset.getXIndex(xmin);
                        if (i < 0) {
                            i = 0;
                        }
                        double x0 = xAxis.getDisplayPosition(dataset.getX(i));
                        double y0 = yAxis.getDisplayPosition(dataset.getY(i));
                        i++;
                        double x1 = xAxis.getDisplayPosition(dataset.getX(i));
                        double y1 = yAxis.getDisplayPosition(dataset.getY(i));
                        double delta = Math.abs(y1 - y0);
                        i++;
                        int j = d - 2;
                        for (; i < Math.min(dataset.getXIndex(xmax) + 1, dataset.getDataCount()); i++) {
                            if (j > 0) {
                                final double x2 = xAxis.getDisplayPosition(dataset.getX(i));
                                final double y2 = yAxis.getDisplayPosition(dataset.getY(i));
                                if (Math.abs(y2 - y0) > delta) {
                                    x1 = x2;
                                    y1 = y2;
                                    delta = Math.abs(y2 - y0);
                                }
                                j--;
                            } else {
                                gc.strokeLine(x0, y0, x1, y1);
                                x0 = x1;
                                y0 = y1;
                                x1 = xAxis.getDisplayPosition(dataset.getX(i));
                                y1 = yAxis.getDisplayPosition(dataset.getY(i));
                                delta = Math.abs(y1 - y0);
                                j = d - 1;
                            }
                        }
                    }
                }
                gc.restore();
            } finally {
                dataset.unlock();
            }
            index++;
        }
        ProcessingProfiler.getTimeDiff(start);
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // not implemented for this class
        return null;
    }
}
