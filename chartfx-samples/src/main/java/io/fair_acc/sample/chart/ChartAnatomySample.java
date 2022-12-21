package io.fair_acc.sample.chart;

import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.ui.geometry.Corner;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;

/**
 * @author rstein
 */
public class ChartAnatomySample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChartAnatomySample.class);

    @Override
    public void start(final Stage primaryStage) {
        final VBox root = new VBox();
        root.setAlignment(Pos.CENTER);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("x-Axis1", 0, 100, 1);
        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis("x-Axis2", 0, 100, 1);
        final DefaultNumericAxis xAxis3 = new DefaultNumericAxis("x-Axis3", -50, +50, 10);
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y-Axis1", 0, 100, 1);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("y-Axis2", 0, 100, 1);
        final DefaultNumericAxis yAxis3 = new DefaultNumericAxis("y-Axis3", 0, 100, 1);
        final DefaultNumericAxis yAxis4 = new DefaultNumericAxis("y-Axis4", -50, +50, 10);

        xAxis1.setSide(Side.BOTTOM);
        xAxis2.setSide(Side.TOP);
        xAxis3.setSide(Side.CENTER_HOR);
        xAxis3.setMinorTickCount(2);
        xAxis3.setAxisLabelTextAlignment(TextAlignment.RIGHT);
        yAxis1.setSide(Side.LEFT);
        yAxis2.setSide(Side.RIGHT);
        yAxis3.setSide(Side.RIGHT);
        yAxis4.setSide(Side.CENTER_VER);
        yAxis4.setMinorTickCount(2);
        yAxis4.setAxisLabelTextAlignment(TextAlignment.RIGHT);

        final Chart chart = new Chart() {
            @Override
            protected void axesChanged(Change<? extends Axis> change) {
                // TODO Auto-generated method stub
            }

            @Override
            protected void redrawCanvas() {
                // TODO Auto-generated method stub
            }

            @Override
            public void updateAxisRange() {
                // TODO Auto-generated method stub
            }

            @Override
            protected void updateLegend(final List<DataSet> dataSets, final List<Renderer> renderers) {
                // TODO Auto-generated method stub
            }
        };
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.getAxes().addAll(xAxis1, yAxis1);
        chart.setTitle("<Title> Hello World Chart </Title>");
        // chart.setToolBarSide(Side.LEFT);
        // chart.setToolBarSide(Side.BOTTOM);

        chart.getToolBar().getChildren().add(new Label("ToolBar Menu: "));
        for (final Side side : Side.values()) {
            final Button toolBarButton = new Button("ToolBar to " + side); // NOPMD
            toolBarButton.setOnMouseClicked(mevt -> chart.setToolBarSide(side));
            chart.getToolBar().getChildren().add(toolBarButton);
        }

        chart.getAxesPane(Side.BOTTOM).getChildren().add(xAxis1);
        chart.getAxesPane(Side.TOP).getChildren().add(xAxis2);
        chart.getAxesPane(Side.CENTER_HOR).getChildren().add(xAxis3);
        chart.getAxesPane(Side.LEFT).getChildren().add(yAxis1);
        chart.getAxesPane(Side.RIGHT).getChildren().add(yAxis2);
        chart.getAxesPane(Side.RIGHT).getChildren().add(yAxis3);
        chart.getAxesPane(Side.CENTER_VER).getChildren().add(yAxis4);

        chart.getTitleLegendPane(Side.LEFT).getChildren().add(new MyLabel("Title/Legend - left", true));
        chart.getTitleLegendPane(Side.RIGHT).getChildren().add(new MyLabel("Title/Legend - right", true));
        chart.getTitleLegendPane(Side.TOP).getChildren().add(new MyLabel("Title/Legend - top"));
        chart.getTitleLegendPane(Side.BOTTOM).getChildren().add(new MyLabel("Title/Legend - bottom"));

        chart.getAxesCornerPane(Corner.BOTTOM_LEFT).getChildren().add(new MyLabel("(BL)"));
        chart.getAxesCornerPane(Corner.BOTTOM_RIGHT).getChildren().add(new MyLabel("(BR)"));
        chart.getAxesCornerPane(Corner.TOP_LEFT).getChildren().add(new MyLabel("(TL)"));
        chart.getAxesCornerPane(Corner.TOP_RIGHT).getChildren().add(new MyLabel("(TR)"));

        for (final Corner corner : Corner.values()) {
            chart.getAxesCornerPane(corner).setStyle("-fx-background-color: rgba(125, 125, 125, 0.5);");
            chart.getTitleLegendCornerPane(corner).setStyle("-fx-background-color: rgba(175, 175, 175, 0.5);");
        }

        for (final Side side : Side.values()) {
            chart.getMeasurementBar(side).getChildren().add(new MyLabel("ParBox - " + side)); // NOPMD
            chart.getMeasurementBar(side).setStyle("-fx-background-color: rgba(125, 125, 125, 0.5);");
            // chart.setPinned(side, true);
        }

        chart.getCanvas().setMouseTransparent(false);
        chart.getCanvas().setOnMouseClicked(mevt -> LOGGER.atInfo().log("clicked on canvas"));
        ((Node) chart.getAxes().get(0)).setOnMouseClicked(mevt -> LOGGER.atInfo().log("clicked on xAxis"));
        chart.getCanvas().addEventHandler(MouseEvent.MOUSE_CLICKED,
                mevt -> LOGGER.atInfo().log("clicked on canvas - alt implementation"));

        root.getChildren().add(chart);

        final Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static class MyLabel extends Label {
        public MyLabel(final String label) {
            super(label);
            VBox.setVgrow(this, Priority.ALWAYS);
            HBox.setHgrow(this, Priority.ALWAYS);
            setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        }

        public MyLabel(final String label, boolean rotate) {
            this(label);
            if (rotate) {
                setRotate(90);
            }
        }
    }
}
