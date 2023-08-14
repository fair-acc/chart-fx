package io.fair_acc.chartfx.renderer.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetMetaData;
import io.fair_acc.dataset.utils.ProcessingProfiler;

public class MetaDataRenderer extends AbstractMetaDataRendererParameter<MetaDataRenderer> implements Renderer {
    protected BorderPane borderPane = new BorderPane();
    protected FlowPane messageBox = new FlowPane();
    protected HBox infoBox = new InfoHBox();
    protected HBox warningBox = new InfoHBox();
    protected HBox errorBox = new InfoHBox();
    protected final ObservableList<Axis> axesList = FXCollections.observableArrayList();
    protected Chart chart;

    protected List<String> oldInfoMessages;
    protected List<String> oldWarningMessages;
    protected List<String> oldErrorMessages;

    protected final BooleanProperty drawOnCanvas = new SimpleBooleanProperty(this, "drawOnCanvas", true) {
        boolean oldValue = true;

        @Override
        public void set(boolean newValue) {
            if (oldValue == newValue) {
                return;
            }
            super.set(newValue);
            oldValue = newValue;
            updateInfoBoxLocation();
        }
    };

    protected final ObjectProperty<Side> infoBoxSide = new SimpleObjectProperty<>(this, "infoBoxSide", Side.TOP) {
        Side oldSide = null;

        @Override
        public void set(final Side side) {
            if (side == null) {
                throw new InvalidParameterException("side must not be null");
            }

            if (oldSide != null && oldSide == side) {
                return;
            }
            super.set(side);
            oldSide = side;

            updateInfoBoxLocation();
        }
    };

    public MetaDataRenderer(final Chart chart) {
        super();
        this.chart = chart;
        updateCSS();
        messageBox.getChildren().addAll(errorBox, warningBox, infoBox);
        messageBox.setMouseTransparent(true);
        messageBox.setPrefWidth(1000);
        messageBox.setCache(true);
        // HBox.setHgrow(messageBox, Priority.SOMETIMES);
        // VBox.setVgrow(messageBox, Priority.SOMETIMES);

        chart.getCanvasForeground().getChildren().add(borderPane);
        final ChangeListener<Number> canvasChange = (ch, oldVal, newVal) -> borderPane.setPrefSize(chart.getCanvasForeground().getWidth(), chart.getCanvas().getHeight());

        chart.getCanvas().widthProperty().addListener(canvasChange);
        chart.getCanvas().heightProperty().addListener(canvasChange);

        setInfoBoxSide(Side.TOP); // NOPMD by rstein on 13/06/19 14:25
    }

    public BooleanProperty drawOnCanvasProperty() {
        return drawOnCanvas;
    }

    private List<String> extractMessages(List<DataSet> metaDataSets, boolean singleDS, MsgType msgType) {
        final List<String> list = new ArrayList<>();

        for (final DataSet dataSet : metaDataSets) {
            if (!(dataSet instanceof DataSetMetaData)) {
                continue;
            }
            final String dataSetName = dataSet.getName();
            final DataSetMetaData metaData = (DataSetMetaData) dataSet;

            List<String> msg;
            switch (msgType) {
            case ERROR:
                msg = metaData.getErrorList();
                break;
            case WARNING:
                msg = metaData.getWarningList();
                break;
            case INFO:
            default:
                msg = metaData.getInfoList();
                break;
            }

            for (final String info : msg) {
                if (singleDS) {
                    // just one applicable data set
                    list.add(info);
                } else {
                    // if duplicates, then add list with
                    // 'InfoMsg(DataSet::Name)'
                    list.add(info + " (" + dataSetName + ")");
                }
            }
        }

        return list;
    }

    @Override
    public ObservableList<Axis> getAxes() {
        return axesList;
    }

    public BorderPane getBorderPaneOnCanvas() {
        return borderPane;
    }

    @Override
    public ObservableList<DataSet> getDatasets() {
        return FXCollections.observableArrayList();
    }

    @Override
    public ObservableList<DataSet> getDatasetsCopy() {
        return FXCollections.observableArrayList();
    }

    @Override
    public ObservableList<DataSetNode> getDatasetNodes() {
        return FXCollections.emptyObservableList();
    }

    protected List<DataSet> getDataSetsWithMetaData(List<DataSet> dataSets) {
        final List<DataSet> list = new ArrayList<>();
        for (final DataSet dataSet : dataSets) {
            if (!(dataSet instanceof DataSetMetaData)) {
                continue;
            }
            list.add(dataSet);
        }

        return list;
    }

    /**
     *
     * @return box that is being filled with Error messages
     */
    public HBox getErrorBox() {
        return errorBox;
    }

    // ******************************* class specific properties **********

    /**
     *
     * @return box that is being filled with Info messages
     */
    public HBox getInfoBox() {
        return infoBox;
    }

    /**
     * whether renderer should draw info box in Side side, ...
     *
     * @return Side
     */
    public final Side getInfoBoxSide() {
        return infoBoxSideProperty().get();
    }

    /**
     *
     * @return FlowPane containing the Info-, Warning- and Error-Boxes
     */
    public FlowPane getMessageBox() {
        return messageBox;
    }

    @Override
    protected MetaDataRenderer getThis() {
        return this;
    }

    /**
     *
     * @return box that is being filled with Warning messages
     */
    public HBox getWarningBox() {
        return warningBox;
    }

    /**
     * whether renderer should draw info box in Side side, ...
     *
     * @return property
     */
    public final ObjectProperty<Side> infoBoxSideProperty() {
        return infoBoxSide;
    }

    public boolean isDrawOnCanvas() {
        return drawOnCanvas.get();
    }

    @Override
    public List<DataSet> render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        final long start = ProcessingProfiler.getTimeStamp();

        final ObservableList<DataSet> allDataSets = chart.getAllDatasets();
        final boolean singleDS = allDataSets.size() <= 1;
        final List<DataSet> metaDataSets = getDataSetsWithMetaData(allDataSets);

        final List<String> infoMessages = isShowInfoMessages() ? extractMessages(metaDataSets, singleDS, MsgType.INFO)
                                                               : new ArrayList<>();
        final List<String> warningMessages = isShowWarningMessages()
                                                     ? extractMessages(metaDataSets, singleDS, MsgType.WARNING)
                                                     : new ArrayList<>();
        final List<String> errorMessages = isShowErrorMessages()
                                                   ? extractMessages(metaDataSets, singleDS, MsgType.ERROR)
                                                   : new ArrayList<>();

        if (!infoMessages.equals(oldInfoMessages)) {
            oldInfoMessages = infoMessages;
            infoBox.getChildren().clear();
            if (!infoMessages.isEmpty()) {
                final VBox msgs = new VBox();
                infoBox.getChildren().addAll(iconInfo, msgs);

                for (final String text : infoMessages) {
                    final MetaLabel info = new MetaLabel(text);
                    msgs.getChildren().add(info);
                }
            }
        }

        if (!warningMessages.equals(oldWarningMessages)) {
            oldWarningMessages = warningMessages;
            warningBox.getChildren().clear();
            if (!warningMessages.isEmpty()) {
                final VBox msgs = new VBox();
                warningBox.getChildren().addAll(iconWarning, msgs);

                for (final String text : warningMessages) {
                    final MetaLabel info = new MetaLabel(text);
                    msgs.getChildren().add(info);
                }
            }
        }

        if (!errorMessages.equals(oldErrorMessages)) {
            oldErrorMessages = errorMessages;
            if (!errorMessages.isEmpty()) {
                final VBox msgs = new VBox();
                for (final String text : errorMessages) {
                    final MetaLabel info = new MetaLabel(text);
                    msgs.getChildren().add(info);
                }
                errorBox.getChildren().setAll(iconError, msgs);
            } else {
                errorBox.getChildren().clear();
            }
        }

        ProcessingProfiler.getTimeDiff(start);
        return Collections.emptyList();
    }

    public void setDrawOnCanvas(boolean state) {
        drawOnCanvas.set(state);
    }

    /**
     * whether renderer should draw info box in Side side, ...
     *
     * @param side the side to draw
     * @return itself (fluent design)
     */
    public final MetaDataRenderer setInfoBoxSide(final Side side) {
        infoBoxSideProperty().set(side);
        return getThis();
    }

    @Override
    public Renderer setShowInLegend(boolean state) {
        return getThis();
    }

    @Override
    public boolean showInLegend() {
        return false;
    }

    @Override
    public BooleanProperty showInLegendProperty() {
        return null;
    }

    protected void updateInfoBoxLocation() {
        final Side side = getInfoBoxSide();

        // remove old pane
        borderPane.getChildren().remove(messageBox);
        chart.getTitleLegendPane().getChildren().remove(messageBox);

        if (isDrawOnCanvas()) {
            switch (side) {
            case RIGHT:
                messageBox.setMaxWidth(300);
                messageBox.setPrefWidth(200);
                borderPane.setRight(messageBox);
                break;
            case LEFT:
                messageBox.setMaxWidth(300);
                messageBox.setPrefWidth(200);
                borderPane.setLeft(messageBox);
                break;
            case BOTTOM:
                messageBox.setPrefWidth(1000);
                messageBox.setMaxWidth(2000);
                borderPane.setBottom(messageBox);
                break;
            case TOP:
            default:
                messageBox.setMaxWidth(2000);
                messageBox.setPrefWidth(1000);
                borderPane.setTop(messageBox);
                break;
            }
        } else {
            chart.getTitleLegendPane().addSide(side, messageBox);
        }
        // chart.requestLayout();
    }

    static class InfoHBox extends HBox {
        public InfoHBox() {
            super();
            setMouseTransparent(true);

            // adjust size to 0 if there are no messages to show
            getChildren().addListener((ListChangeListener<Node>) ch -> {
                if (getChildren().isEmpty()) {
                    setMinWidth(0);
                    setSpacing(0);
                    setPadding(Insets.EMPTY);
                } else {
                    setPadding(new Insets(5, 5, 5, 5));
                    setMinWidth(200);
                    setSpacing(5);
                }
            });
        }
    }

    protected static class MetaLabel extends Label {
        public MetaLabel(final String text) {
            super(text);
            setMouseTransparent(true);
            setMinSize(100, 20);
            setCache(true);
        }
    }

    protected enum MsgType {
        INFO,
        WARNING,
        ERROR
    }
}
