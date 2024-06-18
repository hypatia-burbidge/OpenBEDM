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

/** The KeyNotFound exception is thrown when an access to a KeyStore fails to find a key. 
 * @author pbaker
 *
 */
public class KeyNotFound extends Exception {
	public String entity;

	public KeyNotFound(String entityName) {
		entity = entityName;
	}

}
