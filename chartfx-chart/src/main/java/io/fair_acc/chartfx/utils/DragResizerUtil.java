package io.fair_acc.chartfx.utils;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * Resize JavaFX Node objects based on mouse dragging.
 *
 * <p>
 * Example code:
 * 
 * <pre>
 * {@code
 *     Rectangle rectangle = new Rectangle(50, 50);
 *     rectangle.setFill(Color.BLACK);
 *
 *     DragResizeMod.makeResizable(rectangle, null);
 *     Pane root = new Pane();
 *     root.getChildren().add(rectangle);
 *
 *     primaryStage.setScene(new Scene(root, 300, 275));
 *     primaryStage.show();
 * }
 * </pre>
 *
 * A note on {@code OnDragResizeEventListener}: You need to override
 * OnDragResizeEventListener and
 * <ul>
 * <li>perform out of main field bounds check
 * <li>make changes to the node
 * </ul>
 * (this class will not change anything in node coordinates). There is a default
 * listener that works fine with Canvas, Rectangle and Region-derived Nodes (ie.
 * Pane and most other containers except Control)
 */

public class DragResizerUtil {
    private static final int MARGIN = 8;
    private static final double MIN_HEIGHT = 20;
    private static final double MIN_WIDTH = 30;
    private final Node node;
    protected static final OnDragResizeEventListener DEFAULT_LISTENER = new DefaultListener();
    private final OnDragResizeEventListener listener;

    private double clickX;
    private double clickY;
    private double nodeHeight;
    private double nodeWidth;
    private double nodePositionX;
    private double nodePositionY;
    private DragDirection state = DragDirection.DEFAULT;

    protected DragResizerUtil() {
        // null constructor
        this.node = new Region(); // dummy object
        this.listener = DEFAULT_LISTENER;
    }

    protected DragResizerUtil(Node node, OnDragResizeEventListener listener) {
        this.node = node;
        this.listener = listener != null ? listener : DEFAULT_LISTENER;
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        node.setOnMousePressed(this::mousePressed);
        node.setOnMouseDragged(this::mouseDragged);
        node.setOnMouseMoved(this::mouseOver);
        node.setOnMouseReleased(this::mouseReleased);
        node.setOnMouseClicked(this::resetNodeSize);
    }

    protected DragDirection currentMouseState(MouseEvent event) { // NOPMD -- reviewed NPATH complexity vs. readability
        final boolean west = isLeftResizeZone(event);
        final boolean east = isRightResizeZone(event);
        final boolean north = isTopResizeZone(event);
        final boolean south = isBottomResizeZone(event);

        if (west && north) {
            return DragDirection.NW_RESIZE;
        } else if (west && south) {
            return DragDirection.SW_RESIZE;
        } else if (east && north) {
            return DragDirection.NE_RESIZE;
        } else if (east && south) {
            return DragDirection.SE_RESIZE;
        } else if (east) {
            return DragDirection.E_RESIZE;
        } else if (west) {
            return DragDirection.W_RESIZE;
        } else if (north) {
            return DragDirection.N_RESIZE;
        } else if (south) {
            return DragDirection.S_RESIZE;
        } else if (isInDragZone(event)) {
            return DragDirection.DRAG;
        }

        return DragDirection.DEFAULT;
    }

    protected static boolean intersect(double side, double point) {
        return side + MARGIN > point && side - MARGIN < point;
    }

    protected boolean isBottomResizeZone(MouseEvent event) {
        return intersect(node.getBoundsInParent().getHeight(), event.getY());
    }

    protected boolean isInDragZone(MouseEvent event) {
        final Bounds bounds = node.getBoundsInParent();
        final double xPos = bounds.getMinX() + event.getX();
        final double yPos = bounds.getMinY() + event.getY();
        final double nodeX = bounds.getMinX() + MARGIN;
        final double nodeY = bounds.getMinY() + MARGIN;
        final double nodeX0 = bounds.getMinX() + bounds.getWidth() - MARGIN;
        final double nodeY0 = bounds.getMinY() + bounds.getHeight() - MARGIN;

        return (xPos > nodeX && xPos < nodeX0) && (yPos > nodeY && yPos < nodeY0);
    }

    protected boolean isInResizeZone(final MouseEvent event) {
        return isLeftResizeZone(event) || isRightResizeZone(event) || isBottomResizeZone(event) || isTopResizeZone(event);
    }

    protected static boolean isLeftResizeZone(final MouseEvent event) {
        return intersect(0, event.getX());
    }

    protected boolean isRightResizeZone(final MouseEvent event) {
        final Bounds bounds = node.getBoundsInParent();
        return intersect(bounds.getWidth(), event.getX());
    }

    protected static boolean isTopResizeZone(final MouseEvent event) {
        return intersect(0, event.getY());
    }

    protected void resetNodeSize(final MouseEvent evt) {
        if (!evt.isPrimaryButtonDown() || evt.getClickCount() < 2) {
            return;
        }

        if (!(node instanceof Region)) {
            return;
        }

        ((Region) node).setPrefWidth(Region.USE_COMPUTED_SIZE);
        ((Region) node).setPrefHeight(Region.USE_COMPUTED_SIZE);
    }

    protected void setNewInitialEventCoordinates(final MouseEvent event) {
        final Bounds bounds = node.getBoundsInParent();
        nodePositionX = bounds.getMinX();
        nodePositionY = bounds.getMinY();
        nodeHeight = bounds.getHeight();
        nodeWidth = bounds.getWidth();
        clickX = event.getX();
        clickY = event.getY();
    }

    protected void mouseDragged(final MouseEvent event) { // NOPMD
        final Bounds bounds = node.getBoundsInParent();
        final double mouseX = bounds.getMinX() + event.getX();
        final double mouseY = bounds.getMinY() + event.getY();
        if (state == DragDirection.DRAG) {
            listener.onDrag(node, mouseX - clickX, mouseY - clickY, nodeWidth, nodeHeight);
            return;
        }

        if (state == DragDirection.DEFAULT) {
            return;
        }
        // resizing
        double newX = nodePositionX;
        double newY = nodePositionY;
        double newHeight = nodeHeight;
        double newWidth = nodeWidth;
        // right resize
        if (state.isEast()) {
            newWidth = mouseX - nodePositionX;
        }
        // left resize
        if (state.isWest()) {
            newX = mouseX;
            newWidth = nodeWidth + nodePositionX - newX;
        }
        // bottom resize
        if (state.isSouth()) {
            newHeight = mouseY - nodePositionY;
        }
        // top resize
        if (state.isNorth()) {
            newY = mouseY;
            newHeight = nodeHeight + nodePositionY - newY;
        }
        // min valid rect size check
        if (newWidth < MIN_WIDTH) {
            if (state == DragDirection.W_RESIZE || state == DragDirection.NW_RESIZE || state == DragDirection.SW_RESIZE) {
                newX = newX - MIN_WIDTH + newWidth;
            }
            newWidth = MIN_WIDTH;
        }
        if (newHeight < MIN_HEIGHT) {
            if (state == DragDirection.N_RESIZE || state == DragDirection.NW_RESIZE || state == DragDirection.NE_RESIZE) {
                newY = newY + newHeight - MIN_HEIGHT;
            }
            newHeight = MIN_HEIGHT;
        }
        listener.onResize(node, newX, newY, newWidth, newHeight);
    }

    protected void mouseOver(final MouseEvent event) {
        final DragDirection retState = currentMouseState(event);
        final Cursor cursor = getCursorForState(retState);
        node.setCursor(cursor);
    }

    protected void mousePressed(final MouseEvent event) {
        if (isInResizeZone(event)) {
            setNewInitialEventCoordinates(event);
            state = currentMouseState(event);
            resetNodeSize(event);
        } else if (isInDragZone(event)) {
            setNewInitialEventCoordinates(event);
            state = DragDirection.DRAG;
        } else {
            state = DragDirection.DEFAULT;
        }
    }

    protected void mouseReleased(@SuppressWarnings("unused") final MouseEvent event) {
        node.setCursor(Cursor.DEFAULT);
        state = DragDirection.DEFAULT;
    }

    public static void makeResizable(final Node node) {
        makeResizable(node, null);
    }

    @SuppressWarnings("unused")
    public static void makeResizable(final Node node, final OnDragResizeEventListener listener) {
        new DragResizerUtil(node, listener);
    }

    protected static Cursor getCursorForState(DragDirection state) {
        switch (state) {
        case NW_RESIZE:
            return Cursor.NW_RESIZE;
        case SW_RESIZE:
            return Cursor.SW_RESIZE;
        case NE_RESIZE:
            return Cursor.NE_RESIZE;
        case SE_RESIZE:
            return Cursor.SE_RESIZE;
        case E_RESIZE:
            return Cursor.E_RESIZE;
        case W_RESIZE:
            return Cursor.W_RESIZE;
        case N_RESIZE:
            return Cursor.N_RESIZE;
        case S_RESIZE:
            return Cursor.S_RESIZE;
        default:
            return Cursor.DEFAULT;
        }
    }

    public enum DragDirection {
        DEFAULT,
        DRAG,
        E_RESIZE,
        N_RESIZE,
        NE_RESIZE,
        NW_RESIZE,
        S_RESIZE,
        SE_RESIZE,
        SW_RESIZE,
        W_RESIZE;

        public boolean isEast() {
            switch (this) {
            case NE_RESIZE:
            case E_RESIZE:
            case SE_RESIZE:
                return true;
            default:
                return false;
            }
        }

        public boolean isWest() {
            switch (this) {
            case NW_RESIZE:
            case W_RESIZE:
            case SW_RESIZE:
                return true;
            default:
                return false;
            }
        }

        public boolean isNorth() {
            switch (this) {
            case NW_RESIZE:
            case N_RESIZE:
            case NE_RESIZE:
                return true;
            default:
                return false;
            }
        }

        public boolean isSouth() {
            switch (this) {
            case NW_RESIZE:
            case N_RESIZE:
            case NE_RESIZE:
                return true;
            default:
                return false;
            }
        }
    }

    public interface OnDragResizeEventListener {
        void onDrag(final Node node, final double x, final double y, final double width, final double height);

        void onResize(final Node node, final double x, final double y, final double width, final double height);
    }

    private static class DefaultListener implements OnDragResizeEventListener {
        @Override
        public void onDrag(final Node node, final double x, final double y, final double width, final double height) {
            setNodeSize(node, x, y, width, height);
        }

        @Override
        public void onResize(final Node node, final double x, final double y, final double width, final double height) {
            setNodeSize(node, x, y, width, height);
        }

        protected void setNodeSize(final Node node, final double x, final double y, final double width, final double height) {
            node.setLayoutX(x);
            node.setLayoutY(y);
            // TODO find generic way to set width and height of any node
            // here we cannot set height and width to node directly.

            if (node instanceof Canvas) {
                ((Canvas) node).setWidth(width);
                ((Canvas) node).setHeight(height);
            } else if (node instanceof Rectangle) {
                ((Rectangle) node).setWidth(width);
                ((Rectangle) node).setHeight(height);
            } else if (node instanceof Region) {
                ((Region) node).setPrefWidth(width);
                ((Region) node).setPrefHeight(height);
            }
        }
    }
}
