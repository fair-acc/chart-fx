package de.gsi.chart.renderer.spi.hexagon;

import java.util.Optional;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * This class creates a map of Hexagons from an image file.
 */
class MapGenerator {
    private final HexagonMap map;
    private final Image image;
    private final int mapWidth;
    private Double verticalRelation;
    private Double horizontalRelation;

    /**
     * The image proportions are maintained, therefore only the desired width is specified.
     * 
     * @param map global hex map
     * @param image source image
     * @param mapWidthInHexes desired map width in hex grid quantas
     */
    public MapGenerator(HexagonMap map, Image image, int mapWidthInHexes) {
        this.map = map;
        this.image = image;
        mapWidth = mapWidthInHexes;
    }

    /**
     * You will have to supply an object that will create the Hexagons as you like. E.g.
     * <p>
     * class HexagonCreator implements IHexagonCreator {
     *
     * @param creator the object that will actually create the Hexagon.
     * @Override public void createHexagon(GridPosition position, javafx.scene.paint.Color color) { Hexagon h = new
     *           Hexagon(position, 20, 0, 0); h.setBackgroundColor(color); map.addHexagon(h); } }
     */
    public void generate(IHexagonCreator creator) {
        final PixelReader pr = image.getPixelReader();
        if (pr == null) {
            return;
        }
        final double imageWidth = image.getWidth();
        final double imageHeight = image.getHeight();
        final double hexagonMapWidthInPixels = map.getGraphicsHorizontalDistanceBetweenHexagons() * mapWidth;
        horizontalRelation = imageWidth / hexagonMapWidthInPixels;
        final double estimatedHexagonMapHeightInPixels = imageHeight / horizontalRelation;

        final int mapHeight = (int) (estimatedHexagonMapHeightInPixels
                                     / map.getGraphicsverticalDistanceBetweenHexagons());
        verticalRelation = imageHeight
                           / (map.getGraphicsverticalDistanceBetweenHexagons() * mapHeight + map.getGraphicsHexagonHeight() / 2);
        // Not really sure about the last part but it seems to work. And should I make the corresponding correction on
        // the horizontalRelation ?

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                final int axialQ = x - (y - (y & 1)) / 2;
                final int axialR = y;
                final Hexagon h = new Hexagon(axialQ, axialR);
                h.setMap(map);
                final int xOnImage = (int) ((h.getGraphicsXoffset() - map.graphicsXpadding) * horizontalRelation);
                final int yOnImage = (int) ((h.getGraphicsYoffset() - map.graphicsYpadding) * verticalRelation);
                final Color pixelColor = pr.getColor(xOnImage, yOnImage);
                creator.createHexagon(axialQ, axialR, pixelColor, map);
            }
        }
    }

    public Optional<Double> getHorizontalRelation() {
        return horizontalRelation == null ? Optional.empty() : Optional.of(horizontalRelation);
    }

    public Optional<Double> getVerticalRelation() {
        return verticalRelation == null ? Optional.empty() : Optional.of(verticalRelation);
    }
}
