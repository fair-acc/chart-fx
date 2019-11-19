package de.gsi.math.functions;

public class HeavisideStepFunction extends AbstractFunction1D implements Function1D {

    /**
     * 
     * initialise Heaviside step function
     * 
     * parameter order: parameter[0] = location (default: 0.0) parameter[1] = scaling (default: 1.0 (fixed))
     * 
     * @param name function name
     */
    public HeavisideStepFunction(String name) {
        this(name, null);
    }

    /**
     * 
     * initialise Heaviside step function
     * 
     * parameter order: parameter[0] = location (default: 0.0) parameter[1] = scaling (default: 1.0 (fixed))
     * 
     * @param name function name
     * @param parameter function parameter
     */
    public HeavisideStepFunction(String name, double[] parameter) {
        super(name, new double[3]);
        this.setParameterName(0, "location");
        this.setParameterValue(0, 0.0);
        this.setParameterName(1, "scaling");
        this.setParameterValue(1, 1.0);
        this.fixParameter(1, true);

        if (parameter == null) {
            return;
        }

        for (int i = 0; i < Math.min(parameter.length, 3); i++) {
            this.setParameterValue(i, parameter[i]);
        }
    }

    @Override
    public double getValue(double x) {
        if (x - fparameter[0] < 0.0)
            return 0.0;
        else
            return fparameter[1];
    }
}
