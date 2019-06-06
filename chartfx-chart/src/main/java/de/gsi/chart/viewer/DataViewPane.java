/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.utils.ScientificNotationStringConverter;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import jfxtras.labs.scene.control.window.RotateIcon;
import jfxtras.labs.scene.control.window.Window;
import jfxtras.labs.scene.control.window.WindowIcon;

public class DataViewPane extends Window {
    private final WindowIcon minimizeButton = new WindowIcon();
    private final WindowIcon maximizeRestoreButton = new WindowIcon();
    private final WindowIcon closeButton = new WindowIcon();
    private final WindowIcon detachButton = new WindowIcon();
    private final MyDialog dialog = new MyDialog();
    private double xOffset = 0;
    private double yOffset = 0;
    private XYChart chart;

    public DataViewPane(final String name, final XYChart chart) {
        super(name);
        setName(name);
        this.chart = chart;

        getStyleClass().add("data-view-pane");
        final String css = DataViewPane.class.getResource("window.css").toExternalForm();
        getStylesheets().clear();
        getStylesheets().add(css);
        getLeftIcons().add(new RotateIcon(this));

        minimizeButton.getStyleClass().setAll("window-minimize-icon2");
        maximizeRestoreButton.getStyleClass().setAll("window-maximize-icon");
        closeButton.getStyleClass().setAll("window-close-icon");
        getRightIcons().addAll(minimizeButton);
        getRightIcons().addAll(maximizeRestoreButton);
        getRightIcons().addAll(closeButton);

        // set actions
        minimizeButton.setOnAction(minimizeButtonAction);
        maximizeRestoreButton.setOnAction(maximizeButtonAction);
        closeButton.setOnAction(closeButtonAction);

        detachButton.getStyleClass().setAll("window-detach-icon");
        detachButton.setOnAction(evt -> dialog.show(null));
        getLeftIcons().add(detachButton);

        // setOnMouseClicked(mevt -> System.err.println("mouse clicked on window"));

        // setOnDragDetected(mevt -> {
        // if (isMinimized()) {
        // return;
        // }
        // System.err.println("mouse drag detected 2");
        // startFullDrag();
        // });

        // setOnMouseDragReleased(mevt -> System.err.println("mouse drag released"));

        setOnMouseReleased(mevt -> {
            final boolean isInMinimized = dataView.get().getMinimizedChildren().contains(DataViewPane.this);
            if (isMinimized() || isInMinimized) {
                return;
            }

            final Point2D mouseLoc = new Point2D(mevt.getScreenX(), mevt.getScreenY());
            final Bounds screenBounds = localToScreen(getBoundsInLocal());
            if (!screenBounds.contains(mouseLoc)) {
                System.err.println("mouse move outside window detected -- launch dialog");
                // dropped outside of node window
                if (!dialog.isShowing()) {

                    dialog.show(mevt);
                    return;
                }
                dialog.setX(mevt.getScreenX() - xOffset);
                dialog.setY(mevt.getScreenY() - yOffset);
                return;
            }

            if (dialog.isShowing()) {
                dialog.setX(mevt.getScreenX() - xOffset);
                dialog.setY(mevt.getScreenY() - yOffset);
            }
        });

        setOnMousePressed(event -> {
		    System.err.println("setOnMousePressed dialogue");
		    xOffset = event.getSceneX();
		    yOffset = event.getSceneY();
		});
        setOnMouseDragged(event -> {
		    System.err.println("dragging dialogue");
		    dialog.setX(event.getScreenX() - xOffset);
		    dialog.setY(event.getScreenY() - yOffset);
		});

        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        // chartPane.getPlugins().add((ChartPlugin<X, Y>) new CrosshairIndicator());
        chart.getPlugins().add(new EditAxis());
        chart.legendVisibleProperty().set(true);
        // TODO: axis label alignment right
        if (chart.getXAxis() instanceof DefaultNumericAxis) {
            ((DefaultNumericAxis) chart.getXAxis()).setTickLabelFormatter(new ScientificNotationStringConverter(3));
        }
        if (chart.getYAxis() instanceof DefaultNumericAxis) {
            ((DefaultNumericAxis) chart.getYAxis()).setTickLabelFormatter(new ScientificNotationStringConverter(2));
        }
        // TODO: check if we can reposition the chart legend to be more efficiently located
        // for (Node n : chartPane.getChart().lookupAll(".chart-legend")) {
        // n.setTranslateY(-50);
        // }

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        getContentPane().getChildren().add(chart);
        setPrefSize(300, 200);

        widthProperty().addListener((ch, o, n) -> chart.requestLayout());
        heightProperty().addListener((ch, o, n) -> chart.requestLayout());
        // Detect maximise event

    }

    // TODO: The two methods should be private and be called when we change the
    // view type in the toolbar
    public void switchToTableView() {
        // final SeriesTableView<Object, Object> tableView = new
        // SeriesTableView<>();
        // TODO: We should also take series from the
        // chartPane.getOverlayCharts()
        // tableView.setData(chartPane.getChart().getData());
        // TODO: needs to be implemented
        // mainPane.setCenter(tableView);
    }

    public void switchToChartView() {
        // mainPane.setCenter(new Pane(chartPane));
    }

    private final StringProperty name = new SimpleStringProperty(this, "name");

    public final StringProperty nameProperty() {
        return name;
    }

    public final String getName() {
        return nameProperty().get();
    }

    public final void setName(final String name) {
        nameProperty().set(name);
    }

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");

    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public final Node getGraphic() {
        return graphicProperty().get();
    }

    public final void setGraphic(final Node graphic) {
        graphicProperty().set(graphic);
    }

    private final ObjectProperty<DataView> dataView = new SimpleObjectProperty<>(this, "dataView");

    final void setDataView(final DataView value) {
        dataView.set(value);
    }

    @Override
    protected void layoutChildren() {
        final long start = ProcessingProfiler.getTimeStamp();
        super.layoutChildren();
        ProcessingProfiler.getTimeDiff(start, "pane updated with data set = " + chart.getDatasets().get(0).getName());
    }

    EventHandler<ActionEvent> maximizeButtonAction = event -> {
        if (dialog.isShowing()) {
            // enlarge to maximum screen size
            dialog.maximizeRestore();
            if (!dialog.isMaximized) {
                maximizeRestoreButton.getStyleClass().setAll("window-maximize-icon");
            } else {
                maximizeRestoreButton.getStyleClass().setAll("window-restore-icon");
            }
            return;
        }

        final boolean maximized = dataView.get().getMaximizedView() == DataViewPane.this;

        if (!dataView.get().getVisibleChildren().contains(DataViewPane.this)) {
            // DataViewPane is minimised
            dataView.get().getMinimizedChildren().remove(DataViewPane.this);
            DataViewPane.this.setMinimized(false);
            dataView.get().getVisibleChildren().add(DataViewPane.this);
            minimizeButton.setDisable(false);
            return;
        }

        // DataViewPane is already shown in the regular pane
        // TODO: change
        if (maximized) {
            maximizeRestoreButton.getStyleClass().setAll("window-maximize-icon");
        } else {
            maximizeRestoreButton.getStyleClass().setAll("window-restore-icon");
        }
        // maximizeRestoreButton.setGraphic(maximized ? maximizeGraphicIcon :
        // restoreGraphicIcon);
        dataView.get().setMaximizedView(maximized ? null : DataViewPane.this);
    };

    EventHandler<ActionEvent> minimizeButtonAction = event -> {
        if (dialog.isShowing()) {
            dialog.hide();
            maximizeRestoreButton.getStyleClass().setAll("window-maximize-icon");
            return;
        }

        final boolean maximized = dataView.get().getMaximizedView() == DataViewPane.this;
        if (!maximized) {
            maximizeRestoreButton.getStyleClass().setAll("window-maximize-icon");
        } else {
            dataView.get().setMaximizedView(null);
        }
        DataViewPane.this.setMinimized(true);
        dataView.get().getMinimizedChildren().add(DataViewPane.this);
        dataView.get().getVisibleChildren().remove(DataViewPane.this);
        minimizeButton.setDisable(true);
    };

    EventHandler<ActionEvent> closeButtonAction = event -> {
        System.err.println("asked to remove pane");
        if (dialog.isShowing()) {
            dialog.hide();
            return;
        }

        final boolean maximized = dataView.get().getMaximizedView() == DataViewPane.this;
        if (maximized) {
            dataView.get().setMaximizedView(null);
        }
        if (dataView.get().getMinimizedChildren() == null) {
            System.err.println("getMinimizedChildren list is null");
        }
        if (dataView.get().getVisibleChildren() == null) {
            System.err.println("getVisibleChildren list is null");
        }
        dataView.get().getMinimizedChildren().remove(DataViewPane.this);
        dataView.get().getVisibleChildren().remove(DataViewPane.this);
        dataView.get().getChildren().remove(DataViewPane.this);
    };

    class MyDialog extends Stage {
        private final BorderPane dialogContent = new BorderPane();
        private final Scene scene = new Scene(dialogContent, 640, 480);
        private double posX = 640;
        private double posY = 480;
        private double width = 640;
        private double height = 480;
        private boolean isMaximized = false;

        public MyDialog() {
            super();
            // initStyle(StageStyle.UNDECORATED);
            // initStyle(StageStyle.TRANSPARENT);
            titleProperty().bind(DataViewPane.this.titleProperty());
            setScene(scene);

            setOnShown(windowEvent -> {
                dataView.get().getMinimizedChildren().remove(DataViewPane.this);
                dataView.get().getVisibleChildren().remove(DataViewPane.this);

                dialogContent.setCenter(DataViewPane.this);
            });

            setOnHidden(windowEvent -> {
                // dialogContent.getChildren().remove(DataViewPane.this);
                dialogContent.setCenter(null);
                dataView.get().getVisibleChildren().add(DataViewPane.this);
            });

        }

        public void maximizeRestore() {
            if (isMaximized) {
                // restore
                setWidth(width);
                setHeight(height);
                setX(posX);
                setY(posY);
                isMaximized = false;
                return;
            }
            final Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            width = getWidth();
            height = getHeight();
            posX = getX();
            posY = getY();

            // set Stage boundaries to visible bounds of the main screen
            setX(primaryScreenBounds.getMinX());
            setY(primaryScreenBounds.getMinY());
            setWidth(primaryScreenBounds.getWidth());
            setHeight(primaryScreenBounds.getHeight());
            isMaximized = true;
        }

        public void show(final MouseEvent mouseEvent) {
            if (mouseEvent == null) {
                setX(DataViewPane.this.getScene().getWindow().getX() + 50);
                setY(DataViewPane.this.getScene().getWindow().getY() + 50);
            } else {
                setX(mouseEvent.getScreenX());
                setY(mouseEvent.getScreenY());
            }

            posX = getX();
            posY = getY();

            show();
        }
    }
}
