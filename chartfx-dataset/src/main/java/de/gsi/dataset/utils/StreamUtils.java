package de.gsi.dataset.utils;

import java.io.*;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamUtils {
    private static final Logger logger = LoggerFactory.getLogger(StreamUtils.class);

    public static final String CLASSPATH_PREFIX = "classpath:";
    public static final String ZIP_PREFIX = "zip:";

    /**
     * Get the resource from the file or the jar package
     * From jar file has to be used prefix "classpath:"
     * Zip file resource with prefix: "zip:"
     * @param source resource
     * @return input stream
     * @throws FileNotFoundException - if the file not found
     */
    public static InputStream getInputStream(String source) throws FileNotFoundException {
        InputStream is;
        if (source.startsWith(CLASSPATH_PREFIX)) {
            String resource = source.substring(CLASSPATH_PREFIX.length());
            is = StreamUtils.class.getClassLoader().getResourceAsStream(resource);

        } else if (source.startsWith(ZIP_PREFIX)) {
            String resource = source.substring(ZIP_PREFIX.length());
            int zipSuffixIdx = resource.toLowerCase().indexOf(".zip") + 4;
            String resourceInZip = resource.substring(zipSuffixIdx);
            if (resourceInZip.startsWith("/")) {
                resourceInZip = resourceInZip.substring(1);
            }
            resource = resource.substring(0, zipSuffixIdx);
            try (ZipFile zipFile = new ZipFile(resource)) {
                // do not close zip, it is closed by input stream!
                is = zipFile.getInputStream(zipFile.getEntry(resourceInZip));
            } catch (IOException e) {
                throw new FileNotFoundException(
                        "Zip resource not found for " + source + " IOException=" + e.getMessage());
            }
        } else {
            is = new FileInputStream(new File(source));
        }
        if (is == null) {
            logger.error("The resource is not found: " + source);
        }
        return is;
    }
}
