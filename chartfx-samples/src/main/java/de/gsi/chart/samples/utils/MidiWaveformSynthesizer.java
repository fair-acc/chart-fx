package de.gsi.chart.samples.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.utils.DoubleCircularBuffer;
import de.gsi.math.spectra.Apodization;
import de.gsi.math.spectra.SpectrumTools;
import de.gsi.math.spectra.fft.FloatFFT_1D;

import net.jafama.FastMath;

/**
 * @author rstein
 */
public class MidiWaveformSynthesizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiWaveformSynthesizer.class);
    private static final String[] KEY_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
    private static final int LOCAL_NOTE_ON = 1;
    private static final int LOCAL_NOTE_OFF = 2;
    private static final int N_NOTES = 128; // given by the MIDI 1.0 standard

    protected transient Sequencer sequencer;
    protected transient Sequence sequence;
    protected transient Synthesizer synthesizer;
    protected transient MidiChannel synthesizerChannel;

    // internal sound bank state
    private final float[] noteAmplitude = new float[N_NOTES];
    private final float[] noteFrequency = new float[N_NOTES];
    private boolean muteOutput = true;
    private float noteAmplitudeDecay = 0.1f;
    private final Object lockObject = new Object();
    private DoubleCircularBuffer buffer;
    private int counter = 0;

    public MidiWaveformSynthesizer(final String midiFile, final int bufferSize) {
        super();

        buffer = new DoubleCircularBuffer(bufferSize);

        for (int i = 0; i < N_NOTES; i++) {
            noteFrequency[i] = (float) (2.0f * Math.PI * (440.0f + ((440.0f / 12.0f) * (i - 69))));
        }

        try {
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();

            final InputStream is = new BufferedInputStream(TestDataSetSource.class.getResourceAsStream(midiFile));
            sequence = MidiSystem.getSequence(is);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

            final Track[] tracks = sequence.getTracks();
            final Track trk = sequence.createTrack();
            for (final Track track : tracks) {
                addNotesToTrack(track, trk);
            }
            sequencer.setSequence(sequence);

            // setup optional synthesizer (for audio output)
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            final MidiChannel[] channels = synthesizer.getChannels();
            for (final MidiChannel channel : channels) {
                if (channel != null) {
                    synthesizerChannel = channel;
                    break;
                }
            }

            sequencer.addMetaEventListener(evt -> {
                final int command = evt.getType();
                final byte[] data = evt.getData();
                if ((data.length < 2) || ((command != LOCAL_NOTE_ON) && (command != LOCAL_NOTE_OFF))) {
                    return;
                }
                final int note = evt.getData()[1] & 0xFF;
                final int velocity = evt.getData()[2] & 0xFF;

                if (command == LOCAL_NOTE_ON /* ShortMessage.NOTE_ON */) {
                    noteAmplitude[note] = velocity;
                    if (!muteOutput) {
                        synthesizerChannel.noteOn(note, velocity);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.atDebug().addArgument(evt).addArgument(note).addArgument(velocity).log("note on event = {}  note = {}  velocity {}");
                    }
                } else if (command == LOCAL_NOTE_OFF /* ShortMessage.NOTE_OFF */) {
                    noteAmplitude[note] = 0.0f;
                    synthesizerChannel.noteOff(note, 0);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.atDebug().addArgument(evt).addArgument(note).addArgument(velocity).log("note off event = {}  note = {}  velocity {}");
                    }
                } else if (command == ShortMessage.CONTROL_CHANGE && LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(evt).addArgument(note).addArgument(velocity).log("generic CONTROL_CHANGE evt = {} bytes = {} {}");
                }
            });

        } catch (final MidiUnavailableException e) {
            LOGGER.atError().setCause(e).log("could not initialise MidiSystem");
        } catch (final IOException e) {
            LOGGER.atError().setCause(e).addArgument(TestDataSetSource.class.getResourceAsStream(midiFile)).log("could not open file '{}'");
        } catch (final InvalidMidiDataException e) {
            LOGGER.atError().setCause(e).addArgument(midiFile).log("'{}' does not seem to be recognised as a Midi file");
        }
    }

    public void decode(final float[] data, final int frameSize, final int updatePeriod, final int samplingRate,
            final int nBits) {
        final Track track = mergeShortMessageEvent(sequence.getTracks());
        final float length = 1e-6f * sequence.getMicrosecondLength();
        final float ts = 1.0f / samplingRate;
        final float tickLength = length / track.ticks();
        final int frameCount = data.length / frameSize;
        final float scale = 2 << Math.max(1, nBits + 1);

        final FloatFFT_1D fft = new FloatFFT_1D(2 * frameSize);
        final float[] apodization = new float[2 * frameSize];
        for (int i = 0; i < apodization.length; i++) {
            apodization[i] = (float) Apodization.Hann.getIndex(i, apodization.length);
        }

        int frameCounter = 0;
        int tickIndex = 0;
        final float[] waveForm = new float[2 * frameSize];
        final int nUpdateDistance = (int) (updatePeriod / 1000.0 * samplingRate);
        for (int i = 0; frameCounter < frameCount; i++) {
            final float t = i * ts;
            final MidiEvent tickEvt = track.get(tickIndex);
            final float tickTimeStamp = tickEvt.getTick() * tickLength;

            // update waveform by one sample
            update(samplingRate, nBits);

            if ((t > tickTimeStamp) && (tickIndex < track.size() - 1)) {
                if ((tickEvt.getMessage() instanceof ShortMessage)) {
                    final ShortMessage sm = (ShortMessage) tickEvt.getMessage();
                    final int note = sm.getData1() & 0xFF;
                    final int velocity = sm.getData2() & 0xFF;

                    final int command = sm.getCommand();
                    if ((command == ShortMessage.NOTE_ON) || (command == LOCAL_NOTE_ON)) {
                        noteAmplitude[note] = velocity;
                    } else if ((command == ShortMessage.NOTE_OFF) || (command == LOCAL_NOTE_OFF)) {
                        noteAmplitude[note] = 0.0f;
                    }
                }
                tickIndex++;
            }

            if (i > 0 && (i % nUpdateDistance == 0)) {
                for (int j = 0; j < waveForm.length; j++) {
                    final float noise = (1e-3f * System.nanoTime() % 2); // adds some noise
                    waveForm[j] = apodization[j] * getSample(j) + noise / scale;
                }
                decodeFrame(fft, waveForm, data, (frameCounter * frameSize) % data.length);
                frameCounter++;
            }
        }

        // return synthesizer to its original state
        for (int note = 0; note < N_NOTES; note++) {
            noteAmplitude[note] = 0.0f;
            synthesizerChannel.noteOff(note, 0);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void finalize() { // NOPMD needed and on purpose
        sequencer.close();
        synthesizer.close();
    }

    public DoubleCircularBuffer getBuffer() {
        return buffer;
    }

    public float getNoteAmplitudeDecay() {
        return noteAmplitudeDecay;
    }

    public float getSample(final int readPos) {
        return (float) buffer.get(readPos);
    }

    public boolean isOutputMuted() {
        return muteOutput;
    }

    public final Track mergeShortMessageEvent(final Track[] tracks) {
        final Track trk = sequence.createTrack();
        for (final Track track : tracks) {
            for (int i = 0; i < track.size(); i++) {
                final MidiEvent evt = track.get(i);
                final MidiMessage mm = evt.getMessage();
                if (mm instanceof ShortMessage) {
                    trk.add(evt);
                }
            }
        }
        return trk;
    }

    public void pause() {
        sequencer.stop();
    }

    public void reset() {
        synchronized (lockObject) {
            counter = 0;
            for (int note = 0; note < N_NOTES; note++) {
                noteAmplitude[note] = 0.0f;
                synthesizerChannel.noteOff(note, 0);
            }
        }
    }

    public void setBufferLength(final int bufferSize) {
        synchronized (lockObject) {
            buffer = new DoubleCircularBuffer(bufferSize);
            this.reset();
        }
    }

    public void setNoteAmplitudeDecay(float noteAmplitudeDecay) {
        this.noteAmplitudeDecay = noteAmplitudeDecay;
    }

    public void setOutputMuted(final boolean state) {
        muteOutput = state;
        if (muteOutput) {
            reset();
        }
    }

    public void start() {
        reset();
        sequencer.start();
    }

    public void stop() {
        sequencer.stop();
        sequencer.setTickPosition(0);
        reset();
    }

    public void update(final int samplingRate, final int nBits) {
        synchronized (lockObject) {
            double val = 0.0f;
            final double scale = 2 << Math.max(1, nBits - 8);
            final double ts = 1.0 / samplingRate; // [s]
            final double alpha = Math.exp(-ts / noteAmplitudeDecay);
            for (int i = 0; i < N_NOTES; i++) {
                if (noteAmplitude[i] > 0.0f) {
                    val += scale * noteAmplitude[i] * FastMath.sinQuick((noteFrequency[i] * counter) / samplingRate);
                    noteAmplitude[i] *= alpha;
                    if (noteAmplitude[i] < 1) {
                        noteAmplitude[i] = 0.0f;
                    }
                }
            }
            counter++;
            // put into circular buffer
            buffer.put(val);
        }
    }

    public static final void addNotesToTrack(final Track track, final Track trk) throws InvalidMidiDataException {
        for (int ii = 0; ii < track.size(); ii++) {
            final MidiEvent me = track.get(ii);
            final MidiMessage mm = me.getMessage();
            if (mm instanceof ShortMessage) {
                final ShortMessage sm = (ShortMessage) mm;
                final int command = sm.getCommand();
                int com = -1;
                if (command == ShortMessage.NOTE_ON) {
                    com = LOCAL_NOTE_ON;
                } else if (command == ShortMessage.NOTE_OFF) {
                    com = LOCAL_NOTE_OFF;
                }
                if (com > 0) {
                    final byte[] b = sm.getMessage();
                    final int l = (b == null ? 0 : b.length);
                    final MetaMessage metaMessage = new MetaMessage(com, b, l);
                    final MidiEvent me2 = new MidiEvent(metaMessage, me.getTick());
                    trk.add(me2);
                }
            }
        }
    }

    private static void decodeFrame(final FloatFFT_1D fft, final float[] in, final float[] out, final int offset) {
        fft.realForward(in);
        final float[] mag = SpectrumTools.computeMagnitudeSpectrum_dB(in, true);
        System.arraycopy(mag, 0, out, offset, mag.length);
    }

    public static String keyName(final int nKeyNumber) {
        if (nKeyNumber > 127) {
            return "illegal value";
        }
        final int nNote = nKeyNumber % 12;
        final int nOctave = nKeyNumber / 12;
        return KEY_NAMES[nNote] + (nOctave - 1);
    }
}
