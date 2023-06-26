package io.fair_acc.chartfx.ui.layout;

import io.fair_acc.chartfx.axes.spi.AbstractAxisParameter;
import io.fair_acc.chartfx.ui.geometry.Corner;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * A region that lays out all available axes and places
 * the content in the center.
 * <p>
 * Note: to start off with we can consider the axis height static (a function
 * of letter height), and the width to be a function of the axis resolution and
 * label size (more letters => wider). This breaks down for rotated labels,
 * so we need to add a secondary check whether the actual size resulted in
 * a range/resolution (digits) change.
 *
 * TODO: replace "Side" with generic properties?
 *
 * @author Florian Enner
 * @since 26 Jun 2023
 */
public class AxisLayout extends Region {

    @Override
    protected void layoutChildren() {
        // Account for margin and border insets
        double xLeft = snappedLeftInset();
        double yTop = snappedTopInset();
        final double width = snapSizeX(getWidth()) - xLeft - snappedRightInset();
        final double height = snapSizeY(getHeight()) - yTop - snappedBottomInset();

        // Determine height of all horizontal axes
        cachedSizes.clear();
        cachedSizes.size(getChildren().size());
        var topHeight = 0;
        var bottomHeight = 0;

        int i = 0;
        double size = 0;
        for (AbstractAxisParameter axis : axes) {
            switch (axis.getSide()) {
                case TOP:
                    size = GridLayout.getPrefHeight(axis, width);
                    cachedSizes.set(i, size);
                    topHeight += size;
                    break;
                case BOTTOM:
                    size = GridLayout.getPrefHeight(axis, width);
                    cachedSizes.set(i, size);
                    bottomHeight += size;
                    break;
                case CENTER_HOR:
                    size = GridLayout.getPrefHeight(axis, width);
                    cachedSizes.set(i, size);
                    break;
            }
            i++;
        }
        double contentHeight = height - topHeight - bottomHeight;

        // Determine width of all vertical axes
        double leftWidth = 0;
        double rightWidth = 0;
        i = 0;
        for (AbstractAxisParameter axis : axes) {
            switch (axis.getSide()) {
                case LEFT:
                    size = GridLayout.getPrefWidth(axis, width);
                    cachedSizes.set(i, size);
                    leftWidth += size;
                    break;
                case RIGHT:
                    size = GridLayout.getPrefWidth(axis, width);
                    cachedSizes.set(i, size);
                    rightWidth += size;
                    break;
                case CENTER_VER:
                    size = GridLayout.getPrefWidth(axis, width);
                    cachedSizes.set(i, size);
                    break;
            }
            i++;
        }
        double contentWidth = width - leftWidth - rightWidth;

        // Layout all center content
        final var xContent = xLeft + leftWidth;
        final var yContent = yTop + topHeight;
        for (Node node : content) {
            GridLayout.resizeRelocate(node, xContent, yContent, contentWidth, contentHeight);
        }

        // Corners
//        resizeRelocate(getCorner(Corner.TOP_LEFT), xLeft, yTop, leftWidth, topHeight);
//        resizeRelocate(getCorner(Corner.TOP_RIGHT), xRight, yTop, rightWidth, topHeight);
//        resizeRelocate(getCorner(Corner.BOTTOM_LEFT), xLeft, yBottom, leftWidth, bottomHeight);
//        resizeRelocate(getCorner(Corner.BOTTOM_RIGHT), xRight, yBottom, rightWidth, bottomHeight);

        // Layout all axes
        var xRight = xContent + contentWidth;
        var yBottom = yContent + contentHeight;
        i = 0;
        for (AbstractAxisParameter axis : axes) {
            size = cachedSizes.getDouble(i);
            i++;
            switch (axis.getSide()) {
                case TOP:
                    GridLayout.resizeRelocate(axis, xContent, yTop, contentWidth, size);
                    yTop += size;
                    break;
                case BOTTOM:
                    GridLayout.resizeRelocate(axis, xContent, yBottom, contentWidth, size);
                    yBottom += size;
                    break;
                case LEFT:
                    GridLayout.resizeRelocate(axis, xLeft, yContent, size, contentHeight);
                    xLeft += size;
                    break;
                case RIGHT:
                    GridLayout.resizeRelocate(axis, xRight, yContent, size, contentHeight);
                    xRight += size;
                    break;
                case CENTER_HOR:
                    GridLayout.resizeRelocate(axis, xContent, yContent + contentHeight / 2 - size / 2, contentWidth, size);
                    break;
                case CENTER_VER:
                    GridLayout.resizeRelocate(axis, xContent + contentWidth / 2 - size / 2, yContent, size, contentHeight);
                    break;
            }
        }

    }

    public ObservableList<AbstractAxisParameter> getAxes() {
        return axes;
    }

    public ObservableList<Node> getContentNodes() {
        return content;
    }

    private final ObservableList<AbstractAxisParameter> axes = FXCollections.observableArrayList();

    {
        // TODO: add utility or remove w/ generic property list
        axes.addListener((ListChangeListener<AbstractAxisParameter>) c -> {
            while (c.next()) {
                if (c.getRemovedSize() > 0) {
                    getChildren().removeAll(c.getRemoved());
                }
                if (c.getAddedSize() > 0) {
                    getChildren().addAll(c.getAddedSubList());
                }
            }
            this.requestLayout();
        });
    }

    private final ObservableList<Node> content = FXCollections.observableArrayList();

    {
        content.addListener((ListChangeListener<Node>) c -> {
            while (c.next()) {
                if (c.getRemovedSize() > 0) {
                    getChildren().removeAll(c.getRemoved());
                }
                if (c.getAddedSize() > 0) {
                    getChildren().addAll(c.getAddedSubList());
                }
            }
            this.requestLayout();
        });
    }

    private final DoubleArrayList cachedSizes = new DoubleArrayList();

}
