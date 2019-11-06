package de.gsi.ui.icons;

import de.gsi.chart.viewer.SquareButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class AcquisitionButtonBar extends HBox {
    private static PseudoClass PSEUDO_CLASS_ACTIVATED = PseudoClass.getPseudoClass("activated");
    private final Button buttonPause = new SquareButton("my-pause-button");
    private final Button buttonPlayStop = new SquareButton("my-playstop-button");
    private final Button buttonPlay = new SquareButton("my-play-button");
    private final Button buttonStop = new SquareButton("my-stop-button");
    private final BooleanProperty pauseState = new SimpleBooleanProperty(this, "buttonPauseState", false);
    private final BooleanProperty playStopState = new SimpleBooleanProperty(this, "buttonPlayStopState", false);
    private final BooleanProperty playState = new SimpleBooleanProperty(this, "buttonPlayState", false);
    private final BooleanProperty stopState = new SimpleBooleanProperty(this, "buttonStopState", true);

    public AcquisitionButtonBar(final ButtonStyle setupButtonsAs) {
        super();
        this.getStylesheets().add(getClass().getResource("acq_button_small.css").toExternalForm());

        disabledProperty().addListener((ch, o, n) -> {
            buttonPause.setDisable(n);
            buttonPlayStop.setDisable(n);
            buttonPlay.setDisable(n);
            buttonStop.setDisable(n);
        });

        buttonPause.disableProperty().addListener((ch, o, n) -> {
            if (!n) {
                pauseState.set(false);
            }
        });
        buttonPlayStop.disableProperty().addListener((ch, o, n) -> {
            if (!n) {
                playStopState.set(false);
            }
        });
        buttonPlay.disableProperty().addListener((ch, o, n) -> {
            if (!n) {
                playState.set(false);
            }
        });
        buttonStop.disableProperty().addListener((ch, o, n) -> {
            if (!n) {
                stopState.set(false);
            }
        });

        buttonPause.setOnAction(evt -> {
            pauseState.set(!pauseState.get());
            buttonPause.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, pauseState.get());
        });

        buttonPlayStop.setOnAction(evt -> playStopState.set(!playStopState.get()));
        playStopState.addListener((ch, o, n) -> {
            buttonPause.setDisable(n);
            buttonPlay.setDisable(n);
            stopState.set(!n);

            buttonStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
            buttonPlayStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
        });

        buttonPlay.setOnAction(evt -> playState.set(!playState.get()));
        playState.addListener((ch, o, n) -> {
            buttonPause.setDisable(n);
            buttonPlayStop.setDisable(n);
            stopState.set(!n);

            buttonStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
            buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, n);
        });

        buttonStop.setOnAction(evt -> stopState.set(!stopState.get()));
        stopState.addListener((ch, o, n) -> {
            if (n) {
                pauseState.set(false);
                playStopState.set(false);
                playState.set(false);

                buttonPause.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);
                buttonPlayStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);
                buttonPlay.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);
                buttonStop.pseudoClassStateChanged(PSEUDO_CLASS_ACTIVATED, false);

                buttonPause.setDisable(false);
                buttonPlayStop.setDisable(false);
                buttonPlay.setDisable(false);
                buttonStop.setDisable(false);
            }
        });

        this.getChildren().addAll(buttonPlayStop, buttonPlay, buttonStop);
    }

    public Button getButtonPause() {
        return buttonPause;
    }

    public Button getButtonPlayStop() {
        return buttonPlayStop;
    }

    public Button getButtonPlay() {
        return buttonPlay;
    }

    public Button getButtonStop() {
        return buttonStop;
    }

    public BooleanProperty pauseStateProperty() {
        return pauseState;
    }

    public BooleanProperty playStopStateProperty() {
        return playStopState;
    }

    public BooleanProperty playStateProperty() {
        return playState;
    }

    public BooleanProperty stopStateProperty() {
        return stopState;
    }

    public enum ButtonStyle {
        FEEDBACK,
        DATA_ACQUISITION;
    }
}
