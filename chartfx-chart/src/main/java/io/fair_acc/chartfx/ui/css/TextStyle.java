package io.fair_acc.chartfx.ui.css;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.text.Text;

/**
 * @author ennerf
 */
public class TextStyle extends Text implements StyleUtil.StyleNode {

    public TextStyle(String... styles) {
        StyleUtil.styleNode(this, styles);
        StyleUtil.forEachStyleProp(this, StyleUtil.incrementOnChange(changeCounter));
    }

    protected void incrementChangeCounter() {
        changeCounter.set(changeCounter.get() + 1);
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
