	/****************************************************************CopyrightNotice
	 * Copyright (c) 2013 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *******************************************************************************/

package pygar.cryptography;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.*;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import pygar.identity_authority.KeyNotFound;



/** Objects that implement the abstract class CryptoEngine supply methods for the standard
 * cryptography algorithms. We bundle the algorithms them with an object so that 
 * the algorithms can be applied where needed through an exemplar object instantiated
 * with the configuration. This indirect method allows each application to select
 * an appropriate version of the algorithms.
 * 
 * Attention: the class may contain an order sensitivity. The abstract functions
 * include functions that set the password for the KeyStore for public key 
 * encryption and one of these functions must be called before calling the functions
 * that perform encryption. A preferred implementation of this abstract class would
 * force the code to provide the password as part of the constructor; then, the
 * KeyStore will always be ready when needed. 
 * 
 * @author pbaker
 *
 */
public abstract class CryptoEngine {
	
	protected KeyStore publicKeyStore;
	protected KeyStore privateKeyStore;
	protected KeyStore sessionKeyStore;
	
	protected String privateEntryPassword;
	protected char[] privateEntryPass;
	protected String entityAlias;
    
	public String crypt_algorithm;
    
    public boolean fullyConfigured;
    
    /**
     * Supply the password for the keystore used
     * with the CryptoEngine. Generally, this will 
     * be available at a later time after the object is created
     * because it might require special actions to supply the
     * password value.
     * @param password
     */
    abstract public void setPassword(String password);

    /**
     * Supply the password for the keystore used with the CryptoEngine. Generally, this
     * will be available at a later time after the object is created because it might
     * require special actions to supply the password value.
     *
     * @param password
     */
    abstract public void setPassword(char[] password);

    /**
	 * Return the public key of an entity from the KeyStore
	 * @param entityName name of the entity
	 * @return PublicKey
	 * @throws KeyNotFound
	 */
	abstract public PublicKey getPublicKey(String entityName) throws KeyNotFound;
	
	/**
	 * Return the private key of the current entity from the KeyStore
	 * @return PrivateKey
	 * @throws KeyNotFound
	 * @throws Exception 
	 */
	abstract public PrivateKey getPrivateKey() throws KeyNotFound, Exception;
	
    /**
     * Generate a random symmetric encryption key. Such keys are used for the 
     * session encryption and also in the practical implementation of public
     * key encryption. 
     * @return SecretKeySpec
     */
    abstract public SecretKey randomKey();
    
    /**
     * Wrap a secret key using the public key of the named entity
     * and return the wrapped key as a byte array.
     * 
     * @param spec an object that contains an encryption key
     * @return byte[] the wrapped secret key
     */
    abstract public byte[] wrapSecretKey(Key spec, String name);
    
    /**
     * UnWrap a secret key contained in a byte array using the private key
     * for this entity and return the SecretKeySpec
     * 
     * @param wrapped a byte array containing the wrapped key object that contains an encryption key
     * @return SecretKeySpec the secret key
     */
    abstract public SecretKey unwrapSecretKey(byte[] wrappedKey);
    
	/** 
	 * Encrypt clear text from an input stream using Public Key Encryption applying 
	 * the public key of the named destination and placing the encrypted text on
	 * the output stream.
	 * @param name
	 * @param in
	 * @param out
	 * @throws KeyNotFound 
	 * @throws Exception 
	 * @throws NoSuchAlgorithmException 
	 */
	abstract public void encryptStream(String name, InputStream in, 
			DataOutputStream out) throws KeyNotFound, Exception;
	
	/** 
	 * Decrypt the text on the input stream using the current entities private
	 * key and the public key encryption system. 
	 * @param in
	 * @param out
	 * @throws NoSuchAlgorithmException 
	 * @throws Exception 
	 */
	abstract public void decryptStream(DataInputStream in, 
			OutputStream out) throws NoSuchAlgorithmException, Exception;
	
	/** Perform a symmetric key encryption or decryption on stream.
	 * In this system, unencrypted data is text but encrypted data is
	 * a byte stream. 
	 * @param inStream input data
	 * @param outStream output data
	 * @param cipher an object of type Cipher
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	abstract public void crypt(InputStream inStream, OutputStream outStream, 
			Cipher cipher) throws IOException,
	GeneralSecurityException;
    
    /**
     * Sign a text string by computing its encrypted value under this entities
     * private key. The implementation supplies a standard algorithm and converts
     * the signature to a hexadecimal text representation.
     * 
     * @param text the text to be signed
     * @return a digital signature of the input text as a hexadecimal string representation
     */
    abstract public String signText(String text);
    
    /**
     * Check a signature by decrypting it with the public key of the declared name and
     * comparing the decrypted text with the original text. 
     * 
     * @param name
     * @param signature
     * @return 
     */
    abstract public boolean verifySignedText(String name, String text, String signature);

	/** 
	 * A function to convert a string containing a byte array written
	 * as hexadecimal into a byte array. By our conventions, a hexadecimal
	 * representation of a string must have an even number of characters.
	 * This function will throw an indexing out of bounds exception if
	 * this assumption is false.
	 * @param s A string containing a hexadecimal representation of an
	 * array of bytes.
	 * @return byte array 
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		String subs;
		byte [] ba = new byte[ (len + 1) / 2 ];
		
		for (int i = 0; i < len; i = i + 2) {
			subs = s.substring(i, i + 2);
			ba[ i/2] = (byte) Integer.parseInt(subs, 16);
		}
		
		return ba;
		
	}

	/** 
	 * A function to convert a byte array to a string with hexadecimal conversion
	 * 	
	 * @param ba
	 * @return string containing 2 characters for each byte, the pair being the 
	 * hexadecimal equivalent of the byte. 
	 */
	public static String byteArrayToHex(byte[] ba) {
		StringBuilder sb = new StringBuilder();
		int ib;
		int n16;
		int r16;

		for ( int k = 0; k < ba.length; ++k) {
			ib = ba[k];
			if (ib < 0) {
				ib += 256;
			}
			n16 = ib / 16;
			r16 = ib % 16;

			sb.append( Integer.toHexString(n16));
			sb.append( Integer.toHexString(r16));
		}
		return sb.toString();


	}
	
}
