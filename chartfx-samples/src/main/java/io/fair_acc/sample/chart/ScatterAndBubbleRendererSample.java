package io.fair_acc.sample.chart;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * example to illustrate bubble- and scatter-type plot using either the DataSetError interface or (more customisable)
 * DataStyle Marker interface.
 *
 * @author rstein
 */
public class ScatterAndBubbleRendererSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScatterAndBubbleRendererSample.class);
    private static final String DEMO_DATA_FILE = "testdata/2017_OECD_data.csv";
    private final Map<String, Double> lifeExpectancyWomen = new ConcurrentHashMap<>();
    private final Map<String, Double> lifeExpectancyMen = new ConcurrentHashMap<>();
    private final Map<String, Double> gdpPerCapita = new ConcurrentHashMap<>();
    private final Map<String, Double> population = new ConcurrentHashMap<>();
    private double maxPopulation = 1.0;

    private void loadDemoData(final String fileName) {
        try (BufferedReader csvReader = Files
                                                .newBufferedReader(Paths.get(Objects.requireNonNull(this.getClass().getResource(fileName)).toURI()))) {
            // skip first row
            String row = csvReader.readLine();
            // LOCATION,TIME,LIFEEXP65 – WOMEN,LIFEEXP65 – MEN,TIME,USD_CAP,TIME,MLN_PER
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                final double pop = 1e6 * Double.parseDouble(data[7]);
                maxPopulation = Math.max(maxPopulation, pop);

                lifeExpectancyWomen.put(data[0], 65.0 + Double.parseDouble(data[2]));
                lifeExpectancyMen.put(data[0], 65.0 + Double.parseDouble(data[3]));
                gdpPerCapita.put(data[0], Double.parseDouble(data[5]));
                population.put(data[0], pop);
            }
        } catch (IOException | URISyntaxException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("InterruptedException");
            }
        }
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        // initialises Map based on https://data.oecd.org/ data
        loadDemoData(DEMO_DATA_FILE);

        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        DefaultErrorDataSet bubbleDataSet1a = new DefaultErrorDataSet("women");
        DefaultErrorDataSet bubbleDataSet1b = new DefaultErrorDataSet("men");
        DefaultDataSet bubbleDataSet2a = new DefaultDataSet("women");
        DefaultDataSet bubbleDataSet2b = new DefaultDataSet("men");

        Map<String, Double> sortedGDP = gdpPerCapita.entrySet().stream().sorted(Entry.comparingByValue()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
        // N.B. for the time the being the Zoomer plugin and Data reduction algorithm require
        // DataSets to be sorted along the x coordinate
        int count = 0;
        for (Entry<String, Double> entry : sortedGDP.entrySet()) {
            final String country = entry.getKey();
            final double gdp = entry.getValue();
            final double popSize = 0.2 * population.get(country) / maxPopulation;

            // set bubble size as error of y (also possible to set it as error of X or both)
            // N.B. Tip: Math.sqrt(..) since for the observer the surface are is more important than the diameter
            bubbleDataSet1a.add(gdp, lifeExpectancyWomen.get(country), Math.sqrt(popSize), Math.sqrt(popSize), country);
            bubbleDataSet1b.add(gdp, lifeExpectancyMen.get(country), Math.sqrt(popSize), Math.sqrt(popSize), country);

            // set markerSize according to bubble size
            bubbleDataSet2a.add(gdp, lifeExpectancyWomen.get(country), country);
            bubbleDataSet2b.add(gdp, lifeExpectancyMen.get(country), country);
            // N.B. markerSize is in pixel regardless of the xAxis or yAxis scale
            String markerSize = "markerSize=" + 40 * Math.sqrt(population.get(country) / maxPopulation) + "; index="
                              + count + ";";
            bubbleDataSet2a.addDataStyle(count, markerSize);
            bubbleDataSet2b.addDataStyle(count, markerSize);
            if ("FRA".equals(country)) {
                bubbleDataSet2a.addDataStyle(count, markerSize + "markerColor=darkblue; markerType=circle2;");
                bubbleDataSet2b.addDataStyle(count, markerSize + "markerColor=darkred; markerType=circle2;");
            } else if ("JPN".equals(country)) {
                bubbleDataSet2a.addDataStyle(count, markerSize + "markerColor=green; markerType=diamond;");
                bubbleDataSet2b.addDataStyle(count, markerSize + "markerColor=lightgreen; markerType=diamond;");
            }
            // for more marker types see DefaultMarker

            count++;
        }

        Chart chart1 = getDefaultChart("Bubble-Chart via DataSetError interface");
        final ErrorDataSetRenderer errorRenderer1 = new ErrorDataSetRenderer();
        errorRenderer1.setMarkerSize(1);
        errorRenderer1.setPolyLineStyle(LineStyle.NONE);
        errorRenderer1.setErrorStyle(ErrorStyle.ERRORBARS);
        errorRenderer1.setDrawMarker(false);
        errorRenderer1.setDrawBubbles(true);
        errorRenderer1.setAssumeSortedData(false); // !! important since DS is likely unsorted
        chart1.getRenderers().setAll(errorRenderer1);
        errorRenderer1.getDatasets().addAll(bubbleDataSet1a, bubbleDataSet1b);

        // alternative solution using Marker and DataSet style
        Stage secondaryStage = new Stage();

        Chart chart2 = getDefaultChart("Scatter-Chart via addDataStyle(<index>, <String>) interface");
        final ErrorDataSetRenderer errorRenderer2 = new ErrorDataSetRenderer();
        errorRenderer2.setMarkerSize(5);
        errorRenderer2.setPolyLineStyle(LineStyle.NONE);
        errorRenderer2.setErrorStyle(ErrorStyle.NONE);
        errorRenderer2.setDrawMarker(true);
        errorRenderer2.setAssumeSortedData(false); // !! important since DS is likely unsorted
        // set default marker either via
        bubbleDataSet2a.setStyle("markerType=circle;");
        bubbleDataSet2b.setStyle("markerType=circle;");
        // or via global default, this also allows to set custom marker implementing the 'Marker' interface
        errorRenderer2.setMarker(DefaultMarker.DIAMOND);

        errorRenderer2.setDrawBubbles(false);
        chart2.getRenderers().setAll(errorRenderer2);
        errorRenderer2.getDatasets().addAll(bubbleDataSet2a, bubbleDataSet2b);

        return new TabPane(new Tab("chart1", chart1), new Tab("chart2", chart2));
    }

    private static Chart getDefaultChart(final String title) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis("GDP", "USD/capita");
        xAxis.setAutoUnitScaling(true);
        xAxis.setAutoRangePadding(0.05);
        xAxis.setAutoRanging(false);
        xAxis.setMin(15_000);
        xAxis.setMax(75_000);
        DefaultNumericAxis yAxis = new DefaultNumericAxis("life-expectancy", "years");
        yAxis.setAutoRangePadding(0.05);

        XYChart chart = new XYChart(xAxis, yAxis);
        chart.setTitle(title);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new DataPointTooltip());
        HBox.setHgrow(chart, Priority.ALWAYS);

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
