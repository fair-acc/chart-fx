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
    private Biquad[] m_biquads;

    // the states of the filters
    private DirectFormAbstract[] m_states;

    // number of biquads in the system
    private int m_numBiquads;

    private int numPoles;

    public int getNumBiquads() {
        return m_numBiquads;
    }

    public Biquad getBiquad(final int index) {
        return m_biquads[index];
    }

    public Cascade() {
        m_numBiquads = 0;
        m_biquads = null;
        m_states = null;
    }

    public void reset() {
        for (int i = 0; i < m_numBiquads; i++) {
            m_states[i].reset();
        }
    }

    public double filter(final double in) {
        double out = in;
        for (int i = 0; i < m_numBiquads; i++) {
            if (m_states[i] != null) {
                out = m_states[i].process1(out, m_biquads[i]);
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

        for (int i = 0; i < m_numBiquads; i++) {
            final Biquad stage = m_biquads[i];
            Complex cb = new Complex(1);
            Complex ct = new Complex(stage.getB0() / stage.getA0());
            ct = MathSupplement.addmul(ct, stage.getB1() / stage.getA0(), czn1);
            ct = MathSupplement.addmul(ct, stage.getB2() / stage.getA0(), czn2);
            cb = MathSupplement.addmul(cb, stage.getA1() / stage.getA0(), czn1);
            cb = MathSupplement.addmul(cb, stage.getA2() / stage.getA0(), czn2);
            ch = ch.multiply(ct);
            cbot = cbot.multiply(cb);
        }

        return ch.divide(cbot);
    }

    public void applyScale(final double scale) {
        // For higher order filters it might be helpful
        // to spread this factor between all the stages.
        if (m_biquads.length > 0) {
            m_biquads[0].applyScale(scale);
        }
    }

    public void setLayout(final LayoutBase proto, final int filterTypes) {
        numPoles = proto.getNumPoles();
        m_numBiquads = (numPoles + 1) / 2;
        m_biquads = new Biquad[m_numBiquads];
        switch (filterTypes) {
        case DirectFormAbstract.DIRECT_FORM_I:
            m_states = new DirectFormI[m_numBiquads];
            for (int i = 0; i < m_numBiquads; i++) {
                m_states[i] = new DirectFormI();
            }
            break;
        case DirectFormAbstract.DIRECT_FORM_II:
        default:
            m_states = new DirectFormII[m_numBiquads];
            for (int i = 0; i < m_numBiquads; i++) {
                m_states[i] = new DirectFormII();
            }
            break;
        }
        for (int i = 0; i < m_numBiquads; ++i) {
            final PoleZeroPair p = proto.getPair(i);
            m_biquads[i] = new Biquad();
            m_biquads[i].setPoleZeroPair(p);
        }
        applyScale(proto.getNormalGain() / response(proto.getNormalW() / (2 * Math.PI)).abs());
    }

};
