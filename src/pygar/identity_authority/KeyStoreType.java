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

/** An enumeration for the types of keys in the system. The architecture specifies separate keystores for each 
 * key type so that the keys can be separated by zones during deployment if desired. In principle then, there are three
 * keystores, one for each type. However, a keystore in Java SE 6 does not hold a session key so for historical
 * reasons, session key stores have their own classes and implementations. The confusion is unfortunate.
 * 
 * @author pbaker
 *
 */
public enum KeyStoreType {
 PUBLIC,
 PRIVATE,
 SESSION
}
