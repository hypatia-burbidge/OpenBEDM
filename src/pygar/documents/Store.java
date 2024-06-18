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


/** This interface contains a method to create a Input or Output stream
 * associated with a document in the system. Three types 
 * of storage are envisioned. First, a persistent store  has an indefinite 
 * lifetime. A session store lasts for the duration of a matching session
 * with the match-maker. A temporary store is a staging area (or perhaps
 * a simple pipe) between two components running in different threads and
 * most often assigned to different security zones. 
 * 
 * The constructors take two arguments identifying the session a 
 * desired storage location, and a document type. If the sessionid is zero, then it is ignored
 * and only the location argument is used. The doctype is a string that is typically
 * the file extension associated with the document. 
 * 
 * N.b. the classes that implement this interface should be implemented in a
 * thread safe manner.
 * 
 * @author pbaker
 *
 */

public interface Store {
	
	enum StoreType{
		temporary,
		session,
		persistent
	}
	/** Stream access to data in a data store 
	 * @param sessionid The identity number of the session to which the data document belongs. 
	 * @param store_location A string identifying where the storage should be located.
	 * @return An object conforming to the Reader interface in java.io
	 * */
	public InputStream InputStoreStream(int sessionid, String store_location, String doctype) 
	throws DocumentStoreNotFound ;

	/** Determine if a data store already exists
	 * @param sessionid The identity number of the session to which the data document belongs. 
	 * @param store_location A string identifying where the storage should be located.
	 * @return true if the Store object exists and false otherwise. 
	 * */
	public boolean exists(int sessionid, String store_location, String doctype);
	
	/** Determine if a data store already exists. If it does, clear the content; otherwise,
	 * do nothing. 
	 * @param sessionid The identity number of the session to which the data document belongs. 
	 * @param store_location A string identifying where the storage should be located.
	 * @throws DocumentStoreNotFound 
	 * */
	public void clear(int sessionid, String store_location, String doctype) throws DocumentStoreNotFound;
	


	/** A writer has access to data in a data store 
	 * @param sessionid The identity number of the session to which the data document belongs. 
	 * @param store_location A string identifying where the storage should be located.
	 * @return An object conforming to the Reader interface in java.io
	 * @throws DocumentStoreNotFound 
	 * */
	public OutputStream OutputStoreStream(int sessionid, String store_location, String doctype) throws DocumentStoreNotFound;
	
}
