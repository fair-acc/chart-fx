package de.gsi.chart.plugins;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.chart.renderer.spi.financial.utils.FinancialTestUtils.generateCosData;

import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.spi.DefaultErrorDataSet;

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

        valueWatchIndicatorTested = new YWatchValueIndicator(yAxis, "%1.2f"); // auto
        valueWatchIndicatorTested = new YWatchValueIndicator(yAxis, "%1.2f", 50.12);

        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("TestData");
        generateCosData(dataSet);

        // prepare chart structure
        chart = new XYChart(xAxis, yAxis);
        chart.getDatasets().add(dataSet);
        stage.setScene(new Scene(chart));
        stage.show();
    }

    @TestFx
    void leftSide() {
        chart.getPlugins().add(valueWatchIndicatorTested);
    }

    @TestFx
    void rightSide() {
        yAxis.setSide(Side.RIGHT);
        chart.layoutChildren();
        chart.getPlugins().add(valueWatchIndicatorTested);

        // change to unseen position
        yAxis.setAutoRanging(false);
        yAxis.set(100.0, 150.0);
        valueWatchIndicatorTested.layoutChildren();

        // test drag mouse on it
        valueWatchIndicatorTested.handleDragMouseEvent(new MyMouseEvent(15, 25, MouseButton.PRIMARY, 1));
    }

    @TestFx
    void setMarkerValue() {
        chart.getPlugins().add(valueWatchIndicatorTested);
        valueWatchIndicatorTested.setMarkerValue(35.15);
        assertEquals(" 35.2", valueWatchIndicatorTested.getText());
        assertEquals(35.15, valueWatchIndicatorTested.getValue(), 1e-2);
    }

    @TestFx
    void setLineVisible() {
        chart.getPlugins().add(valueWatchIndicatorTested);
        valueWatchIndicatorTested.setLineVisible(true);
        valueWatchIndicatorTested.setLineVisible(false);
    }

    @Test
    void setId() {
        valueWatchIndicatorTested.setId("price");
        assertEquals("price", valueWatchIndicatorTested.getId());
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
