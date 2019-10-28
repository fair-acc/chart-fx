/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.viewer;

import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
    private final Glyph rootGlyph = new Glyph(FONT_AWESOME, "\uf698").size(FONT_SIZE);
    private final DataView dataViewRoot = new DataView("root", rootGlyph, null, true);
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
                if (!dataViewRoot.getSubDataViews().isEmpty() && (dataViewRoot.getActiveView() == null)) {
                    dataViewRoot.setActiveSubView(dataViewRoot.getSubDataViews().get(0));
                }
            }
        });

        dataViewRoot.activeSubViewProperty().addListener((ch, o, n) -> {
            if (n == null) {
                setCenter(null);
                return;
            }
            setCenter(n);
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
        return dataViewRoot.getSubDataViews();
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
     * Sets the value of the {@link #explorerVisibleProperty()}.
     *
     * @param value {@code true} to make the explorer visible, {@code false} to make
     *            it invisible
     */
    public final void setExplorerVisible(final boolean value) {
        explorerVisibleProperty().set(value);
    }

    public final void setSelectedView(final DataView selectedView) {
        selectedViewProperty().set(selectedView);
    }

    public final void setView(final DataView dataView) {
        LOGGER.atInfo().addArgument(dataView).log("setView('{}')");
        dataViewRoot.activeSubViewProperty().set(dataView);
    }

    public final void setView(final String viewName) {
        dataViewRoot.setView(viewName);
    }

    public void sort() {
        // visibleViewerPane.sort();
    }
}
