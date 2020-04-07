package de.gsi.math.functions;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import de.gsi.math.utils.UpdateListener;

/**
 * abstract class implementing a global function list and local function change listener interface
 *
 * @author rstein
 */
public abstract class AbstractFunction implements Function {

    private static Vector<Function> fallFunctions = new Vector<>();
    private String funcName = "none";
    private int fnbOfParameter = -1;
    protected double[] fparameter = null;
    protected double[] fparameterMin = null;
    protected double[] fparameterMax = null;
    protected double[] fparameterCopy = null;
    protected boolean[] fparameterFixed = null;
    protected String[] fparameterName = null;
    protected boolean isFitterMode = false;
    private final Vector<UpdateListener> flistener = new Vector<>();
    private final long fcreationTime = System.currentTimeMillis();

    /**
     * @param name function name
     * @param parameter parameter vector
     */
    public AbstractFunction(final String name, final double[] parameter) {
        this(name, parameter, new String[parameter.length]);
    }

    /**
     * @param name function name
     * @param parameters parameter array
     * @param parameterNames parameter name array
     */
    public AbstractFunction(final String name, final double[] parameters, final String[] parameterNames) {
        if (name == null) {
            throw new InvalidParameterException("AbstractFunction(String, double[], double[]) - function name is null");
        }
        if (parameters == null) {
            throw new InvalidParameterException(
                    "AbstractFunction(" + name + ", double[], String[]) - parameter array is null");
        }
        if (parameterNames == null) {
            throw new InvalidParameterException(
                    "AbstractFunction(" + name + ", double[], String[]) - parameter name array is null");
        }
        if (parameterNames.length != parameters.length) {
            throw new InvalidParameterException(
                    "AbstractFunction(" + name + ", double[], String[]) - parameter vs. name array dimension mismatch "
                    + "( " + parameters.length + " vs. " + (fparameterName == null ? 0 : fparameterName.length) + ")");
        }
        fnbOfParameter = parameters.length;
        funcName = name;
        reinitialise();
        fparameter = parameters;
        fparameterName = parameterNames;
        addFunction(this);
    }

    /**
     * @param name function name
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
            if (fallFunctions.indexOf(object) < 0) {
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
            if (flistener.indexOf(object) < 0) {
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
        } else {
            throw new InvalidParameterException("AbstractFunction::fixParameter(" + id + "," + state + "):"
                    + " invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
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

        throw new InvalidParameterException("AbstractFunction::GetParameterName(" + id + "):"
                + " invalid parameter index [0," + (fnbOfParameter - 1) + "}");
    }

    @Override
    public double getParameterRangeMaximum(final int id) {
        if (id < 0 || id >= fnbOfParameter) {
            throw new InvalidParameterException("AbstractFunction::getParameterRangeMaximum(" + id
                    + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
        return fparameterMax[id];
    }

    @Override
    public double getParameterRangeMinimum(final int id) {
        if (id < 0 || id >= fnbOfParameter) {
            throw new InvalidParameterException("AbstractFunction::getParameterRangeMinimum(" + id
                    + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
        return fparameterMin[id];
    }

    @Override
    public double getParameterValue(final int id) {
        if (id >= 0 || id <= fnbOfParameter) {
            return fparameter[id];
        }
        throw new InvalidParameterException("AbstractFunction::getParameterValue(" + id
                + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
    }

    /**
     * a convenience method to return all parameter values
     *
     * @return get array with parameter values
     */
    public double[] getParameterValues() {
        return fparameter;
    }

    /**
     * invoke object within update listener list
     */
    public void invokeListener() {
        synchronized (flistener) {
            final Iterator<UpdateListener> flist = flistener.listIterator();
            while (flist.hasNext()) {
                flist.next().Update(this);
            }
        }
    }

    @Override
    public boolean isFitterMode() {
        return isFitterMode;
    }

    @Override
    public boolean isParameterFixed(final int id) {
        if (id >= 0 || id <= fnbOfParameter) {
            return fparameterFixed[id];
        }
        throw new InvalidParameterException("AbstractFunction::isParameterFixed(" + id + "):"
                + " invalid parameter index [0," + (fnbOfParameter - 1) + "}");
    }

    public void printParameters() {
        printParameters(false);
    }

    public void printParameters(final boolean fullDebug) {
        System.out.printf("AbstractFunction - function name: %s\n", getName());
        for (int i = 0; i < getParameterCount(); i++) {
            if (!fullDebug) {
                System.out.printf("Parameter %2d: %-20s = %f\n", i, getParameterName(i), getParameterValue(i));
            } else {
                System.out.printf("Parameter %2d: %-20s = %f \t [%f, %f]\n", i, getParameterName(i),
                        getParameterValue(i), getParameterRangeMinimum(i), getParameterRangeMaximum(i));
            }
        }
    }

    /**
     * create and update parameter copies and names if necessary
     */
    private void reinitialise() {
        if (fparameter != null && fnbOfParameter == fparameter.length) {
            return;
        }

        if (fparameter != null) {
            fparameter = java.util.Arrays.copyOf(fparameter, fnbOfParameter);
            fparameterMin = java.util.Arrays.copyOf(fparameterMin, fnbOfParameter);
            fparameterMax = java.util.Arrays.copyOf(fparameterMax, fnbOfParameter);
            fparameterCopy = java.util.Arrays.copyOf(fparameterCopy, fnbOfParameter);
            fparameterFixed = java.util.Arrays.copyOf(fparameterFixed, fnbOfParameter);
            final String[] oldParmeterNames = fparameterName;
            fparameterName = new String[fnbOfParameter];
            for (int i = 0; i < fnbOfParameter; i++) {
                if (i < oldParmeterNames.length) {
                    fparameterName[i] = oldParmeterNames[i];
                } else {
                    fparameterName[i] = "arg" + i;
                }
            }
        } else {
            fparameter = new double[fnbOfParameter];
            fparameterMin = new double[fnbOfParameter];
            fparameterMax = new double[fnbOfParameter];
            fparameterCopy = new double[fnbOfParameter];
            fparameterName = new String[fnbOfParameter];
            fparameterFixed = new boolean[fnbOfParameter];
            for (int i = 0; i < fnbOfParameter; i++) {
                fparameterName[i] = "arg" + i;
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
            if (fallFunctions.indexOf(object) >= 0) {
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
            if (flistener.indexOf(object) >= 0) {
                flistener.remove(object);
            }
        }
    }

    @Override
    public void setFitterMode(final boolean state) {
        if (isFitterMode == state) {
            throw new InvalidParameterException(
                    "AbstractFunction::setFitterMode(" + state + ") - funciton is already in this mode");
        }
        isFitterMode = state;

        // fitter may need to reset parameter values in the line of fitting
        // following lines ensure that preset parameter values are preserved
        if (isFitterMode) {
            fparameterCopy = java.util.Arrays.copyOf(fparameter, fparameter.length);
        } else {
            fparameter = java.util.Arrays.copyOf(fparameterCopy, fparameterCopy.length);
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
        } else {
            throw new InvalidParameterException("AbstractFunction::setParameterName(" + id + "," + paramName
                    + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
    }

    @Override
    public void setParameterRange(final int id, final double minRange, final double maxRange) {
        if (id < 0 || id >= fnbOfParameter) {
            throw new InvalidParameterException("AbstractFunction::setParameterRangeMaximum(" + id + "," + minRange
                    + "," + maxRange + "):" + " invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
        fparameterMin[id] = minRange;
        fparameterMax[id] = maxRange;
    }

    @Override
    public void setParameterValue(final int id, final double value) {
        if (id >= 0 || id <= fnbOfParameter) {
            fparameter[id] = value;
        } else {
            throw new InvalidParameterException("AbstractFunction::setParameterValue(" + id + "," + value
                    + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
    }

    @Override
    public void setParameterValues(final double[] value) {
        if (value.length <= fnbOfParameter) {
            for (int i = 0; i < Math.min(value.length, fnbOfParameter); i++) {
                fparameter[i] = value[i];
            }
        } else {
            throw new InvalidParameterException("AbstractFunction::setParameterValue(" + value.length + ","
                    + Arrays.toString(value) + "): invalid parameter index [0," + (fnbOfParameter - 1) + "}");
        }
    }
}
