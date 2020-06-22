package de.gsi.chart.renderer.spi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import de.gsi.chart.Chart;

public final class ChartIconFactory { // NOPMD - nomen est omen

    private static final String ICON_INFO = "I";
    private static final String ICON_WARN = "W";
    private static final String ICON_ERROR = "E";
    private static final Map<String, Color[]> colourMap = new HashMap<>();

    private static final int DEFAULT_HEIGHT = 32;
    public static Font iconFont;
    static {
        try {
            try (InputStream fontStream = Chart.class.getResourceAsStream("fonts/fair-chart-icons.ttf")) {
                iconFont = Font.loadFont(fontStream, DEFAULT_HEIGHT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        colourMap.put(ICON_INFO, new Color[] { Color.rgb(0, 0, 200), Color.WHITE });
        colourMap.put(ICON_WARN, new Color[] { Color.rgb(255, 215, 0), Color.BLACK });
        colourMap.put(ICON_ERROR, new Color[] { Color.rgb(237, 28, 36), Color.WHITE });
    }

    private ChartIconFactory() {
        // private constructor
    }
    public static Node getErrorIcon() {
        return ChartIconFactory.getIcon(ChartIconFactory.ICON_ERROR);
    }

    public static Node getErrorIcon(double size) {
        return getIcon(ChartIconFactory.ICON_ERROR, size);
    }

    private static Node getIcon(String iconString) {
        return getIcon(iconString, ChartIconFactory.DEFAULT_HEIGHT);
    }

    public static Node getIcon(String iconString, double size) {
        final Group group = new Group();
        final Text text1 = new Text(iconString);
        text1.setFont(Font.font("fair-chart-icons", size));
        text1.setFill(colourMap.get(iconString)[0]);

        final Text text2 = new Text(Character.toString(iconString.charAt(0) + 1));
        text2.setFont(Font.font("fair-chart-icons", size));
        text2.setFill(colourMap.get(iconString)[1]);

        group.getChildren().addAll(text1, text2);
        return group;
    }

    public static Node getInfoIcon() {
        return ChartIconFactory.getIcon(ChartIconFactory.ICON_INFO);
    }

    public static Node getInfoIcon(double size) {
        return getIcon(ChartIconFactory.ICON_INFO, size);
    }

    public static Node getWarningIcon() {
        return ChartIconFactory.getIcon(ChartIconFactory.ICON_WARN);
    }

    public static Node getWarningIcon(double size) {
        return getIcon(ChartIconFactory.ICON_WARN, size);
    }
}
