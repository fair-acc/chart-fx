package io.fair_acc.misc.samples.plugins;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.math.TRandom;

/**
 * @author rstein
 */
public class Snow extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Snow.class);
    private static final TRandom RND = new TRandom(0);
    private final Group circles = new Group();
    private final IntegerProperty numberOfFlakes = new SimpleIntegerProperty(this, "numberOfFlakes", 100);
    private final DoubleProperty velocity = new SimpleDoubleProperty(this, "velocity", 0.1);
    private final DoubleProperty meanSize = new SimpleDoubleProperty(this, "meanSize", 10.0);
    private final DoubleProperty rmsSize = new SimpleDoubleProperty(this, "rmsSize", 5.0);
    private final ObjectProperty<Color> snowColor = new SimpleObjectProperty<>(this, "snowColor");
    private final BooleanProperty snow = new SimpleBooleanProperty(this, "snow", true);
    private final ChangeListener changeListener = (ch, o, n) -> this.init();

    public Snow() {
        this(100, 10.0, 5.0, Color.web("white", 0.7));
    }

    public Snow(final int nSnowFlakes, final double meanSnowFlakeSize, final double rmsSnowFlakeSize,
            final Color color) {
        super();

        numberOfFlakesProperty().set(nSnowFlakes);
        meanSizeProperty().set(meanSnowFlakeSize);
        rmsSizeProperty().set(rmsSnowFlakeSize);
        snowColorProperty().set(color);

        init(); // NOPMD

        final Timeline timeline = new Timeline(new KeyFrame(Duration.millis(20), t -> {
            for (Node node : circles.getChildren()) {
                if (node instanceof PhysicalSnowFlake) {
                    PhysicalSnowFlake flake = (PhysicalSnowFlake) node;
                    flake.animate();
                }
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        chartProperty().addListener((ch, o, n) -> {
            if (o != null) {
                o.getCanvasForeground().getChildren().remove(circles);
                o.getCanvas().widthProperty().removeListener(changeListener);
                o.getCanvas().heightProperty().removeListener(changeListener);
            }
            if (n != null) {
                n.getCanvasForeground().getChildren().add(circles);
                n.getCanvas().widthProperty().addListener(changeListener);
                n.getCanvas().heightProperty().addListener(changeListener);
                init();
            }
        });

        snowProperty().addListener(changeListener);
        numberOfFlakesProperty().addListener(changeListener);
        meanSizeProperty().addListener(changeListener);
        rmsSizeProperty().addListener(changeListener);
        snowColorProperty().addListener(changeListener);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("init Snow plugin");
        }
    }

    public DoubleProperty meanSizeProperty() {
        return meanSize;
    }

    public IntegerProperty numberOfFlakesProperty() {
        return numberOfFlakes;
    }

    public DoubleProperty rmsSizeProperty() {
        return rmsSize;
    }

    public ObjectProperty<Color> snowColorProperty() {
        return snowColor;
    }

    public BooleanProperty snowProperty() {
        return snow;
    }

    public DoubleProperty velocityProperty() {
        return velocity;
    }

    protected void init() {
        final int n = numberOfFlakesProperty().get();
        List<PhysicalSnowFlake> list = new ArrayList<>(n);
        if (snowProperty().get()) {
            for (int i = 0; i < n; i++) {
                final double radius = RND.Gaus(meanSizeProperty().get(), rmsSizeProperty().get());
                final PhysicalSnowFlake circle = new PhysicalSnowFlake(radius, snowColorProperty().get()); // NOPMD
                circle.setStrokeType(StrokeType.OUTSIDE);
                circle.setStroke(Color.web("black", 0.16));
                circle.init();
                list.add(circle);
            }
        }
        circles.getChildren().setAll(list);
    }

    protected class PhysicalSnowFlake extends SnowFlake {
        private final double size;
        private double x;
        private double y;

        public PhysicalSnowFlake(final double radius, final Paint fill) {
            this(0.0, 0.0, radius, 2, fill);
        }

        protected PhysicalSnowFlake(final double centerX, final double centerY, final double radius,
                final int recursion, final Paint fill) {
            super(centerX, centerY, radius, recursion, fill);
            size = radius;
            init(); // NOPMD
        }

        /**
         * animation based on snowflake velocity analysis taken from:
         * http://iacweb.ethz.ch/doc/publications/schefold_ogden_2002.pdf
         */
        public void animate() {
            if (Snow.this.getChart() == null) {
                return;
            }
            final double yMax = getChart().getCanvas().getHeight();

            final double yp = velocity.get() * Math.sqrt(Math.abs(size));
            final double xp = (Math.random() - 0.5) * yp;
            x += xp;
            y += yp;
            if (y > yMax) {
                init();
                y = -size;
            }

            this.setTranslateX(x);
            this.setTranslateY(y);
        }

        protected void init() {
            if (Snow.this.getChart() == null) {
                return;
            }
            final double xMax = getChart().getCanvas().getWidth();
            final double yMax = getChart().getCanvas().getHeight();

            x = Math.random() * xMax;
            y = Math.random() * yMax;
            this.setTranslateX(x - size / 2);
            this.setTranslateY(y - size / 2);
        }
    }
}
