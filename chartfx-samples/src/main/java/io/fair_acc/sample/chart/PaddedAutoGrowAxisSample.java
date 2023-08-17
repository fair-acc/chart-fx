package io.fair_acc.sample.chart;

import java.util.concurrent.TimeUnit;

import io.fair_acc.dataset.utils.CachedDaemonThreadFactory;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.spi.CircularDoubleErrorDataSet;

/**
 * Auto grow-ranging example.
 *
 * @author ennerf
 * @author akrimm
 */

public class PaddedAutoGrowAxisSample extends ChartSample {
    @Override
    public Node getChartPanel(Stage primaryStage) {
        var xAxis = new DefaultNumericAxis();
        var yAxis = new DefaultNumericAxis();
        var chart = new XYChart(xAxis, yAxis);
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new Zoomer());

        yAxis.setAutoRangePadding(0.05);
        yAxis.setAutoRanging(false);
        yAxis.setAutoGrowRanging(true);
        yAxis.getAutoRange().set(0, 10);

        var ds = new CircularDoubleErrorDataSet("", 150);
        chart.getDatasets().addAll(ds);
        CachedDaemonThreadFactory.getInstance().newThread(() -> {
            while (true) {
                ds.reset();
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                    FXUtils.runAndWait(() -> {
                        yAxis.getAutoRange().set(0, 10);
                    });
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < 500; i++) {
                    ds.add(i, 40 * Math.sin(i * 0.1) + 100 * Math.sin(i * 0.02), 0, 0);
                    try {
                        TimeUnit.MILLISECONDS.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
