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

package pygar.identity_authority;

import java.security.KeyStore;

public abstract class KeyStoreAccess {
	/** The name of the current Entity that is running the software.
	 *	
	 */
	public String currentEntity;
        
        /** A key store is protected by encryption using one of several
         * available methods. The default in Java is JKS. We recommend
         * JCEKS. 
         */
        public String keystoreJavaType;
	
	
	/** Return the KeyStore for the entity that is running the current application.
	 * 
	 * @param kst The type of the keystore desired: 
	 * @return the keystore
	 */
	abstract public KeyStore getKeyStore(KeyStoreType kst) throws Exception;
	
	
	
	
	

}
