package io.fair_acc.chartfx.ui.layout;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/**
 * Similar to AnchorPane where every child node gets
 * resized to the same size as the parent pane. This
 * is used for chart backgrounds that need to have the
 * same size as the Canvas.
 *
 * @author ennerf
 */
public class FullSizePane extends Pane {

    public FullSizePane(Node... children) {
        getChildren().addAll(children);
    }

    public boolean isIgnoreInsets() {
        return ignoreInsets;
    }

    public void setIgnoreInsets(boolean ignoreInsets) {
        if (ignoreInsets != this.ignoreInsets) {
            this.ignoreInsets = ignoreInsets;
            if (!getChildren().isEmpty()) {
                requestLayout();
            }
        }
    }

    private boolean ignoreInsets = true;

    @Override
    protected void layoutChildren() {
        final double x, y, w, h;
        if (isIgnoreInsets()) {
            x = 0;
            y = 0;
            w = getWidth();
            h = getHeight();
        } else {
            x = snappedLeftInset();
            y = snappedTopInset();
            w = snapSizeX(getWidth()) - x - snappedRightInset();
            h = snapSizeY(getHeight()) - y - snappedBottomInset();
        }

        for (Node child : getChildren()) {
            if (!child.isManaged()) {
                continue;
            }

            if (child.isResizable()) {

                // Resize to cover the full area
                child.resizeRelocate(x, y, w, h);

            } else {

                // Special case Canvas as it is not resizable
                // by default, but is 99% what we want for plots.
                if (child instanceof Canvas) {
                    ((Canvas) child).setWidth(w);
                    ((Canvas) child).setHeight(h);
                }

                // Relocate
                child.relocate(x, y);

            }

        }
    }

}
