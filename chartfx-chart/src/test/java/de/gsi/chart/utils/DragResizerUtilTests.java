package de.gsi.chart.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import org.junit.jupiter.api.Test;

import de.gsi.chart.utils.DragResizerUtil.DragDirection;

/**
 * Tests for {@link de.gsi.chart.utils.DragResizerUtil}.
 *
 * @author rstein
 */
public class DragResizerUtilTests {
    @Test
    public void testConstructor() {
        assertDoesNotThrow(() -> new DragResizerUtil());
        assertNotNull(DragResizerUtil.DEFAULT_LISTENER);

        final Rectangle rect = new Rectangle(/* minX */ 0.0, /* minY */ 0.0, /* width */ 100.0, /* height */ 50.0);

        assertDoesNotThrow(() -> DragResizerUtil.makeResizable(rect));
        assertDoesNotThrow(() -> DragResizerUtil.makeResizable(rect, DragResizerUtil.DEFAULT_LISTENER));

        assertThrows(IllegalArgumentException.class, () -> DragResizerUtil.makeResizable(null));
    }

    @Test
    public void testDragDirections() {
        final Rectangle rect = new Rectangle(/* minX */ 0.0, /* minY */ 0.0, /* width */ 100.0, /* height */ 50.0);
        final DragResizerUtil util = new DragResizerUtil(rect, DragResizerUtil.DEFAULT_LISTENER);

        assertEquals(DragDirection.DRAG, util.currentMouseState(new MyMouseEvent(50, 25)));

        assertEquals(DragDirection.N_RESIZE, util.currentMouseState(new MyMouseEvent(50, -1)));
        assertEquals(DragDirection.N_RESIZE, util.currentMouseState(new MyMouseEvent(50, +1)));
        assertEquals(DragDirection.S_RESIZE, util.currentMouseState(new MyMouseEvent(50, 49)));
        assertEquals(DragDirection.S_RESIZE, util.currentMouseState(new MyMouseEvent(50, 51)));
        assertEquals(DragDirection.E_RESIZE, util.currentMouseState(new MyMouseEvent(101, 25)));
        assertEquals(DragDirection.E_RESIZE, util.currentMouseState(new MyMouseEvent(99, 25)));
        assertEquals(DragDirection.W_RESIZE, util.currentMouseState(new MyMouseEvent(-1, 25)));
        assertEquals(DragDirection.W_RESIZE, util.currentMouseState(new MyMouseEvent(+1, 25)));

        assertEquals(DragDirection.NE_RESIZE, util.currentMouseState(new MyMouseEvent(101, -1)));
        assertEquals(DragDirection.NE_RESIZE, util.currentMouseState(new MyMouseEvent(99, +1)));
        assertEquals(DragDirection.SE_RESIZE, util.currentMouseState(new MyMouseEvent(99, 49)));
        assertEquals(DragDirection.SE_RESIZE, util.currentMouseState(new MyMouseEvent(101, 51)));
        assertEquals(DragDirection.NW_RESIZE, util.currentMouseState(new MyMouseEvent(-1, -1)));
        assertEquals(DragDirection.NW_RESIZE, util.currentMouseState(new MyMouseEvent(+1, +1)));
        assertEquals(DragDirection.SW_RESIZE, util.currentMouseState(new MyMouseEvent(-1, 49)));
        assertEquals(DragDirection.SW_RESIZE, util.currentMouseState(new MyMouseEvent(+1, 51)));

        assertEquals(DragDirection.DEFAULT, util.currentMouseState(new MyMouseEvent(-20, -20)));
    }

    @Test
    public void testDragMechanics() {
        final Rectangle rect = new Rectangle(/* minX */ 0.0, /* minY */ 0.0, /* width */ 100.0, /* height */ 50.0);
        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(rect, 0, 0, 100, 50));

        final DragResizerUtil util = new DragResizerUtil(rect, DragResizerUtil.DEFAULT_LISTENER);

        // N.B. these are rather weak tests
        assertDoesNotThrow(() -> util.mouseReleased(new MyMouseEvent(110, 60, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseOver(new MyMouseEvent(110, 60, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mousePressed(new MyMouseEvent(0, 0, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mousePressed(new MyMouseEvent(-10, -10, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mousePressed(new MyMouseEvent(50, 25, MouseButton.PRIMARY, 1)));

        // actual drag operation
        // start drag
        assertDoesNotThrow(() -> util.mousePressed(new MyMouseEvent(0, 0, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseDragged(new MyMouseEvent(-10, -10, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseDragged(new MyMouseEvent(-20, -20, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseDragged(new MyMouseEvent(110, 70, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseDragged(new MyMouseEvent(-50, 70, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseReleased(new MyMouseEvent(50, 25)));

        // reset rectangle
        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(rect, 0, 0, 100, 50));
        // start drag
        assertDoesNotThrow(() -> util.mousePressed(new MyMouseEvent(50, 25, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseDragged(new MyMouseEvent(-10, -10, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseDragged(new MyMouseEvent(50, 25, MouseButton.PRIMARY, 1)));
        assertDoesNotThrow(() -> util.mouseReleased(new MyMouseEvent(50, 25)));
    }

    @Test
    public void testHelper() {
        for (DragDirection direction : DragDirection.values()) {
            // cheap test
            assertNotNull(DragResizerUtil.getCursorForState(direction));
        }
    }

    @Test
    public void testResetNode() {
        final Region region = new Region();

        final DragResizerUtil util = new DragResizerUtil(region, DragResizerUtil.DEFAULT_LISTENER);

        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(region, 0, 0, 10, 20));
        assertEquals(10, region.getPrefWidth());
        assertEquals(20, region.getPrefHeight());

        // non secondary should not trigger
        assertDoesNotThrow(() -> util.resetNodeSize(new MyMouseEvent(110, 60, MouseButton.PRIMARY, 1)));
        assertEquals(10, region.getPrefWidth());
        assertEquals(20, region.getPrefHeight());

        // secondary single-click should not trigger
        assertDoesNotThrow(() -> util.resetNodeSize(new MyMouseEvent(110, 60, MouseButton.PRIMARY, 1)));
        assertEquals(10, region.getPrefWidth());
        assertEquals(20, region.getPrefHeight());

        assertDoesNotThrow(() -> util.resetNodeSize(new MyMouseEvent(110, 60, MouseButton.PRIMARY, 2)));
        assertEquals(-1, region.getPrefWidth());
        assertEquals(-1, region.getPrefHeight());

        final Rectangle rect = new Rectangle(/* minX */ 0.0, /* minY */ 0.0, /* width */ 100.0, /* height */ 50.0);
        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(rect, 0, 0, 10, 20));
        assertEquals(10, rect.getWidth());
        assertEquals(20, rect.getHeight());

        final DragResizerUtil util2 = new DragResizerUtil(rect, DragResizerUtil.DEFAULT_LISTENER);
        assertDoesNotThrow(() -> util2.resetNodeSize(new MyMouseEvent(110, 60, MouseButton.SECONDARY, 2)));
        assertEquals(10, rect.getWidth());
        assertEquals(20, rect.getHeight());
    }

    @Test
    public void testResizeHandler() {
        final Rectangle rect = new Rectangle(/* minX */ 0.0, /* minY */ 0.0, /* width */ 100.0, /* height */ 50.0);

        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(rect, 0, 0, 10, 20));
        assertEquals(10, rect.getWidth());
        assertEquals(20, rect.getHeight());
        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onResize(rect, 0, 0, 11, 21));
        assertEquals(11, rect.getWidth());
        assertEquals(21, rect.getHeight());

        final Canvas canvas = new Canvas(100, 50);
        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(canvas, 0, 0, 10, 20));
        assertEquals(10, canvas.getWidth());
        assertEquals(20, canvas.getHeight());

        final Region region = new Region();
        region.setMinSize(100, 50);
        assertDoesNotThrow(() -> DragResizerUtil.DEFAULT_LISTENER.onDrag(region, 0, 0, 10, 20));
        assertEquals(10, region.getPrefWidth());
        assertEquals(20, region.getPrefHeight());
    }

    @Test
    public void testResizeZones() {
        final Rectangle rect = new Rectangle(/* minX */ 0.0, /* minY */ 0.0, /* width */ 100.0, /* height */ 50.0);

        final DragResizerUtil util = new DragResizerUtil(rect, DragResizerUtil.DEFAULT_LISTENER);

        assertTrue(DragResizerUtil.isTopResizeZone(new MyMouseEvent(50, -1)));
        assertTrue(DragResizerUtil.isTopResizeZone(new MyMouseEvent(50, +1)));
        assertFalse(DragResizerUtil.isTopResizeZone(new MyMouseEvent(50, +51)));

        assertTrue(util.isBottomResizeZone(new MyMouseEvent(50, 49)));
        assertTrue(util.isBottomResizeZone(new MyMouseEvent(50, 51)));
        assertFalse(util.isBottomResizeZone(new MyMouseEvent(50, 0)));

        assertTrue(DragResizerUtil.isLeftResizeZone(new MyMouseEvent(-1, 25)));
        assertTrue(DragResizerUtil.isLeftResizeZone(new MyMouseEvent(+1, 25)));
        assertFalse(DragResizerUtil.isLeftResizeZone(new MyMouseEvent(101, 25)));

        assertTrue(util.isRightResizeZone(new MyMouseEvent(99, 25)));
        assertTrue(util.isRightResizeZone(new MyMouseEvent(101, 25)));
        assertFalse(util.isRightResizeZone(new MyMouseEvent(0, 25)));

        assertTrue(util.isInDragZone(new MyMouseEvent(50, 25)));
        assertFalse(util.isInDragZone(new MyMouseEvent(-10, -10)));
        assertFalse(util.isInDragZone(new MyMouseEvent(-10, 25)));
        assertFalse(util.isInDragZone(new MyMouseEvent(110, 25)));
        assertFalse(util.isInDragZone(new MyMouseEvent(50, -10)));
        assertFalse(util.isInDragZone(new MyMouseEvent(50, 60)));

        assertTrue(util.isInResizeZone(new MyMouseEvent(0, 25)));
        assertTrue(util.isInResizeZone(new MyMouseEvent(100, 25)));
        assertTrue(util.isInResizeZone(new MyMouseEvent(50, 50)));
        assertTrue(util.isInResizeZone(new MyMouseEvent(50, 0)));
        assertTrue(util.isInResizeZone(new MyMouseEvent(50, 50)));
        assertFalse(util.isInResizeZone(new MyMouseEvent(50, 25)));
        assertFalse(util.isInResizeZone(new MyMouseEvent(-10, -10)));
        assertFalse(util.isInResizeZone(new MyMouseEvent(110, -10)));
        assertFalse(util.isInResizeZone(new MyMouseEvent(-10, 60)));
        assertFalse(util.isInResizeZone(new MyMouseEvent(110, 60)));
    }

    private class MyMouseEvent extends MouseEvent {
        private static final long serialVersionUID = 0L;

        MyMouseEvent(final double x, final double y) {
            super(MouseEvent.MOUSE_MOVED, x, y, //
                    x, y, // screen coordinates
                    MouseButton.NONE, // mouse button
                    0, // clickCount
                    false, // shiftDown
                    false, // controlDown
                    false, // altDown
                    false, // metaDown
                    false, // primaryButtonDown
                    false, // middleButtonDown
                    false, // secondaryButtonDown
                    true, // synthesised
                    false, // popupTrigger
                    true, // stillSincePress
                    null // pickResult
            );
        }

        MyMouseEvent(final double x, final double y, final MouseButton mouseButton, final int clickCount) {
            super(MouseEvent.MOUSE_MOVED, x, y, //
                    x, y, // screen coordinates
                    mouseButton, // mouse button
                    clickCount, // clickCount
                    false, // shiftDown
                    false, // controlDown
                    false, // altDown
                    false, // metaDown
                    MouseButton.PRIMARY.equals(mouseButton), // primaryButtonDown
                    MouseButton.MIDDLE.equals(mouseButton), // middleButtonDown
                    MouseButton.SECONDARY.equals(mouseButton), // secondaryButtonDown
                    true, // synthesised
                    false, // popupTrigger
                    true, // stillSincePress
                    null // pickResult
            );
        }
    }
}
