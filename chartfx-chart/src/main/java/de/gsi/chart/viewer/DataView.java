package de.gsi.chart.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.viewer.DataViewTilingPane.Layout;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Holds all charts/tables to be displayed
 *
 * @author Grzegorz Kruk (original idea)
 * @author rstein (adapted to JDVE&lt;-&gt;JavaFX bridge
 */
public class DataView extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataView.class);
    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final boolean standalone;
    private final ToolBar toolBar = new ToolBar();

    private final FlowPane minimisedElements = new FlowPane();
    private final Pane contentPane;
    protected transient DataView activeView;
    private final ObservableList<DataView> subDataViews = FXCollections.observableArrayList();
    private final ObservableList<Node> visibleNodes = FXCollections.observableArrayList();
    private final ObservableList<DataViewWindow> visibleChildren = FXCollections.observableArrayList();
    private final ObservableList<DataViewWindow> minimisedChildren = FXCollections.observableArrayList();
    private final ObservableList<DataViewWindow> undockedChildren = FXCollections.observableArrayList();
    private final ObjectProperty<DataViewWindow> maximizedChild = new SimpleObjectProperty<>(this, "maximizedView") {
        private Optional<DataView> lastActiveView = Optional.empty();

        @Override
        public void set(final DataViewWindow newNode) {
            super.set(newNode);

            if (newNode == null) {
                if (lastActiveView.isPresent()) {
                    setView(lastActiveView.get());
                }
                lastActiveView = Optional.empty();
            } else {
                if (lastActiveView.isEmpty()) {
                    lastActiveView = Optional.of(activeView);
                }
                setNodeLayout(Layout.MAXIMISE);
            }

        }
    };

    public DataView(final String name) {
        this(name, null, true);
        addStandardViews(); // NOPMD, calling of overridable protected method
    }

    public DataView(final String name, final Pane pane) {
        this(name, pane, true);
        subDataViews.add(this);
    }

    protected DataView(final String name, final Pane pane, final boolean isStandalone) {
        super();
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        setName(name);
        contentPane = pane == null ? new StackPane() : pane;
        activeView = this;
        this.standalone = isStandalone;
        VBox.setVgrow(minimisedElements, Priority.NEVER);
        HBox.setHgrow(minimisedElements, Priority.NEVER);
        setFillWidth(true);

        registerListListener(); // NOPMD, calling of overridable protected method
    }

    protected void addStandardViews() {
        for (final Layout layout : Layout.values()) {
            final DataView dataView = new DataView(layout.getName(), new DataViewTilingPane(layout), false); // NOPMD
            final Button selectionButton = new Button(null, layout.getIcon()); // NOPMD
            selectionButton.setTooltip(new Tooltip("configure pane for " + layout.getName() + "-style layout")); // NOPMD
            selectionButton.setOnAction(evt -> setView(dataView));

            subDataViews.add(dataView);
            if (!layout.equals(Layout.MAXIMISE)) {
                toolBar.getItems().add(selectionButton);
            }
        }
        setNodeLayout(Layout.GRID); // NOPMD
    }

    private static void removeChildFromList(final List<DataViewWindow> list, final Node node) {
        Optional<DataViewWindow> found = list.stream().filter(content -> node.equals(content.getWindowContent())).findFirst();
        if (found.isPresent()) {
            list.remove(found.get());
        }
    }

    protected void registerListListener() {
        visibleNodes.addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                for (final Node node : change.getRemoved()) {
                    removeChildFromList(visibleChildren, node);
                    removeChildFromList(minimisedChildren, node);
                    removeChildFromList(undockedChildren, node);
                    if (node.equals(maximizedChild.get())) {
                        setMaximizedChild(null);
                    }
                }

                change.getAddedSubList().forEach(c -> {
                    HBox.setHgrow(c, Priority.ALWAYS);
                    VBox.setVgrow(c, Priority.ALWAYS);
                    if (c instanceof DataViewWindow) {
                        if (activeView.getContentPane().getChildren().contains(c)) {
                            return;
                        }
                        activeView.getContentPane().getChildren().add(c);
                        return;
                    }
                    visibleChildren.add(new DataViewWindow(DataView.this, "", c)); // NOPMD
                });
            }
        });

        visibleChildren.addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (activeView != null) {
                    change.getRemoved().forEach(c -> activeView.getContentPane().getChildren().remove(c));
                    change.getAddedSubList().forEach(c -> activeView.getContentPane().getChildren().add(c));
                }
            }
        });

        minimisedChildren.addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                minimisedElements.getChildren().removeAll(change.getRemoved());
                minimisedElements.getChildren().addAll(change.getAddedSubList());
            }
        });
    }

    public DataView getActiveView() {
        return activeView;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataView)) {
            return false;
        }
        final DataView other = (DataView) obj;
        return getName().equals(other.getName());
    }

    public final DataViewWindow getMaximizedChild() {
        return maximizedChildProperty().get();
    }

    public final ObservableList<DataViewWindow> getMinimisedChildren() {
        return minimisedChildren;
    }

    public final Pane getMinimisedElementsPane() {
        return minimisedElements;
    }

    public final String getName() {
        return nameProperty().get();
    }

    public Pane getContentPane() {
        return contentPane;
    }

    public ToolBar getToolBar() {
        return toolBar;
    }

    public final ObservableList<DataViewWindow> getUndockedChildren() {
        return undockedChildren;
    }

    public final ObservableList<DataView> getSubDataViews() {
        return subDataViews;
    }

    public final ObservableList<DataViewWindow> getVisibleChildren() {
        return visibleChildren;
    }

    public final ObservableList<Node> getVisibleNodes() {
        return visibleNodes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name.get() == null) ? 0 : name.get().hashCode());
        return result;
    }

    /**
     * @return the isStandalone
     */
    public boolean isStandalone() {
        return standalone;
    }

    public final ObjectProperty<DataViewWindow> maximizedChildProperty() {
        return maximizedChild;
    }

    public final StringProperty nameProperty() {
        return name;
    }

    public final void setMaximizedChild(final DataViewWindow view) {
        maximizedChildProperty().set(view);
    }

    public final void setName(final String name) {
        nameProperty().set(name);
    }

    public void setNodeLayout(final Layout nodeLayout) {
        final Optional<DataView> match = getSubDataViews().stream()
                .filter(p -> p.getName().equals(nodeLayout.getName())).findFirst();
        if (match.isPresent()) {
            setView(match.get());
            return;
        }
        LOGGER.atWarn().addArgument(nodeLayout).log("could not find view for requested  layout '{}'");
    }

    public void setView(final String viewerPaneName) {
        if (viewerPaneName == null) {
            LOGGER.atWarn().log("viewerPaneName is null");
            return;
        }
        Optional<DataView> match = getSubDataViews().stream().filter(c -> c.getName().equals(viewerPaneName))
                .findFirst();
        if (match.isEmpty()) {
            LOGGER.atWarn().addArgument(viewerPaneName).log("no DataView for viewerPaneName '{}'");
            return;
        }
        setView(match.get());
    }

    public void setView(final DataView viewerPane) {
        if (viewerPane == null || viewerPane.equals(activeView)) {
            return;
        }
        if (!getSubDataViews().contains(viewerPane)) {
            getSubDataViews().add(viewerPane);
        }

        activeView = viewerPane;
        if (viewerPane.isStandalone()) {
            getChildren().setAll(activeView.getContentPane());
            return;
        }

        for (final DataView subView : subDataViews) {
            if (!subView.isStandalone()) {
                subView.getChildren().clear();
            }
        }
        if (getMaximizedChild() == null) {
            activeView.getContentPane().getChildren().setAll(getVisibleChildren());
        } else {
            activeView.getContentPane().getChildren().setAll(getMaximizedChild());
        }
        getChildren().setAll(activeView.getContentPane(), minimisedElements);
    }

    @Override
    public String toString() {
        return DataView.class.getSimpleName() + "(\"" + getName() + "\")";
    }

    protected Collection<Node> getWrappedChildren(final Collection<Node> children) {
        final Collection<Node> newNodes = new ArrayList<>();
        for (final Node node : children) {
            if (node instanceof DataViewWindow) {
                newNodes.add(node);
                continue;
            }
            final DataViewWindow window = new DataViewWindow(this, "", node); // NOPMD
            window.setMinimized(true);
            newNodes.add(window);
        }
        return newNodes;
    }
}
