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
 * Implementation of a Direct Form I filter with its states. The coefficients
 * are supplied from the outside.
 */
public class DirectFormI extends DirectFormAbstract {

    public DirectFormI() {
        reset();
    }

    @Override
    public void reset() {
        m_x1 = 0;
        m_x2 = 0;
        m_y1 = 0;
        m_y2 = 0;
    }

    @Override
    public double process1(final double in, final Biquad s) {

        final double out = s.m_b0 * in + s.m_b1 * m_x1 + s.m_b2 * m_x2 - s.m_a1 * m_y1 - s.m_a2 * m_y2;
        m_x2 = m_x1;
        m_y2 = m_y1;
        m_x1 = in;
        m_y1 = out;

        return out;
    }

    double m_x2; // x[n-2]
    double m_y2; // y[n-2]
    double m_x1; // x[n-1]
    double m_y1; // y[n-1]
};
