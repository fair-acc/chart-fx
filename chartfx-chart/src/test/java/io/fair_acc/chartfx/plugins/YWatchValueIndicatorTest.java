package io.fair_acc.chartfx.plugins;

import static org.junit.jupiter.api.Assertions.*;

import static io.fair_acc.chartfx.renderer.spi.financial.utils.FinancialTestUtils.generateCosData;

import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
class YWatchValueIndicatorTest {
    private YWatchValueIndicator valueWatchIndicatorTested;
    private XYChart chart;
    private DefaultNumericAxis yAxis;

    @Start
    public void start(Stage stage) throws Exception {
        // check flow in the category too
        final DefaultNumericAxis xAxis = new DefaultNumericAxis("time", "iso");
        xAxis.setTimeAxis(true);
        yAxis = new DefaultNumericAxis("price", "points");

        final YWatchValueIndicator valueWatchIndicatorTested1 = new YWatchValueIndicator(yAxis); // auto
        valueWatchIndicatorTested1.setValue(49.0);
        final YWatchValueIndicator valueWatchIndicatorTested2 = new YWatchValueIndicator(yAxis, 50.3); // auto
        valueWatchIndicatorTested = new YWatchValueIndicator(yAxis, 50.12);

        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("TestData");
        generateCosData(dataSet);

        // prepare chart structure
        chart = new XYChart(xAxis, yAxis);
        chart.getDatasets().add(dataSet);
        chart.getPlugins().addAll(valueWatchIndicatorTested, valueWatchIndicatorTested1, valueWatchIndicatorTested2);
        stage.setScene(new Scene(chart));
        stage.show();
    }

    @TestFx
    void leftSide() {
        yAxis.setSide(Side.RIGHT);
        chart.layoutChildren();
    }

    @TestFx
    void rightSide() {
        yAxis.setSide(Side.RIGHT);
        chart.layoutChildren();

        // change to unseen position
        yAxis.setAutoRanging(false);
        yAxis.set(100.0, 150.0);
        valueWatchIndicatorTested.layoutChildren();

        // test drag mouse on it
        valueWatchIndicatorTested.handleDragMouseEvent(new MyMouseEvent(15, 25, MouseButton.PRIMARY, 1));
    }

    @TestFx
    void setMarkerValue() {
        valueWatchIndicatorTested.setMarkerValue(35.15);
        assertEquals(" 35.2", valueWatchIndicatorTested.getText());
        assertEquals(35.15, valueWatchIndicatorTested.getValue(), 1e-2);
    }

    @TestFx
    void setLineVisible() {
        valueWatchIndicatorTested.setLineVisible(false);
        valueWatchIndicatorTested.setLineVisible(true);
    }

    @Test
    void setId() {
        valueWatchIndicatorTested.setId("price");
        assertEquals("price", valueWatchIndicatorTested.getId());
    }

    @Test
    void setOcclusionPrevention(FxRobot robot) {
        robot.interact(() -> {
            assertFalse(valueWatchIndicatorTested.isPreventOcclusion());
            valueWatchIndicatorTested.setPreventOcclusion(true);
            assertTrue(valueWatchIndicatorTested.isPreventOcclusion());
            chart.requestLayout();
        });
        robot.interrupt(1);
        robot.interact(() -> {
            yAxis.setSide(Side.RIGHT);
        });
        robot.interrupt(1);
        robot.interact(() -> {
            valueWatchIndicatorTested.setPreventOcclusion(false);
            assertFalse(valueWatchIndicatorTested.isPreventOcclusion());
            yAxis.setSide(Side.LEFT);
        });
    }

    private static class MyMouseEvent extends MouseEvent {
        private static final long serialVersionUID = 0L;

        MyMouseEvent(final double x, final double y, final MouseButton mouseButton, final int clickCount) {
            super(MouseEvent.MOUSE_MOVED, x, y, //
                    x, y, // screen coordinates
                    mouseButton, // mouse button
                    clickCount, // clickCount
                    false, // shiftDown
                    false, // controlDown
                    false, // altDown
                    false, // metaDown
                    MouseButton.PRIMARY.equals(mouseButton), // primaryButtonDown
                    MouseButton.MIDDLE.equals(mouseButton), // middleButtonDown
                    MouseButton.SECONDARY.equals(mouseButton), // secondaryButtonDown
                    true, // synthesised
                    false, // popupTrigger
                    true, // stillSincePress
                    null // pickResult
            );
        }
    }
}
