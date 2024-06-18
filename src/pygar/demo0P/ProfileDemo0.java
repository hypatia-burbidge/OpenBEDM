	/****************************************************************CopyrightNotice
	 * Copyright (c) 2010 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *******************************************************************************/

package pygar.demo0P;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import pygar.communication.SimpleMessageSystem;
import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;
import pygar.cryptography.CryptoDefault;
import pygar.cryptography.CryptoEngine;
import pygar.documents.EncryptedFieldTable;
import pygar.documents.EncryptedFieldTable.EFTYPE;
import pygar.identity_authority.KeyStoreAccess;
import pygar.zoneable.KeyStoreFS;

/** This class is used to initialize a configuration for Demo0.
 * 
 * @author pbaker
 *
 */

public class ProfileDemo0 extends Profile {
    
    /** demo0 is limited to one session at a time. the fixed
     * string "defaultsession" identifies this singular session. 
     * @return 
     */
    static public String sessionID = "defaultsession";
    
/* n.b. the order of the initialization of static parameters is
 * important.	
 */
	
	public CryptoEngine getCrypto() {
		return cryptoEngine;
	}
	
	public String getCharsetName() {
		return charSetName;
	}
	
	public EncryptedFieldTable getEncryptedFieldTable() {
		return fieldTable;		
	}

	public KeyStoreAccess getKeyStoreAccess() {
		return keystoreObject;
	}

    
	private EncryptedFieldTable initFieldTable() throws ConfigurationError {
		
		
		// Instantiate a table of field descriptions providing now the desired
		// default for those fields not listed in the table:
		EncryptedFieldTable tbl = new EncryptedFieldTable(EFTYPE.F_CLEAR);
		
		
		// define the rules for partial encryption of the negotiation position
		tbl.addRow(new EncryptedFieldTable.Row(
				"title", EFTYPE.F_STRING, "the title of the text body"));
		tbl.addRow(new EncryptedFieldTable.Row(
				"accession", EFTYPE.F_STRING, "the library locator index of the text body"));
		tbl.addRow(new EncryptedFieldTable.Row(
				"word", EFTYPE.F_STRING, "one of the words in the text"));
		tbl.addRow(new EncryptedFieldTable.Row(
				"count", EFTYPE.F_CLEAR, "the number of time the associated word appears in the text body"));
		tbl.addRow(new EncryptedFieldTable.Row(
				"body", EFTYPE.F_SUPPRESS, "the text body - redacted from negotitations"));
		
		// add rules for the BFA (basis for agreement) which must be 
		// decrypted when delivered to the team members. 
		
		tbl.addRow(new EncryptedFieldTable.Row("accession1", EFTYPE.F_STRING, "accession of the first of the matching records"));
		tbl.addRow(new EncryptedFieldTable.Row("accession2", EFTYPE.F_STRING, "accession of the second of the matching records"));
		tbl.addRow(new EncryptedFieldTable.Row("title1", EFTYPE.F_STRING, "title of the first of the matching records"));
		tbl.addRow(new EncryptedFieldTable.Row("title2", EFTYPE.F_STRING, "title of the second of the matching records"));

		tbl.addRow(new EncryptedFieldTable.Row("similarity", EFTYPE.F_CLEAR, "similarity measure of the two documents"));
		
		return tbl;
	}

	public ProfileDemo0(String entityName, String dirPath) throws ConfigurationError {
		
		// Configure the program
		
		// Record the name given to this running program
		entityAlias = entityName;
		
		// Set directory paths 
		this.configurationDirPath = dirPath + File.separator + "config";
		this.dataDirPath = dirPath + File.separator + "data";
		
		// Setup the Configuration Profile
		/** A single instance of this class. 
		 * 
		 */
		profile = this;
		
		/** Configure Cryptography:
		 *    1. The desired version of the cryptographic algorithms.
		 *    2. Session key management
		 *
		 * Session keys currently have a special storage mechanism  
		 * that is insecure and requires replacement for serious security.
		 */

		// 1. 4/29/2015 - password was null, now try to add fixed
		cryptoEngine = new CryptoDefault(entityName, this, "OpenBEDM");
		// 2. 
                String sessionKeyStorePath = configurationDirPath + 
                        File.separator + entityName;
                sessionKeyStore = new KeyStoreFS(sessionKeyStorePath);

                
		/** Configure Communication:
		 *    1. The desired message delivery system 
		 * 
		 */
		messageSystem = new SimpleMessageSystem(entityName);
		
		/** Configure Identity Management:
		 *    1. The desired identity key management system 
	     *    2. Implementation dependent token for key management - may be null.
		 */
		// 2.
		keystorePasswordToken =  "OpenBEDM";
		// 1.
		String keystorePath =  dirPath + File.separator + "config" + File.separator + entityName;
		keystoreObject = new KeyStoreAccessDemo0(entityName, "JCEKS", keystorePath, keystorePasswordToken);
		
		/** Configure properties of data documents
		 * This table defines the fields in the documents exchanged
		 * in the distributed system. For each field, it specifies how it should
		 * be encrypted. 
		 */
		fieldTable = initFieldTable();

		/** Miscellaneous properties are set by means of old Java properties
		 * file mechanism.
		 */
        /* XXXX
		// Load properties from configuration file
		properties = new Properties();
		
		String propertyFileDir = dirPath + File.separator + "config" + File.separator + entityName; 
		String propertyFile = "demo0.props";
		try {
			this.loadProperties(propertyFileDir, propertyFile);
		} catch (IOException e) {
			System.out.printf("Failed to find profileFile:%s%n", propertyFile);
			e.printStackTrace();
		}
*/
		
		charSetName = "UTF-8";
		
		
	}
	
    public void initEE() throws ConfigurationError {
        System.err.println("Unimplemented method initEE()");
        throw new ConfigurationError();
    }

}
