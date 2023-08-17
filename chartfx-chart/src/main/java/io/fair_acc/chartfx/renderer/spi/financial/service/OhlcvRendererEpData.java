package io.fair_acc.chartfx.renderer.spi.financial.service;

import io.fair_acc.chartfx.renderer.spi.financial.FinancialDataSetNode;
import javafx.scene.canvas.GraphicsContext;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModelAware;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItemAware;

/**
 * Domain object for OHLC/V Renderer Extension Points
 */
public class OhlcvRendererEpData {
    public GraphicsContext gc;
    public DataSet ds;
    public FinancialDataSetNode style; // style information (may wrap a different dataset than ds)
    public AttributeModelAware attrs; // addon (if available)
    public IOhlcvItemAware ohlcvItemAware; // get item by index (if available)
    public IOhlcvItem ohlcvItem; // item domain object (if available)
    public int minIndex; // min index of rendered bar
    public int maxIndex; // max index of rendered bar
    public int index; // index of rendered bar
    public double barWidth; // width of bar
    public double barWidthHalf; // half of bar
    public double xCenter; // x0 center of bar
    public double yOpen; // open in display coords
    public double yHigh; // high in display coords
    public double yLow; // low in display coords
    public double yClose; // close in display coords
    public double yDiff; // diff = open - close
    public double yMin; // minimal y coord of bar
    public Object addon; // addon defined by specific renderer
}
