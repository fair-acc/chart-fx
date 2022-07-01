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

package io.fair_acc.math.filter.iir;

/**
 * Abstract form of the a filter which can have different state variables Direct form I or II is derived from it
 */
public abstract class DirectFormAbstract {

    public static final int DIRECT_FORM_I = 0;

    public static final int DIRECT_FORM_II = 1;

    public DirectFormAbstract() {
        reset();
    }

    public abstract double process1(double in, Biquad s);

    public abstract void reset();

}
