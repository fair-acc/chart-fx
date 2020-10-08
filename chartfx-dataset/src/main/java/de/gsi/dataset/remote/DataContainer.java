package de.gsi.dataset.remote;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Data storage container to store image and other (primarily) binary data
 * 
 * @author rstein
 */
@SuppressWarnings("PMD.DataClass") // yes it's a data storage class with no added functionality
public class DataContainer implements Serializable {
    private static final long serialVersionUID = -4443375672892579564L;
    private String selector; // N.B. first, so that selector can be de-serialised early on
    private String exportName;
    private String category;
    private long updatePeriod;
    private List<Data> data;
    private String rbacToken;
    // end data container
    private long timeStampCreation;
    private long timeStampLastAccess;

    public DataContainer(final String exportNameData, final long updatePeriod, final byte[] imageByteArray, final int imageByteArraySize) {
        this(genExportName(exportNameData), getCategory(exportNameData), updatePeriod, //
                new Data(genExportNameData(exportNameData), MimeType.getEnumByFileName(exportNameData).toString(), imageByteArray, imageByteArraySize));
    }

    public DataContainer(final String exportName, final String category, final long updatePeriod, final Data... data) {
        this.exportName = checkField("exportName", exportName);
        this.category = fixPreAndPost(checkField("category", category));
        this.updatePeriod = updatePeriod;
        this.data = Collections.unmodifiableList(Arrays.asList(data));

        // using deliberately internal server time, since we generate cookies and long-polling out of this
        timeStampCreation = System.currentTimeMillis();
        timeStampLastAccess = timeStampCreation;
    }

    public String getCategory() {
        updateAccess();
        return category;
    }

    public List<Data> getData() {
        updateAccess();
        return data;
    }

    /**
     * @return convenience method
     */
    public byte[] getDataByteArray() {
        return getData().get(0).getDataByteArray();
    }

    /**
     * @return convenience method
     */
    public int getDataByteArraySize() {
        return getData().get(0).getDataByteArraySize();
    }

    public String getExportName() {
        return exportName;
    }

    /**
     * @return convenience method
     */
    public String getExportNameData() {
        return getData().get(0).getExportNameData();
    }

    /**
     * @return convenience method
     */
    public String getMimeType() {
        return getData().get(0).getMimeType();
    }

    public String getRbacToken() {
        return rbacToken;
    }

    public String getSelector() {
        return selector;
    }

    public long getTimeStampCreation() {
        return timeStampCreation;
    }

    public String getTimeStampCreationString() {
        final TimeZone utcTz = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK);
        formatter.setTimeZone(utcTz);
        return formatter.format(new Date(timeStampCreation));
    }

    public long getTimeStampLastAccess() {
        return timeStampLastAccess;
    }

    public long getUpdatePeriod() {
        return updatePeriod;
    }

    public void setRbacToken(String rbacToken) {
        updateAccess();
        this.rbacToken = rbacToken;
    }

    public void setSelector(String selector) {
        updateAccess();
        this.selector = selector;
    }

    public void updateAccess() {
        timeStampLastAccess = System.currentTimeMillis();
    }

    protected static String fixPreAndPost(final String name) {
        final String nonNullName = name == null ? "/" : name.trim();
        final String fixedPrefix = (nonNullName.startsWith("/") ? nonNullName : '/' + nonNullName);
        return fixedPrefix.endsWith("/") ? fixedPrefix : fixedPrefix + '/';
    }

    protected static String genExportName(final String name) {
        checkField("genExportName(name)", name);
        int p = name.lastIndexOf('/');
        if (p < 0) {
            p = 0;
        }
        int e = name.lastIndexOf('.');
        if (e < 0) {
            e = name.length();
        }
        return name.substring(p, e).replace("/", "");
    }

    protected static String genExportNameData(final String name) {
        checkField("genExportNameData(name)", name);
        int p = name.lastIndexOf('/');
        if (p < 0) {
            p = 0;
        }
        return name.substring(p, name.length()).replace("/", "");
    }

    protected static String getCategory(final String name) {
        checkField("getCategory(name)", name);
        final int p = name.lastIndexOf('/');
        if (p < 0) {
            return fixPreAndPost("");
        }
        return fixPreAndPost(name.substring(0, p + 1));
    }

    private static String checkField(final String field, final String category) {
        if (category == null) {
            throw new IllegalArgumentException(field + "category not be null");
        }
        if (category.isBlank()) {
            throw new IllegalArgumentException(field + "must not be blank");
        }
        return category;
    }
}