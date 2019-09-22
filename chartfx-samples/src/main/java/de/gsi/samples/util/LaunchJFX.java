package de.gsi.samples.util;

import de.gsi.chart.samples.RunChartSamples;
import javafx.application.Application;
import javafx.application.Platform;

/**
 * Helper class to launch JavaFX applications without manually adding modules and exports
 * to the java command line.
 * Add a run configuration for this class in eclipse and in the arguments tab, add "${java_type_name}" as program
 * argument. Then, any JavaFX Application Class can be run by selecting it in the Package Explorer and running this
 * run configuration. <br>
 * To be able to run JavaFX Applications outside of the chartfx project also add a classpath entry of type "Advanced",
 * "Variable", "${project_classpat}", but there is no way to add the current projects dependencies to the classpath,
 * yet.
 * 
 * @see <a href="https://stackoverflow.com/a/55300492" target="_top">
 *      Stackoverflow: How to add JavaFX runtime to Eclipse in Java11 (2b) </a>
 * @author akrimm
 */
public class LaunchJFX {
    public static void main(final String[] args) throws ClassNotFoundException {
        if (args.length < 1) {
            Application.launch(RunChartSamples.class);
        } else {
            Application.launch(Class.forName(args[0]).asSubclass(Application.class));
        }
    }
}
