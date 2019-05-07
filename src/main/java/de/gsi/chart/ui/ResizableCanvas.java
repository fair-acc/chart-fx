package de.gsi.chart.ui;

import javafx.scene.canvas.Canvas;

/**
 * extended default Canvas class to be embeddable and resizable within a
 * StackPane or Region
 *
 * @author rstein
 */
public class ResizableCanvas extends Canvas {
    public ResizableCanvas() {
        super();
    }

    @Override
    public double minHeight(final double width) {
        return 1;
    }

    @Override
    public double maxHeight(final double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double prefHeight(final double width) {
        return getHeight();
    }

    @Override
    public double minWidth(final double height) {
        return 1;
    }

    @Override
    public double maxWidth(final double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public double prefWidth(final double height) {
        return this.getWidth();
    }

    @Override
    public void resize(final double w, final double h) {
        super.resize(w, h);
        setWidth(w);
        setHeight(h);
        // System.err.println("canvas resize to = " + String.format("%fx%f",
        // getWidth(), getHeight()));
    }

    @Override
    public boolean isResizable() {
        return true;
    }
}