package io.fair_acc.chartfx.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import javafx.geometry.VerticalDirection;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
public class ZoomerWithAxisInversionTest {

    private XYChart chart;
    private Axis xAxis;
    private Axis yAxis;
    private Zoomer zoomer;

    @Start
    void start(Stage stage) {
        xAxis = new DefaultNumericAxis("x", 0, 1, 10);
        yAxis = new DefaultNumericAxis("y", 0, 1, 10);
        chart = new XYChart(xAxis, yAxis);
        zoomer = new Zoomer();
        chart.getPlugins().add(zoomer);

        stage.setScene(new Scene(chart));
        stage.show();
    }

    // Test case for issue #705
    @Test
    void testZoomWithInvertedAxis(FxRobot robot) {
        yAxis.invertAxis(true);
        robot.moveTo(chart);

        assertThat(yAxis.getMin()).isLessThan(yAxis.getMax());
        robot.scroll(VerticalDirection.UP);

        // Initially min = 0; max = 1. This is not changed by inverting the axis.
        assertThat(yAxis.getMin()).as("y axis min and max exchanged").isLessThan(yAxis.getMax());
    }

}
