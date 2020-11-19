package de.gsi.dataset.spi.financial;

import java.util.Iterator;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.AbstractDataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

/**
 * Dataset Domain Object for encapsulation of IOhlcv API structure.
 *
 * @see IOhlcv API provides OHLCV domain object access.
 * @see IOhlcvItem API provides OHLCV item domain object access.
 */
public class OhlcvDataSet extends AbstractDataSet<OhlcvDataSet> implements Iterable<IOhlcvItem> {
    public static final int DIM_Y_OPEN = 1;
    public static final int DIM_Y_HIGH = 2;
    public static final int DIM_Y_LOW = 3;
    public static final int DIM_Y_CLOSE = 4;
    public static final int DIM_Y_VOLUME = 5;
    public static final int DIM_Y_OI = 6;

    private IOhlcv ohlcv;

    private boolean isCategoryBased = false;

    public OhlcvDataSet(String title) {
        super(title, 2);
    }

    /**
     * Set the domain object with OHLCV data.
     * @param ohlcv connection with OHLCV data from financial framework.
     */
    public void setData(IOhlcv ohlcv) {
        this.ohlcv = ohlcv;
    }

    /**
     * @return get domain object
     */
    public IOhlcv getData() {
        return ohlcv;
    }

    /**
     * @return is true for a category axis support
     */
    public boolean isCategoryBased() {
        return isCategoryBased;
    }

    /**
     * Change category or time based axis handling.
     * @param categoryBased if true; category based indexes are used. Default false.
     */
    public void setCategoryBased(boolean categoryBased) {
        isCategoryBased = categoryBased;
    }

    @Override
    public double get(int dimIndex, int index) {
        IOhlcvItem ohlcvItem = ohlcv.getOhlcvItem(index);
        switch (dimIndex) {
        case DIM_X:
            return isCategoryBased ? index : ohlcvItem.getTimeStamp().getTime() / 1000.0;
        case DIM_Y_OPEN:
            return ohlcvItem.getOpen();
        case DIM_Y_HIGH:
            return ohlcvItem.getHigh();
        case DIM_Y_LOW:
            return ohlcvItem.getLow();
        case DIM_Y_CLOSE:
            return ohlcvItem.getClose();
        case DIM_Y_VOLUME:
            return ohlcvItem.getVolume();
        case DIM_Y_OI:
            return ohlcvItem.getOpenInterest();
        default:
            throw new IllegalArgumentException("Dimension not allowed! dimIndex=" + dimIndex);
        }
    }

    public IOhlcvItem get(int index) {
        return ohlcv.getOhlcvItem(index);
    }

    public IOhlcvItem getLast() {
        int size = ohlcv.size();
        if (size == 0) {
            return null;
        }
        return ohlcv.getOhlcvItem(size - 1);
    }

    @Override
    public Iterator<IOhlcvItem> iterator() {
        return ohlcv.iterator();
    }

    @Override
    public int getDataCount() {
        return ohlcv.size();
    }

    @Override
    public DataSet set(DataSet other, boolean copy) {
        this.ohlcv = ((OhlcvDataSet) other).ohlcv;
        return this;
    }

    /**
     * Gets the index of the data point closest to the given x coordinate.
     * If the x coordinate lies outside the range of the data set, the index of the first/last point is returned.
     *
     * @param x the x position of the data point
     * @return the index of the data point
     */
    public int getXIndex(double x) {
        return getIndex(DIM_X, x);
    }
}
