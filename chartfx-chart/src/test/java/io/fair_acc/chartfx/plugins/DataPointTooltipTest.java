package io.fair_acc.chartfx.plugins;

import static org.testfx.util.NodeQueryUtils.hasText;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.DebugUtils;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DataSetBuilder;
import io.fair_acc.dataset.testdata.spi.CosineFunction;

/**
 * Test the DataPointTooltip Plugin
 * 
 * @author Alexander Krimm
 */
@ExtendWith(ApplicationExtension.class)
class DataPointTooltipTest {
    private XYChart chart;
    private CosineFunction ds1;
    private CosineFunction ds1Copy;
    private DataSet ds2;
    private Axis xAxis1;
    private Axis yAxis1;
    private Axis xAxis2;
    private Axis yAxis2;
    private ErrorDataSetRenderer renderer2;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();
        final DataPointTooltip tooltip = new DataPointTooltip();
        chart.getPlugins().add(tooltip);
        chart.setId("myChart");
        chart.setPrefWidth(400);
        chart.setPrefHeight(300);

        // ordered Dataset
        xAxis1 = new DefaultNumericAxis("xAxis1", "t");
        xAxis1.setSide(Side.BOTTOM);
        yAxis1 = new DefaultNumericAxis("yAxis1", "m");
        yAxis1.setSide(Side.LEFT);
        final ErrorDataSetRenderer renderer1 = new ErrorDataSetRenderer();
        renderer1.getAxes().setAll(xAxis1, yAxis1);
        ds1 = new CosineFunction("Cosine", 50);
        ds1.addDataLabel(17, "SpecialPoint");
        renderer1.getDatasets().add(ds1);
        ds1Copy = new CosineFunction("Cosine", 50);
        ds1Copy.addDataLabel(17, "Special Point Copy");
        renderer1.getDatasets().add(ds1Copy);

        // unordered dataset
        xAxis2 = new DefaultNumericAxis("xAxis2", "V");
        xAxis2.setSide(Side.TOP);
        yAxis2 = new DefaultNumericAxis("yAxis2", "m");
        yAxis2.setSide(Side.RIGHT);
        renderer2 = new ErrorDataSetRenderer();
        renderer2.getAxes().setAll(xAxis2, yAxis2);
        ds2 = new DataSetBuilder("nonsorted")
                      .setValues(DIM_X, new double[] { 0, 1, 1, 0.5, 0, 0, 0.9, 0.1, 0.9 })
                      .setValues(DIM_Y, new double[] { 0, 0, 1, 1.5, 1, 0.1, 1, 1, 0.1 })
                      .build();
        renderer2.getDatasets().add(ds2);

        chart.getRenderers().setAll(renderer1, renderer2);
        Scene scene = new Scene(chart, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testThatTooltipIsShown(final FxRobot fxRobot) { // NOPMD JUnitTestsShouldIncludeAssert
        final String NL = System.lineSeparator();
        fxRobot.interrupt();

        // ordered dataset
        fxRobot.moveTo(getPointOnDataSet(xAxis1, yAxis1, ds1, 17)).moveBy(1, 0);
        FxAssert.verifyThat("." + DataPointTooltip.STYLE_CLASS_LABEL, hasText("'SpecialPoint'" + NL + "17.0, -0.8090169943749469"), DebugUtils.informedErrorMessage(fxRobot));
        fxRobot.moveTo(getPointOnDataSet(xAxis1, yAxis1, ds1, 36)).moveBy(1, 0);
        FxAssert.verifyThat("." + DataPointTooltip.STYLE_CLASS_LABEL, hasText("'Cosine [36]'" + NL + "36.0, 0.3090169943749491"), DebugUtils.informedErrorMessage(fxRobot));

        // unordered DataSet
        fxRobot.interact(() -> renderer2.setAssumeSortedData(false));
        fxRobot.moveTo(getPointOnDataSet(xAxis2, yAxis2, ds2, 5)).moveBy(1, 0);
        FxAssert.verifyThat("." + DataPointTooltip.STYLE_CLASS_LABEL, hasText("'nonsorted [5]'" + NL + "0.0, 0.1"), DebugUtils.informedErrorMessage(fxRobot));
    }

    private Point2D getPointOnDataSet(final Axis xAxis, final Axis yAxis, final DataSet ds, final int index) {
        return chart.getCanvas().localToScreen(new Point2D(
                xAxis.getDisplayPosition(ds.get(DIM_X, index)),
                yAxis.getDisplayPosition(ds.get(DIM_Y, index))));
    }
}
