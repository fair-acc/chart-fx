package de.gsi.acc.ui.samples;

import de.gsi.acc.ui.AcquisitionButtonBar;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AcqButtonTests extends Application {

    public Pane getAcquisitionBarTest(final boolean isPauseEnabled) {
        final VBox root = new VBox();

        CheckBox cbDisabled = new CheckBox("disable acquisition buttons");

        AcquisitionButtonBar acqBar = new AcquisitionButtonBar(isPauseEnabled);

        cbDisabled.setOnAction(evt -> acqBar.setDisable(cbDisabled.isSelected()));

        CheckBox cb1 = new CheckBox("Property 'PlayStop' selected");
        cb1.selectedProperty().bindBidirectional(acqBar.playStopStateProperty());
        CheckBox cb2 = new CheckBox("Property 'Play' selected");
        cb2.selectedProperty().bindBidirectional(acqBar.playStateProperty());
        CheckBox cb3 = new CheckBox("Property 'Pause' selected");
        cb3.selectedProperty().bindBidirectional(acqBar.pauseStateProperty());
        CheckBox cb4 = new CheckBox("Property 'Stop' selected");
        cb4.selectedProperty().bindBidirectional(acqBar.stopStateProperty());

        root.getChildren()
                .addAll(new Label("AcquisitionButtonBar "
                        + (isPauseEnabled ? "with" : "without") + " pause button functionality "), new HBox(cbDisabled),
                        acqBar, new VBox(new Label("AcquisitionButtonBar state property values:"), cb1, cb2, cb3, cb4));

        return root;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        HBox root = new HBox();
        root.setPrefSize(640, 480);
        root.getStylesheets().add(AcquisitionButtonBar.class.getResource("acq_button_small.css").toExternalForm());

        root.getChildren().addAll(getAcquisitionBarTest(true), getAcquisitionBarTest(false));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle(AcqButtonTests.class.getSimpleName());
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    protected static ImageView getIcon(final String iconName) {
        return new ImageView(new Image(AcqButtonTests.class.getResourceAsStream("./icons/" + iconName)));
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
