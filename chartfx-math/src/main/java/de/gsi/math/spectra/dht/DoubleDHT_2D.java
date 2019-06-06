/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK *****
 */

package de.gsi.math.spectra.dht;

import java.util.concurrent.Future;

import de.gsi.math.utils.ConcurrencyUtils;

/**
 * Computes 2D Discrete Hartley Transform (DHT) of real, double precision data.
 * The sizes of both dimensions can be arbitrary numbers. This is a parallel
 * implementation optimized for SMP systems.<br>
 * <br>
 * Part of code is derived from General Purpose FFT Package written by Takuya
 * Ooura (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html)
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class DoubleDHT_2D {

    private final int rows;

    private final int columns;

    private double[] t;

    private final DoubleDHT_1D dhtColumns;

    private DoubleDHT_1D dhtRows;

    private int oldNthreads;

    private int nt;

    private boolean isPowerOfTwo = false;

    private boolean useThreads = false;

    /**
     * Creates new instance of DoubleDHT_2D.
     *
     * @param rows
     *            number of rows
     * @param column
     *            number of columns
     */
    public DoubleDHT_2D(final int rows, final int column) {
        if (rows <= 1 || column <= 1) {
            throw new IllegalArgumentException("rows and columns must be greater than 1");
        }
        this.rows = rows;
        columns = column;
        if (rows * column >= ConcurrencyUtils.getThreadsBeginN_2D()) {
            useThreads = true;
        }
        if (ConcurrencyUtils.isPowerOf2(rows) && ConcurrencyUtils.isPowerOf2(column)) {
            isPowerOfTwo = true;
            oldNthreads = ConcurrencyUtils.getNumberOfThreads();
            nt = 4 * oldNthreads * rows;
            if (column == 2 * oldNthreads) {
                nt >>= 1;
            } else if (column < 2 * oldNthreads) {
                nt >>= 2;
            }
            t = new double[nt];
        }
        dhtColumns = new DoubleDHT_1D(column);
        if (column == rows) {
            dhtRows = dhtColumns;
        } else {
            dhtRows = new DoubleDHT_1D(rows);
        }
    }

    /**
     * Computes 2D real, forward DHT leaving the result in <code>a</code>. The
     * data is stored in 1D array in row-major order.
     *
     * @param a
     *            data to transform
     */
    public void forward(final double[] a) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = 4 * nthreads * rows;
                if (columns == 2 * nthreads) {
                    nt >>= 1;
                } else if (columns < 2 * nthreads) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                ddxt2d_subth(-1, a, true);
                ddxt2d0_subth(-1, a, true);
            } else {
                ddxt2d_sub(-1, a, true);
                for (int i = 0; i < rows; i++) {
                    dhtColumns.forward(a, i * columns);
                }
            }
            yTransform(a);
        } else {
            if (nthreads > 1 && useThreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = firstRow; i < lastRow; i++) {
                                dhtColumns.forward(a, i * columns);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);
                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = l == nthreads - 1 ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                for (int r = 0; r < rows; r++) {
                                    temp[r] = a[r * columns + c];
                                }
                                dhtRows.forward(temp);
                                for (int r = 0; r < rows; r++) {
                                    a[r * columns + c] = temp[r];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int i = 0; i < rows; i++) {
                    dhtColumns.forward(a, i * columns);
                }
                final double[] temp = new double[rows];
                for (int c = 0; c < columns; c++) {
                    for (int r = 0; r < rows; r++) {
                        temp[r] = a[r * columns + c];
                    }
                    dhtRows.forward(temp);
                    for (int r = 0; r < rows; r++) {
                        a[r * columns + c] = temp[r];
                    }
                }
            }
            yTransform(a);
        }
    }

    /**
     * Computes 2D real, forward DHT leaving the result in <code>a</code>. The
     * data is stored in 2D array.
     *
     * @param a
     *            data to transform
     */
    public void forward(final double[][] a) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = 4 * nthreads * rows;
                if (columns == 2 * nthreads) {
                    nt >>= 1;
                } else if (columns < 2 * nthreads) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                ddxt2d_subth(-1, a, true);
                ddxt2d0_subth(-1, a, true);
            } else {
                ddxt2d_sub(-1, a, true);
                for (int i = 0; i < rows; i++) {
                    dhtColumns.forward(a[i]);
                }
            }
            y_transform(a);
        } else {
            if (nthreads > 1 && useThreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = firstRow; i < lastRow; i++) {
                                dhtColumns.forward(a[i]);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = l == nthreads - 1 ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                for (int r = 0; r < rows; r++) {
                                    temp[r] = a[r][c];
                                }
                                dhtRows.forward(temp);
                                for (int r = 0; r < rows; r++) {
                                    a[r][c] = temp[r];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int i = 0; i < rows; i++) {
                    dhtColumns.forward(a[i]);
                }
                final double[] temp = new double[rows];
                for (int c = 0; c < columns; c++) {
                    for (int r = 0; r < rows; r++) {
                        temp[r] = a[r][c];
                    }
                    dhtRows.forward(temp);
                    for (int r = 0; r < rows; r++) {
                        a[r][c] = temp[r];
                    }
                }
            }
            y_transform(a);
        }
    }

    /**
     * Computes 2D real, inverse DHT leaving the result in <code>a</code>. The
     * data is stored in 1D array in row-major order.
     *
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final double[] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = 4 * nthreads * rows;
                if (columns == 2 * nthreads) {
                    nt >>= 1;
                } else if (columns < 2 * nthreads) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                ddxt2d_subth(1, a, scale);
                ddxt2d0_subth(1, a, scale);
            } else {
                ddxt2d_sub(1, a, scale);
                for (int i = 0; i < rows; i++) {
                    dhtColumns.inverse(a, i * columns, scale);
                }
            }
            yTransform(a);
        } else {
            if (nthreads > 1 && useThreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = firstRow; i < lastRow; i++) {
                                dhtColumns.inverse(a, i * columns, scale);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = l == nthreads - 1 ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                for (int r = 0; r < rows; r++) {
                                    temp[r] = a[r * columns + c];
                                }
                                dhtRows.inverse(temp, scale);
                                for (int r = 0; r < rows; r++) {
                                    a[r * columns + c] = temp[r];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int i = 0; i < rows; i++) {
                    dhtColumns.inverse(a, i * columns, scale);
                }
                final double[] temp = new double[rows];
                for (int c = 0; c < columns; c++) {
                    for (int r = 0; r < rows; r++) {
                        temp[r] = a[r * columns + c];
                    }
                    dhtRows.inverse(temp, scale);
                    for (int r = 0; r < rows; r++) {
                        a[r * columns + c] = temp[r];
                    }
                }
            }
            yTransform(a);
        }
    }

    /**
     * Computes 2D real, inverse DHT leaving the result in <code>a</code>. The
     * data is stored in 2D array.
     *
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final double[][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = 4 * nthreads * rows;
                if (columns == 2 * nthreads) {
                    nt >>= 1;
                } else if (columns < 2 * nthreads) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                ddxt2d_subth(1, a, scale);
                ddxt2d0_subth(1, a, scale);
            } else {
                ddxt2d_sub(1, a, scale);
                for (int i = 0; i < rows; i++) {
                    dhtColumns.inverse(a[i], scale);
                }
            }
            y_transform(a);
        } else {
            if (nthreads > 1 && useThreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = firstRow; i < lastRow; i++) {
                                dhtColumns.inverse(a[i], scale);
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = columns / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstColumn = l * p;
                    final int lastColumn = l == nthreads - 1 ? columns : firstColumn + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[rows];
                            for (int c = firstColumn; c < lastColumn; c++) {
                                for (int r = 0; r < rows; r++) {
                                    temp[r] = a[r][c];
                                }
                                dhtRows.inverse(temp, scale);
                                for (int r = 0; r < rows; r++) {
                                    a[r][c] = temp[r];
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int i = 0; i < rows; i++) {
                    dhtColumns.inverse(a[i], scale);
                }
                final double[] temp = new double[rows];
                for (int c = 0; c < columns; c++) {
                    for (int r = 0; r < rows; r++) {
                        temp[r] = a[r][c];
                    }
                    dhtRows.inverse(temp, scale);
                    for (int r = 0; r < rows; r++) {
                        a[r][c] = temp[r];
                    }
                }
            }
            y_transform(a);
        }
    }

    private void ddxt2d_subth(final int isgn, final double[] a, final boolean scale) {
        int nthread = ConcurrencyUtils.getNumberOfThreads();
        int nt = 4 * rows;
        if (columns == 2 * nthread) {
            nt >>= 1;
        } else if (columns < 2 * nthread) {
            nthread = columns;
            nt >>= 2;
        }
        final int nthreads = nthread;
        final Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {
                @Override
                public void run() {
                    int idx1, idx2;
                    if (columns > 2 * nthreads) {
                        if (isgn == -1) {
                            for (int c = 4 * n0; c < columns; c += 4 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + rows + r;
                                    t[startt + r] = a[idx1];
                                    t[idx2] = a[idx1 + 1];
                                    t[idx2 + rows] = a[idx1 + 2];
                                    t[idx2 + 2 * rows] = a[idx1 + 3];
                                }
                                dhtRows.forward(t, startt);
                                dhtRows.forward(t, startt + rows);
                                dhtRows.forward(t, startt + 2 * rows);
                                dhtRows.forward(t, startt + 3 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + rows + r;
                                    a[idx1] = t[startt + r];
                                    a[idx1 + 1] = t[idx2];
                                    a[idx1 + 2] = t[idx2 + rows];
                                    a[idx1 + 3] = t[idx2 + 2 * rows];
                                }
                            }
                        } else {
                            for (int c = 4 * n0; c < columns; c += 4 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + rows + r;
                                    t[startt + r] = a[idx1];
                                    t[idx2] = a[idx1 + 1];
                                    t[idx2 + rows] = a[idx1 + 2];
                                    t[idx2 + 2 * rows] = a[idx1 + 3];
                                }
                                dhtRows.inverse(t, startt, scale);
                                dhtRows.inverse(t, startt + rows, scale);
                                dhtRows.inverse(t, startt + 2 * rows, scale);
                                dhtRows.inverse(t, startt + 3 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = r * columns + c;
                                    idx2 = startt + rows + r;
                                    a[idx1] = t[startt + r];
                                    a[idx1 + 1] = t[idx2];
                                    a[idx1 + 2] = t[idx2 + rows];
                                    a[idx1 + 3] = t[idx2 + 2 * rows];
                                }
                            }
                        }
                    } else if (columns == 2 * nthreads) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = r * columns + 2 * n0;
                            idx2 = startt + r;
                            t[idx2] = a[idx1];
                            t[idx2 + rows] = a[idx1 + 1];
                        }
                        if (isgn == -1) {
                            dhtRows.forward(t, startt);
                            dhtRows.forward(t, startt + rows);
                        } else {
                            dhtRows.inverse(t, startt, scale);
                            dhtRows.inverse(t, startt + rows, scale);
                        }
                        for (int r = 0; r < rows; r++) {
                            idx1 = r * columns + 2 * n0;
                            idx2 = startt + r;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + rows];
                        }
                    } else if (columns == nthreads) {
                        for (int r = 0; r < rows; r++) {
                            t[startt + r] = a[r * columns + n0];
                        }
                        if (isgn == -1) {
                            dhtRows.forward(t, startt);
                        } else {
                            dhtRows.inverse(t, startt, scale);
                        }
                        for (int r = 0; r < rows; r++) {
                            a[r * columns + n0] = t[startt + r];
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt2d_subth(final int isgn, final double[][] a, final boolean scale) {
        int nthread = ConcurrencyUtils.getNumberOfThreads();
        int nt = 4 * rows;
        if (columns == 2 * nthread) {
            nt >>= 1;
        } else if (columns < 2 * nthread) {
            nthread = columns;
            nt >>= 2;
        }
        final int nthreads = nthread;
        final Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {
                @Override
                public void run() {
                    int idx2;
                    if (columns > 2 * nthreads) {
                        if (isgn == -1) {
                            for (int c = 4 * n0; c < columns; c += 4 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + rows + r;
                                    t[startt + r] = a[r][c];
                                    t[idx2] = a[r][c + 1];
                                    t[idx2 + rows] = a[r][c + 2];
                                    t[idx2 + 2 * rows] = a[r][c + 3];
                                }
                                dhtRows.forward(t, startt);
                                dhtRows.forward(t, startt + rows);
                                dhtRows.forward(t, startt + 2 * rows);
                                dhtRows.forward(t, startt + 3 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + rows + r;
                                    a[r][c] = t[startt + r];
                                    a[r][c + 1] = t[idx2];
                                    a[r][c + 2] = t[idx2 + rows];
                                    a[r][c + 3] = t[idx2 + 2 * rows];
                                }
                            }
                        } else {
                            for (int c = 4 * n0; c < columns; c += 4 * nthreads) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + rows + r;
                                    t[startt + r] = a[r][c];
                                    t[idx2] = a[r][c + 1];
                                    t[idx2 + rows] = a[r][c + 2];
                                    t[idx2 + 2 * rows] = a[r][c + 3];
                                }
                                dhtRows.inverse(t, startt, scale);
                                dhtRows.inverse(t, startt + rows, scale);
                                dhtRows.inverse(t, startt + 2 * rows, scale);
                                dhtRows.inverse(t, startt + 3 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + rows + r;
                                    a[r][c] = t[startt + r];
                                    a[r][c + 1] = t[idx2];
                                    a[r][c + 2] = t[idx2 + rows];
                                    a[r][c + 3] = t[idx2 + 2 * rows];
                                }
                            }
                        }
                    } else if (columns == 2 * nthreads) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = startt + r;
                            t[idx2] = a[r][2 * n0];
                            t[idx2 + rows] = a[r][2 * n0 + 1];
                        }
                        if (isgn == -1) {
                            dhtRows.forward(t, startt);
                            dhtRows.forward(t, startt + rows);
                        } else {
                            dhtRows.inverse(t, startt, scale);
                            dhtRows.inverse(t, startt + rows, scale);
                        }
                        for (int r = 0; r < rows; r++) {
                            idx2 = startt + r;
                            a[r][2 * n0] = t[idx2];
                            a[r][2 * n0 + 1] = t[idx2 + rows];
                        }
                    } else if (columns == nthreads) {
                        for (int r = 0; r < rows; r++) {
                            t[startt + r] = a[r][n0];
                        }
                        if (isgn == -1) {
                            dhtRows.forward(t, startt);
                        } else {
                            dhtRows.inverse(t, startt, scale);
                        }
                        for (int r = 0; r < rows; r++) {
                            a[r][n0] = t[startt + r];
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt2d0_subth(final int isgn, final double[] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows
                : ConcurrencyUtils.getNumberOfThreads();

        final Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                @Override
                public void run() {
                    if (isgn == -1) {
                        for (int r = n0; r < rows; r += nthreads) {
                            dhtColumns.forward(a, r * columns);
                        }
                    } else {
                        for (int r = n0; r < rows; r += nthreads) {
                            dhtColumns.inverse(a, r * columns, scale);
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt2d0_subth(final int isgn, final double[][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows
                : ConcurrencyUtils.getNumberOfThreads();

        final Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                @Override
                public void run() {
                    if (isgn == -1) {
                        for (int r = n0; r < rows; r += nthreads) {
                            dhtColumns.forward(a[r]);
                        }
                    } else {
                        for (int r = n0; r < rows; r += nthreads) {
                            dhtColumns.inverse(a[r], scale);
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt2d_sub(final int isgn, final double[] a, final boolean scale) {
        int idx1, idx2;

        if (columns > 2) {
            if (isgn == -1) {
                for (int c = 0; c < columns; c += 4) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = rows + r;
                        t[r] = a[idx1];
                        t[idx2] = a[idx1 + 1];
                        t[idx2 + rows] = a[idx1 + 2];
                        t[idx2 + 2 * rows] = a[idx1 + 3];
                    }
                    dhtRows.forward(t, 0);
                    dhtRows.forward(t, rows);
                    dhtRows.forward(t, 2 * rows);
                    dhtRows.forward(t, 3 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = rows + r;
                        a[idx1] = t[r];
                        a[idx1 + 1] = t[idx2];
                        a[idx1 + 2] = t[idx2 + rows];
                        a[idx1 + 3] = t[idx2 + 2 * rows];
                    }
                }
            } else {
                for (int c = 0; c < columns; c += 4) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = rows + r;
                        t[r] = a[idx1];
                        t[idx2] = a[idx1 + 1];
                        t[idx2 + rows] = a[idx1 + 2];
                        t[idx2 + 2 * rows] = a[idx1 + 3];
                    }
                    dhtRows.inverse(t, 0, scale);
                    dhtRows.inverse(t, rows, scale);
                    dhtRows.inverse(t, 2 * rows, scale);
                    dhtRows.inverse(t, 3 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = r * columns + c;
                        idx2 = rows + r;
                        a[idx1] = t[r];
                        a[idx1 + 1] = t[idx2];
                        a[idx1 + 2] = t[idx2 + rows];
                        a[idx1 + 3] = t[idx2 + 2 * rows];
                    }
                }
            }
        } else if (columns == 2) {
            for (int r = 0; r < rows; r++) {
                idx1 = r * columns;
                t[r] = a[idx1];
                t[rows + r] = a[idx1 + 1];
            }
            if (isgn == -1) {
                dhtRows.forward(t, 0);
                dhtRows.forward(t, rows);
            } else {
                dhtRows.inverse(t, 0, scale);
                dhtRows.inverse(t, rows, scale);
            }
            for (int r = 0; r < rows; r++) {
                idx1 = r * columns;
                a[idx1] = t[r];
                a[idx1 + 1] = t[rows + r];
            }
        }
    }

    private void ddxt2d_sub(final int isgn, final double[][] a, final boolean scale) {
        int idx2;

        if (columns > 2) {
            if (isgn == -1) {
                for (int c = 0; c < columns; c += 4) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = rows + r;
                        t[r] = a[r][c];
                        t[idx2] = a[r][c + 1];
                        t[idx2 + rows] = a[r][c + 2];
                        t[idx2 + 2 * rows] = a[r][c + 3];
                    }
                    dhtRows.forward(t, 0);
                    dhtRows.forward(t, rows);
                    dhtRows.forward(t, 2 * rows);
                    dhtRows.forward(t, 3 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx2 = rows + r;
                        a[r][c] = t[r];
                        a[r][c + 1] = t[idx2];
                        a[r][c + 2] = t[idx2 + rows];
                        a[r][c + 3] = t[idx2 + 2 * rows];
                    }
                }
            } else {
                for (int c = 0; c < columns; c += 4) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = rows + r;
                        t[r] = a[r][c];
                        t[idx2] = a[r][c + 1];
                        t[idx2 + rows] = a[r][c + 2];
                        t[idx2 + 2 * rows] = a[r][c + 3];
                    }
                    dhtRows.inverse(t, 0, scale);
                    dhtRows.inverse(t, rows, scale);
                    dhtRows.inverse(t, 2 * rows, scale);
                    dhtRows.inverse(t, 3 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx2 = rows + r;
                        a[r][c] = t[r];
                        a[r][c + 1] = t[idx2];
                        a[r][c + 2] = t[idx2 + rows];
                        a[r][c + 3] = t[idx2 + 2 * rows];
                    }
                }
            }
        } else if (columns == 2) {
            for (int r = 0; r < rows; r++) {
                t[r] = a[r][0];
                t[rows + r] = a[r][1];
            }
            if (isgn == -1) {
                dhtRows.forward(t, 0);
                dhtRows.forward(t, rows);
            } else {
                dhtRows.inverse(t, 0, scale);
                dhtRows.inverse(t, rows, scale);
            }
            for (int r = 0; r < rows; r++) {
                a[r][0] = t[r];
                a[r][1] = t[rows + r];
            }
        }
    }

    private void yTransform(final double[] a) {
        int mRow, mCol, idx1, idx2;
        double A, B, C, D, E;
        for (int r = 0; r <= rows / 2; r++) {
            mRow = (rows - r) % rows;
            idx1 = r * columns;
            idx2 = mRow * columns;
            for (int c = 0; c <= columns / 2; c++) {
                mCol = (columns - c) % columns;
                A = a[idx1 + c];
                B = a[idx2 + c];
                C = a[idx1 + mCol];
                D = a[idx2 + mCol];
                E = (A + D - (B + C)) / 2;
                a[idx1 + c] = A - E;
                a[idx2 + c] = B + E;
                a[idx1 + mCol] = C + E;
                a[idx2 + mCol] = D - E;
            }
        }
    }

    private void y_transform(final double[][] a) {
        int mRow, mCol;
        double A, B, C, D, E;
        for (int r = 0; r <= rows / 2; r++) {
            mRow = (rows - r) % rows;
            for (int c = 0; c <= columns / 2; c++) {
                mCol = (columns - c) % columns;
                A = a[r][c];
                B = a[mRow][c];
                C = a[r][mCol];
                D = a[mRow][mCol];
                E = (A + D - (B + C)) / 2;
                a[r][c] = A - E;
                a[mRow][c] = B + E;
                a[r][mCol] = C + E;
                a[mRow][mCol] = D - E;
            }
        }
    }

}
