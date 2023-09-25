package io.fair_acc.chartfx.renderer;

/**
 * @author rstein N.B. in-place computation have to be assumed (ie. the reduced data set is part and overwrites of the
 *         input arrays)
 */
public interface RendererDataReducer {
    /**
     * Internal function to the ErrorDataSetRenderer arrays are cached copies and operations are assumed to be performed
     * in-place (&lt;-&gt; for performance reasons/minimisation of memory allocation)
     *
     * @param xValues array of x coordinates
     * @param yValues array of y coordinates
     * @param xPointErrorsPos array of coordinates containing x+exp
     * @param xPointErrorsNeg array of coordinates containing x-exn
     * @param yPointErrorsPos array of coordinates containing x+eyp
     * @param yPointErrorsNeg array of coordinates containing x+eyn
     * @param styles point styles
     * @param pointSelected array containing the points that have been specially selected by the user
     * @param indexMin minimum index of those array that shall be considered
     * @param indexMax maximum index of those array that shall be considered
     * @return effective number of points that remain after the reduction
     */
    int reducePoints(final double[] xValues, final double[] yValues, final double[] xPointErrorsPos,
            final double[] xPointErrorsNeg, final double[] yPointErrorsPos, final double[] yPointErrorsNeg,
            final String[] styles, final boolean[] pointSelected, final int indexMin, final int indexMax);
}
