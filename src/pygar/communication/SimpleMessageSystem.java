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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import pygar.communication.MessageAgent;
import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;
import pygar.demo0P.ProfileDemo0;

/** 
 * The SimpleMessageSystem implements message passing between threads that share memory and
 * file space. It implements message queues that the threads use to communicate. Message
 * bodies are stored in files which never actually move because the file space is shared.
 * <p><b>Warning</b> - only demo0P uses this package. It is suitable for testing simple
 * software on a single machine but inadequate for testing JMS software even on a single
 * machine.
 * <p>In an operational system, the sender and recipient would be separate process initialized
 * with separate identities. In this simple, one process implementation, senders and receivers
 * are separate threads in the same process. Consequently, this implementation requires a 
 * different SimpleMessageSystem object for each thread. The first object that is instantiated
 * prepares class members shared by the objects. 
 * </p>
 * 
 * <p>Warning: this simple implementation does not protect data from loss if the 
 * software is shutdown while there are still messages in the queue. Furthermore,
 * it saves all messages and never cleans up. Consequently, it is only suitable
 * for short-term tests or demonstrations. </p>
 * 
 * <p>Warning: this simple implementation does not sign a message so that the 
 * recipient may verify it. The interface is agnostic about the message body which
 * will be encrypted and/or signed as the case may require. However, a message
 * without a body is also used as a protocol even and those should be signed and
 * verified to protect the integrity of the system. Be sure to select a more
 * complete implementation of PmessageSystem for production software. </p>
 * 
 * @author pbaker
 *
 */
@ThreadSafe
public class SimpleMessageSystem implements PmessageSystemEE {
		
	// shared resources
	@GuardedBy("this")
	public static boolean isInitialized = false;
	@GuardedBy("this")
	public static Map<String, Queue<Pmessage> > messages;
	
	@GuardedBy("this")
	private static SimpleMessageSystem example;
	
	// uncomment the following to get log file for debugging. 
//	private static File logFile;
//	static PrintWriter messageLog;
	
	// instance resource
	public String id;
    
    public static Logger theLogger =
            Logger.getLogger(SimpleMessageSystem.class.getName());
	
/**
 * The SimpleMessageSystem has no mechanism to determine when it can
 * shutdown so the condition shutdownNow() is always false.
 */
	public synchronized boolean shutdownNow()  {
		
		return false;
	}
	
	public synchronized  void printAgents() {
		System.out.println("Current Agent Names in SimpleMessageSystem");
		Object[] msgArray;
		Pmessage message;
		Iterator<String> i = messages.keySet().iterator();
		String key;
		while (i.hasNext()) {
			key = i.next();
			System.out.println(key);
			msgArray =  messages.get(key).toArray();
			if (msgArray.length > 0) {
				for (int k = 0; k < msgArray.length; k++) {
					message = (Pmessage) msgArray[k];
					System.out.printf("     ---%s: %s, %s-->%s, %s, %s, %s%n", key,
							message.getSubject(), message.getSenderId(), 
							message.getRecipientId(), message.getBrokerId(), message.getMarketId(), message.getSessionId());

				}
			}
		}
		System.out.println("-----------------");
	}
	
	/**
	 * This constructor sets up the shared area and returns an instance that is only
	 * suitable for using the factory method.
	 * 
	 */
	protected SimpleMessageSystem() {
		if ( ! isInitialized) {
			isInitialized = true;
			messages = new HashMap<String, Queue<Pmessage> >();
		}
		example = this;
		this.id = null;
        
	}

    public PmessageSystem getPmessageSystem(String senderId) {
		return new SimpleMessageSystem(senderId);
	}

	/**
	 * The constructor sets up one sender. If this is the first object to be constructed,
	 * it will also set up the logfile and other shared resources. Note that the filepath is
	 * used for the first object but it is ignored for all the rest because the shared
	 * resources were previously established.
	 * 
	 * This constructor is not compatible with the convention for program configuration.
	 * Instead, one should use one exemplar of the class to provide additional instances
	 * via the factory method. 
	 * 
	 * @param senderId The identification of the thread that will send/get from this service
	 * @throws MessageSystemException
	 */
	public SimpleMessageSystem(String senderId) {

		this();
		this.id = senderId;
		// messages that arrive for this ID will be place in the following queue
		messages.put(senderId, new LinkedList<Pmessage>());		
		
		System.out.printf("pms initialized sender: %s, there are %d ids in pms%n", 
				senderId, messages.keySet().size());
	}
	
	/** 
	 * Send a simple event from one message agent to another. Only the name
	 * of the event is sent.
	 */
	public synchronized void send(String sender, String receiver, String event) {
		try {
			this.send(new Pmessage(sender, receiver, event));
		} catch (MessageSystemException e) {
			e.printStackTrace();
		}
	}
    
    /**
     * Test whether the system is ready to send to a given recipient. 
     */
    public synchronized boolean canSend(Pmessage message) {
        return messages.containsKey(message.getRecipientId());
    }

	/**
	 * Send a Pmessage to the destination specified in the Pmessage object.
	 * The Pmessage cannot provide a body as an InputStream. That option is
	 * not implemented. 
	 * @param message
	 * @throws MessageSystemException
	 */
	public synchronized void send(Pmessage message) throws MessageSystemException {

        // set the sendID to own ID
		message.setSenderId(this.id);
		// log message
//		writeLog(message);
        theLogger.log(Level.INFO, "msg: " + message.getSubject() + " " +
                message.getSenderId() + "-->" + message.getRecipientId() + 
                ", session: " + message.getSessionId()); 
        String s = message.getStringBody();
        if ( s != null ) {
            theLogger.log(Level.FINE, s);
        }
		// add message to approriate queue
		if ( !messages.containsKey(message.getRecipientId())) {
			System.err.printf("ERROR: attempt to Pmessage send to unknown recepient %s%n", message.getRecipientId());
			System.err.println("Known recepients:");
			Iterator<String> kset = messages.keySet().iterator();
			while (kset.hasNext()) {
				System.err.printf("     %s%n", kset.next());
				
			}
			
			throw new MessageSystemException();
		}
		messages.get(message.getRecipientId()).add( new Pmessage(message));
//		System.out.println("Send diagnostic");
//		SimpleMessageSystem.printAgents();
	}
	
	/**
	 * Return true if there is a Pmessage waiting to be received.
	 * @return boolean
	 */
	public synchronized boolean hasNext() {
		return ! messages.get(this.id).isEmpty();
	
	}
	
	/**
	 * Return the next waiting Pmessage or void if there is none.
	 * @return Pmessage
	 * @throws MessageSystemException 
	 */
	public synchronized Pmessage next() throws MessageSystemException {
		if (this.hasNext()) {
			return messages.get(this.id).remove();
		} else {
			return null;
		}
	}
	/**
	 * @param args
	 * @throws MessageSystemException 
	 * @throws InterruptedException 
	 * @throws ConfigurationError 
	 */
	public static void main(String[] args) throws MessageSystemException, InterruptedException, ConfigurationError {
		System.out.println("Test SimpleMessageSystem");
		// setup profile for any entity - doesn't matter which.
		Profile p = new ProfileDemo0("BAN", "/Users/pbaker/Coding/BEDM/testdata/demo0");
		MessageAgent a = new MessageAgent(p, "A");
		MessageAgent b = new MessageAgent(p, "B");
		MessageAgent c = new MessageAgent(p, "C");

		// put a few messages in the system
		Pmessage msg = new Pmessage();
		msg.setRecipientId("B");
		msg.setSubject("test 1");
		try {
			a.pms.send(msg);
		} catch (MessageSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		msg.setRecipientId("A");
		msg.setSubject("test 2");
		try {
			c.pms.send(msg);
		} catch (MessageSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
//		// start the message agents		
//		a.t.start();
//		b.t.start();
//		c.t.start();
//
//		// wait for all the message agents to finish
//		a.t.join();
//		b.t.join();
//		c.t.join();



		//		SimpleMessageSystem.printAgents();

		System.out.println("End Test SimpleMessageSystem");
//		SimpleMessageSystem.messageLog.close();
		

	}

    @Override
    public PmessageSystem getPmessageSystem(Profile p) {
        return getPmessageSystem(p.entityAlias); 

    }

}
