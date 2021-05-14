package de.gsi.financial.samples.service.footprint;

import java.util.Collection;

import de.gsi.chart.renderer.spi.financial.FootprintRenderer;
import de.gsi.chart.renderer.spi.financial.service.footprint.FootprintRendererAttributes;
import de.gsi.chart.renderer.spi.financial.service.footprint.NbColumnColorGroup;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import de.gsi.financial.samples.dos.OHLCVItem;

/**
 * Specific implementation of adapter for your trading framework.
 * This adapter is just demonstration to show fast mapping to trading domain objects for providing footprint data for renderers.
 *
 * @author afischer
 */
public class FootprintRenderedAPIAdapter implements FootprintRenderer.IFootprintRenderedAPI {
    private final FootprintRendererAttributes footprintAttrs;
    private final NbColumnColorGroupService nbColumnColorGroupService;

    public FootprintRenderedAPIAdapter(FootprintRendererAttributes footprintAttrs, NbColumnColorGroupService nbColumnColorGroupService) {
        this.footprintAttrs = footprintAttrs;
        this.nbColumnColorGroupService = nbColumnColorGroupService;
    }

    @Override
    public FootprintRendererAttributes getFootprintAttributes() {
        return footprintAttrs;
    }

    @Override
    public boolean isFootprintAvailable(IOhlcvItem ohlcvItem) {
        return ((OHLCVItem) ohlcvItem).getExtended() != null;
    }

    @Override
    public Collection<Double[]> getPriceVolumeList(IOhlcvItem ohlcvItem) {
        return ((OHLCVItem) ohlcvItem).getExtended().getPriceVolumeContainer().getCompletedPriceVolume();
    }

    @Override
    public double getPocPrice(IOhlcvItem ohlcvItem) {
        return ((OHLCVItem) ohlcvItem).getExtended().getPriceVolumeContainer().getPocPrice();
    }

    @Override
    public IOhlcvItem getPullbackColumn(IOhlcvItem ohlcvItem) {
        return ((OHLCVItem) ohlcvItem).getExtended().getPullbackOhlcvItem();
    }

    @Override
    public Object getLock(IOhlcvItem ohlcvItem) {
        return ((OHLCVItem) ohlcvItem).getExtended().lock;
    }

    @Override
    public NbColumnColorGroup getColumnColorGroup(IOhlcvItem ohlcvItem) {
        return nbColumnColorGroupService.calculate(((OHLCVItem) ohlcvItem).getExtended().getPriceVolumeContainer());
    }
}
