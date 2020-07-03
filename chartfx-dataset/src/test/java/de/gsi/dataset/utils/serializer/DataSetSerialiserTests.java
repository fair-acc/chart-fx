package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.iobuffer.DataSetSerialiser;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.DoubleGridDataSet;
import de.gsi.dataset.spi.MultiDimDoubleDataSet;
import de.gsi.dataset.testdata.spi.TriangleFunction;

/**
 * @author Alexander Krimm
 */
public class DataSetSerialiserTests {
    @Test
    public void testDataSetFloatError() {
        boolean asFloat32 = true;
        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", new double[] { 1f, 2f, 3f }, new double[] { 6f, 7f, 8f },
                new double[] { 7f, 8f, 9f }, new double[] { 7f, 8f, 9f }, 3, false);
        addMetaData(original, true);

        final FastByteBuffer byteBuffer = new FastByteBuffer();

        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DataSet restored = DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        assertEquals(original, restored);
    }

    private static void addMetaData(final AbstractDataSet<?> dataSet, final boolean addLabelsStyles) {
        if (addLabelsStyles) {
            dataSet.addDataLabel(1, "test");
            dataSet.addDataStyle(2, "color: red");
        }
        dataSet.getMetaInfo().put("Test", "Value");
        dataSet.getErrorList().add("TestError");
        dataSet.getWarningList().add("TestWarning");
        dataSet.getInfoList().add("TestInfo");
    }

    @Test
    public void testDataSetErrorSymmetric() {
        boolean asFloat32 = false;

        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", new double[] { 1, 2, 3 }, new double[] { 6, 7, 8 }, new double[] { 7, 8, 9 },
                new double[] { 7, 8, 9 }, 3, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public ErrorType getErrorType(int dimIndex) {
                if (dimIndex == 1) {
                    return ErrorType.SYMMETRIC;
                }
                return super.getErrorType(dimIndex);
            }
        };
        addMetaData(original, true);

        final FastByteBuffer byteBuffer = new FastByteBuffer();
        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DefaultErrorDataSet restored = (DefaultErrorDataSet) DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        assertEquals(new DefaultErrorDataSet(original), new DefaultErrorDataSet(restored));
    }

    @Test
    public void testDataSetFloatErrorSymmetric() {
        boolean asFloat32 = true;

        final DefaultErrorDataSet original = new DefaultErrorDataSet("test", new double[] { 1f, 2f, 3f }, new double[] { 6f, 7f, 8f },
                new double[] { 7f, 8f, 9f }, new double[] { 7f, 8f, 9f }, 3, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public ErrorType getErrorType(int dimIndex) {
                if (dimIndex == 1) {
                    return ErrorType.SYMMETRIC;
                }
                return super.getErrorType(dimIndex);
            }
        };
        addMetaData(original, true);

        final FastByteBuffer byteBuffer = new FastByteBuffer();

        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DefaultErrorDataSet restored = (DefaultErrorDataSet) DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        assertEquals(new DefaultErrorDataSet(original), new DefaultErrorDataSet(restored));
    }

    @Test
    public void testDataSet() {
        boolean asFloat32 = false;
        final DoubleDataSet original = new DoubleDataSet(new TriangleFunction("test", 1009));
        addMetaData(original, true);

        final FastByteBuffer byteBuffer = new FastByteBuffer();

        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DataSet restored = DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        assertEquals(original, restored);
    }

    @Test
    public void testErrorDataSet() {
        boolean asFloat32 = false;
        final DoubleErrorDataSet original = new DoubleErrorDataSet(new TriangleFunction("test", 1009));
        addMetaData(original, true);

        final FastByteBuffer byteBuffer = new FastByteBuffer();

        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DataSet restored = DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        assertEquals(original, restored);
    }

    @Test
    public void testMultiDimDataSet() {
        boolean asFloat32 = false;
        final DoubleGridDataSet original = new DoubleGridDataSet("test", false, new double[][] { { 1, 2, 3 }, { 10, 20 } }, new double[] { 0.5, 1, 1.5, 2, 2.5, 3 });
        // Labels and styles are not correctly handled by multi dim data set because it is not really defined on which
        // dimension the label index is defined
        addMetaData(original, false);

        final FastByteBuffer byteBuffer = new FastByteBuffer();

        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        byteBuffer.reset(); // reset to read position (==0)
        final DataSet restored = DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        assertEquals(original, restored);
    }

    @Test
    public void testMultiDimDataSetFloatNoMetaDataAndLabels() {
        boolean asFloat32 = true;
        final DoubleGridDataSet original = new DoubleGridDataSet("test", false, new double[][] { { 1, 2, 3 }, { 10, 20 } }, new double[] { 0.5, 1, 1.5, 2, 2.5, 3 });
        addMetaData(original, false);

        final FastByteBuffer byteBuffer = new FastByteBuffer();

        DataSetSerialiser.setDataLablesSerialised(false);
        DataSetSerialiser.setMetaDataSerialised(false);
        DataSetSerialiser.writeDataSetToByteArray(original, byteBuffer, asFloat32);
        DataSetSerialiser.setDataLablesSerialised(true);
        DataSetSerialiser.setMetaDataSerialised(true);
        byteBuffer.reset(); // reset to read position (==0)
        final DataSet restored = DataSetSerialiser.readDataSetFromByteArray(byteBuffer);

        MultiDimDoubleDataSet originalNoMetaData = new MultiDimDoubleDataSet(original);
        original.getMetaInfo().clear();
        original.getErrorList().clear();
        original.getWarningList().clear();
        original.getInfoList().clear();

        assertEquals(originalNoMetaData, restored);
    }
}
