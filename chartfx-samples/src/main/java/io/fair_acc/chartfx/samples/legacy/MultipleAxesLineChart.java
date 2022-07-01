package io.fair_acc.chartfx.samples.legacy;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * https://stackoverflow.com/questions/9667405/how-to-draw-multiple-axis-on-a-chart-using-javafx-charts This is intended
 * for performance comparison and not part of the library.
 *
 * @author jediz
 */
@Deprecated
public class MultipleAxesLineChart extends StackPane {
    private final LineChart<?, ?> baseChart;
    private final ObservableList<LineChart<?, ?>> backgroundCharts = FXCollections.observableArrayList();
    private final Map<LineChart<?, ?>, Color> chartColorMap = new HashMap<>();

    private final double yAxisWidth = 60;
    private final AnchorPane detailsWindow;

    private final double yAxisSeparation = 20;
    private double strokeWidth = 0.3;

    public MultipleAxesLineChart(final LineChart<?, ?> baseChart, final Color lineColor) {
        this(baseChart, lineColor, null);
    }

    public MultipleAxesLineChart(final LineChart<?, ?> baseChart, final Color lineColor, final Double strokeWidth) {
        if (strokeWidth != null) {
            this.strokeWidth = strokeWidth;
        }
        this.baseChart = baseChart;

        chartColorMap.put(baseChart, lineColor);

        styleBaseChart(baseChart);
        styleChartLine(baseChart, lineColor);
        setFixedAxisWidth(baseChart);

        setAlignment(Pos.CENTER_LEFT);

        backgroundCharts.addListener((final Observable observable) -> rebuildChart());

        detailsWindow = new AnchorPane();
        bindMouseEvents(baseChart, this.strokeWidth);

        rebuildChart();
    }

    public void addSeries(final XYChart.Series<Number, Number> series, final Color lineColor) {
        final NumberAxis yAxis = new NumberAxis();
        final NumberAxis xAxis = new NumberAxis();

        // style x-axis
        xAxis.setAutoRanging(false);
        xAxis.setVisible(false);
        xAxis.setOpacity(0.0); // somehow the upper setVisible does not work
        xAxis.lowerBoundProperty().bind(((NumberAxis) baseChart.getXAxis()).lowerBoundProperty());
        xAxis.upperBoundProperty().bind(((NumberAxis) baseChart.getXAxis()).upperBoundProperty());
        xAxis.tickUnitProperty().bind(((NumberAxis) baseChart.getXAxis()).tickUnitProperty());

        // style y-axis
        yAxis.setSide(Side.RIGHT);
        yAxis.setLabel(series.getName());

        // create chart
        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.getData().add(series);

        styleBackgroundChart(lineChart, lineColor);
        setFixedAxisWidth(lineChart);

        chartColorMap.put(lineChart, lineColor);
        backgroundCharts.add(lineChart);
    }

    private void bindMouseEvents(final LineChart<?, ?> baseChart, final Double strokeWidth) {
        getChildren().add(detailsWindow);
        detailsWindow.prefHeightProperty().bind(heightProperty());
        detailsWindow.prefWidthProperty().bind(widthProperty());
        detailsWindow.setMouseTransparent(true);

        setOnMouseMoved(null);
        setMouseTransparent(false);

        final Axis<?> xAxis = baseChart.getXAxis();
        final Axis<?> yAxis = baseChart.getYAxis();

        final Line xLine = new Line();
        final Line yLine = new Line();
        yLine.setFill(Color.GRAY);
        xLine.setFill(Color.GRAY);
        yLine.setStrokeWidth(strokeWidth / 2);
        xLine.setStrokeWidth(strokeWidth / 2);
        xLine.setVisible(false);
        yLine.setVisible(false);

        final Node chartBackground = baseChart.lookup(".chart-plot-background");
        for (final Node n : chartBackground.getParent().getChildrenUnmodifiable()) {
            if ((n != chartBackground) && (n != xAxis) && (n != yAxis)) {
                n.setMouseTransparent(true);
            }
        }
    }

    public Node getLegend() {
        final HBox hBox = new HBox();

        final CheckBox baseChartCheckBox = new CheckBox(baseChart.getYAxis().getLabel());
        baseChartCheckBox.setSelected(true);
        baseChartCheckBox
                .setStyle("-fx-text-fill: " + toRGBCode(chartColorMap.get(baseChart)) + "; -fx-font-weight: bold;");
        baseChartCheckBox.setDisable(true);
        baseChartCheckBox.getStyleClass().add("readonly-checkbox");
        baseChartCheckBox.setOnAction(event -> baseChartCheckBox.setSelected(true));
        hBox.getChildren().add(baseChartCheckBox);

        for (final LineChart<?, ?> lineChart : backgroundCharts) {
            final CheckBox checkBox = new CheckBox(lineChart.getYAxis().getLabel());
            checkBox.setStyle("-fx-text-fill: " + toRGBCode(chartColorMap.get(lineChart)) + "; -fx-font-weight: bold");
            checkBox.setSelected(true);
            checkBox.setOnAction(event -> {
                if (backgroundCharts.contains(lineChart)) {
                    backgroundCharts.remove(lineChart);
                } else {
                    backgroundCharts.add(lineChart);
                }
            });
            hBox.getChildren().add(checkBox);
        }

        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(20);
        hBox.setStyle("-fx-padding: 0 10 20 10");

        return hBox;
    }

    private void rebuildChart() {
        getChildren().clear();

        getChildren().add(resizeBaseChart(baseChart));
        for (final LineChart<?, ?> lineChart : backgroundCharts) {
            getChildren().add(resizeBackgroundChart(lineChart));
        }
        getChildren().add(detailsWindow);
    }

    private Node resizeBackgroundChart(final LineChart<?, ?> lineChart) {
        final HBox hBox = new HBox(lineChart);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.prefHeightProperty().bind(heightProperty());
        hBox.prefWidthProperty().bind(widthProperty());
        hBox.setMouseTransparent(true);

        lineChart.minWidthProperty()
                .bind(widthProperty().subtract((yAxisWidth + yAxisSeparation) * backgroundCharts.size()));
        lineChart.prefWidthProperty()
                .bind(widthProperty().subtract((yAxisWidth + yAxisSeparation) * backgroundCharts.size()));
        lineChart.maxWidthProperty()
                .bind(widthProperty().subtract((yAxisWidth + yAxisSeparation) * backgroundCharts.size()));

        lineChart.translateXProperty().bind(baseChart.getYAxis().widthProperty());
        lineChart.getYAxis().setTranslateX((yAxisWidth + yAxisSeparation) * backgroundCharts.indexOf(lineChart));
        lineChart.getXAxis().tickLabelRotationProperty().bind(baseChart.getXAxis().tickLabelRotationProperty());
        return hBox;
    }

    private Node resizeBaseChart(final LineChart<?, ?> lineChart) {
        final HBox hBox = new HBox(lineChart);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.prefHeightProperty().bind(heightProperty());
        hBox.prefWidthProperty().bind(widthProperty());

        lineChart.minWidthProperty()
                .bind(widthProperty().subtract((yAxisWidth + yAxisSeparation) * backgroundCharts.size()));
        lineChart.prefWidthProperty()
                .bind(widthProperty().subtract((yAxisWidth + yAxisSeparation) * backgroundCharts.size()));
        lineChart.maxWidthProperty()
                .bind(widthProperty().subtract((yAxisWidth + yAxisSeparation) * backgroundCharts.size()));

        return lineChart;
    }

    private void setFixedAxisWidth(final LineChart<?, ?> chart) {
        chart.getYAxis().setPrefWidth(yAxisWidth);
        chart.getYAxis().setMaxWidth(yAxisWidth);
    }

    private void styleBackgroundChart(final LineChart<?, ?> lineChart, final Color lineColor) {
        styleChartLine(lineChart, lineColor);

        final Node chartContent = lineChart.lookup(".chart-content");
        if (chartContent != null) {
            final Node chartPlotBackground = chartContent.lookup(".chart-plot-background");
            if (chartPlotBackground != null) {
                chartPlotBackground.setStyle("-fx-background-color: transparent;");
            }
        }

        lineChart.setVerticalZeroLineVisible(false);
        lineChart.setHorizontalZeroLineVisible(false);
        lineChart.setVerticalGridLinesVisible(false);
        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setCreateSymbols(false);
    }

    private void styleBaseChart(final LineChart<?, ?> baseChart) {
        baseChart.setCreateSymbols(false);
        baseChart.setLegendVisible(false);
        baseChart.getXAxis().setAutoRanging(false);
        baseChart.getXAxis().setAnimated(false);
        baseChart.getYAxis().setAnimated(false);
    }

    private void styleChartLine(final LineChart<?, ?> chart, final Color lineColor) {
        chart.getYAxis().lookup(".axis-label").setStyle("-fx-text-fill: " + toRGBCode(lineColor) + "; -fx-font-weight: bold;");
        final Node seriesLine = chart.lookup(".chart-series-line");
        seriesLine.setStyle("-fx-stroke: " + toRGBCode(lineColor) + "; -fx-stroke-width: " + strokeWidth + ";");
    }

    private String toRGBCode(final Color color) {
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
