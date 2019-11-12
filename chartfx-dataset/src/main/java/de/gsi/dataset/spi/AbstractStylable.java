package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;

/**
 * Class to define CSS-based style features N.B. needed for DataSet and rendering styling
 *
 * @author rstein
 * 
 * @param <D> java generics handling of DataSet for derived classes (needed for fluent design)
 */
public abstract class AbstractStylable<D extends DataSet> implements DataSet {
    private static final long serialVersionUID = 1L;
    private String style = "";
    private String styleClass = ""; // TODO: check whether this is needed

    AbstractStylable() {
        super();
    }

    /**
     * A string representation of the CSS style associated with this specific {@code Node}. This is analogous to the
     * "style" attribute of an HTML element. Note that, like the HTML style attribute, this variable contains style
     * properties and values and not the selector portion of a style rule.
     */
    @Override
    public String getStyle() {
        return style;
    }

    @SuppressWarnings("unchecked")
    protected D getThis() {
        return (D) this;
    }

    @Override
    public D setStyle(final String style) {
        this.style = style;
        return getThis();
    }
}
