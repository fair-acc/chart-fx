package de.gsi.chart.renderer.spi;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.datareduction.MaxDataReducer;
import de.gsi.chart.renderer.datareduction.RamanDouglasPeukerDataReducer;
import de.gsi.chart.renderer.datareduction.VisvalingamMaheswariWhyattDataReducer;
import de.gsi.dataset.utils.AssertUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;

/**
 * simple class to move the various parameters out of the class containing the algorithms uses the shadow field pattern
 * to minimise memory usage (lots of boiler-plate code ... sorry)
 *
 * @author rstein
 * @param <R> generic object type for renderer parameter
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.TooManyFields", "PMD.ExcessivePublicCount" }) // designated purpose of this class
public abstract class AbstractErrorDataSetRendererParameter<R extends AbstractErrorDataSetRendererParameter<R>>
        extends AbstractDataSetManagement<R> {

    // intensity fading factor per stage
    protected static final double DEFAULT_HISTORY_INTENSITY_FADING = 0.65;
    private final ObjectProperty<ErrorStyle> errorStyle = new SimpleObjectProperty<>(this, "errorStyle",
            ErrorStyle.ERRORCOMBO);
    private final ObjectProperty<RendererDataReducer> rendererDataReducer = new SimpleObjectProperty<>(this,
            "rendererDataReducer", new DefaultDataReducer());
    private final BooleanProperty pointReduction = new SimpleBooleanProperty(this, "pointReduction", true);
    private final IntegerProperty dashSize = new SimpleIntegerProperty(this, "dashSize", 3);
    private final IntegerProperty minRequiredReductionSize = new SimpleIntegerProperty(this, "minRequiredReductionSize",
            5);
    private final DoubleProperty markerSize = new SimpleDoubleProperty(this, "markerSize", 1.5);
    private final BooleanProperty drawMarker = new SimpleBooleanProperty(this, "drawMarker", true);
    private final ObjectProperty<LineStyle> polyLineStyle = new SimpleObjectProperty<>(this, "polyLineStyle",
            LineStyle.NORMAL);
    private final BooleanProperty drawBars = new SimpleBooleanProperty(this, "drawBars", false);
    private final BooleanProperty shiftBar = new SimpleBooleanProperty(this, "shiftBar", true);
    private final IntegerProperty shiftBarOffset = new SimpleIntegerProperty(this, "shiftBarOffset", 3);
    private final BooleanProperty dynamicBarWidth = new SimpleBooleanProperty(this, "dynamicBarWidth", true);
    private final DoubleProperty barWidthPercentage = new SimpleDoubleProperty(this, "barWidthPercentage", 70.0);
    private final IntegerProperty barWidth = new SimpleIntegerProperty(this, "barWidth", 5);
    private final BooleanProperty parallelImplementation = new SimpleBooleanProperty(this, "parallelImplementation",
            false);
    private final DoubleProperty intensityFading = new SimpleDoubleProperty(this, "intensityFading",
            AbstractErrorDataSetRendererParameter.DEFAULT_HISTORY_INTENSITY_FADING);
    private final BooleanProperty drawBubbles = new SimpleBooleanProperty(this, "drawBubbles", false);

    /**
     * @return the instance of this AbstractErrorDataSetRendererParameter.
     */
    @Override
    protected abstract R getThis();

    protected R bind(final R other) {
        errorStyleProperty().bind(other.errorStyleProperty());
        pointReductionProperty().bind(other.pointReductionProperty());
        dashSizeProperty().bind(other.dashSizeProperty());
        minRequiredReductionSizeProperty().bind(other.minRequiredReductionSizeProperty());
        markerSizeProperty().bind(other.markerSizeProperty());
        drawMarkerProperty().bind(other.drawMarkerProperty());
        polyLineStyleProperty().bind(other.polyLineStyleProperty());
        drawBarsProperty().bind(other.drawBarsProperty());
        drawBubblesProperty().bind(other.drawBubblesProperty());
        shiftBarProperty().bind(other.shiftBarProperty());
        shiftBarOffsetProperty().bind(other.shiftBarOffsetProperty());
        dynamicBarWidthProperty().bind(other.dynamicBarWidthProperty());
        barWidthPercentageProperty().bind(other.barWidthPercentageProperty());
        barWidthProperty().bind(other.barWidthProperty());
        getAxes().setAll(other.getAxes());
        intensityFadingProperty().bind(other.intensityFadingProperty());

        // Bindings.bindContent(axesList(), other.axesList());

        other.getAxes().addListener((ListChangeListener<? super Axis>) change -> {
            while (change.next()) {
                getAxes().addAll(change.getAddedSubList());
                getAxes().removeAll(change.getRemoved());
            }
        });
        return getThis();
    }

    protected R unbind() {
        errorStyleProperty().unbind();
        pointReductionProperty().unbind();
        dashSizeProperty().unbind();
        minRequiredReductionSizeProperty().unbind();
        markerSizeProperty().unbind();
        drawMarkerProperty().unbind();
        polyLineStyleProperty().unbind();
        drawBarsProperty().unbind();
        drawBubblesProperty().unbind();
        shiftBarProperty().unbind();
        shiftBarOffsetProperty().unbind();
        dynamicBarWidthProperty().unbind();
        barWidthPercentageProperty().unbind();
        barWidthProperty().unbind();
        intensityFadingProperty().unbind();

        return getThis();
    }

    /**
     * sets the error bar/surface plotting style ErrorBarRenderer.ESTYLE_NONE: no errors are drawn
     * ErrorBarRenderer.ESTYLE_BAR: error bars are drawn (default) ErrorBarRenderer.ESTYLE_SURFACE: error surface is
     * drawn
     *
     * @param style ErrorStyle @see ErrorStyle enum
     * @return itself (fluent design)
     */
    public R setErrorType(final ErrorStyle style) {
        errorStyle.set(style);
        return getThis();
    }

    /**
     * @return returns error plotting style
     * @see ErrorDataSetRenderer#setErrorType(ErrorStyle style) for details
     */
    public ErrorStyle getErrorType() {
        return errorStyle.get();
    }

    /**
     * sets the error bar/surface plotting style ErrorBarRenderer.ESTYLE_NONE: no errors are drawn
     * ErrorBarRenderer.ESTYLE_BAR: error bars are drawn (default) ErrorBarRenderer.ESTYLE_SURFACE: error surface is
     * drawn
     *
     * @return the errorStyle
     */
    public ObjectProperty<ErrorStyle> errorStyleProperty() {
        return errorStyle;
    }

    /**
     * sets the data reduction algorithm: possibly implementations are<br>
     * <ul>
     * <li>{@link DefaultDataReducer} (default)</li>
     * <li>{@link MaxDataReducer} (a simple down-sampling algorithm, returning fixed number of max. 1000 points)</li>
     * <li>{@link RamanDouglasPeukerDataReducer}</li>
     * <li>{@code DouglasPeukerDataReducer}</li>
     * <li>{@link VisvalingamMaheswariWhyattDataReducer} (being developed)</li>
     * </ul>
     *
     * @return the rendererDataReducerProperty
     */
    public ObjectProperty<RendererDataReducer> rendererDataReducerProperty() {
        return rendererDataReducer;
    }

    /**
     * @see #rendererDataReducerProperty()
     * @return the active data set reducer algorithm
     */
    public RendererDataReducer getRendererDataReducer() {
        return rendererDataReducer.get();
    }

    /**
     * @see #rendererDataReducerProperty()
     * @param algorithm the new data reducing algorithm to be set (null -&gt; {@link DefaultDataReducer})
     * @return itself (fluent design)
     */
    public R getRendererDataReducer(final RendererDataReducer algorithm) {
        if (algorithm == null) {
            rendererDataReducer.set(new DefaultDataReducer());
        } else {
            rendererDataReducer.set(algorithm);
        }
        return getThis();
    }

    /**
     * Sets whether superfluous points, otherwise drawn on the same pixel area, are merged and represented by the
     * multiple point average.
     *
     * @return true if point reduction is on (default) else false.
     */
    public boolean isReducePoints() {
        return pointReduction.get();
    }

    /**
     * Sets whether superfluous points, otherwise drawn on the same pixel area, are merged and represented by the
     * multiple point average.
     *
     * @param state true if data points are supposed to be reduced
     * @return itself (fluent design)
     */
    public R setPointReduction(final boolean state) {
        pointReduction.set(state);
        return getThis();
    }

    /**
     * Sets whether superfluous points, otherwise drawn on the same pixel area, are merged and represented by the
     * multiple point average.
     *
     * @return true if data points are supposed to be reduced
     */
    public BooleanProperty pointReductionProperty() {
        return pointReduction;
    }

    /**
     * Returns the <code>dashSize</code>.
     *
     * @return the <code>dashSize</code>.
     */
    public int getDashSize() {
        return dashSize.get();
    }

    /**
     * Sets the <code>dashSize</code> to the specified value. The dash is the horizontal line painted at the ends of the
     * vertical line. It is not painted if set to 0.
     *
     * @param dashSize the <code>dashSize</code> to set.
     * @return itself (fluent design)
     */
    public R setDashSize(final int dashSize) {
        AssertUtils.gtEqThanZero("dash size", dashSize);
        this.dashSize.setValue(dashSize);
        return getThis();
    }

    public IntegerProperty dashSizeProperty() {
        return dashSize;
    }

    /**
     * @return the minimum number of samples before performing data reduction
     */
    public int getMinRequiredReductionSize() {
        return minRequiredReductionSize.get();
    }

    /**
     * @param size the minimum number of samples before performing data reduction
     * @return itself (fluent design)
     */
    public R setMinRequiredReductionSize(final int size) {
        minRequiredReductionSize.setValue(size);
        return getThis();
    }

    public IntegerProperty minRequiredReductionSizeProperty() {
        return minRequiredReductionSize;
    }

    /**
     * Returns the <code>markerSize</code>.
     *
     * @return the <code>markerSize</code>.
     */
    public double getMarkerSize() {
        return markerSize.get();
    }

    /**
     * Sets the <code>markerSize</code> to the specified value.
     *
     * @param size the <code>markerSize</code> to set.
     * @return itself (fluent design)
     */
    public R setMarkerSize(final double size) {
        AssertUtils.gtEqThanZero("marker size ", size);
        markerSize.setValue(size);
        return getThis();
    }

    public DoubleProperty markerSizeProperty() {
        return markerSize;
    }

    /**
     * Returns the <code>intensityFading</code>.
     *
     * @return the <code>intensityFading</code>.
     */
    public double getIntensityFading() {
        return intensityFading.get();
    }

    /**
     * Sets the <code>intensityFading</code> to the specified value.
     *
     * @param size the <code>intensityFading</code> to set.
     * @return itself (fluent design)
     */
    public R setIntensityFading(final double size) {

        intensityFading.setValue(size);
        return getThis();
    }

    public DoubleProperty intensityFadingProperty() {
        return intensityFading;
    }

    /**
     * @return true if point reduction is on (default) else false.
     */
    public boolean isDrawMarker() {
        return drawMarker.get();
    }

    /**
     * @param state true -&gt; draws markers
     * @return itself (fluent design)
     */
    public R setDrawMarker(final boolean state) {
        drawMarker.set(state);
        return getThis();
    }

    /**
     * @return the drawMarker state
     */
    public BooleanProperty drawMarkerProperty() {
        return drawMarker;
    }

    /**
     * @return true if bars from the data points to the y==0 axis shall be drawn
     */
    public boolean isDrawBars() {
        return drawBars.get();
    }

    /**
     * @param state true if bars from the data points to the y==0 axis shall be drawn
     * @return itself (fluent design)
     */
    public R setDrawBars(final boolean state) {
        drawBars.set(state);
        return getThis();
    }

    /**
     * @return the drawBars state
     */
    public BooleanProperty drawBarsProperty() {
        return drawBars;
    }

    /**
     * @return true if bars drawn to the y==0 axis shall be horizontally shifted for each DataSet
     */
    public boolean isShiftBar() {
        return shiftBar.get();
    }

    /**
     * @param state true if bars drawn to the y==0 axis shall be horizontally shifted for each DataSet
     * @return itself (fluent design)
     */
    public R setShiftBar(final boolean state) {
        shiftBar.set(state);
        return getThis();
    }

    /**
     * @return the shiftBar state
     */
    public BooleanProperty shiftBarProperty() {
        return shiftBar;
    }

    /**
     * Returns the <code>shiftBarOffset</code>.
     *
     * @return the <code>shiftBarOffset</code>.
     */
    public int getShiftBarOffset() {
        return shiftBarOffset.get();
    }

    /**
     * Sets the <code>shiftBarOffset</code> to the specified value.
     *
     * @param shiftBarOffset the <code>shiftBarOffset</code> to set.
     * @return itself (fluent design)
     */
    public R setshiftBarOffset(final int shiftBarOffset) {
        AssertUtils.gtEqThanZero("shiftBarOffset", shiftBarOffset);
        this.shiftBarOffset.setValue(shiftBarOffset);
        return getThis();
    }

    public IntegerProperty shiftBarOffsetProperty() {
        return shiftBarOffset;
    }

    /**
     * @return true whether the width of bars drawn to the '0' shall be dynamically to the shown axis width
     */
    public boolean isDynamicBarWidth() {
        return dynamicBarWidth.get();
    }

    /**
     * @param state true whether the width of bars drawn to the '0' shall be dynamically to the shown axis width
     * @return itself (fluent design)
     */
    public R setDynamicBarWidth(final boolean state) {
        dynamicBarWidth.set(state);
        return getThis();
    }

    /**
     * @return the dynamicBarWidth state
     */
    public BooleanProperty dynamicBarWidthProperty() {
        return dynamicBarWidth;
    }

    /**
     * @return the <code>barWidthPercentage</code> of the total X space should be taken to paint // bars.
     */
    public double getBarWidthPercentage() {
        return barWidthPercentage.get();
    }

    /**
     * @param size the <code>barWidthPercentage</code> of the total X space should be taken to paint
     * @return itself (fluent design)
     */
    public R setBarWidthPercentage(final double size) {
        AssertUtils.gtEqThanZero("barWidthPercentage", size);
        barWidthPercentage.setValue(size);
        return getThis();
    }

    public DoubleProperty barWidthPercentageProperty() {
        return barWidthPercentage;
    }

    /**
     * @return the <code>barWidth</code> in case of constant width bards being drawn @see setDynamicBarWidth()
     */
    public int getBarWidth() {
        return barWidth.get();
    }

    /**
     * @param barWidth the <code>barWidth</code> in case of constant width bards being drawn @see setDynamicBarWidth()
     * @return itself (fluent design)
     */
    public R setBarWidth(final int barWidth) {
        AssertUtils.gtEqThanZero("barWidth", barWidth);
        this.barWidth.setValue(barWidth);
        return getThis();
    }

    public IntegerProperty barWidthProperty() {
        return barWidth;
    }

    /**
     * whether renderer should aim at parallelising sub-functionalities
     *
     * @return true if renderer is parallelising sub-functionalities
     */
    public boolean isParallelImplementation() {
        return parallelImplementation.get();
    }

    /**
     * Sets whether renderer should aim at parallelising sub-functionalities
     *
     * @param state true if renderer is parallelising sub-functionalities
     * @return itself (fluent design)
     */
    public R setParallelImplementation(final boolean state) {
        parallelImplementation.set(state);
        return getThis();
    }

    /**
     * Sets whether renderer should aim at parallelising sub-functionalities
     *
     * @return true if data points are supposed to be reduced
     */
    public BooleanProperty parallelImplementationProperty() {
        return parallelImplementation;
    }

    /**
     * whether renderer should draw no, simple (point-to-point), stair-case, Bezier, ... lines
     *
     * @return LineStyle
     */
    public LineStyle getPolyLineStyle() {
        return polyLineStyleProperty().get();
    }

    /**
     * Sets whether renderer should draw no, simple (point-to-point), stair-case, Bezier, ... lines
     *
     * @param style draw no, simple (point-to-point), stair-case, Bezier, ... lines
     * @return itself (fluent design)
     */
    public R setPolyLineStyle(final LineStyle style) {
        polyLineStyleProperty().set(style);
        return getThis();
    }

    /**
     * Sets whether renderer should draw no, simple (point-to-point), stair-case, Bezier, ... lines
     *
     * @return property
     */
    public ObjectProperty<LineStyle> polyLineStyleProperty() {
        return polyLineStyle;
    }

    /**
     * @return true if bubbles shall be draw
     */
    public boolean isDrawBubbles() {
        return drawBubbles.get();
    }

    /**
     * @param state true if bubbles shall be draw
     * @return itself (fluent design)
     */
    public R setDrawBubbles(final boolean state) {
        drawBubbles.set(state);
        return getThis();
    }

    /**
     * @return the drawBubbles property
     */
    public BooleanProperty drawBubblesProperty() {
        return drawBubbles;
    }
}
