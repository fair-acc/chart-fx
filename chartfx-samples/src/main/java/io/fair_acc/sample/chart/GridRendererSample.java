package io.fair_acc.sample.chart;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;

public class GridRendererSample extends ChartSample {
    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final FlowPane root = new FlowPane();
        root.setAlignment(Pos.CENTER);

        final XYChart xyChart1 = new XYChart(new DefaultNumericAxis("x-Axis 1", 0, 100, 10),
                new DefaultNumericAxis("y-Axis 1", 0, 100, 20));
        xyChart1.setPrefSize(600, 300);

        final XYChart xyChart2 = new XYChart(new DefaultNumericAxis("x-Axis 2", 0, 100, 10),
                new DefaultNumericAxis("y-Axis 2", 0, 100, 20));
        xyChart2.setPrefSize(600, 300);
        xyChart2.getGridRenderer().getHorizontalMinorGrid().setVisible(true);
        xyChart2.getGridRenderer().getVerticalMinorGrid().setVisible(true);

        xyChart2.getGridRenderer().getHorizontalMajorGrid().setVisible(false);
        xyChart2.getGridRenderer().getHorizontalMinorGrid().setVisible(true); // implicit major = true
        xyChart2.getGridRenderer().getVerticalMajorGrid().setVisible(true);
        xyChart2.getGridRenderer().getVerticalMinorGrid().setVisible(true);
        //        xyChart2.getGridRenderer().getVerticalMinorGrid().setStyle(".chart-minor-grid-lines{visible:true}");
        //        xyChart2.getGridRenderer().getHorizontalMajorGrid().setStyle("-fx-stroke: blue;-fx-stroke-width:4;");
        //        xyChart2.getGridRenderer().getVerticalMajorGrid().setStyle("-fx-stroke: darkblue");

        final XYChart xyChart3 = new XYChart(new DefaultNumericAxis("x-Axis 3", 0, 100, 10),
                new DefaultNumericAxis("y-Axis 3", 0, 100, 20));
        xyChart3.setPrefSize(600, 300);
        xyChart3.getGridRenderer().getHorizontalMinorGrid().setVisible(true);
        xyChart3.getGridRenderer().getVerticalMinorGrid().setVisible(true);
        xyChart3.getGridRenderer().getHorizontalMajorGrid().setStroke(Color.BLUE);
        xyChart3.getGridRenderer().getVerticalMajorGrid().setStroke(Color.BLUE);
        xyChart3.getGridRenderer().getHorizontalMajorGrid().setStrokeWidth(1);
        xyChart3.getGridRenderer().getVerticalMajorGrid().setStrokeWidth(1);

        final XYChart xyChart4 = new XYChart(new DefaultNumericAxis("x-Axis 4", 0, 100, 10),
                new DefaultNumericAxis("y-Axis 4", 0, 100, 20));
        xyChart4.setPrefSize(600, 300);
        xyChart4.getGridRenderer().getHorizontalMajorGrid().getStrokeDashArray().setAll(15.0,
                15.0);
        xyChart4.getGridRenderer().getVerticalMajorGrid().getStrokeDashArray().setAll(5.0,
                5.0);
        xyChart4.getGridRenderer().getHorizontalMajorGrid().setStrokeWidth(2);
        xyChart4.getGridRenderer().getVerticalMajorGrid().setStrokeWidth(2);

        root.getChildren().addAll(xyChart1, xyChart2, xyChart3, xyChart4);

        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}