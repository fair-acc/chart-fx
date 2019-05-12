/********************************************************************************
 * * Common Chart - static data set utilities *<br>
 * modified: 2018-08-27 Harald Braeuning *<br>
 * modified: 2019-04-01 Ralph Steinhagen - added CSV and error parsing routines *
 ********************************************************************************/

package de.gsi.chart.utils;

import static de.gsi.chart.utils.DataSetUtils.ErrType.EYN;
import static de.gsi.chart.utils.DataSetUtils.ErrType.EYP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.DataSetError;
import de.gsi.chart.data.DataSetMetaData;
import de.gsi.chart.data.spi.AbstractDataSet;
import de.gsi.chart.data.spi.DefaultDataSet;
import de.gsi.chart.data.spi.DoubleDataSet;
import de.gsi.chart.data.spi.DoubleErrorDataSet;
import de.gsi.math.DataSetMath;
import de.gsi.math.TMath;

/**
 * @author braeun
 * @author rstein
 * @author akrimm
 */
public class DataSetUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetUtils.class);
    private static final String DEFAULT_TIME_FORMAT = "yyyyMMdd_HHmmss";

    private DataSetUtils() {
        // static class nothing to be initialised
    }

    public static AbstractDataSet<?> copyDataSet(final DataSet ds) {
        if (ds instanceof DoubleDataSet) {
            final DoubleDataSet dds = (DoubleDataSet) ds;
            final DoubleDataSet d = new DoubleDataSet(dds.getName(), dds.getXValues(), dds.getYValues(), true);
            d.setStyle(dds.getStyle());
            d.getDataLabelProperty().putAll(dds.getDataLabelProperty());
            d.getDataStyleProperty().putAll(dds.getDataStyleProperty());
            return d;
        }
        if (ds instanceof DefaultDataSet) {
            final DefaultDataSet dds = (DefaultDataSet) ds;
            final DefaultDataSet d = new DefaultDataSet(dds.getName());
            d.setStyle(dds.getStyle());
            d.getDataProperty().setAll(dds.getDataProperty());
            d.getDataLabelProperty().putAll(dds.getDataLabelProperty());
            d.getDataStyleProperty().putAll(dds.getDataStyleProperty());
            return d;
        }
        final double[] x = new double[ds.getDataCount()];
        final double[] y = new double[ds.getDataCount()];
        for (int i = 0; i < ds.getDataCount(); i++) {
            x[i] = ds.getX(i);
            y[i] = ds.getY(i);
        }
        final DoubleDataSet d = new DoubleDataSet(ds.getName(), x, y);
        d.setStyle(ds.getStyle());
        for (int i = 0; i < ds.getDataCount(); i++) {
            final String style = ds.getStyle(i);
            if (style != null) {
                d.addDataStyle(i, style);
            }
            final String label = ds.getDataLabel(i);
            if (label != null) {
                d.addDataLabel(i, label);
            }
        }
        return d;
    }

    public enum ErrType {
        /**
         * Negative x error
         */
        EXN,
        /**
         * Positive x error
         */
        EXP,
        /**
         * Negative y error
         */
        EYN,
        /**
         * Positive y error
         */
        EYP;
    }

    /**
     * convenience short-hand notation for getting error variable at a specific data point index. Returns 0.0 if no
     * errors are defined.
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param index the data set index
     * @return the given error at data point "index"
     */
    public static double error(final DataSet dataSet, final ErrType eType, final int index) {
        return error(dataSet, eType, index, 0.0, false);
    }

    /**
     * convenience short-hand notation for getting error variable at an exact x value. Returns 0.0 if no errors are
     * defined.
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param x the data set x-value for which the error should be interpolated
     * @return the given interpolated error
     */
    public static double error(final DataSet dataSet, final ErrType eType, final double x) {
        return error(dataSet, eType, -1, x, true);
    }

    /**
     * convenience short-hand notation for getting error variables. Returns 0.0 if no errors are defined.
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param index the data set index
     * @param x the data set x-value for which the error should be interpolated
     * @param interpolate determines if the value at index or the interpolated value at x should be returned
     * @return the given error
     */
    protected static double error(final DataSet dataSet, final ErrType eType, final int index, final double x,
            final boolean interpolate) {
        if (!(dataSet instanceof DataSetError)) {
            // data set does not have any error definition
            return 0.0;
        }
        final DataSetError ds = (DataSetError) dataSet;
        if (interpolate) {
            switch (eType) {
            case EXN:
                return ds.getXErrorNegative(x);
            case EXP:
                return ds.getXErrorPositive(x);
            case EYN:
                return ds.getYErrorNegative(x);
            case EYP:
                return ds.getYErrorPositive(x);
            }
        } else {
            switch (eType) {
            case EXN:
                return ds.getXErrorNegative(index);
            case EXP:
                return ds.getXErrorPositive(index);
            case EYN:
                return ds.getYErrorNegative(index);
            case EYP:
                return ds.getYErrorPositive(index);
            }
        }

        return 0;
    }

    /**
     * small helper routine to crop data array in case it's to long
     *
     * @param in input data array
     * @param length length of output array
     * @return cropped/zero-padded array of size length.
     */
    protected static double[] cropToLength(final double[] in, final int length) {
        if (in.length == length) {
            return in;
        }
        return Arrays.copyOf(in, length);
    }

    /**
     * convenience short-hand notation for getting error variables (if defined for dataset)
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @return the given error array (cropped to data set length if necessary)
     */
    public static double[] errors(final DataSet dataSet, final ErrType eType) {
        final int nDim = dataSet.getDataCount();
        if (!(dataSet instanceof DataSetError)) {
            // data set does not have any error definition
            return new double[nDim];
        }
        final DataSetError ds = (DataSetError) dataSet;
        switch (eType) {
        case EXN:
            return cropToLength(ds.getXErrorsNegative(), nDim);
        case EXP:
            return cropToLength(ds.getXErrorsPositive(), nDim);
        case EYN:
            return cropToLength(ds.getYErrorsNegative(), nDim);
        case EYP:
        default:
            return cropToLength(ds.getYErrorsPositive(), nDim);
        }
    }

    /**
     * Get ISO date from milliseconds since Jan 01, 1970
     *
     * @param time_ms
     * @param format
     * @return ISO formatted UTC datetime string
     */
    public static String getISODate(final long time_ms, final String format) {
        final long time = TimeUnit.MILLISECONDS.toMillis(time_ms);
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final DateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(tz);
        return df.format(new Date(time));
    }

    /**
     * Supported Compression Types
     */
    public enum Compression {
        /**
         * Determine Compression from file extension
         */
        AUTO,
        /**
         * GZIP compression
         */
        GZIP,
        /**
         * ZIP compression. Allways reads from first entry in archive and writes to a new entry with same filename as
         * achive minus zip extension.
         */
        ZIP,
        /**
         * Plaintext csv data
         */
        NONE
    }

    /**
     * Open a PrintWriter that is backed by the appropriate compression and file handling streams.
     *
     * @param file File to open
     * @param compression Compression method
     * @return A ready-to-go writer that is agnostic to the underlying compression method
     * @throws IOException
     */
    private static PrintWriter openPrintWriter(final File file, final Compression compression) throws IOException {
        switch (compression) {
        case NONE:
            return new PrintWriter(file);
        case GZIP:
            return new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
        case ZIP:
            final ZipOutputStream zipOStream = new ZipOutputStream(new FileOutputStream(file));
            final String filename = file.getName();
            final String zipentryname = filename.toLowerCase().endsWith(".zip")
                    ? filename.substring(0, filename.length() - 4)
                    : filename;
            zipOStream.putNextEntry(new ZipEntry(zipentryname));
            return new PrintWriter(zipOStream);
        default:
            throw new IllegalArgumentException("Unknown Compression format: " + compression.toString());
        }
    }

    /**
     * Open a buffered Reader that is backed by the appropriate stream classes for the chosen compression method.
     *
     * @param fileName
     * @param compression
     * @return
     * @throws IOException
     */
    private static BufferedReader openBufferedReader(final File file, final Compression compression)
            throws IOException {
        switch (compression) {
        case ZIP:
            final ZipInputStream zipIStream = new ZipInputStream(new FileInputStream(file));
            if (zipIStream.getNextEntry() == null) {
                throw new ZipException("Corrupt zip archive has no entries");
            }
            return new BufferedReader(new InputStreamReader(zipIStream));
        case GZIP:
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
        case NONE:
            return new BufferedReader(new FileReader(file));
        default:
            throw new IOException("Unimplemented Compression");
        }
    }

    /**
     * Produce a filename from a dataSet and a String that can contain {datafield;type;format} style placeholders.
     * Datafield references a field in the metadata as specified by the metaDataDataSet interface. The special field
     * "systemTime" can be used to use the current system time. Also the standard DataSet fields can be used
     * ("dataSetName", "xMin", "xMax", "yMin", "yMax"). The optional type field supports "string", "date", "int/long"
     * and "float/double", where "string" is the default. The optional format field can be used to provide format
     * strings. The default for date is SimpleDateFormat "yyyyMMdd_HHmmss", for int and float it is printf's "%d" and
     * "%e".
     *
     * @param dataSet A dataSet containing all the data field referenced in the filename pattern
     * @param fileName Filename (with "{metadatafield;type;format}" placeholders for variables)
     * @return The fielname with the placeholders replaced
     */
    public static String getFileName(final DataSet dataSet, final String fileName) {
        final Pattern placeholder = Pattern.compile("\\{([^\\{\\}]*)\\}");
        final Matcher matcher = placeholder.matcher(fileName);
        return matcher.replaceAll(match -> {
            final String[] substitutionparams = match.group(1).split(";");
            if (substitutionparams.length == 0) {
                throw new IllegalArgumentException("fileName contains empty placeholder: " + match.group());
            }
            String value;
            if (substitutionparams[0].equals("systemTime")) {
                value = Long.toString(System.currentTimeMillis());
            } else {
                switch (substitutionparams[0]) {
                case "dataSetName":
                    value = dataSet.getName();
                    break;
                case "xMin":
                    value = Double.toString(dataSet.getXMin());
                    break;
                case "xMax":
                    value = Double.toString(dataSet.getXMax());
                    break;
                case "yMin":
                    value = Double.toString(dataSet.getYMin());
                    break;
                case "yMax":
                    value = Double.toString(dataSet.getYMax());
                    break;
                default:
                    if (!(dataSet instanceof DataSetMetaData)) {
                        throw new IllegalArgumentException(
                                "fileName placeholder references meta data but dataSet is not instanceof DataSetMetaData");
                    }
                    final DataSetMetaData metaDataSet = (DataSetMetaData) dataSet;
                    value = metaDataSet.getMetaInfo().get(substitutionparams[0]);
                    if (value == null) {
                        throw new IllegalArgumentException(
                                "fileName placeholder references nonexisting metaData field: " + substitutionparams[0]);
                    }
                }
            }
            if (substitutionparams.length == 1 || substitutionparams[1].equals("string")) {
                return value;
            }
            String format;
            switch (substitutionparams[1]) {
            case "date":
                format = (substitutionparams.length < 3) ? DEFAULT_TIME_FORMAT : substitutionparams[2];
                return getISODate(Long.valueOf(value), format);
            case "int":
            case "long":
                format = (substitutionparams.length < 3) ? "%d" : substitutionparams[2];
                return String.format(format, Long.valueOf(value));
            case "float":
            case "double":
                format = (substitutionparams.length < 3) ? "%e" : substitutionparams[2];
                return String.format(format, Double.valueOf(value));
            default:
                throw new IllegalArgumentException(
                        "fileName contains placeholder with illegal type: " + substitutionparams[1]);
            }
        });
    }

    /**
     * Determine the compression method from the file extension.
     *
     * @param fileName
     * @return Compression Enum for the extension of the supplied filename. Defaults to Compression.NONE
     */
    private static Compression evaluateAutoCompression(final String fileName) {
        if (fileName.toLowerCase().endsWith(".gz")) {
            return Compression.GZIP;
        }
        if (fileName.toLowerCase().endsWith(".zip")) {
            return Compression.ZIP;
        }
        return Compression.NONE;
    }

    /**
     * Export the contents of the supplied dataSet to file as comma separated values with an additional comment header
     * containing metaData if existent.<br>
     * The filename can contain placeholders of the form {metadatafield;type;format}, where metadatafield references a
     * field in the metadata as specified by the metaDataDataSet interface. The special field systemTime can be used to
     * use the current system time. Also the standard DataSet fields can be used ("dataSetName", "xMin", "xMax", "yMin",
     * "yMax"). The optional type field supports "string", "date", "int/long" and "float/double", where "string" is the
     * default. The optional format field can be used to provide format strings. The default for date is
     * SimpleDateFormat "yyyyMMdd_HHmmss", for int and float it is printf's "%d" and "%e".<br>
     * The compression method is automatically determined from the file extension. <br>
     * The data format is a custom extension of csv with an additional #-commented Metadata Header and a $-commented
     * column header. Expects the following columns in this order to be present: index, x, y, eyn, eyp.
     *
     * @param fileName Path and name of file containing csv data.
     * @param dataSet The DataSet to export
     * @param path Path to the location of the file
     * @param fileName Filename (with "{metadatafield;type;format}" placeholders for variables)
     * @return actual name of the file that was written or none in case of errors
     */
    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName) {
        return writeDataSetToFile(dataSet, path, fileName, Compression.AUTO);
    }

    /**
     * Export the contents of the supplied dataSet to file as comma separated values with an additional comment header
     * containing metaData if existent.<br>
     * The filename can contain placeholders of the form {metadatafield;type;format}, where metadatafield references a
     * field in the metadata as specified by the metaDataDataSet interface. The special field systemTime can be used to
     * use the current system time. Also the standard DataSet fields can be used ("dataSetName", "xMin", "xMax", "yMin",
     * "yMax"). The optional type field supports "string", "date", "int/long" and "float/double", where "string" is the
     * default. The optional format field can be used to provide format strings. The default for date is
     * SimpleDateFormat "yyyyMMdd_HHmmss", for int and float it is printf's "%d" and "%e".<br>
     * The data format is a custom extension of csv with an additional #-commented Metadata Header and a $-commented
     * column header. Expects the following columns in this order to be present: index, x, y, eyn, eyp.
     *
     * @param dataSet The DataSet to export
     * @param path Path to the location of the file
     * @param fileName Filename (with "{metadatafield;type;format}" placeholders for variables)
     * @param compression Compression of the file (GZIP, ZIP or NONE). Supply AUTO or ommit this value to use file
     *            extension.
     * @return actual name of the file that was written or none in case of errors
     */
    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName,
            Compression compression) {
        if (compression == Compression.AUTO) {
            compression = evaluateAutoCompression(fileName);
        }
        if (dataSet == null) {
            throw new IllegalArgumentException("dataSet must not be null or empty");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }

        try {
            final String realFileName = getFileName(dataSet, fileName);
            final String longFileName = path.toFile() + "/" + realFileName;
            final File file = new File(longFileName);
            if (file.getParentFile().mkdirs()) {
                LOGGER.info("needed to create directory for file: " + longFileName);
            }

            // create PrintWriter
            try (PrintWriter outputfile = openPrintWriter(file, compression)) {
                dataSet.lock();

                outputfile.write("#file producer : " + DataSetUtils.class.getCanonicalName());
                outputfile.write('\n');

                writeHeaderDataToFile(outputfile, dataSet);

                writeMetaDataToFile(outputfile, dataSet);

                writeNumericDataToFile(outputfile, dataSet);

                // automatically closing writer connection
            } catch (final IOException e) {
                LOGGER.error("could not write to file: '" + fileName + "'", e);
            } finally {
                dataSet.unlock();
            }

            LOGGER.debug("write data set '" + dataSet.getName() + "' to " + longFileName);

            return longFileName;
        } catch (final Exception e) {
            LOGGER.error("could not write to file: '" + fileName + "'", e);
            return null;
        }
    }

    protected static void writeHeaderDataToFile(final PrintWriter outputFile, final DataSet dataSet) {
        try {
            // common header data
            final StringBuffer buffer = new StringBuffer();

            buffer.append("#dataSetName : ");
            buffer.append(dataSet.getName());
            buffer.append('\n');

            buffer.append("#xMin : ");
            buffer.append(dataSet.getXMin());
            buffer.append('\n');

            buffer.append("#xMax : ");
            buffer.append(dataSet.getXMax());
            buffer.append('\n');

            buffer.append("#yMin : ");
            buffer.append(dataSet.getYMin());
            buffer.append('\n');

            buffer.append("#yMax : ");
            buffer.append(dataSet.getYMax());
            buffer.append('\n');

            try {
                buffer.append("#integral : ");
                buffer.append(DataSetMath.integralSimple(dataSet));
                buffer.append('\n');

                buffer.append("#mean : ");
                buffer.append(TMath.Mean(dataSet.getYValues()));
                buffer.append('\n');

                buffer.append("#rms : ");
                buffer.append(TMath.RMS(dataSet.getYValues()));
                buffer.append('\n');
            } catch (final Exception e) {
                LOGGER.error("writeHeaderDataToFile - compute Math error for dataSet = '" + dataSet.getName() + "'", e);
            }

            outputFile.print(buffer.toString());
        } catch (final Exception e) {
            LOGGER.error("writeHeaderDataToFile - error for dataSet = '" + dataSet.getName() + "'", e);
        }
    }

    protected static void writeMetaDataToFile(final PrintWriter outputFile, final DataSet dataSet) {
        if (!(dataSet instanceof DataSetMetaData)) {
            return;
        }
        final DataSetMetaData metaDataSet = (DataSetMetaData) dataSet;
        try {
            final StringBuffer buffer = new StringBuffer();

            for (final String info : metaDataSet.getInfoList()) {
                buffer.append("#info : ");
                buffer.append(info);
                buffer.append('\n');
            }

            for (final String warning : metaDataSet.getWarningList()) {
                buffer.append("#warning : ");
                buffer.append(warning);
                buffer.append('\n');
            }

            for (final String error : metaDataSet.getErrorList()) {
                buffer.append("#error : ");
                buffer.append(error);
                buffer.append('\n');
            }

            final Map<String, String> map = metaDataSet.getMetaInfo();
            for (final String key : metaDataSet.getMetaInfo().keySet()) {
                buffer.append("#metaKey -");
                buffer.append(key);
                buffer.append(" : ");
                buffer.append(map.get(key));
                buffer.append('\n');
            }

            outputFile.print(buffer.toString());
        } catch (final Exception e) {
            LOGGER.error("writeMetaDataToFile - error for dataSet = '" + dataSet.getName() + "'", e);
        }
    }

    protected static void writeNumericDataToFile(final PrintWriter outputFile, final DataSet dataSet) {
        try {
            // formatter definition, we always write the y errors to file
            final int nSamples = dataSet.getDataCount();
            outputFile.println("#nSamples : " + nSamples);
            // use '$' sign as special indicator that from now on only numeric data is to be expected
            outputFile.println("$index, x, y, eyn, eyp");
            final StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < nSamples; i++) {
                buffer.append(i); // data index
                buffer.append(',');
                buffer.append(dataSet.getX(i)); // x-coordinate
                buffer.append(',');
                buffer.append(dataSet.getY(i)); // y-coordinate
                buffer.append(',');
                buffer.append(error(dataSet, EYN, i)); // negative error in y
                buffer.append(',');
                buffer.append(error(dataSet, EYP, i)); // positive error in y
                buffer.append('\n');
            }
            outputFile.print(buffer.toString());
        } catch (final Exception e) {
            LOGGER.error("writeNumericDataToFile - error for dataSet = '" + dataSet.getName() + "'", e);
        }
    }

    protected static String getKey(final String line, final String replace) {
        if (line == null) {
            return null;
        }
        final String[] split = line.split(" : ");
        if (split.length < 2) {
            return null;
        }
        return split[0].replaceAll(replace, "");
    }

    protected static String getValue(final String line) {
        if (line == null) {
            return null;
        }
        final String[] split = line.split(" : ");
        if (split.length < 2) {
            return null;
        }
        return split[1];
    }

    /**
     * Read a Dataset from a file containing comma separated values.<br>
     * Automatically determines compression from the file extension.<br>
     * The data format is a custom extension of csv with an additional #-commented Metadata Header and a $-commented
     * column header. Expects the following columns in this order to be present: index, x, y, eyn, eyp.
     *
     * @param fileName Path and name of file containing csv data.
     * @return DataSet with the data and metadata read from the file
     */
    public static DataSet readDataSetFromFile(final String fileName) {
        return readDataSetFromFile(fileName, Compression.AUTO);
    }

    /**
     * Read a Dataset from a file containing comma separated values.<br>
     * The data format is a custom extension of csv with an additional #-commented Metadata Header and a $-commented
     * column header. Expects the following columns in this order to be present: index, x, y, eyn, eyp.
     *
     * @param fileName Path and name of file containing csv data.
     * @param compression Compression of the file (GZIP, ZIP or NONE). Supply AUTO or ommit this value to use file
     *            extension.
     * @return DataSet with the data and metadata read from the file
     */
    public static DataSet readDataSetFromFile(final String fileName, Compression compression) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }
        if (compression == Compression.AUTO) {
            compression = evaluateAutoCompression(fileName);
        }
        DoubleErrorDataSet dataSet = null;
        try {
            final File file = new File(fileName);
            try (BufferedReader inputFile = openBufferedReader(file, compression);) {
                String dataSetName = "unknown data set";
                int nDataCountEstimate = 0;
                final ArrayList<String> info = new ArrayList<>();
                final ArrayList<String> warning = new ArrayList<>();
                final ArrayList<String> error = new ArrayList<>();
                final Map<String, String> metaInfoMap = new ConcurrentHashMap<>();

                // skip first file format header
                String line = inputFile.readLine();
                for (; (line = inputFile.readLine()) != null;) {
                    if (line.contains("$")) {
                        break;
                    }

                    if (line.contains("#dataSetName")) {
                        dataSetName = getValue(line);
                    }

                    if (line.contains("#nSamples")) {
                        nDataCountEstimate = Integer.parseInt(getValue(line));
                    }

                    if (line.contains("#info")) {
                        info.add(getValue(line));
                    }

                    if (line.contains("#warning")) {
                        warning.add(getValue(line));
                    }

                    if (line.contains("#error")) {
                        error.add(getValue(line));
                    }

                    if (line.contains("#metaKey -")) {
                        final String key = getKey(line, "#metaKey -");
                        final String value = getValue(line);
                        metaInfoMap.put(key, value);
                    }

                }
                dataSet = new DoubleErrorDataSet(dataSetName, nDataCountEstimate);
                dataSet.getMetaInfo().putAll(metaInfoMap);

                dataSet.getInfoList();

                readNumericDataFromFile(inputFile, dataSet);

                // automatically closing writer connection
            } catch (final IOException e) {
                e.printStackTrace();
                LOGGER.error("could not open/parse file: '" + fileName + "'", e);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            LOGGER.error("could not open/parse file: '" + fileName + "'", e);
            return dataSet;
        }
        return dataSet;
    }

    protected static void readNumericDataFromFile(final BufferedReader inputFile, final DoubleErrorDataSet dataSet) {
        try {
            String line = inputFile.readLine();
            for (; (line = inputFile.readLine()) != null;) {
                final String[] parse = line.split(",");
                if (parse.length == 0) {
                    continue;
                }

                final long index = Long.parseLong(parse[0]); // ignored only for cross-checks
                final double x = Double.parseDouble(parse[1]);
                final double y = Double.parseDouble(parse[2]);
                final double eyn = parse.length <= 5 ? 0.0 : Double.parseDouble(parse[3]);
                final double eyp = parse.length <= 5 ? 0.0 : Double.parseDouble(parse[4]);
                dataSet.add(x, y, eyn, eyp);
            }
        } catch (

        final Exception e) {
            LOGGER.error("readNumericDataFromFile could not parse numeric data for: '" + dataSet.getName() + "'", e);
        }
    }

}
