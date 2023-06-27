package io.fair_acc.chartfx.ui.layout;

import io.fair_acc.chartfx.axes.spi.AbstractAxisParameter;
import io.fair_acc.chartfx.ui.geometry.Corner;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * A 3x3 grid pane that is divided into 4 sides, 4 corners, and a center content area.
 * The sides are based on the preferred size, and the content and corner sections use
 * whatever space is left.
 * <p>
 * TL    -   TOP      -   TR
 * LEFT  -   CONTENT  -   RIGHT
 * BL    -   BOTTOM   -   BR
 * <p>
 * Multiple corner and center nodes get laid out on top of each other, similar to using
 * a StackPane. Side nodes get laid out next to each other, similar to the behavior if
 * they were wrapped in a HBox or VBox.
 * <p>
 * Note: to start off with we can consider the axis height static (a function
 * of letter height), and the width to be a function of the axis resolution and
 * label size (more letters => wider). This breaks down for rotated labels,
 * so in the future we need to add a secondary check whether the actual size
 * resulted in a range/resolution (digits) change.
 * <p>
 * TODO: disallow computePrefWidth and computePrefHeight?
 * TODO: remove nodes if the area is too small?
 *
 * @author Florian Enner
 * @since 22 Jun 2023
 */
public class ChartPane extends Pane {

    private static final String CHART_ELEMENT = "chart-pane-element";

    /**
     * @param node input node
     * @return the side, corner, or null for center
     */
    public static Object getLocation(Node node) {
        return FXUtils.getConstraint(node, CHART_ELEMENT);
    }

    public static void setSide(Node node, Side value) {
        FXUtils.setConstraint(node, CHART_ELEMENT, value);
    }

    public static void setCorner(Node node, Corner value) {
        FXUtils.setConstraint(node, CHART_ELEMENT, value);
    }

    public static void setCenter(Node node) {
        FXUtils.setConstraint(node, CHART_ELEMENT, null);
    }

    public ChartPane addSide(Side side, Node... nodes) {
        getChildren().addAll(nodes);
        for (Node node : nodes) {
            setSide(node, side);
        }
        return this;
    }

    public ChartPane addCorner(Corner corner, Node... nodes) {
        getChildren().addAll(nodes);
        for (Node node : nodes) {
            setCorner(node, corner);
        }
        return this;
    }

    public ChartPane addCenter(Node... node) {
        getChildren().addAll(node);
        requestLayout();
        return this;
    }

    public void setSide(Side side, Node node) {
        addNode(side, node);
    }

    private void addNode(Object section, Node node) {
        getChildren().add(node);
        node.getProperties().put(CHART_ELEMENT, section);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        // Account for margin and border insets
        final double xLeft = snappedLeftInset();
        final double yTop = snappedTopInset();
        final double width = snapSizeX(getWidth()) - xLeft - snappedRightInset();
        final double height = snapSizeY(getHeight()) - yTop - snappedBottomInset();

        // Determine height of all horizontal parts
        cachedPrefSize.clear();
        cachedPrefSize.size(getChildren().size());
        var topHeight = 0;
        var bottomHeight = 0;

        int i = 0;
        double prefSize = 0;
        for (Node child : getChildren()) {
            Object location = getLocation(child);
            if (location instanceof Side) {
                switch ((Side) location) {
                    case TOP:
                        prefSize = GridLayout.getPrefHeight(child, width);
                        cachedPrefSize.set(i, prefSize);
                        topHeight += prefSize;
                        break;
                    case BOTTOM:
                        prefSize = GridLayout.getPrefHeight(child, width);
                        cachedPrefSize.set(i, prefSize);
                        bottomHeight += prefSize;
                        break;
                    case CENTER_HOR:
                        prefSize = GridLayout.getPrefHeight(child, width);
                        cachedPrefSize.set(i, prefSize);
                        break;
                }
            }
            i++;
        }
        final double contentHeight = height - topHeight - bottomHeight;

        // Determine width of all vertical parts
        double leftWidth = 0;
        double rightWidth = 0;
        i = 0;
        for (Node child : getChildren()) {
            Object location = getLocation(child);
            if (location instanceof Side) {
                switch ((Side) location) {
                    case LEFT:
                        prefSize = GridLayout.getPrefWidth(child, contentHeight);
                        cachedPrefSize.set(i, prefSize);
                        leftWidth += prefSize;
                        break;
                    case RIGHT:
                        prefSize = GridLayout.getPrefWidth(child, contentHeight);
                        cachedPrefSize.set(i, prefSize);
                        rightWidth += prefSize;
                        break;
                    case CENTER_VER:
                        prefSize = GridLayout.getPrefWidth(child, contentHeight);
                        cachedPrefSize.set(i, prefSize);
                        break;
                }
            }
            i++;
        }
        final double contentWidth = width - leftWidth - rightWidth;

        // Layout all center content
        final var xContent = xLeft + leftWidth;
        final var yContent = yTop + topHeight;
        final var xRight = xContent + contentWidth;
        final var yBottom = yContent + contentHeight;

        double xOffsetLeft = 0;
        double xOffsetRight = 0;
        double yOffsetTop = 0;
        double yOffsetBottom = 0;

        // Layout all children
        i = 0;
        for (Node child : getChildren()) {
            Object location = getLocation(child);
            prefSize = cachedPrefSize.getDouble(i++);

            if (location == null) { // Center nodes
                GridLayout.resizeRelocate(child, xContent, yContent, contentWidth, contentHeight);
            } else if (location instanceof Corner) { // Corner nodes
                switch ((Corner) location) {
                    case TOP_LEFT:
                        GridLayout.resizeRelocate(child, xLeft, yTop, leftWidth, topHeight);
                        break;
                    case TOP_RIGHT:
                        GridLayout.resizeRelocate(child, xRight, yTop, rightWidth, topHeight);
                        break;
                    case BOTTOM_LEFT:
                        GridLayout.resizeRelocate(child, xLeft, yBottom, leftWidth, bottomHeight);
                        break;
                    case BOTTOM_RIGHT:
                        GridLayout.resizeRelocate(child, xRight, yBottom, rightWidth, bottomHeight);
                        break;
                }
            } else if (location instanceof Side) {  // Side nodes
                switch ((Side) location) {
                    case TOP:
                        GridLayout.resizeRelocate(child, xContent, yTop + yOffsetTop, contentWidth, prefSize);
                        yOffsetTop += prefSize;
                        break;
                    case BOTTOM:
                        GridLayout.resizeRelocate(child, xContent, yBottom + yOffsetBottom, contentWidth, prefSize);
                        yOffsetBottom += prefSize;
                        break;
                    case LEFT:
                        GridLayout.resizeRelocate(child, xLeft + xOffsetLeft, yContent, prefSize, contentHeight);
                        xOffsetLeft += prefSize;
                        break;
                    case RIGHT:
                        GridLayout.resizeRelocate(child, xRight + xOffsetRight, yContent, prefSize, contentHeight);
                        xOffsetRight += prefSize;
                        break;
                    case CENTER_HOR:
                        GridLayout.resizeRelocate(child, xContent, yContent + contentHeight / 2 - prefSize / 2, contentWidth, prefSize);
                        break;
                    case CENTER_VER:
                        GridLayout.resizeRelocate(child, xContent + contentWidth / 2 - prefSize / 2, yContent, prefSize, contentHeight);
                        break;
                }
            }

        }

    }

    private final DoubleArrayList cachedPrefSize = new DoubleArrayList();

}
