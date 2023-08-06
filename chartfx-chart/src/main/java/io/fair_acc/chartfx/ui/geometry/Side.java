package io.fair_acc.chartfx.ui.geometry;

import javafx.css.PseudoClass;
import javafx.scene.Node;

/**
 * Re-implementation of JavaFX's {@code javafx.geometry.Side} implementation to also include centre axes.
 * 
 * @author rstein
 *
 */
public enum Side {
    /**
     * Represents top side of a rectangle.
     */
    TOP,

    /**
     * Represents bottom side of a rectangle.
     */
    BOTTOM,

    /**
     * Represents left side of a rectangle.
     */
    LEFT,

    /**
     * Represents right side of a rectangle.
     */
    RIGHT,
    /**
     * Represents horizontal centre axis of a rectangle.
     */
    CENTER_HOR,
    /**
     * Represents vertical centre axis of a rectangle.
     */
    CENTER_VER;

    /**
     * Indicates whether this is horizontal side of a rectangle (returns {@code true} for {@code TOP} and
     * {@code BOTTOM}.
     * 
     * @return {@code true} if this represents a horizontal side of a rectangle
     */
    public boolean isHorizontal() {
        return this == TOP || this == BOTTOM || this == CENTER_HOR;
    }

    /**
     * Indicates whether this is vertical side of a rectangle (returns {@code true} for {@code LEFT} and {@code RIGHT}.
     * 
     * @return {@code true} if this represents a vertical side of a rectangle
     */
    public boolean isVertical() {
        return this == LEFT || this == RIGHT || this == CENTER_VER;
    }

    public boolean isCenter() {
        return this == CENTER_VER || this == CENTER_HOR;
    }

    public void applyPseudoClasses(Node node) {
        node.pseudoClassStateChanged(CSS_TOP, this == TOP);
        node.pseudoClassStateChanged(CSS_BOTTOM, this == BOTTOM);
        node.pseudoClassStateChanged(CSS_LEFT, this == LEFT);
        node.pseudoClassStateChanged(CSS_RIGHT, this == RIGHT);
        node.pseudoClassStateChanged(CSS_CENTER_HOR, this == CENTER_HOR);
        node.pseudoClassStateChanged(CSS_CENTER_VER, this == CENTER_VER);
        node.pseudoClassStateChanged(CSS_HORIZONTAL, isHorizontal());
        node.pseudoClassStateChanged(CSS_VERTICAL, isVertical());
        node.pseudoClassStateChanged(CSS_CENTER, isCenter());
    }

    private static final PseudoClass CSS_TOP = PseudoClass.getPseudoClass("top");
    private static final PseudoClass CSS_BOTTOM = PseudoClass.getPseudoClass("bottom");
    private static final PseudoClass CSS_LEFT = PseudoClass.getPseudoClass("left");
    private static final PseudoClass CSS_RIGHT = PseudoClass.getPseudoClass("right");
    private static final PseudoClass CSS_CENTER_HOR = PseudoClass.getPseudoClass("center-hor");
    private static final PseudoClass CSS_CENTER_VER = PseudoClass.getPseudoClass("center-ver");
    private static final PseudoClass CSS_HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
    private static final PseudoClass CSS_VERTICAL = PseudoClass.getPseudoClass("vertical");
    private static final PseudoClass CSS_CENTER = PseudoClass.getPseudoClass("center");

}
