package de.gsi.samples.util;

import de.gsi.chart.samples.RunChartSamples;
import javafx.application.Application;

// for library loggers
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

// for application loggers
//import de.gsi.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 *
 * @author akrimm
 */
public class LaunchJFX {

    // You can choose a logger (needed imports are given in the import section as comments):
    // for libraries:
    // private static final Logger LOGGER = LoggerFactory.getLogger(TestFX11.class);
    // for swing applications:
    // private static final AppLogger LOGGER = AppLogger.getLogger();
    // for fx applications:
    // private static final AppLogger LOGGER = AppLogger.getFxLogger();
    public static void main(final String[] args) throws ClassNotFoundException {
        //Application.launch(RunChartSamples.class);
        Application.launch(Class.forName(args[0]).asSubclass(Application.class));
    }
}

