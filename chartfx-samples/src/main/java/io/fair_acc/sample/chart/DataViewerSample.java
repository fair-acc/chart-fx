package io.fair_acc.sample.chart;

import java.util.*;

import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.StateListener;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.viewer.DataView;
import io.fair_acc.chartfx.viewer.DataViewWindow;
import io.fair_acc.chartfx.viewer.DataViewWindow.WindowDecoration;
import io.fair_acc.chartfx.viewer.DataViewer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.testdata.TestDataSet;
import io.fair_acc.dataset.testdata.spi.RandomStepFunction;
import io.fair_acc.dataset.testdata.spi.RandomWalkFunction;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * @author Grzegorz Kruk
 * @author rstein
 */
@DefaultProperty("views")
public class DataViewerSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewerSample.class);
    private static final String TITLE = DataViewerSample.class.getSimpleName();
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 22;
    private static final int NUMBER_OF_POINTS = 10_000; // default: 32000
    private static final int UPDATE_PERIOD = 1000; // [ms]

    private static final int NUM_OF_POINTS = 20;

    private final StateListener dataWindowEventListener = (srcState, bits) -> {
        if (!ChartBits.DataViewWindow.isSet(bits)) {
            return;
        }
        srcState.clear(ChartBits.DataViewWindow); // clear manually to keep example simple
        var viewWindow = (DataViewWindow) srcState.getSource();
        LOGGER.atInfo().addArgument(viewWindow.getWindowState()).log("received window update event for new state {}");
    };

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        primaryStage.setTitle(DataViewerSample.TITLE);

        // the new JavaFX Chart Dataviewer
        final DataViewer viewer = setupDataViewer();

        final Label focusedOwner = new Label();
        // focusedOwner.textProperty().bind(scene.focusOwnerProperty().asString());
        return new VBox(viewer.getToolBar(), viewer, new HBox(new Label("focus on: "), focusedOwner));
    }

    private DataViewer setupDataViewer() {
        final DataViewer viewer = new DataViewer();

        final FontIcon chartIcon = new FontIcon("fa-line-chart:" + FONT_SIZE);
        final DataView view1 = new DataView("ChartViews", chartIcon);

        final FontIcon customViewIcon = new FontIcon("fa-users:" + FONT_SIZE);
        final DataView view2 = new DataView("Custom View", customViewIcon, getDemoPane());

        viewer.getViews().addAll(view1, view2);
        viewer.setExplorerVisible(true);

        final DataViewWindow energyView = setupEnergyView();
        final DataViewWindow currentView = setupCurrentView();
        final DataViewWindow jDataViewerPane = setupJDataViewPane();
        view1.getVisibleChildren().addAll(energyView, currentView, jDataViewerPane);

        final ComboBox<InitialWindowState> initialWindowState = new ComboBox<>();
        initialWindowState.getItems().setAll(InitialWindowState.values());
        initialWindowState.setValue(InitialWindowState.VISIBLE);

        // set default view
        // viewer.setSelectedView(view2);
        // set user default interactors
        final CheckBox listView = new CheckBox();
        listView.setGraphic(new FontIcon("fa-list-alt:" + FONT_SIZE));
        listView.setTooltip(new Tooltip("click to switch between button and list-style DataView selection"));
        listView.setSelected(viewer.showListStyleDataViewProperty().get());
        listView.selectedProperty().bindBidirectional(viewer.showListStyleDataViewProperty());

        final ComboBox<WindowDecoration> windowDecoration = new ComboBox<>(FXCollections.observableArrayList(WindowDecoration.values()));
        windowDecoration.getSelectionModel().select(viewer.getWindowDecoration());
        windowDecoration.setOnAction(evt -> viewer.setWindowDecoration(windowDecoration.getSelectionModel().getSelectedItem()));

        final CheckBox detachable = new CheckBox();
        final Label detachableBox = new Label("allow windows to detach: ", detachable);
        detachableBox.setContentDisplay(ContentDisplay.RIGHT);
        detachable.setAlignment(Pos.CENTER_RIGHT);
        detachable.setTooltip(new Tooltip("enable/disable windows to detach"));
        detachable.setSelected(viewer.isDetachableWindow());
        detachable.selectedProperty().bindBidirectional(viewer.detachableWindowProperty());

        final Button newView = new Button(null, new HBox(new FontIcon("fa-plus:" + FONT_SIZE), new FontIcon("fa-line-chart:" + FONT_SIZE)));
        newView.setTooltip(new Tooltip("add new view"));
        newView.setOnAction(evt -> addChartToView(view1, windowDecoration, initialWindowState));

        viewer.getUserToolBarItems().addAll(new ProfilerInfoBox(), newView, initialWindowState, new Label("Win-Decor:"), windowDecoration, detachableBox, listView);
        return viewer;
    }

    private void addChartToView(DataView view1, final ComboBox<WindowDecoration> windowDecoration, final ComboBox<InitialWindowState> initialWindowState) {
        final int count = view1.getVisibleChildren().size() + view1.getMinimisedChildren().size();
        final XYChart jChart = createChart();
        final DataViewWindow newDataViewerPane = new DataViewWindow("Chart" + count, jChart, windowDecoration.getValue());
        switch (initialWindowState.getValue()) {
        case DETACHED:
            // alternate: add immediately to undocked state
            view1.getUndockedChildren().add(newDataViewerPane);
            break;
        case MINIMISED:
            // alternate: add immediately to minimised state
            view1.getMinimisedChildren().add(newDataViewerPane);
            break;
        case VISIBLE:
        default:
            view1.getVisibleChildren().add(newDataViewerPane);
            break;
        }

        newDataViewerPane.addListener(dataWindowEventListener);
        newDataViewerPane.closedProperty().addListener((ch, o, n) -> {
            LOGGER.atInfo().log("newDataViewerPane Window '" + newDataViewerPane.getName()
                                + "' has been closed - performing clean-up actions");
            // perform some custom clean-up action
        });

        // add listener on specific events
        final ChangeListener<Boolean> changeListener = (ch, o, n) -> {
            // small debugging routine to check state-machine
            LOGGER.atInfo().addArgument(newDataViewerPane.isMinimised()).addArgument(newDataViewerPane.isMaximised()) //
                    .addArgument(newDataViewerPane.isRestored())
                    .addArgument(newDataViewerPane.isDetached())
                    .addArgument(newDataViewerPane.isClosed()) //
                    .log("minimised: {}, maximised {}, restored {}, detached {}, closed {}");
        };
        newDataViewerPane.minimisedProperty().addListener(changeListener);
        newDataViewerPane.maximisedProperty().addListener(changeListener);
        newDataViewerPane.restoredProperty().addListener(changeListener);
        newDataViewerPane.detachedProperty().addListener(changeListener);
        newDataViewerPane.closedProperty().addListener(changeListener);

        // view1.getVisibleNodes().add(jChart);
    }

    private DataViewWindow setupJDataViewPane() {
        final XYChart jDataViewerChart = createChart();
        final DataViewWindow jDataViewerPane = new DataViewWindow("Chart", jDataViewerChart);
        jDataViewerPane.addListener(dataWindowEventListener);
        logStatePropertyChanges(jDataViewerPane.getName(), jDataViewerPane);
        return jDataViewerPane;
    }

    private DataViewWindow setupEnergyView() {
        final XYChart energyChart = new TestChart();
        energyChart.getYAxis().setName("Energy");
        energyChart.getDatasets().addAll(createSeries());

        final DataViewWindow energyView = new DataViewWindow("Energy", energyChart);
        energyView.setGraphic(new FontIcon("fa-adjust"));
        energyView.addListener(dataWindowEventListener);
        logStatePropertyChanges(energyView.getName(), energyView);
        return energyView;
    }

    private DataViewWindow setupCurrentView() {
        final XYChart currentChart = new TestChart();
        currentChart.getRenderers().clear();
        final ErrorDataSetRenderer errorDataSetRenderer = new ErrorDataSetRenderer();
        errorDataSetRenderer.setErrorStyle(ErrorStyle.NONE);
        currentChart.getRenderers().add(errorDataSetRenderer);
        ((Region) currentChart.getYAxis()).lookup(".axis-label").setStyle("-fx-text-fill: green;");
        currentChart.getYAxis().setName("Current");
        currentChart.getYAxis().setSide(Side.RIGHT);
        currentChart.getDatasets().addAll(createSeries());

        final DataViewWindow currentView = new DataViewWindow("Current", currentChart);
        currentView.addListener(dataWindowEventListener);
        logStatePropertyChanges(currentView.getName(), currentView);
        return currentView;
    }

    /**
     * create demo JDataViewer Chart
     *
     * @return the Swing-based chart component
     */
    private XYChart createChart() {
        final XYChart chart = new TestChart();
        chart.getXAxis().set("time", "s");
        chart.getYAxis().set("y-axis", "A");

        final RandomWalkFunction dataset1 = new RandomWalkFunction("Test1", DataViewerSample.NUMBER_OF_POINTS);
        final RandomWalkFunction dataset2 = new RandomWalkFunction("Test2", DataViewerSample.NUMBER_OF_POINTS);
        final RandomStepFunction dataset3 = new RandomStepFunction("Test3", DataViewerSample.NUMBER_OF_POINTS);
        chart.getRenderers().clear();
        chart.getRenderers().add(new ErrorDataSetRenderer());
        chart.getDatasets().addAll(Arrays.asList(dataset1, dataset2, dataset3));

        // Start task adding new data
        final UpdateTask updateTask = new UpdateTask(dataset1, dataset3);
        final Timer timer = new Timer("sample-update-timer", true);
        // Start update in 2sec.
        timer.schedule(updateTask, 2000, DataViewerSample.UPDATE_PERIOD);

        return chart;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static DoubleDataSet createData(final String name) {
        final DoubleDataSet dataSet = new DoubleDataSet(name, DataViewerSample.NUM_OF_POINTS);
        final Random rnd = new Random();
        for (int i = 0; i < DataViewerSample.NUM_OF_POINTS; i++) {
            dataSet.set(i, i, i * i * rnd.nextDouble());
        }
        return dataSet;
    }

    private static List<DataSet> createSeries() {
        final List<DataSet> series = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            series.add(createData("Series " + i));
        }
        return series;
    }

    private static Pane getDemoPane() {
        final Rectangle rect = new Rectangle(-130, -40, 80, 80);
        rect.setFill(Color.BLUE);
        final Circle circle = new Circle(0, 0, 40);
        circle.setFill(Color.GREEN);
        final Polygon triangle = new Polygon(60, -40, 120, 0, 50, 40);
        triangle.setFill(Color.RED);

        final Group group = new Group(rect, circle, triangle);
        group.setTranslateX(300);
        group.setTranslateY(200);

        final RotateTransition rotateTransition = new RotateTransition(Duration.millis(4000), group);
        rotateTransition.setByAngle(3.0 * 360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setAutoReverse(true);
        rotateTransition.play();

        final RotateTransition rotateTransition1 = new RotateTransition(Duration.millis(1000), rect);
        rotateTransition1.setByAngle(360);
        rotateTransition1.setCycleCount(Animation.INDEFINITE);
        rotateTransition1.setAutoReverse(false);
        rotateTransition1.play();

        final RotateTransition rotateTransition2 = new RotateTransition(Duration.millis(1000), triangle);
        rotateTransition2.setByAngle(360);
        rotateTransition2.setCycleCount(Animation.INDEFINITE);
        rotateTransition2.setAutoReverse(false);
        rotateTransition2.play();
        group.setManaged(true);

        HBox.setHgrow(group, Priority.ALWAYS);
        final HBox box = new HBox(group);
        VBox.setVgrow(box, Priority.ALWAYS);
        box.setId("demoPane");
        return box;
    }

    private static void logPropertyChange(final BooleanProperty property, final String name) {
        property.addListener((ch, o, n) -> LOGGER.atInfo().log("Property '{}' changed to '{}'", name, n));
    }

    private static void logStatePropertyChanges(final String windowName, final DataViewWindow currentView) {
        logPropertyChange(currentView.minimisedProperty(), windowName + " minimized");
        logPropertyChange(currentView.detachedProperty(), windowName + " detached");
        logPropertyChange(currentView.closedProperty(), windowName + " closed");
        logPropertyChange(currentView.restoredProperty(), windowName + " restored");
        logPropertyChange(currentView.maximisedProperty(), windowName + " maximized");
    }

    private enum InitialWindowState {
        VISIBLE,
        MINIMISED,
        DETACHED
    }

    private static class TestChart extends XYChart {
        private TestChart() {
            super();
            getPlugins().add(new ParameterMeasurements());
            getPlugins().add(new Zoomer());
            getPlugins().add(new TableViewer());
            getPlugins().add(new EditAxis());
        }
    }

    private static class UpdateTask extends TimerTask {
        private final TestDataSet<?>[] dataSets;
        private int count;

        private UpdateTask(final TestDataSet<?>... dataSet) {
            super();
            dataSets = dataSet.clone();
        }

        @Override
        public void run() {
            final long start = System.currentTimeMillis();
            for (final TestDataSet<?> dataSet : dataSets) {
                dataSet.update();
            }

            if (count % 10 == 0) {
                final long diff = System.currentTimeMillis() - start;
                LOGGER.atDebug().log(String.format("update #%d took %d ms", count, diff));
            }

            count = (count + 1) % 1000;
        }
    }
}
