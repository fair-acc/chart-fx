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

/**
 * Implementation of a Direct Form II filter with its states. The coefficients
 * are supplied from the outside.
 */

public class DirectFormII extends DirectFormAbstract {
    public double mV1; // v[-1]
    public double mV2; // v[-2]
    
    public DirectFormII() {
        reset();
    }

    @Override
    public final void reset() {
        mV1 = 0;
        mV2 = 0;
    }

    @Override
    public double process1(final double in, final Biquad s) {
        if (s != null) {
            final double w = in - s.mA1 * mV1 - s.mA2 * mV2;
            final double out = s.mB0 * w + s.mB1 * mV1 + s.mB2 * mV2;

            mV2 = mV1;
            mV1 = w;

            return out;
        }
        return in;
    }
}
