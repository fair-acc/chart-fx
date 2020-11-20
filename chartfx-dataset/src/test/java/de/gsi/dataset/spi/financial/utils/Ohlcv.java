package de.gsi.dataset.spi.financial.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.gsi.dataset.spi.financial.api.attrs.AttributeModel;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

public class Ohlcv implements IOhlcv {
    final List<IOhlcvItem> items = new ArrayList<>();

    @Override
    public IOhlcvItem getOhlcvItem(int index) {
        return items.get(index);
    }

    public Ohlcv addOhlcvItem(IOhlcvItem ohlcvItem) {
        items.add(ohlcvItem);
        return this;
    }

    // just for unit tests
    public List<IOhlcvItem> getItems() {
        return items;
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