package de.gsi.chart.ui.css;

import java.util.function.BinaryOperator;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableStringProperty;
import javafx.css.Styleable;

/**
 * Short-hand to reduce boiler-plate type code of customisation of {@code SimpleStyleableStringProperty} to always
 * include an axis re-layout. N.B. Also, the warning of inheriting more than 'n' generations is thrown only once this
 * way.
 * 
 * @author rstein
 */
public class StylishStringProperty extends SimpleStyleableStringProperty {
    protected final Runnable preInvalidateAction;
    protected final Runnable postInvalidateAction;
    protected final BinaryOperator<String> filter;

    /**
     * The constructor of the {@code StylishStringProperty}.
     *
     * @param cssMetaData the CssMetaData associated with this {@code StylishObjectProperty}
     * @param bean the bean of this {@code ObjectProperty}
     * @param name the name of this {@code ObjectProperty}
     * @param initialValue the initial value of the wrapped {@code Object}
     * @param filter Possibility to modify the new value based on old and updated value
     * @param invalidateActions lambda expressions executed after and before invalidation
     */
    public StylishStringProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, String> cssMetaData, @NamedArg("bean") Object bean,
            @NamedArg("name") String name, @NamedArg("initialValue") String initialValue, BinaryOperator<String> filter, Runnable... invalidateActions) {
        super(cssMetaData, bean, name, initialValue);
        this.filter = filter;
        if (invalidateActions.length > 2) {
            throw new IllegalArgumentException("Only post- and pre invalidation actions allowed, but more than two actions supplied");
        }
        this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
        this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[0] : null;
    }

    @Override
    public void set(String v) {
        if (preInvalidateAction != null) {
            preInvalidateAction.run();
        }
        if (filter == null) {
            super.set(v);
        } else {
            super.set(filter.apply(get(), v));
        }
        if (postInvalidateAction != null) {
            postInvalidateAction.run();
        }
    }
}
