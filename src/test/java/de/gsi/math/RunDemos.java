package de.gsi.math;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.gsi.chart.utils.PeriodicScreenCapture;
import de.gsi.math.demo.DataSetAverageSample;
import de.gsi.math.demo.DataSetFilterSample;
import de.gsi.math.demo.DataSetIntegrateDifferentiateSample;
import de.gsi.math.demo.DataSetIntegrationWithLimitsSample;
import de.gsi.math.demo.DataSetSpectrumSample;
import de.gsi.math.demo.EMDSample;
import de.gsi.math.demo.FourierSample;
import de.gsi.math.demo.FrequencyFilterSample;
import de.gsi.math.demo.GaussianFitSample;
import de.gsi.math.demo.IIRFilterSample;
import de.gsi.math.demo.WaveletDenoising;
import de.gsi.math.demo.WaveletScalogram;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class RunDemos extends Application {

    private static final int DEFAULT_DELAY = 2;
    private static final int DEFAULT_PERIOD = 5;
    final String userHome = System.getProperty("user.home");
    final Path path = Paths.get(userHome + "/ChartMathSamples");

    Stage stage = new Stage();
    CheckBox makeScreenShot = new CheckBox("make screenshot to home directory");

    @Override
    public void start(final Stage primaryStage) {
        final BorderPane root = new BorderPane();

        final FlowPane buttons = new FlowPane();
        buttons.setAlignment(Pos.CENTER_LEFT);
        root.setCenter(buttons);
        root.setBottom(makeScreenShot);

        buttons.getChildren().add(new MyButton("DataSetAverageSample", new DataSetAverageSample()));
        buttons.getChildren().add(new MyButton("DataSetFilterSample", new DataSetFilterSample()));
        buttons.getChildren()
                .add(new MyButton("DataSetIntegrateDifferentiateSample", new DataSetIntegrateDifferentiateSample()));
        buttons.getChildren()
                .add(new MyButton("DataSetIntegrationWithLimitsSample", new DataSetIntegrationWithLimitsSample()));
        buttons.getChildren().add(new MyButton("DataSetSpectrumSample", new DataSetSpectrumSample()));
        buttons.getChildren().add(new MyButton("EMDSample", new EMDSample()));
        buttons.getChildren().add(new MyButton("FourierSample", new FourierSample()));
        buttons.getChildren().add(new MyButton("FrequencyFilterSample", new FrequencyFilterSample()));
        buttons.getChildren().add(new MyButton("GaussianFitSample", new GaussianFitSample()));
        buttons.getChildren().add(new MyButton("IIRFilterSample", new IIRFilterSample()));
        buttons.getChildren().add(new MyButton("WaveletDenoising", new WaveletDenoising()));
        buttons.getChildren().add(new MyButton("WaveletScalogram", new WaveletScalogram()));

        final Scene scene = new Scene(root);

        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
    }

    protected class MyButton extends Button {

        public MyButton(final String buttonText, final Application run) {
            super(buttonText);
            this.setOnAction(e -> {
                try {
                    run.start(stage);
                    stage.getScene().getRoot().layout();
                    stage.show();

                    if (makeScreenShot.isSelected()) {
                        new Thread() {

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(2000);
                                    Platform.runLater(() -> {
                                        System.err.println(
                                                "make screen shot to file of " + run.getClass().getSimpleName());
                                        final PeriodicScreenCapture screenCapture = new PeriodicScreenCapture(path,
                                                run.getClass().getSimpleName(), stage.getScene(), DEFAULT_DELAY,
                                                DEFAULT_PERIOD, false);
                                        screenCapture.performScreenCapture();
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();

                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            });
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);

    }
}
