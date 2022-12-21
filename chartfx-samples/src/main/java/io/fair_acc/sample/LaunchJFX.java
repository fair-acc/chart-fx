package io.fair_acc.sample;

import javafx.application.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to launch JavaFX applications without manually adding modules and exports to the java command line.
 * Add a run configuration for this class in the arguments tab and add as program argument:
 * <ul>
 *     <li>for Eclipse IDE: "${java_type_name}"</li>
 *     <li>for Intellij IDE: "$FileClass$"</li>
 * </ul>
 *
 * Then, any JavaFX Application Class can be run by selecting it in the Package Explorer and running this run
 * configuration. <br>
 * To be able to run JavaFX Applications outside of the chartfx project also add a classpath entry of type "Advanced",
 * "Variable", "${project_classpath}", but there is no way to add the current projects dependencies to the classpath,
 * yet.
 *
 * @see <a href="https://stackoverflow.com/a/55300492" target="_top"> Stackoverflow: How to add JavaFX runtime to
 *      Eclipse in Java11 (2b) </a>
 * @author akrimm
 */
public class LaunchJFX { // NOMEN EST OMEN
    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchJFX.class);
    public static void main(final String[] args) throws ClassNotFoundException {
        if (args.length < 1 || args[0].contains(LaunchJFX.class.getName())) {
            LOGGER.atInfo().log("no argument provided");
        } else {
            try {
                Class<? extends Application> clazz = Class.forName(args[0]).asSubclass(Application.class);
                if (Application.class.isAssignableFrom(clazz)) {
                    Application.launch(clazz);
                } else {
                    LOGGER.atInfo().addArgument(clazz).log("{} is not an Application - starting default view");
                }
            } catch (ClassCastException e) {
                LOGGER.atInfo().addArgument(args[0]).log("{} is not an Application - starting default view");
            }
        }
    }
}
