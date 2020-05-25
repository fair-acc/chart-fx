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
 * Transforms from an analogue lowpass filter to a digital lowpass filter
 */
public final class LowPassTransform { // NOPMD - nomen est omen

    private LowPassTransform() {
        // utility class
    }

    private static Complex transform(final Complex in, final double f) {
        if (in.isInfinite()) {
            return new Complex(-1, 0);
        }
        Complex c;
        // frequency transform
        c = in.multiply(f);

        final Complex one = new Complex(1, 0);

        // bilinear low pass transform
        return one.add(c).divide(one.subtract(c));
    }

    public static void transform(final double fc, final LayoutBase digital, final LayoutBase analog) {
        final double f;
        digital.reset();

        // pre-warp
        f = Math.tan(Math.PI * fc);

        final int numPoles = analog.getNumPoles();
        final int pairs = numPoles / 2;
        for (int i = 0; i < pairs; ++i) {
            final PoleZeroPair pair = analog.getPair(i);
            digital.addPoleZeroConjugatePairs(transform(pair.poles.first, f), transform(pair.zeros.first, f));
        }

        if ((numPoles & 1) == 1) {
            final PoleZeroPair pair = analog.getPair(pairs);
            digital.add(transform(pair.poles.first, f), transform(pair.zeros.first, f));
        }

        digital.setNormal(analog.getNormalW(), analog.getNormalGain());
    }
}
