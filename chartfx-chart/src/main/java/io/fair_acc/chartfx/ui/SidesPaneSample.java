package io.fair_acc.chartfx.ui;

import io.fair_acc.chartfx.ui.geometry.Side;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class SidesPaneSample extends Application {
    protected static final int SHUTDOWN_PERIOD = 5000; // [ms]
    protected static final int UPDATE_PERIOD = 100; // [ms]

    @Override
    public void start(final Stage stage) {
        stage.setTitle("TitledPane");

        final Label label = new Label("top content\ntop content");
        final Pane topContent = new Pane(label);
        topContent.setStyle("-fx-background-color: rgba(0,255,0,0.2)");

        final Pane leftContent = new Pane(new Label("left content"));
        leftContent.setStyle("-fx-background-color: rgba(255,0,0,0.2)");

        final Button button1 = new Button("press me to shrink");
        button1.setOnAction(evt -> topContent.setPrefHeight(50));
        final Button button2 = new Button("press me to enlarge");
        button2.setOnAction(evt -> topContent.setPrefHeight(100));

        final Pane mainContent = new Pane(new HBox(new Label("main content"), button1, button2));

        final SidesPane pane = new SidesPane();
        pane.setTriggerDistance(50);
        final Scene scene = new Scene(pane, 800, 600);
        pane.setTop(topContent);
        pane.setLeft(leftContent);
        pane.setContent(mainContent);

        topContent.setOnMouseClicked(mevt -> {
            final boolean isPinned = !pane.isPinned(Side.TOP);
            pane.setPinned(Side.TOP, isPinned);
            label.textProperty().set(String.format("top content(%b)%ntop content(%b)", isPinned, isPinned));
        });

        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}