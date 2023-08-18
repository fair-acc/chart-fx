package io.fair_acc.chartfx;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.css.*;
import io.fair_acc.chartfx.ui.layout.TitleLabel;
import io.fair_acc.chartfx.ui.layout.ChartPane;
import io.fair_acc.chartfx.ui.layout.FullSizePane;
import io.fair_acc.chartfx.ui.*;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.EventSource;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.*;
import javafx.geometry.*;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.*;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AbstractAxis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.legend.Legend;
import io.fair_acc.chartfx.legend.spi.DefaultLegend;
import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.LabelledMarkerRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.utils.NoDuplicatesList;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Chart designed primarily to display data traces using DataSet interfaces which are more flexible and efficient than
 * the observable lists used by XYChart. Brief history: original design inspired by Oracle, extended by CERN (i.e.
 * plugin concept/zoomer), modified to mitigate JavaFX performance issues and extended renderer
 * concept/canvas-concept/interfaces/+more plugins by GSI. Refactored and re-write in 2018 to make it compatible with
 * GPLv3 which -- in the spirit of 'Ship of Theseus' -- makes it de-facto a new development. Contributions, bug-fixes,
 * and modifications are welcome. Hope you find this library useful and enjoy!
 *
 * @author original conceptual design by Oracle (2010, 2014)
 * @author hbraeun, rstein, major refactoring, re-implementation and re-design
 */
public abstract class Chart extends Region implements EventSource {

    // The chart has two different states, one that includes everything and is only ever on the JavaFX thread, and
    // a thread-safe one that receives dataSet updates and forwards them on the JavaFX thread.
    protected final BitState state = BitState.initDirty(this, BitState.ALL_BITS)
            .addChangeListener(ChartBits.ChartLayout, (src, bits) -> super.requestLayout())
            .addChangeListener(ChartBits.KnownMask, (src, bits) -> ensureJavaFxPulse());

    // DataSets are the only part that can potentially get updated from different threads, so we use a separate
    // state object that can handle multithreaded updates. The state always represents the current aggregate state
    // of all datasets, but the JavaFX change listener may not forward the dirty bits to the chart until the next frame.
    // This creates a race condition where delta bits that are already cleared in the datasets may end up dirtying the
    // chart and trigger an unnecessary redraw. To avoid this issue we ignore the delta and pass the current state.
    protected final BitState dataSetState = BitState.initDirtyMultiThreaded(this, BitState.ALL_BITS)
            .addChangeListener(FXUtils.runOnFxThread((src, deltaBits) -> state.setDirty(src.getBits())));

    private static final Logger LOGGER = LoggerFactory.getLogger(Chart.class);
    private static final String CHART_CSS = Objects.requireNonNull(Chart.class.getResource("chart.css")).toExternalForm();
    private static final CssPropertyFactory<Chart> CSS = new CssPropertyFactory<>(Region.getClassCssMetaData());
    private static final int DEFAULT_TRIGGER_DISTANCE = 50;
    protected static final boolean DEBUG = Boolean.getBoolean("chartfx.debug"); // for more verbose debugging
    protected final BooleanProperty showing = new SimpleBooleanProperty(this, "showing", false);


    /**
     * When true any data changes will be animated.
     */
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", false);
    // TODO: Check whether 'this' or chart contents need to be added
    /**
     * Animator for animating stuff on the chart
     */
    protected final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);

    protected final ObservableList<Axis> axesList = FXCollections.observableList(new NoDuplicatesList<>());
    private final Map<ChartPlugin, Pane> pluginPanes = new HashMap<>();
    private final ObservableList<ChartPlugin> plugins = FXCollections.observableList(new LinkedList<>());
    protected final ObservableList<DataSet> allDataSets = FXCollections.observableArrayList();
    private final ObservableList<Renderer> renderers = FXCollections.observableArrayList();

    // Inner canvas for the drawn content
    protected final ResizableCanvas canvas = StyleUtil.addStyles(new ResizableCanvas(), "chart-canvas");
    protected final Pane canvasForeground = StyleUtil.addStyles(new FullSizePane(), "chart-canvas-foreground");
    protected final Pane pluginsArea = FXUtils.createUnmanagedPane();

    // Area where plots get drawn
    protected final Pane plotBackground = StyleUtil.addStyles(new FullSizePane(), "chart-plot-background");
    protected final HiddenSidesPane plotArea = StyleUtil.addStyles(new HiddenSidesPane(), "chart-plot-area");
    protected final Pane plotForeGround = StyleUtil.addStyles(new FullSizePane(), "chart-plot-foreground");

    // Outer chart elements
    protected final ChartPane measurementPane = StyleUtil.addStyles(new ChartPane(), "chart-measurement-pane");
    protected final ChartPane titleLegendPane = StyleUtil.addStyles(new ChartPane(),"chart-title-pane", "chart-legend-pane");
    protected final ChartPane axesAndCanvasPane = StyleUtil.addStyles(new ChartPane(), "chart-content");

    // Outer area with hidden toolbars
    protected final HiddenSidesPane menuPane = new HiddenSidesPane();
    protected final ToolBarFlowPane toolBar = new ToolBarFlowPane(this);
    protected final BooleanProperty toolBarPinned = new SimpleBooleanProperty(this, "toolBarPinned", false);

    // Other nodes that need to be styled via CSS
    protected final StyleGroup styleableNodes = new StyleGroup(this, getChildren(), "chart");

    protected final TitleLabel titleLabel = StyleUtil.addStyles(new TitleLabel(), "chart-title");


    // Listeners
    protected final ListChangeListener<Renderer> rendererChangeListener = this::rendererChanged;
    protected final ListChangeListener<Axis> axesChangeListenerLocal = this::axesChangedLocal;
    protected final ListChangeListener<Axis> axesChangeListener = this::axesChanged;
    protected final ListChangeListener<DataSet> datasetChangeListener = this::datasetsChanged;
    protected final ListChangeListener<ChartPlugin> pluginsChangedListener = this::pluginsChanged;

    /**
     * The node to display as the Legend. Subclasses can set a node here to be displayed on a side as the legend. If no
     * legend is wanted then this can be set to null
     */
    private final ObjectProperty<Legend> legend = new SimpleObjectProperty<>(this, "legend", new DefaultLegend()) {
        private Legend oldLegend = get();
        {
            getTitleLegendPane().addSide(oldLegend.getSide(), oldLegend.getNode());
        }

        @Override
        protected void invalidated() {
            Legend newLegend = get();

            if (oldLegend != null) {
                getTitleLegendPane().remove(oldLegend.getNode());
            }

            if (newLegend != null) {
                getTitleLegendPane().addSide(newLegend.getSide(), newLegend.getNode());
            }
            super.set(newLegend);
            oldLegend = newLegend;
            fireInvalidated(ChartBits.ChartLegend);
        }
    };

    private final StyleableObjectProperty<ColorPalette> colorPalette = CSS.createEnumProperty(this, "colorPalette", ColorPalette.DEFAULT, true, ColorPalette.class);

    private final StyleableObjectProperty<Side> toolBarSide = CSS.createObjectProperty(this, "toolBarSide", Side.TOP, false,
            StyleConverter.getEnumConverter(Side.class), (oldVal, newVal) -> {
                AssertUtils.notNull("Side must not be null", newVal);
                // remove tool bar from potential other chart side pane locations
                menuPane.setTop(null);
                menuPane.setBottom(null);
                menuPane.setLeft(null);
                menuPane.setRight(null);
                switch (newVal) {
                case LEFT:
                    getToolBar().setOrientation(Orientation.VERTICAL);
                    menuPane.setLeft(getToolBar());
                    break;
                case RIGHT:
                    getToolBar().setOrientation(Orientation.VERTICAL);
                    menuPane.setRight(getToolBar());
                    break;
                case BOTTOM:
                    getToolBar().setOrientation(Orientation.HORIZONTAL);
                    menuPane.setBottom(getToolBar());
                    break;
                case TOP:
                default:
                    getToolBar().setOrientation(Orientation.HORIZONTAL);
                    menuPane.setTop(getToolBar());
                    break;
                }
                return (newVal);
            });

    /**
     * Creates a new default Chart instance.
     *
     * @param axes axes to be added to the chart
     */
    public Chart(Axis... axes) {
        for (int dim = 0; dim < axes.length; dim++) {
            final Axis axis = axes[dim];
            if (!(axis instanceof AbstractAxis)) {
                continue;
            }
            final AbstractAxis abstractAxis = (AbstractAxis) axis;
            if (abstractAxis.getDimIndex() < 0) {
                abstractAxis.setDimIndex(dim);
            }
        }

        // Register the layout hooks where chart elements get drawn
        FXUtils.registerLayoutHooks(this, this::runPreLayout, this::runPostLayout);

        // Setup listeners
        showing.bind(FXUtils.getShowingBinding(this));
        getRenderers().addListener(rendererChangeListener);
        getPlugins().addListener(pluginsChangedListener);
        getAxes().addListener(axesChangeListenerLocal);
        getAxes().addListener(axesChangeListener);

        // Apply color palette
        PropUtil.runOnChange(() -> {
            getColorPalette().applyPseudoClasses(this);
            applyCss(); // avoid extra pulse when set during CSS phase
        }, colorPalette);

        menuPane.setTriggerDistance(Chart.DEFAULT_TRIGGER_DISTANCE);
        plotBackground.toBack();
        plotForeGround.toFront();
        plotForeGround.setMouseTransparent(true);

        // hiddenPane.setTriggerDistance(DEFAULT_TRIGGER_DISTANCE);
        plotArea.triggerDistanceProperty().bindBidirectional(menuPane.triggerDistanceProperty());
        plotArea.setAnimationDelay(Duration.millis(500));
        // hiddenPane.setMouseTransparent(true);
        plotArea.setPickOnBounds(false);

        getCanvasForeground().setManaged(false);
        getCanvasForeground().setMouseTransparent(true);
        getCanvas().toFront();
        getCanvasForeground().toFront();
        pluginsArea.toFront();

        if (!canvas.isCache()) {
            canvas.setCache(true);
            canvas.setCacheHint(CacheHint.QUALITY);
        }

        // add default chart content ie. ToolBar and Legend
        // can be repositioned via setToolBarSide(...) and setLegendSide(...)
        titleLabel.focusTraversableProperty().bind(Platform.accessibilityActiveProperty());
        getTitleLegendPane().getChildren().add(titleLabel);

        // register listener in tool bar FlowPane
        toolBar.registerListener();
        menuPane.setTop(getToolBar());

        // Chart hierarchy
        // > style nodes
        // > menuPane (hidden toolbars that slide in from top/bottom)
        //   > measurement pane (labels/menus for working with data)
        //     > legend & title pane (static legend and title)
        //       > axes pane (x/y axes)
        //         > axes
        //         > plot background/foreground
        //         > plot content
        //           > hidden elements for zoom etc.
        //           > plot area
        //             > canvas (main)
        //             > canvas foreground
        //             > plugins
        var canvasArea = StyleUtil.addStyles(new FullSizePane(canvas, canvasForeground, pluginsArea), "chart-canvas-area");
        plotArea.setContent(canvasArea);
        axesAndCanvasPane.addCenter(plotBackground, plotArea, plotForeGround);
        titleLegendPane.addCenter(axesAndCanvasPane);
        measurementPane.addCenter(titleLegendPane);
        menuPane.setContent(measurementPane);
        getChildren().add(menuPane);

    }

    @Override
    public BitState getBitState() {
        return state;
    }

    @Override
    public String getUserAgentStylesheet() {
        return CHART_CSS;
    }

    /**
     * Play a animation involving the given keyframes. On every frame of the animation the chart will be relayed out
     *
     * @param keyFrames Array of KeyFrames to play
     */
    public void animate(final KeyFrame... keyFrames) {
        animator.animate(keyFrames);
    }

    public final BooleanProperty animatedProperty() {
        return animated;
    }

    /**
     * @return datasets attached to all renderers
     */
    public ObservableList<DataSet> getAllDatasets() {
        allDataSets.clear();
        for (Renderer renderer : renderers) {
            if (renderer instanceof LabelledMarkerRenderer) {
                continue;
            }
            allDataSets.addAll(renderer.getDatasets());
        }
        return allDataSets;
    }

    public ObservableList<Axis> getAxes() {
        return axesList;
    }

    public ChartPane getAxesAndCanvasPane() {
        return axesAndCanvasPane;
    }

    /**
     * @return the actual canvas the data is being drawn upon
     */
    public final Canvas getCanvas() {
        return canvas;
    }

    public final Pane getCanvasForeground() {
        return canvasForeground;
    }

    public Axis getFirstAxis(final Orientation orientation) {
        for (final Axis axis : getAxes()) {
            if (axis.getSide() == null) {
                continue;
            }
            switch (orientation) {
            case VERTICAL:
                if (axis.getSide().isVertical()) {
                    return axis;
                }
                break;
            case HORIZONTAL:
            default:
                if (axis.getSide().isHorizontal()) {
                    return axis;
                }
                break;
            }
        }
        // Add default axis if no suitable axis is available
        switch (orientation) {
        case HORIZONTAL:
            DefaultNumericAxis newXAxis = new DefaultNumericAxis("x-Axis");
            newXAxis.setSide(Side.BOTTOM);
            newXAxis.setDimIndex(DataSet.DIM_X);
            getAxes().add(newXAxis);
            return newXAxis;
        case VERTICAL:
        default:
            DefaultNumericAxis newYAxis = new DefaultNumericAxis("y-Axis");
            newYAxis.setSide(Side.LEFT);
            newYAxis.setDimIndex(DataSet.DIM_Y);
            getAxes().add(newYAxis);
            return newYAxis;
        }
    }

    public final Legend getLegend() {
        return legend.getValue();
    }

    public final ChartPane getMeasurementPane() {
        return measurementPane;
    }

    public final HiddenSidesPane getPlotArea() {
        return plotArea;
    }

    public final HiddenSidesPane getMenuPane() {
        return menuPane;
    }

    public final Pane getPlotBackground() {
        return plotBackground;
    }

    public final Pane getPlotForeground() {
        return plotForeGround;
    }

    /**
     * Returns a list of plugins added to this chart pane.
     *
     * @return a modifiable list of plugins
     */
    public final ObservableList<ChartPlugin> getPlugins() {
        return plugins;
    }

    /**
     * @return observable list of associated chart renderers
     */
    public ObservableList<Renderer> getRenderers() {
        return renderers;
    }

    public final String getTitle() {
        return titleProperty().get();
    }

    public final TitleLabel getTitleLabel() {
        return titleLabel;
    }

    public final ChartPane getTitleLegendPane() {
        return titleLegendPane;
    }

    public final FlowPane getToolBar() {
        return toolBar;
    }

    public final ObjectProperty<Side> getToolBarSideProperty() {
        return toolBarSide;
    }

    public final Side getToolBarSide() {
        return toolBarSideProperty().get();
    }

    public ColorPalette getColorPalette() {
        return colorPalette.get();
    }

    public StyleableObjectProperty<ColorPalette> colorPaletteProperty() {
        return colorPalette;
    }

    public void setColorPalette(ColorPalette colorPalette) {
        this.colorPalette.set(colorPalette);
    }

    /**
     * Indicates whether data changes will be animated or not.
     *
     * @return true if data changes will be animated and false otherwise.
     */
    public final boolean isAnimated() {
        return animated.get();
    }

    /**
     * @return true: if chart is being visible in Scene/Window
     */
    public boolean isShowing() {
        return showing.get();
    }

    public boolean isToolBarPinned() {
        return toolBarPinned.get();
    }

    /**
     * Explicit invalidation call for backwards compatibility that
     * replaced all sample calls to requestLayout().
     * <p>
     * requestLayout() shouldn't trigger an event because it would
     * cause many unnecessary redraws whenever a hidden node gets
     * animated into the chart (HiddenSidesPaneSkin::112).
     * <p>
     * TODO: get rid of this after updating the samples
     */
    public void invalidate() {
        fireInvalidated(ChartBits.ChartLayout, ChartBits.ChartCanvas);
    }

    protected void runPreLayout() {
        state.setDirty(dataSetState.clear());
        if (state.isClean()) {
            return;
        }

        // Update axis mapping in the renderers
        for (Renderer renderer : renderers) {
            renderer.updateAxes();
        }

        // Update legend
        if (state.isDirty(ChartBits.ChartLegend)) {
            updateLegend(getRenderers());
        }
        state.clear(ChartBits.ChartLegend);

        // Update data ranges
        final long start = ProcessingProfiler.getTimeStamp();
        ensureLockedDataSets();
        updateAxisRange(); // Update data ranges etc. to trigger anything that might need a layout
        ProcessingProfiler.getTimeDiff(start, "updateAxisRange()");

        // Update other components
        for (Renderer renderer : renderers) {
            renderer.runPreLayout();
            for (DataSetNode datasetNode : renderer.getDatasetNodes()) {
                datasetNode.runPreLayout();
            }
        }
        for (ChartPlugin plugin : plugins) {
            plugin.runPreLayout();
        }

    }

    @Override
    public void layoutChildren() {
        // Size all nodes to full size. Account for margin and border insets.
        final double x = snappedLeftInset();
        final double y = snappedTopInset();
        final double w = snapSizeX(getWidth()) - x - snappedRightInset();
        final double h = snapSizeY(getHeight()) - y - snappedBottomInset();
        for (Node child : getChildren()) {
            child.resizeRelocate(x, y, w, h);
        }

        // Note: size changes should maybe trigger a redraw, but this
        // creates problems with the HiddenSidesPane because for some
        // reason the size fluctuates a few pixels during animations.
        // Commenting this out prevents unnecessary redraws, and it
        // looks like actual size changes still trigger subsequent
        // events through other components (e.g. axes).
        // fireInvalidated(ChartBits.ChartCanvas);

        // Note: there are some rare corner cases, e.g., computing
        // the pref size of the scene (and the HiddenSidesPane),
        // that call for a layout without any dirty bits. It is also
        // possible that the layout triggers a resizing, so we may
        // need to lock the datasets here.
        if (state.isDirty()) {
            ensureLockedDataSets();
            layoutPluginsChildren();
        }

    }

    protected void runPostLayout() {
        // nothing to do
        if (state.isClean() && !hasLocked) {
            return;
        }

        ensureLockedDataSets();

        // Make sure that renderer axes that are not part of
        // the chart still produce an accurate axis transform.
        updateStandaloneRendererAxes();

        final long start = ProcessingProfiler.getTimeStamp();

        // Redraw the axes (they internally check dirty bits)
        for (Axis axis : axesList) {
            axis.drawAxis();
        }

        // Redraw legend icons
        // TODO: only update if the style actually changed
        legend.get().drawLegend();

        // Redraw the main canvas
        redrawCanvas();

        // Update other components
        for (Renderer renderer : renderers) {
            renderer.runPostLayout();
        }
        for (var plugin : plugins) {
            plugin.runPostLayout();
        }

        // Clear bits
        clearStates();

        // TODO: plugins etc., do locking
        ProcessingProfiler.getTimeDiff(start, "updateCanvas()");
    }

    protected void ensureLockedDataSets() {
        if (!hasLocked) {
            forEachDataSet(ds -> lockedDataSets.add(ds.lock().readLock()));
            hasLocked = true;
        }
    }

    protected void clearStates() {
        for (var renderer : getRenderers()) {
            if (renderer instanceof EventSource) {
                ((EventSource) renderer).getBitState().clear();
            }
        }

        for (var plugin : plugins) {
            if (plugin instanceof EventSource) {
                ((EventSource) plugin).getBitState().clear();
            }
        }

        state.clear();

        for (var ds : lockedDataSets) {
            for (AxisDescription axisDescription : ds.getAxisDescriptions()) {
                axisDescription.getBitState().clear();
            }
            ds.getBitState().clear(); // technically a 'write'
            ds.lock().readUnLock();
        }
        hasLocked = false;
        lockedDataSets.clear();
    }

    /**
     * Update axes that are not part of the SceneGraph and that would
     * otherwise not have a size. We could alternatively add renderer
     * axes to the chart, but that would not match previous behavior,
     * and it is probably a good idea to require it to be explicit.
     */
    private void updateStandaloneRendererAxes() {
        for (Renderer renderer : getRenderers()) {
            for (Axis axis : renderer.getAxes()) {
                if (axis instanceof AbstractAxis) {
                    var axisNode = (AbstractAxis) axis;
                    if (axisNode.getParent() == null) {
                        if (axis.getSide().isHorizontal()) {
                            axisNode.prefWidth(plotArea.getHeight());
                        } else {
                            axisNode.prefHeight(plotArea.getWidth());
                        }
                    }
                }
            }
        }
    }

    protected void forEachDataSet(Consumer<DataSet> action) {
        for (Renderer renderer : renderers) {
            for (DataSet dataset : renderer.getDatasets()) {
                action.accept(dataset);
            }
        }
    }

    private final List<DataSet> lockedDataSets = new ArrayList<>();
    private boolean hasLocked = false;

    public final ObjectProperty<Legend> legendProperty() {
        return legend;
    }

    @Deprecated // TODO: used in tests/examples. Should be replaced with getLegend().setVisible(value)?
    public final BooleanProperty legendVisibleProperty() {
        return getLegend().getNode().visibleProperty();
    }

    public final void setAnimated(final boolean value) {
        animated.set(value);
    }

    public final void setLegend(final Legend value) {
        legend.set(value);
    }

    public final void setLegendVisible(final boolean value) {
        getLegend().getNode().setVisible(value);
    }

    public final void setTitle(final String value) {
        titleProperty().set(value);
    }

    public Chart setToolBarPinned(boolean value) {
        toolBarPinned.set(value);
        return this;
    }

    public final void setToolBarSide(final Side value) {
        toolBarSide.set(value);
    }

    /**
     * @return property indicating if chart is actively visible in Scene/Window
     */
    public ReadOnlyBooleanProperty showingProperty() {
        return showing;
    }

    public final StringProperty titleProperty() {
        return titleLabel.textProperty();
    }

    public BooleanProperty toolBarPinnedProperty() {
        return toolBarPinned;
    }

    public final ObjectProperty<Side> toolBarSideProperty() {
        return toolBarSide;
    }

    // -------------- CONSTRUCTOR
    // --------------------------------------------------------------------------------------

    /**
     * Translates point from chart pane coordinates to the plot area coordinates.
     *
     * @param xCoord the x coordinate within XYChartPane coordinates system
     * @param yCoord the y coordinate within XYChartPane coordinates system
     * @return point in plot area coordinates
     */
    public final Point2D toPlotArea(final double xCoord, final double yCoord) {
        final Bounds plotAreaBounds = getCanvas().getBoundsInParent();
        return new Point2D(xCoord - plotAreaBounds.getMinX(), yCoord - plotAreaBounds.getMinY());
    }

    // -------------- METHODS
    // ------------------------------------------------------------------------------------------

    /**
     * update axes ranges (if necessary). This is supposed to be implemented in derived classes
     */
    public abstract void updateAxisRange();

    /**
     * Play the given animation on every frame of the animation the chart will be relayed out until the animation
     * finishes. So to add a animation to a chart, create a animation on data model, during layoutChartContent() map
     * data model to nodes then call this method with the animation.
     *
     * @param animation The animation to play
     */
    protected void animate(final Animation animation) {
        animator.animate(animation);
    }

    /**
     * add Chart specific axis handling (ie. placement around charts, add new DefaultNumericAxis if one is missing,
     * etc.)
     *
     * @param change the new axis change that is being added
     */
    protected abstract void axesChanged(final ListChangeListener.Change<? extends Axis> change);

    /**
     * add Chart specific axis handling (ie. placement around charts, add new DefaultNumericAxis if one is missing,
     * etc.)
     *
     * @param change the new axis change that is being added
     */
    protected void axesChangedLocal(final ListChangeListener.Change<? extends Axis> change) {
        while (change.next()) {
            for (Axis axis : change.getRemoved()) {
                // remove axis invalidation listener
                AssertUtils.notNull("to be removed axis is null", axis);
                axis.removeListener(state);
                removeAxisFromChildren(axis);
            }
            for (final Axis axis : change.getAddedSubList()) {
                // check if axis is associated with an existing renderer,
                // if yes -> throw an exception
                AssertUtils.notNull("to be added axis is null", axis);
                axis.addListener(state);
                addAxisToChildren(axis);
            }
        }
        fireInvalidated(ChartBits.ChartLayout, ChartBits.ChartAxes);
    }

    private boolean addAxisToChildren(Axis axis) {
        final Side side = axis.getSide();
        if (side == null) {
            throw new InvalidParameterException("axis '" + axis.getName() + "' has 'null' as side being set");
        }
        var children = getAxesAndCanvasPane().getChildren();
        if (axis instanceof Node && !children.contains(axis)) {
            getAxesAndCanvasPane().addSide(side, (Node) axis);
            return true;
        }
        return false;
    }

    private boolean removeAxisFromChildren(Axis axis) {
        var children = getAxesAndCanvasPane().getChildren();
        if (axis instanceof Node) {
            children.remove((Node) axis);
            return true;
        }
        return false;
    }

    protected void datasetsChanged(final ListChangeListener.Change<? extends DataSet> change) {
        FXUtils.assertJavaFxThread();
        while (change.next()) {
            for (final DataSet set : change.getRemoved()) {
                set.removeListener(dataSetState);
            }
            for (final DataSet set : change.getAddedSubList()) {
                set.addListener(dataSetState);
            }
        }

        // set global indices
        updateDataSetIndices();

        // Rebuild legend (modifies SceneGraph and needs to be done before styling)
        fireInvalidated(ChartBits.ChartLayout, ChartBits.ChartDataSets, ChartBits.ChartLegend);
    }

    protected void updateDataSetIndices() {
        int globalIndexOffset = 0;
        for (Renderer renderer : renderers) {
            renderer.setGlobalIndexOffset(globalIndexOffset);
            globalIndexOffset += renderer.getDatasetNodes().size();
        }
    }

    /**
     * @return unmodifiable list of the controls css styleable properties
     * @since JavaFX 8.0
     */
    @Deprecated // A remnant of extending Control. Do we need it?
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return Chart.getClassCssMetaData();
    }

    protected void layoutPluginsChildren() {
        plugins.forEach(ChartPlugin::layoutChildren);
    }

    protected void pluginAdded(final ChartPlugin plugin) {
        plugin.setChart(Chart.this);
        final Pane pane = FXUtils.createUnmanagedPane();
        Bindings.bindContent(pane.getChildren(), plugin.getChartChildren());
        pluginPanes.put(plugin, pane);
    }

    // -------------- STYLESHEET HANDLING
    // ------------------------------------------------------------------------------

    protected void pluginRemoved(final ChartPlugin plugin) {
        plugin.setChart(null);
        final Pane pane = pluginPanes.remove(plugin);
        Bindings.unbindContent(pane, plugin.getChartChildren());
        pane.getChildren().clear();
        pluginsArea.getChildren().remove(pane);
    }

    protected void pluginsChanged(final ListChangeListener.Change<? extends ChartPlugin> change) {
        while (change.next()) {
            change.getRemoved().forEach(this::pluginRemoved);
            change.getAddedSubList().forEach(this::pluginAdded);
        }
        updatePluginsArea();
    }

    /**
     * (re-)draw canvas (if necessary). This is supposed to be implemented in derived classes
     */
    protected abstract void redrawCanvas();

    // -------------- LISTENER HANDLING
    // ------------------------------------------------------------------------------

    protected void rendererChanged(final ListChangeListener.Change<? extends Renderer> change) {
        FXUtils.assertJavaFxThread();
        while (change.next()) {

            // handle added renderer
            for (Renderer renderer : change.getAddedSubList()) {
                for (DataSet dataset : renderer.getDatasets()) {
                    dataset.addListener(dataSetState);
                }
                renderer.getDatasets().addListener(datasetChangeListener);
                renderer.setChart(this);
                var node = renderer.getNode();
                if (node != null && !styleableNodes.getChildren().contains(node)) {
                    styleableNodes.getChildren().add(node);
                }
            }

            // handle removed renderer
            for (Renderer renderer : change.getRemoved()) {
                for (DataSet dataset : renderer.getDatasets()) {
                    dataset.removeListener(dataSetState);
                }
                renderer.getDatasets().removeListener(datasetChangeListener);
                styleableNodes.getChildren().remove(renderer.getNode());
                renderer.setChart(null);
            }

        }

        updateDataSetIndices();

        // reset change to allow derived classes to add additional listeners to renderer changes
        change.reset();
        fireInvalidated(ChartBits.ChartLayout, ChartBits.ChartRenderers, ChartBits.ChartLegend);

    }

    /**
     * This is used to check if any given animation should run. It returns true if animation is enabled and the node is
     * visible and in a scene.
     *
     * @return true if should animate
     */
    protected final boolean shouldAnimate() {
        return isAnimated() && getScene() != null;
    }

    protected void updateLegend(final List<Renderer> renderers) {
        final Legend legend = getLegend();
        if (legend == null) {
            return;
        }
        legend.updateLegend(renderers);
    }

    protected void updatePluginsArea() {
        var pluginChildren = plugins.stream().map(pluginPanes::get).collect(Collectors.toList());
        pluginsArea.getChildren().setAll(pluginChildren);
        fireInvalidated(ChartBits.ChartPlugins);
    }

    /**
     * Dataset changes do not trigger a pulse, so in order
     * to ensure a redraw we manually request a layout. We
     * use an unmanaged node without a layout implementation,
     * so that we don't accidentally do unnecessary work.
     */
    private void ensureJavaFxPulse() {
        styleableNodes.requestLayout();
    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS.getCssMetaData();
    }

}
