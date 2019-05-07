package de.gsi.chart.utils;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

/**
 * Simple class to make a periodic (or on-demand) screen-shot of given JavaFX scene to file. Class permits to add an ISO
 * date-time string
 *
 * @author rstein
 */
public class PeriodicScreenCapture implements Observable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicScreenCapture.class);
    private static final String DEFAULT_TIME_FORMAT = "yyyyMMdd_HHmmss";
    private static final String FILE_LOGGING_SUFFIX = ".png";
    private static final String IMAGE_FORMAT = "png";
    private final Scene primaryScene;
    private final Path path;
    private final String fileName;
    private final double delay;
    private final double period;
    private Timeline periodicTask; // for JavaFX tasks
    private String isoDateTimeFormatString = DEFAULT_TIME_FORMAT;
    private final boolean addDateTime;
    protected final List<InvalidationListener> listeners = new LinkedList<>();
    private final Timer timer = new Timer(); // for non-JavaFX tasks

    public PeriodicScreenCapture(final Path path, final String fileName, final Scene scene, final double delay,
            final double period) {
        this(path, fileName, scene, delay, period, false);

    }

    public PeriodicScreenCapture(final Path path, final String fileName, final Scene scene, final double delay,
            final double period, final boolean addDateTime) {
        this.path = path;
        this.fileName = fileName.replaceAll(".png", "").replaceAll(".PNG", "");
        primaryScene = scene;
        this.delay = delay;
        this.period = period;
        this.addDateTime = addDateTime;
    }

    public void start() {
        if (periodicTask != null) {
            periodicTask.stop();
        }
        periodicTask = new Timeline(new KeyFrame(Duration.seconds(period), new EventHandler<ActionEvent>() {

            @Override
            public void handle(final ActionEvent event) {
                performScreenCapture();
            }
        }));
        periodicTask.setDelay(Duration.seconds(delay));
        periodicTask.setCycleCount(Animation.INDEFINITE);
        periodicTask.play();
    }

    public void stop() {
        if (periodicTask != null) {
            periodicTask.stop();
        }
    }

    public void performScreenCapture() {
        try {
            final WritableImage image = primaryScene.snapshot(null);
            // open save in separate thread
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    writeImage(image);
                }
            }, 0);

            LOGGER.debug("this is called periodic on UI thread");
        } catch (final Exception e) {
            // continue at all costs
            LOGGER.error("error while writing screen captured image to file", e);
        }
    }

    public void setIsoDateTimeFormatterString(final String newFormat) {
        if (newFormat == null || newFormat.isEmpty()) {
            throw new IllegalArgumentException("new format must not be null or empty");
        }
        isoDateTimeFormatString = newFormat;
    }

    public String getIsoDateTimeFormatterString() {
        return isoDateTimeFormatString;
    }

    protected static String getISODate(final long time_ms, final String format) {
        final long time = TimeUnit.MILLISECONDS.toMillis(time_ms);
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final DateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(tz);
        return df.format(new Date(time));
    }

    private void writeImage(final Image image) {
        final long now = System.currentTimeMillis();
        try {
            final String format = getIsoDateTimeFormatterString();
            final String longFileName = addDateTime && format != null && !format.isEmpty()
                    ? path.toFile() + String.format("/%s_%s%s", fileName, getISODate(now, format), FILE_LOGGING_SUFFIX)
                    : path.toFile() + "/" + fileName;
            final String tempFileName = longFileName + "_temp.png";
            final File file = new File(tempFileName);
            if (file.getParentFile().mkdirs()) {
                LOGGER.info("needed to create directory for file: " + longFileName);
            }

            ImageIO.write(SwingFXUtils.fromFXImage(image, null), IMAGE_FORMAT, file);
            Files.move(Paths.get(tempFileName), Paths.get(longFileName), REPLACE_EXISTING);
            fireInvalidated();
            LOGGER.debug("write screenshot to " + tempFileName + " -> " + longFileName);
        } catch (final Exception e) {
            LOGGER.error("could not write to file: '" + fileName + "'", e);
        }
    }

    @Override
    public void addListener(final InvalidationListener listener) {
        Objects.requireNonNull(listener, "InvalidationListener must not be null");
        // N.B. suppress duplicates
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final InvalidationListener listener) {
        listeners.remove(listener);
    }

    public void fireInvalidated() {
        if (listeners.isEmpty()) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            executeFireInvalidated();
        } else {
            Platform.runLater(this::executeFireInvalidated);
        }
    }

    protected void executeFireInvalidated() {
        for (final InvalidationListener listener : new ArrayList<>(listeners)) {
            listener.invalidated(this);
        }
    }

}
