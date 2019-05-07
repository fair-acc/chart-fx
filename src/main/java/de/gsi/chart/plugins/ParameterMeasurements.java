package de.gsi.chart.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.measurements.SimpleMeasurements;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType;
import de.gsi.chart.plugins.measurements.ValueIndicator;
import javafx.geometry.Orientation;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * plugin to implement simple measurements and valueTextField indicators such as
 * <p>
 * * valueTextField at horizontal/vertical marker position
 * -- coloured indication if min/max thresholds are exceeded
 * * rise-time
 * * trigger-to-rising edge detector
 * * absolute signal difference (ie. S0 - S1)
 * * relative signal change (ie. (S0 - S1)/S0)
 *
 * @author rstein
 */
public class ParameterMeasurements extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterMeasurements.class);
    protected final Pane measurmentDisplayPane = new Pane();
    private HBox parameterMenu;

    /**
     * Creates a new instance of ParameterMeasurements.
     */
    public ParameterMeasurements() {
        super();

        chartProperty().addListener((change, oldChart, newChart) -> {
            if (oldChart != null) {
                // remove tool bar items
                oldChart.getToolBar().getChildren().remove(parameterMenu);
                // oldChart.getChart().getPlotArea().setBottom(null); //TODO: replace meas display
                // remove measurement display pane

            }
            if (newChart != null) {
                // add tool bar items
            	parameterMenu = getMenuBar();
                newChart.getToolBar().getChildren().add(parameterMenu);

                // add measurement display pane

            }
        });
    }

    private HBox getMenuBar() {
        final HBox fileMenuBar = new HBox();
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);

        final MenuBar menuBar = new MenuBar();
        final Menu fileMenu = new Menu("Add Measurement:");

        // loop through category
        for (final MeasurementCategory category : MeasurementCategory.values()) {
            final Menu newCategory = new Menu(category.toString());
            fileMenu.getItems().addAll(newCategory);

            // loop through measurements within categories
            for (final MeasurementType measType : MeasurementType.values()) {
                if (measType.getCategory() != category) {
                    continue;
                }
                final MenuItem newMeasurement = new MenuItem(measType.toString());
                final XYChart xyChart = (XYChart)getChart();
                newMeasurement.setOnAction(evt -> new SimpleMeasurements(xyChart, measType).initialize());
                newCategory.getItems().addAll(newMeasurement);
            }

        }

        final Menu newCategory = new Menu("Misc");
        fileMenu.getItems().addAll(newCategory);

        final MenuItem newMeasurement1 = new MenuItem("Hor. Indicator");
        final XYChart xyChart = (XYChart)getChart();
        newMeasurement1.setOnAction(evt -> new ValueIndicator(xyChart, AxisMode.X).initialize());
        newCategory.getItems().addAll(newMeasurement1);

        final MenuItem newMeasurement2 = new MenuItem("Ver. Indicator");
        newMeasurement2.setOnAction(evt -> new ValueIndicator(xyChart, AxisMode.Y).initialize());
        newCategory.getItems().addAll(newMeasurement2);

        // add further miscellaneous items here

        menuBar.getMenus().addAll(fileMenu);

        fileMenuBar.getChildren().addAll(separator, menuBar);
        return fileMenuBar;
    }

}
