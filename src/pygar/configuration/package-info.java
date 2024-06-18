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
 * Classes that determine the configuration for running an application.
 * <p>The configuration step begins in each application when the application
 * instantiates a single object of a subclass of the class <b>Profile</b>.
 * That single object can be obtained at any point in the code by a call
 * to Profile.getProfile. The Profile object contains configuration information
 * obtained in three ways:</p><ol>
 * <li>exemplar objects with methods that perform a useful algorithm
 * implemented in a particular way. Different applications might be configured
 * to use different implementations for different reasons. 
 * <li>properties that are read from file, e.g., these properties may set
 * the paths to files or the names of the current application or other entity.
 * <li>cryptographic algorithms selected by the Java class loader. The configuration
 * software does not influence this selection. It is determined by the cryptographic
 * service providers installed in the host computer that runs the Pygar software.
 * </ol> 
*/
package pygar.configuration;
