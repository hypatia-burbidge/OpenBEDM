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

// 
package pygar.zoneable;

// fragments of work in progress!!!!!!!!!!!!!!!

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import pygar.cryptography.CryptoEngine;
import pygar.documents.EncryptedFieldTable;

public class FieldCryptoDemo1 extends FieldCryptoXmlTxt0 {

	public FieldCryptoDemo1(CryptoEngine crypto, Key key,
			EncryptedFieldTable table) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException {
		super(crypto, key, table);
	}

	@Override
	public void decode(InputStream in, OutputStream out) {
		// TODO Auto-generated method stub

	}

	@Override
	public void encode(InputStream in, OutputStream out) {
		// TODO Auto-generated method stub

	}
	
	/** The main procedure is used only during development to test
	 * some internal methods. 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {


//		String demo1path = "/Users/pbaker/Coding/eclipse_workspace/demo1/data/";
//		String sourcepath=  demo1path + "test/testCryptDoc/";
//		@SuppressWarnings("unused")
//		ProfileDemo1 profileExample; // creating this object creates configuration
//		profileExample = new ProfileDemo1("clientA");
//		Profile profile = Profile.getProfile();
//
//		profile.loadProperties("demo1.props", 
//				demo1path + "clientA");
//
//		Properties props = profile.getProperties();
//
//		// copy the CharsetName
//		props.setProperty("CharsetName", profile.getCharsetName());
//
//		// get CryptoEngine object
//		CryptoEngine crypt = profile.getCrypto();
//
//		SecretKey tkey;
//		try {
//			KeyGenerator keygen = KeyGenerator.getInstance(crypt.crypt_algorithm);
//			SecureRandom random = new SecureRandom();
//			keygen.init(random);
//			tkey = keygen.generateKey();
//		} 
//		catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw e;
//		}
//		catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw e;
//		}
//
//
//		System.out.println("Begin Test symmetric key encryption");
//
//		EncryptedFieldTable table = new EncryptedFieldTable();
//		table.addRow( new Row("ask", EFTYPE.F_CLEAR, ""));
//		table.addRow( new Row("offer", EFTYPE.F_CLEAR, ""));
//		table.addRow( new Row("ref_id", EFTYPE.F_CLEAR, ""));
//
//		table.addRow( new Row("have", EFTYPE.F_STRING, ""));
//		table.addRow( new Row("want", EFTYPE.F_STRING, ""));
//		table.addRow( new Row("interval", EFTYPE.F_STRING, ""));
//		table.addRow( new Row("accept", EFTYPE.F_STRING, ""));
//		table.addRow( new Row("for", EFTYPE.F_STRING, ""));
//
//		FieldCrypto fcrypt = new FieldCryptoDemo1(crypt, tkey, null);
//
//		String testString = "now is the time for the lazy dog to jump";
//		byte[] testBlack1 = fcrypt.encodeField(EFTYPE.F_STRING, testString);
//		
//		byte[] testBlack2 = fcrypt.encodeField(EFTYPE.F_STRING, testString);;
//		boolean equals;
//
//		for (int i = 0; i < 3; i++) {
//			// test arrays for equality
//			equals = true;
//			for (int j = 0; j < testBlack1.length; j++ ) {
//				if (testBlack1[j] != testBlack2[j])
//					equals = false;
//			}
//			if (equals)
//				System.out.println("ok, bytes equal on pass " + i);
//			else
//				System.out.println("bad!, bytes unequal on pass " + 1);
//			testBlack2 = fcrypt.encodeField(EFTYPE.F_STRING, testString);
//		}
//
//		System.out.println(" clear1: " + fcrypt.decodeField(EFTYPE.F_STRING, testBlack2));
//		System.out.println(" clear2: " + fcrypt.decodeField(EFTYPE.F_STRING, testBlack1));
//
//
//
//		System.out.println("End Test symmetric key encryption");
//
	}

}
