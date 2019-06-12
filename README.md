[![Codacy Badge](https://api.codacy.com/project/badge/Grade/19bab6e7f156442c912bb742d494c0c0)](https://www.codacy.com/app/GSI/chart-fx?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=GSI-CS-CO/chart-fx&amp;utm_campaign=Badge_Grade)
[![Coverity Build Status](https://scan.coverity.com/projects/chart-fx/badge.svg)](https://scan.coverity.com/projects/chart-fx)
[![travis-ci Build Status JDK8](https://img.shields.io/travis/GSI-CS-CO/chart-fx/JDK8.svg?label=build%20JDK8)](https://travis-ci.org/GSI-CS-CO/chart-fx)
[![Maven Central](https://img.shields.io/maven-central/v/de.gsi.chart/chartfx-chart/8.svg)](https://search.maven.org/artifact/de.gsi.chart-fx/chartfx-chart)
[![travis-ci Build Status OpenJDK11](https://img.shields.io/travis/GSI-CS-CO/chart-fx/master.svg?label=build%20JDK11)](https://travis-ci.org/GSI-CS-CO/chart-fx)
[![Maven Central](https://img.shields.io/maven-central/v/de.gsi.chart/chartfx-chart/11.svg)](https://search.maven.org/artifact/de.gsi.chart-fx/chartfx-chart)

# ChartFx

ChartFx is scientific charting library developed at [GSI](https://www.gsi.de) for FAIR with focus on performance optimised real-time data visualisation at 25 Hz update rates for data sets with a few 10 thousand up to 5 million data points common in digital signal processing applications.
Based on earlier Swing-based designs used at GSI and CERN, it is a re-write of JavaFX's default [Chart](https://docs.oracle.com/javase/8/javafx/api/javafx/scene/chart/Chart.html) implementation and aims to preserve the feature-rich and extensible functionality of earlier and other similar Swing-based libraries while addressing the performance bottlenecks and API issues. 
The motivation for the re-design has been presented at [IPAC'19](https://ipac19.org/) ([paper](docs/THPRB028.pdf), [poster](docs/THPRB028_poster.pdf)).

<figure>
  <img src="docs/pics/chartfx-example1.png" alt="ChartFx example" width=1200/>
  <figcaption>
  Example showing error-bar and error-surface representations, display of mock meta-data, `ChartPlugin` interactors and data parameter measurement indicators (here: '20%-80% rise-time' between 'Marker#0' and 'Marker#1').
  </figcaption>
</figure>

## Functionalities and Features
The library offers a wide variety of plot types common in the scientific signal processing field, a flexible plugin system as well as online parameter measurements commonly found in lab instrumentation. Some of its features include (see demos for more details):
* `DataSet`: basic XY-type datasets, extendable by `DataSetError` to account for measurement uncertainties, `DataSetMetaData`, `EditableDataSet`, `Histogram`, or `DataSet3D` interfaces;
* math sub-library: FFTs, Wavelet and other spectral and linear algebra routines, numerically robust integration and differentiation, IIR- & FIR-type filtering, linear regression and non-linear chi-square-type function fitting;
* `Chart`: providing euclidean, polar, or 2D projections of 3D data sets, and a configurable legend;
* `Axis`: one or multiple axes that are linear, logarithmic, time-series, inverted, dynamic auto-(grow)-ranging, automatic range-based SI and time unit conversion;
* `Renderer`: scatter-plot, poly-line, area-plot, error-bar and error-surfaces, vertical bar-plots, Bezier-curve, stair-case, 1D/2D histograms, mountain-range display, true contour plots, heatmaps, fading DataSet history, labelled chart range and indicator marker, hexagon-map, meta data (i.e. for indicating common measurement errors, warnings or infos such as over- or under-ranging, device or configuration errors etc.);
* `ChartPlugin`: data zoomer with history, zoom-to-origin, and option to limit this to X and/or Y coordinates, panner, data value and range indicators, cross-hair indicator, data point tool-tip, `DataSet` editing, table view, export to CSV and system clipboard, online axis editing, data set parameter measurement such as rise-time, min, max, rms, etc.

In order to provide some of the scenegraph-level functionality while using a `Canvas` as graphics backend, the functionality of each module was extended to be readily customized through direct API methods as well as through external CSS-type style sheets.

## Examples
### Simple example

<img src="docs/pics/SimpleChartSample.png" width=800 alt="simple ChartFx example"/>

<details><summary>The corresponding source code (expand)</summary>

```Java
    private static final int N_SAMPLES = 100;
    // [..]
    @Override
    public void start(final Stage primaryStage) {
        final StackPane root = new StackPane();

        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        root.getChildren().add(chart);

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");
        // lineChartPlot.getDatasets().add(dataSet1); // for single data set
        chart.getDatasets().addAll(dataSet1, dataSet2); // two data sets

        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues1 = new double[N_SAMPLES];
        final double[] yValues2 = new double[N_SAMPLES];
        for (int n = 0; n < N_SAMPLES; n++) {
            xValues[n] = n;
            yValues1[n] = Math.cos(Math.toRadians(10.0 * n));
            yValues2[n] = Math.sin(Math.toRadians(10.0 * n));
        }
        dataSet1.set(xValues, yValues1);
        dataSet2.set(xValues, yValues2);

        final Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
    }
    // [..]
```
</details>

### more examples

<table>

<tr>
<td><figure><img src="docs/pics/CategoryAxisSample.png" alt="CategoryAxisSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/CategoryAxisSample.java">CategoryAxisSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/MultipleAxesSample.png" alt="MultipleAxesSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/MultipleAxesSample.java">MultipleAxesSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/TimeAxisSample.png" alt="TimeAxisSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/TimeAxisSample.java">TimeAxisSample.java</a></figcaption></figure></td>                         
</tr>

<tr>
<td><figure><img src="docs/pics/LogAxisSample.png" alt="LogAxisSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/LogAxisSample.java">LogAxisSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/HistogramSample.png" alt="HistogramSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/HistogramSample.java">HistogramSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/Histogram2DimSample.png" alt="Histogram2DimSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/Histogram2DimSample.java">Histogram2DimSample.java</a></figcaption></figure></td>
</tr>

<tr>
<td><figure><img src="docs/pics/EditDataSample.png" alt="EditDataSetSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/EditDataSetSample.java">EditDataSetSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/PolarPlotSample.png" alt="PolarPlotSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/PolarPlotSample.java">PolarPlotSample.java</a></figcaption></figure></td>                
<td><figure><img src="docs/pics/MetaDataRendererSample2.png" alt="EditDataSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/MetaDataRendererSample.java">MetaDataRendererSample.java</a></figcaption></figure></td>
</tr>

<tr>
<td><figure><img src="docs/pics/HistoryDataSetRendererSample.png" alt="HistoryDataSetRendererSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/HistoryDataSetRendererSample.java">HistoryDataSetRendererSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/MountainRangeRendererSample.png" alt="MountainRangeRendererSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/MountainRangeRendererSample.java">MountainRangeRendererSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/ChartAnatomySample.png" alt="ChartAnatomySample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/ChartAnatomySample.java">ChartAnatomySample.java</a></figcaption></figure></td>
</tr>

<tr>
<td><figure><img src="docs/pics/ErrorDataSetRendererStylingSample1.png" alt="ErrorDataSetRendererStylingSample1" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/ErrorDataSetRendererStylingSample.java">ErrorDataSetRendererStylingSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/ErrorDataSetRendererStylingSample2.png" alt="ErrorDataSetRendererStylingSample2" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/ErrorDataSetRendererStylingSample.java">ErrorDataSetRendererStylingSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/LabelledMarkerSample.png" alt="LabelledMarkerSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/LabelledMarkerSample.java">LabelledMarkerSample.java</a></figcaption></figure></td>
</tr>

<tr>
<td colspan=2><figure><img src="docs/pics/ContourChartSample1.png" alt="ContourChartSample1" width=600/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/ContourChartSample.java">ContourChartSample.java</a></figcaption></figure></td>
<td></td>
</tr>
<tr>
<td colspan=2><figure><img src="docs/pics/ContourChartSample2.png" alt="ContourChartSample" width=600/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/ContourChartSample.java">ContourChartSample.java</a></figcaption></figure></td>
<td></td>
</tr>

<tr>
<td colspan=2><figure><img src="docs/pics/ChartIndicatorSample.png" alt="ChartIndicatorSample" width=600/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/chart/samples/ChartIndicatorSample.java">ChartIndicatorSample.java</a></figcaption></figure></td>         
<td></td>
</tr>

</table>

### Math- & Signal-Processing related examples

<table>
<tr>
<td><figure><img src="docs/pics/DataSetAverageSample.png" alt="DataSetAverageSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/DataSetAverageSample.java">DataSetAverageSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/DataSetFilterSample.png" alt="DataSetFilterSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/DataSetFilterSample.java">DataSetFilterSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/DataSetIntegrateDifferentiateSample.png" alt="DataSetIntegrateDifferentiateSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/DataSetIntegrateDifferentiateSample.java">DataSetIntegrateDifferentiateSample.java</a></figcaption></figure></td>
</tr>

<tr>
<td><figure><img src="docs/pics/DataSetSpectrumSample.png" alt="DataSetSpectrumSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/DataSetSpectrumSample.java">DataSetSpectrumSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/FourierSample.png" alt="FourierSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/FourierSample.java">FourierSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/FrequencyFilterSample.png" alt="FrequencyFilterSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/FrequencyFilterSample.java">FrequencyFilterSample.java</a></figcaption></figure></td>
</tr>

<tr>
<td><figure><img src="docs/pics/GaussianFitSample.png" alt="GaussianFitSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/GaussianFitSample.java">GaussianFitSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/IIRFilterSample.png" alt="IIRFilterSample" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/IIRFilterSample.java">IIRFilterSample.java</a></figcaption></figure></td>
<td><figure><img src="docs/pics/WaveletScalogram.png" alt="WaveletScalogram" width=300/><figcaption><a href="chartfx-samples/src/main/java/de/gsi/math/samples/WaveletScalogram.java">WaveletScalogram.java</a></figcaption></figure></td>
</tr>
</table>

## Performance Comparison
Besides the extended functionality outlined above, the ChartFx optimisation goal also included achieving real-time update rates of up to 25 Hz for data sets with a few 10k up to 5 million data points. In order to optimise and compare the performance with other charting libraries, especially those with only reduced functionality, a reduced simple oscilloscope-style test case has been chosen (see `RollingBufferSample` in demos) that displays two curves with independent auto-ranging y-axes, common sliding time-series axis, and without further `ChartPlugin`s. The test-case and direct performance comparison between the ChartFx and JavaFX charting library for update rates at 25 Hz and 2 Hz is shown below.

<table style="width:100%">
<tr><td colspan=2><figure>
	<img src="docs/pics/chartfx-performance-test-case.png" alt="ChartFx performance comparison test-case" width=99%/>
  	<figcaption>Performance test scenario with two independent graphs, independent auto-ranging y-axes, and common scrolling time-series axis. Test system: Linux, 4.12.14, Intel(R) Core(TM) i7 CPU 860 @2.80GHz and GeForce GTX 670 GPU (NVIDIA driver).</figcaption>
</figure></td></tr>
<tr><td><figure>
	<img src="docs/pics/chartfx-performance1a.png" alt="JavaFX-ChartFx performance comparison for 25 Hz" width=90%/>
  	<figcaption>Performance comparison @ 25 Hz update rate.</figcaption>
</figure></td>
<td><figure>
  	<img src="docs/pics/chartfx-performance1a.png" alt="JavaFX-ChartFx performance comparison for 2 Hz" width=90%/>
  	<figcaption>Performance comparison @ 2 Hz update rate.</figcaption>
</figure></td></tr>
</table> 

While the ChartFx implementation already achieved a better functionality and a by two orders of magnitude improved performance for very large datasets, the basic test scenario has also been checked against popular existing Java-Swing and non-Java based UI charting frameworks. The Figure below provides a summary of the evaluated chart libraries for update rates at 25 Hz and 1k samples.

<figure>
  <img src="docs/pics/chartfx-performance1.png" alt="ChartFx performance comparison" width=800/>
  <figcaption>
  Chart performance comparison for popular JavaFX, Java-Swing, C++/Qt and WebAssembly-based implementations: <a href="https://github.com/extjfx/extjfx">ExtJFX</a>, <a href="https://github.com/GSI-CS-CO/chart-fx">ChartFx</a>, <a href="https://github.com/HanSolo/charts">HanSolo Charts</a>, <a href="http://www.jfree.org/jfreechart/">JFreeChart</a>, <a href="https://cds.cern.ch/record/1215878">JDataViewer</a>, <a href="https://www.qcustomplot.com/">QCustomPlot</a>, <a href="https://doc.qt.io/qt-5/qtcharts-index.html">Qt-Charts</a>, <a href="https://doc.qt.io/qt-5/wasm.html">WebAssembly</a>. The last `Qt Charts` entries show results for 100k data points being updated at 25 Hz.
  </figcaption>
</figure>

## Some thoughts
While starting out to improve the JDK's JavaFX Chart functionality and performance through initially extending, then gradually replacing bottle-necks, and eventually re-designing and replacing the original implementations, the resulting ChartFx library provides a substantially larger functionality and achieved an about two orders of magnitude performance improvement. 
Nevertheless, improved functionality aside, a direct performance comparison even for the best-case JavaFX scenario (static axes) with other non-JavaFX libraries demonstrated the raw JavaFX graphics performance -- despite the redesign -- being still behind the existing Java Swing-based JDataViewer and most noticeable the Qt Charts implementations. The library will continued to be maintained here at GitHub and further used for existing and future JavaFX-based control room UIs at GSI. 
The gained experience and interfaces will provide a starting point for a planned C++-based counter-part implementation using Qt or another suitable low-level charting library.

## Acknowledgements
We express our thanks and gratitude to the JavaFX community, in particular to @GregKrug and Vito Baggiolini at CERN for their valuable insights, discussions and feedback on this topic.
