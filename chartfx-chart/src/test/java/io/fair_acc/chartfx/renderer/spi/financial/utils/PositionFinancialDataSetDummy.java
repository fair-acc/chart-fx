package io.fair_acc.chartfx.renderer.spi.financial.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.fair_acc.chartfx.renderer.spi.financial.PositionFinancialRendererPaintAfterEP;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.AbstractDataSet;

public class PositionFinancialDataSetDummy extends AbstractDataSet<PositionFinancialDataSetDummy> implements PositionFinancialRendererPaintAfterEP.PositionRenderedAware {
    private Map<Long, PositionFinancialRendererPaintAfterEP.PositionRendered> renderedPositionsBased;
    private List<PositionFinancialRendererPaintAfterEP.PositionRendered> renderedPositionsList;

    public PositionFinancialDataSetDummy(List<PositionFinancialRendererPaintAfterEP.PositionRendered> positionList) {
        super("positions-dummy", 2);
        fillDataSet(positionList);
    }

    private void fillDataSet(List<PositionFinancialRendererPaintAfterEP.PositionRendered> positionList) {
        renderedPositionsBased = new TreeMap<>();
        renderedPositionsList = new ArrayList<>();
        for (PositionFinancialRendererPaintAfterEP.PositionRendered positionRendered : positionList) {
            renderedPositionsBased.put(positionRendered.index, positionRendered);
            renderedPositionsList.add(positionRendered);
        }
    }

    public PositionFinancialRendererPaintAfterEP.PositionRendered getPositionByTime(long corr) {
        return renderedPositionsBased.get(corr);
    }

    @Override
    public double get(int dimIndex, int index) {
        PositionFinancialRendererPaintAfterEP.PositionRendered positionRendered = renderedPositionsList.get(index);
        if (dimIndex == DIM_X) { // return coordination by index
            return positionRendered == null ? -1.0 : positionRendered.index;
        }
        return positionRendered.price;
    }

    @Override
    public int getDataCount() {
        return renderedPositionsList.size();
    }

    @Override
    public DataSet set(DataSet other, boolean copy) {
        this.renderedPositionsBased = new TreeMap<>(((PositionFinancialDataSetDummy) other).renderedPositionsBased);
        this.renderedPositionsList = new ArrayList<>(((PositionFinancialDataSetDummy) other).renderedPositionsList);
        return this;
    }
}
