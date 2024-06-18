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

import pygar.documents.EncryptedFieldTable;

/** This data type is used for values in a negotiation position and 
 * includes their unencrypted <em>value</em>, their <em>fieldName</em> in the XML schema,
 * and the type with respect to the encryption system: <em>ftype</em>.
 * @author pbaker
 *
 * @param <ValueType>
 */
public abstract class EncryptedClearValue<ValueType> {
	
	public String fieldName;
	
	EncryptedFieldTable.EFTYPE ftype;
	
	public ValueType value;
	
	public EncryptedClearValue(String fieldName, EncryptedFieldTable.EFTYPE ftype, ValueType value) {
		this.fieldName = fieldName;
		this.ftype = ftype;
		this.value = value;
	}
	/** Encypt the value field of the object and return it as a String
	 * Note: the function depends on the successful initialization of
	 * static member profile in class pygar.configuration.Profile. 
	 * @return encrypted string
	 */
	public abstract String encrypt();

	/** Decrypt the encrypted value in the string according to the specification
	 * 
	 * @param encryptedValue
	 * @param fieldName
	 * @param ftype
	 * @return EncryptedClearValue
	 */
	public abstract EncryptedClearValue<ValueType> decrypt(String encryptedValue,
			String fieldName, EncryptedFieldTable.EFTYPE ftype);



}
