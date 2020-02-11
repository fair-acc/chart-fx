package de.gsi.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.legend.Legend;
import de.gsi.chart.legend.spi.DefaultLegend;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.LabelledMarkerRenderer;
import de.gsi.chart.ui.ChartLayoutAnimator;
import de.gsi.chart.ui.HiddenSidesPane;
import de.gsi.chart.ui.ResizableCanvas;
import de.gsi.chart.ui.ToolBarFlowPane;
import de.gsi.chart.ui.css.StylishBooleanProperty;
import de.gsi.chart.ui.css.StylishObjectProperty;
import de.gsi.chart.ui.geometry.Corner;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.utils.AssertUtils;
import de.gsi.dataset.utils.NoDuplicatesList;
import de.gsi.dataset.utils.ProcessingProfiler;

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
public abstract class Chart extends HiddenSidesPane implements Observable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Chart.class);
    private static final String CHART_CSS = Chart.class.getResource("chart.css").toExternalForm();
    private static final int DEFAULT_TRIGGER_DISTANCE = 50;
    protected static boolean DEBUG; // for more verbose debugging

    protected BooleanBinding showingBinding;
    protected final BooleanProperty showing = new SimpleBooleanProperty(this, "showing", false);
    protected final ChangeListener<? super Boolean> showingListener = (ch2, o, n) -> showing.set(n);
    /** When true any data changes will be animated. */
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);
    // TODO: Check whether 'this' or chart contents need to be added
    /** Animator for animating stuff on the chart */
    protected final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);
    /**
     * When true the chart will display a legend if the chart implementation supports a legend.
     */
    private final BooleanProperty legendVisible = new StylishBooleanProperty(StyleableProperties.LEGEND_VISIBLE, this,
            "legendVisible", true, () -> {
                updateLegend(getDatasets(), getRenderers());
                requestLayout();
            });

    // isCanvasChangeRequested is a recursion guard to update canvas only once
    protected boolean isCanvasChangeRequested;
    // layoutOngoing is a recursion guard to update canvas only once
    protected boolean layoutOngoing;
    protected final ObservableList<Axis> axesList = FXCollections.observableList(new NoDuplicatesList<Axis>());
    private final Map<ChartPlugin, Group> pluginGroups = new ConcurrentHashMap<>();
    private final ObservableList<ChartPlugin> plugins = FXCollections.observableList(new LinkedList<>());
    private final ObservableList<DataSet> datasets = FXCollections.observableArrayList();
    protected final ObservableList<DataSet> allDataSets = FXCollections.observableArrayList();
    protected final List<InvalidationListener> listeners = new ArrayList<>();
    protected final BooleanProperty autoNotification = new SimpleBooleanProperty(this, "autoNotification", true);
    private final ObservableList<Renderer> renderers = FXCollections.observableArrayList();
    {
        getRenderers().addListener(this::rendererChanged);
    }

    protected final ResizableCanvas canvas = new ResizableCanvas();
    // contains axes (left, bottom, top, right) panes & HiddenSidePane with the
    // Canvas at it's centre
    protected final GridPane axesAndCanvasPane = new GridPane();
    protected final Group pluginsArea = Chart.createChildGroup();

    protected boolean isAxesUpdate;
    // containing the plugin handler/modifier
    protected final ToolBarFlowPane toolBar = new ToolBarFlowPane(this);
    protected final BooleanProperty toolBarPinned = new SimpleBooleanProperty(this, "toolBarPinned", false);

    protected final HiddenSidesPane hiddenPane = new HiddenSidesPane();
    protected final Pane plotBackground = new Pane();
    protected final Pane plotForeGround = new Pane();
    protected final Pane canvasForeground = new Pane();

    protected final Map<Corner, StackPane> axesCorner = new ConcurrentHashMap<>(4);
    protected final Map<Side, Pane> axesPane = new ConcurrentHashMap<>(4);
    protected final Map<Side, Pane> parameterDisplayPane = new ConcurrentHashMap<>(4);
    protected final Map<Corner, StackPane> titleLegendCorner = new ConcurrentHashMap<>(4);
    protected final Map<Side, Pane> titleLegendPane = new ConcurrentHashMap<>(4);
    {
        for (final Corner corner : Corner.values()) {
            axesCorner.put(corner, new StackPane()); // NOPMD - default init
            titleLegendCorner.put(corner, new StackPane()); // NOPMD - default init
        }
        for (final Side side : Side.values()) {
            titleLegendPane.put(side, side.isVertical() ? new ChartHBox() : new ChartVBox()); // NOPMD - default init
            axesPane.put(side, side.isVertical() ? new ChartHBox() : new ChartVBox()); // NOPMD - default init
            if (side == Side.CENTER_HOR || side == Side.CENTER_VER) {
                axesPane.get(side).setMouseTransparent(true);
            }

            // Stack multiple Measurements on top of each other by using a vertical FlowPane
            if (side == Side.RIGHT) {
                final FlowPane flowPane = new FlowPane(Orientation.VERTICAL); // NOPMD - default init
                flowPane.setAlignment(Pos.TOP_LEFT);
                flowPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
                parameterDisplayPane.put(side, flowPane);
                continue;
            }
            parameterDisplayPane.put(side, side.isVertical() ? new ChartHBox() : new ChartVBox()); // NOPMD - default
            // init
        }
    }

    private final EventListener axisChangeListener = obs -> FXUtils.runFX(() -> axesInvalidated(obs));
    protected final ListChangeListener<Axis> axesChangeListenerLocal = this::axesChangedLocal;
    protected final ListChangeListener<Axis> axesChangeListener = this::axesChanged;
    protected final ListChangeListener<DataSet> datasetChangeListener = this::datasetsChanged;
    protected final EventListener dataSetDataListener = obs -> FXUtils.runFX(this::dataSetInvalidated);
    protected final ListChangeListener<ChartPlugin> pluginsChangedListener = this::pluginsChanged;
    protected final ChangeListener<? super Window> windowPropertyListener = (ch1, oldWindow, newWindow) -> {
        if (oldWindow != null) {
            oldWindow.showingProperty().removeListener(showingListener);
        }
        if (newWindow == null) {
            showing.set(false);
            return;
        }
        newWindow.showingProperty().addListener(showingListener);
    };
    private final ChangeListener<? super Scene> scenePropertyListener = (ch, oldScene, newScene) -> {
        if (oldScene == newScene) {
            return;
        }
        if (oldScene != null) {
            // remove listener
            oldScene.windowProperty().removeListener(windowPropertyListener);
        }

        if (newScene == null) {
            showing.set(false);
            return;
        }

        // add listener
        newScene.windowProperty().addListener(windowPropertyListener);
    };
    {
        getDatasets().addListener(datasetChangeListener);
        getAxes().addListener(axesChangeListener);
        // update listener to propagate axes changes to chart changes
        getAxes().addListener(axesChangeListenerLocal);
    }

    protected final Label titleLabel = new Label();

    protected final StringProperty title = new StringPropertyBase() {
        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public String getName() {
            return "title";
        }

        @Override
        protected void invalidated() {
            titleLabel.setText(get());
        }
    };

    /**
     * The side of the chart where the title is displayed default Side.TOP
     */
    private final ObjectProperty<Side> titleSide = new StylishObjectProperty<Side>(StyleableProperties.TITLE_SIDE, this,
            "titleSide", Side.TOP, this::requestLayout) {
        @Override
        public void set(final Side side) {
            AssertUtils.notNull("Side must not be null", side);

            for (final Side s : Side.values()) {
                getMeasurementBar(s).getChildren().remove(titleLabel);
            }
            getMeasurementBar(side).getChildren().add(titleLabel);
            super.set(side);
        }
    };

    /**
     * The side of the chart where the title is displayed default Side.TOP
     */
    private final ObjectProperty<Side> measurementBarSide = new StyleableObjectProperty<Side>(Side.RIGHT) {
        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public CssMetaData<Chart, Side> getCssMetaData() {
            return StyleableProperties.MEASUREMENT_SIDE_BAR;
        }

        @Override
        public String getName() {
            return "measurementBarSide";
        }

        @Override
        public void set(final Side side) {
            AssertUtils.notNull("Side must not be null", side);
            super.set(side);
        }

        @Override
        protected void invalidated() {
            requestLayout();
        }
    };

    /**
     * The node to display as the Legend. Subclasses can set a node here to be displayed on a side as the legend. If no
     * legend is wanted then this can be set to null
     */
    private final ObjectProperty<Legend> legend = new SimpleObjectProperty<Legend>(this, "legend") {
        private Legend oldLegend;

        @Override
        protected void invalidated() {
            final Legend newLegend = get();
            if (oldLegend != null) {
                getChildren().remove(oldLegend);
            }
            if (newLegend != null) {
                getChildren().add(newLegend.getNode());
                newLegend.getNode().setVisible(isLegendVisible());
            }
            oldLegend = newLegend;
        }
    };

    /**
     * The side of the chart where the legend should be displayed default value Side.BOTTOM
     */
    private final ObjectProperty<Side> legendSide = new StylishObjectProperty<Side>(StyleableProperties.LEGEND_SIDE,
            this, "legendSide", Side.BOTTOM, this::requestLayout) {
        @Override
        public void set(final Side side) {
            AssertUtils.notNull("Side must not be null", side);

            final Legend legend = getLegend();
            for (final Side s : Side.values()) {
                getMeasurementBar(s).getChildren().remove(legend);
            }
            getMeasurementBar(side).getChildren().add(legend.getNode());

            if (legend instanceof Legend) {
                legend.setVertical(side.isVertical());
            }
            super.set(side);
        }
    };

    private final ObjectProperty<Side> toolBarSide = new StyleableObjectProperty<Side>(Side.TOP) {
        @Override
        public Object getBean() {
            return Chart.this;
        }

        @Override
        public CssMetaData<Chart, Side> getCssMetaData() {
            return StyleableProperties.TOOLBAR_SIDE;
        }

        @Override
        public String getName() {
            return "titleSide";
        }

        @Override
        public void set(final Side side) {
            AssertUtils.notNull("Side must not be null", side);

            // remove tool bar from potential other chart side pane locations
            Chart.this.setTop(null);
            Chart.this.setBottom(null);
            Chart.this.setLeft(null);
            Chart.this.setRight(null);
            switch (side) {
            case LEFT:
                getToolBar().setOrientation(Orientation.VERTICAL);
                Chart.this.setLeft(getToolBar());
                break;
            case RIGHT:
                getToolBar().setOrientation(Orientation.VERTICAL);
                Chart.this.setRight(getToolBar());
                break;
            case BOTTOM:
                getToolBar().setOrientation(Orientation.HORIZONTAL);
                Chart.this.setBottom(getToolBar());
                break;
            case TOP:
            default:
                getToolBar().setOrientation(Orientation.HORIZONTAL);
                Chart.this.setTop(getToolBar());
                break;
            }
            super.set(side);
        }

        @Override
        protected void invalidated() {
            requestLayout();
        }
    };

    /**
     * Creates a new default Chart instance.
     */
    public Chart() {
        super();
        getStylesheets().add(Chart.CHART_CSS);

        setTriggerDistance(Chart.DEFAULT_TRIGGER_DISTANCE);
        setMinSize(0, 0);
        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        setPadding(Insets.EMPTY);

        // populate SidesPane with default container
        final BorderPane localBorderPane = new BorderPane();
        axesAndCanvasPane.setPadding(Insets.EMPTY);
        localBorderPane.setCenter(new StackPane(plotBackground, axesAndCanvasPane, plotForeGround));
        plotBackground.toBack();
        plotForeGround.toFront();
        plotForeGround.setMouseTransparent(true);

        for (final Side side : Side.values()) {
            BorderPane.setAlignment(getMeasurementBar(side), Pos.CENTER);
        }
        localBorderPane.setTop(getMeasurementBar(Side.TOP));
        localBorderPane.setBottom(getMeasurementBar(Side.BOTTOM));
        localBorderPane.setLeft(getMeasurementBar(Side.LEFT));
        localBorderPane.setRight(getMeasurementBar(Side.RIGHT));

        super.setContent(localBorderPane);

        // hiddenPane.setTriggerDistance(DEFAULT_TRIGGER_DISTANCE);
        hiddenPane.triggerDistanceProperty().bindBidirectional(triggerDistanceProperty());
        hiddenPane.setAnimationDelay(Duration.millis(500));
        // hiddenPane.setMouseTransparent(true);
        hiddenPane.setPickOnBounds(false);

        final StackPane stackPane = new StackPane(getCanvas(), getCanvasForeground(), pluginsArea);
        hiddenPane.setContent(stackPane);

        // alt: canvas resize (default JavaFX Canvas does not automatically
        // resize to pref width/height according to parent constraints
        // canvas.widthProperty().bind(stackPane.widthProperty());
        // canvas.heightProperty().bind(stackPane.heightProperty());
        getCanvasForeground().setManaged(false);
        final ChangeListener<Number> canvasSizeChangeListener = (ch, o, n) -> {
            if (n == o) {
                return;
            }
            final double width = getCanvas().getWidth();
            final double height = getCanvas().getHeight();

            if (getCanvasForeground().getWidth() != width || getCanvasForeground().getHeight() != height) {
                // workaround needed so that pane within pane does not trigger
                // recursions w.r.t. repainting
                getCanvasForeground().resize(width, height);
            }

            if (!isCanvasChangeRequested) {
                isCanvasChangeRequested = true;
                Platform.runLater(() -> {
                    this.layoutChildren();
                    isCanvasChangeRequested = false;
                });
            }
        };
        canvas.widthProperty().addListener(canvasSizeChangeListener);
        canvas.heightProperty().addListener(canvasSizeChangeListener);

        getCanvasForeground().setMouseTransparent(true);
        getCanvas().toFront();
        getCanvasForeground().toFront();
        pluginsArea.toFront();

        hiddenPane.getStyleClass().setAll("plot-content");

        plotBackground.getStyleClass().setAll("chart-plot-background");

        if (!canvas.isCache()) {
            canvas.setCache(true);
            canvas.setCacheHint(CacheHint.QUALITY);
        }

        axesAndCanvasPane.add(hiddenPane, 2, 2); // centre-centre
        canvas.setStyle("-fx-background-color: rgba(200, 250, 200, 0.5);");

        final int ROW_SPAN1 = 1;
        final int COL_SPAN1 = 1;
        final int ROW_SPAN3 = 3;
        final int COL_SPAN3 = 3;

        // outer title/legend/parameter pane border (outer rim)
        axesAndCanvasPane.add(getTitleLegendPane(Side.LEFT), 0, 1, COL_SPAN1, ROW_SPAN3); // left-centre
        axesAndCanvasPane.add(getTitleLegendPane(Side.RIGHT), 4, 1, COL_SPAN1, ROW_SPAN3); // centre-centre
        axesAndCanvasPane.add(getTitleLegendPane(Side.TOP), 1, 0, COL_SPAN3, ROW_SPAN1); // centre-top
        axesAndCanvasPane.add(getTitleLegendPane(Side.BOTTOM), 1, 4, COL_SPAN3, ROW_SPAN1); // centre-bottom

        // add default axis panes (inner rim)
        axesAndCanvasPane.add(getAxesPane(Side.LEFT), 1, 2); // left-centre
        axesAndCanvasPane.add(getAxesPane(Side.RIGHT), 3, 2); // centre-centre
        axesAndCanvasPane.add(getAxesPane(Side.TOP), 2, 1); // centre-top
        axesAndCanvasPane.add(getAxesPane(Side.BOTTOM), 2, 3); // centre-bottom

        final Pane pane = getAxesPane(Side.CENTER_VER);
        GridPane.setFillHeight(pane, true);
        GridPane.setFillWidth(pane, true);

        axesAndCanvasPane.add(getAxesPane(Side.CENTER_VER), 2, 2); // centre-vertical
        axesAndCanvasPane.add(getAxesPane(Side.CENTER_HOR), 2, 2); // centre-vertical

        // add default corner BorderPane fields -- inner rim
        axesAndCanvasPane.add(getAxesCornerPane(Corner.TOP_LEFT), 1, 1);
        axesAndCanvasPane.add(getAxesCornerPane(Corner.TOP_RIGHT), 3, 1);
        axesAndCanvasPane.add(getAxesCornerPane(Corner.BOTTOM_LEFT), 1, 3);
        axesAndCanvasPane.add(getAxesCornerPane(Corner.BOTTOM_RIGHT), 3, 3);

        // add default corner BorderPane fields -- outer rim
        axesAndCanvasPane.add(getTitleLegendCornerPane(Corner.TOP_LEFT), 0, 0);
        axesAndCanvasPane.add(getTitleLegendCornerPane(Corner.TOP_RIGHT), 4, 0);
        axesAndCanvasPane.add(getTitleLegendCornerPane(Corner.BOTTOM_LEFT), 0, 4);
        axesAndCanvasPane.add(getTitleLegendCornerPane(Corner.BOTTOM_RIGHT), 4, 4);

        // set row/colum constraints for grid pane
        for (int i = 0; i < 4; i++) {
            final RowConstraints rowConstraint = new RowConstraints();
            if (i == 2) {
                rowConstraint.setVgrow(Priority.ALWAYS);
                rowConstraint.setFillHeight(true);
            }
            axesAndCanvasPane.getRowConstraints().add(i, rowConstraint);

            final ColumnConstraints colConstraint = new ColumnConstraints();
            if (i == 2) {
                colConstraint.setHgrow(Priority.ALWAYS);
                colConstraint.setFillWidth(true);
            }
            axesAndCanvasPane.getColumnConstraints().add(i, colConstraint);
        }

        // add plugin handling and listeners
        getPlugins().addListener(pluginsChangedListener);

        // add default chart content ie. ToolBar and Legend
        // can be repositioned via setToolBarSide(...) and setLegendSide(...)
        titleLabel.setAlignment(Pos.CENTER);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        VBox.setVgrow(titleLabel, Priority.ALWAYS);
        titleLabel.focusTraversableProperty().bind(Platform.accessibilityActiveProperty());

        // register listener in tool bar FlowPane
        toolBar.registerListener();
        HBox topbox = new HBox(getToolBar());
        topbox.setAlignment(Pos.TOP_CENTER);
        // getPlotArea().setTop(topbox);
        setTop(topbox);

        getTitleLegendPane(Side.TOP).getChildren().add(titleLabel);
        setLegend(new DefaultLegend());

        legendVisibleProperty().addListener((ch, old, visible) -> {
            if (getLegend() == null) {
                return;
            }
            getLegend().getNode().setVisible(visible);
            if (visible) {
                if (!getTitleLegendPane(getLegendSide()).getChildren().contains(getLegend().getNode())) {
                    getTitleLegendPane(getLegendSide()).getChildren().add(getLegend().getNode());
                }
            } else {
                getTitleLegendPane(getLegendSide()).getChildren().remove(getLegend().getNode());
            }
        });
        getTitleLegendPane(getLegendSide()).getChildren().add(getLegend().getNode());

        // set CSS stuff
        titleLabel.getStyleClass().add("chart-title");
        getStyleClass().add("chart");
        axesAndCanvasPane.getStyleClass().add("chart-content");

        registerShowingListener(); // NOPMD - unlikely but allowed override
    }

    @Override
    public void addListener(final InvalidationListener listener) {
        Objects.requireNonNull(listener, "InvalidationListener must not be null");
        listeners.add(listener);
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

    public BooleanProperty autoNotificationProperty() {
        return autoNotification;
    }

    /**
     * Notifies listeners that the data has been invalidated. If the data is added to the chart, it triggers repaint.
     *
     * @return itself (fluent design)
     */
    public Chart fireInvalidated() {
        synchronized (autoNotification) {
            if (!isAutoNotification() || listeners.isEmpty()) {
                return this;
            }
        }

        if (Platform.isFxApplicationThread()) {
            executeFireInvalidated();
        } else {
            Platform.runLater(this::executeFireInvalidated);
        }

        return this;
    }

    /**
     * @return datasets attached to the chart and datasets attached to all renderers
     */
    public ObservableList<DataSet> getAllDatasets() {
        if (getRenderers() == null) {
            return allDataSets;
        }

        allDataSets.clear();
        allDataSets.addAll(getDatasets());
        getRenderers().stream().filter(renderer -> !(renderer instanceof LabelledMarkerRenderer)).forEach(renderer -> allDataSets.addAll(renderer.getDatasets()));

        return allDataSets;
    }

    public ObservableList<Axis> getAxes() {
        return axesList;
    }

    public GridPane getAxesAndCanvasPane() {
        return axesAndCanvasPane;
    }

    public final StackPane getAxesCornerPane(final Corner corner) {
        return axesCorner.get(corner);
    }

    public final Pane getAxesPane(final Side side) {
        return axesPane.get(side);
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

    /**
     * @return datasets attached to the chart and drawn by all renderers
     */
    public ObservableList<DataSet> getDatasets() {
        return datasets;
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
            Axis newXAxis = new DefaultNumericAxis("x-Axis");
            newXAxis.setSide(Side.BOTTOM);
            getAxes().add(newXAxis);
            return newXAxis;
        case VERTICAL:
        default:
            Axis newYAxis = new DefaultNumericAxis("y-Axis");
            newYAxis.setSide(Side.LEFT);
            getAxes().add(newYAxis);
            return newYAxis;
        }
    }

    public final Legend getLegend() {
        return legend.getValue();
    }

    public final Side getLegendSide() {
        return legendSide.get();
    }

    public final Pane getMeasurementBar(final Side side) {
        return parameterDisplayPane.get(side);
    }

    public final Side getMeasurementBarSide() {
        return measurementBarSide.get();
    }

    public final HiddenSidesPane getPlotArea() {
        return hiddenPane;
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
        return title.get();
    }

    public final StackPane getTitleLegendCornerPane(final Corner corner) {
        return titleLegendCorner.get(corner);
    }

    public final Pane getTitleLegendPane(final Side side) {
        return titleLegendPane.get(side);
    }

    public final Side getTitleSide() {
        return titleSide.get();
    }

    public final FlowPane getToolBar() {
        return toolBar;
    }

    public final Side getToolBarSide() {
        return toolBarSide.get();
    }

    /**
     * Indicates whether data changes will be animated or not.
     *
     * @return true if data changes will be animated and false otherwise.
     */
    public final boolean isAnimated() {
        return animated.get();
    }

    public boolean isAutoNotification() {
        return autoNotification.get();
    }

    public final boolean isLegendVisible() {
        return legendVisible.getValue();
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

    @Override
    public void layoutChildren() {
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("chart layoutChildren() - pre");
        }
        if (layoutOngoing) {
            return;
        }
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("chart layoutChildren() - execute");
        }
        final long start = ProcessingProfiler.getTimeStamp();
        layoutOngoing = true;

        // update axes range first because this may change the overall layout
        updateAxisRange();
        for (final Axis axis : getAxes()) {
            final boolean oldState = axis.autoNotification().getAndSet(false);
            try {
                axis.requestAxisLayout();
            } finally {
                axis.autoNotification().set(oldState);
            }
        }
        ProcessingProfiler.getTimeDiff(start, "updateAxisRange()");

        // update chart parent according to possible size changes
        super.layoutChildren();

        // request re-layout of canvas
        redrawCanvas();

        ProcessingProfiler.getTimeDiff(start, "updateCanvas()");

        // request re-layout of plugins
        layoutPluginsChildren();
        ProcessingProfiler.getTimeDiff(start, "layoutPluginsChildren()");

        ProcessingProfiler.getTimeDiff(start, "end");

        layoutOngoing = false;
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("chart layoutChildren() - done");
        }
        fireInvalidated();
    }

    public final ObjectProperty<Legend> legendProperty() {
        return legend;
    }

    public final ObjectProperty<Side> legendSideProperty() {
        return legendSide;
    }

    public final BooleanProperty legendVisibleProperty() {
        return legendVisible;
    }

    public final ObjectProperty<Side> measurementBarSideProperty() {
        return measurementBarSide;
    }

    public boolean removeFromAllAxesPanes(final Axis node) {
        if (!(node instanceof Node)) {
            return false;
        }
        final Node axisNode = (Node) node;
        // remove axis from all axis panes
        for (final Side side : Side.values()) {
            if (getAxesPane(side).getChildren().remove(axisNode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeListener(final InvalidationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void requestLayout() {
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("chart requestLayout() - called by {}", ProcessingProfiler.getCallingClassMethod(1));
        }
        FXUtils.assertJavaFxThread();
        super.requestLayout();
    }

    public final void setAnimated(final boolean value) {
        animated.set(value);
    }

    public void setAutoNotification(final boolean flag) {
        autoNotification.set(flag);
    }

    public final void setLegend(final Legend value) {
        legend.setValue(value);
    }

    public final void setLegendSide(final Side value) {
        legendSide.set(value);
    }

    public final void setLegendVisible(final boolean value) {
        legendVisible.set(value);
    }

    public final void setMeasurementBarSide(final Side value) {
        measurementBarSide.set(value);
    }

    public final void setTitle(final String value) {
        title.set(value);
    }

    public final void setTitleSide(final Side value) {
        titleSide.set(value);
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
        return title;
    }

    public final ObjectProperty<Side> titleSideProperty() {
        return titleSide;
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
            change.getRemoved().forEach(set -> {
                AssertUtils.notNull("to be removed axis is null", set);
                // remove axis invalidation listener
                set.removeListener(axisChangeListener);
            });
            for (final Axis set : change.getAddedSubList()) {
                // check if axis is associated with an existing renderer,
                // if yes -> throw an exception
                AssertUtils.notNull("to be added axis is null", set);
                set.addListener(axisChangeListener);
            }
        }

        requestLayout();
    }

    /**
     * function called whenever a axis has been invalidated (e.g. range change or parameter plotting changes). Typically
     * calls 'requestLayout()' but can be overwritten in derived classes.
     *
     * @param axisObj the calling axis object
     */
    protected void axesInvalidated(final Object axisObj) {
        if (!(axisObj instanceof Axis) || layoutOngoing || isAxesUpdate) {
            return;
        }
        FXUtils.assertJavaFxThread();
        isAxesUpdate = true;
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("chart axesInvalidated() - called by (1) {}", ProcessingProfiler.getCallingClassMethod(1));
            LOGGER.debug("chart axesInvalidated() - called by (3) {}", ProcessingProfiler.getCallingClassMethod(3));
        }
        requestLayout();
        isAxesUpdate = false;
    }

    protected void dataSetInvalidated() {
        // DataSet has notified and invalidate
        if (DEBUG && LOGGER.isDebugEnabled()) {
            LOGGER.debug("chart dataSetDataListener change notified");
        }
        FXUtils.assertJavaFxThread();
        // updateAxisRange();
        // TODO: check why the following does not always forces a layoutChildren
        requestLayout();
    }

    protected void datasetsChanged(final ListChangeListener.Change<? extends DataSet> change) {
        boolean dataSetChanges = false;
        FXUtils.assertJavaFxThread();
        while (change.next()) {
            for (final DataSet set : change.getRemoved()) {
                set.removeListener(dataSetDataListener);
                dataSetChanges = true;
            }

            for (final DataSet set : change.getAddedSubList()) {
                set.addListener(dataSetDataListener);
                dataSetChanges = true;
            }
        }

        if (dataSetChanges) {
            if (DEBUG && LOGGER.isDebugEnabled()) {
                LOGGER.debug("chart datasetsChanged(Change) - has dataset changes");
            }
            // updateAxisRange();
            updateLegend(getDatasets(), getRenderers());
            requestLayout();
        }
    }

    protected void executeFireInvalidated() {
        new ArrayList<>(listeners).forEach(listener -> listener.invalidated(this));
    }

    /**
     * @return unmodifiable list of the controls css styleable properties
     * @since JavaFX 8.0
     */
    @Override
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return Chart.getClassCssMetaData();
    }

    protected void layoutPluginsChildren() {
        plugins.forEach(ChartPlugin::layoutChildren);
    }

    protected void pluginAdded(final ChartPlugin plugin) {
        plugin.setChart(Chart.this);
        final Group group = Chart.createChildGroup();
        Bindings.bindContent(group.getChildren(), plugin.getChartChildren());
        pluginGroups.put(plugin, group);
    }

    // -------------- STYLESHEET HANDLING
    // ------------------------------------------------------------------------------

    protected void pluginRemoved(final ChartPlugin plugin) {
        plugin.setChart(null);
        final Group group = pluginGroups.remove(plugin);
        Bindings.unbindContent(group, plugin.getChartChildren());
        group.getChildren().clear();
        pluginsArea.getChildren().remove(group);
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

    protected void registerShowingListener() {
        sceneProperty().addListener(scenePropertyListener);

        showing.addListener((ch, o, n) -> {
            if (n.equals(n)) {
                return;
            }
            if (Boolean.TRUE.equals(n)) {
                // requestLayout();

                // alt implementation in case of start-up issues
                final KeyFrame kf1 = new KeyFrame(Duration.millis(20), e -> requestLayout());

                final Timeline timeline = new Timeline(kf1);
                Platform.runLater(timeline::play);
            }
        });
    }

    protected void rendererChanged(final ListChangeListener.Change<? extends Renderer> change) {
        FXUtils.assertJavaFxThread();
        while (change.next()) {
            // handle added renderer
            for (final Renderer renderer : change.getAddedSubList()) {
                // update legend and recalculateLayout on datasetChange
                renderer.getDatasets().addListener(datasetChangeListener);
            }

            // handle removed renderer
            change.getRemoved().forEach(renderer -> renderer.getDatasets().removeListener(datasetChangeListener));
        }
        // reset change to allow derived classes to add additional listeners to renderer changes
        change.reset();

        requestLayout();
        updateLegend(getDatasets(), getRenderers());
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

    protected void updateLegend(final List<DataSet> dataSets, final List<Renderer> renderers) {
        final Legend legend = getLegend();
        if (legend == null) {
            return;
        }
        legend.updateLegend(dataSets, renderers);
    }

    protected void updatePluginsArea() {
        pluginsArea.getChildren().setAll(plugins.stream().map(pluginGroups::get).collect(Collectors.toList()));
        requestLayout();
    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    protected static Group createChildGroup() {
        final Group group = new Group();
        group.setManaged(false);
        group.setAutoSizeChildren(false);
        group.relocate(0, 0);
        return group;
    }

    private static class StyleableProperties {
        private static final CssMetaData<Chart, Side> TITLE_SIDE = new CssMetaData<Chart, Side>("-fx-title-side",
                new EnumConverter<>(Side.class), Side.TOP) {
            @Override
            public StyleableProperty<Side> getStyleableProperty(final Chart node) {
                return (StyleableProperty<Side>) (WritableValue<Side>) node.titleSideProperty();
            }

            @Override
            public boolean isSettable(final Chart node) {
                return node != null && !node.titleSide.isBound();
            }
        };

        private static final CssMetaData<Chart, Side> MEASUREMENT_SIDE_BAR = new CssMetaData<Chart, Side>(
                "-fx-measurement-bar-side", new EnumConverter<>(Side.class), Side.RIGHT) {
            @Override
            public StyleableProperty<Side> getStyleableProperty(final Chart node) {
                return (StyleableProperty<Side>) (WritableValue<Side>) node.measurementBarSideProperty();
            }

            @Override
            public boolean isSettable(final Chart node) {
                return node != null && !node.measurementBarSide.isBound();
            }
        };

        private static final CssMetaData<Chart, Side> TOOLBAR_SIDE = new CssMetaData<Chart, Side>("-fx-toolbar-side",
                new EnumConverter<>(Side.class), Side.TOP) {
            @Override
            public StyleableProperty<Side> getStyleableProperty(final Chart node) {
                return (StyleableProperty<Side>) (WritableValue<Side>) node.toolBarSideProperty();
            }

            @Override
            public boolean isSettable(final Chart node) {
                return node != null && !node.toolBarSide.isBound();
            }
        };

        private static final CssMetaData<Chart, Side> LEGEND_SIDE = new CssMetaData<Chart, Side>("-fx-legend-side",
                new EnumConverter<>(Side.class), Side.BOTTOM) {
            @Override
            public StyleableProperty<Side> getStyleableProperty(final Chart node) {
                return (StyleableProperty<Side>) (WritableValue<Side>) node.legendSideProperty();
            }

            @Override
            public boolean isSettable(final Chart node) {
                return node != null && !node.legendSide.isBound();
            }
        };

        private static final CssMetaData<Chart, Boolean> LEGEND_VISIBLE = new CssMetaData<Chart, Boolean>(
                "-fx-legend-visible", BooleanConverter.getInstance(), Boolean.TRUE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final Chart node) {
                return (StyleableProperty<Boolean>) node.legendVisibleProperty();
            }

            @Override
            public boolean isSettable(final Chart node) {
                return node != null && !node.legendVisible.isBound();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(StyleableProperties.TITLE_SIDE);
            styleables.add(StyleableProperties.MEASUREMENT_SIDE_BAR);
            styleables.add(StyleableProperties.TOOLBAR_SIDE);
            styleables.add(StyleableProperties.LEGEND_SIDE);
            styleables.add(StyleableProperties.LEGEND_VISIBLE);

            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    protected class ChartHBox extends HBox {
        public ChartHBox() {
            super();
            setAlignment(Pos.CENTER);
            setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        }

        public ChartHBox(final boolean fill) {
            this();
            setFillHeight(fill);
        }
    }

    protected class ChartVBox extends VBox {
        public ChartVBox() {
            super();
            setAlignment(Pos.CENTER);
            setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        }

        public ChartVBox(final boolean fill) {
            this();
            setFillWidth(fill);
        }
    }
}
