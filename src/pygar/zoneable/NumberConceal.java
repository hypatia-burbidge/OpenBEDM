	/****************************************************************CopyrightNotice
	 * Copyright (c) 2011 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *******************************************************************************/
package pygar.zoneable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import net.jcip.annotations.ThreadSafe;

import pygar.configuration.ConfigurationError;
import pygar.cryptography.CryptoEngine;
import pygar.cryptography.CryptoException;

/**
 * @author pbaker
 *
 */
@ThreadSafe
public class NumberConceal {
	private volatile Cipher cipher;
	private volatile Key key;
	private volatile int nseg;
	private volatile double cmin;
	private volatile double cmax;
	private volatile double cdelta;
	

	private class NameParams {
		volatile double vmin; 
		volatile double vmax;
		volatile double vdelta;
		volatile double afactor;
		volatile double pwlf[];
		
	}
	
	private volatile ConcurrentMap<String, NameParams> nameMap;
	
	/** Instances of this class provide a method to conceal the value of real numbers using 
	 * a piecewise linear mapping that is unique to the value's name. The mapping function is 
	 * derived by encrypting the value name and converting the encrypted value into a numeric
	 * function. 
	 * 
	 * The value of nsegments has practical constaints. If nsegments is too small,
	 * the algorithm as implemented here will attempt to convert a string as a hexidecimal number that is  
	 * too large for the Java converter. In the other direction, nsegments cannot be larger than 
	 * the number of bytes in the encryption algorithm's block size. Neither of these number 
	 * has a convenient, universal name, so we don't currently check the limits. During testing,
	 * the algorithm worked well for nsegments >= 4 and <= 32. Moreover, we only verified the 
	 * powers of two: 4, 8, 16, 32.
	 * 
	 * @param nsegments the number of linear segments in the piecewise linear function
	 * @param key the encryption key used to derive the function from a name
	 * @param number_min the most negative value that can be represented enterprise wide
	 * @param number_max the most positive value that can be represented enterprise wide
	 * @param number_range the agreed range of numerical representation which should be less than cmax-cmin
	 */
	public NumberConceal(Cipher crypto, String key, int nsegments, double number_min, double number_max, double number_range) {
		nseg = nsegments;
		cmin = number_min;
		cmax = number_max;
		cdelta = number_range;
		cipher = crypto;
		nameMap = new  ConcurrentHashMap<String, NameParams>();
	}
	
	/** Conceal a named value.
	 * @param name
	 * @param value
	 * @return the concealed value
	 * @throws ConfigurationError 
	 */
	public double conceal(String name, double value) throws ConfigurationError {
		NameParams p = nameMap.get(name);
		if (p == null) {
			System.err.printf("Unknown name provided to NumberConceal.conceal: %s %n", name);
			throw new ConfigurationError();
		}
		double xindex = (value - p.vmin )  / p.vdelta;
		int i1 = (int) Math.floor(xindex);
		int i2 = (int) Math.ceil(xindex);
//		System.out.printf("%nconceal %g %d %d %g %g %n", value, i1, i2, p.vmin + i1 * p.vdelta, p.vmin + i2 * p.vdelta);
		return p.pwlf[i1] + (p.pwlf[i2] - p.pwlf[i1]) * (xindex - (double) i1);
		
	}
	
	public double reveal(String name, double value) throws ConfigurationError {
		NameParams p = nameMap.get(name);
		if (p == null) {
			System.err.printf("Unknown name provided to reveal: %s %n", name);
			throw new ConfigurationError();
		}
		int i1 = 0;
		int i2 = nseg;
		int ic = nseg / 2;
		double x;
		while ( i2 -i1 > 1) {
			x = p.pwlf[ic];
			if ( value < x) {
				i2 = ic;
				ic = (i2 + i1) / 2;
			} else {
				i1 = ic;
				ic = (i2 + i1) / 2;
			}
		}
		x = (double)i1 + (value - p.pwlf[i1]) / (p.pwlf[i2] - p.pwlf[i1]);
//		System.out.printf("%nreveal %g, %d %d %g %g %n", value, i1, i2, p.pwlf[i1], p.pwlf[i2]);
		return p.vmin + x * p.vdelta;
		
	}
	
	/** Add the name of a value that will later be concealed by the conceal function
	 * @param name name of the value
	 * @param value_min smallest value expected for the named value
	 * @param value_max largest value expected for the named value
	 * @throws ConfigurationError
	 * @throws CryptoException 
	 */
	public void addName(String name, double value_min, double value_max) throws ConfigurationError, CryptoException {
		if (hasName(name)) {
			System.err.printf("Duplicate name provided to NumberConceal.addName: %s %n", name);
			throw new ConfigurationError();
		}
		NameParams np = new NameParams();
		np.vmin = value_min;
		np.vmax = value_max;
		np.vdelta = (value_max / (double)nseg) - (value_min / (double)nseg);
		np.pwlf = new double[nseg + 1];
		
		String encName = encodeString(name);
		System.out.printf("Encoded name %s %n", encName);
		int subLen = encName.length() / nseg;
		String substring;
		long Islope[]  = new long[nseg];
		double sum = 0.0;
		for (int i = 0; i < nseg; i++) {
			substring = encName.substring(subLen * i, subLen * (i+1) );
			Islope[i] = Long.parseLong(substring, 16);
			System.out.printf("substring %d is <%s> interpreted as %d %n", i + 1, substring, Islope[i]);
			sum += (double)Islope[i];
		}
		np.afactor = ((cmax - cdelta)/sum ) - cmin / sum;
		np.pwlf[0] = cmin;
		System.out.printf("pwlf %d = %g %n", 0, np.pwlf[0]);
		for (int i = 0; i < nseg; i++) {
			np.pwlf[i+1] = np.pwlf[i] + np.afactor * Islope[i] + cdelta / nseg;
			System.out.printf("pwlf %d = %g %n", i+ 1, np.pwlf[i+1]);
		}
		System.out.printf("cmin/max = %g %g cdelta %g reduced range %g %n", cmin, cmax, cdelta, np.afactor * sum);
		
		nameMap.put(name, np);
	}
	
	/** Check whether a value name is defined already.
	 * 
	 * @param name name of a value
	 * @return true if the name is defined
	 */
	public boolean hasName(String name) {
		return nameMap.containsKey(name);
	}

	public String encodeString(String s) throws CryptoException {
		int cipherBlockSize = cipher.getBlockSize();
		int cipherOutputSize = cipher.getOutputSize(cipherBlockSize); 
		
		byte[] valueBytes = s.getBytes();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		int outlength = 0;
		byte[] outBytes = new byte[cipherOutputSize];
		try {
			for (int i = 0; i < valueBytes.length; i += cipherOutputSize) {
				int k = Math.min( cipherOutputSize, valueBytes.length - i);
				if ( k == cipherOutputSize) {
					outlength = cipher.update(valueBytes, i, cipherOutputSize, outBytes);
					outStream.write(outBytes, 0, outlength);
				} else {
					if ( k > 0) {
						outBytes = cipher.doFinal(valueBytes, i, k);
					} else {
						outBytes = cipher.doFinal();
					}
					outStream.write(outBytes);
				}
			}
		} catch (ShortBufferException e) {
			e.printStackTrace();
			throw new CryptoException();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			throw new CryptoException();
		} catch (BadPaddingException e) {
			e.printStackTrace();
			throw new CryptoException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new CryptoException();
		}

		return CryptoEngine.byteArrayToHex(outStream.toByteArray());

	}

}
