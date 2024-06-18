/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pygar.zoneable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import pygar.configuration.ConfigurationError;
import pygar.cryptography.CryptoEngine;
import pygar.cryptography.CryptoDefault;
import pygar.cryptography.SessionKeyStore;
import pygar.identity_authority.KeyStoreAccess;
import pygar.zoneable.KeyStoreFS;

/**
 *
 * @author pbaker
 */
public class SessionKeyStoreImpl implements SessionKeyStore {

    /**
     * A key store is protected by encryption using one of several available
     * methods. The default in Java is JKS. We recommend JCEKS.
     */
    public String keystoreJavaType;
    
    private String sessionKeyAlgorithm;

    private String keystorePassword;
    private char[] passwd;
    private String keystoreDir;
    
    private String filename; 

    private KeyStore kstore;

    SessionKeyStoreImpl(String javaKeystoreType, String algorithm, String keystorePath, String keyPass) throws Exception {
        keystoreJavaType = javaKeystoreType;
        sessionKeyAlgorithm = algorithm;
        keystoreDir = keystorePath;
        keystorePassword = keyPass;

        passwd = keystorePassword.toCharArray();
        try {
            kstore = KeyStore.getInstance(keystoreJavaType);

            filename = keystoreDir + File.separator + "session.keystore";
            System.out.println("SessionKeyStoreImpl openning: " + filename);
            FileInputStream fs = new FileInputStream(filename);
            kstore.load(fs, passwd);
            fs.close();
            System.out.println("KeyStore has " + kstore.size() + " entries");
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

    
    SessionKeyStoreImpl() {
    }

    public void putKey(String sessionid, byte[] key) throws ConfigurationError {
        SecretKeySpec sks = new SecretKeySpec(key, sessionKeyAlgorithm);
        System.out.println("in putKey sks algor" + sks.getAlgorithm() + " key " + 
                CryptoDefault.byteArrayToHex(sks.getEncoded()));
        
        SecretKeyEntry ske = new SecretKeyEntry(sks);
    //    System.out.println("  ske format: " + ske.getSecretKey().getFormat());

        
     //   Entry entry = new SecretKeyEntry(new SecretKeySpec(key, sessionKeyAlgorithm));
  
        
//        try {
//            System.out.println("Entry for putKey: " + ske.toString());
//            System.out.println("Before putKey store has entries: " + kstore.size());
//            if (kstore.containsAlias(sessionid)) {
//                System.out.println("Before putKey store contains the sessionid");
//            } else {
//                System.out.println("Before putKey store does not contain: " + sessionid);
//            }
//        } catch (KeyStoreException ex) {
//            throw new ConfigurationError();
//        }

        try {
            kstore.setEntry(sessionid, ske, new KeyStore.PasswordProtection(passwd));
        } catch (KeyStoreException ex) {
            throw new ConfigurationError();
        }
        
//        try {
//            System.out.println("After putKey store has entries: " + kstore.size());
//            if (kstore.containsAlias(sessionid)) {
//                System.out.println("After putKey store contains the sessionid");
//                System.out.println("  using sessionid entry: " + 
//                kstore.getEntry(sessionid, new KeyStore.PasswordProtection(passwd)).toString() );
//            } else {
//                System.out.println("After putKey store does not contain: " + sessionid);
//            }
//        } catch (KeyStoreException ex) {
//            throw new ConfigurationError();
//            
//        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (UnrecoverableEntryException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        FileOutputStream fs;
//        try {
//            fs = new FileOutputStream(filename);
//            kstore.store(fs, passwd);
//            fs.close();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (KeyStoreException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (CertificateException ex) {
//            Logger.getLogger(SessionKeyStoreImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        
//        
//        
//        
    }

   
    public SecretKeySpec getKey(String sessionid) throws ConfigurationError {
        SecretKeySpec ky;
        try {
            System.out.println("  getKey starting with keystore size " + kstore.size() + 
                    " entryname " + sessionid + " passwd " + passwd);
                    
            if (kstore.containsAlias(sessionid)) {
                System.out.println("  getKey ready to retrieve: " + sessionid );
            } else {
                System.out.println("  getKey kstore does not have  " + sessionid + " surprise!");
            }
            
            ky = (SecretKeySpec) kstore.getKey(sessionid, passwd);
            
            return ky;
        } catch (KeyStoreException ex) {
            throw new ConfigurationError();
        } catch (NoSuchAlgorithmException ex) {
            throw new ConfigurationError();
        } catch (UnrecoverableKeyException ex) {
            throw new ConfigurationError();
        }
    }

  
    public SecretKey newRandomKey() {
        KeyGenerator keygen;
        try {
            keygen = KeyGenerator.getInstance("AES");
            SecureRandom random = new SecureRandom();
            keygen.init(random);
            return keygen.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(KeyStoreFS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;    }

}
