package de.gsi.chart.renderer.spi;

import static javafx.scene.paint.CycleMethod.NO_CYCLE;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.hexagon.Hexagon;
import de.gsi.chart.renderer.spi.hexagon.HexagonMap;
import de.gsi.chart.renderer.spi.hexagon.HexagonMap.Direction;
import de.gsi.chart.renderer.spi.marchingsquares.GeneralPath;
import de.gsi.chart.renderer.spi.marchingsquares.MarchingSquares;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.WritableImageCache;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * Default renderer for the display of 3D surface-type DataSets.
 * 
 * The following drawing options controlled via {@link #setContourType(de.gsi.chart.renderer.ContourType)} are provided:
 * <ul>
 * <li>CONTOUR: marching-square based contour plotting algorithm, see e.g.
 * <a href="https://en.wikipedia.org/wiki/Marching_squares#Isoline">reference</a>
 * <li>CONTOUR_FAST: an experimental contour plotting algorithm,
 * <li>CONTOUR_HEXAGON: a hexagon-map based contour plotting algorithm,
 * <li>HEATMAP: an 2D orthogonal projection based plotting algorithm
 * <li>HEATMAP_HEXAGON: an 2D orthogonal hexagon-projection based plotting algorithm.
 * </ul>
 * 
 * Most of the internal processing algorithm are parallelised which can be controlled via
 * {@link #setParallelImplementation(boolean)}. A data reduction is performed in case the number of data points exceed
 * the the underlying number Canvas pixels number in order to improve efficiency and required texture GPU buffer. This
 * data reduction is controlled via {@link #setPointReduction(boolean)} and the reduction type (MIN, MAX, AVERAGE,
 * DOWN_SAMPLE) via {@link #setReductionType}, and the {@link #setReductionFactorX(int)} and
 * {@link #setReductionFactorY(int)} functions.
 * 
 * N.B. Regarding implementation of user-level DataSet interfaces: While the DataSet3D::getZ(int) and
 * DataSet::get(DIM_Z, int) routines should match, the DataSet3D is considered a convenience interface primarily to be
 * used for external user-level code.
 * 
 * This renderer primarily relies for performance reasons internally on the more generic DataSet::get(DIM_Z, int index)
 * interface. DataSet::get(DIM_Z, int index) is assumed to be a row-major ordered matrix with the (0, 0) coordinate (ie.
 * first index get(DIM_Z, 0)) being drawn at the bottom left corner of the canvas for non-inverted axes.
 *
 * @author rstein
 */
public class ContourDataSetRenderer extends AbstractContourDataSetRendererParameter<ContourDataSetRenderer>
        implements Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContourDataSetRenderer.class);

    private ContourDataSetCache localCache;
    private Axis zAxis;
    protected final Rectangle gradientRect = new Rectangle();

    private int clamp(int value, int range) {
        return Math.max(Math.min(value, range), 0);
    }

    private void drawContour(final GraphicsContext gc, final ContourDataSetCache lCache) {
        final double[] levels = new double[getNumberQuantisationLevels()];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = (i + 1) / (double) levels.length;
        }

        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double[][] data = new double[ySize][xSize];
        for (int yIndex = 0; yIndex < ySize; yIndex++) {
            for (int xIndex = 0; xIndex < xSize; xIndex++) {
                final double offset = lCache.reduced[yIndex * xSize + xIndex];
                data[ySize - 1 - yIndex][xIndex] = offset;
            }
        }

        // abort if min/max == 0 -> cannot compute contours
        final double zRange = Math.abs(lCache.zMax - lCache.zMin);
        if (zRange <= 0) {
            return;
        }

        final ColorGradient colorGradient = getColorGradient();
        final MarchingSquares marchingSquares = new MarchingSquares();
        final double scaleX = lCache.xDataPixelRange / xSize;
        final double scaleY = lCache.yDataPixelRange / ySize;
        gc.save();
        gc.translate(lCache.xDataPixelMin, lCache.yDataPixelMin);
        gc.scale(scaleX, scaleY);
        final GeneralPath[] isolines;
        try {
            isolines = marchingSquares.buildContours(data, levels);
            int levelCount = 0;
            for (final GeneralPath path : isolines) {
                if (path.size() > getMaxContourSegments()) {
                    levelCount++;
                    continue;
                }
                final Color color = lCache.zInverted ? colorGradient.getColor(1 - levels[levelCount++])
                                                     : colorGradient.getColor(levels[levelCount++]);
                gc.setStroke(color);
                gc.setLineDashes(1.0);
                gc.setMiterLimit(10);
                gc.setFill(color);
                gc.setLineWidth(0.5);
                path.draw(gc);
            }
        } catch (InterruptedException | ExecutionException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("marchingSquares algorithm");
            }
        } finally {
            gc.restore();
        }
    }

    private void drawContourFast(final GraphicsContext gc, final AxisTransform axisTransform,
            final ContourDataSetCache lCache) {
        final long start = ProcessingProfiler.getTimeStamp();
        final int xSize = lCache.xSize;
        final int ySize = lCache.ySize;
        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);

        // N.B. works only since OpenJFX 12!! fall-back for JDK8 is the old implementation
        gc.setImageSmoothing(isSmooth());

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

        final int length = xSize * ySize;
        for (int i = 0; i < length; i++) {
            final int x = i % xSize;
            final int y = i / xSize;
            input[x][y] = lCache.reduced[i];
        }

        final WritableImage image = WritableImageCache.getInstance().getImage(xSize, ySize);
        final PixelWriter pixelWriter = image.getPixelWriter();
        if (pixelWriter == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().log("Could not get PixelWriter for image");
            }
            return;
        }

        final ColorGradient colorGradient = getColorGradient();
        for (final double level : levels) {
            ContourDataSetRenderer.sobelOperator(input, output2, zMin, zMax, level);
            ContourDataSetRenderer.erosionOperator(output2, output, zMin, zMax, level);

            for (int yIndex = 0; yIndex < ySize; yIndex++) {
                final int yIndex2 = ySize - 1 - yIndex;
                for (int xIndex = 0; xIndex < xSize; xIndex++) {
                    final double z = output[xIndex][yIndex];

                    if (z <= 0) {
                        continue;
                    }
                    Color color = lCache.zInverted ? colorGradient.getColor(1 - level) : colorGradient.getColor(level);

                    pixelWriter.setColor(xIndex, yIndex2, color);
                }
            }
        }

        gc.drawImage(image, lCache.xDataPixelMin, lCache.yDataPixelMin, lCache.xDataPixelRange, lCache.yDataPixelRange);
        
        WritableImageCache.getInstance().add(image);
        ProcessingProfiler.getTimeDiff(start, "sobel");
    }

    private void drawHeatMap(final GraphicsContext gc, final ContourDataSetCache lCache) {
        final long start = ProcessingProfiler.getTimeStamp();

        // N.B. works only since OpenJFX 12!! fall-back for JDK8 is the old implementation
        gc.setImageSmoothing(isSmooth());

        // process z quantisation to colour transform
        final WritableImage image = ContourDataSetCache.convertDataArrayToImage(lCache.reduced, lCache.xSize, lCache.ySize, getColorGradient());
        ProcessingProfiler.getTimeDiff(start, "color map");

        gc.drawImage(image, lCache.xDataPixelMin, lCache.yDataPixelMin, lCache.xDataPixelRange, lCache.yDataPixelRange);

        WritableImageCache.getInstance().add(image);
        ProcessingProfiler.getTimeDiff(start, "drawHeatMap");
    }

    private void drawHeatMapOld(final GraphicsContext gc, final AxisTransform axisTransform,
            final ContourDataSetCache lCache) {
        if (!(lCache.dataSet instanceof DataSet3D)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(lCache.dataSet).log("dataSet {} is not of type DataSet3D -> early return");
            }
            return;
        }
        final long start = ProcessingProfiler.getTimeStamp();

        final int scaleX = isSmooth() ? 1 : Math.max((int) lCache.xAxisWidth / lCache.xSize, 1);
        final int scaleY = isSmooth() ? 1 : Math.max((int) lCache.yAxisHeight / lCache.ySize, 1);

        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexXMax = lCache.indexXMax;
        final int indexYMin = lCache.indexYMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = (DataSet3D) lCache.dataSet;
        final int xSize = Math.abs(indexXMax - indexXMin) + 1;
        final int ySize = Math.abs(indexYMax - indexYMin) + 1;

        final int nQuant = getNumberQuantisationLevels();
        final ColorGradient colorGradient = getColorGradient();
        final WritableImage image = WritableImageCache.getInstance().getImage(xSize * scaleX, ySize * scaleY);

        final PixelWriter pixelWriter = image.getPixelWriter();
        if (pixelWriter == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().log("Could not get PixelWriter for image");
            }
            return;
        }
        for (int xIndex = 0; xIndex < xSize; xIndex++) {
            for (int yIndex = 0; yIndex < ySize; yIndex++) {
                final double z = dataSet.getZ(xIndex + indexXMin, yIndex + indexYMin);
                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                final Color color = lCache.zInverted ? colorGradient.getColor(quantize(1 - offset, nQuant))
                                                     : colorGradient.getColor(quantize(offset, nQuant));

                final int x = xIndex * scaleX;
                final int y = (ySize - 1 - yIndex) * scaleY;
                for (int dx = 0; dx < scaleX; dx++) {
                    for (int dy = 0; dy < scaleY; dy++) {
                        pixelWriter.setColor(x + dx, y + dy, color);
                    }
                }
            }
        }

        gc.drawImage(image, lCache.xDataPixelMin, lCache.yDataPixelMin, lCache.xDataPixelRange, lCache.yDataPixelRange);
        ProcessingProfiler.getTimeDiff(start, "drawHeatMap");
    }

    private void drawHexagonHeatMap(final GraphicsContext gc, final ContourDataSetCache lCache) {
        final long start = ProcessingProfiler.getTimeStamp();

        // process z quantisation to colour transform
        final WritableImage image = ContourDataSetCache.convertDataArrayToImage(lCache.reduced, lCache.xSize, lCache.ySize,
                getColorGradient());

        final int tileSize = Math.max(getMinHexTileSizeProperty(), (int) lCache.xAxisWidth / lCache.xSize);
        final int nWidthInTiles = (int) (lCache.xAxisWidth / (tileSize * Math.sqrt(3))) + 1;

        final HexagonMap map2 = new HexagonMap(tileSize, image, nWidthInTiles, (q, r, imagePixelColor, map) -> {
            final Hexagon h = new Hexagon(q, r);
            h.setFill(imagePixelColor);
            h.setStroke(imagePixelColor);

            h.setStrokeWidth(0.5);
            map.addHexagon(h);
        });
        WritableImageCache.getInstance().add(image);

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMap - prepare");
        final double scaleX = lCache.xDataPixelRange / lCache.xAxisWidth;
        final double scaleY = lCache.yDataPixelRange / lCache.yAxisHeight;

        gc.save();
        gc.translate(lCache.xDataPixelMin, lCache.yDataPixelMin);
        gc.scale(scaleX, scaleY);

        map2.render(gc.getCanvas());
        gc.restore();

        // gc.drawImage(image, lCache.xDataPixelMin, lCache.yDataPixelMin, lCache.xDataPixelRange,
        // lCache.yDataPixelRange);

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMap");
    }

    private void drawHexagonMapContour(final GraphicsContext gc, final ContourDataSetCache lCache) {
        final long start = ProcessingProfiler.getTimeStamp();

        // process z quantisation to colour transform
        final WritableImage image = ContourDataSetCache.convertDataArrayToImage(lCache.reduced, lCache.xSize, lCache.ySize,
                getColorGradient());

        final int tileSize = Math.max(getMinHexTileSizeProperty(), (int) lCache.xAxisWidth / lCache.xSize);
        final int nWidthInTiles = (int) (lCache.xAxisWidth / (tileSize * Math.sqrt(3)));
        final HexagonMap hexMap = new HexagonMap(tileSize, image, nWidthInTiles, (q, r, imagePixelColor, map) -> {
            final Hexagon h = new Hexagon(q, r);
            h.setFill(Color.TRANSPARENT); // contour being plotted
            h.setStroke(imagePixelColor);
            h.setStrokeType(StrokeType.CENTERED);

            h.setStrokeWidth(1);
            map.addHexagon(h);
        });
        WritableImageCache.getInstance().add(image);

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour - prepare");

        final double scaleX = lCache.xDataPixelRange / lCache.xAxisWidth;
        final double scaleY = lCache.yDataPixelRange / lCache.yAxisHeight;
        gc.save();
        gc.translate(lCache.xDataPixelMin, lCache.yDataPixelMin);
        gc.scale(scaleX, scaleY);

        hexMap.renderContour(gc.getCanvas());
        gc.restore();

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour");
    }

    private void drawHexagonMapContourAlt(final GraphicsContext gc, final AxisTransform axisTransform,
            final ContourDataSetCache lCache) {
        if (!(lCache.dataSet instanceof DataSet3D)) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(lCache.dataSet).log("dataSet {} is not of type DataSet3D -> early return");
            }
            return;
        }
        final long start = ProcessingProfiler.getTimeStamp();

        final double zMin = axisTransform.forward(lCache.zMin);
        final double zMax = axisTransform.forward(lCache.zMax);
        final int indexXMin = lCache.indexXMin;
        final int indexXMax = lCache.indexXMax;
        final int indexYMin = lCache.indexYMin;
        final int indexYMax = lCache.indexYMax;
        final DataSet3D dataSet = (DataSet3D) lCache.dataSet;
        final int xSize = Math.abs(indexXMax - indexXMin) + 1;
        final int ySize = Math.abs(indexYMax - indexYMin) + 1;

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
        final ColorGradient colorGradient = getColorGradient();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                final int axialQ = x - (y - (y & 1)) / 2;
                final int axialR = y;
                final Hexagon hex = new Hexagon(axialQ, axialR);
                map.addHexagon(hex);

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
                        z += dataSet.getZ(indexXMin + clamp(i, xSize - 1), indexYMax - clamp(j, ySize - 1));
                        count++;
                    }
                }
                if (count > 0) {
                    z /= count;
                }

                final double offset = (axisTransform.forward(z) - zMin) / (zMax - zMin);
                final double quant = lCache.zInverted ? ContourDataSetRenderer.quantize(1 - offset, nQuant)
                                                      : ContourDataSetRenderer.quantize(offset, nQuant);
                final Color color = colorGradient.getColor(quant);

                hex.setStroke(color);
                hex.setFill(Color.TRANSPARENT);
                hex.setUserData(Double.valueOf(quant));
            }
        }

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour - prepare");

        final double scaleX = lCache.xDataPixelRange / lCache.xAxisWidth;
        final double scaleY = lCache.yDataPixelRange / lCache.yAxisHeight;
        gc.save();
        gc.translate(lCache.xDataPixelMin, lCache.yDataPixelMin);
        gc.scale(scaleX, scaleY);

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

            hexagon.drawHexagon(gc, list.toArray(new Direction[list.size()]));

            gc.restore();
        }
        gc.restore();

        ProcessingProfiler.getTimeDiff(start, "drawHexagonMapContour");
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        // TODO: implement
        return null;
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

    protected void layoutZAxis(final Axis zAxis) {
        if (zAxis.getSide() == null || !(zAxis instanceof Node)) {
            return;
        }
        Node zAxisNode = (Node) zAxis;
        zAxisNode.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, Boolean.TRUE);

        if (zAxis.getSide().isHorizontal()) {
            zAxisNode.setLayoutX(50);
            gradientRect.setX(0);
            gradientRect.setWidth(zAxis.getWidth());
            gradientRect.setHeight(20);
            zAxisNode.setLayoutX(0);
            gradientRect.setFill(new LinearGradient(0, 0, 1, 0, true, NO_CYCLE, getColorGradient().getStops()));

            if (!(zAxisNode.getParent() instanceof VBox)) {
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

            if (!(zAxisNode.getParent() instanceof HBox)) {
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

    private void paintCanvas(final GraphicsContext gc) {
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
            drawContour(gc, localCache);
            break;
        case CONTOUR_FAST:
            drawContourFast(gc, axisTransform, localCache);
            break;
        case CONTOUR_HEXAGON:
            if (isAltImplementation()) {
                drawHexagonMapContour(gc, localCache);
            } else {
                drawHexagonMapContourAlt(gc, axisTransform, localCache);
            }
            break;
        case HEATMAP_HEXAGON:
            drawHexagonHeatMap(gc, localCache);
            break;
        case HEATMAP:
        default:
            if (isAltImplementation()) {
                drawHeatMapOld(gc, axisTransform, localCache);
            } else {
                drawHeatMap(gc, localCache);
            }
            break;
        }
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        final long start = ProcessingProfiler.getTimeStamp();
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        localDataSetList.addAll(getDatasets());

        // If there are no data sets
        if (localDataSetList.isEmpty()) {
            return;
        }

        final XYChart xyChart = (XYChart) chart;
        long mid = ProcessingProfiler.getTimeDiff(start, "init");
        // N.B. importance of reverse order: start with last index, so that
        // most(-like) important DataSet is drawn on
        // top of the others
        for (int dataSetIndex = localDataSetList.size() - 1; dataSetIndex >= 0; dataSetIndex--) {
            final DataSet dataSet = localDataSetList.get(dataSetIndex);
            if (dataSet.getDimension() <= 2) {
                // minimum dimension criteria not met
                continue;
            }
            final boolean result = dataSet.lock().readLockGuard(() -> {
                long stop = ProcessingProfiler.getTimeDiff(mid, "dataSet.lock()");

                if (dataSet.getDataCount(DIM_X) == 0 || dataSet.getDataCount(DIM_Y) == 0) {
                    return false;
                }

                localCache = new ContourDataSetCache(xyChart, this, dataSet); // NOPMD
                ProcessingProfiler.getTimeDiff(stop, "updateCachedVariables");
                return true;
            });

            if (result) {
                layoutZAxis(getZAxis());
                // data reduction algorithm here
                paintCanvas(gc);

                localCache.releaseCachedVariables();
            }

            ProcessingProfiler.getTimeDiff(mid, "finished drawing");

        } // end of 'dataSetIndex' loop

        ProcessingProfiler.getTimeDiff(start);
    }

    public void shiftZAxisToLeft() {
        gradientRect.toBack();
        if (zAxis instanceof Node) {
            ((Node) zAxis).toBack();
        }
    }

    public void shiftZAxisToRight() {
        gradientRect.toFront();
        if (zAxis instanceof Node) {
            ((Node) zAxis).toFront();
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
        return ((int) (value * nLevels)) / (double) nLevels;
        // original: return Math.round(value * nLevels) / (double) nLevels;
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

                    double zNorm = Math.abs(gX[i][j]) + Math.abs(gY[i][j]);
                    // - zMin) / (zMax - zMin);

                    pixelMatrix[0][0] = input[i - 1][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[0][1] = input[i - 1][j] > level ? 1.0 : 0.0;
                    pixelMatrix[0][2] = input[i - 1][j + 1] > level ? 1.0 : 0.0;
                    pixelMatrix[1][0] = input[i][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[1][2] = input[i][j + 1] > level ? 1.0 : 0.0;
                    pixelMatrix[2][0] = input[i + 1][j - 1] > level ? 1.0 : 0.0;
                    pixelMatrix[2][1] = input[i + 1][j] > level ? 1.0 : 0.0;
                    pixelMatrix[2][2] = input[i + 1][j + 1] > level ? 1.0 : 0.0;

                    zNorm = ContourDataSetRenderer.convolution(pixelMatrix);
                    output[i][j] = zNorm; // > level ? 1.0 : 0.0;

                    // output[i][j] = zNorm;
                }
            }
        }
    }
}
