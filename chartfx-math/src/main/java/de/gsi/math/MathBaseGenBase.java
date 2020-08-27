/*************************************************************************
 * Originally Based on CERN's ROOT analysis frame work:
 * 
 * @see root.cern.ch for details Copyright (C) 1995-2004, Rene Brun and Fons Rademakers. Authors: Rene Brun, Anna
 *      Kreshuk, Eddy Offermann, Fons Rademakers All rights reserved. Java port and extension by: Ralph J. Steinhagen,
 *      CERN, BE-BI, 2009 For the licensing terms see LICENSE. For the list of contributors see $ROOTSYS/README/CREDITS
 *************************************************************************/

package de.gsi.math;
@SuppressWarnings("PMD.UnnecessaryFullyQualifiedName") // the fully qualified name is needed, because we have our own Math class
public class MathBaseGenBase {
    /**
     * Square root of two
     */
    public static final double SQRT_2 = 1.4142135623730950488016887242097;
    /**
     * PI constant
     */
    public static final double PI = 3.14159265358979323846;
    /**
     * half PI
     */
    public static final double PI_OVER_2 = PI / 2.0;
    /**
     * quarter PI
     */
    public static final double PI_OVER_4 = PI / 4.0;
    /**
     * conversion factor for radians from degrees
     */
    public static final double DEG_TO_RAD = PI / 180.0;
    /**
     * conversion factor for degrees from radians
     */
    public static final double RAD_TO_DEG = 180.0 / PI;
    /**
     * two pi
     */
    public static final double TWO_PI = 2.0 * PI;
    /**
     * inverse of pi
     */
    public static final double INV_PI = 1.0 / PI;
    /**
     * e (base of natural log)
     */
    public static final double E = 2.71828182845904523536;
    /**
     * base-10 log of e (to convert ln to log)
     */
    public static final double LOG_E = 0.43429448190325182765;
    /**
     * base-e log of 2
     */
    public static final double LOG_2 = 0.201029996;
    /**
     * natural log of 10 (to convert log to ln)
     */
    public static final double LN_10 = 2.30258509299404568402;
    /**
     * Euler-Mascheroni Constant
     */
    public static final double EULER_GAMMA = 0.577215664901532860606512090082402431042;
    /**
     * velocity of light in [m s^-1]
     */
    public static final double C = 2.99792458e8;
    /**
     * Uncertainty of the light velocity (assumes exact value for c)
     */
    public static final double C_UNCERTAINTY = 0.0;
    /**
     * velocity of light in [cm s^-1]
     */
    public static final double C_CGS = 100.0 * C;
    /**
     * gravitational constant in [m^3 kg^-1 s^-2]
     */
    public static final double G = 6.673e-11;
    /**
     * uncertainty of the gravitational constant
     */
    public static final double G_UNCERTAINTY = 0.010e-11;
    /**
     * gravitational constant in [cm^3 g^-1 s^-2]
     */
    public static final double G_CGS = G / 1000.0;
    /**
     * G over h-bar C [(GeV/c^2)^-2]
     */
    public static final double GHBAR_C = 6.707e-39;
    /**
     * uncertainty of g over h-bar C
     */
    public static final double GHBAR_C_UNCERTAINTY = 0.010e-39;
    /**
     * standard acceleration of gravity [m s^-2]
     */
    public static final double GN = 9.80665;
    /**
     * uncertainty of the local standard acceleration
     */
    public static final double GN_UNCERTAINTY = 0.0; // exact
    /**
     * Elementary charge [C], [A s]
     */
    public static final double QE = 1.602176462e-19;
    /**
     * uncertainty of the elementary charge
     */
    public static final double QE_UNCERTAINTY = 0.000000063e-19;
    /**
     * Planck's constant [J s]
     */
    public static final double H = 6.62606876e-34;
    /**
     * h-bar (h over 2 pi) [J s]
     */
    public static final double H_BAR = 1.054571596e-34;
    /**
     * uncertainty of h-bar
     */
    public static final double H_BAR_UNCERTAINTY = 0.000000082e-34;
    /**
     * h-bar in [erg s]
     */
    public static final double H_BAR_CGS = 1.0e7 * H_BAR;
    /**
     * hc (h * c) [J m]
     */
    public static final double HC = H * C;
    /**
    * Boltzmann's constant [J K^-1]
     */
    public static final double K = 1.3806503e-23;
    /**
     * uncertainty of Boltzmann's constant
     */
    public static final double K_UNCERTAINTY = 0.0000024e-23;
    /**
     * Boltzmann's constant in  [erg K^-1]
     */
    public static final double K_CGS = 1.0e7 * K;
    /**
     * Stefan-Boltzmann constant [W m^-2 K^-4]
     */
    public static final double SIGMA = 5.6704e-8;
    /**
     * Uncertainty of Stefan-Boltzmann constant
     */
    public static final double SIGMA_UNCERTAINTY = 0.000040e-8;
    /**
     * Avogadro constant (Avogadro's Number) [mol^-1]
     */
    public static final double N_A = 6.02214199e+23;
    /**
     * Uncertainty of the Avogadro constant
     */
    public static final double N_A_UNCERTAINTY = 0.00000047e+23;
    /**
     * universal gas constant (Na * K) [J K^-1 mol^-1]
     * http://scienceworld.wolfram.com/physics/UniversalGasConstant.html
     */
    public static final double R = K * N_A;
    /**
     * Uncertainty of the Dry Air Gas Constant
     */
    public static final double H_UNCERTAINTY = 0.00000052e-34;
    /**
     * Dry Air Gas Constant (R / MWair) [erg s]
     * http://atmos.nmsu.edu/education_and_outreach/encyclopedia/gas_constant.htm
     */
    public static final double H_CGS = 1.0e7 * H;
    /**
     * h_cgs * c_cgs [erg cm]
     */
    public static final double HC_CGS = H_CGS * C_CGS;
    /**
     * Molecular weight of dry air [kg kmol^-1 (or gm mol^-1)]
     *  1976 US Standard Atmosphere,
     *  also see http://atmos.nmsu.edu/jsdap/encyclopediawork.html
     */
    public static final double MW_AIR = 28.9644;
    /**
     *  [J kg^-1 K^-1]
     */
    public static final double RG_AIR = (1000.0 * R) / MW_AIR;
    /**
     *
     */
    public static final double R_UNCERTAINTY = R * ((K_UNCERTAINTY / K) + (N_A_UNCERTAINTY / N_A));

    public static double log(double x) {
        return java.lang.Math.log(x);
    }

    public static double log10(double x) {
        return java.lang.Math.log10(x);
    }

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
            return PI / 2;
        else
            return -PI / 2;
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

    public static boolean isNaN(double x) {
        return Double.isNaN(x);
    }

    public static double ldExp(double x, int exp) {
        return x * java.lang.Math.pow(2, exp);
    }

    public static double log2(double x) {
        return java.lang.Math.log(x) / LOG_2;
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

    public static double pow(double x, double y) {
        return java.lang.Math.pow(x, y);
    }

    //// codegen: double -> int, long, short
    public static double range(double lb, double ub, double x) {
        return x < lb ? lb : (x > ub ? ub : x);
    }
    //// end codegen

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

    public static double tan(double arg0) {
        return java.lang.Math.tan(arg0);
    }

    public static double tanH(double arg0) {
        return java.lang.Math.tanh(arg0);
    }
}
