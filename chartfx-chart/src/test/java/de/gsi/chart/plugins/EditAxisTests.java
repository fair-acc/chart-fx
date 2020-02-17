package de.gsi.chart.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.util.Duration;

import org.junit.jupiter.api.Test;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis.AxisEditor;

/**
 * Basic interface tests for EditAxis plugin
 *
 * @author rstein
 */
public class EditAxisTests {
    private boolean jfxIsSetup;

    @Test
    public void basicEditAxisInterfaceTests() {

        assertDoesNotThrow(() -> new EditAxis());

        assertDoesNotThrow(() -> {
            EditAxis plugin = new EditAxis(false);
            assertEquals(false, plugin.isAnimated());

            plugin.setAnimated(true);
            assertEquals(true, plugin.isAnimated());

            plugin.setZoomDuration(Duration.millis(100));
            assertEquals(Duration.millis(100), plugin.getZoomDuration());

            plugin.setZoomDuration(Duration.millis(120));
            assertEquals(Duration.millis(120), plugin.getZoomDuration());

            assertEquals(0, plugin.popUpList.size());
            plugin.removeMouseEventHandlers(null);
            assertEquals(0, plugin.popUpList.size());
        });

        assertDoesNotThrow(() -> {
            for (AxisMode mode : AxisMode.values()) {
                EditAxis plugin = new EditAxis(mode);
                assertEquals(mode, plugin.getAxisMode());
            }
        });

    }

    @Test
    public void attachChartTests() {
        assertDoesNotThrow(() -> doOnJavaFXThread(() -> {
            DefaultNumericAxis xAxis = new DefaultNumericAxis("x");
            DefaultNumericAxis yAxis = new DefaultNumericAxis("y");
            XYChart chart = new XYChart(xAxis, yAxis);
            assertEquals(2, chart.getAxes().size());

            EditAxis plugin = new EditAxis();
            assertEquals(0, plugin.popUpList.size());
            chart.getPlugins().add(plugin);
            assertEquals(2, plugin.popUpList.size());

            chart.getPlugins().remove(plugin);
            assertEquals(0, plugin.popUpList.size());

            plugin.addMouseEventHandlers(chart);
            assertEquals(2, plugin.popUpList.size());

            chart.getAxes().remove(yAxis);
            //TODO: check -- removes all axis references but does not leave the remaining axes
            assertEquals(0, plugin.popUpList.size());

            chart.getPlugins().add(plugin);
            assertEquals(1, plugin.popUpList.size());

            // duplicates not allowed
            // assertThrows(IllegalArgumentException.class, () -> chart.getPlugins().add(plugin));

        }));
    }

    @Test
    public void changeAxisRangeTests() {
        assertDoesNotThrow(() -> doOnJavaFXThread(() -> {
            DefaultNumericAxis xAxis = new DefaultNumericAxis("x", -100, +100, 10);
            xAxis.setTickUnit(10);
            DefaultNumericAxis yAxis = new DefaultNumericAxis("y", -100, +100, 10);
            XYChart chart = new XYChart(xAxis, yAxis);
            assertEquals(2, chart.getAxes().size());

            EditAxis plugin = new EditAxis();
            assertEquals(0, plugin.popUpList.size());
            chart.getPlugins().add(plugin);
            assertEquals(2, plugin.popUpList.size());

            AxisEditor xEditor = plugin.new AxisEditor(xAxis, true);

            assertEquals(+100, xAxis.getMax());
            assertEquals(-100, xAxis.getMin());
            assertDoesNotThrow(() -> xEditor.changeAxisRangeLimit(xAxis, true, true));
            // assertEquals(+110, xAxis.getMax());
            // assertEquals(-110, xAxis.getMin());

        }));
    }

    private void doOnJavaFXThread(Runnable pRun) throws RuntimeException {
        if (!jfxIsSetup) {
            setupJavaFX();
            jfxIsSetup = true;
        }
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            pRun.run();
            countDownLatch.countDown();
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setupJavaFX() throws RuntimeException {
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            new JFXPanel(); // NOPMD initializes JavaFX environment
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
