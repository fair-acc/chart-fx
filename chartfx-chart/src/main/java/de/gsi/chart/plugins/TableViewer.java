package de.gsi.chart.plugins;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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

/**
 * Displays the all visible data sets inside a table on demand. Implements
 * copy-paste functionality into system clip-board and *.csv file export to
 * allow further processing in other applications. Also enables editing of
 * values if the underlying DataSet allows it.
 * 
 * @author rstein
 * @author akrimm
 */
public class TableViewer extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableViewer.class);

    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private final Glyph tableView = new Glyph(FONT_AWESOME, FontAwesome.Glyph.TABLE).size(FONT_SIZE);
    private final Glyph graphView = new Glyph(FONT_AWESOME, FontAwesome.Glyph.LINE_CHART).size(FONT_SIZE);
    private final Glyph saveIcon = new Glyph(FONT_AWESOME, "\uf0c7").size(FONT_SIZE);
    private final Glyph clipBoardIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.CLIPBOARD).size(FONT_SIZE);
    private final HBox interactorButtons = getInteractorBar();
    private final TableView<DataSetsRow> table = new TableView<>();
    private final DataSetsModel dsModel = new DataSetsModel();
    protected boolean editable;

    /**
     * Creates a new instance of DataSetTableViewer class and setup the required
     * listeners.
     */
    public TableViewer() {
        super();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setEditable(true); // Generally the TableView is editable, actual
                                 // editability is configured column-wise
        table.setItems(dsModel);

        table.getColumns().addAll(dsModel.getColumns());
        dsModel.getColumns().addListener((ListChangeListener<TableColumn<DataSetsRow, ?>>) (change -> table.getColumns()
                .setAll(dsModel.getColumns())));
        dsModel.setRefreshFunction(() -> {
            // workaround: force table to acknowledge changed data (by setting
            // to empty list and then back)
            FXUtils.runFX(() -> {
                ObservableList<DataSetsRow> tmp = table.getItems();
                table.setItems(FXCollections.emptyObservableList());
                table.setItems(tmp);
            });
            return null;
        });

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
                newChart.getPlotForeground().getChildren().add(table);
                table.toFront();
                table.setVisible(false); // table is initially invisible above
                                         // the chart
                table.prefWidthProperty().bind(newChart.getPlotForeground().widthProperty());
                table.prefHeightProperty().bind(newChart.getPlotForeground().heightProperty());
            }
            dsModel.chartChanged(oldChart, newChart);
        });

        addButtonsToToolBarProperty().addListener((ch, o, n) -> {
            final Chart chartLocal = getChart();
            if (chartLocal == null || o.equals(n)) {
                return;
            }
            if (n) {
                chartLocal.getToolBar().getChildren().add(interactorButtons);
            } else {
                chartLocal.getToolBar().getChildren().remove(interactorButtons);
            }
        });
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
        buttonBar.setPadding(new Insets(1, 1, 1, 1));
        final Button switchTableView = new Button(null, tableView);
        switchTableView.setPadding(new Insets(3, 3, 3, 3));
        switchTableView.setTooltip(new Tooltip("switches between graph and table view"));

        final Button copyToClipBoard = new Button(null, clipBoardIcon);
        copyToClipBoard.setPadding(new Insets(3, 3, 3, 3));
        copyToClipBoard.setTooltip(new Tooltip("copy selected content top system clipboard"));
        copyToClipBoard.setOnAction(e -> this.copySelectedToClipboard());

        final Button saveTableView = new Button(null, saveIcon);
        saveTableView.setPadding(new Insets(3, 3, 3, 3));
        saveTableView.setTooltip(new Tooltip("store actively shown content as .csv file"));
        saveTableView.setOnAction(e -> this.exportGridToCSV());

        switchTableView.setOnAction(evt -> {
            switchTableView.setGraphic(table.isVisible() ? tableView : graphView);
            table.setVisible(!table.isVisible());
            getChart().getPlotForeground().setMouseTransparent(!table.isVisible());
            table.setMouseTransparent(!table.isVisible());
            dsModel.refresh();
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

    /**
     * Show a FileChooser and export the (selected) Table Data to the choosen
     * .csv File.
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
            LOGGER.error("error while exporting data to csv", ex);
        }
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
     * Model Abstraction to the DataSets of a chart as the backing for a JavaFX
     * TableView. Only elements visible on screen are allocated and new elements
     * are generated onDemand using Cell Factories. Also generates the column
     * Objects for the TableView and subscribes Change Listeners to update the
     * Table whenever the datasets change or new Datasets are added
     * 
     * @author akrimm
     */
    protected class DataSetsModel extends ObservableListBase<DataSetsRow> {
        private int nRows;
        private final ObservableList<TableColumn<DataSetsRow, ?>> columns = FXCollections.observableArrayList();
        private Callable<Void> refreshFunction;

        private final ListChangeListener<Renderer> rendererChangeListener = this::rendererChanged;
        private final ListChangeListener<DataSet> datasetChangeListener = this::datasetsChanged;
        private final EventListener dataSetDataUpdateListener = (UpdateEvent evt) -> {
            nRows = 0;
            for (TableColumn<DataSetsRow, ?> col : columns) {
                if (col instanceof DataSetTableColumns) {
                    DataSetTableColumns dataSetColumn = ((DataSetTableColumns) col);
                    nRows = Math.max(nRows, dataSetColumn.dataSet.getDataCount(DIM_X));
                    for (final TableColumn<DataSetsRow, ?> subColumn : dataSetColumn.getColumns()) {
                        if (subColumn instanceof DataSetTableColumn) {
                            ((DataSetTableColumn) subColumn).updateEditableState();
                        }
                    }
                }
            }
            refresh();
        };

        public DataSetsModel() {
            super();
            columns.add(new RowIndexHeaderTableColumn());
        }

        /**
         * @param refreshFunction the refreshFunction to set
         */
        public void setRefreshFunction(final Callable<Void> refreshFunction) {
            this.refreshFunction = refreshFunction;
        }

        protected void datasetsChanged(final ListChangeListener.Change<? extends DataSet> change) {
            boolean dataSetChanges = false;

            while (change.next()) {
                for (final DataSet set : change.getRemoved()) {
                    set.removeListener(dataSetDataUpdateListener);
                    columns.removeIf(
                            col -> (col instanceof DataSetTableColumns && ((DataSetTableColumns) col).dataSet == set));
                    nRows = 0;
                    for (TableColumn<DataSetsRow, ?> col : columns) {
                        if (col instanceof DataSetTableColumn) {
                            nRows = Math.max(nRows, ((DataSetTableColumn) col).ds.getDataCount(DIM_X));
                        }
                    }
                    dataSetChanges = true;
                }

                for (final DataSet set : change.getAddedSubList()) {
                    set.addListener(dataSetDataUpdateListener);
                    columns.add(new DataSetTableColumns(set)); // NOPMD - necessary for function
                    nRows = Math.max(nRows, set.getDataCount(DIM_X));
                    dataSetChanges = true;
                }
            }

            if (dataSetChanges) {
                this.refresh();
            }
        }

        private void refresh() {
            try {
                refreshFunction.call();
            } catch (Exception e) { // NOPMD 'call()' issues generic 'Exception'
                LOGGER.error("Error refreshing table model", e);
            }
        }

        /**
         * @param oldChart The old chart the plugin is operating on
         * @param newChart The new chart the plugin is operating on
         */
        public void chartChanged(final Chart oldChart, final Chart newChart) {
            if (oldChart != null) {
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
            }
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
                this.refresh();
            }
        }

        @Override
        public DataSetsRow get(final int row) {
            return new DataSetsRow(row, this);
            // return getDataSetsRow(row);
        }

        @Override
        public int size() {
            return nRows;
        }

        @Override
        public boolean isEmpty() {
            return (nRows >= 0);
        }

        @Override
        public boolean contains(final Object o) {
            if (o instanceof DataSetsRow) {
                return (nRows > ((DataSetsRow) o).getRow());
            }
            return false;
        }

        @Override
        public int indexOf(final Object o) {
            if (o instanceof DataSetsRow) {
                final int row = ((DataSetsRow) o).row;
                return row < nRows ? row : -1;
            }
            return -1;
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

        protected String getAllData() {
            final StringBuilder sb = new StringBuilder();
            sb.append('#');
            for (TableColumn<DataSetsRow, ?> col : columns) {
                sb.append(col.getText()).append(", ");
            }
            sb.setCharAt(sb.length() - 2, '\n');
            sb.deleteCharAt(sb.length() - 1);
            for (int r = 0; r < nRows; r++) {
                for (TableColumn<DataSetsRow, ?> col : columns) {
                    if (col instanceof DataSetTableColumn) {
                        sb.append(((DataSetTableColumn) col).getValue(r)).append(", ");
                    } else {
                        sb.append(col.getCellData(r)).append(", ");
                    }
                }
                sb.setCharAt(sb.length() - 2, '\n');
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        }

        public double getValue(final int row, final DataSet ds, final ColumnType type) {
            if (row >= ds.getDataCount(DIM_X)) {
                return 0.0;
            }
            switch (type) {
            case X:
                return ds.get(DIM_X, row);
            case Y:
                return ds.get(DIM_Y, row);
            default:
                break;
            }
            DataSetError eds = (DataSetError) ds;
            switch (type) {
            case EXN:
                return eds.getErrorNegative(DIM_X, row);
            case EXP:
                return eds.getErrorPositive(DIM_X, row);
            case EYN:
                return eds.getErrorNegative(DIM_Y, row);
            case EYP:
                return eds.getErrorPositive(DIM_Y, row);
            default:
                return 0.0;
            }
        }

        /**
         * A simple Column displaying the Table Row and styled like the Table
         * Header, non editable
         *
         * @author akrimm
         */
        protected class RowIndexHeaderTableColumn extends TableColumn<DataSetsRow, Integer> {
            public RowIndexHeaderTableColumn() {
                super();
                setCellValueFactory(dataSetsRow -> {
                    return new ReadOnlyObjectWrapper<>(dataSetsRow.getValue().getRow());
                });
                getStyleClass().add("column-header"); // make the column look
                                                      // like a header
                setEditable(false);
            }
        }

        /**
         * Columns for a DataSet. Manages the the nested subcolumns for the
         * actual data and handles updates of the DataSet.
         * 
         * @author akrimm
         */
        protected class DataSetTableColumns extends TableColumn<DataSetsRow, Double> {
            private final DataSet dataSet;

            public DataSetTableColumns(final DataSet dataSet) {
                super(dataSet.getName());
                this.dataSet = dataSet;
                addSubcolumns();

            }

            private void addSubcolumns() {
                this.getColumns().add(new DataSetTableColumn("x", dataSet, ColumnType.X));
                this.getColumns().add(new DataSetTableColumn("y", dataSet, ColumnType.Y));

                if (!(dataSet instanceof DataSetError)) {
                    return;
                }
                DataSetError eDs = (DataSetError) dataSet;

                if (eDs.getErrorType() == ErrorType.X || eDs.getErrorType() == ErrorType.XY) {
                    this.getColumns().add(new DataSetTableColumn("e_x", dataSet, ColumnType.EXN));
                }
                if (eDs.getErrorType() == ErrorType.X_ASYMMETRIC || eDs.getErrorType() == ErrorType.XY_ASYMMETRIC) {
                    this.getColumns().add(new DataSetTableColumn("-e_x", dataSet, ColumnType.EXN));
                    this.getColumns().add(new DataSetTableColumn("+e_x", dataSet, ColumnType.EXP));
                }
                if (eDs.getErrorType() == ErrorType.Y || eDs.getErrorType() == ErrorType.XY) {
                    this.getColumns().add(new DataSetTableColumn("e_y", dataSet, ColumnType.EYN));
                }
                if (eDs.getErrorType() == ErrorType.Y_ASYMMETRIC || eDs.getErrorType() == ErrorType.XY_ASYMMETRIC) {
                    this.getColumns().add(new DataSetTableColumn("-e_y", dataSet, ColumnType.EYN));
                    this.getColumns().add(new DataSetTableColumn("+e_y", dataSet, ColumnType.EYP));
                }
            }
        }

        /**
         * A Column representing an actual colum displaying Double values from a
         * DataSet.
         *
         * @author akrimm
         */
        protected class DataSetTableColumn extends TableColumn<DataSetsRow, Double> {
            private final DataSet ds;
            private final ColumnType type;

            /**
             * Creates a TableColumn with the text set to the provided string,
             * with default comparator. The cell factory and onEditCommit
             * implementation facilitate editing of the DataSet column
             * identified by the ds and type Parameter
             * 
             * @param text The string to show when the TableColumn is placed
             *            within the TableView
             * @param dataSet The dataset containing the column
             * @param type The field of the data to be shown
             */
            public DataSetTableColumn(final String text, final DataSet dataSet, final ColumnType type) {
                super(text);
                this.ds = dataSet;
                this.type = type;
                this.setCellValueFactory(dataSetsRowFeature -> new ReadOnlyObjectWrapper<>(
                        dataSetsRowFeature.getValue().getValue(ds, type)));

                if (editable) {
                    updateEditableState();
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

                if (type == ColumnType.X && editConstraints != null && !editConstraints.isXEditable()) {
                    // editing of x coordinate is excluded
                    return;
                }
                if (type == ColumnType.Y && editConstraints != null && !editConstraints.isYEditable()) {
                    // editing of y coordinate is excluded
                    return;
                }

                // column can theoretically be edited as long as
                // 'canChange(index)' is true for the selected index
                // and isAcceptable(index, double, double) is also true
                this.setEditable(true);
                this.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

                this.setOnEditCommit(e -> {
                    final int row = e.getRowValue().getRow();
                    final double oldX = editableDataSet.getX(row);
                    final double oldY = editableDataSet.getY(row);

                    if (editConstraints != null && !editConstraints.canChange(row)) {
                        // may not edit value, revert to old value (ie. via
                        // rewriting old value)
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
                        // Errors are not editable, as there is no
                        // interface for manipulating them
                        editableDataSet.set(row, oldX, oldY);
                        break;
                    }
                });
            }

            public double getValue(final int row) {
                return dsModel.getValue(row, ds, type);
            }
        }
    }

    protected enum ColumnType {
        X,
        Y,
        EXN,
        EXP,
        EYN,
        EYP
    }

    protected class DataSetsRow {
        private final int row;
        private final DataSetsModel model;

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + model.hashCode();
            hash = 31 * hash + row;
            return hash;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof DataSetsRow) {
                final DataSetsRow dsr = (DataSetsRow) o;
                return ((dsr.getRow() == row) && model.equals(dsr.getModel()));
            }
            return false;
        }

        private DataSetsRow(final int row, final DataSetsModel model) {
            this.row = row;
            this.model = model;
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
    }
}
