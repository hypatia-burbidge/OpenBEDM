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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import net.jcip.annotations.ThreadSafe;

import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;
import pygar.identity_authority.KeyNotFound;
import pygar.identity_authority.KeyStoreAccess;
import pygar.identity_authority.KeyStoreType;

/** CryptoDefault is the default provider for basic cryptographic services. It is a basic implementation
 * of the CryptoEngine using only Core Java class components. This class must be instantiated with
 * a reference to a fully initialized Profile object from which it will obtain installation parameters.
 * 
 * The current version is thread safe but probably inefficient because it may force more exclusion 
 * that is necessary to execute correctly. This should be investigated in the context of the thread
 * safety convention of the underlying Java cryptographic library. 
 * 
 * The overall system of classes permits the selection of a cryptographic algorithm 
 * through a configuration system. However, this default provider may break for 
 * some choices of algorithms because it frequently assumes the key and block lengths
 * for AES algorithm. At this time, the entire library has been tested only with 
 * complementary set of algorithms: AES, RSA, and SHA-1.
 * 
 * @author pbaker
 *
 */
@ThreadSafe
public class CryptoDefault extends CryptoEngine {
	
	private Profile profile;
	
    public CryptoDefault(String name, Profile p, String password) {
        super();
        profile = p;
        crypt_algorithm = "AES";
        entityAlias = name;
        fullyConfigured = false;
        System.out.println("Initializing CryptoDefault for " + name + " password " + password);
        if (password != null) {
            privateEntryPassword = password;
            privateEntryPass = password.toCharArray();
            fullyConfigured = true;
        }

    }
    
    public void setPassword(String password) {
        privateEntryPassword = password;
        privateEntryPass = password.toCharArray();
        fullyConfigured = true;

    }

    public void setPassword(char[] password) {
        privateEntryPass = password;
        privateEntryPassword = new String(password);
        fullyConfigured = true;

    }


    private synchronized void verifyPublicKeystore() throws Exception {
		if (publicKeyStore == null) {
//			Profile profile = Profile.getProfile();
			KeyStoreAccess ksa = profile.getKeyStoreAccess();
			publicKeyStore = ksa.getKeyStore(KeyStoreType.PUBLIC);
		}
	}

	private synchronized void verifyPrivateKeystore() throws Exception {
        System.out.println("IN verifyPrivateKeystore password string is:" + privateEntryPassword);
        System.out.println("IN verifyPrivateKeystore password char[] is:" + new String(privateEntryPass));
		if (privateKeyStore == null) {
            System.out.println("IN verifyPrivateKeystore get keystore" + privateEntryPassword);

			KeyStoreAccess ksa = profile.getKeyStoreAccess();
			privateKeyStore = ksa.getKeyStore(KeyStoreType.PRIVATE);
		}
	}

	private synchronized void verifySessionKeystore() throws Exception {
		if (sessionKeyStore == null) {
//			Profile profile = Profile.getProfile();
			KeyStoreAccess ksa = profile.getKeyStoreAccess();
			sessionKeyStore = ksa.getKeyStore(KeyStoreType.SESSION);
		}
	}


	public synchronized PublicKey getPublicKey(String entityName) throws KeyNotFound {
		Certificate ct;

		try {
			verifyPublicKeystore();
			Enumeration<String> aliases = publicKeyStore.aliases();
			// TODO remove
			System.out.printf("Aliases from keystore %s%n", this.entityAlias);
			while( aliases.hasMoreElements()) {
				System.out.printf("    alias:%s:%n", aliases.nextElement());
			}
			
			ct = publicKeyStore.getCertificate(entityName);
		} catch (KeyStoreException e) {
			System.err.println(" Crypto.getPublicKey cannot find entry: " + entityName);
			e.printStackTrace();
			throw new KeyNotFound(entityName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new KeyNotFound(entityName);
		}
		if (ct == null) {
			System.out.printf("CryptoDefault.getPublicKey failed for %s%n", entityName);
			throw new KeyNotFound(entityName);
		}
		// TODO simplify
		PublicKey kkk = ct.getPublicKey();
		if (kkk == null) {
			System.out.printf("CryptoDefault.getPublicKey pt t failed for %s%n", entityName);
		}
		return kkk;
	}

	public synchronized PrivateKey getPrivateKey() throws Exception {
		PrivateKey pkey;
        
        System.out.println("   in getPrivateKey for entity:" + entityAlias);
		try {
			verifyPrivateKeystore();
            
            Enumeration<String> aliases = privateKeyStore.aliases();
            String alias;
            while (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
                System.out.println("     " + "owner=" + entityAlias + "   alias=" + alias);
            }

            PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) privateKeyStore.getEntry(entityAlias,
					new PasswordProtection(privateEntryPass));

            
			pkey = entry.getPrivateKey();


		} catch (KeyStoreException e) {
			e.printStackTrace();
			throw new KeyNotFound(entityAlias);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new KeyNotFound(entityAlias);
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
			throw new KeyNotFound(entityAlias);
		}
		return pkey;
	}

	public synchronized void crypt(InputStream in, OutputStream out, Cipher cipher) throws IOException,
	GeneralSecurityException
	{
		int blockSize = cipher.getBlockSize();
        
        System.out.println("debug2 --- cypher name||" + cipher.getAlgorithm() + "|| block Size of =" + blockSize);
        
		int outputSize = cipher.getOutputSize(blockSize);
		byte[] inBytes = new byte[blockSize];
		byte[] outBytes = new byte[outputSize];

		int inLength = 0;
		boolean more = true;
        
		while (more )
		{
            
			inLength = in.read(inBytes);
			if (inLength == blockSize)
			{
				int outLength = cipher.update(inBytes, 0, blockSize, outBytes);
				out.write(outBytes, 0, outLength);
//                System.out.println("crypt step " + debugstop + " inLength=" + inLength + " outlength=" + outLength);
			}
			else more = false;
		}
		if (inLength > 0) outBytes = cipher.doFinal(inBytes, 0, inLength);
		else outBytes = cipher.doFinal();
		out.write(outBytes);
	}

    /**
     * Generate a random symmetric encryption key. Such keys are used for the 
     * session encryption and also in the practical implementation of public
     * key encryption. 
     * @return SecretKeySpec
     */
    public SecretKey randomKey() {
        try {
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecureRandom random = new SecureRandom();
            keygen.init(random);
            return keygen.generateKey();
            
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CryptoDefault.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
        
//        // use the current time in millisec to see the random number generator
//        long current = System.currentTimeMillis();
//        // start a random number generate and fill an array of random bytes
//        java.util.Random rand = new java.util.Random(current);
//        byte[] bytes = new byte[16];
//        rand.nextBytes(bytes);
//        // turn the random array into a secret key for AES 128
//        return new SecretKeySpec(bytes, "AES");
    }


    /**
     * Use Sun Java SE 6.0 facilities to Wrap a secret key using the public key of the named entity
     * and return the wrapped key as a byte array - this doesn't work.
     * 
     * @param spec an object that contains an encryption key
     * @return byte[] the wrapped secret key
     */
    public byte[]wrapSecretKeyAlt(Key spec, String name) {
		PublicKey key;
		try {
			key = this.getPublicKey(name);
            System.out.println("length of public key from keystore:" + key.getEncoded().length);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.WRAP_MODE, key);
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.INFO,
                    "wrap cipher block size " + cipher.getBlockSize());
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.INFO,
                    "wrap cipher output size pre wrap" + cipher.getOutputSize(0));
			byte[] wrappedKey = cipher.wrap(spec);
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.INFO,
                    "wrap cipher output size post wrap" + cipher.getOutputSize(0));
            return wrappedKey;
        }
		catch (NoSuchAlgorithmException e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not load RSA cipher from library.", e);
		}
        catch (KeyNotFound e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not find key for: " + name, e);
            
        }
        catch (Exception e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey exceptional exception " + name, e);
            
        }
        return null;
        
    }
    
    /**
     * Use Sun Java SE 6.0 facilities to UnWrap a secret key contained in a byte array using the private key
     * for this entity and return the SecretKeySpec  - this doesn't work.
     * 
     * @param wrappedKey a byte array containing the wrapped key object that contains an encryption key
     * @return SecretKeySpec the secret key
     */
    public Key unwrapSecretKeyAlt(byte[] wrappedKey) {
		Key key;
		try {
			key = this.getPrivateKey();
			// unwrap with RSA private key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.UNWRAP_MODE, key);
			Key tkey = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

            return key;
        }
		catch (NoSuchAlgorithmException e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not load RSA cipher from library.", e);
		}
        catch (KeyNotFound e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not find private key for: " + this.entityAlias, e);
            
        }
        catch (Exception e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey exceptional exception " + this.entityAlias, e);
            
        }
        return null;
        
    }
 
    

    /**
     * Wrap a secret key using the public key of the named entity
     * and return the wrapped key as a byte array.
     * 
     * This custom code does not use the wrap and unwrap functions of the
     * Java SE 6.0 library. The reason is that the official code does not work
     * properly with keypairs produced by keytool and managed by the KeyStore code.
     * It is simpler to replace wrap and unwrap than to replace the keytool and/or
     * KeyStore capabilities. Unfortunately, however, the code here is a little 
     * weird. It works - but it would be hard to prove why. Beware of future 
     * changes in the Java SE libraries!
     * 
     * @param spec an object that contains an encryption key
     * @return byte[] the wrapped secret key
     */
    public byte[]wrapSecretKey(Key spec, String name) {
		PublicKey key;
		try {
			key = this.getPublicKey(name);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, key);
            
            int blocksize = cipher.getBlockSize();
            byte[] keyBytes = spec.getEncoded();
            int outputSize = cipher.getOutputSize(blocksize);
            byte[]  outBytes = new byte[outputSize]; 
            
            outBytes = cipher.doFinal(keyBytes);
            return outBytes;
            
        }
		catch (NoSuchAlgorithmException e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not load RSA cipher from library.", e);
		}
        catch (KeyNotFound e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not find key for: " + name, e);
            
        }
        catch (Exception e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey exceptional exception " + name, e);
            
        }
        return null;
        
    }
    
    /**
     * UnWrap a secret key contained in a byte array using the private key
     * for this entity and return the SecretKeySpec
     * 
     * @param wrappedKey a byte array containing the wrapped key object that contains an encryption key
     * @return SecretKeySpec the secret key
     */
    public SecretKey unwrapSecretKey(byte[] wrappedKey) {
		Key key;
		try {
			key = this.getPrivateKey();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, key);
            
            int blocksize = cipher.getBlockSize();
            int outputSize = cipher.getOutputSize(blocksize);
            byte[]  outBytes = new byte[outputSize]; 
            
            outBytes = cipher.doFinal(wrappedKey);
            return new SecretKeySpec(outBytes, "AES");
        }
		catch (NoSuchAlgorithmException e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not load RSA cipher from library.", e);
		}
        catch (KeyNotFound e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey did not find private key for: " + this.entityAlias, e);
            
        }
        catch (Exception e) {
            java.util.logging.Logger.getLogger(CryptoDefault.class.getName()).log(java.util.logging.Level.SEVERE, 
                    "wrapSecretKey exceptional exception " + this.entityAlias, e);
            
        }
        return null;
        
    }
 
    

	/** 
	 * Encrypt clear text from an input stream using Public Key Encryption applying 
	 * the public key of the named destination and placing the encrypted text on
	 * the output stream.
	 * @param name
	 * @param in
	 * @param out
	 * @throws KeyNotFound 
	 * @throws Exception 
	 */
	public synchronized void encryptStream(String name, InputStream in, 
			DataOutputStream out) throws Exception {
		Key key;
		try {
			key = this.getPublicKey(name);
			
			// TODO remove
			System.out.printf("encryptStream is requesting public key for %s%n", name);
			
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			SecureRandom random = new SecureRandom();
			keygen.init(random);
			SecretKey tkey = keygen.generateKey();

			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.WRAP_MODE, key);
			byte[] wrappedKey = cipher.wrap(tkey);
			
			System.out.printf("encryptStream is storing key length %d%n", 
					wrappedKey.length);

			out.writeInt(wrappedKey.length);
			out.write(wrappedKey);

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, tkey);
			crypt(in, out, cipher);
			in.close();
			out.close();
		} 
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}

	}

	/** 
	 * Decrypt the text on the input stream using the current entities private
	 * key and the public key encryption system. 
	 * @param in
	 * @param out
	 * @throws Exception 
	 */
	public synchronized void decryptStream(DataInputStream in, 
			OutputStream out) throws Exception {

		Key key;
		try {
			key = this.getPrivateKey();

			int length = in.readInt();
			System.out.printf("  decryptStream claimed key length %d%n", length);
			byte[] wrappedKey = new byte[length];
			in.read(wrappedKey, 0, length);

			// unwrap with RSA private key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.UNWRAP_MODE, key);
			Key tkey = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, tkey);

			crypt(in, out, cipher);
			in.close();
			out.close();
		} 
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}

    /**
     * Sign a text string by computing its encrypted value under this entities
     * private key. The signature is generated by the SHA1withRSA algorithm.
     * 
     * @param text the text to be signed
     * @return array of characters representing a digital signature of the input text.
     */
        public synchronized String signText(String text) {
        System.out.println("sign text as " + entityAlias + " text: " + text);
        
        Provider[] providers = Security.getProviders();
//        for (int i = 0; i < providers.length; ++i) {
//            System.out.println( "provider: " + providers[i].getName() );
//        }
        
        
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        DataOutputStream outD = new DataOutputStream(out); 
        
        try {
            Signature signer = Signature.getInstance("SHA1withRSA"); 

            // get our private key which we use to sign and use it to sign
            PrivateKey key = this.getPrivateKey();
            signer.initSign(key);
            
            // supply the text that will be signed and get the signature byte array
            signer.update(text.getBytes());
            byte[] signatureBytes = signer.sign();
            
            // return the signature converted to a string with hexadecimal representation.
            return byteArrayToHex(signatureBytes);

        } catch (Exception ex) {
            Logger.getLogger(CryptoDefault.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new String();
    }

    /**
     * Check a signature by decrypting it with the public key of the declared name and
     * comparing the decrypted text with the provided original text. The signature
     * is the hexadecimal representation of the digital signature generated by the
     * SHA1withRSA algorithm. 
     * 
     * @param name
     * @param signature
     * @return 
  
     */
    public synchronized boolean verifySignedText(String name, String text, String signature) {
        
        // convert the signature from hexadecimal to bytes
        byte[] inBytes = CryptoEngine.hexStringToByteArray(signature);
        try {
            // prepare a signature object with the public key corresponding to the "name"
            Signature signer = Signature.getInstance("SHA1withRSA"); 
            PublicKey key = this.getPublicKey(name);
            signer.initVerify(key);
            
            // supply the text for verification
            signer.update(text.getBytes());
            
            // verify and return the result
            return signer.verify(inBytes);
            
        } catch (Exception ex) {
            Logger.getLogger(CryptoDefault.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
}
