/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2023 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/*
 * Copyright 2018-2020 Raffaello Giulietti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.fair_acc.chartfx.utils;

import static io.fair_acc.chartfx.utils.Schubfach.MathUtils.*;
import static java.lang.Double.*;
import static java.lang.Float.*;
import static java.lang.Math.*;

/**
 * This class exposes methods to decompose a {@double} or {@float} into
 * its significand and exponent parts.
 * <p>
 * The Schubfach algorithm was created by Raffaelo Giuletti. Besides isolating
 * the decomposition part, the algorithm was not changed in any way. The paper
 * and sources with license can be found at the following links:
 * <p>
 * <a href="https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view">"The Schubfach way to render doubles"</a>
 * <a href="https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/FloatToDecimal.java">...</a>
 * <p>
 * This work is a refactored version of Raffaello Giulietti's.
 *
 * @author Raffaello Giulietti
 * @author Florian Enner (copied from QuickBuffers)
 */
public class Schubfach {
      /*
    For full details about this code see the following references:

    [1] Giulietti, "The Schubfach way to render doubles",
        https://drive.google.com/open?id=1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN

    [2] IEEE Computer Society, "IEEE Standard for Floating-Point Arithmetic"

    [3] Bouvier & Zimmermann, "Division-Free Binary-to-Decimal Conversion"

    Divisions are avoided altogether for the benefit of those architectures
    that do not provide specific machine instructions or where they are slow.
    This is discussed in section 10 of [1].
     */

    public static final int NON_SPECIAL = 0;
    public static final int PLUS_ZERO = 1;
    public static final int MINUS_ZERO = 2;
    public static final int PLUS_INF = 3;
    public static final int MINUS_INF = 4;
    public static final int NAN = 5;
    public static final int H_FLOAT = 9; // H is as in section 8 of [1].
    public static final int H_DOUBLE = 17; // H is as in section 8 of [1].

    /**
     * Encodes a floating point number v as decimal values f,e where v = f * 10^e
     *
     * @param v       input value
     * @param encoder Encoder for the result. Only gets called for non-special numbers
     * @return NON_SPECIAL     any non-special number
     * PLUS_ZERO       iff v is 0.0
     * MINUS_ZERO      iff v is -0.0
     * PLUS_INF        iff v is POSITIVE_INFINITY
     * MINUS_INF       iff v is NEGATIVE_INFINITY
     * NAN             iff v is NaN
     */
    public static int encodeFloat(float v, FloatEncoder encoder) {
        return FloatToDecimal.encode(v, encoder);
    }

    /*
         For details not discussed here see section 10 of [1].
         Determine length such that
           10^(len-1) <= f < 10^len
         */
    public static int getDecimalLength(int f) {
        int len = flog10pow2(Integer.SIZE - Integer.numberOfLeadingZeros(f));
        if (f >= pow10(len)) {
            len += 1;
        }
        return len;
    }

    /*
         For details not discussed here see section 10 of [1].
         Determine length such that
           10^(len-1) <= f < 10^len
         */
    public static int getDecimalLength(long f) {
        int len = flog10pow2(Long.SIZE - Long.numberOfLeadingZeros(f));
        if (f >= pow10(len)) {
            len += 1;
        }
        return len;
    }

    /**
     * @param len length of the significand
     * @return scalar needed to normalize the significand to always have H digits
     */
    public static long getNormalizationScale(int H, int len) {
        return pow10(H - len);
    }

    public interface FloatEncoder {
        void encodeFloat(boolean negative, int significand, int exponent);
    }

    /**
     * Encodes a floating point number v as decimal values f,e where v = f * 10^e
     *
     * @param v       input value
     * @param encoder Encoder for the result. Only gets called for non-special numbers
     * @return NON_SPECIAL     any non-special number
     * PLUS_ZERO       iff v is 0.0
     * MINUS_ZERO      iff v is -0.0
     * PLUS_INF        iff v is POSITIVE_INFINITY
     * MINUS_INF       iff v is NEGATIVE_INFINITY
     * NAN             iff v is NaN
     */
    public static int encodeDouble(double v, DoubleEncoder encoder) {
        return DoubleToDecimal.encode(v, encoder);
    }

    public interface DoubleEncoder {
        void encodeDouble(boolean negative, long significand, int exponent);
    }

    public static DecomposedDouble decomposeDouble(double v) {
        return decomposeDouble(v, new DecomposedDouble());
    }

    public static DecomposedDouble decomposeDouble(double v, DecomposedDouble result) {
        result.zero();
        result.type = encodeDouble(v, result::setResult);
        return result;
    }

    public static class DecomposedDouble {

        public DecomposedDouble zero() {
            type = NON_SPECIAL;
            negative = false;
            f = 0;
            exp = 0;
            return this;
        }

        public DecomposedDouble shiftExponentTo(int exp) {
            int shift = exp - this.exp;
            if (shift == 0) {
                return this;
            } else if (shift > 0 && shift < MathUtils.pow10.length) {
                this.f /= pow10(shift);
                this.exp += shift;
                return this;
            } else {
                throw new IllegalArgumentException("Can't shift to a smaller exponent or by more than 10^17");
            }
        }

        void setResult(boolean negative, long f, int e) {
            // Normalize result to populate H(17) digits
            final int len = getDecimalLength(f);
            this.f = f * getNormalizationScale(H_DOUBLE, len);
            this.exp = e + len;
            this.negative = negative;
        }

        @Override
        public String toString() {
            return "DecomposedDouble{" +
                    "type=" + type +
                    ", negative=" + negative +
                    ", significand=" + f +
                    ", exponent=" + exp +
                    '}';
        }

        public int getType() {
            return type;
        }

        public boolean isFinite() {
            switch (type) {
                case NON_SPECIAL:
                case PLUS_ZERO:
                case MINUS_ZERO:
                    return true;
            }
            return false;
        }

        public boolean isNegative() {
            return negative;
        }

        public long getSignificand() {
            return f;
        }

        public int getExponent() {
            return exp;
        }

        int type;
        boolean negative;
        long f;
        int exp;
    }

    public static long getRoundingOffset(int maxSignificantDigits) {
        if (maxSignificantDigits < 0 || maxSignificantDigits > 17) {
            return 0;
        }
        return ROUNDING_OFFSET[maxSignificantDigits];
    }

    /**
     * Offset map for rounding up/down to the desired precision
     */
    private static long[] ROUNDING_OFFSET = new long[]{
            50000000000000000L,
            5000000000000000L,
            500000000000000L,
            50000000000000L,
            5000000000000L,
            500000000000L,
            50000000000L,
            5000000000L,
            500000000L,
            50000000L,
            5000000L,
            500000L,
            50000L,
            5000L,
            500L,
            50L,
            5L,
            0L
    };

    static final class FloatToDecimal {

        private static final int P = 24; // The precision in bits.
        private static final int W = (Float.SIZE - 1) - (P - 1); // Exponent width in bits.
        private static final int Q_MIN = (-1 << W - 1) - P + 3;  // Minimum value of the exponent: -(2^(W-1)) - P + 3.
        private static final int Q_MAX = (1 << W - 1) - P; // Maximum value of the exponent: 2^(W-1) - P.
        private static final int E_MIN = -44; // 10^(E_MIN - 1) <= MIN_VALUE < 10^E_MIN
        private static final int E_MAX = 39; // 10^(E_MAX - 1) <= MAX_VALUE < 10^E_MAX
        private static final int C_TINY = 8; // Threshold to detect tiny values, as in section 8.1.1 of [1]
        private static final int K_MIN = -45; // The minimum and maximum k, as in section 8 of [1]
        private static final int K_MAX = 31;
        private static final int C_MIN = 1 << P - 1; // Minimum value of the significand of a normal value: 2^(P-1).
        private static final int BQ_MASK = (1 << W) - 1; // Mask to extract the biased exponent.
        private static final int T_MASK = (1 << P - 1) - 1; // Mask to extract the fraction bits.
        private static final long MASK_32 = (1L << 32) - 1; // Used in rop().

        private FloatToDecimal() {
        }

        private static int encode(float v, FloatEncoder encoder) {
            int bits = floatToRawIntBits(v);
            int t = bits & T_MASK;
            int bq = (bits >>> P - 1) & BQ_MASK;
            if (bq < BQ_MASK) {
                final boolean negative = bits < 0;
                if (bq != 0) {
                    // normal value. Here mq = -q
                    int mq = -Q_MIN + 1 - bq;
                    int c = C_MIN | t;
                    // The fast path discussed in section 8.2 of [1].
                    if (0 < mq & mq < P) {
                        int f = c >> mq;
                        if (f << mq == c) {
                            return encodeDecimal(negative, f, 0, encoder);
                        }
                    }
                    return toDecimal(negative, -mq, c, 0, encoder);
                }
                if (t != 0) {
                    // subnormal value
                    return t < C_TINY
                            ? toDecimal(negative, Q_MIN, 10 * t, -1, encoder)
                            : toDecimal(negative, Q_MIN, t, 0, encoder);
                }
                return bits == 0 ? PLUS_ZERO : MINUS_ZERO;
            }
            if (t != 0) {
                return NAN;
            }
            return bits > 0 ? PLUS_INF : MINUS_INF;
        }

        private static int toDecimal(boolean negative, int q, int c, int dk, FloatEncoder consumer) {
            int out = c & 0x1;
            long cb = c << 2;
            long cbr = cb + 2;
            long cbl;
            int k;
            if (c != C_MIN | q == Q_MIN) {
                // regular spacing
                cbl = cb - 2;
                k = flog10pow2(q);
            } else {
                // irregular spacing0
                cbl = cb - 1;
                k = flog10threeQuartersPow2(q);
            }
            int h = q + flog2pow10(-k) + 33;

            // g is as in the appendix
            long g = g1(k) + 1;

            int vb = rop(g, cb << h);
            int vbl = rop(g, cbl << h);
            int vbr = rop(g, cbr << h);

            int s = vb >> 2;
            if (s >= 100) {
                int sp10 = 10 * (int) (s * 1717986919L >>> 34);
                int tp10 = sp10 + 10;
                boolean upin = vbl + out <= sp10 << 2;
                boolean wpin = (tp10 << 2) + out <= vbr;
                if (upin != wpin) {
                    return encodeDecimal(negative, upin ? sp10 : tp10, k, consumer);
                }
            }

            int t = s + 1;
            boolean uin = vbl + out <= s << 2;
            boolean win = (t << 2) + out <= vbr;
            if (uin != win) {
                // Exactly one of u or w lies in Rv.
                return encodeDecimal(negative, uin ? s : t, k + dk, consumer);
            }
            int cmp = vb - (s + t << 1);
            return encodeDecimal(negative, cmp < 0 || cmp == 0 && (s & 0x1) == 0 ? s : t, k + dk, consumer);
        }

        private static int rop(long g, long cp) {
            long x1 = multiplyHigh(g, cp);
            long vbp = x1 >>> 31;
            return (int) (vbp | (x1 & MASK_32) + MASK_32 >>> 32);
        }

        private static int encodeDecimal(boolean negative, int f, int e, FloatEncoder encoder) {
            encoder.encodeFloat(negative, f, e);
            return NON_SPECIAL;
        }

    }

    static final public class DoubleToDecimal {
        static final int P = 53; // The precision in bits.
        private static final int W = (Double.SIZE - 1) - (P - 1); // Exponent width in bits.
        private static final int Q_MIN = (-1 << W - 1) - P + 3; // Minimum value of the exponent: -(2^(W-1)) - P + 3.
        private static final int Q_MAX = (1 << W - 1) - P; // Maximum value of the exponent: 2^(W-1) - P.
        private static final int E_MIN = -323; // 10^(E_MIN - 1) <= MIN_VALUE < 10^E_MIN
        private static final int E_MAX = 309; // 10^(E_MAX - 1) <= MAX_VALUE < 10^E_MAX
        private static final long C_TINY = 3; // Threshold to detect tiny values, as in section 8.1.1 of [1]
        private static final int K_MIN = -324; // The minimum and maximum k, as in section 8 of [1]
        private static final int K_MAX = 292;
        private static final long C_MIN = 1L << P - 1; // Minimum value of the significand of a normal value: 2^(P-1).
        private static final int BQ_MASK = (1 << W) - 1; // Mask to extract the biased exponent.
        private static final long T_MASK = (1L << P - 1) - 1; // Mask to extract the fraction bits.
        private static final long MASK_63 = (1L << 63) - 1; // Used in rop().

        private DoubleToDecimal() {
        }

        static int encode(double v, DoubleEncoder encoder) {
            long bits = doubleToRawLongBits(v);
            long t = bits & T_MASK;
            int bq = (int) (bits >>> P - 1) & BQ_MASK;
            if (bq < BQ_MASK) {
                boolean negative = bits < 0;
                if (bq != 0) {
                    // normal value. Here mq = -q
                    int mq = -Q_MIN + 1 - bq;
                    long c = C_MIN | t;
                    // The fast path discussed in section 8.2 of [1].
                    if (0 < mq & mq < P) {
                        long f = c >> mq;
                        if (f << mq == c) {
                            return encodeDecimal(negative, f, 0, encoder);
                        }
                    }
                    return toDecimal(negative, -mq, c, 0, encoder);
                }
                if (t != 0) {
                    // subnormal value
                    return t < C_TINY
                            ? toDecimal(negative, Q_MIN, 10 * t, -1, encoder)
                            : toDecimal(negative, Q_MIN, t, 0, encoder);
                }
                return bits == 0 ? PLUS_ZERO : MINUS_ZERO;
            }
            if (t != 0) {
                return NAN;
            }
            return bits > 0 ? PLUS_INF : MINUS_INF;
        }

        private static int toDecimal(boolean negative, int q, long c, int dk, DoubleEncoder encoder) {
            int out = (int) c & 0x1;
            long cb = c << 2;
            long cbr = cb + 2;
            long cbl;
            int k;
            if (c != C_MIN | q == Q_MIN) {
                // regular spacing
                cbl = cb - 2;
                k = flog10pow2(q);
            } else {
                // irregular spacing
                cbl = cb - 1;
                k = flog10threeQuartersPow2(q);
            }
            int h = q + flog2pow10(-k) + 2;

            // g1 and g0 are as in section 9.9.3 of [1], so g = g1 2^63 + g0
            long g1 = g1(k);
            long g0 = g0(k);

            long vb = rop(g1, g0, cb << h);
            long vbl = rop(g1, g0, cbl << h);
            long vbr = rop(g1, g0, cbr << h);

            long s = vb >> 2;
            if (s >= 100) {
                long sp10 = 10 * multiplyHigh(s, 115292150460684698L << 4);
                long tp10 = sp10 + 10;
                boolean upin = vbl + out <= sp10 << 2;
                boolean wpin = (tp10 << 2) + out <= vbr;
                if (upin != wpin) {
                    return encodeDecimal(negative, upin ? sp10 : tp10, k, encoder);
                }
            }

            long t = s + 1;
            boolean uin = vbl + out <= s << 2;
            boolean win = (t << 2) + out <= vbr;
            if (uin != win) {
                // Exactly one of u or w lies in Rv.
                return encodeDecimal(negative, uin ? s : t, k + dk, encoder);
            }
            long cmp = vb - (s + t << 1);
            return encodeDecimal(negative, cmp < 0 || cmp == 0 && (s & 0x1) == 0 ? s : t, k + dk, encoder);
        }

        private static long rop(long g1, long g0, long cp) {
            long x1 = multiplyHigh(g0, cp);
            long y0 = g1 * cp;
            long y1 = multiplyHigh(g1, cp);
            long z = (y0 >>> 1) + x1;
            long vbp = y1 + (z >>> 63);
            return vbp | (z & MASK_63) + MASK_63 >>> 63;
        }

        private static int encodeDecimal(boolean negative, long f, int e, DoubleEncoder encoder) {
            encoder.encodeDouble(negative, f, e);
            return NON_SPECIAL;
        }

    }

    static final class MathUtils {
        /*
        The boundaries for k in g0(int) and g1(int).
        K_MIN must be DoubleToDecimal.K_MIN or less.
        K_MAX must be DoubleToDecimal.K_MAX or more.
         */
        private static final int K_MIN = -324;
        private static final int K_MAX = 292;

        // Must be DoubleToDecimal.H or more
        private static final int H = 17;

        // C_10 = floor(log10(2) * 2^Q_10), A_10 = floor(log10(3/4) * 2^Q_10)
        private static final int Q_10 = 41;
        private static final long C_10 = 661971961083L;
        private static final long A_10 = -274743187321L;

        // C_2 = floor(log2(10) * 2^Q_2)
        private static final int Q_2 = 38;
        private static final long C_2 = 913124641741L;

        private MathUtils() {
        }

        // The first powers of 10. The last entry must be 10^H.
        private static final long[] pow10 = {
                1L,
                10L,
                100L,
                1000L,
                10000L,
                100000L,
                1000000L,
                10000000L,
                100000000L,
                1000000000L,
                10000000000L,
                100000000000L,
                1000000000000L,
                10000000000000L,
                100000000000000L,
                1000000000000000L,
                10000000000000000L,
                100000000000000000L,
        };

        static long pow10(int e) {
            return pow10[e];
        }

        static int flog10pow2(int e) {
            return (int) (e * C_10 >> Q_10);
        }

        static int flog10threeQuartersPow2(int e) {
            return (int) (e * C_10 + A_10 >> Q_10);
        }

        static int flog2pow10(int e) {
            return (int) (e * C_2 >> Q_2);
        }

        static long g1(int k) {
            return g[k - K_MIN << 1];
        }

        static long g0(int k) {
            return g[k - K_MIN << 1 | 1];
        }

        private static final long[] g = {
                /* -324 */ 0x4F0CEDC95A718DD4L, 0x5B01E8B09AA0D1B5L,
                /* -323 */ 0x7E7B160EF71C1621L, 0x119CA780F767B5EEL,
                /* -322 */ 0x652F44D8C5B011B4L, 0x0E16EC672C52F7F2L,
                /* -321 */ 0x50F29D7A37C00E29L, 0x581256B8F0425FF5L,
                /* -320 */ 0x40C21794F96671BAL, 0x79A84560C0351991L,
                /* -319 */ 0x679CF287F570B5F7L, 0x75DA089ACD21C281L,
                /* -318 */ 0x52E3F5399126F7F9L, 0x44AE6D48A41B0201L,
                /* -317 */ 0x424FF76140EBF994L, 0x36F1F106E9AF34CDL,
                /* -316 */ 0x6A198BCECE465C20L, 0x57E981A4A918547BL,
                /* -315 */ 0x54E13CA571D1E34DL, 0x2CBACE1D541376C9L,
                /* -314 */ 0x43E763B78E4182A4L, 0x23C8A4E44342C56EL,
                /* -313 */ 0x6CA56C58E39C043AL, 0x060DD4A06B9E08B0L,
                /* -312 */ 0x56EABD13E9499CFBL, 0x1E7176E6BC7E6D59L,
                /* -311 */ 0x458897432107B0C8L, 0x7EC12BEBC9FEBDE1L,
                /* -310 */ 0x6F40F20501A5E7A7L, 0x7E01DFDFA9979635L,
                /* -309 */ 0x5900C19D9AEB1FB9L, 0x4B34B319547944F7L,
                /* -308 */ 0x4733CE17AF227FC7L, 0x55C3C27AA9FA9D93L,
                /* -307 */ 0x71EC7CF2B1D0CC72L, 0x560603F7765DC8EAL,
                /* -306 */ 0x5B2397288E40A38EL, 0x7804CFF92B7E3A55L,
                /* -305 */ 0x48E945BA0B66E93FL, 0x13370CC755FE9511L,
                /* -304 */ 0x74A86F90123E41FEL, 0x51F1AE0BBCCA881BL,
                /* -303 */ 0x5D538C7341CB67FEL, 0x74C1580963D539AFL,
                /* -302 */ 0x4AA93D29016F8665L, 0x43CDE0078310FAF3L,
                /* -301 */ 0x77752EA8024C0A3CL, 0x0616333F381B2B1EL,
                /* -300 */ 0x5F90F22001D66E96L, 0x3811C298F9AF55B1L,
                /* -299 */ 0x4C73F4E667DEBEDEL, 0x600E35472E25DE28L,
                /* -298 */ 0x7A532170A6313164L, 0x3349EED849D6303FL,
                /* -297 */ 0x61DC1AC084F42783L, 0x42A18BE03B11C033L,
                /* -296 */ 0x4E49AF006A5CEC69L, 0x1BB46FE695A7CCF5L,
                /* -295 */ 0x7D42B19A43C7E0A8L, 0x2C53E63DBC3FAE55L,
                /* -294 */ 0x64355AE1CFD31A20L, 0x237651CAFCFFBEAAL,
                /* -293 */ 0x502AAF1B0CA8E1B3L, 0x35F8416F30CC9888L,
                /* -292 */ 0x402225AF3D53E7C2L, 0x5E603458F3D6E06DL,
                /* -291 */ 0x669D0918621FD937L, 0x4A3386F4B957CD7BL,
                /* -290 */ 0x52173A79E8197A92L, 0x6E8F9F2A2DDFD796L,
                /* -289 */ 0x41AC2EC7ECE12EDBL, 0x720C7F54F17FDFABL,
                /* -288 */ 0x69137E0CAE3517C6L, 0x1CE0CBBB1BFFCC45L,
                /* -287 */ 0x540F980A24F74638L, 0x171A3C95AFFFD69EL,
                /* -286 */ 0x433FACD4EA5F6B60L, 0x127B63AAF3331218L,
                /* -285 */ 0x6B991487DD657899L, 0x6A5F05DE51EB5026L,
                /* -284 */ 0x5614106CB11DFA14L, 0x5518D17EA7EF7352L,
                /* -283 */ 0x44DCD9F08DB194DDL, 0x2A7A41321FF2C2A8L,
                /* -282 */ 0x6E2E2980E2B5BAFBL, 0x5D906850331E043FL,
                /* -281 */ 0x5824EE00B55E2F2FL, 0x647386A68F4B3699L,
                /* -280 */ 0x4683F19A2AB1BF59L, 0x36C2D21ED908F87BL,
                /* -279 */ 0x70D31C29DDE93228L, 0x579E1CFE280E5A5DL,
                /* -278 */ 0x5A427CEE4B20F4EDL, 0x2C7E7D98200B7B7EL,
                /* -277 */ 0x483530BEA280C3F1L, 0x09FECAE019A2C932L,
                /* -276 */ 0x73884DFDD0CE064EL, 0x43314499C29E0EB6L,
                /* -275 */ 0x5C6D0B3173D8050BL, 0x4F5A9D47CEE4D891L,
                /* -274 */ 0x49F0D5C129799DA2L, 0x72AEE4397250AD41L,
                /* -273 */ 0x764E22CEA8C295D1L, 0x377E39F583B44868L,
                /* -272 */ 0x5EA4E8A553CEDE41L, 0x12CB61913629D387L,
                /* -271 */ 0x4BB72084430BE500L, 0x756F8140F8217605L,
                /* -270 */ 0x792500D39E796E67L, 0x6F18CECE59CF233CL,
                /* -269 */ 0x60EA670FB1FABEB9L, 0x3F470BD847D8E8FDL,
                /* -268 */ 0x4D885272F4C89894L, 0x329F3CAD064720CAL,
                /* -267 */ 0x7C0D50B7EE0DC0EDL, 0x37652DE1A3A50143L,
                /* -266 */ 0x633DDA2CBE716724L, 0x2C50F1814FB73436L,
                /* -265 */ 0x4F64AE8A31F45283L, 0x3D0D8E010C92902BL,
                /* -264 */ 0x7F077DA9E986EA6BL, 0x7B48E334E0EA8045L,
                /* -263 */ 0x659F97BB2138BB89L, 0x49071C2A4D88669DL,
                /* -262 */ 0x514C796280FA2FA1L, 0x20D27CEEA46D1EE4L,
                /* -261 */ 0x4109FAB533FB594DL, 0x670ECA58838A7F1DL,
                /* -260 */ 0x680FF788532BC216L, 0x0B4ADD5A6C10CB62L,
                /* -259 */ 0x533FF939DC2301ABL, 0x22A24AAEBCDA3C4EL,
                /* -258 */ 0x4299942E49B59AEFL, 0x354EA22563E1C9D8L,
                /* -257 */ 0x6A8F537D42BC2B18L, 0x554A9D089FCFA95AL,
                /* -256 */ 0x553F75FDCEFCEF46L, 0x776EE406E63FBAAEL,
                /* -255 */ 0x4432C4CB0BFD8C38L, 0x5F8BE99F1E996225L,
                /* -254 */ 0x6D1E07AB466279F4L, 0x327975CB64289D08L,
                /* -253 */ 0x574B3955D1E86190L, 0x28612B091CED4A6DL,
                /* -252 */ 0x45D5C777DB204E0DL, 0x06B4226DB0BDD524L,
                /* -251 */ 0x6FBC72595E9A167BL, 0x24536A491AC95506L,
                /* -250 */ 0x59638EADE54811FCL, 0x1D0F883A7BD44405L,
                /* -249 */ 0x4782D88B1DD34196L, 0x4A72D361FCA9D004L,
                /* -248 */ 0x726AF411C952028AL, 0x43EAEBCFFAA94CD3L,
                /* -247 */ 0x5B88C3416DDB353BL, 0x4FEF230CC88770A9L,
                /* -246 */ 0x493A35CDF17C2A96L, 0x0CBF4F3D6D3926EEL,
                /* -245 */ 0x7529EFAFE8C6AA89L, 0x61321862485B717CL,
                /* -244 */ 0x5DBB262653D22207L, 0x675B46B506AF8DFDL,
                /* -243 */ 0x4AFC1E850FDB4E6CL, 0x52AF6BC405593E64L,
                /* -242 */ 0x77F9CA6E7FC54A47L, 0x377F12D33BC1FD6DL,
                /* -241 */ 0x5FFB085866376E9FL, 0x45FF42429634CABDL,
                /* -240 */ 0x4CC8D379EB5F8BB2L, 0x6B329B68782A3BCBL,
                /* -239 */ 0x7ADAEBF64565AC51L, 0x2B842BDA59DD2C77L,
                /* -238 */ 0x6248BCC5045156A7L, 0x3C69BCAEAE4A89F9L,
                /* -237 */ 0x4EA0970403744552L, 0x6387CA25583BA194L,
                /* -236 */ 0x7DCDBE6CD253A21EL, 0x05A6103BC05F68EDL,
                /* -235 */ 0x64A498570EA94E7EL, 0x37B80CFC99E5ED8AL,
                /* -234 */ 0x5083AD1272210B98L, 0x2C933D96E184BE08L,
                /* -233 */ 0x40695741F4E73C79L, 0x7075CADF1AD09807L,
                /* -232 */ 0x670EF2032171FA5CL, 0x4D8944982AE759A4L,
                /* -231 */ 0x52725B35B45B2EB0L, 0x3E076A135585E150L,
                /* -230 */ 0x41F515C49048F226L, 0x64D2BB42AAD1810DL,
                /* -229 */ 0x698822D41A0E503EL, 0x07B7920444826815L,
                /* -228 */ 0x546CE8A9AE71D9CBL, 0x1FC60E69D0685344L,
                /* -227 */ 0x438A53BAF1F4AE3CL, 0x196B3EBB0D20429DL,
                /* -226 */ 0x6C1085F7E9877D2DL, 0x0F11FDF815006A94L,
                /* -225 */ 0x56739E5FEE05FDBDL, 0x58DB319344005543L,
                /* -224 */ 0x45294B7FF19E6497L, 0x60AF5ADC3666AA9CL,
                /* -223 */ 0x6EA878CCB5CA3A8CL, 0x344BC4938A3DDDC7L,
                /* -222 */ 0x5886C70A2B082ED6L, 0x5D096A0FA1CB17D2L,
                /* -221 */ 0x46D238D4EF39BF12L, 0x173ABB3FB4A27975L,
                /* -220 */ 0x71505AEE4B8F981DL, 0x0B912B992103F588L,
                /* -219 */ 0x5AA6AF25093FACE4L, 0x0940EFADB4032AD3L,
                /* -218 */ 0x488558EA6DCC8A50L, 0x07672624900288A9L,
                /* -217 */ 0x74088E43E2E0DD4CL, 0x723EA36DB337410EL,
                /* -216 */ 0x5CD3A5031BE71770L, 0x5B654F8AF5C5CDA5L,
                /* -215 */ 0x4A42EA68E31F45F3L, 0x62B772D5916B0AEBL,
                /* -214 */ 0x76D1770E38320986L, 0x0458B7BC1BDE77DDL,
                /* -213 */ 0x5F0DF8D82CF4D46BL, 0x1D13C630164B9318L,
                /* -212 */ 0x4C0B2D79BD90A9EFL, 0x30DC9E8CDEA2DC13L,
                /* -211 */ 0x79AB7BF5FC1AA97FL, 0x0160FDAE31049351L,
                /* -210 */ 0x6155FCC4C9AEEDFFL, 0x1AB3FE24F403A90EL,
                /* -209 */ 0x4DDE63D0A158BE65L, 0x6229981D9002EDA5L,
                /* -208 */ 0x7C97061A9BC130A2L, 0x69DC2695B337E2A1L,
                /* -207 */ 0x63AC04E2163426E8L, 0x54B01EDE28F9821BL,
                /* -206 */ 0x4FBCD0B4DE901F20L, 0x43C018B1BA6134E2L,
                /* -205 */ 0x7F9481216419CB67L, 0x1F99C11C5D68549DL,
                /* -204 */ 0x6610674DE9AE3C52L, 0x4C7B00E37DED107EL,
                /* -203 */ 0x51A6B90B21583042L, 0x09FC00B5FE574065L,
                /* -202 */ 0x41522DA2811359CEL, 0x3B3000919845CD1DL,
                /* -201 */ 0x68837C3734EBC2E3L, 0x784CCDB5C06FAE95L,
                /* -200 */ 0x539C635F5D8968B6L, 0x2D0A3E2B00595877L,
                /* -199 */ 0x42E382B2B13ABA2BL, 0x3DA1CB5599E11393L,
                /* -198 */ 0x6B059DEAB52AC378L, 0x629C7888F634EC1EL,
                /* -197 */ 0x559E17EEF755692DL, 0x3549FA072B5D89B1L,
                /* -196 */ 0x447E798BF91120F1L, 0x1107FB38EF7E07C1L,
                /* -195 */ 0x6D9728DFF4E834B5L, 0x01A65EC17F300C68L,
                /* -194 */ 0x57AC20B32A535D5DL, 0x4E1EB23465C009EDL,
                /* -193 */ 0x46234D5C21DC4AB1L, 0x24E55B5D1E333B24L,
                /* -192 */ 0x70387BC69C93AAB5L, 0x216EF894FD1EC506L,
                /* -191 */ 0x59C6C96BB076222AL, 0x4DF2607730E56A6CL,
                /* -190 */ 0x47D23ABC8D2B4E88L, 0x3E5B805F5A5121F0L,
                /* -189 */ 0x72E9F79415121740L, 0x63C59A322A1B697FL,
                /* -188 */ 0x5BEE5FA9AA74DF67L, 0x03047B5B54E2BACCL,
                /* -187 */ 0x498B7FBAEEC3E5ECL, 0x0269FC4910B5623DL,
                /* -186 */ 0x75ABFF917E063CACL, 0x6A432D41B45569FBL,
                /* -185 */ 0x5E2332DACB38308AL, 0x21CF5767C37787FCL,
                /* -184 */ 0x4B4F5BE23C2CF3A1L, 0x67D912B9692C6CCAL,
                /* -183 */ 0x787EF969F9E185CFL, 0x595B5128A8471476L,
                /* -182 */ 0x60659454C7E79E3FL, 0x6115DA86ED05A9F8L,
                /* -181 */ 0x4D1E1043D31FB1CCL, 0x4DAB1538BD9E2193L,
                /* -180 */ 0x7B634D3951CC4FADL, 0x62AB552795C9CF52L,
                /* -179 */ 0x62B5D7610E3D0C8BL, 0x0222AA86116E3F75L,
                /* -178 */ 0x4EF7DF80D830D6D5L, 0x4E822204DABE992AL,
                /* -177 */ 0x7E59659AF38157BCL, 0x17369CD49130F510L,
                /* -176 */ 0x65145148C2CDDFC9L, 0x5F5EE3DD40F3F740L,
                /* -175 */ 0x50DD0DD3CF0B196EL, 0x1918B64A9A5CC5CDL,
                /* -174 */ 0x40B0D7DCA5A27ABEL, 0x4746F83BAEB09E3EL,
                /* -173 */ 0x678159610903F797L, 0x253E59F91780FD2FL,
                /* -172 */ 0x52CDE11A6D9CC612L, 0x50FEAE60DF9A6426L,
                /* -171 */ 0x423E4DAEBE1704DBL, 0x5A65584D7FAEB685L,
                /* -170 */ 0x69FD4917968B3AF9L, 0x10A226E265E4573BL,
                /* -169 */ 0x54CAA0DFABA29594L, 0x0D4E8581EB1D1295L,
                /* -168 */ 0x43D54D7FBC821143L, 0x243ED134BC174211L,
                /* -167 */ 0x6C887BFF94034ED2L, 0x06CAE85460253682L,
                /* -166 */ 0x56D396661002A574L, 0x6BD586A9E6842B9BL,
                /* -165 */ 0x457611EB40021DF7L, 0x09779EEE52035616L,
                /* -164 */ 0x6F234FDECCD02FF1L, 0x5BF297E3B66BBCEFL,
                /* -163 */ 0x58E90CB23D73598EL, 0x165BACB62B8963F3L,
                /* -162 */ 0x4720D6F4FDF5E13EL, 0x451623C4EFA11CC2L,
                /* -161 */ 0x71CE24BB2FEFCECAL, 0x3B569FA17F682E03L,
                /* -160 */ 0x5B0B5095BFF30BD5L, 0x15DEE61ACC535803L,
                /* -159 */ 0x48D5DA11665C0977L, 0x2B18B8157042ACCFL,
                /* -158 */ 0x74895CE8A3C6758BL, 0x5E8DF355806AAE18L,
                /* -157 */ 0x5D3AB0BA1C9EC46FL, 0x653E5C4466BBBE7AL,
                /* -156 */ 0x4A955A2E7D4BD059L, 0x3765169D1EFC9861L,
                /* -155 */ 0x77555D172EDFB3C2L, 0x256E8A94FE60F3CFL,
                /* -154 */ 0x5F777DAC257FC301L, 0x6ABED543FEB3F63FL,
                /* -153 */ 0x4C5F97BCEACC9C01L, 0x3BCBDDCFFEF65E99L,
                /* -152 */ 0x7A328C6177ADC668L, 0x5FAC961997F0975BL,
                /* -151 */ 0x61C209E792F16B86L, 0x7FBD44E1465A12AFL,
                /* -150 */ 0x4E34D4B9425ABC6BL, 0x7FCA9D810514DBBFL,
                /* -149 */ 0x7D21545B9D5DFA46L, 0x32DDC8CE6E87C5FFL,
                /* -148 */ 0x641AA9E2E44B2E9EL, 0x5BE4A0A525396B32L,
                /* -147 */ 0x501554B5836F587EL, 0x7CB6E6EA842DEF5CL,
                /* -146 */ 0x4011109135F2AD32L, 0x30925255368B25E3L,
                /* -145 */ 0x6681B41B89844850L, 0x4DB6EA21F0DEA304L,
                /* -144 */ 0x52015CE2D469D373L, 0x57C5881B2718826AL,
                /* -143 */ 0x419AB0B576BB0F8FL, 0x5FD139AF527A01EFL,
                /* -142 */ 0x68F781225791B27FL, 0x4C81F5E550C3364AL,
                /* -141 */ 0x53F9341B79415B99L, 0x239B2B1DDA35C508L,
                /* -140 */ 0x432DC3492DCDE2E1L, 0x02E288E4AE916A6DL,
                /* -139 */ 0x6B7C6BA849496B01L, 0x516A74A1174F10AEL,
                /* -138 */ 0x55FD22ED076DEF34L, 0x4121F6E745D8DA25L,
                /* -137 */ 0x44CA82573924BF5DL, 0x1A8192529E4714EBL,
                /* -136 */ 0x6E10D08B8EA1322EL, 0x5D9C1D50FD3E87DDL,
                /* -135 */ 0x580D73A2D880F4F2L, 0x17B01773FDCB9FE4L,
                /* -134 */ 0x4671294F139A5D8EL, 0x4626792997D61984L,
                /* -133 */ 0x70B50EE4EC2A2F4AL, 0x3D0A5B75BFBCF59FL,
                /* -132 */ 0x5A2A7250BCEE8C3BL, 0x4A6EAF916630C47FL,
                /* -131 */ 0x4821F50D63F209C9L, 0x21F2260DEB5A36CCL,
                /* -130 */ 0x736988156CB6760EL, 0x69837016455D247AL,
                /* -129 */ 0x5C546CDDF091F80BL, 0x6E02C011D1175062L,
                /* -128 */ 0x49DD23E4C074C66FL, 0x719BCCDB0DAC404EL,
                /* -127 */ 0x762E9FD467213D7FL, 0x68F947C4E2AD33B0L,
                /* -126 */ 0x5E8BB3105280FDFFL, 0x6D94396A4EF0F627L,
                /* -125 */ 0x4BA2F5A6A8673199L, 0x3E102DEEA58D91B9L,
                /* -124 */ 0x7904BC3DDA3EB5C2L, 0x3019E3176F48E927L,
                /* -123 */ 0x60D09697E1CBC49BL, 0x4014B5AC590720ECL,
                /* -122 */ 0x4D73ABACB4A303AFL, 0x4CDD5E237A6C1A57L,
                /* -121 */ 0x7BEC45E12104D2B2L, 0x47C8969F2A46908AL,
                /* -120 */ 0x63236B1A80D0A88EL, 0x6CA0787F5505406FL,
                /* -119 */ 0x4F4F88E200A6ED3FL, 0x0A19F9FF773766BFL,
                /* -118 */ 0x7EE5A7D0010B1531L, 0x5CF65CCBF1F23DFEL,
                /* -117 */ 0x6584864000D5AA8EL, 0x172B7D6FF4C1CB32L,
                /* -116 */ 0x5136D1CCCD77BBA4L, 0x78EF978CC3CE3C28L,
                /* -115 */ 0x40F8A7D70AC62FB7L, 0x13F2DFA3CFD83020L,
                /* -114 */ 0x67F43FBE77A37F8BL, 0x398499061959E699L,
                /* -113 */ 0x5329CC985FB5FFA2L, 0x6136E0D1ADE18548L,
                /* -112 */ 0x4287D6E04C91994FL, 0x00F8B3DAF181376DL,
                /* -111 */ 0x6A72F166E0E8F54BL, 0x1B27862B1C01F247L,
                /* -110 */ 0x5528C11F1A53F76FL, 0x2F52D1BC1667F506L,
                /* -109 */ 0x44209A7F48432C59L, 0x0C424163451FF738L,
                /* -108 */ 0x6D00F7320D3846F4L, 0x7A039BD208332526L,
                /* -107 */ 0x5733F8F4D76038C3L, 0x7B361641A028EA85L,
                /* -106 */ 0x45C32D90AC4CFA36L, 0x2F5E78348020BB9EL,
                /* -105 */ 0x6F9EAF4DE07B29F0L, 0x4BCA59ED99CDF8FCL,
                /* -104 */ 0x594BBF71806287F3L, 0x563B7B247B0B2D96L,
                /* -103 */ 0x476FCC5ACD1B9FF6L, 0x11C92F50626F57ACL,
                /* -102 */ 0x724C7A2AE1C5CCBDL, 0x02DB7EE703E55912L,
                /* -101 */ 0x5B7061BBE7D17097L, 0x1BE2CBEC031DE0DCL,
                /* -100 */ 0x4926B496530DF3ACL, 0x164F09899C17E716L,
                /*  -99 */ 0x750ABA8A1E7CB913L, 0x3D4B4275C68CA4F0L,
                /*  -98 */ 0x5DA22ED4E530940FL, 0x4AA29B916BA3B726L,
                /*  -97 */ 0x4AE825771DC07672L, 0x6EE87C74561C9285L,
                /*  -96 */ 0x77D9D58B62CD8A51L, 0x3173FA53BCFA8408L,
                /*  -95 */ 0x5FE177A2B5713B74L, 0x278FFB7630C869A0L,
                /*  -94 */ 0x4CB45FB55DF42F90L, 0x1FA662C4F3D387B3L,
                /*  -93 */ 0x7ABA32BBC986B280L, 0x32A3D13B1FB8D91FL,
                /*  -92 */ 0x622E8EFCA1388ECDL, 0x0EE9742F4C93E0E6L,
                /*  -91 */ 0x4E8BA596E760723DL, 0x58BAC3590A0FE71EL,
                /*  -90 */ 0x7DAC3C24A5671D2FL, 0x412AD228101971C9L,
                /*  -89 */ 0x6489C9B6EAB8E426L, 0x00EF0E8673478E3BL,
                /*  -88 */ 0x506E3AF8BBC71CEBL, 0x1A58D86B8F6C71C9L,
                /*  -87 */ 0x40582F2D6305B0BCL, 0x1513E0560C56C16EL,
                /*  -86 */ 0x66F37EAF04D5E793L, 0x3B530089AD579BE2L,
                /*  -85 */ 0x525C6558D0AB1FA9L, 0x15DC006E2446164FL,
                /*  -84 */ 0x41E384470D55B2EDL, 0x5E4999F1B69E783FL,
                /*  -83 */ 0x696C06D81555EB15L, 0x7D428FE92430C065L,
                /*  -82 */ 0x54566BE0111188DEL, 0x31020CBA835A3384L,
                /*  -81 */ 0x4378564CDA746D7EL, 0x5A680A2ECF7B5C69L,
                /*  -80 */ 0x6BF3BD47C3ED7BFDL, 0x770CDD17B25EFA42L,
                /*  -79 */ 0x565C976C9CBDFCCBL, 0x1270B0DFC1E59502L,
                /*  -78 */ 0x4516DF8A16FE63D5L, 0x5B8D5A4C9B1E10CEL,
                /*  -77 */ 0x6E8AFF4357FD6C89L, 0x127BC3ADC4FCE7B0L,
                /*  -76 */ 0x586F329C466456D4L, 0x0EC96957D0CA52F3L,
                /*  -75 */ 0x46BF5BB038504576L, 0x3F07877973D50F29L,
                /*  -74 */ 0x71322C4D26E6D58AL, 0x31A5A58F1FBB4B75L,
                /*  -73 */ 0x5A8E89D75252446EL, 0x5AEAEAD8E62F6F91L,
                /*  -72 */ 0x487207DF750E9D25L, 0x2F22557A51BF8C74L,
                /*  -71 */ 0x73E9A63254E42EA2L, 0x1836EF2A1C65AD86L,
                /*  -70 */ 0x5CBAEB5B771CF21BL, 0x2CF8BF54E3848AD2L,
                /*  -69 */ 0x4A2F22AF927D8E7CL, 0x23FA32AA4F9D3BDBL,
                /*  -68 */ 0x76B1D118EA627D93L, 0x5329EAAA18FB92F8L,
                /*  -67 */ 0x5EF4A74721E86476L, 0x0F54BBBB472FA8C6L,
                /*  -66 */ 0x4BF6EC38E7ED1D2BL, 0x25DD62FC38F2ED6CL,
                /*  -65 */ 0x798B138E3FE1C845L, 0x22FBD1938E517BDFL,
                /*  -64 */ 0x613C0FA4FFE7D36AL, 0x4F2FDADC71DAC97FL,
                /*  -63 */ 0x4DC9A61D998642BBL, 0x58F3157D27E23ACCL,
                /*  -62 */ 0x7C75D695C2706AC5L, 0x74B82261D969F7ADL,
                /*  -61 */ 0x63917877CEC0556BL, 0x10934EB4ADEE5FBEL,
                /*  -60 */ 0x4FA793930BCD1122L, 0x4075D8908B251965L,
                /*  -59 */ 0x7F7285B812E1B504L, 0x00BC8DB411D4F56EL,
                /*  -58 */ 0x65F537C675815D9CL, 0x66FD3E29A7DD9125L,
                /*  -57 */ 0x5190F96B91344AE3L, 0x6BFDCB54864ADA84L,
                /*  -56 */ 0x4140C78940F6A24FL, 0x6FFE3C439EA2486AL,
                /*  -55 */ 0x6867A5A867F103B2L, 0x7FFD2D38FDD073DCL,
                /*  -54 */ 0x53861E2053273628L, 0x6664242D97D9F64AL,
                /*  -53 */ 0x42D1B1B375B8F820L, 0x51E9B68ADFE191D5L,
                /*  -52 */ 0x6AE91C5255F4C034L, 0x1CA924116635B621L,
                /*  -51 */ 0x558749DB77F70029L, 0x63BA83411E915E81L,
                /*  -50 */ 0x446C3B15F9926687L, 0x6962029A7EDAB201L,
                /*  -49 */ 0x6D79F82328EA3DA6L, 0x0F03375D97C45001L,
                /*  -48 */ 0x5794C6828721CAEBL, 0x259C2C4ADFD04001L,
                /*  -47 */ 0x46109ECED2816F22L, 0x5149BD08B30D0001L,
                /*  -46 */ 0x701A97B150CF1837L, 0x3542C80DEB480001L,
                /*  -45 */ 0x59AEDFC10D7279C5L, 0x7768A00B22A00001L,
                /*  -44 */ 0x47BF19673DF52E37L, 0x79208008E8800001L,
                /*  -43 */ 0x72CB5BD86321E38CL, 0x5B67334174000001L,
                /*  -42 */ 0x5BD5E313828182D6L, 0x7C528F6790000001L,
                /*  -41 */ 0x4977E8DC68679BDFL, 0x16A872B940000001L,
                /*  -40 */ 0x758CA7C70D7292FEL, 0x5773EAC200000001L,
                /*  -39 */ 0x5E0A1FD271287598L, 0x45F6556800000001L,
                /*  -38 */ 0x4B3B4CA85A86C47AL, 0x04C5112000000001L,
                /*  -37 */ 0x785EE10D5DA46D90L, 0x07A1B50000000001L,
                /*  -36 */ 0x604BE73DE4838AD9L, 0x52E7C40000000001L,
                /*  -35 */ 0x4D0985CB1D3608AEL, 0x0F1FD00000000001L,
                /*  -34 */ 0x7B426FAB61F00DE3L, 0x31CC800000000001L,
                /*  -33 */ 0x629B8C891B267182L, 0x5B0A000000000001L,
                /*  -32 */ 0x4EE2D6D415B85ACEL, 0x7C08000000000001L,
                /*  -31 */ 0x7E37BE2022C0914BL, 0x1340000000000001L,
                /*  -30 */ 0x64F964E68233A76FL, 0x2900000000000001L,
                /*  -29 */ 0x50C783EB9B5C85F2L, 0x5400000000000001L,
                /*  -28 */ 0x409F9CBC7C4A04C2L, 0x1000000000000001L,
                /*  -27 */ 0x6765C793FA10079DL, 0x0000000000000001L,
                /*  -26 */ 0x52B7D2DCC80CD2E4L, 0x0000000000000001L,
                /*  -25 */ 0x422CA8B0A00A4250L, 0x0000000000000001L,
                /*  -24 */ 0x69E10DE76676D080L, 0x0000000000000001L,
                /*  -23 */ 0x54B40B1F852BDA00L, 0x0000000000000001L,
                /*  -22 */ 0x43C33C1937564800L, 0x0000000000000001L,
                /*  -21 */ 0x6C6B935B8BBD4000L, 0x0000000000000001L,
                /*  -20 */ 0x56BC75E2D6310000L, 0x0000000000000001L,
                /*  -19 */ 0x4563918244F40000L, 0x0000000000000001L,
                /*  -18 */ 0x6F05B59D3B200000L, 0x0000000000000001L,
                /*  -17 */ 0x58D15E1762800000L, 0x0000000000000001L,
                /*  -16 */ 0x470DE4DF82000000L, 0x0000000000000001L,
                /*  -15 */ 0x71AFD498D0000000L, 0x0000000000000001L,
                /*  -14 */ 0x5AF3107A40000000L, 0x0000000000000001L,
                /*  -13 */ 0x48C2739500000000L, 0x0000000000000001L,
                /*  -12 */ 0x746A528800000000L, 0x0000000000000001L,
                /*  -11 */ 0x5D21DBA000000000L, 0x0000000000000001L,
                /*  -10 */ 0x4A817C8000000000L, 0x0000000000000001L,
                /*   -9 */ 0x7735940000000000L, 0x0000000000000001L,
                /*   -8 */ 0x5F5E100000000000L, 0x0000000000000001L,
                /*   -7 */ 0x4C4B400000000000L, 0x0000000000000001L,
                /*   -6 */ 0x7A12000000000000L, 0x0000000000000001L,
                /*   -5 */ 0x61A8000000000000L, 0x0000000000000001L,
                /*   -4 */ 0x4E20000000000000L, 0x0000000000000001L,
                /*   -3 */ 0x7D00000000000000L, 0x0000000000000001L,
                /*   -2 */ 0x6400000000000000L, 0x0000000000000001L,
                /*   -1 */ 0x5000000000000000L, 0x0000000000000001L,
                /*    0 */ 0x4000000000000000L, 0x0000000000000001L,
                /*    1 */ 0x6666666666666666L, 0x3333333333333334L,
                /*    2 */ 0x51EB851EB851EB85L, 0x0F5C28F5C28F5C29L,
                /*    3 */ 0x4189374BC6A7EF9DL, 0x5916872B020C49BBL,
                /*    4 */ 0x68DB8BAC710CB295L, 0x74F0D844D013A92BL,
                /*    5 */ 0x53E2D6238DA3C211L, 0x43F3E0370CDC8755L,
                /*    6 */ 0x431BDE82D7B634DAL, 0x698FE69270B06C44L,
                /*    7 */ 0x6B5FCA6AF2BD215EL, 0x0F4CA41D811A46D4L,
                /*    8 */ 0x55E63B88C230E77EL, 0x3F70834ACDAE9F10L,
                /*    9 */ 0x44B82FA09B5A52CBL, 0x4C5A02A23E254C0DL,
                /*   10 */ 0x6DF37F675EF6EADFL, 0x2D5CD10396A21347L,
                /*   11 */ 0x57F5FF85E592557FL, 0x3DE3DA69454E75D3L,
                /*   12 */ 0x465E6604B7A84465L, 0x7E4FE1EDD10B9175L,
                /*   13 */ 0x709709A125DA0709L, 0x4A19697C81AC1BEFL,
                /*   14 */ 0x5A126E1A84AE6C07L, 0x54E1213067BCE326L,
                /*   15 */ 0x480EBE7B9D58566CL, 0x43E74DC052FD8285L,
                /*   16 */ 0x734ACA5F6226F0ADL, 0x530BAF9A1E626A6DL,
                /*   17 */ 0x5C3BD5191B525A24L, 0x426FBFAE7EB521F1L,
                /*   18 */ 0x49C97747490EAE83L, 0x4EBFCC8B9890E7F4L,
                /*   19 */ 0x760F253EDB4AB0D2L, 0x4ACC7A78F41B0CBAL,
                /*   20 */ 0x5E72843249088D75L, 0x223D2EC729AF3D62L,
                /*   21 */ 0x4B8ED0283A6D3DF7L, 0x34FDBF05BAF29781L,
                /*   22 */ 0x78E480405D7B9658L, 0x54C931A2C4B758CFL,
                /*   23 */ 0x60B6CD004AC94513L, 0x5D6DC14F03C5E0A5L,
                /*   24 */ 0x4D5F0A66A23A9DA9L, 0x31249AA59C9E4D51L,
                /*   25 */ 0x7BCB43D769F762A8L, 0x4EA0F76F60FD4882L,
                /*   26 */ 0x63090312BB2C4EEDL, 0x254D92BF80CAA068L,
                /*   27 */ 0x4F3A68DBC8F03F24L, 0x1DD7A89933D54D20L,
                /*   28 */ 0x7EC3DAF941806506L, 0x62F2A75B86221500L,
                /*   29 */ 0x65697BFA9ACD1D9FL, 0x025BB91604E810CDL,
                /*   30 */ 0x51212FFBAF0A7E18L, 0x684960DE6A5340A4L,
                /*   31 */ 0x40E7599625A1FE7AL, 0x203AB3E521DC33B6L,
                /*   32 */ 0x67D88F56A29CCA5DL, 0x19F7863B696052BDL,
                /*   33 */ 0x5313A5DEE87D6EB0L, 0x7B2C6B62BAB37564L,
                /*   34 */ 0x42761E4BED31255AL, 0x2F56BC4EFBC2C450L,
                /*   35 */ 0x6A5696DFE1E83BC3L, 0x655793B192D13A1AL,
                /*   36 */ 0x5512124CB4B9C969L, 0x377942F475742E7BL,
                /*   37 */ 0x440E750A2A2E3ABAL, 0x5F9435905DF68B96L,
                /*   38 */ 0x6CE3EE76A9E3912AL, 0x65B9EF4D63241289L,
                /*   39 */ 0x571CBEC554B60DBBL, 0x6AFB25D782834207L,
                /*   40 */ 0x45B0989DDD5E7163L, 0x08C8EB12CECF6806L,
                /*   41 */ 0x6F80F42FC8971BD1L, 0x5ADB11B7B14BD9A3L,
                /*   42 */ 0x5933F68CA078E30EL, 0x157C0E2C8DD647B5L,
                /*   43 */ 0x475CC53D4D2D8271L, 0x5DFCD823A4AB6C91L,
                /*   44 */ 0x722E086215159D82L, 0x632E269F6DDF141BL,
                /*   45 */ 0x5B5806B4DDAAE468L, 0x4F581EE5F17F4349L,
                /*   46 */ 0x49133890B1558386L, 0x72ACE584C1329C3BL,
                /*   47 */ 0x74EB8DB44EEF38D7L, 0x6AAE3C079B842D2AL,
                /*   48 */ 0x5D893E29D8BF60ACL, 0x5558300616035755L,
                /*   49 */ 0x4AD431BB13CC4D56L, 0x7779C004DE6912ABL,
                /*   50 */ 0x77B9E92B52E07BBEL, 0x258F99A163DB5111L,
                /*   51 */ 0x5FC7EDBC424D2FCBL, 0x37A614811CAF740DL,
                /*   52 */ 0x4C9FF163683DBFD5L, 0x7951AA00E3BF900BL,
                /*   53 */ 0x7A998238A6C932EFL, 0x754F7667D2CC19ABL,
                /*   54 */ 0x6214682D523A8F26L, 0x2AA5F8530F09AE22L,
                /*   55 */ 0x4E76B9BDDB620C1EL, 0x55519375A5A1581BL,
                /*   56 */ 0x7D8AC2C95F034697L, 0x3BB5B8BC3C3559C5L,
                /*   57 */ 0x646F023AB2690545L, 0x7C9160969691149EL,
                /*   58 */ 0x5058CE955B87376BL, 0x16DAB3ABABA743B2L,
                /*   59 */ 0x40470BAAAF9F5F88L, 0x78AEF622EFB902F5L,
                /*   60 */ 0x66D812AAB29898DBL, 0x0DE4BD04B2C19E54L,
                /*   61 */ 0x524675555BAD4715L, 0x57EA30D08F014B76L,
                /*   62 */ 0x41D1F7777C8A9F44L, 0x4654F3DA0C01092CL,
                /*   63 */ 0x694FF258C7443207L, 0x23BB1FC346680EACL,
                /*   64 */ 0x543FF513D29CF4D2L, 0x4FC8E635D1ECD88AL,
                /*   65 */ 0x43665DA9754A5D75L, 0x263A51C4A7F0AD3BL,
                /*   66 */ 0x6BD6FC425543C8BBL, 0x56C3B607731AAEC4L,
                /*   67 */ 0x5645969B77696D62L, 0x789C919F8F488BD0L,
                /*   68 */ 0x4504787C5F878AB5L, 0x46E3A7B2D906D640L,
                /*   69 */ 0x6E6D8D93CC0C1122L, 0x3E390C515B3E239AL,
                /*   70 */ 0x5857A4763CD6741BL, 0x4B60D6A77C31B615L,
                /*   71 */ 0x46AC8391CA4529AFL, 0x55E7121F968E2B44L,
                /*   72 */ 0x711405B6106EA919L, 0x0971B698F0E3786DL,
                /*   73 */ 0x5A766AF80D255414L, 0x078E2BAD8D82C6BDL,
                /*   74 */ 0x485EBBF9A41DDCDCL, 0x6C71BC8AD79BD231L,
                /*   75 */ 0x73CAC65C39C96161L, 0x2D82C7448C2C8382L,
                /*   76 */ 0x5CA23849C7D44DE7L, 0x3E023903A356CF9BL,
                /*   77 */ 0x4A1B603B06437185L, 0x7E682D9C82ABD949L,
                /*   78 */ 0x76923391A39F1C09L, 0x4A4048FA6AAC8EDBL,
                /*   79 */ 0x5EDB5C7482E5B007L, 0x55003A61EEF07249L,
                /*   80 */ 0x4BE2B05D35848CD2L, 0x773361E7F259F507L,
                /*   81 */ 0x796AB3C855A0E151L, 0x3EB89CA6508FEE71L,
                /*   82 */ 0x6122296D114D810DL, 0x7EFA16EB73A6585BL,
                /*   83 */ 0x4DB4EDF0DAA4673EL, 0x3261ABEF8FB846AFL,
                /*   84 */ 0x7C54AFE7C43A3ECAL, 0x1D691318E5F3A44BL,
                /*   85 */ 0x6376F31FD02E98A1L, 0x64540F471E5C836FL,
                /*   86 */ 0x4F925C1973587A1BL, 0x0376729F4B7D35F3L,
                /*   87 */ 0x7F50935BEBC0C35EL, 0x38BD84321261EFEBL,
                /*   88 */ 0x65DA0F7CBC9A35E5L, 0x13CAD0280EB4BFEFL,
                /*   89 */ 0x517B3F96FD482B1DL, 0x5CA240200BC3CCBFL,
                /*   90 */ 0x412F66126439BC17L, 0x63B50019A3030A33L,
                /*   91 */ 0x684BD683D38F9359L, 0x1F88002904D1A9EAL,
                /*   92 */ 0x536FDECFDC72DC47L, 0x32D3335403DAEE55L,
                /*   93 */ 0x42BFE57316C249D2L, 0x5BDC291003158B77L,
                /*   94 */ 0x6ACCA251BE03A951L, 0x12F9DB4CD1BC1258L,
                /*   95 */ 0x557081DAFE695440L, 0x7594AF70A7C9A847L,
                /*   96 */ 0x445A017BFEBAA9CDL, 0x4476F2C0863AED06L,
                /*   97 */ 0x6D5CCF2CCAC442E2L, 0x3A57EACDA3917B3CL,
                /*   98 */ 0x577D728A3BD03581L, 0x7B7988A482DAC8FDL,
                /*   99 */ 0x45FDF53B630CF79BL, 0x15FAD3B6CF156D97L,
                /*  100 */ 0x6FFCBB923814BF5EL, 0x565E1F8AE4EF15BEL,
                /*  101 */ 0x5996FC74F9AA32B2L, 0x11E4E608B725AAFFL,
                /*  102 */ 0x47ABFD2A6154F55BL, 0x27EA51A0928488CCL,
                /*  103 */ 0x72ACC843CEEE555EL, 0x7310829A84074146L,
                /*  104 */ 0x5BBD6D030BF1DDE5L, 0x42739BAED005CDD2L,
                /*  105 */ 0x49645735A327E4B7L, 0x4EC2E2F24004A4A8L,
                /*  106 */ 0x756D5855D1D96DF2L, 0x4AD16B1D333AA10CL,
                /*  107 */ 0x5DF11377DB1457F5L, 0x2241227DC2954DA3L,
                /*  108 */ 0x4B2742C648DD132AL, 0x4E9A81FE35443E1CL,
                /*  109 */ 0x783ED13D4161B844L, 0x175D9CC9EED39694L,
                /*  110 */ 0x603240FDCDE7C69CL, 0x7917B0A18BDC7876L,
                /*  111 */ 0x4CF500CB0B1FD217L, 0x1412F3B46FE39392L,
                /*  112 */ 0x7B219ADE7832E9BEL, 0x535185ED7FD285B6L,
                /*  113 */ 0x628148B1F9C25498L, 0x42A79E57997537C5L,
                /*  114 */ 0x4ECDD3C1949B76E0L, 0x3552E512E12A9304L,
                /*  115 */ 0x7E161F9C20F8BE33L, 0x6EEB081E3510EB39L,
                /*  116 */ 0x64DE7FB01A609829L, 0x3F226CE4F740BC2EL,
                /*  117 */ 0x50B1FFC0151A1354L, 0x3281F0B72C33C9BEL,
                /*  118 */ 0x408E66334414DC43L, 0x42018D5F568FD498L,
                /*  119 */ 0x674A3D1ED354939FL, 0x1CCF48988A7FBA8DL,
                /*  120 */ 0x52A1CA7F0F76DC7FL, 0x30A5D3AD3B99620BL,
                /*  121 */ 0x421B0865A5F8B065L, 0x73B7DC8A96144E6FL,
                /*  122 */ 0x69C4DA3C3CC11A3CL, 0x52BFC7442353B0B1L,
                /*  123 */ 0x549D7B6363CDAE96L, 0x756639034F7626F4L,
                /*  124 */ 0x43B12F82B63E2545L, 0x4451C735D92B525DL,
                /*  125 */ 0x6C4EB26ABD303BA2L, 0x3A1C71EFC1DEEA2EL,
                /*  126 */ 0x56A55B889759C94EL, 0x61B05B2634B254F2L,
                /*  127 */ 0x45511606DF7B0772L, 0x1AF37C1E908EAA5BL,
                /*  128 */ 0x6EE8233E325E7250L, 0x2B1F2CFDB41776F8L,
                /*  129 */ 0x58B9B5CB5B7EC1D9L, 0x6F4C23FE29AC5F2DL,
                /*  130 */ 0x46FAF7D5E2CBCE47L, 0x72A34FFE87BD18F1L,
                /*  131 */ 0x71918C896ADFB073L, 0x04387FFDA5FB5B1BL,
                /*  132 */ 0x5ADAD6D4557FC05CL, 0x0360666484C915AFL,
                /*  133 */ 0x48AF1243779966B0L, 0x02B3851D3707448CL,
                /*  134 */ 0x744B506BF28F0AB3L, 0x1DEC082EBE720746L,
                /*  135 */ 0x5D090D2328726EF5L, 0x64BCD358985B3905L,
                /*  136 */ 0x4A6DA41C205B8BF7L, 0x6A30A913AD15C738L,
                /*  137 */ 0x7715D36033C5ACBFL, 0x5D1AA81F7B560B8CL,
                /*  138 */ 0x5F44A919C3048A32L, 0x7DAEECE5FC44D609L,
                /*  139 */ 0x4C36EDAE359D3B5BL, 0x7E258A51969D7808L,
                /*  140 */ 0x79F17C49EF61F893L, 0x16A276E8F0FBF33FL,
                /*  141 */ 0x618DFD07F2B4C6DCL, 0x121B9253F3FCC299L,
                /*  142 */ 0x4E0B30D328909F16L, 0x41AFA84329970214L,
                /*  143 */ 0x7CDEB4850DB431BDL, 0x4F7F739EA8F19CEDL,
                /*  144 */ 0x63E55D373E29C164L, 0x3F99294BBA5AE3F1L,
                /*  145 */ 0x4FEAB0F8FE87CDE9L, 0x7FADBAA2FB7BE98DL,
                /*  146 */ 0x7FDDE7F4CA72E30FL, 0x7F7C5DD1925FDC15L,
                /*  147 */ 0x664B1FF7085BE8D9L, 0x4C637E4141E649ABL,
                /*  148 */ 0x51D5B32C06AFED7AL, 0x704F983434B83AEFL,
                /*  149 */ 0x4177C2899EF32462L, 0x26A6135CF6F9C8BFL,
                /*  150 */ 0x68BF9DA8FE51D3D0L, 0x3DD685618B294132L,
                /*  151 */ 0x53CC7E20CB74A973L, 0x4B12044E08EDCDC2L,
                /*  152 */ 0x4309FE80A2C3BAC2L, 0x6F419D0B3A57D7CEL,
                /*  153 */ 0x6B4330CDD1392AD1L, 0x320294DEC3BFBFB0L,
                /*  154 */ 0x55CF5A3E40FA88A7L, 0x419BAA4BCFCC995AL,
                /*  155 */ 0x44A5E1CB672ED3B9L, 0x1AE2EEA30CA3ADE1L,
                /*  156 */ 0x6DD636123EB152C1L, 0x77D17DD1ADD2AFCFL,
                /*  157 */ 0x57DE91A832277567L, 0x797464A7BE42263FL,
                /*  158 */ 0x464BA7B9C1B92AB9L, 0x4790508631CE84FFL,
                /*  159 */ 0x70790C5C6928445CL, 0x0C1A1A704FB0D4CCL,
                /*  160 */ 0x59FA7049EDB9D049L, 0x567B4859D95A43D6L,
                /*  161 */ 0x47FB8D07F161736EL, 0x11FC39E17AAE9CABL,
                /*  162 */ 0x732C14D98235857DL, 0x032D2968C44A9445L,
                /*  163 */ 0x5C2343E134F79DFDL, 0x4F575453D03BA9D1L,
                /*  164 */ 0x49B5CFE75D92E4CAL, 0x72AC4376402FBB0EL,
                /*  165 */ 0x75EFB30BC8EB07ABL, 0x0446D256CD192B49L,
                /*  166 */ 0x5E595C096D88D2EFL, 0x1D0575123DADBC3AL,
                /*  167 */ 0x4B7AB0078AD3DBF2L, 0x4A6AC40E97BE302FL,
                /*  168 */ 0x78C44CD8DE1FC650L, 0x771139B0F2C9E6B1L,
                /*  169 */ 0x609D0A4718196B73L, 0x78DA948D8F07EBC1L,
                /*  170 */ 0x4D4A6E9F467ABC5CL, 0x60AEDD3E0C065634L,
                /*  171 */ 0x7BAA4A9870C46094L, 0x344AFB9679A3BD20L,
                /*  172 */ 0x62EEA2138D69E6DDL, 0x103BFC78614FCA80L,
                /*  173 */ 0x4F254E760ABB1F17L, 0x26966393810CA200L,
                /*  174 */ 0x7EA21723445E9825L, 0x2423D2859B476999L,
                /*  175 */ 0x654E78E9037EE01DL, 0x69B642047C392148L,
                /*  176 */ 0x510B93ED9C658017L, 0x6E2B680396941AA0L,
                /*  177 */ 0x40D60FF149EACCDFL, 0x71BC53361210154DL,
                /*  178 */ 0x67BCE64EDCAAE166L, 0x1C6085235019BBAEL,
                /*  179 */ 0x52FD850BE3BBE784L, 0x7D1A041C40149625L,
                /*  180 */ 0x42646A6FE9631F9DL, 0x4A7B367D0010781DL,
                /*  181 */ 0x6A3A43E642383295L, 0x5D91F0C8001A59C8L,
                /*  182 */ 0x54FB698501C68EDEL, 0x17A7F3D3334847D4L,
                /*  183 */ 0x43FC546A67D20BE4L, 0x79532975C2A03976L,
                /*  184 */ 0x6CC6ED770C83463BL, 0x0EEB75893766C256L,
                /*  185 */ 0x57058AC5A39C382FL, 0x25892AD42C523512L,
                /*  186 */ 0x459E089E1C7CF9BFL, 0x37A0EF102374F742L,
                /*  187 */ 0x6F6340FCFA618F98L, 0x59017E8038BB2536L,
                /*  188 */ 0x591C33FD951AD946L, 0x7A67986693C8EA91L,
                /*  189 */ 0x4749C33144157A9FL, 0x151FAD1EDCA0BBA8L,
                /*  190 */ 0x720F9EB539BBF765L, 0x0832AE97C76792A5L,
                /*  191 */ 0x5B3FB22A94965F84L, 0x068EF21305EC7551L,
                /*  192 */ 0x48FFC1BBAA11E603L, 0x1ED8C1A8D189F774L,
                /*  193 */ 0x74CC692C434FD66BL, 0x4AF4690E1C0FF253L,
                /*  194 */ 0x5D705423690CAB89L, 0x225D20D816732843L,
                /*  195 */ 0x4AC0434F873D5607L, 0x35174D79AB8F5369L,
                /*  196 */ 0x779A054C0B955672L, 0x21BEE25C45B21F0EL,
                /*  197 */ 0x5FAE6AA33C77785BL, 0x3498B5169E2818D8L,
                /*  198 */ 0x4C8B888296C5F9E2L, 0x5D46F7454B534713L,
                /*  199 */ 0x7A78DA6A8AD65C9DL, 0x7BA4BED545520B52L,
                /*  200 */ 0x61FA48553BDEB07EL, 0x2FB6FF110441A2A8L,
                /*  201 */ 0x4E61D37763188D31L, 0x72F8CC0D9D014EEDL,
                /*  202 */ 0x7D6952589E8DAEB6L, 0x1E5AE015C80217E1L,
                /*  203 */ 0x645441E07ED7BEF8L, 0x1848B344A001ACB4L,
                /*  204 */ 0x504367E6CBDFCBF9L, 0x603A2903B3348A2AL,
                /*  205 */ 0x4035ECB8A3196FFBL, 0x002E873628F6D4EEL,
                /*  206 */ 0x66BCADF43828B32BL, 0x19E40B89DB2487E3L,
                /*  207 */ 0x52308B29C686F5BCL, 0x14B66FA17C1D3983L,
                /*  208 */ 0x41C06F549ED25E30L, 0x1091F2E7967DC79CL,
                /*  209 */ 0x6933E554315096B3L, 0x341CB7D8F0C93F5FL,
                /*  210 */ 0x542984435AA6DEF5L, 0x767D5FE0C0A0FF80L,
                /*  211 */ 0x435469CF7BB8B25EL, 0x2B977FE70080CC66L,
                /*  212 */ 0x6BBA42E592C11D63L, 0x5F58CCA4CD9AE0A3L,
                /*  213 */ 0x562E9BEADBCDB11CL, 0x4C470A1D7148B3B6L,
                /*  214 */ 0x44F216557CA48DB0L, 0x3D05A1B1276D5C92L,
                /*  215 */ 0x6E5023BBFAA0E2B3L, 0x7B3C35E83F1560E9L,
                /*  216 */ 0x58401C96621A4EF6L, 0x2F635E5365AAB3EDL,
                /*  217 */ 0x4699B0784E7B725EL, 0x591C4B75EAEEF658L,
                /*  218 */ 0x70F5E726E3F8B6FDL, 0x74FA125644B18A26L,
                /*  219 */ 0x5A5E5285832D5F31L, 0x43FB41DE9D5AD4EBL,
                /*  220 */ 0x484B75379C244C27L, 0x4FFC34B2177BDD89L,
                /*  221 */ 0x73ABEEBF603A1372L, 0x4CC6BAB68BF96274L,
                /*  222 */ 0x5C898BCC4CFB42C2L, 0x0A38955ED6611B90L,
                /*  223 */ 0x4A07A309D72F689BL, 0x21C6DDE5784DAFA7L,
                /*  224 */ 0x76729E762518A75EL, 0x693E2FD58D49190BL,
                /*  225 */ 0x5EC2185E8413B918L, 0x5431BFDE0AA0E0D5L,
                /*  226 */ 0x4BCE79E536762DADL, 0x29C1664B3BB3E711L,
                /*  227 */ 0x794A5CA1F0BD15E2L, 0x0F9BD6DEC5ECA4E8L,
                /*  228 */ 0x61084A1B26FDAB1BL, 0x2616457F04BD50BAL,
                /*  229 */ 0x4DA03B48EBFE227CL, 0x1E783798D09773C8L,
                /*  230 */ 0x7C33920E46636A60L, 0x30C058F480F252D9L,
                /*  231 */ 0x635C74D8384F884DL, 0x0D66AD9067284247L,
                /*  232 */ 0x4F7D2A469372D370L, 0x711EF14052869B6CL,
                /*  233 */ 0x7F2EAA0A85848581L, 0x34FE4ECD50D75F14L,
                /*  234 */ 0x65BEEE6ED136D134L, 0x2A650BD773DF7F43L,
                /*  235 */ 0x51658B8BDA9240F6L, 0x551DA312C319329CL,
                /*  236 */ 0x411E093CAEDB672BL, 0x5DB14F4235ADC217L,
                /*  237 */ 0x68300EC77E2BD845L, 0x7C4EE536BC49368AL,
                /*  238 */ 0x5359A56C64EFE037L, 0x7D0BEA92303A9208L,
                /*  239 */ 0x42AE1DF050BFE693L, 0x173CBBA8269541A0L,
                /*  240 */ 0x6AB02FE6E79970EBL, 0x3EC792A6A422029AL,
                /*  241 */ 0x5559BFEBEC7AC0BCL, 0x3239421EE9B4CEE1L,
                /*  242 */ 0x4447CCBCBD2F0096L, 0x5B6101B25490A581L,
                /*  243 */ 0x6D3FADFAC84B3424L, 0x2BCE691D541AA268L,
                /*  244 */ 0x576624C8A03C29B6L, 0x563EBA7DDCE21B87L,
                /*  245 */ 0x45EB50A08030215EL, 0x78322ECB171B4939L,
                /*  246 */ 0x6FDEE76733803564L, 0x59E9E47824F87527L,
                /*  247 */ 0x597F1F85C2CCF783L, 0x6187E9F9B72D2A86L,
                /*  248 */ 0x4798E6049BD72C69L, 0x346CBB2E2C242205L,
                /*  249 */ 0x728E3CD42C8B7A42L, 0x20ADF849E039D007L,
                /*  250 */ 0x5BA4FD768A092E9BL, 0x33BE603B19C7D99FL,
                /*  251 */ 0x4950CAC53B3A8BAFL, 0x42FEB3627B0647B3L,
                /*  252 */ 0x754E113B91F745E5L, 0x5197856A5E7072B8L,
                /*  253 */ 0x5DD80DC941929E51L, 0x27AC6ABB7EC05BC6L,
                /*  254 */ 0x4B133E3A9ADBB1DAL, 0x52F05562CBCD1638L,
                /*  255 */ 0x781EC9F75E2C4FC4L, 0x1E4D556ADFAE89F3L,
                /*  256 */ 0x6018A192B1BD0C9CL, 0x7EA444557FBED4C3L,
                /*  257 */ 0x4CE0814227CA707DL, 0x4BB69D1132FF109CL,
                /*  258 */ 0x7B00CED03FAA4D95L, 0x5F8A94E851981A93L,
                /*  259 */ 0x62670BD9CC883E11L, 0x32D543ED0E134875L,
                /*  260 */ 0x4EB8D647D6D364DAL, 0x5BDDCFF0D80F6D2BL,
                /*  261 */ 0x7DF48A0C8AEBD491L, 0x12FC7FE7C018AEABL,
                /*  262 */ 0x64C3A1A3A25643A7L, 0x28C9FFEC99AD5889L,
                /*  263 */ 0x509C814FB511CFB9L, 0x0707FFF07AF113A1L,
                /*  264 */ 0x407D343FC40E3FC7L, 0x1F39998D2F2742E7L,
                /*  265 */ 0x672EB9FFA016CC71L, 0x7EC28F484B7204A4L,
                /*  266 */ 0x528BC7FFB345705BL, 0x189BA5D36F8E6A1DL,
                /*  267 */ 0x42096CCC8F6AC048L, 0x7A161E42BFA521B1L,
                /*  268 */ 0x69A8AE1418AACD41L, 0x435696D132A1CF81L,
                /*  269 */ 0x5486F1A9AD557101L, 0x1C454574288172CEL,
                /*  270 */ 0x439F27BAF1112734L, 0x169DD129BA0128A5L,
                /*  271 */ 0x6C31D92B1B4EA520L, 0x242FB50F9001DAA1L,
                /*  272 */ 0x568E4755AF721DB3L, 0x368C90D940017BB4L,
                /*  273 */ 0x453E9F77BF8E7E29L, 0x120A0D7A999AC95DL,
                /*  274 */ 0x6ECA98BF98E3FD0EL, 0x50101590F5C47561L,
                /*  275 */ 0x58A213CC7A4FFDA5L, 0x26734473F7D05DE8L,
                /*  276 */ 0x46E80FD6C83FFE1DL, 0x6B8F69F65FD9E4B9L,
                /*  277 */ 0x71734C8AD9FFFCFCL, 0x45B24323CC8FD45CL,
                /*  278 */ 0x5AC2A3A247FFFD96L, 0x6AF502830A0CA9E3L,
                /*  279 */ 0x489BB61B6CCCCADFL, 0x08C402026E7087E9L,
                /*  280 */ 0x742C569247AE1164L, 0x746CD003E3E73FDBL,
                /*  281 */ 0x5CF04541D2F1A783L, 0x76BD73364FEC3315L,
                /*  282 */ 0x4A59D101758E1F9CL, 0x5EFDF5C50CBCF5ABL,
                /*  283 */ 0x76F61B3588E365C7L, 0x4B2FEFA1ADFB22ABL,
                /*  284 */ 0x5F2B48F7A0B5EB06L, 0x08F3261AF195B555L,
                /*  285 */ 0x4C22A0C61A2B226BL, 0x20C284E25ADE2AABL,
                /*  286 */ 0x79D1013CF6AB6A45L, 0x1AD0D49D5E304444L,
                /*  287 */ 0x617400FD9222BB6AL, 0x48A7107DE4F369D0L,
                /*  288 */ 0x4DF6673141B562BBL, 0x53B8D9FE50C2BB0DL,
                /*  289 */ 0x7CBD71E869223792L, 0x52C15CCA1AD12B48L,
                /*  290 */ 0x63CAC186BA81C60EL, 0x75677D6E7BDA8906L,
                /*  291 */ 0x4FD5679EFB9B04D8L, 0x5DEC645863153A6CL,
                /*  292 */ 0x7FBBD8FE5F5E6E27L, 0x497A3A2704EEC3DFL,
        };

    }

}
