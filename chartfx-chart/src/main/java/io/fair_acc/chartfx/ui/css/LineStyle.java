package io.fair_acc.chartfx.ui.css;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;

/**
 * An invisible node that lets users change styles
 * via CSS. The actual drawing is done in a canvas to
 * reduce the number of nodes on the SceneGraph. Each change
 * increments a counter, so that it is easy to invalidate outdated
 * renderings.
 *
 * @author ennerf
 */
public class LineStyle extends Line {

    public LineStyle(String... styles) {
       this(true, styles);
    }

    public LineStyle(boolean hide, String... styles) {
        StyleUtil.addStyles(this, styles);
        setManaged(false);
        // It looks like a manual set will overwrite any CSS styling
        if (hide) {
            setVisible(false);
        }
        StyleUtil.registerShapeListener(this, StyleUtil.incrementOnChange(changeCounter));
    }

    public void copyStyleTo(GraphicsContext gc) {
        StyleUtil.copyShapeStyle(this, gc);
    }

    public long getChangeCounter() {
        return changeCounter.get();
    }

    public ReadOnlyLongProperty changeCounterProperty() {
        return changeCounter;
    }

    LongProperty changeCounter = new SimpleLongProperty(0);

}
