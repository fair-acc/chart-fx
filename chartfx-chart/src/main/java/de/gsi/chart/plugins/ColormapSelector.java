package de.gsi.chart.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.renderer.spi.utils.ColorGradient;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.shape.Rectangle;

/**
 * Adds a Dropdown to the toolbar to select different Colormaps. The selected Colormap can be accessed from via
 * colormapProperty() and bound to Renderers/Axes.
 * 
 * @author Alexander Krimm
 */
public class ColormapSelector extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColormapSelector.class);

    private BooleanProperty showInToolbar = new SimpleBooleanProperty(this, "show in toolbar", true);
    private ComboBox<ColorGradient> dropdown = new ColormapComboBox();

    public ColormapSelector() {
        chartProperty().addListener((change, o, n) -> {
            if (o != null) {
                o.getToolBar().getChildren().remove(dropdown);
                dropdown.setOnShown(null);
                dropdown.setOnHidden(null);
            }
            if (n != null) {
                if (isShowInToolbar()) {
                    n.getToolBar().getChildren().add(dropdown);
                    // Prevent the toolbar HiddenSidePane from vanishing when using the menu
                    dropdown.setOnShown((evt) -> getChart().setPinnedSide(javafx.geometry.Side.TOP));
                    dropdown.setOnHidden((evt) -> {
                        if (!getChart().isToolBarPinned())
                            getChart().setPinnedSide(null);
                    });
                }
            }
        });
        showInToolbar.addListener((prop, o, n) -> {
            if (n) {
                getChart().getToolBar().getChildren().add(dropdown);
                // Prevent the toolbar HiddenSidePane from vanishing when using the menu
                dropdown.setOnShown((evt) -> getChart().setPinnedSide(javafx.geometry.Side.TOP));
                dropdown.setOnHidden((evt) -> {
                    if (!getChart().isToolBarPinned())
                        getChart().setPinnedSide(null);
                });
            } else {
                getChart().getToolBar().getChildren().remove(dropdown);
                dropdown.setOnShown(null);
                dropdown.setOnHidden(null);
            }

        });
    }

    public ObjectProperty<ColorGradient> colormapProperty() {
        return dropdown.valueProperty();
    }

    public ColorGradient getColormap() {
        return dropdown.getValue();
    }

    public ObservableList<ColorGradient> getGradientsList() {
        return dropdown.getItems();
    }

    public boolean isShowInToolbar() {
        return showInToolbar.get();
    }

    public void setColormap(final ColorGradient newGradient) {
        if (!getGradientsList().contains(newGradient)) {
            getGradientsList().add(newGradient);
        }
        dropdown.setValue(newGradient);
    }

    public void setShowInToolbar(final boolean show) {
        showInToolbar.set(show);
    }

    public BooleanProperty showInToolbarProperty() {
        return showInToolbar;
    }

    public static class ColormapComboBox extends ComboBox<ColorGradient> {
        public ColormapComboBox() {
            setCellFactory((listView) -> new ColormapListCell());
            setButtonCell(new ColormapListCell());
            getItems().addAll(ColorGradient.colorGradients());
            setValue(ColorGradient.DEFAULT);
        }
    }

    public static class ColormapListCell extends ListCell<ColorGradient> {
        private static final double COLORMAP_WIDTH = 30;
        private static final double COLORMAP_HEIGHT = 10;

        private final Rectangle rect = new Rectangle(COLORMAP_WIDTH, COLORMAP_HEIGHT);

        public ColormapListCell() {
            super();
            setContentDisplay(ContentDisplay.LEFT);
        }

        @Override
        protected void updateItem(ColorGradient gradient, boolean empty) {
            super.updateItem(gradient, empty);
            if (gradient == null || empty) {
                setGraphic(null);
                setText("-");
            } else {
                rect.setFill(
                        new LinearGradient(0, 0, COLORMAP_WIDTH, 0, false, CycleMethod.NO_CYCLE, gradient.getStops()));
                setGraphic(rect);
                setText(gradient.toString());
            }
        }
    }
}
