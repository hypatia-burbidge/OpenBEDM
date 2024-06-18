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

/**
 * This package implements the system component responsible for identities as 
 * represented via public key encryption.
 * <p>The package builds on java.security.KeyStore and uses that class to store
 * keys and certificates. The package adds access methods that are particular
 * to our system. For that reason, we hide a lot of the generality and 
 * complexity of the java.security utilities in order to have a convenient
 * and concise way to express our code. </p>
 * <p>At the present time, the classes in this package are considered and 
 * annotated as @Dangerous because we have not defined anyway to ensure the 
 * security of the facility. In particular, there are passwords running around
 * in clear text. These need to be eliminated in future releases. </p> 
 *
 */

package pygar.identity_authority;

