package io.fair_acc.sample.chart;

import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
        xAxis3.getAxisLabel().setTextAlignment(TextAlignment.RIGHT);
        yAxis1.setSide(Side.LEFT);
        yAxis2.setSide(Side.RIGHT);
        yAxis3.setSide(Side.RIGHT);
        yAxis4.setSide(Side.CENTER_VER);
        yAxis4.setMinorTickCount(2);
        yAxis4.getAxisLabel().setTextAlignment(TextAlignment.RIGHT);

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
            protected void updateLegend(final List<Renderer> renderers) {
                // TODO Auto-generated method stub
            }
        };

        chart.getAxes().addAll(
                xAxis1, xAxis2, // horizontal
                yAxis1, yAxis2, yAxis3, // vertical
                xAxis3, yAxis4); // center

        chart.setTitle("<Title> Hello World Chart </Title>");
        // chart.setToolBarSide(Side.LEFT);
        // chart.setToolBarSide(Side.BOTTOM);

        chart.getToolBar().getChildren().add(new Label("ToolBar Menu: "));
        for (final Side side : Side.values()) {
            final Button toolBarButton = new Button("ToolBar to " + side); // NOPMD
            toolBarButton.setOnMouseClicked(mevt -> chart.setToolBarSide(side));
            chart.getToolBar().getChildren().add(toolBarButton);
        }

        Color cornerCol = Color.color(0.5, 0.5, 0.5, 0.5);
        Color horCol = Color.color(0.0, 0.5, 0.5, 0.5);
        Color verCol = Color.color(0.0, 1.0, 0.5, 0.5);

        chart.getMeasurementPane()
                .addSide(Side.LEFT, new LabelPane("ParBox - left", true, verCol.darker()))
                .addSide(Side.RIGHT, new LabelPane("ParBox - right", true, verCol.darker()))
                .addSide(Side.TOP, new LabelPane("ParBox - top", horCol.darker()))
                .addSide(Side.BOTTOM, new LabelPane("ParBox - bottom", horCol.darker()));

        chart.getTitleLegendPane()
                .addSide(Side.LEFT, new LabelPane("Title/Legend - left", true, verCol.brighter()))
                .addSide(Side.RIGHT, new LabelPane("Title/Legend - right", true, verCol.brighter()))
                .addSide(Side.TOP, new LabelPane("Title/Legend - top", horCol.brighter()))
                .addSide(Side.BOTTOM, new LabelPane("Title/Legend - bottom", horCol.brighter()));

        chart.getTitleLegendPane()
                .addCorner(Corner.BOTTOM_LEFT, new LabelPane("(BL)", cornerCol.darker()))
                .addCorner(Corner.BOTTOM_RIGHT, new LabelPane("(BR)", cornerCol.darker()))
                .addCorner(Corner.TOP_LEFT, new LabelPane("(TL)", cornerCol.darker()))
                .addCorner(Corner.TOP_RIGHT, new LabelPane("(TR)", cornerCol.darker()));

        cornerCol = cornerCol.darker();
        chart.getAxesAndCanvasPane()
                .addCorner(Corner.BOTTOM_LEFT, new LabelPane("(BL)", cornerCol.brighter()))
                .addCorner(Corner.BOTTOM_RIGHT, new LabelPane("(BR)", cornerCol.brighter()))
                .addCorner(Corner.TOP_LEFT, new LabelPane("(TL)", cornerCol.brighter()))
                .addCorner(Corner.TOP_RIGHT, new LabelPane("(TR)", cornerCol.brighter()));

        chart.getCanvas().setMouseTransparent(false);
        chart.getCanvas().setOnMouseClicked(mevt -> LOGGER.atInfo().log("clicked on canvas"));
        ((Node) chart.getAxes().get(0)).setOnMouseClicked(mevt -> LOGGER.atInfo().log("clicked on xAxis"));
        chart.getCanvas().addEventHandler(MouseEvent.MOUSE_CLICKED,
                mevt -> LOGGER.atInfo().log("clicked on canvas - alt implementation"));

        final Scene scene = new Scene(chart, 1000, 600);
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

    private static class LabelPane extends StackPane {
        public LabelPane(final String text, Color bg) {
            label = new Label(text);
            getChildren().add(label);
            setBackground(new Background(new BackgroundFill(bg, CornerRadii.EMPTY, Insets.EMPTY)));
        }

        public LabelPane(final String text, boolean rotate, Color bg) {
            this(text, bg);
            if (rotate) {
                label.setRotate(90);
            }
        }

        final Label label;

    }
}
