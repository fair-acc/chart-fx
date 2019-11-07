/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.plugins.MouseEventsHelper;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DataViewWindow extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewWindow.class);
    private static final String WINDOW_CSS = "DataViewer.css";
    private static final String CSS_WINDOW = "window";
    private static final String CSS_WINDOW_DETACH_ICON = "window-detach-icon";
    private static final String CSS_WINDOW_CLOSE_ICON = "window-close-icon";
    private static final String CSS_WINDOW_MINIMIZE_ICON = "window-minimize-icon2";
    private static final String CSS_WINDOW_RESTORE_ICON = "window-restore-icon";
    private static final String CSS_WINDOW_MAXIMIZE_ICON = "window-maximize-icon";
    private static final String CSS_WINDOW_TITLE_BAR = "window-titlebar";
    private static final String CSS_TITLE_LABEL = "window-titlelabel";

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final HBox leftButtons = new HBox();
    private final Label titleLabel = new Label();
    private final HBox rightButtons = new HBox();
    private final HBox windowDecoration = new HBox();
    private final BooleanProperty minimisedWindow = new SimpleBooleanProperty(this, "minimisedWindow", false);
    private final BooleanProperty decorationVisible = new SimpleBooleanProperty(this, "windowDecorationVisible", true);
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");

    private final Button detachButton = new SquareButton(CSS_WINDOW_DETACH_ICON);
    private final Button minimizeButton = new SquareButton(CSS_WINDOW_MINIMIZE_ICON);
    private final Button maximizeRestoreButton = new SquareButton(CSS_WINDOW_MAXIMIZE_ICON);
    private final Button closeButton = new SquareButton(CSS_WINDOW_CLOSE_ICON);

    private final ExternalStage dialog = new ExternalStage();

    private transient double xOffset;
    private transient double yOffset;
    private final DataView parentView;
    private final Predicate<MouseEvent> mouseFilter = MouseEventsHelper::isOnlyPrimaryButtonDown;

    private final ObjectProperty<Cursor> dragCursor = new SimpleObjectProperty<>(this, "dragCursor",
            Cursor.CLOSED_HAND);
    private Cursor originalCursor;
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    protected final EventHandler<ActionEvent> maximizeButtonAction = event -> {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("maximizeButtonAction");
        }
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("minimizeButtonAction");
        }
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("closeButtonAction");
        }
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
        this(parent, name, content, true, true);
    }

    public DataViewWindow(final DataView parent, final String name, final Node content,
            final boolean windowDecorationsVisible, final boolean addCloseButton) {
        super();
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        parentView = parent;
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }

        getStyleClass().setAll(CSS_WINDOW);
        final String css = DataViewWindow.class.getResource(WINDOW_CSS).toExternalForm();
        getStylesheets().clear();
        getStylesheets().add(css);

        contentProperty().addListener((ch, o, n) -> setCenter(n));

        leftButtons.setPrefWidth(USE_COMPUTED_SIZE);
        HBox.setHgrow(leftButtons, Priority.SOMETIMES);
        final Pane spacer1 = new Pane();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        titleLabel.getStyleClass().setAll(CSS_TITLE_LABEL);
        titleLabel.textProperty().bindBidirectional(nameProperty());
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        final Pane spacer2 = new Pane();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        rightButtons.setPrefWidth(USE_COMPUTED_SIZE);
        HBox.setHgrow(rightButtons, Priority.SOMETIMES);

        windowDecoration.getChildren().addAll(leftButtons, spacer1, titleLabel, spacer2, rightButtons);
        minimisedProperty().addListener((ch, o, n) -> setCenter(n ? null : getContent()));
        windowDecorationVisible().addListener((ch, o, n) -> setTop(n ? windowDecoration : null));
        windowDecoration.getStyleClass().setAll(CSS_WINDOW_TITLE_BAR);
        windowDecorationVisible().set(windowDecorationsVisible);
        if (windowDecorationsVisible) {
            setTop(windowDecoration);
        }

        getLeftIcons().addAll(detachButton);
        getRightIcons().addAll(minimizeButton);
        getRightIcons().addAll(maximizeRestoreButton);
        getRightIcons().addAll(closeButton);

        // set actions
        detachButton.setOnAction(evt -> dialog.show(null));
        minimizeButton.setOnAction(minimizeButtonAction);
        maximizeRestoreButton.setOnAction(maximizeButtonAction);
        if (addCloseButton) {
            closeButton.setOnAction(closeButtonAction);
        }

        // install drag handler
        setOnMouseReleased(this::dragFinish);
        windowDecoration.setOnMousePressed(this::dragStart);
        setOnMouseDragged(this::dragOngoing);

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setName(name);
        setContent(content);
    }

    public final void addCloseWindowButton() {
        final Button button = getCloseButton();
        if (!getRightIcons().contains(button)) {
            this.getRightIcons().add(button);
        }
    }

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    /**
     * Mouse cursor to be used during drag operation.
     *
     * @return the mouse cursor property
     */
    public final ObjectProperty<Cursor> dragCursorProperty() {
        return dragCursor;
    }

    private void dragFinish(final MouseEvent mevt) {
        if (isMinimised() || parentView.getMinimisedChildren().contains(this)) {
            return;
        }
        if (mouseFilter != null && !mouseFilter.test(mevt)) {
            return;
        }
        uninstallCursor();

        final Point2D mouseLoc = new Point2D(mevt.getScreenX(), mevt.getScreenY());
        final Bounds screenBounds = localToScreen(windowDecoration.getBoundsInLocal());
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
        } else {
            this.requestFocus();
        }

        if (dialog.isShowing()) {
            dialog.setX(mevt.getScreenX() - xOffset);
            dialog.setY(mevt.getScreenY() - yOffset);
        }
    }

    private void dragOngoing(final MouseEvent mevt) {
        if (mouseFilter != null && !mouseFilter.test(mevt)) {
            return;
        }
        // launch dragging dialogue
        dialog.setX(mevt.getScreenX() - xOffset);
        dialog.setY(mevt.getScreenY() - yOffset);
    }

    private void dragStart(final MouseEvent mevt) {
        if (mouseFilter != null && !mouseFilter.test(mevt)) {
            return;
        }
        installCursor();
        xOffset = mevt.getSceneX();
        yOffset = mevt.getSceneY();
    }

    /**
     * @return the closeButton
     */
    public Button getCloseButton() {
        return closeButton;
    }

    public Node getContent() {
        return contentProperty().get();
    }

    /**
     * @return the detachButton
     */
    public Button getDetachButton() {
        return detachButton;
    }

    /**
     * @return the dialog
     */
    public ExternalStage getDialog() {
        return dialog;
    }

    /**
     * Returns the value of the {@link #dragCursorProperty()}
     *
     * @return the current cursor
     */
    public final Cursor getDragCursor() {
        return dragCursorProperty().get();
    }

    public final Node getGraphic() {
        return graphicProperty().get();
    }

    public ObservableList<Node> getLeftIcons() {
        return leftButtons.getChildren();
    }

    /**
     * @return the maximizeRestoreButton
     */
    public Button getMaximizeRestoreButton() {
        return maximizeRestoreButton;
    }

    /**
     * @return the minimizeButton
     */
    public Button getMinimizeButton() {
        return minimizeButton;
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
        return titleLabel;
    }

    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    protected void installCursor() {
        originalCursor = this.getCursor();
        if (getDragCursor() != null) {
            this.setCursor(getDragCursor());
        }
    }

    public boolean isMinimised() {
        return minimisedProperty().get();
    }

    public boolean isWindowDecorationVisible() {
        return windowDecorationVisible().get();
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

    public BooleanProperty minimisedProperty() {
        return minimisedWindow;
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public final void removeCloseWindowButton() {
        final Button button = getCloseButton();
        this.getLeftIcons().remove(button);
        this.getRightIcons().remove(button);
    }

    public final void setContent(final Node content) {
        contentProperty().set(content);
    }

    /**
     * Sets value of the {@link #dragCursorProperty()}.
     *
     * @param cursor the cursor to be used by the plugin
     */
    public final void setDragCursor(final Cursor cursor) {
        dragCursorProperty().set(cursor);
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

    @Override
    public String toString() {
        return DataViewWindow.class.getSimpleName() + this.getName();
    }

    private void uninstallCursor() {
        this.setCursor(originalCursor);
    }

    public BooleanProperty windowDecorationVisible() {
        return decorationVisible;
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
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(getName()).log("restore window '{}'");
                }
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(getName()).log("maximise window '{}'");
            }
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(getName()).log("show window '{}'");
            }
            show();
        }
    }

}
