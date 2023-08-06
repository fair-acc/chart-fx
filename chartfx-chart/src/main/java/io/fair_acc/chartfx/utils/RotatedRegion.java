package io.fair_acc.chartfx.utils;

import javafx.scene.Node;

import java.util.function.DoubleUnaryOperator;

/**
 * Utility class for working with rotating regions/controls that are
 * inside layout containers that do not properly account for rotations.
 *
 * @author ennerf
 */
public class RotatedRegion {

    @FunctionalInterface
    public interface ResizeRelocateMethod {
        void apply(double x, double y, double width, double height);
    }

    public RotatedRegion(Node node, DoubleUnaryOperator computePrefWidth, DoubleUnaryOperator computePrefHeight, ResizeRelocateMethod resizeRelocate) {
        this.node = node;
        this.computePrefWidth = computePrefWidth;
        this.computePrefHeight = computePrefHeight;
        this.resizeRelocate = resizeRelocate;
    }

    public double computePrefWidth(double length) {
        return getRotate() == 0 ? computePrefWidth.applyAsDouble(length) :
                bounds.setSize(computePrefWidth.applyAsDouble(length), computePrefHeight.applyAsDouble(length))
                        .rotateCenter(getRotate())
                        .getWidth();
    }

    public double computePrefHeight(double length) {
        return getRotate() == 0 ? computePrefHeight.applyAsDouble(length) :
                bounds.setSize(computePrefWidth.applyAsDouble(length), computePrefHeight.applyAsDouble(length))
                        .rotateCenter(getRotate())
                        .getHeight();
    }

    public void resizeRelocate(double x, double y, double width, double height) {
        if (getRotate() == 0) {
            resizeRelocate.apply(x, y, width, height);
        } else {
            // The bounds need to be set rotated so that the underlying container
            // computes a layout with appropriate word wrap, cutoff etc.
            bounds.setBounds(x, y, width, height).rotateCenter(getRotate());
            resizeRelocate.apply(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }
    }

    private double getRotate() {
        return node.getRotate();
    }

    private final Node node;
    private final DoubleUnaryOperator computePrefWidth;
    private final DoubleUnaryOperator computePrefHeight;
    private final ResizeRelocateMethod resizeRelocate;
    private final RotatedBounds bounds = new RotatedBounds();

}
