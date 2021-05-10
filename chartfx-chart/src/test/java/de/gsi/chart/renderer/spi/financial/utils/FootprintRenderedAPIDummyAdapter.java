package de.gsi.chart.renderer.spi.financial.utils;

import static de.gsi.chart.renderer.spi.financial.service.footprint.FootprintRendererAttributes.BID_ASK_VOLUME_FONTS;
import static de.gsi.chart.renderer.spi.financial.service.footprint.FootprintRendererAttributes.COLUMN_COLOR_GROUP_SETTINGS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import de.gsi.chart.renderer.spi.financial.FootprintRenderer;
import de.gsi.chart.renderer.spi.financial.service.footprint.FootprintRendererAttributes;
import de.gsi.chart.renderer.spi.financial.service.footprint.NbColumnColorGroup;
import de.gsi.chart.renderer.spi.financial.service.footprint.NbColumnColorGroup.FontColor;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;

public class FootprintRenderedAPIDummyAdapter implements FootprintRenderer.IFootprintRenderedAPI {
    private final FootprintRendererAttributes footprintAttrs;
    private Random randomNum = new Random();
    private int idx = -1;

    public FootprintRenderedAPIDummyAdapter(FootprintRendererAttributes footprintAttrs) {
        this.footprintAttrs = footprintAttrs;
    }

    @Override
    public FootprintRendererAttributes getFootprintAttributes() {
        return footprintAttrs;
    }

    @Override
    public boolean isFootprintAvailable(IOhlcvItem ohlcvItem) {
        idx++;
        return idx != 2;
    }

    @Override
    public Collection<Double[]> getPriceVolumeList(IOhlcvItem ohlcvItem) {
        List<Double[]> pba = new ArrayList<>();
        pba.add(new Double[] { ohlcvItem.getOpen(), genRand100(), genRand100() });
        pba.add(new Double[] { ohlcvItem.getHigh(), genRand100(), genRand100() });
        pba.add(new Double[] { ohlcvItem.getLow(), genRand100(), genRand100() });
        pba.add(new Double[] { ohlcvItem.getClose(), genRand100(), genRand100() });

        return pba;
    }

    @Override
    public double getPocPrice(IOhlcvItem ohlcvItem) {
        return ohlcvItem.getClose();
    }

    @Override
    public IOhlcvItem getPullbackColumn(IOhlcvItem ohlcvItem) {
        if (idx == 0 || idx == 5) {
            return ohlcvItem;
        }
        return null;
    }

    @Override
    public Object getLock(IOhlcvItem ohlcvItem) {
        return new Object();
    }

    @Override
    public NbColumnColorGroup getColumnColorGroup(IOhlcvItem ohlcvItem) {
        if (idx == 0 || idx == 1) {
            Font[] fonts = footprintAttrs.getAttribute(BID_ASK_VOLUME_FONTS);
            Color[][] colors = footprintAttrs.getAttribute(COLUMN_COLOR_GROUP_SETTINGS);
            NbColumnColorGroup nbColumnColorGroup = new NbColumnColorGroup();
            nbColumnColorGroup.fontColorMap.put(ohlcvItem.getOpen(), new FontColor(fonts[1], colors[1][3], fonts[1], colors[0][3]));
            nbColumnColorGroup.fontColorMap.put(ohlcvItem.getHigh(), new FontColor(fonts[2], colors[1][2], fonts[2], colors[0][2]));
            nbColumnColorGroup.fontColorMap.put(ohlcvItem.getLow(), new FontColor(fonts[1], colors[1][1], fonts[2], colors[0][1]));
            nbColumnColorGroup.fontColorMap.put(ohlcvItem.getClose(), new FontColor(fonts[1], colors[1][0], fonts[2], colors[0][0]));

            return nbColumnColorGroup;
        }
        return null;
    }

    private double genRand100() {
        return randomNum.nextInt(100);
    }
}
