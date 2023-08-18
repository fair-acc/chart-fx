package io.fair_acc.chartfx.legend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;

/**
 * Tests {@link io.fair_acc.chartfx.legend.Legend }, {@link io.fair_acc.chartfx.legend.spi.DefaultLegend } and it's position in {@link Chart }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class LegendTests { // TODO: fix legend tests
/*
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private final Renderer testRenderer = new TestRenderer();
    private final DataSetNode testDataSet = new SineFunction("sine", 100);
    private final DataSet testDataSetAlt = new SineFunction("sineAlt", 100);
    private XYChart chart;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(DefaultLegend::new);

        chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.getRenderers().setAll(testRenderer);
        testRenderer.getDatasets().add(testDataSet);
        assertEquals(testDataSet, testRenderer.getDatasets().get(0));
        assertEquals(testRenderer.getDatasets(), testRenderer.getDatasetsCopy());

        stage.setScene(new Scene(chart, WIDTH, HEIGHT));
        stage.show();
    }

    @TestFx
    public void testLegendItemSetterGetter() {
        Node symbol1 = new Rectangle();
        Node symbol2 = new Circle();

        assertDoesNotThrow(() -> new LegendItem("test", symbol1));

        LegendItem legendItem = new LegendItem("test", symbol1);

        assertEquals("test", legendItem.getText());

        assertEquals(symbol1, legendItem.getSymbol());
        assertDoesNotThrow(() -> legendItem.setSymbol(symbol2));
        assertEquals(symbol2, legendItem.getSymbol());
    }

    @TestFx
    public void testSetterGetter() {
        assertDoesNotThrow(DefaultLegend::new);
        final DefaultLegend legend = new DefaultLegend();

        assertEquals(legend, legend.getNode());

        assertFalse(legend.isVertical());
        legend.setVertical(true);
        assertTrue(legend.isVertical());
        legend.setVertical(false);
        assertFalse(legend.isVertical());

        final LegendItem legendItem1 = new LegendItem("test2", new Rectangle());
        final LegendItem legendItem2 = new LegendItem("test1", new Circle());

        final ObservableList<LegendItem> legendItems = FXCollections.observableArrayList(legendItem1, legendItem2);

        // add legend items
        assertTrue(legend.getItems().isEmpty());
        assertDoesNotThrow(() -> legend.setItems(legendItems));
        assertEquals(legendItems, legend.getItems());
        assertFalse(legend.getItems().isEmpty());

        // assertDoesNotThrow(() ->  legend.setItems(null));
        // assertTrue(legend.getItems().isEmpty());

        LegendItem legendItem = legend.getNewLegendItem(testRenderer, testDataSet, 0);
        assertEquals("sine", legendItem.getText());
        assertTrue(legendItem.getSymbol() instanceof Canvas);

        legend.getItems().clear();
        assertTrue(legend.getItems().isEmpty());
        legend.updateLegend(List.of(testDataSet), List.of(testRenderer), false);
        assertFalse(legend.getItems().isEmpty());
        assertEquals("sine", legend.getItems().get(0).getText());
        assertTrue(legend.getItems().get(0).getSymbol() instanceof Canvas);

        legend.updateLegend(List.of(testDataSet, testDataSet), List.of(testRenderer), false);
        assertEquals(1, legend.getItems().size());

        legend.updateLegend(List.of(testDataSet), List.of(testRenderer, testRenderer), false);
        assertEquals(1, legend.getItems().size());

        testDataSet.setStyle(XYChartCss.DATASET_SHOW_IN_LEGEND + "=false;");
        legend.updateLegend(List.of(testDataSet), List.of(testRenderer), true);
        assertEquals(0, legend.getItems().size());
        testDataSet.setStyle(XYChartCss.DATASET_SHOW_IN_LEGEND + "=true;");
        legend.updateLegend(List.of(testDataSet), List.of(testRenderer), true);
        assertEquals(1, legend.getItems().size());

        legend.updateLegend(List.of(testDataSet), Collections.emptyList(), true);
        assertEquals(0, legend.getItems().size());

        testRenderer.setShowInLegend(false);
        legend.updateLegend(List.of(testDataSet), List.of(testRenderer), true);
        assertEquals(0, legend.getItems().size());

        testRenderer.setShowInLegend(true);
        legend.updateLegend(List.of(testDataSet), List.of(testRenderer), true);
        assertEquals(1, legend.getItems().size());

        legend.updateLegend(List.of(testDataSetAlt), List.of(testRenderer), false);
        assertEquals(2, legend.getItems().size());
    }

    private static class TestRenderer implements Renderer {
        private final BooleanProperty showInLegend = new SimpleBooleanProperty(this, "showInLegend", true);
        private final ObservableList<Axis> axisList = FXCollections.observableArrayList();
        private final ObservableList<DataSet> dataSetList = FXCollections.observableArrayList();

        @Override
        public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
            final Canvas canvas = new Canvas(width, height);
            canvas.getGraphicsContext2D().setFill(Color.DARKRED);
            canvas.getGraphicsContext2D().fillRect(0, 0, width, height);
            return canvas;
        }

        @Override
        public ObservableList<Axis> getAxes() {
            return axisList;
        }

        @Override
        public ObservableList<DataSet> getDatasets() {
            return dataSetList;
        }

        @Override
        public ObservableList<DataSet> getDatasetsCopy() {
            return FXCollections.observableArrayList(dataSetList);
        }

        @Override
        public List<DataSet> render(GraphicsContext gc, Chart chart, int dataSetOffset, ObservableList<DataSet> datasets) {
            // not (yet) needed in this test case -- only Legend aspects are considered
            //TODO: add DataSet reference counting once 'void renderer(...)' is changed to 'int renderer(...)'
            return Collections.emptyList();
        }

        @Override
        public Renderer setShowInLegend(boolean state) {
            showInLegendProperty().set(state);
            return this;
        }

        @Override
        public boolean showInLegend() {
            return showInLegendProperty().get();
        }

        @Override
        public BooleanProperty showInLegendProperty() {
            return showInLegend;
        }
    }
*/
}
