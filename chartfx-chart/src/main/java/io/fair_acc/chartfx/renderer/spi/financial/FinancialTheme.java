package io.fair_acc.chartfx.renderer.spi.financial;

import io.fair_acc.chartfx.Chart;
import javafx.css.PseudoClass;

/**
 * A port of the string-ified themes to CSS pseudo classes
 *
 * @author ennerf
 */
public enum FinancialTheme {
    Default, // no pseudo classes
    Classic("classic"),
    Clearlook("clearlook"),
    Sand("sand"),
    Blackberry("blackberry"),
    Dark("dark");

    private FinancialTheme() {
        this.pseudoClass = null;
    }

    private FinancialTheme(String name) {
        this.pseudoClass = PseudoClass.getPseudoClass("financial-" + name);
    }

    public PseudoClass getPseudoClass() {
        return pseudoClass;
    }

    public void applyPseudoClasses(Chart node) {
        for (var palette : values) {
            node.pseudoClassStateChanged(palette.getPseudoClass(), this == palette);
        }
    }

    private final PseudoClass pseudoClass;
    private static final FinancialTheme[] values = values();

}
