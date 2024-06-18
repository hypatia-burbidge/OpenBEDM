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

import pygar.configuration.ConfigurationError;

/** A class that supports the FieldMatchList interface will allow the specification of
 * a match operation between records in two different negotiation positions. Furthermore,
 * it specifies what parts of the original records are included in the reported match in the
 * event that a match takes place. By default, an input record is not included in the report
 * because some fields are proprietary, for example, fields that represent the price 
 * flexibility of one of the parties. However, fields can be specifically added to the report. 
 * 
 * @author pbaker
 *
 */
public interface FieldMatchList {
	
	/** The MATCHACTION enumeration lists the possible actions as a consequence of a 
	 * successful match between record fields.
	 * <p>
	 * NOCOPY - means no action is taken. A match on the field may be necessary for the
	 * successful match of the record, but the match result for this field is not recorded
	 * in the basis for agreement. </p>
	 * <p>
	 * TRUEONLY - means that a match on the field is reported simply by the literal 
	 * value "true".  </p>
	 * <p>
	 * COPYEQUALS - this option works only for fields that are compared for equality. If
	 * the match on the field is successful, that is the fields are equal, the value of
	 * the field is reported as the result of the match. </p>
	 * <p>
	 * COPYMATCH - this option works for a comparison between an item in one record
	 * and a list of items in the other field. If such a comparison succeeds, the
	 * matching value is reported. The option also works when two lists of items are
	 * compared. If one or more items are found in common between the list, then those
	 * items (the set intersection) are reported.  </p>
	 * <p>
	 * COPYRANGE - this option works for a comparison between two numeric ranges. If the
	 * ranges overlap, then the calculated overlap range will be reported. </p>
	 * <p>
	 * COMPROMISE - this option works for a comparison between two numeric ranges. If the
	 * ranges overlap, then a calculated compromise value will be reported. </p>
	 * 
	 * @author pbaker
	 *
	 */
	public enum MATCHACTION {
		NOCOPY, TRUEONLY, COPYEQUALS, COPYMATCH, COPYRANGE, COMPROMISE
	}
	
	/**
	 * Add to match list a comparison between fields in two records in two documents.
	 * The comparison looks for an equality between field values or an overlap between 
	 * real value ranges or a real and a real range, or an overlap between lists, or list
	 * membership. Where a field name is required, a qualified name is used.
	 * Logically, this qualified name begins with the record name in which the field
	 * is found. As our convention, we supply the record name as a separate parameter. 
	 * The remainder of the field name is supplied by a second parameter.   
	 * <p>
	 * Comparison occur between records in two documents. The parameters in this method 
	 * name two records in different documents. The order of the records in the method
	 * invocation is not important because the matcher will try to match
	 * records in both directions. For example, to match record A with record B in documents 1 and 2
	 * the matcher will first look for A in document 1 and B in document 2. Next it will reverse
	 * the roles and look for A in document 2 and B in document 1. 
	 * </p>
	 * <p>
	 * The order in which the list is constructed can be an optimization hint to the matcher.
	 * The first comparisons should have the least probability of success thereby reducing
	 * the total number of comparisons. However, subclasses may implement the comparison
	 * in an manner.
	 * </p>
	 * @param resultName a name to refer to this match specification, optionally a name
	 * in the EncryptedFieldTable for use in a result record in the basis for agreement. 
	 * @param tbl the encrypted field table that describes the documents
	 * @param required true if these fields must match in order for the records to match
	 * @param report specifies how the result of the comparison is reported in the result
	 * @param tagNameA the XML tag that identifies the first record in the comparison
	 * @param fieldA the field in the first record for the comparison
	 * @param tagNameB the XML tag that identifies the first record in the comparison
	 * @param fieldB the field in the first record for the comparison
	 * @throws ConfigurationError 
	 */
	public void addComparison(String resultName, EncryptedFieldTable tbl, 
			boolean required,
			MATCHACTION report,
			String tagNameA, String fieldA,
			String tagNameB, String fieldB) throws ConfigurationError;
	
	
	/**
	 * Add a field from the input records that will be included in the match report (basis)
	 * when a match occurs. A field must be included or it is omitted from the report
	 * by default. An included field is copied in its entirety even if it has substructure. 
	 * Thus, fields that are not available for comparison because they are structure may 
	 * by copied for the report, if a match is found. 
	 * <p>
	 * The purpose of the field match list is primarily the specification of how to match
	 * fields in records within documents. However, a document may have, in addition to
	 * records, various administrative fields located at the level 1 of the document. It
	 * does not make sense to match these, but it may be desirable to copy them to the 
	 * result of a match. The list of included fields can be extended to handle these
	 * administative fields quite easily. For these fields, specify the name of the field
	 * as the tagName and provide an empty string for both the fieldName and the resultName. 
	 * </p>
	 * @param tagName the name of the record where the field is found
	 * @param fieldName the name of the field
	 * @param resultName the name of the field when it is copied to the basis
	 */
	public void addInclude(String tagName, String fieldName, String resultName);
	

}
