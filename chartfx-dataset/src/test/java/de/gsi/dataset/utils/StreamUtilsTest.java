package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.dataset.utils.StreamUtils.CLASSPATH_PREFIX;
import static de.gsi.dataset.utils.StreamUtils.ZIP_PREFIX;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class StreamUtilsTest {
    @Test
    void getInputStream() {
        try (InputStream stream = StreamUtils.getInputStream(CLASSPATH_PREFIX + "junit-platform.properties")) {
            assertNotNull(stream);
        } catch (IOException e) {
            fail(e);
        }
        try (InputStream stream = StreamUtils.getInputStream(CLASSPATH_PREFIX + "null")) {
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
            try (InputStream stream = StreamUtils.getInputStream(ZIP_PREFIX + "target/test-classes/junit-platform.zip/test/javafile.txt")) {
                fail("Stream " + stream + " should not be created successully");
            }
        });
        assertThrows(FileNotFoundException.class, () -> {
            try (InputStream stream = StreamUtils.getInputStream(ZIP_PREFIX + "/target/test-classes/junit-platform.ziptest/javafile.txt")) {
                fail("Stream " + stream + " should not be created successully");
            }
        });
    }
}