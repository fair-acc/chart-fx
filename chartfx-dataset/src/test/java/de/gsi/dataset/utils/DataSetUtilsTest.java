package de.gsi.dataset.utils;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.utils.DataSetUtils.Compression;
import de.gsi.dataset.utils.DataSetUtils.SplitCharByteInputStream;

/**
 * @author akrimm
 */
class DataSetUtilsTest {
    private static final double EPSILON = 1e-6;

    @DisplayName("Serialize and Deserialize DataSet3D into StringBuffer and back")
    @ParameterizedTest(name = "binary: {0}, float: {1}")
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void serializeAndDeserializeDataSet3D(boolean binary, boolean useFloat) {
        // initialize dataSet
        DataSet dataSet = new DataSetBuilder("Test 3D Dataset") //
                                  .setValues(DIM_X, new double[] { 1.0f, 2.0f, 3.0f }) //
                                  .setValues(DIM_Y, new double[] { 0.001f, 4.2f })
                                  .setValues(DIM_Z, new double[][] { { 1.3f, 3.7f, 4.2f }, { 2.3f, 1.8f, 5.0f } }) //
                                  .setAxisName(DIM_X, "U")
                                  .setAxisUnit(DIM_X, "V") //
                                  .setAxisName(DIM_Y, "I")
                                  .setAxisUnit(DIM_Y, "A") //
                                  .setAxisName(DIM_Z, "P")
                                  .setAxisUnit(DIM_Z, "W") //
                                  .build();
        // write and read dataSet
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataSetUtils.writeDataSetToByteArray(dataSet, byteBuffer, binary, useFloat);
        DataSet dataSetRead = DataSetUtils.readDataSetFromByteArray(byteBuffer.toByteArray());
        // assert that DataSet was written and read correctly
        assertEquals(dataSet, dataSetRead);
    }

    @DisplayName("Serialize and Deserialize DefaultDataSet into StringBuffer and back")
    @ParameterizedTest(name = "binary: {0}, float: {1}")
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void serializeAndDeserializeDefaultDataSet(boolean binary, boolean useFloat) {
        // initialize dataSet
        DataSet dataSet = new DataSetBuilder() //
                                  .setName("TestSerialize") //
                                  .setValuesNoCopy(DIM_X, new double[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }) //
                                  .setValuesNoCopy(DIM_Y, new double[] { 1.3f, 3.7f, 4.2f, 2.3f, 1.8f }) //
                                  .setPosErrorNoCopy(DIM_Y, new double[] { 0.1f, 0.3f, 0.2f, 0.3f, 0.8f }) //
                                  .setNegErrorNoCopy(DIM_Y, new double[] { 0.1f, 0.3f, 0.2f, 0.3f, 0.8f }) // DataSetUtils defaults to ASYMMETRIC
                                  .setAxisName(DIM_X, "index")
                                  .setAxisUnit(DIM_X, "") //
                                  .setAxisName(DIM_Y, "Voltage")
                                  .setAxisUnit(DIM_Y, "V") //
                                  .setMetaInfoMap(Map.of("test", "asdf", "testval", "5.24532")) //
                                  .setMetaWarningList("testWarning") //
                                  .setMetaInfoList("testInfo") //
                                  .setMetaErrorList("testError") //
                                  .build();
        // write and read dataSet
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        DataSetUtils.writeDataSetToByteArray(dataSet, byteBuffer, binary, useFloat);
        DataSet dataSetRead = DataSetUtils.readDataSetFromByteArray(byteBuffer.toByteArray());
        // assert that DataSet was written and read correctly
        assertNotNull(dataSetRead);
        requireNonNull(dataSet);
        assertEquals(dataSet, dataSetRead);
        assertEquals(dataSet.getDataCount(), dataSetRead.getDataCount());
        assertTrue(dataSetRead instanceof DoubleErrorDataSet);
        for (int dim = 0; dim < dataSet.getDimension(); dim++) {
            final String msg = "Dimension#" + dim;
            assertEquals(dataSet.getName(), dataSetRead.getName(), msg);
            int dataCount = dataSet.getDataCount();
            assertArrayEquals( //
                    Arrays.copyOfRange(dataSet.getValues(dim), 0, dataCount), //
                    Arrays.copyOfRange(dataSetRead.getValues(dim), 0, dataCount), //
                    EPSILON, msg);
            if (dataSet instanceof DataSetError && ((DataSetError) dataSet).getErrorType(dim) != de.gsi.dataset.DataSetError.ErrorType.NO_ERROR) {
                assertArrayEquals( //
                        Arrays.copyOfRange(((DataSetError) dataSet).getErrorsPositive(dim), 0, dataCount), //
                        Arrays.copyOfRange(((DataSetError) dataSetRead).getErrorsPositive(dim), 0, dataCount), //
                        EPSILON, msg);
                assertArrayEquals( //
                        Arrays.copyOfRange(((DataSetError) dataSet).getErrorsNegative(dim), 0, dataCount), //
                        Arrays.copyOfRange(((DataSetError) dataSetRead).getErrorsNegative(dim), 0, dataCount), //
                        EPSILON, msg);
            }
        }
        assertEquals("asdf", ((DoubleErrorDataSet) dataSetRead).getMetaInfo().get("test"));
        for (int i = 0; i < 2; i++) {
            assertEquals(dataSet.getAxisDescription(i).getName(), dataSetRead.getAxisDescription(i).getName());
            assertEquals(dataSet.getAxisDescription(i).getUnit(), dataSetRead.getAxisDescription(i).getUnit());
            assertEquals(dataSet.getAxisDescription(i).getMin(), dataSetRead.getAxisDescription(i).getMin(), EPSILON);
            assertEquals(dataSet.getAxisDescription(i).getMax(), dataSetRead.getAxisDescription(i).getMax(), EPSILON);
        }
    }

    @DisplayName("Serialize and Deserialize DefaultDataSet into file and back")
    @ParameterizedTest(name = "binary: {0}, filename: {1}")
    @CsvSource({
            "false, dataset.csv.gz",
            "true, dataset.bin.gz",
            "false, dataset.csv.zip",
            "true, dataset.bin.zip",
            "false, dataset.csv",
            "true, dataset.bin",
    })
    void
    readAndWriteDefaultDataSetToFile(boolean binary, String filename, @TempDir Path tmpdir) {
        // initialize dataSet
        DataSet dataSet = new DataSetBuilder() //
                                  .setName("TestSerialize") //
                                  .setValuesNoCopy(DIM_X, new double[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }) //
                                  .setValuesNoCopy(DIM_Y, new double[] { 1.3f, 3.7f, 4.2f, 2.3f, 1.8f }) //
                                  .setAxisName(DIM_X, "index")
                                  .setAxisUnit(DIM_X, "") //
                                  .setAxisName(DIM_Y, "Voltage")
                                  .setAxisUnit(DIM_Y, "V") //
                                  .setMetaInfoMap(Map.of("test", "asdf", "testval", "5.24532")) //
                                  .setMetaWarningList("testWarning") //
                                  .setMetaInfoList("testInfo") //
                                  .setMetaErrorList("testError") //
                                  .build();
        // write and read dataSet
        DataSetUtils.writeDataSetToFile(dataSet, tmpdir, filename, binary);
        DataSet dataSetRead = DataSetUtils.readDataSetFromFile(tmpdir.toAbsolutePath().toString() + '/' + filename);
        // assert that DataSet was written and read correctly
        assertNotNull(dataSetRead);
        assertTrue(dataSetRead instanceof DoubleErrorDataSet);
        assertEquals(dataSet.getDataCount(), dataSetRead.getDataCount());
        for (int dim = 0; dim < dataSet.getDimension(); dim++) {
            final String msg = "Dimension#" + dim;
            assertEquals(dataSet.getName(), dataSetRead.getName(), msg);
            int dataCount = dataSet.getDataCount();
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
    }

    @Test
    void testFailureCases() {
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.readDataSetFromByteArray(null));
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.readDataSetFromByteArray(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.writeDataSetToFile(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.writeDataSetToFile(new DefaultDataSet("test"), null, null));
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.writeDataSetToFile(new DefaultDataSet("test"), Path.of("/tmp"), ""));
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.writeDataSetToFile(new DefaultDataSet("test"), Path.of("/tmp"), null));
    }

    @Test
    void testEvaluateCompression() {
        assertEquals(Compression.GZIP, DataSetUtils.evaluateAutoCompression("test.bin.gz"));
        assertEquals(Compression.ZIP, DataSetUtils.evaluateAutoCompression("test.csv.zip"));
        assertEquals(Compression.NONE, DataSetUtils.evaluateAutoCompression("test.csv"));
    }

    @Test
    void testHelperFunctions() {
        // copyDataSets
        // cropToLength
        final double[] array = new double[] { 1, 2, 3, 4 };
        assertArrayEquals(new double[] { 1, 2, 3 }, DataSetUtils.cropToLength(array, 3));
        assertSame(array, DataSetUtils.cropToLength(array, 4));
        // isodate String getISODate(final long timeMillis, final String format) {
    }

    @Test
    void testSplitCharByteInputStream() throws IOException {
        final byte[] byteArray = new byte[] { 'a', 'b', 'c', SplitCharByteInputStream.MARKER, 120, 96 };
        SplitCharByteInputStream scbiStream = new SplitCharByteInputStream(new PushbackInputStream(new ByteArrayInputStream(byteArray)));
        // single byte method
        assertFalse(scbiStream.reachedSplit());
        assertEquals('a', scbiStream.read());
        assertEquals('b', scbiStream.read());
        assertEquals('c', scbiStream.read());
        assertFalse(scbiStream.reachedSplit());
        assertEquals(-1, scbiStream.read());
        assertTrue(scbiStream.reachedSplit());
        assertEquals(-1, scbiStream.read());
        scbiStream.switchToBinary();
        assertFalse(scbiStream.reachedSplit());
        assertEquals(120, scbiStream.read());
        assertEquals(96, scbiStream.read());
        assertEquals(-1, scbiStream.read());
        // chunk methods
        scbiStream = new SplitCharByteInputStream(new PushbackInputStream(new ByteArrayInputStream(byteArray)));
        byte[] output = new byte[2];
        assertEquals(2, scbiStream.read(output));
        assertArrayEquals(new byte[] { 'a', 'b' }, output);
        assertFalse(scbiStream.reachedSplit());
        assertEquals(1, scbiStream.read(output));
        assertArrayEquals(new byte[] { 'c', SplitCharByteInputStream.MARKER }, output);
        assertTrue(scbiStream.reachedSplit());
        assertEquals(-1, scbiStream.read(output));
        scbiStream.switchToBinary();
        assertFalse(scbiStream.reachedSplit());
        assertEquals(2, scbiStream.read(output));
        assertArrayEquals(new byte[] { 120, 96 }, output);
        assertEquals(-1, scbiStream.read(output));
        assertArrayEquals(new byte[] { 120, 96 }, output);
        // chunk methods with offset
        scbiStream = new SplitCharByteInputStream(new PushbackInputStream(new ByteArrayInputStream(byteArray)));
        output = new byte[8];
        assertEquals(2, scbiStream.read(output, 1, 2));
        scbiStream.switchToBinary();
        assertArrayEquals(new byte[] { 0, 'a', 'b', 0, 0, 0, 0, 0 }, output);
        assertFalse(scbiStream.reachedSplit());
        assertEquals(1, scbiStream.read(output, 3, 2));
        assertArrayEquals(new byte[] { 0, 'a', 'b', 'c', SplitCharByteInputStream.MARKER, 0, 0, 0 }, output);
        assertTrue(scbiStream.reachedSplit());
        assertEquals(-1, scbiStream.read(output, 4, 4));
        scbiStream.switchToBinary();
        assertFalse(scbiStream.reachedSplit());
        assertEquals(2, scbiStream.read(output, 4, 4));
        scbiStream.switchToBinary();
        assertArrayEquals(new byte[] { 0, 'a', 'b', 'c', 120, 96, 0, 0 }, output);
        assertEquals(-1, scbiStream.read(output));
    }

    @Test
    void testGenerateFileName() {
        final long acqStamp = System.currentTimeMillis();
        final DataSet dataSet = new DataSetBuilder("dsName") //
                                        .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValuesNoCopy(DIM_Y, new double[] { 9, 7, 8 }) //
                                        .setMetaInfoMap(Map.of("metaInt", "1337", "metaString", "testMetaInfo", "metaDouble", "1.337", "metaEng", "1.33e-7", "acqStamp",
                                                Long.toString(acqStamp)))
                                        .build();
        assertEquals("file.bin.gz", DataSetUtils.getFileName(dataSet, "file.bin.gz"));
        assertEquals("file_metaDataFieldMissing.bin.gz", DataSetUtils.getFileName(dataSet, "file_{}.bin.gz"));
        assertEquals("dsName", DataSetUtils.getFileName(dataSet, "{dataSetName}"));
        assertEquals("noDataset", DataSetUtils.getFileName(null, "{dataSetName}"));
        assertEquals("1.0", DataSetUtils.getFileName(dataSet, "{xMin}"));
        assertEquals("noDataset", DataSetUtils.getFileName(null, "{xMin}"));
        assertEquals("3.0", DataSetUtils.getFileName(dataSet, "{xMax}"));
        assertEquals("noDataset", DataSetUtils.getFileName(null, "{xMax}"));
        assertEquals("7.0", DataSetUtils.getFileName(dataSet, "{yMin}"));
        assertEquals("noDataset", DataSetUtils.getFileName(null, "{yMin}"));
        assertEquals("9.0", DataSetUtils.getFileName(dataSet, "{yMax}"));
        assertEquals("noDataset", DataSetUtils.getFileName(null, "{yMax}"));
        assertEquals(System.currentTimeMillis(), Long.parseLong(DataSetUtils.getFileName(null, "{systemTime}")), 1000L);
        assertEquals("testMetaInfo", DataSetUtils.getFileName(dataSet, "{metaString}"));
        assertEquals("metaDataMissing", DataSetUtils.getFileName(null, "{metaString}"));
        assertEquals(1337, Integer.parseInt(DataSetUtils.getFileName(dataSet, "{metaInt;int}")));
        assertEquals("0001337", DataSetUtils.getFileName(dataSet, "{metaInt;int;%07d}"));
        assertEquals(1.337, Double.parseDouble(DataSetUtils.getFileName(dataSet, "{metaDouble;float}")));
        assertEquals("1.3", DataSetUtils.getFileName(dataSet, "{metaDouble;float;%.1f}"));
        assertEquals(0.000000133, Double.parseDouble(DataSetUtils.getFileName(dataSet, "{metaEng;float}")));
        assertEquals(acqStamp, Long.parseLong(DataSetUtils.getFileName(dataSet, "{acqStamp}")));
        assertEquals(DataSetUtils.getISODate(acqStamp, "yyyyMMdd_HHmmss"), DataSetUtils.getFileName(dataSet, "{acqStamp;date}"));
        assertEquals(DataSetUtils.getISODate(acqStamp, "mmss"), DataSetUtils.getFileName(dataSet, "{acqStamp;date;mmss}"));
        assertThrows(IllegalArgumentException.class, () -> DataSetUtils.getFileName(dataSet, "{metaInt;nonexistentType}"));
    }

    @BeforeAll
    static void resetLocalization() {
        Locale.setDefault(Locale.US);
    }
}
