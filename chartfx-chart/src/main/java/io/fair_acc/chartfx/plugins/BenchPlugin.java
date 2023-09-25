package io.fair_acc.chartfx.plugins;

import io.fair_acc.bench.BenchLevel;
import io.fair_acc.bench.MeasurementRecorder;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.bench.LiveDisplayRecorder;
import io.fair_acc.chartfx.bench.MeasurementRecorders;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.utils.AssertUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;
import java.util.function.Function;

/**
 * Experimental plugin that measures and displays the internal
 * performance of the rendered chart.
 *
 * @author ennerf
 */
public class BenchPlugin extends ChartPlugin {

    private static final int FONT_SIZE = 22;
    private static final String ICON_ENABLE_BENCH = "fa-hourglass-start:" + FONT_SIZE;
    private static final String ICON_DISABLE_BENCH = "fa-hourglass-end:" + FONT_SIZE;
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final HBox buttons = getButtonBar();
    private Function<MeasurementRecorder, MeasurementRecorder> filterFunc = rec -> rec
            .atLevel(BenchLevel.Info)
            .contains("draw");

    public HBox getButtonBar() {
        final Button enableBench = new Button(null, new FontIcon(ICON_ENABLE_BENCH));
        enableBench.setPadding(new Insets(3, 3, 3, 3));
        enableBench.setTooltip(new Tooltip("displays live benchmark chart"));
        final Button disableBench = new Button(null, new FontIcon(ICON_DISABLE_BENCH));
        disableBench.setPadding(new Insets(3, 3, 3, 3));
        disableBench.setTooltip(new Tooltip("stops live benchmarks"));

        FXUtils.managedVisibilityProperty(enableBench).bind(enabled.not());
        enableBench.setOnAction(this::enable);
        FXUtils.managedVisibilityProperty(disableBench).bind(enabled);
        disableBench.setOnAction(this::disable);

        final HBox buttons = new HBox(enableBench, disableBench);
        buttons.setPadding(new Insets(1, 1, 1, 1));
        return buttons;
    }

    public BenchPlugin() {
        chartProperty().addListener((obs, o, n) -> {
            if (o != null) {
                o.getToolBar().getChildren().remove(buttons);
                if (o instanceof XYChart) {
                    ((XYChart) o).setGlobalRecorder(MeasurementRecorder.DISABLED);
                }
                enabled.set(false);
            }
            if (n != null && isAddButtonsToToolBar()){
                n.getToolBar().getChildren().add(buttons);
            }
        });
    }

    public BenchPlugin setFilter(Function<MeasurementRecorder, MeasurementRecorder> filterFunc) {
        this.filterFunc = AssertUtils.notNull("Filter", filterFunc);
        return this;
    }

    private void enable(ActionEvent event) {
        if (!enabled.get() && getChart() != null && getChart() instanceof XYChart) {
            XYChart chart = (XYChart) getChart();
            String title = Optional.ofNullable(chart.getTitle())
                    .filter(string->!string.isEmpty())
                    .orElse("Benchmark");
            LiveDisplayRecorder recorder = LiveDisplayRecorder.createChart(title, pane -> {

                Scene scene = new Scene(pane);
                scene.getStylesheets().addAll(chart.getScene().getStylesheets());

                Stage stage = new Stage();
                stage.initOwner(chart.getScene().getWindow());
                stage.setScene(scene);
                stage.showingProperty().addListener((observable, oldValue, showing) -> {
                    if (!showing) {
                        chart.setGlobalRecorder(MeasurementRecorder.DISABLED);
                        disable(event);
                    }
                });
                stage.show();

            });
            chart.setGlobalRecorder(filterFunc.apply(recorder));
            enabled.set(true);
        }
    }

    private void disable(ActionEvent event) {
        if (enabled.get() && getChart() != null && getChart() instanceof XYChart) {
            XYChart chart = (XYChart) getChart();
            chart.setGlobalRecorder(MeasurementRecorder.DISABLED);
            enabled.set(false);
        }
    }

}
