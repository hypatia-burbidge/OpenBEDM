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

import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;

/** The MessageAgent class is the super class for all the active participants in the system: BAN and TeamMember. The participants
 * inherit from this class an ability to run on a thread and watch for new messages delivered by the 
 * message system. The subclasses override this classes run method with a procedure that does something useful
 * with the messages. 
 * 
 * @author pbaker
 *
 */
public class MessageAgent implements Runnable {
	public String name;
	public PmessageSystem pms;
	public Thread t;
	public Profile profile;
	
	public MessageAgent(Profile p, String me) throws MessageSystemException, ConfigurationError {
		profile = p;
		if (me != null && me.length() > 0) {
		System.out.printf("Init Message Agent for %s%n", me);
		this.name = me;
		this.pms = p.messageSystem.getPmessageSystem(me);
		pms.printAgents();
		} else {
			System.out.printf("Skipped Message Agent for anonymous instance%n");
			throw new pygar.configuration.ConfigurationError();
		}

		t = new Thread(this);
	}
	
	/**
	 * The default version of the run procedure will exit when all the queues are empty.
 * This version is useful for testing only. When messages arrive from the outside world,
 * the queues may fill up again at any time.
 */
	public void run() {
		System.out.printf("MessagAgent Start %s%n", this.name);
		Pmessage msg = new Pmessage();
		// get messages and quit when appropriate (must be defined in implementations)
		while ( ! pms.shutdownNow()) {
			while( pms.hasNext()) {
				try {
					msg = pms.next();
				} catch (MessageSystemException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.printf("%s receives: %s, %s-->%s, %s, %s, %s%n", this.name,
						msg.getSubject(), msg.getSenderId(),  
						msg.getRecipientId(), msg.getBrokerId(), msg.getMarketId(), msg.getSessionId());
				Thread.yield();
			}
			Thread.yield();
		}
		System.out.printf("End %s%n", this.name);
	}



}

