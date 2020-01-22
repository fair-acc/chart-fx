package de.gsi.chart.plugins;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * Plugin allowing to take a screenshot of the complete chart.
 * Allows to copy image to clipboard or to save as a file.
 *
 * @author Alexander Krimm
 */
public class Screenshot extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Screenshot.class);
    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 20;

    private final HBox screenshotButtons = getScreenshotInteractorBar();

    /**
     * Create a screenshot plugin instance
     */
    public Screenshot() {
        super();

        chartProperty().addListener((change, o, n) -> {
            if (o != null) {
                o.getToolBar().getChildren().remove(screenshotButtons);
            }
            if (n != null) {
                if (isAddButtonsToToolBar()) {
                    n.getToolBar().getChildren().add(screenshotButtons);
                }
            }
        });
    }

    /**
     * @return A node with screenshot buttons which can be inserted into the toolbar 
     */
    public HBox getScreenshotInteractorBar() {
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        final HBox buttonBar = new HBox();
        buttonBar.setPadding(new Insets(1, 1, 1, 1));
        final Button screenshotToClipboard = new Button(null,
                new HBox(0.1, new Glyph(FONT_AWESOME, FontAwesome.Glyph.CAMERA).size(FONT_SIZE),
                        new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLIPBOARD).size(FONT_SIZE - 8)));
        screenshotToClipboard.setPadding(new Insets(3, 3, 3, 3));
        screenshotToClipboard.setTooltip(new Tooltip("Copy screenshot into clipboard"));
        screenshotToClipboard.setOnAction(evt -> screenshotToClipboard());

        final Button screenshotToFile = new Button(null,
                new HBox(1.0, new Glyph(FONT_AWESOME, FontAwesome.Glyph.CAMERA).size(FONT_SIZE),
                        new Glyph(FONT_AWESOME, FontAwesome.Glyph.FILE).size(FONT_SIZE - 8)));
        screenshotToFile.setPadding(new Insets(3, 3, 3, 3));
        screenshotToFile.setTooltip(new Tooltip("Save screenshot as file"));
        screenshotToFile.setOnAction(evt -> screenshotToFile());

        buttonBar.getChildren().addAll(separator, screenshotToClipboard, screenshotToFile);
        return buttonBar;
    }

    /**
     * Save screenshot to clipbaord
     */
    private void screenshotToClipboard() {
        Image image = getScreenshot();
        Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putImage(image);
        clipboard.setContent(content);
        LOGGER.atInfo().log("Copied screenshot to clipboard");
    }

    /**
     * saves a screenshot to a file that can be chosen with a file opener
     */
    private void screenshotToFile() {
        Image image = getScreenshot();
        File file = showFileDialog();
        if (file == null)
            return;
        saveImage(image, file);
        LOGGER.atInfo().addArgument(file.getName()).log("Saved screenshot to {}");
    }

    /**
     * @return An image containing a screenshot of the complete chart
     */
    private Image getScreenshot() {
        SnapshotParameters snapParams = new SnapshotParameters();
        WritableImage result = chartProperty().get().snapshot(snapParams, null);
        return result;
    }

    /**
     * Saves a file to a png file
     * @param image the imaga data
     * @param file The file to save to
     */
    private static void saveImage(final Image image, final File file) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            LOGGER.atError().addArgument(file.getName()).log("Error saving screenshot to {}");
        }
    }

    /**
     * @return The file to save the screenshot to (or null if canceled)
     */
    private File showFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().setAll(new ExtensionFilter("PNG-Image", new String[] {"*.png"}));
        return fileChooser.showSaveDialog(getChart().getScene().getWindow());
    }
}
