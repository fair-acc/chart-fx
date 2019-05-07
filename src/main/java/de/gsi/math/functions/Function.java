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
     * the number of free parameter
     * 
     * @param count
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
     * @param id
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
     * @param id
     * @return the value of a by 'id' given parameter
     */
    public double getParameterValue(int id);

    /**
     * sets the value of a by 'id' given parameter
     * 
     * @param id the parameter id
     * @param value
     */
    public void setParameterValue(int id, double value);

    /**
     * sets the parameter values using an arrary the array is required to have at most getParameterCount() indices
     * 
     * @param value
     */
    void setParameterValues(double[] value);

    /**
     * resets all parameter values to zero
     */
    public void clearParameterValues();

    /**
     * @param id
     * @return the minimum value of a by 'id' given parameter range N.B. depending on the fitter, the range may be used
     *         only as a hint
     */
    public double getParameterRangeMinimum(int id);

    /**
     * @param id
     * @return the maximum value of a by 'id' given parameter range N.B. depending on the fitter, the range may be used
     *         only as a hint
     */
    public double getParameterRangeMaximum(int id);

    /**
     * sets the range of a by 'id' given parameter N.B. depending on the fitter, the range may be used only as a hint
     * 
     * @param id the parameter id
     * @param minRange
     * @param maxRange
     */
    public void setParameterRange(int id, double minRange, double maxRange);

    /**
     * @param id
     * @return the name of a by 'id' given parameter
     */
    public String getParameterName(int id);

    /**
     * sets the name of a by 'id' given parameter
     * 
     * @param id the parameter id
     * @param value
     */
    public void setParameterName(int id, String paramName);

    /**
     * sets whether function is in use by fitting routine <expert function>
     * 
     * @param state true: function is used within a fitting routine
     */
    public void setFitterMode(boolean state);

    /**
     * returns whether function is in use by fitting routine <expert function>
     * 
     * @return true: function is used within a fitting routine
     */
    public boolean isFitterMode();

}
