/*****************************************************************************
 * * Chart Common - simple reducing line renderer * * modified: 2019-02-01 Harald Braeuning * *
 ****************************************************************************/

package io.fair_acc.chartfx.renderer.spi;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Simple, uncomplicated reducing line renderer
 * 
 * @author braeun
 */
public class ReducingLineRenderer extends AbstractRendererXY<ReducingLineRenderer> implements Renderer {
    private int maxPoints;

    public ReducingLineRenderer() {
        maxPoints = 300;
    }

    public ReducingLineRenderer(final int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    /**
     * @return the instance of this ReducingLineRenderer.
     */
    @Override
    protected ReducingLineRenderer getThis() {
        return this;
    }

    @Override
    protected void render(GraphicsContext gc, DataSet ds, DataSetNode style) {
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());

        if (ds.getDataCount() > 0) {
            final int indexMin = Math.max(0, ds.getIndex(DIM_X, xMin));
            final int indexMax = Math.min(ds.getIndex(DIM_X, xMax) + 1, ds.getDataCount());
            final int n = Math.abs(indexMax - indexMin);
            final int d = n / maxPoints;
            if (d <= 1) {
                int i = ds.getIndex(DIM_X, xMin);
                if (i < 0) {
                    i = 0;
                }
                double x0 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                double y0 = yAxis.getDisplayPosition(ds.get(DIM_Y, i));
                i++;
                for (; i < Math.min(ds.getIndex(DIM_X, xMax) + 1, ds.getDataCount()); i++) {
                    final double x1 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                    final double y1 = yAxis.getDisplayPosition(ds.get(DIM_Y, i));
                    gc.strokeLine(x0, y0, x1, y1);
                    x0 = x1;
                    y0 = y1;
                }
            } else {
                int i = ds.getIndex(DIM_X, xMin);
                if (i < 0) {
                    i = 0;
                }
                double x0 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                double y0 = yAxis.getDisplayPosition(ds.get(DIM_Y, i));
                i++;
                double x1 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                double y1 = yAxis.getDisplayPosition(ds.get(DIM_Y, i));
                double delta = Math.abs(y1 - y0);
                i++;
                int j = d - 2;
                for (; i < Math.min(ds.getIndex(DIM_X, xMax) + 1, ds.getDataCount()); i++) {
                    if (j > 0) {
                        final double x2 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                        final double y2 = yAxis.getDisplayPosition(ds.get(DIM_Y, i));
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
                        x1 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                        y1 = yAxis.getDisplayPosition(ds.get(DIM_Y, i));
                        delta = Math.abs(y1 - y0);
                        j = d - 1;
                    }
                }
            }
        }
        gc.restore();
    }

    @Override
    public boolean drawLegendSymbol(final DataSetNode style, final Canvas canvas) {
        var gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());
        gc.strokeLine(1, canvas.getHeight() / 2.0, canvas.getWidth() - 2.0, canvas.getHeight() / 2.0);
        gc.restore();
        return true;
    }

    public void setMaxPoints(final int maxPoints) {
        this.maxPoints = maxPoints;
    }
}
