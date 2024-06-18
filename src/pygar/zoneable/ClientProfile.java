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

import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;
import pygar.identity_authority.KeyStoreAccess;

/**
 * This subclass extends the Profile with encryption operations that
 * are used only by the innermost zone of client applications and never
 * in the blind-agent server. For the moment
 * it is only a placeholder that does nothing. Needs work!!
 * 
 * @author pbaker
 *
 */

public class ClientProfile extends Profile {
	
	/** The single instance of this class.
	 * 
	 */
	protected static ClientProfile clientProfile;

	/** The keystore for zone10 is used for the session keys that are used
	 * to partially encrypt position documents. N.b. this part out of sync
	 * with the emerging system design. 
	 * 
	 */
	protected static KeyStoreAccess  keystoreZone10;

	public static ClientProfile getProfile() throws ConfigurationError {
		if (clientProfile == null) {
			System.err.println("ClientProfile.getProfile() called before initialization.");
			throw new ConfigurationError();
		}
		return clientProfile;
	}
    
	
    public void initEE() throws ConfigurationError {
        System.err.println("Unimplemented method initEE()");
        throw new ConfigurationError();
    }
    


}
