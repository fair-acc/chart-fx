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
 * Digital/analogue filter coefficient storage space organising the storage as PoleZeroPairs so that we have as always a
 * 2nd order filter
 */
public class LayoutBase {
    private int mNumPoles;
    private PoleZeroPair[] mPair;
    private double mNormalW;
    private double mNormalGain;

    public LayoutBase(final int numPoles) {
        mNumPoles = 0;
        if (numPoles % 2 == 1) {
            mPair = new PoleZeroPair[numPoles / 2 + 1];
        } else {
            mPair = new PoleZeroPair[numPoles / 2];
        }
    }

    public LayoutBase(final PoleZeroPair[] pairs) { // NOPMD
        mNumPoles = pairs.length * 2;
        mPair = pairs;
    }

    public void add(final Complex pole, final Complex zero) {
        mPair[mNumPoles / 2] = new PoleZeroPair(pole, zero);
        ++mNumPoles;
    }

    public void add(final ComplexPair poles, final ComplexPair zeros) {
        mPair[mNumPoles / 2] = new PoleZeroPair(poles.first, zeros.first, poles.second, zeros.second);
        mNumPoles += 2;
    }

    public void addPoleZeroConjugatePairs(final Complex pole, final Complex zero) {
        if (pole == null) {
            throw new IllegalArgumentException("LayoutBase addConj() pole == null");
        }
        if (zero == null) {
            throw new IllegalArgumentException("LayoutBase addConj() zero == null");
        }
        if (mPair == null) {
            throw new IllegalArgumentException("LayoutBase addConj() m_pair == null");
        }
        mPair[mNumPoles / 2] = new PoleZeroPair(pole, zero, pole.conjugate(), zero.conjugate());
        mNumPoles += 2;
    }

    public double getNormalGain() {
        return mNormalGain;
    }

    public double getNormalW() {
        return mNormalW;
    }

    public int getNumPoles() {
        return mNumPoles;
    }

    public PoleZeroPair getPair(final int pairIndex) {
        return mPair[pairIndex];
    }

    public void reset() {
        mNumPoles = 0;
    }

    public void setNormal(final double w, final double g) {
        mNormalW = w;
        mNormalGain = g;
    }
}
