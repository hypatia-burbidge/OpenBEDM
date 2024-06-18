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
* <p>This package provides facades and interfaces to cryptographic functions
*  used in Pygar software.</p>
 * <p>
 * * The overall system of classes permits the selection of a cryptographic algorithm 
 * through a configuration system. However, this default provider may break for 
 * some choices of algorithms because it frequently assumes the key and block lengths
 * for AES algorithm. At this time, the entire library has been tested only with 
 * complementary set of algorithms: AES, RSA, and SHA-1.
 * </p>
*/

package pygar.cryptography;
