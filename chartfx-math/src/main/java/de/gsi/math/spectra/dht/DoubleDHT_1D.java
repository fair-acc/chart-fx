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

import de.gsi.math.spectra.fft.DoubleFFT_1D;
import de.gsi.math.utils.ConcurrencyUtils;

/**
 * Computes 1D Discrete Hartley Transform (DHT) of real, double precision data.
 * The size of the data can be an arbitrary number. It uses FFT algorithm. This
 * is a parallel implementation optimized for SMP systems.
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 */
public class DoubleDHT_1D {
    private final int n;
    private final DoubleFFT_1D fft;

    /**
     * Creates new instance of DoubleDHT_1D.
     *
     * @param n
     *            size of data
     */
    public DoubleDHT_1D(final int n) {
        this.n = n;
        fft = new DoubleFFT_1D(n);
    }

    /**
     * Computes 1D real, forward DHT leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     */
    public void forward(final double[] a) {
        forward(a, 0);
    }

    /**
     * Computes 1D real, forward DHT leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     */
    public void forward(final double[] a, final int offa) {
        if (n == 1) {
            return;
        }
        fft.realForward(a, offa);
        final double[] b = new double[n];
        System.arraycopy(a, offa, b, 0, n);
        final int nd2 = n / 2;
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && nd2 > ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2;
            final int k1 = nd2 / nthreads;
            final Future<?>[] futures = new Future[nthreads];
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = 1 + i * k1;
                final int lastIdx = i == nthreads - 1 ? nd2 : firstIdx + k1;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {

                    @Override
                    public void run() {
                        int idx1, idx2;
                        for (int i = firstIdx; i < lastIdx; i++) {
                            idx1 = 2 * i;
                            idx2 = idx1 + 1;
                            a[offa + i] = b[idx1] - b[idx2];
                            a[offa + n - i] = b[idx1] + b[idx2];
                        }
                    }

                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            int idx1, idx2;
            for (int i = 1; i < nd2; i++) {
                idx1 = 2 * i;
                idx2 = idx1 + 1;
                a[offa + i] = b[idx1] - b[idx2];
                a[offa + n - i] = b[idx1] + b[idx2];
            }
        }
        if (n % 2 == 0) {
            a[offa + nd2] = b[1];
        } else {
            a[offa + nd2] = b[n - 1] - b[1];
            a[offa + nd2 + 1] = b[n - 1] + b[1];
        }

    }

    /**
     * Computes 1D real, inverse DHT leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final double[] a, final boolean scale) {
        inverse(a, 0, scale);
    }

    /**
     * Computes 1D real, inverse DHT leaving the result in <code>a</code>.
     *
     * @param a
     *            data to transform
     * @param offa
     *            index of the first element in array <code>a</code>
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final double[] a, final int offa, final boolean scale) {
        if (n == 1) {
            return;
        }
        forward(a, offa);
        if (scale) {
            scale(n, a, offa);
        }
    }

    private void scale(final double m, final double[] a, final int offa) {
        final double norm = 1.0 / m;
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && n >= ConcurrencyUtils.getThreadsBeginN_1D_FFT_2Threads()) {
            nthreads = 2;
            final int k = n / nthreads;
            final Future<?>[] futures = new Future[nthreads];
            for (int i = 0; i < nthreads; i++) {
                final int firstIdx = offa + i * k;
                final int lastIdx = i == nthreads - 1 ? offa + n : firstIdx + k;
                futures[i] = ConcurrencyUtils.submit(new Runnable() {

                    @Override
                    public void run() {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            a[i] *= norm;
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);
        } else {
            final int lastIdx = offa + n;
            for (int i = offa; i < lastIdx; i++) {
                a[i] *= norm;
            }

        }
    }
}
