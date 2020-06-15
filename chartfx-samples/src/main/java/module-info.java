module de.gsi.chartfx.samples {
    requires org.slf4j;

    requires java.desktop;

    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;

    requires de.gsi.chartfx.chart;
    requires de.gsi.chartfx.math;
    requires de.gsi.chartfx.dataset;
    requires de.gsi.chartfx.acc;

    requires org.controlsfx.controls;

    requires jafama;
    requires JTransforms;

    opens de.gsi.chart.samples to javafx.graphics;
    opens de.gsi.chart.samples.legacy to javafx.graphics;
    opens de.gsi.acc.ui.samples to javafx.graphics;
    opens de.gsi.math.samples to javafx.graphics;

    exports de.gsi.chart.samples;
    exports de.gsi.math.samples;
    exports de.gsi.dataset.samples;
    exports de.gsi.samples.util;
}