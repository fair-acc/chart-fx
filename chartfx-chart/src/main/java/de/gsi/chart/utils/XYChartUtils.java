/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.gsi.chart.utils;

import java.util.LinkedList;
import java.util.List;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * @author braeun
 */
public final class XYChartUtils {

    private XYChartUtils() {

    }

    /**
     * Returns Chart instance containing given child node.
     *
     * @param chartChildNode the node contained within the chart
     * @return chart or {@code null} if the node does not belong to chart
     */
    public static Chart getChart(final Node chartChildNode) {
        Node node = chartChildNode;
        while (node != null && !(node instanceof Chart)) {
            node = node.getParent();
        }
        return (Chart) node;
    }

    public static Region getChartContent(final Chart chart) {
        return (Region) chart.lookup(".chart-content");
    }

    public static List<Label> getChildLabels(final List<? extends Parent> parents) {
        final List<Label> labels = new LinkedList<>();
        for (final Parent parent : parents) {
            for (final Node node : parent.getChildrenUnmodifiable()) {
                if (node instanceof Label) {
                    labels.add((Label) node);
                }
            }
        }
        return labels;
    }

    public static double getHorizontalInsets(final Insets insets) {
        return insets.getLeft() + insets.getRight();
    }

    public static Pane getLegend(final XYChart chart) {
        return (Pane) chart.lookup(".chart-legend");
    }

    public static double getLocationX(final Node node) {
        return node.getLayoutX() + node.getTranslateX();
    }

    public static double getLocationY(final Node node) {
        return node.getLayoutY() + node.getTranslateY();
    }

    public static Node getPlotContent(final XYChart chart) {
        return chart.lookup(".plot-content");
    }

    public static double getVerticalInsets(final Insets insets) {
        return insets.getTop() + insets.getBottom();
    }

}
