package de.gsi.chart.samples;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import de.gsi.chart.XYChart;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.testdata.spi.CosineFunction;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;

/**
 * Example on how to use chart-fx from fxml.
 * 
 * @author Alexander Krimm
 */
public class FxmlSample extends Application implements Initializable {
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
    public void start(Stage stage) {
        Parent root;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FxmlSample.fxml"));
        loader.setController(this);
        try {
            root = loader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        Scene scene = new Scene(root, 300, 275);
        stage.setTitle("FXML Welcome");
        stage.setScene(scene);
        stage.show();
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
