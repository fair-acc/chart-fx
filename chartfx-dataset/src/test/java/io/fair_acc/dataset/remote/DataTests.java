package io.fair_acc.dataset.remote;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link Data}
 *
 * @author rstein
 */
public class DataTests {
    @Test
    public void genericTests() {
        final byte[] testBytes = new byte[5];
        final Data test = new Data("testExportName", "testMimeType", testBytes, 4);

        assertEquals("testExportName", test.getExportNameData());
        assertEquals("testMimeType", test.getMimeType());
        assertEquals(testBytes, test.getDataByteArray());
        assertArrayEquals(testBytes, test.getDataByteArray());
        assertEquals(4, test.getDataByteArraySize());
    }

    @Test
    public void cornerCaseTests() {
        final byte[] testBytes = new byte[5];
        assertDoesNotThrow(() -> new Data("testExportName", "testMimeType", testBytes, 4));

        // assertThrows(IllegalArgumentException.class, () -> new Data("testExportName", "testMimeType", testBytes, 4));

        assertThrows(IllegalArgumentException.class, () -> new Data(null, "testMimeType", testBytes, 4));
        assertThrows(IllegalArgumentException.class, () -> new Data("", "testMimeType", testBytes, 4));
        assertThrows(IllegalArgumentException.class, () -> new Data("  ", "testMimeType", testBytes, 4));

        assertThrows(IllegalArgumentException.class, () -> new Data("testExportName", null, testBytes, 4));
        assertThrows(IllegalArgumentException.class, () -> new Data("testExportName", "", testBytes, 4));
        assertThrows(IllegalArgumentException.class, () -> new Data("testExportName", "  ", testBytes, 4));

        assertThrows(IllegalArgumentException.class, () -> new Data("testExportName", "testMimeType", null, 4));

        assertThrows(IllegalArgumentException.class, () -> new Data("testExportName", "testMimeType", testBytes, 6));
    }
}
