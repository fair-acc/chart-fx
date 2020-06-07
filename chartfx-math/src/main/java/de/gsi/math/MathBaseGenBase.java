/*************************************************************************
 * Originally Based on CERN's ROOT analysis frame work:
 * 
 * @see root.cern.ch for details Copyright (C) 1995-2004, Rene Brun and Fons Rademakers. Authors: Rene Brun, Anna
 *      Kreshuk, Eddy Offermann, Fons Rademakers All rights reserved. Java port and extension by: Ralph J. Steinhagen,
 *      CERN, BE-BI, 2009 For the licensing terms see LICENSE. For the list of contributors see $ROOTSYS/README/CREDITS
 *************************************************************************/

package de.gsi.math;

public class MathBaseGenBase {
    public static double aCosH(double arg0) {
        if (arg0 == 0.0)
            return 0.0;
        double ax = java.lang.Math.abs(arg0);
        return java.lang.Math.log(arg0 + ax * java.lang.Math.sqrt(1. - 1. / (ax * ax)));
    }

    public static double aSinH(double arg0) {
        if (arg0 == 0.0)
            return 0.0;
        double ax = java.lang.Math.abs(arg0);
        return java.lang.Math.log(arg0 + ax * java.lang.Math.sqrt(1. + 1. / (ax * ax)));
    }

    public static double aTanH(double arg0) {
        return java.lang.Math.log((1 + arg0) / (1 - arg0)) / 2;
    }

    // codegen: double -> long
    /**
     * some integer math
     *
     * @param x input px
     * @param y input py
     * @return sqrt(px*px + py*py)
     */
    public static double hypot(double x, double y) {
        return java.lang.Math.hypot(x, y);
    }
    //// end codegen

    //// codegen: double -> float, int, long
    public static double abs(double d) {
        return (d >= 0) ? d : -d;
        // short: return (d >= 0) ? (short) d : (short) (-d);
    }
    //// end codegen

    public static double aCos(double arg0) {
        return java.lang.Math.acos(arg0);
    }

    public static double aSin(double arg0) {
        return java.lang.Math.asin(arg0);
    }

    public static double aTan(double arg0) {
        return java.lang.Math.atan(arg0);
    }

    public static double aTan2(double y, double x) {
        if (x != 0)
            return java.lang.Math.atan2(y, x);
        else if (y == 0)
            return 0;
        else if (y > 0)
            return pi() / 2;
        else
            return -pi() / 2;
    }

    /**
     * @return velocity of light in [m s^-1]
     */
    public static final double c() {
        return 2.99792458e8;
    }

    /**
     * @return velocity of light in [cm s^-1]
     */
    public static double cCgs() {
        return 100.0 * c();
    }

    public static double ceil(double x) {
        return java.lang.Math.ceil(x);
    }

    public static int ceilNInt(double x) {
        return nInt(java.lang.Math.ceil(x));
    }

    public static double cos(double arg0) {
        return java.lang.Math.cos(arg0);
    }

    public static double cosH(double arg0) {
        return java.lang.Math.cosh(arg0);
    }

    public static double cUncertainty() {
        return 0.0;
    } // exact

    public static double degToRad() {
        return pi() / 180.0;
    }

    /**
     * @return e (base of natural log)
     */
    public static double e() {
        return 2.71828182845904523536;
    }

    // Euler-Mascheroni Constant
    public static double eulerGamma() {
        return 0.577215664901532860606512090082402431042;
    }

    public static boolean even(long a) {
        return !((a & 1) != 0);
    }

    public static double exp(double x) {
        return java.lang.Math.exp(x);
    }

    public static boolean finite(double x) {
        return !(Double.isInfinite(x) || Double.isNaN(x));
    }

    public static double floor(double x) {
        return java.lang.Math.floor(x);
    }

    public static int floorNint(double x) {
        return nInt(java.lang.Math.floor(x));
    }

    /**
     * @return gravitational constant in [m^3 kg^-1 s^-2]
     */
    public static double g() {
        return 6.673e-11;
    }

    /**
     * @return gravitational constant in [cm^3 g^-1 s^-2]
     */
    public static double gCgs() {
        return g() / 1000.0;
    }

    // TODO: continue documentation here
    // G over h-bar C
    public static final double ghbarC() {
        return 6.707e-39;
    } // [(GeV/c^2)^-2]

    public static final double ghbarCUncertainty() {
        return 0.010e-39;
    }

    // standard acceleration of gravity
    public static final double gn() {
        return 9.80665;
    } // [m s^-2]

    public static final double gnUncertainty() {
        return 0.0;
    } // exact

    public static final double gUncertainty() {
        return 0.010e-11;
    }

    // Planck's constant
    public static final double h() {
        return 6.62606876e-34;
    } // [J s]

    // h-bar (h over 2 pi)
    public static final double hBar() {
        return 1.054571596e-34;
    } // [J s]

    public static final double hBarCgs() {
        return 1.0e7 * hBar();
    } // [erg s]

    public static final double hBarUncertainty() {
        return 0.000000082e-34;
    }

    // hc (h * c)
    public static final double hc() {
        return h() * c();
    } // [J m]

    public static final double hcCgs() {
        return hCgs() * cCgs();
    } // [erg cm]

    public static final double hCgs() {
        return 1.0e7 * h();
    } // [erg s]

    public static final double hUncertainty() {
        return 0.00000052e-34;
    }

    public static final double invPi() {
        return 1.0 / pi();
    }

    public static boolean isNaN(double x) {
        return Double.isNaN(x);
    }

    // Boltzmann's constant
    public static final double k() {
        return 1.3806503e-23;
    } // [J K^-1]

    public static final double kCgs() {
        return 1.0e7 * k();
    } // [erg K^-1]

    public static final double kUncertainty() {
        return 0.0000024e-23;
    }

    public static double ldExp(double x, int exp) {
        return x * java.lang.Math.pow(2, exp);
    }

    /**
     * @return natural log of 10 (to convert log to ln)
     */
    public static final double ln10() {
        return 2.30258509299404568402;
    }

    public static double log(double x) {
        return java.lang.Math.log(x);
    }

    public static double log10(double x) {
        return java.lang.Math.log10(x);
    }

    /**
     * @return base-e log of 2
     */
    public static final double log2() {
        return 0.201029996;
    }

    public static double log2(double x) {
        return java.lang.Math.log(x) / log2();
    }

    /**
     * @return base-10 log of e (to convert ln to log)
     */
    public static final double logE() {
        return 0.43429448190325182765;
    }

    //// codegen: double -> float, int, long, short
    public static double max(double a, double b) {
        return a >= b ? a : b;
    }
    //// end codegen

    //// codegen: double -> float, int, long, short
    public static double min(double a, double b) {
        return a <= b ? a : b;
    }
    //// end codegen

    // Molecular weight of dry air
    // 1976 US Standard Atmosphere,
    // also see http://atmos.nmsu.edu/jsdap/encyclopediawork.html
    public static final double mwAir() {
        return 28.9644;
    } // [kg kmol^-1 (or gm mol^-1)]

    // Avogadro constant (Avogadro's Number)
    public static final double nA() {
        return 6.02214199e+23;
    } // [mol^-1]

    public static final double nAUncertainty() {
        return 0.00000047e+23;
    }

    /**
     * Return next prime number after x, unless x is a prime in which case x is returned.
     * 
     * @param x input
     * @return next prime number greater/equal x
     */
    public static long nextPrime(long x) {
        if (x <= 2)
            return 2;
        if (x == 3)
            return 3;
        if (x % 2 == 0)
            x++;

        long sqr = (long) java.lang.Math.sqrt(x) + 1;

        for (;;) {
            long n;
            for (n = 3; (n <= sqr) && ((x % n) != 0); n += 2)
                ;
            if (n > sqr) {
                return x;
            }
            x += 2;
        }
    }

    /**
     * @param x input
     * @return Round to nearest integer. Rounds half integers to the nearest even integer.
     */
    public static int nInt(double x) {
        int i;
        if (x >= 0) {
            i = (int) (x + 0.5);
            if ((x + 0.5 == i) && ((i & 1) > 0))
                i--;
        } else {
            i = (int) (x - 0.5);
            if ((x - 0.5 == i) && ((i & 1) > 0))
                i++;
        }
        return i;
    }

    /**
     * @param x input
     * @return Round to nearest integer. Rounds half integers to the nearest
     */
    public static int nInt(float x) {
        int i;
        if (x >= 0) {
            i = (int) (x + 0.5f);
            if ((x + 0.5f == i) && ((i & 1) > 0))
                i--;
        } else {
            i = (int) (x - 0.5);
            if ((x - 0.5f == i) && ((i & 1) > 0))
                i++;
        }
        return i;
    }

    public static boolean odd(long a) {
        return (a & 1) != 0;
    }

    // Fundamental constants
    public static final double pi() {
        return 3.14159265358979323846;
    }

    public static final double piOver2() {
        return pi() / 2.0;
    }

    public static final double piOver4() {
        return pi() / 4.0;
    }

    public static double pow(double x, double y) {
        return java.lang.Math.pow(x, y);
    }

    // Elementary charge
    public static final double qe() {
        return 1.602176462e-19;
    } // [C], [A s]

    public static final double qeUncertainty() {
        return 0.000000063e-19;
    }

    // universal gas constant (Na * K)
    // http://scienceworld.wolfram.com/physics/UniversalGasConstant.html
    public static final double R() {
        return k() * nA();
    } // [J K^-1 mol^-1]

    public static final double radToDeg() {
        return 180.0 / pi();
    }

    //// codegen: double -> int, long, short
    public static double range(double lb, double ub, double x) {
        return x < lb ? lb : (x > ub ? ub : x);
    }
    //// end codegen

    // Dry Air Gas Constant (R / MWair)
    // http://atmos.nmsu.edu/education_and_outreach/encyclopedia/gas_constant.htm
    public static final double rgAir() {
        return (1000.0 * R()) / mwAir();
    } // [J kg^-1 K^-1]

    public static final double rUncertainty() {
        return R() * ((kUncertainty() / k()) + (nAUncertainty() / nA()));
    }

    // Stefan-Boltzmann constant
    public static final double sigma() {
        return 5.6704e-8;
    } // [W m^-2 K^-4]

    public static final double sigmaUncertainty() {
        return 0.000040e-8;
    }

    // codegen: double -> float, int, long, short
    public static double sign(double a, double b) {
        return (b >= 0) ? abs(a) : -abs(a);
    }
    // end codegen

    public static double sin(double arg0) {
        return java.lang.Math.sin(arg0);
    }

    public static double sinH(double arg0) {
        return java.lang.Math.sinh(arg0);
    }

    public static double sqr(double x) {
        return x * x;
    }

    public static double sqrt(double x) {
        return java.lang.Math.sqrt(x);
    }

    static final double sqrt2 = 1.4142135623730950488016887242097;

    public static double tan(double arg0) {
        return java.lang.Math.tan(arg0);
    }

    public static double tanH(double arg0) {
        return java.lang.Math.tanh(arg0);
    }

    public static final double twoPi() {
        return 2.0 * pi();
    }
}
