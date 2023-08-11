package io.fair_acc.chartfx.ui.css;

import io.fair_acc.chartfx.utils.PropUtil;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import javax.swing.event.ChangeListener;

/**
 * A hidden node that holds styles. This hides style nodes even
 * if the visibility property is set to true. This node is
 * unmanaged and does not do any layout.
 *
 * @author ennerf
 */
public class StyleGroup extends Parent {

    public StyleGroup(Pane pane, String... paneStyles) {
        this(pane, pane.getChildren(), paneStyles);
    }

    public StyleGroup(Node parent, ObservableList<Node> children, String... parentStyles) {
        this(children);
        StyleUtil.addStyles(parent, parentStyles);
    }

    public StyleGroup(ObservableList<Node> children) {
        StyleUtil.hiddenStyleNode(this);
        children.add(this);
    }

    @Override
    public void layoutChildren() {
        // do nothing
    }

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    public LineStyle newLineStyle(String... styles) {
        return addToChildren(new LineStyle(styles));
    }

    public TextStyle newTextStyle(String... styles) {
        return addToChildren(new TextStyle(styles));
    }

    private <T extends Node> T addToChildren(T style) {
        getChildren().add(style);
        return style;
    }

}
