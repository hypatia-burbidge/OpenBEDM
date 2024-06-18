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

import java.security.KeyStore;

import pygar.identity_authority.KeyStoreAccess;
import pygar.identity_authority.KeyStoreType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

import pygar.configuration.Dangerous;
import pygar.configuration.RestrictedImport;

/**
 * DONOT USE THIS CLASS - NEVER WORKED!
 * The KeyStoreAccessZone10FS class implements the KeyStoreAccess abstract class
 * keystore on a local file store in zone10. 
 * <p>It has proven impossible to make this class work with the available implementation
 * from of the JRE 1.6. The documentation claims that the JRE KeyStore can store a secret key
 * but when you try it, the JRE itself throws and exception after writing out an error message
 * that it will accept *only* private keys and certificates. </p>
 * <p>Currently, this class does not work and has not been tested.
 * There would be many limitations if it worked: It was developed for a demonstration
 * and contains password information that should not be present in production
 * code. It also assumes the special directory structure of 
 * the demonstration. Also, note that it must be initialized with the name
 * of the current application: that is,the alias of the current user in 
 * the keystore. </p>
 * 
 * @author pbaker
 *
 */
@Dangerous
@RestrictedImport

public class KeyStoreAccessZone10FS extends KeyStoreAccess {

	private String keystorePassword = "Zone10BEDM";
//	private String entryPassword = "pygardemo1";
	private String keystoreDir = "/Users/pbaker/Coding/eclipse_workspace/demo1/data/";

	public KeyStoreAccessZone10FS(String entityName) {
		currentEntity = entityName;
	}

	/** Return the KeyStore for the entity that is running the current application.
	 * 
	 * @param kst The type of the keystore desired
	 * @return the KeyStore
	 * @throws Exception 
	 */
	public KeyStore getKeyStore(KeyStoreType kst) throws Exception {

		String storeName;
		switch (kst) {
		case PRIVATE:
			storeName = "private.keystore";
			break;
		case PUBLIC:
			storeName = "public.keystore";
			break;
		case SESSION:
			storeName = "session.keystore";
		default:
			storeName = "";
		}

		char [] passwd = keystorePassword.toCharArray();
		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

			String filename = keystoreDir + currentEntity + File.separator + storeName;
			System.out.println("KeyStoreAccess ready to open " + filename);
			FileInputStream fs = new FileInputStream( filename);
			System.out.println("ready to load");
			ks.load(fs, passwd);
			return ks;
		} catch (KeyStoreException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (CertificateException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception();
		}
	}
	
	/** Create initial session keystore, initialize with provided single key
	 * under the alias name "test", and store keystore in the file system.
	 * @param key the key to be stored under name "test"
	 * @throws Exception 
	 * 
	 */
	public void createKeyStore(SecretKey key) throws Exception {
		char [] passwd = keystorePassword.toCharArray();
		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

			String filename = keystoreDir + currentEntity + File.separator + "session.keystore";
			System.out.println("KeyStoreAccess ready to open " + filename);
			FileOutputStream fs = new FileOutputStream( filename);
			System.out.println("ready to initialize");
			ks.load(null, passwd);
			// add the key
//			KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(key);
			
//			ks.setEntry("secretKeyAlias", skEntry, 
//			        new KeyStore.PasswordProtection(passwd));
			ks.setKeyEntry("test", key, passwd, null);
			// store
			ks.store(fs, passwd);
			fs.close();
			
//			return ks;
			
		} catch (KeyStoreException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (CertificateException e) {
			e.printStackTrace();
			throw new Exception();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception();
		}
		
	}

//    // save my secret key
//    javax.crypto.SecretKey mySecretKey;
//    KeyStore.SecretKeyEntry skEntry =
//        new KeyStore.SecretKeyEntry(mySecretKey);
//    ks.setEntry("secretKeyAlias", skEntry, 
//        new KeyStore.PasswordProtection(password));

//	public static void main(String[] args) throws Exception {
//		System.out.println("testing zone10 keystore");
//		KeyStoreAccess ksa = new KeyStoreAccessZone10FS("clientA");
//		KeyStore ks = ksa.getKeyStore(KeyStoreType.SESSION); 
//		ks.s
//		System.out.println("finished testing zone10 keystore");
//	}

}
