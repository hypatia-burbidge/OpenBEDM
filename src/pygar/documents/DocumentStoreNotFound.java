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

/** This exception is thrown by any method that expects to find a document Store
 * but fails to find it.
 * @author pbaker
 *
 */
public class DocumentStoreNotFound extends Exception {
	int id;
	String location;
	String doctype;
	/**
	 * This exception is thrown by any method that expects to find a document Store but fails to find it.
	 * @param sessionid The session of the document sought when the exception occurred. 
	 * @param store_location The location of the document sought when the exception occurred.
	 */	
	public DocumentStoreNotFound(int sessionid, String store_location, String dtype) {
		super();
		id = sessionid;
		location = store_location;
		doctype = dtype;
	}

	private static final long serialVersionUID = 1L;};

