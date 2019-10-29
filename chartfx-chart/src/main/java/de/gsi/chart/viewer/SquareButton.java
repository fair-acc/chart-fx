package de.gsi.chart.viewer;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

public class SquareButton extends Button {
    private static final int MAX_BUTTON_SIZE = 30;

    SquareButton(final String cssName) {
        super();
        getStyleClass().setAll(cssName);
        setPadding(Insets.EMPTY);
        updateListener();
    }

    SquareButton(final String text, Node graphic) {
        super(text, graphic);
        setPadding(Insets.EMPTY);
        updateListener();
    }

    private void adjustDimension() {
        if (!(this.getParent() instanceof Region)) {
            return;
        }
        final Region titlePane = (Region) this.getParent();
        final double marginBar = titlePane.getInsets().getTop() + titlePane.getInsets().getBottom();
        final double paddingButton = this.getPadding().getTop() + this.getPadding().getBottom();
        final double max = titlePane.getHeight() - marginBar - paddingButton;
        this.setPrefSize(max, max);
        this.setMaxSize(MAX_BUTTON_SIZE, MAX_BUTTON_SIZE);
    }

    private void updateListener() {
        ChangeListener<? super Number> listener = (ch, o, n) -> adjustDimension();
        this.widthProperty().addListener(listener);
        this.heightProperty().addListener(listener);

        this.parentProperty().addListener((ch, o, n) -> {
            if (o != null) {
                if (!(o instanceof Region)) {
                    return;
                }
                final Region titlePane = (Region) o;
                titlePane.heightProperty().removeListener(listener);
            }

            if (n != null) {
                if (!(n instanceof Region)) {
                    return;
                }
                final Region titlePane = (Region) n;
                titlePane.heightProperty().addListener(listener);
                adjustDimension();
            }
        });
        adjustDimension();
    }

}
