package de.gsi.chart.plugins;

import org.controlsfx.glyphfont.Glyph;

import de.gsi.chart.plugins.measurements.AbstractChartMeasurement;
import de.gsi.chart.plugins.measurements.DataSetMeasurements;
import de.gsi.chart.plugins.measurements.SimpleMeasurements;
import de.gsi.chart.ui.TilingPane.Layout;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.viewer.DataView;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;

/**
 * Plugin to implement simple measurements and valueTextField indicators such as
 * <p>
 * <ul>
 * <li> value at horizontal/vertical marker position with coloured indication if min/max thresholds are exceeded,
 * <li> min/max/rms/median/... values of functions
 * <li> rise-time estiamtes
 * <li> trigger-to-rising edge detector
 * <li> absolute signal difference (ie. S0- S1)
 * <li> relative signal change (ie. (S0 - S1)/S0)
 * <li> and many more.
 * </ul>
 * For a more complete list see {@link de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType} or {@link de.gsi.chart.plugins.measurements.DataSetMeasurements.MeasurementType}
 *
 * @author rstein
 */
public class ParameterMeasurements extends ChartPlugin {
    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 20;
    // private static final char TOOLBUTTON_DRAFTING_COMPASS = '\uf568'; // drafting compass
    private static final char TOOLBUTTON_ALT = '\uf0ad'; // wrench
    private static final char TOOLBUTTON = TOOLBUTTON_ALT; // TODO: replace by drafting compass
    protected final ObservableList<AbstractChartMeasurement> chartMeasurements = FXCollections.observableArrayList();
    private final Glyph chartIcon = new Glyph(FONT_AWESOME, TOOLBUTTON).size(FONT_SIZE);
    private final DataView dataView = new DataView("ChartViews", chartIcon);
    private MenuBar parameterMenu;

    private final ChangeListener<Boolean> parentVisibleListener = (obs, o, n) -> {
        if (Boolean.FALSE.equals(n)) {
            parameterMenu.getMenus().get(0).hide();
        }
    };

    /**
     * Creates a new instance of ParameterMeasurements.
     */
    public ParameterMeasurements() {
        this(Side.RIGHT, true);
    }

    /**
     * Creates a new instance of ParameterMeasurements.
     *
     * @param side where to place the measurement results
     * @param addToToolBar true: add to internal ToolBar; false: optionally add ToolBar to another other Pane
     */
    public ParameterMeasurements(final Side side, final boolean addToToolBar) {
        super();
        parameterMenu = getMenuBar(); // NOPMD
        parameterMenu.setId("ParameterMeasurements::parameterMenu"); // N.B. not a unique name but for testing this suffices
        parameterMenu.parentProperty().addListener((obs, o, n) -> {
            if (o != null) {
                o.visibleProperty().addListener(parentVisibleListener);
            }
            if (n != null) {
                n.visibleProperty().addListener(parentVisibleListener);
            }
        });

        chartProperty().addListener((change, oldChart, newChart) -> {
            if (oldChart != null) {
                // remove tool bar items
                oldChart.getToolBar().getChildren().remove(parameterMenu);

                // remove measurement display pane
                oldChart.getMeasurementBar(side).getChildren().remove(dataView);
            }

            if (newChart != null) {
                if (addToToolBar) {
                    // add tool bar items
                    newChart.getToolBar().getChildren().add(parameterMenu);
                }

                // add measurement display pane
                newChart.getMeasurementBar(side).getChildren().add(dataView);
            }
        });

        dataView.setNodeLayout(Layout.VBOX);
    }

    public ObservableList<AbstractChartMeasurement> getChartMeasurements() {
        return chartMeasurements;
    }

    public DataView getDataView() {
        return dataView;
    }

    public MenuBar getMenuBar() {
        if (parameterMenu != null) {
            return parameterMenu;
        }
        parameterMenu = new MenuBar();

        final Menu measurementMenu = new Menu(null, new Glyph(FONT_AWESOME, TOOLBUTTON).size(FONT_SIZE));
        measurementMenu.getProperties().put("TOOL_TIP", new Tooltip("add measurement indicator"));
        measurementMenu.setId("ParameterMeasurements::measurementMenu"); // N.B. not a unique name but for testing this suffices

        // loop through SimpleMeasurements categories
        for (final SimpleMeasurements.MeasurementCategory category : SimpleMeasurements.MeasurementCategory.values()) {
            final Menu newCategory = new Menu(category.getName()); // NOPMD dynamic (but finite) menu generation
            measurementMenu.getItems().addAll(newCategory);

            // loop through measurements within categories
            for (final SimpleMeasurements.MeasurementType measType : SimpleMeasurements.MeasurementType.values()) {
                if (measType.getCategory() != category) {
                    continue;
                }
                final MenuItem newMeasurement = new MenuItem(measType.getName()); // NOPMD dynamic (but finite) menu generation
                newMeasurement.setId("ParameterMeasurements::newMeasurement::" + measType.toString()); // N.B. not a unique name but for testing this suffices
                newMeasurement.setOnAction(evt -> new SimpleMeasurements(this, measType).initialize()); // NOPMD
                newCategory.getItems().addAll(newMeasurement);
            }
        }

        // loop through DataSetMeasurements categories
        for (final DataSetMeasurements.MeasurementCategory category : DataSetMeasurements.MeasurementCategory.values()) {
            final Menu newCategory = new Menu(category.getName()); // NOPMD dynamic (but finite) menu generation
            measurementMenu.getItems().addAll(newCategory);

            // loop through measurements within categories
            for (final DataSetMeasurements.MeasurementType measType : DataSetMeasurements.MeasurementType.values()) {
                if (measType.getCategory() != category) {
                    continue;
                }
                final MenuItem newMeasurement = new MenuItem(measType.getName()); // NOPMD dynamic (but finite) menu generation
                newMeasurement.setId("ParameterMeasurements::newMeasurement::" + measType.toString()); // N.B. not a unique name but for testing this suffices
                newMeasurement.setOnAction(evt -> new DataSetMeasurements(this, measType).initialize()); // NOPMD
                newCategory.getItems().addAll(newMeasurement);
            }
        }

        // add further miscellaneous items here if needed

        parameterMenu.getMenus().addAll(measurementMenu);

        return parameterMenu;
    }
}
