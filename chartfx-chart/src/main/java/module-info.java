open module de.gsi.chartfx.chart {
    requires org.slf4j;

    requires com.ibm.icu;
    requires it.unimi.dsi.fastutil;

    requires java.desktop;
    requires java.management;
    requires jdk.management;

    requires javafx.controls;
    requires javafx.graphics;

    requires de.gsi.chartfx.dataset;
    requires de.gsi.chartfx.math;

    requires org.controlsfx.controls;

    requires javafxsvg;
    requires pngj;

    exports de.gsi.chart;
    exports de.gsi.chart.axes;
    exports de.gsi.chart.axes.spi;
    exports de.gsi.chart.axes.spi.format;
    exports de.gsi.chart.axes.spi.transforms;
    exports de.gsi.chart.legend;
    exports de.gsi.chart.legend.spi;
    exports de.gsi.chart.marker;
    exports de.gsi.chart.plugins;
    exports de.gsi.chart.plugins.measurements;
    exports de.gsi.chart.renderer;
    exports de.gsi.chart.renderer.datareduction;
    exports de.gsi.chart.renderer.spi;
    exports de.gsi.chart.renderer.spi.hexagon;
    exports de.gsi.chart.renderer.spi.marchingsquares;
    exports de.gsi.chart.renderer.spi.utils;
    exports de.gsi.chart.ui;
    exports de.gsi.chart.ui.css;
    exports de.gsi.chart.ui.geometry;
    exports de.gsi.chart.utils;
    exports de.gsi.chart.viewer;
    exports de.gsi.chart.viewer.event;
}
