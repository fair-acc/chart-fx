package io.fair_acc.sample.financial.dos;

import java.util.*;

import io.fair_acc.sample.financial.service.period.EodPeriod;
import io.fair_acc.sample.financial.service.period.Period;
import org.apache.commons.lang3.time.DateUtils;

import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModel;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcv;

/**
 * Basic example OHLCV domain object implementation for chart samples.
 * There is a direct implementation of API IOhlcv. In the real implementation is necessary to build
 * middle layer which implements IOhlcv API and encapsulate real OHLCV implementation of your financial framework.
 */
public class DefaultOHLCV implements IOhlcv {
    private OHLCVItem[] ohlcvItems = new OHLCVItem[0];

    private String id;
    private String name;
    private String title;
    private String description;
    private String assetName;
    private String symbol;
    private String studyCategory;
    private Period period = new EodPeriod(); // DAILY
    private OHLCVStateAttributes stateAttributes = new OHLCVStateAttributes();
    private AttributeModel addon;

    public void setAddon(AttributeModel addon) {
        this.addon = addon;
    }

    @Override
    public AttributeModel getAddon() {
        return addon;
    }

    @Override
    public AttributeModel getAddonOrCreate() {
        if (addon == null) {
            addon = new AttributeModel();
        }
        return addon;
    }

    public boolean isConsolidated() {
        return stateAttributes.consolidated;
    }

    public void setConsolidated(boolean consolidated) {
        this.stateAttributes.consolidated = consolidated;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStudyCategory() {
        return studyCategory;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getSymbol() {
        return symbol;
    }

    public Period getPeriod() {
        return period;
    }

    @Override
    public OHLCVItem getOhlcvItem(int sampleId) {
        return ohlcvItems[sampleId + stateAttributes.lowerBound];
    }

    public OHLCVItem getBackOhlcvItem(int sampleId) {
        return getOhlcvItem(size() - 1 - sampleId);
    }

    public OHLCVItem[] getOhlcvItemsArray() {
        if (stateAttributes.lowerBound == 0 && stateAttributes.userUpperBound == -1 && stateAttributes.upperBound == -1) {
            return ohlcvItems;
        }
        OHLCVItem[] newItemArray = new OHLCVItem[stateAttributes.size];
        System.arraycopy(ohlcvItems, stateAttributes.lowerBound, newItemArray, 0, stateAttributes.size);

        return newItemArray;
    }

    public List<OHLCVItem> getOhlcvItemsList() {
        return Arrays.asList(getOhlcvItemsArray());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Iterator iterator() {
        if (stateAttributes.upperBound == -1) {
            return new ArrayIterator(ohlcvItems, stateAttributes.lowerBound);
        } else {
            return new ArrayIterator(ohlcvItems, stateAttributes.lowerBound, stateAttributes.upperBound);
        }
    }

    @Override
    public int size() {
        return stateAttributes.size;
    }

    public Date getTimestamp(int sampleId) {
        return ohlcvItems[sampleId + stateAttributes.lowerBound].getTimeStamp();
    }

    public double getValue(int sampleId) {
        // as simple value is used CLOSE value of OHLC
        return ohlcvItems[sampleId + stateAttributes.lowerBound].getClose();
    }

    public double getBackValue(int sampleId) {
        return getValue(size() - 1 - sampleId);
    }

    public OHLCVItem getLastValue() {
        return ohlcvItems[stateAttributes.lastValueIndex];
    }

    public OHLCVItem getOhlcvItemByTimestamp(Date timestamp, final int field) {
        // usage of binary search
        int index = getIndexByTimestamp(timestamp, field);
        if (index < 0) {
            return null;
        }
        return ohlcvItems[index + stateAttributes.lowerBound];
    }

    public synchronized int getIndexByTimestamp(Date timestamp, final int field) {
        OHLCVItem item = new OHLCVItem(timestamp, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        if (ohlcvTimestampComparator == null || ohlcvTimestampComparator.field != field) {
            ohlcvTimestampComparator = new OhlcvTimestampComparator(field);
        }
        // usage of binary search
        int i = Arrays.binarySearch(ohlcvItems, item, ohlcvTimestampComparator);
        // solve boundaries
        if (i < 0) {
            if (Math.abs(i) - 1 == ohlcvItems.length) {
                return -1 * (stateAttributes.size + 1);
            }
            return -(-i - stateAttributes.lowerBound);
        }
        i = i - stateAttributes.lowerBound;
        if (i >= stateAttributes.size) {
            return -1 * (stateAttributes.size + 1);
        }
        return i;
    }

    //------------------------------ setters ------------------------------

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStudyCategory(String studyCategory) {
        this.studyCategory = studyCategory;
    }

    public void setOhlcvItems(OHLCVItem[] ohlcvItems) {
        this.ohlcvItems = ohlcvItems;
        datasetChanged();
    }

    public void addOhlcvItems(List<OHLCVItem> ohlcvItems) {
        addOhlcvItems(ohlcvItems.toArray(new OHLCVItem[ohlcvItems.size()]));
    }

    public void addOhlcvItems(OHLCVItem[] _ohlcvItems) {
        OHLCVItem[] newOhlcvItems = new OHLCVItem[ohlcvItems.length + _ohlcvItems.length];
        System.arraycopy(ohlcvItems, 0, newOhlcvItems, 0, ohlcvItems.length);
        for (int i = 0; i < _ohlcvItems.length; i++) {
            newOhlcvItems[ohlcvItems.length + i] = _ohlcvItems[i];
        }
        ohlcvItems = newOhlcvItems;
        minMaxValueChanged(ohlcvItems);
        datasetChanged();
    }

    public void addOhlcvItem(OHLCVItem ohlcvItem) {
        OHLCVItem[] newOhlcvItems = new OHLCVItem[ohlcvItems.length + 1];
        System.arraycopy(ohlcvItems, 0, newOhlcvItems, 0, ohlcvItems.length);
        newOhlcvItems[ohlcvItems.length] = ohlcvItem;
        ohlcvItems = newOhlcvItems;
        minMaxValueChanged(ohlcvItem);
        datasetChanged();
    }

    private void minMaxValueChanged(OHLCVItem[] ohlcvItems) {
        for (OHLCVItem ohlcvItem : ohlcvItems) {
            minMaxValueChanged(ohlcvItem);
        }
    }

    private void minMaxValueChanged(OHLCVItem ohlcvItem) {
        double high = ohlcvItem.getHigh();
        double low = ohlcvItem.getLow();
        if (high > stateAttributes.maxValue || Double.isNaN(stateAttributes.maxValue)) {
            stateAttributes.maxValue = high;
        }
        if (low < stateAttributes.minValue || Double.isNaN(stateAttributes.minValue)) {
            stateAttributes.minValue = low;
        }
    }

    public void updateOhlcvItem(int index, OHLCVItem ohlcvItem) {
        ohlcvItems[index] = ohlcvItem;
        minMaxValueChanged(ohlcvItem);
    }

    public void setMinValue(double minValue) {
        stateAttributes.minValue = minValue;
    }

    public void setMaxValue(double maxValue) {
        stateAttributes.maxValue = maxValue;
    }

    public double getMinValue() {
        return stateAttributes.minValue;
    }

    public double getMaxValue() {
        return stateAttributes.maxValue;
    }

    //------------------- IOhlcvRangeSupport ------------------------

    public static class OHLCVStateAttributes {
        public boolean consolidated = false;
        public int lowerBound = 0;
        public int upperBound = -1;
        public int userUpperBound = -1;
        public int size;
        public int lastValueIndex;
        public double minValue = Double.NaN;
        public double maxValue = Double.NaN;

        @Override
        public String toString() {
            return "OHLCVStateAttributes [consolidated=" + consolidated + ", lowerBound="
                    + lowerBound + ", upperBound=" + upperBound + ", userUpperBound=" + userUpperBound + ", size=" + size + ", lastValueIndex="
                    + lastValueIndex + ", minValue=" + minValue + ", maxValue=" + maxValue + "]";
        }
    }

    public static IOhlcv createShellCopyRangedOHLCV(DefaultOHLCV rOHLCV) {
        DefaultOHLCV rangedOHLCV = new DefaultOHLCV();
        rangedOHLCV.setAssetName(rOHLCV.assetName);
        rangedOHLCV.setDescription(rOHLCV.title);
        rangedOHLCV.setId(rOHLCV.title);
        rangedOHLCV.setName(rOHLCV.title);
        rangedOHLCV.setSymbol(rOHLCV.symbol);
        rangedOHLCV.setTitle(rOHLCV.title);
        rangedOHLCV.setOhlcvItems(rOHLCV.ohlcvItems);
        rangedOHLCV.stateAttributes = rOHLCV.stateAttributes;

        return rangedOHLCV;
    }

    public static IOhlcv createOHLCV(
            String assetName, String symbol, String title, OHLCVItem[] ohlcvItems, double minValue, double maxValue) {
        DefaultOHLCV rangedOHLCV = new DefaultOHLCV();

        rangedOHLCV.setAssetName(assetName);
        rangedOHLCV.setDescription(title);
        rangedOHLCV.setId(title);
        rangedOHLCV.setName(title);
        rangedOHLCV.setSymbol(symbol);
        rangedOHLCV.setTitle(title);
        rangedOHLCV.setOhlcvItems(ohlcvItems);
        rangedOHLCV.setMinValue(minValue);
        rangedOHLCV.setMaxValue(maxValue);

        return rangedOHLCV;
    }

    public IOhlcv createRestrictedOHLCV(int[] range) {
        DefaultOHLCV rangedOHLCV = new DefaultOHLCV();
        rangedOHLCV.stateAttributes.lowerBound = range[0];
        rangedOHLCV.stateAttributes.upperBound = range[1];

        rangedOHLCV.setAssetName(assetName);
        rangedOHLCV.setDescription(description);
        rangedOHLCV.setId(id);
        rangedOHLCV.setName(name);
        rangedOHLCV.setSymbol(symbol);
        rangedOHLCV.setTitle(title);
        rangedOHLCV.setConsolidated(stateAttributes.consolidated);

        rangedOHLCV.setOhlcvItems(ohlcvItems); // shell copy!

        return rangedOHLCV;
    }

    public void setUserUpperBound(int upper) {
        this.stateAttributes.userUpperBound = upper + stateAttributes.lowerBound;
        datasetChanged();
    }

    public int getUserUpperBound() {
        return stateAttributes.userUpperBound - stateAttributes.lowerBound;
    }

    public void clearUserUpperBound() {
        stateAttributes.userUpperBound = -1;
        datasetChanged();
    }

    public int getLowerBound() {
        return stateAttributes.lowerBound;
    }

    public int getUpperBound() {
        return stateAttributes.upperBound;
    }

    public void increaseUserUpperBound(int i) {
        stateAttributes.userUpperBound += i;
        datasetChanged();
    }

    protected void datasetChanged() {
        if (stateAttributes.userUpperBound == -1) {
            if (stateAttributes.upperBound == -1) {
                stateAttributes.size = ohlcvItems.length - stateAttributes.lowerBound;

            } else {
                stateAttributes.size = stateAttributes.upperBound + 1 - stateAttributes.lowerBound;
            }
        } else {
            stateAttributes.size = stateAttributes.userUpperBound + 1 - stateAttributes.lowerBound;
        }
        stateAttributes.lastValueIndex = stateAttributes.size - 1 + stateAttributes.lowerBound;
    }

    //-----------------------------------------------------------------

    private OhlcvTimestampComparator ohlcvTimestampComparator = null;

    private class OhlcvTimestampComparator implements Comparator<OHLCVItem> {
        public int field;

        public OhlcvTimestampComparator(int field) {
            this.field = field;
        }

        @Override
        public int compare(OHLCVItem o1, OHLCVItem o2) {
            return DateUtils.truncatedCompareTo(o1.getTimeStamp(), o2.getTimeStamp(), field);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assetName == null) ? 0 : assetName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultOHLCV other = (DefaultOHLCV) obj;
        if (assetName == null) {
            if (other.assetName != null)
                return false;
        } else if (!assetName.equals(other.assetName))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (period == null) {
            if (other.period != null)
                return false;
        } else if (!period.equals(other.period))
            return false;
        if (symbol == null) {
            if (other.symbol != null)
                return false;
        } else if (!symbol.equals(other.symbol))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OHLCV [id=" + id + ", name=" + name + ", title=" + title + ", assetName=" + assetName + ", symbol=" + symbol + ", period=" + period + "]";
    }
}
