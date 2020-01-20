package de.gsi.dataset.testdata.spi;

/**
 * abstract error data set for graphical testing purposes this implementation generates a triangular function
 * 
 * @author Alexander Krimm
 */
public class TriangleFunction extends AbstractTestFunction<TriangleFunction> {
    private static final long serialVersionUID = -3391027911729531271L;
    private double offset;

    /**
     * Creates a triangular function which rises from zero to one and back.
     * 
     * @param name data set name
     * @param count number of samples
     */
    public TriangleFunction(final String name, final int count) {
        super(name, count);
    }

    /**
     * Creates  a triangular function which rises from offset to offset+1 and back.
     * 
     * @param name data set name
     * @param count number of samples
     * @param offset offset to zero
     */
    public TriangleFunction(final String name, final int count, double offset) {
        super(name, count);
        this.offset = offset;
        update();
    }

    @Override
    public double[] generateY(final int count) {
        final double[] retVal = new double[count];
        for (int i = 0; i < (count + 1) / 2; i++) {
            retVal[i] = offset + ((double) (i * 2)) / count;
            retVal[count - 1 - i] = retVal[i];
        }
        return retVal;
    }

}
