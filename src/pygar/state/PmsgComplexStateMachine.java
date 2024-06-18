package pygar.state;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import pygar.communication.Pmessage;
import pygar.state.StateMachine.Context;

/**
 * UNDER CONSTRUCTION!! NOT READY FOR USE
 * This class implements a special type of compound state machine that is composed
 * of instances of the PmessageStateMachine superclass. The specialization allows
 * an instance of this class to receive messages and then route them to the component
 * state machines based on the session, groupID, and senderID. 
 * 
 * The class itself exhibits three predefined states: sBegin, sActive, sEnd which
 * correspond to the initial situation, the state when the entire state machine is
 * active and the state reached when all the components have reached their final 
 * state. 
 * 
 * The class is modelled on StateMachine but it is not a subclass because we not
 * want to support any but the predefined states; therefore, addTransition() is not
 * available in this class.
 * 
 * N.b. not currently in use anywhere. 
 * Requires modifications to use current version of other classes.
 * 
 * See especially //TODO CRITICAL where code is commented out
 * 
 * @author pbaker
 *
 */
@ThreadSafe
public class PmsgComplexStateMachine extends Observable implements Observer {
	// an instance of StateMachine is necessary to create
	// instances of Context. N.b. this class would be a subclass of StateMachine
	// except that we do not want the capability to add transitions. 
	private static PmessageStateMachine sm;

	/**
	 * All state machines know the name of the current state and
	 * the name of the previous event.
	 */
	@GuardedBy("this")
	String currentState;
	@GuardedBy("this")
	String previousState;
	@GuardedBy("this")
	String previousEvent;
	
	public synchronized String getState() {
		return currentState;
	}

	/**
	 * Internal function to preform the duties of an observable
	 * n.b. all the callers of this procedure should synchronize on "this"
	 */
	private void _notifyObservers(String message) {
		this.setChanged();
                //TODO CRITICAL
		//this.notifyObservers(sm.new Context(previousState, currentState, previousEvent, message));
	}
	
	public synchronized void setState(String state) {
		previousState = currentState;
		currentState = state;
		previousEvent = "setState";
		_notifyObservers("");
	}

	public synchronized void acceptEvent(String event, Pmessage context) {
		// TODO Auto-generated method stub
	}

	@GuardedBy("this")
	private Map<String, PmessageStateMachine> playerStates;
	@GuardedBy("this")
	private Map<String, Map<String, PmessageStateMachine> > groupIdStates;
	@GuardedBy("this")
	private Map<String, Map<String, Map<String, PmessageStateMachine> > > sessionIdStates;
	
	public PmsgComplexStateMachine() {
		currentState = "sBegin";
		previousState = "null";
		previousEvent = "BEGIN";
		// need an object for reference. --- TODO also need to start this with a sessionId
                //TODO CRITICAL
		//sm = new StateMachine<Pmessage>("sBegin");
	}

	/**
	 * The update method conforms to the interface Observer but requires 
	 * @param o the object that issues the update request
	 * @param arg the Pmessage object - all others cause a failed casting operation.
	 */
	public void update(Observable o, Object arg) {
		// TODO over ride the following to implement the distribution to the component 
		Pmessage pmsg = (Pmessage) arg;
		String event = pmsg.getSubject();
		this.acceptEvent(event, pmsg);

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
