package de.gsi.chart.ui.css;

import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;

/**
 * 
 * Short-hand to reduce boiler-plate type code of customisation of SimpleStyleableBooleanProperty to always include an
 * axis re-layout.
 * 
 * N.B. Also, the warning of inheriting more than 'n' generations is thrown only once this way.
 * 
 * @author rstein
 *
 */
public class StylishBooleanProperty extends SimpleStyleableBooleanProperty {
    protected final Runnable preInvalidateAction;
    protected final Runnable postInvalidateAction;
    protected final BinaryOperator<Boolean> filter;

    /**
     * The constructor of the {@code StylishBooleanProperty}.
     *
     * @param cssMetaData the CssMetaData associated with this {@code StyleableProperty}
     * @param bean the bean of this {@code BooleanProperty}
     * @param name the name of this {@code BooleanProperty}
     * @param initialValue the initial value of the wrapped {@code Object}
     * @param filter Possibility to modify the new value based on old and updated value
     * @param invalidateActions lambda expressions executed after and before invalidation
     */
    public StylishBooleanProperty(@NamedArg("cssMetaData") CssMetaData<? extends Styleable, Boolean> cssMetaData,
            @NamedArg("bean") Object bean, @NamedArg("name") String name,
            @NamedArg("initialValue") boolean initialValue, BinaryOperator<Boolean> filter,
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
    public void set(boolean v) {
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