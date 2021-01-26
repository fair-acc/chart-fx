package de.gsi.chart.plugins;

import static de.gsi.chart.plugins.TableViewer.BUTTON_BAR_STYLE_CLASS;
import static de.gsi.chart.plugins.TableViewer.BUTTON_SWITCH_TABLE_VIEW_STYLE_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.NodeQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.TableViewer.DataSetsRow;
import de.gsi.dataset.testdata.spi.CosineFunction;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * Test the table viewer plugin
 * 
 * @author Benjamin Peter
 */
@ExtendWith(ApplicationExtension.class)
class TableViewerTest {
    private final FxRobot fxRobot = new FxRobot();
    private XYChart chart;
    private TableViewer tableViewer;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();
        chart.setId("myChart");
        Scene scene = new Scene(chart, 400, 300);
        tableViewer = new TableViewer();
        chart.setPrefWidth(400);
        chart.setPrefHeight(300);
        chart.getDatasets().add(new CosineFunction("Cosine", 50));
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testThatTableIsOnlyAddedToSceneWhenActive() throws TimeoutException { // NOPMD JUnitTestsShouldIncludeAssert
        fxRobot.interact(() -> {
            chart.getPlugins().add(tableViewer);
            chart.setToolBarPinned(true);
        });

        // Expect the table view not to be absent initially
        FxAssert.verifyThat(chart.getPlotForeground(), Matchers.not(NodeMatchers.hasChild(".table-view")));

        final Button switchTableViewButton = locateTableViewButton(chart.getToolBar());
        // Wait for the slowly opening toolbar to show
        waitForNodeToBeVisible(switchTableViewButton);
        fxRobot.clickOn(switchTableViewButton);
        WaitForAsyncUtils.waitForFxEvents();

        // Expect the table view to be present after clicking the button
        verifyThatWithTimeout(chart.getPlotForeground(), NodeMatchers.hasChild(".table-view"));

        // hide/close table view again
        fxRobot.clickOn(switchTableViewButton);
        WaitForAsyncUtils.waitForFxEvents();

        // Expect the table view to be removed after clicking the button
        fxRobot.sleep(200); // it might need some time to be gone
        FxAssert.verifyThat(chart.getPlotForeground(), Matchers.not(NodeMatchers.hasChild(".table-view")));
    }
    
    @Test
    public void testThatDataSetsRowHashCodeEqualsWorks() throws TimeoutException {
        fxRobot.interact(() -> {
            chart.getPlugins().add(tableViewer);
            chart.setToolBarPinned(true);
        });

        // Open the table view
        final Button switchTableViewButton = locateTableViewButton(chart.getToolBar());
        waitForNodeToBeVisible(switchTableViewButton); // Wait for the slowly opening toolbar to show
        fxRobot.clickOn(switchTableViewButton);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThatWithTimeout(chart.getPlotForeground(), NodeMatchers.hasChild(".table-view"));
        
        @SuppressWarnings("unchecked")
        TableView<DataSetsRow> tableView = (TableView<DataSetsRow>) tableViewer.getTable();
        tableView.getSelectionModel().select(0);
        DataSetsRow anyRowItem = tableView.getSelectionModel().getSelectedItem();
        assertEquals(anyRowItem.hashCode(), anyRowItem.hashCode());
        assertTrue(anyRowItem.equals(anyRowItem));
    }

    private Button locateTableViewButton(final FlowPane toolbar) {
        return fxRobot.from(toolbar) //
                .lookup("." + BUTTON_BAR_STYLE_CLASS + " > ." + BUTTON_SWITCH_TABLE_VIEW_STYLE_CLASS) //
                .queryButton();
    }

    private static void verifyThatWithTimeout(Node node, Matcher<Node> matcher) throws TimeoutException {
        WaitForAsyncUtils.waitFor(1000, TimeUnit.MILLISECONDS, () -> matcher.matches(node));
        FxAssert.verifyThat(node, matcher);
    }

    private static void waitForNodeToBeVisible(final Node switchTableViewButton) throws TimeoutException {
        final Callable<Boolean> cala = () -> NodeQueryUtils.isVisible().test(switchTableViewButton);
        WaitForAsyncUtils.waitFor(1000, TimeUnit.MILLISECONDS, cala);
    }
}
