package de.gsi.acc.remote;

import javafx.application.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchClipboard {
    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchClipboard.class);

    public static void main(final String[] args) throws ClassNotFoundException {
        LOGGER.atInfo().log("ideally start this with the following VM arguments: " //
                            + "-server -XX:G1HeapRegionSize=32M -Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=j2d,sw -Dprism.verbose=true");
        Application.launch(ClipboardSample.class);
    }
}
