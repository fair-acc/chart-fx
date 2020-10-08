package de.gsi.dataset.remote;

import java.io.Serializable;

/**
 * Simple data storage container for net-based file/data-transfers.
 * 
 * Please note, this container classes stores direct references.
 * 
 * @author rstein
 *
 */
@SuppressWarnings("PMD.ArrayIsStoredDirectly")
public class Data implements Serializable {
    private static final long serialVersionUID = 415405233360249553L;
    private final String exportNameData;
    private final String mimeType;
    private final byte[] dataByteArray;
    private final int dataByteArraySize;

    public Data(final String exportNameData, final String mimeType, final byte[] dataByteArray, final int dataByteArraySize) {
        if (exportNameData == null || exportNameData.isBlank()) {
            throw new IllegalArgumentException("exportNameData must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType must not be blank");
        }
        if (dataByteArray == null || dataByteArray.length < dataByteArraySize) {
            throw new IllegalArgumentException("dataByteArray[" + (dataByteArray == null ? "null" : dataByteArray.length) + "] must be larger than dataByteArraySize=" + dataByteArraySize);
        }
        this.exportNameData = exportNameData;
        this.mimeType = mimeType;
        this.dataByteArray = dataByteArray;
        this.dataByteArraySize = dataByteArraySize;
    }

    public byte[] getDataByteArray() {
        return dataByteArray; // NOPMD -- array is stored directly
    }

    public int getDataByteArraySize() {
        return dataByteArraySize;
    }

    public String getExportNameData() {
        return exportNameData;
    }

    protected String getMimeType() {
        return mimeType;
    }
}