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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 *
 */
public class DataViewer extends BorderPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewer.class);
    private final ObservableList<DataView> views = FXCollections.observableArrayList();
    //    private final VisibleViewerPane visibleViewerPane = new VisibleViewerPane();
    //    private final VBox viewerPane;

    //    private final SplitPane splitPane = new SplitPane();
    //    private final TreeView<Node> explorerTreeView = new TreeView<>();

    private final BooleanProperty explorerVisible = new SimpleBooleanProperty(false);

    private final ObjectProperty<DataView> selectedView = new SimpleObjectProperty<>(this, "selectedView") {

        @Override
        protected void invalidated() {
            //            visibleViewerPane.getChildren().setAll(getValue().getViewerPanes());
            requestLayout();
        }
    };

    public DataViewer() {
        super();
        getStylesheets().add(getClass().getResource("DataViewer.css").toExternalForm());

        //        VBox.setVgrow(visibleViewerPane, Priority.ALWAYS);
        //        viewerPane = new VBox(visibleViewerPane, minimizedViewerPane);

        //        setCenter(viewerPane);

        final ListChangeListener<? super DataView> childChangeListener = (final Change<? extends DataView> c) -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("added child -> update pane");
            }
            layout();
        };    

        final ListChangeListener<Node> viewsChangeListener = change -> {
            if (views.size() == 1) {
                setSelectedView(views.get(0));
            }
            requestLayout();
        };
        views.addListener(viewsChangeListener);

        requestLayout();
    }

    /**
     * Determines if the explorer view is visible.
     *
     * @return boolean property (true: visible)
     */
    public final BooleanProperty explorerVisibleProperty() {
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
        return views;
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
     * @param value {@code true} to make the explorer visible, {@code false} to make it invisible
     */
    public final void setExplorerVisible(final boolean value) {
        explorerVisibleProperty().set(value);
    }

    public final void setSelectedView(final DataView selectedView) {
        selectedViewProperty().set(selectedView);
    }

    public void sort() {
        //        visibleViewerPane.sort();
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
