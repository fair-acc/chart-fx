package io.fair_acc.dataset.testdata.spi;

/**
 * abstract error data set for graphical testing purposes this implementation generates a Gaussian function
 *
 * @author rstein
 */
public class GaussFunction extends AbstractTestFunction<GaussFunction> {
    private static final long serialVersionUID = -2090964369869257806L;
    private final double centre;
    private final double sigma;

    /**
     *
     * @param name data set name
     * @param count number of samples
     */
    public GaussFunction(final String name, final int count) {
        super(name, count);
        centre = 0.5 * count;
        sigma = 0.1 * count;
        this.update();
    }

    /**
     *
     * @param name data set name
     * @param count number of samples
     * @param mean mean position [0,count[
     * @param sigma standard deviation [0, inf[
     */
    public GaussFunction(final String name, final int count, final double mean, final double sigma) {
        super(name, count);
        this.centre = mean;
        this.sigma = sigma;
        this.update();
    }

    @Override
    public double[] generateY(final int count) {
        final double[] retVal = new double[count];
        for (int i = 0; i < count; i++) {
            retVal[i] = GaussFunction.gauss(i, centre, sigma) * sigma;
        }
        return retVal;
    }

    /**
     *
     * @param x coordinate X
     * @return value of Gaussian function at x (mean = 0, sigma = 1)
     */
    public static double gauss(double x) {
        return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
    }

    /**
     *
     * @param x coordiante x
     * @param mu mean value of Gaussian function
     * @param sigma standard deviation of Gaussian function
     * @return value of Gaussian function at x
     */
    public static double gauss(double x, double mu, double sigma) {
        return GaussFunction.gauss((x - mu) / sigma) / sigma;
    }
}
