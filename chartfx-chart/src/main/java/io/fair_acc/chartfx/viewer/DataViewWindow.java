package io.fair_acc.chartfx.viewer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javafx.beans.DefaultProperty;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.plugins.MouseEventsHelper;
import io.fair_acc.chartfx.ui.BorderedTitledPane;
import io.fair_acc.chartfx.utils.DragResizerUtil;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.MouseUtils;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.EventSource;

/**
 * DataViewWindow containing content pane (based on BorderPane) and window
 * decorations to detach, minimise, maximise, close the window.
 *
 * @author rstein
 */
@DefaultProperty(value = "content")
public class DataViewWindow extends BorderPane implements EventSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewWindow.class);
    private final BitState state = BitState.initDirty(this);
    private static final int MIN_DRAG_BORDER_WIDTH = 20;
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
    private final transient AtomicBoolean updatingStage = new AtomicBoolean(false);
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final HBox leftButtons = new HBox();
    private final Label titleLabel = new Label();
    private final HBox rightButtons = new HBox();
    private final HBox windowDecorationBar = new HBox();
    private final BooleanProperty detachableWindow = new SimpleBooleanProperty(this, "detachableWindow", true);
    private final BooleanProperty minimisedWindow = new SimpleBooleanProperty(this, "minimisedWindow", false);
    private final BooleanProperty maximisedWindow = new SimpleBooleanProperty(this, "maximisedWindow", false);
    private final BooleanProperty restoredWindow = new SimpleBooleanProperty(this, "restoredWindow", true);
    private final BooleanProperty closedWindow = new SimpleBooleanProperty(this, "closedWindow", false);
    private final ObjectProperty<WindowState> windowState = new SimpleObjectProperty<>(this, "windowState", WindowState.WINDOW_RESTORED) {
        @Override
        public void set(final WindowState state) {
            // already armed, ignore redundant state changes
            // arm and allow for new notifications
            if (windowState.get() == state) {
                // already armed, ignore redundant state changes
                return;
            }
            // implements the window state-machine and state notifications
            switch (state) {
            case WINDOW_MINIMISED:
                setMinimised(true);
                setMaximised(false);
                setRestored(false);
                break;
            case WINDOW_MAXIMISED:
                setMinimised(false);
                setMaximised(true);
                setRestored(false);
                break;
            case WINDOW_RESTORED:
                setMinimised(false);
                setMaximised(false);
                setRestored(true);
                break;
            case WINDOW_CLOSED:
                setClosed(true);
                break;
            default:
            }

            super.set(state);
            fireInvalidated(ChartBits.DataViewWindow);
        }
    };

    private final BooleanProperty detachedWindow = new SimpleBooleanProperty(this, "detachedWindow", false);
    private final ObjectProperty<WindowDecoration> windowDecoration = new SimpleObjectProperty<>(this, "windowDecoration");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");

    private final Button detachButton = new SquareButton(CSS_WINDOW_DETACH_ICON);
    private final Button minimizeButton = new SquareButton(CSS_WINDOW_MINIMIZE_ICON);
    private final Button maximizeRestoreButton = new SquareButton(CSS_WINDOW_MAXIMIZE_ICON);
    private final Button closeButton = new SquareButton(CSS_WINDOW_CLOSE_ICON);

    // second stage per DataViewWindow to be used when content is requested to be
    // detached
    private final ExternalStage dialog = new ExternalStage();

    private transient double xOffset;
    private transient double yOffset;
    private final ObjectProperty<DataView> parentView = new SimpleObjectProperty<>(this, "parentView");
    private final Predicate<MouseEvent> mouseFilter = MouseEventsHelper::isOnlyPrimaryButtonDown;

    private final ObjectProperty<Cursor> dragCursor = new SimpleObjectProperty<>(this, "dragCursor", Cursor.CLOSED_HAND);
    private Cursor originalCursor;
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    protected final Runnable maximizeButtonAction = () -> {
        if (updatingStage.get() || getParentView() == null || isDetached()) {
            // update guard
            return;
        }

        updatingStage.set(true);
        fireInvalidated(ChartBits.DataViewWindow);

        if (dialog.isShowing()) {
            // enlarge to maximum screen size
            dialog.maximizeRestore(this);
            updatingStage.set(false);
            return;
        }

        if (getParentView().getMinimisedChildren().contains(this)) {
            // this DataViewWindow is minimised
            fireInvalidated(ChartBits.DataViewWindow);
            getParentView().getMinimisedChildren().remove(this);
            setMinimised(false);
            getParentView().getVisibleChildren().add(this);
            setWindowState(WindowState.WINDOW_RESTORED);
            updatingStage.set(false);
            return;
        }

        if (this.equals(getParentView().getMaximizedChild())) {
            // this DataViewWindow is already maximised
            maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_MAXIMIZE_ICON);
            getParentView().setMaximizedChild(null);
            getParentView().getVisibleChildren().add(this);
            setWindowState(WindowState.WINDOW_RESTORED);
            updatingStage.set(false);
            return;
        }

        maximizeRestoreButton.getStyleClass().setAll(CSS_WINDOW_RESTORE_ICON);
        getParentView().getVisibleChildren().remove(this);
        getParentView().setMaximizedChild(this);
        setWindowState(WindowState.WINDOW_MAXIMISED);
        updatingStage.set(false);
    };

    protected final Runnable minimizeButtonAction = () -> {
        if (isDetached() || isMinimised()) {
            // update guard
            return;
        }
        updatingStage.set(true);

        fireInvalidated(ChartBits.DataViewWindow);
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
        setWindowState(WindowState.WINDOW_MINIMISED);
        getParentView().getMinimisedChildren().add(this);
        updatingStage.set(false);
    };

    protected Runnable closeButtonAction = () -> {
        if (updatingStage.get() || isClosed()) {
            // update guard
            return;
        }
        updatingStage.set(true);
        fireInvalidated(ChartBits.DataViewWindow);

        // asked to remove pane
        getParentView().getMinimisedChildren().remove(this);
        getParentView().getVisibleChildren().remove(this);
        // TODO: investigate why there are duplicates in the list... following is a work-around
        getParentView().getVisibleChildren().remove(this);
        getParentView().getUndockedChildren().remove(this);

        if (this.equals(getParentView().getMaximizedChild())) {
            getParentView().setMaximizedChild(null);
        }
        setWindowState(WindowState.WINDOW_CLOSED);
        updatingStage.set(false);
    };

    public DataViewWindow(@NamedArg(value = "name") final String name, @NamedArg(value = "content") final Node content) {
        this(name, content, WindowDecoration.BAR);
    }

    public DataViewWindow(final String name, final Node content, final WindowDecoration windowDecoration) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        setName(name);
        GridPane.setHgrow(this, Priority.ALWAYS);
        GridPane.setVgrow(this, Priority.ALWAYS);

        getStyleClass().setAll(CSS_WINDOW);

        contentProperty().addListener((ch, o, newNode) -> setLocalCenter(newNode));
        DragResizerUtil.makeResizable(this);

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

        windowDecorationBar.getStyleClass().setAll(CSS_WINDOW_TITLE_BAR);
        windowDecorationBar.getChildren().addAll(leftButtons, spacer1, titleLabel, spacer2, rightButtons);
        getLeftIcons().add(detachButton);
        getRightIcons().addAll(minimizeButton, maximizeRestoreButton);

        minimisedProperty().addListener((ch, o, n) -> {
            if (!isDetached()) {
                setLocalCenter(Boolean.TRUE.equals(n) ? null : getContent());
            }
        });

        windowDecorationProperty().addListener((ch, o, n) -> {
            switch (n) {
            case NONE:
            case FRAME:
                setTop(null);
                break;
            case BAR_WO_CLOSE:
                setTop(getWindowDecorationBar());
                removeCloseWindowButton();
                break;
            case BAR:
            default:
                setTop(getWindowDecorationBar());
                addCloseWindowButton();
                break;
            }
            this.setLocalCenter(getContent());
        });
        // initial setting
        setWindowDecoration(windowDecoration); // NOPMD initialisation

        minimisedWindow.addListener((ch, o, n) -> minimizeButtonAction.run());
        maximisedWindow.addListener((ch, o, n) -> maximizeButtonAction.run());
        restoredWindow.addListener((ch, o, n) -> maximizeButtonAction.run());
        detachedWindow.addListener((ch, o, n) -> {
            if (Boolean.TRUE.equals(n)) {
                if (!isDetachableWindow()) {
                    return;
                }
                dialog.show(this, null);
            } else {
                dialog.hide();
            }
        });
        closedWindow.addListener((ch, o, n) -> {
            if (Boolean.TRUE.equals(n)) {
                closeButtonAction.run();
            }
        });

        // set actions
        detachButton.setOnAction(evt -> setDetached(true));
        detachButton.setId("detachButton");
        minimizeButton.setOnAction(evt -> minimizeButtonAction.run());
        minimizeButton.setId("minimizeButton");
        maximizeRestoreButton.setOnAction(evt -> maximizeButtonAction.run());
        maximizeRestoreButton.setId("maximizeRestoreButton");
        closeButton.setOnAction(evt -> closeButtonAction.run());
        closeButton.setId("closeButton");

        // install drag handler
        windowDecorationBar.setOnMouseReleased(this::dragFinish);
        windowDecorationBar.setOnMousePressed(this::dragStart);
        windowDecorationBar.setOnMouseDragged(this::dragOngoing);

        // install hide function on external window
        dialog.setOnHiding(we -> dialog.hide(this));

        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setContent(content);
    }

    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(DataViewWindow.class.getResource(WINDOW_CSS)).toExternalForm();
    }

    public final void addCloseWindowButton() {
        final Button button = getCloseButton();
        if (!getRightIcons().contains(button)) {
            this.getRightIcons().add(button);
        }
    }

    public BooleanProperty closedProperty() {
        return closedWindow;
    }

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    /**
     *
     * @return detachableWindow property that controls whether window can be detached by dragging or not
     */
    public BooleanProperty detachableWindowProperty() {
        return detachableWindow;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataViewWindow)) {
            return false;
        }
        final DataViewWindow other = (DataViewWindow) obj;
        return Objects.equals(graphic, other.graphic) && Objects.equals(name, other.name);
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
        return parentViewProperty().get();
    }

    public ObservableList<Node> getRightIcons() {
        return rightButtons.getChildren();
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public WindowDecoration getWindowDecoration() {
        return windowDecorationProperty().get();
    }

    public WindowState getWindowState() {
        return windowStateProperty().get();
    }

    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphic, name);
    }

    public boolean isClosed() {
        return closedProperty().get();
    }

    /**
     *
     * @return true: window can be detached by dragging gesture
     */
    public boolean isDetachableWindow() {
        return detachableWindowProperty().get();
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

    public BooleanProperty maximisedProperty() {
        return maximisedWindow;
    }

    public BooleanProperty minimisedProperty() {
        return minimisedWindow;
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public ObjectProperty<DataView> parentViewProperty() {
        return parentView;
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

    /**
     *
     * @param state true: window can be detached by dragging gesture
     */
    public void setDetachableWindow(final boolean state) {
        detachableWindowProperty().set(state);
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

    public void setParentView(final DataView view) {
        parentViewProperty().set(view);
    }

    public void setRestored(final boolean state) {
        restoredProperty().set(state);
    }

    public void setWindowDecoration(final WindowDecoration state) {
        windowDecorationProperty().set(state);
    }

    @Override
    public String toString() {
        return DataViewWindow.class.getSimpleName() + "(\"" + this.getName() + "\")";
    }

    @Override
    public BitState getBitState() {
        return state;
    }

    public ObjectProperty<WindowDecoration> windowDecorationProperty() {
        return windowDecoration;
    }

    public ObjectProperty<WindowState> windowStateProperty() {
        return windowState;
    }

    /**
     * @return the windowDecorationBar
     */
    protected HBox getWindowDecorationBar() {
        return windowDecorationBar;
    }

    protected void installCursor() {
        originalCursor = this.getCursor();
        if (getDragCursor() != null) {
            this.setCursor(getDragCursor());
        }
    }

    /**
     * @param content node that put into the BoderPane centre and depending on
     *            {@link #windowDecorationProperty} with or w/o frame
     */
    protected void setLocalCenter(final Node content) {
        switch (getWindowDecoration()) {
        case FRAME:
            if (content == null) {
                setCenter(null);
                break;
            }
            final Node node = new BorderedTitledPane(getName(), content);
            node.setOnMousePressed(this::dragStart);
            node.setOnMouseDragged(this::dragOngoing);
            node.setOnMouseReleased(this::dragFinish);
            setCenter(node);
            break;
        case BAR:
        case BAR_WO_CLOSE:
        case NONE:
        default:
            setCenter(content);
            break;
        }
    }

    protected void setWindowState(final WindowState state) {
        windowStateProperty().set(state);
    }

    private void dragFinish(final MouseEvent mevt) {
        if (isMinimised() || getParentView() == null || getParentView().getMinimisedChildren().contains(this)) {
            return;
        }
        if (!mouseFilter.test(mevt)) {
            return;
        }
        uninstallCursor();

        if (!isDetachableWindow()) {
            return;
        }
        // detach only if window is dragged outside Scene
        if (getScene() == null) {
            return;
        }
        final Point2D mouseLoc = new Point2D(mevt.getScreenX(), mevt.getScreenY());
        final Window window = getScene().getWindow();
        final Bounds sceneBounds = new BoundingBox(window.getX(), window.getY(), window.getWidth(), window.getHeight());

        final Bounds screenBounds = localToScreen(WindowDecoration.FRAME.equals(getWindowDecoration()) ? this.getBoundsInLocal() : getWindowDecorationBar().getBoundsInLocal());
        if (MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, mouseLoc) > MIN_DRAG_BORDER_WIDTH && MouseUtils.mouseOutsideBoundaryBoxDistance(sceneBounds, mouseLoc) > MIN_DRAG_BORDER_WIDTH) {
            // mouse move outside window and surrounding stage detected -- launch dialog
            if (!dialog.isShowing()) {
                dialog.show(this, mevt);
                return;
            }
            dialog.setX(mevt.getScreenX() - xOffset);
            dialog.setY(mevt.getScreenY() - yOffset);
            return;
        }

        if (!dialog.isShowing()) {
            return;
        }
        dialog.setX(mevt.getScreenX() - xOffset);
        dialog.setY(mevt.getScreenY() - yOffset);
    }

    private void dragOngoing(final MouseEvent mevt) {
        if (!mouseFilter.test(mevt)) {
            return;
        }
        // launch dragging dialogue
        dialog.setX(mevt.getScreenX() - xOffset);
        dialog.setY(mevt.getScreenY() - yOffset);
    }

    private void dragStart(final MouseEvent mevt) {
        if (!mouseFilter.test(mevt)) {
            return;
        }
        installCursor();
        xOffset = mevt.getSceneX();
        yOffset = mevt.getSceneY();
    }

    private void uninstallCursor() {
        this.setCursor(originalCursor);
    }

    public enum WindowDecoration {
        NONE, // w/o any decoration
        BAR, // classic window title bar with title label and min, max, close and detach buttons
        BAR_WO_CLOSE, // classic window title bar w/o close buttons
        FRAME // decoration and window title as frame label
    }

    public enum WindowState {
        WINDOW_RESTORED, // normal state
        WINDOW_MINIMISED, // minimised window
        WINDOW_MAXIMISED, // maximised window
        WINDOW_CLOSED // closed and final window state
    }

    protected static class ExternalStage extends Stage {
        private transient double posX = 640;
        private transient double posY = 480;
        private transient double width = 640;
        private transient double height = 480;
        private transient boolean maximized;
        private final transient Scene scene = new Scene(new StackPane(), 640, 480);

        public ExternalStage() {
            setScene(scene);
        }

        public void hide(final DataViewWindow dataViewWindow) {
            titleProperty().unbind();
            scene.setRoot(new StackPane());

            dataViewWindow.setLocalCenter(dataViewWindow.getContent());
            dataViewWindow.getParentView().getUndockedChildren().remove(dataViewWindow);
            dataViewWindow.getParentView().getVisibleChildren().add(dataViewWindow);

            dataViewWindow.fireInvalidated(ChartBits.DataViewWindow);
        }

        public boolean isMaximised() {
            return maximized;
        }

        public void maximizeRestore(final DataViewWindow dataViewWindow) {
            if (maximized) {
                // restore
                setWidth(width);
                setHeight(height);
                setX(posX);
                setY(posY);
                maximized = false;

                dataViewWindow.setWindowState(WindowState.WINDOW_RESTORED);
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
            maximized = true;

            dataViewWindow.setWindowState(WindowState.WINDOW_MAXIMISED);
        }

        public void show(final DataViewWindow dataViewWindow, final MouseEvent mouseEvent) {
            if (isShowing()) {
                return;
            }

            if (mouseEvent == null) {
                setX(dataViewWindow.getScene().getWindow().getX() + 50);
                setY(dataViewWindow.getScene().getWindow().getY() + 50);
            } else {
                setX(mouseEvent.getScreenX());
                setY(mouseEvent.getScreenY());
            }

            dataViewWindow.fireInvalidated(ChartBits.DataViewWindow);

            posX = getX();
            posY = getY();

            titleProperty().set(dataViewWindow.getName());
            titleProperty().bind(dataViewWindow.nameProperty());

            // needs to be executed here to active guard above
            show();

            if (dataViewWindow.equals(dataViewWindow.getParentView().getMaximizedChild())) {
                dataViewWindow.getParentView().setMaximizedChild(null);
            }
            dataViewWindow.getParentView().getMinimisedChildren().remove(dataViewWindow);
            dataViewWindow.getParentView().getVisibleChildren().remove(dataViewWindow);
            dataViewWindow.getParentView().getUndockedChildren().add(dataViewWindow);

            dataViewWindow.setLocalCenter(null);
            final Node dataWindowContent = dataViewWindow.getContent();
            if (dataWindowContent.getParent() instanceof Pane) {
                // make sure that the content in the DataViewWindow is not attached to the previous stage/scene graph
                FXUtils.assertJavaFxThread();
                ((Pane) dataWindowContent.getParent()).getChildren().remove(dataWindowContent);
            }

            if (dataWindowContent instanceof Pane) {
                scene.setRoot((Pane) dataWindowContent);
            } else {
                scene.setRoot(new StackPane(dataWindowContent));
            }

            dataViewWindow.setWindowState(WindowState.WINDOW_RESTORED);
            dataViewWindow.setDetached(true);
            dataViewWindow.fireInvalidated(ChartBits.DataViewWindow);
        }

        private String getName() {
            return titleProperty().getValue();
        }
    }
}
