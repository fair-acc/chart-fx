/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 *
 */
public class DataViewer extends BorderPane {

    private final ObservableList<DataView> views = FXCollections.observableArrayList();
    private final VisibleViewerPane visibleViewerPane = new VisibleViewerPane();
    private final MinimizedViewerPane minimizedViewerPane = new MinimizedViewerPane();
    private final VBox viewerPane;

    //    private final SplitPane splitPane = new SplitPane();
    //    private final TreeView<Node> explorerTreeView = new TreeView<>();

    private final BooleanProperty explorerVisible = new SimpleBooleanProperty(false);

    private final ObjectProperty<DataView> selectedView = new SimpleObjectProperty<DataView>(this, "selectedView") {

        @Override
        protected void invalidated() {
            visibleViewerPane.getChildren().setAll(getValue().getChildren());
            requestLayout();
        }
    };

    public DataViewer() {
        super();
        getStylesheets().add(getClass().getResource("DataViewer.css").toExternalForm());

        VBox.setVgrow(visibleViewerPane, Priority.ALWAYS);
        VBox.setVgrow(minimizedViewerPane, Priority.SOMETIMES);
        viewerPane = new VBox(visibleViewerPane, minimizedViewerPane);

        setCenter(viewerPane);

        final ListChangeListener<? super DataViewPane> childChangeListener = (
                final Change<? extends DataViewPane> c) -> {
            System.err.println("added child -> update pane");
            //requestLayout();
            layout();
        };

        final ChangeListener<DataViewPane> maximizedViewPropertyListner = (obs, oldV, newV) -> {
            visibleViewerPane.requestLayout();
            minimizedViewerPane.requestLayout();
            requestLayout();
        };

        final ListChangeListener<DataViewPane> minimizedViewPropertyListner = (
                final Change<? extends DataViewPane> c) -> {
            minimizedViewerPane.requestLayout();
            // visibleViewerPane.requestLayout();
            requestLayout();
        };

        final ListChangeListener<DataViewPane> visibleViewPropertyListner = (
                final Change<? extends DataViewPane> c) -> {
            visibleViewerPane.requestLayout();
            // minimizedViewerPane.requestLayout();
            requestLayout();
        };

        final ListChangeListener<DataView> viewsChangeListener = change -> {
            if (views.size() == 1) {
                setSelectedView(views.get(0));
            }
            requestLayout();
        };
        views.addListener(viewsChangeListener);

        selectedViewProperty().addListener((obs, oldView, newView) -> {
            if (oldView != null) {
                oldView.getChildren().removeListener(childChangeListener);
                oldView.maximizedViewProperty().removeListener(maximizedViewPropertyListner);
                oldView.getMinimizedChildren().removeListener(minimizedViewPropertyListner);
                oldView.getVisibleChildren().removeListener(visibleViewPropertyListner);
                System.err.println("old view");
            }

            if (newView != null) {
                newView.getChildren().addListener(childChangeListener);
                newView.maximizedViewProperty().addListener(maximizedViewPropertyListner);
                newView.getMinimizedChildren().addListener(minimizedViewPropertyListner);
                newView.getVisibleChildren().addListener(visibleViewPropertyListner);
                System.err.println("new view");
            }
            requestLayout();
        });
        requestLayout();
    }

    private static <T> boolean listEqualsIgnoreOrder(final ObservableList<Node> observableList,
            final ObservableList<DataViewPane> observableList2) {
        return new HashSet<>(observableList).equals(new HashSet<>(observableList2));
    }

    /**
     * Determines if the explorer view is visible.
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

    // @Override
    // protected void layoutChildren() {
    // super.layoutChildren();
    // // mainPane.resizeRelocate(0, 0, getWidth(), getHeight());
    // }

    public void sort() {
        visibleViewerPane.sort();
    }

    private class MinimizedViewerPane extends GridPane {

        MinimizedViewerPane() {
            getStyleClass().add("minimizedviewer-pane");
        }

        private void removeOldChildren(final DataView view) {
            final ObservableList<Node> existingChildren = getChildren();
            for (final Node child : existingChildren) {
                if (child instanceof DataViewPane) {
                    final DataViewPane addedPane = (DataViewPane) child;
                    if (!view.getMinimizedChildren().contains(addedPane)) {
                        getChildren().remove(addedPane);
                    }
                }
            }
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            final DataView view = getSelectedView();
            if (view == null) {
                return;
            }

            removeOldChildren(view);

            // System.err.println("layout minimized view");

            int countX = 0;
            int countY = 0;
            for (final DataViewPane child : view.getMinimizedChildren()) {
                child.setVisible(true);
                GridPane.setColumnIndex(child, countX);
                GridPane.setRowIndex(child, countY);
                GridPane.setFillWidth(child, true);
                if (!MinimizedViewerPane.this.getChildren().contains(child)) {
                    System.err.println("minimized added child = " + child);
                    MinimizedViewerPane.this.getChildren().add(child);
                }

                // child.resize(200, 20);
                // child.resizeRelocate(countX, countY, getPrefTileWidth(), getPrefTileHeight());
                countX++;
                if (countX > 3) {
                    countY++;
                    countX = 0;
                }

            }
            // TODO: relayout super Vbox
            viewerPane.requestLayout();
        }
    }

    private class VisibleViewerPane extends GridPane {

        private int oldColsCount = -1;

        VisibleViewerPane() {
            super();
            getStyleClass().add("viewer-pane");

        }

        private int getColumnsCount(final DataView.Layout layout, final int childCount) {
            switch (layout) {
            case HBOX:
                return childCount;
            case VBOX:
                return 1;
            case GRID:
            default:
                if (childCount < 4) {
                    return 1;
                }
                int ncols = (int) Math.ceil(Math.sqrt(childCount));
                if (ncols == 0) {
                    ncols = 1;
                }
                return ncols;
            }
        }

        private void layoutMaximized(final DataView view) {
            for (final DataViewPane child : view.getVisibleChildren()) {
                if (child == view.getMaximizedView()) {
                    if (!getChildren().contains(child)) {
                        System.err.println("maximized added child = " + child);
                        getChildren().add(child);
                    }
                    child.resizeRelocate(0, 0, getWidth(), getHeight());
                    child.setVisible(true);
                } else {
                    child.setVisible(false);
                    child.resizeRelocate(0, 0, 0, 0);
                }
            }
        }

        private void layoutNormal(final DataView view) {
            final DataView.Layout layout = view.getLayout() == null ? DataView.Layout.GRID : view.getLayout();
            final int childCount = view.getVisibleChildren().size();
            final int colsCount = getColumnsCount(layout, childCount);

            if (oldColsCount != colsCount) {
                final ArrayList<ColumnConstraints> colConstraintList = new ArrayList<>();
                for (int i = 0; i < colsCount; i++) {
                    final ColumnConstraints colConstraints = new ColumnConstraints();
                    colConstraints.setPercentWidth(100.0 / colsCount);
                    colConstraints.setHgrow(Priority.ALWAYS); // allow column to grow
                    colConstraints.setFillWidth(true);
                    colConstraintList.add(colConstraints);
                }

                getColumnConstraints().clear();
                getColumnConstraints().addAll(colConstraintList);
                System.err.println("update column constraints");
                oldColsCount = colsCount;
            }

            if (!DataViewer.listEqualsIgnoreOrder(getChildren(), view.getVisibleChildren())) {
                System.err.println("cleared dataset");
                getChildren().clear();
            }
            int rowIndex = 0;
            int colIndex = 0;
            for (final DataViewPane child : view.getVisibleChildren()) {
                // GridPane.setFillWidth(child, true);
                // GridPane.setFillHeight(child, true);
                GridPane.setHgrow(child, Priority.ALWAYS);
                GridPane.setVgrow(child, Priority.ALWAYS);
                child.setVisible(true);
                child.setPrefSize(300, 200);
                GridPane.setColumnIndex(child, colIndex);
                GridPane.setRowIndex(child, rowIndex);
                if (!VisibleViewerPane.this.getChildren().contains(child)) {
                    VisibleViewerPane.this.getChildren().add(child);
                }
                // System.out.println(String.format("add (layout=%s) child %s at %dx%d", layout, child.getName(),
                // rowIndex,
                // colIndex));

                colIndex++;
                if (colIndex >= colsCount) {
                    colIndex = 0;
                    rowIndex++;
                }
            }

            if (rowIndex == 0) {
                rowIndex++;
            }
        }

        private void removeOldChildren(final DataView view) {

            // getChildren().setAll(view.getVisibleChildren());
            // for (int index = 0; index < getChildren().size(); index++) {
            // final Node child = getChildren().get(index);
            // if (child instanceof DataViewPane) {
            // final DataViewPane addedPane = (DataViewPane) child;
            // if (!view.getVisibleChildren().contains(addedPane)) {
            // getChildren().remove(addedPane);
            // }
            // }
            // }
            //
            //
            // for (final DataViewPane child : view.getVisibleChildren()) {
            // if (!VisibleViewerPane.this.getChildren().contains(child)) {
            // System.err.println("visible added child = " + child);
            // // getChildren().add(child);
            // }
            // }

        }

        private void sort() {
            final DataView view = getSelectedView();
            final ObservableList<DataViewPane> workingCollection = FXCollections
                    .observableArrayList(view.getVisibleChildren());
            Collections.sort(workingCollection, (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }

                return o1.titleProperty().getValue().compareTo(o2.titleProperty().getValue());
            });
            view.getVisibleChildren().setAll(workingCollection);
            layoutChildren();
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            // System.err.println(String.format("layout visible -> size %d x %d", (int) getWidth(), (int) getHeight()));
            final DataView view = getSelectedView();
            if (view == null) {
                return;
            }

            removeOldChildren(view);

            if (view.getMaximizedView() == null) {
                layoutNormal(view);
            } else {
                layoutMaximized(view);
            }
        }
    }
}
