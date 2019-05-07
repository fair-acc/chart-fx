/*************************************************************************
 * Originally Based on CERN's ROOT analysis frame work:
 * 
 * @see root.cern.ch for details Copyright (C) 1995-2004, Rene Brun and Fons Rademakers. Authors: Rene Brun, Anna
 *      Kreshuk, Eddy Offermann, Fons Rademakers All rights reserved. Java port and extension by: Ralph J. Steinhagen,
 *      CERN, BE-BI, 2009 For the licensing terms see LICENSE. For the list of contributors see $ROOTSYS/README/CREDITS
 *************************************************************************/

package de.gsi.math;

public class TMathConstants {

    // Fundamental constants
    public final static double Pi() {
        return 3.14159265358979323846;
    }

    public final static double TwoPi() {
        return 2.0 * Pi();
    }

    public final static double PiOver2() {
        return Pi() / 2.0;
    }

    public final static double PiOver4() {
        return Pi() / 4.0;
    }

    public final static double InvPi() {
        return 1.0 / Pi();
    }

    public final static double RadToDeg() {
        return 180.0 / Pi();
    }

    public final static double DegToRad() {
        return Pi() / 180.0;
    }

    public final static double Sqrt2() {
        return 1.4142135623730950488016887242097;
    }

    /**
     * e (base of natural log)
     */
    public final static double E() {
        return 2.71828182845904523536;
    }

    /**
     * @return natural log of 10 (to convert log to ln)
     */
    public final static double Ln10() {
        return 2.30258509299404568402;
    }

    /**
     * @return base-10 log of e (to convert ln to log)
     */
    public final static double LogE() {
        return 0.43429448190325182765;
    }

    /**
     * @return base-e log of 2
     */
    public final static double Log2() {
        return 0.201029996;
    }

    /**
     * @return velocity of light in [m s^-1]
     */
    public final static double C() {
        return 2.99792458e8;
    }

    /**
     * @return velocity of light in [cm s^-1]
     */
    public final static double Ccgs() {
        return 100.0 * C();
    }

    public final static double CUncertainty() {
        return 0.0;
    } // exact

    /**
     * @return gravitational constant in [m^3 kg^-1 s^-2]
     */
    public final static double G() {
        return 6.673e-11;
    }

    /**
     * @return gravitational constant in [cm^3 g^-1 s^-2]
     */
    public final static double Gcgs() {
        return G() / 1000.0;
    }

    public final static double GUncertainty() {
        return 0.010e-11;
    }

    //TODO: continue documentation here
    // G over h-bar C
    public final static double GhbarC() {
        return 6.707e-39;
    } // [(GeV/c^2)^-2]

    public final static double GhbarCUncertainty() {
        return 0.010e-39;
    }

    // standard acceleration of gravity
    public final static double Gn() {
        return 9.80665;
    } // [m s^-2]

    public final static double GnUncertainty() {
        return 0.0;
    } // exact

    // Planck's constant
    public final static double H() {
        return 6.62606876e-34;
    } // [J s]

    public final static double Hcgs() {
        return 1.0e7 * H();
    } // [erg s]

    public final static double HUncertainty() {
        return 0.00000052e-34;
    }

    // h-bar (h over 2 pi)
    public final static double Hbar() {
        return 1.054571596e-34;
    } // [J s]

    public final static double Hbarcgs() {
        return 1.0e7 * Hbar();
    } // [erg s]

    public final static double HbarUncertainty() {
        return 0.000000082e-34;
    }

    // hc (h * c)
    public final static double HC() {
        return H() * C();
    } // [J m]

    public final static double HCcgs() {
        return Hcgs() * Ccgs();
    } // [erg cm]

    // Boltzmann's constant
    public final static double K() {
        return 1.3806503e-23;
    } // [J K^-1]

    public final static double Kcgs() {
        return 1.0e7 * K();
    } // [erg K^-1]

    public final static double KUncertainty() {
        return 0.0000024e-23;
    }

    // Stefan-Boltzmann constant
    public final static double Sigma() {
        return 5.6704e-8;
    } // [W m^-2 K^-4]

    public final static double SigmaUncertainty() {
        return 0.000040e-8;
    }

    // Avogadro constant (Avogadro's Number)
    public final static double Na() {
        return 6.02214199e+23;
    } // [mol^-1]

    public final static double NaUncertainty() {
        return 0.00000047e+23;
    }

    // universal gas constant (Na * K)
    // http://scienceworld.wolfram.com/physics/UniversalGasConstant.html
    public final static double R() {
        return K() * Na();
    } // [J K^-1 mol^-1]

    public final static double RUncertainty() {
        return R() * ((KUncertainty() / K()) + (NaUncertainty() / Na()));
    }

    // Molecular weight of dry air
    // 1976 US Standard Atmosphere,
    // also see http://atmos.nmsu.edu/jsdap/encyclopediawork.html
    public final static double MWair() {
        return 28.9644;
    } // [kg kmol^-1 (or gm mol^-1)]

    // Dry Air Gas Constant (R / MWair)
    // http://atmos.nmsu.edu/education_and_outreach/encyclopedia/gas_constant.htm
    public final static double Rgair() {
        return (1000.0 * R()) / MWair();
    } // [J kg^-1 K^-1]

    // Euler-Mascheroni Constant
    public final static double EulerGamma() {
        return 0.577215664901532860606512090082402431042;
    }

    // Elementary charge
    public final static double Qe() {
        return 1.602176462e-19;
    } // [C], [A s]

    public final static double QeUncertainty() {
        return 0.000000063e-19;
    }

    // Trigo
    public static double Sin(double arg0) {
        return Math.sin(arg0);
    }

    public static double Cos(double arg0) {
        return Math.cos(arg0);
    }

    public static double Tan(double arg0) {
        return Math.tan(arg0);
    }

    public static double SinH(double arg0) {
        return Math.sinh(arg0);
    }

    public static double CosH(double arg0) {
        return Math.cosh(arg0);
    }

    public static double TanH(double arg0) {
        return Math.tanh(arg0);
    }

    public static double ASin(double arg0) {
        return Math.asin(arg0);
    }

    public static double ACos(double arg0) {
        return Math.acos(arg0);
    }

    public static double ATan(double arg0) {
        return Math.atan(arg0);
    }

    public static double ATan2(double y, double x) {
        if (x != 0)
            return Math.atan2(y, x);
        else if (y == 0)
            return 0;
        else if (y > 0)
            return Pi() / 2;
        else
            return -Pi() / 2;
    }

    double ASinH(double arg0) {
        if (arg0 == 0.0)
            return 0.0;
        double ax = Math.abs(arg0);
        return Math.log(arg0 + ax * Math.sqrt(1. + 1. / (ax * ax)));
    }

    double ACosH(double arg0) {
        if (arg0 == 0.0)
            return 0.0;
        double ax = Math.abs(arg0);
        return Math.log(arg0 + ax * Math.sqrt(1. - 1. / (ax * ax)));
    }

    double ATanH(double arg0) {
        return Math.log((1 + arg0) / (1 - arg0)) / 2;
    }

    public double Hypot(double x, double y) {
        return Math.hypot(x, y);
    }

    // Misc	
    public static double Sqrt(double x) {
        return Math.sqrt(x);
    }

    public static double Sqr(double x) {
        return x * x;
    }

    public final static double Ceil(double x) {
        return Math.ceil(x);
    }

    public final static int CeilNint(double x) {
        return Nint(Math.ceil(x));
    }

    public static double Floor(double x) {
        return Math.floor(x);
    }

    public static int FloorNint(double x) {
        return Nint(Math.floor(x));
    }

    public static double Exp(double x) {
        return Math.exp(x);
    }

    public static double Ldexp(double x, int exp) {
        return x * Math.pow(2, exp);
    }

    public static double Power(double x, double y) {
        return Math.pow(x, y);
    }

    public static double Log(double x) {
        return Math.log(x);
    }

    public static double Log10(double x) {
        return Math.log10(x);
    }

    public static double Log2(double x) {
        return Math.log(x) / Log2();
    }

    /**
     * @param x
     * @return Round to nearest integer. Rounds half integers to the nearest
     */
    public static int Nint(float x) {
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

    /**
     * @param x
     * @return Round to nearest integer. Rounds half integers to the nearest even integer.
     */
    public static int Nint(double x) {
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

    public static boolean Finite(double x) {
        return !(Double.isInfinite(x) || Double.isNaN(x));
    }

    public static boolean IsNaN(double x) {
        return Double.isNaN(x);
    }

    /**
     * some integer math
     * 
     * @param x
     * @param y
     * @return sqrt(px*px + py*py)
     */
    public static long Hypot(long x, long y) {
        return (long) Math.hypot(x, y);
    }

    public static boolean Even(long a) {
        return !((a & 1) != 0);
    }

    public static boolean Odd(long a) {
        return (a & 1) != 0;
    }

    //---- Abs ---------------------------------------------------------------------
    public static short Abs(short d) {
        return (d >= 0) ? (short) d : (short) (-d);
    }

    public static int Abs(int d) {
        return (d >= 0) ? d : -d;
    }

    public static long Abs(long d) {
        return (d >= 0) ? d : -d;
    }

    public static float Abs(float d) {
        return (d >= 0) ? d : -d;
    }

    public static double Abs(double d) {
        return (d >= 0) ? d : -d;
    }

    //---- Sign --------------------------------------------------------------------
    public static short Sign(short a, short b) {
        return (b >= 0) ? (short) Abs(a) : (short) (-Abs(a));
    }

    public static int Sign(int a, int b) {
        return (b >= 0) ? Abs(a) : -Abs(a);
    }

    public static long Sign(long a, long b) {
        return (b >= 0) ? Abs(a) : -Abs(a);
    }

    public static float Sign(float a, float b) {
        return (b >= 0) ? Abs(a) : -Abs(a);
    }

    public static double Sign(double a, double b) {
        return (b >= 0) ? Abs(a) : -Abs(a);
    }

    //---- Min ---------------------------------------------------------------------
    public static short Min(short a, short b) {
        return a <= b ? a : b;
    }

    public static int Min(int a, int b) {
        return a <= b ? a : b;
    }

    public static long Min(long a, long b) {
        return a <= b ? a : b;
    }

    public static float Min(float a, float b) {
        return a <= b ? a : b;
    }

    public static double Min(double a, double b) {
        return a <= b ? a : b;
    }

    //---- Max ---------------------------------------------------------------------
    public static short Max(short a, short b) {
        return a >= b ? a : b;
    }

    public static int Max(int a, int b) {
        return a >= b ? a : b;
    }

    public static long Max(long a, long b) {
        return a >= b ? a : b;
    }

    public static float Max(float a, float b) {
        return a >= b ? a : b;
    }

    public static double Max(double a, double b) {
        return a >= b ? a : b;
    }

    //---- Range -------------------------------------------------------------------
    public static short Range(short lb, short ub, short x) {
        return x < lb ? lb : (x > ub ? ub : x);
    }

    public static int Range(int lb, int ub, int x) {
        return x < lb ? lb : (x > ub ? ub : x);
    }

    public static long Range(long lb, long ub, long x) {
        return x < lb ? lb : (x > ub ? ub : x);
    }

    public static double Range(double lb, double ub, double x) {
        return x < lb ? lb : (x > ub ? ub : x);
    }

    /**
     * Return next prime number after x, unless x is a prime in which case x is returned.
     * 
     * @param x
     * @return next prime number greater/equal x
     */
    public static long NextPrime(long x) {
        if (x <= 2)
            return 2;
        if (x == 3)
            return 3;
        if (x % 2 == 0)
            x++;

        long sqr = (long) Math.sqrt(x) + 1;

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

}
