package io.fair_acc.chartfx.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.ui.ProfilerInfoBox.DebugLevel;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.SimplePerformanceMeter;

/**
 * Tests {@link ProfilerInfoBox }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class ProfilerInfoBoxTests {
    private final Pane pane = new Pane();
    private Scene scene;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow((ThrowingSupplier<ProfilerInfoBox>) ProfilerInfoBox::new);
        scene = new Scene(pane, 600, 100);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testButtons(FxRobot robot) throws Exception {
        final ProfilerInfoBox infoBox = new ProfilerInfoBox(); // force fastest update
        // test updater
        FXUtils.runAndWait(() -> pane.getChildren().add(infoBox));
        pane.requestLayout();

        // click through hierarchy
        infoBox.setSelectedCrumb(infoBox.getTreeRoot());
        assertEquals("ProfilerInfoBox-treeRoot", infoBox.getSelectedCrumb().getValue().getId());

        robot.interrupt();
        robot.clickOn("#ProfilerInfoBox-treeRoot");
        assertEquals("ProfilerInfoBox-fpsItem", infoBox.getSelectedCrumb().getValue().getId());

        robot.interrupt();
        robot.clickOn("#ProfilerInfoBox-fpsItem");
        assertEquals("ProfilerInfoBox-cpuItem", infoBox.getSelectedCrumb().getValue().getId());

        robot.interrupt();
        robot.clickOn("#ProfilerInfoBox-cpuItem");
        assertEquals("ProfilerInfoBox-versionItem", infoBox.getSelectedCrumb().getValue().getId());

        robot.interrupt();
        robot.clickOn("#ProfilerInfoBox-versionItem");
        assertEquals("ProfilerInfoBox-versionItem", infoBox.getSelectedCrumb().getValue().getId());
    }
    @TestFx
    public void testSetterGetter() {
        assertDoesNotThrow((ThrowingSupplier<ProfilerInfoBox>) ProfilerInfoBox::new);
        assertDoesNotThrow(() -> new ProfilerInfoBox(1000));
        assertDoesNotThrow(() -> new ProfilerInfoBox(new Scene(new Pane(), 100, 100)));

        //        final Scene scene = new Scene(new Pane(), 100,100);
        final ProfilerInfoBox infoBox = new ProfilerInfoBox(); // force fastest update

        for (final DebugLevel debugLevel : DebugLevel.values()) {
            infoBox.setDebugLevel(debugLevel);
            assertEquals(debugLevel, infoBox.getDebugLevel());
        }
        // set and check initial state
        infoBox.setDebugLevel(DebugLevel.NONE);
        assertEquals(DebugLevel.NONE, infoBox.getDebugLevel());

        // set and check bread crumb tree/hierarchy
        TreeItem<VBox> crumb = infoBox.getTreeRoot();
        assertNotNull(crumb);
        infoBox.setSelectedCrumb(crumb);
        assertEquals(crumb, infoBox.getSelectedCrumb());
        while (!crumb.getChildren().isEmpty()) {
            crumb = crumb.getChildren().get(0);
            infoBox.setSelectedCrumb(crumb);
            assertEquals(crumb, infoBox.getSelectedCrumb());
        }

        // check scene listener
        assertNull(infoBox.getScene());
        pane.getChildren().add(infoBox);
        assertEquals(scene, infoBox.getScene());
        pane.getChildren().remove(infoBox);
        assertNull(infoBox.getScene());
    }

    @TestFx
    public void testSimplePerformanceTrackerBitsAndBobs() {
        assertThrows(IllegalArgumentException.class, () -> new SimplePerformanceMeter(null, 20));
        final SimplePerformanceMeter meter = new SimplePerformanceMeter(scene, 20);

        assertDoesNotThrow(SimplePerformanceMeter::getProcessCpuLoadInternal);
        assertDoesNotThrow(meter::isSceneDirty);
        assertDoesNotThrow(meter::getActualFrameRate);
        assertDoesNotThrow(meter::getAverageFrameRate);
        assertDoesNotThrow(meter::getAverageFxFrameRate);
        assertDoesNotThrow(meter::getAverageProcessCpuLoad);
        assertDoesNotThrow(meter::getAverageSystemCpuLoad);
        assertDoesNotThrow(meter::getFxFrameRate);
        assertDoesNotThrow(meter::getProcessCpuLoad);
        assertDoesNotThrow(meter::getSystemCpuLoad);
        assertDoesNotThrow(meter::resetAverages);

        assertNotNull(meter.actualFrameRateProperty());
        assertNotNull(meter.averageFactorProperty());
        assertNotNull(meter.averageFrameRateProperty());
        assertNotNull(meter.averageFxFrameRateProperty());
        assertNotNull(meter.averageProcessCpuLoadProperty());
        assertNotNull(meter.averageSystemCpuLoadProperty());

        assertDoesNotThrow(meter::deregisterListener);
        assertDoesNotThrow(meter::deregisterListener);
    }
}
