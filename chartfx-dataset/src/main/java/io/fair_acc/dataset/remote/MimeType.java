package io.fair_acc.dataset.remote;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Definition and convenience methods for common MIME types according to RFC6838 and RFC4855
 * <p>
 * Since the official list is rather and contains types we likely never
 * encounter, we chose the specific sub-selection from:
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
 *
 * @author rstein
 *
 */
public enum MimeType {
    /* audio MIME types */
    AAC("audio/aac", "AAC audio", ".aac"),
    MIDI("audio/midi audio/x-midi", "Musical Instrument Digital Interface (MIDI)", ".mid", ".midi"),
    MP3("audio/mpeg", "MP3 audio", ".mp3"),
    OTF("audio/opus", "Opus audio", ".opus"),
    WAV("audio/wav", "Waveform Audio Format", ".wav"),
    WEBM_AUDIO("audio/webm", "WEBM audio", ".weba"),

    /* image MIME types */
    BMP("image/bmp", "Windows OS/2 Bitmap Graphics", ".bmp"),
    GIF("image/gif", "Graphics Interchange Format (GIF)", ".gif"),
    ICO("image/vnd.microsoft.icon", "Icon format", ".ico"),
    JPEG("image/jpeg", "JPEG images", ".jpg", ".jpeg"),
    PNG("image/png", "Portable Network Graphics", ".png"),
    APNG("image/apng", "Portable Network Graphics", ".png", ".apng"),
    SVG("image/svg+xml", "Scalable Vector Graphics (SVG)", ".svg"),
    TIFF("image/tiff", "Tagged Image File Format (TIFF)", ".tif", ".tiff"),
    WEBP("image/webp", "WEBP image", ".webp"),

    /* text MIME types */
    CSS("text/css", "Cascading Style Sheets (CSS)", ".css"),
    CSV("text/csv", "Comma-separated values (CSV)", ".csv"),
    EVENT_STREAM("text/event-stream", "SSE stream"),
    HTML("text/html", "HyperText Markup Language (HTML)", ".htm", ".html"),
    ICS("text/calendar", "iCalendar format", ".ics"),
    JAVASCRIPT("text/javascript", "JavaScript", ".js", ".mjs"),
    TEXT("text/plain", "Text, (generally ASCII or ISO 8859-n)", ".txt"),
    XML("text/xml", "XML", ".xml"), // if readable from casual users (RFC 3023, section 3)
    YAML("text/yaml", "YAML Ain't Markup Language File", ".yml", ".yaml"), // not yet an IANA standard

    /* video MIME types */
    AVI("video/x-msvideo", "AVI: Audio Video Interleave", ".avi"),
    MP2T("video/mp2t", "MPEG transport stream", ".ts"),
    MPEG("video/mpeg", "MPEG Video", ".mpeg"),
    WEBM_VIDEO("video/webm", "WEBM video", ".webm"),

    /* application-specific audio MIME types -- mostly binary-type formats */
    BINARY("application/octet-stream", "Any kind of binary data", ".bin"),
    // BZIP("application/x-bzip", "BZip archive", ".bz"), // affected by patent
    BZIP2("application/x-bzip2", "BZip2 archive", ".bz2"),
    DOC("application/msword", "Microsoft Word", ".doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Microsoft Word (OpenXML)", ".docx"),
    GZIP("application/gzip", "GZip Compressed Archive", ".gz"),
    JAR("application/java-archive", "Java Archive (JAR)", ".jar"),
    JSON("application/json", "JSON format", ".json"),
    JSON_LD("application/ld+json", "JSON-LD format", ".jsonld"),
    ODP("application/vnd.oasis.opendocument.presentation", "OpenDocument presentation document", ".odp"),
    ODS("application/vnd.oasis.opendocument.spreadsheet", "OpenDocument spreadsheet document", ".ods"),
    ODT("application/vnd.oasis.opendocument.text", "OpenDocument text document", ".odt"),
    OGG("application/ogg", "OGG Audio/Video File", ".ogx", ".ogv", ".oga"),
    PDF("application/pdf", "Adobe Portable Document Format (PDF)", ".pdf"),
    PHP("application/x-httpd-php", "Hypertext Preprocessor (Personal Home Page)", ".php"),
    PPT("application/vnd.ms-powerpoint", "Microsoft PowerPoint", ".ppt"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "Microsoft PowerPoint (OpenXML)", ".pptx"),
    RAR("application/vnd.rar", "RAR archive", ".rar"),
    RTF("application/rtf", "Rich Text Format (RTF)", ".rtf"),
    TAR("application/x-tar", "Tape Archive (TAR)", ".tar"),
    VSD("application/vnd.visio", "Microsoft Visio", ".vsd"),
    XHTML("application/xhtml+xml", "XHTML", ".xhtml"),
    XLS("application/vnd.ms-excel", "Microsoft Excel", ".xls"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Microsoft Excel (OpenXML)", ".xlsx"),
    ZIP("application/zip", "ZIP archive", ".zip"),

    /* fall-back */
    UNKNOWN("application/octet-stream", "unknown data format");

    private final String mediaType;
    private final String description;
    private final Type type;
    private final String subType;
    private final List<String> fileEndings;

    MimeType(final String definition, final String description, final String... endings) {
        mediaType = definition;
        this.description = description;
        type = Type.getEnum(definition);
        subType = definition.split("/")[1];
        fileEndings = Arrays.asList(endings);
    }

    /**
     * @return the commonly defined file-endings for the given MIME type
     */
    public List<String> getFileEndings() {
        return fileEndings;
    }

    /**
     * @return the specific media sub-type, such as "plain" or "png", "mpeg", "mp4"
     *         or "xml".
     */
    public String getSubType() {
        return subType;
    }

    /**
     * @return the high-level media type, such as "text", "image", "audio", "video",
     *         or "application".
     */
    public Type getType() {
        return type;
    }

    public boolean isImageData() {
        return Type.IMAGE.equals(this.getType());
    }

    public boolean isNonDisplayableData() {
        return !isImageData() && !isVideoData();
    }

    public boolean isTextData() {
        return Type.TEXT.equals(this.getType());
    }

    public boolean isVideoData() {
        return Type.VIDEO.equals(this.getType());
    }

    @Override
    public String toString() {
        return mediaType;
    }

    /**
     * @return human-readable description of the format
     */
    public String getDescription() {
        return description;
    }

    /**
     * Case-insensitive mapping between MIME-type string and enumumeration value.
     *
     * @param mimeType the string equivalent mime-type, e.g. "image/png"
     * @return the enumeration equivalent mime-type, e.g. MimeType.PNG or
     *         MimeType.UNKNOWN as fall-back
     */
    public static MimeType getEnum(final String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return UNKNOWN;
        }

        final String trimmed = mimeType.toLowerCase(Locale.UK).trim();
        for (MimeType mType : MimeType.values()) {
            // N.B.trimmed can contain several MIME types, e.g "image/webp,image/apng,image/*"
            if (trimmed.contains(mType.mediaType)) {
                return mType;
            }
        }
        return UNKNOWN;
    }

    /**
     * Case-insensitive mapping between MIME-type string and enumeration value.
     *
     * @param fileName the string equivalent mime-type, e.g. "image/png"
     * @return the enumeration equivalent mime-type, e.g. MimeType.PNG or
     *         MimeType.UNKNOWN as fall-back
     */
    public static MimeType getEnumByFileName(final String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return UNKNOWN;
        }

        final String trimmed = fileName.toLowerCase(Locale.UK).trim();
        for (MimeType mType : MimeType.values()) {
            for (String ending : mType.getFileEndings()) {
                if (trimmed.endsWith(ending)) {
                    return mType;
                }
            }
        }

        return UNKNOWN;
    }

    public enum Type {
        AUDIO("audio"),
        IMAGE("image"),
        VIDEO("video"),
        TEXT("text"),
        APPLICATION("application"),
        UNKNOWN("unknown");

        private final String typeDef;

        Type(final String subType) {
            typeDef = subType;
        }

        @Override
        public String toString() {
            return typeDef;
        }

        public static Type getEnum(final String type) {
            if (type == null || type.isBlank()) {
                return UNKNOWN;
            }
            final String stripped = type.split("/")[0];
            for (Type mSubType : Type.values()) {
                if (mSubType.typeDef.equalsIgnoreCase(stripped)) {
                    return mSubType;
                }
            }
            return UNKNOWN;
        }
    }
}
