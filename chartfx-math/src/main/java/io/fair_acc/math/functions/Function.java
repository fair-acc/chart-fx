package io.fair_acc.math.functions;

/**
 * generic function interface
 * 
 * @author rstein
 */
public interface Function {
    /**
     * resets all parameter values to zero
     */
    void clearParameterValues();

    /**
     * sets
     * 
     * @param id the parameter id
     * @param state true: parameter is fixed, false: parameter is free
     */
    void fixParameter(int id, boolean state);

    /**
     * @return the number of free parameter
     */
    int getFreeParameterCount();

    /**
     * @return unique function ID
     */
    String getID();

    /**
     * @return the input dimension of the function
     */
    int getInputDimension();

    /**
     * @return function name (e.g. brief description/Latex notation)
     */
    String getName();

    /**
     * @return the output dimension of the function
     */
    int getOutputDimension();

    /**
     * @return the number of parameter
     */
    int getParameterCount();

    /**
     * @param id the parameter id
     * @return the name of a by 'id' given parameter
     */
    String getParameterName(int id);

    /**
     * @param id the parameter id
     * @return the maximum value of a by 'id' given parameter range N.B. depending on the fitter, the range may be used
     *         only as a hint
     */
    double getParameterRangeMaximum(int id);

    /**
     * @param id the parameter id
     * @return the minimum value of a by 'id' given parameter range N.B. depending on the fitter, the range may be used
     *         only as a hint
     */
    double getParameterRangeMinimum(int id);

    /**
     * @param id parameter id
     * @return the value of a by 'id' given parameter
     */
    double getParameterValue(int id);

    /**
     * returns whether function is in use by fitting routine &lt;expert function&gt;
     * 
     * @return true: function is used within a fitting routine
     */
    boolean isFitterMode();

    /**
     * returns whether given parameter is fixed (static) or not
     * 
     * @param id parameter id
     * @return true: if parameter is fixed
     */
    boolean isParameterFixed(int id);

    /**
     * sets whether function is in use by fitting routine &lt;expert function&gt;
     * 
     * @param state true: function is used within a fitting routine
     */
    void setFitterMode(boolean state);

    /**
     * @param count number of free parameter
     */
    void setParameterCount(int count);

    /**
     * sets the name of a by 'id' given parameter
     * 
     * @param id the parameter id
     * @param paramName name for the given pararameter 'id'
     */
    void setParameterName(int id, String paramName);

    /**
     * sets the range of a by 'id' given parameter N.B. depending on the fitter, the range may be used only as a hint
     * 
     * @param id the parameter id
     * @param minRange minimum parameter range
     * @param maxRange maximum parameter range
     */
    void setParameterRange(int id, double minRange, double maxRange);

    /**
     * sets the value of a by 'id' given parameter
     * 
     * @param id the parameter id
     * @param value new value of parameter
     */
    void setParameterValue(int id, double value);

    /**
     * sets the parameter values using an array the array is required to have at most getParameterCount() indices
     * 
     * @param value new parameter values
     */
    void setParameterValues(double[] value);
}
