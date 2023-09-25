package io.fair_acc.sample.chart;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.testdata.spi.CosineFunction;
import io.fair_acc.dataset.testdata.spi.GaussFunction;
import io.fair_acc.dataset.testdata.spi.RandomWalkFunction;

/**
 * Example on how to use chart-fx from fxml.
 *
 * @author Alexander Krimm
 */
public class FxmlSample extends ChartSample implements Initializable {
    private static final int N_SAMPLES = 500;

    @FXML
    private Menu mMain;
    @FXML
    private MenuItem addCos;
    @FXML
    private MenuItem addRandom;
    @FXML
    private MenuItem addGauss;
    @FXML
    private MenuItem clearChart;
    @FXML
    private MenuItem mExit;
    @FXML
    private XYChart chart;
    @FXML
    private ErrorDataSetRenderer errorDataSetRenderer;

    @Override
    public Node getChartPanel(Stage stage) {
        Parent root;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FxmlSample.fxml"));
        loader.setController(this);
        try {
            root = loader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return root;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorDataSetRenderer.getDatasets().add(new CosineFunction("cos", N_SAMPLES));
        addGauss.setOnAction(evt -> errorDataSetRenderer.getDatasets().add(new GaussFunction("gauss", N_SAMPLES)));
        addCos.setOnAction(evt -> errorDataSetRenderer.getDatasets().add(new CosineFunction("cos", N_SAMPLES)));
        addRandom.setOnAction(
                evt -> errorDataSetRenderer.getDatasets().add(new RandomWalkFunction("Random", N_SAMPLES)));
        clearChart.setOnAction(evt -> errorDataSetRenderer.getDatasets().clear());
        mExit.setOnAction(evt -> Platform.exit());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
