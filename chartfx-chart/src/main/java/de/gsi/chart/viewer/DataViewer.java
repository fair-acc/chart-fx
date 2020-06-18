package de.gsi.chart.viewer;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.ui.TilingPane;
import de.gsi.chart.ui.TilingPane.Layout;
import de.gsi.chart.viewer.DataViewWindow.WindowDecoration;
import de.gsi.dataset.utils.NoDuplicatesList;

/**
 * DataViewer to manage multiple 'DatavView's which contain either custom 'Pane' derived nodes and or managed
 * 'DataViewWindow' that can be detached and re-attached to the DataView
 * <p>
 * usage example:
 * <pre>
 * <code>
 * final DataView view1 = new DataView("ChartViews", chartIcon);
 * // [..] these nodes are detachable sub-windows (with decorated minimize, maximize, close buttons)
 * view1.getVisibleChildren().addAll(customNode1, customNode2, customNode3);
 *
 *  // [..] add undecorated custom Pan
 * final DataView view2 = new DataView("Custom View", customViewIcon, getDemoPane());
 *
 * final DataViewer viewer = new DataViewer();
 * viewer.getViews().addAll(view1, view2);
 *
 * // [..] to switch between configurations one may use:
 * // a) Toolbar which contains the 'Text-Icon' Buttons for each view
 * viewer.getToolBar();
 * // b) directly/programmatically, e.g.:
 * viewer.setSelectedView(view2);
 *
 * </code>
 * </pre>
 *
 * @author Grzegorz Kruk
 * @author rstein
 */
@DefaultProperty(value = "views")
public class DataViewer extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewer.class);
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private final ObjectProperty<WindowDecoration> windowDecoration = new SimpleObjectProperty<>(this, "windowDecoration", WindowDecoration.BAR);
    private final BooleanProperty detachableWindow = new SimpleBooleanProperty(this, "detachableWindow", true);
    private final Glyph rootGlyph = new Glyph(FONT_AWESOME, "\uf698").size(FONT_SIZE);
    private final DataView dataViewRoot = new DataView("root", rootGlyph, null);
    private final HBox viewList = new HBox();
    private final Separator separator1 = new Separator(Orientation.HORIZONTAL);
    private final Separator separator2 = new Separator(Orientation.HORIZONTAL);
    private final ToolBar toolBar;
    private final ObservableList<Node> userToolBarItems = FXCollections.observableList(new NoDuplicatesList<Node>());
    private final BooleanProperty showListStyleDataViews = new SimpleBooleanProperty(this, "listStyleViews", false);
    // private final VisibleViewerPane visibleViewerPane = new VisibleViewerPane();
    // private final VBox viewerPane;
    // private final SplitPane splitPane = new SplitPane();
    // private final TreeView<Node> explorerTreeView = new TreeView<>();
    protected final BooleanProperty explorerVisible = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            // visibleViewerPane.getChildren().setAll(getValue().getViewerPanes());
            requestLayout();
        }
    };
    protected final ListChangeListener<? super DataView> subDataViewChangeListener = change -> {
        while (change.next()) {
            final DataView activeView = dataViewRoot.getActiveView();
            if (change.getAddedSize() > 0 && (activeView == null || activeView == dataViewRoot)) { // NOPMD
                dataViewRoot.setActiveSubView(change.getAddedSubList().get(0));
            }
        }
    };
    protected final ChangeListener<? super DataView> activeSubDataViewChangeListener = (ch, o, n) -> {
        if (n == null) {
            dataViewRoot.getChildren().clear();
            updateToolBar();
            return;
        }
        dataViewRoot.getChildren().setAll(n);
        if (!dataViewRoot.getSubDataViews().contains(n)) {
            dataViewRoot.getSubDataViews().add(n);
        }
        updateToolBar();
    };
    protected final ChangeListener<? super Boolean> closeWindowButtonHandler = (ch, o, n) -> {
        switch (getWindowDecoration()) {
        case BAR:
        case BAR_WO_CLOSE:
            this.setWindowDecoration(Boolean.TRUE.equals(n) ? WindowDecoration.BAR : WindowDecoration.BAR_WO_CLOSE);
            break;
        case NONE:
        case FRAME:
        default:
            break;
        }
    };

    public DataViewer() {
        super();
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
        getStylesheets().add(getClass().getResource("DataViewer.css").toExternalForm());

        dataViewRoot.getSubDataViews().addListener(subDataViewChangeListener);
        dataViewRoot.activeSubViewProperty().addListener(activeSubDataViewChangeListener);
        userToolBarItems.addListener((ListChangeListener<Node>) change -> updateToolBar());
        showListStyleDataViews.addListener((ch, o, n) -> updateToolBar());
        selectedViewProperty().addListener((ch, o, n) -> updateToolBar());

        windowDecorationProperty().addListener((ch, o, n) -> updateWindowDecorations(dataViewRoot));
        detachableWindowProperty().addListener((ch, o, n) -> updateDetachableWindowProperty(dataViewRoot));

        final Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolBar = new ToolBar(separator1, viewList, separator2, spacer);
        this.setCenter(dataViewRoot);
        requestLayout();
    }

    public DataViewer(final DataView... views) {
        this();
        getViews().addAll(views);
    }

    /**
     * 
     * @return detachableWindow property that controls whether window can be detached by dragging or not
     */
    public BooleanProperty detachableWindowProperty() {
        return detachableWindow;
    }

    /**
     * Determines if the explorer view is visible.
     *
     * @return boolean property (true: visible)
     */
    public BooleanProperty explorerVisibleProperty() {
        return explorerVisible;
    }

    public final DataView getSelectedView() {
        return selectedViewProperty().get();
    }

    public ToolBar getToolBar() {
        return toolBar;
    }

    public ObservableList<Node> getUserToolBarItems() {
        return userToolBarItems;
    }

    /**
     * Returns a modifiable list of views displayed by the viewer.
     *
     * @return list of views
     */
    public final ObservableList<DataView> getViews() {
        return dataViewRoot.getSubDataViews();
    }

    public WindowDecoration getWindowDecoration() {
        return windowDecorationProperty().get();
    }

    /**
     * 
     * @return true: window can be detached by dragging gesture
     */
    public boolean isDetachableWindow() {
        return detachableWindowProperty().get();
    }

    /**
     * Returns the value of the {@link #explorerVisibleProperty()}.
     *
     * @return {@code true} if the explorer view is visible, {@code false} otherwise
     */
    public final boolean isExplorerVisible() {
        return explorerVisibleProperty().get();
    }

    public final ObjectProperty<DataView> selectedViewProperty() {
        return dataViewRoot.activeSubViewProperty();
    }

    /**
     * 
     * @param state true: window can be detached by dragging gesture
     */
    public void setDetachableWindow(final boolean state) {
        detachableWindowProperty().set(state);
    }

    /**
     * Sets the value of the {@link #explorerVisibleProperty()}.
     *
     * @param value {@code true} to make the explorer visible, {@code false} to make it invisible
     */
    public final void setExplorerVisible(final boolean value) {
        explorerVisibleProperty().set(value);
    }

    public final void setSelectedView(final DataView selectedView) {
        selectedViewProperty().set(selectedView);
    }

    public final void setSelectedView(final String viewName) {
        for (DataView view : getViews()) {
            if (view.getName() != null && view.getName().equals(viewName)) {
                setSelectedView(view);
                return;
            }
        }
    }

    public void setWindowDecoration(final WindowDecoration state) {
        windowDecorationProperty().set(state);
    }

    public BooleanProperty showListStyleDataViewProperty() {
        return showListStyleDataViews;
    }

    public void updateMenuButton(Menu menuButton, DataView dataView) {
        for (DataView view : dataView.getSubDataViews()) {
            final String name = view.getName();
            final Node icon = view.getIcon();

            if (view.getSubDataViews().isEmpty()) {
                MenuItem menuItem = new MenuItem(name, icon); // NOPMD - allocation within loop ok in this context

                menuItem.setOnAction(evt -> dataView.setView(view));
                menuButton.getItems().add(menuItem);
                continue;
            }

            Menu subMenuButton = new Menu(name, icon); // NOPMD - allocation within loop ok in this context
            subMenuButton.setOnAction(evt -> dataView.setView(view));
            menuButton.getItems().add(subMenuButton);
            updateMenuButton(subMenuButton, view);
        }
    }

    public ObjectProperty<WindowDecoration> windowDecorationProperty() {
        return windowDecoration;
    }

    protected void updateDetachableWindowProperty(final DataView root) {
        for (DataView view : root.getSubDataViews()) {
            updateDetachableWindowProperty(view);
        }

        if (root.getContentPane() == null) {
            return;
        }
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(root.getContentPane());
        nodeList.addAll(root.getChildren());
        nodeList.addAll(root.getContentPane().getChildren());
        // check for child in content, DataView and Content itself
        for (Node child : nodeList) {
            if (!(child instanceof DataViewWindow)) {
                continue;
            }
            DataViewWindow window = (DataViewWindow) child;
            window.setDetachableWindow(isDetachableWindow());
        }
    }

    protected void updateToolBar() { // NOPMD
        toolBar.getItems().clear();
        toolBar.getItems().addAll(userToolBarItems);
        if (getSelectedView() == null) {
            return;
        }

        toolBar.getItems().add(separator1);

        // add view button
        for (DataView view : dataViewRoot.getSubDataViews()) {
            final String name = view.getName();
            final Node icon = view.getIcon();
            final Button viewButton;
            if (icon == null && name == null) {
                viewButton = new Button(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.QUESTION)); // NOPMD - allocation within loop ok in this context
            } else if (icon == null) {
                viewButton = new Button(name, null); // NOPMD - allocation within loop ok in this context
            } else {
                viewButton = new Button(null, icon); // NOPMD - allocation within loop ok in this context
            }
            viewButton.setTooltip(new Tooltip("activates view '" + name + "'")); // NOPMD - allocation within loop ok in this context

            if (!showListStyleDataViewProperty().get()) {
                viewButton.setOnAction(evt -> this.setSelectedView(view));
                toolBar.getItems().add(viewButton);
            }
        }
        // menu style list
        if (showListStyleDataViewProperty().get()) {
            Menu rootMenu = new Menu(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.LIST_UL));
            MenuBar menuButton = new MenuBar(rootMenu);
            updateMenuButton(rootMenu, dataViewRoot);
            if (!rootMenu.getItems().isEmpty()) {
                toolBar.getItems().add(menuButton);
            }
        }

        if (getSelectedView().isStandalone()) {
            toolBar.requestLayout();
            return;
        }

        toolBar.getItems().add(separator2);

        final Button sortButton = new SquareButton(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.SORT_ALPHA_ASC));
        sortButton.setTooltip(new Tooltip("sort children"));

        sortButton.setOnAction(evt -> getSelectedView().getActiveView().sort());
        toolBar.getItems().add(sortButton);

        // add layout buttons

        for (DataView view : getSelectedView().getSubDataViews()) {
            if (!(view.getContentPane() instanceof TilingPane)) {
                continue;
            }
            TilingPane tilingPane = (TilingPane) view.getContentPane();
            Layout layout = tilingPane.getLayout();
            final Button selectionButton = new SquareButton(null, layout.getIcon()); // NOPMD
            selectionButton.setTooltip(new Tooltip("configure pane for " + layout.getName() + "-style layout")); // NOPMD
            selectionButton.setOnAction(evt -> getSelectedView().setView(view));

            if (!layout.equals(Layout.MAXIMISE)) {
                toolBar.getItems().add(selectionButton);
            }
        }
        toolBar.requestLayout();
    }

    protected void updateWindowDecorations(final DataView root) {
        for (DataView view : root.getSubDataViews()) {
            updateWindowDecorations(view);
        }

        if (root.getContentPane() == null) {
            return;
        }
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(root.getContentPane());
        nodeList.addAll(root.getChildren());
        nodeList.addAll(root.getContentPane().getChildren());
        // check for child in content, DataView and Content itself
        for (Node child : nodeList) {
            if (!(child instanceof DataViewWindow)) {
                continue;
            }
            DataViewWindow window = (DataViewWindow) child;
            window.setWindowDecoration(getWindowDecoration());
        }
    }
}
