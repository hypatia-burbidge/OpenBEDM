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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.jcip.annotations.ThreadSafe;

import pygar.communication.Pmessage;

/**
 * A class used in a Blind Agent Negotiator (BAN) application to keep track of the negotiation
 * groups. A negotiation group (NG) consists of two or more clients of the BAN who have mutually
 * agreed to negotiation. Within a particular market, the BAN regularly starts a negotiation
 * session among members of the NG and waits for the session to end before starting another. 
 * 
 * N.b. there is no method to alter the groupMembers after construction; therefore, the
 * field should be thread safe if we declare it volatile.
 * 
 * @author pbaker
 *
 */
@ThreadSafe
public class NegotiationGroupStateMachine extends StateMachine<String> {
	
//	public String currentSession;
	
	public volatile List<String> groupMembers;
	
//	private PmessageDispatcher dispatch;
	
	EventListener<String> prepare = new EventListener<String>() {

		public void eventHandler(String session, String eventName) {
			// Tell members to prepare for session
			Pmessage msg = new Pmessage();
			msg.setBrokerId("broker0");
			msg.setMarketId("market0");
			msg.setSessionId("session0");
			msg.setRecipientId("recipient0");
			msg.setSenderId("sender0");
			msg.setSubject("DispatcherTest");
					Iterator<String> i = groupMembers.iterator();
			while (i.hasNext()) {
				String memberName = i.next();
//				dispatch.
			}
			
			
		}
		
	};

	public NegotiationGroupStateMachine(List<String> members) {
		super("begin");
		groupMembers = new LinkedList<String>(members);
		Iterator<String> i = groupMembers.iterator();
	}
	

}
