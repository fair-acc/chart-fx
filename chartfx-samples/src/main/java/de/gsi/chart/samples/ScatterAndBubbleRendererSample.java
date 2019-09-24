package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.marker.DefaultMarker;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * example to illustrate bubble- and scatter-type plot
 * using either the DataSetError interface or (more customisable) DataStyle Marker interface.
 *
 * @author rstein
 */
public class ScatterAndBubbleRendererSample extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScatterAndBubbleRendererSample.class);
    private static String demoDataFile = "testdata/2017_OECD_data.csv";
    private final Map<String, Double> lifeExpectancyWomen = new ConcurrentHashMap<>();
    private final Map<String, Double> lifeExpectancyMen = new ConcurrentHashMap<>();
    private final Map<String, Double> gdpPerCapita = new ConcurrentHashMap<>();
    private final Map<String, Double> population = new ConcurrentHashMap<>();
    private double maxPopulation = 1.0;

    @Override
    public void start(final Stage primaryStage) {

        // initialises Map based on https://data.oecd.org/ data
        loadDemoData(demoDataFile);

        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        DefaultErrorDataSet bubbleDataSet1a = new DefaultErrorDataSet("women");
        DefaultErrorDataSet bubbleDataSet1b = new DefaultErrorDataSet("men");
        DefaultDataSet bubbleDataSet2a = new DefaultDataSet("women");
        DefaultDataSet bubbleDataSet2b = new DefaultDataSet("men");

        Map<String, Double> sortedGDP = gdpPerCapita.entrySet().stream().sorted(Entry.comparingByValue())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
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
            String markerSize = "markerSize=" + 40 * Math.sqrt(population.get(country) / maxPopulation) + "; index="+count+";";
            bubbleDataSet2a.addDataStyle(count, markerSize);
            bubbleDataSet2b.addDataStyle(count, markerSize);
            if (country.equals("FRA")) {
                bubbleDataSet2a.addDataStyle(count, markerSize + "markerColor=darkblue; markerType=circle2;");
                bubbleDataSet2b.addDataStyle(count, markerSize + "markerColor=darkred; markerType=circle2;");
            } else if (country.equals("JPN")) {
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
        errorRenderer1.setErrorType(ErrorStyle.ERRORBARS);
        errorRenderer1.setDrawMarker(false);
        errorRenderer1.setDrawBubbles(true);
        chart1.getRenderers().setAll(errorRenderer1);
        errorRenderer1.getDatasets().addAll(bubbleDataSet1a, bubbleDataSet1b);

        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(new Scene(chart1, 800, 600));
        primaryStage.setOnCloseRequest(evt -> System.exit(0)); // NOPMD by rstein on 05/08/2019
        primaryStage.show();

        // alternative solution using Marker and DataSet style
        Stage secondaryStage = new Stage();

        Chart chart2 = getDefaultChart("Scatter-Chart via addDataStyle(<index>, <String>) interface");
        final ErrorDataSetRenderer errorRenderer2 = new ErrorDataSetRenderer();
        errorRenderer2.setMarkerSize(5);
        errorRenderer2.setPolyLineStyle(LineStyle.NONE);
        errorRenderer2.setErrorType(ErrorStyle.NONE);
        errorRenderer2.setDrawMarker(true);
        // set default marker either via
        bubbleDataSet2a.setStyle("markerType=circle;");
        bubbleDataSet2b.setStyle("markerType=circle;");
        // or via global default, this also allows to set custom marker implementing the 'Marker' interface
        errorRenderer2.setMarker(DefaultMarker.DIAMOND);

        errorRenderer2.setDrawBubbles(false);
        chart2.getRenderers().setAll(errorRenderer2);
        errorRenderer2.getDatasets().addAll(bubbleDataSet2a, bubbleDataSet2b);

        secondaryStage.setTitle(this.getClass().getSimpleName());
        secondaryStage.setScene(new Scene(chart2, 800, 600));
        secondaryStage.setOnCloseRequest(evt -> System.exit(0)); // NOPMD by rstein on 05/08/2019
        secondaryStage.show();
    }

    private static Chart getDefaultChart(final String title) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis("GDP", "USD/capita");
        xAxis.setAutoUnitScaling(true);
        xAxis.setAutoRangePadding(0.05);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(15000);
        xAxis.setUpperBound(75000);
        DefaultNumericAxis yAxis = new DefaultNumericAxis("life-expectancy", "years");
        yAxis.setAutoRangePadding(0.05);

        XYChart chart = new XYChart(xAxis, yAxis);
        chart.setTitle(title);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new DataPointTooltip());
        HBox.setHgrow(chart, Priority.ALWAYS);

        return chart;
    }

    private void loadDemoData(final String fileName) {
        try (BufferedReader csvReader = Files
                .newBufferedReader(Paths.get(this.getClass().getResource(fileName).toURI()))) {
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

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
