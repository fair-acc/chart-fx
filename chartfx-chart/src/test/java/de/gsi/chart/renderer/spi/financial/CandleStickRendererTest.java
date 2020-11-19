package de.gsi.chart.renderer.spi.financial;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.SAND;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConfig;
import de.gsi.chart.renderer.spi.financial.utils.FinancialTestUtils;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.dataset.spi.financial.OhlcvDataSet;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class CandleStickRendererTest {
    private OhlcvDataSet ohlcvDataSet;
    private CandleStickRenderer renderer;

    @BeforeEach
    public void setUp() {
        ohlcvDataSet = new OhlcvDataSet("ohlc1");
        ohlcvDataSet.setData(FinancialTestUtils.createTestOhlcv());
        renderer = new CandleStickRenderer(true);
    }

    @Start
    public void start(Stage stage) throws Exception {
        setUp();
        // check flow in the category too
        final CategoryAxis xAxis = new CategoryAxis("time [iso]");
        xAxis.setTickLabelRotation(90);
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("price", "points");

        // prepare chart structure
        XYChart chart = new XYChart(xAxis, yAxis);

        renderer.getDatasets().add(ohlcvDataSet);
        chart.getRenderers().add(renderer);

        // PaintBar extension usage
        renderer.setPaintBarMarker(
                ohlcvItem -> ohlcvItem.getOpen() - ohlcvItem.getClose() > 100.0 ? Color.MAGENTA : null);

        // Extension point usage
        renderer.addPaintAfterEp((gc, ohlcvItem, localBarWidth, barWidthHalf,
                                         x0, yOpen, yClose, yLow, yHigh, yDiff, yMin) -> {
            assertNotNull(gc);
        });

        new FinancialColorSchemeConfig().applyTo(SAND, chart);

        stage.setScene(new Scene(chart));
        stage.show();
    }

    @Test
    public void testVolumeContructor() {
        CandleStickRenderer candleStickRenderer = new CandleStickRenderer(true);
        assertTrue(candleStickRenderer.isPaintVolume());
        candleStickRenderer = new CandleStickRenderer(false);
        assertFalse(candleStickRenderer.isPaintVolume());
        candleStickRenderer = new CandleStickRenderer();
        assertFalse(candleStickRenderer.isPaintVolume());
    }

    @Test
    void getThis() {
        assertEquals(CandleStickRenderer.class, renderer.getThis().getClass());
    }

    @Test
    void addPaintAfterEp() {
        // just check that call of method
        renderer.addPaintAfterEp((gc, ohlcvItem, localBarWidth, barWidthHalf,
                                         x0, yOpen, yClose, yLow, yHigh, yDiff, yMin) -> {});
        assertFalse(renderer.paintAfterEPS.isEmpty());
    }
}