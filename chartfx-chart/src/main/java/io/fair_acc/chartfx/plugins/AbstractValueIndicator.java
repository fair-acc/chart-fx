/*
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package io.fair_acc.chartfx.plugins;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.EventSource;
import io.fair_acc.dataset.events.StateListener;

/**
 * Base class for plugins indicating a specific value or range of values on a {@link Chart} with an optional
 * {@link #textProperty() text label} description.
 *
 * @author mhrabia
 */
public abstract class AbstractValueIndicator extends ChartPlugin implements EventSource {
    private final Axis axis;
    private final StateListener axisBoundsListener = (source, bits) -> runPostLayout();

    private final BitState state = BitState.initDirty(this);

    private final ListChangeListener<? super ChartPlugin> pluginsListListener = (final Change<? extends ChartPlugin> change) -> updateStyleClass();

    private double xOffset;
    private double yOffset;

    protected final Label label = new Label();
    protected final TextField labelEdit = new TextField();
    /* Difference between the mouse press position and the indicators center */
    protected final Delta dragDelta = new Delta();

    protected final BooleanProperty editableIndicator = new SimpleBooleanProperty(this, "editableIndicator", true) {
        @Override
        protected void invalidated() {
            runPostLayout();
        }
    };

    private final ObjectProperty<HPos> labelHorizontalAnchor = new SimpleObjectProperty<>(this,
            "labelHorizontalAnchor", HPos.CENTER) {
        @Override
        protected void invalidated() {
            runPostLayout();
        }
    };

    private final ObjectProperty<VPos> labelVerticalAnchor = new SimpleObjectProperty<>(this, "labelVerticalAnchor",
            VPos.CENTER) {
        @Override
        protected void invalidated() {
            runPostLayout();
        }
    };

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     */
    protected AbstractValueIndicator(Axis axis, final String text) {
        super();
        this.axis = axis;
        setText(text);

        label.pickOnBoundsProperty().set(true);
        label.toFront();

        label.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isPrimaryButtonDown() && isEditable()) {
                /*
                 * Record a delta distance for the drag and drop operation. PROBLEM: At this point, we need to know the
                 * relative position of the label with respect to the indicator value.
                 */
                Point2D c = label.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
                dragDelta.x = -(c.getX() + xOffset);
                dragDelta.y = c.getY() + yOffset;
                label.setCursor(Cursor.MOVE);
                mouseEvent.consume();
            }
        });
        // mouse handler to edit the indicator on right click
        label.setOnMouseClicked(evt -> {
            if (evt.getButton().equals(MouseButton.SECONDARY) && isEditable()) {
                label.setVisible(false);
                getChartChildren().add(labelEdit);
                labelEdit.requestFocus();
                labelEdit.setLayoutX(label.getLayoutX());
                labelEdit.setLayoutY(label.getLayoutY());
                labelEdit.resizeRelocate(label.getLayoutX() - 20, label.getLayoutY() - 5, label.getWidth() + 40, label.getHeight() + 10);
                // change the label on exit
                labelEdit.setOnAction(actionEvt -> {
                    label.setText(labelEdit.getText());
                    getChartChildren().remove(labelEdit);
                    label.setVisible(true);
                });
                labelEdit.setOnKeyPressed(keyEvt -> {
                    // remove the indicator when pressing ctl + delete
                    if (keyEvt.getCode().equals(KeyCode.DELETE) && keyEvt.isControlDown()) {
                        getChart().getPlugins().remove(this);
                    }
                    // restore the old label when pressing esc
                    if (keyEvt.getCode().equals(KeyCode.ESCAPE)) {
                        getChartChildren().remove(labelEdit);
                        label.setVisible(true);
                    }
                });
            }
        });

        editableIndicatorProperty().addListener((ch, o, n) -> updateMouseListener(n));
        updateMouseListener(isEditable());

        chartProperty().addListener((obs, oldChart, newChart) -> {
            if (oldChart != null) {
                removeAxisListener();
                removePluginsListListener(oldChart);
            }
            if (newChart != null) {
                addAxisListener();
                addPluginsListListener(newChart);
            }
        });

        textProperty().addListener((obs, oldText, newText) -> runPostLayout());
    }

    private void addAxisListener() {
        final Axis valueAxis = getAxis();
        valueAxis.getBitState().addChangeListener(ChartBits.AxisRange, axisBoundsListener);
    }

    protected void addChildNodeIfNotPresent(final Node node) {
        if (!getChartChildren().contains(node)) {
            getChartChildren().add(0, node); // add elements always at the bottom so they cannot steal focus
            node.setManaged(false);
        }
    }

    private void addPluginsListListener(final Chart chart) {
        chart.getPlugins().addListener(pluginsListListener);
        updateStyleClass();
    }

    /**
     * whether this plugin is editable
     *
     * @return boolean property
     */
    public final BooleanProperty editableIndicatorProperty() {
        return editableIndicator;
    }

    private int getIndicatorInstanceIndex() {
        if (getChart() == null) {
            return 0;
        }
        final Class<?> thisClass = getClass();
        int instanceIndex = -1;
        for (final ChartPlugin plugin : getChart().getPlugins()) {
            if (plugin.getClass().equals(thisClass)) {
                instanceIndex++;
            }
            if (plugin == this) {
                break;
            }
        }
        return Math.max(instanceIndex, 0);
    }

    /**
     * Returns the value of the {@link #labelHorizontalAnchorProperty()}.
     *
     * @return value of the labelHorizontalAnchor property
     */
    public final HPos getLabelHorizontalAnchor() {
        return labelHorizontalAnchorProperty().get();
    }

    /**
     * Returns the value of the {@link #labelVerticalAnchorProperty()}.
     *
     * @return value of the labelVerticalAnchor property
     */
    public final VPos getLabelVerticalAnchor() {
        return labelVerticalAnchorProperty().get();
    }

    /**
     * Returns the Axis that this indicator is associated with.
     *
     * @return associated Axis
     */
    public Axis getAxis() {
        return axis;
    }

    /**
     * Returns the value of the {@link #textProperty()}.
     *
     * @return text displayed within or next to the indicator
     */
    public final String getText() {
        return textProperty().get();
    }

    /**
     * @return whether this plugin is editable
     */
    public final boolean isEditable() {
        return editableIndicatorProperty().get();
    }

    /**
     * Specifies anchor of the {@link #textProperty() text label} with respect to the horizontal label position i.e. it
     * describes whether the position is related to the LEFT, CENTER or RIGHT side of the label. The position itself
     * should be specified by the extending classes.
     * <p>
     * <b>Default value: {@link HPos#CENTER}</b>
     * </p>
     *
     * @return labelHorizontalAnchor property
     */
    public final ObjectProperty<HPos> labelHorizontalAnchorProperty() {
        return labelHorizontalAnchor;
    }

    /**
     * Specifies anchor of the {@link #textProperty() text label} with respect to the vertical label position i.e. it
     * describes whether the position is related to the TOP, CENTER, BASELINE or BOTTOM of of the label. The position
     * itself should be specified by the extending classes.
     * <p>
     * <b>Default value: {@link VPos#CENTER}</b>
     * </p>
     *
     * @return labelVerticalAnchor property
     */
    public final ObjectProperty<VPos> labelVerticalAnchorProperty() {
        return labelVerticalAnchor;
    }

    /**
     * Layouts the label within specified bounds and given horizontal and vertical position, taking into account
     * {@link #labelHorizontalAnchorProperty() horizontal} and {@link #labelVerticalAnchorProperty() vertical} anchor.
     *
     * @param bounds the bounding rectangle with respect to which the label should be positioned
     * @param hPos relative [0, 1] horizontal position of the label within the bounds
     * @param vPos relative [0, 1] vertical position of the label within the bounds
     */
    protected final void layoutLabel(final Bounds bounds, final double hPos, final double vPos) {
        if (label.getText() == null || label.getText().isEmpty()) {
            getChartChildren().remove(label);
            return;
        }

        double xPos = bounds.getMinX();
        double yPos = bounds.getMinY();

        xOffset = bounds.getWidth() * hPos;
        yOffset = bounds.getHeight() * (1 - vPos);

        final double width = label.prefWidth(-1);
        final double height = label.prefHeight(width);

        if (getLabelHorizontalAnchor() == HPos.CENTER) {
            xOffset -= width / 2;
        } else if (getLabelHorizontalAnchor() == HPos.RIGHT) {
            xOffset -= width;
        }

        if (getLabelVerticalAnchor() == VPos.CENTER) {
            yOffset -= height / 2;
        } else if (getLabelVerticalAnchor() == VPos.BASELINE) {
            yOffset -= label.getBaselineOffset();
        } else if (getLabelVerticalAnchor() == VPos.BOTTOM) {
            yOffset -= height;
        }

        label.resizeRelocate(xPos + xOffset, yPos + yOffset, width, height);
        addChildNodeIfNotPresent(label);
    }

    private void removeAxisListener() {
        final Axis valueAxis = getAxis();
        valueAxis.getBitState().removeChangeListener(axisBoundsListener);
    }

    private void removePluginsListListener(final Chart chart) {
        chart.getPlugins().removeListener(pluginsListListener);
    }

    /**
     * Sets the state whether this plugin is editable
     *
     * @param newState true: edits are allowed
     */
    public final void setEditable(final boolean newState) {
        editableIndicatorProperty().set(newState);
    }

    /**
     * Sets the value of the {@link #labelHorizontalAnchorProperty()}.
     *
     * @param anchor new anchor
     */
    public final void setLabelHorizontalAnchor(final HPos anchor) {
        labelHorizontalAnchorProperty().set(anchor);
    }

    /**
     * Sets the value of the {@link #labelVerticalAnchorProperty()}.
     *
     * @param anchor new anchor
     */
    public final void setLabelVerticalAnchor(final VPos anchor) {
        labelVerticalAnchorProperty().set(anchor);
    }

    public void setStyleClasses(final Node node, final String prefix, final String root) {
        node.getStyleClass().setAll(root, prefix + root, prefix + root + getIndicatorInstanceIndex());
    }

    /**
     * Sets the value of the {@link #textProperty()}.
     *
     * @param text the new text. If {@code null}, the label will be hidden.
     */
    public final void setText(final String text) {
        textProperty().set(text);
    }

    /**
     * Text to be displayed by the label. If set to {@code null}, the label is not shown.
     *
     * @return text of the indicator's label
     */
    public final StringProperty textProperty() {
        return label.textProperty();
    }

    private void updateMouseListener(final boolean state) {
        if (state) {
            label.setOnMouseReleased(mouseEvent -> label.setCursor(Cursor.HAND));
            label.setOnMouseEntered(mouseEvent -> label.setCursor(Cursor.HAND));
        } else {
            label.setOnMouseReleased(null);
            label.setOnMouseEntered(null);
        }
    }

    /**
     * There might be several instances of a given indicator class. If one wants to specify different CSS for each
     * instance - we need a unique class name for each, so whenever the list of plugins changes, this method should
     * update name of it's CSS class.
     */
    public abstract void updateStyleClass();

    // records relative x and y co-ordinates.
    protected static class Delta {
        protected double x;
        protected double y;
    }

    @Override
    public BitState getBitState() {
        return state;
    }
}
