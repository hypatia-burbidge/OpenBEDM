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

import pygar.documents.*;

/** All work on this interface is TBD. There are some suggested methods but no 
 * recommendation about how they might be used **/
public interface OutboundOperations {
	/** Documents for BEDM matching are created in the innermost zone and partially encrypted
	 * by this method. The encryption uses the encryption key associated with the
	 * sessionid. 
	 * @param doc
	 */
	public void partially_encrypt_document(SessionDoc doc);
	
	/** Accept a document from the inner zone.
	 * @param doc
	 */
	public void accept_document(SessionDoc doc);

	/** Outbound documents are encrypted with the public key of the destination 
	 * by this method.
	 * @param doc
	 */
	public void encrypt_for_destination(SessionDoc doc);
	
	/** In this method, outbound documents are signed by encrypting 
	 * with the private key of the source.
	 * @param doc
	 */
	public void sign_for_source(SessionDoc doc);
	
	/** send a document to the next outer zone
	 * 
	 * @param doc
	 */
	public void send_document(SessionDoc doc);
}
