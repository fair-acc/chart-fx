package de.gsi.chart.renderer.spi.financial.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.gsi.chart.renderer.spi.financial.PositionFinancialRendererPaintAfterEP.PositionRendered;
import de.gsi.chart.renderer.spi.financial.PositionFinancialRendererPaintAfterEP.PositionRenderedAware;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.AbstractDataSet;

public class PositionFinancialDataSetDummy extends AbstractDataSet<PositionFinancialDataSetDummy> implements PositionRenderedAware {
    private Map<Long, PositionRendered> renderedPositionsBased;
    private List<PositionRendered> renderedPositionsList;

    public PositionFinancialDataSetDummy(List<PositionRendered> positionList) {
        super("positions-dummy", 2);
        fillDataSet(positionList);
    }

    private void fillDataSet(List<PositionRendered> positionList) {
        renderedPositionsBased = new TreeMap<>();
        renderedPositionsList = new ArrayList<>();
        for (PositionRendered positionRendered : positionList) {
            renderedPositionsBased.put(positionRendered.index, positionRendered);
            renderedPositionsList.add(positionRendered);
        }
    }

    public PositionRendered getPositionByTime(long corr) {
        return renderedPositionsBased.get(corr);
    }

    @Override
    public double get(int dimIndex, int index) {
        PositionRendered positionRendered = renderedPositionsList.get(index);
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
