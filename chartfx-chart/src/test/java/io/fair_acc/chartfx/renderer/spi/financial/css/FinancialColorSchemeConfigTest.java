package io.fair_acc.chartfx.renderer.spi.financial.css;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.financial.*;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEP;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEPAware;
import io.fair_acc.chartfx.renderer.spi.financial.utils.FinancialTestUtils;
import io.fair_acc.chartfx.renderer.spi.financial.utils.FootprintRenderedAPIDummyAdapter;
import io.fair_acc.chartfx.renderer.spi.financial.utils.PositionFinancialDataSetDummy;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
class FinancialColorSchemeConfigTest {
    private FinancialColorSchemeConfig financialColorSchemeConfig;
    private OhlcvDataSet ohlcvDataSet;
    private Renderer renderer;
    private XYChart chart;

    @BeforeEach
    void setUp() {
        financialColorSchemeConfig = new FinancialColorSchemeConfig();
        ohlcvDataSet = new OhlcvDataSet("ohlc1");
        ohlcvDataSet.setData(FinancialTestUtils.createTestOhlcv());
        renderer = new CandleStickRenderer();
    }

    @Start
    public void start(Stage stage) {
        setUp();
        chart = new XYChart();
        // possibility to configure extension points
        ((CandleStickRenderer) renderer).addPaintAfterEp(new PositionFinancialRendererPaintAfterEP(new PositionFinancialDataSetDummy(new ArrayList<>()), chart));
        renderer.getDatasets().add(ohlcvDataSet);
        chart.getRenderers().add(renderer); // one possibility
        chart.getDatasets().add(ohlcvDataSet); // second possibility
        stage.setScene(new Scene(chart));
        stage.show();
    }

    @Test
    void applySchemeToDataset() {
        financialColorSchemeConfig.applySchemeToDataset(FinancialColorSchemeConstants.BLACKBERRY, "custom1=red", ohlcvDataSet, renderer);
        assertEquals("custom1=red", ohlcvDataSet.getStyle());
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, null, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", null, ohlcvDataSet, renderer));

        renderer = new HighLowRenderer();
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, null, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", null, ohlcvDataSet, renderer));

        renderer = new FootprintRenderer(new FootprintRenderedAPIDummyAdapter(null));
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, null, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", null, ohlcvDataSet, renderer));
    }

    @Test
    void testApplySchemeToDataset() {
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", ohlcvDataSet, renderer));

        renderer = new HighLowRenderer();
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", ohlcvDataSet, renderer));

        renderer = new FootprintRenderer(new FootprintRenderedAPIDummyAdapter(null));
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", ohlcvDataSet, renderer));

        renderer = new EmptyFinancialRenderer();
        ((EmptyFinancialRenderer) renderer).addPaintAfterEp(new PositionFinancialRendererPaintAfterEP(new PositionFinancialDataSetDummy(new ArrayList<>()), chart));
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", ohlcvDataSet, renderer));
    }

    @TestFx
    void applyTo() throws Exception {
        // just test pass of the all configuration, no test result in the chart - just configuration which is changed
        financialColorSchemeConfig.applyTo(FinancialColorSchemeConstants.BLACKBERRY, "custom1=white", chart);
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applyTo(colorScheme, null, chart);
        }
    }

    @TestFx
    void testApplyTo() throws Exception {
        // just test pass of the all configuration, no test result in the chart - just configuration which is changed
        financialColorSchemeConfig.applyTo(FinancialColorSchemeConstants.BLACKBERRY, chart);
        for (String colorScheme : FinancialColorSchemeConstants.getDefaultColorSchemes()) {
            financialColorSchemeConfig.applyTo(colorScheme, chart);
        }
    }

    private static class EmptyFinancialRenderer extends AbstractFinancialRenderer<EmptyFinancialRenderer> implements RendererPaintAfterEPAware {
        protected List<RendererPaintAfterEP> paintAfterEPS = new ArrayList<>();

        @Override
        public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
            // not used for test
            return null;
        }

        @Override
        public void render() {
            // not used for test
            return null;
        }

        @Override
        protected EmptyFinancialRenderer getThis() {
            return this;
        }

        @Override
        public void addPaintAfterEp(RendererPaintAfterEP paintAfterEP) {
            paintAfterEPS.add(paintAfterEP);
        }

        @Override
        public List<RendererPaintAfterEP> getPaintAfterEps() {
            return paintAfterEPS;
        }
    }
}
