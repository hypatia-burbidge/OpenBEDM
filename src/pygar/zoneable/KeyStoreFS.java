/**
 * **************************************************************CopyrightNotice
 * Copyright (c) 2011 WWN Software LLC All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Pygar Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://ectn.typepad.com/pygar/pygar-public-license.html
 *
 * Contributors: Paul Baker, WWN Software LLC
	 ******************************************************************************
 */
package pygar.zoneable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import javax.crypto.spec.SecretKeySpec;

import net.jcip.annotations.NotThreadSafe;

import pygar.configuration.ConfigurationError;
import pygar.cryptography.CryptoEngine;
import pygar.cryptography.SessionKeyStore;

/**
 * This class contains methods to read and write a secret key stored in a file
 * in the file system. We assume that the file is named "sessionkey" and that it
 * is identified by the directory path. Consequently, we are also assuming that
 * different sessions are stored in different file directories. Hence this class
 * is intended for Pygar implementations that use the file system for managing
 * session operations.
 *
 * Thread safety is dependent on the thread safety of the file system.
 *
 * Use only for testing - it does not distinguish session keys for different
 * sessions.
 *
 * @author pbaker
 *
 */
@NotThreadSafe
public class KeyStoreFS implements SessionKeyStore {

    private String sessionKeyAlgorithm;
    private String path;
    
    private KeyStoreFS() {
    }

    public KeyStoreFS(String keyStorePath) {
        path = keyStorePath;
        sessionKeyAlgorithm = "AES";
    }

    public KeyStoreFS(String keyStorePath, String algorithm) {
        path = keyStorePath;
        sessionKeyAlgorithm = algorithm;
    }

    /**
     * Put a key in a file named "sessionkey" in the directory given by path
     *
     * @param path the directory to put the key
     * @param key the key
     * @throws ConfigurationError
     */
    public void putKey(String sessionid, byte[] key) throws ConfigurationError {
        String filename = path + File.separator + "sessionkey";
        System.out.println("KeyStoreFS ready to write " + filename);
        PrintWriter pw;
        try {
            pw = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            System.err.printf("Cannot create file for session key at %s %n", path);
            e.printStackTrace();
            throw new ConfigurationError();
        }
        pw.println(CryptoEngine.byteArrayToHex(key));
        pw.close();

    }

    /**
     * Retrieve a key from a file named "sessionkey" in the directory given by
     * path
     *
     * @param path
     * @return SecretKeySpec
     * @throws ConfigurationError
     */
    public SecretKeySpec getKey(String sessionid) throws ConfigurationError {
        String filename = path + File.separator + "sessionkey";
        System.out.println("KeyStoreFS ready to read " + filename);
        Scanner sc;
        try {
            sc = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            System.err.printf("Cannot open file for session key at %s %n", path);
            e.printStackTrace();
            throw new ConfigurationError();
        }
        String skey = sc.nextLine();
        byte[] keybytes = CryptoEngine.hexStringToByteArray(skey);
        return new SecretKeySpec(keybytes, sessionKeyAlgorithm);
    }

    /**
     * Create a key from a string value.
     *
     * @param s
     * @return SecretKeySpec
     */
    public SecretKeySpec makeKey(String s) {
        byte[] keybytes = CryptoEngine.hexStringToByteArray(s);
        return new SecretKeySpec(keybytes, sessionKeyAlgorithm);

    }

    /**
     * Create a new, random session key.
     */
    public SecretKey newRandomKey() {
        KeyGenerator keygen;
        try {
            keygen = KeyGenerator.getInstance(sessionKeyAlgorithm);
            SecureRandom random = new SecureRandom();
            keygen.init(random);
            return keygen.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(KeyStoreFS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }


}
