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

package pygar.documents;

/** This exception thrown if the document has been sent to the wrong site. 
* The actual test uses the private key of the entity at this site to verify
* the source's encryption of the document using the public key of this site. 
* @author pbaker
*
*/
public class SourceVerifyFail extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
