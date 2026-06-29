package io.fair_acc.chartfx.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.testdata.spi.CosineFunction;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class XValueIndicatorTest {

    private XYChart chart;
    private XValueIndicator indicator;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();
        indicator = new XValueIndicator(chart.getXAxis(), 5, "POI");
        indicator.setLabelPosition(0.5);
        chart.getPlugins().add(indicator);
        chart.setPrefWidth(400);
        chart.setPrefHeight(300);

        DataSet ds = new CosineFunction("Cosine", 30);
        chart.getDatasets().add(ds);

        Scene scene = new Scene(chart, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testThatIndicatorEditLabelTextFieldIsShownWhenEditable(final FxRobot fxRobot) { // NOPMD JUnitTestsShouldIncludeAssert
        fxRobot.interrupt();

        assertThat(lookupIndicatorTextFields(fxRobot), hasSize(0));
        fxRobot.moveTo(lookupFirstIndicatorLabel(fxRobot));
        fxRobot.clickOn(MouseButton.SECONDARY);

        assertThat(lookupIndicatorTextFields(fxRobot), hasSize(1));
    }

    @Test
    void testThatIndicatorNoEditLabelTextFieldIsShownWhenNotEditable(final FxRobot fxRobot) { // NOPMD JUnitTestsShouldIncludeAssert
        fxRobot.interrupt();

        indicator.setEditable(false);

        assertThat(lookupIndicatorTextFields(fxRobot), hasSize(0));
        fxRobot.moveTo(lookupFirstIndicatorLabel(fxRobot));
        fxRobot.clickOn(MouseButton.SECONDARY);

        assertThat(lookupIndicatorTextFields(fxRobot), hasSize(0));
    }

    private Node lookupFirstIndicatorLabel(final FxRobot fxRobot) {
        return fxRobot.from(chart).lookup("." + AbstractSingleValueIndicator.STYLE_CLASS_LABEL).query();
    }

    private Set<Node> lookupIndicatorTextFields(final FxRobot fxRobot) {
        return fxRobot.from(chart).lookup(".text-field").queryAll();
    }

}
