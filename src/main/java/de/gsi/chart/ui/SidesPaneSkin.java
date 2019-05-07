package de.gsi.chart.ui;

import java.security.InvalidParameterException;

import de.gsi.chart.ui.geometry.Side;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.MapChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.util.Duration;

public class SidesPaneSkin extends SkinBase<SidesPane> {

    private final BorderPane borderPane = new BorderPane();
    private final EventHandler<MouseEvent> exitedHandler;
    private boolean mousePressed;

    public SidesPaneSkin(final SidesPane pane) {
        super(pane);

        exitedHandler = event -> {
            if (isMouseEnabled() /* && getSkinnable().getPinnedSide() == null */ && !mousePressed) {
                for (final Side s : Side.values()) {
                    if (!getSkinnable().isPinned(s)) {
                        hide(s);
                    }
                }
            }
        };

        getChildren().add(borderPane);
        updateBorderPane();

        final InvalidationListener rebuildListener = observable -> updateBorderPane();
        pane.contentProperty().addListener(rebuildListener);
        pane.topProperty().addListener(rebuildListener);
        pane.rightProperty().addListener(rebuildListener);
        pane.bottomProperty().addListener(rebuildListener);
        pane.leftProperty().addListener(rebuildListener);

        pane.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (isMouseEnabled() /* && getSkinnable().getPinnedSide() == null */) {
                final Side side = getSide(event);
                if (side != null) {
                    show(side);
                } else if (isMouseMovedOutsideSides(event)) {
                    for (final Side s : Side.values()) {
                        if (!getSkinnable().isPinned(s)) {
                            hide(s);
                        }
                    }
                }
            }
        });

        pane.addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);

        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> mousePressed = true);

        pane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            mousePressed = false;

            if (isMouseEnabled() /* && getSkinnable().getPinnedSide() == null */) {
                final Side side = getSide(event);
                if (side != null) {
                    show(side);
                }
            }
        });

        for (final Side side : Side.values()) {
            visibility[side.ordinal()] = new SimpleDoubleProperty(0);
            visibility[side.ordinal()].addListener(observable -> getSkinnable().requestLayout());
        }

        for (final Side side : Side.values()) {
            if (getSkinnable().isPinned(side)) {
                show(side);
            }
        }

        pane.pinnedSideProperty().addListener((MapChangeListener<Side, Boolean>) change -> {
            for (final Side side : Side.values()) {
                if (getSkinnable().isPinned(side)) {
                    show(side);
                } else {
                    hide(side);
                }
            }
        });

        final Rectangle clip = new Rectangle();
        clip.setX(0);
        clip.setY(0);
        clip.widthProperty().bind(getSkinnable().widthProperty());
        clip.heightProperty().bind(getSkinnable().heightProperty());

        getSkinnable().setClip(clip);
    }

    private boolean isMouseMovedOutsideSides(final MouseEvent event) {
        return !(getSkinnable().getLeft() != null
                && getSkinnable().getLeft().getBoundsInParent().contains(event.getX(), event.getY()))
                && (!(getSkinnable().getTop() != null
                        && getSkinnable().getTop().getBoundsInParent().contains(event.getX(), event.getY()))
                        && (!(getSkinnable().getRight() != null
                                && getSkinnable().getRight().getBoundsInParent().contains(event.getX(), event.getY()))
                                && !(getSkinnable().getBottom() != null && getSkinnable().getBottom()
                                        .getBoundsInParent().contains(event.getX(), event.getY()))));
    }

    private boolean isMouseEnabled() {
        return getSkinnable().getTriggerDistance() > 0;
    }

    private Side getSide(final MouseEvent evt) {
        if (borderPane.getBoundsInLocal().contains(evt.getX(), evt.getY())) {
            final double trigger = getSkinnable().getTriggerDistance();
            // changed to check for trigger only for side-panes with actual
            // content
            if (evt.getX() <= trigger && getSkinnable().getLeft() != null) {
                return Side.LEFT;
            } else if (evt.getX() > getSkinnable().getWidth() - trigger && getSkinnable().getRight() != null) {
                return Side.RIGHT;
            } else if (evt.getY() <= trigger && getSkinnable().getTop() != null) {
                return Side.TOP;
            } else if (evt.getY() > getSkinnable().getHeight() - trigger && getSkinnable().getBottom() != null) {
                return Side.BOTTOM;
            }
        }

        return null;
    }

    private final DoubleProperty[] visibility = new SimpleDoubleProperty[Side.values().length];

    private final Timeline showTimeline[] = new Timeline[Side.values().length];

    private void show(final Side side) {
        if (hideTimeline[side.ordinal()] != null) {
            hideTimeline[side.ordinal()].stop();
        }

        if (showTimeline[side.ordinal()] != null && showTimeline[side.ordinal()].getStatus() == Status.RUNNING) {
            return;
        }

        final KeyValue[] keyValues = new KeyValue[Side.values().length];

        keyValues[side.ordinal()] = new KeyValue(visibility[side.ordinal()], 1);

        final Duration delay = getSkinnable().getAnimationDelay() != null ? getSkinnable().getAnimationDelay()
                : Duration.millis(300);
        final Duration duration = getSkinnable().getAnimationDuration() != null ? getSkinnable().getAnimationDuration()
                : Duration.millis(200);

        final KeyFrame keyFrame = new KeyFrame(duration, keyValues);
        showTimeline[side.ordinal()] = new Timeline(keyFrame);
        showTimeline[side.ordinal()].setDelay(delay);
        showTimeline[side.ordinal()].play();
    }

    private final Timeline hideTimeline[] = new Timeline[Side.values().length];

    private void hide(final Side side) {
        if (showTimeline[side.ordinal()] != null) {
            showTimeline[side.ordinal()].stop();
        }

        if (hideTimeline[side.ordinal()] != null && hideTimeline[side.ordinal()].getStatus() == Status.RUNNING) {
            return;
        }

        boolean sideVisible = visibility[side.ordinal()].get() > 0;
        // nothing to do here
        if (!sideVisible) {
            return;
        }

        final KeyValue[] keyValues = new KeyValue[Side.values().length];
        keyValues[side.ordinal()] = new KeyValue(visibility[side.ordinal()], 0);

        final Duration delay = getSkinnable().getAnimationDelay() != null ? getSkinnable().getAnimationDelay()
                : Duration.millis(300);
        final Duration duration = getSkinnable().getAnimationDuration() != null ? getSkinnable().getAnimationDuration()
                : Duration.millis(200);

        final KeyFrame keyFrame = new KeyFrame(duration, keyValues);
        hideTimeline[side.ordinal()] = new Timeline(keyFrame);
        hideTimeline[side.ordinal()].setDelay(delay);
        hideTimeline[side.ordinal()].play();
    }

    private void updateBorderPane() {
        borderPane.getChildren().clear();

        if (getSkinnable().getContent() != null) {
            borderPane.setCenter(getSkinnable().getContent());
        }
        if (getSkinnable().getTop() != null) {
            borderPane.setTop(getSkinnable().getTop());
            getSkinnable().getTop().setManaged(true);
            getSkinnable().getTop().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getTop().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
        if (getSkinnable().getRight() != null) {
            borderPane.setRight(getSkinnable().getRight());
            getSkinnable().getRight().setManaged(true);
            getSkinnable().getRight().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getRight().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
        if (getSkinnable().getBottom() != null) {
            borderPane.setBottom(getSkinnable().getBottom());
            getSkinnable().getBottom().setManaged(true);
            getSkinnable().getBottom().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getBottom().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
        if (getSkinnable().getLeft() != null) {
            borderPane.setLeft(getSkinnable().getLeft());
            getSkinnable().getLeft().setManaged(true);
            getSkinnable().getLeft().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getLeft().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
    }

    @Override
    protected void layoutChildren(final double contentX, final double contentY, final double contentWidth,
            final double contentHeight) {

        /*
         * Layout the BorderPane in a normal way (equals
         * "lay out the content node", the only managed node)
         */
        super.layoutChildren(contentX, contentY, contentWidth, contentHeight);

        // layout the managed side nodes && update preferred initial size
        // estimate
        final Node bottom = getSkinnable().getBottom();
        if (bottom != null) {
            final double prefHeight = getSkinnable().prefHeightBottomProperty().get();
            if (bottom.prefHeight(-1) > 0) {
                getSkinnable().prefHeightBottomProperty().set(Math.max(bottom.prefHeight(-1), prefHeight));
            }
            final double offset = prefHeight * visibility[Side.BOTTOM.ordinal()].get();
            bottom.setVisible(visibility[Side.BOTTOM.ordinal()].get() > 0);
            SidesPaneSkin.setPrefHeight(bottom, offset);
        }

        final Node left = getSkinnable().getLeft();
        if (left != null) {
            final double prefWidth = getSkinnable().prefWidthLeftProperty().get();
            if (left.prefWidth(-1) > 0) {
                getSkinnable().prefWidthLeftProperty().set(Math.max(left.prefWidth(-1), prefWidth));
            }
            final double offset = prefWidth * visibility[Side.LEFT.ordinal()].get();
            left.setVisible(visibility[Side.LEFT.ordinal()].get() > 0);
            SidesPaneSkin.setPrefWidth(left, offset);
        }

        final Node right = getSkinnable().getRight();
        if (right != null) {
            final double prefWidth = getSkinnable().prefWidthRightProperty().get();
            if (right.prefWidth(-1) > 0) {
                getSkinnable().prefWidthRightProperty().set(Math.max(right.prefWidth(-1), prefWidth));
            }
            final double offset = prefWidth * visibility[Side.RIGHT.ordinal()].get();
            right.setVisible(visibility[Side.RIGHT.ordinal()].get() > 0);
            SidesPaneSkin.setPrefWidth(right, offset);
        }

        final Node top = getSkinnable().getTop();
        if (top != null) {
            final double prefHeight = getSkinnable().prefHeightTopProperty().get();
            if (top.prefHeight(-1) > 0) {
                getSkinnable().prefHeightTopProperty().set(Math.max(top.prefHeight(-1), prefHeight));
            }
            final double offset = prefHeight * visibility[Side.TOP.ordinal()].get();
            top.setVisible(visibility[Side.TOP.ordinal()].get() > 0);
            top.setClip(new Rectangle(contentWidth, offset));
            SidesPaneSkin.setPrefHeight(top, offset);
        }
    }

    /**
     * Sets the preferred height of the generic node object by casting it onto
     * the known Region, ... object
     *
     * @param node
     *            the node to be adapted
     * @param prefHeight
     *            the desired preferred height
     */
    private static void setPrefHeight(final Node node, final double prefHeight) {
        if (node instanceof Region) {
            ((Region) node).setPrefHeight(prefHeight);
            return;
        } else if (node instanceof Canvas) {
            ((Canvas) node).setHeight(prefHeight);
            return;
        } else if (node instanceof WebView) {
            ((WebView) node).setPrefHeight(prefHeight);
            return;
        }
        // add other derivative of 'Node'
        throw new InvalidParameterException("no prefHeight for class type:" + node.getClass().getCanonicalName());
    }

    /**
     * Sets the preferred width of the generic node object by casting it onto
     * the known Region, ... object
     *
     * @param node
     *            the node to be adapted
     * @param prefWidth
     *            the desired preferred height
     */
    private static void setPrefWidth(final Node node, final double prefWidth) {
        if (node instanceof Region) {
            ((Region) node).setPrefWidth(prefWidth);
            return;
        } else if (node instanceof Canvas) {
            ((Canvas) node).setWidth(prefWidth);
            return;
        } else if (node instanceof WebView) {
            ((WebView) node).setPrefWidth(prefWidth);
            return;
        }
        // add other derivative of 'Node'
        throw new InvalidParameterException("no prefWidth for class type:" + node.getClass().getCanonicalName());
    }
}
