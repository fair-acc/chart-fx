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

/**
 * Transforms from an analogue bandpass filter to a digital bandstop filter
 */
public class BandPassTransform {

    private double wc2;
    private double wc;
    private final double a, b;
    private final double a2, b2;
    private final double ab, ab_2;

    public BandPassTransform(final double fc, final double fw, final LayoutBase digital, final LayoutBase analog) {

        digital.reset();

        final double ww = 2 * Math.PI * fw;

        // pre-calcs
        wc2 = 2 * Math.PI * fc - ww / 2;
        wc = wc2 + ww;

        // what is this crap?
        if (wc2 < 1e-8) {
            wc2 = 1e-8;
        }
        if (wc > Math.PI - 1e-8) {
            wc = Math.PI - 1e-8;
        }

        a = Math.cos((wc + wc2) * 0.5) / Math.cos((wc - wc2) * 0.5);
        b = 1 / Math.tan((wc - wc2) * 0.5);
        a2 = a * a;
        b2 = b * b;
        ab = a * b;
        ab_2 = 2 * ab;

        final int numPoles = analog.getNumPoles();
        final int pairs = numPoles / 2;
        for (int i = 0; i < pairs; ++i) {
            final PoleZeroPair pair = analog.getPair(i);
            final ComplexPair p1 = transform(pair.poles.first);
            final ComplexPair z1 = transform(pair.zeros.first);

            digital.addPoleZeroConjugatePairs(p1.first, z1.first);
            digital.addPoleZeroConjugatePairs(p1.second, z1.second);
        }

        if ((numPoles & 1) == 1) {
            final ComplexPair poles = transform(analog.getPair(pairs).poles.first);
            final ComplexPair zeros = transform(analog.getPair(pairs).zeros.first);

            digital.add(poles, zeros);
        }

        final double wn = analog.getNormalW();
        digital.setNormal(2 * Math.atan(Math.sqrt(Math.tan((wc + wn) * 0.5) * Math.tan((wc2 + wn) * 0.5))),
                analog.getNormalGain());
    }

    private ComplexPair transform(Complex c) {
        if (c.isInfinite()) {
            return new ComplexPair(new Complex(-1), new Complex(1));
        }

        c = new Complex(1).add(c).divide(new Complex(1).subtract(c)); // bilinear

        Complex v = new Complex(0);
        v = MathSupplement.addmul(v, 4 * (b2 * (a2 - 1) + 1), c);
        v = v.add(8 * (b2 * (a2 - 1) - 1));
        v = v.multiply(c);
        v = v.add(4 * (b2 * (a2 - 1) + 1));
        v = v.sqrt();

        Complex u = v.multiply(-1);
        u = MathSupplement.addmul(u, ab_2, c);
        u = u.add(ab_2);

        v = MathSupplement.addmul(v, ab_2, c);
        v = v.add(ab_2);

        Complex d = new Complex(0);
        d = MathSupplement.addmul(d, 2 * (b - 1), c).add(2 * (1 + b));

        return new ComplexPair(u.divide(d), v.divide(d));
    }

}
