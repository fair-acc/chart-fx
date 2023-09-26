package io.fair_acc.chartfx.plugins;

import java.util.Optional;
import java.util.function.UnaryOperator;

import io.fair_acc.chartfx.Chart;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;

import io.fair_acc.bench.BenchLevel;
import io.fair_acc.bench.MeasurementRecorder;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.bench.LiveDisplayRecorder;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.utils.AssertUtils;

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
    protected final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final HBox buttons = createButtonBar();
    private UnaryOperator<MeasurementRecorder> measurementFilter = rec -> rec.atLevel(BenchLevel.Info).contains("draw");
    protected final Stage stage = new Stage();

    protected HBox createButtonBar() {
        final Button enableBtn = new Button(null, new FontIcon(ICON_ENABLE_BENCH));
        enableBtn.setPadding(new Insets(3, 3, 3, 3));
        enableBtn.setTooltip(new Tooltip("starts displaying live benchmark stats"));
        enableBtn.disableProperty().bind(chartProperty().isNull());
        final Button disableBtn = new Button(null, new FontIcon(ICON_DISABLE_BENCH));
        disableBtn.setPadding(new Insets(3, 3, 3, 3));
        disableBtn.setTooltip(new Tooltip("stops displaying live benchmark stats"));

        FXUtils.bindManagedToVisible(enableBtn).bind(enabled.not());
        enableBtn.setOnAction(evt -> enable());
        FXUtils.bindManagedToVisible(disableBtn).bind(enabled);
        disableBtn.setOnAction(evt -> disable());

        final HBox buttonBar = new HBox(enableBtn, disableBtn);
        buttonBar.setPadding(new Insets(1, 1, 1, 1));
        return buttonBar;
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
            if (n != null && isAddButtonsToToolBar()) {
                n.getToolBar().getChildren().add(buttons);
            }
        });
        stage.showingProperty().addListener((observable, oldValue, showing) -> {
            if (!showing) {
                disable();
            }
        });
    }

    public BenchPlugin setFilter(UnaryOperator<MeasurementRecorder> measurementFilter) {
        this.measurementFilter = AssertUtils.notNull("MeasurementFilter", measurementFilter);
        return this;
    }

    public void enable() {
        if (!enabled.get() && getChart() != null && getChart() instanceof XYChart) {
            XYChart chart = (XYChart) getChart();
            MeasurementRecorder recorder = createRecorder(chart);
            chart.setGlobalRecorder(measurementFilter.apply(recorder));
            resetRecorder = () -> chart.setGlobalRecorder(MeasurementRecorder.DISABLED);
            enabled.set(true);
        }
    }

    protected MeasurementRecorder createRecorder(XYChart chart) {
        String title = Optional.ofNullable(chart.getTitle())
                .filter(string -> !string.isEmpty())
                .orElse("Benchmark");
        return LiveDisplayRecorder.createChart(title, pane -> {
            stage.setScene(new Scene(pane));
            stage.show();
        });
    }

    public void disable() {
        if (enabled.get()) {
            enabled.set(false);
            resetRecorder.run();
            resetRecorder = NO_OP;
            stage.hide();
        }
    }

    private static final Runnable NO_OP = () -> {
    };
    private Runnable resetRecorder = NO_OP;
}
