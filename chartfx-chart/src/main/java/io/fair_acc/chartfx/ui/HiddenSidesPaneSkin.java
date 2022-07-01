/*
 * Copyright (c) 2013, ControlsFX
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.fair_acc.chartfx.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class HiddenSidesPaneSkin extends SkinBase<HiddenSidesPane> {
    private final StackPane stackPane;
    private final EventHandler<MouseEvent> exitedHandler;
    private boolean mousePressed;
    private final DoubleProperty[] visibility = new SimpleDoubleProperty[Side.values().length];
    private Timeline showTimeline;
    private Timeline hideTimeline;

    private Node lastHideBlockingNode = null;
    private long blockedSince = 0;

    public HiddenSidesPaneSkin(HiddenSidesPane pane) {
        super(pane);

        exitedHandler = event -> {
            if (isMouseEnabled() && getSkinnable().getPinnedSide() == null && !mousePressed) {
                hide();
            }
        };

        stackPane = new StackPane();
        stackPane.setId("HiddenSidesPaneSkin::StackPane");
        getChildren().add(stackPane);
        updateStackPane();

        final InvalidationListener rebuildListener = observable -> updateStackPane();
        pane.contentProperty().addListener(rebuildListener);
        pane.topProperty().addListener(rebuildListener);
        pane.rightProperty().addListener(rebuildListener);
        pane.bottomProperty().addListener(rebuildListener);
        pane.leftProperty().addListener(rebuildListener);

        pane.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (isMouseEnabled() && getSkinnable().getPinnedSide() == null) {
                final Side side = getSide(event);
                if (side == null) {
                    if (isMouseMovedOutsideSides(event)) {
                        hide();
                    }
                } else {
                    show(side);
                }
            }
        });

        pane.addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);

        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> mousePressed = true);

        pane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            mousePressed = false;

            if (isMouseEnabled() && getSkinnable().getPinnedSide() == null) {
                final Side side = getSide(event);
                if (side == null) {
                    hide();
                } else {
                    show(side);
                }
            }
        });

        for (final Side side : Side.values()) {
            visibility[side.ordinal()] = new SimpleDoubleProperty(0); // NOPMD initial allocation, not recurrent
            visibility[side.ordinal()].addListener(observable -> getSkinnable().requestLayout());
        }

        final Side pinnedSide = getSkinnable().getPinnedSide();
        if (pinnedSide != null) {
            show(pinnedSide);
        }

        pane.pinnedSideProperty().addListener(observable -> show(getSkinnable().getPinnedSide()));

        final Rectangle clip = new Rectangle();
        clip.setX(0);
        clip.setY(0);
        clip.widthProperty().bind(getSkinnable().widthProperty());
        clip.heightProperty().bind(getSkinnable().heightProperty());

        getSkinnable().setClip(clip);
    }

    private double getPrefAlignmentH(final Node node, final double contentWidth) {
        final Pos alignment = StackPane.getAlignment(node);
        if (alignment == null) {
            return 0.0;
        }
        switch (alignment.getHpos()) {
        case RIGHT:
            return contentWidth - node.getLayoutBounds().getWidth();
        case CENTER:
            return 0.5 * (contentWidth - node.getLayoutBounds().getWidth());
        case LEFT:
        default:
            return 0.0;
        }
    }

    private double getPrefAlignmentV(final Node node, final double contentHeight) {
        final Pos alignment = StackPane.getAlignment(node);
        if (alignment == null) {
            return 0.0;
        }
        switch (alignment.getVpos()) {
        case BOTTOM:
            return contentHeight - node.getLayoutBounds().getHeight();
        case CENTER:
            return 0.5 * (contentHeight - node.getLayoutBounds().getHeight());
        case TOP:
        default:
            return 0.0;
        }
    }

    private Side getSide(MouseEvent evt) {
        if (stackPane.getBoundsInLocal().contains(evt.getX(), evt.getY())) {
            final double trigger = getSkinnable().getTriggerDistance();
            // changed to check for trigger only for side-panes with actual content
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

    private boolean hasShowingChild(Node n) {
        if (n == null) {
            return false;
        }
        if (n.isHover()) {
            lastHideBlockingNode = n;
            return true;
        }
        try {
            Method showingMethod = getMethod(n.getClass(), "isShowing");
            if (showingMethod != null && (Boolean) showingMethod.invoke(n)) {
                lastHideBlockingNode = n;
                return true;
            }
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // do nothing
        }
        if (n instanceof Parent) {
            return ((Parent) n).getChildrenUnmodifiable().stream().anyMatch(this::hasShowingChild);
        }
        return false;
    }

    private void hide() {
        if (showTimeline != null) {
            showTimeline.stop();
        }

        if (hideTimeline != null && hideTimeline.getStatus() == Status.RUNNING) {
            return;
        }

        Side visibleSide = null;
        for (final Side side : Side.values()) {
            if (visibility[side.ordinal()].get() > 0) {
                visibleSide = side;
                break;
            }
        }

        // nothing to do here
        if (visibleSide == null) {
            return;
        }

        // check for children having focus (eg Combo boxes/menus)
        if (hasShowingChild(lastHideBlockingNode) || hasShowingChild(getSidePane(visibleSide))) {
            final long now = System.currentTimeMillis();
            if (blockedSince == 0) {
                blockedSince = now;
            }
            if ((now - blockedSince) < getSkinnable().getAnimationDelay().toMillis()) {
                return;
            }
        }
        blockedSince = 0;

        // collapse open menus/comboboxes before hiding side pane
        if (hasShowingChild(lastHideBlockingNode)) {
            Method closeMethod = getMethod(lastHideBlockingNode.getClass(), "hide");
            if (closeMethod != null) {
                try {
                    closeMethod.invoke(lastHideBlockingNode);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // do nothing
                }
            }
        }

        final KeyValue[] keyValues = new KeyValue[Side.values().length];
        for (final Side side : Side.values()) {
            keyValues[side.ordinal()] = new KeyValue(visibility[side.ordinal()], 0);
        }

        final Duration delay = getSkinnable().getAnimationDelay() == null ? Duration.millis(300) : getSkinnable().getAnimationDelay();
        final Duration duration = getSkinnable().getAnimationDuration() == null ? Duration.millis(300) : getSkinnable().getAnimationDuration();

        final KeyFrame keyFrame = new KeyFrame(duration, keyValues);
        hideTimeline = new Timeline(keyFrame);
        hideTimeline.setDelay(delay);
        hideTimeline.play();
    }

    private boolean isMouseEnabled() {
        return getSkinnable().getTriggerDistance() > 0;
    }

    private boolean isMouseMovedOutsideSides(MouseEvent event) {
        return !(getSkinnable().getLeft() != null && getSkinnable().getLeft().getBoundsInParent().contains(event.getX(), event.getY()))
     && (!(getSkinnable().getTop() != null && getSkinnable().getTop().getBoundsInParent().contains(event.getX(), event.getY()))
             && (!(getSkinnable().getRight() != null && getSkinnable().getRight().getBoundsInParent().contains(event.getX(), event.getY()))
                     && !(getSkinnable().getBottom() != null && getSkinnable().getBottom().getBoundsInParent().contains(event.getX(), event.getY()))));
    }

    private void show(Side side) {
        if (hideTimeline != null) {
            hideTimeline.stop();
        }

        if (showTimeline != null && showTimeline.getStatus() == Status.RUNNING) {
            return;
        }

        final KeyValue[] keyValues = new KeyValue[Side.values().length];
        for (final Side s : Side.values()) {
            keyValues[s.ordinal()] = new KeyValue(visibility[s.ordinal()], s.equals(side) ? 1 : 0);
        }

        final Duration delay = getSkinnable().getAnimationDelay() == null ? Duration.millis(300) : getSkinnable().getAnimationDelay();
        final Duration duration = getSkinnable().getAnimationDuration() == null ? Duration.millis(200) : getSkinnable().getAnimationDuration();

        final KeyFrame keyFrame = new KeyFrame(duration, keyValues);
        showTimeline = new Timeline(keyFrame);
        showTimeline.setDelay(delay);
        showTimeline.play();
    }

    private void updateStackPane() {
        stackPane.getChildren().clear();

        if (getSkinnable().getContent() != null) {
            stackPane.getChildren().add(getSkinnable().getContent());
        }
        if (getSkinnable().getTop() != null) {
            stackPane.getChildren().add(getSkinnable().getTop());
            getSkinnable().getTop().setManaged(false);
            getSkinnable().getTop().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getTop().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
        if (getSkinnable().getRight() != null) {
            stackPane.getChildren().add(getSkinnable().getRight());
            getSkinnable().getRight().setManaged(false);
            getSkinnable().getRight().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getRight().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
        if (getSkinnable().getBottom() != null) {
            stackPane.getChildren().add(getSkinnable().getBottom());
            getSkinnable().getBottom().setManaged(false);
            getSkinnable().getBottom().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getBottom().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
        if (getSkinnable().getLeft() != null) {
            stackPane.getChildren().add(getSkinnable().getLeft());
            getSkinnable().getLeft().setManaged(false);
            getSkinnable().getLeft().removeEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
            getSkinnable().getLeft().addEventFilter(MouseEvent.MOUSE_EXITED, exitedHandler);
        }
    }

    protected Node getSidePane(Side side) {
        if (getSkinnable() == null) {
            return null;
        }
        switch (side) {
        case BOTTOM:
            return getSkinnable().getBottom();
        case LEFT:
            return getSkinnable().getLeft();
        case RIGHT:
            return getSkinnable().getRight();
        case TOP:
            return getSkinnable().getTop();
        default:
            return null;
        }
    }

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        /*
         * Layout the StackPane in a normal way (equals "lay out the content node", the only managed node)
         */
        super.layoutChildren(contentX, contentY, contentWidth, contentHeight);

        /*
         * Layout the centre node (content) explicitly. This is unaffected by and/or covered by the potentially open/visible SidePanes.
         */
        if (getSkinnable().getContent() != null) {
            getSkinnable().getContent().resizeRelocate(contentX, contentY, contentWidth, contentHeight);
        }

        // layout the unmanaged side nodes

        final Node bottom = getSkinnable().getBottom();
        if (bottom != null) {
            final double prefHeight = bottom.prefHeight(-1);
            final double offset = prefHeight * visibility[Side.BOTTOM.ordinal()].get();
            final double offsetX = getPrefAlignmentH(bottom, contentWidth);
            bottom.resizeRelocate(contentX + offsetX, contentY + contentHeight - offset, contentWidth, prefHeight);
            bottom.setVisible(visibility[Side.BOTTOM.ordinal()].get() > 0);
        }

        final Node left = getSkinnable().getLeft();
        if (left != null) {
            final double prefWidth = left.prefWidth(-1);
            final double offset = prefWidth * visibility[Side.LEFT.ordinal()].get();
            final double offsetY = getPrefAlignmentV(left, contentHeight);
            left.resizeRelocate(contentX - (prefWidth - offset), contentY + offsetY, prefWidth, contentHeight);
            left.setVisible(visibility[Side.LEFT.ordinal()].get() > 0);
        }

        final Node right = getSkinnable().getRight();
        if (right != null) {
            final double prefWidth = right.prefWidth(-1);
            final double offset = prefWidth * visibility[Side.RIGHT.ordinal()].get();
            final double offsetY = getPrefAlignmentV(right, contentHeight);
            right.resizeRelocate(contentX + contentWidth - offset, contentY + offsetY, prefWidth, contentHeight);
            right.setVisible(visibility[Side.RIGHT.ordinal()].get() > 0);
        }

        final Node top = getSkinnable().getTop();
        if (top != null) {
            final double prefHeight = top.prefHeight(-1);
            final double offset = prefHeight * visibility[Side.TOP.ordinal()].get();
            final double offsetX = getPrefAlignmentH(top, contentWidth);
            top.resizeRelocate(contentX + offsetX, contentY - (prefHeight - offset), contentWidth, prefHeight);
            top.setVisible(visibility[Side.TOP.ordinal()].get() > 0);
        }
    }

    public static Method getMethod(final Class<?> clazz, final String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }
}
