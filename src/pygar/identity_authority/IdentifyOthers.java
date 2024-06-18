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

/** This interface describes the component that will provide a public
 * identifer for an agent. In the simplest case, the agent is identified by
 * name and the identifier that will be returned for the name is the PKE
 * public key associated with the name. The interface should hide the details
 * of connecting with trusted identity authorities.
 * 
 * @author pbaker
 *
 */
public interface IdentifyOthers<NameType, KeyType> {
	/** The primary operation of the interface obtains a Public Key
	 * for Public Key Encryption that is associated with the name.
	 * @param name The name or other identifier for the agent. 
	 * @return The public key associated with the name.
	 */
	public KeyType getPublicKey(NameType name);
	
}
