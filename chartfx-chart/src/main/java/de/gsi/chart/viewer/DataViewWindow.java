/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.plugins.MouseEventsHelper;
import de.gsi.chart.viewer.event.WindowClosedEvent;
import de.gsi.chart.viewer.event.WindowClosingEvent;
import de.gsi.chart.viewer.event.WindowDetachedEvent;
import de.gsi.chart.viewer.event.WindowDetachingEvent;
import de.gsi.chart.viewer.event.WindowMaximisedEvent;
import de.gsi.chart.viewer.event.WindowMaximisingEvent;
import de.gsi.chart.viewer.event.WindowMinimisedEvent;
import de.gsi.chart.viewer.event.WindowMinimisingEvent;
import de.gsi.chart.viewer.event.WindowRestoredEvent;
import de.gsi.chart.viewer.event.WindowRestoringEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventSource;
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

public class DataViewWindow extends BorderPane implements EventSource {
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

    // needed for EventSource listener interface
    protected transient boolean parallelListeners = false;
    private final transient AtomicBoolean autoNotification = new AtomicBoolean(false);
    private final transient List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());

    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final HBox leftButtons = new HBox();
    private final Label titleLabel = new Label();
    private final HBox rightButtons = new HBox();
    private final HBox windowDecoration = new HBox();
    private final BooleanProperty minimisedWindow = new SimpleBooleanProperty(this, "minimisedWindow", false);
    private final BooleanProperty maximisedWindow = new SimpleBooleanProperty(this, "maximisedWindow", false);
    private final BooleanProperty restoredWindow = new SimpleBooleanProperty(this, "restoredWindow", true);
    private final BooleanProperty closedWindow = new SimpleBooleanProperty(this, "closedWindow", false);
    private final ObjectProperty<WindowState> windowState = new SimpleObjectProperty<>(this, "windowState",
            WindowState.WINDOW_RESTORED) {
        @Override
        public void set(final WindowState state) {
            // arm and allow for new notifications
            if (windowState.get().equals(state)) {
                // already armed, ignore redundant state changes
                return;
            }
            if (autoNotification.getAndSet(true)) {
                return;
            }
            // implements the window state-machine and state notifications
            switch (state) {
            case WINDOW_MINIMISED:
                setMinimised(true);
                setMaximised(false);
                setRestored(false);
                setClosed(false);
                break;
            case WINDOW_MAXIMISED:
                setMinimised(false);
                setMaximised(true);
                setRestored(false);
                setClosed(false);
                break;
            case WINDOW_RESTORED:
                setMinimised(false);
                setMaximised(false);
                setRestored(true);
                setClosed(false);
                break;
            case WINDOW_CLOSED:
                setMinimised(false);
                setMaximised(false);
                setRestored(false);
                setClosed(true);
                break;
            default:
            }

            super.set(state);

            switch (state) {
            case WINDOW_MINIMISED:
                invokeListener(new WindowMinimisedEvent(DataViewWindow.this), parallelListeners);
                break;
            case WINDOW_MAXIMISED:
                invokeListener(new WindowMaximisedEvent(DataViewWindow.this), parallelListeners);
                break;
            case WINDOW_RESTORED:
                invokeListener(new WindowRestoredEvent(DataViewWindow.this), parallelListeners);
                break;
            case WINDOW_CLOSED:
                invokeListener(new WindowClosedEvent(DataViewWindow.this), parallelListeners);
                break;
            default:
            }
            // disarm and allow for new notifications
            autoNotification.set(false);
        }
    };

    private final BooleanProperty detachedWindow = new SimpleBooleanProperty(this, "detachedWindow", false);
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
        if (autoNotification.get()) {
            // update guard
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("maximizeButtonAction");
        }
        if (!this.isMaximised()) {
            // either minimised, normal (and/or detached) state
            if (isMinimised()) {
                invokeListener(new WindowRestoringEvent(this), parallelListeners);
            } else {
                invokeListener(new WindowMaximisingEvent(this), parallelListeners);
            }
        } else {
            invokeListener(new WindowRestoringEvent(this), parallelListeners);
        }
        if (dialog.isShowing()) {
            // enlarge to maximum screen size
            dialog.maximizeRestore();
            if (dialog.isMaximised()) {
                maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_RESTORE_ICON);
                setWindowState(WindowState.WINDOW_RESTORED);
            } else {
                maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
                setWindowState(WindowState.WINDOW_MAXIMISED);
            }
            return;
        }

        if (getParentView().getMinimisedChildren().contains(this)) {
            // this DataViewWindow is minimised
            invokeListener(new WindowRestoringEvent(this), parallelListeners);
            getParentView().getMinimisedChildren().remove(this);
            setMinimised(false);
            getParentView().getVisibleChildren().add(this);
            setWindowState(WindowState.WINDOW_RESTORED);
            return;
        }

        if (this.equals(getParentView().getMaximizedChild())) {
            // this DataViewWindow is already maximised
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            getParentView().setMaximizedChild(null);
            getParentView().getVisibleChildren().add(this);
            setWindowState(WindowState.WINDOW_RESTORED);
            return;
        }

        maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_RESTORE_ICON);
        getParentView().getVisibleChildren().remove(this);
        getParentView().setMaximizedChild(this);
        setWindowState(WindowState.WINDOW_MAXIMISED);
    };

    protected final EventHandler<ActionEvent> minimizeButtonAction = event -> {
        if (autoNotification.get()) {
            // update guard
            return;
        }
        invokeListener(new WindowMinimisingEvent(this), parallelListeners);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("minimizeButtonAction");
        }
        if (dialog.isShowing()) {
            dialog.hide();
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            setWindowState(WindowState.WINDOW_MINIMISED);
            return;
        }

        if (this.equals(getParentView().getMaximizedChild())) {
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            getParentView().setMaximizedChild(null);
            getParentView().getVisibleChildren().add(this);
        }

        maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
        getParentView().getVisibleChildren().remove(this);
        getParentView().getMinimisedChildren().add(this);
        setWindowState(WindowState.WINDOW_MINIMISED);
        autoNotification.set(false);
    };

    protected EventHandler<ActionEvent> closeButtonAction = event -> {
        if (autoNotification.get()) {
            // update guard
            return;
        }
        invokeListener(new WindowClosingEvent(this), parallelListeners);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("closeButtonAction");
        }
        // asked to remove pane
        if (dialog.isShowing()) {
            dialog.hide();
            setDetached(false);
            setWindowState(WindowState.WINDOW_RESTORED);
            return;
        }

        getParentView().getMinimisedChildren().remove(this);
        getParentView().getVisibleChildren().remove(this);
        getParentView().getUndockedChildren().remove(this);

        if (this.equals(getParentView().getMaximizedChild())) {
            getParentView().setMaximizedChild(null);
        }
        setWindowState(WindowState.WINDOW_CLOSED);
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

        contentProperty().addListener((ch, o, newNode) -> setCenter(newNode));

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

        getLeftIcons().add(detachButton);
        getRightIcons().add(minimizeButton);
        getRightIcons().add(maximizeRestoreButton);
        if (addCloseButton) {
            getRightIcons().add(closeButton);
        }

        minimisedWindow.addListener((ch, o, n) -> {
            minimizeButtonAction.handle(null);
        });
        maximisedWindow.addListener((ch, o, n) -> {
            maximizeButtonAction.handle(null);
        });
        restoredWindow.addListener((ch, o, n) -> {
            maximizeButtonAction.handle(null);
        });
        detachedWindow.addListener((ch, o, n) -> {
            if (Boolean.TRUE.equals(n)) {
                dialog.show(null);
            } else {
                dialog.hide();
            }
        });
        closedWindow.addListener((ch, o, n) -> {
            if (Boolean.TRUE.equals(n)) {
                closeButtonAction.handle(null);
            }
        });

        // set actions
        // old actions
        //        detachButton.setOnAction(evt -> dialog.show(null));
        //        minimizeButton.setOnAction(minimizeButtonAction);
        //        maximizeRestoreButton.setOnAction(maximizeButtonAction);
        //        closeButton.setOnAction(closeButtonAction);
        // new actions
        detachButton.setOnAction(evt -> setDetached(true));
        minimizeButton.setOnAction(evt -> setMinimised(true));
        maximizeRestoreButton.setOnAction(maximizeButtonAction);
        closeButton.setOnAction(evt -> setClosed(true));

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

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
    }

    public BooleanProperty closedProperty() {
        return closedWindow;
    }

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    public BooleanProperty detachedProperty() {
        return detachedWindow;
    }

    /**
     * Mouse cursor to be used during drag operation.
     *
     * @return the mouse cursor property
     */
    public final ObjectProperty<Cursor> dragCursorProperty() {
        return dragCursor;
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

    public WindowState getWindowState() {
        return windowStateProperty().get();
    }

    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public boolean isClosed() {
        return closedProperty().get();
    }

    public boolean isDetached() {
        return detachedProperty().get();
    }

    public boolean isMaximised() {
        return maximisedProperty().get();
    }

    public boolean isMinimised() {
        return minimisedProperty().get();
    }

    public boolean isRestored() {
        return restoredProperty().get();
    }

    public boolean isWindowDecorationVisible() {
        return windowDecorationVisible().get();
    }

    public BooleanProperty maximisedProperty() {
        return maximisedWindow;
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

    public BooleanProperty restoredProperty() {
        return restoredWindow;
    }

    public void setClosed(final boolean state) {
        closedProperty().set(state);
    }

    public final void setContent(final Node content) {
        contentProperty().set(content);
    }

    public void setDetached(final boolean state) {
        detachedProperty().set(state);
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

    public void setMaximised(final boolean state) {
        maximisedProperty().set(state);
    }

    public void setMinimised(final boolean state) {
        minimisedProperty().set(state);
    }

    public final void setName(final String name) {
        nameProperty().set(name);
    }

    public void setRestored(final boolean state) {
        restoredProperty().set(state);
    }

    public void setWindowDecorationVisible(final boolean state) {
        windowDecorationVisible().set(state);
    }

    public void setWindowState(final WindowState state) {
        windowStateProperty().set(state);
    }

    @Override
    public String toString() {
        return DataViewWindow.class.getSimpleName() + "(\"" + this.getName() + "\")";
    }

    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    public BooleanProperty windowDecorationVisible() {
        return decorationVisible;
    }

    public ObjectProperty<WindowState> windowStateProperty() {
        return windowState;
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
        }
        this.requestFocus();

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

    private void uninstallCursor() {
        this.setCursor(originalCursor);
    }

    protected void installCursor() {
        originalCursor = this.getCursor();
        if (getDragCursor() != null) {
            this.setCursor(getDragCursor());
        }
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

    public enum WindowState {
        WINDOW_RESTORED, // normal state
        WINDOW_MINIMISED, // minimised window
        WINDOW_MAXIMISED, // maximised window
        WINDOW_CLOSED; // closed and final window state
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
                invokeListener(new WindowDetachingEvent(DataViewWindow.this), parallelListeners);
                if (DataViewWindow.this.equals(parentView.getMaximizedChild())) {
                    parentView.setMaximizedChild(null);
                }
                parentView.getMinimisedChildren().remove(DataViewWindow.this);
                parentView.getVisibleChildren().remove(DataViewWindow.this);
                parentView.getUndockedChildren().add(DataViewWindow.this);

                setCenter(null);
                dialogContent.setCenter(getContent());

                setMinimised(false);
                setMaximised(false);
                setRestored(true);
                setDetached(true);
                invokeListener(new WindowDetachedEvent(DataViewWindow.this), parallelListeners);
            });

            setOnHidden(windowEvent -> {
                invokeListener(new WindowRestoringEvent(DataViewWindow.this), parallelListeners);
                dialogContent.setCenter(null);
                setCenter(getContent());
                parentView.getUndockedChildren().remove(DataViewWindow.this);
                parentView.getVisibleChildren().add(DataViewWindow.this);

                setMinimised(false);
                setMaximised(false);
                setRestored(true);
                setDetached(false);
                invokeListener(new WindowRestoredEvent(DataViewWindow.this), parallelListeners);
            });

            this.maximizedProperty().addListener((ch, o, n) -> {
                setDetached(true);
                if (Boolean.TRUE.equals(n)) {
                    setWindowState(WindowState.WINDOW_MAXIMISED);
                } else {
                    setWindowState(WindowState.WINDOW_RESTORED);
                }
            });

            this.iconifiedProperty().addListener((ch, o, n) -> {
                setDetached(true);
                if (Boolean.TRUE.equals(n)) {
                    setWindowState(WindowState.WINDOW_MINIMISED);
                } else {
                    if (ExternalStage.this.isMaximised()) {
                        setWindowState(WindowState.WINDOW_MINIMISED);
                    } else {
                        setWindowState(WindowState.WINDOW_RESTORED);
                    }
                }
            });
        }

        public boolean isMaximised() {
            return maximized;
        }

        public void maximizeRestore() {
            if (maximized) {
                invokeListener(new WindowRestoringEvent(DataViewWindow.this), parallelListeners);
                // restore
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(getName()).log("restore window '{}'");
                }
                setWidth(width);
                setHeight(height);
                setX(posX);
                setY(posY);
                maximized = false;

                setWindowState(WindowState.WINDOW_RESTORED);
                return;
            }
            invokeListener(new WindowMaximisingEvent(DataViewWindow.this), parallelListeners);
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

            setWindowState(WindowState.WINDOW_MAXIMISED);
        }

        public void show(final MouseEvent mouseEvent) {
            if (mouseEvent == null) {
                setX(DataViewWindow.this.getScene().getWindow().getX() + 50);
                setY(DataViewWindow.this.getScene().getWindow().getY() + 50);
            } else {
                setX(mouseEvent.getScreenX());
                setY(mouseEvent.getScreenY());
            }
            invokeListener(new WindowDetachingEvent(DataViewWindow.this), parallelListeners);

            posX = getX();
            posY = getY();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(getName()).log("show window '{}'");
            }
            show();
            setMinimised(false);
            setMaximised(false);
            setRestored(true);
            setDetached(true);
            invokeListener(new WindowDetachedEvent(DataViewWindow.this), parallelListeners);
        }
    }

}
