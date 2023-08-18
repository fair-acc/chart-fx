package io.fair_acc.chartfx.ui.css;

import javafx.css.PseudoClass;
import javafx.scene.Node;

/**
 * Built-in color palettes that apply pseudo classes.
 *
 * @author ennerf
 */
public enum ColorPalette {
    DEFAULT,
    MISC("misc"),
    ADOBE("adobe"),
    DELL("dell"),
    EQUIDISTANT("equidistant"),
    TUNEVIEWER("tuneviewer"),
    MATLAB_LIGHT("matlab-light"),
    MATLAB_DARK("matlab-dark"),
    MODENA("modena"),
    ATLANTAFX("atlantafx");

    private ColorPalette() {
        this.pseudoClass = null;
    }

    private ColorPalette(String name) {
        this.pseudoClass = PseudoClass.getPseudoClass("palette-" + name);
    }

    public PseudoClass getPseudoClass() {
        return pseudoClass;
    }

    public void applyPseudoClasses(Node node) {
        for (var palette : values) {
            node.pseudoClassStateChanged(palette.getPseudoClass(), this == palette);
        }
    }

    private final PseudoClass pseudoClass;
    private static final ColorPalette[] values = values();

}
