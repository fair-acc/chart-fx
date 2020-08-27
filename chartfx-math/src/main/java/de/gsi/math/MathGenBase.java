/*************************************************************************
 * Originally Based on CERN's ROOT analysis frame work:
 * 
 * @see root.cern.ch for details Copyright (C) 1995-2004, Rene Brun and Fons Rademakers. Authors: Rene Brun, Anna
 *      Kreshuk, Eddy Offermann, Fons Rademakers All rights reserved. Java port and extension by: Ralph J. Steinhagen,
 *      CERN, BE-BI, 2009 For the licensing terms see LICENSE. For the list of contributors see $ROOTSYS/README/CREDITS
 *************************************************************************/

package de.gsi.math;

import java.util.Locale;

public class MathGenBase extends MathBase {
    private static final int kWorkMax = 100;

    /**
     * Calculates the Kolmogorov distribution function, which gives the probability that Kolmogorov's test statistic
     * will exceed the value z assuming the null hypothesis. This gives a very powerful test for comparing two
     * one-dimensional distributions. see, for example, Eadie et al, "statistocal Methods in Experimental Physics', pp
     * 269-270). This function returns the confidence level for the null hypothesis, where: z = dn*sqrt(n), and dn is
     * the maximum deviation between a hypothetical distribution function and an experimental distribution with n events
     * NOTE: To compare two experimental distributions with m and n events, use z = sqrt(m*n/(m+n))*dn Accuracy: The
     * function is far too accurate for any imaginable application. Probabilities less than 10^-15 are returned as zero.
     * However, remember that the formula is only valid for "large" n. Theta function inversion formula is used for {@code z <= 1} 
     * This function was translated by Rene Brun from PROBKL in CERNLIB.
     * 
     * @param z input value
     * @return the computed result
     */
    public double kolmogorovProb(double z) {
        final double[] fj = { -2, -8, -18, -32 };
        final double[] r = new double[4];
        final double w = 2.50662827;
        // c1 - -pi**2/8, c2 = 9*c1, c3 = 25*c1
        final double c1 = -1.2337005501361697;
        final double c2 = -11.103304951225528;
        final double c3 = -30.842513753404244;

        final double u = abs(z);
        final double p;
        if (u < 0.2) {
            p = 1;
        } else if (u < 0.755) {
            final double v = 1. / (u * u);
            p = 1 - w * (exp(c1 * v) + exp(c2 * v) + exp(c3 * v)) / u;
        } else if (u < 6.8116) {
            r[1] = 0;
            r[2] = 0;
            r[3] = 0;
            final double v = u * u;
            final int maxj = max(1, nInt(3. / u));
            for (int j = 0; j < maxj; j++) {
                r[j] = exp(fj[j] * v);
            }
            p = 2 * (r[0] - r[1] + r[2] - r[3]);
        } else {
            p = 0;
        }
        return p;
    }

    /**
     * Statistical test whether two one-dimensional sets of points are compatible with coming from the same parent
     * distribution, using the Kolmogorov test. That is, it is used to compare two experimental distributions of
     * unbinned data. Input: a,b: One-dimensional arrays of length na, nb, respectively. The elements of a and b must be
     * given in ascending order option is a character string to specify options "D" Put out a line of "Debug" printout
     * "M" Return the Maximum Kolmogorov distance instead of prob Output: The returned value prob is a calculated
     * confidence level which gives a statistical test for compatibility of a and b. Values of prob close to zero are
     * taken as indicating a small probability of compatibility. For two point sets drawn randomly from the same parent
     * distribution, the value of prob should be uniformly distributed between zero and one. in case of error the
     * function return -1 If the 2 sets have a different number of points, the minimum of the two sets is used. Method:
     * The Kolmogorov test is used. The test statistic is the maximum deviation between the two integrated distribution
     * functions, multiplied by the normalizing factor (rdmax*sqrt(na*nb/(na+nb)). Code adapted by Rene Brun from
     * CERNLIB routine TKOLMO (Fred James) (W.T. Eadie, D. Drijard, F.E. James, M. Roos and B. Sadoulet, Statistical
     * Methods in Experimental Physics, (North-Holland, Amsterdam 1971) 269-271) Method Improvement by Jason A Detwiler
     * (JADetwiler@lbl.gov) ----------------------------------------------------------- The nuts-and-bolts of the
     * KolmogorovTest() algorithm is a for-loop over the two sorted arrays a and b representing empirical distribution
     * functions. The for-loop handles 3 cases: when the next points to be evaluated satisfy a>b, a<b, or a=b: for (int
     * i=0;i<na+nb;i++) { if (a[ia-1] < b[ib-1]) { rdiff -= sa; ia++; if (ia > na) {ok = true; break;} } else if
     * (a[ia-1] > b[ib-1]) { rdiff += sb; ib++; if (ib > nb) {ok = true; break;} } else { rdiff += sb - sa; ia++; ib++;
     * if (ia > na) {ok = true; break;} if (ib > nb) {ok = true; break;} } rdmax = Max(rdmax,Abs(rdiff)); } For the last
     * case, a=b, the algorithm advances each array by one index in an attempt to move through the equality. However,
     * this is incorrect when one or the other of a or b (or both) have a repeated value, call it x. For the KS
     * statistic to be computed properly, rdiff needs to be calculated after all of the a and b at x have been tallied
     * (this is due to the definition of the empirical distribution function; another way to convince yourself that the
     * old CERNLIB method is wrong is that it implies that the function defined as the difference between a and b is
     * multi-valued at x -- besides being ugly, this would invalidate Kolmogorov's theorem). The solution is to just add
     * while-loops into the equality-case handling to perform the tally: } else { double x = a[ia-1]; while(a[ia-1] == x
     * && ia <= na) { rdiff -= sa; ia++; } while(b[ib-1] == x && ib <= nb) { rdiff += sb; ib++; } if (ia > na) {ok =
     * true; break;} if (ib > nb) {ok = true; break;} } NOTE1 A good description of the Kolmogorov test can be seen at:
     * http://www.itl.nist.gov/div898/handbook/eda/section3/eda35g.htm
     * 
     * @param na see above
     * @param a see above
     * @param nb see above
     * @param b see above
     * @param option see above
     * @return the computed result
     */
    public double kolmogorovTest(int na, double[] a, int nb, double[] b, String option) {
        double prob = -1;
        // Require at least two points in each graph
        if (a == null || b == null || na <= 2 || nb <= 2) {
            System.err.println("KolmogorovTest(): Sets must have more than 2 points");
            return prob;
        }
        // Constants needed
        final double rna = na;
        final double rnb = nb;
        final double sa = 1. / rna;
        final double sb = 1. / rnb;
        double rdiff;
        int ia;
        // Starting values for main loop
        int ib;

        if (a[0] < b[0]) {
            rdiff = -sa;
            ia = 2;
            ib = 1;
        } else {
            rdiff = sb;
            ib = 2;
            ia = 1;
        }

        double rdmax = abs(rdiff);

        // Main loop over point sets to find max distance
        // rdiff is the running difference, and rdmax the max.
        boolean ok = false;
        for (int i = 0; i < na + nb; i++) {
            if (a[ia - 1] < b[ib - 1]) {
                rdiff -= sa;
                ia++;
                if (ia > na) {
                    ok = true;
                    break;
                }
            } else if (a[ia - 1] > b[ib - 1]) {
                rdiff += sb;
                ib++;
                if (ib > nb) {
                    ok = true;
                    break;
                }
            } else {
                final double x = a[ia - 1];
                while (a[ia - 1] == x && ia <= na) {
                    rdiff -= sa;
                    ia++;
                }
                while (b[ib - 1] == x && ib <= nb) {
                    rdiff += sb;
                    ib++;
                }
                if (ia > na) {
                    ok = true;
                    break;
                }
                if (ib > nb) {
                    ok = true;
                    break;
                }
            }

            rdmax = max(rdmax, abs(rdiff));
        }
        // Should never terminate this loop with ok = false!

        if (ok) {
            rdmax = max(rdmax, abs(rdiff));
            final double z = rdmax * sqrt(rna * rnb / (rna + rnb));
            prob = kolmogorovProb(z);
        }
        // debug printout
        final String opt = option.toUpperCase(Locale.UK);
        if (opt.contains("D")) {
            System.out.println(String.format(" Kolmogorov Probability = %g, Max Dist = %g", prob, rdmax));
        }
        if (opt.contains("M")) {
            return rdmax;
        } else {
            return prob;
        }
    }

    /**
     * Returns k_th order statistic of the array a of size n (k_th smallest element out of n elements). C-convention is
     * used for array indexing, so if you want the second smallest element, call KOrdStat(n, a, 1). If work is supplied,
     * it is used to store the sorting index and assumed to be &gt;= n. If work=0, local storage is used, either on the
     * stack if n < kWorkMax or on the heap for n &gt;= kWorkMax. Taken from "Numerical Recipes in C++" without the
     * index array implemented by Anna Khreshuk. See also the declarations at the top of this file
     * 
     * @param n see above
     * @param a see above
     * @param k see above
     * @param work see above
     * @return the computed result
     */
    double kOrdStat(int n, double[] a, int k, int[] work) {
        boolean isAllocated = false;
        int i;
        int ir;
        int j;
        int l;
        int mid;
        int arr;
        int[] ind;
        final int[] workLocal = new int[kWorkMax];
        int temp;

        if (work != null) {
            ind = work;
        } else {
            ind = workLocal;
            if (n > kWorkMax) {
                isAllocated = true;
                ind = new int[n];
            }
        }

        for (int ii = 0; ii < n; ii++) {
            ind[ii] = ii;
        }
        final int rk = k;
        l = 0;
        ir = n - 1;
        for (;;) {
            if (ir <= l + 1) { // active partition contains 1 or 2 elements
                if (ir == l + 1 && a[ind[ir]] < a[ind[l]]) {
                    temp = ind[l];
                    ind[l] = ind[ir];
                    ind[ir] = temp;
                }
                final double tmp = a[ind[rk]];
                if (isAllocated) {
                    ind = null;
                }
                return tmp;
            } else {
                mid = (l + ir) >> 1; // choose median of left, center and right
                {
                    temp = ind[mid];
                    ind[mid] = ind[l + 1];
                    ind[l + 1] = temp;
                } // elements as partitioning element arr.
                if (a[ind[l]] > a[ind[ir]]) // also rearrange so that a[l]<=a[l+1]
                {
                    temp = ind[l];
                    ind[l] = ind[ir];
                    ind[ir] = temp;
                }

                if (a[ind[l + 1]] > a[ind[ir]]) {
                    temp = ind[l + 1];
                    ind[l + 1] = ind[ir];
                    ind[ir] = temp;
                }

                if (a[ind[l]] > a[ind[l + 1]]) {
                    temp = ind[l];
                    ind[l] = ind[l + 1];
                    ind[l + 1] = temp;
                }

                i = l + 1; // initialize pointers for partitioning
                j = ir;
                arr = ind[l + 1];
                for (;;) {
                    do {
                        i++;
                    } while (a[ind[i]] < a[arr]);
                    do {
                        j--;
                    } while (a[ind[j]] > a[arr]);
                    if (j < i) {
                        break; // pointers crossed, partitioning complete
                    }
                    {
                        temp = ind[i];
                        ind[i] = ind[j];
                        ind[j] = temp;
                    }
                }
                ind[l + 1] = ind[j];
                ind[j] = arr;
                if (j >= rk) {
                    ir = j - 1; // keep active the partition that
                }
                if (j <= rk) {
                    l = i; // contains the k_th element
                }
            }
        }
    }

    /**
     * Computes sample quantiles, corresponding to the given probabilities Parameters:
     * 
     * @param x -the data sample
     * @param n - its size
     * @param quantiles - computed quantiles are returned in there
     * @param prob - probabilities where to compute quantiles
     * @param nprob - size of prob array
     * @param isSorted - is the input array x sorted? NOTE, that when the input is not sorted, an array of integers of
     *        size n needs to be allocated. It can be passed by the user in parameter index, or, if not passed, it will
     *        be allocated inside the function type - method to compute (from 1 to 9). Following types are provided:
     *        Discontinuous: type=1 - inverse of the empirical distribution function type=2 - like type 1, but with
     *        averaging at discontinuities type=3 - SAS definition: nearest even order statistic Piecwise linear
     *        continuous: In this case, sample quantiles can be obtained by linear interpolation between the k-th order
     *        statistic and p(k). type=4 - linear interpolation of empirical cdf, p(k)=k/n; type=5 - a very popular
     *        definition, p(k) = (k-0.5)/n; type=6 - used by Minitab and SPSS, p(k) = k/(n+1); type=7 - used by S-Plus
     *        and R, p(k) = (k-1)/(n-1); type=8 - resulting sample quantiles are approximately median unbiased
     *        regardless of the distribution of x. p(k) = (k-1/3)/(n+1/3); type=9 - resulting sample quantiles are
     *        approximately unbiased, when the sample comes from Normal distribution. p(k)=(k-3/8)/(n+1/4); default type
     *        = 7 References: 1) Hyndman, R.J and Fan, Y, (1996) "Sample quantiles in statistical packages" American
     *        Statistician, 50, 361-365 2) R Project documentation for the function quantile of package {stats}
     * @param index see above
     * @param type see above
     */
    void quantiles(int n, int nprob, double[] x, double[] quantiles, double[] prob, boolean isSorted, int[] index, int type) {
        if (type < 1 || type > 9) {
            System.err.println("illegal value of type");
            return;
        }
        double g;
        double npm;
        double np;
        double xj;
        double xjj;
        int j;
        int intnpm;
        int[] ind = null;
        if (!isSorted) {
            if (index == null) {
                ind = index;
            } else {
                ind = new int[n];
            }
        }
        npm = 0;
        // Discontinuous functions
        if (type < 4) {
            for (int i = 0; i < nprob; i++) {
                npm = n * prob[i];
                if (npm < 1) {
                    if (isSorted) {
                        quantiles[i] = x[0];
                    } else {
                        quantiles[i] = kOrdStat(n, x, 0, ind);
                    }
                } else {
                    j = max(floorNint(npm) - 1, 0);
                    if (npm - j - 1 > 1e-14) {
                        if (isSorted) {
                            quantiles[i] = x[j + 1];
                        } else {
                            quantiles[i] = kOrdStat(n, x, j + 1, ind);
                        }
                    } else {
                        if (isSorted) {
                            xj = x[j];
                        } else {
                            xj = kOrdStat(n, x, j, ind);
                        }
                        if (type == 1) {
                            quantiles[i] = xj;
                        }
                        if (type == 2) {
                            if (isSorted) {
                                xjj = x[j + 1];
                            } else {
                                xjj = kOrdStat(n, x, j + 1, ind);
                            }
                            quantiles[i] = 0.5 * (xj + xjj);
                        }

                        if (type == 3) {
                            if (!even(j - 1)) {
                                if (isSorted) {
                                    xjj = x[j + 1];
                                } else {
                                    xjj = kOrdStat(n, x, j + 1, ind);
                                }
                                quantiles[i] = xjj;
                            } else {
                                quantiles[i] = xj;
                            }
                        }
                    }
                }
            }
        }

        if (type > 3) {
            for (int i = 0; i < nprob; i++) {
                np = n * prob[i];
                if (np < 1 && type != 7 && type != 4) {
                    quantiles[i] = kOrdStat(n, x, 0, ind);
                } else {
                    if (type == 4) {
                        npm = np;
                    }
                    if (type == 5) {
                        npm = np + 0.5;
                    }
                    if (type == 6) {
                        npm = np + prob[i];
                    }
                    if (type == 7) {
                        npm = np - prob[i] + 1;
                    }
                    if (type == 8) {
                        npm = np + (1. / 3.) * (1 + prob[i]);
                    }
                    if (type == 9) {
                        npm = np + 0.25 * prob[i] + 0.375;
                    }
                    intnpm = floorNint(npm);
                    j = max(intnpm - 1, 0);
                    g = npm - intnpm;
                    if (isSorted) {
                        xj = x[j];
                        xjj = x[j + 1];
                    } else {
                        xj = kOrdStat(n, x, j, ind);
                        xjj = kOrdStat(n, x, j + 1, ind);
                    }
                    quantiles[i] = (1 - g) * xj + g * xjj;
                }
            }
        }
    }

    /**
     * Calculates roots of polynomial of 3rd order a*x^3 + b*x^2 + c*x + d, where a == coef[3], b == coef[2], c ==
     * coef[1], d == coef[0] coef[3] must be different from 0 If the boolean returned by the method is false: ==> there
     * are 3 real roots a,b,c stored in roots If the boolean returned by the method is true: ==> there is one real root
     * a and 2 complex conjugates roots (b+i*c,b-i*c) Author: Francois-Xavier Gentit
     *
     * @param coef cubic polynomial coefficients (see above)
     * @param roots vector containing the computed result
     * @return true if successful
     */
    public boolean rootsCubic(double[] coef, double[] roots) {
        if (coef[3] == 0) {
            return false;
        }
        boolean complex = false;
        final double r;
        final double s;
        final double t;
        final double p;
        final double q;
        double d;
        double ps3;
        double ps33;
        final double qs2;
        double u;
        double v;
        double tmp;
        final double lnu;
        final double lnv;
        final double su;
        final double sv;
        final double y1;
        final double y2;
        final double y3;
        final double a;
        final double b;
        final double c;

        r = coef[2] / coef[3];
        s = coef[1] / coef[3];
        t = coef[0] / coef[3];
        p = s - (r * r) / 3;
        ps3 = p / 3;
        q = (2 * r * r * r) / 27.0 - (r * s) / 3 + t;
        qs2 = q / 2;
        ps33 = ps3 * ps3 * ps3;
        d = ps33 + qs2 * qs2;
        if (d >= 0) {
            complex = true;
            d = sqrt(d);
            u = -qs2 + d;
            v = -qs2 - d;
            tmp = 1. / 3.;
            lnu = log(abs(u));
            lnv = log(abs(v));
            su = sign(1., u);
            sv = sign(1., v);
            u = su * exp(tmp * lnu);
            v = sv * exp(tmp * lnv);
            y1 = u + v;
            y2 = -y1 / 2;
            y3 = ((u - v) * sqrt(3.)) / 2;
            tmp = r / 3;
            a = y1 - tmp;
            b = y2 - tmp;
            c = y3;
        } else {
            final double phi;
            final double cphi;
            final double phis3;
            final double c1;
            final double c2;
            final double c3;
            final double pis3;
            ps3 = -ps3;
            ps33 = -ps33;
            cphi = -qs2 / sqrt(ps33);
            phi = aCos(cphi);
            phis3 = phi / 3;
            pis3 = PI / 3;
            c1 = cos(phis3);
            c2 = cos(pis3 + phis3);
            c3 = cos(pis3 - phis3);
            tmp = sqrt(ps3);
            y1 = 2 * tmp * c1;
            y2 = -2 * tmp * c2;
            y3 = -2 * tmp * c3;
            tmp = r / 3;
            a = y1 - tmp;
            b = y2 - tmp;
            c = y3 - tmp;
        }

        roots[0] = a;
        roots[1] = b;
        roots[2] = c;
        return complex;
    }

    /**
     * Computation of Voigt function (normalised). Voigt is a convolution of gauss(xx) = 1/(sqrt(2*pi)*sigma) *
     * exp(xx*xx/(2*sigma*sigma) and lorentz(xx) = (1/pi) * (lg/2) / (xx*xx + g*g/4) functions. The Voigt function is
     * known to be the real part of Faddeeva function also called complex error function [2]. The algoritm was developed
     * by J. Humlicek [1]. This code is based on fortran code presented by R. J. Wells [2]. Translated and adapted by
     * Miha D. Puc later ported to java by R. Steinhagen To calculate the Faddeeva function with relative error less
     * than 10^(-r). r can be set by the the user subject to the constraints 2 <= r <= 5. [1] J. Humlicek, JQSRT, 21,
     * 437 (1982). [2] R.J. Wells "Rapid Approximation to the Voigt/Faddeeva Function and its Derivatives" JQSRT 62
     * (1999), pp 29-48. http://www-atm.physics.ox.ac.uk/user/wells/voigt.html
     * 
     * @param xx see above
     * @param sigma see above
     * @param lg see above
     * @param r see above
     * @return the computed result
     */
    public double voigt(double xx, double sigma, double lg, int r) {
        if ((sigma < 0 || lg < 0) || (sigma == 0 && lg == 0)) {
            return 0; // Not meant to be for those who want to be thinner than 0
        }

        if (sigma == 0) {
            return lg * 0.159154943 / (xx * xx + lg * lg / 4); // pure Lorentz
        }

        if (lg == 0) { // pure gauss
            return 0.39894228 / sigma * exp(-xx * xx / (2 * sigma * sigma));
        }

        final double x;
        final double y;
        double k;
        x = xx / sigma / 1.41421356;
        y = lg / 2 / sigma / 1.41421356;

        final double r0;
        final double r1;

        if (r < 2) {
            r = 2;
        }
        if (r > 5) {
            r = 5;
        }

        r0 = 1.51 * exp(1.144 * r);
        r1 = 1.60 * exp(0.554 * r);

        // Constants

        final double rrtpi = 0.56418958; // 1/SQRT(pi)

        final double y0; // for CPF12 algorithm
        final double y0py0;
        final double y0q;
        y0 = 1.5;
        y0py0 = y0 + y0;
        y0q = y0 * y0;

        final double c[] = { 1.0117281, -0.75197147, 0.012557727, 0.010022008, -0.00024206814, 0.00000050084806 };
        final double s[] = { 1.393237, 0.23115241, -0.15535147, 0.0062183662, 0.000091908299, -0.00000062752596 };
        final double t[] = { 0.31424038, 0.94778839, 1.5976826, 2.2795071, 3.0206370, 3.8897249 };

        // Local variables

        int j; // Loop variables
        int rg1; // y polynomial flags
        int rg2;
        int rg3;
        final double abx; // --x--, x^2, y^2, y/SQRT(pi)
        final double xq;
        final double yq;
        final double yrrtpi;
        final double xlim0; // --x-- on region boundaries
        double xlim1;
        double xlim2;
        final double xlim3;
        final double xlim4;
        double a0 = 0; // W4 temporary variables
        double d0 = 0;
        double d2 = 0;
        double e0 = 0;
        double e2 = 0;
        double e4 = 0;
        double h0 = 0;
        double h2 = 0;
        double h4 = 0;
        double h6 = 0;
        double p0 = 0;
        double p2 = 0;
        double p4 = 0;
        double p6 = 0;
        double p8 = 0;
        double z0 = 0;
        double z2 = 0;
        double z4 = 0;
        double z6 = 0;
        double z8 = 0;
        final double[] xp = new double[6]; // CPF12 temporary values
        final double[] xm = new double[6];
        final double[] yp = new double[6];
        final double[] ym = new double[6];
        final double[] mq = new double[6];
        final double[] pq = new double[6];
        final double[] mf = new double[6];
        final double[] pf = new double[6];
        double d;
        final double yf;
        final double ypy0;
        final double ypy0q;

        // ***** Start of executable code
        // *****************************************

        rg1 = 1; // Set flags
        rg2 = 1;
        rg3 = 1;
        yq = y * y; // y^2
        yrrtpi = y * rrtpi; // y/SQRT(pi)

        // Region boundaries when both k and L are required or when R<>4

        xlim0 = r0 - y;
        xlim1 = r1 - y;
        xlim3 = 3.097 * y - 0.45;
        xlim2 = 6.8 - y;
        xlim4 = 18.1 * y + 1.65;
        if (y <= 1e-6) { // When y<10^-6 avoid W4 algorithm
            xlim1 = xlim0;
            xlim2 = xlim0;
        }

        abx = abs(x); // |x|
        xq = abx * abx; // x^2
        if (abx > xlim0) { // Region 0 algorithm
            k = yrrtpi / (xq + yq);
        } else if (abx > xlim1) { // Humlicek W4 Region 1
            if (rg1 != 0) { // First point in Region 1
                rg1 = 0;
                a0 = yq + 0.5; // Region 1 y-dependents
                d0 = a0 * a0;
                d2 = yq + yq - 1.0;
            }
            d = rrtpi / (d0 + xq * (d2 + xq));
            k = d * y * (a0 + xq);
        } else if (abx > xlim2) { // Humlicek W4 Region 2
            if (rg2 != 0) { // First point in Region 2
                h0 = 0.5625 + yq * (4.5 + yq * (10.5 + yq * (6.0 + yq)));
                // Region 2 y-dependents
                h2 = -4.5 + yq * (9.0 + yq * (6.0 + yq * 4.0));
                h4 = 10.5 - yq * (6.0 - yq * 6.0);
                h6 = -6.0 + yq * 4.0;
                e0 = 1.875 + yq * (8.25 + yq * (5.5 + yq));
                e2 = 5.25 + yq * (1.0 + yq * 3.0);
                e4 = 0.75 * h6;
            }
            d = rrtpi / (h0 + xq * (h2 + xq * (h4 + xq * (h6 + xq))));
            k = d * y * (e0 + xq * (e2 + xq * (e4 + xq)));
        } else if (abx < xlim3) { // Humlicek W4 Region 3
            if (rg3 != 0) { // First point in Region 3
                // Region 3 y-dependents
                z0 = 272.1014 + y * (1280.829 + y * (2802.870 + y * (3764.966 + y * (3447.629 + y * (2256.981 + y * (1074.409 + y * (369.1989 + y * (88.26741 + y * (13.39880 + y)))))))));
                z2 = 211.678 + y * (902.3066 + y * (1758.336 + y * (2037.310 + y * (1549.675 + y * (793.4273 + y * (266.2987 + y * (53.59518 + y * 5.0)))))));
                z4 = 78.86585 + y * (308.1852 + y * (497.3014 + y * (479.2576 + y * (269.2916 + y * (80.39278 + y * 10.0)))));
                z6 = 22.03523 + y * (55.02933 + y * (92.75679 + y * (53.59518 + y * 10.0)));
                z8 = 1.496460 + y * (13.39880 + y * 5.0);
                p0 = 153.5168 + y * (549.3954 + y * (919.4955 + y * (946.8970 + y * (662.8097 + y * (328.2151 + y * (115.3772 + y * (27.93941 + y * (4.264678 + y * 0.3183291))))))));
                p2 = -34.16955 + y * (-1.322256 + y * (124.5975 + y * (189.7730 + y * (139.4665 + y * (56.81652 + y * (12.79458 + y * 1.2733163))))));
                p4 = 2.584042 + y * (10.46332 + y * (24.01655 + y * (29.81482 + y * (12.79568 + y * 1.9099744))));
                p6 = -0.07272979 + y * (0.9377051 + y * (4.266322 + y * 1.273316));
                p8 = 0.0005480304 + y * 0.3183291;
            }
            d = 1.7724538 / (z0 + xq * (z2 + xq * (z4 + xq * (z6 + xq * (z8 + xq)))));
            k = d * (p0 + xq * (p2 + xq * (p4 + xq * (p6 + xq * p8))));
        } else { // Humlicek CPF12 algorithm
            ypy0 = y + y0;
            ypy0q = ypy0 * ypy0;
            k = 0.0;
            for (j = 0; j <= 5; j++) {
                d = x - t[j];
                mq[j] = d * d;
                mf[j] = 1.0 / (mq[j] + ypy0q);
                xm[j] = mf[j] * d;
                ym[j] = mf[j] * ypy0;
                d = x + t[j];
                pq[j] = d * d;
                pf[j] = 1.0 / (pq[j] + ypy0q);
                xp[j] = pf[j] * d;
                yp[j] = pf[j] * ypy0;
            }
            if (abx <= xlim4) { // Humlicek CPF12 Region I
                for (j = 0; j <= 5; j++) {
                    k = k + c[j] * (ym[j] + yp[j]) - s[j] * (xm[j] - xp[j]);
                }
            } else { // Humlicek CPF12 Region II
                yf = y + y0py0;
                for (j = 0; j <= 5; j++) {
                    k = k + (c[j] * (mq[j] * mf[j] - y0 * ym[j]) + s[j] * yf * xm[j]) / (mq[j] + y0q)
                        + (c[j] * (pq[j] * pf[j] - y0 * yp[j]) - s[j] * yf * xp[j]) / (pq[j] + y0q);
                }
                k = y * k + exp(-xq);
            }
        }

        return k / 2.506628 / sigma; // Normalize by dividing by sqrt(2*pi)*sigma.
    }

    /**
    *  Compute the Integer Order Modified Bessel function I_n(x)
    *  for n=0,1,2,... and any real x.
    *
    *  --- NvE 12-mar-2000 UU-SAP Utrecht
     */
    public static double besselI(int n, double x) {
        if (n < 0) {
            System.err.println("BesselI(): *I* Invalid argument(s) (n,x) = (" + n + ", " + x + ")");
            return 0;
        }
        if (n == 0) {
            return besselI0(x);
        }
        if (n == 1) {
            return besselI1(x);
        }

        if (x == 0) {
            return 0;
        }

        final double kBigPositive = 1.e10;
        if (abs(x) > kBigPositive) {
            return 0;
        }

        final int iacc = 40; // Increase to enhance accuracy

        final double kBigNegative = 1.e-10;

        final double tox = 2 / abs(x);
        double bip = 0;
        double bim = 0;
        double bi = 1;
        double result = 0;
        final int m = 2 * (n + (int) (sqrt(iacc * n)));
        for (int j = m; j >= 1; j--) {
            bim = bip + (j) *tox * bi;
            bip = bi;
            bi = bim;
            // Renormalise to prevent overflows
            if (abs(bi) > kBigPositive) {
                result *= kBigNegative;
                bi *= kBigNegative;
                bip *= kBigNegative;
            }
            if (j == n) {
                result = bip;
            }
        }

        result *= besselI0(x) / bi; // Normalise with BesselI0(x)
        if ((x < 0) && (n % 2 == 1)) {
            result = -result;
        }

        return result;
    }

    /**
    *  --- NvE 12-mar-2000 UU-SAP Utrecht
    *  Parameters of the polynomial approximation
    *  Compute the modified Bessel function I_0(x) for any real x.
    *
    *  --- NvE 12-mar-2000 UU-SAP Utrecht
     */
    public static double besselI0(double x) {
        // Parameters of the polynomial approximation
        final double p1 = 1.0;
        final double p2 = 3.5156229;
        final double p3 = 3.0899424;
        final double p4 = 1.2067492;
        final double p5 = 0.2659732;
        final double p6 = 3.60768e-2;
        final double p7 = 4.5813e-3;

        final double q1 = 0.39894228;
        final double q2 = 1.328592e-2;
        final double q3 = 2.25319e-3;
        final double q4 = -1.57565e-3;
        final double q5 = 9.16281e-3;
        final double q6 = -2.057706e-2;
        final double q7 = 2.635537e-2;
        final double q8 = -1.647633e-2;
        final double q9 = 3.92377e-3;

        final double k1 = 3.75;
        final double ax = abs(x);

        double y = 0;
        double result = 0;

        if (ax < k1) {
            final double xx = x / k1;
            y = xx * xx;
            result = p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * (p6 + y * p7)))));
        } else {
            y = k1 / ax;
            result = (exp(ax) / sqrt(ax))
                     * (q1 + y * (q2 + y * (q3 + y * (q4 + y * (q5 + y * (q6 + y * (q7 + y * (q8 + y * q9))))))));
        }
        return result;
    }

    /**
    * M.Abramowitz and I.A.Stegun, Handbook of Mathematical Functions,
    * Applied Mathematics Series vol. 55 (1964), Washington.
    *
    * --- NvE 12-mar-2000 UU-SAP Utrecht
    * Parameters of the polynomial approximation
    * Compute the modified Bessel function I_1(x) for any real x.
    *
    * M.Abramowitz and I.A.Stegun, Handbook of Mathematical Functions,
    * Applied Mathematics Series vol. 55 (1964), Washington.
    *
    * --- NvE 12-mar-2000 UU-SAP Utrecht
     */
    public static double besselI1(double x) {
        // Parameters of the polynomial approximation
        final double p1 = 0.5;
        final double p2 = 0.87890594;
        final double p3 = 0.51498869;
        final double p4 = 0.15084934;
        final double p5 = 2.658733e-2;
        final double p6 = 3.01532e-3;
        final double p7 = 3.2411e-4;

        final double q1 = 0.39894228;
        final double q2 = -3.988024e-2;
        final double q3 = -3.62018e-3;
        final double q4 = 1.63801e-3;
        final double q5 = -1.031555e-2;
        final double q6 = 2.282967e-2;
        final double q7 = -2.895312e-2;
        final double q8 = 1.787654e-2;
        final double q9 = -4.20059e-3;

        final double k1 = 3.75;
        final double ax = abs(x);

        double y = 0;
        double result = 0;

        if (ax < k1) {
            final double xx = x / k1;
            y = xx * xx;
            result = x * (p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * (p6 + y * p7))))));
        } else {
            y = k1 / ax;
            result = (exp(ax) / sqrt(ax))
                     * (q1 + y * (q2 + y * (q3 + y * (q4 + y * (q5 + y * (q6 + y * (q7 + y * (q8 + y * q9))))))));
            if (x < 0) {
                result = -result;
            }
        }
        return result;
    }

    /**
    * Returns the Bessel function J0(x) for any real x.
     */
    public static double besselJ0(double x) {
        final double ax;
        final double z;
        final double xx;
        final double y;
        final double result;
        final double result1;
        final double result2;
        final double p1 = 57568490574.0;
        final double p2 = -13362590354.0;
        final double p3 = 651619640.7;
        final double p4 = -11214424.18;
        final double p5 = 77392.33017;
        final double p6 = -184.9052456;
        final double p7 = 57568490411.0;
        final double p8 = 1029532985.0;
        final double p9 = 9494680.718;
        final double p10 = 59272.64853;
        final double p11 = 267.8532712;

        final double q1 = 0.785398164;
        final double q2 = -0.1098628627e-2;
        final double q3 = 0.2734510407e-4;
        final double q4 = -0.2073370639e-5;
        final double q5 = 0.2093887211e-6;
        final double q6 = -0.1562499995e-1;
        final double q7 = 0.1430488765e-3;
        final double q8 = -0.6911147651e-5;
        final double q9 = 0.7621095161e-6;
        final double q10 = 0.934935152e-7;
        final double q11 = 0.636619772;

        if ((ax = abs(x)) < 8) {
            y = x * x;
            result1 = p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * p6))));
            result2 = p7 + y * (p8 + y * (p9 + y * (p10 + y * (p11 + y))));
            result = result1 / result2;
        } else {
            z = 8 / ax;
            y = z * z;
            xx = ax - q1;
            result1 = 1 + y * (q2 + y * (q3 + y * (q4 + y * q5)));
            result2 = q6 + y * (q7 + y * (q8 + y * (q9 - y * q10)));
            result = sqrt(q11 / ax) * (cos(xx) * result1 - z * sin(xx) * result2);
        }
        return result;
    }

    /**
    * Returns the Bessel function J1(x) for any real x.
     */
    public static double besselJ1(double x) {
        final double ax;
        final double z;
        final double xx;
        final double y;
        double result;
        final double result1;
        final double result2;
        final double p1 = 72362614232.0;
        final double p2 = -7895059235.0;
        final double p3 = 242396853.1;
        final double p4 = -2972611.439;
        final double p5 = 15704.48260;
        final double p6 = -30.16036606;
        final double p7 = 144725228442.0;
        final double p8 = 2300535178.0;
        final double p9 = 18583304.74;
        final double p10 = 99447.43394;
        final double p11 = 376.9991397;

        final double q1 = 2.356194491;
        final double q2 = 0.183105e-2;
        final double q3 = -0.3516396496e-4;
        final double q4 = 0.2457520174e-5;
        final double q5 = -0.240337019e-6;
        final double q6 = 0.04687499995;
        final double q7 = -0.2002690873e-3;
        final double q8 = 0.8449199096e-5;
        final double q9 = -0.88228987e-6;
        final double q10 = 0.105787412e-6;
        final double q11 = 0.636619772;

        if ((ax = abs(x)) < 8) {
            y = x * x;
            result1 = x * (p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * p6)))));
            result2 = p7 + y * (p8 + y * (p9 + y * (p10 + y * (p11 + y))));
            result = result1 / result2;
        } else {
            z = 8 / ax;
            y = z * z;
            xx = ax - q1;
            result1 = 1 + y * (q2 + y * (q3 + y * (q4 + y * q5)));
            result2 = q6 + y * (q7 + y * (q8 + y * (q9 + y * q10)));
            result = sqrt(q11 / ax) * (cos(xx) * result1 - z * sin(xx) * result2);
            if (x < 0) {
                result = -result;
            }
        }
        return result;
    }

    /**
    * Compute the Integer Order Modified Bessel function K_n(x)
    * for n=0,1,2,... and positive real x.
    *
    * --- NvE 12-mar-2000 UU-SAP Utrecht
    */
    public static double besselK(int n, double x) {
        if (x <= 0 || n < 0) {
            System.err.println("BesselK(): *K* Invalid argument(s) (n,x) = (" + n + ", " + x + ")");
            return 0;
        }

        if (n == 0) {
            return besselK0(x);
        }
        if (n == 1) {
            return besselK1(x);
        }

        // Perform upward recurrence for all x
        final double tox = 2 / x;
        double bkm = besselK0(x);
        double bk = besselK1(x);
        double bkp = 0;
        for (int j = 1; j < n; j++) {
            bkp = bkm + (j) *tox * bk;
            bkm = bk;
            bk = bkp;
        }
        return bk;
    }

    /*
    * Compute the modified Bessel function K_0(x) for positive real x.
    *
    * M.Abramowitz and I.A.Stegun, Handbook of Mathematical Functions,
    * Applied Mathematics Series vol. 55 (1964), Washington.
    *
    * --- NvE 12-mar-2000 UU-SAP Utrecht
    */
    public static double besselK0(double x) {
        if (x <= 0) {
            System.err.println("BesselK0(): *K0* Invalid argument x = " + x);
            return 0;
        }
        // Parameters of the polynomial approximation
        final double p1 = -0.57721566;
        final double p2 = 0.42278420;
        final double p3 = 0.23069756;
        final double p4 = 3.488590e-2;
        final double p5 = 2.62698e-3;
        final double p6 = 1.0750e-4;
        final double p7 = 7.4e-6;

        final double q1 = 1.25331414;
        final double q2 = -7.832358e-2;
        final double q3 = 2.189568e-2;
        final double q4 = -1.062446e-2;
        final double q5 = 5.87872e-3;
        final double q6 = -2.51540e-3;
        final double q7 = 5.3208e-4;

        double y = 0;
        double result = 0;

        if (x <= 2) {
            y = x * x / 4;
            result = (-log(x / 2.) * besselI0(x))
                     + (p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * (p6 + y * p7))))));
        } else {
            y = 2 / x;
            result = (exp(-x) / sqrt(x)) * (q1 + y * (q2 + y * (q3 + y * (q4 + y * (q5 + y * (q6 + y * q7))))));
        }
        return result;
    }

    /**
    * Compute the modified Bessel function K_1(x) for positive real x.
    *
    * M.Abramowitz and I.A.Stegun, Handbook of Mathematical Functions,
    * Applied Mathematics Series vol. 55 (1964), Washington.
    *
    * --- NvE 12-mar-2000 UU-SAP Utrecht
    */
    public static double besselK1(double x) {
        if (x <= 0) {
            System.err.println("BesselK1(): *K1* Invalid argument x = " + x);
            return 0;
        }
        // Parameters of the polynomial approximation
        final double p1 = 1.;
        final double p2 = 0.15443144;
        final double p3 = -0.67278579;
        final double p4 = -0.18156897;
        final double p5 = -1.919402e-2;
        final double p6 = -1.10404e-3;
        final double p7 = -4.686e-5;

        final double q1 = 1.25331414;
        final double q2 = 0.23498619;
        final double q3 = -3.655620e-2;
        final double q4 = 1.504268e-2;
        final double q5 = -7.80353e-3;
        final double q6 = 3.25614e-3;
        final double q7 = -6.8245e-4;

        double y = 0;
        double result = 0;

        if (x <= 2) {
            y = x * x / 4;
            result = (log(x / 2.) * besselI1(x))
                     + (1. / x) * (p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * (p6 + y * p7))))));
        } else {
            y = 2 / x;
            result = (exp(-x) / sqrt(x)) * (q1 + y * (q2 + y * (q3 + y * (q4 + y * (q5 + y * (q6 + y * q7))))));
        }
        return result;
    }

    /**
     * Returns the Bessel function Y0(x) for positive x.
     */
    public static double besselY0(double x) {
        final double z;
        final double xx;
        final double y;
        final double result;
        final double result1;
        final double result2;
        final double p1 = -2957821389.;
        final double p2 = 7062834065.0;
        final double p3 = -512359803.6;
        final double p4 = 10879881.29;
        final double p5 = -86327.92757;
        final double p6 = 228.4622733;
        final double p7 = 40076544269.;
        final double p8 = 745249964.8;
        final double p9 = 7189466.438;
        final double p10 = 47447.26470;
        final double p11 = 226.1030244;
        final double p12 = 0.636619772;

        final double q1 = 0.785398164;
        final double q2 = -0.1098628627e-2;
        final double q3 = 0.2734510407e-4;
        final double q4 = -0.2073370639e-5;
        final double q5 = 0.2093887211e-6;
        final double q6 = -0.1562499995e-1;
        final double q7 = 0.1430488765e-3;
        final double q8 = -0.6911147651e-5;
        final double q9 = 0.7621095161e-6;
        final double q10 = -0.934945152e-7;
        final double q11 = 0.636619772;

        if (x < 8) {
            y = x * x;
            result1 = p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * p6))));
            result2 = p7 + y * (p8 + y * (p9 + y * (p10 + y * (p11 + y))));
            result = (result1 / result2) + p12 * besselJ0(x) * log(x);
        } else {
            z = 8 / x;
            y = z * z;
            xx = x - q1;
            result1 = 1 + y * (q2 + y * (q3 + y * (q4 + y * q5)));
            result2 = q6 + y * (q7 + y * (q8 + y * (q9 + y * q10)));
            result = sqrt(q11 / x) * (sin(xx) * result1 + z * cos(xx) * result2);
        }
        return result;
    }

    /**
     * Returns the Bessel function Y1(x) for positive x.
     */
    public static double besselY1(double x) {
        final double z;
        final double xx;
        final double y;
        final double result;
        final double result1;
        final double result2;
        final double p1 = -0.4900604943e13;
        final double p2 = 0.1275274390e13;
        final double p3 = -0.5153438139e11;
        final double p4 = 0.7349264551e9;
        final double p5 = -0.4237922726e7;
        final double p6 = 0.8511937935e4;
        final double p7 = 0.2499580570e14;
        final double p8 = 0.4244419664e12;
        final double p9 = 0.3733650367e10;
        final double p10 = 0.2245904002e8;
        final double p11 = 0.1020426050e6;
        final double p12 = 0.3549632885e3;
        final double p13 = 0.636619772;
        final double q1 = 2.356194491;
        final double q2 = 0.183105e-2;
        final double q3 = -0.3516396496e-4;
        final double q4 = 0.2457520174e-5;
        final double q5 = -0.240337019e-6;
        final double q6 = 0.04687499995;
        final double q7 = -0.2002690873e-3;
        final double q8 = 0.8449199096e-5;
        final double q9 = -0.88228987e-6;
        final double q10 = 0.105787412e-6;
        final double q11 = 0.636619772;

        if (x < 8) {
            y = x * x;
            result1 = x * (p1 + y * (p2 + y * (p3 + y * (p4 + y * (p5 + y * p6)))));
            result2 = p7 + y * (p8 + y * (p9 + y * (p10 + y * (p11 + y * (p12 + y)))));
            result = (result1 / result2) + p13 * (besselJ1(x) * log(x) - 1 / x);
        } else {
            z = 8 / x;
            y = z * z;
            xx = x - q1;
            result1 = 1 + y * (q2 + y * (q3 + y * (q4 + y * q5)));
            result2 = q6 + y * (q7 + y * (q8 + y * (q9 + y * q10)));
            result = sqrt(q11 / x) * (sin(xx) * result1 + z * cos(xx) * result2);
        }
        return result;
    }

    /**
     * Calculates Beta-function Gamma(p)*Gamma(q)/Gamma(p+q).
     */
    public static double beta(double p, double q) {
        return exp(lnGamma(p) + lnGamma(q) - lnGamma(p + q));
    }

    /**
     * Continued fraction evaluation by modified Lentz's method used in calculation of incomplete Beta function.
     */
    public static double betaCf(double x, double a, double b) {
        final int itmax = 500;
        final double eps = 3.e-14;
        final double fpmin = 1.e-30;

        int m;
        int m2;
        double aa;
        double c;
        double d;
        double del;
        final double qab;
        final double qam;
        final double qap;
        double h;
        qab = a + b;
        qap = a + 1.0;
        qam = a - 1.0;
        c = 1.0;
        d = 1.0 - qab * x / qap;
        if (abs(d) < fpmin) {
            d = fpmin;
        }
        d = 1.0 / d;
        h = d;
        for (m = 1; m <= itmax; m++) {
            m2 = m * 2;
            aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1.0 + aa * d;
            if (abs(d) < fpmin) {
                d = fpmin;
            }
            c = 1 + aa / c;
            if (abs(c) < fpmin) {
                c = fpmin;
            }
            d = 1.0 / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1.0 + aa * d;
            if (abs(d) < fpmin) {
                d = fpmin;
            }
            c = 1.0 + aa / c;
            if (abs(c) < fpmin) {
                c = fpmin;
            }
            d = 1.0 / d;
            del = d * c;
            h *= del;
            if (abs(del - 1) <= eps) {
                break;
            }
        }
        if (m > itmax) {
            System.err.printf("BetaCf: a or b too big, or itmax too small,"
                                      + " a=%e, b=%e, x=%e, h=%e, itmax=%e",
                    a, b,
                    x, h, itmax);
        }
        return h;
    }

    /**
     * Computes the probability density function of the Beta distribution (the distribution function is computed in
     * BetaDistI). The first argument is the point, where the function will be computed, second and third are the
     * function parameters. Since the Beta distribution is bounded on both sides, it's often used to represent processes
     * with natural lower and upper limits.
     * 
     * @param x input value
     * @param p p parameter of beta function
     * @param q q parameter of beta function
     * @return probability density function of the Beta distribution
     */
    public static double betaDist(double x, double p, double q) {
        if ((x < 0) || (x > 1) || (p <= 0) || (q <= 0)) {
            System.err.println("BetaDist(): - parameter value outside allowed range");
            return 0;
        }
        final double beta = beta(p, q);
        final double r = pow(x, p - 1) * pow(1 - x, q - 1) / beta;
        return r;
    }

    /**
     * Computes the distribution function of the Beta distribution.
     * The first argument is the point, where the function will be
     * computed, second and third are the function parameters.
     * Since the Beta distribution is bounded on both sides, it's often
     * used to represent processes with natural lower and upper limits.
     */
    public static double betaDistI(double x, double p, double q) {
        if ((x < 0) || (x > 1) || (p <= 0) || (q <= 0)) {
            System.err.println("BetaDistI(): parameter value outside allowed range");
            return 0;
        }
        final double betai = betaIncomplete(x, p, q);
        return betai;
    }

    /**
    * Calculates the incomplete Beta-function.
    * -- implementation by Anna Kreshuk
     */
    public static double betaIncomplete(double x, double a, double b) {
        if ((x < 0.0) || (x > 1.0)) {
            System.err.println("BetaIncomplete(): X must between 0 and 1");
            return 0.0;
        }
        final double bt;
        if ((x == 0.0) || (x == 1.0)) {
            bt = 0.0;
        } else {
            bt = pow(x, a) * pow(1 - x, b) / beta(a, b);
        }
        if (x < (a + 1) / (a + b + 2)) {
            return bt * betaCf(x, a, b) / a;
        } else {
            return (1 - bt * betaCf(1 - x, b, a) / b);
        }
    }

    //// codegen: double -> float, int, long, short
    /**
     * Binary search in an array of n values to locate value. Array is supposed to be sorted prior to this call. If
     * match is found, function returns position of element. If no match found, function gives nearest element smaller
     * than value.
     *
     * @param array input vector
     * @param value to be searched
     * @return index of found value, -1 otherwise
     */
    public static long binarySearch(double[] array, double value) {
        return binarySearch(array, 0, array.length, value);
    }

    /**
     * Binary search in an array of n values to locate value. Array is supposed to be sorted prior to this call. If
     * match is found, function returns position of element. If no match found, function gives nearest element smaller
     * than value.
     * 
     * @param array input vector
     * @param length &lt;= data.length elements to be used
     * @param offset starting index in the array
     * @param value to be searched
     * @return index of found value, -1 otherwise
     */
    public static long binarySearch(double[] array, int offset, int length, double value) {
        if (array == null || array.length <= 0) {
            return -1;
        }
        final int n = length;
        int nabove;
        int nbelow;
        int middle;
        nabove = n + offset + 1;
        nbelow = offset;

        while (nabove - nbelow > 1) {
            middle = (nabove + nbelow) / 2;
            if (value == array[middle - 1]) {
                return middle - 1;
            }
            if (value < array[middle - 1]) {
                nabove = middle;
            } else {
                nbelow = middle;
            }
        }

        return nbelow - 1;
    }
    //// end codegen

    /**
     * Calculate the binomial coefficient n over k.
     */
    public static double binomial(int n, int k) {
        if (k == 0 || n == k) {
            return 1;
        }
        if (n <= 0 || k < 0 || n < k) {
            return 0;
        }

        final int k1 = min(k, n - k);
        final int k2 = n - k1;
        double fact = k2 + 1;
        for (int i = k1; i > 1; i--) {
            fact *= (double) (k2 + i) / i;
        }
        return fact;
    }

    /**
    * Suppose an event occurs with probability _p_ per trial Then the probability P of its occuring _k_ or more times
    * in _n_ trials is termed a cumulative binomial probability the formula is P = sum_from_j=k_to_n(Binomial(n, j)*
    * *Power(p, j)*Power(1-p, n-j) For _n_ larger than 12 BetaIncomplete is a much better way
    * to evaluate the sum than would be the straightforward sum calculation for _n_ smaller than 12 either method is acceptable
    * ("Numerical Recipes")
    * --implementation by Anna Kreshuk
    */
    public static double binomialI(double p, int n, int k) {
        if (k <= 0) {
            return 1.0;
        }
        if (k > n) {
            return 0.0;
        }
        if (k == n) {
            return pow(p, n);
        }

        return betaIncomplete(p, k, n - k + 1);
    }

    /**
     * Calculate a Breit Wigner function with mean and gamma.
     * 
     * @param x input parameter
     * @param mean centre of distribution
     * @param gamma width of distribution
     * @return the computed result
     */
    public static double breitWigner(double x, double mean, double gamma) {
        final double bw = gamma / ((x - mean) * (x - mean) + gamma * gamma / 4);
        return bw / (2 * PI);
    }

    /**
     * Computes the density of Cauchy distribution at point x The Cauchy distribution, also called Lorentzian
     * distribution, is a continuous distribution describing resonance behavior The mean and standard deviation of the
     * Cauchy distribution are undefined. The practical meaning of this is that collecting 1,000 data points gives no
     * more accurate an estimate of the mean and standard deviation than does a single point. The formula was taken from
     * "Engineering Statistics Handbook" on site http://www.itl.nist.gov/div898/handbook/eda/section3/eda3663.htm
     * Implementation by Anna Kreshuk.
     * 
     * @param x input value
     * @param t the location parameter
     * @param s the scale parameter
     * @return Cauchy distribution at point x
     */
    public static double cauchyDist(double x, double t, double s) {
        final double temp = (x - t) * (x - t) / (s * s);
        final double result = 1 / (s * PI * (1 + temp));
        return result;
    }

    /**
     * Evaluate the quantiles of the chi-squared probability distribution function. Algorithm AS 91 Appl. Statist.
     * (1975) Vol.24, P.35 implemented by Anna Kreshuk. Incorporates the suggested changes in AS R85 (vol.40(1),
     * pp.233-5, 1991)
     * 
     * @param p the probability value, at which the quantile is computed
     * @param ndf number of degrees of freedom
     * @return quantiles of the chi-squared probability distribution
     */
    public static double chisquareQuantile(double p, double ndf) {
        if (ndf <= 0) {
            return 0;
        }
        final double c[] = { 0, 0.01, 0.222222, 0.32, 0.4, 1.24, 2.2, 4.67, 6.66, 6.73, 13.32, 60.0, 70.0, 84.0, 105.0, 120.0,
            127.0, 140.0, 175.0, 210.0, 252.0, 264.0, 294.0, 346.0, 420.0, 462.0, 606.0, 672.0, 707.0, 735.0, 889.0,
            932.0, 966.0, 1141.0, 1182.0, 1278.0, 1740.0, 2520.0, 5040.0 };
        final double e = 5e-7;
        final double aa = 0.6931471806;
        double ch;
        double p1;
        double p2;
        double q;
        double t;
        double a;
        final double x;
        final double g = lnGamma(0.5 * ndf);

        final double xx = 0.5 * ndf;
        final double cp = xx - 1;
        if (ndf >= log(p) * (-c[5])) {
            // starting approximation for ndf less than or equal to 0.32
            if (ndf > c[3]) {
                x = normQuantile(p);
                // starting approximation using Wilson and Hilferty estimate
                p1 = c[2] / ndf;
                ch = ndf * pow((x * sqrt(p1) + 1 - p1), 3);
                if (ch > c[6] * ndf + 6) {
                    ch = -2 * (log(1 - p) - cp * log(0.5 * ch) + g);
                }
            } else {
                ch = c[4];
                a = log(1 - p);
                do {
                    q = ch;
                    p1 = 1 + ch * (c[7] + ch);
                    p2 = ch * (c[9] + ch * (c[8] + ch));
                    t = -0.5 + (c[7] + 2 * ch) / p1 - (c[9] + ch * (c[10] + 3 * ch)) / p2;
                    ch = ch - (1 - exp(a + g + 0.5 * ch + cp * aa) * p2 / p1) / t;
                } while (abs(q / ch - 1) > c[1]);
            }
        } else {
            ch = pow((p * xx * exp(g + xx * aa)), (1. / xx));
            if (ch < e) {
                return ch;
            }
        }
        // call to algorithm AS 239 and calculation of seven term Taylor series
        final int maxit = 20;
        double b;
        double s1;
        double s2;
        double s3;
        double s4;
        double s5;
        double s6;
        for (int i = 0; i < maxit; i++) {
            q = ch;
            p1 = 0.5 * ch;
            p2 = p - gamma(xx, p1);

            t = p2 * exp(xx * aa + g + p1 - cp * log(ch));
            b = t / ch;
            a = 0.5 * t - b * cp;
            s1 = (c[19] + a * (c[17] + a * (c[14] + a * (c[13] + a * (c[12] + c[11] * a))))) / c[24];
            s2 = (c[24] + a * (c[29] + a * (c[32] + a * (c[33] + c[35] * a)))) / c[37];
            s3 = (c[19] + a * (c[25] + a * (c[28] + c[31] * a))) / c[37];
            s4 = (c[20] + a * (c[27] + c[34] * a) + cp * (c[22] + a * (c[30] + c[36] * a))) / c[38];
            s5 = (c[13] + c[21] * a + cp * (c[18] + c[26] * a)) / c[37];
            s6 = (c[15] + cp * (c[23] + c[16] * cp)) / c[38];
            ch = ch + t * (1 + 0.5 * t * s1 - b * cp * (s1 - b * (s2 - b * (s3 - b * (s4 - b * (s5 - b * s6))))));
            if (abs(q / ch - 1) > e) {
                break;
            }
        }
        return ch;
    }

    /**
     * Calculate weighted correlation coefficient x y data and weights w as double
     * 
     * @param x input vector 1
     * @param y input vector 2
     * @param w wheights weight vector
     * @return weighted correlation coefficient
     */
    public static double correlationCoefficient(double[] x, double[] y, double[] w) {
        final int n = x.length;
        if (y.length != n) {
            throw new IllegalArgumentException("x and y array lengths must be equal");
        }
        if (w.length != n) {
            throw new IllegalArgumentException("x and weight array lengths must be equal");
        }

        final double sxy = covariance(x, y, w);
        final double sx = variance(x, w);
        final double sy = variance(y, w);
        return sxy / Math.sqrt(sx * sy);
    }

    /**
     * calculated weighted covariance xx and yy with weights ww
     * 
     * @param xx input vector 1
     * @param yy input vector 2
     * @param ww weights
     * @return weighted covariance
     */
    public static double covariance(double[] xx, double[] yy, double[] ww) {
        final int n = xx.length;
        if (n != yy.length) {
            throw new IllegalArgumentException(
                    "length of x variable array, " + n + " and length of y array, " + yy.length + " are different");
        }
        if (n != ww.length) {
            throw new IllegalArgumentException("length of x variable array, " + n + " and length of weight array, "
                                               + yy.length + " are different");
        }
        final double nn = Math.effectiveSampleNumber(ww);
        final double nterm = nn / (nn - 1.0);
        if (/* n-factor tue: */ true) {
            // nterm = 1.0;
        }
        double sumx = 0.0;
        double sumy = 0.0;
        double sumw = 0.0;
        double meanx;
        double meany;
        final double[] weight = invertAndSquare(ww); // invert and square weights

        for (int i = 0; i < n; i++) {
            sumx += xx[i] * weight[i];
            sumy += yy[i] * weight[i];
            sumw += weight[i];
        }
        meanx = sumx / sumw;
        meany = sumy / sumw;

        double sum = 0.0D;
        for (int i = 0; i < n; i++) {
            sum += weight[i] * (xx[i] - meanx) * (yy[i] - meany);
        }
        return sum * nterm / sumw;
    }

    //// codegen: double -> float
    /**
     * Calculate the Cross Product of two vectors:
     * 
     * @param v1 input vector1
     * @param v2 input vector2
     * @param out output vector
     * @return out = [v1 x v2]
     */
    public static double[] cross(double v1[], double v2[], double out[]) {
        out[0] = v1[1] * v2[2] - v1[2] * v2[1];
        out[1] = v1[2] * v2[0] - v1[0] * v2[2];
        out[2] = v1[0] * v2[1] - v1[1] * v2[0];
        return out;
    }
    //// end codegen

    /**
     * The DiLogarithm function Code translated by from CERNLIB DILOG function C332
     * 
     * @param x input
     * @return the computed result
     */
    public static double diLog(double x) {
        final double hf = 0.5;
        final double pi = PI;
        final double pi2 = pi * pi;
        final double pi3 = pi2 / 3;
        final double pi6 = pi2 / 6;
        final double pi12 = pi2 / 12;
        final double[] c = { +0.42996693560813697, +0.40975987533077105, -0.01858843665014592, +0.00145751084062268,
            -0.00014304184442340, +0.00001588415541880, -0.00000190784959387, +0.00000024195180854,
            -0.00000003193341274, +0.00000000434545063, -0.00000000060578480, +0.00000000008612098,
            -0.00000000001244332, +0.00000000000182256, -0.00000000000027007, +0.00000000000004042,
            -0.00000000000000610, +0.00000000000000093, -0.00000000000000014, +0.00000000000000002 };

        final double t;
        double h;
        final double y;
        final double s;
        double a;
        final double alfa;
        double b1;
        double b2;
        double b0 = 0.0;

        if (x == 1) {
            h = pi6;
        } else if (x == -1) {
            h = -pi12;
        } else {
            t = -x;
            if (t <= -2) {
                y = -1 / (1 + t);
                s = 1;
                b1 = log(-t);
                b2 = log(1 + 1 / t);
                a = -pi3 + hf * (b1 * b1 - b2 * b2);
            } else if (t < -1) {
                y = -1 - t;
                s = -1;
                a = log(-t);
                a = -pi6 + a * (a + log(1 + 1 / t));
            } else if (t <= -0.5) {
                y = -(1 + t) / t;
                s = 1;
                a = log(-t);
                a = -pi6 + a * (-hf * a + log(1 + t));
            } else if (t < 0) {
                y = -t / (1 + t);
                s = -1;
                b1 = log(1 + t);
                a = hf * b1 * b1;
            } else if (t <= 1) {
                y = t;
                s = 1;
                a = 0;
            } else {
                y = 1 / t;
                s = -1;
                b1 = log(t);
                a = pi6 + hf * b1 * b1;
            }

            h = y + y - 1;
            alfa = h + h;
            b1 = 0;
            b2 = 0;

            for (int i = 19; i >= 0; i--) {
                b0 = c[i] + alfa * b1 - b2;
                b2 = b1;
                b1 = b0;
            }

            h = -(s * (b0 - h * b2) + a);
        }

        return h;
    }

    /**
     * Calculation of the effective sample number (double)
     */
    public static double effectiveSampleNumber(double[] ww) {
        final double[] weight = ww.clone();
        for (int i = 0; i < weight.length; i++) {
            weight[i] = 1.0 / MathBase.sqr(weight[i]);
        }
        final int n = weight.length;

        double nEff;
        double sum2w = 0.0D;
        double sumw2 = 0.0D;
        for (int i = 0; i < n; i++) {
            sum2w += weight[i];
            sumw2 += weight[i] * weight[i];
        }
        sum2w *= sum2w;
        nEff = sum2w / sumw2;

        return nEff;
    }

    /**
     * Computation of the error function erf(x). Erf(x) = (2/sqrt(pi)) Integral(exp(-t^2))dt between 0 and x --- NvE
     * 14-nov-1998 UU-SAP Utrecht
     * 
     * @param x input
     * @return the computed result
     */
    public static double erf(double x) {
        return (1 - erfc(x));
    }

    /**
     * Compute the complementary error function erfc(x). Erfc(x) = (2/sqrt(pi)) Integral(exp(-t^2))dt between x and
     * infinity Nve 14-nov-1998 UU-SAP Utrecht
     * 
     * @param x input parameter
     * @return the computed result
     */
    public static double erfc(double x) {
        double v = 1; // The return value
        final double z = Math.abs(x);

        if (z <= 0) {
            return v; // erfc(0)=1
        }

        // The parameters of the Chebyshev fit
        final double a1 = -1.26551223;
        final double a2 = 1.00002368;
        final double a3 = 0.37409196;
        final double a4 = 0.09678418;
        final double a5 = -0.18628806;
        final double a6 = 0.27886807;
        final double a7 = -1.13520398;
        final double a8 = 1.48851587;
        final double a9 = -0.82215223;
        final double a10 = 0.17087277;

        final double t = 1 / (1 + 0.5 * z);

        v = t * exp((-z * z) + a1 + t * (a2 + t * (a3 + t * (a4 + t * (a5 + t * (a6 + t * (a7 + t * (a8 + t * (a9 + t * a10)))))))));

        if (x < 0) {
            v = 2 - v; // erfc(-x)=2-erfc(x)
        }

        return v;
    }

    /**
     * returns the inverse error function
     * 
     * @param x must be &lt;-1&lt;x&lt;1
     * @return the computed result
     */
    public static double erfInverse(double x) {
        final double kEps = 1e-14;
        final double kConst = 0.8862269254527579; // sqrt(pi)/2.0

        if (abs(x) <= kEps) {
            return kConst * x;
        }

        // Newton iterations
        double erfi;
        double derfi;
        double y0;
        double y1;
        double dy0;
        double dy1;
        final int kMaxit = 50;
        if (abs(x) < 1.0) {
            erfi = kConst * abs(x);
            y0 = erf(0.9 * erfi);
            derfi = 0.1 * erfi;
            for (int iter = 0; iter < kMaxit; iter++) {
                y1 = 1. - erfc(erfi);
                dy1 = abs(x) - y1;
                if (abs(dy1) < kEps) {
                    if (x < 0) {
                        return -erfi;
                    } else {
                        return erfi;
                    }
                }
                dy0 = y1 - y0;
                derfi *= dy1 / dy0;
                y0 = y1;
                erfi += derfi;

                if (abs(derfi / erfi) < kEps) {
                    if (x < 0) {
                        return -erfi;
                    } else {
                        return erfi;
                    }
                }
            }
        }

        return Double.NaN; // did not converge
    }

    /**
     * Compute factorial(n).
     * 
     * @param n input parameter
     * @return the computed result
     */
    public static double factorial(int n) {
        if (n <= 0) {
            return 1.;
        }
        double x = 1;
        int b = 0;
        do {
            b++;
            x *= b;
        } while (b != n);
        return x;
    }

    /**
     * Computes the density function of F-distribution * (probability function, integral of density, is computed in FDistI).
     *
     * Parameters N and M stand for degrees of freedom of chi-squares * mentioned above parameter F is the actual
     * variable x of the density function p(x) and the point at which the density function is calculated.
     *
     * About F distribution:
     * F-distribution arises in testing whether two random samples have the same variance. It is the ratio of two
     * chi-square distributions, with N and M degrees of freedom respectively, where each chi-square is first divided
     * by it's number of degrees of freedom.
     *
     * Implementation by Anna Kreshuk.
     */
    public static double fDist(double F, double N, double M) {
        if ((F < 0) || (N < 1) || (M < 1)) {
            return 0;
        } else {
            final double denom = gamma(N / 2) * gamma(M / 2) * pow(M + N * F, (N + M) / 2);
            final double div = gamma((N + M) / 2) * pow(N, N / 2) * pow(M, M / 2) * pow(F, 0.5 * N - 1);
            return div / denom;
        }
    }

    /**
     * Calculates the cumulative distribution function of F-distribution, this function occurs in the statistical test
     * of whether two observed samples have the same variance. For this test a certain statistic F, the ratio of
     * observed dispersion of the first sample to that of the second sample, is calculated. N and M stand for numbers
     * of degrees of freedom in the samples 1-FDistI() is the significance level at which the hypothesis "1 has smaller
     * variance than 2" can be rejected. A small numerical value of 1 - FDistI() implies a very significant rejection,
     * in turn implying high confidence in the hypothesis "1 has variance greater than 2".
     *
     * Implementation by Anna Kreshuk.
     */
    public static double fDistI(double F, double N, double M) {
        final double fi = 1 - betaIncomplete((M / (M + N * F)), M * 0.5, N * 0.5);
        return fi;
    }

    /**
     * Computation of the normal frequency function freq(x). Freq(x) = (1/sqrt(2pi)) Integral(exp(-t^2/2))dt between
     * -infinity and x. Translated from CERNLIB C300 by Rene Brun.
     * 
     * @param x input parameter
     * @return the computed result
     */
    public static double freq(double x) {
        final double c1 = 0.56418958354775629;
        final double w2 = 1.41421356237309505;

        final double p10 = 2.4266795523053175e+2;
        final double q10 = 2.1505887586986120e+2;
        final double p11 = 2.1979261618294152e+1;
        final double q11 = 9.1164905404514901e+1;
        final double p12 = 6.9963834886191355e+0;
        final double q12 = 1.5082797630407787e+1;
        final double p13 = -3.5609843701815385e-2;
        final double q13 = 1;

        final double p20 = 3.00459261020161601e+2;
        final double q20 = 3.00459260956983293e+2;
        final double p21 = 4.51918953711872942e+2;
        final double q21 = 7.90950925327898027e+2;
        final double p22 = 3.39320816734343687e+2;
        final double q22 = 9.31354094850609621e+2;
        final double p23 = 1.52989285046940404e+2;
        final double q23 = 6.38980264465631167e+2;
        final double p24 = 4.31622272220567353e+1;
        final double q24 = 2.77585444743987643e+2;
        final double p25 = 7.21175825088309366e+0;
        final double q25 = 7.70001529352294730e+1;
        final double p26 = 5.64195517478973971e-1;
        final double q26 = 1.27827273196294235e+1;
        final double p27 = -1.36864857382716707e-7;
        final double q27 = 1;

        final double p30 = -2.99610707703542174e-3;
        final double q30 = 1.06209230528467918e-2;
        final double p31 = -4.94730910623250734e-2;
        final double q31 = 1.91308926107829841e-1;
        final double p32 = -2.26956593539686930e-1;
        final double q32 = 1.05167510706793207e+0;
        final double p33 = -2.78661308609647788e-1;
        final double q33 = 1.98733201817135256e+0;
        final double p34 = -2.23192459734184686e-2;
        final double q34 = 1;

        final double v = abs(x) / w2;
        final double vv = v * v;
        double ap;
        double aq;
        final double h;
        final double hc;
        final double y;

        if (v < 0.5) {
            y = vv;
            ap = p13;
            aq = q13;
            ap = p12 + y * ap;
            ap = p11 + y * ap;
            ap = p10 + y * ap;
            aq = q12 + y * aq;
            aq = q11 + y * aq;
            aq = q10 + y * aq;
            h = v * ap / aq;
            hc = 1 - h;
        } else if (v < 4) {
            ap = p27;
            aq = q27;
            ap = p26 + v * ap;
            ap = p25 + v * ap;
            ap = p24 + v * ap;
            ap = p23 + v * ap;
            ap = p22 + v * ap;
            ap = p21 + v * ap;
            ap = p20 + v * ap;
            aq = q26 + v * aq;
            aq = q25 + v * aq;
            aq = q24 + v * aq;
            aq = q23 + v * aq;
            aq = q22 + v * aq;
            aq = q21 + v * aq;
            aq = q20 + v * aq;
            hc = exp(-vv) * ap / aq;
            h = 1 - hc;
        } else {
            y = 1 / vv;
            ap = p34;
            aq = q34;
            ap = p33 + y * ap;
            ap = p32 + y * ap;
            ap = p31 + y * ap;
            ap = p30 + y * ap;
            aq = q33 + y * aq;
            aq = q32 + y * aq;
            aq = q31 + y * aq;
            aq = q30 + y * aq;
            hc = exp(-vv) * (c1 + y * ap / aq) / v;
            h = 1 - hc;
        }

        if (x > 0) {
            return 0.5 + 0.5 * h;
        } else {
            return 0.5 * hc;
        }
    }

    /**
     * Computation of the incomplete gamma function P(a,x) via its continued fraction representation. --- Nve
     * 14-nov-1998 UU-SAP Utrecht
     * 
     * @param a input parameter
     * @param x input parameter
     * @return the computed result
     */
    public static double gamCf(double a, double x) {
        if (a <= 0 || x <= 0) {
            return 0;
        }

        final int itmax = 100; // Maximum number of iterations
        final double eps = 3.e-14; // Relative accuracy
        final double fpmin = 1.e-30; // Smallest double value allowed here

        final double gln = lnGamma(a);
        double b = x + 1 - a;
        double c = 1 / fpmin;
        double d = 1 / b;
        double h = d;
        double an;
        double del;
        for (int i = 1; i <= itmax; i++) {
            an = (-i) * ((i) -a);
            b += 2;
            d = an * d + b;
            if (abs(d) < fpmin) {
                d = fpmin;
            }
            c = b + an / c;
            if (abs(c) < fpmin) {
                c = fpmin;
            }
            d = 1 / d;
            del = d * c;
            h *= del;
            if (abs(del - 1) < eps) {
                break;
                // if (i==itmax) cout << "*GamCf(a,x)* a too large or itmax too
                // small" << endl;
            }
        }
        final double v = exp(-x + a * log(x) - gln) * h;
        return (1 - v);
    }

    /**
     * Computation of gamma(z) for all z&gt;0. C.Lanczos, SIAM Journal of Numerical Analysis B1 (1964), 86. --- Nve
     * 14-nov-1998 UU-SAP Utrecht
     * 
     * @param z input parameter
     * @return gamma(z)
     */
    public static double gamma(double z) {
        if (z <= 0) {
            return 0;
        }

        final double v = lnGamma(z);
        return exp(v);
    }

    /**
     * Computation of the normalized lower incomplete gamma function P(a,x) as defined in the Handbook of Mathematical
     * Functions by Abramowitz and Stegun, formula 6.5.1 on page 260 . Its normalization is such that Gamma(a,+infinity)
     * = 1 . Begin_Latex P(a, x) = #frac{1}{#Gamma(a) } #int_{0}^{x} t^{a-1} e^{-t} dt End_Latex --- Nve 14-nov-1998
     * UU-SAP Utrecht
     * 
     * @param a input parameter
     * @param x input parameter
     * @return the computed result
     */
    public static double gamma(double a, double x) {
        if (a <= 0 || x <= 0) {
            return 0;
        }

        if (x < (a + 1)) {
            return gamSer(a, x);
        } else {
            return gamCf(a, x);
        }
    }

    /**
     * Computes the density function of Gamma distribution at point x.
     *
     * The formula was taken from "Engineering Statistics Handbook" on site
     * http://www.itl.nist.gov/div898/handbook/eda/section3/eda366b.htm
     * Implementation by Anna Kreshuk.
     *
     * @param x
     * @param gamma shape parameter
     * @param mu location parameter
     * @param beta scale parameter
     */
    public static double gammaDist(double x, double gamma, double mu, double beta) {
        if ((x < mu) || (gamma <= 0) || (beta <= 0)) {
            System.err.println("GammaDist(): illegal parameter values");
            return 0;
        }
        final double temp = (x - mu) / beta;
        final double temp2 = beta * gamma(gamma);
        final double result = (pow(temp, gamma - 1) * exp(-temp)) / temp2;
        return result;
    }

    /**
     * Computation of the incomplete gamma function P(a,x) via its series representation. --- Nve 14-nov-1998 UU-SAP
     * Utrecht
     * 
     * @param a input parameter
     * @param x input parameter
     * @return the computed result
     */
    public static double gamSer(double a, double x) {
        if (a <= 0 || x <= 0) {
            return 0;
        }
        final int itmax = 100; // Maximum number of iterations
        final double eps = 3.e-14; // Relative accuracy

        final double gln = lnGamma(a);
        double ap = a;
        double sum = 1 / a;
        double del = sum;
        for (int n = 1; n <= itmax; n++) {
            ap += 1;
            del = del * x / ap;
            sum += del;
            if (abs(del) < abs(sum * eps)) {
                break;
                // if (n==itmax) cout << "*GamSer(a,x)* a too large or itmax too
                // small" << endl;
            }
        }
        final double v = sum * exp(-x + a * log(x) - gln);
        return v;
    }

    /**
     * Calculate a Gaussian function with mean and sigma. If norm=true (default is false) the result is divided by
     * sqrt(2*Pi)*sigma.
     * 
     * @param x input parameter
     * @param mean centre of distribution
     * @param sigma width of distribution
     * @param norm normalisation factor
     * @return the computed result
     */
    public static double gauss(double x, double mean, double sigma, boolean norm) {
        if (sigma == 0) {
            return 1.e30;
        }
        final double arg = (x - mean) / sigma;
        final double res = exp(-0.5 * arg * arg);
        if (!norm) {
            return res;
        }
        return res / (2.50662827463100024 * sigma); // sqrt(2*Pi)=2.50662827463100024
    }

    //// codegen: double -> float, int, long, short
    /**
     * geometric_mean = (\Prod_{i=0}^{n-1} \abs{a[i]})^{1/n}
     * 
     * @param a input vector
     * @param offset starting index in the array
     * @param length &lt;= data.length elements to be used
     * @return geometric mean of an array a with length n.
     */
    public static double geometricMean(double[] a, int offset, int length) {
        if (a == null || a.length <= 0) {
            return -1;
        }
        double logsum = 0.0; //// codegen: skip all

        // use logarithm representation: product in linear regime -> sum in logarithmic scale
        for (int i = offset; i < length + offset; i++) {
            if (a[i] == 0) {
                return 0;
            }
            final double absa = Math.abs(a[i]); //// codegen: skip all
            logsum += Math.log(absa);
        }

        return Math.exp(logsum / length); //// codegen: returncast all
    }

    public static double geometricMean(double[] data) {
        return geometricMean(data, 0, data.length);
    }
    //// end codegen

    private static double[] invertAndSquare(double[] values) {
        final double[] ret = new double[values.length];

        // TODO: check NaN, inf... cases in other derived function
        for (int i = 0; i < values.length; i++) {
            final double val = values[i];
            if (val == 0.0) {
                ret[i] = Double.POSITIVE_INFINITY;
            } else if (Double.isNaN(val)) {
                ret[i] = Double.NaN;
            } else if (Double.isInfinite(val)) {
                ret[i] = 0.0;
            } else {
                ret[i] = 1.0 / MathBase.pow(val, 2);
            }
        }
        return ret;
    }

    //// codegen: double -> float, int
    /**
     * Function which returns true if point xp,yp lies inside the polygon defined by the np points in arrays x and y,
     * false otherwise NOTE that the polygon must be a closed polygon (1st and last point must be identical).
     * 
     * @param xp test point coordinate x
     * @param yp test point coordinate y
     * @param np number of polygon edges
     * @param x x coordinates of polygon
     * @param y y coordinates of polygon
     * @return true if point xp,yp lies inside
     */
    public static boolean isInside(double xp, double yp, int np, double x[], double y[]) {
        double xint; //// codegen: skip all
        int inter = 0;
        double xn;
        double yn;
        for (int i = 0; i < np; i++) {
            if (i < np - 1) {
                xn = x[i + 1];
                yn = y[i + 1];
            } else {
                xn = x[0];
                yn = y[0];
            }
            if (y[i] == yn) {
                continue;
            }
            if (yp <= y[i] && yp <= yn) {
                continue;
            }
            if (y[i] < yp && yn < yp) {
                continue;
            }
            xint = x[i] + (yp - y[i]) * (xn - x[i]) / ((yn - y[i])); //// codegen: subst:int:/ (:/ ((double)
            if (xp < xint) {
                inter++;
            }
        }

        return inter % 2 > 0;
    }
    //// end codegen

    /**
     * The LANDAU function with mpv(most probable value) and sigma. This function has been adapted from the CERNLIB
     * routine G110 denlan. If norm=true (default is false) the result is divided by sigma
     * 
     * @param x input variable
     * @param mpv most probable value
     * @param sigma width of distribution
     * @param norm normalisation of distribution
     * @return the computed result
     */
    public static double landau(double x, double mpv, double sigma, boolean norm) {
        if (sigma <= 0) {
            return 0;
        }
        final double[] p1 = { 0.4259894875, -0.1249762550, 0.03984243700, -0.006298287635, 0.001511162253 };
        final double[] q1 = { 1.0, -0.3388260629, 0.09594393323, -0.01608042283, 0.003778942063 };

        final double[] p2 = { 0.1788541609, 0.1173957403, 0.01488850518, -0.001394989411, 0.0001283617211 };
        final double[] q2 = { 1.0, 0.7428795082, 0.3153932961, 0.06694219548, 0.008790609714 };

        final double[] p3 = { 0.1788544503, 0.09359161662, 0.006325387654, 0.00006611667319, -0.000002031049101 };
        final double[] q3 = { 1.0, 0.6097809921, 0.2560616665, 0.04746722384, 0.006957301675 };

        final double[] p4 = { 0.9874054407, 118.6723273, 849.2794360, -743.7792444, 427.0262186 };
        final double[] q4 = { 1.0, 106.8615961, 337.6496214, 2016.712389, 1597.063511 };

        final double[] p5 = { 1.003675074, 167.5702434, 4789.711289, 21217.86767, -22324.94910 };
        final double[] q5 = { 1.0, 156.9424537, 3745.310488, 9834.698876, 66924.28357 };

        final double[] p6 = { 1.000827619, 664.9143136, 62972.92665, 475554.6998, -5743609.109 };
        final double[] q6 = { 1.0, 651.4101098, 56974.73333, 165917.4725, -2815759.939 };

        final double[] a1 = { 0.04166666667, -0.01996527778, 0.02709538966 };
        final double[] a2 = { -1.845568670, -4.284640743 };

        final double v = (x - mpv) / sigma;
        final double u;
        final double ue;
        final double us;
        final double den;
        if (v < -5.5) {
            u = exp(v + 1.0);
            if (u < 1e-10) {
                return 0.0;
            }
            ue = exp(-1 / u);
            us = sqrt(u);
            den = 0.3989422803 * (ue / us) * (1 + (a1[0] + (a1[1] + a1[2] * u) * u) * u);
        } else if (v < -1) {
            u = exp(-v - 1);
            den = exp(-u) * sqrt(u) * (p1[0] + (p1[1] + (p1[2] + (p1[3] + p1[4] * v) * v) * v) * v)
                  / (q1[0] + (q1[1] + (q1[2] + (q1[3] + q1[4] * v) * v) * v) * v);
        } else if (v < 1) {
            den = (p2[0] + (p2[1] + (p2[2] + (p2[3] + p2[4] * v) * v) * v) * v)
                  / (q2[0] + (q2[1] + (q2[2] + (q2[3] + q2[4] * v) * v) * v) * v);
        } else if (v < 5) {
            den = (p3[0] + (p3[1] + (p3[2] + (p3[3] + p3[4] * v) * v) * v) * v)
                  / (q3[0] + (q3[1] + (q3[2] + (q3[3] + q3[4] * v) * v) * v) * v);
        } else if (v < 12) {
            u = 1 / v;
            den = u * u * (p4[0] + (p4[1] + (p4[2] + (p4[3] + p4[4] * u) * u) * u) * u)
                  / (q4[0] + (q4[1] + (q4[2] + (q4[3] + q4[4] * u) * u) * u) * u);
        } else if (v < 50) {
            u = 1 / v;
            den = u * u * (p5[0] + (p5[1] + (p5[2] + (p5[3] + p5[4] * u) * u) * u) * u)
                  / (q5[0] + (q5[1] + (q5[2] + (q5[3] + q5[4] * u) * u) * u) * u);
        } else if (v < 300) {
            u = 1 / v;
            den = u * u * (p6[0] + (p6[1] + (p6[2] + (p6[3] + p6[4] * u) * u) * u) * u)
                  / (q6[0] + (q6[1] + (q6[2] + (q6[3] + q6[4] * u) * u) * u) * u);
        } else {
            u = 1 / (v - v * log(v) / (v + 1));
            den = u * u * (1 + (a2[0] + a2[1] * u) * u);
        }

        if (!norm) {
            return den;
        }

        return den / sigma;
    }

    /**
     * Returns the value of the Landau distribution function at point x.
     * The algorithm was taken from the Cernlib function dislan(G110)
     * Reference: K.S.Kolbig and B.Schorr, "A program package for the Landau distribution", Computer Phys.Comm., 31(1984), 97-111
     */
    public static double landauI(double x) {
        final double[] p1 = { 0.2514091491e+0, -0.6250580444e-1, 0.1458381230e-1, -0.2108817737e-2, 0.7411247290e-3 };
        final double[] q1 = { 1.0, -0.5571175625e-2, 0.6225310236e-1, -0.3137378427e-2, 0.1931496439e-2 };

        final double[] p2 = { 0.2868328584e+0, 0.3564363231e+0, 0.1523518695e+0, 0.2251304883e-1 };
        final double[] q2 = { 1.0, 0.6191136137e+0, 0.1720721448e+0, 0.2278594771e-1 };

        final double[] p3 = { 0.2868329066e+0, 0.3003828436e+0, 0.9950951941e-1, 0.8733827185e-2 };
        final double[] q3 = { 1.0, 0.4237190502e+0, 0.1095631512e+0, 0.8693851567e-2 };

        final double[] p4 = { 0.1000351630e+1, 0.4503592498e+1, 0.1085883880e+2, 0.7536052269e+1 };
        final double[] q4 = { 1.0, 0.5539969678e+1, 0.1933581111e+2, 0.2721321508e+2 };

        final double[] p5 = { 0.1000006517e+1, 0.4909414111e+2, 0.8505544753e+2, 0.1532153455e+3 };
        final double[] q5 = { 1.0, 0.5009928881e+2, 0.1399819104e+3, 0.4200002909e+3 };

        final double[] p6 = { 0.1000000983e+1, 0.1329868456e+3, 0.9162149244e+3, -0.9605054274e+3 };
        final double[] q6 = { 1.0, 0.1339887843e+3, 0.1055990413e+4, 0.5532224619e+3 };

        final double[] a1 = { 0, -0.4583333333e+0, 0.6675347222e+0, -0.1641741416e+1 };

        final double[] a2 = { 0, 1.0, -0.4227843351e+0, -0.2043403138e+1 };

        final double u;
        final double v;
        final double lan;
        v = x;
        if (v < -5.5) {
            u = exp(v + 1);
            lan = 0.3989422803 * exp(-1. / u) * sqrt(u) * (1 + (a1[1] + (a1[2] + a1[3] * u) * u) * u);
        } else if (v < -1) {
            u = exp(-v - 1);
            lan = (exp(-u) / sqrt(u)) * (p1[0] + (p1[1] + (p1[2] + (p1[3] + p1[4] * v) * v) * v) * v)
                  / (q1[0] + (q1[1] + (q1[2] + (q1[3] + q1[4] * v) * v) * v) * v);
        } else if (v < 1) {
            lan = (p2[0] + (p2[1] + (p2[2] + p2[3] * v) * v) * v) / (q2[0] + (q2[1] + (q2[2] + q2[3] * v) * v) * v);
        } else if (v < 4) {
            lan = (p3[0] + (p3[1] + (p3[2] + p3[3] * v) * v) * v) / (q3[0] + (q3[1] + (q3[2] + q3[3] * v) * v) * v);
        } else if (v < 12) {
            u = 1. / v;
            lan = (p4[0] + (p4[1] + (p4[2] + p4[3] * u) * u) * u) / (q4[0] + (q4[1] + (q4[2] + q4[3] * u) * u) * u);
        } else if (v < 50) {
            u = 1. / v;
            lan = (p5[0] + (p5[1] + (p5[2] + p5[3] * u) * u) * u) / (q5[0] + (q5[1] + (q5[2] + q5[3] * u) * u) * u);
        } else if (v < 300) {
            u = 1. / v;
            lan = (p6[0] + (p6[1] + (p6[2] + p6[3] * u) * u) * u) / (q6[0] + (q6[1] + (q6[2] + q6[3] * u) * u) * u);
        } else {
            u = 1. / (v - v * log(v) / (v + 1));
            lan = 1 - (a2[1] + (a2[2] + a2[3] * u) * u) * u;
        }
        return lan;
    }

    /**
     * Computes the probability density funciton of Laplace distribution
     * at point x, with location parameter alpha and shape parameter beta.
     * By default, alpha=0, beta=1
     * This distribution is known under different names, most common is
     * double exponential distribution, but it also appears as
     * the two-tailed exponential or the bilateral exponential distribution
     */
    public static double laplaceDist(double x, double alpha, double beta) {
        double temp;
        temp = exp(-abs((x - alpha) / beta));
        temp /= (2. * beta);
        return temp;
    }

    /**
     * Computes the distribution funciton of Laplace distribution
     * at point x, with location parameter alpha and shape parameter beta.
     * By default, alpha=0, beta=1
     * This distribution is known under different names, most common is
     * double exponential distribution, but it also appears as
     * the two-tailed exponential or the bilateral exponential distribution
     */
    public static double laplaceDistI(double x, double alpha, double beta) {
        final double temp;
        if (x <= alpha) {
            temp = 0.5 * exp(-abs((x - alpha) / beta));
        } else {
            temp = 1 - 0.5 * exp(-abs((x - alpha) / beta));
        }
        return temp;
    }

    /**
     * Computation of ln[gamma(z)] for all z&gt;0. C.Lanczos, SIAM Journal of Numerical Analysis B1 (1964), 86 The
     * accuracy of the result is better than 2e-10. --- Nve 14-nov-1998 UU-SAP Utrecht
     * 
     * @param z input paramater
     * @return ln[gamma(z)]
     */
    public static double lnGamma(double z) {
        if (z <= 0) {
            return 0.0;
        }

        // Coefficients for the series expansion
        final double c[] = { 2.5066282746310005, 76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155,
            0.1208650973866179e-2, -0.5395239384953e-5 };

        final double x = z;
        double y = x;
        double tmp = x + 5.5;
        tmp = (x + 0.5) * log(tmp) - tmp;
        double ser = 1.000000000190015;
        for (int i = 1; i < 7; i++) {
            y += 1;
            ser += c[i] / y;
        }
        return tmp + log(c[0] * ser / x);
    }

    //// codegen: double -> float, int, long, short
    /**
     * @param a input vector
     * @param length &lt;= data.length elements to be used
     * @return index of array with the minimum element. If more than one element is minimum returns first found.
     */
    public static long locationMaximum(double[] a, int length) {
        if (a == null || a.length <= 0) {
            return -1;
        }
        double xmax = a[0];
        long location = 0;
        for (int i = 1; i < length; i++) {
            if (xmax < a[i]) {
                xmax = a[i];
                location = i;
            }
        }
        return location;
    }

    /**
     * @param a input vector
     * @param length &lt;= data.length elements to be used
     * @return index of array with the minimum element. If more than one element is minimum returns first found.
     */
    public static long locationMinimum(double[] a, int length) {
        if (a == null || a.length <= 0) {
            return -1;
        }
        double xmin = a[0];
        long location = 0;
        for (int i = 1; i < length; i++) {
            if (xmin > a[i]) {
                xmin = a[i];
                location = i;
            }
        }
        return location;
    }
    //// end codegen

    /**
     * Computes the density of LogNormal distribution at point x. Variable X has lognormal distribution if Y=Ln(X) has
     * normal distribution. The formula was taken from "Engineering Statistics Handbook" on site
     * http://www.itl.nist.gov/div898/handbook/eda/section3/eda3669.htm Implementation by Anna Kreshuk.
     * 
     * @param x input value
     * @param sigma the shape parameter
     * @param theta the location parameter
     * @param m the scale parameter
     * @return density of LogNormal distribution at point x
     */
    public static double logNormal(double x, double sigma, double theta, double m) {
        if ((x < theta) || (sigma <= 0) || (m <= 0)) {
            System.err.println("Lognormal(): illegal parameter values");
            return 0;
        }
        final double templog2 = log((x - theta) / m) * log((x - theta) / m);
        final double temp1 = exp(-templog2 / (2 * sigma * sigma));
        final double temp2 = (x - theta) * sigma * sqrt(2 * PI);

        return temp1 / temp2;
    }

    //// codegen: double -> float, int, long, short
    public static double maximum(double[] data) {
        return maximum(data, data.length);
    }

    /**
     * @param data the input vector
     * @param length &lt;= data.length elements to be used
     * @return value of largest vector element
     */
    public static double maximum(double[] data, int length) {
        double val = -Double.MAX_VALUE; //// codegen: subst:int:Int:Integer
        for (int i = 0; i < length; i++) {
            val = Math.max(val, data[i]);
        }
        return val;
    }

    public static double mean(double[] data) {
        return mean(data, data.length);
    }

    /**
     * @param data the input vector
     * @param length &lt;= data.length elements to be used
     * @return average of vector elements
     */
    public static double mean(double[] data, int length) {
        final double norm = 1.0 / (length); //// codegen: skip int, long, short //// subst:float:1.0:1.0f
        double val = 0.0; //// codegen: skip int, long, short //// subst:float:0.0:0.0f
        for (int i = 0; i < length; i++) {
            val += norm * data[i];
        }
        return val; //// codegen: returncast int, long, short
    }

    public static double median(double[] data) {
        return median(data, data.length);
    }

    /**
     * @param data the input vector
     * @param length &lt;= data.length elements to be used
     * @return median value of vector element
     */
    public static double median(double[] data, int length) {
        final double[] temp = sort(data, length, false);

        if (length % 2 == 0) {
            return (0.5 * (temp[length / 2] + temp[length / 2 + 1])); //// codegen: returncast all
        } else {
            return temp[length / 2];
        }
    }

    public static double minimum(double[] data) {
        return minimum(data, data.length);
    }

    /**
     * @param data the input vector
     * @param length &lt;= data.length elements to be used
     * @return value of smallest vector element
     */
    public static double minimum(double[] data, int length) {
        double val = +Double.MAX_VALUE; //// codegen: subst:int:Int:Integer
        for (int i = 0; i < length; i++) {
            val = min(val, data[i]);
        }
        return val;
    }
    //// end codegen

    //// codegen: double -> float
    /**
     * Calculate a normal vector of a plane.
     * 
     * @param p1 first 3D points belonged the plane to define it.
     * @param p2 second 3D points belonged the plane to define it.
     * @param p3 third 3D points belonged the plane to define it.
     * @param normal Pointer to 3D normal vector (normalised)
     * @return the computed result
     */
    public static double[] normal2Plane(double p1[], double p2[], double p3[], double normal[]) {
        final double[] v1 = new double[3];
        final double[] v2 = new double[3];

        v1[0] = p2[0] - p1[0];
        v1[1] = p2[1] - p1[1];
        v1[2] = p2[2] - p1[2];

        v2[0] = p3[0] - p1[0];
        v2[1] = p3[1] - p1[1];
        v2[2] = p3[2] - p1[2];

        normCross(v1, v2, normal);
        return normal;
    }
    //// end codegen

    /**
     * Normalise a vector v in place. Returns the norm of the original vector. This implementation (thanks Kevin Lynch
     * &lt;krlynch@bu.edu&gt;) is protected against possible overflows. Find the largest element, and divide that one
     * out.
     * 
     * @param v input parameter vector
     * @return the computed result
     */
    public static double normalize(double[] v) {
        final double av0 = abs(v[0]);
        final double av1 = abs(v[1]);
        final double av2 = abs(v[2]);

        final double amax;
        final double foo;
        final double bar;
        // 0 >= {1, 2}
        if (av0 >= av1 && av0 >= av2) {
            amax = av0;
            foo = av1;
            bar = av2;
        }

        // 1 >= {0, 2}
        else if (av1 >= av0 && av1 >= av2) {
            amax = av1;
            foo = av0;
            bar = av2;
        }

        // 2 >= {0, 1}
        else {
            amax = av2;
            foo = av0;
            bar = av1;
        }

        if (amax == 0.0) {
            return 0.;
        }

        final double foofrac = foo / amax;
        final double barfrac = bar / amax;
        final double d = amax * sqrt(1. + foofrac * foofrac + barfrac * barfrac);

        v[0] /= d;
        v[1] /= d;
        v[2] /= d;
        return d;
    }

    /**
     * Normalise a vector 'v' in place.
     * 
     * @param v input parameter vector
     * @return the computed result the norm of the original vector.
     */
    public static float normalize(float v[]) {
        final float d = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (d != 0) {
            v[0] /= d;
            v[1] /= d;
            v[2] /= d;
        }
        return d;
    }

    //// codegen: double -> float
    /**
     * Calculate the Normalized Cross Product of two vectors
     * 
     * @param v1 input vector1
     * @param v2 input vector2
     * @param out output vector
     * @return the computed result
     */
    public static double normCross(double v1[], double v2[], double out[]) {
        return normalize(cross(v1, v2, out));
    }
    //// end codegen

    /**
     * Computes quantiles for standard normal distribution N(0, 1) at probability p ALGORITHM AS241 APPL. STATIST.
     * (1988) VOL. 37, NO. 3, 477-484.
     * 
     * @param p input value
     * @return quantiles for standard normal distribution N(0, 1) at probability p
     */
    public static double normQuantile(double p) {
        if ((p <= 0) || (p >= 1)) {
            System.err.println("NormQuantile(): probability outside (0, 1)");
            return 0;
        }

        final double a0 = 3.3871328727963666080e0;
        final double a1 = 1.3314166789178437745e+2;
        final double a2 = 1.9715909503065514427e+3;
        final double a3 = 1.3731693765509461125e+4;
        final double a4 = 4.5921953931549871457e+4;
        final double a5 = 6.7265770927008700853e+4;
        final double a6 = 3.3430575583588128105e+4;
        final double a7 = 2.5090809287301226727e+3;
        final double b1 = 4.2313330701600911252e+1;
        final double b2 = 6.8718700749205790830e+2;
        final double b3 = 5.3941960214247511077e+3;
        final double b4 = 2.1213794301586595867e+4;
        final double b5 = 3.9307895800092710610e+4;
        final double b6 = 2.8729085735721942674e+4;
        final double b7 = 5.2264952788528545610e+3;
        final double c0 = 1.42343711074968357734e0;
        final double c1 = 4.63033784615654529590e0;
        final double c2 = 5.76949722146069140550e0;
        final double c3 = 3.64784832476320460504e0;
        final double c4 = 1.27045825245236838258e0;
        final double c5 = 2.41780725177450611770e-1;
        final double c6 = 2.27238449892691845833e-2;
        final double c7 = 7.74545014278341407640e-4;
        final double d1 = 2.05319162663775882187e0;
        final double d2 = 1.67638483018380384940e0;
        final double d3 = 6.89767334985100004550e-1;
        final double d4 = 1.48103976427480074590e-1;
        final double d5 = 1.51986665636164571966e-2;
        final double d6 = 5.47593808499534494600e-4;
        final double d7 = 1.05075007164441684324e-9;
        final double e0 = 6.65790464350110377720e0;
        final double e1 = 5.46378491116411436990e0;
        final double e2 = 1.78482653991729133580e0;
        final double e3 = 2.96560571828504891230e-1;
        final double e4 = 2.65321895265761230930e-2;
        final double e5 = 1.24266094738807843860e-3;
        final double e6 = 2.71155556874348757815e-5;
        final double e7 = 2.01033439929228813265e-7;
        final double f1 = 5.99832206555887937690e-1;
        final double f2 = 1.36929880922735805310e-1;
        final double f3 = 1.48753612908506148525e-2;
        final double f4 = 7.86869131145613259100e-4;
        final double f5 = 1.84631831751005468180e-5;
        final double f6 = 1.42151175831644588870e-7;
        final double f7 = 2.04426310338993978564e-15;

        final double split1 = 0.425;
        final double split2 = 5.;
        final double konst1 = 0.180625;
        final double konst2 = 1.6;

        final double q;
        double r;
        double quantile;
        q = p - 0.5;
        if (abs(q) < split1) {
            r = konst1 - q * q;
            quantile = q * (((((((a7 * r + a6) * r + a5) * r + a4) * r + a3) * r + a2) * r + a1) * r + a0)
                       / (((((((b7 * r + b6) * r + b5) * r + b4) * r + b3) * r + b2) * r + b1) * r + 1.);
        } else {
            if (q < 0) {
                r = p;
            } else {
                r = 1 - p;
            }
            // error case
            if (r <= 0) {
                quantile = 0;
            } else {
                r = sqrt(-log(r));
                if (r <= split2) {
                    r -= konst2;
                    quantile = (((((((c7 * r + c6) * r + c5) * r + c4) * r + c3) * r + c2) * r + c1) * r + c0)
                               / (((((((d7 * r + d6) * r + d5) * r + d4) * r + d3) * r + d2) * r + d1) * r + 1);
                } else {
                    r -= split2;
                    quantile = (((((((e7 * r + e6) * r + e5) * r + e4) * r + e3) * r + e2) * r + e1) * r + e0)
                               / (((((((f7 * r + f6) * r + f5) * r + f4) * r + f3) * r + f2) * r + f1) * r + 1);
                }
                if (q < 0) {
                    quantile = -quantile;
                }
            }
        }
        return quantile;
    }

    //// codegen: double -> float, int, long, short
    public static double peakToPeak(double[] data) {
        return peakToPeak(data, data.length);
    }

    /**
     * @param data the input vector
     * @param length &lt;= data.length elements to be used
     * @return peak-to-peak value of vector element
     */
    public static double peakToPeak(double[] data, int length) {
        return Math.abs(maximum(data, length) - minimum(data, length)); //// codegen: returncast short
    }
    //// end codegen

    /**
     * Simple recursive algorithm to find the permutations of n natural numbers, not necessarily all distinct adapted
     * from CERNLIB routine PERMU. The input array has to be initialised with a non descending sequence. The method
     * returns false when all combinations are exhausted.
     * 
     * @param n size of input vector
     * @param a input vector
     * @return false when all combinations are exhausted
     */
    public static boolean permute(int n, int[] a) {
        int i;
        int itmp;
        int i1 = -1;

        // find rightmost upward transition
        for (i = n - 2; i > -1; i--) {
            if (a[i] < a[i + 1]) {
                i1 = i;
                break;
            }
        }
        // no more upward transitions, end of the story
        if (i1 == -1) {
            return false;
        } else {
            // find lower right element higher than the lower
            // element of the upward transition
            for (i = n - 1; i > i1; i--) {
                if (a[i] > a[i1]) {
                    // swap the two
                    itmp = a[i1];
                    a[i1] = a[i];
                    a[i] = itmp;
                    break;
                }
            }
            // order the rest, in fact just invert, as there
            // are only downward transitions from here on
            for (i = 0; i < (n - i1 - 1) / 2; i++) {
                itmp = a[i1 + i + 1];
                a[i1 + i + 1] = a[n - i - 1];
                a[n - i - 1] = itmp;
            }
        }
        return true;
    }

    /**
     * compute the Poisson distribution function for (x,par) The Poisson PDF is implemented by means of Euler's
     * Gamma-function (for the factorial), so for all integer arguments it is correct. BUT for non-integer values it IS
     * NOT equal to the Poisson distribution.
     * 
     * see PoissonI to get a non-smooth function. Note that for large values of par, it is better to call
     * Gaus(x,par,sqrt(par),true) Begin_Html
     * 
     * @param x input value
     * @param par input parameter
     * @return the computed result
     */
    public static double poisson(double x, double par) {
        if (x < 0) {
            return 0;
        } else if (x == 0.0) {
            return 1. / exp(par);
        } else {
            final double lnpoisson = x * log(par) - par - lnGamma(x + 1.);
            return exp(lnpoisson);
        }
        // An alternative strategy is to transition to a Gaussian approximation
        // for
        // large values of par ...
        // else {
        // return Gaus(x,par,Sqrt(par),true);
        // }
    }

    /**
     * compute the Poisson distribution function for (x,par) This is a non-smooth function
     * 
     * @param x input value
     * @param par input parameter
     * @return the computed result
     */
    public static double poissonI(double x, double par) {
        if (x < 0) {
            return 0;
        }
        if (x < 1) {
            return exp(-par);
        }
        final double gam;
        final int ix = (int) (x);
        final double kMaxInt = 2e6;
        if (x < kMaxInt) {
            gam = pow(par, ix) / gamma(ix + 1);
        } else {
            gam = pow(par, x) / gamma(x + 1);
        }
        return gam / exp(par);
    }

    /**
     * Computation of the probability for a certain Chi-squared (chi2) and number of degrees of freedom (ndf).
     * Calculations are based on the incomplete gamma function P(a,x), where a=ndf/2 and x=chi2/2. P(a,x) represents the
     * probability that the observed Chi-squared for a correct model should be less than the value chi2. The returned
     * probability corresponds to 1-P(a,x), which denotes the probability that an observed Chi-squared exceeds the value
     * chi2 by chance, even for a correct model. --- NvE 14-nov-1998 UU-SAP Utrecht
     * 
     * @param chi2 input
     * @param ndf number of degrees of freedom
     * @return the computed result
     */
    public static double prob(double chi2, int ndf) {
        if (ndf <= 0) {
            return 0; // Set CL to zero in case ndf<=0
        }

        if (chi2 <= 0) {
            if (chi2 < 0) {
                return 0;
            } else {
                return 1;
            }
        }

        if (ndf == 1) {
            return 1.0 - erf(sqrt(chi2) / sqrt(2.));
        }

        // Gaussian approximation for large ndf
        final double q = sqrt(2 * chi2) - sqrt(2 * ndf - 1);
        if (!(ndf > 30 && q > 5)) {
            // Evaluate the incomplete gamma function
            return (1 - gamma(0.5 * ndf, 0.5 * chi2));
        }
        return 0.5 * (1 - erf(q / sqrt(2.)));
    }

    //// codegen: double -> float, int, long, short
    public static double rms(double[] data) {
        return rms(data, data.length);
    }

    /**
     * @param data the input vector
     * @param length &lt;= data.length elements to be used
     * @return un-biased r.m.s. of vector elements
     */
    public static double rms(double[] data, int length) {
        if (length <= 0) {
            return -1;
        }

        final double norm = 1.0 / (length); //// codegen: skip all
        double val1 = 0.0; //// codegen: skip all
        double val2 = 0.0; //// codegen: skip all
        for (int i = 0; i < length; i++) {
            val1 += data[i];
            val2 += data[i] * data[i];
        }

        val1 *= norm;
        val2 *= norm;
        // un-biased rms!
        return sqrt(Math.abs(val2 - val1 * val1)); //// codegen: returncast all
    }
    //// end codegen

    /**
     * Calculate the sinc = sin(x)/x function if norm ==true then sinc = sinc(pi*x)/(pi*x) is used
     * 
     * @param x input parameter
     * @param norm normalisation factor
     * @return the computed result
     */
    public static double sinc(double x, boolean norm) {
        if (x == 0) {
            return 1.0;
        }
        final double val = norm ? MathBase.PI : 1.0;
        return MathBase.sin(val * x) / (val * x);
    }

    //// codegen: double -> float, int, long, short
    /**
     * Sorts the input a array
     * 
     * @param a the input array
     * @param length &lt;= data.length elements to be used
     * @param down true: ascending , false: descending order
     * @return the sorted array
     */
    public static double[] sort(double[] a, int length, boolean down) {
        if (a == null || a.length <= 0) {
            return new double[0];
        }
        final double[] index = java.util.Arrays.copyOf(a, length);
        java.util.Arrays.sort(index);

        if (down) {
            double temp;
            final int nlast = length - 1;
            for (int i = 0; i < (length / 2); i++) {
                // swap values
                temp = index[i];
                index[i] = index[nlast - i];
                index[nlast - i] = temp;
            }
        }
        return index;
    }
    //// end codegen

    /**
     * Struve Functions of Order 0
     *
     * Converted from CERNLIB M342 by Rene Brun.
     */
    public static double struveH0(double x) {
        final int n1 = 15;
        final int n2 = 25;
        final double c1[] = { 1.00215845609911981, -1.63969292681309147, 1.50236939618292819, -.72485115302121872,
            .18955327371093136, -.03067052022988, .00337561447375194, -2.6965014312602e-4, 1.637461692612e-5,
            -7.8244408508e-7, 3.021593188e-8, -9.6326645e-10, 2.579337e-11, -5.8854e-13, 1.158e-14, -2e-16 };
        final double c2[] = { .99283727576423943, -.00696891281138625, 1.8205103787037e-4, -1.063258252844e-5,
            9.8198294287e-7, -1.2250645445e-7, 1.894083312e-8, -3.44358226e-9, 7.1119102e-10, -1.6288744e-10,
            4.065681e-11, -1.091505e-11, 3.12005e-12, -9.4202e-13, 2.9848e-13, -9.872e-14, 3.394e-14, -1.208e-14,
            4.44e-15, -1.68e-15, 6.5e-16, -2.6e-16, 1.1e-16, -4e-17, 2e-17, -1e-17 };

        final double c0 = 2 / PI;

        int i;
        final double alfa;
        double h;
        final double r;
        final double y;
        double b0;
        double b1;
        double b2;
        double v = abs(x);

        v = abs(x);
        if (v < 8) {
            y = v / 8;
            h = 2 * y * y - 1;
            alfa = h + h;
            b0 = 0;
            b1 = 0;
            b2 = 0;
            for (i = n1; i >= 0; --i) {
                b0 = c1[i] + alfa * b1 - b2;
                b2 = b1;
                b1 = b0;
            }
            h = y * (b0 - h * b2);
        } else {
            r = 1 / v;
            h = 128 * r * r - 1;
            alfa = h + h;
            b0 = 0;
            b1 = 0;
            b2 = 0;
            for (i = n2; i >= 0; --i) {
                b0 = c2[i] + alfa * b1 - b2;
                b2 = b1;
                b1 = b0;
            }
            h = besselY0(v) + r * c0 * (b0 - h * b2);
        }
        if (x < 0) {
            h = -h;
        }
        return h;
    }

    /**
     * Struve Functions of Order 1
     *
     * Converted from CERNLIB M342 by Rene Brun.
     */
    public static double struveH1(double x) {
        final int n3 = 16;
        final int n4 = 22;
        final double[] c3 = { .5578891446481605, -.11188325726569816, -.16337958125200939, .32256932072405902,
            -.14581632367244242, .03292677399374035, -.00460372142093573, 4.434706163314e-4, -3.142099529341e-5,
            1.7123719938e-6, -7.416987005e-8, 2.61837671e-9, -7.685839e-11, 1.9067e-12, -4.052e-14, 7.5e-16,
            -1e-17 };
        final double[] c4 = { 1.00757647293865641, .00750316051248257, -7.043933264519e-5, 2.66205393382e-6, -1.8841157753e-7,
            1.949014958e-8, -2.6126199e-9, 4.236269e-10, -7.955156e-11, 1.679973e-11, -3.9072e-12, 9.8543e-13,
            -2.6636e-13, 7.645e-14, -2.313e-14, 7.33e-15, -2.42e-15, 8.3e-16, -3e-16, 1.1e-16, -4e-17, 2e-17,
            -1e-17 };

        final double c0 = 2 / PI;
        final double cc = 2 / (3 * PI);

        int i;
        final int i1;
        final double alfa;
        double h;
        double r;
        final double y;
        double b0;
        double b1;
        double b2;
        final double v = abs(x);

        if (v == 0) {
            h = 0;
        } else if (v <= 0.3) {
            y = v * v;
            r = 1;
            h = 1;
            i1 = (int) (-8. / log10(v));
            for (i = 1; i <= i1; ++i) {
                h = -h * y / ((2 * i + 1) * (2 * i + 3));
                r += h;
            }
            h = cc * y * r;
        } else if (v < 8) {
            h = v * v / 32 - 1;
            alfa = h + h;
            b0 = 0;
            b1 = 0;
            b2 = 0;
            for (i = n3; i >= 0; --i) {
                b0 = c3[i] + alfa * b1 - b2;
                b2 = b1;
                b1 = b0;
            }
            h = b0 - h * b2;
        } else {
            h = 128 / (v * v) - 1;
            alfa = h + h;
            b0 = 0;
            b1 = 0;
            b2 = 0;
            for (i = n4; i >= 0; --i) {
                b0 = c4[i] + alfa * b1 - b2;
                b2 = b1;
                b1 = b0;
            }
            h = besselY1(v) + c0 * (b0 - h * b2);
        }
        return h;
    }

    /**
     * Modified Struve Function of Order 0.
     * By Kirill Filimonov.
     */
    public static double struveL0(double x) {
        final double pi = PI;

        double s = 1.0;
        double r = 1.0;

        final double a0;
        final double sl0;
        final double a1;
        double bi0;

        int km;

        if (x <= 20.) {
            a0 = 2.0 * x / pi;
            for (int i = 1; i <= 60; i++) {
                r *= (x / (2 * i + 1)) * (x / (2 * i + 1));
                s += r;
                if (abs(r / s) < 1.e-12) {
                    break;
                }
            }
            sl0 = a0 * s;
        } else {
            km = (int) (5 * (x + 1.0));
            if (x >= 50.0) {
                km = 25;
            }
            for (int i = 1; i <= km; i++) {
                r *= (2 * i - 1) * (2 * i - 1) / x / x;
                s += r;
                if (abs(r / s) < 1.0e-12) {
                    break;
                }
            }
            a1 = exp(x) / sqrt(2 * pi * x);
            r = 1.0;
            bi0 = 1.0;
            for (int i = 1; i <= 16; i++) {
                r = 0.125 * r * (2.0 * i - 1.0) * (2.0 * i - 1.0) / (i * x);
                bi0 += r;
                if (abs(r / bi0) < 1.0e-12) {
                    break;
                }
            }

            bi0 *= a1;
            sl0 = -2.0 / (pi * x) * s + bi0;
        }
        return sl0;
    }

    /**
     * Modified Struve Function of Order 1.
     * By Kirill Filimonov.
     */
    public static double struveL1(double x) {
        final double pi = PI;
        final double a1;
        double sl1;
        double bi1;
        double s;
        double r = 1.0;
        int km;
        int i;

        if (x <= 20.) {
            s = 0.0;
            for (i = 1; i <= 60; i++) {
                r *= x * x / (4.0 * i * i - 1.0);
                s += r;
                if (abs(r) < abs(s) * 1.e-12) {
                    break;
                }
            }
            sl1 = 2.0 / pi * s;
        } else {
            s = 1.0;
            km = (int) (0.5 * x);
            if (x > 50.0) {
                km = 25;
            }
            for (i = 1; i <= km; i++) {
                r *= (2 * i + 3) * (2 * i + 1) / x / x;
                s += r;
                if (abs(r / s) < 1.0e-12) {
                    break;
                }
            }
            sl1 = 2.0 / pi * (-1.0 + 1.0 / (x * x) + 3.0 * s / (x * x * x * x));
            a1 = exp(x) / sqrt(2 * pi * x);
            r = 1.0;
            bi1 = 1.0;
            for (i = 1; i <= 16; i++) {
                r = -0.125 * r * (4.0 - (2.0 * i - 1.0) * (2.0 * i - 1.0)) / (i * x);
                bi1 += r;
                if (abs(r / bi1) < 1.0e-12) {
                    break;
                }
            }
            sl1 += a1 * bi1;
        }
        return sl1;
    }

    /**
     * Computes density function for Student's t- distribution (the probability function (integral of density) is
     * computed in StudentI). First parameter stands for x - the actual variable of the density function p(x) and the
     * point at which the density is calculated. Second parameter stands for number of degrees of freedom. About Student
     * distribution: Student's t-distribution is used for many significance tests, for example, for the Student's
     * t-tests for the statistical significance of difference between two sample means and for confidence intervals for
     * the difference between two population means. Example: suppose we have a random sample of size n drawn from normal
     * distribution with mean Mu and st.deviation Sigma. Then the variable t = (sample_mean - Mu)/(sample_deviation /
     * sqrt(n)) has Student's t-distribution with n-1 degrees of freedom. NOTE that this function's second argument is
     * number of degrees of freedom, not the sample size. As the number of degrees of freedom grows, t-distribution
     * approaches Normal(0,1) distribution. Implementation by Anna Kreshuk.
     * 
     * @param T input value
     * @param ndf number of degrees of freedom
     * @return value of the density function for Student's t- distribution
     */
    public static double student(double T, double ndf) {
        if (ndf < 1) {
            return 0;
        }

        final double r = ndf;
        final double rh = 0.5 * r;
        final double rh1 = rh + 0.5;
        final double denom = sqrt(r * PI) * gamma(rh) * pow(1 + T * T / r, rh1);
        return gamma(rh1) / denom;
    }

    /**
     * Calculates the cumulative distribution function of Student's t-distribution second parameter stands for number of
     * degrees of freedom, not for the number of samples if x has Student's t-distribution, the function returns the
     * probability of x being less than T. Implementation by Anna Kreshuk.
     * 
     * @param T input value
     * @param ndf number of degrees of freedom
     * @return cumulative distribution function of Student's t-distribution
     */
    public static double studentI(double T, double ndf) {
        final double r = ndf;
        final double si = (T > 0) ? (1 - 0.5 * betaIncomplete((r / (r + T * T)), r * 0.5, 0.5))
                                  : 0.5 * betaIncomplete((r / (r + T * T)), r * 0.5, 0.5);
        return si;
    }

    /**
     * Computes quantiles of the Student's t-distribution
     *
     * the algorithm was taken from
     * G.W.Hill, "Algorithm 396, Student's t-quantiles"
     * "Communications of the ACM", 13(10), October 1970
     * @param p the probability, at which the quantile is computed
     * @param ndf the number of degrees of freedom of the
     * @param lower_tail determine whether to reurn the quantile for the upper or the lower tail
     *                   true: the algorithm returns such x0, that P(x &lt; x0)=p
     *                   false: the algorithm returns such x0, that  P(x &gt; x0)=p
     */
    public static double studentQuantile(double p, double ndf, boolean lower_tail) {
        if (ndf < 1 || p >= 1 || p <= 0) {
            System.err.println("StudentQuantile() - illegal parameter values");
            return 0;
        }

        double quantile;
        final double temp;
        final boolean neg;
        final double q;
        if ((lower_tail && p > 0.5) || (!lower_tail && p < 0.5)) {
            neg = false;
            q = 2 * (lower_tail ? (1 - p) : p);
        } else {
            neg = true;
            q = 2 * (lower_tail ? p : (1 - p));
        }

        if ((ndf - 1) < 1e-8) {
            temp = PI_OVER_2 * q;
            quantile = cos(temp) / sin(temp);
        } else {
            if ((ndf - 2) < 1e-8) {
                quantile = sqrt(2. / (q * (2 - q)) - 2);
            } else {
                final double a = 1. / (ndf - 0.5);
                final double b = 48. / (a * a);
                double c = ((20700 * a / b - 98) * a - 16) * a + 96.36;
                final double d = ((94.5 / (b + c) - 3.) / b + 1) * sqrt(a * PI_OVER_2) * ndf;
                double x = q * d;
                double y = pow(x, (2. / ndf));
                if (y > 0.05 + a) {
                    // asymptotic inverse expansion about normal
                    x = normQuantile(q * 0.5);
                    y = x * x;
                    if (ndf < 5) {
                        c += 0.3 * (ndf - 4.5) * (x + 0.6);
                    }
                    c += (((0.05 * d * x - 5.) * x - 7.) * x - 2.) * x + b;
                    y = (((((0.4 * y + 6.3) * y + 36.) * y + 94.5) / c - y - 3.) / b + 1) * x;
                    y = a * y * y;
                    if (y > 0.002) {
                        y = exp(y) - 1;
                    } else {
                        y += 0.5 * y * y;
                    }
                } else {
                    y = ((1. / (((ndf + 6.) / (ndf * y) - 0.089 * d - 0.822) * (ndf + 2.) * 3) + 0.5 / (ndf + 4.)) * y - 1.)
                                * (ndf + 1.) / (ndf + 2.)
                        + 1 / y;
                }
                quantile = sqrt(ndf * y);
            }
        }
        if (neg) {
            quantile = -quantile;
        }
        return quantile;
    }

    public static double variance(double[] aa, double[] ww) {
        final int n = aa.length;
        if (n != ww.length) {
            throw new IllegalArgumentException(
                    "length of variable array, " + n + " and length of weight array, " + ww.length + " are different");
        }
        final double nn = Math.effectiveSampleNumber(ww);
        final double nterm = nn / (nn - 1.0);
        // nterm = 1.0; // n

        double sumx = 0.0D;
        double sumw = 0.0D;
        double mean = 0.0D;
        final double[] weight = invertAndSquare(ww);
        for (int i = 0; i < n; i++) {
            sumx += aa[i] * weight[i];
            sumw += weight[i];
        }
        mean = sumx / sumw;
        sumx = 0.0D;
        for (int i = 0; i < n; i++) {
            sumx += weight[i] * MathBase.sqr(aa[i] - mean);
        }
        return sumx * nterm / sumw;
    }

    /**
     * Returns the value of the Vavilov density function
     *
     * The algorithm was taken from the CernLib function vavden(G115)
     * Reference: A.Rotondi and P.Montagna, Fast Calculation of Vavilov distribution Nucl.Instr. and Meth. B47(1990), 215-224
     * Accuracy: quote from the reference above:
     * "The resuls of our code have been compared with the values of the Vavilov density function computed numerically
     * in an accurate way: our approximation shows a difference of less than 3% around the peak of the density
     * function, slowly increasing going towards the extreme tails to the right and to the left"
     *
     * @param x the point were the density function is evaluated
     * @param kappa value of kappa (distribution parameter)
     * @param beta2 value of beta2 (distribution parameter)
     * @return
     */
    public static double vavilov(double x, double kappa, double beta2) {
        final double[] ac = new double[14];
        final double[] hc = new double[9];

        final int[] itype = new int[1];
        final int[] npt = new int[1];
        vavilovSet(kappa, beta2, false, null, ac, hc, itype, npt);
        final double v = vavilovDenEval(x, ac, hc, itype[0]);
        return v;
    }

    /**
     * Internal function, called by Vavilov and VavilovSet
     * 
     * @param rlam ???
     * @param AC ???
     * @param HC ???
     * @param itype ???
     * @return internal value
     */
    protected static double vavilovDenEval(double rlam, double[] AC, double[] HC, int itype) {
        if (rlam < AC[0] || rlam > AC[8]) {
            return 0;
        }
        double v = 0;
        int k;
        final double x;
        double fn;
        double s;
        final double[] h = new double[10];
        if (itype == 1) {
            fn = 1;
            x = (rlam + HC[0]) * HC[1];
            h[1] = x;
            h[2] = x * x - 1;
            for (k = 2; k <= 8; k++) {
                fn++;
                h[k + 1] = x * h[k] - fn * h[k - 1];
            }
            s = 1 + HC[7] * h[9];
            for (k = 2; k <= 6; k++) {
                s += HC[k] * h[k + 1];
            }
            v = HC[8] * exp(-0.5 * x * x) * max(s, 0.);
        } else if (itype == 2) {
            x = rlam * rlam;
            v = AC[1] * exp(-AC[2] * (rlam + AC[5] * x) - AC[3] * exp(-AC[4] * (rlam + AC[6] * x)));
        } else if (itype == 3) {
            if (rlam < AC[7]) {
                x = rlam * rlam;
                v = AC[1] * exp(-AC[2] * (rlam + AC[5] * x) - AC[3] * exp(-AC[4] * (rlam + AC[6] * x)));
            } else {
                x = 1. / rlam;
                v = (AC[11] * x + AC[12]) * x;
            }
        } else if (itype == 4) {
            // TODO: check this line
            // v = AC[13]*Landau(rlam);
        }
        return v;
    }

    /**
     * Returns the value of the Vavilov distribution function
     *
     * The algorithm was taken from the CernLib function vavden(G115)
     * Reference: A.Rotondi and P.Montagna, Fast Calculation of Vavilov distribution Nucl.Instr. and Meth. B47(1990), 215-224
     * Accuracy: quote from the reference above:
     * "The resuls of our code have been compared with the values of the Vavilov density function computed numerically
     * in an accurate way: our approximation shows a difference of less than 3% around the peak of the density
     * function, slowly increasing going towards the extreme tails to the right and to the left"
     *
     * @param x the point were the density function is evaluated
     * @param kappa value of kappa (distribution parameter)
     * @param beta2 value of beta2 (distribution parameter)
     * @return
     */
    public static double vavilovI(double x, double kappa, double beta2) {
        final double[] ac = new double[14];
        final double[] hc = new double[9];
        final double[] wcm = new double[200];
        final int[] itype = new int[0];
        final int[] npt = new int[0];
        final int k;
        final double xx;
        final double v;
        vavilovSet(kappa, beta2, true, wcm, ac, hc, itype, npt);
        if (x < ac[0]) {
            v = 0;
        } else if (x >= ac[8]) {
            v = 1;
        } else {
            xx = x - ac[0];
            k = (int) (xx * ac[10]);
            v = min(wcm[k] + (xx - k * ac[9]) * (wcm[k + 1] - wcm[k]) * ac[10], 1.);
        }

        return v;
    }

    /**
     * Internal function, called by Vavilov and VavilovI
     * 
     * @param rkappa ???
     * @param beta2 ???
     * @param mode ???
     * @param WCM ???
     * @param AC ???
     * @param HC ???
     * @param itype ???
     * @param npt ???
     */
    public static void vavilovSet(double rkappa, double beta2, boolean mode, double[] WCM, double[] AC, double[] HC, int[] itype, int[] npt) {
        if (rkappa < 0.01 || rkappa > 12) {
            System.err.println("Vavilov distribution - illegal value of kappa");
            return;
        }

        final double BKMNX1 = 0.02;
        final double BKMNY1 = 0.05;
        final double BKMNX2 = 0.12;
        final double BKMNY2 = 0.05;
        final double BKMNX3 = 0.22;
        final double BKMNY3 = 0.05;
        final double BKMXX1 = 0.1;
        final double BKMXY1 = 1;
        final double BKMXX2 = 0.2;
        final double BKMXY2 = 1;
        final double BKMXX3 = 0.3;
        final double BKMXY3 = 1;

        final double FBKX1 = 2 / (BKMXX1 - BKMNX1);
        final double FBKX2 = 2 / (BKMXX2 - BKMNX2);
        final double FBKX3 = 2 / (BKMXX3 - BKMNX3);
        final double FBKY1 = 2 / (BKMXY1 - BKMNY1);
        final double FBKY2 = 2 / (BKMXY2 - BKMNY2);
        final double FBKY3 = 2 / (BKMXY3 - BKMNY3);

        final double[] FNINV = { 0, 1, 0.5, 0.33333333, 0.25, 0.2 };

        final double[] EDGEC = { 0, 0, 0.16666667e+0, 0.41666667e-1, 0.83333333e-2, 0.13888889e-1, 0.69444444e-2,
            0.77160493e-3 };

        final double[] U1 = { 0, 0.25850868e+0, 0.32477982e-1, -0.59020496e-2, 0., 0.24880692e-1, 0.47404356e-2,
            -0.74445130e-3, 0.73225731e-2, 0., 0.11668284e-2, 0., -0.15727318e-2, -0.11210142e-2 };

        final double[] U2 = { 0, 0.43142611e+0, 0.40797543e-1, -0.91490215e-2, 0., 0.42127077e-1, 0.73167928e-2,
            -0.14026047e-2, 0.16195241e-1, 0.24714789e-2, 0.20751278e-2, 0., -0.25141668e-2, -0.14064022e-2 };

        final double[] U3 = { 0, 0.25225955e+0, 0.64820468e-1, -0.23615759e-1, 0., 0.23834176e-1, 0.21624675e-2,
            -0.26865597e-2, -0.54891384e-2, 0.39800522e-2, 0.48447456e-2, -0.89439554e-2, -0.62756944e-2,
            -0.24655436e-2 };

        final double[] U4 = { 0, 0.12593231e+1, -0.20374501e+0, 0.95055662e-1, -0.20771531e-1, -0.46865180e-1, -0.77222986e-2,
            0.32241039e-2, 0.89882920e-2, -0.67167236e-2, -0.13049241e-1, 0.18786468e-1, 0.14484097e-1 };

        final double[] U5 = { 0, -0.24864376e-1, -0.10368495e-2, 0.14330117e-2, 0.20052730e-3, 0.18751903e-2, 0.12668869e-2,
            0.48736023e-3, 0.34850854e-2, 0., -0.36597173e-3, 0.19372124e-2, 0.70761825e-3, 0.46898375e-3 };

        final double[] U6 = { 0, 0.35855696e-1, -0.27542114e-1, 0.12631023e-1, -0.30188807e-2, -0.84479939e-3, 0.,
            0.45675843e-3, -0.69836141e-2, 0.39876546e-2, -0.36055679e-2, 0., 0.15298434e-2, 0.19247256e-2 };

        final double[] U7 = { 0, 0.10234691e+2, -0.35619655e+1, 0.69387764e+0, -0.14047599e+0, -0.19952390e+1, -0.45679694e+0,
            0., 0.50505298e+0 };
        final double[] U8 = { 0, 0.21487518e+2, -0.11825253e+2, 0.43133087e+1, -0.14500543e+1, -0.34343169e+1, -0.11063164e+1,
            -0.21000819e+0, 0.17891643e+1, -0.89601916e+0, 0.39120793e+0, 0.73410606e+0, 0., -0.32454506e+0 };

        final double[] V1 = { 0, 0.27827257e+0, -0.14227603e-2, 0.24848327e-2, 0., 0.45091424e-1, 0.80559636e-2,
            -0.38974523e-2, 0., -0.30634124e-2, 0.75633702e-3, 0.54730726e-2, 0.19792507e-2 };

        final double[] V2 = { 0, 0.41421789e+0, -0.30061649e-1, 0.52249697e-2, 0., 0.12693873e+0, 0.22999801e-1,
            -0.86792801e-2, 0.31875584e-1, -0.61757928e-2, 0., 0.19716857e-1, 0.32596742e-2 };

        final double[] V3 = { 0, 0.20191056e+0, -0.46831422e-1, 0.96777473e-2, -0.17995317e-2, 0.53921588e-1, 0.35068740e-2,
            -0.12621494e-1, -0.54996531e-2, -0.90029985e-2, 0.34958743e-2, 0.18513506e-1, 0.68332334e-2,
            -0.12940502e-2 };

        final double[] V4 = { 0, 0.13206081e+1, 0.10036618e+0, -0.22015201e-1, 0.61667091e-2, -0.14986093e+0, -0.12720568e-1,
            0.24972042e-1, -0.97751962e-2, 0.26087455e-1, -0.11399062e-1, -0.48282515e-1, -0.98552378e-2 };

        final double[] V5 = { 0, 0.16435243e-1, 0.36051400e-1, 0.23036520e-2, -0.61666343e-3, -0.10775802e-1, 0.51476061e-2,
            0.56856517e-2, -0.13438433e-1, 0., 0., -0.25421507e-2, 0.20169108e-2, -0.15144931e-2 };

        final double[] V6 = { 0, 0.33432405e-1, 0.60583916e-2, -0.23381379e-2, 0.83846081e-3, -0.13346861e-1, -0.17402116e-2,
            0.21052496e-2, 0.15528195e-2, 0.21900670e-2, -0.13202847e-2, -0.45124157e-2, -0.15629454e-2,
            0.22499176e-3 };

        final double[] V7 = { 0, 0.54529572e+1, -0.90906096e+0, 0.86122438e-1, 0., -0.12218009e+1, -0.32324120e+0,
            -0.27373591e-1, 0.12173464e+0, 0., 0., 0.40917471e-1 };

        final double[] V8 = { 0, 0.93841352e+1, -0.16276904e+1, 0.16571423e+0, 0., -0.18160479e+1, -0.50919193e+0,
            -0.51384654e-1, 0.21413992e+0, 0., 0., 0.66596366e-1 };

        final double[] W1 = { 0, 0.29712951e+0, 0.97572934e-2, 0., -0.15291686e-2, 0.35707399e-1, 0.96221631e-2,
            -0.18402821e-2, -0.49821585e-2, 0.18831112e-2, 0.43541673e-2, 0.20301312e-2, -0.18723311e-2,
            -0.73403108e-3 };

        final double[] W2 = { 0, 0.40882635e+0, 0.14474912e-1, 0.25023704e-2, -0.37707379e-2, 0.18719727e+0, 0.56954987e-1,
            0., 0.23020158e-1, 0.50574313e-2, 0.94550140e-2, 0.19300232e-1 };

        final double[] W3 = { 0, 0.16861629e+0, 0., 0.36317285e-2, -0.43657818e-2, 0.30144338e-1, 0.13891826e-1,
            -0.58030495e-2, -0.38717547e-2, 0.85359607e-2, 0.14507659e-1, 0.82387775e-2, -0.10116105e-1,
            -0.55135670e-2 };

        final double[] W4 = { 0, 0.13493891e+1, -0.26863185e-2, -0.35216040e-2, 0.24434909e-1, -0.83447911e-1, -0.48061360e-1,
            0.76473951e-2, 0.24494430e-1, -0.16209200e-1, -0.37768479e-1, -0.47890063e-1, 0.17778596e-1,
            0.13179324e-1 };

        final double[] W5 = { 0, 0.10264945e+0, 0.32738857e-1, 0., 0.43608779e-2, -0.43097757e-1, -0.22647176e-2,
            0.94531290e-2, -0.12442571e-1, -0.32283517e-2, -0.75640352e-2, -0.88293329e-2, 0.52537299e-2,
            0.13340546e-2 };

        final double[] W6 = { 0, 0.29568177e-1, -0.16300060e-2, -0.21119745e-3, 0.23599053e-2, -0.48515387e-2, -0.40797531e-2,
            0.40403265e-3, 0.18200105e-2, -0.14346306e-2, -0.39165276e-2, -0.37432073e-2, 0.19950380e-2,
            0.12222675e-2 };

        final double[] W8 = { 0, 0.66184645e+1, -0.73866379e+0, 0.44693973e-1, 0., -0.14540925e+1, -0.39529833e+0,
            -0.44293243e-1, 0.88741049e-1 };

        itype[0] = 0;

        final double[] DRK = new double[6];
        final double[] DSIGM = new double[6];
        final double[] ALFA = new double[8];
        int j;
        double x;
        double y;
        final double xx;
        final double yy;
        final double x2;
        final double x3;
        final double y2;
        final double y3;
        final double xy;
        double p2;
        final double p3;
        final double q2;
        final double q3;
        final double pq;
        if (rkappa >= 0.29) {
            itype[0] = 1;
            npt[0] = 100;
            final double wk = 1. / sqrt(rkappa);

            AC[0] = (-0.032227 * beta2 - 0.074275) * rkappa + (0.24533 * beta2 + 0.070152) * wk
                    + (-0.55610 * beta2 - 3.1579);
            AC[8] = (-0.013483 * beta2 - 0.048801) * rkappa + (-1.6921 * beta2 + 8.3656) * wk
                    + (-0.73275 * beta2 - 3.5226);
            DRK[1] = wk * wk;
            DSIGM[1] = sqrt(rkappa / (1 - 0.5 * beta2));
            for (j = 1; j <= 4; j++) {
                DRK[j + 1] = DRK[1] * DRK[j];
                DSIGM[j + 1] = DSIGM[1] * DSIGM[j];
                ALFA[j + 1] = (FNINV[j] - beta2 * FNINV[j + 1]) * DRK[j];
            }
            HC[0] = log(rkappa) + beta2 + 0.42278434;
            HC[1] = DSIGM[1];
            HC[2] = ALFA[3] * DSIGM[3];
            HC[3] = (3 * ALFA[2] * ALFA[2] + ALFA[4]) * DSIGM[4] - 3;
            HC[4] = (10 * ALFA[2] * ALFA[3] + ALFA[5]) * DSIGM[5] - 10 * HC[2];
            HC[5] = HC[2] * HC[2];
            HC[6] = HC[2] * HC[3];
            HC[7] = HC[2] * HC[5];
            for (j = 2; j <= 7; j++) {
                HC[j] *= EDGEC[j];
            }
            HC[8] = 0.39894228 * HC[1];
        } else if (rkappa >= 0.22) {
            itype[0] = 2;
            npt[0] = 150;
            x = 1 + (rkappa - BKMXX3) * FBKX3;
            y = 1 + (sqrt(beta2) - BKMXY3) * FBKY3;
            xx = 2 * x;
            yy = 2 * y;
            x2 = xx * x - 1;
            x3 = xx * x2 - x;
            y2 = yy * y - 1;
            y3 = yy * y2 - y;
            xy = x * y;
            p2 = x2 * y;
            p3 = x3 * y;
            q2 = y2 * x;
            q3 = y3 * x;
            pq = x2 * y2;
            AC[1] = W1[1] + W1[2] * x + W1[4] * x3 + W1[5] * y + W1[6] * y2 + W1[7] * y3 + W1[8] * xy + W1[9] * p2
                    + W1[10] * p3 + W1[11] * q2 + W1[12] * q3 + W1[13] * pq;
            AC[2] = W2[1] + W2[2] * x + W2[3] * x2 + W2[4] * x3 + W2[5] * y + W2[6] * y2 + W2[8] * xy + W2[9] * p2
                    + W2[10] * p3 + W2[11] * q2;
            AC[3] = W3[1] + W3[3] * x2 + W3[4] * x3 + W3[5] * y + W3[6] * y2 + W3[7] * y3 + W3[8] * xy + W3[9] * p2
                    + W3[10] * p3 + W3[11] * q2 + W3[12] * q3 + W3[13] * pq;
            AC[4] = W4[1] + W4[2] * x + W4[3] * x2 + W4[4] * x3 + W4[5] * y + W4[6] * y2 + W4[7] * y3 + W4[8] * xy
                    + W4[9] * p2 + W4[10] * p3 + W4[11] * q2 + W4[12] * q3 + W4[13] * pq;
            AC[5] = W5[1] + W5[2] * x + W5[4] * x3 + W5[5] * y + W5[6] * y2 + W5[7] * y3 + W5[8] * xy + W5[9] * p2
                    + W5[10] * p3 + W5[11] * q2 + W5[12] * q3 + W5[13] * pq;
            AC[6] = W6[1] + W6[2] * x + W6[3] * x2 + W6[4] * x3 + W6[5] * y + W6[6] * y2 + W6[7] * y3 + W6[8] * xy
                    + W6[9] * p2 + W6[10] * p3 + W6[11] * q2 + W6[12] * q3 + W6[13] * pq;
            AC[8] = W8[1] + W8[2] * x + W8[3] * x2 + W8[5] * y + W8[6] * y2 + W8[7] * y3 + W8[8] * xy;
            AC[0] = -3.05;
        } else if (rkappa >= 0.12) {
            itype[0] = 3;
            npt[0] = 200;
            x = 1 + (rkappa - BKMXX2) * FBKX2;
            y = 1 + (sqrt(beta2) - BKMXY2) * FBKY2;
            xx = 2 * x;
            yy = 2 * y;
            x2 = xx * x - 1;
            x3 = xx * x2 - x;
            y2 = yy * y - 1;
            y3 = yy * y2 - y;
            xy = x * y;
            p2 = x2 * y;
            p3 = x3 * y;
            q2 = y2 * x;
            q3 = y3 * x;
            pq = x2 * y2;
            AC[1] = V1[1] + V1[2] * x + V1[3] * x2 + V1[5] * y + V1[6] * y2 + V1[7] * y3 + V1[9] * p2 + V1[10] * p3
                    + V1[11] * q2 + V1[12] * q3;
            AC[2] = V2[1] + V2[2] * x + V2[3] * x2 + V2[5] * y + V2[6] * y2 + V2[7] * y3 + V2[8] * xy + V2[9] * p2
                    + V2[11] * q2 + V2[12] * q3;
            AC[3] = V3[1] + V3[2] * x + V3[3] * x2 + V3[4] * x3 + V3[5] * y + V3[6] * y2 + V3[7] * y3 + V3[8] * xy
                    + V3[9] * p2 + V3[10] * p3 + V3[11] * q2 + V3[12] * q3 + V3[13] * pq;
            AC[4] = V4[1] + V4[2] * x + V4[3] * x2 + V4[4] * x3 + V4[5] * y + V4[6] * y2 + V4[7] * y3 + V4[8] * xy
                    + V4[9] * p2 + V4[10] * p3 + V4[11] * q2 + V4[12] * q3;
            AC[5] = V5[1] + V5[2] * x + V5[3] * x2 + V5[4] * x3 + V5[5] * y + V5[6] * y2 + V5[7] * y3 + V5[8] * xy
                    + V5[11] * q2 + V5[12] * q3 + V5[13] * pq;
            AC[6] = V6[1] + V6[2] * x + V6[3] * x2 + V6[4] * x3 + V6[5] * y + V6[6] * y2 + V6[7] * y3 + V6[8] * xy
                    + V6[9] * p2 + V6[10] * p3 + V6[11] * q2 + V6[12] * q3 + V6[13] * pq;
            AC[7] = V7[1] + V7[2] * x + V7[3] * x2 + V7[5] * y + V7[6] * y2 + V7[7] * y3 + V7[8] * xy + V7[11] * q2;
            AC[8] = V8[1] + V8[2] * x + V8[3] * x2 + V8[5] * y + V8[6] * y2 + V8[7] * y3 + V8[8] * xy + V8[11] * q2;
            AC[0] = -3.04;
        } else {
            itype[0] = 4;
            if (rkappa >= 0.02) {
                itype[0] = 3;
            }
            npt[0] = 200;
            x = 1 + (rkappa - BKMXX1) * FBKX1;
            y = 1 + (sqrt(beta2) - BKMXY1) * FBKY1;
            xx = 2 * x;
            yy = 2 * y;
            x2 = xx * x - 1;
            x3 = xx * x2 - x;
            y2 = yy * y - 1;
            y3 = yy * y2 - y;
            xy = x * y;
            p2 = x2 * y;
            p3 = x3 * y;
            q2 = y2 * x;
            q3 = y3 * x;
            pq = x2 * y2;
            if (itype[0] == 3) {
                AC[1] = U1[1] + U1[2] * x + U1[3] * x2 + U1[5] * y + U1[6] * y2 + U1[7] * y3 + U1[8] * xy + U1[10] * p3
                        + U1[12] * q3 + U1[13] * pq;
                AC[2] = U2[1] + U2[2] * x + U2[3] * x2 + U2[5] * y + U2[6] * y2 + U2[7] * y3 + U2[8] * xy + U2[9] * p2
                        + U2[10] * p3 + U2[12] * q3 + U2[13] * pq;
                AC[3] = U3[1] + U3[2] * x + U3[3] * x2 + U3[5] * y + U3[6] * y2 + U3[7] * y3 + U3[8] * xy + U3[9] * p2
                        + U3[10] * p3 + U3[11] * q2 + U3[12] * q3 + U3[13] * pq;
                AC[4] = U4[1] + U4[2] * x + U4[3] * x2 + U4[4] * x3 + U4[5] * y + U4[6] * y2 + U4[7] * y3 + U4[8] * xy
                        + U4[9] * p2 + U4[10] * p3 + U4[11] * q2 + U4[12] * q3;
                AC[5] = U5[1] + U5[2] * x + U5[3] * x2 + U5[4] * x3 + U5[5] * y + U5[6] * y2 + U5[7] * y3 + U5[8] * xy
                        + U5[10] * p3 + U5[11] * q2 + U5[12] * q3 + U5[13] * pq;
                AC[6] = U6[1] + U6[2] * x + U6[3] * x2 + U6[4] * x3 + U6[5] * y + U6[7] * y3 + U6[8] * xy + U6[9] * p2
                        + U6[10] * p3 + U6[12] * q3 + U6[13] * pq;
                AC[7] = U7[1] + U7[2] * x + U7[3] * x2 + U7[4] * x3 + U7[5] * y + U7[6] * y2 + U7[8] * xy;
            }

            AC[8] = U8[1] + U8[2] * x + U8[3] * x2 + U8[4] * x3 + U8[5] * y + U8[6] * y2 + U8[7] * y3 + U8[8] * xy
                    + U8[9] * p2 + U8[10] * p3 + U8[11] * q2 + U8[13] * pq;
            AC[0] = -3.03;
        }

        AC[9] = (AC[8] - AC[0]) / npt[0];
        AC[10] = 1. / AC[9];
        if (itype[0] == 3) {
            x = (AC[7] - AC[8]) / (AC[7] * AC[8]);
            y = 1. / log(AC[8] / AC[7]);
            p2 = AC[7] * AC[7];
            AC[11] = p2 * (AC[1] * exp(-AC[2] * (AC[7] + AC[5] * p2) - AC[3] * exp(-AC[4] * (AC[7] + AC[6] * p2))) - 0.045 * y / AC[7]) / (1 + x * y * AC[7]);
            AC[12] = (0.045 + x * AC[11]) * y;
        }
        if (itype[0] == 4) {
            AC[13] = 0.995 / landauI(AC[8]);
        }

        if (!mode) {
            return;
        }
        //
        x = AC[0];
        if (WCM == null || WCM.length == 0) {
            throw new IllegalArgumentException("WCM must not be null or zero-length");
        }
        WCM[0] = 0;
        double fl;
        double fu;
        int k;
        fl = vavilovDenEval(x, AC, HC, itype[0]);
        for (k = 1; k <= npt[0]; k++) {
            x += AC[9];
            fu = vavilovDenEval(x, AC, HC, itype[0]);
            WCM[k] = WCM[k - 1] + fl + fu;
            fl = fu;
        }
        x = 0.5 * AC[9];
        for (k = 1; k <= npt[0]; k++) {
            WCM[k] *= x;
        }
    }
}
