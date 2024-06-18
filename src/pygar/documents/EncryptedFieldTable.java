/****************************************************************CopyrightNotice
 * Copyright (c) 20011 WWN Software LLC 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Pygar Public License v1.0
 * which accompanies this distribution, and is available at
 * http://ectn.typepad.com/pygar/pygar-public-license.html
 *
 * Contributors:
 *    Paul Baker, WWN Software LLC
 *******************************************************************************/

package pygar.documents;

import java.util.ArrayList;
import java.util.HashMap;

import net.jcip.annotations.NotThreadSafe;

import pygar.configuration.ConfigurationError;

/** An EncryptedFieldTable contains a table that lists those fields of an XML document that are
 * subject to partial encryption. Fields are identified by a partially qualified name.
 * <p>The qualified names are assumed to be complete on the
 * right hand side but possibly incomplete on the left. Thus the incomplete name 
 * d.e.f will match a.b.d.e.f or r.d.e.f but d.e.f.g is not matched because the d.e.f 
 * portion of that name is not in the rightmost position. In other words, the partial
 * names identify leaves of the XML document tree, not branches. </p>
 * <p>
 * Each table row contains one partially qualified name, and a type indicator chosen
 * from the EFTYPE enumeration. This type specifies 
 * how that field should be encrypted during the partial encryption of the statement. Partial
 * encryption occurs only in the client, however, the type information is also used 
 * by the broker to guide the matchmaking process.</p>
 * 
 * @author pbaker
 *
 */
@NotThreadSafe
public class EncryptedFieldTable {
	/** The EFTYPE enumeration recites representation types that may be found while  
	 * reading an XML statement. 
	 * <p>The representation has three significant features: </p>
	 * <ol>
	 * <li>the format of a value when written as string in an XML statement and
	 * <li>The classification of the type as exact or real. Exact values are basically
	 * strings and integer numbers, which - when encrypted - are strongly encrypted. 
	 * The real types have a finite precision and when encrypted are weakly encrypted
	 * in a manner that conceals the true value without disrupting the numerical
	 * comparisons between concealed values and 
	 * <li>whether the field is a single value,
	 * a list of values, or a range of values. 
	 * </ol>
	 * <p>
	 * It provides a basic classification to guide the partial
	 * encryption on the client side or the matching of encrypted fields on the match maker
	 * side. 
	 * </p><p>
	 * The F_CLEAR and F_SUPPRESS types govern what happens to fields that do not
	 * have any other encryption type. These two types are necessary because XML documents have management fields that
	 * may be kept unencrypted in order to facilitate the document flow. These
	 * fields should not contain any sensitive information. To designate such a 
	 * field, we mark it as F_CLEAR  and it will pass through without encryption. 
	 * In other cases, the handling of the field depends on the policies established for
	 * the matching engagement. There may be fields that are simply not desired during
	 * the matching and these should be designated by the enum value F_SURPRESS.
	 * A field that is so designated is removed from the document during encryption.
	 * </p><p>
	 * The action taken for any field that is not listed in this table is determined
	 * by the value of FTYPE_DEFAULT. The default depends on the particular application,
	 * but, if there is any doubt, use F_SUPPRESS.
	 * </p><p>
	 * The type of a simple string field is F_STRING. Such a field may also be used
	 * to hold the name of an individual or place. However, many individuals or places
	 * may have aliases. Thus, the matchmaking process may be different for strings
	 * representing names. For this reason, the F_NAME_STRING enum value is provided. 
	 * </p><p>
	 * An F_LIST field is a comma-separated list of strings. Each 
	 * string in the list is encrypted in the same way as F_STRING. 
	 * However, an F_LIST may be verified against the list of available choices. An 
	 * object that implements this interface maintains a list of choices for each field.
	 * </p><p>
	 * The F_INT_EXACT is a number represented as a string value or a Java <em>int</em> or <em>long</em>.
	 * </p><p>
	 * The preceding types: F_STRING, F_NAME_STRING, F_LIST, and F_INT_EXACT are
	 * encrypted using standard encryption producing a hexadecimal representation of 
	 * the output of applying encryption to the string value of the clear text value.
	 * In contrast, the remaining types: F_REAL and F_REAL_RANGE are treated as numbers. 
	 * An F_REAL is converted to a new number using the encryption key and the pair
	 * of numbers defining an F_REAL_RANGE value is converted to a new pair. This type of
	 * encryption is adopted so that range overlap calculations are possible; but,
	 * that means that the encryption is less secure against statistical data mining. 
	 * According to some definitions of encryption, this process does not qualify and
	 * for that reason might be best referred to as concealing the numeric value
	 * with the encryption key. 
	 * </p><p>
	 * The standard recommendation applies: don't use a real number type
	 * like float or double and expect to compare for exact equality. Hence there is no
	 * EFTYPE for exact reals. 
	 * </p><p>
	 * There are also types to be used with integer ranges: F_INT and F_INT_RANGE. It is
	 * possible to compare an F_INT with an F_INT_RANGE to determine whether
	 * the F_INT lies in the range and to determine the overlap of two F_INT_RANGE
	 * values. 
	 * </p>
	 * <p>Both integers and reals provide the following degree of flexibility to handle the 
	 * needs of the matching operations. There may be contexts where either a single value or
	 * a range may appear. If that is possible and allowed, then the field should be labeled
	 * as F_INT_RANGE or F_REAL_RANGE. The terms F_INT and F_REAL imply that the field that is
	 * so named may contain only a single value. 
	 * </p>
	 * @author pbaker
	 *
	 */
	public enum EFTYPE {
		F_STRING, F_NAME_STRING, F_LIST, F_INT_EXACT, 
		F_REAL, F_REAL_RANGE,
		F_INT, F_INT_RANGE,
		F_CLEAR, F_SUPPRESS
	}

	/**
	 * A RealRange is just a pair of doubles indicating the lower and upper values
	 * of a range of real values.
	 * @author pbaker
	 *
	 */
	static public class RealRange {
		public double lower;
		public double upper;
		
		public RealRange(double xmin, double xmax) {
			this.lower = xmin;
			this.upper = xmax;
		}
		
	}
	/** The Row is the item stored in the table. The first column, "name", is
	 * the index or key to the table. The use of the vmin and vmax fields is
	 * described in the documentation for class ConcealReal 
	 * @see pygar.zoneable.FieldCryptoXmlTxt0.ConcealReal 
	 * @author pbaker
	 *
	 */
	static public class Row {
		/** The name of the field. If XML is used, the name of the XML tag */
		public String name;
		/** The type of the field in the Pygar partial encryption system */
		public EFTYPE ftype;
		/** An optional comment about the field */
		public String remark;
		/** The following is true if vmin and vmax are set and false otherwise */
		public boolean rangeSet;
		/** For floating point fields, specify the estimated minimum value of the field. */
		public double vmin; 
		/** For floating point fields, specify the estimated maximum value of the field. */
		public double vmax;
		
		/** The Row contains two required fields: name and ftype. The choiceList
		 * can be null and the remark can be an empty string.
		 * @param name the partially qualified name of the XML field
		 * @param ftype the type of encryption to be applied to the field
		 * @param remark an explanation of the field in the XML document
		 */
		public Row (String name, EFTYPE ftype, String remark) {
			this.name = name;
			this.ftype = ftype;
			this.remark = remark;
			this.rangeSet = false;
		}

		public Row (String name, EFTYPE ftype, String remark, double vmin, double vmax) {
			this.name = name;
			this.ftype = ftype;
			this.remark = remark;
			this.rangeSet = true;
			this.vmin = vmin;
			this.vmax = vmax;
		}
	}
	
	public EFTYPE EFTYPE_DEFAULT;
	
	public Row defaultRow;
	
	// The table is the main functional data structure of the object
	private HashMap<String, Row> table;
	// We keep a separate list of the entry names for efficiency
	private ArrayList<String> rowList;
	
	// The nameTable maps a fully qualified XML name to a matching name in 
	// the system. The match is on the right side. The fully qualified name
	// must contain the name in the "table" as its right most portion. 
	// The nameTable contains a mapping for each fully qualified name that
	// is seen so that the lookup process is faster when the name is seen
	// again. Note that documents may have XML fields that are not mentioned
	// in the exchange standard. For these fields the default action is F_SUPPRESS and the
	// fully qualified name of such a field will map to a null value in the
	// nameTable. 
	private HashMap<String, String> nameTable;
	
	public EncryptedFieldTable(EFTYPE f_default) {
		table = new HashMap<String, Row>();
		rowList = new ArrayList<String>();
		nameTable = new HashMap<String, String>();
		
		EFTYPE_DEFAULT = f_default;
		defaultRow = new Row("", EFTYPE_DEFAULT, "");
	}
	
	// the partially qualified name of a new row must not match
	// any name that is already in the table. 
	private void nameVerify(String name) throws ConfigurationError {
		for (String oldname : nameTable.keySet()) {
			if (oldname.length() > name.length() ) {
				if ( oldname.endsWith(name)) {
					throw new ConfigurationError();
				} else if ( name.endsWith(oldname))
					throw new ConfigurationError();
			}
		}		
	}



	/** Extend the table with a new row. The name of the new row
	 * must not match the name of an existing row; otherwise, an
	 * exception is thrown.
	 * 
	 * @param row
	 * @throws pygar.configuration.ConfigurationError
	 */
	public void addRow( Row row) throws ConfigurationError {
		nameVerify(row.name);
		table.put(row.name, row);
		rowList.add(row.name);
	}
	
	public String lookupName( String name) {
		// look for the fully qualified name in the table by matching on the right side. if found,
		// return the name in the table. Otherwise return a null string;
		
		// first look for known name correspondences
		if (nameTable.containsKey(name)) {
			return nameTable.get(name);
		}
		
		// next look for match in the table
		// if there is more than one match, the most specific match is used
		String mostSpecific = "";
		
		for (String keyname : rowList) {
			if ( name.endsWith(keyname)) {
				// got a match on right side
				if (keyname.length() > mostSpecific.length()) {
					mostSpecific = keyname;
				}
			}
		}
		if (mostSpecific.length() > 0) {
			// found the name
			nameTable.put(name, mostSpecific);
			return mostSpecific;			
		}

		// failed to find the name entirely
		return null;
	}
	
	public Row getRow(String name) {
		String tableName = lookupName(name);
		if (tableName != null) {
			return table.get(tableName);
		}
		// failed to find the row, return a null
		return null;
	}


}
