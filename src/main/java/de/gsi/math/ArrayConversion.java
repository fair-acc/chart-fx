package de.gsi.math;

/**
 * This utility class converts arrays of frequently used java primitives
 * into other the requested primitive type. For the time being only 'Number'
 * primitives are implemented, e.g. double array to float array etc.
 * 
 * NOTE: USE WITH CARE! Keep in mind that these routines are based on creating new
 * arrays and copying the old data. Consider re-implementing the analysis routines 
 * where necessary.
 *  
 * Also, apologies for the largely redundant code: this is due to the lack of java template 
 * mechanism (generics) handling of primitive types. A hoorray to C++ templates!     
 * @author rstein
 *
 */
public class ArrayConversion {
	
    /**
     * returns a type-converted copy of the input vector 
     * @param in a float array
     * @return a double array containing the values of in
     */
	public static double [] getDoubleArray(float in[])  {
		if (in == null) return null;		
		double ret[] = new double[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (double)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a double array
     * @return a double array containing the values of in
     */
	public static double [] getDoubleArray(double in[])  {
		if (in == null) return null;		
		double ret[] = new double[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (double)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a long integer array
     * @return a double array containing the values of in
     */
	public static double [] getDoubleArray(long in[])  {
		if (in == null) return null;		
		double ret[] = new double[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (double)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in an integer array
     * @return a double array containing the values of in
     */
	public static double [] getDoubleArray(int in[])  {
		if (in == null) return null;		
		double ret[] = new double[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (double)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in an short integer array
     * @return a double array containing the values of in
     */
	public static double [] getDoubleArray(short in[])  {
		if (in == null) return null;		
		double ret[] = new double[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (double)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in an byte array
     * @return a double array containing the values of in
     */
	public static double [] getDoubleArray(byte in[])  {
		if (in == null) return null;		
		double ret[] = new double[in.length];

		for (int i=0; i < in.length; i++) {
			ret[i] = (double)in[i];
		}
		
		return ret;
	}
	
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a float array
     * @return a float array containing the values of in
     */
	public static float [] getFloatArray(float in[])  {
		if (in == null) return null;		
		float ret[] = new float[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (float)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a double array
     * @return a float array containing the values of in
     */
	public static float [] getFloatArray(double in[])  {
		if (in == null) return null;		
		float ret[] = new float[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (float)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a long integer array
     * @return a float array containing the values of in
     */
	public static float [] getFloatArray(long in[])  {
		if (in == null) return null;		
		float ret[] = new float[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (float)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a integer array
     * @return a float array containing the values of in
     */
	public static float [] getFloatArray(int in[])  {
		if (in == null) return null;		
		float ret[] = new float[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (float)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a short integer array
     * @return a float array containing the values of in
     */
	public static float [] getFloatArray(short in[])  {
		if (in == null) return null;		
		float ret[] = new float[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (float)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a byte array
     * @return a float array containing the values of in
     */
	public static float [] getFloatArray(byte in[])  {
		if (in == null) return null;		
		float ret[] = new float[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (float)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a double array
     * @return a long integer array containing the values of in
     */
	public static long [] getLongArray(double in[])  {
		if (in == null) return null;		
		long ret[] = new long[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (long)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a float array
     * @return a long integer array containing the values of in
     */
	public static long [] getLongArray(float in[])  {
		if (in == null) return null;		
		long ret[] = new long[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (long)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a long integer array
     * @return a long integer array containing the values of in
     */
	public static long [] getLongArray(long in[])  {
		if (in == null) return null;		
		long ret[] = new long[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (long)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a integer array
     * @return a long integer array containing the values of in
     */
	public static long [] getLongArray(int in[])  {
		if (in == null) return null;		
		long ret[] = new long[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (long)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a short integer array
     * @return a long integer array containing the values of in
     */
	public static long [] getLongArray(short in[])  {
		if (in == null) return null;		
		long ret[] = new long[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (long)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a byte array
     * @return a long integer array containing the values of in
     */
	public static long [] getLongArray(byte in[])  {
		if (in == null) return null;		
		long ret[] = new long[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (long)in[i];
		}
		
		return ret;
	}
	
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a double array
     * @return a integer array containing the values of in
     */
	public static int [] getIntegerArray(double in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a float array
     * @return a integer array containing the values of in
     */
	public static int [] getIntegerArray(float in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a long integer array
     * @return a integer array containing the values of in
     */
	public static int [] getIntegerArray(long in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a integer array
     * @return a integer array containing the values of in
     */
	public static int [] getIntegerArray(int in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a short integer array
     * @return a integer array containing the values of in
     */
	public static int [] getIntegerArray(short in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a byte array
     * @return a integer array containing the values of in
     */
	public static int [] getIntegerArray(byte in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a double array
     * @return a short integer array containing the values of in
     */
	public static int [] getShortArray(double in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (short)in[i];
		}
		
		return ret;
	}
	

	/**
     * returns a type-converted copy of the input vector 
     * @param in a float array
     * @return a short integer array containing the values of in
     */
	public static int [] getShortArray(float in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (short)in[i];
		}
		
		return ret;
	}
	

	/**
     * returns a type-converted copy of the input vector 
     * @param in a long integer array
     * @return a short integer array containing the values of in
     */
	public static int [] getShortArray(long in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (short)in[i];
		}
		
		return ret;
	}
	

	/**
     * returns a type-converted copy of the input vector 
     * @param in a integer array
     * @return a short integer array containing the values of in
     */
	public static int [] getShortArray(int in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (short)in[i];
		}
		
		return ret;
	}
	

	/**
     * returns a type-converted copy of the input vector 
     * @param in a short integer array
     * @return a short integer array containing the values of in
     */
	public static int [] getShortArray(short in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (short)in[i];
		}
		
		return ret;
	}	
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a short integer array
     * @return a short integer array containing the values of in
     */
	public static int [] getShortArray(byte in[])  {
		if (in == null) return null;		
		int ret[] = new int[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (int)in[i];
		}		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a double array
     * @return a byte array containing the values of in
     */
	public static byte [] getByteArray(double in[])  {
		if (in == null) return null;		
		byte ret[] = new byte[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (byte)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a float array
     * @return a byte array containing the values of in
     */
	public static byte [] getByteArray(float in[])  {
		if (in == null) return null;		
		byte ret[] = new byte[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (byte)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a long integer array
     * @return a byte array containing the values of in
     */
	public static byte [] getByteArray(long in[])  {
		if (in == null) return null;		
		byte ret[] = new byte[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (byte)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a integer array
     * @return a byte array containing the values of in
     */
	public static byte [] getByteArray(int in[])  {
		if (in == null) return null;		
		byte ret[] = new byte[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (byte)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a short integer array
     * @return a byte array containing the values of in
     */
	public static byte [] getByteArray(short in[])  {
		if (in == null) return null;		
		byte ret[] = new byte[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (byte)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input vector 
     * @param in a byte array
     * @return a byte array containing the values of in
     */
	public static byte [] getByteArray(byte in[])  {
		if (in == null) return null;		
		byte ret[] = new byte[in.length];
		
		for (int i=0; i < in.length; i++) {
			ret[i] = (byte)in[i];
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input matrix 
     * @param in a float 2D array
     * @return a double 2D array containing the values of in
     */
	public static double [][] getDouble2DArray(float in[][])  {
		if (in == null) return null;
		if (in.length == 0) {
			System.err.println("Conversion::getDouble2DArray(float[][]): x-dimension is zero");
			return null;
		}
		double ret[][] = new double[in.length][in[0].length];
		
		for (int i=0; i < in.length; i++) {
			for (int j=0; j < in.length; j++) {
				ret[i][j] = (double)in[i][j];
			}
		}
		
		return ret;
	}
	
	/**
     * returns a type-converted copy of the input matrix 
     * @param in a double 2D array
     * @return a float 2D array containing the values of in
     */
	public static float [][] getFloat2DArray(double in[][])  {
		if (in == null) return null;
		if (in.length == 0) {
			System.err.println("Conversion::getDouble2DArray(float[][]): x-dimension is zero");
			return null;
		}
		float ret[][] = new float[in.length][in[0].length];
		
		for (int i=0; i < in.length; i++) {
			for (int j=0; j < in.length; j++) {
				ret[i][j] = (float)in[i][j];
			}
		}
		
		return ret;
	}
	
}
