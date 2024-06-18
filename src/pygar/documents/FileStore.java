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
import java.util.Scanner;

import net.jcip.annotations.NotThreadSafe;

/** This implements document storage through the file system.
 * 
 * The thread safety of this class depends entirely on the thread safety of the file operations 
 * that are called from the operating system. You should assume the class is not thread safe
 * and use it from a dedicated thread. 
 * 
 * @author pbaker
 *
 */
@NotThreadSafe
public class FileStore implements Store {

	public InputStream InputStoreStream(int sessionid, String storeLocation, String doctype)
			throws DocumentStoreNotFound {
		String filename;
		if (sessionid == 0) {
			filename = storeLocation;
		} else {
			filename = storeLocation +  String.valueOf(sessionid) 
			+ "." + doctype;
		}
		try {			
			return new FileInputStream(filename);			
		} catch (Exception e) {
			System.err.printf("Failed to find file %s %n", filename);
			throw new DocumentStoreNotFound(sessionid, filename, doctype);
		}
	}

	public OutputStream OutputStoreStream(int sessionid, String storeLocation, String doctype) 
	throws DocumentStoreNotFound {
		String filename;
		if (sessionid == 0) {
			filename = storeLocation;
		} else {
			filename = storeLocation +  String.valueOf(sessionid) 
			+ "." + doctype;
		}
		try {
			return new FileOutputStream(filename);			
		} catch (Exception e) {
			throw new DocumentStoreNotFound(sessionid, filename, doctype);
		}
		 
	}

	public void clear(int sessionid, String storeLocation, String doctype) 
	throws DocumentStoreNotFound {
		OutputStream out = OutputStoreStream(sessionid, storeLocation, doctype);
		try {
			out.close();
		} catch (IOException e) {
			// This would really be exceptional. Nothing to do but print trace
			e.printStackTrace();
		}

	}

	public boolean exists(int sessionid, String storeLocation, String doctype) {
		String filename;
		if (sessionid == 0) {
			filename = storeLocation;
		} else {
			filename = storeLocation +  String.valueOf(sessionid) 
			+ "." + doctype;
		}
		try {
			FileInputStream fin = new FileInputStream(filename);
			return true;			
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// This main method is used only during development.
		System.out.println("Testing FileStore");
		FileStore fs = new FileStore();
		try {
			InputStream inStream = fs.InputStoreStream(0, 
					"./testdata/demo1/data/clientA/positions/playerA_position.xml",
					"");
			Scanner in = new Scanner(inStream);
			String line;
			while (in.hasNextLine()) {
				line = in.nextLine();
				System.out.println(line);
			}
		} catch (DocumentStoreNotFound e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
