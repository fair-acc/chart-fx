package de.gsi.chart.data.spi;

import de.gsi.chart.data.DataSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Class to define CSS-based style features N.B. needed for DataSet and
 * rendering styling
 *
 * @author rstein
 */
public abstract class AbstractStylable<D extends DataSet> implements DataSet {
    private final StringProperty style = new SimpleStringProperty(this, "style", null);
    private final StringProperty styleClass = new SimpleStringProperty(this, "styleClass", null);

    AbstractStylable() {
        super();
    }

    @SuppressWarnings("unchecked")
    protected D getThis() {
        return (D) this;
    }

    @Override
    public final StringProperty styleClassProperty() {
        return styleClass;
    }

    /**
     * A string representation of the CSS style associated with this
     * specific {@code Node}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     */
    @Override
    public String getStyle() {
        return style.get();
    }

    @Override
    public D setStyle(final String style) {
        this.style.set(style);
        return getThis();
    }

    @Override
    public StringProperty styleProperty() {
        return style;
    }

    // // -------------- STYLESHEET HANDLING
    // ------------------------------------------------------------------------------
    // private static class StyleableProperties {
    //
    // // private static final CssMetaData<AbstractStylable, Side> LEGEND_SIDE = new CssMetaData<AbstractStylable,
    // // Side>(
    // // "-fx-legend-side", new EnumConverter<>(Side.class), Side.BOTTOM) {
    // //
    // // @Override
    // // public boolean isSettable(final AbstractStylable chartPane) {
    // // return chartPane.legendSide == null || !chartPane.legendSide.isBound();
    // // }
    // //
    // // @Override
    // // public StyleableProperty<Side> getStyleableProperty(final AbstractStylable chartPane) {
    // // return (StyleableProperty<Side>) (WritableValue<Side>) chartPane.legendSideProperty();
    // // }
    // // };
    //
    // // private static final CssMetaData<AbstractStylable, Boolean> LEGEND_VISIBLE = new
    // // CssMetaData<AbstractStylable, Boolean>(
    // // "-fx-legend-visible", BooleanConverter.getInstance(), Boolean.TRUE) {
    // //
    // // @Override
    // // public boolean isSettable(final AbstractStylable chartPane) {
    // // return chartPane.legendVisible == null || !chartPane.legendVisible.isBound();
    // // }
    // //
    // // @SuppressWarnings("unchecked")
    // // @Override
    // // public StyleableProperty<Boolean> getStyleableProperty(final AbstractStylable chartPane) {
    // // return (StyleableProperty<Boolean>) chartPane.legendVisibleProperty();
    // // }
    // // };
    //
    // private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    //
    // static {
    // final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
    // // styleables.add(LEGEND_VISIBLE);
    // // styleables.add(LEGEND_SIDE);
    // STYLEABLES = Collections.unmodifiableList(styleables);
    // }
    // }
    //
    // public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
    // return StyleableProperties.STYLEABLES;
    // }
    //
    // @Override
    // public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    // return getClassCssMetaData();
    // }
}
