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
package pygar.configuration;

import java.util.Map;
// import javax.jms.ConnectionFactory;
import javax.naming.NamingException;
import pygar.cryptography.CryptoEngine;
import pygar.cryptography.SessionKeyStore;
import pygar.communication.PmessageSystem;
import pygar.documents.EncryptedFieldTable;
import pygar.identity_authority.KeyStoreAccess;

/** An application is configured by instantiating various engines from a repertoire of 
 * different implementations. The instances are collected in an object for use in the application.
 * This interface defines the necessary configuration information that must be found in 
 * various configurations. 
 * 
 * This class is intended to be  used as a singleton class. For that reason, the class includes
 * a static member that contains an instance of the class. However, we do not enforce any limitation.
 * For testing it is so convenient to instantiate multiple profiles. If you adopt that practice,
 * then do not call the function, getProfile, which 
 * will always return the singleton if that has been instantiated.
 * 
 * Notes:
 * 1. Configuration information is also required for class KeyStoreAccess
 * and is usually provided in a subclass
 * 
 * 2. the word "Profile" is used in the narrow context of certain computer standards that allow
 * a conforming system to select from different optional sections of a general standard. 
 * 
 * @author pbaker
 *
 */

import net.jcip.annotations.NotThreadSafe;

/**
 * <p>This abstract class defines objects that are used to perform initialization and 
 * configuration for applications. Each application constructs a single object from
 * the class and the class contains data members that specify the configuration.</p>
 * <p>
 * An important design pattern here is the use of data members that are objects which contain
 * factory methods. The purpose of the pattern is that it allows the program to be configured
 * easily with different implementations according the the following straightforward method. 
 * </p><ul>
 * <li>Define the desired functions as part of an interface or abstract class</li>
 * </li><li>Write one or more implementations of the functions as subclasses.</li>
 * <li>Configure the Profile object by initializing the corresponding field with an
 * object provided by the factory method of the desired subclass.</li></ul>
 * <li>Employ the implemented function by referencing the field in the profile object and
 * invoking the method from the object.</li></ul>
 * <p>This configuration method is used for the communication system and cryptography systems
 * as well as others. </p>
 * 
 * The factory method returns an object that is used in the program. Such
 * objects have functions that are 
 * @author pbaker
 *
 */
@NotThreadSafe
abstract public class Profile {
	/** A single instance of this class. 
	 * 
	 */
	public static Profile profile;
	
	/** Configure Cryptography:
	 *    1. The desired version of the cryptographic algorithms.
	 *    2. Session key management
	 */
	// 1.
	public CryptoEngine cryptoEngine;
	// 2.
	public SessionKeyStore sessionKeyStore;
	
	/** Configure Communication:
	 *    1. The desired message delivery system 
	 * 
	 */
	public PmessageSystem messageSystem;
	
	/** Configure Identity Management:
	 *    1. The desired identity key management system 
	 *    2a. Implementation dependent token for key management - may be null.
	 *    2b. Implementation dependent token for key management - may be null.
     * important: 2b is regarded as more secure and should be preferred over 2a
	 */
	// 1.
	public KeyStoreAccess  keystoreObject;
	// 2a. 
	public String keystorePasswordToken;
	// 2b. 
    public char[] vaultPassword;
	
	/** Configure properties of data documents
	 * This table defines the fields in the documents exchanged
	 * in the distributed system. For each field, it specifies how it should
	 * be encrypted. 
	 */
	protected EncryptedFieldTable fieldTable;
	
	public String charSetName;
    
    /** Configure the Enterprise Java environment.
     *  The queueNameTable uses the name of a peer or agent as a key to lookup
     *  the name of the JMS queue that accepts messages bound for that entity.
     */
//    public ConnectionFactory connectionFactory;
    public String connectionFactoryName;
    public Map<String,String> queueNameTable;
	
	/** Name of the entity in the distributed, networked system that is
	 * running the current application. This name is necessary to look up
	 * identities and manage communication between entities. 
	 */
	public String entityAlias;

    /** An optional title that may appear on application GUI windows that
     * may be associated with this entity.
     */
    public String entityTitle = "";
    
	/** 
	 * The profile must contain directory paths for files that the system
	 * will use. We believe two such paths should be sufficient 
	 */
	
	/**
	 * Path to the configuration files.
	 */
	public String configurationDirPath;

	/**
	 * Path to the data files, if any. Alternatively this might store the 
	 * path to the DBMS where data is actually stored.
	 */
	public String dataDirPath;
    
    /** Whoops, it turns out that for testing in configurations that adopt a shared
     * file space for interprocess communication, we need a path to that common
     * file area. 
     */
    public String commonDirPath;

	public EncryptedFieldTable getEncryptedFieldTable() throws ConfigurationError {
		System.out.println("Configurationerror: do not have instance for EncrypteFieldTable");
		throw new ConfigurationError();
		
	}
	
	/** Configuration options are set into a single example of the
	 * class Profile via the subclass constructors. The single example is stored
	 * statically in the Profile class and provided to clients by a call
	 * to the following, getProfile.
	 * 
	 * @return the single instance of the class
	 * @throws ConfigurationError 
	 */
	public static Profile getProfile() throws ConfigurationError {
		if (profile == null) {
			System.err.println("Profile.getProfile() called before initialization.");
			throw new ConfigurationError();
		}
		return (Profile) profile;
	}
    
    
	/** Return the alias name of the entity running the software as appears
	 * in the KeyStore.
	 * 
	 * @return the identity of the entity running the software
	 */	
	public String getThisAlias() {
		return entityAlias;
	}
	
	
	/** Cryptographic operations are performed by methods in an object of type CryptoEngine - an 
	 * exemplar of which is returned by this function.
	 * 
	 * @return object
	 * @throws ConfigurationError 
	 */
	public CryptoEngine getCrypto() throws ConfigurationError {
		System.out.println("Configurationerror: do not have instance for CryptoEngine");
		throw new ConfigurationError();
	}
	
	/** Retrieve the name of the character set used in the documents. N.B. we assume this
	 * character set when we convert from text to the byte arrays that are supplied for
	 * encryption and decryption.
	 * 
	 * @return character_set_name
	 * @throws ConfigurationError 
	 */
	public String getCharsetName() throws ConfigurationError {
		System.out.println("Configurationerror: do not have CharsetName");
		throw new ConfigurationError();
	}
	
	/** Return an object that has a method to obtain a Java KeyStore. 
	 *
	 * @return object
	 * @throws ConfigurationError 
	 */
	public KeyStoreAccess  getKeyStoreAccess() throws ConfigurationError {
		System.out.println("Configurationerror: do not have instance for KeyStoreAccess");
		throw new ConfigurationError();
	}
	
//	/** Return the application properties, i.e., resources
//	 * @return properties
//	 * @throws ConfigurationError 
//	 */
//	public Properties getProperties() throws ConfigurationError {
//		System.out.println("Configurationerror: do not have instance for Properties");
//		throw new ConfigurationError();
//	}
	
    /** Complete the construction of the object for the Java EE version of Demo0G.
     * 
     */
    abstract public void initEE() throws NamingException, ConfigurationError;
 
}
