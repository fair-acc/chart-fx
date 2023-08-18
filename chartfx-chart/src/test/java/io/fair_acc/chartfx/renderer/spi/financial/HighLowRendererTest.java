package io.fair_acc.chartfx.renderer.spi.financial;

import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidParameterException;
import java.util.Calendar;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.spi.financial.service.OhlcvRendererEpData;
import io.fair_acc.chartfx.renderer.spi.financial.utils.CalendarUtils;
import io.fair_acc.chartfx.renderer.spi.financial.utils.FinancialTestUtils;
import io.fair_acc.chartfx.renderer.spi.financial.utils.FinancialTestUtils.TestChart;
import io.fair_acc.chartfx.renderer.spi.financial.utils.Interval;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.AbstractDataSet;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
class HighLowRendererTest {
    private HighLowRenderer rendererTested;
    private XYChart chart;
    private OhlcvDataSet ohlcvDataSet;

    @Start
    void start(Stage stage) throws Exception {
        ProcessingProfiler.setDebugState(false); // enable for detailed renderer tracing
        ohlcvDataSet = new OhlcvDataSet("ohlc1");
        ohlcvDataSet.setData(FinancialTestUtils.createTestOhlcv());
        rendererTested = new HighLowRenderer(true);
        rendererTested.setComputeLocalRange(false);
        rendererTested.setComputeLocalRange(true);

        assertNull(rendererTested.getPaintBarColor(new OhlcvRendererEpData()));

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("time", "iso");
        xAxis.setTimeAxis(true);
        xAxis.setAutoRangeRounding(false);
        xAxis.setAutoRanging(false);
        Interval<Calendar> xrange = CalendarUtils.createByDateInterval("2020/11/18-2020/11/25");
        xAxis.set(xrange.from.getTime().getTime() / 1000.0, xrange.to.getTime().getTime() / 1000.0);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("price", "points");
        yAxis.setAutoRanging(false);

        // prepare chart structure
        chart = new XYChart(xAxis, yAxis);
        chart.getGridRenderer().setDrawOnTop(false);

        rendererTested.getDatasets().add(ohlcvDataSet);
        chart.getRenderers().clear();
        chart.getRenderers().add(rendererTested);

        // PaintBar extension usage
        rendererTested.setPaintBarMarker(d -> d.ds != null && (d.ds.get(OhlcvDataSet.DIM_Y_OPEN, d.index) - d.ds.get(OhlcvDataSet.DIM_Y_CLOSE, d.index) > 100.0) ? Color.MAGENTA : null);

        // Extension point usage
        rendererTested.addPaintAfterEp(data -> assertNotNull(data.gc));
        assertEquals(1, rendererTested.getPaintAfterEps().size());

        FinancialTheme.Sand.applyPseudoClasses(chart);

        stage.setScene(new Scene(chart, 800, 600));
        stage.show();
    }

    @TestFx
    void categoryAxisTest() {
        final CategoryAxis xAxis = new CategoryAxis("time [iso]");
        xAxis.getTickLabelStyle().setRotate(90);
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);
        ohlcvDataSet.setCategoryBased(true);

        chart.getAxes().add(0, xAxis);
        chart.layoutChildren();
    }

    @TestFx
    void checkMinimalDimRequired() {
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
    void testVolumeConstructor() {
        HighLowRenderer highLowRenderer = new HighLowRenderer(true);
        assertTrue(highLowRenderer.isPaintVolume());
        highLowRenderer = new HighLowRenderer(false);
        assertFalse(highLowRenderer.isPaintVolume());
        highLowRenderer = new HighLowRenderer();
        assertFalse(highLowRenderer.isPaintVolume());
    }

    @TestFx
    void noXyChartInstance() {
        assertThrows(InvalidParameterException.class, () -> rendererTested.setChart(new TestChart())); // NOSONAR NOPMD
    }

    @Test
    void getThis() {
        assertEquals(HighLowRenderer.class, rendererTested.getThis().getClass());
    }
}
