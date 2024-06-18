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

package pygar.communication;

import pygar.communication.Pmessage;

/**
 * The PmessageSystem abstract class contains virtual methods that send Pmessages 
 * and get them. Subclasses provide implementations suitable for different systems. 
 * <p>
 * This interface is not ideal for use with Java EE. Currently, we can 
 * accommodate EE by adding one constructor to an extension of this interface
 * called PmessageSystemEE
 * <p>This interface, as originally written, assumes that processes share 
 * a communication area containing message queues. 
 * A robust implementation of a subclass will be sure to provide ways to save the
 * queues when the system is going down. Simple implementations are allowed to ignore
 * this area of functionality with only a message on the error log. A conforming 
 * Java EE concrete class may rely on the persistence of the queues in the EE server. 
 * 
 * <p>
 * For ease of use with the pygar configuration convention, this class must provide
 * a factory method in addition to the normal constructor. Thus, the configuration
 * profile can contain an instance of the desired subclass which will subsequently
 * provide instances during program execution. 
 * 
 * @author pbaker
 *
 */
public interface PmessageSystem {
	
	/** Factory method returns a working instance for the specified senderId.
	 * @param senderId the Id of the entity that will use this instance of the message system.
	 * @return exemplar
	 */
	public abstract PmessageSystem getPmessageSystem(String senderId);
	
	/**
	 * Send a Pmessage to the destination specified in the Pmessage object.
	 * @param message
	 * @throws MessageSystemException
	 */
	public abstract void send(Pmessage message) throws MessageSystemException;
	
    /**
     * Test whether the system is ready to send to a given recipient. 
	 * @param message
     * @return true if ready to send
     */
    public abstract boolean canSend(Pmessage message);
    
    /**
	 * Construct and send a simple event Pmessage to a destination.
	 * @param sender
	 * @param receiver
	 * @param event
	 * @throws MessageSystemException
	 */
	public abstract void send(String sender, String receiver, String event) 
	throws MessageSystemException;
	
	/**
	 * Return true if there is a Pmessage waiting to be received.
	 * @return boolean
	 */
	public abstract boolean hasNext();
	
	/**
	 * Return the next waiting Pmessage or void if there is none.
	 * @return Pmessage
	 * @throws MessageSystemException 
	 */
	public abstract Pmessage next() throws MessageSystemException;
	
	/**
	 * Return all the agent names associated with this instance. Usually, there is just one.
	 * However, in certain test procedures as well as in the subclass SimpleMessageSystem
	 * there may be several. 
	 */
	
	public abstract void printAgents();
	
	/**
	 * Return true if the system should shutdown. There is no general way to determine this
	 * condition. It will depend on the implementation in the subclasses.
	 * 
	 */
	public abstract boolean shutdownNow();
}
