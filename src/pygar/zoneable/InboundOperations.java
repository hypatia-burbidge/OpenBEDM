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

import pygar.documents.SessionDoc;
import pygar.documents.SourceVerifyFail;

/** All work on this interface is TBD. There are some suggested methods but no 
 * recommendations about how they might be used. **/
public interface InboundOperations {
	/** This method pulls the document from the next outer security zone and then
	 * performs the destination_verify operation which may throw exception if it fails
	 * @param doc
	 */
	public void get_document(SessionDoc doc) throws SourceVerifyFail;
	
	/** This method is called when the document is ready to hand off to the next 
	 * inner zone.  It is also responsible for logging the processing of the document in
	 * this zone. 
	 * @param doc
	 */
	public void stage_document(SessionDoc doc);
	
	/** Verify the source of the document by using the source public key to decode
	 * part of the document. At the very least, the source will sign the document by encrypting
	 * a checksum of the encrypted payload. 
	 * @param doc
	 * @throws SourceVerifyFail
	 */
	public void source_verify(SessionDoc doc) throws SourceVerifyFail;

}
