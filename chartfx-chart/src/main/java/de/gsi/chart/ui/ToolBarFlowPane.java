package de.gsi.chart.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import de.gsi.chart.Chart;

/**
 * tool bar for plugins to add their controls (if necessary)
 *
 * @author rstein
 */
public class ToolBarFlowPane extends FlowPane {
    private static final int DEFAULT_TOOLBAR_HEIGHT = 28;
    private static final double CORNER_SPACE_PADDING_FACTOR = 1.8;
    private Color defaultColour = Color.web("#f4f4f4", 0.85).deriveColor(0, 1.0, .94, 1.0);
    private Color selectedColour = Color.web("#f4f4f4", 0.85).deriveColor(0, 1.0, .92, 1.0);
    private Insets toolBarPadding = new Insets(1, 12.5, 5, 12.5);
    private final DoubleProperty cornerRadius = new SimpleDoubleProperty(this, "cornerRadius", 25.0);
    private final Chart chart;

    /**
     * @param chart the associated chart pane
     */
    public ToolBarFlowPane(final Chart chart) {
        super();
        this.chart = chart;

        this.setId(this.getClass().getSimpleName() + "(Chart)"); // N.B. not a unique name but for testing this suffices
        StackPane.setAlignment(this, Pos.TOP_CENTER);
        this.setPrefHeight(USE_COMPUTED_SIZE);
        this.setBackground(new Background(new BackgroundFill(defaultColour, CornerRadii.EMPTY, Insets.EMPTY)));
        this.setMinHeight(0);

        this.setShape(ToolBarShapeHelper.getToolBarShape(this.getWidth(), this.getHeight(), cornerRadius.get()));


        this.setAlignment(Pos.TOP_CENTER);
        this.setMinWidth(0);
        setPadding(calculateInsets()); // NOPMD
        HBox.setHgrow(this, Priority.NEVER);

        ChangeListener<Number> toolBarSizeListener = (ch, o, n) -> {
            if (n.equals(o)) {
                return;
            }
            adjustToolBarWidth();
        };

        this.widthProperty().addListener(toolBarSizeListener);
        this.heightProperty().addListener(toolBarSizeListener);
        chart.getCanvas().widthProperty().addListener(toolBarSizeListener);
        cornerRadius.addListener(toolBarSizeListener);
    }

    private void adjustToolBarWidth() {
        final double maxLength = 0.60 * chart.getWidth();
        double length = 0.0;
        for (Node node : this.getChildren()) {
            length += node.prefWidth(DEFAULT_TOOLBAR_HEIGHT);
        }
        length += 4 * cornerRadius.get();
        final double wrapLength = Math.min(maxLength, Math.max(length, 50));
        this.setPrefWrapLength(wrapLength);
        this.setMaxWidth(wrapLength);
        this.setWidth(maxLength);
        final int height = (int)Math.max(getHeight(), Math.max(getBoundsInParent().getHeight(), getBoundsInLocal().getHeight()));
        this.setMinHeight(height);

        this.setShape(ToolBarShapeHelper.getToolBarShape(wrapLength, height, cornerRadius.get()));
    }

    /**
     * Calculate the tool bar insets. It is called on tool bar width or it's contents changes and on corner radius
     * changes.
     *
     * @return Insets for the tool bar pane
     */
    protected Insets calculateInsets() {
        final double roundCornerSpace = CORNER_SPACE_PADDING_FACTOR * cornerRadius.get();
        final double sidePaddingRight = Math.max(roundCornerSpace, toolBarPadding.getRight());
        final double sidePaddingLeft = Math.max(roundCornerSpace, toolBarPadding.getLeft());
        return new Insets(toolBarPadding.getTop(), sidePaddingRight, toolBarPadding.getBottom(), sidePaddingLeft);
    }

    public DoubleProperty cornerRadiusProperty() {
        return cornerRadius;
    }

    public Color getToolBarDefaultColor() {
        return this.defaultColour;
    }

    public Insets getToolBarInsets() {
        return toolBarPadding;
    }

    public Color getToolBarSelectedColor() {
        return this.selectedColour;
    }

    public void registerListener() {
        this.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> adjustToolBarWidth());

        setOnMouseClicked(mevt -> {
            if (chart.toolBarPinnedProperty().isBound()) {
                return;
            }
            chart.setToolBarPinned(!chart.isToolBarPinned());
        });
        chart.toolBarPinnedProperty().addListener((obj, valOld, valNew) -> {
            if (valNew) {
                chart.setPinnedSide(javafx.geometry.Side.TOP);
                this.setBackground(new Background(new BackgroundFill(selectedColour, CornerRadii.EMPTY, Insets.EMPTY)));
            } else {
                chart.setPinnedSide(null);
                this.setBackground(new Background(new BackgroundFill(defaultColour, CornerRadii.EMPTY, Insets.EMPTY)));
            }
            chart.requestLayout();
        });
    }

    public void setToolBarDefaultColor(final Color color) {
        this.defaultColour = color;
    }

    public void setToolBarInsets(final Insets padding) {
        toolBarPadding = padding;
        this.setPadding(toolBarPadding);
    }

    public void setToolBarSelectedColor(final Color color) {
        this.selectedColour = color;
    }
}
