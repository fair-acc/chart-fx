package de.gsi.silly.samples.plugins;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;

/**
 * Basic implementation of a fractal Koch snowflake.
 * For details see: https://en.wikipedia.org/wiki/Koch_snowflake
 *
 * @author rstein
 */
public class SnowFlake extends Path {
    /**
     * Defines the horizontal position of the center of the circle in pixels.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty centerX = new SimpleDoubleProperty(this, "centerX", 0.0) {
        @Override
        public void invalidated() {
            NodeHelper.markDirty(SnowFlake.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(SnowFlake.this);
            setTranslateX(get() - radius.get());
        }
    };

    /**
     * Defines the vertical position of the center of the circle in pixels.
     *
     * @defaultValue 0.0
     */
    private final DoubleProperty centerY = new SimpleDoubleProperty(this, "centerY", 0.0) {
        @Override
        public void invalidated() {
            NodeHelper.markDirty(SnowFlake.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(SnowFlake.this);
            setTranslateX(get() - radius.get());
        }
    };

    /**
     * Defines the radius of the circle in pixels.
     *
     * @defaultValue 5.0
     */
    private final DoubleProperty radius = new SimpleDoubleProperty(this, "radius", 5.0) {
        @Override
        public void invalidated() {
            NodeHelper.markDirty(SnowFlake.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(SnowFlake.this);
            updatePath();
        }
    };

    /**
     * Defines the number of recursive iterations for Koch's snowflake
     *
     * @defaultValue 3
     */
    private final IntegerProperty nIterations = new SimpleIntegerProperty(this, "nIterations", 3) {
        @Override
        public void invalidated() {
            NodeHelper.markDirty(SnowFlake.this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(SnowFlake.this);
            updatePath();
        }
    };

    private double xState;
    private double yState;
    private double angleState;

    public SnowFlake(final double centerX, final double centerY, final double radius, final int recursion,
            final Paint fill) {
        super();
        setCenterX(centerX);
        setCenterY(centerY);
        setRadius(radius);
        setFill(fill);
        setRecursion(recursion);
        updatePath(); // NOPMD
    }

    public SnowFlake(final double radius, final Paint fill) {
        this(0.0, 0.0, radius, 3, fill);
    }

    public final DoubleProperty centerXProperty() {
        return centerX;
    }

    public final DoubleProperty centerYProperty() {
        return centerY;
    }

    public final double getCenterX() {
        return centerX.get();
    }

    public final double getCenterY() {
        return centerY.get();
    }

    public final double getRadius() {
        return radius.get();
    }

    public final int getRecursion() {
        return nIterations.get();
    }

    public final DoubleProperty radiusProperty() {
        return radius;
    }

    public final IntegerProperty recursionProperty() {
        return nIterations;
    }

    public final void setCenterX(double value) {
        if (value != 0.0) {
            centerXProperty().set(value);
        }
    }

    public final void setCenterY(double value) {
        if (value != 0.0) {
            centerYProperty().set(value);
        }
    }

    public final void setRadius(double value) {
        radius.set(value);
    }

    public final void setRecursion(int value) {
        nIterations.set(value);
    }

    private void koch(int n, double size) {
        if (n == 0) {
            xState += size * Math.cos(Math.toRadians(angleState));
            yState += size * Math.sin(Math.toRadians(angleState));
            this.getElements().add(new LineTo(xState, yState));
        } else {
            koch(n - 1, size);
            angleState += 60.0;
            koch(n - 1, size);
            angleState -= 120.0;
            koch(n - 1, size);
            angleState += 60.0;
            koch(n - 1, size);
        }
    }

    protected void updatePath() {
        getElements().clear();
        final int n = Math.max(getRecursion(), 1);
        final double flakeRadius = getRadius();
        final double side = Math.abs(flakeRadius * Math.sqrt(3) / Math.pow(3, n));
        xState = 0.0;
        yState = 0.0;
        angleState = 0.0;
        getElements().add(new MoveTo(flakeRadius, 0.0f));
        for (int i = 0; i < 3; i++) {
            koch(n, side);
            angleState -= 120.0;
        }
        getElements().add(new LineTo(flakeRadius, 0.0f));

        setTranslateX(this.getCenterX() - flakeRadius);
        setTranslateY(this.getCenterY() - flakeRadius);
    }
}
