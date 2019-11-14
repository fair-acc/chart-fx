package de.gsi.acc.ui;

import java.util.List;

import de.gsi.chart.viewer.SquareButton;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class AcquisitionButtonBar extends HBox {
    private static String DEFAULT_CSS = AcquisitionButtonBar.class.getResource("acq_button_medium.css")
            .toExternalForm();
    private static PseudoClass PSEUDO_CLASS_ACTIVATED = PseudoClass.getPseudoClass("activated");
    private static PseudoClass PSEUDO_CLASS_PAUSE = PseudoClass.getPseudoClass("paused");
    private final Button buttonPlayStop = new SquareButton("my-playstop-button");
    private final Button buttonPlay = new SquareButton("my-play-button");
    private final Button buttonStop = new SquareButton("my-stop-button");
    private final BooleanProperty pauseState = new SimpleBooleanProperty(this, "buttonPauseState", false);
    private final BooleanProperty playStopState = new SimpleBooleanProperty(this, "buttonPlayStopState", false);
    private final BooleanProperty playState = new SimpleBooleanProperty(this, "buttonPlayState", false);
    private final BooleanProperty stopState = new SimpleBooleanProperty(this, "buttonStopState", true);

    public AcquisitionButtonBar(@NamedArg(value = "isPauseEnabled") boolean isPauseEnabled) {
        super();
        if (this.getStylesheets().isEmpty()) {
            this.getStylesheets().add(getClass().getResource("acq_button_small.css").toExternalForm());
        }

        disabledProperty().addListener((ch, o, n) -> {
            buttonPlayStop.setDisable(n);
            buttonPlay.setDisable(n);
            buttonStop.setDisable(n);
        });

        buttonPlayStop.disableProperty().addListener((ch, o, n) -> {
            if (n) {
                playStopState.set(false);
            }
        });
        buttonPlay.disableProperty().addListener((ch, o, n) -> {
            if (n) {
                playState.set(false);
            }
        });
        buttonStop.disableProperty().addListener((ch, o, n) -> {
            stopState.set(!n);
        });

        pauseState.addListener((ch, o, n) -> {
            if (isPauseEnabled) {
                if (n) {
                    playState.set(true);
                }
                buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_PAUSE, isPauseEnabled ? n : playState.get());
            } else {
                pauseState.set(false);
            }
        });

        buttonPlayStop.setOnAction(evt -> playStopState.set(!playStopState.get()));
        playStopState.addListener((ch, o, n) -> {
            buttonPlay.setDisable(n);
            stopState.set(!n);

            buttonStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
            buttonPlayStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
            if (n) {
                buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);
                buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_PAUSE, false);
            }
        });

        buttonPlay.setOnAction(evt -> {
            if (isPauseEnabled) {
                if (!playState.get()) {
                    playState.set(true);
                } else {
                    pauseState.set(!pauseState.get());
                }
            } else {
                playState.set(!playState.get());
                pauseState.set(false);
            }
        });

        playState.addListener((ch, o, n) -> {
            buttonPlayStop.setDisable(n);
            stopState.set(!n);

            buttonStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
            buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
            buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_PAUSE, isPauseEnabled ? pauseState.get() : true);
        });

        buttonStop.setOnAction(evt -> stopState.set(!stopState.get()));
        stopState.addListener((ch, o, n) -> {
            if (n) {
                pauseState.set(false);
                playStopState.set(false);
                playState.set(false);

                buttonPlayStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);
                buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);

                buttonPlayStop.setDisable(false);
                buttonPlay.setDisable(false);
            }
            buttonStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, !n);
        });

        this.getChildren().addAll(buttonPlayStop, buttonPlay, buttonStop);
    }

    public Button getButtonPlay() {
        return buttonPlay;
    }

    public Button getButtonPlayStop() {
        return buttonPlayStop;
    }

    public Button getButtonStop() {
        return buttonStop;
    }

    public BooleanProperty pauseStateProperty() {
        return pauseState;
    }

    public BooleanProperty playStateProperty() {
        return playState;
    }

    public BooleanProperty playStopStateProperty() {
        return playStopState;
    }

    public BooleanProperty stopStateProperty() {
        return stopState;
    }
}
