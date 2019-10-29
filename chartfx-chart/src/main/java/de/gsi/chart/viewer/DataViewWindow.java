/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import jfxtras.labs.scene.control.window.WindowIcon;

public class DataViewWindow extends BorderPane {
    private static final String CSS_WINDOW_DETACH_ICON = "window-detach-icon";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewWindow.class);
    private static final String WINDOW_CSS = "window.css";
    private static final String CSS_DATA_VIEW_PANE = "data-view-pane";
    private static final String CSS_WINDOW_CLOSE_ICON = "window-close-icon";
    private static final String CSS_WINDOW_MINIMIZE_ICON2 = "window-minimize-icon2";
    private static final String CSS_WINDOW_RESTORE_ICON = "window-restore-icon";
    private static final String CSS_WINDOW_MAXIMIZE_ICON = "window-maximize-icon";
    private static final String CSS_WINDOW_TITLE_BAR = "window-titlebar";
    private static final String CSS_TITLE_LABEL = "title-label";

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final HBox leftButtons = new HBox();
    private final Label label = new Label();
    private final HBox rightButtons = new HBox();
    private final HBox windowDecoration = new HBox();
    private final BooleanProperty minimisedWindow = new SimpleBooleanProperty(this, "minimisedWindow", false);
    private final BooleanProperty decorationVisible = new SimpleBooleanProperty(this, "windowDecorationVisible", false);
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");

    private final transient WindowIcon minimizeButton = new MyWindowIcon();
    private final transient WindowIcon maximizeRestoreButton = new MyWindowIcon();
    private final transient WindowIcon closeButton = new MyWindowIcon();
    private final transient WindowIcon detachButton = new MyWindowIcon();
    private final transient ExternalStage dialog = new ExternalStage();
    private transient double xOffset;
    private transient double yOffset;
    private final DataView parentView;

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");

    protected final EventHandler<ActionEvent> maximizeButtonAction = event -> {
        LOGGER.atDebug().log("maximizeButtonAction");
        if (dialog.isShowing()) {
            // enlarge to maximum screen size
            dialog.maximizeRestore();
            if (dialog.isMaximised()) {
                maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_RESTORE_ICON);
            } else {
                maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            }
            return;
        }

        if (getParentView().getMinimisedChildren().contains(this)) {
            // this DataViewWindow is minimised
            getParentView().getMinimisedChildren().remove(this);
            setMinimised(false);
            getParentView().getVisibleChildren().add(this);
            return;
        }

        if (equals(getParentView().getMaximizedChild())) {
            // this DataViewWindow is already maximised
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            getParentView().setMaximizedChild(null);
            getParentView().getVisibleChildren().add(this);
            return;
        }

        maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_RESTORE_ICON);
        getParentView().getVisibleChildren().remove(this);
        getParentView().setMaximizedChild(this);
    };

    protected final EventHandler<ActionEvent> minimizeButtonAction = event -> {
        LOGGER.atDebug().log("minimizeButtonAction");
        if (dialog.isShowing()) {
            dialog.hide();
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            return;
        }

        if (equals(getParentView().getMaximizedChild())) {
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            getParentView().setMaximizedChild(null);
            getParentView().getVisibleChildren().add(this);
        }

        maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
        setMinimised(true);
        getParentView().getVisibleChildren().remove(this);
        getParentView().getMinimisedChildren().add(this);
    };

    protected EventHandler<ActionEvent> closeButtonAction = event -> {
        LOGGER.atDebug().log("closeButtonAction");
        // asked to remove pane
        if (dialog.isShowing()) {
            dialog.hide();
            return;
        }

        getParentView().getMinimisedChildren().remove(this);
        getParentView().getVisibleChildren().remove(this);
        getParentView().getUndockedChildren().remove(this);

        if (equals(getParentView().getMaximizedChild())) {
            getParentView().setMaximizedChild(null);
        }
    };

    public DataViewWindow(final DataView parent, final String name, final Node content) {
        super();
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        parentView = parent;
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }

        contentProperty().addListener((ch, o, n) -> setCenter(n));
        label.textProperty().bindBidirectional(nameProperty());

        final Pane spacer1 = new Pane();
        final Pane spacer2 = new Pane();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox.setHgrow(label, Priority.ALWAYS);

        leftButtons.setPrefWidth(USE_COMPUTED_SIZE);
        rightButtons.setPrefWidth(USE_COMPUTED_SIZE);
        HBox.setHgrow(leftButtons, Priority.SOMETIMES);
        HBox.setHgrow(rightButtons, Priority.SOMETIMES);

        windowDecoration.getChildren().addAll(leftButtons, spacer1, label, spacer2, rightButtons);
        minimisedProperty().addListener((ch, o, n) -> setCenter(n ? null : getContent()));
        windowDecorationVisible().addListener((ch, o, n) -> setTop(n ? null : windowDecoration));
        windowDecoration.setPrefHeight(15);
        windowDecoration.getStyleClass().setAll(CSS_WINDOW_TITLE_BAR);
        setTop(windowDecoration);

        getStyleClass().add(CSS_DATA_VIEW_PANE);
        final String css = DataViewWindow.class.getResource(WINDOW_CSS).toExternalForm();
        getStylesheets().clear();
        getStylesheets().add(css);
        //getLeftIcons().add(new RotateIcon(this));

        label.getStyleClass().setAll(CSS_TITLE_LABEL);
        minimizeButton.getStyleClass().setAll(CSS_WINDOW_MINIMIZE_ICON2);
        maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
        closeButton.getStyleClass().setAll(CSS_WINDOW_CLOSE_ICON);
        getRightIcons().addAll(minimizeButton);
        getRightIcons().addAll(maximizeRestoreButton);
        getRightIcons().addAll(closeButton);

        // set actions
        minimizeButton.setOnAction(minimizeButtonAction);
        maximizeRestoreButton.setOnAction(maximizeButtonAction);
        closeButton.setOnAction(closeButtonAction);

        detachButton.getStyleClass().setAll(CSS_WINDOW_DETACH_ICON);
        detachButton.setOnAction(evt -> dialog.show(null));
        getLeftIcons().add(detachButton);

        setOnMouseReleased(mevt -> {
            if (isMinimised() || parentView.getMinimisedChildren().contains(this)) {
                return;
            }

            final Point2D mouseLoc = new Point2D(mevt.getScreenX(), mevt.getScreenY());
            final Bounds screenBounds = localToScreen(getBoundsInLocal());
            if (!screenBounds.contains(mouseLoc)) {
                // mouse move outside window detected -- launch dialog
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
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        setOnMouseDragged(event -> {
            // launch dragging dialogue
            dialog.setX(event.getScreenX() - xOffset);
            dialog.setY(event.getScreenY() - yOffset);
        });

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setName(name);
        setContent(content);
    }

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    public Node getContent() {
        return contentProperty().get();
    }

    public final Node getGraphic() {
        return graphicProperty().get();
    }

    public ObservableList<Node> getLeftIcons() {
        return leftButtons.getChildren();
    }

    public final String getName() {
        return nameProperty().get();
    }

    public DataView getParentView() {
        return parentView;
    }

    public ObservableList<Node> getRightIcons() {
        return rightButtons.getChildren();
    }

    public Label getTitleLabel() {
        return label;
    }

    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public boolean isMinimised() {
        return minimisedProperty().get();
    }

    public boolean isWindowDecorationVisible() {
        return windowDecorationVisible().get();
    }

    public BooleanProperty minimisedProperty() {
        return minimisedWindow;
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public final void setContent(final Node content) {
        contentProperty().set(content);
    }

    public final void setGraphic(final Node graphic) {
        graphicProperty().set(graphic);
    }

    public void setMinimised(final boolean state) {
        minimisedProperty().set(state);
    }

    public final void setName(final String name) {
        nameProperty().set(name);
    }

    public void setWindowDecorationVisible(final boolean state) {
        windowDecorationVisible().set(state);
    }

    public BooleanProperty windowDecorationVisible() {
        return decorationVisible;
    }

    @Override
    protected void layoutChildren() {
        final long start = ProcessingProfiler.getTimeStamp();
        super.layoutChildren();
        if (getContent() instanceof Chart) {
            ProcessingProfiler.getTimeDiff(start,
                    "pane updated with data set = " + ((Chart) getContent()).getDatasets().get(0).getName());
        }
    }

    protected class ExternalStage extends Stage {
        private transient double posX = 640;
        private transient double posY = 480;
        private transient double width = 640;
        private transient double height = 480;
        private transient boolean maximized;

        public ExternalStage() {
            super();
            titleProperty().bind(nameProperty());
            final BorderPane dialogContent = new BorderPane();
            final Scene scene = new Scene(dialogContent, 640, 480);
            setScene(scene);

            setOnShown(windowEvent -> {
                if (DataViewWindow.this.equals(parentView.getMaximizedChild())) {
                    parentView.setMaximizedChild(null);
                }
                parentView.getMinimisedChildren().remove(DataViewWindow.this);
                parentView.getVisibleChildren().remove(DataViewWindow.this);
                parentView.getUndockedChildren().add(DataViewWindow.this);

                setCenter(null);
                dialogContent.setCenter(getContent());
            });

            setOnHidden(windowEvent -> {
                dialogContent.setCenter(null);
                setCenter(getContent());
                parentView.getUndockedChildren().remove(DataViewWindow.this);
                parentView.getVisibleChildren().add(DataViewWindow.this);
            });

        }

        public boolean isMaximised() {
            return maximized;
        }

        public void maximizeRestore() {
            if (maximized) {
                // restore
                LOGGER.atDebug().addArgument(getName()).log("restore window '{}'");
                setWidth(width);
                setHeight(height);
                setX(posX);
                setY(posY);
                maximized = false;
                return;
            }
            final Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            width = getWidth();
            height = getHeight();
            posX = getX();
            posY = getY();
            LOGGER.atDebug().addArgument(getName()).log("maximise window '{}'");
            // set Stage boundaries to visible bounds of the main screen
            setX(primaryScreenBounds.getMinX());
            setY(primaryScreenBounds.getMinY());
            setWidth(primaryScreenBounds.getWidth());
            setHeight(primaryScreenBounds.getHeight());
            maximized = true;
        }

        public void show(final MouseEvent mouseEvent) {
            if (mouseEvent == null) {
                setX(DataViewWindow.this.getScene().getWindow().getX() + 50);
                setY(DataViewWindow.this.getScene().getWindow().getY() + 50);
            } else {
                setX(mouseEvent.getScreenX());
                setY(mouseEvent.getScreenY());
            }

            posX = getX();
            posY = getY();
            LOGGER.atDebug().addArgument(getName()).log("show window '{}'");
            show();
        }
    }

    protected class MyWindowIcon extends WindowIcon {
        MyWindowIcon() {
            ChangeListener<? super Number> listener = (ch, o, n) -> {
                final double marginBar = DataViewWindow.this.windowDecoration.getInsets().getTop()
                        + DataViewWindow.this.windowDecoration.getInsets().getBottom();
                final double marginButton = this.getInsets().getTop() + this.getInsets().getBottom();
                final double max = DataViewWindow.this.windowDecoration.getHeight() - marginBar - marginButton;
                this.setPrefSize(max, max);
            };
            this.widthProperty().addListener(listener);
            this.heightProperty().addListener(listener);
            DataViewWindow.this.windowDecoration.heightProperty().addListener(listener);
        }
    }
}
