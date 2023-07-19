package io.fair_acc.chartfx.ui.css;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Path;

/**
 * An invisible node that lets users change styles
 * via CSS. The actual drawing is done in a canvas to
 * reduce the number of nodes on the SceneGraph. Each change
 * increments a counter, so that it is easy to invalidate outdated
 * renderings.
 *
 * @author ennerf
 */
public class PathStyle extends Path {

    public PathStyle(String... styles) {
        StyleUtil.hiddenStyleNode(this, styles);
        strokeProperty().addListener(onChange);
        fillProperty().addListener(onChange);
        strokeWidthProperty().addListener(onChange);
    }

    public void copyStyleTo(GraphicsContext gc) {
        gc.setStroke(getStroke());
        gc.setFill(getFill());
        gc.setLineWidth(getStrokeWidth());
    }

    public long getChangeCounter() {
        return changeCounter.get();
    }

    public ReadOnlyLongProperty changeCounterProperty() {
        return changeCounter;
    }

    LongProperty changeCounter = new SimpleLongProperty(0);
    ChangeListener<Object> onChange = (obs, old, value) -> changeCounter.set(changeCounter.get());

}
