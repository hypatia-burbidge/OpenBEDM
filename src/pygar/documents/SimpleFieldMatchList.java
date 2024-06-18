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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

import pygar.configuration.ConfigurationError;
import pygar.documents.EncryptedFieldTable.EFTYPE;
import pygar.documents.EncryptedFieldTable.Row;

/** This class supports the FieldMatchList interface and accepts
 * specifications of a match operation between records in two different negotiation positions. Furthermore,
 * it specifies what parts of the original records are included in the reported match in the
 * event that a match takes place. By default, an input record is not included in the report
 * because some fields are proprietary, for example, fields that represent the price 
 * flexibility of one of the parties. However, fields can be specifically added to the report.
 * <p>
 * This class is restricted to use with simple documents. In an XML document, there will be
 * a record tag that identifies a record for a comparison operation. Within the record,
 * there are attributes with tags. The simple format does not support comparison on 
 * fields nested more deeply than the attributes contained a the level below the record
 * level. However, structured fields of the records can be copied to a basis of agreement
 * if the record matches based on the attributes. 
 * </p>
 * <p>
 * TBD Thread safety? at the moment I don't see how the class is being used and 
 * am not sure we have protected it from multithreading errors.
 * </p>
 * @author pbaker
 *
 */
@NotThreadSafe
public class SimpleFieldMatchList implements FieldMatchList {
	
	public class MatchItem {
		public String resultName;
		public boolean required;
		public MATCHACTION report;
		public String fieldA;
		public EFTYPE typeA;
		public String fieldB;
		public EFTYPE typeB;
		
		
		public MatchItem( 
				String resultName,
				boolean required,
				MATCHACTION report,
				EncryptedFieldTable ftable,
				String tagNameA, String fieldA,
				String tagNameB, String fieldB) throws ConfigurationError {
			this.resultName = resultName;
			this.required = required;
			this.report = report;
			this.fieldA = fieldA;
			this.fieldB = fieldB;
			
			Row row;
			row = ftable.getRow( tagNameA + "." + fieldA);
			if (row == null) {
				System.err.printf("SimpleFieldMatchList.MatchItem cannot find name %s.%s %n", 
						tagNameA, fieldA);
				throw new ConfigurationError();
			}
			this.typeA = row.ftype;
			
			row = ftable.getRow( tagNameB + "." + fieldB);
			if (row == null) {
				System.err.printf("SimpleFieldMatchList.MatchItem cannot find name %s.%s %n", 
						tagNameB, fieldB);
				throw new ConfigurationError();
			}
			this.typeB = row.ftype;
		}
		
	}
	
	// The two keys are the first and second record names
	@GuardedBy("this")
	HashMap<String, HashMap<String, List<MatchItem>>> lookup;
	
	// The two keys are the record name and the field name
	@GuardedBy("this")
	HashMap<String, HashMap<String, String>> includeTable;
	
	@GuardedBy("this")
	HashSet<String> recordNames;
	
	public SimpleFieldMatchList() {
		lookup = new HashMap<String, HashMap<String, List<MatchItem>>>();
		includeTable = new HashMap<String, HashMap<String, String>>();
		recordNames = new HashSet<String>();
	}

	public synchronized void addComparison(String resultName, EncryptedFieldTable tbl,
			boolean required, MATCHACTION report, String tagNameA,
			String fieldA, String tagNameB, String fieldB) throws ConfigurationError {
		// make sure the record names are in the table
		recordNames.add(tagNameA);
		recordNames.add(tagNameB);
		
		// First determine if we need to add index structure for the record names
		HashMap<String, List<MatchItem>> innerTable;
		List<MatchItem> list;
		if (lookup.containsKey(tagNameA)) {
			innerTable = lookup.get(tagNameA);
			if (innerTable.containsKey(tagNameB)) {
				list = innerTable.get(tagNameB);
			} else {
				list = new LinkedList<MatchItem>();
				innerTable.put(tagNameB, list);
			}
		} else {
			innerTable = new HashMap<String, List<MatchItem>>();
			list = new LinkedList<MatchItem>();
			innerTable.put(tagNameB, list);
			lookup.put(tagNameA, innerTable);
		}
		
		// now add the item
		list.add(new MatchItem(resultName, required, report, tbl, 
				tagNameA, fieldA, tagNameB, fieldB));
		

	}

	public synchronized void addInclude(String tagName, String fieldName, String resultName) {
		HashMap<String, String> innerTable;
		if (includeTable.containsKey(tagName) ) {
			innerTable = includeTable.get(tagName);
		} else {
			innerTable = new HashMap<String, String> ();
		}
		innerTable.put(fieldName, new String(resultName));
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
