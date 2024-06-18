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

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.xml.stream.XMLStreamException;

import pygar.configuration.DocumentError;
import pygar.cryptography.CryptoEngine;
import pygar.cryptography.CryptoException;
import pygar.documents.EncryptedFieldTable;
import pygar.documents.EncryptedFieldTable.EFTYPE;
import pygar.documents.EncryptedFieldTable.Row;

/** Perform the innermost encryption step: the encryption of fields but not the
 * semantic tags of the statements. 
 * <p>According to the original plan, this class is abstract because there may be
 * alternative document encodings during development and perhaps even in deployment. 
 * Thus, we introduce this abstract class and work is done in a subclass: FieldCryptoXmlTxt0
 * @see pygar.zoneable.FieldCryptoXmlTxt0
 * </p>
 * <p>A potential alternative method would use document tree structures that are
 * stored and transmitted in binary. In order to supply that alternative, it will be
 * necessary write a new implementation in a subclass. </p>
 * 
 * @author pbaker
 *
 */
public abstract class FieldCrypto {
	
	protected Cipher cipherEncrypt;
	protected Cipher cipherDecrypt;
	protected CryptoEngine crypto;
	protected int cipherBlockSize;
	protected int cipherOutputSize;
	protected volatile EncryptedFieldTable table;
	protected Key key;
	
	/** Create an object initializing it for encryption using the table definitions
	 * of the fields.
	 * @param crypto the CryptoEngine object for this configuration
	 * @param key the symmetric encryption key
	 * @param table the definitions of the fields
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 */
	public FieldCrypto(CryptoEngine crypto, Key key, EncryptedFieldTable table) throws 
	NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		this.table = table;
		this.key = key;
		// set up encryption algorithms
		this.crypto = crypto;
		this.cipherEncrypt = Cipher.getInstance(crypto.crypt_algorithm);
		this.cipherDecrypt = Cipher.getInstance(crypto.crypt_algorithm);
		
		if (this.cipherEncrypt == null || this.cipherDecrypt == null) {
			System.out.println("Cipher.getInstance failed " + crypto.crypt_algorithm);
		}
		this.cipherEncrypt.init(Cipher.ENCRYPT_MODE, key);
		this.cipherDecrypt.init(Cipher.DECRYPT_MODE, key);
		this.cipherBlockSize = cipherEncrypt.getBlockSize();
				
	}
	
	/** Encode the in stream applying the encryption algorithm to the fields
	 * as specified in the table and placing results on the out stream.
	 * @param in
	 * @param out
	 * @throws DocumentError 
	 */
	public abstract void encode(InputStream in, OutputStream out) throws DocumentError;
	
	/** Decode the in stream applying the encryption algorithm to the fields
	 * as specified in the table and placing the results on the out stream.
	 * @param in
	 * @param out
	 * @throws CryptoException 
	 */
	public abstract void decode(InputStream in, OutputStream out) throws CryptoException;
	
	/** Encode a single field represented as a string creating an encrypted
	 * byte array using the field type specified.
	 * @param row - from EncryptedFieldTable that describes the field
	 * @param value
	 * @return encoded value as string of hexadecimal
	 * @throws CryptoException 
	 */
	public abstract String encodeField(Row row, String value) throws CryptoException;
	
	/** Decode a single encrypted field according to its type creating
	 * a clear text string for the value. 
	 * @param ftype the type of the field
	 * @param name the name of the field
	 * @param value
	 * @return the decoded value as a string
	 * @throws CryptoException 
	 */
	public abstract String decodeField(EFTYPE ftype, String name, byte[] value) throws CryptoException;

	/** Decode a single encrypted field according to its type creating
	 * a clear text string for the value. 
	 * @param row the description of the field
	 * @param value
	 * @return the decoded value as a string
	 * @throws CryptoException 
	 */
	public abstract String decodeField(Row row, String value) throws CryptoException;

	/** Partially encrypt the input stream according to the specifications of the 
	 * EncryptedFieldTable and using the current session key.
	 * @throws XMLStreamException 
	 * @throws CryptoException 
	 */
	public abstract void partiallyEncryptStream(InputStream inStream, OutputStream outStream,
			EncryptedFieldTable table) throws XMLStreamException, CryptoException;

	/**
	 * Decrypt the document on the inStream assuming it was partially encrypted with the current
	 * session encryption key.
	 * @param inStream
	 * @param outStream
	 * @param table
	 * @throws XMLStreamException
	 * @throws CryptoException
	 */

	public abstract void decryptPartiallyEncryptedStream(InputStream inStream, OutputStream outStream,
			EncryptedFieldTable table) throws XMLStreamException, CryptoException;
	
	/**
	 * Compare two streams containing XML documents containing fields described by the
	 * field table. Check each field for equality. Allow a tolerance in the comparison
	 * of real value fields. Other fields are compared for exact identity.
	 * This test procedure is provided to check the results a 
	 * round-trip encryption and decryption.
	 * 
	 * @param inStream1 first xml document to compare
	 * @param inStream2 second xml document
	 * @param tolerance allowable difference between two real values as a fraction of value
	 * @param table description of fields
	 * @return true if the documents and equal in every field within the tolerance
	 * @throws XMLStreamException
	 * @throws Exception 
	 */
	public abstract boolean compareStreams(InputStream inStream1, InputStream inStream2,
			Double tolerance,
			EncryptedFieldTable table) throws XMLStreamException, Exception;

	
	public abstract boolean verifyField(Row row, Double tolerance, String value1, String value2)
			throws CryptoException;

	
	/** 
	 * Encode a string with a symmetric session key and return it
	 * as a string representation in hexadecimal.  	 
	 * @param s - clear text string
	 * @return encoded string
	 * @throws CryptoException 
	 */
	protected abstract String encodeString(String s) throws CryptoException;
	
	}
