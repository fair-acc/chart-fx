package io.fair_acc.financial.samples.service.footprint;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.FootprintRendererAttributes;
import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.NbColumnColorGroup;
import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.NbColumnColorGroup.FontColor;
import io.fair_acc.financial.samples.dos.PriceVolumeContainer;

/**
 * Standard calculation of computation by percentage of diagonal dominant bid x ask volumes 
 */
public class DiagonalDominantNbColumnColorGroupService implements NbColumnColorGroupService {
    private final boolean columnColoringFeatureActive;
    private final Color[][] columnColorGroupSettings;
    private final Double[] columnColorGroupThresholds;
    private final Font[] bidAskVolumeFonts;
    private final Double bidAskBoldThreshold;

    public DiagonalDominantNbColumnColorGroupService(FootprintRendererAttributes footprintAttrs) {
        columnColoringFeatureActive = footprintAttrs.getAttribute(FootprintRendererAttributes.COLUMN_COLORING_FEATURE_ACTIVE);
        columnColorGroupSettings = footprintAttrs.getAttribute(FootprintRendererAttributes.COLUMN_COLOR_GROUP_SETTINGS);
        columnColorGroupThresholds = footprintAttrs.getAttribute(FootprintRendererAttributes.COLUMN_COLOR_GROUP_THRESHOLDS);
        bidAskBoldThreshold = footprintAttrs.getAttribute(FootprintRendererAttributes.BID_ASK_BOLD_THRESHOLD);
        bidAskVolumeFonts = footprintAttrs.getAttribute(FootprintRendererAttributes.BID_ASK_VOLUME_FONTS);
    }

    @Override
    public NbColumnColorGroup calculate(PriceVolumeContainer priceVolumeContainer) {
        if (!columnColoringFeatureActive)
            return null;

        NbColumnColorGroup result = new NbColumnColorGroup();

        Double[] prevPriceVolume = null;
        Color prevBidColor;
        Color askColor;
        Color prevAskColor = null;
        Font bidVolumeFont = null;
        Font askVolumeFont = null;

        for (Double[] priceVolume : priceVolumeContainer.getCompletedPriceVolume()) {
            double bidVolume = priceVolume[1];
            double askVolume = priceVolume[2];

            if (prevPriceVolume != null) { // diagonal computation
                double prevBidVolume = prevPriceVolume[1];

                double prevBidPercentage = prevBidVolume / askVolume * 100.0;
                double askPercentage = askVolume / prevBidVolume * 100.0;

                prevBidColor = getColumnColorGroup(prevBidPercentage, 0);
                askColor = getColumnColorGroup(askPercentage, 1);

                result.fontColorMap.put(prevPriceVolume[0], new FontColor(bidVolumeFont, prevBidColor, askVolumeFont, prevAskColor));

                prevAskColor = askColor;
                bidVolumeFont = getFontForBidAskVolume(bidVolume);
                askVolumeFont = getFontForBidAskVolume(askVolume);

            } else { // first bottom line
                prevAskColor = columnColorGroupSettings[1][1]; // group 1
                bidVolumeFont = getFontForBidAskVolume(bidVolume);
                askVolumeFont = getFontForBidAskVolume(askVolume);
            }

            prevPriceVolume = priceVolume;
        }
        // last top line
        prevBidColor = columnColorGroupSettings[0][1]; // group 1
        if (prevPriceVolume != null) {
            result.fontColorMap.put(prevPriceVolume[0], new FontColor(bidVolumeFont, prevBidColor, askVolumeFont, prevAskColor));
        }

        return result;
    }

    private Font getFontForBidAskVolume(double volume) {
        if (volume >= bidAskBoldThreshold) {
            return bidAskVolumeFonts[1];
        } else {
            return bidAskVolumeFonts[0];
        }
    }

    private Color getColumnColorGroup(double percentage, int bidOrAsk) {
        if (percentage < columnColorGroupThresholds[0]) {
            return columnColorGroupSettings[bidOrAsk][0];
        }
        if (percentage >= columnColorGroupThresholds[0] && percentage < columnColorGroupThresholds[1]) {
            return columnColorGroupSettings[bidOrAsk][1];
        }
        if (percentage >= columnColorGroupThresholds[1] && percentage < columnColorGroupThresholds[2]) {
            return columnColorGroupSettings[bidOrAsk][2];
        }
        if (percentage >= columnColorGroupThresholds[2]) {
            return columnColorGroupSettings[bidOrAsk][3];
        }
        return null;
    }
}
