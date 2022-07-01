package io.fair_acc.chartfx.axes.spi;

import javafx.scene.text.Text;

public class AxisLabel extends Text {

    public AxisLabel() {
        super();
        getStyleClass().add("axis-label");
        // make invisible: we only need it for style-sheet information
        setVisible(false);
        // this.setTranslateX(-10000);
        // this.setFill(Color.TRANSPARENT);
    }

}
