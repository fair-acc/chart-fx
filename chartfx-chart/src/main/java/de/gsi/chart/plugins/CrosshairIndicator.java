/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.plugins;

import de.gsi.chart.axes.Axis;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 * Horizontal and vertical {@link Line} drawn on the plot area, crossing at the mouse cursor location, together with a
 * {@link Label} displaying the cursor coordinates in data units.
 * <p>
 * CSS style class names: {@value #STYLE_CLASS_PATH} and {@value #STYLE_CLASS_LABEL}
 *
 * @author Grzegorz Kruk
 */
public class CrosshairIndicator extends AbstractDataFormattingPlugin {

    /**
     * Name of the CSS class of the horizontal and vertical lines path.
     */
    public static final String STYLE_CLASS_PATH = "chart-crosshair-path";

    /**
     * Name of the CSS class of the label displaying mouse coordinates.
     */
    public static final String STYLE_CLASS_LABEL = "chart-crosshair-label";

    private static final int LABEL_X_OFFSET = 15;
    private static final int LABEL_Y_OFFSET = 5;

    private final Path crosshairPath = new Path();
    private final Label coordinatesLabel = new Label();

    private final EventHandler<MouseEvent> mouseMoveHandler = (final MouseEvent event) -> {
        final Bounds plotAreaBounds = getChart().getBoundsInLocal();
        if (!plotAreaBounds.contains(event.getX(), event.getY())) {
            getChartChildren().clear();
            return;
        }

        updatePath(event, plotAreaBounds);
        updateLabel(event, plotAreaBounds);

        if (!getChartChildren().contains(crosshairPath)) {
            getChartChildren().addAll(crosshairPath, coordinatesLabel);
        }
    };

    /**
     * Creates a new instance of CrosshairIndicator class.
     */
    public CrosshairIndicator() {
        crosshairPath.getStyleClass().add(CrosshairIndicator.STYLE_CLASS_PATH);
        crosshairPath.setManaged(false);
        coordinatesLabel.getStyleClass().add(CrosshairIndicator.STYLE_CLASS_LABEL);
        coordinatesLabel.setManaged(false);

        registerInputEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler);
    }

    private String formatLabelText(final Point2D displayPointInPlotArea) {
        final Axis yAxis = getChart().getFirstAxis(Orientation.VERTICAL);
        if (yAxis == null) {
            return getChart() + " - " + "no y-axis present to translate point " + displayPointInPlotArea;
        }
        return formatData(getChart(), toDataPoint(yAxis, displayPointInPlotArea));
    }

    private void updateLabel(final MouseEvent event, final Bounds plotAreaBounds) {
        coordinatesLabel.setText(formatLabelText(getLocationInPlotArea(event)));

        final double width = coordinatesLabel.prefWidth(-1);
        final double height = coordinatesLabel.prefHeight(width);

        double xLocation = event.getX() + CrosshairIndicator.LABEL_X_OFFSET;
        double yLocation = event.getY() + CrosshairIndicator.LABEL_Y_OFFSET;

        if (xLocation + width > plotAreaBounds.getMaxX()) {
            xLocation = event.getX() - CrosshairIndicator.LABEL_X_OFFSET - width;
        }
        if (yLocation + height > plotAreaBounds.getMaxY()) {
            yLocation = event.getY() - CrosshairIndicator.LABEL_Y_OFFSET - height;
        }
        coordinatesLabel.resizeRelocate(xLocation, yLocation, width, height);
    }

    private void updatePath(final MouseEvent event, final Bounds plotAreaBounds) {
        final ObservableList<PathElement> path = crosshairPath.getElements();
        path.clear();
        path.add(new MoveTo(plotAreaBounds.getMinX() + 1, event.getY()));
        path.add(new LineTo(plotAreaBounds.getMaxX(), event.getY()));
        path.add(new MoveTo(event.getX(), plotAreaBounds.getMinY() + 1));
        path.add(new LineTo(event.getX(), plotAreaBounds.getMaxY()));
    }
}
