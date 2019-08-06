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
import org.apache.commons.math3.complex.ComplexUtils;

/**
 * The mother of all filters. It contains the coefficients of all
 * filter stages as a sequence of 2nd order filters and the states
 * of the 2nd order filters which also imply if it's direct form I or II
 */
public class Cascade {

    // coefficients
    private Biquad[] mBiquads;

    // the states of the filters
    private DirectFormAbstract[] mStates;

    // number of biquads in the system
    private int mNumBiquads;

    public int getNumBiquads() {
        return mNumBiquads;
    }

    public Biquad getBiquad(final int index) {
        return mBiquads[index];
    }

    public Cascade() {
        mNumBiquads = 0;
        mBiquads = null;
        mStates = null;
    }

    public void reset() {
        for (int i = 0; i < mNumBiquads; i++) {
            mStates[i].reset();
        }
    }

    public double filter(final double in) {
        double out = in;
        for (int i = 0; i < mNumBiquads; i++) {
            if (mStates[i] != null) {
                out = mStates[i].process1(out, mBiquads[i]);
            }
        }
        return out;
    }

    public Complex response(final double normalizedFrequency) {
        final double w = 2 * Math.PI * normalizedFrequency;
        final Complex czn1 = ComplexUtils.polar2Complex(1., -w);
        final Complex czn2 = ComplexUtils.polar2Complex(1., -2 * w);
        Complex ch = new Complex(1);
        Complex cbot = new Complex(1);

        for (int i = 0; i < mNumBiquads; i++) {
            final Biquad stage = mBiquads[i];

            Complex ct = new Complex(stage.getB0() / stage.getA0());
            ct = ct.add(czn1.multiply(stage.getB1() / stage.getA0()));
            ct = ct.add(czn2.multiply(stage.getB2() / stage.getA0()));

            Complex cb = new Complex(1);
            cb = cb.add(czn1.multiply(stage.getA1() / stage.getA0()));
            cb = cb.add(czn2.multiply(stage.getA2() / stage.getA0()));

            ch = ch.multiply(ct);
            cbot = cbot.multiply(cb);
        }

        return ch.divide(cbot);
    }

    public void applyScale(final double scale) {
        // For higher order filters it might be helpful
        // to spread this factor between all the stages.
        if (mBiquads.length > 0) {
            mBiquads[0].applyScale(scale);
        }
    }

    public void setLayout(final LayoutBase proto, final int filterTypes) {
        final int numPoles = proto.getNumPoles();
        mNumBiquads = (numPoles + 1) / 2;
        mBiquads = new Biquad[mNumBiquads];
        switch (filterTypes) {
        case DirectFormAbstract.DIRECT_FORM_I:
            mStates = new DirectFormI[mNumBiquads];
            for (int i = 0; i < mNumBiquads; i++) {
                mStates[i] = new DirectFormI();
            }
            break;
        case DirectFormAbstract.DIRECT_FORM_II:
        default:
            mStates = new DirectFormII[mNumBiquads];
            for (int i = 0; i < mNumBiquads; i++) {
                mStates[i] = new DirectFormII();
            }
            break;
        }
        for (int i = 0; i < mNumBiquads; ++i) {
            final PoleZeroPair p = proto.getPair(i);
            mBiquads[i] = new Biquad();
            mBiquads[i].setPoleZeroPair(p);
        }
        applyScale(proto.getNormalGain() / response(proto.getNormalW() / (2 * Math.PI)).abs());
    }
}
