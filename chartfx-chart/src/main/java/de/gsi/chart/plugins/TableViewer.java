package de.gsi.chart.plugins;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;

/**
 * Displays the all visible data sets inside a table on demand. Implements copy-paste functionality into system
 * clip-board and *.csv file export to allow further processing in other applications. Also enables editing of values if
 * the underlying DataSet allows it.
 * 
 * @author rstein
 * @author akrimm
 */
public class TableViewer extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableViewer.class);
    // prevent allocating an infinite number of columns in case something goes wrong
    private static final int MAX_DATASETS_IN_TABLE = 100;
    protected static final int FONT_SIZE = 22;
    /* default */ static final String BUTTON_SAVE_TABLE_VIEW_STYLE_CLASS = "save-table-view";
    /* default */ static final String BUTTON_COPY_TO_CLIPBOARD_STYLE_CLASS = "copy-to-clip-board";
    /* default */ static final String BUTTON_SWITCH_TABLE_VIEW_STYLE_CLASS = "switch-table-view";
    /* default */ static final String BUTTON_BAR_STYLE_CLASS = "table-viewer-button-bar";

    protected static final int MIN_REFRESH_RATE_WARN = 20; // [ms] warn if refresh rate is set lower than this value
    private final FontIcon tableView = new FontIcon("fa-table:" + FONT_SIZE);
    private final FontIcon graphView = new FontIcon("fa-line-chart:" + FONT_SIZE);
    private final FontIcon saveIcon = new FontIcon("fa-save:" + FONT_SIZE);
    private final FontIcon clipBoardIcon = new FontIcon("far-clipboard:" + FONT_SIZE);
    private final HBox interactorButtons = getInteractorBar();
    private final TableView<DataSetsRow> table = new TableView<>();
    private final DataSetsModel dsModel = new DataSetsModel();
    protected boolean editable;
    private final Timer timer = new Timer("TableViewer-update-task", true);
    private final IntegerProperty refreshRate = new SimpleIntegerProperty(this, "refreshRate", 1000) {
        @Override
        public void set(int newValue) {
            if (newValue < 0) {
                throw new IllegalArgumentException("refresh rate must be positive");
            }
            if (newValue < MIN_REFRESH_RATE_WARN) {
                LOGGER.atWarn().addArgument(newValue).addArgument(MIN_REFRESH_RATE_WARN).log("New refresh rate ({}ms) lower than recommended minimun ({}ms)");
            }
            super.set(newValue);
        }
    };

    /**
     * Creates a new instance of DataSetTableViewer class and setup the required listeners.
     */
    public TableViewer() {
        super();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setEditable(true); // Generally the TableView is editable, actual editability is configured column-wise
        table.setItems(dsModel);
        Bindings.bindContent(table.getColumns(), dsModel.getColumns());

        chartProperty().addListener((change, oldChart, newChart) -> {
            if (oldChart != null) {
                // plugin has already been initialised for old chart
                oldChart.getToolBar().getChildren().remove(interactorButtons);
                oldChart.getPlotForeground().getChildren().remove(table);
                oldChart.getPlotArea().setBottom(null);
                table.prefWidthProperty().unbind();
                table.prefHeightProperty().unbind();
            }
            if (newChart != null) {
                if (isAddButtonsToToolBar()) {
                    newChart.getToolBar().getChildren().add(interactorButtons);
                }
            }
            dsModel.chartChanged(oldChart, newChart);
        });

        addButtonsToToolBarProperty().addListener((ch, o, n) -> {
            final Chart chartLocal = getChart();
            if (chartLocal == null || o.equals(n)) {
                return;
            }
            if (Boolean.TRUE.equals(n)) {
                chartLocal.getToolBar().getChildren().add(interactorButtons);
            } else {
                chartLocal.getToolBar().getChildren().remove(interactorButtons);
            }
        });
    }

    /**
     * The refresh Rate limits minimum amount of time between table updates in milliseconds and defaults to 100ms.
     * Setting this below 20ms is discouraged and will produce warnings.
     * 
     * @return The refreshRate property
     */
    public IntegerProperty refreshRateProperty() {
        return refreshRate;
    }

    /**
     * gets {@link #refreshRateProperty()}
     * @return the value of the refreshRate Property
     */
    public int getRefreshRate() {
        return refreshRate.get();
    }

    /**
     * sets {@link #refreshRateProperty()}
     * @param newVal the new value for the refreshRate Property
     */
    public void setRefreshRate(final int newVal) {
        refreshRate.set(newVal);
    }

    /**
     * Copies the (selected) table data to the clipboard in csv Format.
     */
    public void copySelectedToClipboard() {
        final ClipboardContent content = new ClipboardContent();
        content.putString(dsModel.getSelectedData(table.getSelectionModel()));
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Show a FileChooser and export the (selected) Table Data to the choosen .csv File.
     */
    public void exportGridToCSV() {
        final FileChooser chooser = new FileChooser();
        final File save = chooser.showSaveDialog(getChart().getScene().getWindow());
        if (save == null) {
            return;
        }
        final String data = dsModel.getSelectedData(table.getSelectionModel());
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(save.getPath() + ".csv"),
                     StandardCharsets.UTF_8)) {
            writer.write(data);
        } catch (IOException ex) {
            LOGGER.atError().setCause(ex).log("error while exporting data to csv");
        }
    }

    /**
     * Helper function to initialize the UI elements for the Interactor toolbar.
     * 
     * @return HBox node with the toolbar elements
     */
    protected HBox getInteractorBar() {
        final Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        final HBox buttonBar = new HBox();
        buttonBar.getStyleClass().add(BUTTON_BAR_STYLE_CLASS);
        buttonBar.setPadding(new Insets(1, 1, 1, 1));
        final Button switchTableView = new Button("", tableView);
        switchTableView.getStyleClass().add(BUTTON_SWITCH_TABLE_VIEW_STYLE_CLASS);
        switchTableView.setPadding(new Insets(3, 3, 3, 3));
        switchTableView.setTooltip(new Tooltip("switches between graph and table view"));

        final Button copyToClipBoard = new Button("", clipBoardIcon);
        copyToClipBoard.getStyleClass().add(BUTTON_COPY_TO_CLIPBOARD_STYLE_CLASS);
        copyToClipBoard.setPadding(new Insets(3, 3, 3, 3));
        copyToClipBoard.setTooltip(new Tooltip("copy selected content top system clipboard"));
        copyToClipBoard.setOnAction(e -> this.copySelectedToClipboard());

        final Button saveTableView = new Button("", saveIcon);
        saveTableView.getStyleClass().add(BUTTON_SAVE_TABLE_VIEW_STYLE_CLASS);
        saveTableView.setPadding(new Insets(3, 3, 3, 3));
        saveTableView.setTooltip(new Tooltip("store actively shown content as .csv file"));
        saveTableView.setOnAction(e -> this.exportGridToCSV());

        switchTableView.setOnAction(evt -> {
            final ObservableList<Node> plotForegroundChildren = getChart().getPlotForeground().getChildren();
            final boolean isTablePresent = plotForegroundChildren.contains(table);
            if (isTablePresent) {
                plotForegroundChildren.remove(table);
                table.prefWidthProperty().unbind();
                table.prefHeightProperty().unbind();
            } else {
                plotForegroundChildren.add(table);
                table.toFront();
                table.prefWidthProperty().bind(getChart().getPlotForeground().widthProperty());
                table.prefHeightProperty().bind(getChart().getPlotForeground().heightProperty());
            }

            switchTableView.setGraphic(isTablePresent ? tableView : graphView);
            getChart().getPlotForeground().setMouseTransparent(!isTablePresent);
            table.setMouseTransparent(!isTablePresent);
            dsModel.datasetsChanged(null);
        });

        buttonBar.getChildren().addAll(separator, switchTableView, copyToClipBoard, saveTableView);
        return buttonBar;
    }

    /**
     * @return The TableView JavaFX control element
     */
    public TableView<?> getTable() {
        return table;
    }

    protected enum ColumnType {
        X(DIM_X, "x", false, true),
        Y(DIM_Y, "y", false, true),
        EXN(DIM_X, "e_x", true, false),
        EXP(DIM_X, "e_x", true, true),
        EYN(DIM_Y, "e_y", true, false),
        EYP(DIM_Y, "e_y", true, true);

        int dimIdx;
        String label;
        boolean errorCol;
        boolean positive;

        ColumnType(final int dimIdx, final String label, final boolean errorCol, final boolean positive) {
            this.dimIdx = dimIdx;
            this.label = label;
            this.errorCol = errorCol;
            this.positive = positive;
        }
    }

    /**
     * Model Abstraction to the DataSets of a chart as the backing for a JavaFX TableView. Only elements visible on
     * screen are allocated and new elements are generated onDemand using Cell Factories. Also generates the column
     * Objects for the TableView and subscribes Change Listeners to update the Table whenever the datasets change or new
     * Datasets are added
     * 
     * @author akrimm
     */
    protected class DataSetsModel extends ObservableListBase<DataSetsRow> {
        protected static final double DEFAULT_COL_WIDTH = 150;
        private int nRows;
        private final ObservableList<TableColumn<DataSetsRow, ?>> columns = FXCollections.observableArrayList();

        private long lastColumnUpdate = 0;
        private final AtomicBoolean columnUpdateScheduled = new AtomicBoolean(false);

        private final ListChangeListener<Renderer> rendererChangeListener = this::rendererChanged;
        private final InvalidationListener datasetChangeListener = this::datasetsChanged;
        private final EventListener dataSetDataUpdateListener = (UpdateEvent evt) -> FXUtils.runFX(() -> this.datasetsChanged(null));
        private TimerTask timerTask;

        public DataSetsModel() {
            super();
            columns.add(new RowIndexHeaderTableColumn());
            table.visibleProperty().addListener((prop, oldVal, newVal) -> {
                if (Boolean.TRUE.equals(newVal)) {
                    datasetsChanged(null);
                }
            });
        }

        @Override
        public String toString() {
            return "TableModel";
        }

        public void datasetsChanged(@SuppressWarnings("unused") Observable obs) { // unused parameter is needed for listener interface
            if (getChart() == null) { // the plugin was removed from the chart
                return;
            }
            if (!table.isVisible()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastColumnUpdate > refreshRate.get()) {
                List<DataSet> columnsUpdated = getChart().getAllDatasets().stream().sorted(Comparator.comparing(DataSet::getName)).collect(Collectors.toList());
                int nRowsNew = 0;
                for (int i = 0; i < columns.size() - 1 || i < columnsUpdated.size(); i++) {
                    if (i > MAX_DATASETS_IN_TABLE) {
                        LOGGER.atWarn().addArgument(columnsUpdated.size()).log("Limiting number of DataSets shown in Table, chart has {} DataSets.");
                        break;
                    }
                    if (i < columnsUpdated.size()) {
                        if (i >= columns.size() - 1) {
                            columns.add(new DataSetTableColumns());
                        }
                        DataSet ds = columnsUpdated.get(i);
                        ds.removeListener(dataSetDataUpdateListener);
                        ds.addListener(dataSetDataUpdateListener);
                        ((DataSetTableColumns) columns.get(i + 1)).update(ds);
                        nRowsNew = Math.max(nRowsNew, ds.getDataCount());
                    } else {
                        ((DataSetTableColumns) columns.get(i + 1)).update(null);
                    }
                }
                lastColumnUpdate = now;
                if (nRows != nRowsNew) {
                    // Workaround, let the selection model realize, that the number of cols has changed
                    // in the process the selection is lost
                    nRows = nRowsNew;
                    table.setItems(null);
                    table.setItems(dsModel);
                } else {
                    table.refresh();
                }
            } else {
                if (columnUpdateScheduled.compareAndExchange(false, true)) {
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            columnUpdateScheduled.set(false);
                            FXUtils.runFX(() -> datasetsChanged(null));
                        }
                    };
                    timer.schedule(timerTask, refreshRate.get());
                }
            }
        }

        /**
         * @param oldChart The old chart the plugin is operating on
         * @param newChart The new chart the plugin is operating on
         */
        public void chartChanged(final Chart oldChart, final Chart newChart) {
            if (oldChart != null) {
                if (timerTask != null) {
                    timerTask.cancel();
                }
                // de-register data set listeners
                oldChart.getDatasets().removeListener(datasetChangeListener);
                oldChart.getDatasets().forEach(dataSet -> dataSet.removeListener(dataSetDataUpdateListener));
                oldChart.getRenderers().removeListener(rendererChangeListener);
                if (newChart != null) {
                    newChart.getRenderers()
                            .forEach(renderer -> renderer.getDatasets().removeListener(datasetChangeListener));
                }
            }
            if (newChart != null) {
                // register data set listeners
                newChart.getDatasets().addListener(datasetChangeListener);
                newChart.getDatasets().forEach(dataSet -> dataSet.addListener(dataSetDataUpdateListener));
                newChart.getRenderers().addListener(rendererChangeListener);
                newChart.getRenderers().forEach(renderer -> renderer.getDatasets().addListener(datasetChangeListener));
                datasetsChanged(null);
            }
        }

        @Override
        public boolean contains(final Object o) {
            if (o instanceof DataSetsRow) {
                return (nRows > ((DataSetsRow) o).getRow());
            }
            return false;
        }

        @Override
        public DataSetsRow get(final int row) {
            return new DataSetsRow(row, this);
        }

        protected String getAllData() {
            final StringBuilder sb = new StringBuilder();
            sb.append('#');
            int dataSetNo = 0;
            for (TableColumn<DataSetsRow, ?> col : columns) {
                if (col instanceof DataSetTableColumns && col.isVisible()) {
                    dataSetNo++;
                    for (TableColumn<DataSetsRow, ?> subcol : col.getColumns()) {
                        if (subcol instanceof DataSetTableColumn && ((DataSetTableColumn) subcol).active) {
                            sb.append(subcol.getText()).append(dataSetNo).append(", ");
                        }
                    }
                }
            }
            sb.setCharAt(sb.length() - 2, '\n');
            sb.deleteCharAt(sb.length() - 1);
            for (int r = 0; r < nRows; r++) {
                for (TableColumn<DataSetsRow, ?> col : columns) {
                    if (col instanceof DataSetTableColumns && col.isVisible()) {
                        for (TableColumn<DataSetsRow, ?> subcol : col.getColumns()) {
                            if (subcol instanceof DataSetTableColumn && ((DataSetTableColumn) subcol).active) {
                                sb.append(((DataSetTableColumn) subcol).getValue(r)).append(", ");
                            }
                        }
                    } else if (col instanceof RowIndexHeaderTableColumn) {
                        sb.append(col.getCellData(r)).append(", ");
                    }
                }
                sb.setCharAt(sb.length() - 2, '\n');
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }

        public ObservableList<TableColumn<DataSetsRow, ?>> getColumns() {
            return columns;
        }

        protected String getSelectedData(final TableViewSelectionModel<DataSetsRow> selModel) {
            // Construct a sorted Set/Map with all the selected columns.
            // This means, that if you select (1,1) and (4,5), (1,5) and (4,1)
            // will also be exported.
            // A better approach would be a custom Selection model, which also
            // visualises this behaviour
            @SuppressWarnings("rawtypes") // getSelectedCells returns raw type
            final ObservableList<TablePosition> selected = selModel.getSelectedCells();
            if (selected.isEmpty()) {
                return getAllData();
            }
            final TreeSet<Integer> rows = new TreeSet<>();
            final TreeMap<Integer, TableColumn<DataSetsRow, ?>> cols = new TreeMap<>();
            for (final TablePosition<DataSetsRow, ?> cell : selected) {
                cols.put(cell.getColumn(), cell.getTableColumn());
                rows.add(cell.getRow());
            }
            // Generate a string from the selected data
            StringBuilder sb = new StringBuilder();
            sb.append('#');
            for (final Map.Entry<Integer, TableColumn<DataSetsRow, ?>> col : cols.entrySet()) {
                sb.append(col.getValue().getText()).append(", ");
            }
            sb.setCharAt(sb.length() - 2, '\n');
            sb.deleteCharAt(sb.length() - 1);
            for (final int r : rows) {
                for (final Map.Entry<Integer, TableColumn<DataSetsRow, ?>> col : cols.entrySet()) {
                    if (col.getValue() instanceof DataSetTableColumn) {
                        sb.append(((DataSetTableColumn) col.getValue()).getValue(r)).append(", ");
                    } else {
                        sb.append(col.getValue().getCellData(r)).append(", ");
                    }
                }
                sb.setCharAt(sb.length() - 2, '\n');
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }

        public double getValue(final int row, final DataSet ds, final ColumnType type) {
            if (ds == null || row >= ds.getDataCount()) {
                return 0.0;
            }
            if (!type.errorCol) {
                return ds.get(type.dimIdx, row);
            }
            if (!(ds instanceof DataSetError))
                return 0.0;
            DataSetError eds = (DataSetError) ds;
            if (type.positive) {
                return eds.getErrorPositive(type.dimIdx, row);
            }
            return eds.getErrorNegative(type.dimIdx, row);
        }

        @Override
        public int indexOf(final Object o) {
            if (o instanceof DataSetsRow) {
                final int row = ((DataSetsRow) o).row;
                return row < nRows ? row : -1;
            }
            return -1;
        }

        @Override
        public boolean isEmpty() {
            return (nRows >= 0);
        }

        protected void rendererChanged(final ListChangeListener.Change<? extends Renderer> change) {
            boolean dataSetChanges = false;
            while (change.next()) {
                // handle added renderer
                change.getAddedSubList().forEach(renderer -> renderer.getDatasets().addListener(datasetChangeListener));
                if (!change.getAddedSubList().isEmpty()) {
                    dataSetChanges = true;
                }

                // handle removed renderer
                change.getRemoved().forEach(renderer -> renderer.getDatasets().removeListener(datasetChangeListener));
                if (!change.getRemoved().isEmpty()) {
                    dataSetChanges = true;
                }
            }

            if (dataSetChanges) {
                datasetsChanged(null);
            }
        }

        @Override
        public int size() {
            return nRows;
        }

        /**
         * A Column representing an actual colum displaying Double values from a DataSet.
         *
         * @author akrimm
         */
        protected class DataSetTableColumn extends TableColumn<DataSetsRow, Double> {
            private DataSet ds;
            private final ColumnType type;
            protected boolean active = false;

            /**
             * Creates a TableColumn with the text set to the provided string, with default comparator. The cell factory
             * and onEditCommit implementation facilitate editing of the DataSet column identified by the ds and type
             * Parameter
             * 
             * @param type The field of the data to be shown
             */
            public DataSetTableColumn(final ColumnType type) {
                super("");
                this.setSortable(false);
                this.setReorderable(false);
                this.ds = null;
                this.type = type;
                this.setCellValueFactory(dataSetsRowFeature -> new ReadOnlyObjectWrapper<>(dataSetsRowFeature.getValue().getValue(ds, type)));

                this.setPrefWidth(0);
            }

            public double getValue(final int row) {
                return dsModel.getValue(row, ds, type);
            }

            public void update(final DataSet newDataSet) {
                ds = newDataSet;
                if (ds == null) {
                    this.setText("");
                    this.setPrefWidth(0);
                    active = false;
                    return;
                }
                if (editable) {
                    updateEditableState();
                }
                if (!type.errorCol) {
                    setText(type.label);
                    this.setPrefWidth(DEFAULT_COL_WIDTH);
                    active = true;
                    return;
                }
                if (!(newDataSet instanceof DataSetError)) {
                    this.setText("");
                    this.setPrefWidth(0);
                    active = false;
                    return;
                }
                DataSetError eDs = (DataSetError) newDataSet;
                switch (eDs.getErrorType(type.dimIdx)) {
                case SYMMETRIC:
                    setText(type.positive ? "" : type.label);
                    this.setPrefWidth(type.positive ? 0 : DEFAULT_COL_WIDTH);
                    active = !type.positive;
                    return;
                case ASYMMETRIC:
                    setText((type.positive ? '+' : '-') + type.label);
                    this.setPrefWidth(DEFAULT_COL_WIDTH);
                    active = true;
                    return;
                case NO_ERROR:
                default:
                    this.setText("");
                    this.setPrefWidth(0);
                    active = false;
                    break;
                }
            }

            private void updateEditableState() {
                this.setEditable(false);
                this.setOnEditCommit(null);
                if (!(ds instanceof EditableDataSet) || (type != ColumnType.X && type != ColumnType.Y)) {
                    // can edit only 'EditableDataSet's and (X or Y) columns
                    return;
                }
                final EditableDataSet editableDataSet = (EditableDataSet) ds;
                final EditConstraints editConstraints = editableDataSet.getEditConstraints();

                if (type == ColumnType.X && editConstraints != null && !editConstraints.isEditable(DIM_X)) {
                    // editing of x coordinate is excluded
                    return;
                }
                if (type == ColumnType.Y && editConstraints != null && !editConstraints.isEditable(DIM_Y)) {
                    // editing of y coordinate is excluded
                    return;
                }

                // column can theoretically be edited as long as 'canChange(index)' is true for the selected index
                // and isAcceptable(index, double, double) is also true
                this.setEditable(true);
                this.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

                this.setOnEditCommit(e -> {
                    DataSetsRow rowValue = e.getRowValue();
                    if (rowValue == null) {
                        LOGGER.atError().log("DataSet row should not be null");
                        return;
                    }
                    final int row = rowValue.getRow();
                    final double oldX = editableDataSet.get(DIM_X, row);
                    final double oldY = editableDataSet.get(DIM_Y, row);

                    if (editConstraints != null && !editConstraints.canChange(row)) {
                        // may not edit value, revert to old value (ie. via rewriting old value)
                        editableDataSet.set(row, oldX, oldY);
                        return;
                    }

                    final double newVal = e.getNewValue();
                    switch (type) {
                    case X:
                        if (editConstraints != null && !editConstraints.isAcceptable(row, newVal, oldY)) {
                            // may not edit x
                            editableDataSet.set(row, oldX, oldY);
                            break;
                        }
                        editableDataSet.set(row, newVal, oldY);
                        break;
                    case Y:
                        if (editConstraints != null && !editConstraints.isAcceptable(row, oldX, newVal)) {
                            // may not edit y
                            editableDataSet.set(row, oldX, oldY);
                            break;
                        }
                        editableDataSet.set(row, oldX, newVal);
                        break;
                    default:
                        // Errors are not editable, as there is no interface for manipulating them
                        editableDataSet.set(row, oldX, oldY);
                        break;
                    }
                });
            }
        }

        /**
         * Columns for a DataSet. Manages the the nested subcolumns for the actual data and handles updates of the
         * DataSet.
         * 
         * @author akrimm
         */
        protected class DataSetTableColumns extends TableColumn<DataSetsRow, Double> {
            private DataSet dataSet;

            public DataSetTableColumns() {
                super("");
                this.setSortable(false);
                this.setReorderable(false);
                this.dataSet = null;
                for (ColumnType type : ColumnType.values()) {
                    this.getColumns().add(new DataSetTableColumn(type));
                }
            }

            public void update(final DataSet newDataSet) {
                this.dataSet = newDataSet;
                if (newDataSet != null) {
                    this.setText(newDataSet.getName());
                    this.setPrefWidth(DEFAULT_COL_WIDTH);
                } else {
                    this.setText("");
                    this.setPrefWidth(0);
                }
                this.getColumns().forEach(col -> {
                    if (col instanceof DataSetTableColumn) {
                        ((DataSetTableColumn) col).update(this.dataSet);
                    }
                });
            }
        }

        /**
         * A simple Column displaying the Table Row and styled like the Table Header, non editable
         *
         * @author akrimm
         */
        protected class RowIndexHeaderTableColumn extends TableColumn<DataSetsRow, Integer> {
            public RowIndexHeaderTableColumn() {
                super();
                this.setSortable(false);
                this.setReorderable(false);
                setCellValueFactory(dataSetsRow -> new ReadOnlyObjectWrapper<>(dataSetsRow.getValue().getRow()));
                getStyleClass().add("column-header"); // make the column look like a header
                setEditable(false);
            }
        }
    }

    protected class DataSetsRow {
        private final int row;
        private final DataSetsModel model;

        private DataSetsRow(final int row, final DataSetsModel model) {
            this.row = row;
            this.model = model;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof DataSetsRow) {
                final DataSetsRow dsr = (DataSetsRow) o;
                /* 
                 * Use model object identity here since model relies on it's children's 
                 * equals/hashCore and to prevent endless loop
                 */
                return ((dsr.getRow() == row) && model == dsr.getModel());
            }
            return false;
        }

        protected DataSetsModel getModel() {
            return model;
        }

        public int getRow() {
            return row;
        }

        public double getValue(final DataSet ds, final ColumnType type) {
            return model.getValue(row, ds, type);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            /* Use model object identity here since model relies on it's children's 
             * equals/hashCore and to prevent endless loop
             */
            hash = 31 * hash + System.identityHashCode(model);
            hash = 31 * hash + row;
            return hash;
        }
    }
}
