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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.jcip.annotations.NotThreadSafe;


import pygar.configuration.ConfigurationError;
import pygar.configuration.DocumentError;
import pygar.documents.EncryptedFieldTable.Row;

/** 
 * The SimpleXmlMatcher looks for matching records in XML documents according to a 
 * specification contained in a FieldMatchList object. The name of this class 
 * roughly describes its properties and limitations. It must work with XML documents
 * provided on an InputStream. It writes a basis for agreement (the results of matching)
 * in an OutputStream. The XML documents have a carefully limited format described as
 * follows:
 * <ol>
 * <li>The outermost tagged block in the input XML document uses the tag "position" - meaning the
 * negotiation position of one of the members of the match group. 
 * <li>The second level of nesting within the &lt;position&gt; block has a tag that identifies
 * the type of record in the position. The matching operation will match a record of one
 * type with a record of another type. 
 * <li>The third level contains blocks that specify attributes. The tag name at level 3 is
 * the name of the field. The body of the block is its value. Attributes of this type can
 * be matched by the matcher.
 * <li>The third level might also contain a block that has substructure at nesting level 4 or
 * higher. Such blocks cannot be matched by the matcher. However, when a successful match occurs,
 * the FieldMatchList can specify that information in blocks with this property can be 
 * copied without change into the resulting basis. <b>However, this feature is not
 * currently supported by this class!</b>
 * </ol>
 * <p>Internally, the class operates fully in-memory. It creates and then sorts lists of 
 * field values for rapid evaluation of the match criteria. 
 * </p>
 * <p><b>This module is under active development.</b> It is incomplete and the parts that work
 * need to be re-engineered to sort as efficiently as possible. The matching part remains to 
 * be written. Also, thread safety has not been considered.</p>
 * 
 * @author pbaker
 *
 */
@NotThreadSafe
public class SimpleXmlMatcher implements Matcher<InputStream, OutputStream> {
	
	public class FieldHolder implements Comparable<FieldHolder> {
		int recordNum;
		Comparable<?> sortValue;
		String stringValue;
		
		public FieldHolder(int num, Comparable<?>value, String sv) {
			this.recordNum = num;
			this.sortValue = value;
			this.stringValue = sv;
		}

		public int compareTo(FieldHolder x) {
			// TODO
			// works fine but it is ridiculous to perform all this casting deeply nested
			// in the sorting procedures. We must figure out how to segregate by field name
			// and then directly invoke sort with the appropriate Comparator object. TBD for
			// December. 
			
			// can't compare if sort value is null
			if ( x.sortValue == null || this.sortValue == null) {
				return 0;
			}
			
			try {
				String s1 = (String)this.sortValue;
				String s2 = (String)x.sortValue;
				return s1.compareTo(s2);
			} catch (Exception e) {
				try {
					Double x1 = (Double)this.sortValue;
					Double x2 = (Double)x.sortValue;
					return x1.compareTo(x2);
				} catch (Exception e2) {
					try {
						ComparableRange f1 = (ComparableRange)this.sortValue;
						ComparableRange f2 = (ComparableRange)x.sortValue;
						return f1.compareTo(f2);

					} catch (Exception e3){
						// can't compare these.
						return 0;
					}
				}
			}
		}


	}
	
	public class DocLevelInclude {
		// keep the number of the member who submitted the document
		int memberNum;
		// keep the (tagname, tagbody)
		LinkedList< Pair<String, String> > includes;
		
		public DocLevelInclude(int n) {
			memberNum = n;
			includes = new LinkedList< Pair<String, String>>();
		}
	}
	
	public class MemberData {
		// fields is a map keyed on the record name followed by a second
		// key on the field name. The map holds a list of field values.
		HashMap<String, HashMap<String, List<FieldHolder>>> fields;
		
		public MemberData() {
			fields = new HashMap<String, HashMap<String, List<FieldHolder>>>();
		}
	}

	private void sortData(MemberData md) {
		// TODO Auto-generated method stub
		Set<String> records = md.fields.keySet();
		Iterator<String> recIter = records.iterator();
		String recName;
		System.out.println("member data");
		while (recIter.hasNext()) {
			recName = recIter.next();
			System.out.printf("Record Name: %s %n", recName);
			Set<String> fields = md.fields.get(recName).keySet();
			Iterator<String> fIter = fields.iterator();
			String fieldname;
			while ( fIter.hasNext()) {
				fieldname = fIter.next();
				System.out.printf("     field: %s with %d values %n", 
						fieldname, md.fields.get(recName).get(fieldname).size());
				// now sort the list
				List<FieldHolder> ll = md.fields.get(recName).get(fieldname);
				Collections.sort(ll);
				 
//				Iterator<FieldHolder> lfIter = md.fields.get(recName).get(fieldname).iterator();
				Iterator<FieldHolder> lfIter = ll.iterator();
				while (lfIter.hasNext()) {
					FieldHolder fh = lfIter.next();
					System.out.printf("             %s%n", fh.stringValue);
				}
			}
		}
		
		
	}


	public SimpleXmlMatcher(EncryptedFieldTable table, SimpleFieldMatchList ml) {
		isOpen = false;
		this.table = table;
		this.matchList = ml;
	}
	
	EncryptedFieldTable table;
	SimpleFieldMatchList matchList;
	int sizeMatchGroup;
	boolean inclusiveGroup;
	boolean isOpen;
	// TODO
	
	String[] memberNames;
	DocLevelInclude[] docIncludes;
	MemberData[] memberData;

	public void open(int nMembers, boolean inclusive) {
		sizeMatchGroup = nMembers;
		inclusiveGroup = inclusive;
		memberNames = new String[sizeMatchGroup];
		docIncludes = new DocLevelInclude[sizeMatchGroup];
		memberData = new MemberData[sizeMatchGroup];
		
		for (int i = 0; i < sizeMatchGroup; i++) {
			memberNames[i] = null;
			docIncludes[i] = new DocLevelInclude(i);
			memberData[i] = new MemberData();
		}
		isOpen = true;
		
	}
	
	private void add_fieldvalue(MemberData md, int recordNum, Comparable<?> sortval, 
			String recordName, String fieldName, String fieldValue) {
		// TODO
		System.out.printf("add_fieldvalue %d, %s, %s, %s %n", recordNum, recordName, fieldName, fieldValue);
		HashMap<String, List<FieldHolder>> fh;
		if ( !md.fields.containsKey(recordName))  {
			md.fields.put(recordName, new HashMap<String, List<FieldHolder>>());
		}
		fh = md.fields.get(recordName);
		if ( ! fh.containsKey(fieldName)) {
			fh.put(fieldName, new LinkedList<FieldHolder>());		
		}
		List<FieldHolder> ph;
		ph = fh.get(fieldName);
		ph.add( new FieldHolder(recordNum, sortval, fieldValue ));
		
	}
	
	/** The ComparableRange class describes ranges of real value and introduces
	 * the idea of comparison based on overlap. Two ranges compare as "equal"
	 * if they overlap. Otherwise we return the order of the ranges along the
	 * real line. <b>Note that this interpretation of equality is different than
	 * one expects for scalar values.</b>
	 * @param low
	 * @param high
	 */
	public class ComparableRange implements Comparable<ComparableRange> {
		
		double xlow;
		double xhigh;
		
		ComparableRange(double low, double high) {
			xlow = low;
			xhigh = high;
		}

		public int compareTo(ComparableRange o) {
			if (o.xhigh < this.xlow) {
				// o is completely below this
				return 1;
			} else if ( o.xlow > this.xhigh) {
				// o is completely above this
				return -1;
			}
			// this and o overlap and we interpret that as "equality"
			return 0;
		}
		
	}
	
	public Comparable<?> returnComparable(EncryptedFieldTable.EFTYPE ftype, String value) {
		/*
		 * 	public enum EFTYPE {
		F_STRING, F_NAME_STRING, F_LIST, F_INT_EXACT, 
		F_REAL, F_REAL_RANGE,
		F_INT, F_INT_RANGE,
		F_CLEAR, F_SUPPRESS
	}

		 */
		switch (ftype) {
		case F_STRING:
		case F_NAME_STRING:
			return value;
			
		case F_INT:
		case F_INT_EXACT:
		case F_REAL:
			return new Double(value);
			
		case F_LIST:
		case F_CLEAR:
		case F_SUPPRESS:
			// cannot sort, return null
			return null;
		case F_INT_RANGE:
		case F_REAL_RANGE:
			int comma = value.indexOf(",");
			ComparableRange range;
			if (comma > 0) {
				range = new ComparableRange( new Double(value.substring(0, comma)),
						new Double(value.substring( comma + 1)));
				return range;
			} else {
				// there is only one value so we return a range of zero extent around value
				Double x = new Double(value);
				range = new ComparableRange(x, x);
				return range;
			}
			// TODO
		}
			
		return null;
	}

	public void load(int memberNumber, java.lang.String memberName,
			InputStream position) throws ConfigurationError, DocumentError {
		if (! isOpen) {
			System.err.println("Error - open SimpleXmlMatcher before calling load");
			throw new ConfigurationError();
		}
		/* 1. for this memberNumber, prepare a hash table keyed on field name that accesses
		 * a set of value lists.
		 * 2. read each record and store every field in a list that has (record number, value)
		 * 3. When all records are added, sort the lists by value
		 */
		memberNames[memberNumber] = memberName;
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser;
		try {
			parser = factory.createXMLStreamReader(position);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new DocumentError();
		}
		int event;
		int recordNum = 0;
		int depth = 0;
		String text;
		String text2;
		String element = "";
		String currentRecordTag;
		Pair<String,String> openInclude = null;
		String recordOfInterest = null;
		Row row;
		Stack<String> xmlPath = new Stack<String>();
		Stack<Row> currentRow = new Stack<Row>();
		
		String currentPath = new String();
		xmlPath.push(currentPath);
		row = new Row("", table.EFTYPE_DEFAULT, "");
		currentRow.push(row);
		boolean suppressp;
		
		try {
			while (parser.hasNext()) {
				event = parser.next();
				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					depth++;
					xmlPath.push(currentPath);
					element = parser.getLocalName();
					StringBuilder sb = new StringBuilder(currentPath);
					if (currentPath.length() > 0) {
						sb.append(".");
					}
					sb.append(element);
					currentPath = sb.toString();
					System.out.printf("depth %d, start element %s path %s\n", depth, parser.getLocalName(), currentPath);
					// check outer layer
					if (depth == 1 && ! element.endsWith("position")) {
						System.err.printf("Document error, level one tag is %s not 'position' %n", element);
						throw new DocumentError();
					}
					// check for level 2
					if (depth == 2) {
						// we are starting a record - do we recognize it?
						if (matchList.includeTable.containsKey(element)) {
							// this is a document level include - start the entry - add body later
							openInclude = new Pair<String,String>(element, "");
							docIncludes[memberNumber].includes.add(openInclude);
							// we are not interested in matching this record
							recordOfInterest = null;
						} else if (matchList.recordNames.contains(element)) {
							// this is a record we are interested in matching
							recordOfInterest = element;
							recordNum++;
						} else {
							// level 2 block is not recognized and therefore will not be loaded
						}
					}
					// find field in table if possible
					row = table.getRow(currentPath);
					
					if (row == null) {
						row = table.defaultRow;
					}
					currentRow.add(row);
					
					break;
					
				case XMLStreamConstants.END_ELEMENT:				
					currentPath = xmlPath.pop();
					currentRow.pop();
					depth--;
					if (depth == 1) {
						recordOfInterest = null;
						openInclude = null;
					}
					break;

				case XMLStreamConstants.CHARACTERS:
					text = parser.getText();
					row = currentRow.peek();
					// in this version, we only process fields at level 3
					if (depth == 3 ) {
						// don't process whitespace
						text2 = text.trim();
						if (text2.length() > 0) {
							if (recordOfInterest != null ) {
								add_fieldvalue(memberData[memberNumber], recordNum, 
										returnComparable(row.ftype, text2), recordOfInterest, element, text2);

							} else if (openInclude != null) {
								System.out.printf("overlooked include %n ");
							} else {
								System.out.printf("do nothing with %s in %s and %s %n", text2, recordOfInterest, element);
							}

						}
					}

					// TODO
					break;
				default:
					break;
				}
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new DocumentError();
		}
		
		sortData(memberData[memberNumber]);
		
	}

	public void compare() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * This class does not implement the method for getBasis. It will throw and 
	 * exception if called. Users of this class are expected to use the method: writeBasis().
	 */
	public OutputStream getBasis(int memberNumber) throws ConfigurationError {
		System.err.println("SimpleMatcherXml.getBasis was called. Do not use, use writeBasis()");
		throw new ConfigurationError();
	}
	
	/**
	 * Return an XML document containing the basis of agreement determined for the memberNumber
	 * placing the document on the OutputStream.
	 * @param memberNumber number of the member who receives the basis
	 * @param out stream where the basis document is written 
	 */
	public void writeBasis(int memberNumber, OutputStream out) {
		// TODO Auto-generated method stub
	}
	
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
	// internal classes need for sorting and matching
	
	private SimpleXmlMatcher() {
		
	}
	

	public boolean test1() {
		boolean result = true;
		LinkedList<FieldHolder> li = new LinkedList<FieldHolder>();
		li.add(new FieldHolder(0, 2.0, "2"));
		li.add(new FieldHolder(0, 1.0, "1"));
		li.add(new FieldHolder(0, 4.0, "4"));
		li.add(new FieldHolder(0, 3.0, "3"));
		
		Collections.sort(li);
		Double prev = null;
		Double current;
		
		Iterator<FieldHolder> iter = li.iterator();
		while (iter.hasNext()) {
			current = (Double)iter.next().sortValue;
			if (prev != null) {
				
				result = result && ( current >= prev );
			}
			prev = current;
		}
		System.out.println("done test1");
		return result;
				
	}
	public boolean test2() {
		boolean result = true;
		
		LinkedList<FieldHolder> li = new LinkedList<FieldHolder>();
		li.add(new FieldHolder(0, "2", "2"));
		li.add(new FieldHolder(0, "1", "1"));
		li.add(new FieldHolder(0, "4", "4"));
		li.add(new FieldHolder(0, "3", "3"));
		
		Collections.sort(li);
		String prev = null;
		String current;
		
		Iterator<FieldHolder> iter = li.iterator();
		while (iter.hasNext()) {
			current = (String)iter.next().sortValue;
			if (prev != null) {
				result = result && ( current.compareTo(prev) >= 0 );
			}
			prev = current;
		}
		System.out.println("done test2");
		return result;
				
	}
	
	public boolean test3() {
		boolean result = true;
		
		LinkedList<FieldHolder> li = new LinkedList<FieldHolder>();
		li.add(new FieldHolder(0, new ComparableRange(5.0, 10.0), "5-10"));
		li.add(new FieldHolder(0, new ComparableRange(15.0, 20.0), "15-20"));
		li.add(new FieldHolder(0, new ComparableRange(7.0, 12.0), "7-12"));
		li.add(new FieldHolder(0, new ComparableRange(1.0, 3.0), "1-3"));
		
		Collections.sort(li);

		String prev = null;
		String current;
		ComparableRange prevRange = null;
		ComparableRange curRange;
		FieldHolder fh;
		
		Iterator<FieldHolder> iter = li.iterator();
		while (iter.hasNext()) {
			fh = iter.next();
			curRange = (ComparableRange)fh.sortValue;
			current = fh.stringValue;
			System.out.printf("sorted range: %f, %f %n", curRange.xlow, curRange.xhigh);
//			System.out.println(current);
			if (prev != null) {
				result = result && ( curRange.compareTo(prevRange) >= 0 );
			}
			prev = current;
			prevRange = curRange;
		}
		System.out.println("done test3");
		return result;
				
	}
	
	
}
