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

/**
 * <p>
 * In a highly secure installation of the software, the software should be split into applications 
 * running in several security zones. Security can be further enhanced if the installation
 * limits the distribution of classes
 * so that software capabilities are present on one host computer but not another. If each host
 * in a zoned security environment has only the essential software components, then attacks that
 * target the software applications can have only limited success. 
 * </p><p>
 * In each zone, one layer of encryption is added to outgoing
 * documents while the same layer of encryption is removed from incoming documents. </p>
 * <p>The transfer of documents between zones is accomplished differently in the inbound and
 * outbound directions. Inbound documents are staged for transfer to a higher security zone and
 * then pulled inward from that zone. Outbound documents are simply sent to the lower zone and 
 * immediately accepted there. In practice, the movement involves reading and writing to a 
 * data storage (pygar.documents.Store) and the rules for transfer are saying essentially that
 * a high security zone can read and write in a low security zone but the low security zone
 * has no such privileges in the high security zone. </p> 
 * <p>The zone system defense is not implemented in the current set of pygar pages.</p>
*/

package pygar.zoneable;
