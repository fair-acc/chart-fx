package de.gsi.chart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.api.FxToolkitContext;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.service.support.FiredEvents;

import de.gsi.chart.plugins.*;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.testdata.spi.CosineFunction;
import de.gsi.dataset.testdata.spi.GaussFunction;

import de.sandec.jmemorybuddy.JMemoryBuddy;

@ExtendWith(ApplicationExtension.class)
class MemoryLeakTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLeakTests.class);
    private static final double HEIGHT = 200;
    private static final double WIDTH = 300;
    private BorderPane rootNode;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow((ThrowingSupplier<ErrorDataSetRenderer>) ErrorDataSetRenderer::new);
        rootNode = new BorderPane();
        stage.setScene(new Scene(rootNode, WIDTH, HEIGHT));
        stage.show();
    }

    @Test
    void simpleLeakTest(final FxRobot fxRobot){
        JMemoryBuddy.memoryTest(checker -> {
            AtomicReference<XYChart> chartRef = new AtomicReference<>();
            // add chart to scene
            fxRobot.interact(() -> {
                final XYChart chart = new XYChart();
                chart.getPlugins().addAll(
                        new Zoomer(),
                        new EditAxis(),
                        new ParameterMeasurements(),
                        new DataPointTooltip(),
                        new EditDataSet(),
                        new Screenshot(),
                        new CrosshairIndicator());
                chart.getDatasets().addAll(
                        new CosineFunction("cosine", 50),
                        new GaussFunction("gauss", 50));
                rootNode.setCenter(chart);
                chartRef.set(chart);
            });
            // track chart components in memory checker
            fxRobot.interact(() -> {
                final XYChart chart = chartRef.get();
                checker.assertCollectable(chart);
                checker.assertCollectable(chart.getLegend());
                chart.getAxes().forEach(checker::assertCollectable);
                chart.getRenderers().forEach(checker::assertCollectable);
                chart.getPlugins().forEach(checker::assertCollectable);
                chart.getDatasets().forEach(checker::assertCollectable);
            });
            // remove the chart from the scene, clear the reference and remove testFx event log
            fxRobot.interact(() -> rootNode.setCenter(null));
            chartRef.set(null);
            clearTestFxRecordedEvents();
        });
    }

    /**
     * Remove event recording done by testfx.
     * Useful for garbage collector tests where event logging can prevent objects which are in event's target fields
     * from being garbage collected.
     */
    private static void clearTestFxRecordedEvents() {
        final FxToolkitContext tkContext = FxToolkit.toolkitContext(); // <- this list is unmodifiable
        try {
            final Field field = tkContext.getClass().getDeclaredField("firedEvents");
            field.setAccessible(true);
            ((FiredEvents) field.get(tkContext)).clearEvents();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LOGGER.atError().setCause(e).log("Failed to ues reflection to clear recorded events");
        }
    }
}
