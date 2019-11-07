package de.gsi.math.fitter;

public class GaussFitting { // NOPMD - nomen est omen

    private static double fmean = 1.0;
    private static double fmeanError = 0.0;
    private static double frms = 1.0;
    private static double frmsError = 0.0;
    private static double farea = 0.0;
    private static double fareaError = 0.0;
    private static double fconstant = 1.0;
    private static double fconstantError = 1.0;

    /**
     * fit precise Gaussian curve width and location based on peak indication and width estimate
     * 
     * @param sliceX horizontal slice
     * @param sliceY vertical slice
     * @param meanEstimate initial mean estimate
     * @param sigma initial sigma estimate
     * @param nSigma n-sigma definition to be used
     */
    public static void fitData(double[] sliceX, double[] sliceY, double meanEstimate, double sigma, double nSigma) {
        int tmin = 0;
        int tmax = sliceX.length - 1;
        int center = (int) meanEstimate;
        int nadd = 100;
        // for zero padding at the front and end of the measurements to simplify the fit

        int halfWidth = (int) (nSigma * sigma);
        if (center - tmin < halfWidth)
            halfWidth = center - tmin;
        if (tmax - center < halfWidth)
            halfWidth = tmax - center;

        // create initial starting values for parameters
        double x = 0.;
        double x2 = 0.;
        double norm = 0.;
        double dx = sliceX[1] - sliceX[0];
        double area = 0.0;
        double eqArea = 0.0;
        double[] pos = new double[2 * halfWidth + 2 * nadd];
        double[] meas = new double[2 * halfWidth + 2 * nadd];
        double[] var = new double[2 * halfWidth + 2 * nadd];
        // GaussFcn theFCN = new GaussFcn(meas, pos, var);

        // copy data
        for (int i = 0; i < 2 * halfWidth; i++) {
            double valX = sliceX[center - halfWidth + i];
            double valY = sliceY[center - halfWidth + i];
            pos[i + nadd] = valX;
            meas[i + nadd] = valY;
        }

        // zero padding at the begin and end of the data sample
        for (int i = 0; i < nadd; i++) {
            pos[nadd - i] = pos[nadd] - dx * i;
            pos[nadd + 2 * halfWidth + i] = pos[nadd + 2 * halfWidth - 1] + dx * i;
            meas[i] = 0.0;
            meas[nadd + 2 * halfWidth + i] = 0.0;
        }

        for (int i = 0; i < meas.length; i++) {

            // pos[i] = (double)i/(double)(meas.length);
            // meas[i] = testdata.valueAt(pos[i]);

            var[i] = Math.pow(meas[i] * 0.1, 2);
            norm += meas[i];
            x += (meas[i] * pos[i]);
            x2 += (meas[i] * pos[i] * pos[i]);
            area += dx * meas[i];
            eqArea += meas[i];
        }

        double mean = x / norm;
        double rms2 = x2 / norm - mean * mean;
        double rms = rms2 > 0. ? Math.sqrt(rms2) : 1.;

        // System.out.printf("initial assumption: %g %g %g\n",mean,rms,area);
        // System.out.printf("measurement array as %s elements\n", meas.length);
        // MnUserParameters upar = new MnUserParameters();
        // upar.add("mean", mean, 0.01);
        // upar.add("rms", rms, rms*0.1);
        // upar.add("area", area, area*0.1);
        // upar.add("mean", mean, mean);
        // upar.add("rms", rms, rms);
        // upar.add("area", area, area);

        // redirect migrad output to nirvana
        // TODO: redirect output to some error string
        // PrintStream temp = System.out;
        // System.setOut(null);
        // create minimizer (default constructor)
        // MnMigrad migrad = new MnMigrad(theFCN, init_par, init_err);
        // MnMigrad migrad = new MnMigrad(theFCN, upar);
        // migrad.setCheckAnalyticalDerivatives(false);
        // migrad.setErrorDef(2);

        // ... and minimise
        // FunctionMinimum min = migrad.minimize(300, 0.3);
        // fmean = min.userState().value("mean");
        // frms = min.userState().value("rms");
        // fconstant = min.userState().value("area");

        fmean = mean;
        frms = rms;
        fconstant = area;
        farea = eqArea;

        // output
        // System.out.println("minimum: " + min);

        // create MINOS error factory
        // MnMinos minos = new MnMinos(theFCN, min);
        // System.setOut(temp);
        // System.out.printf("mean %s rms %s\n", fmean, frms);

        // fmean_error = choose_absmax(minos.lower(0),minos.upper(0));
        // frms_error = choose_absmax(minos.lower(1),minos.upper(1));
        // fconstant_error = choose_absmax(minos.lower(2),minos.upper(2));
    }

    public static double getArea() {
        return farea;
    }

    public static double getAreaError() {
        return fareaError;
    }

    public static double getConstant() {
        return fconstant;
    }

    public static double getConstantError() {
        return fconstantError;
    }

    public static double getMean() {
        return fmean;
    }

    public static double getMeanError() {
        return fmeanError;
    }

    public static double getRMS() {
        return frms;
    }

    public static double getRMSError() {
        return frmsError;
    }

    public static void main(String[] args) {
        int n = 100;
        double[] valX = new double[n];
        double[] valY = new double[n];
        double mu = 3, sigma = 0.5;
        for (int i = 0; i < n; i++) {
            valX[i] = 0.1 * i;
            valY[i] = Math.exp(-0.5 * Math.pow((valX[i] - mu) / sigma, 2)) / (Math.sqrt(2 * Math.PI) * sigma);
        }
        GaussFitting.fitData(valX, valY, 40, 20, 20);

        GaussFitting.print();
    }

    public static void print() {
        System.out.printf("mean    : %s \t+- %s%nrms     : %s \t+- %s%nconstant: %s \t+- %s%narea    : %s \t+- %s\n", // NOPMD
                                                                                                                      // --
                                                                                                                      // acceptable
                                                                                                                      // debugging
                                                                                                                      // use
                getMean(), getMeanError(), getRMS(), getRMSError(), getConstant(), getConstantError(), getArea(),
                getAreaError());
    }

//    public static int removeSpuriousPeaks(double[] posX, double[] measY, double sigma) {
//        int npeaks = posX.length;
//
//        // quick (since only few peaks) bubble sorting of peaks
//        for (int i = 0; i < npeaks; i++) {
//            for (int j = i + 1; j < npeaks; j++) {
//                if (posX[i] >= posX[j]) {
//                    double temp;
//                    temp = posX[i];
//                    posX[i] = posX[j];
//                    posX[j] = temp;
//                    temp = measY[i];
//                    measY[i] = measY[j];
//                    measY[j] = temp;
//                }
//            }
//        }
//
//        double[] tempPos = new double[npeaks];
//        double[] tempMeas = new double[npeaks];
//        int nPeaksReduced = 0;
//        // simple peak reduction by dropping peaks that are closer than one sigma
//        double last = -1e99;
//        for (int i = 0; i < npeaks; i++) {
//            if (posX[i] - last >= 6 * sigma) {
//                // keep sample				
//                tempPos[nPeaksReduced] = posX[i];
//                tempMeas[nPeaksReduced] = measY[i];
//                last = posX[i];
//                nPeaksReduced++;
//            } else {
//                // drop sample
//            }
//        }
//        posX = new double[nPeaksReduced];
//        measY = new double[nPeaksReduced];
//        for (int i = 0; i < nPeaksReduced; i++) {
//            posX[i] = tempPos[i];
//            measY[i] = tempMeas[i];
//        }
//
//        return nPeaksReduced;
//    }

}
