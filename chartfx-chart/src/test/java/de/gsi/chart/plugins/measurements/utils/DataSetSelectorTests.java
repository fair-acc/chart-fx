package de.gsi.chart.plugins.measurements.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.testdata.spi.SineFunction;

/**
* Tests {@link de.gsi.chart.plugins.measurements.utils.DataSetSelector }
* @author rstein
*
*/
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class DataSetSelectorTests {
    private DataSetSelector field;
    private DataSet testFunction1;
    private DataSet testFunction2;

    @Start
    public void start(Stage stage) {
        XYChart chart = new XYChart();
        ParameterMeasurements plugin = new ParameterMeasurements();

        assertThrows(IllegalArgumentException.class, () -> new DataSetSelector(null, 0));
        for (int nDataSets = 0; nDataSets < 3; nDataSets++) {
            final int nDataSetsLocal = nDataSets;
            assertDoesNotThrow(() -> new DataSetSelector(plugin, nDataSetsLocal));
        }

        testFunction1 = new SineFunction("sine1", 1000);
        chart.getDatasets().add(testFunction1);
        testFunction2 = new SineFunction("sine2", 1001);
        chart.getDatasets().add(testFunction2);
        chart.getPlugins().add(plugin);
        field = new DataSetSelector(plugin, 2);

        stage.setScene(new Scene(field, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertEquals(2, field.getNumberDataSets());
        assertEquals(2, field.getSelectedDataSets().size());

        assertNotNull(field.getSelectedDataSet());
        assertEquals(testFunction2, field.getSelectedDataSet());

        assertNotNull(field.getDataSetListView());
        assertEquals(2, field.getDataSetListView().getItems().size());

        assertEquals(testFunction1, field.getSelectedDataSets().get(0));
        assertEquals(testFunction2, field.getSelectedDataSets().get(1));
    }
}
