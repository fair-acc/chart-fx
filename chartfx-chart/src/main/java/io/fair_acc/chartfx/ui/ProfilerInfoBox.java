package io.fair_acc.chartfx.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import org.controlsfx.control.BreadCrumbBar;
import org.kordamp.ikonli.javafx.FontIcon;

import io.fair_acc.chartfx.utils.SimplePerformanceMeter;

/**
 * Simple JavaFX and Chart Performance metrics indicator. To be added into e.g. a ToolBar
 * N.B. these are only indicative
 *
 * @author rstein
 */
public class ProfilerInfoBox extends BreadCrumbBar<VBox> {
    private static final int DEFAULT_DEBUG_UPDATE_RATE = 100;
    private static final double LEVEL_WARNING = 50.0; // FPS threshold to show orange chevron
    private static final double LEVEL_ERROR = 30.0; // FPS threshold to show red chevron
    private static final String FONT_MONO_SPACE = "Monospaced";
    private static final String FONT_AWESOME = "FontAwesome";
    private static final int FONT_SIZE = 9;
    private final FontIcon chevronIcon = new FontIcon("fa-thermometer-quarter:" + (2 * FONT_SIZE));
    private final ObjectProperty<DebugLevel> debugLevel = new SimpleObjectProperty<>(this, "debugLevel", DebugLevel.NONE);
    private final TreeItem<VBox> treeRoot;

    public ProfilerInfoBox() {
        this(DEFAULT_DEBUG_UPDATE_RATE);
    }

    /**
     * @param updateRateMillis static update rate in milli-seconds
     */
    public ProfilerInfoBox(final int updateRateMillis) {
        super();
        SimplePerformanceMeter meter = new SimplePerformanceMeter(updateRateMillis);
        // note: meter has to be added to a node which is visible  when the stats are shown, in this case the CPU crumb
        // adding it to this.getChildren() does not work, since it is overwritten by the bread crumb bar with the content

        setCrumbFactory((TreeItem<VBox> param) -> new CustomBreadCrumbButton(param.getValue()));
        setAutoNavigationEnabled(false);

        final Label chevron = new Label(null, chevronIcon);
        chevron.setPadding(new Insets(3, 0, 4, 0));
        VBox.setVgrow(chevron, Priority.ALWAYS);

        final CustomLabel fxFPS = new CustomLabel();
        fxFPS.setTooltip(new Tooltip("internal JavaFX tick frame-rate (aka. pulse, usually around 60 FPS)"));
        fxFPS.textProperty().bind(meter.fxFrameRateProperty().asString("FX %4.1f FPS"));
        final CustomLabel chartFPS = new CustomLabel();
        chartFPS.setTooltip(new Tooltip("(visible) frame update (usually <= 25 FPS"));
        chartFPS.textProperty().bind(meter.actualFrameRateProperty().asString("actual %4.1f FPS"));
        final CustomLabel cpuLoadProcess = new CustomLabel();
        cpuLoadProcess.setTooltip(new Tooltip("CPU load of this process"));
        cpuLoadProcess.textProperty().bind(meter.averageProcessCpuLoadProperty().asString("Process %5.1f %%"));
        final CustomLabel cpuLoadSystem = new CustomLabel();
        cpuLoadSystem.setTooltip(new Tooltip("CPU system load (100% <-> 1 core fully loaded)"));
        cpuLoadSystem.textProperty().bind(meter.systemCpuLoadProperty().asString("System %5.1f %%"));
        meter.fxFrameRateProperty().addListener((b, o, n) -> {
            if (n.doubleValue() < LEVEL_ERROR) {
                chevronIcon.setFill(Color.RED);
            } else if (n.doubleValue() < LEVEL_WARNING) {
                chevronIcon.setFill(Color.DARKORANGE);
            } else {
                chevronIcon.setFill(Color.BLACK);
            }
        });

        final Label javaVersion = new CustomLabel(System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        final Label javafxVersion = new CustomLabel("JavaFX: " + System.getProperty("javafx.runtime.version") /*+ " Chart-fx: " + System.getProperty("chartfx.version")*/);
        // TODO: add Chart-fx version (commit ID, release version)

        treeRoot = new TreeItem<>(new VBox(chevron));
        treeRoot.getValue().setId("ProfilerInfoBox-treeRoot");
        final TreeItem<VBox> fpsItem = new TreeItem<>(new VBox(fxFPS, chartFPS));
        fpsItem.getValue().setId("ProfilerInfoBox-fpsItem");
        final TreeItem<VBox> cpuItem = new TreeItem<>(new VBox(cpuLoadProcess, cpuLoadSystem, meter));
        cpuItem.getValue().setId("ProfilerInfoBox-cpuItem");
        final TreeItem<VBox> versionItem = new TreeItem<>(new VBox(javaVersion, javafxVersion));
        versionItem.getValue().setId("ProfilerInfoBox-versionItem");
        treeRoot.getChildren().add(fpsItem);
        fpsItem.getChildren().add(cpuItem);
        cpuItem.getChildren().add(versionItem);

        setOnCrumbAction(updateSelectedCrumbActionListener());
        debugLevelProperty().addListener(updateSelectedCrumbLevelListener(fpsItem, cpuItem, versionItem));

        setSelectedCrumb(treeRoot);
    }

    public ObjectProperty<DebugLevel> debugLevelProperty() {
        return debugLevel;
    }

    public DebugLevel getDebugLevel() {
        return debugLevelProperty().get();
    }

    public TreeItem<VBox> getTreeRoot() {
        return treeRoot;
    }

    public ProfilerInfoBox setDebugLevel(final DebugLevel level) {
        debugLevelProperty().set(level);
        return this;
    }

    private EventHandler<BreadCrumbActionEvent<VBox>> updateSelectedCrumbActionListener() {
        return bae -> {
            if (bae.getSelectedCrumb().equals(getSelectedCrumb()) && !bae.getSelectedCrumb().getChildren().isEmpty()) {
                setSelectedCrumb(bae.getSelectedCrumb().getChildren().get(0));
            } else {
                setSelectedCrumb(bae.getSelectedCrumb());
            }
        };
    }

    private ChangeListener<? super DebugLevel> updateSelectedCrumbLevelListener(final TreeItem<VBox> fpsItem, final TreeItem<VBox> cpuItem, final TreeItem<VBox> versionItem) {
        return (ch, o, n) -> {
            switch (n) {
            case FRAMES_PER_SECOND:
                setSelectedCrumb(fpsItem);
                break;
            case CPU_LOAD:
                setSelectedCrumb(cpuItem);
                break;
            case VERSION:
                setSelectedCrumb(versionItem);
                break;
            case NONE:
            default:
                setSelectedCrumb(treeRoot);
                break;
            }
        };
    }

    public enum DebugLevel {
        NONE,
        FRAMES_PER_SECOND,
        CPU_LOAD,
        VERSION
    }

    protected static class CustomBreadCrumbButton extends BreadCrumbButton {
        public CustomBreadCrumbButton(Node gfx) {
            super(null, gfx);
            setId(gfx.getId());
            setPadding(new Insets(0, getArrowWidth(), 0, getArrowWidth()));
        }
    }

    protected static class CustomLabel extends Label {
        public CustomLabel() {
            this(null);
        }

        public CustomLabel(final String text) {
            super(text);
            setPadding(Insets.EMPTY);
            setFont(Font.font(FONT_MONO_SPACE, FONT_SIZE));
        }
    }
}
