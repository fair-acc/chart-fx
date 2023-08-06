package io.fair_acc.chartfx.ui.layout;

import io.fair_acc.chartfx.ui.css.CssPropertyFactory;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.chartfx.utils.RotatedRegion;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.scene.control.Label;

import java.util.List;

/**
 * A label that has a styleable side and accounts
 * for rotation in the size computations.
 *
 * @author ennerf
 */
public class TitleLabel extends Label {

    private static final CssPropertyFactory<TitleLabel> CSS = new CssPropertyFactory<>(Label.getClassCssMetaData());

    public TitleLabel() {
        managedProperty().bind(visibleProperty().and(textProperty().isNotEmpty()));
        PropUtil.runOnChange(this::applyCss, sideProperty(), rotateProperty());
    }

    /**
     * The side of the chart where the title is displayed default Side.TOP
     */
    private final StyleableObjectProperty<Side> side = CSS.createSideProperty(this, Side.TOP);

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return TitleLabel.getClassCssMetaData();
    }

    @Override
    protected double computePrefWidth(double height) {
        return rotated.computePrefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return rotated.computePrefHeight(width);
    }

    @Override
    public void resizeRelocate(double x, double y, double width, double height) {
        rotated.resizeRelocate(x, y, width, height);
    }

    public Side getSide() {
        return side.get();
    }

    public StyleableObjectProperty<Side> sideProperty() {
        return side;
    }

    public void setSide(Side side) {
        this.side.set(side);
    }

    private final RotatedRegion rotated = new RotatedRegion(this,
            super::computePrefWidth,
            super::computePrefHeight,
            super::resizeRelocate);

}
