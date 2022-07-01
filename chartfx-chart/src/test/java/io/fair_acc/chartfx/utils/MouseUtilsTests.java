package io.fair_acc.chartfx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link io.fair_acc.chartfx.utils.MouseUtils}.
 *
 * @author rstein
 */
public class MouseUtilsTests {
    @Test
    public void mouseInsideBoundaryBoxDistanceTests() {
        final Bounds screenBounds = new BoundingBox(/* minX */ 0.0, /* minY */ 0.0, /* width */ 10.0, /* height */ 5.0);

        // mouse outside
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(-2, 0)));
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(12, 0)));
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(0, -2)));
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(0, 7)));

        // mouse inside
        assertEquals(2, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(2.0, 2.5)));
        assertEquals(2, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(8.0, 2.5)));
        assertEquals(2, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(5, 2)));
        assertEquals(2, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(5, 3)));

        // mouse on boundary
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(0, 3)));
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(3, 0)));
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(10, 3)));
        assertEquals(0, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(3, 5)));

        // test Manhattan-Norm
        assertEquals(1, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(1, 3)));
        assertEquals(1, MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, new Point2D(5, 1)));
    }

    @Test
    public void mouseOutsideBoundaryBoxDistanceTests() {
        final Bounds screenBounds = new BoundingBox(/* minX */ 0.0, /* minY */ 0.0, /* width */ 10.0, /* height */ 5.0);

        // mouse inside
        assertEquals(0, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(1, 1)));
        assertEquals(0, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(0, 0)));
        assertEquals(0, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(10, 5)));

        // mouse outside
        assertEquals(2, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(-2.0, 0)));
        assertEquals(2, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(12.0, 0)));
        assertEquals(2, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(0.0, -2)));
        assertEquals(2, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(0.0, 7)));

        // test Manhattan-Norm
        assertEquals(4, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(-2.0, 9)));
        assertEquals(4, MouseUtils.mouseOutsideBoundaryBoxDistance(screenBounds, new Point2D(-4.0, 7)));
    }
}
