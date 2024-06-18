	/****************************************************************CopyrightNotice
	 * Copyright (c) 2013 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *******************************************************************************/

/**
 * The component responsible for communication between entities in the distributed
 * system. 
 * <p>
 *The package provides specialized entry points that are implemented with standard
 * Java and Internet utility libraries. 
 * <p>Currently, the communication is viewed as an 
 * exchange of "Pmessages" where we invent a term which stands for a limited variety 
 * of inter-application message. You can view the "p" as standing for either "protocol"
 * or "Pygar". The Pmessage is described by ???</p>
 * <p>Notes from Original Version:</p>
 * <ul>
 * <li>A Pmessage has a standardized set of fields plus an optional XML document attachment. 
 * <li>The Pmessage contains a handle for accessing the XML document rather than the
 * document itself. A handle might be a file name or a Java stream. 
 * <li>Pmessages may be interpreted as events that change the state of the
 * session.
 * <li>One possible implementation would use a current standard for RPC, 
 * perhaps SOAP. Another possible implementation would write files 
 * directly to the recepient's server using sftp or an equivalent. 
 * <li>For preliminary testing on a single host, the Pmessages are sent 
 * between program threads via a shared
 * data collection. 
 * </ul>
 * <p>Notes on the Current Version:
 * <ul>
 * <li>The stream option was never implemented and the code for it has
 * been commented out. 
 * <li>It was not possible to extend the Pmessage class directly for use with JMS. 
 * Therefore, we added a new base class PmessageBase that contains the fields of
 * Pmessage that are useful for the JMS implementation. Pmessage now derives from
 * PmessageBase.
 * <li>A new class PmessageJMSv0 derives from Pmessage and works well with 
 * a simplified facade for the JMS facilities. 
 * </ul>
 * <p> $Revision: 59 $ </p>
*/

package pygar.communication;
