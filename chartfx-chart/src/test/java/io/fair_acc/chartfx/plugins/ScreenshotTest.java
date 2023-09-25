package io.fair_acc.chartfx.plugins;

import static java.util.Objects.requireNonNull;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.testdata.spi.CosineFunction;

/**
 * Test the screenshot plugin
 *
 * @author Alexander Krimm
 */
@ExtendWith(ApplicationExtension.class)
class ScreenshotTest {
    private final FxRobot fxRobot = new FxRobot();
    private XYChart chart;
    private Screenshot screenshotPlugin;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();
        chart.setId("myChart");
        Scene scene = new Scene(chart, 400, 300);
        screenshotPlugin = new Screenshot();
        chart.setPrefWidth(350);
        chart.setPrefHeight(250);
        chart.getDatasets().add(new CosineFunction("Cosine", 50));
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void screenshotTests(@TempDir Path tmpdir) throws IOException {
        // setup some useful variables
        final FlowPane toolbar = chart.getToolBar();

        // add Screenshot Plugin
        fxRobot.interact(() -> {
            chart.getPlugins().add(screenshotPlugin);
        });

        // set and test Properties
        fxRobot.interact(() -> {
            // pattern
            assertEquals("", screenshotPlugin.getPattern());
            screenshotPlugin.setPattern("{dataSetName}_{xMin}.png");
            assertEquals("{dataSetName}_{xMin}.png", screenshotPlugin.getPattern());
            // directory
            screenshotPlugin.setDirectory(tmpdir.toAbsolutePath().toString());
            assertEquals(tmpdir.toAbsolutePath().toString(), screenshotPlugin.getDirectory());
        });

        // verify that controls were added to toolbar
        FxAssert.verifyThat(toolbar, node -> node.getChildren().size() == 1);

        // check dimensions of screenshot match chart dimensions
        fxRobot.interact(() -> {
            final Image screenshot = screenshotPlugin.getScreenshot();
            assertEquals(chart.getWidth(), screenshot.getWidth());
            assertEquals(chart.getHeight(), screenshot.getHeight());
        });

        // check copying of screenshot to clipboard
        fxRobot.interact(() -> {
            screenshotPlugin.screenshotToClipboard();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final Image clipboardContents = (Image) clipboard.getContent(DataFormat.IMAGE);
            assertNotNull(clipboardContents);
            assertEquals(chart.getWidth(), clipboardContents.getWidth());
            assertEquals(chart.getHeight(), clipboardContents.getHeight());
        });

        // check saving screenshot to file
        fxRobot.interact(() -> screenshotPlugin.screenshotToFile(false));

        File[] filelist = tmpdir.toAbsolutePath().toFile().listFiles();
        assertNotNull(filelist);
        requireNonNull(filelist); // coverity needs to be assured that the variable cannot be null
        assertEquals(1, filelist.length);
        assertEquals(new File(tmpdir.toAbsolutePath().toString(), "Cosine_0.0.png"), filelist[0]);
        try (InputStream file = new FileInputStream(filelist[0])) {
            Image imageRecovered = new Image(file);
            fxRobot.interact(() -> {
                assertEquals(chart.getWidth(), imageRecovered.getWidth());
                assertEquals(chart.getHeight(), imageRecovered.getHeight());
            });
        }

        // remove plugin
        fxRobot.interact(() -> chart.getPlugins().remove(screenshotPlugin));

        // verify that controls were removed from toolbar
        FxAssert.verifyThat(toolbar, node -> node.getChildren().size() == 0);
    }

    @Test
    public void filenamePatternTests() {
        assertDoesNotThrow(() -> fxRobot.interact(() -> {
            chart.getPlugins().add(screenshotPlugin);
            screenshotPlugin.setPattern("");
            screenshotPlugin.setDirectory("");
            chart.getDatasets().clear();
            assertEquals("myChart", screenshotPlugin.generateScreenshotName()); // chart id
            chart.setTitle("testChartTitle");
            assertEquals("testChartTitle", screenshotPlugin.generateScreenshotName()); // chart title
            screenshotPlugin.setPattern("asdf.png");
            screenshotPlugin.setDirectory("/tmp/testScreenshotPlugin");
            assertEquals("asdf.png", screenshotPlugin.generateScreenshotName()); // static pattern
            DoubleDataSet dataset = new DoubleDataSet("testDataSet");
            dataset.getMetaInfo().put("testField", "0.0000045");
            dataset.add(0.0, 4.5);
            chart.getDatasets().setAll(dataset);
            screenshotPlugin.setPattern("{dataSetName}_{yMax}_{testField;float;%e}.png"); // dynamic pattern
            assertEquals("testDataSet_4.5_4.500000e-06.png", screenshotPlugin.generateScreenshotName());
            screenshotPlugin.setPattern("");
            chart.setTitle("");
            chart.getTitleLabel().setSide(Side.RIGHT);
            chart.getTitleLabel().setTextFill(Color.BLUE);

            assertEquals("testDataSet", screenshotPlugin.generateScreenshotName()); // first data set name
            chart.getDatasets().clear();
            chart.setId("");
            assertEquals("UnknownChart", screenshotPlugin.generateScreenshotName()); // first data set name
        }));
    }
}
