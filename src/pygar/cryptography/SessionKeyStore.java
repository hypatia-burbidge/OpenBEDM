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

package pygar.cryptography;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import pygar.configuration.ConfigurationError;

/** This interface declares methods to read and write a secret key used to conceal
 * session data. This kind of key is used by team/group members but is never
 * shared with the BAN. Implementations my use symmetric or one-way keys as
 * session keys but symmetric is the simpler to implement. 
 * 
 * @author pbaker
 *
 */
public interface SessionKeyStore {
	/** Put a key in the store at a location specified by the path.
	 * 
         * @param sessionid a key is associated with a session identified here.
	 * @param key the key
	 * @throws ConfigurationError 
	 */
	abstract public  void putKey(String sessionid, byte[] key) throws ConfigurationError;
	
	/** Retrieve a key from a store in the location given by path
         * @param sessionid a key is associated with a session identified here.
	 * @return SecretKeySpec
	 * @throws ConfigurationError 
	 * @throws ConfigurationError
	 */
	abstract public SecretKeySpec getKey( String sessionid) throws ConfigurationError;
    
    /** Create a new, random session key. */
    abstract public SecretKey newRandomKey();
    
    

}
