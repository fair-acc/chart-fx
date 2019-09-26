package de.gsi.math.filter.iir;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2009 by Vinnie Falco
 * Copyright (c) 2016 by Bernd Porr
 * Copyright (c) 2019 by Ralph J. Steinhagen
 */

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;

/**
 * User facing class which contains all the methods the user uses to create
 * Bessel filters. This done in this way: Bessel bessel = new Bessel(); Then
 * call one of the methods below to create low-,high-,band-, or stopband
 * filters. For example: bessel.bandPass(2,250,50,5);
 */
public class Bessel extends Cascade {

    class AnalogLowPass extends LayoutBase {

        private int degree;
        private double[] mA;
        private Complex[] mRoot;

        // returns the k-th zero based coefficient of the reverse bessel
        // polynomial of degree n
        private double reversebessel(final int k, final int n) {
            final int diff = n - k;
            return factorial(2 * n - k) / (factorial(diff) * factorial(k) * Math.pow(2.0, diff));
        }

        // returns fact(n) = n!
        private double factorial(final int n) {
            if (n == 0) {
                return 1;
            }

            double y = n;
            for (double m = n - 1.0; m > 0; m--) {
                y = y * m;
            }

            return y;
        }

        // ------------------------------------------------------------------------------

        public AnalogLowPass(final int _degree) {
            super(_degree);
            degree = _degree;
            mA = new double[degree + 1]; // input coefficients (degree+1 elements)
            mRoot = new Complex[degree]; // array of roots (degree elements)
            setNormal(0, 1);
        }

        public void design() {
            reset();

            for (int i = 0; i < degree + 1; ++i) {
                mA[i] = reversebessel(i, degree);
            }

            final LaguerreSolver laguerreSolver = new LaguerreSolver();

            mRoot = laguerreSolver.solveAllComplex(mA, 0.0);

            final Complex inf = Complex.INF;
            final int pairs = degree / 2;
            for (int i = 0; i < pairs; ++i) {
                final Complex c = mRoot[i];
                addPoleZeroConjugatePairs(c, inf);
            }

            if ((degree & 1) == 1) {
                add(new Complex(mRoot[pairs].getReal()), inf);
            }
        }

    }

    private void setupLowPass(final int order, final double sampleRate, final double cutoffFrequency,
            final int directFormType) {

        final AnalogLowPass analogProto = new AnalogLowPass(order);

        analogProto.design();

        final LayoutBase digitalProto = new LayoutBase(order);

        LowPassTransform.transform(cutoffFrequency / sampleRate, digitalProto, analogProto);

        setLayout(digitalProto, directFormType);
    }

    /**
     * Bessel Lowpass filter with default topology
     *
     * @param order
     *            The order of the filter
     * @param sampleRate
     *            The sampling rate of the system
     * @param cutoffFrequency
     *            the cutoff frequency
     */
    public void lowPass(final int order, final double sampleRate, final double cutoffFrequency) {
        setupLowPass(order, sampleRate, cutoffFrequency, DirectFormAbstract.DIRECT_FORM_II);
    }

    /**
     * Bessel Lowpass filter with custom topology
     *
     * @param order
     *            The order of the filter
     * @param sampleRate
     *            The sampling rate of the system
     * @param cutoffFrequency
     *            The cutoff frequency
     * @param directFormType
     *            The filter topology. This is either
     *            DirectFormAbstract.DIRECT_FORM_I or DIRECT_FORM_II
     */
    public void lowPass(final int order, final double sampleRate, final double cutoffFrequency,
            final int directFormType) {
        setupLowPass(order, sampleRate, cutoffFrequency, directFormType);
    }

    private void setupHighPass(final int order, final double sampleRate, final double cutoffFrequency,
            final int directFormType) {

        final AnalogLowPass m_analogProto = new AnalogLowPass(order);
        m_analogProto.design();

        final LayoutBase m_digitalProto = new LayoutBase(order);

        HighPassTransform.transform(cutoffFrequency / sampleRate, m_digitalProto, m_analogProto);

        setLayout(m_digitalProto, directFormType);
    }

    /**
     * Highpass filter with custom topology
     *
     * @param order
     *            Filter order (ideally only even orders)
     * @param sampleRate
     *            Sampling rate of the system
     * @param cutoffFrequency
     *            Cutoff of the system
     * @param directFormType
     *            The filter topology. See DirectFormAbstract.
     */
    public void highPass(final int order, final double sampleRate, final double cutoffFrequency,
            final int directFormType) {
        setupHighPass(order, sampleRate, cutoffFrequency, directFormType);
    }

    /**
     * Highpass filter with default filter topology
     *
     * @param order
     *            Filter order (ideally only even orders)
     * @param sampleRate
     *            Sampling rate of the system
     * @param cutoffFrequency
     *            Cutoff of the system
     */
    public void highPass(final int order, final double sampleRate, final double cutoffFrequency) {
        setupHighPass(order, sampleRate, cutoffFrequency, DirectFormAbstract.DIRECT_FORM_II);
    }

    private void setupBandStop(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final int directFormType) {

        final AnalogLowPass m_analogProto = new AnalogLowPass(order);
        m_analogProto.design();

        final LayoutBase m_digitalProto = new LayoutBase(order * 2);

        BandStopTransform.transform(centerFrequency / sampleRate, widthFrequency / sampleRate, m_digitalProto,
                m_analogProto);

        setLayout(m_digitalProto, directFormType);
    }

    /**
     * Bandstop filter with default topology
     *
     * @param order
     *            Filter order (actual order is twice)
     * @param sampleRate
     *            Samping rate of the system
     * @param centerFrequency
     *            Center frequency
     * @param widthFrequency
     *            Width of the notch
     */
    public void bandStop(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency) {
        setupBandStop(order, sampleRate, centerFrequency, widthFrequency, DirectFormAbstract.DIRECT_FORM_II);
    }

    /**
     * Bandstop filter with custom topology
     *
     * @param order
     *            Filter order (actual order is twice)
     * @param sampleRate
     *            Samping rate of the system
     * @param centerFrequency
     *            Center frequency
     * @param widthFrequency
     *            Width of the notch
     * @param directFormType
     *            The filter topology
     */
    public void bandStop(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final int directFormType) {
        setupBandStop(order, sampleRate, centerFrequency, widthFrequency, directFormType);
    }

    private void setupBandPass(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final int directFormType) {

        final AnalogLowPass m_analogProto = new AnalogLowPass(order);
        m_analogProto.design();

        final LayoutBase m_digitalProto = new LayoutBase(order * 2);

        BandPassTransform.transform(centerFrequency / sampleRate, widthFrequency / sampleRate, m_digitalProto,
                m_analogProto);

        setLayout(m_digitalProto, directFormType);

    }

    /**
     * Bandpass filter with default topology
     *
     * @param order
     *            Filter order
     * @param sampleRate
     *            Sampling rate
     * @param centerFrequency
     *            Center frequency
     * @param widthFrequency
     *            Width of the notch
     */
    public void bandPass(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency) {
        setupBandPass(order, sampleRate, centerFrequency, widthFrequency, DirectFormAbstract.DIRECT_FORM_II);
    }

    /**
     * Bandpass filter with custom topology
     *
     * @param order
     *            Filter order
     * @param sampleRate
     *            Sampling rate
     * @param centerFrequency
     *            Center frequency
     * @param widthFrequency
     *            Width of the notch
     * @param directFormType
     *            The filter topology (see DirectFormAbstract)
     */
    public void bandPass(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final int directFormType) {
        setupBandPass(order, sampleRate, centerFrequency, widthFrequency, directFormType);
    }

}
