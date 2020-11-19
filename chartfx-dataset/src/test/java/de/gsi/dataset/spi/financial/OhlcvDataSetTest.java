package de.gsi.dataset.spi.financial;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OhlcvDataSetTest {

    private OhlcvDataSet datasetTested;
    private IOhlcv ohlcv;

    @BeforeEach
    public void setUp() {
        ohlcv = createTestOhlcv();
        datasetTested = new OhlcvDataSet("ohlc1");
        datasetTested.setData(ohlcv);
    }

    @Test
    public void setData() {
        datasetTested.setData(null);
        assertNull(datasetTested.getData());
        datasetTested.setData(ohlcv);
        assertEquals(ohlcv, datasetTested.getData());
    }

    @Test
    public void setCategoryBased() {
        assertFalse(datasetTested.isCategoryBased());
        datasetTested.setCategoryBased(true);
        assertTrue(datasetTested.isCategoryBased());
    }

    @Test
    public void get() {
        IOhlcvItem ohlcvItem = datasetTested.get(0);
        assertEquals(ohlcv.getOhlcvItem(0), ohlcvItem);
        ohlcvItem = datasetTested.get(datasetTested.getDataCount()-1);
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size()-1), ohlcvItem);
    }

    @Test
    public void testGetDimIndex() {
        assertEquals(ohlcv.getOhlcvItem(0).getTimeStamp().getTime() / 1000.0,
                datasetTested.get(DataSet.DIM_X, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size()-1).getTimeStamp().getTime() / 1000.0,
                datasetTested.get(DataSet.DIM_X, datasetTested.getDataCount()-1), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getTimeStamp().getTime() / 1000.0,
                datasetTested.get(OhlcvDataSet.DIM_X, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getOpen(), datasetTested.get(OhlcvDataSet.DIM_Y_OPEN, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getHigh(), datasetTested.get(OhlcvDataSet.DIM_Y_HIGH, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getLow(), datasetTested.get(OhlcvDataSet.DIM_Y_LOW, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getClose(), datasetTested.get(OhlcvDataSet.DIM_Y_CLOSE, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getVolume(), datasetTested.get(OhlcvDataSet.DIM_Y_VOLUME, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getOpenInterest(), datasetTested.get(OhlcvDataSet.DIM_Y_OI, 0), 1e-5);
    }

    @Test
    public void getLast() {
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size()-1), datasetTested.getLast());
        ((Ohlcv)ohlcv).clear();
        assertNull(datasetTested.getLast());
    }

    @Test
    public void iterator() {
        List<IOhlcvItem> items = new ArrayList<>();
        for (IOhlcvItem ohlcvItem : datasetTested) {
            items.add(ohlcvItem);
        }
        assertEquals(((Ohlcv)ohlcv).items, items);
    }

    @Test
    public void getDataCount() {
        assertEquals(5, datasetTested.getDataCount());
    }

    @Test
    public void set() {
        IOhlcv ohlcv2 = createTestOhlcv();
        OhlcvDataSet dataSet2 = new OhlcvDataSet("test2");
        dataSet2.setData(ohlcv2);
        datasetTested.set(dataSet2, false);
        assertEquals(ohlcv2, datasetTested.getData());
    }

    @Test
    public void getXIndex() {
        String dateString = "2020-11-19";
        LocalDate date = LocalDate.parse(dateString);
        LocalDate dateLast = LocalDate.parse(dateString).plusDays(4);
        LocalDate afterDateLast = LocalDate.parse(dateString).plusDays(5);
        LocalDate beforeDateLast = LocalDate.parse(dateString).minusDays(1);

        assertEquals(ohlcv.getOhlcvItem(0), datasetTested.get(datasetTested.getXIndex(toDate(date).getTime() / 1000.0)));
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size()-1), datasetTested.get(datasetTested.getXIndex(toDate(dateLast).getTime() / 1000.0)));
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size()-1), datasetTested.get(datasetTested.getXIndex(toDate(afterDateLast).getTime() / 1000.0)));
        assertEquals(ohlcv.getOhlcvItem(0), datasetTested.get(datasetTested.getXIndex(toDate(beforeDateLast).getTime() / 1000.0)));
    }

    //-------------------- helpers ---------------------

    private IOhlcv createTestOhlcv() {
        LocalDate date = LocalDate.parse("2020-11-19");
        return new Ohlcv()
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3001.0, 3005.0, 3000.10, 3002.84, 15001.0, 18007.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3002.0, 3007.0, 3001.35, 3005.64, 16005.0, 19002.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 3003.0, 3009.15, 3002.50, 3008.75, 14004.0, 20005.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 2999.0, 3000.75, 2997.15, 2998.10, 100085.0, 35001.0))
                .addOhlcvItem(new OhlcvItem(toDate(date.plusDays(1)), 2996.0, 2998.0, 2994.10, 2993.50, 135001.0, 64010.0));
    }

    private Date toDate(LocalDate ldate) {
        return Date.from(ldate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static class Ohlcv implements IOhlcv {

        final List<IOhlcvItem> items = new ArrayList<>();

        @Override
        public IOhlcvItem getOhlcvItem(int index) {
            return items.get(index);
        }

        public Ohlcv addOhlcvItem(IOhlcvItem ohlcvItem) {
            items.add(ohlcvItem);
            return this;
        }

        @Override
        public int size() {
            return items.size();
        }

        public void clear() {
            items.clear();
        }

        @Override
        public AttributeModel getAddon() {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public AttributeModel getAddonOrCreate() {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public Iterator<IOhlcvItem> iterator() {
            return items.iterator();
        }
    }

    private static class OhlcvItem implements IOhlcvItem {

        private final Date timestamp;
        private final double open;
        private final double high;
        private final double low;
        private final double close;
        private final double volume;
        private final double oi;

        private AttributeModel addon;

        public OhlcvItem(Date timestamp, double open, double high, double low, double close, double volume, double oi) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.oi = oi;
        }

        @Override
        public Date getTimeStamp() {
            return timestamp;
        }

        @Override
        public double getOpen() {
            return open;
        }

        @Override
        public double getHigh() {
            return high;
        }

        @Override
        public double getLow() {
            return low;
        }

        @Override
        public double getClose() {
            return close;
        }

        @Override
        public double getVolume() {
            return volume;
        }

        @Override
        public double getOpenInterest() {
            return oi;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OhlcvItem ohlcvItem = (OhlcvItem) o;
            return Double.compare(ohlcvItem.open, open) == 0 &&
                    Double.compare(ohlcvItem.high, high) == 0 &&
                    Double.compare(ohlcvItem.low, low) == 0 &&
                    Double.compare(ohlcvItem.close, close) == 0 &&
                    Double.compare(ohlcvItem.volume, volume) == 0 &&
                    Double.compare(ohlcvItem.oi, oi) == 0 &&
                    Objects.equals(timestamp, ohlcvItem.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, open, high, low, close, volume, oi);
        }
    }

}