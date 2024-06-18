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

import javax.xml.stream.XMLStreamException;

import pygar.configuration.ConfigurationError;
import pygar.configuration.DocumentError;

/** The Match interface describes the basic features of a matchmaker process that
 * the broker runs to compare negotiating positions and discover a basis for 
 * agreement. It is parameterized by the type of the object used o convey the
 * position and basis information. The entry points have a natural sequence to their
 * calls that must be followed or an exception will be raised. 
 * 
 * @author pbaker
 *
 * @param <T>
 */
public interface Matcher<T, Tout> {
	
	/** Prepare the matcher object to conduct a match among a match group
	 * of nMembers participants. Usually there are two members. Obviously, they
	 * both receive any basis for agreement. For three or more members, we need 
	 * a policy about whether a member receives only matches to which they contributed
	 * data or whether every member of the group receives the same basis of agreement
	 * regardless of contribution. It is difficult to supply the correct decision for
	 * this policy, which is why most match groups will probably have two members
	 * for most applications. 
	 * @param nMembers
	 * @param inclusive if there are more than two members, should they all share a match
	 */
	public void open(int nMembers, boolean inclusive);
	
	/** Load the negotiation position for one of the members into the match object. Throw
	 * exception if the object was not initialized by an open call.
	 * @param memberNumber sequence number in the group, zero based, e.g. for two members: 0 and 1
	 * @param memberName optional name for the member
	 * @param position the member's negotiation statement for matching
	 * @throws ConfigurationError 
	 * @throws DocumentError 
	 */
	public void load(int memberNumber, String memberName, T position) throws ConfigurationError, DocumentError;
	
	/** Compare negotiation statements and find matches. The position for each member
	 * must be loaded before this method is called or an exception is thrown.
	 */
	public void compare();
	
	/** Return the basis of agreement from the previous "compare" operation. 
	 * An exception is thrown if there was no previous compare operation. 
	 * @return the basis for agreement with matching statements. 
	 * @throws ConfigurationError 
	 */
	public Tout getBasis(int memberNumber) throws ConfigurationError;
	
	/** Complete any cleanup operations after the matching operation.
	 * 
	 */
	public void close();

}
