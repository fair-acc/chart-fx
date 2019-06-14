package de.gsi.chart.renderer.spi.utils;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import de.gsi.chart.XYChart;
import javafx.scene.image.Image;

public final class ChartIconFactory { // NOPMD

    private static final String ICON_INFO = XYChart.class.getResource("icons/info_icon.svg").toString();
    private static final String ICON_WARN = XYChart.class.getResource("icons/warn_icon.svg").toString();
    private static final String ICON_ERROR = XYChart.class.getResource("icons/error_icon.svg").toString();

    private static final int MIN_WIDTH = 10;
    private static final int MIN_HEIGHT = 10;
    private static final int DEFAULT_WIDTH = 32;
    private static final int DEFAULT_HEIGHT = 32;
    private static final int MAX_WIDTH = 100;
    private static final int MAX_HEIGHT = 100;

    static {
        SvgImageLoaderFactory.install();
        // SvgImageLoaderFactory.install(new PrimitiveDimensionProvider());
    }

    private ChartIconFactory() {
        // private constructor
    }

    public static Image getInfoIcon() {
        return ChartIconFactory.getIcon(ChartIconFactory.ICON_INFO);
    }

    public static Image getInfoIcon(double width, double height) {
        return new Image(ChartIconFactory.ICON_INFO, width, height, true, false);
    }

    public static Image getWarningIcon() {
        return ChartIconFactory.getIcon(ChartIconFactory.ICON_WARN);
    }

    public static Image getWarningIcon(double width, double height) {
        return new Image(ChartIconFactory.ICON_WARN, width, height, true, false);
    }

    public static Image getErrorIcon() {
        return ChartIconFactory.getIcon(ChartIconFactory.ICON_ERROR);
    }

    public static Image getErrorIcon(double width, double height) {
        return new Image(ChartIconFactory.ICON_ERROR, width, height, true, false);
    }

    private static Image getIcon(String file) {
        return new Image(file, ChartIconFactory.DEFAULT_WIDTH, ChartIconFactory.DEFAULT_HEIGHT, true, false);
    }

    public static Image getIcon(String file, double width, double height) {
        final double w = Math.min(Math.max(width, ChartIconFactory.MIN_WIDTH), ChartIconFactory.MAX_WIDTH);
        final double h = Math.min(Math.max(height, ChartIconFactory.MIN_HEIGHT), ChartIconFactory.MAX_HEIGHT);
        return new Image(file, w, h, true, false);
    }

}
