package io.fair_acc.chartfx.ui.css;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.shape.Line;

/**
 * @author ennerf
 */
public class LineStyle extends Line implements StyleUtil.StyleNode {
    public LineStyle(String... styles) {
        StyleUtil.styleNode(this, styles);
        StyleUtil.forEachStyleProp(this, StyleUtil.incrementOnChange(changeCounter));
    }

    @Override
    public String toString() {
        return StyleUtil.toStyleString(this);
    }

    public ReadOnlyLongProperty changeCounterProperty() {
        return changeCounter;
    }

    private final LongProperty changeCounter = new SimpleLongProperty(0);
}
