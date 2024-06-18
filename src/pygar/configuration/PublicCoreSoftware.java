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

package pygar.configuration;

/** A unit annotated as "PublicCoreSoftware" contains code that defines the
 * essential core of the system that must be resistant to security
 * attacks and inappropriate use of the features. Modules so marked
 * should be inspected carefully for weakness in each module individually
 * and in combination with other core software modules.  
 * 
 * @author pbaker
 *
 */
public @interface PublicCoreSoftware {

}
