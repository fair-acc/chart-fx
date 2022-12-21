package io.fair_acc.sample.chart;

import fxsampler.SampleBase;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public abstract class ChartSample extends SampleBase {
    @Override
    public String getSampleName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getProjectVersion() {
        return "11.3.0";
    }

    @Override
    public Node getPanel(final Stage stage) {
        System.out.println("loading sample");
        return getChartPanel(stage);
    }

    public Node getChartPanel(final Stage stage) {
        System.out.println("loading sample");
        return new Label("Sample hast to override getPanel() or getChartPanel()");
    }

    @Override
    public String getJavaDocURL() {
        return "";
    }

    @Override
    public String getControlStylesheetURL() {
        return null;
    }

    @Override
    public String getSampleSourceURL() {
        return "";
    }
}
