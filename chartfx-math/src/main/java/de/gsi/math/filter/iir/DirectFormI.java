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
 * Implementation of a Direct Form I filter with its states. The coefficients are supplied from the outside.
 */
public class DirectFormI extends DirectFormAbstract {
    public double mX2; // x[n-2]
    public double mY2; // y[n-2]
    public double mX1; // x[n-1]
    public double mY1; // y[n-1]

    @Override
    public double process1(final double input, final Biquad s) {
        final double output = s.mB0 * input + s.mB1 * mX1 + s.mB2 * mX2 - s.mA1 * mY1 - s.mA2 * mY2;
        mX2 = mX1;
        mY2 = mY1;
        mX1 = input;
        mY1 = output;

        return output;
    }

    @Override
    public final void reset() {
        mX1 = 0;
        mX2 = 0;
        mY1 = 0;
        mY2 = 0;
    }
}
