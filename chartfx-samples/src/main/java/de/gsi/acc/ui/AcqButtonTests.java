package de.gsi.acc.ui;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import de.gsi.chart.viewer.SquareButton;
import de.gsi.ui.icons.AcquisitionButtonBar;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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
        root.getStylesheets().add(getClass().getResource("acq_button_small.css").toExternalForm());

        Button button1 = new Button(">>");
        Button button2 = new SquareButton(">>", null);
        Button button3 = new SquareButton(null,
                new Glyph(FONT_AWESOME, FontAwesome.Glyph.FAST_FORWARD).size(FONT_SIZE));
        Button button4 = new SquareButton(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.PLAY).size(FONT_SIZE));
        button4.setPadding(new Insets(6, 8, 8, 10));
        button4.setAlignment(Pos.CENTER);
        button4.setShape(new Circle(1.5));

        CheckBox cbDisabled = new CheckBox();

        AcquisitionButtonBar acqBar = new AcquisitionButtonBar();

        cbDisabled.setOnAction(evt -> {
            acqBar.setDisable(cbDisabled.isSelected());
        });

        CheckBox cb1 = new CheckBox("Property 'PlayStop' selected");
        cb1.selectedProperty().bindBidirectional(acqBar.playStopStateProperty());
        CheckBox cb2 = new CheckBox("Property 'Play' selected");
        cb2.selectedProperty().bindBidirectional(acqBar.playStateProperty());
        CheckBox cb3 = new CheckBox("Property 'Stop' selected");
        cb3.selectedProperty().bindBidirectional(acqBar.playStopStateProperty());

        root.getChildren().addAll(new HBox(button1, button2, button3, button4, cbDisabled), acqBar,
                new VBox(cb1, cb2, cb3));

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
