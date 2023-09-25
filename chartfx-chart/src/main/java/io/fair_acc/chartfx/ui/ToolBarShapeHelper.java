package io.fair_acc.chartfx.ui;

import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;

/**
 * @author rstein
 */
public final class ToolBarShapeHelper {
    private ToolBarShapeHelper() {
    }

    public static Shape getToolBarShape(final double width, final double height, final double radii) {
        final double centreX = 0.0;
        final double centreY = 0.0;
        final double halfWidth = 0.5 * width + 2 * radii;
        final double halfHeight = 0.5 * height;

        Path path = new Path();

        // go to left-top most corner
        path.getElements().add(new MoveTo(centreX - halfWidth - 4 * radii, centreY - halfHeight));

        // cubic sweep down
        path.getElements().add(new CubicCurveTo( //
                centreX - halfWidth - 2 * radii, centreY - halfHeight, // first control point
                centreX - halfWidth - 2 * radii, centreY + halfHeight, // second control point
                centreX - halfWidth, centreY + halfHeight)); // to coordinate
        // line on bottom
        path.getElements().add(new LineTo(centreX + halfWidth, centreY + halfHeight));

        // cubic sweep up
        path.getElements().add(new CubicCurveTo( //
                centreX + halfWidth + 2 * radii, centreY + halfHeight, // first control point
                centreX + halfWidth + 2 * radii, centreY - halfHeight, // second control point
                centreX + halfWidth + 4 * radii, centreY - halfHeight)); // to coordinate

        // return to top left corner
        path.getElements().add(new LineTo(centreX - halfWidth - 2 * radii, centreY - halfHeight));
        return path;
    }
}
