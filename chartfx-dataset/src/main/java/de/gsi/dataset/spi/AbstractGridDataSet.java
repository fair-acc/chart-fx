package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.GridDataSet;

public abstract class AbstractGridDataSet<U> extends AbstractDataSet<AbstractGridDataSet<U>> implements GridDataSet {
    /**
     * default constructor
     *
     * @param name the default name of the data set (meta data)
     * @param dimension dimension of this data set
     */
    public AbstractGridDataSet(final String name, final int dimension) {
        super(name, dimension);
    }

    @Override
    public int getGridIndex(final int dimIndex, final double x) {
        if (dimIndex >= getNGrid()) {
            throw new IndexOutOfBoundsException("dim index out of bounds");
        }
        if (getShape(dimIndex) == 0) {
            return 0;
        }

        if (!Double.isFinite(x)) {
            return 0;
        }

        if (x <= this.getAxisDescription(dimIndex).getMin()) {
            return 0;
        }

        final int lastIndex = getShape(dimIndex) - 1;
        if (x >= this.getAxisDescription(dimIndex).getMax()) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        return binarySearch(x, 0, lastIndex, i -> getGrid(dimIndex, i));
    }

    @Override
    public double getValue(final int dimIndex, final double... x) {
        final int nGrid = getNGrid();
        // get the lower corner of the cube containing x and the relative position of x inside it
        final int[] indices = new int[nGrid]; // lower corner of cube containing x
        final double[] pos = new double[nGrid]; // relative position of x inside the cube
        for (int dim = 0; dim < nGrid; dim++) {
            if (x[dim] < getAxisDescription(dim).getMin() || x[dim] > getAxisDescription(dim).getMax()) {
                return Double.NaN; // x outside of data range
            }
            indices[dim] = getGridIndex(dim, x[dim]);
            double x1;
            double x2;
            x1 = getGrid(dim, indices[dim]);
            if (x[dim] < x1) {
                x2 = x1;
                indices[dim] -= 1;
                x1 = getGrid(dim, indices[dim]);
            } else {
                x2 = getGrid(dim, indices[dim] + 1);
            }
            pos[dim] = (x2 - x[dim]) / (x2 - x1);
        }
        // get the values on all corners and their respective weights
        double result = 0;
        final int[] indicesLoc = new int[nGrid];
        for (int cubeIdx = 0; cubeIdx < 1 << nGrid; cubeIdx++) { // loop over all nGrid^2 corners
            System.arraycopy(indices, 0, indicesLoc, 0, nGrid);
            double cornerWeight = 1;
            for (int dim = 0; dim < nGrid; dim++) { // get the weight of that corner along all dimensions
                // get weigtht of current corner
                if (((cubeIdx >> dim) & 1) > 0) { // upper corner
                    indicesLoc[dim]++; // add local to global coordinate
                    cornerWeight *= 1 - pos[dim];
                } else { // lower corner
                    cornerWeight *= pos[dim];
                }
            }
            result += cornerWeight * get(dimIndex, indicesLoc);
        }
        return result;
    }

    @Override
    public DataSet recomputeLimits(final int dimIndex) {
        // first compute range (does not trigger notify events)
        DataRange newRange = new DataRange();
        if (dimIndex < getNGrid()) {
            final int dataCount = getShape(dimIndex);
            for (int i = 0; i < dataCount; i++) {
                newRange.add(getGrid(dimIndex, i));
            }
        } else {
            final int dataCount = getDataCount();
            for (int i = 0; i < dataCount; i++) {
                newRange.add(get(dimIndex, i));
            }
        }
        // set to new computed one and trigger notify event if different to old limits
        getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());
        return this;
    }
}
