/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Base class for plugins indicating a specific value or range of values on a
 * {@link Chart} with an optional {@link #textProperty() text label}
 * description.
 *
 * @author mhrabia
 */
public abstract class AbstractValueIndicator extends ChartPlugin {
    private final Axis axis;
    private final ChangeListener<? super Number> axisBoundsListener = (obs, oldVal, newVal) -> layoutChildren();

    private final ListChangeListener<? super ChartPlugin> pluginsListListener = (
            final Change<? extends ChartPlugin> change) -> updateStyleClass();

    private double xOffset;
    private double yOffset;

    protected final Label label = new Label();
    /* Difference between the mouse press position and the indicators center */
    protected final Delta dragDelta = new Delta();

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis
     *            the axis this indicator is associated with
     * @param text
     *            the text to be shown by the label. Value of
     *            {@link #textProperty()}.
     */
    protected AbstractValueIndicator(Axis axis, final String text) {
        super();
        this.axis = axis;
        setText(text);

        label.mouseTransparentProperty().bind(editableIndicatorProperty().not());
        label.pickOnBoundsProperty().set(true);

        label.setOnMousePressed(mouseEvent -> {
            /*
             * Record a delta distance for the drag and drop operation. PROBLEM:
             * At this point, we need to know the relative position of the label
             * with respect to the indicator value.
             */
            Point2D c = label.sceneToLocal(mouseEvent.getSceneX(), mouseEvent.getSceneY());
            dragDelta.x = -(c.getX() + xOffset);
            dragDelta.y = c.getY() + yOffset;
            label.setCursor(Cursor.MOVE);
            mouseEvent.consume();
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

        textProperty().addListener((obs, oldText, newText) -> layoutChildren());
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

    protected final BooleanProperty editableIndicator = new SimpleBooleanProperty(this, "editableIndicator", true) {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    /**
     * whether this plugin is editable
     *
     * @return boolean property
     */
    public final BooleanProperty editableIndicatorProperty() {
        return editableIndicator;
    }

    /**
     * @return whether this plugin is editable
     */
    public final boolean isEditable() {
        return editableIndicatorProperty().get();
    }

    /**
     * Sets the state whether this plugin is editable
     *
     * @param newState
     *            true: edits are allowed
     */
    public final void setEditable(final boolean newState) {
        editableIndicatorProperty().set(newState);
    }

    private void addAxisListener() {
        final Axis valueAxis = getNumericAxis();
        valueAxis.minProperty().addListener(axisBoundsListener);
        valueAxis.maxProperty().addListener(axisBoundsListener);
    }

    private void removeAxisListener() {
        final Axis valueAxis = getNumericAxis();
        valueAxis.minProperty().removeListener(axisBoundsListener);
        valueAxis.maxProperty().removeListener(axisBoundsListener);
    }

    private void addPluginsListListener(final Chart chart) {
        chart.getPlugins().addListener(pluginsListListener);
        updateStyleClass();
    }

    private void removePluginsListListener(final Chart chart) {
        chart.getPlugins().removeListener(pluginsListListener);
    }

    /**
     * There might be several instances of a given indicator class. If one wants
     * to specify different CSS for each instance - we need a unique class name
     * for each, so whenever the list of plugins changes, this method should
     * update name of it's CSS class.
     */
    public abstract void updateStyleClass();

    public void setStyleClasses(final Node node, final String prefix, final String root) {
        node.getStyleClass().setAll(root, prefix + root, prefix + root + getIndicatorInstanceIndex());
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
        return instanceIndex < 0 ? 0 : instanceIndex;
    }

    /**
     * Returns the ValueAxis that this indicator is associated with.
     *
     * @return associated ValueAxis
     */
    protected Axis getNumericAxis() {
        return axis;
    }

    /**
     * Text to be displayed by the label. If set to {@code null}, the label is
     * not shown.
     *
     * @return text of the indicator's label
     */
    public final StringProperty textProperty() {
        return label.textProperty();
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
     * Sets the value of the {@link #textProperty()}.
     *
     * @param text
     *            the new text. If {@code null}, the label will be hidden.
     */
    public final void setText(final String text) {
        textProperty().set(text);
    }

    private final ObjectProperty<HPos> labelHorizontalAnchor = new SimpleObjectProperty<HPos>(this,
            "labelHorizontalAnchor", HPos.CENTER) {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    /**
     * Specifies anchor of the {@link #textProperty() text label} with respect
     * to the horizontal label position i.e. it describes whether the position
     * is related to the LEFT, CENTER or RIGHT side of the label. The position
     * itself should be specified by the extending classes.
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
     * Returns the value of the {@link #labelHorizontalAnchorProperty()}.
     *
     * @return value of the labelHorizontalAnchor property
     */
    public final HPos getLabelHorizontalAnchor() {
        return labelHorizontalAnchorProperty().get();
    }

    /**
     * Sets the value of the {@link #labelHorizontalAnchorProperty()}.
     *
     * @param anchor
     *            new anchor
     */
    public final void setLabelHorizontalAnchor(final HPos anchor) {
        labelHorizontalAnchorProperty().set(anchor);
    }

    private final ObjectProperty<VPos> labelVerticalAnchor = new SimpleObjectProperty<VPos>(this, "labelVerticalAnchor",
            VPos.CENTER) {
        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    /**
     * Specifies anchor of the {@link #textProperty() text label} with respect
     * to the vertical label position i.e. it describes whether the position is
     * related to the TOP, CENTER, BASELINE or BOTTOM of of the label. The
     * position itself should be specified by the extending classes.
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
     * Returns the value of the {@link #labelVerticalAnchorProperty()}.
     *
     * @return value of the labelVerticalAnchor property
     */
    public final VPos getLabelVerticalAnchor() {
        return labelVerticalAnchorProperty().get();
    }

    /**
     * Sets the value of the {@link #labelVerticalAnchorProperty()}.
     *
     * @param anchor
     *            new anchor
     */
    public final void setLabelVerticalAnchor(final VPos anchor) {
        labelVerticalAnchorProperty().set(anchor);
    }

    /**
     * Layouts the label within specified bounds and given horizontal and
     * vertical position, taking into account
     * {@link #labelHorizontalAnchorProperty() horizontal} and
     * {@link #labelVerticalAnchorProperty() vertical} anchor.
     *
     * @param bounds
     *            the bounding rectangle with respect to which the label should
     *            be positioned
     * @param hPos
     *            relative [0, 1] horizontal position of the label within the
     *            bounds
     * @param vPos
     *            relative [0, 1] vertical position of the label within the
     *            bounds
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

    protected void addChildNodeIfNotPresent(final Node node) {
        if (!getChartChildren().contains(node)) {
            getChartChildren().add(node);
        }
    }

    // records relative x and y co-ordinates.
    protected class Delta {
        protected double x;
        protected double y;
    }
}
