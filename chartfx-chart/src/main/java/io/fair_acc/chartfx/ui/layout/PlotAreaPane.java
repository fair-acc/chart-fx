package io.fair_acc.chartfx.ui.layout;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/**
 * Similar to AnchorPane where every node is
 * anchored to full size.
 *
 * @author ennerf
 */
public class PlotAreaPane extends Pane {

    public PlotAreaPane(Node... children) {
        getChildren().addAll(children);
    }

    @Override
    protected void layoutChildren() {
        final double x = snappedLeftInset();
        final double y = snappedTopInset();
        final double w = snapSizeX(getWidth()) - x - snappedRightInset();
        final double h = snapSizeY(getHeight()) - y - snappedBottomInset();

        for (Node child : getChildren()) {
            // Resize to cover the full area
            child.resizeRelocate(x, y, w, h);

            // Special case Canvas as it is not resizable
            // by default, but is 99% what we want for plots.
            if (!child.isResizable() && child instanceof Canvas) {
                ((Canvas) child).setWidth(w);
                ((Canvas) child).setHeight(h);
            }
        }
    }

}
