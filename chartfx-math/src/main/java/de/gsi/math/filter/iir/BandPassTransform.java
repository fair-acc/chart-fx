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
 * Transforms from an analogue bandpass filter to a digital band-stop filter
 */
public final class BandPassTransform { // NOPMD - nomen est omen

    private BandPassTransform() {
        // utility class
    }

    public static void transform(final double fc, final double fw, final LayoutBase digital, final LayoutBase analog) {
        double wc2;
        double wc;
        final double a;
        final double b;

        digital.reset();

        final double ww = 2 * Math.PI * fw;

        // pre-calcs
        wc2 = 2 * Math.PI * fc - ww / 2;
        wc = wc2 + ww;

        // what is this crap?
        if (wc2 < 1e-8) { // NOPMD - needed apparently for numeric stability
            wc2 = 1e-8;
        }
        if (wc > Math.PI - 1e-8) {
            wc = Math.PI - 1e-8;
        }

        a = Math.cos((wc + wc2) * 0.5) / Math.cos((wc - wc2) * 0.5);
        b = 1 / Math.tan((wc - wc2) * 0.5);

        final int numPoles = analog.getNumPoles();
        final int pairs = numPoles / 2;
        for (int i = 0; i < pairs; ++i) {
            final PoleZeroPair pair = analog.getPair(i);
            final ComplexPair p1 = transform(pair.poles.first, a, b);
            final ComplexPair z1 = transform(pair.zeros.first, a, b);

            digital.addPoleZeroConjugatePairs(p1.first, z1.first);
            digital.addPoleZeroConjugatePairs(p1.second, z1.second);
        }

        if ((numPoles & 1) == 1) {
            final ComplexPair poles = transform(analog.getPair(pairs).poles.first, a, b);
            final ComplexPair zeros = transform(analog.getPair(pairs).zeros.first, a, b);

            digital.add(poles, zeros);
        }

        final double wn = analog.getNormalW();
        digital.setNormal(2 * Math.atan(Math.sqrt(Math.tan((wc + wn) * 0.5) * Math.tan((wc2 + wn) * 0.5))),
                analog.getNormalGain());
    }

    private static ComplexPair transform(final Complex in, final double a, final double b) {
        if (in.isInfinite()) {
            return new ComplexPair(new Complex(-1), new Complex(1));
        }

        final Complex c = new Complex(1).add(in).divide(new Complex(1).subtract(in)); // bilinear

        final double a2 = a * a;
        final double b2 = b * b;
        final double ab = a * b;
        final double ab2 = 2 * ab;
        Complex v = new Complex(0).add(c.multiply(4 * (b2 * (a2 - 1) + 1)));
        v = v.add(8 * (b2 * (a2 - 1) - 1));
        v = v.multiply(c);
        v = v.add(4 * (b2 * (a2 - 1) + 1));
        v = v.sqrt();

        final Complex u = v.multiply(-1).add(c.multiply(ab2)).add(ab2);

        v = v.add(c.multiply(ab2)).add(ab2);

        final Complex d = new Complex(0).add(c.multiply(2 * (b - 1))).add(2 * (1 + b));

        return new ComplexPair(u.divide(d), v.divide(d));
    }
}
