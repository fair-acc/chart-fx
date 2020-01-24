package de.gsi.chart.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.GlyphFactory;
import de.gsi.chart.viewer.DataView;
import de.gsi.chart.viewer.DataViewWindow;
import de.gsi.chart.viewer.DataViewer;
import de.gsi.chart.viewer.event.WindowClosedEvent;
import de.gsi.chart.viewer.event.WindowUpdateEvent;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.testdata.TestDataSet;
import de.gsi.dataset.testdata.spi.RandomStepFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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

/**
 * @author Grzegorz Kruk
 * @author rstein
 */
@DefaultProperty(value = "views")
public class DataViewerSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewerSample.class);
    private static final String TITLE = DataViewerSample.class.getSimpleName();
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private static final int NUMBER_OF_POINTS = 10000; // default: 32000
    private static final int UPDATE_PERIOD = 1000; // [ms]

    private static final int NUM_OF_POINTS = 20;

    private EventListener dataWindowEventListener = evt -> {
        if (evt instanceof WindowUpdateEvent) {
            WindowUpdateEvent wEvt = (WindowUpdateEvent) evt;
            LOGGER.atInfo().addArgument(wEvt).addArgument(wEvt.getType())
                    .log("received window update event {} of type {}");
        } else {
            LOGGER.atInfo().addArgument(evt).addArgument(evt.getMessage())
                    .log("received generic window update event {} with message {}");
        }

        if (evt instanceof WindowClosedEvent) {
            LOGGER.atInfo().addArgument(((WindowClosedEvent) evt).getSource()).log("window {} closed");
        }
    };

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        primaryStage.setTitle(DataViewerSample.TITLE);

        // the new JavaFX Chart Dataviewer
        final Glyph chartIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.LINE_CHART).size(FONT_SIZE);
        final DataView view1 = new DataView("ChartViews", chartIcon);

        final Glyph customViewIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.USERS).size(FONT_SIZE);
        final DataView view2 = new DataView("Custom View", customViewIcon, getDemoPane());

        final DataViewer viewer = new DataViewer();
        viewer.getViews().addAll(view1, view2);
        viewer.setExplorerVisible(true);

        final XYChart energyChart = new TestChart();
        energyChart.getYAxis().setName("Energy");
        energyChart.getDatasets().addAll(createSeries());

        final XYChart currentChart = new TestChart();
        currentChart.getRenderers().clear();
        final ErrorDataSetRenderer errorDataSetRenderer = new ErrorDataSetRenderer();
        errorDataSetRenderer.setErrorType(ErrorStyle.NONE);
        currentChart.getRenderers().add(errorDataSetRenderer);
        ((Region) currentChart.getYAxis()).lookup(".axis-label").setStyle("-fx-text-fill: green;");
        currentChart.getYAxis().setName("Current");
        currentChart.getYAxis().setSide(Side.RIGHT);
        currentChart.getDatasets().addAll(createSeries());

        final DataViewWindow currentView = new DataViewWindow("Current", currentChart);
        currentView.addListener(dataWindowEventListener);
        logStatePropertyChanges(currentView.getName(), currentView);

        final XYChart jDataViewerChart = createChart();
        final DataViewWindow jDataViewerPane = new DataViewWindow("Chart", jDataViewerChart);
        jDataViewerPane.addListener(dataWindowEventListener);
        logStatePropertyChanges(jDataViewerPane.getName(), jDataViewerPane);

        final DataViewWindow energyView = new DataViewWindow("Energy", energyChart);
        energyView.setGraphic(GlyphFactory.create(FontAwesome.Glyph.ADJUST));
        energyView.addListener(dataWindowEventListener);
        logStatePropertyChanges(energyView.getName(), energyView);
        view1.getVisibleChildren().addAll(energyView, currentView, jDataViewerPane);
        // view1.getVisibleNodes().addAll(energyChart, currentChart, jDataViewerChart);

        ComboBox<InitialWindowState> initialWindowState = new ComboBox<>();
        initialWindowState.getItems().setAll(InitialWindowState.values());
        initialWindowState.setValue(InitialWindowState.VISIBLE);

        final Button newView = new Button(null, new HBox( //
                new Glyph(FONT_AWESOME, FontAwesome.Glyph.PLUS).size(FONT_SIZE), //
                new Glyph(FONT_AWESOME, FontAwesome.Glyph.LINE_CHART).size(FONT_SIZE)));
        newView.setTooltip(new Tooltip("add new view"));
        newView.setOnAction(evt -> {
            final int count = view1.getVisibleChildren().size() + view1.getMinimisedChildren().size();
            final XYChart jChart = createChart();
            final DataViewWindow newDataViewerPane = new DataViewWindow("Chart" + count, jChart);
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
            newDataViewerPane.addListener(windowEvent -> {
                // print window state explicitly
                LOGGER.atInfo().addArgument(newDataViewerPane.getName()).addArgument(newDataViewerPane.getWindowState())
                        .log("explicit '{}' window state is {}");
            });
            newDataViewerPane.closedProperty().addListener((ch, o, n) -> {
                LOGGER.atInfo().log("newDataViewerPane Window '" + newDataViewerPane.getName()
                        + "' has been closed - performing clean-up actions");
                // perform some custom clean-up action
            });

            // add listener on specific events
            ChangeListener<Boolean> changeListener = (ch, o, n) -> {
                // small debugging routine to check state-machine
                LOGGER.atInfo().addArgument(newDataViewerPane.isMinimised())
                        .addArgument(newDataViewerPane.isMaximised()).addArgument(newDataViewerPane.isRestored())
                        .addArgument(newDataViewerPane.isDetached()).addArgument(newDataViewerPane.isClosed())
                        .log("minimised: {}, maximised {}, restored {}, detached {}, closed {}");
            };
            newDataViewerPane.minimisedProperty().addListener(changeListener);
            newDataViewerPane.maximisedProperty().addListener(changeListener);
            newDataViewerPane.restoredProperty().addListener(changeListener);
            newDataViewerPane.detachedProperty().addListener(changeListener);
            newDataViewerPane.closedProperty().addListener(changeListener);

            // view1.getVisibleNodes().add(jChart);
        });

        // set default view
        //        viewer.setSelectedView(view2);
        // set user default interactors
        CheckBox listView = new CheckBox();
        listView.setGraphic(new Glyph(FONT_AWESOME, '\uf022').size(FONT_SIZE));
        listView.setTooltip(new Tooltip("click to switch between button and list-style DataView selection"));
        listView.setSelected(viewer.showListStyleDataViewProperty().get());
        listView.selectedProperty().bindBidirectional(viewer.showListStyleDataViewProperty());
        CheckBox windowDeco = new CheckBox();
        windowDeco.setTooltip(new Tooltip("click to remove sub-window decorations"));
        windowDeco.setSelected(viewer.windowDecorationVisible().get());
        windowDeco.setGraphic(new Glyph(FONT_AWESOME, '\uf2d0').size(FONT_SIZE));
        windowDeco.selectedProperty().bindBidirectional(viewer.windowDecorationVisible());
        CheckBox closeDeco = new CheckBox();
        closeDeco.setSelected(true);
        closeDeco.setGraphic(new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLOSE).size(FONT_SIZE));
        closeDeco.selectedProperty().bindBidirectional(viewer.closeWindowButtonVisibleProperty());

        Label focusedOwner = new Label();

        viewer.getUserToolBarItems().addAll(newView, listView, windowDeco, closeDeco, initialWindowState,
                new Separator());
        final Scene scene = new Scene(
                new VBox(viewer.getToolBar(), viewer, new HBox(new Label("focus on: "), focusedOwner)), 800, 600);
        scene.focusOwnerProperty().addListener((ch, o, n) -> {
            if (n == null) {
                focusedOwner.setText(null);
                return;
            }
            focusedOwner.setText(n.toString());
        });
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(evt -> System.exit(0)); //NOPMD
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
        Rectangle rect = new Rectangle(-130, -40, 80, 80);
        rect.setFill(Color.BLUE);
        Circle circle = new Circle(0, 0, 40);
        circle.setFill(Color.GREEN);
        Polygon triangle = new Polygon(60, -40, 120, 0, 50, 40);
        triangle.setFill(Color.RED);

        Group group = new Group(rect, circle, triangle);
        group.setTranslateX(300);
        group.setTranslateY(200);

        RotateTransition rotateTransition = new RotateTransition(Duration.millis(4000), group);
        rotateTransition.setByAngle(3.0 * 360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setAutoReverse(true);
        rotateTransition.play();

        RotateTransition rotateTransition1 = new RotateTransition(Duration.millis(1000), rect);
        rotateTransition1.setByAngle(360);
        rotateTransition1.setCycleCount(Animation.INDEFINITE);
        rotateTransition1.setAutoReverse(false);
        rotateTransition1.play();

        RotateTransition rotateTransition2 = new RotateTransition(Duration.millis(1000), triangle);
        rotateTransition2.setByAngle(360);
        rotateTransition2.setCycleCount(Animation.INDEFINITE);
        rotateTransition2.setAutoReverse(false);
        rotateTransition2.play();
        group.setManaged(true);

        HBox.setHgrow(group, Priority.ALWAYS);
        HBox box = new HBox(group);
        VBox.setVgrow(box, Priority.ALWAYS);
        box.setId("demoPane");
        return box;
    }

    private static void logPropertyChange(BooleanProperty property, String name) {
        property.addListener((ch, o, n) -> LOGGER.atInfo().log("Property '{}' changed to '{}'", name, n));
    }

    private static void logStatePropertyChanges(String windowName, final DataViewWindow currentView) {
        logPropertyChange(currentView.minimisedProperty(), windowName + " minimized");
        logPropertyChange(currentView.detachedProperty(), windowName + " detached");
        logPropertyChange(currentView.closedProperty(), windowName + " closed");
        logPropertyChange(currentView.restoredProperty(), windowName + " restored");
        logPropertyChange(currentView.maximisedProperty(), windowName + " maximized");
    }

    private enum InitialWindowState {
        VISIBLE,
        MINIMISED,
        DETACHED;
    }

    private class TestChart extends XYChart {

        private TestChart() {
            super();
            getPlugins().add(new ParameterMeasurements());
            getPlugins().add(new Zoomer());
            getPlugins().add(new TableViewer());
            getPlugins().add(new EditAxis());
        }

    }

    private class UpdateTask extends TimerTask {
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
