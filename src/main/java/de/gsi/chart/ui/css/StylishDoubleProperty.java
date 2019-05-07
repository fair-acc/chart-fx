package de.gsi.chart.ui.css;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;

public class StylishDoubleProperty extends SimpleStyleableDoubleProperty {
    protected Runnable invalidateAction;

    /**
     * The constructor of the {@code StylishDoubleProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this
     *            {@code StylishDoubleProperty}
     * @param bean
     *            the bean of this {@code BooleanProperty}
     * @param name
     *            the name of this {@code BooleanProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     * @param invalidateAction
     *            lambda expression executed in invalidated
     */
    public StylishDoubleProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData,
            @NamedArg("bean") Object bean, @NamedArg("name") String name, @NamedArg("initialValue") Double initialValue,
            Runnable invalidateAction) {
        super(cssMetaData, bean, name, initialValue);
        this.invalidateAction = invalidateAction;
    }

    @Override
    protected void invalidated() {
        invalidateAction.run();
    }
}