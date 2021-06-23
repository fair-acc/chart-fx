package de.gsi.chart.ui;

import java.util.Objects;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Simple Pane with line border and title on the top-left corner
 *
 * <p>
 * N.B. this is a limited function replacement for ControlsFX's Borders class, i.e.
 * {@code Node node = Borders.wrap(content).lineBorder().title(getName()).build().build(); }
 * that when used in larger layouts causes infinite JavaFX layout loops.
 *
 * @author rstein
 */
public class BorderedTitledPane extends StackPane {
    private final Label title;
    private final StackPane contentPane;

    public BorderedTitledPane(final String titleString, final Node content) {
        getStyleClass().add("bordered-titled-border");
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }

        title = new Label(titleString);
        title.getStyleClass().add("bordered-titled-title");
        StackPane.setAlignment(title, Pos.TOP_LEFT);

        contentPane = new StackPane();
        content.getStyleClass().add("bordered-titled-content");
        contentPane.getChildren().add(content);

        getChildren().addAll(contentPane, title);
    }

    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(getClass().getResource("titled-border.css")).toExternalForm();
    }

    public StackPane getContentPane() {
        return contentPane;
    }

    public Label getTitle() {
        return title;
    }
}
