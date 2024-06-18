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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.jcip.annotations.ThreadSafe;

import pygar.configuration.DocumentError;
import pygar.cryptography.CryptoEngine;
import pygar.cryptography.CryptoException;
import pygar.documents.EncryptedFieldTable;
import pygar.documents.EncryptedFieldTable.*;

/** Perform the innermost encryption step: the encryption of fields but not the
 * semantic tags of the statements. This specialization of class FieldCrypto 
 * operates on XML documents in textual form. Encrypted text is represented
 * in hexadecimal. This is the earliest version; it was written for demo0.
 * 
 * Most operations on the XML fields are fairly stable and insensitive to the 
 * application. The one source of variability is the choice of an order preserving
 * encryption for numerical quantities that might be searched on range and/or 
 * order comparison. For that reason, particular 
 * applications may supply a subclass that contains an override for particular
 * encryption operations.

 * @author pbaker
 *
 */
@ThreadSafe
public class FieldCryptoXmlTxt0 extends FieldCrypto {

	public class RealDetail {
		volatile public double scale;
		volatile public boolean useOffset;
		volatile public double offset;

		public RealDetail(double scale, boolean useOffset, double offset) {
			this.scale = scale;
			this.useOffset = useOffset;
			if (useOffset) {
				this.offset = offset;
			}
		}
	}

	private volatile ConcealReal realHandler;
	private volatile ConcurrentHashMap<String, RealDetail> detailMap;

	public FieldCryptoXmlTxt0(CryptoEngine crypto, Key key,
			EncryptedFieldTable table) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException {

		super(crypto, key, table);

		realHandler = new ConcealReal();
		detailMap = new ConcurrentHashMap<String, RealDetail>();
	}
	
	/**
	 * This class contains the algorithm for concealing or revealing real numbers. In Java,
	 * there are two real number types: double and float. We use double exclusively to avoid
	 * concerns about loss of accuracy. 
	 * <p>
	 * The algorithm conceals by mapping the value in a linear fashion that cannot be
	 * reversed without the encryption key. The mapping preserves the numerical order
	 * relationship between values so that one can say the concealment effectively hides
	 * the true value but the method of hiding it falls short of some definitions of
	 * encryption because encryption, by those definitions, would randomize the order relationship 
	 * in an apparently random fashion.
	 * </p><p>
	 * In the current algorithm, the linear relationship scales by a factor between 
	 * a very large and a very small amount depending on the bit pattern found by
	 * encryption of the name. 
	 * </p><p>
	 * If the estimated range of the values has been given
	 * in the EncryptedFieldTable, then the value will first be offset by a positive amount
	 * between vmin and vmax. The offset is applied before scaling to prevent certain roundoff
	 * errors that occur with low probability when the addition operation is performed after 
	 * scaling.
	 * </p>
	 * 
	 * @author pbaker
	 *
	 */
	public class ConcealReal {
		
		private RealDetail makeDetail(Row row) throws CryptoException {
			String codeString = encodeString(row.name);
			if (codeString.length() < 32) {
				throw new CryptoException();
			}
			/* There is some tortured coding here because the parseLong function of Long
			 * does not tolerate negative hexadecimal values. We must get rid of the sign
			 * bit, read a positive number, and then make it negative. 
			 */

			String upperS = codeString.substring(0, 16);
			String lowerS = codeString.substring(16, 32);
//			System.out.printf("codeString:%s upper: %s lower: %s %n", codeString, upperS, lowerS);
			long upperLong;
			long lowerLong;
			
			String cUpper = upperS.substring(0,1);
			String cLower = lowerS.substring(0,1);
			int intUpper = Integer.parseInt(cUpper, 16);
			int intLower = Integer.parseInt(cLower, 16);
			
			if (intUpper > 7) {
				intUpper = intUpper - 8;
				StringBuilder sb = new StringBuilder();
				sb.append(String.valueOf(intUpper));
				sb.append(upperS.substring(1));
				upperLong = - Long.parseLong( sb.toString(), 16);
			} else {
				upperLong = Long.parseLong(upperS, 16);
			}
			
			
			if (intLower > 7) {
				intLower = intLower - 8;
				StringBuilder sb = new StringBuilder();
				sb.append(String.valueOf(intLower));
				sb.append(lowerS.substring(1));
				lowerLong = - Long.parseLong( sb.toString(), 16);
			} else {
				lowerLong = Long.parseLong(upperS, 16);
			}

			double scale;
			double offset = 0.0;

			if (upperLong < 0.0) {
				scale =  1.0 / (double) (-upperLong);
			} else {
				scale = (double) (upperLong);
			}
			
			if (row.rangeSet) {
				double lowerDouble = (double) lowerLong;
				double minDouble = (double) Long.MIN_VALUE;
				double rangeLong = ((double) Long.MAX_VALUE - (double) Long.MIN_VALUE);
				offset = (row.vmax - row.vmin) * ( lowerDouble - minDouble ) / rangeLong + row.vmin;
			}
				
			return new RealDetail(scale, row.rangeSet, offset);
			
		}

		/**
		 * Conceal the real value contained in the string "value" according to the
		 * description provided in the row. The concealment algorithm uses scale and
		 * offset factors determined from the encrypted value of the name - a value
		 * that appears random and can only be determined if the encryption key is 
		 * known. 
		 * <p>Note that the name that is used for the method is partially qualified
		 * name found in the EncryptedFieldTable. We do not use two other alternatives
		 * those being the fully qualified name in the document and the single, rightmost
		 * part of the qualified name.</p>
		 * 
		 * @param row from EncryptedFieldTable that describes the field 
		 * @param value
		 * @return concealed value
		 * @throws CryptoException 
		 */
		public String concealReal(Row row, String value) throws CryptoException {
			return concealReal(row, Double.parseDouble(value));
		}
		
		/**
		 * Reveal the real value contained in the string "value" according to the
		 * description provided in the row. Reverse the concealment algorithm 
		 * performed by function concealReal.
		 * 
		 * @param row from EncryptedFieldTable that describes the field
		 * @param value - concealed real value
		 * @return clear text real value
		 * @throws CryptoException 
		 */
		public String revealReal(Row row, String value) throws CryptoException {
			return revealReal(row, Double.parseDouble(value));
		}
		
		/**
		 * Conceal the real value contained in "xvalue" according to the
		 * description provided in the "row". The concealment algorithm uses scale and
		 * offset factors determined from the encrypted value of the name - a value
		 * that appears random and can only be determined if the encryption key is 
		 * known. 
		 * <p>Note that the name that is used for the method is partially qualified
		 * name found in the EncryptedFieldTable. We do not use two other alternatives
		 * those being the fully qualified name in the document and the single, rightmost
		 * part of the qualified name.</p>
		 * 
		 * @param row from EncryptedFieldTable that describes the field 
		 * @param xvalue
		 * @return concealed value
		 * @throws CryptoException 
		 */
		public String concealReal(Row row, double xvalue) throws CryptoException {
			String name = row.name;
			RealDetail detail;
			
			if (detailMap.containsKey(name)) {
				detail = detailMap.get(name);
			} else {
				detail = makeDetail(row);
				detailMap.putIfAbsent(name, detail);
			}
			
			double yvalue;
			
			if (detail.useOffset) {
				yvalue = detail.scale * (xvalue + detail.offset);
			} else {
				yvalue = detail.scale * xvalue;
			}
			
			return String.valueOf(yvalue);
		}
		
		/**
		 * Reveal the real value contained in "xvalue" according to the
		 * description provided in the "row".
		 * Reverse the effect of concealReal.
		 * 
		 * @param row from EncryptedFieldTable that describes the field
		 * @param xvalue - concealed value
		 * @return clear text real value
		 * @throws CryptoException 
		 */
		public String revealReal(Row row, double xvalue) throws CryptoException {
			String name = row.name;
			RealDetail detail;
			
			if (detailMap.containsKey(name)) {
				detail = detailMap.get(name);
			} else {
				detail = makeDetail(row);
				detailMap.putIfAbsent(name, detail);
			}
			
			double yvalue;
			
			if (detail.useOffset) {
				yvalue = (xvalue / detail.scale) - detail.offset;
			} else {
				yvalue = xvalue / detail.scale;
			}
			
			return String.valueOf(yvalue);
		}
		/** 
		 * Conceal a real range by concealing both the high and low values with 
		 * the concealReal function.
		 * @param row from EncryptedFieldTable that describes the field
		 * @param range
		 * @return concealed value range
		 * @throws CryptoException
		 */
		public String concealRange(Row row, RealRange range) throws CryptoException {
			StringBuilder sb = new StringBuilder();
			sb.append( concealReal(row, range.lower));
			sb.append(",");
			sb.append( concealReal(row, range.upper));
			return sb.toString();
		}
		
		/** 
		 * Reveal a real range by revealing both the high and low values with 
		 * the revealReal function.
		 * @param row - from EncryptedFieldTable that describes the field
		 * @param range - concealed range
		 * @return clear text range
		 * @throws CryptoException
		 */
		public String revealRange(Row row, RealRange range) throws CryptoException {
			StringBuilder sb = new StringBuilder();
			sb.append( revealReal(row, range.lower));
			sb.append(",");
			sb.append( revealReal(row, range.upper));
			return sb.toString();
		}
		
		/** 
		 * Conceal a real range by concealing both the high and low values with 
		 * the concealReal function.
		 * @param row - from EncryptedFieldTable that describes the field
		 * @param s - a string containing a text representation of a real range
		 * @return a string containing the range concealed with encryption key
		 * @throws CryptoException
		 */
		public String concealRange(Row row, String s) throws CryptoException {
			int commaLoc = s.indexOf(",");
			String lowerS = s.substring(0, commaLoc).trim();
			String upperS = s.substring(commaLoc + 1).trim();
			return concealRange( row, new RealRange(Double.parseDouble(lowerS), Double.parseDouble(upperS)));
		}
		
		/** 
		 * Reveal a real range by revealing both the high and low values with 
		 * the revealReal function. Reverse the effect of the concealRange function.
		 * 
		 * @param row - from EncryptedFieldTable that describes the field
		 * @param s -string containing a concealed text representation of a real range
		 * @return clear text real range
		 * @throws CryptoException
		 */
		public String revealRange(Row row, String s) throws CryptoException {
			int commaLoc = s.indexOf(",");
			String lowerS = s.substring(0, commaLoc).trim();
			String upperS = s.substring(commaLoc + 1).trim();
			return revealRange( row, new RealRange(Double.parseDouble(lowerS), Double.parseDouble(upperS)));
		}

	}
	
	
	@Override
	public void decode(InputStream in, OutputStream out) throws CryptoException {
		throw new CryptoException();

	}

	/** Decode a field value according to the ftype specifier
	 * returning a string. N.b. the internal logic
	 * is written to handle the possibility of a input byte array whose length
	 * is not a multiple of the cipherBlockSize. This does not appear to happen
	 * no doubt because of padding; therefore, the logic has never been tested
	 * for the possibility that a different encryption algorithm might not 
	 * perform padding.
	 * @param ftype the type of the field
	 * @param value the encrypted value as a byte array
	 * @return the unencrypted value as a string
	 * @throws CryptoException if any problem occurs with decryption
	 */
	@Override
	public synchronized String decodeField(EFTYPE ftype, String name, byte[] value) throws CryptoException {
		// allocate a place for the output as it is returned by the cipher
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();

		int outlength;
//		try {
//			cipher.init(Cipher.DECRYPT_MODE, key);
//		} catch (InvalidKeyException e) {
//			e.printStackTrace();
//			throw new CryptoException();
//		}
		cipherOutputSize = cipherDecrypt.getOutputSize(cipherBlockSize);

		try {
			switch (ftype) {
			case F_STRING:
			case F_INT_EXACT:
			case F_NAME_STRING:
				byte[] outBytes = new byte[cipherOutputSize];
//				System.out.printf("decoding started on %d bytes, output size %d %n",
//						value.length, cipherOutputSize);
				boolean finalized = false;

				for (int i = 0; i < value.length; i += cipherBlockSize) {
					int k = Math.min( cipherBlockSize, value.length - i);
					if ( k == cipherBlockSize) {
						outlength = cipherDecrypt.update(value, i, cipherBlockSize, outBytes);
						outStream.write(outBytes, 0, outlength);
					} else {
						if ( k > 0) {
							outBytes = cipherDecrypt.doFinal(value, i, k);
						} else {
							outBytes = cipherDecrypt.doFinal();
						}
						finalized = true;
						outStream.write(outBytes);
					}
				}

				if (!finalized) {
					outBytes = cipherDecrypt.doFinal();
					outStream.write(outBytes);
					finalized = true;
				}
				return outStream.toString();

			default:
				System.err.println("not implemented for " + ftype);
				throw new CryptoException();

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
	}


	@Override
	public void encode(InputStream in, OutputStream out) throws DocumentError {
		try {
			partiallyEncryptStream(in, out, table);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new DocumentError();
		} catch (CryptoException e) {
			e.printStackTrace();
			throw new DocumentError();
		}

	}
	
	/** 
	 * Encode a string with a symmetric session key and return it
	 * as a string representation in hexadecimal.  	 *
	 * @param s - clear text string
	 * @return encoded string
	 * @throws CryptoException 
	 */
	protected synchronized String encodeString(String s) throws CryptoException {

		cipherOutputSize = cipherEncrypt.getOutputSize(cipherBlockSize); 

		byte[] valueBytes = s.getBytes();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		int outlength;
		byte[] outBytes = new byte[cipherOutputSize];
		try {
			for (int i = 0; i < valueBytes.length; i += cipherBlockSize) {
				int k = Math.min( cipherBlockSize, valueBytes.length - i);
				if ( k == cipherBlockSize) {
					outlength = cipherEncrypt.update(valueBytes, i, cipherBlockSize, outBytes);
					outStream.write(outBytes, 0, outlength);
				} else {
					if ( k > 0) {
						outBytes = cipherEncrypt.doFinal(valueBytes, i, k);
					} else {
						outBytes = cipherEncrypt.doFinal();
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

	@Override
	public boolean verifyField(Row row, Double tolerance, String value1, String value2) throws CryptoException {
		return _verifyField(row.ftype, tolerance, value1, value2);
	}
	
	private boolean _verifyField(EFTYPE ftype, Double tolerance, String value1, String value2) throws CryptoException {	
		// There are several different procedures so we start with testing for the
		// most common case, then other cases, and finally we throw and exception if
		// we haven't handled the cases
		
			switch (ftype) {

			case F_STRING:
			case F_INT_EXACT:
				// easy case, expanded value is same a given value
				return value1.equals(value2);
				
			case F_REAL:
				Double x1 = Double.valueOf(value1);
				Double x2 = Double.valueOf(value2);
				Double diff;
				if (x1 == 0.0) {
					if ( x2 == 0.0) {
						return true;
					} else {
						diff = x2;
					}
				} else {
					diff = (x2 - x1) / x1;
				}
				return Math.abs(diff) < tolerance;
				
			case F_REAL_RANGE:
				int commaLoc1 = value1.indexOf(",");
				String lowerS1 = value1.substring(0, commaLoc1).trim();
				String upperS1 = value1.substring(commaLoc1 + 1).trim();
				
				int commaLoc2 = value2.indexOf(",");
				String lowerS2 = value2.substring(0, commaLoc2).trim();
				String upperS2 = value2.substring(commaLoc2 + 1).trim();
				
				return _verifyField(EFTYPE.F_REAL, tolerance, lowerS1, lowerS2) && 
				       _verifyField(EFTYPE.F_REAL, tolerance, upperS1, upperS2);

			default:
				return value1.equals(value2);
			}
	}
	
	@Override
	public String encodeField(Row row, String value) throws CryptoException {
		// There are several different procedures so we start with testing for the
		// most common case, then other cases, and finally we throw and exception if
		// we haven't handled the cases
		
			switch (row.ftype) {

			case F_STRING:
			case F_INT_EXACT:
				// easy case, expanded value is same a given value
				return encodeString(value.trim());
				
			case F_REAL:
				return realHandler.concealReal(row, value);
				
			case F_REAL_RANGE:
				return realHandler.concealRange(row, value);
				
			default:
				return value;
//				System.err.println("not implemented for " + ftype);
//				throw new CryptoException();

			}
		

		

	
	}
	public synchronized String decodeString(String value) throws CryptoException {
		// where do we get the correct cipher??
		byte [] inBytes = CryptoEngine.hexStringToByteArray(value);
		byte[] outBytes = new byte[cipherOutputSize];
		boolean finalized = false;
		int outlength;
		StringBuilder sb = new StringBuilder();
		try {
			for (int i = 0; i < inBytes.length; i += cipherBlockSize) {
				int k = Math.min( cipherBlockSize, value.length() - i);
				if ( k == cipherBlockSize) {
					outlength = cipherDecrypt.update(inBytes, i, cipherBlockSize, outBytes);
					sb.append( outBytes.toString().substring(0, outlength));
					//				outStream.write(outBytes, 0, outlength);
				} else {
					if ( k > 0) {
						outBytes = cipherDecrypt.doFinal(inBytes, i, k);
					} else {
						outBytes = cipherDecrypt.doFinal();
					}
					finalized = true;
					sb.append( new String(outBytes));
					//				outStream.write(outBytes);
				}
			}

			if (!finalized) {
				outBytes = cipherDecrypt.doFinal();
				sb.append( new String(outBytes));
				//			outStream.write(outBytes);
				finalized = true;
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
		}
		return sb.toString();
	}

	@Override
	public String decodeField(Row row, String value) throws CryptoException {
		// There are several different procedures so we start with testing for the
		// most common case, then other cases, and finally we throw and exception if
		// we haven't handled the cases
		
		//System.out.println(" invoke decodeField " + valueBytes.length);
			switch (row.ftype) {

			case F_STRING:
			case F_INT_EXACT:
				// easy case, expanded value is same a given value
				return decodeString(value.trim());
				
			case F_REAL:
				return realHandler.revealReal(row, value);
				
			case F_REAL_RANGE:
				return realHandler.revealRange(row, value);
				
			default:
				return value;
//				System.err.println("not implemented for " + ftype);
//				throw new CryptoException();

			}
		
	}
	
	/** Partially encrypt the input stream according to the specifications of the 
	 * EncryptedFieldTable and using the current session key.
	 * @throws XMLStreamException 
	 * @throws CryptoException 
	 */
	public void partiallyEncryptStream(InputStream inStream, OutputStream outStream,
			EncryptedFieldTable table) throws XMLStreamException, CryptoException {
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = factory.createXMLStreamReader(inStream);
		XMLOutputFactory outfact = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = outfact.createXMLStreamWriter(outStream);
		
		writer.writeStartDocument();
		
		int event;
		String text;
		String text2;
		String element;
		Row row;
		Stack<String> xmlPath = new Stack<String>();
		Stack<Boolean> suppressState = new Stack<Boolean>();
		Stack<Row> currentRow = new Stack<Row>();
		
		
		String currentPath = new String();
		xmlPath.push(currentPath);
		suppressState.push(false);
		row = new Row("", table.EFTYPE_DEFAULT, "");
		currentRow.push(row);
		boolean suppressp;
		
		while (parser.hasNext()) {
			event = parser.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				xmlPath.push(currentPath);
				element = parser.getLocalName();
				StringBuilder sb = new StringBuilder(currentPath);
				if (currentPath.length() > 0) {
					sb.append(".");
				}
				sb.append(element);
				currentPath = sb.toString();
//				System.out.printf("start element %s path %s\n", parser.getLocalName(), currentPath);
				
				// find field in table if possible
				row = table.getRow(currentPath);
				
				if (row == null) {
					row = table.defaultRow;
				}
				currentRow.add(row);
				
				// find out if we are working or not
				if (suppressState.peek() ) {
					// once the XML processing is suppressed, we suppress
					// all the subelements from here down the document tree
					suppressp = true;
				} else {
					// if we are running normally, see if suppress flag
					// is raised for the current element
					suppressp = row.ftype == EFTYPE.F_SUPPRESS;
				}
				// stack our suppression state
				suppressState.push(suppressp);
				
				if (!suppressp) {
					writer.writeStartElement(element);
				}				
				break;
			case XMLStreamConstants.END_ELEMENT:				
				suppressp = suppressState.peek();
				if (!suppressp) {
					writer.writeEndElement();
				}
				suppressState.pop();
				currentPath = xmlPath.pop();
				currentRow.pop();
				break;
			case XMLStreamConstants.CHARACTERS:
				suppressp = suppressState.peek();
				text = parser.getText();
				row = currentRow.peek();
				// the text returned by the parser could be data or just whitespace
				// we need to distinguish the two and avoid encrypting white space
				if (!suppressp ) {
					text2 = text.trim();
					if (text2.length() > 0) {
						// show the text that must be encrypted
						writer.writeCharacters(encodeField(row, text2));
//						String text3 = encodeField(row, text2);
//						writer.writeCharacters(text3);
//						writer.writeCharacters("||");
//						writer.writeCharacters(decodeField(row, text3));
					} else {
						// show whitespace that provides document formatting
						writer.writeCharacters(text);
					}
				} 
				break;
			default:
				break;
			}
		}

		writer.writeEndDocument();

		writer.close();

//		System.out.println("end of partiallyEncryptStream");
		
	}
	
	
	/**
	 * Decrypt the document on the inStream assuming it was partially encrypted with the current
	 * session encryption key.
	 * @param inStream
	 * @param outStream
	 * @param table
	 * @throws XMLStreamException
	 * @throws CryptoException
	 */
	public void decryptPartiallyEncryptedStream(InputStream inStream, OutputStream outStream,
			EncryptedFieldTable table) throws XMLStreamException, CryptoException {
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = factory.createXMLStreamReader(inStream);
		XMLOutputFactory outfact = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = outfact.createXMLStreamWriter(outStream);
		
		writer.writeStartDocument();
		
		int event;
		String text;
		String text2;
		String element;
		Row row;
		Stack<String> xmlPath = new Stack<String>();
		Stack<Boolean> suppressState = new Stack<Boolean>();
		Stack<Row> currentRow = new Stack<Row>();
		
		
		String currentPath = new String();
		xmlPath.push(currentPath);
		suppressState.push(false);
		row = new Row("", table.EFTYPE_DEFAULT, "");
		currentRow.push(row);
		boolean suppressp;
		
		while (parser.hasNext()) {
			event = parser.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				xmlPath.push(currentPath);
				element = parser.getLocalName();
				StringBuilder sb = new StringBuilder(currentPath);
				if (currentPath.length() > 0) {
					sb.append(".");
				}
				sb.append(element);
				currentPath = sb.toString();
//				System.out.printf("start element %s path %s\n", parser.getLocalName(), currentPath);
				
				// find field in table if possible
				row = table.getRow(currentPath);
				
				if (row == null) {
					row = table.defaultRow;
				}
				currentRow.add(row);
				
				// find out if we are working or not
				// Surpression should not be necessary if it was applied during partial encryption
				if (suppressState.peek() ) {
					// once the XML processing is suppressed, we suppress
					// all the subelements from here down the document tree
					suppressp = true;
				} else {
					// if we are running normally, see if suppress flag
					// is raised for the current element
					suppressp = row.ftype == EFTYPE.F_SUPPRESS;
				}
				// stack our suppression state
				suppressState.push(suppressp);
				
				if (!suppressp) {
					writer.writeStartElement(element);
				}				
				break;
			case XMLStreamConstants.END_ELEMENT:				
				suppressp = suppressState.peek();
				if (!suppressp) {
					writer.writeEndElement();
				}
				suppressState.pop();
				currentPath = xmlPath.pop();
				currentRow.pop();
				break;
			case XMLStreamConstants.CHARACTERS:
				suppressp = suppressState.peek();
				text = parser.getText();
				row = currentRow.peek();
				// the text returned by the parser could be data or just whitespace
				// we need to distinguish the two and avoid encrypting white space
				if (!suppressp ) {
					text2 = text.trim();
					if (text2.length() > 0) {
						// show the text that must be encrypted
						writer.writeCharacters(decodeField(row, text2));
					} else {
						// show whitespace that provides document formatting
						writer.writeCharacters(text);
					}
				} 
				break;
			default:
				break;
			}
		}

		writer.writeEndDocument();

		writer.close();

//		System.out.println("end of partiallyEncryptStream");
		
	}

	/**
	 * Compare two streams containing XML documents containing fields described by the
	 * field table. Check each field for equality. Allow a tolerance in the comparison
	 * of real value fields. Other fields are compared for exact identity.
	 * This test procedure is provided to check the results a 
	 * round-trip encryption and decryption. However note the following before
	 * using the procedure.
	 * <p>
	 * The partial encryption allows the original document to contain fields that
	 * are simply deleted in the production of the partially encrypted document. 
	 * Consequently, a decrypted copy of the document will omit these fields. Therefore
	 * be sure that inStream1 contains the original document and inStream2 contains
	 * the document produced by decryption. 
	 * </p>
	 * 
	 * @param inStream1 first xml document to compare
	 * @param inStream2 second xml document
	 * @param tolerance allowable difference between two real values as a fraction of value
	 * @param table description of fields
	 * @return true if the documents and equal in every field within the tolerance
	 * @throws Exception 
	 */
	@Override
	public boolean compareStreams(InputStream inStream1, InputStream inStream2,
			Double tolerance, EncryptedFieldTable table)
			throws Exception {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser1 = factory.createXMLStreamReader(inStream1);
		XMLStreamReader parser2 = factory.createXMLStreamReader(inStream2);

		
		
		int event1;
		int event2;
		int count = 0;
		String text;
		String text2, text3, text4;
		String element;
		Row row;
		Stack<String> xmlPath = new Stack<String>();
		Stack<Boolean> suppressState = new Stack<Boolean>();
		Stack<Row> currentRow = new Stack<Row>();
		
		
		String currentPath = new String();
		xmlPath.push(currentPath);
		suppressState.push(false);
		row = new Row("", table.EFTYPE_DEFAULT, "");
		currentRow.push(row);
		boolean suppressp;
		
		while (parser1.hasNext()) {
			event1 = parser1.next();
			switch (event1) {
			case XMLStreamConstants.START_ELEMENT:
				xmlPath.push(currentPath);
				element = parser1.getLocalName();
				StringBuilder sb = new StringBuilder(currentPath);
				if (currentPath.length() > 0) {
					sb.append(".");
				}
				sb.append(element);
				currentPath = sb.toString();
//				System.out.printf("start element %s path %s\n", parser.getLocalName(), currentPath);
				
				// find field in table if possible
				row = table.getRow(currentPath);
				
				if (row == null) {
					row = table.defaultRow;
				}
				currentRow.add(row);
				
				// find out if we are working or not
				if (suppressState.peek() ) {
					// once the XML processing is suppressed, we suppress
					// all the subelements from here down the document tree
					suppressp = true;
				} else {
					// if we are running normally, see if suppress flag
					// is raised for the current element
					suppressp = row.ftype == EFTYPE.F_SUPPRESS;
				}
				// stack our suppression state
				suppressState.push(suppressp);
				
				if (!suppressp) {
					// more forward on the second document
					if (parser2.hasNext()) {
						event2 = parser2.next();
						if (event2 != XMLStreamConstants.START_ELEMENT) {
							System.out.println("unexpected element on second XML document");
							throw new Exception();
						}
					} else {
						System.out.println("unexpected EOF on second XML document");
						throw new Exception();
					}
				}				
				break;
			case XMLStreamConstants.END_ELEMENT:				
				suppressp = suppressState.peek();
				if (!suppressp) {
					if (parser2.hasNext()) {
						event2 = parser2.next();
						if (event2 != XMLStreamConstants.END_ELEMENT) {
							System.out.println("unexpected element on second XML document");
							throw new Exception();
						}
					} else {
						System.out.println("unexpected EOF on second XML document");
						throw new Exception();
					}
				}
				suppressState.pop();
				currentPath = xmlPath.pop();
				currentRow.pop();
				break;
			case XMLStreamConstants.CHARACTERS:
				suppressp = suppressState.peek();
				text = parser1.getText();
				row = currentRow.peek();
				// the text returned by the parser could be data or just whitespace
				// we need to distinguish the two and avoid encrypting white space
				if (!suppressp ) {
					if (parser2.hasNext()) {
						event2 = parser2.next();
						if (event2 != XMLStreamConstants.CHARACTERS) {
							System.out.println("unexpected element on second XML document");
							throw new Exception();
						}
						text3 = parser2.getText();
						text4 = text3.trim();
					} else {
						System.out.println("unexpected EOF on second XML document");
						throw new Exception();
					}
					
					
					
					text2 = text.trim();
					if (text2.length() > 0) {
						// this is not whitespace so check whether it was translated
						// not finished, need to perform a better test
						if ( ! verifyField(row, tolerance, text2, text4) ) {
							System.out.printf("mismatch real value fields :%s: and :%s: %n", text2, text4);
							throw new Exception();
						} else {
							count++;
						}
					} 
					// white space is not compared
				} 
				break;
			default:
				break;
			}
		}
		System.out.printf("compareStreams verified %d fields in XML documents %n", count);
		// if we reach here the documents are essentially the same
		return true;
	}


}
