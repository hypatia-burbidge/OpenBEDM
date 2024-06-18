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

package pygar.communication;

import pygar.configuration.Profile;

/**
 * A specialization of the PmessageSystem interface that adds a constructor that
 * require more configuration information and allows the proper initialization
 * of a Java EE implementation. 
 * @author pbaker
 */
public interface PmessageSystemEE extends PmessageSystem {
    
	
	/** Factory method returns a working instance for the specified configuration Profile.
	 * @param p the Profile containing configuration information. 
	 * @return exemplar
	 */
	public abstract PmessageSystem getPmessageSystem(Profile p);    
}
