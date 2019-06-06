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

package de.gsi.math.spectra.dst;

import java.util.concurrent.Future;

import de.gsi.math.spectra.dct.FloatDCT_1D;
import de.gsi.math.utils.ConcurrencyUtils;

/**
 * Computes 1D Discrete Sine Transform (DST) of single precision data. The size
 * of data can be an arbitrary number. It uses DCT algorithm. This is a parallel
 * implementation optimized for SMP systems.
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class FloatDST_1D {

    private final int n;

    private final FloatDCT_1D dct;

    /**
     * Creates new instance of FloatDST_1D.
     *
     * @param n
     *            size of data
     */
    public FloatDST_1D(final int n) {
        this.n = n;
        dct = new FloatDCT_1D(n);
    }

    /**
     * Computes 1D forward DST (DST-II) leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void forward(final float[] a, final boolean scale) {
        forward(a, 0, scale);
    }

    /**
     * Computes 1D forward DST (DST-II) leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     * @param scale
     *            if true then scaling is performed
     */
    public void forward(final float[] a, final int offa, final boolean scale) {
        if (n == 1) {
            return;
        }
        float tmp;
        final int nd2 = n / 2;
        final int startIdx = 1 + offa;
        final int stopIdx = offa + n;
        for (int i = startIdx; i < stopIdx; i += 2) {
            a[i] = -a[i];
        }
        dct.forward(a, offa, scale);
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && nd2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2;
            final int k = nd2 / nthreads;
            final Future<?>[] futures = new Future[nthreads];
            for (int j = 0; j < nthreads; j++) {
                final int firstIdx = j * k;
                final int lastIdx = j == nthreads - 1 ? nd2 : firstIdx + k;
                futures[j] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        float tmp;
                        final int idx0 = offa + n - 1;
                        int idx1;
                        int idx2;
                        for (int i = firstIdx; i < lastIdx; i++) {
                            idx2 = offa + i;
                            tmp = a[idx2];
                            idx1 = idx0 - i;
                            a[idx2] = a[idx1];
                            a[idx1] = tmp;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            final int idx0 = offa + n - 1;
            int idx1;
            int idx2;
            for (int i = 0; i < nd2; i++) {
                idx2 = offa + i;
                tmp = a[idx2];
                idx1 = idx0 - i;
                a[idx2] = a[idx1];
                a[idx1] = tmp;
            }
        }
    }

    /**
     * Computes 1D inverse DST (DST-III) leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final float[] a, final boolean scale) {
        inverse(a, 0, scale);
    }

    /**
     * Computes 1D inverse DST (DST-III) leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final float[] a, final int offa, final boolean scale) {
        if (n == 1) {
            return;
        }
        float tmp;
        final int nd2 = n / 2;
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && nd2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2;
            final int k = nd2 / nthreads;
            final Future<?>[] futures = new Future[nthreads];
            for (int j = 0; j < nthreads; j++) {
                final int firstIdx = j * k;
                final int lastIdx = j == nthreads - 1 ? nd2 : firstIdx + k;
                futures[j] = ConcurrencyUtils.submit(new Runnable() {
                    @Override
                    public void run() {
                        float tmp;
                        final int idx0 = offa + n - 1;
                        int idx1, idx2;
                        for (int i = firstIdx; i < lastIdx; i++) {
                            idx2 = offa + i;
                            tmp = a[idx2];
                            idx1 = idx0 - i;
                            a[idx2] = a[idx1];
                            a[idx1] = tmp;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            final int idx0 = offa + n - 1;
            for (int i = 0; i < nd2; i++) {
                tmp = a[offa + i];
                a[offa + i] = a[idx0 - i];
                a[idx0 - i] = tmp;
            }
        }
        dct.inverse(a, offa, scale);
        final int startidx = 1 + offa;
        final int stopidx = offa + n;
        for (int i = startidx; i < stopidx; i += 2) {
            a[i] = -a[i];
        }
    }
}
