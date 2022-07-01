package io.fair_acc.chartfx.renderer.datareduction;

/**
 * @author rstein
 */
public final class DefaultDataReducer3D { // NOPMD nomen est omen

    private DefaultDataReducer3D() {
        // static helper class
    }

    public static void resample(double[] src, final int srcWidth, final int srcHeight, double[] target,
            final int targetWidth, final int targetHeight, //
            ReductionType reductionType) {
        final int xRatio = ((srcWidth << 16) / targetWidth) + 1;
        final int yRatio = ((srcHeight << 16) / targetHeight) + 1;
        final int xLimit = xRatio >> 16;
        final int yLimit = yRatio >> 16;

        final double norm = (xLimit * yLimit);
        switch (reductionType) {
        case MIN:
            for (int i = 0; i < targetHeight; i++) {
                final int y2 = ((i * yRatio) >> 16);
                final int srcRowStart = y2 * srcWidth;
                final int targetRowStart = i * targetWidth;
                for (int j = 0; j < targetWidth; j++) {
                    final int x2 = ((j * xRatio) >> 16);
                    double val = Double.MAX_VALUE;
                    for (int k = 0; k < yLimit; k++) {
                        final int srcRowOffset = k * srcWidth;
                        for (int l = 0; l < xLimit; l++) {
                            val = Math.min(val, src[srcRowStart + srcRowOffset + x2 + l]);
                        }
                    }
                    target[targetRowStart + j] = val;
                }
            }
            break;
        case MAX:
            for (int i = 0; i < targetHeight; i++) {
                final int y2 = ((i * yRatio) >> 16);
                final int srcRowStart = y2 * srcWidth;
                final int targetRowStart = i * targetWidth;
                for (int j = 0; j < targetWidth; j++) {
                    final int x2 = ((j * xRatio) >> 16);
                    double val = -Double.MAX_VALUE;
                    for (int k = 0; k < yLimit; k++) {
                        final int srcRowOffset = k * srcWidth;
                        for (int l = 0; l < xLimit; l++) {
                            val = Math.max(val, src[srcRowStart + srcRowOffset + x2 + l]);
                        }
                    }
                    target[targetRowStart + j] = val;
                }
            }
            break;

        case DOWN_SAMPLE:
            for (int i = 0; i < targetHeight; i++) {
                final int y2 = ((i * yRatio) >> 16);
                final int srcRowStart = y2 * srcWidth;
                final int targetRowStart = i * targetWidth;
                for (int j = 0; j < targetWidth; j++) {
                    final int x2 = ((j * xRatio) >> 16);
                    target[targetRowStart + j] = src[srcRowStart + x2];
                }
            }
            break;
        case AVERAGE:
        default:
            for (int i = 0; i < targetHeight; i++) {
                final int y2 = ((i * yRatio) >> 16);
                final int srcRowStart = y2 * srcWidth;
                final int targetRowStart = i * targetWidth;
                for (int j = 0; j < targetWidth; j++) {
                    final int x2 = ((j * xRatio) >> 16);
                    double val = 0.0;
                    for (int k = 0; k < yLimit; k++) {
                        final int srcRowOffset = k * srcWidth;
                        for (int l = 0; l < xLimit; l++) {
                            val += src[srcRowStart + srcRowOffset + x2 + l];
                        }
                    }
                    target[targetRowStart + j] = val / norm;
                }
            }
            break;
        }
    }

    public static void scaleDownByFactorTwo(final double[] target, final int targetWidth, // NOPMD
            final int targetHeight, double[] source, int srcWidth, int srcHeight, final int yMinIndex,
            final int yMaxIndex, final ReductionType reductionType) {
        final int scalingX = srcWidth / targetWidth;
        if (scalingX != 2 && scalingX != 1) {
            throw new IllegalArgumentException("targetWidth=" + targetWidth + " to srcWidth=" + srcWidth + " mismatch");
        }
        final int scalingY = srcHeight / targetHeight;
        if (scalingY != 2 && scalingY != 1) {
            throw new IllegalArgumentException(
                    "targetHeight=" + targetHeight + " to srcHeight=" + srcHeight + " mismatch");
        }

        final int yMaxIndexLimited = Math.min(yMaxIndex, targetHeight);
        ScaleAxis scaleAxis = ScaleAxis.get(scalingX >= 2, scalingY >= 2);
        switch (reductionType) {
        case MIN:
            scaleDownByFactorTwoMin(target, targetWidth, source, srcWidth, yMinIndex, yMaxIndexLimited, scaleAxis);
            break;
        case MAX:
            scaleDownByFactorTwoMax(target, targetWidth, source, srcWidth, yMinIndex, yMaxIndexLimited, scaleAxis);
            break;
        case AVERAGE:
        default:
            scaleDownByFactorTwoAvg(target, targetWidth, source, srcWidth, yMinIndex, yMaxIndexLimited, scaleAxis);
            break;
        }
    }

    private static void copyIdentity(final double[] target, final int targetWidth, final int yMinIndex, double[] source,
            final int yMaxIndex) {
        for (int y = yMinIndex; y <= yMaxIndex; y++) {
            final int rowStart = y * targetWidth;
            if (targetWidth >= 0)
                System.arraycopy(source, rowStart + 0, target, rowStart + 0, targetWidth);
        }
    }

    private static void scaleDownByFactorTwoAvg(final double[] target, final int targetWidth, double[] source,
            int srcWidth, final int yMinIndex, final int yMaxIndex, final ScaleAxis scaleOption) {
        switch (scaleOption) {
        case BOTH:
            for (int y = yMinIndex; y < yMaxIndex; y++) {
                final int y2 = y << 1;
                final int rowStart1 = y2 * srcWidth;
                final int rowStart2 = rowStart1 + srcWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final int pixelIndex1 = rowStart1 + (x << 1);
                    final int pixelIndex2 = rowStart2 + (x << 1);
                    final double p = 0.5 * (source[pixelIndex1] + source[pixelIndex1 + 1]);
                    final double q = 0.5 * (source[pixelIndex2] + source[pixelIndex2 + 1]);
                    final double r = 0.5 * (p + q);
                    target[y * targetWidth + x] = r;
                }
            }
            return;
        case X_ONLY:
            for (int y = yMinIndex; y <= yMaxIndex; y++) {
                final int rowStartSrc = y * srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final int rowStartX2 = rowStartSrc + (x << 1);
                    final double r = 0.5 * (source[rowStartX2] + source[rowStartX2 + 1]);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case Y_ONLY:
            for (int y = yMinIndex; y < yMaxIndex; y++) {
                final int y2 = y << 1;
                final int rowStartSrc1 = y2 * srcWidth;
                final int rowStartSrc2 = rowStartSrc1 + srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final double r = 0.5 * (source[rowStartSrc1 + x] + source[rowStartSrc2 + x]);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case NONE:
        default:
            copyIdentity(target, targetWidth, yMinIndex, source, yMaxIndex);
        }
    }

    private static void scaleDownByFactorTwoMax(final double[] target, final int targetWidth, double[] source,
            int srcWidth, final int yMinIndex, final int yMaxIndex, final ScaleAxis scaleOption) {
        switch (scaleOption) {
        case BOTH:
            for (int y = yMinIndex; y < yMaxIndex; y++) {
                final int y2 = y << 1;
                final int rowStartSrc1 = y2 * srcWidth;
                final int rowStartSrc2 = rowStartSrc1 + srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final int pixelIndex1 = rowStartSrc1 + (x << 1);
                    final int pixelIndex2 = rowStartSrc2 + (x << 1);
                    final double p = Math.max(source[pixelIndex1], source[pixelIndex1 + 1]);
                    final double q = Math.max(source[pixelIndex2], source[pixelIndex2 + 1]);
                    final double r = Math.max(p, q);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case X_ONLY:
            for (int y = yMinIndex; y <= yMaxIndex; y++) {
                final int rowStartSrc = y * srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final int rowStartX2 = rowStartSrc + (x << 1);
                    final double r = Math.max(source[rowStartX2], source[rowStartX2 + 1]);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case Y_ONLY:
            for (int y = yMinIndex; y < yMaxIndex; y++) {
                final int y2 = y << 1;
                final int rowStartSrc1 = y2 * srcWidth;
                final int rowStartSrc2 = rowStartSrc1 + srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final double r = Math.max(source[rowStartSrc1 + x], source[rowStartSrc2 + x]);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case NONE:
        default:
            copyIdentity(target, targetWidth, yMinIndex, source, yMaxIndex);
        }
    }

    private static void scaleDownByFactorTwoMin(final double[] target, final int targetWidth, double[] source,
            int srcWidth, final int yMinIndex, final int yMaxIndex, final ScaleAxis scaleOption) {
        switch (scaleOption) {
        case BOTH:
            for (int y = yMinIndex; y < yMaxIndex; y++) {
                final int y2 = y << 1;
                final int rowStartSrc1 = y2 * srcWidth;
                final int rowStartSrc2 = rowStartSrc1 + srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final int pixelIndex1 = rowStartSrc1 + (x << 1);
                    final int pixelIndex2 = rowStartSrc2 + (x << 1);
                    final double p = Math.min(source[pixelIndex1], source[pixelIndex1 + 1]);
                    final double q = Math.min(source[pixelIndex2], source[pixelIndex2 + 1]);
                    final double r = Math.min(p, q);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case X_ONLY:
            for (int y = yMinIndex; y <= yMaxIndex; y++) {
                final int rowStartDst = y * targetWidth;
                final int rowStart = y * srcWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final int rowStartX2 = rowStart + (x << 1);
                    final double r = Math.min(source[rowStartX2], source[rowStartX2 + 1]);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case Y_ONLY:
            for (int y = yMinIndex; y < yMaxIndex; y++) {
                final int y2 = y << 1;
                final int rowStartSrc1 = y2 * srcWidth;
                final int rowStartSrc2 = rowStartSrc1 + srcWidth;
                final int rowStartDst = y * targetWidth;
                for (int x = 0; x < targetWidth; x++) {
                    final double r = Math.min(source[rowStartSrc1 + x], source[rowStartSrc2 + x]);
                    target[rowStartDst + x] = r;
                }
            }
            return;
        case NONE:
        default:
            copyIdentity(target, targetWidth, yMinIndex, source, yMaxIndex);
        }
    }

    private enum ScaleAxis {
        BOTH,
        X_ONLY,
        Y_ONLY,
        NONE;

        static ScaleAxis get(final boolean scaleX, final boolean scaleY) {
            if (scaleX && scaleY) {
                return BOTH;
            } else if (scaleX) {
                return X_ONLY;
            } else if (scaleY) {
                return Y_ONLY;
            }

            return NONE;
        }
    }
}
