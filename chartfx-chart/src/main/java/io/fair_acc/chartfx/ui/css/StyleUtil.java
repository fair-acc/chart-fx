package io.fair_acc.chartfx.ui.css;

import javafx.scene.Node;

/**
 * Utility class for styleable nodes
 *
 * @author ennerf
 */
public class StyleUtil {

    private StyleUtil() {
    }


    public static <NODE extends Node> NODE hiddenStyleNode(NODE node, String... styles) {
        hide(node);
        addStyles(node, styles);
        return node;
    }

    public static <NODE extends Node> NODE addStyles(NODE node, String... styles) {
        node.getStyleClass().addAll(styles);
        return node;
    }

    public static <NODE extends Node> NODE hide(NODE node) {
        node.setVisible(false);
        node.setManaged(false);
        return node;
    }

}
