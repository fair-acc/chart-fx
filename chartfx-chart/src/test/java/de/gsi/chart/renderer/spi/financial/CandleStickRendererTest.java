package de.gsi.chart.renderer.spi.financial;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.SAND;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConfig;
import de.gsi.chart.renderer.spi.financial.utils.FinancialTestUtils;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class CandleStickRendererTest {
    private CandleStickRenderer rendererTested;
    private XYChart chart;

    @Start
    public void start(Stage stage) throws Exception {
        OhlcvDataSet ohlcvDataSet = new OhlcvDataSet("ohlc1");
        ohlcvDataSet.setData(FinancialTestUtils.createTestOhlcv());
        rendererTested = new CandleStickRenderer(true);

        // check flow in the category too
        final CategoryAxis xAxis = new CategoryAxis("time [iso]");
        xAxis.setTickLabelRotation(90);
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("price", "points");

        // prepare chart structure
        chart = new XYChart(xAxis, yAxis);

        rendererTested.getDatasets().add(ohlcvDataSet);
        chart.getRenderers().add(rendererTested);

        // PaintBar extension usage
        rendererTested.setPaintBarMarker(d -> d.ohlcvItem != null ? d.ohlcvItem.getOpen() - d.ohlcvItem.getClose() > 100.0 ? Color.MAGENTA : null : null);

        // Extension point usage
        rendererTested.addPaintAfterEp(data -> assertNotNull(data.gc));

        new FinancialColorSchemeConfig().applyTo(SAND, chart);

        stage.setScene(new Scene(chart));
        stage.show();
    }

    @TestFx
    public void checkMinimalDimRequired() {
        rendererTested.getDatasets().clear();
        rendererTested.getDatasets().add(new AbstractDataSet<OhlcvDataSet>("testDim", 6) {
            @Override
            public double get(int dimIndex, int index) {
                return 0;
            }

            @Override
            public int getDataCount() {
                return 1;
            }

            @Override
            public DataSet set(DataSet other, boolean copy) {
                return null;
            }
        });
        var ref = new Object() {
            AssertionFailedError e = null;
        };
        rendererTested.addPaintAfterEp(data -> ref.e = new AssertionFailedError("The renderer method cannot be call, because dimensions are lower as required!"));
        chart.layoutChildren();
        if (ref.e != null) {
            throw ref.e;
        }
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
        assertEquals(CandleStickRenderer.class, rendererTested.getThis().getClass());
    }
}
