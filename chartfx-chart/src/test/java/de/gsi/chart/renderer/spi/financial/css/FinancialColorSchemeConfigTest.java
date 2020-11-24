package de.gsi.chart.renderer.spi.financial.css;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.BLACKBERRY;
import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.getDefaultColorSchemes;

import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.financial.CandleStickRenderer;
import de.gsi.chart.renderer.spi.financial.HighLowRenderer;
import de.gsi.chart.renderer.spi.financial.utils.FinancialTestUtils;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.spi.financial.OhlcvDataSet;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
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
        renderer.getDatasets().add(ohlcvDataSet);
        chart.getRenderers().add(renderer); // one possibility
        chart.getDatasets().add(ohlcvDataSet); // second possibility
        stage.setScene(new Scene(chart));
        stage.show();
    }

    @Test
    void applySchemeToDataset() {
        financialColorSchemeConfig.applySchemeToDataset(BLACKBERRY, "custom1=red", ohlcvDataSet, renderer);
        assertEquals("custom1=red", ohlcvDataSet.getStyle());
        for (String colorScheme : getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, null, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", null, ohlcvDataSet, renderer));

        renderer = new HighLowRenderer();
        for (String colorScheme : getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, null, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", null, ohlcvDataSet, renderer));
    }

    @Test
    void testApplySchemeToDataset() {
        for (String colorScheme : getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", ohlcvDataSet, renderer));

        renderer = new HighLowRenderer();
        for (String colorScheme : getDefaultColorSchemes()) {
            financialColorSchemeConfig.applySchemeToDataset(colorScheme, ohlcvDataSet, renderer);
        }
        assertThrows(IllegalArgumentException.class, () -> financialColorSchemeConfig.applySchemeToDataset("NOT_EXIST", ohlcvDataSet, renderer));
    }

    @TestFx
    void applyTo() throws Exception {
        // just test pass of the all configuration, no test result in the chart - just configuration which is changed
        financialColorSchemeConfig.applyTo(BLACKBERRY, "custom1=white", chart);
        for (String colorScheme : getDefaultColorSchemes()) {
            financialColorSchemeConfig.applyTo(colorScheme, null, chart);
        }
    }

    @TestFx
    void testApplyTo() throws Exception {
        // just test pass of the all configuration, no test result in the chart - just configuration which is changed
        financialColorSchemeConfig.applyTo(BLACKBERRY, chart);
        for (String colorScheme : getDefaultColorSchemes()) {
            financialColorSchemeConfig.applyTo(colorScheme, chart);
        }
    }
}
