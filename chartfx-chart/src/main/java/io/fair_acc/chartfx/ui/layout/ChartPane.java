package io.fair_acc.chartfx.ui.layout;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import io.fair_acc.chartfx.axes.spi.AbstractAxis;
import io.fair_acc.chartfx.ui.geometry.Corner;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import io.fair_acc.dataset.utils.AssertUtils;

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
 * The axis size depends on the resolution, the rotation, and whether labels overlap
 * (i.e. label shifting). Since the real overlap is not know until after the layout,
 * we use some basic heuristics for approximation to avoid recursive loops.
 * 1) guess an approximate height of the content
 * 2) determine the content width based on approximated vertical labels
 * 3) determine the content height based on correct horizontal labels
 * 4) lay out everything
 * <p>
 * TODO: remove nodes if the area is too small?
 *
 * @author ennerf
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
        AssertUtils.notNull("Side must not be null", value);
        value.applyPseudoClasses(node);
        FXUtils.setConstraint(node, CHART_ELEMENT, value);
    }

    public static void setCorner(Node node, Corner value) {
        FXUtils.setConstraint(node, CHART_ELEMENT, value);
    }

    public static void setCenter(Node node) {
        FXUtils.setConstraint(node, CHART_ELEMENT, null);
    }

    static void resizeRelocate(Node node, double x, double y, double width, double height) {
        if (node != null) {
            node.resizeRelocate(x, y, width, height);
        }
    }

    static double getPrefHeight(Node node, double width) {
        if (node == null || !node.isVisible()) {
            return 0;
        }
        return node.prefHeight(width);
    }

    static double getPrefWidth(Node node, double height) {
        if (node == null || !node.isVisible()) {
            return 0;
        }
        return node.prefWidth(height);
    }

    public ChartPane addSide(Side side, Node node, Node... more) {
        getChildren().add(node);
        setSide(node, side);
        for (Node next : more) {
            addSide(side, next);
        }
        return this;
    }

    public ChartPane addCorner(Corner corner, Node node, Node... more) {
        getChildren().add(node);
        setCorner(node, corner);
        for (Node next : more) {
            addCorner(corner, next);
        }
        return this;
    }

    public ChartPane addCenter(Node node, Node... more) {
        getChildren().add(node);
        setCenter(node);
        for (Node next : more) {
            addCenter(next);
        }
        return this;
    }

    public void remove(Node node, Node... more) {
        getChildren().remove(node);
        if (more.length > 0) {
            getChildren().removeAll(more);
        }
    }

    private void addNode(Object section, Node node) {
        getChildren().add(node);
        node.getProperties().put(CHART_ELEMENT, section);
        requestLayout();
    }

    private double estimateHeightOfAllHorizontalAxes() {
        double sum = 0;
        for (Node child : getChildren()) {
            if (!child.isManaged() || !(child instanceof AbstractAxis)) {
                continue;
            }
            Object location = getLocation(child);
            if (location == Side.TOP || location == Side.BOTTOM) {
                // default to use the previous height or some set minimum
                var axis = (AbstractAxis) child;
                sum += Math.max(0, axis.getHeight() > 0 ? axis.getHeight() : axis.getMinHeight());
            }
        }
        return sum;
    }

    @Override
    protected double computeMinWidth(double height) {
        double verticalSum = 0;
        double maxHorizontalWidth = 0;
        for (Node child : getChildren()) {
            if (!child.isManaged())
                continue;
            Object location = getLocation(child);
            if (location == null) {
                maxHorizontalWidth = Math.max(maxHorizontalWidth, child.minWidth(height));
            } else if (location instanceof Side) {
                switch ((Side) location) {
                case CENTER_HOR:
                case CENTER_VER:
                case TOP:
                case BOTTOM:
                    maxHorizontalWidth = Math.max(maxHorizontalWidth, child.minWidth(height));
                    break;
                case LEFT:
                case RIGHT:
                    verticalSum += child.minWidth(height);
                    break;
                }
            }
        }
        return verticalSum + maxHorizontalWidth;
    }

    @Override
    protected double computeMinHeight(double width) {
        double horizontalSum = 0;
        double maxVerticalHeight = 0;
        for (Node child : getChildren()) {
            if (!child.isManaged())
                continue;
            Object location = getLocation(child);
            if (location == null) {
                maxVerticalHeight = Math.max(maxVerticalHeight, child.minHeight(width));
            } else if (location instanceof Side) {
                switch ((Side) location) {
                case CENTER_HOR:
                case CENTER_VER:
                case LEFT:
                case RIGHT:
                    maxVerticalHeight = Math.max(maxVerticalHeight, child.minHeight(width));
                    break;
                case TOP:
                case BOTTOM:
                    horizontalSum += child.minHeight(width);
                    break;
                }
            }
        }
        return horizontalSum + maxVerticalHeight;
    }

    @Override
    protected void layoutChildren() {
        // Account for margin and border insets
        final double xLeft = snappedLeftInset();
        final double yTop = snappedTopInset();
        final double width = snapSizeX(getWidth()) - xLeft - snappedRightInset();
        final double height = snapSizeY(getHeight()) - yTop - snappedBottomInset();

        // Reset state
        cachedPrefSize.clear();
        cachedPrefSize.size(getChildren().size());
        double prefSize;
        int i;

        // (1) Approximate axis height to improve the label overlap estimate
        final double guessedContentHeight = snapSizeY(height - estimateHeightOfAllHorizontalAxes());

        // (2) Determine width of all vertical parts. The labels are generated
        // for the approximate height, so the overlap may not be perfect, but it's
        // a pretty good guess with a low cost of failure.
        double leftWidth = 0;
        double rightWidth = 0;
        i = 0;
        for (Node child : getChildren()) {
            if (!child.isManaged())
                continue;
            Object location = getLocation(child);
            if (location instanceof Side) {
                switch ((Side) location) {
                case LEFT:
                    prefSize = snapSizeX(getPrefWidth(child, guessedContentHeight));
                    cachedPrefSize.set(i, prefSize);
                    leftWidth += prefSize;
                    break;
                case RIGHT:
                    prefSize = snapSizeX(getPrefWidth(child, guessedContentHeight));
                    cachedPrefSize.set(i, prefSize);
                    rightWidth += prefSize;
                    break;
                case CENTER_VER:
                    prefSize = snapSizeX(getPrefWidth(child, guessedContentHeight));
                    cachedPrefSize.set(i, prefSize);
                    break;
                }
            }
            i++;
        }
        final double contentWidth = snapSizeX(width - leftWidth - rightWidth);

        // (3) Determine the height of all horizontal parts. The labels are generated
        // for the actual width, so the placement should be correct.
        var topHeight = 0;
        var bottomHeight = 0;
        i = 0;
        for (Node child : getChildren()) {
            if (!child.isManaged())
                continue;
            Object location = getLocation(child);
            if (location instanceof Side) {
                switch ((Side) location) {
                case TOP:
                    prefSize = snapSizeY(getPrefHeight(child, contentWidth));
                    cachedPrefSize.set(i, prefSize);
                    topHeight += prefSize;
                    break;
                case BOTTOM:
                    prefSize = snapSizeY(getPrefHeight(child, contentWidth));
                    cachedPrefSize.set(i, prefSize);
                    bottomHeight += prefSize;
                    break;
                case CENTER_HOR:
                    prefSize = snapSizeY(getPrefHeight(child, contentWidth));
                    cachedPrefSize.set(i, prefSize);
                    break;
                }
            }
            i++;
        }
        final double contentHeight = snapSizeY(height - topHeight - bottomHeight);

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
            if (!child.isManaged())
                continue;
            Object location = getLocation(child);
            prefSize = cachedPrefSize.getDouble(i++);

            if (location == null) { // Center nodes
                resizeRelocate(child, xContent, yContent, contentWidth, contentHeight);
            } else if (location instanceof Corner) { // Corner nodes
                switch ((Corner) location) {
                case TOP_LEFT:
                    resizeRelocate(child, xLeft, yTop, leftWidth, topHeight);
                    break;
                case TOP_RIGHT:
                    resizeRelocate(child, xRight, yTop, rightWidth, topHeight);
                    break;
                case BOTTOM_LEFT:
                    resizeRelocate(child, xLeft, yBottom, leftWidth, bottomHeight);
                    break;
                case BOTTOM_RIGHT:
                    resizeRelocate(child, xRight, yBottom, rightWidth, bottomHeight);
                    break;
                }
            } else if (location instanceof Side) { // Side nodes
                switch ((Side) location) {
                case TOP:
                    resizeRelocate(child, xContent, yTop + yOffsetTop, contentWidth, prefSize);
                    yOffsetTop += prefSize;
                    break;
                case BOTTOM:
                    resizeRelocate(child, xContent, yBottom + yOffsetBottom, contentWidth, prefSize);
                    yOffsetBottom += prefSize;
                    break;
                case LEFT:
                    resizeRelocate(child, xLeft + xOffsetLeft, yContent, prefSize, contentHeight);
                    xOffsetLeft += prefSize;
                    break;
                case RIGHT:
                    resizeRelocate(child, xRight + xOffsetRight, yContent, prefSize, contentHeight);
                    xOffsetRight += prefSize;
                    break;
                case CENTER_HOR:
                    resizeRelocate(child, xContent, snapSizeY(yContent + contentHeight / 2 - prefSize / 2), contentWidth, prefSize);
                    break;
                case CENTER_VER:
                    resizeRelocate(child, snapSizeX(xContent + contentWidth / 2 - prefSize / 2), yContent, prefSize, contentHeight);
                    break;
                }
            }
        }
    }

    private final DoubleArrayList cachedPrefSize = new DoubleArrayList();
}
