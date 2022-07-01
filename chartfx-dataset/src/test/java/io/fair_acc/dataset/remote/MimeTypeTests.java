package io.fair_acc.dataset.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests of {@link MimeType}
 * 
 * @author rstein
 */
public class MimeTypeTests {
    @ParameterizedTest
    @EnumSource(MimeType.class)
    public void genericTests(final MimeType mType) {
        assertNotNull(mType.toString());
        final String mimeTypeName = "'" + mType + "'";

        if (MimeType.UNKNOWN.equals(mType)) {
            //N.B. exception to the rule UNKNONW maps to the default BINARY enum as safe fallback
            assertEquals(MimeType.BINARY, MimeType.getEnum(mType.toString()), "getEnum: mType = " + mimeTypeName);
        } else {
            assertEquals(mType, MimeType.getEnum(mType.toString()), "getEnum: mType = " + mimeTypeName);
        }

        assertNotNull(mType.getDescription(), "description: mType = " + mimeTypeName);
        assertNotNull(mType.getType(), "type: mType = " + mimeTypeName);
        assertNotNull(mType.getSubType(), "subType: mType = " + mimeTypeName);
        assertNotNull(mType.getFileEndings(), "fileEndings: mType = " + mimeTypeName);

        if (!mType.getFileEndings().isEmpty()) {
            for (String fileName : mType.getFileEndings()) {
                if (mType.equals(MimeType.APNG)) {
                    // skip file-ending tests since PNG and APNG have same/very similar ending .png (and the more rare .apng)
                    continue;
                }
                assertEquals(mType, MimeType.getEnumByFileName(fileName), "fileEndings - match: mType = " + mimeTypeName);
            }
        }

        final String typeRaw = mType.toString().split("/")[0];
        assertTrue(typeRaw.contentEquals(mType.getType().toString()), "mType = " + mimeTypeName);

        final String description = mType.toString();
        if (mType.isImageData()) {
            assertTrue(description.startsWith("image"), "image?: mType = " + mimeTypeName);
        } else {
            assertFalse(description.startsWith("image"), "image?: mType = " + mimeTypeName);
        }

        if (mType.isVideoData()) {
            assertTrue(description.startsWith("video"), "video?: mType = " + mimeTypeName);
        } else {
            assertFalse(description.startsWith("video"), "video?: mType = " + mimeTypeName);
        }

        if (mType.isTextData()) {
            assertTrue(description.startsWith("text"), "text?: mType = " + mimeTypeName);
        } else {
            assertFalse(description.startsWith("text"), "text?: mType = " + mimeTypeName);
        }

        if (mType.isNonDisplayableData()) {
            assertFalse(mType.isImageData(), "nonDisplayableData?: mType = " + mimeTypeName);
            assertFalse(mType.isVideoData(), "nonDisplayableData?: mType = " + mimeTypeName);
        }
    }

    @Test
    public void cornerCaseTests() {
        assertEquals(MimeType.UNKNOWN, MimeType.getEnum(null));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnum(""));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnum("  "));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnum("video/made-up-format"));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnum("wormhole/made-up-format"));

        assertEquals(MimeType.UNKNOWN, MimeType.getEnumByFileName(null));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnumByFileName(""));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnumByFileName("  "));
        assertEquals(MimeType.UNKNOWN, MimeType.getEnumByFileName(".xyz42"));

        assertEquals(MimeType.Type.UNKNOWN, MimeType.Type.getEnum(null));
        assertEquals(MimeType.Type.UNKNOWN, MimeType.Type.getEnum(""));
        assertEquals(MimeType.Type.UNKNOWN, MimeType.Type.getEnum("  "));
        assertEquals(MimeType.Type.UNKNOWN, MimeType.Type.getEnum("made-up-type"));
    }
}
