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


/** This interface describes the component that will retrieve the
 * private key for an agent. In the simplest case, the agent is identified by
 * name and the identifier that will be returned for the name is the PKE
 * private key associated with the name. The interface should hide the details
 * of connecting with trusted identity authorities. At any site in the system,
 * it will be possible to return keys for the local aliases but not for the 
 * names associated with other sites. 
 * 
 * @author pbaker
 *
 */
public interface IdentifySelf<NameType, KeyType> {
	/** The primary operation of the interface obtains a Private Key
	 * for Public Key Encryption that is associated with the name.
	 * Note that this must be an alias for the current user
	 * otherwise the method must return null.
	 * @param name The name or other identifier for the agent. 
	 * @return The private key associated with the name.
	 */
	public KeyType getPrivateKey(NameType name);
	
}

