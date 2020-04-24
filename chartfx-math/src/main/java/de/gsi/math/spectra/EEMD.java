package de.gsi.math.spectra;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.math.Spline;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.TRandom;
import de.gsi.math.matrix.MatrixD;
import de.gsi.math.utils.ConcurrencyUtils;

/**
 * @author rstein
 */
public class EEMD {

    private static TRandom rnd = new TRandom(0);
    private int fstatus = 100;

    public MatrixD eemd(final double[] data, final double rms_noise, final double NE) {
        final int xsize = data.length;
        final double[] X1 = new double[xsize];
        final double[] xorigin = new double[xsize];
        final double[] xstart = new double[xsize];
        final double[] xstart_old = new double[xsize];
        final double[] xend = new double[xsize];
        // dd=1:1:xsize;
        final double Ystd = TMath.RMS(data);

        final int TNM = (int) Math.floor(TMathConstants.Log2(xsize)) - 1;
        final int TNM2 = TNM + 2;

        final MatrixD allmode = new MatrixD(xsize, TNM2 + 1);
        final MatrixD mode = new MatrixD(xsize, TNM2 + 1);

        for (int iii = 0; iii < NE; iii++) {
            for (int i = 0; i < xsize; i++) {
                final double temp = rnd.Gaus(0, rms_noise);
                X1[i] = data[i] / Ystd + temp;
            }

            for (int jj = 0; jj < xsize; jj++) {
                mode.set(jj, 0, data[jj]);
            }

            System.arraycopy(X1, 0, xorigin, 0, xsize);
            System.arraycopy(X1, 0, xend, 0, xsize);

            for (int nmode = 1; nmode < TNM; nmode++) {
                System.arraycopy(xend, 0, xstart, 0, xsize);
                System.arraycopy(xend, 0, xstart_old, 0, xsize);

                fstatus = (int) ((double) nmode / (double) TNM) * 100;
                // the sifting process
                // need to implement a more proper break condition than
                // limited number of interactions
                boolean abort = false;
                for (int iter = 0; iter < 30000; iter++) {
                    final double[][] spmax = SpectrumTools.computeMaxima(xstart);
                    final double[][] spmin = SpectrumTools.computeMinima(xstart);
                    final int nextrema = spmax[0].length + spmin[0].length;
                    final int ncrossing = computeZeroCrossings(xstart);

                    if (spmax[0].length < 3 || spmin[0].length < 3) {
                        abort = true;
                        System.err.println("break loop: iter = " + iter + " nmode " + nmode);
                        break;
                    }

                    // System.err.printf("extrema %d vs. %d zero-crossings = %d vs (%d)\n", spmax[0].length,
                    // spmin[0].length, ncrossing, nextrema);

                    final Spline upper = new Spline(spmax[0], spmax[1]);
                    final Spline lower = new Spline(spmin[0], spmin[1]);

                    for (int i = 0; i < xsize; i++) {
                        final double x = i;
                        final double mean_ul = (upper.getValue(x) + lower.getValue(x)) / 2.0;
                        xstart[i] -= mean_ul;
                    }
                    // final double residual = TMath.Mean(xstart);
                    // final double rms = TMath.RMS(xstart);
                    // System.err.printf("mode %d iter %d -> residual = %f\n",
                    // nmode, iter, residual/rms);

                    // check breaking condition
                    double sum_sqr = 0, diff_sqr = 0;
                    for (int i = 0; i < xstart.length; i++) {
                        diff_sqr += TMathConstants.Sqr(xstart_old[i] - xstart[i]);
                        sum_sqr += TMathConstants.Sqr(xstart_old[i]);
                    }

                    final double break_crit = 1e-12; // 0.3;

                    final double estimate = sum_sqr != 0 ? diff_sqr / sum_sqr : 42;

                    if (true) {
                        if (sum_sqr == 0 || estimate < break_crit) {
                            System.err.printf("break at mode %d and  iteration %d with criteria %e\n", nmode, iter,
                                    estimate);
                            break;
                        }
                    }

                    if (true) {
                        if (sum_sqr == 0 || Math.abs(nextrema - ncrossing) <= 0) {
                            System.err.printf("break (crossing) at mode %d and  iteration %d with criteria %f\n", nmode,
                                    iter, diff_sqr / sum_sqr);
                            break;
                        }
                    }

                    System.arraycopy(xstart, 0, xstart_old, 0, xstart.length);
                }
                for (int i = 0; i < xsize; i++) {
                    xend[i] -= xstart[i];
                }

                // System.out.printf("store mode %d -> %f\n", nmode,
                // TMath.RMS(xstart));
                for (int jj = 0; jj < xsize; jj++) {
                    mode.set(jj, nmode, xstart[jj]);
                }

                if (abort) {
                    nmode = TNM + 1;
                }
            }

            // store remainder of the sifting process
            for (int jj = 0; jj < xsize; jj++) {
                mode.set(jj, TNM + 1, xend[jj]);
            }

            allmode.plus(mode);
        }

        allmode.times(1.0 / NE);
        allmode.times(Ystd);

        return mode;
    }

    /**
     * EMD spectrum implementation
     * 
     * @param data input data
     * @param nQuantx quantisation in X
     * @param nQuanty quantisation in Y
     * @return the complex HHT spectrum
     */
    public synchronized DataSet getScalogram(final double[] data, final int nQuantx, final int nQuanty) {
        // create and return data set.
        fstatus = 0;
        final int nsamples = data.length;
        final double[] time = new double[nsamples];
        final double[] frequency = new double[nsamples / 2];

        for (int i = 0; i < nsamples; i++) {
            time[i] = i;
        }

        for (int i = 0; i < nsamples / 2; i++) {
            frequency[i] = (double) i / (double) nsamples;
        }

        final DataSet ds = new DataSetBuilder("HilbertSpectrum") //
                .setValues(DataSet.DIM_X, time) //
                .setValues(DataSet.DIM_Y, frequency) //
                .setValues(DataSet.DIM_Z, getSpectrumArray(data, nQuantx, nQuanty)) //
                .build();

        fstatus = 100;
        return ds;
    }

    public double[][] getSpectrumArray(final double[] data, final int nQuantx, final int Quanty) {
        final int nsamples = data.length;
        // required index[yrange][xrange]
        final double[][] ret = new double[nsamples / 2][nsamples];
        for (int i = 0; i < nsamples / 2; i++) {
            for (int j = 0; j < nsamples; j++) {
                ret[i][j] = Double.NaN;
            }
        }

        final HilbertTransform hilbert = new HilbertTransform();
        final MatrixD emd = eemd(data, 0, 1.0);
        final double[] mode = new double[nsamples];
        final int nmodes = emd.getColumnDimension() - 1;
        for (int nmode = 1; nmode < nmodes; nmode++) {
            for (int j = 0; j < nsamples; j++) {
                mode[j] = emd.get(j, nmode);
            }
            final Convolution decon = new Convolution();
            final double[] lowPass = Convolution.getLowPassFilter(ConcurrencyUtils.nextPow2(3 * nsamples), 0.3);
            final double[] amplitude = hilbert.computeInstantaneousAmplitude(mode);
            final double[] frequency = hilbert.computeInstantaneousFrequency(mode);
            final double[] amplitude_filtered = decon.transform(amplitude, lowPass, false);
            final double[] frequency_filtered = decon.transform(frequency, lowPass, false);

            for (int j = 0; j < nsamples; j++) {

                if (j < nsamples) {

                    int yIndex = (int) (frequency_filtered[j] * nsamples);

                    if (yIndex < 0) {
                        yIndex = 0;
                    } else if (yIndex >= nsamples / 2) {
                        yIndex = nsamples / 2 - 1;
                    }

                    // ret[j][y_index] = Math.log(amplitude);
                    ret[yIndex][j] = 10 * Math.log(amplitude_filtered[j]);
                    if (ret[yIndex][j] < -10 || ret[yIndex][j] > 10) {
                        ret[yIndex][j] = Double.NaN;
                        // ret[j][y_index] = TMath.Sqr(amplitude);
                    }
                }
            }

        }

        return ret;
    }

    /**
     * @return progress of pending calculations in percent
     */
    public int getStatus() {
        return fstatus;
    }

    /**
     * @return whether class is busy computing a spectra
     */
    public boolean isBusy() {
        return fstatus < 100 ? true : false;
    }

    public static int computeZeroCrossings(final double[] data) {
        final int dsize = data.length;
        int val = 0;

        boolean positive = data[0] >= 0;
        for (int i = 1; i < dsize; i++) {
            if (positive) {
                if (data[i] < 0) {
                    positive = false;
                    val++;
                }
            } else {
                if (data[i] > 0) {
                    positive = true;
                    val++;
                }
            }
        }

        return val;
    }

    public static int extrema(final double[] data, final double[][] spmax, final double[][] spmin) {
        int flag = 1;
        int dsize = data.length;

        spmax[0][0] = 0;
        spmax[1][0] = data[1];
        int jj = 1;
        int kk = 1;
        while (jj < dsize - 1) {
            if (data[jj - 1] <= data[jj] & data[jj] >= data[jj + 1]) {
                spmax[0][kk] = jj;
                spmax[1][kk] = data[jj];
                kk = kk + 1;
            }
            jj = jj + 1;
        }

        spmax[0][kk] = dsize - 1;
        spmax[1][kk] = data[dsize - 1];

        if (kk >= 3) {
            final double slope1 = (spmax[1][1] - spmax[1][2]) / (spmax[0][1] - spmax[0][2]);
            final double tmp1 = slope1 * (spmax[0][0] - spmax[0][1]) + spmax[1][1];
            if (tmp1 > spmax[1][0]) {
                spmax[1][0] = tmp1;
            }

            final double slope2 = (spmax[1][kk - 1] - spmax[1][kk - 2]) / (spmax[0][kk - 1] - spmax[0][kk - 2]);
            final double tmp2 = slope2 * (spmax[0][kk] - spmax[0][kk - 1]) + spmax[1][kk - 1];
            if (tmp2 > spmax[1][kk]) {
                spmax[1][kk] = tmp2;
            }
        } else {
            flag = -1;
        }

        dsize = data.length;

        spmin[0][0] = 0;
        spmin[1][0] = data[0];
        jj = 2;
        kk = 2;
        while (jj < dsize - 1) {
            if (data[jj - 1] >= data[jj] && data[jj] <= data[jj + 1]) {
                spmin[0][kk] = jj;
                spmin[1][kk] = data[jj];
                kk++;
            }
            jj = jj + 1;
        }

        spmin[0][kk] = dsize - 1;
        spmin[1][kk] = data[dsize - 1];

        if (kk >= 3) {
            final double slope1 = (spmin[1][1] - spmin[1][2]) / (spmin[0][1] - spmin[0][2]);
            final double tmp1 = slope1 * (spmin[0][0] - spmin[0][1]) + spmin[1][1];
            if (tmp1 < spmin[1][0]) {
                spmin[1][0] = tmp1;
            }

            final double slope2 = (spmin[1][kk - 1] - spmin[1][kk - 2]) / (spmin[0][kk - 1] - spmin[0][kk - 2]);
            final double tmp2 = slope2 * (spmin[0][kk] - spmin[0][kk - 1]) + spmin[1][kk - 1];

            if (tmp2 < spmin[1][kk]) {
                spmin[1][kk] = tmp2;
            }
        } else {
            flag = -1;
        }

        flag = 1;

        return flag;
    }
}
