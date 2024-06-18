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

/** A PmessageDispatcher will receive Pmessage objects from an observed source and will dispatch
 * them as updates to the appropriate state machine or compatible observer. Prior to operation,
 * state machines, or other observers, register to receive specific messages. The destination 
 * is determined by the name of the recipient, the name of the sender, the broker, the market, and the session
 * in that order. N.b. the class does not extend nor fully implement an Observable.
 * 
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import pygar.communication.Pmessage;

/** A PmessageDispatcher will receive Pmessage objects from an observed source and will dispatch
 * them as updates to the appropriate state machine or compatible observer. Prior to operation,
 * state machines, or other observers, register to receive specific messages. The destination 
 * is determined by the name of the recipient, the name of the sender, the broker, the market, and the session
 * in that order. N.b. the class does not extend nor fully implement an Observable.
 * 
 */
@ThreadSafe
public class PmessageDispatcher extends Observable implements Observer {
	
	/** Multi-dimensional map to look up the proper observer for the message.
	 * Indices are: recipient, sender, broker, market, session
	 */
	@GuardedBy("this")
	private static 
	Map<String, // recipient
	Map<String, // sender
	Map<String, // broker
	Map<String, // market
	Map<String, // session
	Observer > > > > > observers;
	
	// the defaultObserver will be called, if it exists, when no specific observer is found
	@GuardedBy("this")
	private static Observer defaultObserver;
	
	// the universalObserver will be called, if it exists, for every message
	@GuardedBy("this")
	private static Observer universalObserver; 
	

	public PmessageDispatcher() {
		if (observers == null) {
			observers = new HashMap<String, // recipient
			Map<String, // sender
			Map<String, // broker
			Map<String, // market
			Map<String, // session
			Observer > > > > >();
		}
	}
	
	public synchronized void addObserver(Observer o, String recipient, String sender,
			String broker, String market, String session) {
		Map<String, // sender
		Map<String, // broker
		Map<String, // market
		Map<String, // session
		Observer > > > > x1;
		if ( observers.containsKey(recipient) ) {
			x1 = observers.get(recipient);
		} else {
			x1 = new  HashMap<String, // sender
			Map<String, // broker
			Map<String, // market
			Map<String, // session
			Observer > > > >();
			observers.put(recipient, x1);
		}

		Map<String, // broker
		Map<String, // market
		Map<String, // session
		Observer > > > x2;
		if ( x1.containsKey(sender) ) {
			x2 = x1.get(sender);
		} else {
			x2 = new  HashMap<String, // broker
			Map<String, // market
			Map<String, // session
			Observer > > >();
			x1.put(sender, x2);
		}


		Map<String, // market
		Map<String, // session
		Observer > > x3;
		if (x2.containsKey(broker)) {
			x3 = x2.get(broker); 
		} else {
			x3 = new  HashMap<String, // market
			Map<String, // session
			Observer > > ();
			x2.put(broker, x3);			
		}
		

		Map<String, // session
		Observer >     x4;
		if (x3.containsKey(market)) {
			x4 = x3.get(market);
		} else {
			x4 = new HashMap<String, // session
			Observer > ();
			x3.put(market, x4);
		}
		
		x4.put(session, o);

	}
	
	public synchronized void printObservers() {
//		String blanks = "        ";
		Set<String> recipients = observers.keySet();
		Iterator<String> i1 = recipients.iterator();
		while (i1.hasNext()) {
			String recipient =  i1.next();
			Map<String, // sender
			Map<String, // broker
			Map<String, // market
			Map<String, // session
			Observer > > > > x1 = observers.get(recipient);
			System.out.printf("Recipient: %s%n", recipient);
			
			Set<String> senders = x1.keySet();
			Iterator<String> i2 = senders.iterator();
			while (i2.hasNext()) {
				String sender = i2.next();
				Map<String, // broker
				Map<String, // market
				Map<String, // session
				Observer > > > x2 = x1.get(sender);
				System.out.printf("  Sender: %s%n", sender);

				Set<String> brokers = x2.keySet();
				Iterator<String> i3 = brokers.iterator();
				while (i3.hasNext()) {
					String broker = i3.next();
					
					Map<String, // market
					Map<String, // session
					Observer > > x3 = x2.get(broker);
					System.out.printf("    Broker: %s%n", broker);

					Set<String> markets = x3.keySet();
					Iterator<String> i4 = markets.iterator();
					while (i4.hasNext()) {
						String market = i4.next();
						
						Map<String, // session
						Observer > x4 = x3.get(market);
						System.out.printf("      Market: %s%n", market);

						Set<String> sessions = x4.keySet();
						Iterator<String> i5 = sessions.iterator();
						while (i5.hasNext()) {
							String session = i5.next();
							System.out.printf("        Session: %s%n", session);
						}
					}
				}
				
				
			}
			
		}

	}
	
	public synchronized void addDefaultObserver(Observer o) {
		defaultObserver = o;
	}
	
	public synchronized void addUniversalObserver(Observer o) {
		universalObserver = o;
	}

	public synchronized void update(Observable o, Object arg) {
		// update correct observer, ignore if none is known
		Pmessage msg = (Pmessage) arg;
//		msg.partialPrint();
		boolean notSpecific = true;
		if ( observers.containsKey(msg.getRecipientId()) ) {
			Map<String, // sender
			Map<String, // broker
			Map<String, // market
			Map<String, // session
			Observer > > > > x1 = observers.get(msg.getRecipientId());
			if ( x1.containsKey(msg.getSenderId()) ) {
				Map<String, // broker
				Map<String, // market
				Map<String, // session
				Observer > > >   x2 = x1.get(msg.getSenderId());
				if (x2.containsKey(msg.getBrokerId())) {
					Map<String, // market
					Map<String, // session
					Observer > >    x3 = x2.get(msg.getBrokerId());
					if (x3.containsKey(msg.getMarketId())) {
						Map<String, // session
						Observer >     x4 = x3.get(msg.getMarketId());
						if (x4.containsKey(msg.getSessionId())) {
							Observer obs = x4.get(msg.getSessionId());
							obs.update((Observable)this, arg);
							notSpecific = false;
						}
					}
				}
			}
		}

		// call default if needed
		if ( notSpecific && ( defaultObserver != null)) {
			System.out.println("defaultObserver called");
			defaultObserver.update(this, arg);
		}
		// call universal if it exists
		if (universalObserver != null) {
			universalObserver.update(this, arg);
		}

	}

}
