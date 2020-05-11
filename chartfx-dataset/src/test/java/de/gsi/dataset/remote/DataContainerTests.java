package de.gsi.dataset.remote;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link de.gsi.dataset.remote.DataContainer}
 * 
 * @author rstein
 */
public class DataContainerTests {
    private final static Data testData1 = new Data("testData1.bin", "application/octet-stream", new byte[10], 10);
    private final static Data testData2 = new Data("testData2.bin", "application/octet-stream", new byte[11], 11);

    @Test
    public void constructorTests() {
        assertDoesNotThrow(() -> new DataContainer("exportName", "category", -1, testData1));
        assertDoesNotThrow(() -> new DataContainer("exportName", "category", -1, testData1, testData2));
        assertThrows(IllegalArgumentException.class, () -> new DataContainer(null, "category", -1, testData1, testData2));
        assertThrows(IllegalArgumentException.class, () -> new DataContainer("", "category", -1, testData1, testData2));
        assertThrows(IllegalArgumentException.class, () -> new DataContainer(" ", "category", -1, testData1, testData2));
        assertThrows(IllegalArgumentException.class, () -> new DataContainer("exportName", null, -1, testData1, testData2));
        assertThrows(IllegalArgumentException.class, () -> new DataContainer("exportName", "", -1, testData1, testData2));
        assertThrows(IllegalArgumentException.class, () -> new DataContainer("exportName", "  ", -1, testData1, testData2));

        final byte[] data = new byte[] { 0x00, 0x01, 0x02, 0x03 };
        DataContainer test = new DataContainer("category/image.png", 40, data, 3);
        assertEquals("image", test.getExportName());
        assertEquals("/category/", test.getCategory());
        assertEquals(MimeType.PNG, MimeType.getEnum(test.getMimeType()));
        assertEquals(40, test.getUpdatePeriod());
        assertEquals(data, test.getDataByteArray());
        assertArrayEquals(data, test.getDataByteArray());
        assertEquals(3, test.getDataByteArraySize());
    }

    @Test
    public void cornerCaseTests() {
        assertDoesNotThrow(() -> DataContainer.fixPreAndPost("test"));
        assertEquals("/", DataContainer.fixPreAndPost(null));
        assertEquals("/", DataContainer.fixPreAndPost(""));
        assertEquals("/", DataContainer.fixPreAndPost("/"));
        assertEquals("/test/", DataContainer.fixPreAndPost("test"));
        assertEquals("/test/", DataContainer.fixPreAndPost("/test"));
        assertEquals("/test/", DataContainer.fixPreAndPost("test/"));
        assertEquals("/test/", DataContainer.fixPreAndPost("/test/"));

        assertDoesNotThrow(() -> DataContainer.genExportName("test"));
        assertThrows(IllegalArgumentException.class, () -> DataContainer.genExportName(null));
        assertThrows(IllegalArgumentException.class, () -> DataContainer.genExportName(""));
        assertEquals("test", DataContainer.genExportName("test.png"));
        assertEquals("test", DataContainer.genExportName("test/test.png"));
        assertEquals("test", DataContainer.genExportName("/test/test.png"));
        assertEquals("test", DataContainer.genExportName("testA/testB/test.png"));
        assertEquals("test", DataContainer.genExportName("testA/testB/test"));

        assertDoesNotThrow(() -> DataContainer.genExportNameData("test.png"));
        assertThrows(IllegalArgumentException.class, () -> DataContainer.genExportNameData(null));
        assertThrows(IllegalArgumentException.class, () -> DataContainer.genExportNameData(""));
        assertEquals("test.png", DataContainer.genExportNameData("test.png"));
        assertEquals("test.png", DataContainer.genExportNameData("test/test.png"));
        assertEquals("test.png", DataContainer.genExportNameData("/test/test.png"));
        assertEquals("test.png", DataContainer.genExportNameData("testA/testB/test.png"));
        assertEquals("test", DataContainer.genExportNameData("testA/testB/test"));

        assertDoesNotThrow(() -> DataContainer.getCategory("test"));
        assertThrows(IllegalArgumentException.class, () -> DataContainer.getCategory(null));
        assertThrows(IllegalArgumentException.class, () -> DataContainer.getCategory(""));
        assertEquals("/", DataContainer.getCategory("test.png"));
        assertEquals("/test/", DataContainer.getCategory("test/test.png"));
        assertEquals("/test/", DataContainer.getCategory("/test/test.png"));
        assertEquals("/testA/testB/", DataContainer.getCategory("testA/testB/test.png"));
        assertEquals("/testA/testB/", DataContainer.getCategory("testA/testB/test"));
    }

    @Test
    public void genericTests() {
        DataContainer test = new DataContainer("exportName", "category", -1, testData1, testData2);

        assertEquals("exportName", test.getExportName());
        assertEquals("/category/", test.getCategory());

        final long createTimeStamp = test.getTimeStampCreation();
        assertTrue(createTimeStamp > 0);
        assertNotNull(test.getTimeStampCreationString());
        assertEquals(-1, test.getUpdatePeriod());
        List<Data> dataList = test.getData();
        assertFalse(dataList.isEmpty());

        // test delegate access
        assertEquals(testData1.getExportNameData(), test.getExportNameData());
        assertEquals(testData1.getMimeType(), test.getMimeType());
        assertEquals(testData1.getDataByteArray(), test.getDataByteArray());
        assertArrayEquals(testData1.getDataByteArray(), test.getDataByteArray());
        assertEquals(testData1.getDataByteArraySize(), test.getDataByteArraySize());

        final long lastAccess1 = test.getTimeStampLastAccess();
        assertTrue(lastAccess1 > 0);
        Awaitility.waitAtMost(10, TimeUnit.MILLISECONDS);
        test.updateAccess();
        final long lastAccess2 = test.getTimeStampLastAccess();
        assertTrue(lastAccess2 > 0);
        assertTrue(lastAccess2 > lastAccess1);

        assertDoesNotThrow(() -> test.setRbacToken("rbacToken"));
        assertEquals("rbacToken", test.getRbacToken());

        assertDoesNotThrow(() -> test.setSelector("selector"));
        assertEquals("selector", test.getSelector());
    }
}
