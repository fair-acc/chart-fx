package de.gsi.acc.ui.samples;

import de.gsi.ui.icons.AcquisitionButtonBar;
import de.gsi.ui.icons.AcquisitionButtonBar.ButtonStyle;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AcqButtonTests extends Application {
    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 28;
    BooleanProperty bStatePause = new SimpleBooleanProperty(this, "bStatePause", false);
    BooleanProperty bStatePlayStop = new SimpleBooleanProperty(this, "bStatePlayStop", false);
    BooleanProperty bStatePlay = new SimpleBooleanProperty(this, "bStatePlay", false);
    BooleanProperty bStateStop = new SimpleBooleanProperty(this, "bStateStop", false);

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox root = new VBox();
        root.setPrefSize(640, 480);
        root.getStylesheets().add(AcquisitionButtonBar.class.getResource("acq_button_small.css").toExternalForm());

        CheckBox cbDisabled = new CheckBox("disable acquisition buttons");

        AcquisitionButtonBar acqBar = new AcquisitionButtonBar(ButtonStyle.FEEDBACK);

        cbDisabled.setOnAction(evt -> {
            acqBar.setDisable(cbDisabled.isSelected());
        });

        CheckBox cb1 = new CheckBox("Property 'PlayStop' selected");
        cb1.selectedProperty().bindBidirectional(acqBar.playStopStateProperty());
        CheckBox cb2 = new CheckBox("Property 'Play' selected");
        cb2.selectedProperty().bindBidirectional(acqBar.playStateProperty());
        CheckBox cb3 = new CheckBox("Property 'Stop' selected");
        cb3.selectedProperty().bindBidirectional(acqBar.stopStateProperty());

        root.getChildren().addAll(new HBox(cbDisabled), acqBar, new VBox(cb1, cb2, cb3));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle(AcqButtonTests.class.getSimpleName());
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> {
//            System.exit(0);
        });
    }

    protected static ImageView getIcon(final String iconName) {
        return new ImageView(new Image(AcqButtonTests.class.getResourceAsStream("./icons/" + iconName)));
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
