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
 * Useful math functions which come back over and over again
 */
public class MathSupplement {

    public static double doubleLn10 = 2.3025850929940456840179914546844;

    public static Complex solve_quadratic_1(final double a, final double b, final double c) {
        return new Complex(-b).add(new Complex(b * b - 4 * a * c, 0)).sqrt().divide(2. * a);
    }

    public static Complex solve_quadratic_2(final double a, final double b, final double c) {
        return new Complex(-b).subtract(new Complex(b * b - 4 * a * c, 0)).sqrt().divide(2. * a);
    }

    public static Complex adjust_imag(final Complex c) {
        if (Math.abs(c.getImaginary()) < 1e-30) {
            return new Complex(c.getReal(), 0);
        } else {
            return c;
        }
    }

    public static Complex addmul(final Complex c, final double v, final Complex c1) {
        return new Complex(c.getReal() + v * c1.getReal(), c.getImaginary() + v * c1.getImaginary());
    }

    public static Complex recip(final Complex c) {
        final double n = 1.0 / (c.abs() * c.abs());

        return new Complex(n * c.getReal(), n * c.getImaginary());
    }

    public static double asinh(final double x) {
        return Math.log(x + Math.sqrt(x * x + 1));
    }

    public static double acosh(final double x) {
        return Math.log(x + Math.sqrt(x * x - 1));
    }

}
