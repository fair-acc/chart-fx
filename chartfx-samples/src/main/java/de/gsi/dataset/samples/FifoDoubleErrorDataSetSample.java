package de.gsi.dataset.samples;

import java.util.Timer;
import java.util.TimerTask;

import de.gsi.dataset.spi.CircularDoubleErrorDataSet;
import de.gsi.dataset.spi.FifoDoubleErrorDataSet;
import de.gsi.dataset.utils.ProcessingProfiler;

public class FifoDoubleErrorDataSetSample {
	/**
	 * meant for testing/illustrating usage
	 *
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {
		final int bufferLength = 10;
		final int fillBufferLength = 35;
		final FifoDoubleErrorDataSet buffer1 = new FifoDoubleErrorDataSet("test", bufferLength, 10);
		final CircularDoubleErrorDataSet buffer2 = new CircularDoubleErrorDataSet("test", bufferLength);

		for (int i = 0; i < fillBufferLength; i++) {

			final double delta = i >= 13 ? 5 : 0;
			final double t = i + delta;
			if (i == 13) {
				System.err.println("jump in time by +" + delta);
			}
			buffer1.add(t, i, 0, 0);
			buffer2.add(t, i, 0, 0);
			final int max1 = Math.max(0, buffer1.getDataCount() - 1);
			final int max2 = Math.max(0, buffer2.getDataCount() - 1);
			final String msg = String.format("%2d - [ %2d vs. %2d , %2d vs. %2d] - length = %2d vs %2d", i,
					(int) buffer1.getX(0), (int) buffer2.getX(0), (int) buffer1.getX(max1), (int) buffer2.getX(max2),
					max1, max2);
			System.err.println(msg);
		}

		// some simple performance tests
		ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(true);

		final long start = ProcessingProfiler.getTimeStamp();
		final FifoDoubleErrorDataSet buffer3 = new FifoDoubleErrorDataSet("test", 1000, 100);
		for (int i = 0; i < 10000; i++) {
			buffer3.add(i, 1.0, 0.0, 0.0);
		}
		ProcessingProfiler.getTimeDiff(start, "init and write 10k times");

		final Timer timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				System.gc();
			}
		}, 5, 2000);
		while (true) {
			buffer3.add(System.currentTimeMillis() * 1e-3, 1.0, 0.0, 0.0);
		}
	}

}
