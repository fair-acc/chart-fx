package io.fair_acc.math.functions;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.math.utils.UpdateListener;

/**
 * abstract class implementing a global function list and local function change listener interface
 *
 * @author rstein
 */
@SuppressWarnings("PMD.ShortVariable")
public abstract class AbstractFunction implements Function {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFunction.class);
    private static final List<Function> fallFunctions = new ArrayList<>();
    private String funcName = "none";
    private int fnbOfParameter = -1;
    protected double[] fparameter;
    protected double[] fparameterMin;
    protected double[] fparameterMax;
    protected double[] fparameterCopy;
    protected boolean[] fparameterFixed;
    protected String[] fparameterName;
    protected boolean fitterMode;
    private final List<UpdateListener> flistener = new ArrayList<>();
    private final long fcreationTime = System.currentTimeMillis();

    /**
     * @param name      function name
     * @param parameter parameter vector
     */
    public AbstractFunction(final String name, final double... parameter) {
        this(name, parameter, new String[parameter.length]);
    }

    /**
     * @param name           function name
     * @param parameters     parameter array
     * @param parameterNames parameter name array
     */
    public AbstractFunction(final String name, final double[] parameters, final String... parameterNames) {
        if (name == null) {
            throw new InvalidParameterException("AbstractFunction(String, double[], double[]) - function name is null");
        }
        if (parameters == null) {
            throw new InvalidParameterException("AbstractFunction(" + name + ", double[], String[]) - parameter array is null");
        }
        if (parameterNames.length != parameters.length) {
            throw new InvalidParameterException("AbstractFunction(" + name + ", double[], String[]) - parameter vs. name array dimension mismatch "
                                                + "( " + parameters.length + " vs. "
                                                + (fparameterName == null ? 0 : fparameterName.length) + ")");
        }
        fnbOfParameter = parameters.length;
        funcName = name;
        reinitialise();
        fparameter = Arrays.copyOf(parameters, parameters.length);
        fparameterName = Arrays.copyOf(parameterNames, parameterNames.length);
        addFunction(this);
    }

    /**
     * @param name  function name
     * @param nparm number of free parameter
     */
    public AbstractFunction(final String name, final int nparm) {
        this(name, new double[nparm], new String[nparm]);
    }

    /**
     * add object to global function list
     *
     * @param object function to be added
     */
    public void addFunction(final Function object) {
        synchronized (fallFunctions) {
            if (!fallFunctions.contains(object)) {
                fallFunctions.add(object);
            }
        }
    }

    /**
     * add object to update listener list
     *
     * @param object update listener to be added
     */
    public void addListener(final UpdateListener object) {
        synchronized (flistener) {
            if (!flistener.contains(object)) {
                flistener.add(object);
            }
        }
    }

    @Override
    public void clearParameterValues() {
        for (int i = 0; i < fparameter.length; i++) {
            if (!isParameterFixed(i)) {
                fparameter[i] = 0.0;
            }
        }
    }

    @Override
    public void fixParameter(final int id, final boolean state) {
        if (id >= 0 || id <= fnbOfParameter) {
            fparameterFixed[id] = state;
            return;
        }
        throw new InvalidParameterException(exceptionForIdString("fixParameter", id));
    }

    @Override
    public int getFreeParameterCount() {
        int nDim = 0;
        for (int i = 0; i < fnbOfParameter; i++) {
            if (!isParameterFixed(i)) {
                nDim++;
            }
        }
        return nDim;
    }

    /**
     * @return all registered functions
     */
    public Function[] getFunctions() {
        synchronized (fallFunctions) {
            if (!fallFunctions.isEmpty()) {
                return fallFunctions.toArray(new Function[0]);
            }
        }
        return new Function[0];
    }

    @Override
    public String getID() {
        return getName() + fcreationTime;
    }

    @Override
    public String getName() {
        return funcName;
    }

    @Override
    public int getParameterCount() {
        return fnbOfParameter;
    }

    @Override
    public String getParameterName(final int id) {
        if (id >= 0 || id <= fnbOfParameter) {
            return fparameterName[id];
        }
        throw new InvalidParameterException(exceptionForIdString("getParameterName", id));
    }

    @Override
    public double getParameterRangeMaximum(final int id) {
        if (id < 0 || id >= fnbOfParameter) {
            throw new InvalidParameterException(exceptionForIdString("getParameterRangeMaximum", id));
        }
        return fparameterMax[id];
    }

    private String exceptionForIdString(final String functionName, final int id, final String... parameter) {
        return functionName + "(" + id + (parameter.length > 0 ? ", " + parameter[0] : "") + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}";
    }

    @Override
    public double getParameterRangeMinimum(final int id) {
        if (id < 0 || id >= fnbOfParameter) {
            throw new InvalidParameterException(exceptionForIdString("getParameterRangeMinimum", id));
        }
        return fparameterMin[id];
    }

    @Override
    public double getParameterValue(final int id) {
        if (id >= 0 || id <= fnbOfParameter) {
            return fparameter[id];
        }
        throw new InvalidParameterException(exceptionForIdString("getParameterValue", id));
    }

    /**
     * a convenience method to return all parameter values
     *
     * @return get array with parameter values
     */
    public double[] getParameterValues() {
        return fparameter; // NOPMD -- direct export on purpose
    }

    /**
     * invoke object within update listener list
     */
    public void invokeListener() {
        synchronized (flistener) {
            for (final UpdateListener updateListener : flistener) {
                updateListener.Update(this);
            }
        }
    }

    @Override
    public boolean isFitterMode() {
        return fitterMode;
    }

    @Override
    public boolean isParameterFixed(final int id) {
        if (id >= 0 || id <= fnbOfParameter) {
            return fparameterFixed[id];
        }
        throw new InvalidParameterException(exceptionForIdString("isParameterFixed", id));
    }

    public void printParameters() {
        printParameters(false);
    }

    public void printParameters(final boolean fullDebug) {
        LOGGER.atInfo().log(String.format("AbstractFunction - function name: %s", getName()));
        for (int i = 0; i < getParameterCount(); i++) {
            if (fullDebug) {
                LOGGER.atInfo().log(String.format("Parameter %2d: %-20s = %f \t [%f, %f]", i, getParameterName(i), getParameterValue(i), getParameterRangeMinimum(i), getParameterRangeMaximum(i)));
            } else {
                LOGGER.atInfo().log(String.format("Parameter %2d: %-20s = %f", i, getParameterName(i), getParameterValue(i)));
            }
        }
    }

    /**
     * remove object to global function list
     *
     * @param object function to be removed
     */
    public void removeFunction(final Function object) {
        synchronized (fallFunctions) {
            if (fallFunctions.contains(object)) {
                fallFunctions.remove(object);
            }
        }
    }

    /**
     * remove object to update listener list
     *
     * @param object update listener to be removed
     */
    public void removeListener(final UpdateListener object) {
        synchronized (flistener) {
            if (flistener.contains(object)) {
                flistener.remove(object);
            }
        }
    }

    @Override
    public void setFitterMode(final boolean state) {
        if (fitterMode == state) {
            throw new InvalidParameterException("setFitterMode(" + state + ") - funciton is already in this mode");
        }
        fitterMode = state;

        // fitter may need to reset parameter values in the line of fitting
        // following lines ensure that preset parameter values are preserved
        if (fitterMode) {
            fparameterCopy = Arrays.copyOf(fparameter, fparameter.length);
        } else {
            fparameter = Arrays.copyOf(fparameterCopy, fparameterCopy.length);
        }
    }

    @Override
    public void setParameterCount(final int count) {
        if (fnbOfParameter == count || count < 0) {
            return;
        }
        fnbOfParameter = count;
        reinitialise();
    }

    @Override
    public void setParameterName(final int id, final String paramName) {
        if (id >= 0 || id <= fnbOfParameter) {
            fparameterName[id] = paramName;
            return;
        }
        throw new InvalidParameterException(exceptionForIdString("setParameterName", id, paramName));
    }

    @Override
    public void setParameterRange(final int id, final double minRange, final double maxRange) {
        if (id < 0 || id >= fnbOfParameter) {
            throw new InvalidParameterException(
                    "setParameterRangeMaximum(" + id + "," + minRange + "," + maxRange + "):"
                    + " invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
        fparameterMin[id] = minRange;
        fparameterMax[id] = maxRange;
    }

    @Override
    public void setParameterValue(final int id, final double value) {
        if (id >= 0 || id <= fnbOfParameter) {
            fparameter[id] = value;
            return;
        }
        throw new InvalidParameterException(exceptionForIdString("setParameterValue", id, Double.toString(value)));
    }

    @Override
    public void setParameterValues(final double[] value) {
        if (value.length <= fnbOfParameter) {
            System.arraycopy(value, 0, fparameter, 0, value.length);
            return;
        }
        throw new InvalidParameterException("setParameterValue(" + value.length + "," + Arrays.toString(value) + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
    }

    /**
     * create and update parameter copies and names if necessary
     */
    private void reinitialise() {
        if (fparameter != null && fnbOfParameter == fparameter.length) {
            return;
        }

        if (fparameter == null) {
            fparameter = new double[fnbOfParameter];
            fparameterMin = new double[fnbOfParameter];
            fparameterMax = new double[fnbOfParameter];
            fparameterCopy = new double[fnbOfParameter];
            fparameterName = new String[fnbOfParameter];
            fparameterFixed = new boolean[fnbOfParameter];
            for (int i = 0; i < fnbOfParameter; i++) {
                fparameterName[i] = "arg" + i;
            }
            return;
        }
        fparameter = Arrays.copyOf(fparameter, fnbOfParameter);
        fparameterMin = Arrays.copyOf(fparameterMin, fnbOfParameter);
        fparameterMax = Arrays.copyOf(fparameterMax, fnbOfParameter);
        fparameterCopy = Arrays.copyOf(fparameterCopy, fnbOfParameter);
        fparameterFixed = Arrays.copyOf(fparameterFixed, fnbOfParameter);
        final String[] oldParmeterNames = fparameterName;
        fparameterName = new String[fnbOfParameter];
        for (int i = 0; i < fnbOfParameter; i++) {
            if (i < oldParmeterNames.length) {
                fparameterName[i] = oldParmeterNames[i];
            } else {
                fparameterName[i] = "arg" + i;
            }
        }
    }
}
