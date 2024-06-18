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

package pygar.state;

import java.lang.reflect.InvocationTargetException;
import pygar.communication.MessageSystemException;

/**
 * The EventListener interface contains just one procedure which handles
 * an event by performing actions. The procedure is passed two parameters
 * that it may use to control the action. Either may be null depending 
 * on the function of the procedure.
 * @author pbaker
 *
 * @param <T>
 */
public interface EventListener<T> {
	
	public void eventHandler(T context, String eventName) throws 
            MessageSystemException, InterruptedException, InvocationTargetException;

}
