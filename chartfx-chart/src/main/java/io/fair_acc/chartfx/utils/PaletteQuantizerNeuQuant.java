package io.fair_acc.chartfx.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeuQuant Neural-Net Quantisation Algorithm
 *
 * Copyright (c) 1994 Anthony Dekker
 *
 * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994. See
 * "Kohonen neural networks for optimal colour quantization" in "Network: Computation in Neural Systems" Vol. 5 (1994)
 * pp 351-367. for a discussion of the algorithm.
 *
 * Any party obtaining a copy of these files from the author, directly or indirectly, is granted, free of charge, a full
 * and unrestricted irrevocable, world-wide, paid up, royalty-free, non-exclusive right and license to deal in this
 * software and documentation files (the "Software"), including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sub-license, and/or sell copies of the Software, and to permit persons who receive copies
 * from any such party to do so, with the only requirement being that this copyright notice remain intact.

 * @author Anthony Dekker - original author
 * @author Hernan J Gonzalez - modified for PngReader - no special colours - sequential read
 * @author rstein adapted to a modern Java and chart-fx usage
 *
 */
@SuppressWarnings("PMD") // original author's coding convention
public class PaletteQuantizerNeuQuant implements PaletteQuantizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaletteQuantizerNeuQuant.class);
    // parameters - do not change during running - naming convention:
    // parNcolors ... parameters setteable with set()
    // _parMmaxnetpos ... derived parameter, not settaable, computed at initParams()
    private int parNcolors = 256; // number of colours used

    private int parNcycles = 330; // no. of learning cycles
    private int _parCutnetsize; // = ncolors;//
    private int _parMaxnetpos; // = ncolors - 1;
    //private int _parInitrad; // = ncolors / 8; // for 256 cols, radius starts at 32

    private int parRadiusbiasshift = 6;
    private int _parRadiusbias; // = 1 << radiusbiasshift;
    private int _parInitBiasRadius; // = initrad * radiusbias;
    private int parRadiusdec = 30; // factor of 1/30 each cycle
    private int parTransparencyThreshold = 127;

    private int parAlphabiasshift = 10; // alpha starts at 1

    private int _parIinitalpha; // = 1 << alphabiasshift; // biased by 10 bits
    private double parGamma = 1024.0;

    private double parBeta = 1.0 / 1024.0;
    private double _parGammaBetta; // = beta * gamma;
    private boolean parReserveAlphaColor = false;
    private int parMaxPixelsToSample = 30000;

    private int _parSamplefac; // 1-30
    private double[][] network; // the network itself //WARNING: BGR
    protected int[][] colormap; // the network itself //WARNING: BGR
    private final int[] netindex = new int[256]; // for network lookup - really 256
    private double[] bias; // = new double[parNcolors]; // bias and freq arrays for learnin//*g
    private double[] freq; // = new double[parNcolors];
    private final int width;
    private final int height;
    private final PixelGetter pixelGetter;
    private boolean done = false;

    public PaletteQuantizerNeuQuant(final int w, final int h, final PixelGetter pixelGetter) {
        width = w;
        height = h;
        this.pixelGetter = pixelGetter;
    }

    public int[] convert(final int r, final int g, final int b) {
        initGuard();
        final int i = indexSearch(b, g, r);
        final int bb = colormap[i][0];
        final int gg = colormap[i][1];
        final int rr = colormap[i][2];
        return new int[] { rr, gg, bb };
    }

    public int[] convert(final int r, final int g, final int b, final int a) {
        initGuard();
        if (parReserveAlphaColor && a < parTransparencyThreshold) {
            return new int[] { 0, 0, 0, 0 };
        }
        final int i = indexSearch(b, g, r);
        final int bb = colormap[i][0];
        final int gg = colormap[i][1];
        final int rr = colormap[i][2];
        return new int[] { rr, gg, bb };
    }

    @Override
    public int[] getColor(final int i) {
        initGuard();
        int index = i;
        if (parReserveAlphaColor) {
            index--;
            if (index < 0) {
                return new int[] { 0, 0, 0, 0 };
            }
        }
        if (index < 0 || index >= parNcolors) {
            throw new IllegalArgumentException("index out of range [0, " + parNcolors + "[");
        }
        final int bb = colormap[index][0];
        final int gg = colormap[index][1];
        final int rr = colormap[index][2];
        return new int[] { rr, gg, bb, 255 };
    }

    @Override
    public int getColorCount() { // includes transparent color if applicable
        initGuard();
        return parReserveAlphaColor ? parNcolors + 1 : parNcolors;
    }

    @Override
    public int getTransparentIndex() { // -1 if not exists
        initGuard();
        return parReserveAlphaColor ? 0 : -1;
    }

    public boolean isParReserveAlphaColor() {
        return parReserveAlphaColor;
    }

    @Override
    public int lookup(final int r, final int g, final int b) {
        initGuard();
        final int i = indexSearch(b, g, r);
        return parReserveAlphaColor ? i + 1 : i;
    }

    @Override
    public int lookup(final int r, final int g, final int b, final int a) {
        //initGuard();
        if (a < parTransparencyThreshold) {
            return 0; // extra entry: transparent
        }
        return indexSearch(b, g, r) + 1;
    }

    public void run() {
        if (done) {
            return;
        }
        initParams();
        setUpArrays();
        learn();
        fix();
        inxbuild();
        done = true;
    }

    public void setParAlphabiasshift(final int parAlphabiasshift) {
        this.parAlphabiasshift = parAlphabiasshift;
    }

    public void setParBeta(final double parBeta) {
        this.parBeta = parBeta;
    }

    public void setParGamma(final double parGamma) {
        this.parGamma = parGamma;
    }

    public void setParMaxPixelsToSample(final int parMaxPixelsToSample) {
        this.parMaxPixelsToSample = parMaxPixelsToSample;
    }

    public void setParNcolors(final int parNcolors) {
        this.parNcolors = parNcolors;
    }

    public void setParNcycles(final int parNcycles) {
        this.parNcycles = parNcycles;
    }

    public void setParRadiusbiasshift(final int parRadiusbiasshift) {
        this.parRadiusbiasshift = parRadiusbiasshift;
    }

    public void setParRadiusdec(final int parRadiusdec) {
        this.parRadiusdec = parRadiusdec;
    }

    public void setParReserveAlphaColor(final boolean parReserveAlphaColor) {
        this.parReserveAlphaColor = parReserveAlphaColor;
    }

    public void setParTransparencyThreshold(final int parTransparencyThreshold) {
        this.parTransparencyThreshold = parTransparencyThreshold;
    }

    /**
     * computes neural network if it hasn't been already done
     */
    protected void initGuard() {
        if (done) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("need to execute run() computation - fallback should be done prior in user-land");
        }
        run();
    }

    protected int indexSearch(final int b, final int g, final int r) {
        // Search for BGR values 0..255 and return colour index
        int bestd = 1000; // biggest possible dist is 256*3
        int best = -1;
        int i = netindex[g]; // index on g
        int j = i - 1; // start at netindex[g] and work outwards

        while (i < parNcolors || j >= 0) {
            if (i < parNcolors) {
                final int[] p = colormap[i];
                int dist = p[1] - g; // inx key
                if (dist >= bestd) {
                    i = parNcolors; // stop iter
                } else {
                    if (dist < 0) {
                        dist = -dist;
                    }
                    int a = p[0] - b;
                    if (a < 0) {
                        a = -a;
                    }
                    dist += a;
                    if (dist < bestd) {
                        a = p[2] - r;
                        if (a < 0) {
                            a = -a;
                        }
                        dist += a;
                        if (dist < bestd) {
                            bestd = dist;
                            best = i;
                        }
                    }
                    i++;
                }
            }
            if (j >= 0) {
                final int[] p = colormap[j];
                int dist = g - p[1]; // inx key - reverse dif
                if (dist >= bestd) {
                    j = -1; // stop iter
                } else {
                    dist = Math.abs(dist);
                    int a = p[0] - b;
                    if (a < 0) {
                        a = -a;
                    }
                    dist += a;
                    if (dist < bestd) {
                        a = p[2] - r;
                        if (a < 0) {
                            a = -a;
                        }
                        dist += a;
                        if (dist < bestd) {
                            bestd = dist;
                            best = j;
                        }
                    }
                    j--;
                }
            }
        }

        return best;
    }

    protected void setUpArrays() {
        network = new double[parNcolors][3]; // the network itself //WARNING: BGR
        colormap = new int[parNcolors][4]; // the network itself //WARNING: BGR
        bias = new double[parNcolors];
        freq = new double[parNcolors];
        network[0][0] = 0.0; // black
        network[0][1] = 0.0;
        network[0][2] = 0.0;

        network[1][0] = 255.0; // white
        network[1][1] = 255.0;
        network[1][2] = 255.0;

        for (int i = 0; i < parNcolors; i++) {
            final double[] p = network[i];
            p[0] = 255.0 * i / _parCutnetsize;
            p[1] = 255.0 * i / _parCutnetsize;
            p[2] = 255.0 * i / _parCutnetsize;

            freq[i] = 1.0 / parNcolors;
            bias[i] = 0.0;
        }
    }

    private void alterneigh(final double alpha, final int rad, final int i, final double b, final double g, final double r) {
        int lo = i - rad;
        if (lo < -1) {
            lo = -1;
        }
        int hi = i + rad;
        if (hi > parNcolors) {
            hi = parNcolors;
        }

        int j = i + 1;
        int k = i - 1;
        int q = 0;
        while (j < hi || k > lo) {
            final double a = alpha * (rad * rad - q * q) / (rad * rad);
            q++;
            if (j < hi) {
                final double[] p = network[j];
                p[0] -= a * (p[0] - b);
                p[1] -= a * (p[1] - g);
                p[2] -= a * (p[2] - r);
                j++;
            }
            if (k > lo) {
                final double[] p = network[k];
                p[0] -= a * (p[0] - b);
                p[1] -= a * (p[1] - g);
                p[2] -= a * (p[2] - r);
                k--;
            }
        }
    }

    private void altersingle(final double alpha, final int i, final double b, final double g, final double r) {
        // Move neuron i towards biased (b,g,r) by factor alpha
        final double[] n = network[i]; // alter hit neuron
        n[0] -= alpha * (n[0] - b);
        n[1] -= alpha * (n[1] - g);
        n[2] -= alpha * (n[2] - r);
    }

    private int contest(final double b, final double g, final double r) { // Search for biased BGR values
        // finds closest neuron (min dist) and updates freq
        // finds best neuron (min dist-bias) and returns position
        // for frequently chosen neurons, freq[i] is high and bias[i] is negative
        // bias[i] = gamma*((1/netsize)-freq[i])

        double bestd = Float.MAX_VALUE;
        double bestbiasd = bestd;
        int bestpos = -1;
        int bestbiaspos = bestpos;

        for (int i = 0; i < parNcolors; i++) {
            final double[] n = network[i];
            double dist = n[0] - b;
            if (dist < 0) {
                dist = -dist;
            }
            double a = n[1] - g;
            if (a < 0) {
                a = -a;
            }
            dist += a;
            a = n[2] - r;
            if (a < 0) {
                a = -a;
            }
            dist += a;
            if (dist < bestd) {
                bestd = dist;
                bestpos = i;
            }
            final double biasdist = dist - bias[i];
            if (biasdist < bestbiasd) {
                bestbiasd = biasdist;
                bestbiaspos = i;
            }
            freq[i] -= parBeta * freq[i];
            bias[i] += _parGammaBetta * freq[i];
        }
        freq[bestpos] += parBeta;
        bias[bestpos] -= _parGammaBetta;
        return bestbiaspos;
    }

    private void fix() {
        for (int i = 0; i < parNcolors; i++) {
            for (int j = 0; j < 3; j++) {
                int x = (int) (0.5 + network[i][j]);
                x = Math.max(Math.min(255, x), 0);
                colormap[i][j] = x;
            }
            colormap[i][3] = i;
        }
    }

    private void initParams() {
        if (parReserveAlphaColor && parNcolors % 2 == 0) {
            parNcolors--;
        }
        _parGammaBetta = parBeta * parGamma;
        _parCutnetsize = parNcolors; //
        _parMaxnetpos = parNcolors - 1;
        final int _parInitrad = (parNcolors + 7) / 8; // for 256 cols, radius starts at 32
        _parRadiusbias = 1 << parRadiusbiasshift;
        _parInitBiasRadius = _parInitrad * _parRadiusbias;
        _parIinitalpha = 1 << parAlphabiasshift; // biased by 10 bits
        _parSamplefac = width * height / parMaxPixelsToSample;
        if (_parSamplefac < 1) {
            _parSamplefac = 1;
        } else if (_parSamplefac > 30) {
            _parSamplefac = 30;
        }
    }

    private void inxbuild() {
        // Insertion sort of network and building of netindex[0..255]

        int previouscol = 0;
        int startpos = 0;

        for (int i = 0; i < parNcolors; i++) {
            final int[] p = colormap[i];
            int[] q = null;
            int smallpos = i;
            int smallval = p[1]; // index on g
            // find smallest in i..netsize-1
            for (int j = i + 1; j < parNcolors; j++) {
                q = colormap[j];
                if (q[1] < smallval) { // index on g
                    smallpos = j;
                    smallval = q[1]; // index on g
                }
            }
            q = colormap[smallpos];
            // swap p (i) and q (smallpos) entries
            if (i != smallpos) {
                int j = q[0];
                q[0] = p[0];
                p[0] = j;
                j = q[1];
                q[1] = p[1];
                p[1] = j;
                j = q[2];
                q[2] = p[2];
                p[2] = j;
                j = q[3];
                q[3] = p[3];
                p[3] = j;
            }
            // smallval entry is now in position i
            if (smallval != previouscol) {
                netindex[previouscol] = startpos + i >> 1;
                for (int j = previouscol + 1; j < smallval; j++) {
                    netindex[j] = i;
                }
                previouscol = smallval;
                startpos = i;
            }
        }
        netindex[previouscol] = startpos + _parMaxnetpos >> 1;
        for (int j = previouscol + 1; j < 256; j++) {
            netindex[j] = _parMaxnetpos; // really 256
        }
    }

    private void learn() {
        int biasRadius = _parInitBiasRadius;
        final int alphadec = 30 + (_parSamplefac - 1) / 3;
        final int lengthcount = width * height;
        int samplepixels = lengthcount / _parSamplefac;
        if (samplepixels < 1000) {
            samplepixels = 1000;
        }
        if (samplepixels > lengthcount) {
            samplepixels = lengthcount;
        }
        int delta = samplepixels / parNcycles;
        if (delta < 1) {
            delta = 1;
        }
        int alpha = _parIinitalpha;
        final int stepx = (int) (Math.sqrt(lengthcount / samplepixels) + 0.5);
        int stepy = (int) (lengthcount / (samplepixels * (double) stepx) + 0.5);
        stepy = Math.max(1, stepy);
        int rad = biasRadius >> parRadiusbiasshift;
        if (rad <= 1) {
            rad = 0;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.atTrace().addArgument(samplepixels).addArgument(rad).log("beginning 1D learning: samplepixels = {} rad = {}");
        }
        int i = 1;
        for (int row = 0; row < height; row += stepy) {
            for (int col = 0; col < width; col += stepx) {
                final int rgbaPixel = pixelGetter.getPixel(row, (col + row) % width);
                final int aaa = !parReserveAlphaColor ? 255 : rgbaPixel >> 24 & 0xff; // alpha
                if (aaa < parTransparencyThreshold) {
                    continue;
                }
                final double r = rgbaPixel >> 16 & 0xff; //red
                final double g = rgbaPixel >> 8 & 0xff; // green
                final double b = rgbaPixel & 0xff; // blue

                final int j = contest(b, g, r);

                final double a = 1.0 * alpha / _parIinitalpha;
                altersingle(a, j, b, g, r);
                if (rad > 0) {
                    alterneigh(a, rad, j, b, g, r); // alter neighbours
                }
                i++;
                if (i % delta == 0) {
                    alpha -= alpha / alphadec;
                    biasRadius -= biasRadius / parRadiusdec;
                    rad = biasRadius >> parRadiusbiasshift;
                    if (rad <= 1) {
                        rad = 0;
                    }
                }
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.atTrace().addArgument((1.0 * alpha) / _parIinitalpha).log("finished 1D learning: final alpha = {}");
        }
    }

    public interface PixelGetter {
        // 3 ints if not alpha, 4 if alpha
        int getPixel(int row, int col);
    }
}
