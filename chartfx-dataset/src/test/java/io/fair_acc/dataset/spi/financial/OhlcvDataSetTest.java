package io.fair_acc.dataset.spi.financial;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcv;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import io.fair_acc.dataset.spi.financial.utils.FinancialTestUtils;
import io.fair_acc.dataset.spi.financial.utils.Ohlcv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OhlcvDataSetTest {
    private OhlcvDataSet datasetTested;
    private IOhlcv ohlcv;

    @BeforeEach
    public void setUp() {
        ohlcv = FinancialTestUtils.createTestOhlcv();
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
    public void getItem() {
        IOhlcvItem ohlcvItem = datasetTested.getItem(0);
        assertEquals(ohlcv.getOhlcvItem(0), ohlcvItem);
        ohlcvItem = datasetTested.getItem(datasetTested.getDataCount() - 1);
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size() - 1), ohlcvItem);
    }

    @Test
    public void testGetDimIndex() {
        assertEquals(ohlcv.getOhlcvItem(0).getTimeStamp().getTime() / 1000.0,
                datasetTested.get(DataSet.DIM_X, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size() - 1).getTimeStamp().getTime() / 1000.0,
                datasetTested.get(DataSet.DIM_X, datasetTested.getDataCount() - 1), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getTimeStamp().getTime() / 1000.0,
                datasetTested.get(OhlcvDataSet.DIM_X, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getOpen(), datasetTested.get(OhlcvDataSet.DIM_Y_OPEN, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getHigh(), datasetTested.get(OhlcvDataSet.DIM_Y_HIGH, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getLow(), datasetTested.get(OhlcvDataSet.DIM_Y_LOW, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getClose(), datasetTested.get(OhlcvDataSet.DIM_Y_CLOSE, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getVolume(), datasetTested.get(OhlcvDataSet.DIM_Y_VOLUME, 0), 1e-5);
        assertEquals(ohlcv.getOhlcvItem(0).getOpenInterest(), datasetTested.get(OhlcvDataSet.DIM_Y_OI, 0), 1e-5);
        assertThrows(IllegalArgumentException.class, () -> datasetTested.get(7, 0));
    }

    @Test
    public void testGetDimIndexCategory() {
        datasetTested.setCategoryBased(true);
        assertEquals(0, datasetTested.get(DataSet.DIM_X, 0), 1e-5);
        assertEquals(1, datasetTested.get(DataSet.DIM_X, 1), 1e-5);
    }

    @Test
    public void getLastItem() {
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size() - 1), datasetTested.getLastItem());
        ((Ohlcv) ohlcv).clear();
        assertNull(datasetTested.getLastItem());
    }

    @Test
    public void iterator() {
        List<IOhlcvItem> items = new ArrayList<>();
        for (IOhlcvItem ohlcvItem : datasetTested) {
            items.add(ohlcvItem);
        }
        assertEquals(((Ohlcv) ohlcv).getItems(), items);
    }

    @Test
    public void getDataCount() {
        assertEquals(5, datasetTested.getDataCount());
    }

    @Test
    public void set() {
        IOhlcv ohlcv2 = FinancialTestUtils.createTestOhlcv();
        OhlcvDataSet dataSet2 = new OhlcvDataSet("test2");
        dataSet2.setData(ohlcv2);
        datasetTested.set(dataSet2, false);
        assertEquals(ohlcv2, datasetTested.getData());
    }
}