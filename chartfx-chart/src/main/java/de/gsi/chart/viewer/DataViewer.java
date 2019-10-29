/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.viewer.DataViewTilingPane.Layout;
import de.gsi.dataset.utils.NoDuplicatesList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

/**
 *
 */
public class DataViewer extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewer.class);
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private final BooleanProperty decorationVisible = new SimpleBooleanProperty(this, "windowDecorationVisible", true);
    private final BooleanProperty closeWindowButtonVisible = new SimpleBooleanProperty(this, "closeButtonVisible",
            true);
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

    private final BooleanProperty explorerVisible = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            // visibleViewerPane.getChildren().setAll(getValue().getViewerPanes());
            requestLayout();
        }
    };

    public DataViewer() {
        super();
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
        getStylesheets().add(getClass().getResource("DataViewer.css").toExternalForm());

        dataViewRoot.getSubDataViews().addListener((ListChangeListener<DataView>) change -> {
            while (change.next()) {
                final DataView activeView = dataViewRoot.getActiveView();
                if (change.getAddedSize() > 0 && (activeView == null || activeView == dataViewRoot)) {
                    dataViewRoot.setActiveSubView(change.getAddedSubList().get(0));
                }
            }
        });

        dataViewRoot.activeSubViewProperty().addListener((ch, o, n) -> {
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
        });

        userToolBarItems.addListener((ListChangeListener<Node>) change -> updateToolBar());
        showListStyleDataViews.addListener((ch, o, n) -> updateToolBar());
        selectedViewProperty().addListener((ch, o, n) -> updateToolBar());
        windowDecorationVisible().addListener((ch, o, n) -> this.updateWindowDecorations(dataViewRoot));
        closeWindowButtonVisibleProperty().addListener((ch, o, n) -> this.updateCloseWindowButton(dataViewRoot, n));

        final Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolBar = new ToolBar(separator1, viewList, separator2, spacer);
        this.setCenter(dataViewRoot);
        requestLayout();

    }

    public BooleanProperty closeWindowButtonVisibleProperty() {
        return closeWindowButtonVisible;
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

    public boolean isCloseWindowButtonVisible() {
        return closeWindowButtonVisibleProperty().get();
    }

    /**
     * Returns the value of the {@link #explorerVisibleProperty()}.
     *
     * @return {@code true} if the explorer view is visible, {@code false} otherwise
     */
    public final boolean isExplorerVisible() {
        return explorerVisibleProperty().get();
    }

    public boolean isWindowDecorationVisible() {
        return windowDecorationVisible().get();
    }

    public final ObjectProperty<DataView> selectedViewProperty() {
        return dataViewRoot.activeSubViewProperty();
    }

    public void setCloseWindowButtonVisible(final boolean state) {
        closeWindowButtonVisibleProperty().set(state);
    }

    /**
     * Sets the value of the {@link #explorerVisibleProperty()}.
     *
     * @param value {@code true} to make the explorer visible, {@code false} to make
     *            it invisible
     */
    public final void setExplorerVisible(final boolean value) {
        explorerVisibleProperty().set(value);
    }

    public final void setSelectedView(final DataView selectedView) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(selectedView).log("setSelectedView('{}')");
        }
        selectedViewProperty().set(selectedView);
    }

    public final void setSelectedView(final String viewName) {
        setSelectedView(viewName);
    }

    public void setWindowDecorationVisible(final boolean state) {
        windowDecorationVisible().set(state);
    }

    public BooleanProperty showListStyleDataViewProperty() {
        return showListStyleDataViews;
    }

    public void updateMenuButton(Menu menuButton, DataView dataView) {
        for (DataView view : dataView.getSubDataViews()) {
            final String name = view.getName();
            final Node icon = view.getIcon();

            if (view.getSubDataViews().isEmpty()) {
                MenuItem menuItem = new MenuItem(name, icon);

                menuItem.setOnAction(evt -> dataView.setView(view));
                menuButton.getItems().add(menuItem);
                continue;
            }

            Menu subMenuButton = new Menu(name, icon);
            subMenuButton.setOnAction(evt -> dataView.setView(view));
            menuButton.getItems().add(subMenuButton);
            updateMenuButton(subMenuButton, view);
        }
    }

    public BooleanProperty windowDecorationVisible() {
        return decorationVisible;
    }

    protected void updateCloseWindowButton(final DataView root, final boolean state) {
        for (DataView view : root.getSubDataViews()) {
            updateCloseWindowButton(view, state);
        }

        List<Node> nodeList = new ArrayList<>();
        if (root.getContentPane() != null) {
            nodeList.add(root.getContentPane());
        }
        nodeList.addAll(root.getChildren());
        nodeList.addAll(root.getContentPane().getChildren());
        // check for child in content, DataView and Content itself
        for (Node child : nodeList) {
            if (!(child instanceof DataViewWindow)) {
                continue;
            }
            DataViewWindow window = (DataViewWindow) child;
            if (state) {
                window.addCloseWindowButton();
            } else {
                window.removeCloseWindowButton();
            }
        }
    }

    protected void updateToolBar() {
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
                viewButton = new Button(null, new Glyph(FONT_AWESOME, FontAwesome.Glyph.QUESTION));
            } else if (icon == null) {
                viewButton = new Button(name, null);
            } else {
                viewButton = new Button(null, icon);
            }
            viewButton.setTooltip(new Tooltip("activates view '" + name + "'"));

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
            if (!(view.getContentPane() instanceof DataViewTilingPane)) {
                continue;
            }
            DataViewTilingPane tilingPane = (DataViewTilingPane) view.getContentPane();
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
            window.setWindowDecorationVisible(this.isWindowDecorationVisible());
        }
    }
}
