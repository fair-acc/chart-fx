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
 * Transforms from an analogue low-pass filter to a digital high-pass filter
 */
public class HighPassTransform {

    double f;

    public HighPassTransform(final double fc, final LayoutBase digital, final LayoutBase analog) {
        digital.reset();

        // pre-warp
        f = 1. / Math.tan(Math.PI * fc);

        final int numPoles = analog.getNumPoles();
        final int pairs = numPoles / 2;
        for (int i = 0; i < pairs; ++i) {
            final PoleZeroPair pair = analog.getPair(i);
            digital.addPoleZeroConjugatePairs(transform(pair.poles.first), transform(pair.zeros.first));
        }

        if ((numPoles & 1) == 1) {
            final PoleZeroPair pair = analog.getPair(pairs);
            digital.add(transform(pair.poles.first), transform(pair.zeros.first));
        }

        digital.setNormal(Math.PI - analog.getNormalW(), analog.getNormalGain());
    }

    private Complex transform(Complex c) {
        if (c.isInfinite()) {
            return new Complex(1, 0);
        }

        // frequency transform
        c = c.multiply(f);

        // bilinear high pass transform
        return new Complex(-1).multiply(new Complex(1).add(c)).divide(new Complex(1).subtract(c));
    }

}
