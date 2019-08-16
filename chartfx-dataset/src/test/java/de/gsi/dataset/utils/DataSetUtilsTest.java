package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.event.AxisNameChangeEvent;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.spi.DoubleErrorDataSet;

/**
 * @author akrimm
 */
public class DataSetUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetUtilsTest.class);
    private static final double EPSILON = 1e-6;

    @ParameterizedTest()
    @CsvSource({ //
            "test.csv,               test.csv               ", // plain filename
            "test_{dataSetName}.csv, test_TestSerialize.csv ", // data Set name
            "test_{xMin}-{xMax}.csv, test_1.0-5.0.csv ", // x min/max
            "test_{yMin}-{yMax}.csv, test_1.3-4.2.csv ", // y min/max
            "{test;string}.csv     , asdf.csv ", // metadata field
            "val_{testval;float;%.2f}.csv, val_5.25.csv ", // metadata field with formated cast to double
    })
    @DisplayName("Test Filename Generation")
    public void getFileNameTest(String pattern, String fileName) {
        DataSet dataSet = getTestDataSet();
        assertEquals(fileName, DataSetUtils.getFileName(dataSet, pattern));
    }

    @DisplayName("Serialize and Deserialize DataSet3D into StringBuffer and back")
    @ParameterizedTest(name = "binary: {0}, float: {1}")
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    public void serializeAndDeserializeDataSet3D(boolean binary, boolean useFloat) {
        // initialize dataSet
        int nx = 3;
        double[] xvalues = new double[] { 1.0, 2.0, 3.0 };
        int ny = 2;
        double[] yvalues = new double[] { 0.001, 4.2 };
        int n = 6;
        double[][] zvalues = new double[][] { { 1.3, 3.7, 4.2 }, { 2.3, 1.8, 5.0 } };
        DataSet3D dataSet = new DoubleDataSet3D("Test 3D Dataset", xvalues, yvalues, zvalues);
        dataSet.getAxisDescription(0).set("U", "V", 1.0, 3.0);
        dataSet.getAxisDescription(1).set("I", "A", 0.001, 4.2);
        dataSet.getAxisDescription(2).set("P", "W", 1.3, 4.2);
        // assert that dataSet was created correctly
        assertArrayEquals(xvalues, dataSet.getXValues(), EPSILON);
        assertArrayEquals(yvalues, dataSet.getYValues(), EPSILON);
        // write and read dataSet
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataSetUtils.writeDataSetToByteArray(dataSet, byteBuffer, binary, useFloat);
        DataSet dataSetRead = DataSetUtils.readDataSetFromByteArray(byteBuffer.toByteArray());
        // assert that DataSet was written and read correctly
        assertTrue(dataSetRead instanceof DataSet3D);
        DataSet3D dataSetRead3D = (DataSet3D) dataSetRead;
        assertEquals(n, dataSetRead.getDataCount(DataSet.DIM_Z));
        assertEquals(dataSet.getName(), dataSetRead.getName());
        assertEquals(3, dataSetRead3D.getDataCount(DataSet.DIM_X));
        assertEquals(2, dataSetRead3D.getDataCount(DataSet.DIM_Y));
        assertArrayEquals(xvalues, dataSetRead.getValues(DataSet.DIM_X), EPSILON);
        assertArrayEquals(yvalues, dataSetRead.getValues(DataSet.DIM_Y), EPSILON);
        for (int ix = 1; ix < nx; ix++) {
            for (int iy = 1; iy < ny; iy++) {
                assertEquals(zvalues[iy][ix], dataSetRead3D.getZ(ix, iy), EPSILON);
            }
        }
        for (int i = 0; i < 3; i++) {
            assertEquals(dataSet.getAxisDescription(i).getName(), dataSetRead.getAxisDescription(i).getName());
            assertEquals(dataSet.getAxisDescription(i).getUnit(), dataSetRead.getAxisDescription(i).getUnit());
            assertEquals(dataSet.getAxisDescription(i).getMin(), dataSetRead.getAxisDescription(i).getMin(), EPSILON);
            assertEquals(dataSet.getAxisDescription(i).getMax(), dataSetRead.getAxisDescription(i).getMax(), EPSILON);
        }
    }

    @DisplayName("Serialize and Deserialize DefaultDataSet into StringBuffer and back")
    @ParameterizedTest(name = "binary: {0}, float: {1}")
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    public void serializeAndDeserializeDefaultDataSet(boolean binary, boolean useFloat) {
        // initialize dataSet
        DataSet dataSet = getTestDataSet();
        // assert that dataSet was created correctly
        assertTrue(dataSet instanceof DefaultDataSet);
        // write and read dataSet
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataSetUtils.writeDataSetToByteArray(dataSet, byteBuffer, binary, useFloat);
        DataSet dataSetRead = DataSetUtils.readDataSetFromByteArray(byteBuffer.toByteArray());
        // assert that DataSet was written and read correctly
        assertTrue(dataSetRead instanceof DoubleErrorDataSet);
        for (int dim = 0; dim < dataSet.getDimension(); dim++) {
            final String msg = "Dimension#" + dim;
            assertEquals(dataSet.getDataCount(dim), dataSetRead.getDataCount(dim), msg);
            assertEquals(dataSet.getName(), dataSetRead.getName(), msg);
            int dataCount = dataSet.getDataCount(dim);
            assertArrayEquals( //
                    Arrays.copyOfRange(dataSet.getValues(dim), 0, dataCount), //
                    Arrays.copyOfRange(dataSetRead.getValues(dim), 0, dataCount), //
                    EPSILON, msg);
        }
        assertEquals("asdf", ((DoubleErrorDataSet) dataSetRead).getMetaInfo().get("test"));
        for (int i = 0; i < 2; i++) {
            assertEquals(dataSet.getAxisDescription(i).getName(), dataSetRead.getAxisDescription(i).getName());
            assertEquals(dataSet.getAxisDescription(i).getUnit(), dataSetRead.getAxisDescription(i).getUnit());
            assertEquals(dataSet.getAxisDescription(i).getMin(), dataSetRead.getAxisDescription(i).getMin(), EPSILON);
            assertEquals(dataSet.getAxisDescription(i).getMax(), dataSetRead.getAxisDescription(i).getMax(), EPSILON);
        }
        // test notification
        AtomicInteger notified = new AtomicInteger(0);
        dataSetRead.addListener((change) -> {
            if (change instanceof AxisNameChangeEvent) {
                // Because the AxisDescription sends the event itself, it does not know its dimemnsion and sends -1
                // assertEquals(1, ((AxisNameChangeEvent) change).getDimension());
                notified.incrementAndGet();
            }
        });
        dataSetRead.getAxisDescription(1).set("Test");
        assertEquals(1, notified.get());
    }

    private static DataSet getTestDataSet() {
        double[] xvalues = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] yvalues = new double[] { 1.3, 3.7, 4.2, 2.3, 1.8 };
        Map<String, String> metaInfoMap = new HashMap<>();
        metaInfoMap.put("test", "asdf");
        metaInfoMap.put("testval", "5.24532");
        DataSet result = new DataSetBuilder() //
                .setName("TestSerialize") //
                .setXValues(xvalues) //
                .setYValues(yvalues) //
                .setMetaInfoMap(metaInfoMap) //
                .build();
        result.getAxisDescription(0).set("index", "", 1.0, 5.0);
        result.getAxisDescription(1).set("Voltage", "V", 1.3, 4.2);
        return result;
    }

    @BeforeAll
    public static void resetLocalization() {
        Locale.setDefault(Locale.US);
    }

}
