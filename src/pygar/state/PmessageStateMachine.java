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

import java.util.Observable;
import java.util.Observer;

import net.jcip.annotations.ThreadSafe;

import pygar.communication.Pmessage;

/** 
 * This class specializes StateMachine for the Type Pmessage and extends
 * it to include an associated sessionID, which ID is also 
 * included in the Context of the notifications sent to Observers.
 * @author pbaker
 *
 */
@ThreadSafe()
public class PmessageStateMachine extends StateMachine<Pmessage>  {
	
	public String sessionId;
	
	/**
	 * Class Context defines the data for observers of this state machine.
	 * @author pbaker
	 *
	 */
	public class Context extends StateMachine.Context {
		public volatile String sessionId;
		
		public Context(String session, String start, String end, String e, String m) {
			super(start, end, e);
			this.sessionId = session;
		}
		
		public Context(StateMachine.Context base, String session) {
			super(base);
			sessionId = session;
		}
		
		public String toString() {
			StringBuffer sb = new StringBuffer("session: ");
			sb.append(sessionId);
			sb.append(" event: ");
			sb.append(event);
			sb.append(" endState: ");
			sb.append(endState);
			return sb.toString();
		}
	}
	
	@Override
	public void notifyObservers() {
		StateMachine.Context cbase = super._getStateContext();
		PmessageStateMachine.Context context = new PmessageStateMachine.Context(cbase, sessionId);
//		System.out.printf("PmsgSM notify: %s %n", context.toString());
		this.setChanged();
		super.notifyObservers(context);
	}
	


	/**
	 * StateMachine for type Pmessage and instance of session
	 * @param start starting state of the machine
	 * @param session id of the session
	 */
	public PmessageStateMachine(String start, String session) {
		super(start);
		sessionId = session;
	}
	

}
