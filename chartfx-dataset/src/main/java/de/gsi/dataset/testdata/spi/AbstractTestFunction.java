package de.gsi.dataset.testdata.spi;

import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.AbstractErrorDataSet;
import de.gsi.dataset.testdata.TestDataSet;

/**
 * abstract error data set for graphical testing purposes
 *
 * @author rstein
 * @param <D> generics for fluent design
 */
public abstract class AbstractTestFunction<D extends AbstractTestFunction<D>> extends AbstractErrorDataSet<D>
		implements TestDataSet<D> {
	private static final long serialVersionUID = 3145097895719258628L;
	private double[] data;

	/**
	 * @param name  data set name
	 * @param count number of samples
	 */
	public AbstractTestFunction(final String name, final int count) {
		super(name);
		// this part needs to be adjusted to you internal applications data
		// transfer/management likings
		data = generateY(count);
	}

	@Override
	public D update() {
		lock().writeLockGuard(() -> {
			data = generateY(data.length);
			recomputeLimits(0);
			recomputeLimits(1);
		});
		return fireInvalidated(new UpdatedDataEvent(this));
	}

	@Override
	public int getDataCount() {
		return data.length;
	}

	@Override
	public double getX(final int index) {
		// returns the i-th index as horizontal X axis value
		return index;
	}

	@Override
	public double getY(final int index) {
		// include for example dimension sanity checks
		if (index < 0 || index >= getDataCount()) {
			return Double.NaN;
		}

		return data[index];
	}

	@Override
	public double getXErrorNegative(final int index) {
		return AbstractTestFunction.getXError();
	}

	@Override
	public double getXErrorPositive(final int index) {
		return AbstractTestFunction.getXError();
	}

	@Override
	public double getYErrorNegative(final int index) {
		return AbstractTestFunction.getYError();
	}

	@Override
	public double getYErrorPositive(final int index) {
		return AbstractTestFunction.getYError();
	}

	private static double getXError() {
		return 0.1;
	}

	private static double getYError() {
		return 0.1;
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.XY_ASYMMETRIC;
	}

	@Override
	public String getStyle(final int index) { // NOPMD by steinhagen on 08/06/19 10:12
		return null;
	}

	@Override
	public double[] generateX(final int count) {
		final double[] retVal = new double[count];
		for (int i = 0; i < count; i++) {
			retVal[i] = i;
		}
		return retVal;
	}

}
