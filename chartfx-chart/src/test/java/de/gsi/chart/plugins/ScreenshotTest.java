package de.gsi.chart.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.testdata.spi.CosineFunction;

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
        Scene scene = new Scene(chart, 800, 600);
        screenshotPlugin = new Screenshot();
        chart.setPrefWidth(800);
        chart.setPrefHeight(600);
        chart.getDatasets().add(new CosineFunction("Cosine", 50));
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void screenshotTests() throws IOException {
        // setup some useful variables
        final FlowPane toolbar = chart.getToolBar();
        Path tmpdir = Files.createTempDirectory("screenshotTest");

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
        assertEquals(1, filelist.length);
        assertEquals(new File(tmpdir.toAbsolutePath().toString(), "Cosine_-0.1.png"), filelist[0]);
        WritableImage imageRecovered = SwingFXUtils.toFXImage(ImageIO.read(filelist[0]), null);
        fxRobot.interact(() -> {
            assertEquals(chart.getWidth(), imageRecovered.getWidth());
            assertEquals(chart.getHeight(), imageRecovered.getHeight());
        });

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
            assertEquals("testDataSet", screenshotPlugin.generateScreenshotName()); //first data set name
            chart.getDatasets().clear();
            chart.setId("");
            assertEquals("UnknownChart", screenshotPlugin.generateScreenshotName()); //first data set name
        }));
    }
}
