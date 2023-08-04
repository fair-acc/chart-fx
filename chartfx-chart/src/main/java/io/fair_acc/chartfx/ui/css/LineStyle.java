package io.fair_acc.chartfx.ui.css;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Line;

/**
 * An invisible node that lets users change styles
 * via CSS. The actual drawing is done in a canvas to
 * reduce the number of nodes on the SceneGraph. Each change
 * increments a counter, so that it is easy to invalidate outdated
 * renderings.
 *
 * @author ennerf
 */
public class LineStyle extends Line implements StyleUtil.ChangeCounter {

    public LineStyle(String... styles) {
        StyleUtil.styleNode(this, styles);
        StyleUtil.registerShapeListener(this, StyleUtil.incrementOnChange(changeCounter));
    }

    public void copyStyleTo(GraphicsContext gc) {
        StyleUtil.copyShapeStyle(this, gc);
    }

    public ReadOnlyLongProperty changeCounterProperty() {
        return changeCounter;
    }

    private final LongProperty changeCounter = new SimpleLongProperty(0);

}
