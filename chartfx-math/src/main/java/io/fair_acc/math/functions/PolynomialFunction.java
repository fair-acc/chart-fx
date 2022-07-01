package io.fair_acc.math.functions;

public class PolynomialFunction extends AbstractFunction1D implements Function1D {

    public PolynomialFunction(final String name, final double[] parameter) {
        super(name, parameter);
        setParameterNames();
        setErrorEstimateComputation(true);
    }

    public PolynomialFunction(final String name, final double[] parameter, final String[] parameterNames) {
        super(name, parameter, parameterNames);
        setErrorEstimateComputation(true);
    }

    public PolynomialFunction(final String name, final int order) {
        super(name, order);
        setParameterNames();
        setErrorEstimateComputation(true);
    }

    @Override
    public double getValue(final double x) {
        double val = 0;
        for (int i = 0; i < getParameterCount(); i++) {
            val += getParameterValue(i) * Math.pow(x, i);
        }
        return val;
    }

    @Override
    public void setParameterCount(final int count) {
        super.setParameterCount(count);
        setParameterNames();
    }

    private void setParameterNames() {
        for (int i = 0; i < getParameterCount(); i++) {
            setParameterName(i, "p" + i);
        }
    }

    public static void main(final String[] args) {
        final PolynomialFunction func = new PolynomialFunction("poly1", new double[] { 0.5, 1, 2, 0.6 });

        for (int i = -3; i <= 3; i++) {
            final double x = i;
            System.out.printf("%+2d: poly(%+4.2f) = %f\n", i, x, func.getValue(x));
        }

        func.printParameters(true);
    }

}
