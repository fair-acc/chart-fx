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
 * A complex pair
 */
public class ComplexPair {
    public Complex first;
    public Complex second;

    public ComplexPair(final Complex c1) {
        first = c1;
        second = new Complex(0, 0);
    }

    public ComplexPair(final Complex c1, final Complex c2) {
        first = c1;
        second = c2;
    }

    public boolean isConjugate() {
        return second.equals(first.conjugate());
    }

    /**
     * @return true if this is either a conjugate pair, or a pair of reals where neither is zero.
     */
    public boolean isMatchedPair() {
        if (first.getImaginary() != 0) {
            return second.equals(first.conjugate());
        }
        return second.getImaginary() == 0 && second.getReal() != 0 && first.getReal() != 0;
    }

    public boolean isNaN() {
        return first.isNaN() || second.isNaN();
    }

    public boolean isReal() {
        return first.getImaginary() == 0 && second.getImaginary() == 0;
    }
}
