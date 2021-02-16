package de.gsi.chart.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.DefaultProperty;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.ui.TilingPane;
import de.gsi.chart.ui.TilingPane.Layout;
import de.gsi.chart.viewer.DataViewWindow.WindowState;
import de.gsi.dataset.utils.NoDuplicatesList;

/**
 * Holds all charts/tables or custom panes to be displayed
 *
 * @author Grzegorz Kruk (original idea)
 * @author rstein (adapted to ChartFX)
 */
@DefaultProperty(value = "visibleChildren")
public class DataView extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataView.class);
    private final StringProperty name = new SimpleStringProperty(this, "name");
    private final ObjectProperty<Node> icon = new SimpleObjectProperty<>(this, "icon");
    private final boolean standalone;

    private final FlowPane minimisedElements = new FlowPane();
    private final ObjectProperty<Pane> contentPane = new SimpleObjectProperty<>(this, "contenPane");
    private final ObjectProperty<DataView> activeSubView = new SimpleObjectProperty<>(this, "activeView");
    private final ObservableList<DataView> subDataViews = FXCollections.observableList(new NoDuplicatesList<>());
    private final ObservableList<Node> visibleNodes = FXCollections.observableList(new NoDuplicatesList<>());
    private final ObservableList<DataViewWindow> visibleChildren = FXCollections.observableList(new NoDuplicatesList<>());
    private final ObservableList<DataViewWindow> minimisedChildren = FXCollections.observableList(new NoDuplicatesList<>());
    private final ObservableList<DataViewWindow> undockedChildren = FXCollections.observableList(new NoDuplicatesList<>());
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
                    lastActiveView = Optional.of(activeSubView.get());
                }
                setNodeLayout(Layout.MAXIMISE);
            }
        }
    };

    public DataView(@NamedArg(value = "name") final String name, @NamedArg(value = "icon") final Node icon) {
        this(name, icon, null, false);
        addStandardViews(); // NOPMD, calling of overridable protected method
    }

    public DataView(@NamedArg(value = "name") final String name, @NamedArg(value = "icon") final Node icon, @NamedArg(value = "pane") final Pane pane) {
        this(name, icon, pane, true);
    }

    protected DataView(final String name, final Node icon, final Pane pane, final boolean isStandalone) {
        super();

        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);
        standalone = isStandalone;
        VBox.setVgrow(minimisedElements, Priority.NEVER);
        HBox.setHgrow(minimisedElements, Priority.NEVER);
        minimisedElements.setPrefWrapLength(0.0);
        final ChangeListener<Number> widthChange = (ch, o, n) -> minimisedElements.setPrefWrapLength(n.doubleValue());
        this.contentPaneProperty().addListener((ch, o, n) -> {
            if (o != null) {
                o.widthProperty().removeListener(widthChange);
            }
            if (n != null) {
                n.widthProperty().addListener(widthChange);
            }
        });

        setFillWidth(true);
        setActiveSubView(this);

        registerListListener(); // NOPMD, calling of overridable protected method

        activeSubView.addListener((ch, o, n) -> {
            if (n == null) {
                getChildren().clear();
                return;
            }

            if (!getSubDataViews().contains(n)) {
                getSubDataViews().add(n);
            }

            if (n.isStandalone()) {
                getChildren().setAll(n);
                return;
            }

            // add dependent sub-DataView (e.g. HBox, VBox, Grid-style layout)
            for (final DataView subView : getSubDataViews()) {
                if (!subView.isStandalone()) {
                    subView.getContentPane().getChildren().clear();
                }
            }

            if (getMaximizedChild() == null) {
                getVisibleChildren().forEach(child -> {
                    if (!getActiveView().getContentPane().getChildren().contains(child)) {
                        getActiveView().getContentPane().getChildren().add(child);
                    }
                });
            } else {
                getActiveView().getContentPane().getChildren().setAll(getMaximizedChild());
            }

            getChildren().setAll(getActiveView().getContentPane(), minimisedElements);
        });

        setName(name == null ? "" : name);
        setIcon(icon);
        setContentPane(pane == null ? new StackPane() : pane);
        if (this.standalone) {
            getChildren().setAll(getContentPane()); // NOPMD
        }
    }

    public ObjectProperty<DataView> activeSubViewProperty() {
        return activeSubView;
    }

    public ObjectProperty<Pane> contentPaneProperty() {
        return contentPane;
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

    public DataView getActiveView() {
        return activeSubViewProperty().get();
    }

    public Pane getContentPane() {
        return contentPaneProperty().get();
    }

    public Node getIcon() {
        return iconProperty().get();
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

    public final ObservableList<DataView> getSubDataViews() {
        return subDataViews;
    }

    public final ObservableList<DataViewWindow> getUndockedChildren() {
        return undockedChildren;
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

    public final ObjectProperty<Node> iconProperty() {
        return icon;
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

    public void setActiveSubView(final DataView pane) {
        activeSubViewProperty().set(pane);
    }

    public final void setContentPane(final Pane pane) {
        contentPaneProperty().set(pane);
    }

    public final void setIcon(final Node icon) {
        iconProperty().set(icon);
    }

    public final void setMaximizedChild(final DataViewWindow view) {
        maximizedChildProperty().set(view);
    }

    public final void setName(final String name) {
        nameProperty().set(name);
    }

    public void setNodeLayout(final Layout nodeLayout) {
        final Optional<DataView> match = getSubDataViews().stream().filter(p -> p.getName().equals(nodeLayout.getName())).findFirst();
        if (match.isPresent()) {
            setView(match.get());
            return;
        }
        LOGGER.atWarn().addArgument(nodeLayout).log("could not find view for requested  layout '{}'");
    }

    public void setView(final DataView viewerPane) {
        if ((viewerPane == null) || viewerPane.equals(getActiveView())) {
            return;
        }

        setActiveSubView(viewerPane);
    }

    public void setView(final String viewerPaneName) {
        if (viewerPaneName == null) {
            LOGGER.atWarn().log("viewerPaneName is null");
            return;
        }
        final Optional<DataView> match = getSubDataViews().stream().filter(c -> c.getName().equals(viewerPaneName)).findFirst();
        if (match.isEmpty()) {
            LOGGER.atWarn().addArgument(viewerPaneName).log("no DataView for viewerPaneName '{}'");
            return;
        }
        setView(match.get());
    }

    public void sort() {
        if (isStandalone() || getContentPane() == null || getContentPane().getChildren().isEmpty()) {
            return;
        }

        FXCollections.sort(getContentPane().getChildren(), Comparator.comparing(n -> n.toString().toLowerCase(Locale.UK)));
    }

    @Override
    public String toString() {
        return DataView.class.getSimpleName() + "(\"" + getName() + "\")";
    }

    protected void addStandardViews() {
        for (final Layout layout : Layout.values()) {
            final DataView dataView = new DataView(layout.getName(), null, new TilingPane(layout), false); // NOPMD
            subDataViews.add(dataView);
        }
        setNodeLayout(Layout.GRID); // NOPMD
    }

    protected Collection<Node> getWrappedChildren(final Collection<Node> children) {
        final Collection<Node> newNodes = new ArrayList<>();
        for (final Node node : children) {
            if (node instanceof DataViewWindow) {
                newNodes.add(node);
                continue;
            }
            final DataViewWindow window = new DataViewWindow("", node); // NOPMD
            window.setParentView(this);
            window.setMinimised(true);
            newNodes.add(window);
        }
        return newNodes;
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
                        if (getActiveView().isStandalone()) {
                            return;
                        }
                        if (!getActiveView().getContentPane().getChildren().contains(c)) {
                            getActiveView().getContentPane().getChildren().add(c);
                        }
                        visibleChildren.add((DataViewWindow) c);
                        return;
                    }
                    final DataViewWindow child = new DataViewWindow("", c); // NOPMD
                    child.setParentView(this);
                    visibleChildren.add(child);
                });
            }
        });

        visibleChildren.addListener((ListChangeListener<DataViewWindow>) change -> {
            while (change.next()) {
                if (getActiveView() != null) {
                    if (getActiveView().isStandalone()) {
                        return;
                    }
                    change.getRemoved().forEach(c -> getActiveView().getContentPane().getChildren().remove(c));
                    change.getAddedSubList().stream().filter(o -> !getActiveView().getContentPane().getChildren().contains(o)).forEach(c -> {
                        c.setParentView(this);
                        getActiveView().getContentPane().getChildren().add(c);
                    });
                }
            }
        });

        minimisedChildren.addListener((ListChangeListener<DataViewWindow>) change -> {
            while (change.next()) {
                minimisedElements.getChildren().removeAll(change.getRemoved());

                change.getAddedSubList().forEach(view -> {
                    view.setParentView(this);
                    if (!view.isMinimised() && view.getWindowState().equals(WindowState.WINDOW_RESTORED)) {
                        view.setMinimised(true);
                    }
                });

                minimisedElements.getChildren()
                        .addAll(change.getAddedSubList().stream().filter(view -> !minimisedElements.getChildren().contains(view)).collect(Collectors.toList()));
            }
        });

        undockedChildren.addListener((ListChangeListener<DataViewWindow>) change -> {
            while (change.next()) {
                minimisedElements.getChildren().removeAll(change.getRemoved());
                visibleChildren.removeAll(change.getRemoved());
                minimisedChildren.removeAll(change.getRemoved());

                change.getRemoved().forEach(view -> view.setDetached(false));

                change.getAddedSubList().forEach(view -> {
                    view.setParentView(this);
                    view.setDetached(true);
                });
            }
        });

        contentPane.addListener((ch, o, n) -> {
            if ((n == null) || n.equals(o)) {
                return;
            }

            if (isStandalone()) {
                getChildren().setAll(n);
            } else {
                getChildren().setAll(n, minimisedElements);
            }
        });
    }

    private static void removeChildFromList(final List<DataViewWindow> list, final Node node) {
        final Optional<DataViewWindow> found = list.stream().filter(content -> node.equals(content.getContent())).findFirst();
        if (found.isPresent()) {
            list.remove(found.get());
        }
    }
}
