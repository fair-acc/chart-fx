package de.gsi.math.functions;

/**
 * generic function interface
 * 
 * @author rstein
 */
public interface Function {

    /**
     * @return function name (e.g. brief description/Latex notation)
     */
    public String getName();

    /**
     * @return unique function ID
     */
    public String getID();

    /**
     * @return the input dimension of the function
     */
    public int getInputDimension();

    /**
     * @return the output dimension of the function
     */
    public int getOutputDimension();

    /**
     * @param count number of free parameter
     */
    public void setParameterCount(int count);

    /**
     * @return the number of parameter
     */
    public int getParameterCount();

    /**
     * @return the number of free parameter
     */
    public int getFreeParameterCount();

    /**
     * returns whether given parameter is fixed (static) or not
     * 
     * @param id parameter id
     * @return true: if parameter is fixed
     */
    boolean isParameterFixed(int id);

    /**
     * sets
     * 
     * @param id the parameter id
     * @param state true: parameter is fixed, false: parameter is free
     */
    void fixParameter(int id, boolean state);

    /**
     * @param id parameter id
     * @return the value of a by 'id' given parameter
     */
    public double getParameterValue(int id);

    /**
     * sets the value of a by 'id' given parameter
     * 
     * @param id the parameter id
     * @param value new value of parameter
     */
    public void setParameterValue(int id, double value);

    /**
     * sets the parameter values using an array the array is required to have at most getParameterCount() indices
     * 
     * @param value new parameter values
     */
    void setParameterValues(double[] value);

    /**
     * resets all parameter values to zero
     */
    public void clearParameterValues();

    /**
     * @param id the parameter id
     * @return the minimum value of a by 'id' given parameter range N.B. depending on the fitter, the range may be used
     *         only as a hint
     */
    public double getParameterRangeMinimum(int id);

    /**
     * @param id the parameter id
     * @return the maximum value of a by 'id' given parameter range N.B. depending on the fitter, the range may be used
     *         only as a hint
     */
    public double getParameterRangeMaximum(int id);

    /**
     * sets the range of a by 'id' given parameter N.B. depending on the fitter, the range may be used only as a hint
     * 
     * @param id the parameter id
     * @param minRange minimum parameter range
     * @param maxRange maximum parameter range
     */
    public void setParameterRange(int id, double minRange, double maxRange);

    /**
     * @param id the parameter id
     * @return the name of a by 'id' given parameter
     */
    public String getParameterName(int id);

    /**
     * sets the name of a by 'id' given parameter
     * 
     * @param id the parameter id
     * @param paramName name for the given pararameter 'id'
     */
    public void setParameterName(int id, String paramName);

    /**
     * sets whether function is in use by fitting routine &lt;expert function&gt;
     * 
     * @param state true: function is used within a fitting routine
     */
    public void setFitterMode(boolean state);

    /**
     * returns whether function is in use by fitting routine &lt;expert function&gt;
     * 
     * @return true: function is used within a fitting routine
     */
    public boolean isFitterMode();

}
