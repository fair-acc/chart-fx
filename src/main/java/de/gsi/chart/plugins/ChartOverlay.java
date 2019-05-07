package de.gsi.chart.plugins;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Plugin that overlays {@link OverlayArea#CHART_PANE chart pane} or chart's {@link OverlayArea#PLOT_AREA plot area}
 * with given node i.e. the bounds of the node are set to cover the indicated area.
 * <p>
 * Typically the node would be a an instance of {@link Pane} containing child nodes. The example below shows how to add
 * a Label to the chart pane, located in the left top corner of the plot area:
 *
 * <pre>
 * XYChartPane<Number, Number> chartPane = ...;
 *
 * Label label = new Label("Info about chart data");
 * AnchorPane.setTopAnchor(label, 5.0);
 * AnchorPane.setLeftAnchor(label, 5.0);
 * AnchorPane anchorPane = new AnchorPane(label);
 * // Pass any mouse events to the underlying chart
 * anchorPane.setMouseTransparent(true);
 *
 * chartPane.getPlugins().add(new ChartOverlay<>(OverlayArea.PLOT_AREA, anchorPane));
 * </pre>
 * </p>
 *
 * @author Grzegorz Kruk
 */
public class ChartOverlay extends ChartPlugin {

    /**
     * Defines possible areas to be overlaid.
     */
    public enum OverlayArea {
        CHART_PANE,
        PLOT_AREA
    }

    /**
     * Creates a new, empty {@code ChartOverlay}, initialized to overlay the specified area.
     *
     * @param area value of {@link #overlayAreaProperty()}
     */
    public ChartOverlay(final OverlayArea area) {
        this(area, null);
    }

    /**
     * Creates a new {@code ChartOverlay}, initialized to overlay the specified area with given node.
     *
     * @param area value of {@link #overlayAreaProperty()}
     * @param node value of the {@link #nodeProperty()}
     */
    public ChartOverlay(final OverlayArea area, final Node node) {
        setOverlayArea(area);
        nodeProperty().addListener((obs, oldNode, newNode) -> {
            getChartChildren().remove(oldNode);
            if (newNode != null) {
                getChartChildren().add(newNode);
            }
            layoutChildren();
        });
        setNode(node);
    }

    private final ObjectProperty<Node> node = new SimpleObjectProperty<>(this, "node");

    /**
     * The node to be overlaid on top of the {@link #overlayAreaProperty() overlay area}.
     *
     * @return the node property
     */
    public final ObjectProperty<Node> nodeProperty() {
        return node;
    }

    /**
     * Returns the value of the {@link #nodeProperty()}.
     *
     * @return the node to be overlaid
     */
    public final Node getNode() {
        return nodeProperty().get();
    }

    /**
     * Sets the value of the {@link #nodeProperty()}.
     *
     * @param newNode the node to overlaid
     */
    public final void setNode(final Node newNode) {
        nodeProperty().set(newNode);
    }

    private final ObjectProperty<OverlayArea> overlayArea = new SimpleObjectProperty<OverlayArea>(this, "overlayArea") {

        @Override
        protected void invalidated() {
            layoutChildren();
        }
    };

    /**
     * Specifies the {@link OverlayArea} to be covered by the {@link #nodeProperty() node}.
     *
     * @return overlayArea property
     */
    public final ObjectProperty<OverlayArea> overlayAreaProperty() {
        return overlayArea;
    }

    /**
     * Returns the value of the {@link #overlayAreaProperty()}.
     *
     * @return the overlay area to be covered by the node
     */
    public final OverlayArea getOverlayArea() {
        return overlayAreaProperty().get();
    }

    /**
     * Sets the value of the {@link #overlayAreaProperty()}.
     *
     * @param area the area to be covered
     */
    public final void setOverlayArea(final OverlayArea area) {
        overlayAreaProperty().set(area);
    }

    @Override
    public void layoutChildren() {
        if (getChart() == null || getNode() == null) {
            return;
        }

        if (getOverlayArea() == OverlayArea.CHART_PANE) {
            getNode().resizeRelocate(0, 0, getChart().getWidth(), getChart().getHeight());
        } else {
            final Bounds plotArea = getChart().getBoundsInLocal();
            getNode().resizeRelocate(plotArea.getMinX(), plotArea.getMinY(), plotArea.getWidth(), plotArea.getHeight());
        }
    }
}
