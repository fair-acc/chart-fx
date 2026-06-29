package io.fair_acc.chartfx.plugins;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static org.testfx.util.NodeQueryUtils.hasText;

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
import io.fair_acc.dataset.testdata.spi.CosineFunction;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Test the DataPointTooltip plugin in the case that axis were only added to the chart and not the renderers.
 *
 * @author Benjamin Peter
 */
@ExtendWith(ApplicationExtension.class)
class DataPointTooltipAxisFromChartTest {

    private XYChart chart;
    private CosineFunction ds;
    private Axis xAxis;
    private Axis yAxis;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();
        chart.getPlugins().add(new DataPointTooltip());
        chart.setPrefWidth(400);
        chart.setPrefHeight(300);

        xAxis = new DefaultNumericAxis("xAxis", "t");
        xAxis.setSide(Side.BOTTOM);
        yAxis = new DefaultNumericAxis("yAxis", "m");
        yAxis.setSide(Side.LEFT);
        chart.getAxes().setAll(xAxis, yAxis);

        final ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();

        ds = new CosineFunction("Cosine", 50);
        renderer.getDatasets().add(ds);

        chart.getRenderers().setAll(renderer);
        Scene scene = new Scene(chart, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testThatTooltipIsShown(final FxRobot fxRobot) { // NOPMD JUnitTestsShouldIncludeAssert
        fxRobot.interrupt();

        fxRobot.moveTo(getPointOnDataSet(xAxis, yAxis, ds, 36)).moveBy(1, 0);
        FxAssert.verifyThat(
                "." + DataPointTooltip.STYLE_CLASS_LABEL,
                hasText("'Cosine [36]'" + System.lineSeparator() + "36.0, 0.3090169943749491"),
                DebugUtils.informedErrorMessage(fxRobot));
    }

    private Point2D getPointOnDataSet(final Axis xAxis, final Axis yAxis, final DataSet ds, final int index) {
        return chart.getCanvas()
                .localToScreen(
                        new Point2D(xAxis.getDisplayPosition(ds.get(DIM_X, index)),
                                yAxis.getDisplayPosition(ds.get(DIM_Y, index))));
    }

}
