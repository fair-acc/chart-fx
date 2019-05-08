/*****************************************************************************
 * *
 * Common Chart - static data set utilities *
 * *
 * modified: 2018-08-27 Harald Braeuning *
 * modified: 2019-04-01 Ralph Steinhagen - added CSV and error parsing routines
 * *
 ****************************************************************************/

package de.gsi.chart.utils;

import static de.gsi.chart.utils.DataSetUtils.ErrType.EYN;
import static de.gsi.chart.utils.DataSetUtils.ErrType.EYP;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
 */
public class DataSetUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetUtils.class);
    private static final String DEFAULT_TIME_FORMAT = "yyyyMMdd_HHmmss";
    private static final String FILE_LOGGING_SUFFIX = ".csv";

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
        EXN,
        EXP,
        EYN,
        EYP;
    }

    /**
     * convenience short-hand notation for getting error variables (if defined for dataset)
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param index the data set index
     * @return the given error
     */
    public static double error(final DataSet dataSet, final ErrType eType, final int index) {
        return error(dataSet, eType, index, 0.0, false);
    }

    /**
     * convenience short-hand notation for getting error variables (if defined for dataset)
     *
     * @param dataSet the source data set
     * @param eType the error type
     * @param x the data set x-value for which the error should be interpolated
     * @return the given interpolated error
     */
    public static double error(final DataSet dataSet, final ErrType eType, final double x) {
        return error(dataSet, eType, -1, x, true);
    }

    // convenience short-hand notation for getting error variables (if defined for dataset)
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

    protected static double[] cropToLength(final double[] in, final int length) {
        // small helper routine to crop data array in case it's to long
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

    public static String getISODate(final long time_ms, final String format) {
        final long time = TimeUnit.MILLISECONDS.toMillis(time_ms);
        final TimeZone tz = TimeZone.getTimeZone("UTC");
        final DateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(tz);
        return df.format(new Date(time));
    }

    public enum Compression {
        AUTO,
        GZIP,
        NONE
    }

    private static PrintWriter openPrintWriter(final File file, final Compression compression) throws Exception {
        switch (compression) {
        case NONE:
            return new PrintWriter(file);
        case GZIP:
            return new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
        default:
            throw new IllegalArgumentException("Unknown Compression format: " + compression.toString());
        }
    }

    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName,
            final Compression compression) {
        return writeDataSetToFile(dataSet, path, fileName, -1, null, compression);
    }

    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName) {
        return writeDataSetToFile(dataSet, path, fileName, -1, null, Compression.AUTO);
    }

    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName,
            final long userTimeStampMillis, final Compression compression) {
        return writeDataSetToFile(dataSet, path, fileName, userTimeStampMillis, null, compression);
    }

    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName,
            final long userTimeStampMillis) {
        return writeDataSetToFile(dataSet, path, fileName, userTimeStampMillis, null, Compression.AUTO);
    }

    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName,
            final long userTimeStampMillis, final String timeFormatString) {
        return writeDataSetToFile(dataSet, path, fileName, userTimeStampMillis, timeFormatString, Compression.AUTO);
    }

    public static String writeDataSetToFile(final DataSet dataSet, final Path path, final String fileName,
            final long userTimeStampMillis, final String timeFormatString, Compression compression) {
        final long now = System.currentTimeMillis();
        final long timeStampMillis = userTimeStampMillis <= 0 ? now : userTimeStampMillis;

        if (compression == Compression.AUTO) {
            if (fileName.toLowerCase().endsWith(".gz")) {
                compression = Compression.GZIP;
            } else {
                compression = Compression.NONE;
            }
        }

        if (dataSet == null) {
            throw new IllegalArgumentException("dataSet must not be null or empty");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }

        try {
            final String format = timeFormatString != null && !timeFormatString.isEmpty() ? timeFormatString
                    : DEFAULT_TIME_FORMAT;
            final String longFileName = userTimeStampMillis > 0 && format != null && !format.isEmpty()
                    ? path.toFile() + String.format("/%s_%s%s", fileName.replaceAll(FILE_LOGGING_SUFFIX, ""),
                            getISODate(timeStampMillis, format), FILE_LOGGING_SUFFIX)
                    : path.toFile() + "/" + fileName;
            final String tempFileName = longFileName + "_temp" + FILE_LOGGING_SUFFIX;
            final File file = new File(tempFileName);
            if (file.getParentFile().mkdirs()) {
                LOGGER.info("needed to create directory for file: " + longFileName);
            }

            // create PrintWriter
            try (PrintWriter outputfile = openPrintWriter(file, compression);) {
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

            Files.move(Paths.get(tempFileName), Paths.get(longFileName), REPLACE_EXISTING);
            LOGGER.debug("write data set '" + dataSet.getName() + "' to " + tempFileName + " -> " + longFileName);

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

    public static DataSet readDataSetFromFile(final String fileName) {
        return readDataSetFromFile(fileName, Compression.AUTO);
    }

    private static BufferedReader openBufferedReader(final String fileName, final Compression compression)
            throws Exception {
        switch (compression) {
        case GZIP:
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
        case NONE:
            return new BufferedReader(new FileReader(fileName));
        default:
            throw new IllegalArgumentException("Unknown Compression format: " + compression.toString());
        }
    }

    public static DataSet readDataSetFromFile(final String fileName, Compression compression) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }
        if (compression == Compression.AUTO) {
            if (fileName.toLowerCase().endsWith(".gz")) {
                compression = Compression.GZIP;
            } else {
                compression = Compression.NONE;
            }
        }
        DoubleErrorDataSet dataSet = null;
        try {
            final DataSetUtils x = new DataSetUtils();
            try (BufferedReader inputFile = openBufferedReader(fileName, compression);) {
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
                LOGGER.error("could not open/parse file: '" + fileName + "'", e);
            }
        } catch (final Exception e) {
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
