/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 *
 */
public class DataViewer extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewer.class);
    private final DataView dataViewRoot = new DataView("root");
    private final ObjectProperty<DataView> selectedView = new SimpleObjectProperty<>(this, "selectedView");
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
        getStylesheets().add(getClass().getResource("DataViewer.css").toExternalForm());

        dataViewRoot.getSubDataViews().addListener((ListChangeListener<DataView>) change -> {
            while (change.next()) {
                if (!dataViewRoot.getSubDataViews().isEmpty() && selectedView.get() == null) {
                    setSelectedView(dataViewRoot.getSubDataViews().get(0));
                }
            }
        });

        selectedView.addListener((ch, o, n) -> {
            setCenter(n);
            if (n == null) {
                return;
            }
            if (!dataViewRoot.getSubDataViews().contains(n)) {
                dataViewRoot.getSubDataViews().add(n);
            }
        });

        requestLayout();

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

    /**
     * Returns a modifiable list of views displayed by the viewer.
     *
     * @return list of views
     */
    public final ObservableList<DataView> getViews() {
        return this.dataViewRoot.getSubDataViews();
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
        return selectedView;
    }

    /**
     * Sets the value of the {@link #explorerVisibleProperty()}.
     *
     * @param value {@code true} to make the explorer visible, {@code false} to make
     *              it invisible
     */
    public final void setExplorerVisible(final boolean value) {
        explorerVisibleProperty().set(value);
    }

    public final void setSelectedView(final DataView selectedView) {
        selectedViewProperty().set(selectedView);
    }

    public final void setView(final DataView dataView) {
        selectedView.set(dataView);
        // dataViewRoot.setView(dataView);
    }

    public final void setView(final String viewName) {
        dataViewRoot.setView(viewName);
        selectedView.set(dataViewRoot.getActiveView());
    }

    public void sort() {
        // visibleViewerPane.sort();
    }

    // @Override
    // protected void layoutChildren() {
    // super.layoutChildren();
    // // mainPane.resizeRelocate(0, 0, getWidth(), getHeight());
    // }

    private static <T> boolean listEqualsIgnoreOrder(final ObservableList<Node> observableList,
            final ObservableList<Node> observableList2) {
        return new HashSet<>(observableList).equals(new HashSet<>(observableList2));
    }
}
