package de.gsi.chart.plugins;

import javafx.geometry.Orientation;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.measurements.SimpleMeasurements;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType;
import de.gsi.chart.plugins.measurements.ValueIndicator;
import de.gsi.chart.ui.TilingPane.Layout;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.viewer.DataView;

/**
 * plugin to implement simple measurements and valueTextField indicators such as
 * <p>
 * * valueTextField at horizontal/vertical marker position -- coloured indication if min/max thresholds are exceeded *
 * rise-time * trigger-to-rising edge detector * absolute signal difference (ie. S0 - S1) * relative signal change (ie.
 * (S0 - S1)/S0)
 *
 * @author rstein
 */
public class ParameterMeasurements extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterMeasurements.class);
    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 20;
    //    private static final char TOOLBUTTON_DRAFTING_COMPASS = '\uf568'; // drafting compass
    private static final char TOOLBUTTON_ALT = '\uf0ad'; // wrench
    private static final char TOOLBUTTON = TOOLBUTTON_ALT; // TODO: replace by drafting compass

    private final Glyph chartIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.LINE_CHART).size(FONT_SIZE);
    private final DataView dataView = new DataView("ChartViews", chartIcon);
    private HBox parameterMenu;

    /**
     * Creates a new instance of ParameterMeasurements.
     */
    public ParameterMeasurements() {
        this(Side.RIGHT);
    }

    /**
     * Creates a new instance of ParameterMeasurements.
     * @param side where to place the measurement results
     */
    public ParameterMeasurements(final Side side) {
        super();

        chartProperty().addListener((change, oldChart, newChart) -> {
            if (oldChart != null) {
                // remove tool bar items
                oldChart.getToolBar().getChildren().remove(parameterMenu);

                // remove measurement display pane
                oldChart.getMeasurementBar(side).getChildren().remove(dataView);
            }

            if (newChart != null) {
                // add tool bar items
                parameterMenu = getMenuBar();
                newChart.getToolBar().getChildren().add(parameterMenu);

                // add measurement display pane
                newChart.getMeasurementBar(side).getChildren().add(dataView);
            }
        });

        dataView.setNodeLayout(Layout.VBOX);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log(ParameterMeasurements.class.getSimpleName() + " - initialised");
        }
    }

    public DataView getDataView() {
        return dataView;
    }

    private HBox getMenuBar() {
        final HBox fileMenuBar = new HBox();
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);

        final MenuBar menuBar = new MenuBar();
        final Menu fileMenu = new Menu(null, new Glyph(FONT_AWESOME, TOOLBUTTON).size(FONT_SIZE));
        final String tooltipKey = "TOOL_TIP";
        fileMenu.getProperties().put(tooltipKey, new Tooltip("add measurement indicator"));

        // loop through category
        for (final MeasurementCategory category : MeasurementCategory.values()) {
            final Menu newCategory = new Menu(category.toString()); // NOPMD dynamic (but finite) menu generation
            fileMenu.getItems().addAll(newCategory);

            // loop through measurements within categories
            for (final MeasurementType measType : MeasurementType.values()) {
                if (measType.getCategory() != category) {
                    continue;
                }
                final MenuItem newMeasurement = new MenuItem(measType.toString()); // NOPMD dynamic (but finite) menu generation
                newMeasurement.setOnAction(evt -> new SimpleMeasurements(this, measType).initialize()); // NOPMD
                newCategory.getItems().addAll(newMeasurement);
            }
        }

        final Menu newCategory = new Menu("Misc");
        fileMenu.getItems().addAll(newCategory);

        //        final MenuItem newMeasurement1 = new MenuItem("Hor. Indicator");
        //        newMeasurement1.setOnAction(evt -> new ValueIndicator(this, AxisMode.X).initialize());
        //        newCategory.getItems().addAll(newMeasurement1);
        //
        //        final MenuItem newMeasurement2 = new MenuItem("Ver. Indicator");
        //        newMeasurement2.setOnAction(evt -> new ValueIndicator(this, AxisMode.Y).initialize());
        //        newCategory.getItems().addAll(newMeasurement2);

        // add further miscellaneous items here

        menuBar.getMenus().addAll(fileMenu);

        fileMenuBar.getChildren().addAll(separator, menuBar);
        return fileMenuBar;
    }
}
