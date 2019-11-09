package de.gsi.chart.renderer.spi;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ContourType;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.hexagon.Hexagon;
import de.gsi.chart.renderer.spi.hexagon.HexagonMap;
import de.gsi.chart.renderer.spi.hexagon.HexagonMap.Direction;
import de.gsi.chart.renderer.spi.marchingsquares.GeneralPath;
import de.gsi.chart.renderer.spi.marchingsquares.MarchingSquares;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

/**
 * https://en.wikipedia.org/wiki/Marching_squares#Isoline
 *
 * @author rstein
 */
public class ContourDataSetRenderer extends AbstractDataSetManagement<ContourDataSetRenderer> implements Renderer {

    private final Cache localCache = new Cache();
    private Axis zAxis;
    protected final Rectangle gradientRect = new Rectangle();

    private final IntegerProperty quantisationLevels = new SimpleIntegerProperty(this, "quantisationLevels", 20) {

        @Override
        public void set(int newValue) {
            super.set(Math.max(2, newValue));
        };
    };

    private final IntegerProperty minHexTileSize = new SimpleIntegerProperty(this, "minHexTileSize", 5) {

        @Override
        public void set(int newValue) {
            super.set(Math.max(2, newValue));
        }
    };

    /**
     * suppresses contour segments being drawn that have more than the specified number of sub-segments
     */
    private final IntegerProperty maxContourSegments = new SimpleIntegerProperty(this, "maxContourSegments", 500) {

        @Override
        public void set(int newValue) {
            super.set(Math.max(2, newValue));
        }
    };

    private final ObjectProperty<ColorGradient> colorGradient = new SimpleObjectProperty<>(this, "colorGradient",
            ColorGradient.DEFAULT);

    private final BooleanProperty smooth = new SimpleBooleanProperty(this, "smooth", false) {

        @Override
        protected void invalidated() {
            // requestChartLayout();
        }
    };

    private final BooleanProperty computeLocalRange = new SimpleBooleanProperty(this, "computeLocalRange", true);

    private final ObjectProperty<ContourType> contourType = new SimpleObjectProperty<>(this, "contourType",
            ContourType.HEATMAP);

    public ContourDataSetRenderer() {
        super();
    }

    private int clamp(int value, int range) {
        return Math.max(Math.min(value, range), 0);
    }

    /**
     * Color gradient (linear) used to encode data point values.
     *
     * @return gradient property
     */
    public ObjectProperty<ColorGradient> colorGradientProperty() {
        return colorGradient;
    }

    /**
     * Returns the value of the {@link #computeLocalRangeProperty()}.
     *
     * @return {@code true} if the local range calculation is applied, {@code false} otherwise
     */
    public boolean computeLocalRange() {
        return computeLocalRangeProperty().get();
    }

    /**
     * Indicates if the chart should compute the min/max z-Axis for the local (true) or global (false) visible range
     *
     * @return computeLocalRange property
     */
    public BooleanProperty computeLocalRangeProperty() {
        return computeLocalRange;
    }

    /**
     * Indicates if the chart should plot contours (true) or color gradient map (false)
     *
     * @return plotContourProperty property
     */
    public ObjectProperty<ContourType> contourTypeProperty() {
        return contourType;
    }

    private void drawContour(final GraphicsContext gc, final AxisTransform axisTransform, final Cache lCache) {
        final double scaleX = Math.max(lCache.xAxisWidth / lCache.xSize, 1.0);
        final double scaleY = Math.max(lCache.yAxisHeight / lCache.ySize, 1.0);
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = lCache.dataSet3D;

        final double[] levels = new double[getNumberQuantisationLevels()];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = (i + 1) / (double) levels.length;
        }
        final double[][] data = new double[lCache.ySize][lCache.xSize];
        for (int xIndex = lCache.indexXMin; xIndex < lCache.indexXMax; xIndex++) {
            for (int yIndex = lCache.indexYMin; yIndex < lCache.indexYMax; yIndex++) {
                final double z = dataSet.getZ(xIndex, yIndex);
                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                data[indexYMax - 1 - yIndex][xIndex - indexXMin] = offset;
            }
        }
        final MarchingSquares marchingSquares = new MarchingSquares();
        try {

            gc.save();
            gc.scale(scaleX, scaleY);

            final GeneralPath[] isolines = marchingSquares.buildContours(data, levels);
            int levelCount = 0;
            for (final GeneralPath path : isolines) {
                if (path.size() > getMaxContourSegments()) {
                    levelCount++;
                    continue;
                }
                final Color color = lCache.zInverted ? getColor(1 - levels[levelCount++])
                        : getColor(levels[levelCount++]);
                gc.setStroke(color);
                gc.setLineDashes(1.0);
                gc.setMiterLimit(10);
                gc.setFill(color);
                gc.setLineWidth(0.5);
                path.draw(gc);
            }
            gc.restore();

        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void drawContourFast(final GraphicsContext gc, final AxisTransform axisTransform, final Cache lCache) {
        final long start = ProcessingProfiler.getTimeStamp();
        final int scaleX = isSmooth() ? 1 : Math.max((int) localCache.xAxisWidth / localCache.xSize, 1);
        final int scaleY = isSmooth() ? 1 : Math.max((int) localCache.yAxisHeight / localCache.ySize, 1);
        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexXMax = lCache.indexXMax;
        final int indexYMin = lCache.indexYMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = lCache.dataSet3D;

        getNumberQuantisationLevels();

        // filter for contour
        final double[][] input = new double[xSize][ySize];
        final double[][] output = new double[xSize][ySize];
        final double[][] output2 = new double[xSize][ySize];

        // setup quantisation levels
        final double[] levels = new double[getNumberQuantisationLevels()];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = (i + 1) / (double) levels.length;
        }

        // setup input
        for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
            for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
                final double z = dataSet.getZ(xIndex, yIndex);
                final int x = xIndex - indexXMin;
                final int y = indexYMax - 1 - yIndex;
                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                input[x][y] = offset;
            }
        }

        final WritableImage image = new WritableImage(xSize * scaleX, ySize * scaleY);
        final PixelWriter pixelWriter = image.getPixelWriter();

        for (final double level : levels) {
            ContourDataSetRenderer.sobelOperator(input, output2, zMin, zMax, level);
            ContourDataSetRenderer.erosionOperator(output2, output, zMin, zMax, level);
            // erosionOperator2(output2, output, zMin, zMax, levels[i]);

            for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
                for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
                    // final double z = dataSet3D.getZ(xIndex, yIndex);
                    final double z = output[xIndex - indexXMin][yIndex - indexYMin];

                    // Color color = getColor(z);
                    Color color = lCache.zInverted ? getColor(1 - z) : getColor(z);
                    if (z > 0) {
                        color = lCache.zInverted ? getColor(1 - level) : getColor(level);
                    } else {
                        color = Color.TRANSPARENT;
                        continue;
                    }

                    for (int dx = 0; dx < scaleX; dx++) {
                        for (int dy = 0; dy < scaleY; dy++) {
                            final int x = (xIndex - indexXMin) * scaleX;
                            final int y = (indexYMax - 1 - yIndex) * scaleY;
                            pixelWriter.setColor(x + dx, y + dy, color);
                        }
                    }
                }
            }
        }

        gc.drawImage(image, 0, 0, lCache.xAxisWidth, lCache.yAxisHeight);
        ProcessingProfiler.getTimeDiff(start, "sobel");
    }

    private void drawHeatMap(final GraphicsContext gc, final AxisTransform axisTransform, final Cache lCache) {
        final long start = ProcessingProfiler.getTimeStamp();
        // this.setSmooth(false);
        final int scaleX = isSmooth() ? 1 : Math.max((int) lCache.xAxisWidth / lCache.xSize, 1);
        final int scaleY = isSmooth() ? 1 : Math.max((int) lCache.yAxisHeight / lCache.ySize, 1);
        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexXMax = lCache.indexXMax;
        final int indexYMin = lCache.indexYMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = lCache.dataSet3D;

        final int nQuant = getNumberQuantisationLevels();

        final WritableImage image = new WritableImage(xSize * scaleX, ySize * scaleY);
        final PixelWriter pixelWriter = image.getPixelWriter();
        for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
            for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
                final double z = dataSet.getZ(xIndex, yIndex);
                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                final Color color = lCache.zInverted ? getColor(ContourDataSetRenderer.quantize(1 - offset, nQuant))
                        : getColor(ContourDataSetRenderer.quantize(offset, nQuant));

                final int x = (xIndex - indexXMin) * scaleX;
                final int y = (indexYMax - 1 - yIndex) * scaleY;
                for (int dx = 0; dx < scaleX; dx++) {
                    for (int dy = 0; dy < scaleY; dy++) {
                        pixelWriter.setColor(x + dx, y + dy, color);
                    }
                }
            }
        }

        gc.drawImage(image, 0, 0, lCache.xAxisWidth, lCache.yAxisHeight);
        ProcessingProfiler.getTimeDiff(start, "drawHeatMap");
    }

    private void drawHexagonHeatMap(final GraphicsContext gc, final AxisTransform axisTransform, final Cache lCache,
            boolean test) {
        final long start = ProcessingProfiler.getTimeStamp();
        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexXMax = lCache.indexXMax;
        final int indexYMin = lCache.indexYMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = lCache.dataSet3D;

        final int nQuant = getNumberQuantisationLevels();

        final WritableImage image = new WritableImage(xSize, ySize);
        final PixelWriter pixelWriter = image.getPixelWriter();
        for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
            for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
                final double z = dataSet.getZ(xIndex, yIndex);
                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                final Color color = lCache.zInverted ? getColor(ContourDataSetRenderer.quantize(1 - offset, nQuant))
                        : getColor(ContourDataSetRenderer.quantize(offset, nQuant));

                final int x = xIndex - indexXMin;
                final int y = indexYMax - 1 - yIndex;
                pixelWriter.setColor(x, y, color);
            }
        }
        final int targetWidth = (int) localCache.xAxisWidth;
        final int targetHeight = (int) localCache.yAxisHeight;
        final Image image2 = test ? scale(image, targetWidth, targetHeight, false)
                : resample(image, targetWidth, targetHeight);

        final int tileSize = Math.max(getMinHexTileSizeProperty(), (int) lCache.xAxisWidth / lCache.xSize);
        final int nWidthInTiles = (int) (lCache.xAxisWidth / (tileSize * Math.sqrt(3))) + 1;
        final HexagonMap map2 = new HexagonMap(tileSize, image2, nWidthInTiles, (q, r, imagePixelColor, map) -> {
            final Hexagon h = new Hexagon(q, r);
            h.setFill(imagePixelColor);
            h.setStroke(imagePixelColor);

            h.setStrokeWidth(0.5);
            map.addHexagon(h);
        });

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMap - prepare");

        map2.render(gc.getCanvas());

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMap");
    }

    private void drawHexagonMapContour(final GraphicsContext gc, final AxisTransform axisTransform, final Cache lCache,
            boolean test) {
        final long start = ProcessingProfiler.getTimeStamp();

        Math.max((int) lCache.xAxisWidth / lCache.xSize, 1);
        Math.max((int) lCache.yAxisHeight / lCache.ySize, 1);
        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexXMax = lCache.indexXMax;
        final int indexYMin = lCache.indexYMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = lCache.dataSet3D;

        final int nQuant = getNumberQuantisationLevels();

        // final int hexagonHeight = 5;
        // final int paddingX = 0;
        // final int paddingY = 0;
        // final HexagonMap map = new HexagonMap(hexagonHeight);
        // map.setPadding(paddingX, paddingY);
        // for (int i = indexXMin; i < indexXMax; i++) {
        // for (int j = indexYMin; j < indexYMax; j++) {
        // final GridPosition grid = GridDrawer.pixelToPosition(i, j, 2 *
        // hexagonHeight, paddingX, paddingY);
        // final Hexagon h = new Hexagon(grid.getQ(), grid.getR());
        // map.addHexagon(h);
        // }
        // }
        //
        // for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
        // for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
        // final Hexagon h = map.getHexagonContainingPixel(xIndex, yIndex);
        // if (h == null) {
        // continue;
        // }
        //
        // final double z = dataSet.getZ(xIndex, yIndex);
        // final double offset = (axisTransform.forward(z) - zMin) / (zMax -
        // zMin);
        // final Color color = lCache.zInverted ? getColor(quantize(1 - offset,
        // nQuant))
        // : getColor(quantize(offset, nQuant));
        //
        // h.setFill(color);
        // h.setStroke(color);
        // }
        // }

        final WritableImage image = new WritableImage(xSize, ySize);
        final PixelWriter pixelWriter = image.getPixelWriter();
        for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
            for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
                final double z = dataSet.getZ(xIndex, yIndex);
                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                final Color color = lCache.zInverted ? getColor(ContourDataSetRenderer.quantize(1 - offset, nQuant))
                        : getColor(ContourDataSetRenderer.quantize(offset, nQuant));

                final int x = xIndex - indexXMin;
                final int y = indexYMax - 1 - yIndex;
                pixelWriter.setColor(x, y, color);
            }
        }
        final int targetWidth = (int) localCache.xAxisWidth;
        final int targetHeight = (int) localCache.yAxisHeight;
        final Image image2 = test ? scale(image, targetWidth, targetHeight, false)
                : resample(image, targetWidth, targetHeight);

        final int tileSize = Math.max(getMinHexTileSizeProperty(), (int) lCache.xAxisWidth / lCache.xSize);
        final int nWidthInTiles = (int) (lCache.xAxisWidth / (tileSize * Math.sqrt(3)));
        final HexagonMap hexMap = new HexagonMap(tileSize, image2, nWidthInTiles, (q, r, imagePixelColor, map) -> {
            final Hexagon h = new Hexagon(q, r);
            h.setFill(Color.TRANSPARENT); // contour being plotted
            h.setStroke(imagePixelColor);
            h.setStrokeType(StrokeType.CENTERED);

            h.setStrokeWidth(1);
            map.addHexagon(h);
        });

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour - prepare");

        // map.render(gc.getCanvas());
        hexMap.renderContour(gc.getCanvas());

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour");
    }

    private void drawHexagonMapContourAlt(final GraphicsContext gc, final AxisTransform axisTransform,
            final Cache lCache, boolean test) {
        final long start = ProcessingProfiler.getTimeStamp();

        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = lCache.dataSet3D;

        final int nQuant = getNumberQuantisationLevels();

        final double imageWidth = lCache.xAxisWidth;
        final double imageHeight = lCache.yAxisHeight;
        final int tileSize = Math.max(getMinHexTileSizeProperty(), (int) lCache.xAxisWidth / lCache.xSize);

        final HexagonMap map = new HexagonMap(tileSize);

        final double w = map.getGraphicsHorizontalDistanceBetweenHexagons();
        final double h = map.getGraphicsverticalDistanceBetweenHexagons();
        final int mapWidth = (int) (lCache.xAxisWidth / w) + 1;
        final double hexagonMapWidthInPixels = map.getGraphicsHorizontalDistanceBetweenHexagons() * mapWidth;

        final double horizontalRelation = imageWidth / hexagonMapWidthInPixels;
        final double estimatedHexMapHeightInPixels = imageHeight / horizontalRelation;

        final int mapHeight = (int) (estimatedHexMapHeightInPixels / map.getGraphicsverticalDistanceBetweenHexagons())
                + 1;

        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                final int axialQ = x - (y - (y & 1)) / 2;
                final int axialR = y;
                final Hexagon hex = new Hexagon(axialQ, axialR);
                map.addHexagon(hex);

                // int xOnImage = (int) ((hex.getGraphicsXoffset() -
                // map.getPaddingX()) / imageWidth * xSize);
                // int yOnImage = (int) ((hex.getGraphicsYoffset() -
                // map.getPaddingY()) / imageHeight * ySize);

                final int xMin = (int) ((hex.getGraphicsXoffset() - map.getPaddingX() - w / 2) / imageWidth * xSize);
                final int xMax = (int) ((hex.getGraphicsXoffset() - map.getPaddingX() + w / 2) / imageWidth * xSize);
                final int yMin = (int) ((hex.getGraphicsYoffset() - map.getPaddingY() - h / 2) / imageHeight * ySize);
                final int yMax = (int) ((hex.getGraphicsYoffset() - map.getPaddingY() + h / 2) / imageHeight * ySize);

                int count = 0;
                double z = 0;
                // integrate over pixels covered by hexagon (ie. square
                // approximation)
                for (int i = xMin; i < xMax; i++) {
                    for (int j = yMin; j < yMax; j++) {
                        z += dataSet.getZ(indexXMin + clamp(i, xSize), indexYMax - clamp(j, ySize));
                        count++;
                    }
                }
                if (count > 0) {
                    z /= count;
                }

                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                final double quant = lCache.zInverted ? ContourDataSetRenderer.quantize(1 - offset, nQuant)
                        : ContourDataSetRenderer.quantize(offset, nQuant);
                final Color color = getColor(quant);

                hex.setStroke(color);
                hex.setFill(Color.TRANSPARENT);
                hex.setUserData(Double.valueOf(quant));
                // h.draw(gc);
            }
        }

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour - prepare");

        // draw contour
        for (final Hexagon hexagon : map.getAllHexagons()) {
            // draw hexagon contour according to Node specifications
            gc.save();
            final Paint stroke = hexagon.getStroke();
            gc.setStroke(stroke);
            gc.setLineWidth(hexagon.getStrokeWidth());
            gc.setFill(hexagon.getFill());
            final double z = ((Double) hexagon.getUserData()).doubleValue();
            final List<Direction> list = new ArrayList<>();
            for (final Direction direction : Direction.values()) {
                final Hexagon neighbour = hexagon.getNeighbour(direction);
                if (neighbour == null) {
                    continue;
                }
                final double neighbourZ = ((Double) neighbour.getUserData()).doubleValue();

                if (stroke != null && z > neighbourZ) {
                    list.add(direction);
                }
            }
            // if (stroke.equals(Color.RED)) {
            // list.add(Direction.NORTHWEST);
            // list.add(Direction.SOUTHWEST);
            // gc.setStroke(Color.BLACK);
            // gc.setLineWidth(4);
            // }
            // gc.setFill(Color.RED);
            hexagon.drawHexagon(gc, list.toArray(new Direction[list.size()]));

            gc.restore();
        }

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour");
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // TODO: implement
        return null;
    }

    private Color getColor(final double offset) {
        double lowerOffset = 0.0;
        double upperOffset = 1.0;
        Color lowerColor = Color.TRANSPARENT;
        Color upperColor = Color.TRANSPARENT;

        for (final Stop stop : getColorGradient().getStops()) {
            final double currentOffset = stop.getOffset();
            if (currentOffset == offset) {
                return stop.getColor();
            } else if (currentOffset < offset) {
                lowerOffset = currentOffset;
                lowerColor = stop.getColor();
            } else {
                upperOffset = currentOffset;
                upperColor = stop.getColor();
                break;
            }
        }

        final double interpolationOffset = (offset - lowerOffset) / (upperOffset - lowerOffset);
        return lowerColor.interpolate(upperColor, interpolationOffset);
    }

    /**
     * Returns the value of the {@link #colorGradientProperty()}.
     *
     * @return the color gradient used for encoding data values
     */
    public ColorGradient getColorGradient() {
        return colorGradientProperty().get();
    }

    /**
     * Returns the value of the {@link #contourTypeProperty()}.
     *
     * @return if the chart should plot contours (true) or color gradient map (false)
     */
    public ContourType getContourType() {
        return contourTypeProperty().get();
    }

    /**
     * @return the maximum number of segments for which a contour is being drawn
     */
    public int getMaxContourSegments() {
        return maxContourSegmentsProperty().get();
    }

    public int getMinHexTileSizeProperty() {
        return minHexTileSizeProperty().get();
    }

    public int getNumberQuantisationLevels() {
        return quantisationLevelsProperty().get();
    }

    /**
     * @return the instance of this ContourDataSetRenderer.
     */
    @Override
    protected ContourDataSetRenderer getThis() {
        return this;
    }

    public Axis getZAxis() {
        final ArrayList<Axis> localAxesList = new ArrayList<>(getAxes());
        localAxesList.remove(getFirstAxis(Orientation.HORIZONTAL));
        localAxesList.remove(getFirstAxis(Orientation.VERTICAL));
        if (localAxesList.isEmpty()) {
            zAxis = new DefaultNumericAxis("z-Axis");
            zAxis.setAnimated(false);
            zAxis.setSide(Side.RIGHT);
            getAxes().add(zAxis);
        } else {
            zAxis = localAxesList.get(0);
            if (zAxis.getSide() == null) {
                zAxis.setSide(Side.RIGHT);
            }
        }
        // small cosmetic change to have colour gradient at the utmost right
        // position
        shiftZAxisToRight();
        return zAxis;
    }

    /**
     * Returns the value of the {@link #smoothProperty()}.
     *
     * @return {@code true} if the smoothing should be applied, {@code false} otherwise
     */
    public boolean isSmooth() {
        return smoothProperty().get();
    }

    protected void layoutZAxis(final Axis zAxis, final Cache lCache) {
        if (zAxis.getSide() == null || !(zAxis instanceof Node)) {
            return;
        }
        final boolean isHorizontal = zAxis.getSide().isHorizontal();
        Node zAxisNode = (Node) zAxis;
        zAxisNode.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, Boolean.TRUE);

        if (isHorizontal) {
            zAxisNode.setLayoutX(50);
            gradientRect.setX(0);
            gradientRect.setWidth(zAxis.getWidth());
            gradientRect.setHeight(20);
            zAxisNode.setLayoutX(0);
            gradientRect.setFill(new LinearGradient(0, 0, 1, 0, true, NO_CYCLE, getColorGradient().getStops()));

            if (zAxisNode.getParent() == null || !(zAxisNode.getParent() instanceof VBox)) {
                return;
            }
            final VBox parent = (VBox) zAxisNode.getParent();
            if (!parent.getChildren().contains(gradientRect)) {
                parent.getChildren().add(gradientRect);
            }
        } else {
            zAxisNode.setLayoutY(50);
            gradientRect.setWidth(20);
            gradientRect.setHeight(zAxis.getHeight());
            gradientRect.setFill(new LinearGradient(0, 1, 0, 0, true, NO_CYCLE, getColorGradient().getStops()));
            gradientRect.setLayoutX(10);

            if (zAxisNode.getParent() == null || !(zAxisNode.getParent() instanceof HBox)) {
                return;
            }
            final HBox parent = (HBox) zAxisNode.getParent();
            if (!parent.getChildren().contains(gradientRect)) {
                parent.getChildren().add(0, gradientRect);
            }
        }

        if (zAxis instanceof Region) {
            ((Region) zAxisNode).requestLayout();
        }
    }

    /**
     * @return the property controlling the maximum number of sub-segments allowed for a contour to be drawn.
     */
    public IntegerProperty maxContourSegmentsProperty() {
        return maxContourSegments;
    }

    public IntegerProperty minHexTileSizeProperty() {
        return minHexTileSize;
    }

    private void paintHeatChart(final GraphicsContext gc, final XYChart chart, final DataSet dataSet) {

        if (localCache.xSize == 0 || localCache.ySize == 0) {
            return;
        }

        final Axis zAxis = getZAxis();
        if (zAxis == null) {
            return;
        }
        final AxisTransform axisTransform = zAxis.getAxisTransform();
        if (axisTransform == null) {
            return;
        }
        switch (getContourType()) {
        case CONTOUR:
            drawContour(gc, axisTransform, localCache);
            break;
        case CONTOUR_FAST:
            drawContourFast(gc, axisTransform, localCache);
            break;
        case CONTOUR_HEXAGON:
            drawHexagonMapContourAlt(gc, axisTransform, localCache, true);
            break;
        case HEATMAP_HEXAGON:
            drawHexagonHeatMap(gc, axisTransform, localCache, true);
            break;
        case HEATMAP:
        default:
            drawHeatMap(gc, axisTransform, localCache);
            break;
        }
    }

    public IntegerProperty quantisationLevelsProperty() {
        return quantisationLevels;
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        final long start = ProcessingProfiler.getTimeStamp();
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        localDataSetList.addAll(getDatasets());

        // If there are no data sets
        if (localDataSetList.isEmpty()) {
            return;
        }

        final Axis xAxis = xyChart.getXAxis();

        // final Axis<X> xAxis = chart.getXAxis();
        final double xAxisWidth = xAxis.getWidth();
        final double xMin = xAxis.getValueForDisplay(0);
        final double xMax = xAxis.getValueForDisplay(xAxisWidth);

        long mid = ProcessingProfiler.getTimeDiff(start, "init");
        // N.B. importance of reverse order: start with last index, so that
        // most(-like) important DataSet is drawn on
        // top of the others
        for (int dataSetIndex = localDataSetList.size() - 1; dataSetIndex >= 0; dataSetIndex--) {
            final DataSet dataSet = localDataSetList.get(dataSetIndex);
            final boolean result = dataSet.lock().readLockGuard(() -> {
                long stop = ProcessingProfiler.getTimeDiff(mid, "dataSet.lock()");

                // stop = ProcessingProfiler.getTimeStamp();
                // check for potentially reduced data range we are supposed to plot

                final int indexMin = Math.max(0, dataSet.getIndex(DIM_X, xMin));
                final int indexMax = Math.min(dataSet.getIndex(DIM_X, Math.min(xMax, dataSet.getDataCount(DIM_X))),
                        dataSet.getDataCount());

                // return if zero length data set
                if (indexMax - indexMin <= 0) {
                    return false;
                }
                stop = ProcessingProfiler.getTimeDiff(stop,
                        "get min/max" + String.format(" from:%d to:%d", indexMin, indexMax));

                // final CachedDataPoints localCachedPoints = new
                // CachedDataPoints(indexMin, indexMax,
                // dataSet.getDataCount(),
                // true);
                stop = ProcessingProfiler.getTimeDiff(start, "get CachedPoints");

                // compute local screen coordinates
                // localCachedPoints.computeScreenCoordinates(chart, dataSet,
                // dataSetIndex, indexMin, indexMax);
                stop = ProcessingProfiler.getTimeDiff(stop, "computeScreenCoordinates()");

                if (dataSet.getDataCount(DIM_X) == 0 || dataSet.getDataCount(DIM_Y) == 0) {
                    return false;
                }

                updateCachedVariables(xyChart, dataSet);
                return true;
            });

            if (result) {
                // data reduction algorithm here
                // localCachedPoints.reduce();
                paintHeatChart(gc, xyChart, dataSet);
            }

            ProcessingProfiler.getTimeDiff(mid, "finished drawing");

            // localCachedPoints.release();
        } // end of 'dataSetIndex' loop

        ProcessingProfiler.getTimeDiff(start);
    }

    private Image resample(Image input, int targetWidth, int targetHeight) {
        if (input.getWidth() == 0 || input.getHeight() == 0) {
            return input;
        }
        final double width = (int) input.getWidth();
        final double height = (int) input.getHeight();
        final double scalingX = targetWidth / width;
        final double scalingY = targetHeight / height;

        final WritableImage output = new WritableImage((int) (width * scalingX), (int) (height * scalingY));

        final PixelReader reader = input.getPixelReader();
        final PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = reader.getArgb(x, y);
                for (int dy = 0; dy < scalingY; dy++) {
                    for (int dx = 0; dx < scalingX; dx++) {
                        final int targetX = (int) (x * scalingX + dx);
                        final int targetY = (int) (y * scalingY + dy);
                        writer.setArgb(targetX, targetY, argb);
                    }
                }
            }
        }

        return output;
    }

    private Image scale(Image source, int targetWidth, int targetHeight, boolean preserveRatio) {
        final ImageView imageView = new ImageView(source);
        imageView.setPreserveRatio(preserveRatio);
        imageView.setSmooth(false);
        imageView.setFitWidth(targetWidth);
        imageView.setFitHeight(targetHeight);

        return imageView.snapshot(null, null);
    }

    /**
     * Sets the value of the {@link #colorGradientProperty()}.
     *
     * @param value the gradient to be used
     */
    public void setColorGradient(final ColorGradient value) {
        colorGradientProperty().set(value);
    }

    /**
     * Sets the value of the {@link #computeLocalRangeProperty()}.
     *
     * @param value {@code true} if the local range calculation is applied, {@code false} otherwise
     */
    public void setComputeLocalRange(final boolean value) {
        computeLocalRangeProperty().set(value);
    }

    /**
     * Sets the value of the {@link #contourTypeProperty()}.
     *
     * @param value if the chart should plot contours (true) or color gradient map (false)
     */
    public void setContourType(final ContourType value) {
        contourTypeProperty().set(value);
    }

    /**
     * suppresses contour segments being drawn that have more than the specified number of sub-segments
     *
     * @param nSegments the maximum number of segments
     * @return itself
     */
    public ContourDataSetRenderer setMaxContourSegments(final int nSegments) {
        maxContourSegmentsProperty().set(nSegments);
        return this;
    }

    public ContourDataSetRenderer setMinHexTileSizeProperty(final int minSize) {
        minHexTileSizeProperty().set(minSize);
        return this;
    }

    public ContourDataSetRenderer setNumberQuantisationLevels(final int nQuantisation) {
        quantisationLevelsProperty().set(nQuantisation);
        return this;
    }

    /**
     * Sets the value of the {@link #smoothProperty()}.
     *
     * @param value {@code true} to enable smoothing
     */
    public void setSmooth(final boolean value) {
        smoothProperty().set(value);
    }

    public void shiftZAxisToLeft() {
        gradientRect.toBack();
        if (zAxis != null && zAxis instanceof Node) {
            ((Node) zAxis).toBack();
        }
    }

    public void shiftZAxisToRight() {
        gradientRect.toFront();
        if (zAxis != null && zAxis instanceof Node) {
            ((Node) zAxis).toFront();
        }
    }

    /**
     * Indicates if the chart should smooth colors between data points or render each data point as a rectangle with
     * uniform color.
     * <p>
     * By default smoothing is disabled.
     * </p>
     *
     * @return smooth property
     * @see ImageView#setFitWidth(double)
     * @see ImageView#setFitHeight(double)
     * @see ImageView#setSmooth(boolean)
     */
    public BooleanProperty smoothProperty() {
        return smooth;
    }

    private void updateCachedVariables(final XYChart chart, final DataSet dataSet) {
        final Axis xAxis = chart.getXAxis();
        final Axis yAxis = chart.getYAxis();
        final Axis zAxis = getZAxis();
        if (!(dataSet instanceof DataSet3D)) {
            return;
        }

        localCache.dataSet3D = (DataSet3D) dataSet;

        localCache.xAxisWidth = xAxis.getWidth();
        localCache.xMin = xAxis.getValueForDisplay(0);
        localCache.xMax = xAxis.getValueForDisplay(localCache.xAxisWidth);
        localCache.indexXMin = Math.max(0, dataSet.getIndex(DIM_X, localCache.xMin));
        localCache.indexXMax = Math.min(dataSet.getIndex(DIM_X, localCache.xMax), dataSet.getDataCount(DIM_X) - 1);

        localCache.yAxisHeight = yAxis.getHeight();
        localCache.yMin = yAxis.getValueForDisplay(0);
        localCache.yMax = yAxis.getValueForDisplay(localCache.yAxisHeight);
        localCache.indexYMin = Math.max(0, dataSet.getIndex(DIM_Y, localCache.yMax));
        localCache.indexYMax = Math.min(dataSet.getIndex(DIM_Y, localCache.yMin), dataSet.getDataCount(DIM_Y) - 1);

        localCache.xSize = Math.abs(localCache.indexXMax - localCache.indexXMin);
        localCache.ySize = Math.abs(localCache.indexYMax - localCache.indexYMin);

        if (computeLocalRange()) {
            ContourDataSetRenderer.computeZrange(zAxis, (DataSet3D) dataSet, localCache.indexXMin, localCache.indexXMax,
                    localCache.indexYMin, localCache.indexYMax);
        } else {
            ContourDataSetRenderer.computeZrange(zAxis, (DataSet3D) dataSet, 0, dataSet.getDataCount(DIM_X) - 1, 0,
                    dataSet.getDataCount(DIM_Y) - 1);
        }

        localCache.zMin = zAxis.getMin();
        localCache.zMax = zAxis.getMax();
        // localCache.zMinTransformed = zAxis.getAxisTransform().forward(zAxis.getLowerBound());
        // localCache.zMaxTransformed = zAxis.getAxisTransform().forward(zAxis.getUpperBound());
        // localCache.xInverted = xAxis.isInvertedAxis();
        // localCache.yInverted = yAxis.isInvertedAxis();
        localCache.zInverted = zAxis.isInvertedAxis();
        layoutZAxis(getZAxis(), localCache);
    }

    private static void computeZrange(final Axis zAxis, final DataSet3D dataSet3D, final int indexXMin,
            final int indexXMax, final int indexYMin, final int indexYMax) {
        if (!zAxis.isAutoRanging() && !zAxis.isAutoGrowRanging()) {
            // keep previous and/or user-set axis range
            return;
        }

        double zMin = +Double.MAX_VALUE;
        double zMax = -Double.MAX_VALUE;
        for (int xIndex = indexXMin; xIndex < indexXMax; xIndex++) {
            for (int yIndex = indexYMin; yIndex < indexYMax; yIndex++) {
                final double z = dataSet3D.getZ(xIndex, yIndex);
                zMin = Math.min(zMin, z);
                zMax = Math.max(zMax, z);
            }
        }

        if (zAxis.isAutoRanging()) {
            zAxis.set(zMin, zMax);
        }

        if (zAxis.isAutoGrowRanging()) {
            zAxis.set(Math.min(zMin, zAxis.getMin()), Math.max(zMax, zAxis.getMax()));
        }
    }

    public static double convolution(final double[][] pixelMatrix) {

        final double gy = pixelMatrix[0][0] * -1 + pixelMatrix[0][1] * -2 + pixelMatrix[0][2] * -1 + pixelMatrix[2][0]
                + pixelMatrix[2][1] * 2 + pixelMatrix[2][2] * 1;
        final double gx = pixelMatrix[0][0] + pixelMatrix[0][2] * -1 + pixelMatrix[1][0] * 2 + pixelMatrix[1][2] * -2
                + pixelMatrix[2][0] + pixelMatrix[2][2] * -1;
        return Math.sqrt(Math.pow(gy, 2) + Math.pow(gx, 2));

    }

    public static double erosionConvolution(final double[][] pixelMatrix) {
        double sum = 0.0;
        sum += pixelMatrix[0][0];
        sum += pixelMatrix[0][1];
        sum += pixelMatrix[0][2];
        sum += pixelMatrix[1][0];
        sum += pixelMatrix[1][1];
        sum += pixelMatrix[1][2];
        sum += pixelMatrix[2][0];
        sum += pixelMatrix[2][1];
        sum += pixelMatrix[2][2];
        return sum;
    }

    public static double erosionConvolution2(final double[][] pixelMatrix) {
        double sum = 0.0;
        sum += pixelMatrix[0][0];
        // sum += pixelMatrix[0][1];
        sum += pixelMatrix[0][2];
        // sum += pixelMatrix[1][0];
        sum += pixelMatrix[1][1];
        // sum += pixelMatrix[1][2];
        sum += pixelMatrix[2][0];
        // sum += pixelMatrix[2][1];
        sum += pixelMatrix[2][2];
        return sum;
    }

    private static void erosionOperator(final double[][] input, final double[][] output, final double zMin,
            final double zMax, final double level) {
        final int width = input.length;
        final int height = input[0].length;
        final double[][] gX = new double[width][height];
        final double[][] gY = new double[width][height];

        final double[][] pixelMatrix = new double[3][3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (i == 0 || i == width - 1 || j == 0 || j == height - 1) {
                    gX[i][j] = gY[i][j] = output[i][j] = 0;
                } else {
                    pixelMatrix[0][0] = input[i - 1][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[0][1] = input[i - 1][j] > level ? 1.0 : 0.0;
                    pixelMatrix[0][2] = input[i - 1][j + 1] > level ? 1.0 : 0.0;
                    pixelMatrix[1][0] = input[i][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[1][2] = input[i][j + 1] > level ? 1.0 : 0.0;
                    pixelMatrix[2][0] = input[i + 1][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[2][1] = input[i + 1][j] > level ? 1.0 : 0.0;
                    pixelMatrix[2][2] = input[i + 1][j + 1] > level ? 1.0 : 0.0;

                    final double zNorm = ContourDataSetRenderer.erosionConvolution(pixelMatrix);
                    output[i][j] = zNorm > 4 ? 1.0 : 0.0;

                    // output[i][j] = zNorm;
                }
            }
        }
    }

    private static double quantize(final double value, final int nLevels) {
        return Math.round(value * nLevels) / (double) nLevels;
    }

    private static void sobelOperator(final double[][] input, final double[][] output, final double zMin,
            final double zMax, final double level) {
        final int width = input.length;
        final int height = input[0].length;
        final double[][] gX = new double[width][height];
        final double[][] gY = new double[width][height];

        final double[][] pixelMatrix = new double[3][3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (i == 0 || i == width - 1 || j == 0 || j == height - 1) {
                    gX[i][j] = gY[i][j] = output[i][j] = 0;
                } else {
                    // Gx[i][j] = input[i + 1][j - 1] + 2 * input[i + 1][j] +
                    // input[i + 1][j + 1];
                    // Gx[i][j] -= input[i - 1][j - 1] + 2 * input[i - 1][j] +
                    // input[i - 1][j + 1];
                    // Gy[i][j] = input[i - 1][j + 1] + 2 * input[i][j + 1] +
                    // input[i + 1][j + 1];
                    // Gy[i][j] -= input[i - 1][j - 1] + 2 * input[i][j - 1] +
                    // input[i + 1][j - 1];

                    // Roberts Cross
                    gX[i][j] = -1.0 * input[i][j - 1] - 0.0 * input[i][j] + 0.0 * input[i][j + 1];
                    gX[i][j] += 0.0 * input[i][j - 1] + 1.0 * input[i][j] + 0.0 * input[i + 1][j + 1];

                    gY[i][j] = 0.0 * input[i][j - 1] - 1.0 * input[i][j] + 0.0 * input[i][j + 1];
                    gY[i][j] += 1.0 * input[i][j - 1] - 0.0 * input[i][j] + 0.0 * input[i + 1][j + 1];

                    double zNorm = Math.abs(gX[i][j]) + Math.abs(gY[i][j]);// -
                                                                           // zMin)
                                                                           // /
                                                                           // (zMax
                                                                           // -
                                                                           // zMin);

                    pixelMatrix[0][0] = input[i - 1][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[0][1] = input[i - 1][j] > level ? 1.0 : 0.0;
                    pixelMatrix[0][2] = input[i - 1][j + 1] > level ? 1.0 : 0.0;
                    pixelMatrix[1][0] = input[i][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[1][2] = input[i][j + 1] > level ? 1.0 : 0.0;
                    pixelMatrix[2][0] = input[i + 1][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[2][1] = input[i + 1][j] > level ? 1.0 : 0.0;
                    pixelMatrix[2][2] = input[i + 1][j + 1] > level ? 1.0 : 0.0;

                    zNorm = ContourDataSetRenderer.convolution(pixelMatrix);
                    output[i][j] = zNorm;// > level ? 1.0 : 0.0;

                    // output[i][j] = zNorm;
                }
            }
        }
    }

    private class Cache {

        protected DataSet3D dataSet3D;

        protected double xAxisWidth;
        protected double xMin;
        protected double xMax;
        protected int indexXMin;
        protected int indexXMax;

        protected double yAxisHeight;
        protected double yMin;
        protected double yMax;
        protected int indexYMin;
        protected int indexYMax;
        protected int xSize;
        protected int ySize;
        protected double zMin;
        protected double zMax;
        // protected double zMinTransformed;
        // protected double zMaxTransformed;
        // protected boolean xInverted;
        // protected boolean yInverted;
        protected boolean zInverted;
    }

}
