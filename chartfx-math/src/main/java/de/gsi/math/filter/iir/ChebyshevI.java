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

package de.gsi.math.filter.iir;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.util.FastMath;

/**
 * User facing class which contains all the methods the user uses to create
 * ChebyshevI filters. This done in this way: ChebyshevI chebyshevI = new
 * ChebyshevI(); Then call one of the methods below to create low-,high-,band-,
 * or stop-band filters. For example: chebyshevI.bandPass(2,250,50,5,0.5);
 */
public class ChebyshevI extends Cascade {

    class AnalogLowPass extends LayoutBase {
        private final int nPoles;

        public AnalogLowPass(final int _nPoles) {
            super(_nPoles);
            nPoles = _nPoles;
        }

        public void design(final double rippleDb) {

            reset();

            final double eps = Math.sqrt(1. / Math.exp(-rippleDb * 0.1 * Math.log(10)) - 1);
            final double v0 = FastMath.asinh(1 / eps) / nPoles;
            final double sinh_v0 = -Math.sinh(v0);
            final double cosh_v0 = Math.cosh(v0);

            final double n2 = 2 * nPoles;
            final int pairs = nPoles / 2;
            for (int i = 0; i < pairs; ++i) {
                final int k = 2 * i + 1 - nPoles;
                final double a = sinh_v0 * Math.cos(k * Math.PI / n2);
                final double b = cosh_v0 * Math.sin(k * Math.PI / n2);

                addPoleZeroConjugatePairs(new Complex(a, b), new Complex(Double.POSITIVE_INFINITY));
            }

            if ((nPoles & 1) == 1) {
                add(new Complex(sinh_v0, 0), new Complex(Double.POSITIVE_INFINITY));
                setNormal(0, 1);
            } else {
                setNormal(0, Math.pow(10, -rippleDb / 20.));
            }
        }
    }

    private void setupLowPass(final int order, final double sampleRate, final double cutoffFrequency,
            final double rippleDb, final int directFormType) {

        final AnalogLowPass analogProto = new AnalogLowPass(order);
        analogProto.design(rippleDb);

        final LayoutBase digitalProto = new LayoutBase(order);

        LowPassTransform.transform(cutoffFrequency / sampleRate, digitalProto, analogProto);

        setLayout(digitalProto, directFormType);
    }

    /**
     * ChebyshevI Low-pass filter with default topology
     * 
     * @param order The order of the filter
     * @param sampleRate The sampling rate of the system
     * @param cutoffFrequency the cutoff frequency
     * @param rippleDb pass.band ripple in decibel sensible value: 1dB
     */
    public void lowPass(final int order, final double sampleRate, final double cutoffFrequency, final double rippleDb) {
        setupLowPass(order, sampleRate, cutoffFrequency, rippleDb, DirectFormAbstract.DIRECT_FORM_I);
    }

    /**
     * ChebyshevI Low-pass filter with custom topology
     * 
     * @param order The order of the filter
     * @param sampleRate The sampling rate of the system
     * @param cutoffFrequency The cutoff frequency
     * @param rippleDb pass-band ripple in decibel sensible value: 1dB
     * @param directFormType The filter topology. This is either
     *            DirectFormAbstract.DIRECT_FORM_I or DIRECT_FORM_II
     */
    public void lowPass(final int order, final double sampleRate, final double cutoffFrequency, final double rippleDb,
            final int directFormType) {
        setupLowPass(order, sampleRate, cutoffFrequency, rippleDb, directFormType);
    }

    private void setupHighPass(final int order, final double sampleRate, final double cutoffFrequency,
            final double rippleDb, final int directFormType) {

        final AnalogLowPass analogProto = new AnalogLowPass(order);
        analogProto.design(rippleDb);

        final LayoutBase digitalProto = new LayoutBase(order);

        HighPassTransform.transform(cutoffFrequency / sampleRate, digitalProto, analogProto);

        setLayout(digitalProto, directFormType);
    }

    /**
     * ChebyshevI High-pass filter with default topology
     * 
     * @param order The order of the filter
     * @param sampleRate The sampling rate of the system
     * @param cutoffFrequency the cutoff frequency
     * @param rippleDb passband ripple in decibel sensible value: 1dB
     */
    public void highPass(final int order, final double sampleRate, final double cutoffFrequency,
            final double rippleDb) {
        setupHighPass(order, sampleRate, cutoffFrequency, rippleDb, DirectFormAbstract.DIRECT_FORM_I);
    }

    /**
     * ChebyshevI Low-pass filter and custom filter topology
     * 
     * @param order The order of the filter
     * @param sampleRate The sampling rate of the system
     * @param cutoffFrequency The cutoff frequency
     * @param rippleDb pass-band ripple in decibel sensible value: 1dB
     * @param directFormType The filter topology. This is either
     *            DirectFormAbstract.DIRECT_FORM_I or DIRECT_FORM_II
     */
    public void highPass(final int order, final double sampleRate, final double cutoffFrequency, final double rippleDb,
            final int directFormType) {
        setupHighPass(order, sampleRate, cutoffFrequency, rippleDb, directFormType);
    }

    private void setupBandStop(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final double rippleDb, final int directFormType) {

        final AnalogLowPass analogProto = new AnalogLowPass(order);
        analogProto.design(rippleDb);

        final LayoutBase digitalProto = new LayoutBase(order * 2);

        BandStopTransform.transform(centerFrequency / sampleRate, widthFrequency / sampleRate, digitalProto,
                analogProto);

        setLayout(digitalProto, directFormType);
    }

    /**
     * Band-stop filter with default topology
     * 
     * @param order Filter order (actual order is twice)
     * @param sampleRate sampling rate of the system
     * @param centerFrequency centre frequency
     * @param widthFrequency Width of the notch
     * @param rippleDb pass-band ripple in decibel sensible value: 1dB
     */
    public void bandStop(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final double rippleDb) {
        setupBandStop(order, sampleRate, centerFrequency, widthFrequency, rippleDb, DirectFormAbstract.DIRECT_FORM_I);
    }

    /**
     * Band-stop filter with custom topology
     * 
     * @param order Filter order (actual order is twice)
     * @param sampleRate Sampling rate of the system
     * @param centerFrequency centre frequency
     * @param widthFrequency Width of the notch
     * @param rippleDb pass-band ripple in decibel sensible value: 1dB
     * @param directFormType the filter topology
     */
    public void bandStop(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final double rippleDb, final int directFormType) {
        setupBandStop(order, sampleRate, centerFrequency, widthFrequency, rippleDb, directFormType);
    }

    private void setupBandPass(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final double rippleDb, final int directFormType) {

        final AnalogLowPass analogProto = new AnalogLowPass(order);
        analogProto.design(rippleDb);

        final LayoutBase digitalProto = new LayoutBase(order * 2);

        BandPassTransform.transform(centerFrequency / sampleRate, widthFrequency / sampleRate, digitalProto,
                analogProto);

        setLayout(digitalProto, directFormType);

    }

    /**
     * Bandpass filter with default topology
     * 
     * @param order Filter order
     * @param sampleRate sampling rate
     * @param centerFrequency center frequency
     * @param widthFrequency width of the notch
     * @param rippleDb pass-band ripple in decibel sensible value: 1dB
     */
    public void bandPass(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final double rippleDb) {
        setupBandPass(order, sampleRate, centerFrequency, widthFrequency, rippleDb, DirectFormAbstract.DIRECT_FORM_I);
    }

    /**
     * Bandpass filter with custom topology
     * 
     * @param order Filter order
     * @param sampleRate Sampling rate
     * @param centerFrequency center frequency
     * @param widthFrequency width of the notch
     * @param rippleDb pass-band ripple in decibel sensible value: 1dB
     * @param directFormType The filter topology (see DirectFormAbstract)
     */
    public void bandPass(final int order, final double sampleRate, final double centerFrequency,
            final double widthFrequency, final double rippleDb, final int directFormType) {
        setupBandPass(order, sampleRate, centerFrequency, widthFrequency, rippleDb, directFormType);
    }

}
