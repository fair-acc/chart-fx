package de.gsi.chart.ui.css;

import java.util.function.DoubleBinaryOperator;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;

public class StylishDoubleProperty extends SimpleStyleableDoubleProperty {
    protected final Runnable preInvalidateAction;
    protected final Runnable postInvalidateAction;
    protected final DoubleBinaryOperator filter;

    /**
     * The constructor of the {@code StylishDoubleProperty}.
     *
     * @param cssMetaData the CssMetaData associated with this {@code StylishDoubleProperty}
     * @param bean the bean of this {@code DoubleProperty}
     * @param name the name of this {@code DoubleProperty}
     * @param initialValue the initial value of the wrapped {@code Object}
     * @param filter Possibility to modify the new value based on old and updated value
     * @param invalidateActions lambda expressions executed after and before invalidation
     */
    public StylishDoubleProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Number> cssMetaData, @NamedArg("bean") Object bean,
            @NamedArg("name") String name, @NamedArg("initialValue") double initialValue, DoubleBinaryOperator filter,
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
    public void set(double v) {
        if (preInvalidateAction != null) {
            preInvalidateAction.run();
        }
        if (filter == null) {
            super.set(v);
        } else {
            super.set(filter.applyAsDouble(get(), v));
        }
        if (postInvalidateAction != null) {
            postInvalidateAction.run();
        }
    }
}
