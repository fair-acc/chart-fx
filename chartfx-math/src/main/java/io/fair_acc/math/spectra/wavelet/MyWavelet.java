package io.fair_acc.math.spectra.wavelet;

public class MyWavelet extends Lift {
    final static double sqrt3 = Math.sqrt(3);
    final static double sqrt2 = Math.sqrt(2);

    @Override
    public void forwardTrans(final double[] vec) {
        final int N = vec.length;

        for (int n = N; n > 1; n = n >> 1) {
            split(vec, n);
            updateOne(vec, n, Direction.forward); // update 1
            predict(vec, n, Direction.forward);
            update(vec, n, Direction.forward); // update 2
            normalize(vec, n, Direction.forward);
        }
    } // forwardTrans

    /**
     * <p>
     * Default two step Lifting Scheme inverse wavelet transform
     * </p>
     * <p>
     * inverseTrans is passed the result of an ordered wavelet transform, consisting of an average and a set of wavelet
     * coefficients. The inverse transform is calculated in-place and the result is returned in the argument array.
     * </p>
     */
    @Override
    public void inverseTrans(final double[] vec) {
        final int N = vec.length;

        for (int n = 2; n <= N; n = n << 1) {
            normalize(vec, n, Direction.inverse);
            update(vec, n, Direction.inverse);
            predict(vec, n, Direction.inverse);
            updateOne(vec, n, Direction.inverse);
            merge(vec, n);
        }
    } // inverseTrans

    protected void normalize(final double[] S, final int N, final Direction direction) {
        final int half = N >> 1;

        for (int n = 0; n < half; n++) {
            if (direction == Direction.forward) {
                // forward
                S[n] = (sqrt3 - 1.0) / sqrt2 * S[n];
                S[n + half] = (sqrt3 + 1.0) / sqrt2 * S[n + half];
            } else {
                // inverse
                S[n] = (sqrt3 + 1.0) / sqrt2 * S[n];
                S[n + half] = (sqrt3 - 1.0) / sqrt2 * S[n + half];
            }
        }
    } // normalise

    @Override
    protected void predict(final double[] S, final int N, final Direction direction) {
        final int half = N >> 1;

        if (direction == Direction.forward) {
            S[half] = S[half] - sqrt3 / 4.0 * S[0] - (sqrt3 - 2) / 4.0 * S[half - 1];
        } else {
            // inverse
            S[half] = S[half] + sqrt3 / 4.0 * S[0] + (sqrt3 - 2) / 4.0 * S[half - 1];
        }
        // predict, forward

        for (int n = 1; n < half; n++) {
            if (direction == Direction.forward) {
                S[half + n] = S[half + n] - sqrt3 / 4.0 * S[n] - (sqrt3 - 2) / 4.0 * S[n - 1];
            } else {
                // inverse
                S[half + n] = S[half + n] + sqrt3 / 4.0 * S[n] + (sqrt3 - 2) / 4.0 * S[n - 1];
            }
        }

    } // predict

    @Override
    protected void update(final double[] S, final int N, final Direction direction) {
        final int half = N >> 1;

        for (int n = 0; n < half - 1; n++) {
            if (direction == Direction.forward) {
                S[n] = S[n] - S[half + n + 1];
            } else {
                // inverse
                S[n] = S[n] + S[half + n + 1];
            }
        }

        if (direction == Direction.forward) {
            S[half - 1] = S[half - 1] - S[half];
        } else {
            S[half - 1] = S[half - 1] + S[half];
        }
    } // update

    protected void updateOne(final double[] S, final int N, final Direction direction) {
        final int half = N >> 1;

        for (int n = 0; n < half; n++) {
            final double updateVal = sqrt3 * S[half + n];

            if (direction == Direction.forward) {
                S[n] = S[n] + updateVal;
            } else {
                // inverse
                S[n] = S[n] - updateVal;
            }
        }
    } // updateOne
}
