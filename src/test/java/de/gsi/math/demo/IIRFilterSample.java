package de.gsi.math.demo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.spi.DoubleDataSet;
import de.gsi.math.DataSetMath;
import de.gsi.math.TRandom;
import de.gsi.math.demo.utils.AbstractDemoApplication;
import de.gsi.math.demo.utils.DemoChart;
import de.gsi.math.filter.iir.Butterworth;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class IIRFilterSample extends AbstractDemoApplication {

    private final int ORDER = 32;
    private final double sampling = 100e6;
    private final double center = 28e6;
    private final double width = 0.5e6;
    private DataSet fraw, fraw1, fraw2;
    private DataSet fspectra, fspectra1, fspectra2;
    private final TRandom rnd = new TRandom(0);

    private DataSet readDemoData(final int offset, final int nSamples) {
        final Butterworth bandPass = new Butterworth();
        bandPass.bandPass(ORDER, sampling, center, width);
        final Butterworth lowPass = new Butterworth();
        lowPass.lowPass(ORDER, sampling, width);
        final DoubleDataSet ret = new DoubleDataSet("raw data@" + offset);
        InputStream inputStream = IIRFilterSample.class.getResourceAsStream("./20190319_Schottky_SumX.csv.zip");
        try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {
            while (zipStream.getNextEntry() != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream))) {
                    for (int i = 0; i < 2; i++) {
                        // skip header;
                        reader.readLine();
                    }

                    int count = 0;
                    int n = 0;
                    System.err.println("start reading from " + offset);
                    for (String line = reader.readLine(); (line = reader.readLine()) != null;) {
                        final String[] str = line.split(",");
                        final double s = Math.sin(2 * Math.PI * (sampling - width) * count);
                        final double y = lowPass.filter(s * bandPass.filter(Double.parseDouble(str[1])));
                        if (count >= offset && n < nSamples) {
                            if (count % 100 == 0) {
                                ret.add(n / 100e6, y);
                                n++;
                            }
                        }

                        count++;
                    }
                    System.err.println("finished reading nSamples(total) = " + count);
                    return ret;

                } catch (final Exception e) {
                    e.printStackTrace();
                }

                zipStream.closeEntry();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initData() {
        final double fs = 100e6;
        final int nBins = 4 * 8192;
        // double[] xValues = new double[MAX_POINTS];
        // double[] yValues = new double[MAX_POINTS];

        // double x = 0;
        // for (int i = 0; i < xValues.length; i++) {
        // xValues[i] = i;
        // yValues[i] = readSignal(xValues[i]);
        // }
        // fraw = new DoubleErrorDataSet("raw data", xValues, yValues);
        fraw = readDemoData(27500, nBins);
        fraw1 = readDemoData(27500 + (int) (0.5e-3 * fs), nBins);
        fraw2 = readDemoData(27500 + (int) (1.5e-3 * fs), nBins);
        System.err.println("length 0 = " + fraw.getDataCount());
        System.err.println("length 1 = " + fraw1.getDataCount());
        System.err.println("length 2 = " + fraw2.getDataCount());

        fspectra = DataSetMath.magnitudeSpectrumDecibel(fraw);
        fspectra1 = DataSetMath.magnitudeSpectrumDecibel(fraw1);
        fspectra2 = DataSetMath.magnitudeSpectrumDecibel(fraw2);
    }

    @Override
    public Node getContent() {
        initData();
        final DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setLabel("time");
        chart1.getXAxis().setUnit("s");
        chart1.getYAxis().setLabel("magnitude");
        chart1.getYAxis().setUnit("a.u.");
        chart1.getDatasets().addAll(fraw1, fraw2);

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setLabel("frequency [fs]");
        chart2.getXAxis().setUnit("fs");
        // chart2.getXAxis().setAutoRanging(false);
        // chart2.getXAxis().setUpperBound(28e6 + 1e6);
        // chart2.getXAxis().setLowerBound(28e6 - 1e6);
        chart2.getYAxis().setLabel("magnitude");
        chart2.getYAxis().setUnit("a.u.");
        chart2.getDatasets().addAll(fspectra, fspectra1, fspectra2);

        return new VBox(chart1, chart2);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
