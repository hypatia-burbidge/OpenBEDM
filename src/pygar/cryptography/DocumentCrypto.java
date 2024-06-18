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

package pygar.cryptography;
/** Work in progress -- may be altered considerably -- see below. 
 * <p>The document_crypto interface declares the minimal cryptographic services
 * that are applied to negotiation documents as well as event messages. 
 * Documents are subjected to three layers of encryption 
 * as described {@link pygar.configuration.RestrictedImport here}.
 * </p><p>
 * At each encryption layer, additional annotation may be added to a document first
 * to provide context and documentation of the operation or to satisfy the 
 * protocol of the transaction.
 * </p>
 * <p><b>Missing Part:</b>This interface obviously lacks the ability to sign documents with the
 * originator's certificate. Must add this soon!!</p>
 * 
 * @author pbaker
 *
 */
public interface DocumentCrypto<KeyType, NameType, DocSchemaType, DocType>  {
	
	
	/** Inner encryption is a partial encryption with a one-way or symmetric key.
	 * It is partial because it is guided by an extended document schema. In general,
	 * XML tags are not encrypted while everything else is subject to encryption.
	 * @param key the key used by the public key encryption algorithm
	 * @param sessionid identity number for the encryption transaction (annotation)
	 * @param datestring date of the encryption (annotation)
	 * @param doc the document that should be encrypted. 
	 * @param schema information on the XML formating of the document
	 * @param extra (annotation)
	 * @return the annotated and then encrypted document 
	 */  
	public DocType innerEncrypt(
			KeyType key, int sessionid, String datestring,
			DocType doc, DocSchemaType schema, DocType extra);

	/** Outer encryption uses a public key encryption algorithm 
	 * with a supplied key. That may be a private or public key. 
	 * @param key the key used by the public key encryption algorithm
	 * @param name the name associated with the key (annotation)
	 * @param datestring date of the encryption (annotation)
	 * @param doc the document that should be encrypted. 
	 * @param extra (annotation)
	 * @return the annotated and then encrypted document
	 */
	public DocSchemaType outerEncrypt(
			KeyType key, String name, String datestring,
			DocType doc, DocType extra);
	

}
