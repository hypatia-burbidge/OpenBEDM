package pygar.demo0P;

//public class KeyStoreAccessDemo0 {
//
//}
/**
 * **************************************************************CopyrightNotice
 * Copyright (c) 2015 WWN Software LLC All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Pygar Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://ectn.typepad.com/pygar/pygar-public-license.html
 *
 * Contributors: Paul Baker, WWN Software LLC
 ******************************************************************************
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import pygar.configuration.Dangerous;
import pygar.configuration.RestrictedImport;
import pygar.cryptography.CryptoException;
import pygar.identity_authority.KeyStoreAccess;
import pygar.identity_authority.KeyStoreType;

/**
 * The KeyStoreAccessDemo0 class implements the KeyStoreAccess abstract class in
 * a manner sufficient for the demonstration. However, there are issues. It
 * assumes the special directory structure used in the demonstration. Also, note
 * that it must be initialized with the name of the current application: that
 * is,the alias of the current user in the keystore. Finally, it needs a
 * password which is passed upon initialization. It does not provide a method to
 * securely obtain this password.
 * <p>
 * Finally, the Java JRE does not appear to implement keystores for SecretKeys,
 * therefore, we cannot get the session keystore option to work. For now, we
 * need a workaround in another class until this issue is resolved.
 * </p>
 * <p>
 * This package is also used in Demo1 versions.
 * </p>
 *
 * @author pbaker
 * 
*/
@Dangerous
@RestrictedImport
public class KeyStoreAccessDemo0 extends KeyStoreAccess {

    private String keystorePassword;
    private String keystoreDir;

    public KeyStoreAccessDemo0(String entityName, String javaKeystoreType, String keystorePath,
            String keystoreToken) {
        currentEntity = entityName;
        keystoreJavaType = javaKeystoreType;
        keystoreDir = keystorePath;
        keystorePassword = keystoreToken;
    }

    /**
     * Return the KeyStore for the entity that is running the current
     * application. Note: the option for a session keystore doesn't work because
     * the underlying JCA method doesn't work in any fashion that I've
     * discovered. That may be a limitation of the JKS keystore. We should test
     * JCEKS.
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
                if (keystoreJavaType.equalsIgnoreCase("jks")) {
                    System.err.println("Class KeyStoreAccessDemo0 can not implement a session keystore.");
                    throw new CryptoException();
                }
                // TODO
                break;
            default:
                storeName = "";
        }

        char[] passwd = keystorePassword.toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance(keystoreJavaType);

            String filename = keystoreDir + File.separator + storeName;
            System.out.println("KeyStoreAccessDemo0 openning: " + filename);
            FileInputStream fs = new FileInputStream(filename);
            ks.load(fs, passwd);
            System.out.println("KeyStore has " + ks.size() + " entries");
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

}
