package de.gsi.chart.ui.css;

import java.util.function.BinaryOperator;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;

public class StylishObjectProperty<T> extends SimpleStyleableObjectProperty<T> {
    protected final Runnable preInvalidateAction;
    protected final Runnable postInvalidateAction;
    protected final BinaryOperator<T> filter;

    /**
     * The constructor of the {@code StylishObjectProperty}.
     *
     * @param cssMetaData the CssMetaData associated with this {@code StylishObjectProperty}
     * @param bean the bean of this {@code ObjectProperty}
     * @param name the name of this {@code ObjectProperty}
     * @param initialValue the initial value of the wrapped {@code Object}
     * @param filter Possibility to modify the new value based on old and updated value
     * @param invalidateActions lambda expressions executed after and before invalidation
     */
    public StylishObjectProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, T> cssMetaData, @NamedArg("bean") Object bean,
            @NamedArg("name") String name, @NamedArg("initialValue") T initialValue, BinaryOperator<T> filter,
            Runnable... invalidateActions) {
        super(cssMetaData, bean, name, initialValue);
        this.filter = filter;
        if (invalidateActions.length > 2) {
            throw new IllegalArgumentException("Only post- and pre invalidation actions allowed, but more than two actions supplied");
        }
        this.postInvalidateAction = invalidateActions.length > 0 ? invalidateActions[0] : null;
        this.preInvalidateAction = invalidateActions.length > 1 ? invalidateActions[0] : null;
    }

    @Override
    public void set(T v) {
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
