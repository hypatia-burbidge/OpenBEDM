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
import java.io.*;

/** An object implementing the SessionDoc interface will contain essential annotation needed 
 * to route documents used in a BEDM session. The document is found in a "payload" that has been  
 * subjected to various degrees of encryption. The internal state of that payload with respect to 
 * encryption inferred from where the document is found in the inbound or 
 * the outbound document stream but implementations may add annotation to make this explicit.  
 * 
 * *** This interface is incomplete, we need to specify ways to get at the payload.
 * 
 * @author pbaker
 *
 */

public interface SessionDoc {
	enum Direction {
		inbound,
		outbound
	}
	/**
	 * Obtain the session id associated with the object
	 * @return id the session id
	 */
	public int getSessionid();
	/**
	 * Set the session id associated with the object
	 * @param id The session id.
	 */
	public void setSessionid(int id);
	
	/**
	 * Obtain the name of the document source
	 * @return String  
	 */
	public String getSourceName();
	/**
	 * Set the Source Name
	 * @param source the source name
	 */
	public void setSourceName(String source);
	
	/**
	 * Obtain the name of the document source
	 * @return String  
	 */
	public String getDestinationName();
	/**
	 * Set the Destination Name
	 * @param destination the destination name
	 */
	public void setDestinationName(String destination);
	
	/**
	 * Return a stream to read from the document.
	 * @param doc The access handle to the document
	 */
	public InputStream getReadStream(Store doc);
	
	/**
	 * Return a stream to write into a new, empty document. 
	 * Creates an object with a Store interface.
	 * @param sessionid The identity number of the session to which the data document belongs. 
	 * @param store_location A string identifying where the storage should be located.
	 * @return A stream for writing document content
	 * */
	public OutputStream getWriteStream(int sessionid, String store_location);
	
	
	/**
	 * Return a stream to write into at the end of a document.
	 * @param doc The access handle to the document 
	 * */
	public OutputStream getAppendStream(Store doc);
	
	
	

	

}
