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

package de.gsi.math.spectra.fft;

import java.util.concurrent.Future;

import de.gsi.math.utils.ConcurrencyUtils;

/**
 * Computes 3D Discrete Fourier Transform (DFT) of complex and real, double precision data. The sizes of all three
 * dimensions can be arbitrary numbers. This is a parallel implementation of split-radix and mixed-radix algorithms
 * optimized for SMP systems. <br>
 * <br>
 * Part of the code is derived from General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html)
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class DoubleFFT_3D {

    private final int slices;
    private final int rows;
    private int columns;
    private int sliceStride;
    private int rowStride;
    private double[] t;
    private final DoubleFFT_1D fftSlices;
    private DoubleFFT_1D fftRows;
    private DoubleFFT_1D fftColumns;
    private int oldNthreads;
    private int nt;
    private boolean isPowerOfTwo = false;
    private boolean useThreads = false;

    /**
     * Creates new instance of DoubleFFT_3D.
     *
     * @param slices number of slices
     * @param rows number of rows
     * @param columns number of columns
     */
    public DoubleFFT_3D(final int slices, final int rows, final int columns) {
        if (slices <= 1 || rows <= 1 || columns <= 1) {
            throw new IllegalArgumentException("slices, rows and columns must be greater than 1");
        }
        this.slices = slices;
        this.rows = rows;
        this.columns = columns;
        sliceStride = rows * columns;
        rowStride = columns;
        if (slices * rows * columns >= ConcurrencyUtils.getThreadsBeginN_3D()) {
            useThreads = true;
        }
        if (ConcurrencyUtils.isPowerOf2(slices) && ConcurrencyUtils.isPowerOf2(rows)
                && ConcurrencyUtils.isPowerOf2(columns)) {
            isPowerOfTwo = true;
            oldNthreads = ConcurrencyUtils.getNumberOfThreads();
            nt = slices;
            if (nt < rows) {
                nt = rows;
            }
            nt *= 8;
            if (oldNthreads > 1) {
                nt *= oldNthreads;
            }
            if (2 * columns == 4) {
                nt >>= 1;
            }
            t = new double[nt];
        }
        fftSlices = new DoubleFFT_1D(slices);
        if (slices == rows) {
            fftRows = fftSlices;
        } else {
            fftRows = new DoubleFFT_1D(rows);
        }
        if (slices == columns) {
            fftColumns = fftSlices;
        } else if (rows == columns) {
            fftColumns = fftRows;
        } else {
            fftColumns = new DoubleFFT_1D(columns);
        }

    }

    private void cdft3db_sub(final int isgn, final double[] a, final boolean scale) {
        int idx0, idx1, idx2, idx3, idx4, idx5;

        if (isgn == -1) {
            if (columns > 4) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int c = 0; c < columns; c += 8) {
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            t[idx2] = a[idx1];
                            t[idx2 + 1] = a[idx1 + 1];
                            t[idx3] = a[idx1 + 2];
                            t[idx3 + 1] = a[idx1 + 3];
                            t[idx4] = a[idx1 + 4];
                            t[idx4 + 1] = a[idx1 + 5];
                            t[idx5] = a[idx1 + 6];
                            t[idx5 + 1] = a[idx1 + 7];
                        }
                        fftSlices.complexForward(t, 0);
                        fftSlices.complexForward(t, 2 * slices);
                        fftSlices.complexForward(t, 4 * slices);
                        fftSlices.complexForward(t, 6 * slices);
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + 1];
                            a[idx1 + 2] = t[idx3];
                            a[idx1 + 3] = t[idx3 + 1];
                            a[idx1 + 4] = t[idx4];
                            a[idx1 + 5] = t[idx4 + 1];
                            a[idx1 + 6] = t[idx5];
                            a[idx1 + 7] = t[idx5 + 1];
                        }
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                    }
                    fftSlices.complexForward(t, 0);
                    fftSlices.complexForward(t, 2 * slices);
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                    }
                    fftSlices.complexForward(t, 0);
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                    }
                }
            }
        } else {
            if (columns > 4) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int c = 0; c < columns; c += 8) {
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            t[idx2] = a[idx1];
                            t[idx2 + 1] = a[idx1 + 1];
                            t[idx3] = a[idx1 + 2];
                            t[idx3 + 1] = a[idx1 + 3];
                            t[idx4] = a[idx1 + 4];
                            t[idx4 + 1] = a[idx1 + 5];
                            t[idx5] = a[idx1 + 6];
                            t[idx5 + 1] = a[idx1 + 7];
                        }
                        fftSlices.complexInverse(t, 0, scale);
                        fftSlices.complexInverse(t, 2 * slices, scale);
                        fftSlices.complexInverse(t, 4 * slices, scale);
                        fftSlices.complexInverse(t, 6 * slices, scale);
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + 1];
                            a[idx1 + 2] = t[idx3];
                            a[idx1 + 3] = t[idx3 + 1];
                            a[idx1 + 4] = t[idx4];
                            a[idx1 + 5] = t[idx4 + 1];
                            a[idx1 + 6] = t[idx5];
                            a[idx1 + 7] = t[idx5 + 1];
                        }
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                    }
                    fftSlices.complexInverse(t, 0, scale);
                    fftSlices.complexInverse(t, 2 * slices, scale);
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                    }
                    fftSlices.complexInverse(t, 0, scale);
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        idx2 = 2 * s;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                    }
                }
            }
        }
    }

    private void cdft3db_sub(final int isgn, final double[][][] a, final boolean scale) {
        int idx2, idx3, idx4, idx5;

        if (isgn == -1) {
            if (columns > 4) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int s = 0; s < slices; s++) {
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            t[idx2] = a[s][r][c];
                            t[idx2 + 1] = a[s][r][c + 1];
                            t[idx3] = a[s][r][c + 2];
                            t[idx3 + 1] = a[s][r][c + 3];
                            t[idx4] = a[s][r][c + 4];
                            t[idx4 + 1] = a[s][r][c + 5];
                            t[idx5] = a[s][r][c + 6];
                            t[idx5 + 1] = a[s][r][c + 7];
                        }
                        fftSlices.complexForward(t, 0);
                        fftSlices.complexForward(t, 2 * slices);
                        fftSlices.complexForward(t, 4 * slices);
                        fftSlices.complexForward(t, 6 * slices);
                        for (int s = 0; s < slices; s++) {
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            a[s][r][c] = t[idx2];
                            a[s][r][c + 1] = t[idx2 + 1];
                            a[s][r][c + 2] = t[idx3];
                            a[s][r][c + 3] = t[idx3 + 1];
                            a[s][r][c + 4] = t[idx4];
                            a[s][r][c + 5] = t[idx4 + 1];
                            a[s][r][c + 6] = t[idx5];
                            a[s][r][c + 7] = t[idx5 + 1];
                        }
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                        t[idx3] = a[s][r][2];
                        t[idx3 + 1] = a[s][r][3];
                    }
                    fftSlices.complexForward(t, 0);
                    fftSlices.complexForward(t, 2 * slices);
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                        a[s][r][2] = t[idx3];
                        a[s][r][3] = t[idx3 + 1];
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                    }
                    fftSlices.complexForward(t, 0);
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                    }
                }
            }
        } else {
            if (columns > 4) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int s = 0; s < slices; s++) {
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            t[idx2] = a[s][r][c];
                            t[idx2 + 1] = a[s][r][c + 1];
                            t[idx3] = a[s][r][c + 2];
                            t[idx3 + 1] = a[s][r][c + 3];
                            t[idx4] = a[s][r][c + 4];
                            t[idx4 + 1] = a[s][r][c + 5];
                            t[idx5] = a[s][r][c + 6];
                            t[idx5 + 1] = a[s][r][c + 7];
                        }
                        fftSlices.complexInverse(t, 0, scale);
                        fftSlices.complexInverse(t, 2 * slices, scale);
                        fftSlices.complexInverse(t, 4 * slices, scale);
                        fftSlices.complexInverse(t, 6 * slices, scale);
                        for (int s = 0; s < slices; s++) {
                            idx2 = 2 * s;
                            idx3 = 2 * slices + 2 * s;
                            idx4 = idx3 + 2 * slices;
                            idx5 = idx4 + 2 * slices;
                            a[s][r][c] = t[idx2];
                            a[s][r][c + 1] = t[idx2 + 1];
                            a[s][r][c + 2] = t[idx3];
                            a[s][r][c + 3] = t[idx3 + 1];
                            a[s][r][c + 4] = t[idx4];
                            a[s][r][c + 5] = t[idx4 + 1];
                            a[s][r][c + 6] = t[idx5];
                            a[s][r][c + 7] = t[idx5 + 1];
                        }
                    }
                }
            } else if (columns == 4) {
                for (int r = 0; r < rows; r++) {
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                        t[idx3] = a[s][r][2];
                        t[idx3 + 1] = a[s][r][3];
                    }
                    fftSlices.complexInverse(t, 0, scale);
                    fftSlices.complexInverse(t, 2 * slices, scale);
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        idx3 = 2 * slices + 2 * s;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                        a[s][r][2] = t[idx3];
                        a[s][r][3] = t[idx3 + 1];
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                    }
                    fftSlices.complexInverse(t, 0, scale);
                    for (int s = 0; s < slices; s++) {
                        idx2 = 2 * s;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                    }
                }
            }
        }
    }

    private void cdft3db_subth(final int isgn, final double[] a, final boolean scale) {
        int nt, i;

        final int nthreads = Math.min(ConcurrencyUtils.getNumberOfThreads(), rows);
        nt = 8 * slices;
        if (columns == 4) {
            nt >>= 1;
        } else if (columns < 4) {
            nt >>= 2;
        }
        final Future<?>[] futures = new Future[nthreads];
        for (i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                @Override
                public void run() {
                    int idx0, idx1, idx2, idx3, idx4, idx5;

                    if (isgn == -1) {
                        if (columns > 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int c = 0; c < columns; c += 8) {
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        t[idx2] = a[idx1];
                                        t[idx2 + 1] = a[idx1 + 1];
                                        t[idx3] = a[idx1 + 2];
                                        t[idx3 + 1] = a[idx1 + 3];
                                        t[idx4] = a[idx1 + 4];
                                        t[idx4 + 1] = a[idx1 + 5];
                                        t[idx5] = a[idx1 + 6];
                                        t[idx5 + 1] = a[idx1 + 7];
                                    }
                                    fftSlices.complexForward(t, startt);
                                    fftSlices.complexForward(t, startt + 2 * slices);
                                    fftSlices.complexForward(t, startt + 4 * slices);
                                    fftSlices.complexForward(t, startt + 6 * slices);
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        a[idx1] = t[idx2];
                                        a[idx1 + 1] = t[idx2 + 1];
                                        a[idx1 + 2] = t[idx3];
                                        a[idx1 + 3] = t[idx3 + 1];
                                        a[idx1 + 4] = t[idx4];
                                        a[idx1 + 5] = t[idx4 + 1];
                                        a[idx1 + 6] = t[idx5];
                                        a[idx1 + 7] = t[idx5 + 1];
                                    }
                                }
                            }
                        } else if (columns == 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                }
                                fftSlices.complexForward(t, startt);
                                fftSlices.complexForward(t, startt + 2 * slices);
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                }
                                fftSlices.complexForward(t, startt);
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                }
                            }
                        }
                    } else {
                        if (columns > 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int c = 0; c < columns; c += 8) {
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        t[idx2] = a[idx1];
                                        t[idx2 + 1] = a[idx1 + 1];
                                        t[idx3] = a[idx1 + 2];
                                        t[idx3 + 1] = a[idx1 + 3];
                                        t[idx4] = a[idx1 + 4];
                                        t[idx4 + 1] = a[idx1 + 5];
                                        t[idx5] = a[idx1 + 6];
                                        t[idx5 + 1] = a[idx1 + 7];
                                    }
                                    fftSlices.complexInverse(t, startt, scale);
                                    fftSlices.complexInverse(t, startt + 2 * slices, scale);
                                    fftSlices.complexInverse(t, startt + 4 * slices, scale);
                                    fftSlices.complexInverse(t, startt + 6 * slices, scale);
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        a[idx1] = t[idx2];
                                        a[idx1 + 1] = t[idx2 + 1];
                                        a[idx1 + 2] = t[idx3];
                                        a[idx1 + 3] = t[idx3 + 1];
                                        a[idx1 + 4] = t[idx4];
                                        a[idx1 + 5] = t[idx4 + 1];
                                        a[idx1 + 6] = t[idx5];
                                        a[idx1 + 7] = t[idx5 + 1];
                                    }
                                }
                            }
                        } else if (columns == 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                }
                                fftSlices.complexInverse(t, startt, scale);
                                fftSlices.complexInverse(t, startt + 2 * slices, scale);
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                }
                                fftSlices.complexInverse(t, startt, scale);
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    idx2 = startt + 2 * s;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                }
                            }
                        }
                    }

                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void cdft3db_subth(final int isgn, final double[][][] a, final boolean scale) {
        int nt, i;

        final int nthreads = Math.min(ConcurrencyUtils.getNumberOfThreads(), rows);
        nt = 8 * slices;
        if (columns == 4) {
            nt >>= 1;
        } else if (columns < 4) {
            nt >>= 2;
        }
        final Future<?>[] futures = new Future[nthreads];
        for (i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                @Override
                public void run() {
                    int idx2, idx3, idx4, idx5;

                    if (isgn == -1) {
                        if (columns > 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        t[idx2] = a[s][r][c];
                                        t[idx2 + 1] = a[s][r][c + 1];
                                        t[idx3] = a[s][r][c + 2];
                                        t[idx3 + 1] = a[s][r][c + 3];
                                        t[idx4] = a[s][r][c + 4];
                                        t[idx4 + 1] = a[s][r][c + 5];
                                        t[idx5] = a[s][r][c + 6];
                                        t[idx5 + 1] = a[s][r][c + 7];
                                    }
                                    fftSlices.complexForward(t, startt);
                                    fftSlices.complexForward(t, startt + 2 * slices);
                                    fftSlices.complexForward(t, startt + 4 * slices);
                                    fftSlices.complexForward(t, startt + 6 * slices);
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        a[s][r][c] = t[idx2];
                                        a[s][r][c + 1] = t[idx2 + 1];
                                        a[s][r][c + 2] = t[idx3];
                                        a[s][r][c + 3] = t[idx3 + 1];
                                        a[s][r][c + 4] = t[idx4];
                                        a[s][r][c + 5] = t[idx4 + 1];
                                        a[s][r][c + 6] = t[idx5];
                                        a[s][r][c + 7] = t[idx5 + 1];
                                    }
                                }
                            }
                        } else if (columns == 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                    t[idx3] = a[s][r][2];
                                    t[idx3 + 1] = a[s][r][3];
                                }
                                fftSlices.complexForward(t, startt);
                                fftSlices.complexForward(t, startt + 2 * slices);
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                    a[s][r][2] = t[idx3];
                                    a[s][r][3] = t[idx3 + 1];
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                }
                                fftSlices.complexForward(t, startt);
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                }
                            }
                        }
                    } else {
                        if (columns > 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        t[idx2] = a[s][r][c];
                                        t[idx2 + 1] = a[s][r][c + 1];
                                        t[idx3] = a[s][r][c + 2];
                                        t[idx3 + 1] = a[s][r][c + 3];
                                        t[idx4] = a[s][r][c + 4];
                                        t[idx4 + 1] = a[s][r][c + 5];
                                        t[idx5] = a[s][r][c + 6];
                                        t[idx5 + 1] = a[s][r][c + 7];
                                    }
                                    fftSlices.complexInverse(t, startt, scale);
                                    fftSlices.complexInverse(t, startt + 2 * slices, scale);
                                    fftSlices.complexInverse(t, startt + 4 * slices, scale);
                                    fftSlices.complexInverse(t, startt + 6 * slices, scale);
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + 2 * s;
                                        idx3 = startt + 2 * slices + 2 * s;
                                        idx4 = idx3 + 2 * slices;
                                        idx5 = idx4 + 2 * slices;
                                        a[s][r][c] = t[idx2];
                                        a[s][r][c + 1] = t[idx2 + 1];
                                        a[s][r][c + 2] = t[idx3];
                                        a[s][r][c + 3] = t[idx3 + 1];
                                        a[s][r][c + 4] = t[idx4];
                                        a[s][r][c + 5] = t[idx4 + 1];
                                        a[s][r][c + 6] = t[idx5];
                                        a[s][r][c + 7] = t[idx5 + 1];
                                    }
                                }
                            }
                        } else if (columns == 4) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                    t[idx3] = a[s][r][2];
                                    t[idx3 + 1] = a[s][r][3];
                                }
                                fftSlices.complexInverse(t, startt, scale);
                                fftSlices.complexInverse(t, startt + 2 * slices, scale);
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    idx3 = startt + 2 * slices + 2 * s;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                    a[s][r][2] = t[idx3];
                                    a[s][r][3] = t[idx3 + 1];
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                }
                                fftSlices.complexInverse(t, startt, scale);
                                for (int s = 0; s < slices; s++) {
                                    idx2 = startt + 2 * s;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                }
                            }
                        }
                    }

                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    /**
     * Computes 3D forward DFT of complex data leaving the result in <code>a</code>. The data is stored in 1D array
     * addressed in slice-major, then row-major, then column-major, in order of significance, i.e. element (i,j,k) of 3D
     * array x[slices][rows][2*columns] is stored in a[i*sliceStride + j*rowStride + k], where sliceStride = rows * 2 *
     * columns and rowStride = 2 * columns. Complex number is stored as two double values in sequence: the real and
     * imaginary part, i.e. the input array must be of size slices*rows*2*columns. The physical layout of the input data
     * is as follows:
     *
     * <pre>
     * a[k1*sliceStride + k2*rowStride + 2*k3] = Re[k1][k2][k3],
     * a[k1*sliceStride + k2*rowStride + 2*k3+1] = Im[k1][k2][k3], 0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;=k3&lt;columns,
     * </pre>
     *
     * @param a data to transform
     */
    public void complexForward(final double[] a) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            final int oldn3 = columns;
            columns = 2 * columns;

            sliceStride = rows * columns;
            rowStride = columns;

            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(0, -1, a, true);
                cdft3db_subth(-1, a, true);
            } else {
                xdft3da_sub2(0, -1, a, true);
                cdft3db_sub(-1, a, true);
            }
            columns = oldn3;
            sliceStride = rows * columns;
            rowStride = columns;
        } else {
            sliceStride = 2 * rows * columns;
            rowStride = 2 * columns;
            if (nthreads > 1 && useThreads && slices >= nthreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                final int idx1 = s * sliceStride;
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexForward(a, idx1 + r * rowStride);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                final int idx1 = s * sliceStride;
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int r = 0; r < rows; r++) {
                                        final int idx3 = idx1 + idx2 + r * rowStride;
                                        final int idx4 = 2 * r;
                                        temp[idx4] = a[idx3];
                                        temp[idx4 + 1] = a[idx3 + 1];
                                    }
                                    fftRows.complexForward(temp);
                                    for (int r = 0; r < rows; r++) {
                                        final int idx3 = idx1 + idx2 + r * rowStride;
                                        final int idx4 = 2 * r;
                                        a[idx3] = temp[idx4];
                                        a[idx3 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                final int idx1 = r * rowStride;
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int s = 0; s < slices; s++) {
                                        final int idx3 = s * sliceStride + idx1 + idx2;
                                        final int idx4 = 2 * s;
                                        temp[idx4] = a[idx3];
                                        temp[idx4 + 1] = a[idx3 + 1];
                                    }
                                    fftSlices.complexForward(temp);
                                    for (int s = 0; s < slices; s++) {
                                        final int idx3 = s * sliceStride + idx1 + idx2;
                                        final int idx4 = 2 * s;
                                        a[idx3] = temp[idx4];
                                        a[idx3 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    final int idx1 = s * sliceStride;
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexForward(a, idx1 + r * rowStride);
                    }
                }

                double[] temp = new double[2 * rows];
                for (int s = 0; s < slices; s++) {
                    final int idx1 = s * sliceStride;
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int r = 0; r < rows; r++) {
                            final int idx3 = idx1 + idx2 + r * rowStride;
                            final int idx4 = 2 * r;
                            temp[idx4] = a[idx3];
                            temp[idx4 + 1] = a[idx3 + 1];
                        }
                        fftRows.complexForward(temp);
                        for (int r = 0; r < rows; r++) {
                            final int idx3 = idx1 + idx2 + r * rowStride;
                            final int idx4 = 2 * r;
                            a[idx3] = temp[idx4];
                            a[idx3 + 1] = temp[idx4 + 1];
                        }
                    }
                }

                temp = new double[2 * slices];
                for (int r = 0; r < rows; r++) {
                    final int idx1 = r * rowStride;
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int s = 0; s < slices; s++) {
                            final int idx3 = s * sliceStride + idx1 + idx2;
                            final int idx4 = 2 * s;
                            temp[idx4] = a[idx3];
                            temp[idx4 + 1] = a[idx3 + 1];
                        }
                        fftSlices.complexForward(temp);
                        for (int s = 0; s < slices; s++) {
                            final int idx3 = s * sliceStride + idx1 + idx2;
                            final int idx4 = 2 * s;
                            a[idx3] = temp[idx4];
                            a[idx3 + 1] = temp[idx4 + 1];
                        }
                    }
                }
            }
            sliceStride = rows * columns;
            rowStride = columns;
        }
    }

    /**
     * Computes 3D forward DFT of complex data leaving the result in <code>a</code>. The data is stored in 3D array.
     * Complex data is represented by 2 double values in sequence: the real and imaginary part, i.e. the input array
     * must be of size slices by rows by 2*columns. The physical layout of the input data is as follows:
     *
     * <pre>
     * a[k1][k2][2*k3] = Re[k1][k2][k3],
     * a[k1][k2][2*k3+1] = Im[k1][k2][k3], 0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;=k3&lt;columns,
     * </pre>
     *
     * @param a data to transform
     */
    public void complexForward(final double[][][] a) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            final int oldn3 = columns;
            columns = 2 * columns;

            sliceStride = rows * columns;
            rowStride = columns;

            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(0, -1, a, true);
                cdft3db_subth(-1, a, true);
            } else {
                xdft3da_sub2(0, -1, a, true);
                cdft3db_sub(-1, a, true);
            }
            columns = oldn3;
            sliceStride = rows * columns;
            rowStride = columns;
        } else {
            if (nthreads > 1 && useThreads && slices >= nthreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexForward(a[s][r]);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int r = 0; r < rows; r++) {
                                        final int idx4 = 2 * r;
                                        temp[idx4] = a[s][r][idx2];
                                        temp[idx4 + 1] = a[s][r][idx2 + 1];
                                    }
                                    fftRows.complexForward(temp);
                                    for (int r = 0; r < rows; r++) {
                                        final int idx4 = 2 * r;
                                        a[s][r][idx2] = temp[idx4];
                                        a[s][r][idx2 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int s = 0; s < slices; s++) {
                                        final int idx4 = 2 * s;
                                        temp[idx4] = a[s][r][idx2];
                                        temp[idx4 + 1] = a[s][r][idx2 + 1];
                                    }
                                    fftSlices.complexForward(temp);
                                    for (int s = 0; s < slices; s++) {
                                        final int idx4 = 2 * s;
                                        a[s][r][idx2] = temp[idx4];
                                        a[s][r][idx2 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexForward(a[s][r]);
                    }
                }

                double[] temp = new double[2 * rows];
                for (int s = 0; s < slices; s++) {
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int r = 0; r < rows; r++) {
                            final int idx4 = 2 * r;
                            temp[idx4] = a[s][r][idx2];
                            temp[idx4 + 1] = a[s][r][idx2 + 1];
                        }
                        fftRows.complexForward(temp);
                        for (int r = 0; r < rows; r++) {
                            final int idx4 = 2 * r;
                            a[s][r][idx2] = temp[idx4];
                            a[s][r][idx2 + 1] = temp[idx4 + 1];
                        }
                    }
                }

                temp = new double[2 * slices];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int s = 0; s < slices; s++) {
                            final int idx4 = 2 * s;
                            temp[idx4] = a[s][r][idx2];
                            temp[idx4 + 1] = a[s][r][idx2 + 1];
                        }
                        fftSlices.complexForward(temp);
                        for (int s = 0; s < slices; s++) {
                            final int idx4 = 2 * s;
                            a[s][r][idx2] = temp[idx4];
                            a[s][r][idx2 + 1] = temp[idx4 + 1];
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes 3D inverse DFT of complex data leaving the result in <code>a</code>. The data is stored in a 1D array
     * addressed in slice-major, then row-major, then column-major, in order of significance, i.e. element (i,j,k) of
     * 3-d array x[slices][rows][2*columns] is stored in a[i*sliceStride + j*rowStride + k], where sliceStride = rows *
     * 2 * columns and rowStride = 2 * columns. Complex number is stored as two double values in sequence: the real and
     * imaginary part, i.e. the input array must be of size slices*rows*2*columns. The physical layout of the input data
     * is as follows:
     *
     * <pre>
     * a[k1*sliceStride + k2*rowStride + 2*k3] = Re[k1][k2][k3],
     * a[k1*sliceStride + k2*rowStride + 2*k3+1] = Im[k1][k2][k3], 0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;=k3&lt;columns,
     * </pre>
     *
     * @param a data to transform
     * @param scale if true then scaling is performed
     */
    public void complexInverse(final double[] a, final boolean scale) {

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();

        if (isPowerOfTwo) {
            final int oldn3 = columns;
            columns = 2 * columns;
            sliceStride = rows * columns;
            rowStride = columns;
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(0, 1, a, scale);
                cdft3db_subth(1, a, scale);
            } else {
                xdft3da_sub2(0, 1, a, scale);
                cdft3db_sub(1, a, scale);
            }
            columns = oldn3;
            sliceStride = rows * columns;
            rowStride = columns;
        } else {
            sliceStride = 2 * rows * columns;
            rowStride = 2 * columns;
            if (nthreads > 1 && useThreads && slices >= nthreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                final int idx1 = s * sliceStride;
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexInverse(a, idx1 + r * rowStride, scale);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                final int idx1 = s * sliceStride;
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int r = 0; r < rows; r++) {
                                        final int idx3 = idx1 + idx2 + r * rowStride;
                                        final int idx4 = 2 * r;
                                        temp[idx4] = a[idx3];
                                        temp[idx4 + 1] = a[idx3 + 1];
                                    }
                                    fftRows.complexInverse(temp, scale);
                                    for (int r = 0; r < rows; r++) {
                                        final int idx3 = idx1 + idx2 + r * rowStride;
                                        final int idx4 = 2 * r;
                                        a[idx3] = temp[idx4];
                                        a[idx3 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                final int idx1 = r * rowStride;
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int s = 0; s < slices; s++) {
                                        final int idx3 = s * sliceStride + idx1 + idx2;
                                        final int idx4 = 2 * s;
                                        temp[idx4] = a[idx3];
                                        temp[idx4 + 1] = a[idx3 + 1];
                                    }
                                    fftSlices.complexInverse(temp, scale);
                                    for (int s = 0; s < slices; s++) {
                                        final int idx3 = s * sliceStride + idx1 + idx2;
                                        final int idx4 = 2 * s;
                                        a[idx3] = temp[idx4];
                                        a[idx3 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    final int idx1 = s * sliceStride;
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexInverse(a, idx1 + r * rowStride, scale);
                    }
                }
                double[] temp = new double[2 * rows];
                for (int s = 0; s < slices; s++) {
                    final int idx1 = s * sliceStride;
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int r = 0; r < rows; r++) {
                            final int idx3 = idx1 + idx2 + r * rowStride;
                            final int idx4 = 2 * r;
                            temp[idx4] = a[idx3];
                            temp[idx4 + 1] = a[idx3 + 1];
                        }
                        fftRows.complexInverse(temp, scale);
                        for (int r = 0; r < rows; r++) {
                            final int idx3 = idx1 + idx2 + r * rowStride;
                            final int idx4 = 2 * r;
                            a[idx3] = temp[idx4];
                            a[idx3 + 1] = temp[idx4 + 1];
                        }
                    }
                }
                temp = new double[2 * slices];
                for (int r = 0; r < rows; r++) {
                    final int idx1 = r * rowStride;
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int s = 0; s < slices; s++) {
                            final int idx3 = s * sliceStride + idx1 + idx2;
                            final int idx4 = 2 * s;
                            temp[idx4] = a[idx3];
                            temp[idx4 + 1] = a[idx3 + 1];
                        }
                        fftSlices.complexInverse(temp, scale);
                        for (int s = 0; s < slices; s++) {
                            final int idx3 = s * sliceStride + idx1 + idx2;
                            final int idx4 = 2 * s;
                            a[idx3] = temp[idx4];
                            a[idx3 + 1] = temp[idx4 + 1];
                        }
                    }
                }
            }
            sliceStride = rows * columns;
            rowStride = columns;
        }
    }

    /**
     * Computes 3D inverse DFT of complex data leaving the result in <code>a</code>. The data is stored in a 3D array.
     * Complex data is represented by 2 double values in sequence: the real and imaginary part, i.e. the input array
     * must be of size slices by rows by 2*columns. The physical layout of the input data is as follows:
     *
     * <pre>
     * a[k1][k2][2*k3] = Re[k1][k2][k3],
     * a[k1][k2][2*k3+1] = Im[k1][k2][k3], 0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;=k3&lt;columns,
     * </pre>
     *
     * @param a data to transform
     * @param scale if true then scaling is performed
     */
    public void complexInverse(final double[][][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            final int oldn3 = columns;
            columns = 2 * columns;
            sliceStride = rows * columns;
            rowStride = columns;
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(0, 1, a, scale);
                cdft3db_subth(1, a, scale);
            } else {
                xdft3da_sub2(0, 1, a, scale);
                cdft3db_sub(1, a, scale);
            }
            columns = oldn3;
            sliceStride = rows * columns;
            rowStride = columns;
        } else {
            if (nthreads > 1 && useThreads && slices >= nthreads && rows >= nthreads && columns >= nthreads) {
                final Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexInverse(a[s][r], scale);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int r = 0; r < rows; r++) {
                                        final int idx4 = 2 * r;
                                        temp[idx4] = a[s][r][idx2];
                                        temp[idx4 + 1] = a[s][r][idx2 + 1];
                                    }
                                    fftRows.complexInverse(temp, scale);
                                    for (int r = 0; r < rows; r++) {
                                        final int idx4 = 2 * r;
                                        a[s][r][idx2] = temp[idx4];
                                        a[s][r][idx2 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = l == nthreads - 1 ? rows : firstRow + p;

                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        @Override
                        public void run() {
                            final double[] temp = new double[2 * slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                for (int c = 0; c < columns; c++) {
                                    final int idx2 = 2 * c;
                                    for (int s = 0; s < slices; s++) {
                                        final int idx4 = 2 * s;
                                        temp[idx4] = a[s][r][idx2];
                                        temp[idx4 + 1] = a[s][r][idx2 + 1];
                                    }
                                    fftSlices.complexInverse(temp, scale);
                                    for (int s = 0; s < slices; s++) {
                                        final int idx4 = 2 * s;
                                        a[s][r][idx2] = temp[idx4];
                                        a[s][r][idx2 + 1] = temp[idx4 + 1];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexInverse(a[s][r], scale);
                    }
                }
                double[] temp = new double[2 * rows];
                for (int s = 0; s < slices; s++) {
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int r = 0; r < rows; r++) {
                            final int idx4 = 2 * r;
                            temp[idx4] = a[s][r][idx2];
                            temp[idx4 + 1] = a[s][r][idx2 + 1];
                        }
                        fftRows.complexInverse(temp, scale);
                        for (int r = 0; r < rows; r++) {
                            final int idx4 = 2 * r;
                            a[s][r][idx2] = temp[idx4];
                            a[s][r][idx2 + 1] = temp[idx4 + 1];
                        }
                    }
                }
                temp = new double[2 * slices];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        final int idx2 = 2 * c;
                        for (int s = 0; s < slices; s++) {
                            final int idx4 = 2 * s;
                            temp[idx4] = a[s][r][idx2];
                            temp[idx4 + 1] = a[s][r][idx2 + 1];
                        }
                        fftSlices.complexInverse(temp, scale);
                        for (int s = 0; s < slices; s++) {
                            final int idx4 = 2 * s;
                            a[s][r][idx2] = temp[idx4];
                            a[s][r][idx2 + 1] = temp[idx4 + 1];
                        }
                    }
                }
            }
        }
    }

    private void fillSymmetric(final double[] a) {
        final int twon3 = 2 * columns;
        final int n2d2 = rows / 2;
        final int n1d2 = slices / 2;

        final int twoSliceStride = rows * twon3;
        final int twoRowStride = twon3;

        int idx1, idx2, idx3, idx4, idx5, idx6;

        for (int s = slices - 1; s >= 1; s--) {
            idx3 = s * sliceStride;
            idx4 = 2 * idx3;
            for (int r = 0; r < rows; r++) {
                idx5 = r * rowStride;
                idx6 = 2 * idx5;
                for (int c = 0; c < columns; c += 2) {
                    idx1 = idx3 + idx5 + c;
                    idx2 = idx4 + idx6 + c;
                    a[idx2] = a[idx1];
                    a[idx1] = 0;
                    idx1++;
                    idx2++;
                    a[idx2] = a[idx1];
                    a[idx1] = 0;
                }
            }
        }

        for (int r = 1; r < rows; r++) {
            idx3 = (rows - r) * rowStride;
            idx4 = (rows - r) * twoRowStride;
            for (int c = 0; c < columns; c += 2) {
                idx1 = idx3 + c;
                idx2 = idx4 + c;
                a[idx2] = a[idx1];
                a[idx1] = 0;
                idx1++;
                idx2++;
                a[idx2] = a[idx1];
                a[idx1] = 0;
            }
        }

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && useThreads && slices >= nthreads) {
            final Future<?>[] futures = new Future[nthreads];
            final int p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx3 = (slices - s) % slices * twoSliceStride;
                            final int idx5 = s * twoSliceStride;
                            for (int r = 0; r < rows; r++) {
                                final int idx4 = (rows - r) % rows * twoRowStride;
                                final int idx6 = r * twoRowStride;
                                for (int c = 1; c < columns; c += 2) {
                                    final int idx1 = idx3 + idx4 + twon3 - c;
                                    final int idx2 = idx5 + idx6 + c;
                                    a[idx1] = -a[idx2 + 2];
                                    a[idx1 - 1] = a[idx2 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            // ---------------------------------------------

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx5 = (slices - s) % slices * twoSliceStride;
                            final int idx6 = s * twoSliceStride;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx4 = idx6 + (rows - r) * twoRowStride;
                                final int idx1 = idx5 + r * twoRowStride + columns;
                                final int idx2 = idx4 + columns;
                                final int idx3 = idx4 + 1;
                                a[idx1] = a[idx3];
                                a[idx2] = a[idx3];
                                a[idx1 + 1] = -a[idx4];
                                a[idx2 + 1] = a[idx4];

                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx3 = (slices - s) % slices * twoSliceStride;
                            final int idx4 = s * twoSliceStride;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx1 = idx3 + (rows - r) * twoRowStride;
                                final int idx2 = idx4 + r * twoRowStride;
                                a[idx1] = a[idx2];
                                a[idx1 + 1] = -a[idx2 + 1];

                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {

            // -----------------------------------------------
            for (int s = 0; s < slices; s++) {
                idx3 = (slices - s) % slices * twoSliceStride;
                idx5 = s * twoSliceStride;
                for (int r = 0; r < rows; r++) {
                    idx4 = (rows - r) % rows * twoRowStride;
                    idx6 = r * twoRowStride;
                    for (int c = 1; c < columns; c += 2) {
                        idx1 = idx3 + idx4 + twon3 - c;
                        idx2 = idx5 + idx6 + c;
                        a[idx1] = -a[idx2 + 2];
                        a[idx1 - 1] = a[idx2 + 1];
                    }
                }
            }

            // ---------------------------------------------

            for (int s = 0; s < slices; s++) {
                idx5 = (slices - s) % slices * twoSliceStride;
                idx6 = s * twoSliceStride;
                for (int r = 1; r < n2d2; r++) {
                    idx4 = idx6 + (rows - r) * twoRowStride;
                    idx1 = idx5 + r * twoRowStride + columns;
                    idx2 = idx4 + columns;
                    idx3 = idx4 + 1;
                    a[idx1] = a[idx3];
                    a[idx2] = a[idx3];
                    a[idx1 + 1] = -a[idx4];
                    a[idx2 + 1] = a[idx4];

                }
            }

            for (int s = 0; s < slices; s++) {
                idx3 = (slices - s) % slices * twoSliceStride;
                idx4 = s * twoSliceStride;
                for (int r = 1; r < n2d2; r++) {
                    idx1 = idx3 + (rows - r) * twoRowStride;
                    idx2 = idx4 + r * twoRowStride;
                    a[idx1] = a[idx2];
                    a[idx1 + 1] = -a[idx2 + 1];

                }
            }
        }

        // ----------------------------------------------------------

        for (int s = 1; s < n1d2; s++) {
            idx1 = s * twoSliceStride;
            idx2 = (slices - s) * twoSliceStride;
            idx3 = n2d2 * twoRowStride;
            idx4 = idx1 + idx3;
            idx5 = idx2 + idx3;
            a[idx1 + columns] = a[idx2 + 1];
            a[idx2 + columns] = a[idx2 + 1];
            a[idx1 + columns + 1] = -a[idx2];
            a[idx2 + columns + 1] = a[idx2];
            a[idx4 + columns] = a[idx5 + 1];
            a[idx5 + columns] = a[idx5 + 1];
            a[idx4 + columns + 1] = -a[idx5];
            a[idx5 + columns + 1] = a[idx5];
            a[idx2] = a[idx1];
            a[idx2 + 1] = -a[idx1 + 1];
            a[idx5] = a[idx4];
            a[idx5 + 1] = -a[idx4 + 1];

        }

        // ----------------------------------------

        a[columns] = a[1];
        a[1] = 0;
        idx1 = n2d2 * twoRowStride;
        idx2 = n1d2 * twoSliceStride;
        idx3 = idx1 + idx2;
        a[idx1 + columns] = a[idx1 + 1];
        a[idx1 + 1] = 0;
        a[idx2 + columns] = a[idx2 + 1];
        a[idx2 + 1] = 0;
        a[idx3 + columns] = a[idx3 + 1];
        a[idx3 + 1] = 0;
        a[idx2 + columns + 1] = 0;
        a[idx3 + columns + 1] = 0;
    }

    private void fillSymmetric(final double[][][] a) {
        final int twon3 = 2 * columns;
        final int n2d2 = rows / 2;
        final int n1d2 = slices / 2;
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && useThreads && slices >= nthreads) {
            final Future<?>[] futures = new Future[nthreads];
            final int p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = (slices - s) % slices;
                            for (int r = 0; r < rows; r++) {
                                final int idx2 = (rows - r) % rows;
                                for (int c = 1; c < columns; c += 2) {
                                    final int idx3 = twon3 - c;
                                    a[idx1][idx2][idx3] = -a[s][r][c + 2];
                                    a[idx1][idx2][idx3 - 1] = a[s][r][c + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            // ---------------------------------------------

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = (slices - s) % slices;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx2 = rows - r;
                                a[idx1][r][columns] = a[s][idx2][1];
                                a[s][idx2][columns] = a[s][idx2][1];
                                a[idx1][r][columns + 1] = -a[s][idx2][0];
                                a[s][idx2][columns + 1] = a[s][idx2][0];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = (slices - s) % slices;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx2 = rows - r;
                                a[idx1][idx2][0] = a[s][r][0];
                                a[idx1][idx2][1] = -a[s][r][1];
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {

            for (int s = 0; s < slices; s++) {
                final int idx1 = (slices - s) % slices;
                for (int r = 0; r < rows; r++) {
                    final int idx2 = (rows - r) % rows;
                    for (int c = 1; c < columns; c += 2) {
                        final int idx3 = twon3 - c;
                        a[idx1][idx2][idx3] = -a[s][r][c + 2];
                        a[idx1][idx2][idx3 - 1] = a[s][r][c + 1];
                    }
                }
            }

            // ---------------------------------------------

            for (int s = 0; s < slices; s++) {
                final int idx1 = (slices - s) % slices;
                for (int r = 1; r < n2d2; r++) {
                    final int idx2 = rows - r;
                    a[idx1][r][columns] = a[s][idx2][1];
                    a[s][idx2][columns] = a[s][idx2][1];
                    a[idx1][r][columns + 1] = -a[s][idx2][0];
                    a[s][idx2][columns + 1] = a[s][idx2][0];
                }
            }

            for (int s = 0; s < slices; s++) {
                final int idx1 = (slices - s) % slices;
                for (int r = 1; r < n2d2; r++) {
                    final int idx2 = rows - r;
                    a[idx1][idx2][0] = a[s][r][0];
                    a[idx1][idx2][1] = -a[s][r][1];
                }
            }
        }

        // ----------------------------------------------------------

        for (int s = 1; s < n1d2; s++) {
            final int idx1 = slices - s;
            a[s][0][columns] = a[idx1][0][1];
            a[idx1][0][columns] = a[idx1][0][1];
            a[s][0][columns + 1] = -a[idx1][0][0];
            a[idx1][0][columns + 1] = a[idx1][0][0];
            a[s][n2d2][columns] = a[idx1][n2d2][1];
            a[idx1][n2d2][columns] = a[idx1][n2d2][1];
            a[s][n2d2][columns + 1] = -a[idx1][n2d2][0];
            a[idx1][n2d2][columns + 1] = a[idx1][n2d2][0];
            a[idx1][0][0] = a[s][0][0];
            a[idx1][0][1] = -a[s][0][1];
            a[idx1][n2d2][0] = a[s][n2d2][0];
            a[idx1][n2d2][1] = -a[s][n2d2][1];

        }
        // ----------------------------------------

        a[0][0][columns] = a[0][0][1];
        a[0][0][1] = 0;
        a[0][n2d2][columns] = a[0][n2d2][1];
        a[0][n2d2][1] = 0;
        a[n1d2][0][columns] = a[n1d2][0][1];
        a[n1d2][0][1] = 0;
        a[n1d2][n2d2][columns] = a[n1d2][n2d2][1];
        a[n1d2][n2d2][1] = 0;
        a[n1d2][0][columns + 1] = 0;
        a[n1d2][n2d2][columns + 1] = 0;
    }

    private void mixedRadixRealForwardFull(final double[] a) {
        final int twon3 = 2 * columns;
        double[] temp = new double[twon3];
        final int ldimn2 = rows / 2 + 1;
        final int n2d2;
        if (rows % 2 == 0) {
            n2d2 = rows / 2;
        } else {
            n2d2 = (rows + 1) / 2;
        }

        final int twoSliceStride = 2 * sliceStride;
        final int twoRowStride = 2 * rowStride;
        final int n1d2 = slices / 2;
        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && useThreads && n1d2 >= nthreads && columns >= nthreads && ldimn2 >= nthreads) {
            final Future<?>[] futures = new Future[nthreads];
            int p = n1d2 / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = slices - 1 - l * p;
                final int lastSlice = l == nthreads - 1 ? n1d2 + 1 : firstSlice - p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[twon3];
                        for (int s = firstSlice; s >= lastSlice; s--) {
                            final int idx1 = s * sliceStride;
                            final int idx2 = s * twoSliceStride;
                            for (int r = rows - 1; r >= 0; r--) {
                                System.arraycopy(a, idx1 + r * rowStride, temp, 0, columns);
                                fftColumns.realForwardFull(temp);
                                System.arraycopy(temp, 0, a, idx2 + r * twoRowStride, twon3);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            final double[][][] temp2 = new double[n1d2 + 1][rows][twon3];

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? n1d2 + 1 : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = s * sliceStride;
                            for (int r = 0; r < rows; r++) {
                                System.arraycopy(a, idx1 + r * rowStride, temp2[s][r], 0, columns);
                                fftColumns.realForwardFull(temp2[s][r]);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? n1d2 + 1 : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = s * twoSliceStride;
                            for (int r = 0; r < rows; r++) {
                                System.arraycopy(temp2[s][r], 0, a, idx1 + r * twoRowStride, twon3);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * rows];

                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = s * twoSliceStride;
                            for (int c = 0; c < columns; c++) {
                                final int idx2 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    final int idx3 = idx1 + r * twoRowStride + idx2;
                                    final int idx4 = 2 * r;
                                    temp[idx4] = a[idx3];
                                    temp[idx4 + 1] = a[idx3 + 1];
                                }
                                fftRows.complexForward(temp);
                                for (int r = 0; r < rows; r++) {
                                    final int idx3 = idx1 + r * twoRowStride + idx2;
                                    final int idx4 = 2 * r;
                                    a[idx3] = temp[idx4];
                                    a[idx3 + 1] = temp[idx4 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = ldimn2 / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = l == nthreads - 1 ? ldimn2 : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * slices];

                        for (int r = firstRow; r < lastRow; r++) {
                            final int idx3 = r * twoRowStride;
                            for (int c = 0; c < columns; c++) {
                                final int idx1 = 2 * c;
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    final int idx4 = s * twoSliceStride + idx3 + idx1;
                                    temp[idx2] = a[idx4];
                                    temp[idx2 + 1] = a[idx4 + 1];
                                }
                                fftSlices.complexForward(temp);
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    final int idx4 = s * twoSliceStride + idx3 + idx1;
                                    a[idx4] = temp[idx2];
                                    a[idx4 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
            p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {

                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx2 = (slices - s) % slices;
                            final int idx5 = idx2 * twoSliceStride;
                            final int idx6 = s * twoSliceStride;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx4 = rows - r;
                                final int idx7 = idx4 * twoRowStride;
                                final int idx8 = r * twoRowStride;
                                final int idx9 = idx5 + idx7;
                                for (int c = 0; c < columns; c++) {
                                    final int idx1 = 2 * c;
                                    final int idx3 = twon3 - idx1;
                                    final int idx10 = idx6 + idx8 + idx1;
                                    a[idx9 + idx3 % twon3] = a[idx10];
                                    a[idx9 + (idx3 + 1) % twon3] = -a[idx10 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {

            for (int s = slices - 1; s >= 0; s--) {
                final int idx1 = s * sliceStride;
                final int idx2 = s * twoSliceStride;
                for (int r = rows - 1; r >= 0; r--) {
                    System.arraycopy(a, idx1 + r * rowStride, temp, 0, columns);
                    fftColumns.realForwardFull(temp);
                    System.arraycopy(temp, 0, a, idx2 + r * twoRowStride, twon3);
                }
            }

            temp = new double[2 * rows];

            for (int s = 0; s < slices; s++) {
                final int idx1 = s * twoSliceStride;
                for (int c = 0; c < columns; c++) {
                    final int idx2 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        final int idx3 = idx1 + r * twoRowStride + idx2;
                        temp[idx4] = a[idx3];
                        temp[idx4 + 1] = a[idx3 + 1];
                    }
                    fftRows.complexForward(temp);
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        final int idx3 = idx1 + r * twoRowStride + idx2;
                        a[idx3] = temp[idx4];
                        a[idx3 + 1] = temp[idx4 + 1];
                    }
                }
            }

            temp = new double[2 * slices];

            for (int r = 0; r < ldimn2; r++) {
                final int idx3 = r * twoRowStride;
                for (int c = 0; c < columns; c++) {
                    final int idx1 = 2 * c;
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        final int idx4 = s * twoSliceStride + idx3 + idx1;
                        temp[idx2] = a[idx4];
                        temp[idx2 + 1] = a[idx4 + 1];
                    }
                    fftSlices.complexForward(temp);
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        final int idx4 = s * twoSliceStride + idx3 + idx1;
                        a[idx4] = temp[idx2];
                        a[idx4 + 1] = temp[idx2 + 1];
                    }
                }
            }

            for (int s = 0; s < slices; s++) {
                final int idx2 = (slices - s) % slices;
                final int idx5 = idx2 * twoSliceStride;
                final int idx6 = s * twoSliceStride;
                for (int r = 1; r < n2d2; r++) {
                    final int idx4 = rows - r;
                    final int idx7 = idx4 * twoRowStride;
                    final int idx8 = r * twoRowStride;
                    final int idx9 = idx5 + idx7;
                    for (int c = 0; c < columns; c++) {
                        final int idx1 = 2 * c;
                        final int idx3 = twon3 - idx1;
                        final int idx10 = idx6 + idx8 + idx1;
                        a[idx9 + idx3 % twon3] = a[idx10];
                        a[idx9 + (idx3 + 1) % twon3] = -a[idx10 + 1];
                    }
                }
            }

        }
    }

    private void mixedRadixRealForwardFull(final double[][][] a) {
        double[] temp = new double[2 * rows];
        final int ldimn2 = rows / 2 + 1;
        final int newn3 = 2 * columns;
        final int n2d2;
        if (rows % 2 == 0) {
            n2d2 = rows / 2;
        } else {
            n2d2 = (rows + 1) / 2;
        }

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && useThreads && slices >= nthreads && columns >= nthreads && ldimn2 >= nthreads) {
            final Future<?>[] futures = new Future[nthreads];
            int p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            for (int r = 0; r < rows; r++) {
                                fftColumns.realForwardFull(a[s][r]);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * rows];

                        for (int s = firstSlice; s < lastSlice; s++) {
                            for (int c = 0; c < columns; c++) {
                                final int idx2 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    final int idx4 = 2 * r;
                                    temp[idx4] = a[s][r][idx2];
                                    temp[idx4 + 1] = a[s][r][idx2 + 1];
                                }
                                fftRows.complexForward(temp);
                                for (int r = 0; r < rows; r++) {
                                    final int idx4 = 2 * r;
                                    a[s][r][idx2] = temp[idx4];
                                    a[s][r][idx2 + 1] = temp[idx4 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = ldimn2 / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = l == nthreads - 1 ? ldimn2 : firstRow + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * slices];

                        for (int r = firstRow; r < lastRow; r++) {
                            for (int c = 0; c < columns; c++) {
                                final int idx1 = 2 * c;
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    temp[idx2] = a[s][r][idx1];
                                    temp[idx2 + 1] = a[s][r][idx1 + 1];
                                }
                                fftSlices.complexForward(temp);
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    a[s][r][idx1] = temp[idx2];
                                    a[s][r][idx1 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
            p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {

                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx2 = (slices - s) % slices;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx4 = rows - r;
                                for (int c = 0; c < columns; c++) {
                                    final int idx1 = 2 * c;
                                    final int idx3 = newn3 - idx1;
                                    a[idx2][idx4][idx3 % newn3] = a[s][r][idx1];
                                    a[idx2][idx4][(idx3 + 1) % newn3] = -a[s][r][idx1 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {

            for (int s = 0; s < slices; s++) {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realForwardFull(a[s][r]);
                }
            }

            for (int s = 0; s < slices; s++) {
                for (int c = 0; c < columns; c++) {
                    final int idx2 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        temp[idx4] = a[s][r][idx2];
                        temp[idx4 + 1] = a[s][r][idx2 + 1];
                    }
                    fftRows.complexForward(temp);
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        a[s][r][idx2] = temp[idx4];
                        a[s][r][idx2 + 1] = temp[idx4 + 1];
                    }
                }
            }

            temp = new double[2 * slices];

            for (int r = 0; r < ldimn2; r++) {
                for (int c = 0; c < columns; c++) {
                    final int idx1 = 2 * c;
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        temp[idx2] = a[s][r][idx1];
                        temp[idx2 + 1] = a[s][r][idx1 + 1];
                    }
                    fftSlices.complexForward(temp);
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        a[s][r][idx1] = temp[idx2];
                        a[s][r][idx1 + 1] = temp[idx2 + 1];
                    }
                }
            }

            for (int s = 0; s < slices; s++) {
                final int idx2 = (slices - s) % slices;
                for (int r = 1; r < n2d2; r++) {
                    final int idx4 = rows - r;
                    for (int c = 0; c < columns; c++) {
                        final int idx1 = 2 * c;
                        final int idx3 = newn3 - idx1;
                        a[idx2][idx4][idx3 % newn3] = a[s][r][idx1];
                        a[idx2][idx4][(idx3 + 1) % newn3] = -a[s][r][idx1 + 1];
                    }
                }
            }

        }
    }

    /* -------- child routines -------- */

    private void mixedRadixRealInverseFull(final double[] a, final boolean scale) {
        final int twon3 = 2 * columns;
        double[] temp = new double[twon3];
        final int ldimn2 = rows / 2 + 1;
        final int n2d2;
        if (rows % 2 == 0) {
            n2d2 = rows / 2;
        } else {
            n2d2 = (rows + 1) / 2;
        }

        final int twoSliceStride = 2 * sliceStride;
        final int twoRowStride = 2 * rowStride;
        final int n1d2 = slices / 2;

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && useThreads && n1d2 >= nthreads && columns >= nthreads && ldimn2 >= nthreads) {
            final Future<?>[] futures = new Future[nthreads];
            int p = n1d2 / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = slices - 1 - l * p;
                final int lastSlice = l == nthreads - 1 ? n1d2 + 1 : firstSlice - p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[twon3];
                        for (int s = firstSlice; s >= lastSlice; s--) {
                            final int idx1 = s * sliceStride;
                            final int idx2 = s * twoSliceStride;
                            for (int r = rows - 1; r >= 0; r--) {
                                System.arraycopy(a, idx1 + r * rowStride, temp, 0, columns);
                                fftColumns.realInverseFull(temp, scale);
                                System.arraycopy(temp, 0, a, idx2 + r * twoRowStride, twon3);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            final double[][][] temp2 = new double[n1d2 + 1][rows][twon3];

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? n1d2 + 1 : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = s * sliceStride;
                            for (int r = 0; r < rows; r++) {
                                System.arraycopy(a, idx1 + r * rowStride, temp2[s][r], 0, columns);
                                fftColumns.realInverseFull(temp2[s][r], scale);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? n1d2 + 1 : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = s * twoSliceStride;
                            for (int r = 0; r < rows; r++) {
                                System.arraycopy(temp2[s][r], 0, a, idx1 + r * twoRowStride, twon3);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * rows];

                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx1 = s * twoSliceStride;
                            for (int c = 0; c < columns; c++) {
                                final int idx2 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    final int idx3 = idx1 + r * twoRowStride + idx2;
                                    final int idx4 = 2 * r;
                                    temp[idx4] = a[idx3];
                                    temp[idx4 + 1] = a[idx3 + 1];
                                }
                                fftRows.complexInverse(temp, scale);
                                for (int r = 0; r < rows; r++) {
                                    final int idx3 = idx1 + r * twoRowStride + idx2;
                                    final int idx4 = 2 * r;
                                    a[idx3] = temp[idx4];
                                    a[idx3 + 1] = temp[idx4 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = ldimn2 / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = l == nthreads - 1 ? ldimn2 : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * slices];

                        for (int r = firstRow; r < lastRow; r++) {
                            final int idx3 = r * twoRowStride;
                            for (int c = 0; c < columns; c++) {
                                final int idx1 = 2 * c;
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    final int idx4 = s * twoSliceStride + idx3 + idx1;
                                    temp[idx2] = a[idx4];
                                    temp[idx2 + 1] = a[idx4 + 1];
                                }
                                fftSlices.complexInverse(temp, scale);
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    final int idx4 = s * twoSliceStride + idx3 + idx1;
                                    a[idx4] = temp[idx2];
                                    a[idx4 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {

                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx2 = (slices - s) % slices;
                            final int idx5 = idx2 * twoSliceStride;
                            final int idx6 = s * twoSliceStride;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx4 = rows - r;
                                final int idx7 = idx4 * twoRowStride;
                                final int idx8 = r * twoRowStride;
                                final int idx9 = idx5 + idx7;
                                for (int c = 0; c < columns; c++) {
                                    final int idx1 = 2 * c;
                                    final int idx3 = twon3 - idx1;
                                    final int idx10 = idx6 + idx8 + idx1;
                                    a[idx9 + idx3 % twon3] = a[idx10];
                                    a[idx9 + (idx3 + 1) % twon3] = -a[idx10 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {

            for (int s = slices - 1; s >= 0; s--) {
                final int idx1 = s * sliceStride;
                final int idx2 = s * twoSliceStride;
                for (int r = rows - 1; r >= 0; r--) {
                    System.arraycopy(a, idx1 + r * rowStride, temp, 0, columns);
                    fftColumns.realInverseFull(temp, scale);
                    System.arraycopy(temp, 0, a, idx2 + r * twoRowStride, twon3);
                }
            }

            temp = new double[2 * rows];

            for (int s = 0; s < slices; s++) {
                final int idx1 = s * twoSliceStride;
                for (int c = 0; c < columns; c++) {
                    final int idx2 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        final int idx3 = idx1 + r * twoRowStride + idx2;
                        temp[idx4] = a[idx3];
                        temp[idx4 + 1] = a[idx3 + 1];
                    }
                    fftRows.complexInverse(temp, scale);
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        final int idx3 = idx1 + r * twoRowStride + idx2;
                        a[idx3] = temp[idx4];
                        a[idx3 + 1] = temp[idx4 + 1];
                    }
                }
            }

            temp = new double[2 * slices];

            for (int r = 0; r < ldimn2; r++) {
                final int idx3 = r * twoRowStride;
                for (int c = 0; c < columns; c++) {
                    final int idx1 = 2 * c;
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        final int idx4 = s * twoSliceStride + idx3 + idx1;
                        temp[idx2] = a[idx4];
                        temp[idx2 + 1] = a[idx4 + 1];
                    }
                    fftSlices.complexInverse(temp, scale);
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        final int idx4 = s * twoSliceStride + idx3 + idx1;
                        a[idx4] = temp[idx2];
                        a[idx4 + 1] = temp[idx2 + 1];
                    }
                }
            }

            for (int s = 0; s < slices; s++) {
                final int idx2 = (slices - s) % slices;
                final int idx5 = idx2 * twoSliceStride;
                final int idx6 = s * twoSliceStride;
                for (int r = 1; r < n2d2; r++) {
                    final int idx4 = rows - r;
                    final int idx7 = idx4 * twoRowStride;
                    final int idx8 = r * twoRowStride;
                    final int idx9 = idx5 + idx7;
                    for (int c = 0; c < columns; c++) {
                        final int idx1 = 2 * c;
                        final int idx3 = twon3 - idx1;
                        final int idx10 = idx6 + idx8 + idx1;
                        a[idx9 + idx3 % twon3] = a[idx10];
                        a[idx9 + (idx3 + 1) % twon3] = -a[idx10 + 1];
                    }
                }
            }

        }
    }

    private void mixedRadixRealInverseFull(final double[][][] a, final boolean scale) {
        double[] temp = new double[2 * rows];
        final int ldimn2 = rows / 2 + 1;
        final int newn3 = 2 * columns;
        final int n2d2;
        if (rows % 2 == 0) {
            n2d2 = rows / 2;
        } else {
            n2d2 = (rows + 1) / 2;
        }

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && useThreads && slices >= nthreads && columns >= nthreads && ldimn2 >= nthreads) {
            final Future<?>[] futures = new Future[nthreads];
            int p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int s = firstSlice; s < lastSlice; s++) {
                            for (int r = 0; r < rows; r++) {
                                fftColumns.realInverseFull(a[s][r], scale);
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * rows];

                        for (int s = firstSlice; s < lastSlice; s++) {
                            for (int c = 0; c < columns; c++) {
                                final int idx2 = 2 * c;
                                for (int r = 0; r < rows; r++) {
                                    final int idx4 = 2 * r;
                                    temp[idx4] = a[s][r][idx2];
                                    temp[idx4 + 1] = a[s][r][idx2 + 1];
                                }
                                fftRows.complexInverse(temp, scale);
                                for (int r = 0; r < rows; r++) {
                                    final int idx4 = 2 * r;
                                    a[s][r][idx2] = temp[idx4];
                                    a[s][r][idx2 + 1] = temp[idx4 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

            p = ldimn2 / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstRow = l * p;
                final int lastRow = l == nthreads - 1 ? ldimn2 : firstRow + p;
                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        final double[] temp = new double[2 * slices];

                        for (int r = firstRow; r < lastRow; r++) {
                            for (int c = 0; c < columns; c++) {
                                final int idx1 = 2 * c;
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    temp[idx2] = a[s][r][idx1];
                                    temp[idx2 + 1] = a[s][r][idx1 + 1];
                                }
                                fftSlices.complexInverse(temp, scale);
                                for (int s = 0; s < slices; s++) {
                                    final int idx2 = 2 * s;
                                    a[s][r][idx1] = temp[idx2];
                                    a[s][r][idx1 + 1] = temp[idx2 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
            p = slices / nthreads;
            for (int l = 0; l < nthreads; l++) {
                final int firstSlice = l * p;
                final int lastSlice = l == nthreads - 1 ? slices : firstSlice + p;

                futures[l] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {

                        for (int s = firstSlice; s < lastSlice; s++) {
                            final int idx2 = (slices - s) % slices;
                            for (int r = 1; r < n2d2; r++) {
                                final int idx4 = rows - r;
                                for (int c = 0; c < columns; c++) {
                                    final int idx1 = 2 * c;
                                    final int idx3 = newn3 - idx1;
                                    a[idx2][idx4][idx3 % newn3] = a[s][r][idx1];
                                    a[idx2][idx4][(idx3 + 1) % newn3] = -a[s][r][idx1 + 1];
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {

            for (int s = 0; s < slices; s++) {
                for (int r = 0; r < rows; r++) {
                    fftColumns.realInverseFull(a[s][r], scale);
                }
            }

            for (int s = 0; s < slices; s++) {
                for (int c = 0; c < columns; c++) {
                    final int idx2 = 2 * c;
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        temp[idx4] = a[s][r][idx2];
                        temp[idx4 + 1] = a[s][r][idx2 + 1];
                    }
                    fftRows.complexInverse(temp, scale);
                    for (int r = 0; r < rows; r++) {
                        final int idx4 = 2 * r;
                        a[s][r][idx2] = temp[idx4];
                        a[s][r][idx2 + 1] = temp[idx4 + 1];
                    }
                }
            }

            temp = new double[2 * slices];

            for (int r = 0; r < ldimn2; r++) {
                for (int c = 0; c < columns; c++) {
                    final int idx1 = 2 * c;
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        temp[idx2] = a[s][r][idx1];
                        temp[idx2 + 1] = a[s][r][idx1 + 1];
                    }
                    fftSlices.complexInverse(temp, scale);
                    for (int s = 0; s < slices; s++) {
                        final int idx2 = 2 * s;
                        a[s][r][idx1] = temp[idx2];
                        a[s][r][idx1 + 1] = temp[idx2 + 1];
                    }
                }
            }

            for (int s = 0; s < slices; s++) {
                final int idx2 = (slices - s) % slices;
                for (int r = 1; r < n2d2; r++) {
                    final int idx4 = rows - r;
                    for (int c = 0; c < columns; c++) {
                        final int idx1 = 2 * c;
                        final int idx3 = newn3 - idx1;
                        a[idx2][idx4][idx3 % newn3] = a[s][r][idx1];
                        a[idx2][idx4][(idx3 + 1) % newn3] = -a[s][r][idx1 + 1];
                    }
                }
            }

        }
    }

    private void rdft3d_sub(final int isgn, final double[] a) {
        int n1h, n2h, i, j, k, l, idx1, idx2, idx3, idx4;
        double xi;

        n1h = slices >> 1;
        n2h = rows >> 1;
        if (isgn < 0) {
            for (i = 1; i < n1h; i++) {
                j = slices - i;
                idx1 = i * sliceStride;
                idx2 = j * sliceStride;
                idx3 = i * sliceStride + n2h * rowStride;
                idx4 = j * sliceStride + n2h * rowStride;
                xi = a[idx1] - a[idx2];
                a[idx1] += a[idx2];
                a[idx2] = xi;
                xi = a[idx2 + 1] - a[idx1 + 1];
                a[idx1 + 1] += a[idx2 + 1];
                a[idx2 + 1] = xi;
                xi = a[idx3] - a[idx4];
                a[idx3] += a[idx4];
                a[idx4] = xi;
                xi = a[idx4 + 1] - a[idx3 + 1];
                a[idx3 + 1] += a[idx4 + 1];
                a[idx4 + 1] = xi;
                for (k = 1; k < n2h; k++) {
                    l = rows - k;
                    idx1 = i * sliceStride + k * rowStride;
                    idx2 = j * sliceStride + l * rowStride;
                    xi = a[idx1] - a[idx2];
                    a[idx1] += a[idx2];
                    a[idx2] = xi;
                    xi = a[idx2 + 1] - a[idx1 + 1];
                    a[idx1 + 1] += a[idx2 + 1];
                    a[idx2 + 1] = xi;
                    idx3 = j * sliceStride + k * rowStride;
                    idx4 = i * sliceStride + l * rowStride;
                    xi = a[idx3] - a[idx4];
                    a[idx3] += a[idx4];
                    a[idx4] = xi;
                    xi = a[idx4 + 1] - a[idx3 + 1];
                    a[idx3 + 1] += a[idx4 + 1];
                    a[idx4 + 1] = xi;
                }
            }
            for (k = 1; k < n2h; k++) {
                l = rows - k;
                idx1 = k * rowStride;
                idx2 = l * rowStride;
                xi = a[idx1] - a[idx2];
                a[idx1] += a[idx2];
                a[idx2] = xi;
                xi = a[idx2 + 1] - a[idx1 + 1];
                a[idx1 + 1] += a[idx2 + 1];
                a[idx2 + 1] = xi;
                idx3 = n1h * sliceStride + k * rowStride;
                idx4 = n1h * sliceStride + l * rowStride;
                xi = a[idx3] - a[idx4];
                a[idx3] += a[idx4];
                a[idx4] = xi;
                xi = a[idx4 + 1] - a[idx3 + 1];
                a[idx3 + 1] += a[idx4 + 1];
                a[idx4 + 1] = xi;
            }
        } else {
            for (i = 1; i < n1h; i++) {
                j = slices - i;
                idx1 = j * sliceStride;
                idx2 = i * sliceStride;
                a[idx1] = 0.5f * (a[idx2] - a[idx1]);
                a[idx2] -= a[idx1];
                a[idx1 + 1] = 0.5f * (a[idx2 + 1] + a[idx1 + 1]);
                a[idx2 + 1] -= a[idx1 + 1];
                idx3 = j * sliceStride + n2h * rowStride;
                idx4 = i * sliceStride + n2h * rowStride;
                a[idx3] = 0.5f * (a[idx4] - a[idx3]);
                a[idx4] -= a[idx3];
                a[idx3 + 1] = 0.5f * (a[idx4 + 1] + a[idx3 + 1]);
                a[idx4 + 1] -= a[idx3 + 1];
                for (k = 1; k < n2h; k++) {
                    l = rows - k;
                    idx1 = j * sliceStride + l * rowStride;
                    idx2 = i * sliceStride + k * rowStride;
                    a[idx1] = 0.5f * (a[idx2] - a[idx1]);
                    a[idx2] -= a[idx1];
                    a[idx1 + 1] = 0.5f * (a[idx2 + 1] + a[idx1 + 1]);
                    a[idx2 + 1] -= a[idx1 + 1];
                    idx3 = i * sliceStride + l * rowStride;
                    idx4 = j * sliceStride + k * rowStride;
                    a[idx3] = 0.5f * (a[idx4] - a[idx3]);
                    a[idx4] -= a[idx3];
                    a[idx3 + 1] = 0.5f * (a[idx4 + 1] + a[idx3 + 1]);
                    a[idx4 + 1] -= a[idx3 + 1];
                }
            }
            for (k = 1; k < n2h; k++) {
                l = rows - k;
                idx1 = l * rowStride;
                idx2 = k * rowStride;
                a[idx1] = 0.5f * (a[idx2] - a[idx1]);
                a[idx2] -= a[idx1];
                a[idx1 + 1] = 0.5f * (a[idx2 + 1] + a[idx1 + 1]);
                a[idx2 + 1] -= a[idx1 + 1];
                idx3 = n1h * sliceStride + l * rowStride;
                idx4 = n1h * sliceStride + k * rowStride;
                a[idx3] = 0.5f * (a[idx4] - a[idx3]);
                a[idx4] -= a[idx3];
                a[idx3 + 1] = 0.5f * (a[idx4 + 1] + a[idx3 + 1]);
                a[idx4 + 1] -= a[idx3 + 1];
            }
        }
    }

    private void rdft3d_sub(final int isgn, final double[][][] a) {
        int n1h, n2h, i, j, k, l;
        double xi;

        n1h = slices >> 1;
        n2h = rows >> 1;
        if (isgn < 0) {
            for (i = 1; i < n1h; i++) {
                j = slices - i;
                xi = a[i][0][0] - a[j][0][0];
                a[i][0][0] += a[j][0][0];
                a[j][0][0] = xi;
                xi = a[j][0][1] - a[i][0][1];
                a[i][0][1] += a[j][0][1];
                a[j][0][1] = xi;
                xi = a[i][n2h][0] - a[j][n2h][0];
                a[i][n2h][0] += a[j][n2h][0];
                a[j][n2h][0] = xi;
                xi = a[j][n2h][1] - a[i][n2h][1];
                a[i][n2h][1] += a[j][n2h][1];
                a[j][n2h][1] = xi;
                for (k = 1; k < n2h; k++) {
                    l = rows - k;
                    xi = a[i][k][0] - a[j][l][0];
                    a[i][k][0] += a[j][l][0];
                    a[j][l][0] = xi;
                    xi = a[j][l][1] - a[i][k][1];
                    a[i][k][1] += a[j][l][1];
                    a[j][l][1] = xi;
                    xi = a[j][k][0] - a[i][l][0];
                    a[j][k][0] += a[i][l][0];
                    a[i][l][0] = xi;
                    xi = a[i][l][1] - a[j][k][1];
                    a[j][k][1] += a[i][l][1];
                    a[i][l][1] = xi;
                }
            }
            for (k = 1; k < n2h; k++) {
                l = rows - k;
                xi = a[0][k][0] - a[0][l][0];
                a[0][k][0] += a[0][l][0];
                a[0][l][0] = xi;
                xi = a[0][l][1] - a[0][k][1];
                a[0][k][1] += a[0][l][1];
                a[0][l][1] = xi;
                xi = a[n1h][k][0] - a[n1h][l][0];
                a[n1h][k][0] += a[n1h][l][0];
                a[n1h][l][0] = xi;
                xi = a[n1h][l][1] - a[n1h][k][1];
                a[n1h][k][1] += a[n1h][l][1];
                a[n1h][l][1] = xi;
            }
        } else {
            for (i = 1; i < n1h; i++) {
                j = slices - i;
                a[j][0][0] = 0.5f * (a[i][0][0] - a[j][0][0]);
                a[i][0][0] -= a[j][0][0];
                a[j][0][1] = 0.5f * (a[i][0][1] + a[j][0][1]);
                a[i][0][1] -= a[j][0][1];
                a[j][n2h][0] = 0.5f * (a[i][n2h][0] - a[j][n2h][0]);
                a[i][n2h][0] -= a[j][n2h][0];
                a[j][n2h][1] = 0.5f * (a[i][n2h][1] + a[j][n2h][1]);
                a[i][n2h][1] -= a[j][n2h][1];
                for (k = 1; k < n2h; k++) {
                    l = rows - k;
                    a[j][l][0] = 0.5f * (a[i][k][0] - a[j][l][0]);
                    a[i][k][0] -= a[j][l][0];
                    a[j][l][1] = 0.5f * (a[i][k][1] + a[j][l][1]);
                    a[i][k][1] -= a[j][l][1];
                    a[i][l][0] = 0.5f * (a[j][k][0] - a[i][l][0]);
                    a[j][k][0] -= a[i][l][0];
                    a[i][l][1] = 0.5f * (a[j][k][1] + a[i][l][1]);
                    a[j][k][1] -= a[i][l][1];
                }
            }
            for (k = 1; k < n2h; k++) {
                l = rows - k;
                a[0][l][0] = 0.5f * (a[0][k][0] - a[0][l][0]);
                a[0][k][0] -= a[0][l][0];
                a[0][l][1] = 0.5f * (a[0][k][1] + a[0][l][1]);
                a[0][k][1] -= a[0][l][1];
                a[n1h][l][0] = 0.5f * (a[n1h][k][0] - a[n1h][l][0]);
                a[n1h][k][0] -= a[n1h][l][0];
                a[n1h][l][1] = 0.5f * (a[n1h][k][1] + a[n1h][l][1]);
                a[n1h][k][1] -= a[n1h][l][1];
            }
        }
    }

    /**
     * Computes 3D forward DFT of real data leaving the result in <code>a</code> . This method only works when the sizes
     * of all three dimensions are power-of-two numbers. The data is stored in a 1D array addressed in slice-major, then
     * row-major, then column-major, in order of significance, i.e. element (i,j,k) of 3-d array
     * x[slices][rows][2*columns] is stored in a[i*sliceStride + j*rowStride + k], where sliceStride = rows * 2 *
     * columns and rowStride = 2 * columns. The physical layout of the output data is as follows:
     *
     * <pre>
     * a[k1*sliceStride + k2*rowStride + 2*k3] = Re[k1][k2][k3]
     *                 = Re[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     * a[k1*sliceStride + k2*rowStride + 2*k3+1] = Im[k1][k2][k3]
     *                   = -Im[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     *     0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;k3&lt;columns/2,
     * a[k1*sliceStride + k2*rowStride] = Re[k1][k2][0]
     *              = Re[(slices-k1)%slices][rows-k2][0],
     * a[k1*sliceStride + k2*rowStride + 1] = Im[k1][k2][0]
     *              = -Im[(slices-k1)%slices][rows-k2][0],
     * a[k1*sliceStride + (rows-k2)*rowStride + 1] = Re[(slices-k1)%slices][k2][columns/2]
     *                 = Re[k1][rows-k2][columns/2],
     * a[k1*sliceStride + (rows-k2)*rowStride] = -Im[(slices-k1)%slices][k2][columns/2]
     *                 = Im[k1][rows-k2][columns/2],
     *     0&lt;=k1&lt;slices, 0&lt;k2&lt;rows/2,
     * a[k1*sliceStride] = Re[k1][0][0]
     *             = Re[slices-k1][0][0],
     * a[k1*sliceStride + 1] = Im[k1][0][0]
     *             = -Im[slices-k1][0][0],
     * a[k1*sliceStride + (rows/2)*rowStride] = Re[k1][rows/2][0]
     *                = Re[slices-k1][rows/2][0],
     * a[k1*sliceStride + (rows/2)*rowStride + 1] = Im[k1][rows/2][0]
     *                = -Im[slices-k1][rows/2][0],
     * a[(slices-k1)*sliceStride + 1] = Re[k1][0][columns/2]
     *                = Re[slices-k1][0][columns/2],
     * a[(slices-k1)*sliceStride] = -Im[k1][0][columns/2]
     *                = Im[slices-k1][0][columns/2],
     * a[(slices-k1)*sliceStride + (rows/2)*rowStride + 1] = Re[k1][rows/2][columns/2]
     *                   = Re[slices-k1][rows/2][columns/2],
     * a[(slices-k1)*sliceStride + (rows/2) * rowStride] = -Im[k1][rows/2][columns/2]
     *                   = Im[slices-k1][rows/2][columns/2],
     *     0&lt;k1&lt;slices/2,
     * a[0] = Re[0][0][0],
     * a[1] = Re[0][0][columns/2],
     * a[(rows/2)*rowStride] = Re[0][rows/2][0],
     * a[(rows/2)*rowStride + 1] = Re[0][rows/2][columns/2],
     * a[(slices/2)*sliceStride] = Re[slices/2][0][0],
     * a[(slices/2)*sliceStride + 1] = Re[slices/2][0][columns/2],
     * a[(slices/2)*sliceStride + (rows/2)*rowStride] = Re[slices/2][rows/2][0],
     * a[(slices/2)*sliceStride + (rows/2)*rowStride + 1] = Re[slices/2][rows/2][columns/2]
     * </pre>
     *
     * This method computes only half of the elements of the real transform. The other half satisfies the symmetry
     * condition. If you want the full real forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     *
     * @param a data to transform
     */
    public void realForward(final double[] a) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("slices, rows and columns must be power of two numbers");
        } else {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth1(1, -1, a, true);
                cdft3db_subth(-1, a, true);
                rdft3d_sub(1, a);
            } else {
                xdft3da_sub1(1, -1, a, true);
                cdft3db_sub(-1, a, true);
                rdft3d_sub(1, a);
            }
        }
    }

    /**
     * Computes 3D forward DFT of real data leaving the result in <code>a</code> . This method only works when the sizes
     * of all three dimensions are power-of-two numbers. The data is stored in a 3D array. The physical layout of the
     * output data is as follows:
     *
     * <pre>
     * a[k1][k2][2*k3] = Re[k1][k2][k3]
     *                 = Re[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     * a[k1][k2][2*k3+1] = Im[k1][k2][k3]
     *                   = -Im[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     *     0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;k3&lt;columns/2,
     * a[k1][k2][0] = Re[k1][k2][0]
     *              = Re[(slices-k1)%slices][rows-k2][0],
     * a[k1][k2][1] = Im[k1][k2][0]
     *              = -Im[(slices-k1)%slices][rows-k2][0],
     * a[k1][rows-k2][1] = Re[(slices-k1)%slices][k2][columns/2]
     *                 = Re[k1][rows-k2][columns/2],
     * a[k1][rows-k2][0] = -Im[(slices-k1)%slices][k2][columns/2]
     *                 = Im[k1][rows-k2][columns/2],
     *     0&lt;=k1&lt;slices, 0&lt;k2&lt;rows/2,
     * a[k1][0][0] = Re[k1][0][0]
     *             = Re[slices-k1][0][0],
     * a[k1][0][1] = Im[k1][0][0]
     *             = -Im[slices-k1][0][0],
     * a[k1][rows/2][0] = Re[k1][rows/2][0]
     *                = Re[slices-k1][rows/2][0],
     * a[k1][rows/2][1] = Im[k1][rows/2][0]
     *                = -Im[slices-k1][rows/2][0],
     * a[slices-k1][0][1] = Re[k1][0][columns/2]
     *                = Re[slices-k1][0][columns/2],
     * a[slices-k1][0][0] = -Im[k1][0][columns/2]
     *                = Im[slices-k1][0][columns/2],
     * a[slices-k1][rows/2][1] = Re[k1][rows/2][columns/2]
     *                   = Re[slices-k1][rows/2][columns/2],
     * a[slices-k1][rows/2][0] = -Im[k1][rows/2][columns/2]
     *                   = Im[slices-k1][rows/2][columns/2],
     *     0&lt;k1&lt;slices/2,
     * a[0][0][0] = Re[0][0][0],
     * a[0][0][1] = Re[0][0][columns/2],
     * a[0][rows/2][0] = Re[0][rows/2][0],
     * a[0][rows/2][1] = Re[0][rows/2][columns/2],
     * a[slices/2][0][0] = Re[slices/2][0][0],
     * a[slices/2][0][1] = Re[slices/2][0][columns/2],
     * a[slices/2][rows/2][0] = Re[slices/2][rows/2][0],
     * a[slices/2][rows/2][1] = Re[slices/2][rows/2][columns/2]
     * </pre>
     *
     * This method computes only half of the elements of the real transform. The other half satisfies the symmetry
     * condition. If you want the full real forward transform, use <code>realForwardFull</code>. To get back the
     * original data, use <code>realInverse</code> on the output of this method.
     *
     * @param a data to transform
     */
    public void realForward(final double[][][] a) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("slices, rows and columns must be power of two numbers");
        } else {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth1(1, -1, a, true);
                cdft3db_subth(-1, a, true);
                rdft3d_sub(1, a);
            } else {
                xdft3da_sub1(1, -1, a, true);
                cdft3db_sub(-1, a, true);
                rdft3d_sub(1, a);
            }
        }
    }

    /**
     * Computes 3D forward DFT of real data leaving the result in <code>a</code> . This method computes full real
     * forward transform, i.e. you will get the same result as from <code>complexForward</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>, the input array must be of size
     * slices*rows*2*columns, with only the first slices*rows*columns elements filled with real data. To get back the
     * original data, use <code>complexInverse</code> on the output of this method.
     *
     * @param a data to transform
     */
    public void realForwardFull(final double[] a) {
        if (isPowerOfTwo) {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(1, -1, a, true);
                cdft3db_subth(-1, a, true);
                rdft3d_sub(1, a);
            } else {
                xdft3da_sub2(1, -1, a, true);
                cdft3db_sub(-1, a, true);
                rdft3d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealForwardFull(a);
        }
    }

    /**
     * Computes 3D forward DFT of real data leaving the result in <code>a</code> . This method computes full real
     * forward transform, i.e. you will get the same result as from <code>complexForward</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>, the input array must be of size slices by
     * rows by 2*columns, with only the first slices by rows by columns elements filled with real data. To get back the
     * original data, use <code>complexInverse</code> on the output of this method.
     *
     * @param a data to transform
     */
    public void realForwardFull(final double[][][] a) {
        if (isPowerOfTwo) {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(1, -1, a, true);
                cdft3db_subth(-1, a, true);
                rdft3d_sub(1, a);
            } else {
                xdft3da_sub2(1, -1, a, true);
                cdft3db_sub(-1, a, true);
                rdft3d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealForwardFull(a);
        }
    }

    /**
     * Computes 3D inverse DFT of real data leaving the result in <code>a</code> . This method only works when the sizes
     * of all three dimensions are power-of-two numbers. The data is stored in a 1D array addressed in slice-major, then
     * row-major, then column-major, in order of significance, i.e. element (i,j,k) of 3-d array
     * x[slices][rows][2*columns] is stored in a[i*sliceStride + j*rowStride + k], where sliceStride = rows * 2 *
     * columns and rowStride = 2 * columns. The physical layout of the input data has to be as follows:
     *
     * <pre>
     * a[k1*sliceStride + k2*rowStride + 2*k3] = Re[k1][k2][k3]
     *                 = Re[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     * a[k1*sliceStride + k2*rowStride + 2*k3+1] = Im[k1][k2][k3]
     *                   = -Im[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     *     0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;k3&lt;columns/2,
     * a[k1*sliceStride + k2*rowStride] = Re[k1][k2][0]
     *              = Re[(slices-k1)%slices][rows-k2][0],
     * a[k1*sliceStride + k2*rowStride + 1] = Im[k1][k2][0]
     *              = -Im[(slices-k1)%slices][rows-k2][0],
     * a[k1*sliceStride + (rows-k2)*rowStride + 1] = Re[(slices-k1)%slices][k2][columns/2]
     *                 = Re[k1][rows-k2][columns/2],
     * a[k1*sliceStride + (rows-k2)*rowStride] = -Im[(slices-k1)%slices][k2][columns/2]
     *                 = Im[k1][rows-k2][columns/2],
     *     0&lt;=k1&lt;slices, 0&lt;k2&lt;rows/2,
     * a[k1*sliceStride] = Re[k1][0][0]
     *             = Re[slices-k1][0][0],
     * a[k1*sliceStride + 1] = Im[k1][0][0]
     *             = -Im[slices-k1][0][0],
     * a[k1*sliceStride + (rows/2)*rowStride] = Re[k1][rows/2][0]
     *                = Re[slices-k1][rows/2][0],
     * a[k1*sliceStride + (rows/2)*rowStride + 1] = Im[k1][rows/2][0]
     *                = -Im[slices-k1][rows/2][0],
     * a[(slices-k1)*sliceStride + 1] = Re[k1][0][columns/2]
     *                = Re[slices-k1][0][columns/2],
     * a[(slices-k1)*sliceStride] = -Im[k1][0][columns/2]
     *                = Im[slices-k1][0][columns/2],
     * a[(slices-k1)*sliceStride + (rows/2)*rowStride + 1] = Re[k1][rows/2][columns/2]
     *                   = Re[slices-k1][rows/2][columns/2],
     * a[(slices-k1)*sliceStride + (rows/2) * rowStride] = -Im[k1][rows/2][columns/2]
     *                   = Im[slices-k1][rows/2][columns/2],
     *     0&lt;k1&lt;slices/2,
     * a[0] = Re[0][0][0],
     * a[1] = Re[0][0][columns/2],
     * a[(rows/2)*rowStride] = Re[0][rows/2][0],
     * a[(rows/2)*rowStride + 1] = Re[0][rows/2][columns/2],
     * a[(slices/2)*sliceStride] = Re[slices/2][0][0],
     * a[(slices/2)*sliceStride + 1] = Re[slices/2][0][columns/2],
     * a[(slices/2)*sliceStride + (rows/2)*rowStride] = Re[slices/2][rows/2][0],
     * a[(slices/2)*sliceStride + (rows/2)*rowStride + 1] = Re[slices/2][rows/2][columns/2]
     * </pre>
     *
     * This method computes only half of the elements of the real transform. The other half satisfies the symmetry
     * condition. If you want the full real inverse transform, use <code>realInverseFull</code>.
     *
     * @param a data to transform
     * @param scale if true then scaling is performed
     */
    public void realInverse(final double[] a, final boolean scale) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("slices, rows and columns must be power of two numbers");
        } else {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                rdft3d_sub(-1, a);
                cdft3db_subth(1, a, scale);
                xdft3da_subth1(1, 1, a, scale);
            } else {
                rdft3d_sub(-1, a);
                cdft3db_sub(1, a, scale);
                xdft3da_sub1(1, 1, a, scale);
            }
        }
    }

    /**
     * Computes 3D inverse DFT of real data leaving the result in <code>a</code> . This method only works when the sizes
     * of all three dimensions are power-of-two numbers. The data is stored in a 3D array. The physical layout of the
     * input data has to be as follows:
     *
     * <pre>
     * a[k1][k2][2*k3] = Re[k1][k2][k3]
     *                 = Re[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     * a[k1][k2][2*k3+1] = Im[k1][k2][k3]
     *                   = -Im[(slices-k1)%slices][(rows-k2)%rows][columns-k3],
     *     0&lt;=k1&lt;slices, 0&lt;=k2&lt;rows, 0&lt;k3&lt;columns/2,
     * a[k1][k2][0] = Re[k1][k2][0]
     *              = Re[(slices-k1)%slices][rows-k2][0],
     * a[k1][k2][1] = Im[k1][k2][0]
     *              = -Im[(slices-k1)%slices][rows-k2][0],
     * a[k1][rows-k2][1] = Re[(slices-k1)%slices][k2][columns/2]
     *                 = Re[k1][rows-k2][columns/2],
     * a[k1][rows-k2][0] = -Im[(slices-k1)%slices][k2][columns/2]
     *                 = Im[k1][rows-k2][columns/2],
     *     0&lt;=k1&lt;slices, 0&lt;k2&lt;rows/2,
     * a[k1][0][0] = Re[k1][0][0]
     *             = Re[slices-k1][0][0],
     * a[k1][0][1] = Im[k1][0][0]
     *             = -Im[slices-k1][0][0],
     * a[k1][rows/2][0] = Re[k1][rows/2][0]
     *                = Re[slices-k1][rows/2][0],
     * a[k1][rows/2][1] = Im[k1][rows/2][0]
     *                = -Im[slices-k1][rows/2][0],
     * a[slices-k1][0][1] = Re[k1][0][columns/2]
     *                = Re[slices-k1][0][columns/2],
     * a[slices-k1][0][0] = -Im[k1][0][columns/2]
     *                = Im[slices-k1][0][columns/2],
     * a[slices-k1][rows/2][1] = Re[k1][rows/2][columns/2]
     *                   = Re[slices-k1][rows/2][columns/2],
     * a[slices-k1][rows/2][0] = -Im[k1][rows/2][columns/2]
     *                   = Im[slices-k1][rows/2][columns/2],
     *     0&lt;k1&lt;slices/2,
     * a[0][0][0] = Re[0][0][0],
     * a[0][0][1] = Re[0][0][columns/2],
     * a[0][rows/2][0] = Re[0][rows/2][0],
     * a[0][rows/2][1] = Re[0][rows/2][columns/2],
     * a[slices/2][0][0] = Re[slices/2][0][0],
     * a[slices/2][0][1] = Re[slices/2][0][columns/2],
     * a[slices/2][rows/2][0] = Re[slices/2][rows/2][0],
     * a[slices/2][rows/2][1] = Re[slices/2][rows/2][columns/2]
     * </pre>
     *
     * This method computes only half of the elements of the real transform. The other half satisfies the symmetry
     * condition. If you want the full real inverse transform, use <code>realInverseFull</code>.
     *
     * @param a data to transform
     * @param scale if true then scaling is performed
     */
    public void realInverse(final double[][][] a, final boolean scale) {
        if (isPowerOfTwo == false) {
            throw new IllegalArgumentException("slices, rows and columns must be power of two numbers");
        } else {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                rdft3d_sub(-1, a);
                cdft3db_subth(1, a, scale);
                xdft3da_subth1(1, 1, a, scale);
            } else {
                rdft3d_sub(-1, a);
                cdft3db_sub(1, a, scale);
                xdft3da_sub1(1, 1, a, scale);
            }
        }
    }

    /**
     * Computes 3D inverse DFT of real data leaving the result in <code>a</code> . This method computes full real
     * inverse transform, i.e. you will get the same result as from <code>complexInverse</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>, the input array must be of size
     * slices*rows*2*columns, with only the first slices*rows*columns elements filled with real data.
     *
     * @param a data to transform
     * @param scale if true then scaling is performed
     */
    public void realInverseFull(final double[] a, final boolean scale) {
        if (isPowerOfTwo) {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(1, 1, a, scale);
                cdft3db_subth(1, a, scale);
                rdft3d_sub(1, a);
            } else {
                xdft3da_sub2(1, 1, a, scale);
                cdft3db_sub(1, a, scale);
                rdft3d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealInverseFull(a, scale);
        }
    }

    /**
     * Computes 3D inverse DFT of real data leaving the result in <code>a</code> . This method computes full real
     * inverse transform, i.e. you will get the same result as from <code>complexInverse</code> called with all
     * imaginary part equal 0. Because the result is stored in <code>a</code>, the input array must be of size slices by
     * rows by 2*columns, with only the first slices by rows by columns elements filled with real data.
     *
     * @param a data to transform
     * @param scale if true then scaling is performed
     */
    public void realInverseFull(final double[][][] a, final boolean scale) {
        if (isPowerOfTwo) {
            final int nthreads = ConcurrencyUtils.getNumberOfThreads();
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 8;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 4) {
                    nt >>= 1;
                } else if (columns < 4) {
                    nt >>= 2;
                }
                t = new double[nt];
                oldNthreads = nthreads;
            }
            if (nthreads > 1 && useThreads) {
                xdft3da_subth2(1, 1, a, scale);
                cdft3db_subth(1, a, scale);
                rdft3d_sub(1, a);
            } else {
                xdft3da_sub2(1, 1, a, scale);
                cdft3db_sub(1, a, scale);
                rdft3d_sub(1, a);
            }
            fillSymmetric(a);
        } else {
            mixedRadixRealInverseFull(a, scale);
        }
    }

    private void xdft3da_sub1(final int icr, final int isgn, final double[] a, final boolean scale) {
        int idx0, idx1, idx2, idx3, idx4, idx5;

        if (isgn == -1) {
            for (int s = 0; s < slices; s++) {
                idx0 = s * sliceStride;
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexForward(a, idx0 + r * rowStride);
                    }
                } else {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realInverse(a, idx0 + r * rowStride, scale);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[idx1];
                            t[idx2 + 1] = a[idx1 + 1];
                            t[idx3] = a[idx1 + 2];
                            t[idx3 + 1] = a[idx1 + 3];
                            t[idx4] = a[idx1 + 4];
                            t[idx4 + 1] = a[idx1 + 5];
                            t[idx5] = a[idx1 + 6];
                            t[idx5 + 1] = a[idx1 + 7];
                        }
                        fftRows.complexForward(t, 0);
                        fftRows.complexForward(t, 2 * rows);
                        fftRows.complexForward(t, 4 * rows);
                        fftRows.complexForward(t, 6 * rows);
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + 1];
                            a[idx1 + 2] = t[idx3];
                            a[idx1 + 3] = t[idx3 + 1];
                            a[idx1 + 4] = t[idx4];
                            a[idx1 + 5] = t[idx4 + 1];
                            a[idx1 + 6] = t[idx5];
                            a[idx1 + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                    }
                    fftRows.complexForward(t, 0);
                    fftRows.complexForward(t, 2 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                    }
                    fftRows.complexForward(t, 0);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                    }
                }
            }
        } else {
            for (int s = 0; s < slices; s++) {
                idx0 = s * sliceStride;
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexInverse(a, idx0 + r * rowStride, scale);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[idx1];
                            t[idx2 + 1] = a[idx1 + 1];
                            t[idx3] = a[idx1 + 2];
                            t[idx3 + 1] = a[idx1 + 3];
                            t[idx4] = a[idx1 + 4];
                            t[idx4 + 1] = a[idx1 + 5];
                            t[idx5] = a[idx1 + 6];
                            t[idx5 + 1] = a[idx1 + 7];
                        }
                        fftRows.complexInverse(t, 0, scale);
                        fftRows.complexInverse(t, 2 * rows, scale);
                        fftRows.complexInverse(t, 4 * rows, scale);
                        fftRows.complexInverse(t, 6 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + 1];
                            a[idx1 + 2] = t[idx3];
                            a[idx1 + 3] = t[idx3 + 1];
                            a[idx1 + 4] = t[idx4];
                            a[idx1 + 5] = t[idx4 + 1];
                            a[idx1 + 6] = t[idx5];
                            a[idx1 + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    fftRows.complexInverse(t, 2 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                    }
                }
                if (icr != 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realForward(a, idx0 + r * rowStride);
                    }
                }
            }
        }
    }

    private void xdft3da_sub1(final int icr, final int isgn, final double[][][] a, final boolean scale) {
        int idx2, idx3, idx4, idx5;

        if (isgn == -1) {
            for (int s = 0; s < slices; s++) {
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexForward(a[s][r]);
                    }
                } else {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realInverse(a[s][r], 0, scale);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[s][r][c];
                            t[idx2 + 1] = a[s][r][c + 1];
                            t[idx3] = a[s][r][c + 2];
                            t[idx3 + 1] = a[s][r][c + 3];
                            t[idx4] = a[s][r][c + 4];
                            t[idx4 + 1] = a[s][r][c + 5];
                            t[idx5] = a[s][r][c + 6];
                            t[idx5 + 1] = a[s][r][c + 7];
                        }
                        fftRows.complexForward(t, 0);
                        fftRows.complexForward(t, 2 * rows);
                        fftRows.complexForward(t, 4 * rows);
                        fftRows.complexForward(t, 6 * rows);
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[s][r][c] = t[idx2];
                            a[s][r][c + 1] = t[idx2 + 1];
                            a[s][r][c + 2] = t[idx3];
                            a[s][r][c + 3] = t[idx3 + 1];
                            a[s][r][c + 4] = t[idx4];
                            a[s][r][c + 5] = t[idx4 + 1];
                            a[s][r][c + 6] = t[idx5];
                            a[s][r][c + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                        t[idx3] = a[s][r][2];
                        t[idx3 + 1] = a[s][r][3];
                    }
                    fftRows.complexForward(t, 0);
                    fftRows.complexForward(t, 2 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                        a[s][r][2] = t[idx3];
                        a[s][r][3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                    }
                    fftRows.complexForward(t, 0);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                    }
                }
            }
        } else {
            for (int s = 0; s < slices; s++) {
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexInverse(a[s][r], scale);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[s][r][c];
                            t[idx2 + 1] = a[s][r][c + 1];
                            t[idx3] = a[s][r][c + 2];
                            t[idx3 + 1] = a[s][r][c + 3];
                            t[idx4] = a[s][r][c + 4];
                            t[idx4 + 1] = a[s][r][c + 5];
                            t[idx5] = a[s][r][c + 6];
                            t[idx5 + 1] = a[s][r][c + 7];
                        }
                        fftRows.complexInverse(t, 0, scale);
                        fftRows.complexInverse(t, 2 * rows, scale);
                        fftRows.complexInverse(t, 4 * rows, scale);
                        fftRows.complexInverse(t, 6 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[s][r][c] = t[idx2];
                            a[s][r][c + 1] = t[idx2 + 1];
                            a[s][r][c + 2] = t[idx3];
                            a[s][r][c + 3] = t[idx3 + 1];
                            a[s][r][c + 4] = t[idx4];
                            a[s][r][c + 5] = t[idx4 + 1];
                            a[s][r][c + 6] = t[idx5];
                            a[s][r][c + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                        t[idx3] = a[s][r][2];
                        t[idx3 + 1] = a[s][r][3];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    fftRows.complexInverse(t, 2 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                        a[s][r][2] = t[idx3];
                        a[s][r][3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                    }
                }
                if (icr != 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realForward(a[s][r], 0);
                    }
                }
            }
        }
    }

    private void xdft3da_sub2(final int icr, final int isgn, final double[] a, final boolean scale) {
        int idx0, idx1, idx2, idx3, idx4, idx5;

        if (isgn == -1) {
            for (int s = 0; s < slices; s++) {
                idx0 = s * sliceStride;
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexForward(a, idx0 + r * rowStride);
                    }
                } else {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realForward(a, idx0 + r * rowStride);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[idx1];
                            t[idx2 + 1] = a[idx1 + 1];
                            t[idx3] = a[idx1 + 2];
                            t[idx3 + 1] = a[idx1 + 3];
                            t[idx4] = a[idx1 + 4];
                            t[idx4 + 1] = a[idx1 + 5];
                            t[idx5] = a[idx1 + 6];
                            t[idx5 + 1] = a[idx1 + 7];
                        }
                        fftRows.complexForward(t, 0);
                        fftRows.complexForward(t, 2 * rows);
                        fftRows.complexForward(t, 4 * rows);
                        fftRows.complexForward(t, 6 * rows);
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + 1];
                            a[idx1 + 2] = t[idx3];
                            a[idx1 + 3] = t[idx3 + 1];
                            a[idx1 + 4] = t[idx4];
                            a[idx1 + 5] = t[idx4 + 1];
                            a[idx1 + 6] = t[idx5];
                            a[idx1 + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                    }
                    fftRows.complexForward(t, 0);
                    fftRows.complexForward(t, 2 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                    }
                    fftRows.complexForward(t, 0);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                    }
                }
            }
        } else {
            for (int s = 0; s < slices; s++) {
                idx0 = s * sliceStride;
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexInverse(a, idx0 + r * rowStride, scale);
                    }
                } else {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realInverse2(a, idx0 + r * rowStride, scale);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[idx1];
                            t[idx2 + 1] = a[idx1 + 1];
                            t[idx3] = a[idx1 + 2];
                            t[idx3 + 1] = a[idx1 + 3];
                            t[idx4] = a[idx1 + 4];
                            t[idx4 + 1] = a[idx1 + 5];
                            t[idx5] = a[idx1 + 6];
                            t[idx5 + 1] = a[idx1 + 7];
                        }
                        fftRows.complexInverse(t, 0, scale);
                        fftRows.complexInverse(t, 2 * rows, scale);
                        fftRows.complexInverse(t, 4 * rows, scale);
                        fftRows.complexInverse(t, 6 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[idx1] = t[idx2];
                            a[idx1 + 1] = t[idx2 + 1];
                            a[idx1 + 2] = t[idx3];
                            a[idx1 + 3] = t[idx3 + 1];
                            a[idx1 + 4] = t[idx4];
                            a[idx1 + 5] = t[idx4 + 1];
                            a[idx1 + 6] = t[idx5];
                            a[idx1 + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                        t[idx3] = a[idx1 + 2];
                        t[idx3 + 1] = a[idx1 + 3];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    fftRows.complexInverse(t, 2 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                        a[idx1 + 2] = t[idx3];
                        a[idx1 + 3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        t[idx2] = a[idx1];
                        t[idx2 + 1] = a[idx1 + 1];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        idx2 = 2 * r;
                        a[idx1] = t[idx2];
                        a[idx1 + 1] = t[idx2 + 1];
                    }
                }
            }
        }
    }

    private void xdft3da_sub2(final int icr, final int isgn, final double[][][] a, final boolean scale) {
        int idx2, idx3, idx4, idx5;

        if (isgn == -1) {
            for (int s = 0; s < slices; s++) {
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexForward(a[s][r]);
                    }
                } else {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realForward(a[s][r]);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[s][r][c];
                            t[idx2 + 1] = a[s][r][c + 1];
                            t[idx3] = a[s][r][c + 2];
                            t[idx3 + 1] = a[s][r][c + 3];
                            t[idx4] = a[s][r][c + 4];
                            t[idx4 + 1] = a[s][r][c + 5];
                            t[idx5] = a[s][r][c + 6];
                            t[idx5 + 1] = a[s][r][c + 7];
                        }
                        fftRows.complexForward(t, 0);
                        fftRows.complexForward(t, 2 * rows);
                        fftRows.complexForward(t, 4 * rows);
                        fftRows.complexForward(t, 6 * rows);
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[s][r][c] = t[idx2];
                            a[s][r][c + 1] = t[idx2 + 1];
                            a[s][r][c + 2] = t[idx3];
                            a[s][r][c + 3] = t[idx3 + 1];
                            a[s][r][c + 4] = t[idx4];
                            a[s][r][c + 5] = t[idx4 + 1];
                            a[s][r][c + 6] = t[idx5];
                            a[s][r][c + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                        t[idx3] = a[s][r][2];
                        t[idx3 + 1] = a[s][r][3];
                    }
                    fftRows.complexForward(t, 0);
                    fftRows.complexForward(t, 2 * rows);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                        a[s][r][2] = t[idx3];
                        a[s][r][3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                    }
                    fftRows.complexForward(t, 0);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                    }
                }
            }
        } else {
            for (int s = 0; s < slices; s++) {
                if (icr == 0) {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.complexInverse(a[s][r], scale);
                    }
                } else {
                    for (int r = 0; r < rows; r++) {
                        fftColumns.realInverse2(a[s][r], 0, scale);
                    }
                }
                if (columns > 4) {
                    for (int c = 0; c < columns; c += 8) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            t[idx2] = a[s][r][c];
                            t[idx2 + 1] = a[s][r][c + 1];
                            t[idx3] = a[s][r][c + 2];
                            t[idx3 + 1] = a[s][r][c + 3];
                            t[idx4] = a[s][r][c + 4];
                            t[idx4 + 1] = a[s][r][c + 5];
                            t[idx5] = a[s][r][c + 6];
                            t[idx5 + 1] = a[s][r][c + 7];
                        }
                        fftRows.complexInverse(t, 0, scale);
                        fftRows.complexInverse(t, 2 * rows, scale);
                        fftRows.complexInverse(t, 4 * rows, scale);
                        fftRows.complexInverse(t, 6 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx2 = 2 * r;
                            idx3 = 2 * rows + 2 * r;
                            idx4 = idx3 + 2 * rows;
                            idx5 = idx4 + 2 * rows;
                            a[s][r][c] = t[idx2];
                            a[s][r][c + 1] = t[idx2 + 1];
                            a[s][r][c + 2] = t[idx3];
                            a[s][r][c + 3] = t[idx3 + 1];
                            a[s][r][c + 4] = t[idx4];
                            a[s][r][c + 5] = t[idx4 + 1];
                            a[s][r][c + 6] = t[idx5];
                            a[s][r][c + 7] = t[idx5 + 1];
                        }
                    }
                } else if (columns == 4) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                        t[idx3] = a[s][r][2];
                        t[idx3 + 1] = a[s][r][3];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    fftRows.complexInverse(t, 2 * rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        idx3 = 2 * rows + 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                        a[s][r][2] = t[idx3];
                        a[s][r][3] = t[idx3 + 1];
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        t[idx2] = a[s][r][0];
                        t[idx2 + 1] = a[s][r][1];
                    }
                    fftRows.complexInverse(t, 0, scale);
                    for (int r = 0; r < rows; r++) {
                        idx2 = 2 * r;
                        a[s][r][0] = t[idx2];
                        a[s][r][1] = t[idx2 + 1];
                    }
                }
            }
        }
    }

    private void xdft3da_subth1(final int icr, final int isgn, final double[] a, final boolean scale) {
        int nt, i;
        final int nthreads = Math.min(ConcurrencyUtils.getNumberOfThreads(), slices);
        nt = 8 * rows;
        if (columns == 4) {
            nt >>= 1;
        } else if (columns < 4) {
            nt >>= 2;
        }
        final Future<?>[] futures = new Future[nthreads];
        for (i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {
                @Override
                public void run() {
                    int idx0, idx1, idx2, idx3, idx4, idx5;

                    if (isgn == -1) {
                        for (int s = n0; s < slices; s += nthreads) {
                            idx0 = s * sliceStride;
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexForward(a, idx0 + r * rowStride);
                                }
                            } else {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realInverse(a, idx0 + r * rowStride, scale);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[idx1];
                                        t[idx2 + 1] = a[idx1 + 1];
                                        t[idx3] = a[idx1 + 2];
                                        t[idx3 + 1] = a[idx1 + 3];
                                        t[idx4] = a[idx1 + 4];
                                        t[idx4 + 1] = a[idx1 + 5];
                                        t[idx5] = a[idx1 + 6];
                                        t[idx5 + 1] = a[idx1 + 7];
                                    }
                                    fftRows.complexForward(t, startt);
                                    fftRows.complexForward(t, startt + 2 * rows);
                                    fftRows.complexForward(t, startt + 4 * rows);
                                    fftRows.complexForward(t, startt + 6 * rows);
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[idx1] = t[idx2];
                                        a[idx1 + 1] = t[idx2 + 1];
                                        a[idx1 + 2] = t[idx3];
                                        a[idx1 + 3] = t[idx3 + 1];
                                        a[idx1 + 4] = t[idx4];
                                        a[idx1 + 5] = t[idx4 + 1];
                                        a[idx1 + 6] = t[idx5];
                                        a[idx1 + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                }
                                fftRows.complexForward(t, startt);
                                fftRows.complexForward(t, startt + 2 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                }
                                fftRows.complexForward(t, startt);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                }
                            }

                        }
                    } else {
                        for (int s = n0; s < slices; s += nthreads) {
                            idx0 = s * sliceStride;
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexInverse(a, idx0 + r * rowStride, scale);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[idx1];
                                        t[idx2 + 1] = a[idx1 + 1];
                                        t[idx3] = a[idx1 + 2];
                                        t[idx3 + 1] = a[idx1 + 3];
                                        t[idx4] = a[idx1 + 4];
                                        t[idx4 + 1] = a[idx1 + 5];
                                        t[idx5] = a[idx1 + 6];
                                        t[idx5 + 1] = a[idx1 + 7];
                                    }
                                    fftRows.complexInverse(t, startt, scale);
                                    fftRows.complexInverse(t, startt + 2 * rows, scale);
                                    fftRows.complexInverse(t, startt + 4 * rows, scale);
                                    fftRows.complexInverse(t, startt + 6 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[idx1] = t[idx2];
                                        a[idx1 + 1] = t[idx2 + 1];
                                        a[idx1 + 2] = t[idx3];
                                        a[idx1 + 3] = t[idx3 + 1];
                                        a[idx1 + 4] = t[idx4];
                                        a[idx1 + 5] = t[idx4 + 1];
                                        a[idx1 + 6] = t[idx5];
                                        a[idx1 + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                fftRows.complexInverse(t, startt + 2 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                }
                            }
                            if (icr != 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realForward(a, idx0 + r * rowStride);
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void xdft3da_subth1(final int icr, final int isgn, final double[][][] a, final boolean scale) {
        int nt, i;

        final int nthreads = Math.min(ConcurrencyUtils.getNumberOfThreads(), slices);
        nt = 8 * rows;
        if (columns == 4) {
            nt >>= 1;
        } else if (columns < 4) {
            nt >>= 2;
        }
        final Future<?>[] futures = new Future[nthreads];
        for (i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {
                @Override
                public void run() {
                    int idx2, idx3, idx4, idx5;

                    if (isgn == -1) {
                        for (int s = n0; s < slices; s += nthreads) {
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexForward(a[s][r]);
                                }
                            } else {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realInverse(a[s][r], 0, scale);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[s][r][c];
                                        t[idx2 + 1] = a[s][r][c + 1];
                                        t[idx3] = a[s][r][c + 2];
                                        t[idx3 + 1] = a[s][r][c + 3];
                                        t[idx4] = a[s][r][c + 4];
                                        t[idx4 + 1] = a[s][r][c + 5];
                                        t[idx5] = a[s][r][c + 6];
                                        t[idx5 + 1] = a[s][r][c + 7];
                                    }
                                    fftRows.complexForward(t, startt);
                                    fftRows.complexForward(t, startt + 2 * rows);
                                    fftRows.complexForward(t, startt + 4 * rows);
                                    fftRows.complexForward(t, startt + 6 * rows);
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[s][r][c] = t[idx2];
                                        a[s][r][c + 1] = t[idx2 + 1];
                                        a[s][r][c + 2] = t[idx3];
                                        a[s][r][c + 3] = t[idx3 + 1];
                                        a[s][r][c + 4] = t[idx4];
                                        a[s][r][c + 5] = t[idx4 + 1];
                                        a[s][r][c + 6] = t[idx5];
                                        a[s][r][c + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                    t[idx3] = a[s][r][2];
                                    t[idx3 + 1] = a[s][r][3];
                                }
                                fftRows.complexForward(t, startt);
                                fftRows.complexForward(t, startt + 2 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                    a[s][r][2] = t[idx3];
                                    a[s][r][3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                }
                                fftRows.complexForward(t, startt);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                }
                            }

                        }
                    } else {
                        for (int s = n0; s < slices; s += nthreads) {
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexInverse(a[s][r], scale);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[s][r][c];
                                        t[idx2 + 1] = a[s][r][c + 1];
                                        t[idx3] = a[s][r][c + 2];
                                        t[idx3 + 1] = a[s][r][c + 3];
                                        t[idx4] = a[s][r][c + 4];
                                        t[idx4 + 1] = a[s][r][c + 5];
                                        t[idx5] = a[s][r][c + 6];
                                        t[idx5 + 1] = a[s][r][c + 7];
                                    }
                                    fftRows.complexInverse(t, startt, scale);
                                    fftRows.complexInverse(t, startt + 2 * rows, scale);
                                    fftRows.complexInverse(t, startt + 4 * rows, scale);
                                    fftRows.complexInverse(t, startt + 6 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[s][r][c] = t[idx2];
                                        a[s][r][c + 1] = t[idx2 + 1];
                                        a[s][r][c + 2] = t[idx3];
                                        a[s][r][c + 3] = t[idx3 + 1];
                                        a[s][r][c + 4] = t[idx4];
                                        a[s][r][c + 5] = t[idx4 + 1];
                                        a[s][r][c + 6] = t[idx5];
                                        a[s][r][c + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                    t[idx3] = a[s][r][2];
                                    t[idx3 + 1] = a[s][r][3];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                fftRows.complexInverse(t, startt + 2 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                    a[s][r][2] = t[idx3];
                                    a[s][r][3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                }
                            }
                            if (icr != 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realForward(a[s][r]);
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void xdft3da_subth2(final int icr, final int isgn, final double[] a, final boolean scale) {
        int nt, i;

        final int nthreads = Math.min(ConcurrencyUtils.getNumberOfThreads(), slices);
        nt = 8 * rows;
        if (columns == 4) {
            nt >>= 1;
        } else if (columns < 4) {
            nt >>= 2;
        }
        final Future<?>[] futures = new Future[nthreads];
        for (i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {
                @Override
                public void run() {
                    int idx0, idx1, idx2, idx3, idx4, idx5;

                    if (isgn == -1) {
                        for (int s = n0; s < slices; s += nthreads) {
                            idx0 = s * sliceStride;
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexForward(a, idx0 + r * rowStride);
                                }
                            } else {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realForward(a, idx0 + r * rowStride);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[idx1];
                                        t[idx2 + 1] = a[idx1 + 1];
                                        t[idx3] = a[idx1 + 2];
                                        t[idx3 + 1] = a[idx1 + 3];
                                        t[idx4] = a[idx1 + 4];
                                        t[idx4 + 1] = a[idx1 + 5];
                                        t[idx5] = a[idx1 + 6];
                                        t[idx5 + 1] = a[idx1 + 7];
                                    }
                                    fftRows.complexForward(t, startt);
                                    fftRows.complexForward(t, startt + 2 * rows);
                                    fftRows.complexForward(t, startt + 4 * rows);
                                    fftRows.complexForward(t, startt + 6 * rows);
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[idx1] = t[idx2];
                                        a[idx1 + 1] = t[idx2 + 1];
                                        a[idx1 + 2] = t[idx3];
                                        a[idx1 + 3] = t[idx3 + 1];
                                        a[idx1 + 4] = t[idx4];
                                        a[idx1 + 5] = t[idx4 + 1];
                                        a[idx1 + 6] = t[idx5];
                                        a[idx1 + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                }
                                fftRows.complexForward(t, startt);
                                fftRows.complexForward(t, startt + 2 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                }
                                fftRows.complexForward(t, startt);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                }
                            }

                        }
                    } else {
                        for (int s = n0; s < slices; s += nthreads) {
                            idx0 = s * sliceStride;
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexInverse(a, idx0 + r * rowStride, scale);
                                }
                            } else {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realInverse2(a, idx0 + r * rowStride, scale);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[idx1];
                                        t[idx2 + 1] = a[idx1 + 1];
                                        t[idx3] = a[idx1 + 2];
                                        t[idx3 + 1] = a[idx1 + 3];
                                        t[idx4] = a[idx1 + 4];
                                        t[idx4 + 1] = a[idx1 + 5];
                                        t[idx5] = a[idx1 + 6];
                                        t[idx5 + 1] = a[idx1 + 7];
                                    }
                                    fftRows.complexInverse(t, startt, scale);
                                    fftRows.complexInverse(t, startt + 2 * rows, scale);
                                    fftRows.complexInverse(t, startt + 4 * rows, scale);
                                    fftRows.complexInverse(t, startt + 6 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[idx1] = t[idx2];
                                        a[idx1 + 1] = t[idx2 + 1];
                                        a[idx1 + 2] = t[idx3];
                                        a[idx1 + 3] = t[idx3 + 1];
                                        a[idx1 + 4] = t[idx4];
                                        a[idx1 + 5] = t[idx4 + 1];
                                        a[idx1 + 6] = t[idx5];
                                        a[idx1 + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                    t[idx3] = a[idx1 + 2];
                                    t[idx3 + 1] = a[idx1 + 3];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                fftRows.complexInverse(t, startt + 2 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                    a[idx1 + 2] = t[idx3];
                                    a[idx1 + 3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[idx1];
                                    t[idx2 + 1] = a[idx1 + 1];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    idx2 = startt + 2 * r;
                                    a[idx1] = t[idx2];
                                    a[idx1 + 1] = t[idx2 + 1];
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void xdft3da_subth2(final int icr, final int isgn, final double[][][] a, final boolean scale) {
        int nt, i;

        final int nthreads = Math.min(ConcurrencyUtils.getNumberOfThreads(), slices);
        nt = 8 * rows;
        if (columns == 4) {
            nt >>= 1;
        } else if (columns < 4) {
            nt >>= 2;
        }
        final Future<?>[] futures = new Future[nthreads];
        for (i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {
                @Override
                public void run() {
                    int idx2, idx3, idx4, idx5;

                    if (isgn == -1) {
                        for (int s = n0; s < slices; s += nthreads) {
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexForward(a[s][r]);
                                }
                            } else {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realForward(a[s][r]);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[s][r][c];
                                        t[idx2 + 1] = a[s][r][c + 1];
                                        t[idx3] = a[s][r][c + 2];
                                        t[idx3 + 1] = a[s][r][c + 3];
                                        t[idx4] = a[s][r][c + 4];
                                        t[idx4 + 1] = a[s][r][c + 5];
                                        t[idx5] = a[s][r][c + 6];
                                        t[idx5 + 1] = a[s][r][c + 7];
                                    }
                                    fftRows.complexForward(t, startt);
                                    fftRows.complexForward(t, startt + 2 * rows);
                                    fftRows.complexForward(t, startt + 4 * rows);
                                    fftRows.complexForward(t, startt + 6 * rows);
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[s][r][c] = t[idx2];
                                        a[s][r][c + 1] = t[idx2 + 1];
                                        a[s][r][c + 2] = t[idx3];
                                        a[s][r][c + 3] = t[idx3 + 1];
                                        a[s][r][c + 4] = t[idx4];
                                        a[s][r][c + 5] = t[idx4 + 1];
                                        a[s][r][c + 6] = t[idx5];
                                        a[s][r][c + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                    t[idx3] = a[s][r][2];
                                    t[idx3 + 1] = a[s][r][3];
                                }
                                fftRows.complexForward(t, startt);
                                fftRows.complexForward(t, startt + 2 * rows);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                    a[s][r][2] = t[idx3];
                                    a[s][r][3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                }
                                fftRows.complexForward(t, startt);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                }
                            }

                        }
                    } else {
                        for (int s = n0; s < slices; s += nthreads) {
                            if (icr == 0) {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.complexInverse(a[s][r], scale);
                                }
                            } else {
                                for (int r = 0; r < rows; r++) {
                                    fftColumns.realInverse2(a[s][r], 0, scale);
                                }
                            }
                            if (columns > 4) {
                                for (int c = 0; c < columns; c += 8) {
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        t[idx2] = a[s][r][c];
                                        t[idx2 + 1] = a[s][r][c + 1];
                                        t[idx3] = a[s][r][c + 2];
                                        t[idx3 + 1] = a[s][r][c + 3];
                                        t[idx4] = a[s][r][c + 4];
                                        t[idx4 + 1] = a[s][r][c + 5];
                                        t[idx5] = a[s][r][c + 6];
                                        t[idx5 + 1] = a[s][r][c + 7];
                                    }
                                    fftRows.complexInverse(t, startt, scale);
                                    fftRows.complexInverse(t, startt + 2 * rows, scale);
                                    fftRows.complexInverse(t, startt + 4 * rows, scale);
                                    fftRows.complexInverse(t, startt + 6 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + 2 * r;
                                        idx3 = startt + 2 * rows + 2 * r;
                                        idx4 = idx3 + 2 * rows;
                                        idx5 = idx4 + 2 * rows;
                                        a[s][r][c] = t[idx2];
                                        a[s][r][c + 1] = t[idx2 + 1];
                                        a[s][r][c + 2] = t[idx3];
                                        a[s][r][c + 3] = t[idx3 + 1];
                                        a[s][r][c + 4] = t[idx4];
                                        a[s][r][c + 5] = t[idx4 + 1];
                                        a[s][r][c + 6] = t[idx5];
                                        a[s][r][c + 7] = t[idx5 + 1];
                                    }
                                }
                            } else if (columns == 4) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                    t[idx3] = a[s][r][2];
                                    t[idx3 + 1] = a[s][r][3];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                fftRows.complexInverse(t, startt + 2 * rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    idx3 = startt + 2 * rows + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                    a[s][r][2] = t[idx3];
                                    a[s][r][3] = t[idx3 + 1];
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    t[idx2] = a[s][r][0];
                                    t[idx2 + 1] = a[s][r][1];
                                }
                                fftRows.complexInverse(t, startt, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx2 = startt + 2 * r;
                                    a[s][r][0] = t[idx2];
                                    a[s][r][1] = t[idx2 + 1];
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }
}
