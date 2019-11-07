package de.gsi.chart.samples;

import de.gsi.chart.utils.SimplePerformanceMeter;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Simple JavaFX and Chart Performance metrics N.B. these are only indicative
 * 
 * @author rstein
 */
public class ProfilerInfoBox extends HBox {
    public static final int DEFAULT_DEBUG_UPDATE_RATE = 500;
    public static final String FONT_MONO_SPACE = "Monospaced";

    /**
     * @param scene superordinate scene
     */
    public ProfilerInfoBox(Scene scene) {
        this(scene, DEFAULT_DEBUG_UPDATE_RATE);
    }

    /**
     * @param scene superordinate scene
     * @param updateRateMillis static update rate in milli-seconds
     */
    public ProfilerInfoBox(Scene scene, final int updateRateMillis) {
        super();
        SimplePerformanceMeter meter = new SimplePerformanceMeter(scene, updateRateMillis);

        Label javaVersion = new Label();
        javaVersion.setFont(Font.font(FONT_MONO_SPACE, 12));
        javaVersion
                .setText("JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " ");

        Label javafxVersion = new Label();
        javafxVersion.setFont(Font.font(FONT_MONO_SPACE, 12));
        javafxVersion.setText("JavaFX: " + System.getProperty("javafx.runtime.version") + " ");

        Label fxFPS = new Label();
        fxFPS.setFont(Font.font(FONT_MONO_SPACE, 12));
        Label chartFPS = new Label();
        chartFPS.setFont(Font.font(FONT_MONO_SPACE, 12));
        Label cpuLoadProcess = new Label();
        cpuLoadProcess.setFont(Font.font(FONT_MONO_SPACE, 12));
        Label cpuLoadSystem = new Label();
        cpuLoadSystem.setFont(Font.font(FONT_MONO_SPACE, 12));
        meter.fxFrameRateProperty().addListener((ch, o, n) -> {
            final String fxRate = String.format("%4.1f", meter.getFxFrameRate());
            final String actualRate = String.format("%4.1f", meter.getActualFrameRate());
            final String cpuProcess = String.format("%5.1f", meter.getProcessCpuLoad());
            final String cpuSystem = String.format("%5.1f", meter.getSystemCpuLoad());
            fxFPS.setText(String.format("%-6s: %4s %s", "JavaFX", fxRate, "FPS, "));
            chartFPS.setText(String.format("%-6s: %4s %s", "Actual", actualRate, "FPS, "));
            cpuLoadProcess.setText(String.format("%-11s: %4s %s", "Process-CPU", cpuProcess, "%"));
            cpuLoadSystem.setText(String.format("%-11s: %4s %s", "System -CPU", cpuSystem, "%"));
        });

        this.getChildren().addAll(new VBox(javaVersion, javafxVersion), new VBox(fxFPS, chartFPS),
                new VBox(cpuLoadProcess, cpuLoadSystem));
    }
}
