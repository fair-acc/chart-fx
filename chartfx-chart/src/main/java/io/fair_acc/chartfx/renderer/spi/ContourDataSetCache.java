package io.fair_acc.chartfx.renderer.spi;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.fair_acc.dataset.utils.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisTransform;
import io.fair_acc.chartfx.renderer.ContourType;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer3D;
import io.fair_acc.chartfx.renderer.datareduction.ReductionType;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;
import io.fair_acc.chartfx.utils.WritableImageCache;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.spi.DataRange;

/**
 * @author rstein
 */
class ContourDataSetCache extends WritableImageCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContourDataSetCache.class);
    private static final String PARALLEL_WORKER_ERROR = "one parallel worker thread finished execution with error";
    private static final int BGRA_BYTE_SIZE = 4;
    private static final int REF_WIDTH_PARALLEL = 1024;
    private static final int REF_HEIGHT_PARALLEL = 1000;

    protected final DataSet dataSet;
    protected final Axis xAxis;
    protected final Axis yAxis;
    protected final Axis zAxis;

    protected double xAxisWidth;
    protected double yAxisHeight;
    protected double xMin;
    protected double yMin;
    protected double xMax;
    protected double yMax;

    protected double xDataPixelMin;
    protected double xDataPixelMax;
    protected double xDataPixelRange;

    protected double yDataPixelMin;
    protected double yDataPixelMax;
    protected double yDataPixelRange;

    protected int indexXMin;
    protected int indexXMax;
    protected int indexYMin;
    protected int indexYMax;

    protected int xSize;
    protected int ySize;
    protected double zMin;
    protected double zMax;

    protected final boolean xInverted;
    protected final boolean yInverted;
    protected final boolean zInverted;

    // temp data variables
    protected final double[] dataBuffer;
    protected double[] tempDataBuffer;
    protected final double[] reduced;

    public ContourDataSetCache(final XYChart chart, final ContourDataSetRenderer renderer, final DataSet dataSet) {
        if (dataSet.getDimension() < 3) {
            throw new IllegalArgumentException("dataSet needs be at least 3D but is " + dataSet.getDimension());
        }
        if (!(dataSet instanceof GridDataSet)) {
            throw new IllegalArgumentException("dataSet needs be GridDataSet");
        }
        final GridDataSet gridDataSet = (GridDataSet) dataSet;
        if (gridDataSet.getNGrid() < 2) {
            throw new IllegalArgumentException("Contour Renderer only supports 2D Grids");
        }
        final long start = ProcessingProfiler.getTimeStamp();
        this.dataSet = dataSet;
        this.xAxis = renderer.xAxis;
        this.yAxis = renderer.yAxis;
        this.zAxis = renderer.zAxis;

        // zMin/zMax from the axis are usually either DataSet driven (via computeLimits)
        // or user-defined limits on the z axis
        this.zMin = zAxis.getMin();
        this.zMax = zAxis.getMax();

        xInverted = xAxis.isInvertedAxis();
        yInverted = yAxis.isInvertedAxis();
        zInverted = zAxis.isInvertedAxis();

        this.xAxisWidth = xAxis.getWidth();
        this.yAxisHeight = yAxis.getHeight();

        // real-world coordinates for given pixel coordinates
        // N.B. (0,0) in pixel coordinates is on the top-left corner of the canvas -> asymmetry between x and y
        this.xMin = xInverted ? xAxis.getValueForDisplay(this.xAxisWidth) : xAxis.getValueForDisplay(0);
        this.xMax = xInverted ? xAxis.getValueForDisplay(0) : xAxis.getValueForDisplay(this.xAxisWidth);
        this.yMin = yInverted ? yAxis.getValueForDisplay(0) : yAxis.getValueForDisplay(this.yAxisHeight);
        this.yMax = yInverted ? yAxis.getValueForDisplay(this.yAxisHeight) : yAxis.getValueForDisplay(0);

        // sorted pixel coordinates of DataSet range extremities
        final double xDataPixelMinTemp = xAxis.getDisplayPosition(dataSet.getAxisDescription(DIM_X).getMin());
        final double xDataPixelMaxTemp = xAxis.getDisplayPosition(dataSet.getAxisDescription(DIM_X).getMax());
        final double yDataPixelMinTemp = yAxis.getDisplayPosition(dataSet.getAxisDescription(DIM_Y).getMax());
        final double yDataPixelMaxTemp = yAxis.getDisplayPosition(dataSet.getAxisDescription(DIM_Y).getMin());
        this.xDataPixelMin = Math.max(Math.min(xDataPixelMinTemp, xDataPixelMaxTemp), 0);
        this.xDataPixelMax = Math.min(Math.max(xDataPixelMinTemp, xDataPixelMaxTemp), this.xAxisWidth);
        this.yDataPixelMin = Math.max(Math.min(yDataPixelMinTemp, yDataPixelMaxTemp), 0);
        this.yDataPixelMax = Math.min(Math.max(yDataPixelMinTemp, yDataPixelMaxTemp), this.yAxisHeight);
        this.xDataPixelRange = Math.abs(this.xDataPixelMax - this.xDataPixelMin);
        this.yDataPixelRange = Math.abs(this.yDataPixelMax - this.yDataPixelMin);

        // min/max data indices w.r.t. DataSet index binning
        final int indexXMinTemp = Math.max(0, gridDataSet.getGridIndex(DIM_X, this.xMin));
        final int indexXMaxTemp = Math.min(gridDataSet.getGridIndex(DIM_X, this.xMax), gridDataSet.getShape(DIM_X) - 1);
        final int indexYMinTemp = Math.max(0, gridDataSet.getGridIndex(DIM_Y, this.yMax));
        final int indexYMaxTemp = Math.min(gridDataSet.getGridIndex(DIM_Y, this.yMin), gridDataSet.getShape(DIM_Y) - 1);
        this.indexXMin = Math.min(indexXMinTemp, indexXMaxTemp);
        this.indexXMax = Math.max(indexXMinTemp, indexXMaxTemp);
        this.indexYMin = Math.min(indexYMinTemp, indexYMaxTemp);
        this.indexYMax = Math.max(indexYMinTemp, indexYMaxTemp);
        this.xSize = Math.abs(this.indexXMax - this.indexXMin) + 1;
        this.ySize = Math.abs(this.indexYMax - this.indexYMin) + 1;

        // copy- transform data
        dataBuffer = DoubleArrayCache.getInstance().getArrayExact(this.xSize * this.ySize);
        // TODO: tune this limit
        final int minSizeThreshold = REF_WIDTH_PARALLEL * REF_HEIGHT_PARALLEL;
        final boolean sufficientlyLarge = xSize * ySize < minSizeThreshold;
        copySubFrame(dataSet, dataBuffer, renderer.isParallelImplementation() && sufficientlyLarge, //
                xInverted, indexXMin, indexXMax, yInverted, indexYMin, indexYMax);
        ProcessingProfiler.getTimeDiff(start, "copySubFrame");

        // reduce data if necessary
        reduced = reduceDataArray(dataBuffer, xSize, ySize, renderer); // NOPMD
        ProcessingProfiler.getTimeDiff(start, "data reduction");

        // compute local Range
        final boolean computeLocalRange = renderer.computeLocalRange()
                                          && (zAxis.isAutoRanging() || zAxis.isAutoGrowRanging());
        final DataRange zDataRange = computeLocalRange(reduced, xSize, ySize, computeLocalRange);
        if (zDataRange.isDefined()) {
            zMin = zDataRange.getMin();
            zMax = zDataRange.getMax();
        }
        ProcessingProfiler.getTimeDiff(start, "recompute local z range");

        // process continuous to quantised z values
        final AxisTransform axisTransform = zAxis.getAxisTransform();
        if (axisTransform == null) {
            throw new IllegalArgumentException("zAxis of renderer needs to have an axis transform for its z-Axis");
        }
        final int nQuant = renderer.getNumberQuantisationLevels();
        quantizeData(reduced, xSize, ySize, zInverted, zMin, zMax, axisTransform, nQuant);
        ProcessingProfiler.getTimeDiff(start, "quantized data");
    }

    protected static void quantizeData(final double[] input, final int width, final int height, final boolean inverted,
            final double min, final double max, final AxisTransform axisTransform, final int nQuant) {
        final double zMinPixel = axisTransform.forward(min);
        final double zRange = Math.abs(axisTransform.forward(max) - zMinPixel);
        final double zRangeInv = 1.0 / zRange;

        final int length = width * height;
        for (int index = 0; index < length; index++) {
            final double z = input[index];
            final double offset = ((axisTransform.forward(z) - zMinPixel) * zRangeInv);
            input[index] = inverted ? quantize(1 - offset, nQuant) : quantize(offset, nQuant);
        }
    }

    public void releaseCachedVariables() {
        DoubleArrayCache.getInstance().add(dataBuffer);
        DoubleArrayCache.getInstance().add(tempDataBuffer);
    }

    protected double[] reduceDataArray(final double[] input, final int srcWidth, final int srcHeight,
            final ContourDataSetRenderer renderer) {
        final int reductionFactorX = Math.max(renderer.getReductionFactorX(), 1);
        final int reductionFactorY = Math.max(renderer.getReductionFactorY(), 1);
        final ReductionType reductionType = renderer.getReductionType();
        final double dataPixelSizeX = (double) reductionFactorX * xSize / xAxisWidth;
        final double dataPixelSizeY = (double) reductionFactorY * ySize / yAxisHeight;
        final boolean mayReduceX = dataPixelSizeX > 1.0 && xSize > 10;
        final boolean mayReduceY = dataPixelSizeY > 1.0 && ySize > 10;

        final double[] reducedData;
        if ((mayReduceX || mayReduceY) && renderer.isActualReducePoints()) {
            int targetWidth = (int) (srcWidth / Math.max((dataPixelSizeX), 1));
            int targetHeight = (int) (srcHeight / Math.max((dataPixelSizeY), 1));

            // special treatment for hexagon-based plots because individual hexagons cannot be asymmetric
            final ContourType contourType = renderer.getContourType();
            if (contourType.equals(ContourType.HEATMAP_HEXAGON)) {
                final double minReductionFactor = Math.min(reductionFactorX, reductionFactorY);
                final double minPixelSizeX = Math.max((minReductionFactor * xSize / xAxisWidth), 1);
                final double minPixelSizeY = Math.max((minReductionFactor * ySize / yAxisHeight), 1);
                targetWidth = (int) (srcWidth / minPixelSizeX);
                targetHeight = (int) (srcHeight / minPixelSizeY);
            }

            //            System.err.printf("image width = %d x %d - reduced from %d x %d\n", targetWidth, targetHeight, xSize, ySize);

            tempDataBuffer = DoubleArrayCache.getInstance().getArrayExact(targetWidth * targetHeight);

            DefaultDataReducer3D.resample(input, srcWidth, srcHeight, tempDataBuffer, targetWidth, targetHeight,
                    reductionType);

            xSize = targetWidth;
            ySize = targetHeight;
            return tempDataBuffer;
        }

        reducedData = input;
        //        System.err.printf("image width = %d x %d\n", xSize, ySize);
        return reducedData;
    }

    protected static void computeCoordinates(final GridDataSet dataSet, final double[] dataBuffer, final int dataLength, //
            final boolean xAxisInverted, final int xMinIndex, final int xMaxIndex, //
            final boolean yAxisInverted, final int yMinIndex, final int yMaxIndex, //
            final int yMinDst) {
        final int dstWidth = Math.abs(xMaxIndex - xMinIndex) + 1;
        final int dataDim = dataSet.getNGrid(); // use values from the first non-grid dimension

        switch (InvertedAxisCase.get(xAxisInverted, yAxisInverted)) {
        case X_ONLY:
            for (int yIndex = yMinIndex; yIndex <= yMaxIndex; yIndex++) {
                final int rowIndex2 = (yIndex - yMinDst + 1) * dstWidth - 1;
                for (int xIndex = 0; xIndex < dstWidth; xIndex++) {
                    dataBuffer[rowIndex2 - xIndex] = dataSet.get(dataDim, xIndex + xMinIndex, yIndex);
                }
            }
            break;
        case Y_ONLY: {
            int rowIndex2 = dataLength - (yMinIndex - yMinDst + 1) * dstWidth;
            for (int yIndex = yMinIndex; yIndex <= yMaxIndex; yIndex++) {
                for (int xIndex = 0; xIndex < dstWidth; xIndex++) {
                    dataBuffer[rowIndex2 + xIndex] = dataSet.get(dataDim, xIndex + xMinIndex, yIndex);
                }
                rowIndex2 -= dstWidth;
            }
            break;
        }
        case BOTH: {
            int rowIndex2 = dataLength - 1 - (yMinIndex - yMinDst) * dstWidth;
            for (int yIndex = yMinIndex; yIndex <= yMaxIndex; yIndex++) {
                for (int xIndex = 0; xIndex < dstWidth; xIndex++) {
                    dataBuffer[rowIndex2 - xIndex] = dataSet.get(dataDim, xIndex + xMinIndex, yIndex);
                }
                rowIndex2 -= dstWidth;
            }
        } break;
        case NORMAL:
        default:
            for (int yIndex = yMinIndex; yIndex <= yMaxIndex; yIndex++) {
                final int rowIndex2 = (yIndex - yMinDst) * dstWidth;
                for (int xIndex = 0; xIndex < dstWidth; xIndex++) {
                    dataBuffer[rowIndex2 + xIndex] = dataSet.get(dataDim, xIndex + xMinIndex, yIndex);
                }
            }
            break;
        }
    }

    protected static DataRange computeLocalRange(final double[] input, final int srcWidth, final int srcHeight,
            final boolean computeLocalRange) {
        final DataRange zDataRange = new DataRange();
        if (computeLocalRange) {
            final int length = srcWidth * srcHeight;
            for (int i = 0; i < length; i++) {
                zDataRange.add(input[i]);
            }
        }
        return zDataRange;
    }

    protected static void copySubFrame(final DataSet dataSet, final double[] dataBuffer,
            final boolean parallelImplementation, //
            final boolean xInverted, final int xMinIndex, final int xMaxIndex, //
            final boolean yInverted, final int yMinIndex, final int yMaxIndex) {
        final int width = Math.abs(xMaxIndex - xMinIndex) + 1;
        final int height = Math.abs(yMaxIndex - yMinIndex) + 1;
        final int dataLength = width * height;

        if (!parallelImplementation) {
            computeCoordinates((GridDataSet) dataSet, dataBuffer, dataLength, xInverted, xMinIndex, xMaxIndex, yInverted, yMinIndex,
                    yMaxIndex, yMinIndex);
            return;
        }

        final int nMaxThreads = CachedDaemonThreadFactory.getNumbersOfThreads();
        final int minthreshold = REF_HEIGHT_PARALLEL / 2; // TODO: tune this limit
        final int divThread = (int) Math.ceil(height / (double) nMaxThreads);
        final int stepSize = Math.max(divThread, minthreshold);
        final List<Callable<Boolean>> workers = new ArrayList<>();
        for (int i = yMinIndex; i < yMaxIndex; i += stepSize) {
            final int start = i;
            workers.add(() -> {
                final int yMinLocal = start;
                final int yMaxLocal = Math.min(start + stepSize, yMaxIndex);
                computeCoordinates((GridDataSet) dataSet, dataBuffer, dataLength, //
                        xInverted, xMinIndex, xMaxIndex, //
                        yInverted, yMinLocal, yMaxLocal, //
                        yMinIndex);
                return Boolean.TRUE;
            });
        }

        try {
            final List<Future<Boolean>> jobs = CachedDaemonThreadFactory.getCommonPool().invokeAll(workers);
            for (final Future<Boolean> future : jobs) {
                final Boolean r = future.get();
                if (Boolean.FALSE.equals(r)) {
                    throw new IllegalStateException(PARALLEL_WORKER_ERROR);
                }
            }
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException(PARALLEL_WORKER_ERROR, e);
        }
    }

    protected static double quantize(final double value, final int nLevels) {
        return ((int) (value * nLevels)) / (double) nLevels;
    }

    protected WritableImage convertDataArrayToImage(final double[] inputData, final int dataWidth, final int dataHeight,
            final ColorGradient colorGradient) {
        final int length = dataWidth * dataHeight;

        final byte[] byteBuffer = ByteArrayCache.getInstance().getArrayExact(length * BGRA_BYTE_SIZE);
        final int rowSizeInBytes = BGRA_BYTE_SIZE * dataWidth;
        final WritableImage image = this.getImage(dataWidth, dataHeight);
        final PixelWriter pixelWriter = image.getPixelWriter();
        if (pixelWriter == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().log("Could not get PixelWriter for image");
            }
            return image;
        }

        final int hMinus1 = dataHeight - 1;
        for (int yIndex = 0; yIndex < dataHeight; yIndex++) {
            final int rowIndex = dataWidth * yIndex;
            final int rowPixelIndex = rowSizeInBytes * (hMinus1 - yIndex);
            for (int xIndex = 0; xIndex < dataWidth; xIndex++) {
                final int[] color = colorGradient.getColorBytes(inputData[rowIndex + xIndex]);

                final int pixelIndex = rowPixelIndex + xIndex * BGRA_BYTE_SIZE;
                byteBuffer[pixelIndex] = (byte) (color[3]);
                byteBuffer[pixelIndex + 1] = (byte) (color[2]);
                byteBuffer[pixelIndex + 2] = (byte) (color[1]);
                byteBuffer[pixelIndex + 3] = (byte) (color[0]);
            }
        }

        pixelWriter.setPixels(0, 0, dataWidth, dataHeight, PixelFormat.getByteBgraPreInstance(), byteBuffer, 0,
                rowSizeInBytes);
        ByteArrayCache.getInstance().add(byteBuffer);
        return image;
    }

    protected static int roundDownEven(double d) {
        return (int) Math.floor(d / 2) * 2;
    }

    protected enum InvertedAxisCase {
        NORMAL,
        X_ONLY,
        Y_ONLY,
        BOTH;

        public static InvertedAxisCase get(final boolean xInverted, final boolean yInverted) {
            if (xInverted && yInverted) {
                return BOTH;
            } else if (xInverted) {
                return X_ONLY;
            } else if (yInverted) {
                return Y_ONLY;
            }
            return NORMAL;
        }
    }
}
