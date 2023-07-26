package io.fair_acc.sample.chart.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import io.fair_acc.dataset.events.ChartBits;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.spi.AbstractDataSet;
import io.fair_acc.dataset.utils.ByteArrayCache;
import io.fair_acc.dataset.utils.DoubleCircularBuffer;
import io.fair_acc.math.ArrayUtils;
import io.fair_acc.math.spectra.Apodization;
import io.fair_acc.math.spectra.SpectrumTools;

import io.fair_acc.dataset.spi.fastutil.FloatArrayList;

/**
 * DataSet source for testing real-time continuous 2D and 3D type data.
 *
 * @author rstein
 */
public class TestDataSetSource extends AbstractDataSet<TestDataSetSource> implements GridDataSet {
    private static final long serialVersionUID = 5374805363297317245L;
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataSetSource.class);
    private static final String DATA_SOURCE_FILE = "../testdata/alla-turca.mid";
    private static final int AUDIO_SAMPLING_RATE = 11000;
    private static final int N_SYNTHESISER_BITS = 16;
    private static final int INITIAL_FRAME_SIZE = 1024;
    private static final int INITIAL_FRAME_COUNT = 1000;
    private static final int CIRCULAR_BUFFER_SIZE = INITIAL_FRAME_SIZE * 16;

    protected transient MidiWaveformSynthesizer synth = new MidiWaveformSynthesizer(DATA_SOURCE_FILE,
            CIRCULAR_BUFFER_SIZE);

    protected transient TargetDataLine line; // the line from which audio data is captured
    protected transient DoubleCircularBuffer lineBuffer = new DoubleCircularBuffer(CIRCULAR_BUFFER_SIZE);
    protected FloatArrayList[] history = {
        //
        new FloatArrayList(INITIAL_FRAME_SIZE), // xValues
        new FloatArrayList(INITIAL_FRAME_COUNT), // yValues
        new FloatArrayList(INITIAL_FRAME_SIZE *INITIAL_FRAME_COUNT) // zValues
    };
    protected transient FloatArrayList frame = new FloatArrayList(INITIAL_FRAME_SIZE);
    protected int circIndex = 0; // circular buffer index
    protected int samplingRate = AUDIO_SAMPLING_RATE;
    protected int frameSize = INITIAL_FRAME_SIZE;
    protected int frameCount = INITIAL_FRAME_COUNT;
    protected int updatePeriod = 40;

    private DataInput inputSource = DataInput.BOTH;
    protected volatile boolean running;
    protected volatile boolean paused;

    protected transient Timer updateTimer;
    protected transient Timer audioTimer;
    protected transient TimerTask traskAudioIO;
    protected transient TimerTask taskDataUpdate;

    public TestDataSetSource() {
        // ToDo: add Enum to select
        // * RAW, FFT, MAG data
        // * initial sampling, binning, history depth
        super(TestDataSetSource.class.getSimpleName(), 3);

        reinitializeData(); // NOPMD

        fillTestData(); // NOPMD

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(TestDataSetSource.class.getSimpleName()).log("initialised '{}'");
        }
    }

    protected void openLineIn() {
        final AudioFormat format = new AudioFormat(samplingRate, N_SYNTHESISER_BITS, 1, true, true);
        final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // checks if system supports the data line
        if (!AudioSystem.isLineSupported(info)) {
            LOGGER.atError().addArgument(info).addArgument(format).log("Line not supported '{}' format was '{}'");
            throw new IllegalArgumentException("Line not supported");
        }

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().log("opened audio line-in, format = " + format);
            }
        } catch (final LineUnavailableException e) {
            LOGGER.atError().setCause(e).addArgument(DATA_SOURCE_FILE).log("'{}' does not seem to be recognised as a Midi file");
        }
    }

    public void fillTestData() {
        lock().writeLockGuard(
                () -> synth.decode(history[2].elements(), frameSize, updatePeriod, samplingRate, N_SYNTHESISER_BITS));
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public double get(final int dimIndex, final int index) {
        if (dimIndex <= DIM_Y) {
            return history[dimIndex].getFloat(dimIndex == DIM_X ? index % frameSize : index / frameSize);
        }
        return history[dimIndex].getFloat((index + circIndex) % (frameSize * frameCount));
    }

    @Override
    public int getDataCount() {
        return frameCount * frameSize;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public DataInput getInputSource() {
        return inputSource;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public int getUpdatePeriod() {
        return updatePeriod;
    }

    @Override
    public double getValue(final int dimIndex, final double... x) {
        return 0;
    }

    public boolean isOutputMuted() {
        return synth.isOutputMuted();
    }

    /**
     * pauses play back of the data source via the sound card
     */
    public void pause() {
        paused = true;
        line.stop();
        synth.pause();
    }

    public void reset() {
        synth.reset();
        fireInvalidated(ChartBits.DataSetData);
    }

    public void setFrameCount(int frameCount) {
        if (this.frameCount == frameCount || frameCount < 2) {
            return;
        }
        this.frameCount = frameCount;

        reinitializeData();
    }

    public void setFrameSize(int frameSize) {
        if (this.frameSize == frameSize || frameSize < 4) {
            return;
        }
        this.frameSize = frameSize;

        reinitializeData();
    }

    public void setInputSource(DataInput inputSource) {
        this.inputSource = inputSource;
    }

    public void setOutputMuted(boolean state) {
        synth.setOutputMuted(state);
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    public void setUpdatePeriod(final int updatePeriod) {
        if (this.updatePeriod == updatePeriod || updatePeriod <= 0) {
            return;
        }
        this.updatePeriod = updatePeriod;

        reinitializeData();
    }

    /**
     * starts play back of the data source via the sound card
     */
    public void start() {
        paused = false;
        synth.start();

        if (audioTimer != null) {
            audioTimer.cancel();
            traskAudioIO.cancel();
            audioTimer = null;
        }

        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        audioTimer = new Timer(TestDataSetSource.class.getSimpleName() + "-Audio", true);
        traskAudioIO = getAudioTimerTask();
        audioTimer.schedule(traskAudioIO, 0);

        updateTimer = new Timer(TestDataSetSource.class.getSimpleName() + "-Data", true);
        taskDataUpdate = getDataUpdateTask();
        updateTimer.scheduleAtFixedRate(taskDataUpdate, updatePeriod, updatePeriod);
    }

    public void step() {
        TimerTask step = getDataUpdateTask();
        if (step == null) {
            step = getDataUpdateTask();
        }
        step.run();
    }

    /**
     * stops and resets play back of the data source via the sound card
     */
    public void stop() {
        if (audioTimer != null) {
            audioTimer.cancel();
        }
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        audioTimer = null;
        updateTimer = null;
        running = false;
        paused = false;
        synth.stop();
    }

    protected TimerTask getAudioTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (line == null || !line.isOpen()) {
                    openLineIn();
                }
                line.start(); // start capturing
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.atInfo().log("started audio line-in");
                }
                running = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().log("Start recording...");
                }

                final int nAudioSamples = AUDIO_SAMPLING_RATE / 10;
                final byte[] buffer = ByteArrayCache.getInstance().getArrayExact(2 * nAudioSamples);
                try (final AudioInputStream ais = new AudioInputStream(line)) {
                    int ret;
                    while ((ret = ais.read(buffer)) != 0 && running) {
                        // update synthesiser
                        for (int i = 0; i < nAudioSamples; i++) {
                            synth.update(samplingRate, N_SYNTHESISER_BITS);

                            final int value = (buffer[2 * i] << 8) | (buffer[(2 * i) + 1] & 0xff);
                            lineBuffer.put(value);
                        }
                    } /* while '((ret = ais.read(buffer)) != 0) && running' [..] */

                    line.stop(); // stop capturing
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.atInfo().log("closed audio line-in");
                    }
                } catch (final IOException e) {
                    LOGGER.atError().setCause(e).log("issue in audio IO loop");
                }
                ByteArrayCache.getInstance().add(buffer);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().log("stop recording...");
                }
            }
        };
    }

    protected TimerTask getDataUpdateTask() {
        final float[] waveform = new float[2 * frameSize];
        final FloatFFT_1D fft = new FloatFFT_1D(waveform.length);
        final float[] apodisation = new float[2 * frameSize];
        for (int i = 0; i < apodisation.length; i++) {
            apodisation[i] = (float) Apodization.Hann.getIndex(i, 2 * frameSize);
        }

        return new TimerTask() {
            @Override
            public void run() {
                switch (getInputSource()) {
                case MIDI:
                    for (int i = 0; i < waveform.length; i++) {
                        waveform[i] = apodisation[i] * (float) synth.getBuffer().get(i);
                    }
                    break;
                case LINE:
                    for (int i = 0; i < waveform.length; i++) {
                        waveform[i] = apodisation[i] * (float) lineBuffer.get(i);
                    }
                    break;
                case BOTH:
                default:
                    for (int i = 0; i < waveform.length; i++) {
                        waveform[i] = apodisation[i] * (float) (lineBuffer.get(i) + synth.getBuffer().get(i));
                    }
                }

                fft.realForward(waveform);
                final float[] mag = SpectrumTools.computeMagnitudeSpectrum_dB(waveform, true);

                lock().writeLockGuard(() -> {
                    System.arraycopy(mag, 0, frame.elements(), 0, frameSize);
                    System.arraycopy(mag, 0, history[DIM_Z].elements(), circIndex, frameSize);
                    circIndex = (circIndex + frameSize) % (frameSize * frameCount);
                });

                fireInvalidated(ChartBits.DataSetData);
            }
        };
    }

    protected void reinitializeData() {
        frame.size(frameSize);
        history[DIM_X].size(frameSize);
        history[DIM_Y].size(frameCount);
        history[DIM_Z].size(frameSize * frameCount);
        circIndex = 100 * frameSize;

        for (int i = 0; i < frameSize; i++) {
            history[DIM_X].elements()[i] = ((0.5f * i) / frameSize) * samplingRate;
        }
        for (int i = 0; i < frameCount; i++) {
            history[DIM_Y].elements()[i] = -0.001f * updatePeriod * (frameCount - 1 - i);
        }

        Arrays.fill(frame.elements(), 0.0f);
        ArrayUtils.fillArray(history[DIM_Z].elements(), 0.0f);
        synth.setBufferLength(2 * frameSize);
        lineBuffer = new DoubleCircularBuffer(2 * frameSize);
        fireInvalidated(ChartBits.DataSetData);

        if (taskDataUpdate != null) {
            start();
        }
    }

    public static void main(final String[] args) throws InterruptedException {
        final TestDataSetSource dataSource = new TestDataSetSource();

        dataSource.start();
        Thread.sleep(5000);
        dataSource.stop();
        Thread.sleep(5000);
        dataSource.start();
    }

    public enum DataInput {
        BOTH,
        MIDI,
        LINE
    }

    @Override
    public int[] getShape() {
        return new int[] { frameSize, frameCount };
    }

    @Override
    public double getGrid(int dimIndex, int index) {
        return history[dimIndex].getFloat(index);
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
    public double get(int dimIndex, int... indices) {
        switch (dimIndex) {
        case DIM_X:
        case DIM_Y:
            return history[dimIndex].getFloat(indices[dimIndex]);
        case DIM_Z:
            return history[DIM_Z].getFloat(((indices[DIM_X] + frameSize * indices[DIM_Y]) + circIndex) % (frameSize * frameCount));
        default:
            throw new IndexOutOfBoundsException("dimIndex out of bound 3");
        }
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
    }
}
