package io.fair_acc.dataset.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class StreamUtilsTest {
    @Test
    void getInputStream() {
        try (InputStream stream = StreamUtils.getInputStream(StreamUtils.CLASSPATH_PREFIX + "junit-platform.properties")) {
            assertNotNull(stream);
        } catch (IOException e) {
            fail(e);
        }
        try (InputStream stream = StreamUtils.getInputStream(StreamUtils.CLASSPATH_PREFIX + "null")) {
            assertNull(stream);
        } catch (IOException e) {
            fail(e);
        }
        try (InputStream stream = StreamUtils.getInputStream("target/test-classes/junit-platform.properties")) {
            assertNotNull(stream);
        } catch (IOException e) {
            fail(e);
        }
        assertThrows(FileNotFoundException.class, () -> {
            try (InputStream stream = StreamUtils.getInputStream(StreamUtils.ZIP_PREFIX + "target/test-classes/junit-platform.zip/test/javafile.txt")) {
                fail("Stream " + stream + " should not be created successully");
            }
        });
        assertThrows(FileNotFoundException.class, () -> {
            try (InputStream stream = StreamUtils.getInputStream(StreamUtils.ZIP_PREFIX + "/target/test-classes/junit-platform.ziptest/javafile.txt")) {
                fail("Stream " + stream + " should not be created successully");
            }
        });
    }
}