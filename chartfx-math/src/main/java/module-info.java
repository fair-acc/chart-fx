module de.gsi.chartfx.math {
    requires org.slf4j;

    requires it.unimi.dsi.fastutil;
    requires jdk.unsupported;

    requires de.gsi.chartfx.dataset;

    requires commons.math3;
    requires JTransforms;

    exports de.gsi.math;
    exports de.gsi.math.filter;
    exports de.gsi.math.filter.fir;
    exports de.gsi.math.filter.iir;
    exports de.gsi.math.fitter;
    exports de.gsi.math.functions;
    exports de.gsi.math.matrix;
    exports de.gsi.math.spectra;
    exports de.gsi.math.spectra.dtft;
    exports de.gsi.math.spectra.fft;
    exports de.gsi.math.spectra.lomb;
    exports de.gsi.math.spectra.wavelet;
    exports de.gsi.math.storage;
    exports de.gsi.math.utils;
}