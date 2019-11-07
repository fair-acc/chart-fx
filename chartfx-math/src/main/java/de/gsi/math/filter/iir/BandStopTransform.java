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
 * Transforms from an analogue lowpass filter to a digital bandstop filter
 */
public class BandStopTransform {
    private BandStopTransform() {
    }

    private static ComplexPair transform(final Complex in, final double a, final double b) {
        Complex c;
        if (in.isInfinite()) {
            c = new Complex(-1);
        } else {
            c = new Complex(1).add(in).divide(new Complex(1).subtract(in)); // bilinear
        }

        final double a2 = a * a;
        final double b2 = b * b;

        Complex u = new Complex(0);
        u = u.add(c.multiply(4 * (b2 + a2 - 1)));
        u = u.add(8 * (b2 - a2 + 1));
        u = u.multiply(c);
        u = u.add(4 * (a2 + b2 - 1));
        u = u.sqrt();

        Complex v = u.multiply(-.5).add(a);
        v = v.add(c.multiply(-a));

        u = u.multiply(.5);
        u = u.add(a);
        u = u.add(c.multiply(-a));

        Complex d = new Complex(b + 1);
        d = d.add(c.multiply(b - 1));

        return new ComplexPair(u.divide(d), v.divide(d));
    }

    public static void transform(final double fc, final double fw, final LayoutBase digital, final LayoutBase analog) {
        double wc;
        double wc2;
        final double a;
        final double b;

        digital.reset();

        final double ww = 2 * Math.PI * fw;

        wc2 = 2 * Math.PI * fc - ww / 2;
        wc = wc2 + ww;

        // this is crap
        if (wc2 < 1e-8) {
            wc2 = 1e-8;
        }
        if (wc > Math.PI - 1e-8) {
            wc = Math.PI - 1e-8;
        }

        a = Math.cos((wc + wc2) * .5) / Math.cos((wc - wc2) * .5);
        b = Math.tan((wc - wc2) * .5);

        final int numPoles = analog.getNumPoles();
        final int pairs = numPoles / 2;
        for (int i = 0; i < pairs; i++) {
            final PoleZeroPair pair = analog.getPair(i);
            final ComplexPair p = transform(pair.poles.first, a, b);
            final ComplexPair z = transform(pair.zeros.first, a, b);
            digital.addPoleZeroConjugatePairs(p.first, z.first);
            digital.addPoleZeroConjugatePairs(p.second, z.second);
        }

        if ((numPoles & 1) == 1) {
            final ComplexPair poles = transform(analog.getPair(pairs).poles.first, a, b);
            final ComplexPair zeros = transform(analog.getPair(pairs).zeros.first, a, b);

            digital.add(poles, zeros);
        }

        if (fc < 0.25) {
            digital.setNormal(Math.PI, analog.getNormalGain());
        } else {
            digital.setNormal(0, analog.getNormalGain());
        }
    }

}
