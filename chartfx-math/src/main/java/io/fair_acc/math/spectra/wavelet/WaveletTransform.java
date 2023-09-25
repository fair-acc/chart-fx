package io.fair_acc.math.spectra.wavelet;

public class WaveletTransform extends WaveletCoefficients {
    private final Wavelet fwavelet = Wavelet.Daubechies2;

    protected final double sqrt_3 = Math.sqrt(3);
    protected final double denom = 4 * Math.sqrt(2);
    //
    // forward transform scaling (smoothing) coefficients
    //
    protected final double h0 = (1 + sqrt_3) / denom;
    protected final double h1 = (3 + sqrt_3) / denom;
    protected final double h2 = (3 - sqrt_3) / denom;
    protected final double h3 = (1 - sqrt_3) / denom;

    //
    // forward transform wavelet coefficients
    //
    protected final double g0 = h3;
    protected final double g1 = -h2;
    protected final double g2 = h1;
    protected final double g3 = -h0;

    //
    // Inverse transform coefficients for smoothed values
    //
    protected final double Ih0 = h2;
    protected final double Ih1 = g2; // h1
    protected final double Ih2 = h0;
    protected final double Ih3 = g0; // h3
    //
    // Inverse transform for wavelet values
    //
    protected final double Ig0 = h3;
    protected final double Ig1 = g3; // -h0
    protected final double Ig2 = h1;
    protected final double Ig3 = g1; // -h2

    int allocating_downsampling_convolution(final double[] input, final int N, final double[] filter, final int F,
            final double[] output, final int step, final MODE mode) {
        int i, j, F_minus_1, N_extended_len, N_extended_right_start;
        int start, stop, index = 0;
        double sum, tmp;
        double[] buffer;

        F_minus_1 = F - 1;
        start = F_minus_1 + step - 1;

        // allocate memory and copy input
        if (mode != MODE.MODE_PERIODIZATION) {
            N_extended_len = N + 2 * F_minus_1;
            N_extended_right_start = N + F_minus_1;

            buffer = new double[N_extended_len];

            System.arraycopy(input, F_minus_1, buffer, 0, N);
            stop = N_extended_len;

        } else {
            N_extended_len = N + F - 1;
            N_extended_right_start = N - 1 + F / 2;

            buffer = new double[N_extended_len];

            System.arraycopy(input, F / 2 - 1, buffer, 0, N);
            start -= 1;

            if (step == 1) {
                stop = N_extended_len - 1;
            } else {
                // step == 2
                stop = N_extended_len;
            }
        }

        // copy extended signal elements
        switch (mode) {
        case MODE_PERIODIZATION:
            if (N % 2 > 0) { // odd - repeat last element
                buffer[N_extended_right_start] = input[N - 1];
                for (j = 1; j < F / 2; ++j) {
                    buffer[N_extended_right_start + j] = buffer[F / 2 - 2 + j]; // copy from begining of `input` to right
                }
                for (j = 0; j < F / 2 - 1; ++j) {
                    // copy from 'buffer' to left
                    buffer[F / 2 - 2 - j] = buffer[N_extended_right_start - j];
                }
            } else {
                for (j = 0; j < F / 2; ++j) {
                    buffer[N_extended_right_start + j] = input[j % N]; // copy from begining of `input` to right
                }
                for (j = 0; j < F / 2 - 1; ++j) {
                    // copy from 'buffer' to left
                    buffer[F / 2 - 2 - j] = buffer[N_extended_right_start - 1 - j];
                }
            }
            break;

        case MODE_SYMMETRIC:
            for (j = 0; j < N; ++j) {
                buffer[F_minus_1 - 1 - j] = input[j % N];
                buffer[N_extended_right_start + j] = input[N - 1 - j % N];
            }
            i = j;
            // use `buffer` as source
            for (; j < F_minus_1; ++j) {
                buffer[F_minus_1 - 1 - j] = buffer[N_extended_right_start - 1 + i - j];
                buffer[N_extended_right_start + j] = buffer[F_minus_1 + j - i];
            }
            break;

        case MODE_ASYMMETRIC:
            for (j = 0; j < N; ++j) {
                buffer[F_minus_1 - 1 - j] = input[0] - input[j % N];
                buffer[N_extended_right_start + j] = input[N - 1] - input[N - 1 - j % N];
            }
            i = j;
            // use `buffer` as source
            for (; j < F_minus_1; ++j) {
                buffer[F_minus_1 - 1 - j] = buffer[N_extended_right_start - 1 + i - j];
                buffer[N_extended_right_start + j] = buffer[F_minus_1 + j - i];
            }
            break;

        case MODE_SMOOTH:
            if (N > 1) {
                tmp = input[0] - input[1];
                for (j = 0; j < F_minus_1; ++j) {
                    buffer[j] = input[0] + tmp * (F_minus_1 - j);
                }
                tmp = input[N - 1] - input[N - 2];
                for (j = 0; j < F_minus_1; ++j) {
                    buffer[N_extended_right_start + j] = input[N - 1] + tmp * j;
                }
                break;
            }

        case MODE_CONSTANT_EDGE:
            for (j = 0; j < F_minus_1; ++j) {
                buffer[j] = input[0];
                buffer[N_extended_right_start + j] = input[N - 1];
            }
            break;

        case MODE_PERIODIC:
            for (j = 0; j < F_minus_1; ++j) {
                buffer[N_extended_right_start + j] = input[j % N]; // copy from beggining of `input` to right
            }

            for (j = 0; j < F_minus_1; ++j) {
                // copy from 'buffer' to left
                buffer[F_minus_1 - 1 - j] = buffer[N_extended_right_start - 1 - j];
            }
            break;

        case MODE_ZEROPAD:
        default:
            // memset(buffer, 0, sizeof(double)*F_minus_1);
            // memset(buffer+N_extended_right_start, 0, sizeof(double)*F_minus_1);
            // memcpy(buffer+N_extended_right_start, buffer, sizeof(double)*F_minus_1);
            break;
        }

        ///////////////////////////////////////////////////////////////////////
        // F - N-1 - filter in input range
        // perform convolution with decimation
        for (i = start; i < stop; i += step) { // input elements
            sum = 0;
            for (j = 0; j < F; ++j) {
                sum += buffer[i - j] * filter[j];
            }
            output[index++] = sum;
        }

        return 0;
    }

    int d_dec_a(final double[] input, final int input_len, final Wavelet wavelet, final double[] output,
            final int output_len, final MODE mode) {
        // check output length
        if (output_len != dwt_buffer_length(input_len, wavelet.getDecompHP().length, mode)) {
            return -1;
        }

        return downsampling_convolution(input, input_len, wavelet.getDecompLP(), wavelet.getDecompLP().length, output,
                2, mode);
    }

    // Decomposition of input with highpass filter
    int d_dec_d(final double[] input, final int input_len, final Wavelet wavelet, final double[] output,
            final int output_len, final MODE mode) {
        // check output length
        if (output_len != dwt_buffer_length(input_len, wavelet.getDecompHP().length, mode)) {
            return -1;
        }

        return downsampling_convolution(input, input_len, wavelet.getDecompHP(), wavelet.getDecompHP().length, output,
                2, mode);
    }

    // IDWT reconstruction from aproximation and detail coeffs
    //
    // If fix_size_diff is 1 then coeffs arrays can differ by one in length (this
    // is useful in multilevel decompositions and reconstructions of odd-length signals)
    // Requires zoer-filled output buffer
    int d_idwt(final double[] coeffs_a, final int coeffs_a_len, final double[] coeffs_d, final int coeffs_d_len,
            final Wavelet wavelet, final double[] output, final int output_len, final MODE mode,
            final int fix_size_diff) {
        int input_len;

        // If one of coeffs array is null then the reconstruction will be performed
        // using the other one

        if (coeffs_a != null && coeffs_d != null) {
            if (fix_size_diff > 0) {
                if ((coeffs_a_len > coeffs_d_len ? coeffs_a_len - coeffs_d_len : coeffs_d_len - coeffs_a_len) > 1) { // abs(a-b)
                    return -1;
                }

                input_len = coeffs_a_len > coeffs_d_len ? coeffs_d_len : coeffs_a_len; // min
            } else {
                if (coeffs_a_len != coeffs_d_len) {
                    return -1;
                }

                input_len = coeffs_a_len;
            }

        } else if (coeffs_a != null) {
            input_len = coeffs_a_len;

        } else if (coeffs_d != null) {
            input_len = coeffs_d_len;

        } else {
            return -1;
        }

        // check output size
        if (output_len != idwt_buffer_length(input_len, wavelet.getReconLP().length, mode)) {
            return -1;
        }

        // // set output to zero (this can be ommited if output array is already cleared)
        // memset(output, 0, output_len * sizeof(double));

        // reconstruct approximation coeffs with lowpass reconstruction filter
        if (coeffs_a != null) {
            if (upsampling_convolution_valid_sf(coeffs_a, input_len, wavelet.getReconLP(), wavelet.getReconLP().length,
                        output, output_len, mode)
                    < 0) {
                return -1;
            }
        }
        // and add reconstruction of details coeffs performed with highpass reconstruction filter
        if (coeffs_d != null) {
            if (upsampling_convolution_valid_sf(coeffs_d, input_len, wavelet.getReconHP(), wavelet.getReconHP().length,
                        output, output_len, mode)
                    < 0) {
                return -1;
            }
        }

        return 0;
    }

    int d_rec_a(final double[] coeffs_a, final int coeffs_len, final Wavelet wavelet, final double[] output,
            final int output_len) {
        // check output length
        if (output_len != reconstruction_buffer_length(coeffs_len, wavelet.getReconLP().length)) {
            return -1;
        }

        return upsampling_convolution_full(coeffs_a, coeffs_len, wavelet.getReconLP(), wavelet.getReconLP().length,
                output, output_len);
    }

    // Decomposition of input with lowpass filter

    int d_rec_d(final double[] coeffs_d, final int coeffs_len, final Wavelet wavelet, final double[] output,
            final int output_len) {
        // check for output length
        if (output_len != reconstruction_buffer_length(coeffs_len, wavelet.getReconHP().length)) {
            return -1;
        }

        return upsampling_convolution_full(coeffs_d, coeffs_len, wavelet.getReconHP(), wavelet.getReconHP().length,
                output, output_len);
    }

    // basic SWT step
    // TODO: optimize
    int d_swt_(final double[] input, final int input_len, final double[] filter, final int filter_len,
            final double[] output, final int output_len, final int level) {
        double[] e_filter;
        int i, e_filter_len;

        if (level < 1) {
            return -1;
        }

        if (level > swt_max_level(input_len)) {
            return -2;
        }

        if (output_len != swt_buffer_length(input_len)) {
            return -1;
        }

        // TODO: quick hack, optimise
        if (level > 1) {
            // allocate filter first
            e_filter_len = filter_len << level - 1;
            if (e_filter_len <= 0) {
                return -1;
            }
            e_filter = new double[e_filter_len];

            // compute upsampled filter values
            for (i = 0; i < filter_len; ++i) {
                e_filter[i << level - 1] = filter[i];
            }
            i = downsampling_convolution(input, input_len, e_filter, e_filter_len, output, 1, MODE.MODE_PERIODIZATION);

            return i;

        } else {
            return downsampling_convolution(input, input_len, filter, filter_len, output, 1, MODE.MODE_PERIODIZATION);
        }
    }

    // Direct reconstruction with lowpass reconstruction filter

    // Approximation at specified level
    // input - approximation coeffs from upper level or signal if level == 1
    int d_swt_a(final double[] input, final int input_len, final Wavelet wavelet, final double[] output,
            final int output_len, final int level) {
        return d_swt_(input, input_len, wavelet.getDecompLP(), wavelet.getDecompLP().length, output, output_len, level);
    }

    // Direct reconstruction with highpass reconstruction filter

    // Details at specified level
    // input - approximation coeffs from upper level or signal if level == 1
    int d_swt_d(final double[] input, final int input_len, final Wavelet wavelet, final double[] output,
            final int output_len, final int level) {
        return d_swt_(input, input_len, wavelet.getDecompHP(), wavelet.getDecompHP().length, output, output_len, level);
    }

    /**
     * Forward wavelet transform
     *
     * @param s input vector
     */
    public void daubTrans(final double[] s) {
        final int N = s.length;
        int n;
        for (n = N; n >= fwavelet.getLength(); n >>= 1) {
            transform(s, n);
        }
    }

    int downsampling_convolution(final double[] input, final int N, final double[] filter, final int F,
            final double[] output, final int step, final MODE mode) {
        // This convolution performs efficient down-sampling by computing every step'th
        // element of normal convolution (currently tested only for step=1 and step=2).
        //
        // It also implements several different strategies of dealing with border
        // distortion problem (the problem of computing convolution for not existing
        // elements of signal). To handle this the signal has to be "extended" on both
        // sides by computing the missing values.
        //
        // General schema is as follows:
        // 1. Handle extended on the left, convolve filter with samples computed for time < 0
        // 2. Do the normal decimated convolution of filter with signal samples
        // 3. Handle extended on the right, convolve filter with samples computed for time > n-1

        int i, j, k, F_2, corr;
        int start, index = 0;
        double sum, tmp;

        i = start = step - 1; // first element taken from input is input[step-1]

        if (F <= N) {
            ///////////////////////////////////////////////////////////////////////
            // 0 - F-1 - sliding in filter

            // signal extension mode
            switch (mode) {
            case MODE_SYMMETRIC:
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j <= i; ++j) {
                        sum += filter[j] * input[i - j];
                    }
                    k = i + 1;
                    for (j = i + 1; j < F; ++j) {
                        sum += filter[j] * input[j - k];
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_ASYMMETRIC:
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j <= i; ++j) {
                        sum += filter[j] * input[i - j];
                    }
                    k = i + 1;
                    for (j = i + 1; j < F; ++j) {
                        sum += filter[j] * (input[0] - input[j - k]); // -=
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_CONSTANT_EDGE:
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j <= i; ++j) {
                        sum += filter[j] * input[i - j];
                    }

                    for (j = i + 1; j < F; ++j) {
                        sum += filter[j] * input[0];
                    }

                    output[index++] = sum;
                }
                break;

            case MODE_SMOOTH:
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j <= i; ++j) {
                        sum += filter[j] * input[i - j];
                    }
                    tmp = input[0] - input[1];
                    for (j = i + 1; j < F; ++j) {
                        sum += filter[j] * (input[0] + tmp * (j - i));
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_PERIODIC:
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j <= i; ++j) {
                        sum += filter[j] * input[i - j];
                    }

                    k = N + i;
                    for (j = i + 1; j < F; ++j) {
                        sum += filter[j] * input[k - j];
                    }

                    output[index++] = sum;
                }
                break;

            case MODE_PERIODIZATION:
                // extending by (F-2)/2 elements
                start = F / 2;

                F_2 = F / 2;
                corr = 0;
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j < i + 1 - corr; ++j) {
                        // overlapping
                        sum += filter[j] * input[i - j - corr];
                    }

                    if (N % 2 > 0) {
                        if (F - j > 0) { // if something to extend
                            sum += filter[j] * input[N - 1];
                            if (F - j > 0) {
                                for (k = 2 - corr; k <= F - j; ++k) {
                                    sum += filter[j - 1 + k] * input[N - k + 1];
                                }
                            }
                        }
                    } else { // extra element from input -> i0 i1 i2 [i2]
                        for (k = 1; k <= F - j; ++k) {
                            sum += filter[j - 1 + k] * input[N - k];
                        }
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_ZEROPAD:
            default:
                for (i = start; i < F; i += step) {
                    sum = 0;
                    for (j = 0; j <= i; ++j) {
                        sum += filter[j] * input[i - j];
                    }
                    output[index++] = sum;
                }
                break;
            }

            ///////////////////////////////////////////////////////////////////////
            // F - N-1 - filter in input range
            // most time is spent in this loop
            for (; i < N; i += step) { // input elements,
                sum = 0;
                for (j = 0; j < F; ++j) {
                    sum += input[i - j] * filter[j];
                }
                output[index++] = sum;
            }

            ///////////////////////////////////////////////////////////////////////
            // N - N+F-1 - sliding out filter
            switch (mode) {
            case MODE_SYMMETRIC:
                for (; i < N + F - 1; i += step) { // input elements
                    sum = 0;
                    k = i - N + 1; // 1, 2, 3 // overlapped elements
                    for (j = k; j < F; ++j) {
                        // TODO: j < F-_offset
                        sum += filter[j] * input[i - j];
                    }

                    for (j = 0; j < k; ++j) {
                        // out of boundary //TODO: j = _offset
                        sum += filter[j] * input[N - k + j]; // j-i-1 0*(N-1), 0*(N-2) 1*(N-1)
                    }

                    output[index++] = sum;
                }
                break;

            case MODE_ASYMMETRIC:
                for (; i < N + F - 1; i += step) { // input elements
                    sum = 0;
                    k = i - N + 1;
                    for (j = k; j < F; ++j) {
                        // overlapped elements
                        sum += filter[j] * input[i - j];
                    }

                    for (j = 0; j < k; ++j) {
                        // out of boundary
                        sum += filter[j] * (input[N - 1] - input[N - k - 1 + j]); // -= j-i-1
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_CONSTANT_EDGE:
                for (; i < N + F - 1; i += step) { // input elements
                    sum = 0;
                    k = i - N + 1;
                    for (j = k; j < F; ++j) {
                        // overlapped elements
                        sum += filter[j] * input[i - j];
                    }

                    for (j = 0; j < k; ++j) {
                        // out of boundary (filter elements [0, k-1])
                        sum += filter[j] * input[N - 1]; // input[N-1] = const
                    }

                    output[index++] = sum;
                }
                break;

            case MODE_SMOOTH:
                for (; i < N + F - 1; i += step) { // input elements
                    sum = 0;
                    k = i - N + 1; // 1, 2, 3, ...
                    for (j = k; j < F; ++j) {
                        // overlapped elements
                        sum += filter[j] * input[i - j];
                    }

                    tmp = input[N - 1] - input[N - 2];
                    for (j = 0; j < k; ++j) {
                        // out of boundary (filter elements [0, k-1])
                        sum += filter[j] * (input[N - 1] + tmp * (k - j));
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_PERIODIC:
                for (; i < N + F - 1; i += step) { // input elements
                    sum = 0;
                    k = i - N + 1;
                    for (j = k; j < F; ++j) {
                        // overlapped elements
                        sum += filter[j] * input[i - j];
                    }
                    for (j = 0; j < k; ++j) {
                        // out of boundary (filter elements [0, k-1])
                        sum += filter[j] * input[k - 1 - j];
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_PERIODIZATION:

                for (; i < N - step + F / 2 + 1 + N % 2; i += step) { // input elements
                    sum = 0;
                    k = i - N + 1;
                    for (j = k; j < F; ++j) {
                        // overlapped elements
                        sum += filter[j] * input[i - j];
                    }

                    if (N % 2 == 0) {
                        for (j = 0; j < k; ++j) { // out of boundary (filter elements [0, k-1])
                            sum += filter[j] * input[k - 1 - j];
                        }
                    } else { // repeating extra element -> i0 i1 i2 [i2]
                        for (j = 0; j < k - 1; ++j) {
                            // out of boundary (filter elements [0, k-1])
                            sum += filter[j] * input[k - 2 - j];
                        }
                        sum += filter[k - 1] * input[N - 1];
                    }
                    output[index++] = sum;
                }
                break;

            case MODE_ZEROPAD:
            default:
                for (; i < N + F - 1; i += step) {
                    sum = 0;
                    for (j = i - (N - 1); j < F; ++j) {
                        sum += input[i - j] * filter[j];
                    }
                    output[index++] = sum;
                }
                break;
            }

            return 0;

        } else {
            // reallocating memory for short signals (shorter than filter) is cheap
            return allocating_downsampling_convolution(input, N, filter, F, output, step, mode);
        }
    }

    int dwt_buffer_length(final int input_len, final int filter_len, final MODE mode) {
        if (input_len < 1 || filter_len < 1) {
            return 0;
        }

        if (mode == MODE.MODE_PERIODIZATION) {
            return (int) Math.ceil(input_len / 2.);
        }
        return (int) Math.floor((input_len + filter_len - 1) / 2.);
    }

    int dwt_max_level(final int input_len, final int filter_len) {
        if (input_len < 1 || filter_len < 2) {
            return 0;
        }

        return (int) Math.floor(Math.log((double) input_len / (double) (filter_len - 1)) / Math.log(2.0));
    }

    int idwt_buffer_length(final int coeffs_len, final int filter_len, final MODE mode) {
        if (coeffs_len < 0 || filter_len < 0) {
            return 0;
        }

        if (mode == MODE.MODE_PERIODIZATION) {
            return 2 * coeffs_len;
        }
        return 2 * coeffs_len - filter_len + 2;
    }

    /**
     * Inverse wavelet transform
     *
     * @param coef input vector
     */
    public void invDaubTrans(final double[] coef) {
        final int N = coef.length;
        int n;
        for (n = fwavelet.getLength(); n <= N; n <<= 1) {
            invTransform(coef, n);
        }
    }

    public double[] invoke(final double[] input) {
        final int nfilter = fwavelet.getLength(); // wavelet filter length
        final double[] h = fwavelet.getDecompHP();
        final double[] g = fwavelet.getDecompLP();

        // This function assumes input.length=2^n, n>1
        final double[] output = new double[input.length];

        for (int length = input.length >> 1;; length >>= 1) {
            // length=2^n, WITH DECREASING n
            for (int i = 0; i < length - nfilter; i++) {
                double sum = 0;
                double diff = 0;
                final int i2 = i * 2;
                for (int k = 0; k < nfilter; k++) {
                    sum += input[i2 + k] * g[k];
                    diff += input[i2 + k] * h[k];
                }
                output[i] = sum;
                output[length + i] = diff;
            }
            if (length == 1) {
                return output;
            }

            // Swap arrays to do next iteration
            System.arraycopy(output, 0, input, 0, length << 1);
        }
    }

    protected void invTransform(final double[] a, final int n) {
        if (n >= 4) {
            int i, j;
            final int half = n >> 1;
            final int halfPls1 = half + 1;

            final double[] tmp = new double[n];

            // last smooth val last coef. first smooth first coef
            tmp[0] = a[half - 1] * Ih0 + a[n - 1] * Ih1 + a[0] * Ih2 + a[half] * Ih3;
            tmp[1] = a[half - 1] * Ig0 + a[n - 1] * Ig1 + a[0] * Ig2 + a[half] * Ig3;
            j = 2;
            for (i = 0; i < half - 1; i++) {
                // smooth val coef. val smooth val coef. val
                tmp[j++] = a[i] * Ih0 + a[i + half] * Ih1 + a[i + 1] * Ih2 + a[i + halfPls1] * Ih3;
                tmp[j++] = a[i] * Ig0 + a[i + half] * Ig1 + a[i + 1] * Ig2 + a[i + halfPls1] * Ig3;
            }
            for (i = 0; i < n; i++) {
                a[i] = tmp[i];
            }
        }
    }

    int reconstruction_buffer_length(final int coeffs_len, final int filter_len) {
        if (coeffs_len < 1 || filter_len < 1) {
            return 0;
        }

        return 2 * coeffs_len + filter_len - 2;
    }

    int swt_buffer_length(final int input_len) {
        if (input_len < 0) {
            return 0;
        }

        return input_len;
    }

    int swt_max_level(int input_len) {
        int i, j;
        i = (int) Math.floor(Math.log(input_len) / Math.log(2.0));

        // check how many times (maximum i times) input_len is divisible by 2
        for (j = 0; j <= i; ++j) {
            if ((input_len & 0x1) == 1) {
                return j;
            }
            input_len >>= 1;
        }
        return i;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // like downsampling_convolution, but with memory allocation
    //

    /**
     * <p>
     * Forward wavelet transform.
     * </p>
     * <p>
     * Note that at the end of the computation the calculation wraps around to the beginning of the signal.
     * </p>
     *
     * @param a input signal, which will be replaced by its output transform
     * @param n length of the signal, and must be a power of 2
     */
    protected void transform(final double[] a, final int n) {
        final int nfilter = fwavelet.getLength(); // wavelet filter length
        final double[] h = fwavelet.getDecompHP();
        final double[] g = fwavelet.getDecompLP();
        System.err.printf("dim %d %d\n", h.length, g.length);
        System.err.printf("A h0 = %f h1 = %f h2 = %f h3= %f\n", h0, h1, h2, h3);
        System.err.printf("B h0 = %f h1 = %f h2 = %f h3= %f\n", h[0], h[1], h[2], h[3]);
        if (n >= nfilter) {
            int i;
            final int half = n >> 1;

            final double[] tmp = new double[n];

            i = 0;
            for (int j = 0; j < n - nfilter - 1; j = j + 2) {
                tmp[i] = 0;
                tmp[i + half] = 0;
                for (int k = 0; k < nfilter; k++) {
                    tmp[i] += a[j + k] * h[k];
                    tmp[i + half] = a[j + k] * g[k];
                }
                i++;
            }

            tmp[i] = a[n - 2] * h[0] + a[n - 1] * h[1] + a[0] * h[2] + a[1] * h[3];
            tmp[i + half] = a[n - 2] * g[0] + a[n - 1] * g[1] + a[0] * g[2] + a[1] * g[3];

            for (i = 0; i < n; i++) {
                a[i] = tmp[i];
            }
        }
    } // transform

    ///////////////////////////////////////////////////////////////////////////////
    // requires zero-filled output buffer
    // output is larger than input
    // performs "normal" convolution of "upsampled" input coeffs array with filter

    // -> swt - todo
    int upsampled_filter_convolution(final double[] input, final int N, final double[] filter, final int F,
            final double[] output, final int step, final MODE mode) {
        return -1;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // performs IDWT for all modes (PERIODIZATION & others)
    //
    // The upsampling is performed by splitting filters to even and odd elements
    // and performing 2 convolutions
    //
    // This code is quite complicated because of special handling of PERIODIZATION mode.
    // The input data has to be periodically extended for this mode.
    // Refactoring this case out can dramatically simplify the code for other modes.
    // TODO: refactor, split into several functions
    // (Sorry for not keeping the 80 chars in line limit)

    int upsampling_convolution_full(final double[] input, final int N, final double[] filter, final int F,
            final double[] output, final int O) {
        int i, j;

        if (F < 2) {
            return -1;
        }

        int offset = N - 1 << 1;

        for (i = N - 1; i >= 0; --i) {
            // sliding in filter from the right (end of input)
            // i0 0 i1 0 i2 0
            // f1 -> o1
            // f1 f2 -> o2
            // f1 f2 f3 -> o3

            for (j = 0; j < F; ++j) {
                output[j + offset] += input[i] * filter[j]; // input[i] - final in loop
            }
            offset -= 2;
        }

        return 0;
    }

    int upsampling_convolution_valid_sf(final double[] input, final int N, final double[] filter, final int F,
            final double[] output, final int O, final MODE mode) {
        double[] filter_even, filter_odd;
        double[] periodization_buf = null;
        // double[] periodization_buf_rear = null;
        int i, j, k, N_p = 0;
        final int F_2 = F / 2;

        if (F % 2 > 0) {
            return -3; // Filter must have even-length.
        }

        ///////////////////////////////////////////////////////////////////////////
        // Handle special situation when input coeff data is shorter than half of
        // the filter's length. The coeff array has to be extended periodically.
        // This can be only valid for PERIODIZATION_MODE
        // TODO: refactor this branch
        if (N < F_2) {
            if (mode != MODE.MODE_PERIODIZATION) {
                return -2; // invalid lengths
            }

            // Input data for periodization mode has to be periodically extended

            // New length for temporary input
            N_p = F_2 - 1 + N;

            // periodization_buf will hold periodically copied input coeffs values
            periodization_buf = new double[N_p];

            // Copy input data to it's place in the periodization_buf
            // -> [0 0 0 i1 i2 i3 0 0 0]
            k = (F_2 - 1) / 2;
            for (i = k; i < k + N; ++i) {
                periodization_buf[i] = input[(i - k) % N];
            }

            // if(N%2)
            // periodization_buf[i++] = input[N-1];

            // [0 0 0 i1 i2 i3 0 0 0]
            // points here ^^
            final int buf_rear = i - 1;

            // copy cyclically () to right
            // [0 0 0 i1 i2 i3 i1 i2 ...]
            j = i - k;
            for (; i < N_p; ++i) {
                periodization_buf[i] = periodization_buf[i - j];
            }

            // copy cyclically () to left
            // [... i2 i3 i1 i2 i3 i1 i2 i3]
            j = 0;
            for (i = k - 1; i >= 0; --i) {
                periodization_buf[i] = periodization_buf[buf_rear + j];
                --j;
            }

            // Now perform the valid convolution
            if (F_2 % 2 > 0) {
                upsampling_convolution_valid_sf(periodization_buf, N_p, filter, F, output, O, MODE.MODE_ZEROPAD);

                // The F_2%2==0 case needs special result fix (oh my, another one..)
            } else {
                // Cheap result fix for short inputs
                // Memory allocation for temporary output os done.
                // Computed temporary result is rewrited to output*

                final double[] ptr_out = new double[idwt_buffer_length(N, F, MODE.MODE_PERIODIZATION)];

                // Convolve here as for (F_2%2) branch above
                upsampling_convolution_valid_sf(periodization_buf, N_p, filter, F, ptr_out, O, MODE.MODE_ZEROPAD);

                // rewrite result to output
                for (i = 2 * N - 1; i > 0; --i) {
                    output[i] += ptr_out[i - 1];
                }
                // and the first element
                output[0] += ptr_out[2 * N - 1];
            }

            return 0;
        }
        //////////////////////////////////////////////////////////////////////////////

        // Otherwise (N >= F_2)

        // Allocate memory for even and odd elements of the filter
        filter_even = new double[F_2];
        filter_odd = new double[F_2];

        // split filter to even and odd values
        for (i = 0; i < F_2; ++i) {
            filter_even[i] = filter[i << 1];
            filter_odd[i] = filter[(i << 1) + 1];
        }

        ///////////////////////////////////////////////////////////////////////////
        // MODE_PERIODIZATION
        // This part is quite complicated and has some wild checking to get results
        // similar to those from Matlab(TM) Wavelet Toolbox

        if (mode == MODE.MODE_PERIODIZATION) {
            k = F_2 - 1;

            // Check if extending is really needed
            N_p = F_2 - 1 + (int) Math.ceil(k / 2.); /* split filter len correct. + extra samples */

            // ok, when is then:
            // 1. Allocate buffers for front and rear parts of extended input
            // 2. Copy periodically appriopriate elements from input to the buffers
            // 3. Convolve front buffer, input and rear buffer with even and odd
            // elements of the filter (this results in upsampling)
            // 4. Free memory

            // TODO: re-enable here
            /*
             * if(N_p > 0){ // ======= // Allocate memory only for the front and rear extension parts, not the // whole
             * input periodization_buf = new double[N_p]; periodization_buf_rear = new double[N_p]; // Memory checking
             * if(periodization_buf == null || periodization_buf_rear == null){ return -1; } // Fill buffers with
             * appropriate elements //if(k <= N){ // F_2-1 <= N // copy from beginning of input to end of buffer
             * System.arraycopy(input, 0, periodization_buf, N_p - k, k); for(i = 1; i <= (N_p - k); ++i)
             * periodization_buf[(N_p - k) - i] = input[N - (i%N)]; // copy from end of input to begginning of buffer
             * System.arraycopy(input, N - k, periodization_buf_rear, 0, k); for(i = 0; i < (N_p - k); ++i)
             * periodization_buf_rear[k + i] = input[i%N]; //} else { //
             * printf("Convolution.c: line %d. This code should not be executed. Please report use case.\n", __LINE__);
             * //} /////////////////////////////////////////////////////////////////// // Convolve filters with the
             * (front) periodization_buf and compute // the first part of output ptr_base = periodization_buf + F_2 - 1;
             * if(k%2 == 1){ sum_odd = 0; for(j = 0; j < F_2; ++j) sum_odd += filter_odd[j] * ptr_base[-j]; (ptr_out++)
             * += sum_odd; --k; if(k) upsampling_convolution_valid_sf(periodization_buf + 1, N_p-1, filter, F, ptr_out,
             * O-1, MODE_ZEROPAD); ptr_out += k; // k0 - 1 // really move backward by 1 } else if(k){
             * upsampling_convolution_valid_sf(periodization_buf, N_p, filter, F, ptr_out, O, MODE_ZEROPAD); ptr_out +=
             * k; } } } // MODE_PERIODIZATION
             * ///////////////////////////////////////////////////////////////////////////
             * /////////////////////////////////////////////////////////////////////////// // Perform _valid_
             * convolution (only when all filter_even and filter_odd elements // are in range of input data). // // This
             * part is simple, no extra hacks, just two convolutions in one loop ptr_base = (double[])input + F_2 - 1;
             * for(i = 0; i < N-(F_2-1); ++i){ // sliding over signal from left to right sum_even = 0; sum_odd = 0;
             * for(j = 0; j < F_2; ++j){ sum_even += filter_even[j] * ptr_base[i-j]; sum_odd += filter_odd[j] *
             * ptr_base[i-j]; } (ptr_out++) += sum_even; (ptr_out++) += sum_odd; } //
             * ///////////////////////////////////////////////////////////////////////////
             * /////////////////////////////////////////////////////////////////////////// // MODE_PERIODIZATION if(mode
             * == MODE.MODE_PERIODIZATION){ if(N_p > 0){ // ======= k = F_2-1; if(k%2 == 1){ if(F/2 <= N_p - 1){ // k >
             * 1 ? upsampling_convolution_valid_sf(periodization_buf_rear , N_p-1, filter, F, ptr_out, O-1,
             * MODE_ZEROPAD); } ptr_out += k; // move forward anyway -> see lower if(F_2%2 == 0){ // remaining one
             * element ptr_base = periodization_buf_rear + N_p - 1; sum_even = 0; for(j = 0; j < F_2; ++j){ sum_even +=
             * filter_even[j] * ptr_base[-j]; } (--ptr_out) += sum_even; // move backward first } } else { if(k){
             * upsampling_convolution_valid_sf(periodization_buf_rear, N_p, filter, F, ptr_out, O, MODE_ZEROPAD); } } }
             */
        } // MODE_PERIODIZATION
        ///////////////////////////////////////////////////////////////////////////

        return 0;
    }
}
