package io.fair_acc.chartfx.plugins;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.sandec.jmemorybuddy.JMemoryBuddy;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * This test was written to detect a suspected memory leak in the {@link EditAxis} plugin. But it turned out to be an
 * out of memory error because of too many nested calls, similar to a stack overflow exception.
 * <p>
 * Cause of the problem was that for each axis change in the axis list, the list change handler was added once more.
 * Because of this root cause the loop count of {@link EditAxisMemoryLeakTest#addAndRemoveAxesDynamicallyTest(FxRobot)}
 * is relevant to detect the error automatically. At this point the tests are configured to 4g max heap and the test
 * will cause an OOM easily on my system with 8 iterations. Please increase the loop count if you would like to verify
 * the bugfix and do not receive an OOM.
 * <p>
 * The test will also work as regular mem leak test. In this case only one loop would suffice.
 *
 * @author Benjamin Peter
 */
@ExtendWith(ApplicationExtension.class)
public class EditAxisMemoryLeakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditAxisMemoryLeakTest.class);

    private static final int AXIS_ADD_REMOVE_LOOP_COUNT_WHICH_SHOULD_OOM = 12;

    @Start
    public void start(final Stage stage) {
        final Scene scene = new Scene(new Pane(), 100, 100);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void addAndRemoveAxesDynamicallyTest(final FxRobot fxRobot) {
        JMemoryBuddy.memoryTest(checker -> {
            final AtomicReference<XYChart> chartRef = new AtomicReference<>();

            fxRobot.interact(() -> {
                final XYChart chart = new XYChart();
                chartRef.setRelease(chart);
                checker.setAsReferenced(chart);
            });

            fxRobot.interact(() -> {
                final var plugin = new EditAxis();
                chartRef.getAcquire().getPlugins().add(plugin);
            });

            LOGGER.debug("Initialized test");

            for (int i = 0; i < AXIS_ADD_REMOVE_LOOP_COUNT_WHICH_SHOULD_OOM; i++) {
                final String axisName = "y" + i;

                final AtomicReference<DefaultNumericAxis> dynamicAxisRef = new AtomicReference<>();
                fxRobot.interact(() -> {
                    dynamicAxisRef.setRelease(new DefaultNumericAxis(axisName, -100, +100, 10));
                    LOGGER.debug("Adding axis {}", axisName);
                    chartRef.getAcquire().getAxes().add(dynamicAxisRef.getAcquire());
                });

                fxRobot.interact(() -> {
                    LOGGER.debug("Removing axis {}", axisName);
                    final DefaultNumericAxis axis = dynamicAxisRef.getAcquire();
                    chartRef.getAcquire().getAxes().remove(axis);
                    checker.assertCollectable(axis);
                });
            }

            LOGGER.debug("Test done");
        });
    }

}
