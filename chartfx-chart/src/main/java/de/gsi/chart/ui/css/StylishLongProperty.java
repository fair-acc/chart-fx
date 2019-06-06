package de.gsi.chart.ui.css;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableLongProperty;
import javafx.css.Styleable;

/**
 * 
 * Short-hand to reduce boiler-plate type code of customisation of
 * SimpleStyleableLongProperty to always include an axis re-layout.
 * 
 * N.B. Also, the warning of inheriting more than 'n' generations is thrown only
 * once this way.
 * 
 * @author rstein
 *
 */
public class StylishLongProperty extends SimpleStyleableLongProperty {
    protected Runnable invalidateAction;

    /**
     * The constructor of the {@code StylishLongProperty}.
     *
     * @param cssMetaData
     *            the CssMetaData associated with this {@code StyleableProperty}
     * @param bean
     *            the bean of this {@code BooleanProperty}
     * @param name
     *            the name of this {@code BooleanProperty}
     * @param initialValue
     *            the initial value of the wrapped {@code Object}
     * @param invalidateAction
     *            lambda expression executed in invalidated
     */
    public StylishLongProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData,
            @NamedArg("bean") Object bean, @NamedArg("name") String name, @NamedArg("initialValue") Long initialValue,
            Runnable invalidateAction) {
        super(cssMetaData, bean, name, initialValue);
        this.invalidateAction = invalidateAction;
    }

    @Override
    protected void invalidated() {
        invalidateAction.run();
    }
}
