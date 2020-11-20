package de.gsi.dataset.spi.financial;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.dataset.spi.financial.utils.FinancialTestUtils.createTestOhlcv;
import static de.gsi.dataset.spi.financial.utils.FinancialTestUtils.toDate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import de.gsi.dataset.spi.financial.utils.Ohlcv;

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
        ohlcvItem = datasetTested.get(datasetTested.getDataCount() - 1);
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
    public void getLast() {
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size() - 1), datasetTested.getLast());
        ((Ohlcv) ohlcv).clear();
        assertNull(datasetTested.getLast());
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
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size() - 1), datasetTested.get(datasetTested.getXIndex(toDate(dateLast).getTime() / 1000.0)));
        assertEquals(ohlcv.getOhlcvItem(ohlcv.size() - 1), datasetTested.get(datasetTested.getXIndex(toDate(afterDateLast).getTime() / 1000.0)));
        assertEquals(ohlcv.getOhlcvItem(0), datasetTested.get(datasetTested.getXIndex(toDate(beforeDateLast).getTime() / 1000.0)));
    }
}