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

import java.io.InputStream;
import java.io.Serializable;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/** The PmessageBase class describes simple messages passed between applications to
 * serve as protocol events. Derived classes of this class are used for a richer
 * message content. The message object contains sufficient
 * identification that a event distributor (not defined here) is able to route
 * the event to the relevant state machine object. 
 * <p>The identification specifies the broker who is running the market, the market
 * that the broker runs, the particular session in that market, the owner of the
 * application sending the 
 * message, owner of the application that should receive the message and the
 * subject of the message. The subject of the message is generally the event name
 * associated with the Pmessage. </p> 
 * @author pbaker
 *
 */
@ThreadSafe
public class PmessageBase implements Serializable {

	// the following fields route the message to the proper end-point
	@GuardedBy("this")
	protected String senderId;
	@GuardedBy("this")
	protected String recipientId;
	@GuardedBy("this")
	protected String brokerId;
	@GuardedBy("this")
	protected String marketId;
	@GuardedBy("this")
	protected String sessionId;
	// the subject field usually defines the event triggered by the message
	@GuardedBy("this")
	protected String subject;
	

    public synchronized String getSenderId() {
        return senderId;
    }

    public synchronized void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public synchronized String getRecipientId() {
        return recipientId;
    }

	public synchronized void setRecipientId(String recipientId) {
		this.recipientId = recipientId;
	}

	public synchronized String getBrokerId() {
		return brokerId;
	}

	public synchronized void setBrokerId(String brokerId) {
		this.brokerId = brokerId;
	}

	public synchronized String getMarketId() {
		return marketId;
	}

	public synchronized void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	public synchronized String getSessionId() {
		return sessionId;
	}

	public synchronized void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

    public synchronized String getSubject() {
		return subject;
	}

	public synchronized void setSubject(String subject) {
		this.subject = subject;
	}

	/** copy constructor
	 * 
	 * @param msg
	 */
	public PmessageBase(PmessageBase msg) {
		this.brokerId = msg.brokerId;
		this.marketId = msg.marketId;
		this.sessionId = msg.sessionId;
		this.senderId = msg.senderId;
		this.recipientId = msg.recipientId;
		this.subject = msg.subject;
	}
	
	public PmessageBase() {
		this.brokerId = "";
		this.marketId = "";
		this.sessionId = "";
		this.senderId = "";
		this.recipientId = "";
		this.subject = "";
	}
	
	public PmessageBase(String myID, String recipient, String eventName) {
		this.brokerId = "";
		this.marketId = "";
		this.sessionId = "";
		this.senderId = myID;
		this.recipientId = recipient;
		this.subject = eventName;
	}
	
	public synchronized void partialPrint() {
		System.out.printf("Pmessage diagnostic, subject: %s to %s from %s session %s%n", this.subject, this.recipientId,
				this.senderId, this.sessionId);
	
	}

}
