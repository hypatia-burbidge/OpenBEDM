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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Set;
import java.lang.reflect.InvocationTargetException;

import pygar.communication.MessageSystemException;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;


/** This class implements simple state machines that respond to events.
 * The events are named and contain context information of type T.
 * State machines have predefined transitions. Each transition is 
 * characterized by a start state, an end state, the action that provokes
 * the transition and an event handle that performs any associated action.
 * <p>
 * Several sub-types of transitions are supported. If an event has no particular
 * starting state, then the transition may be specified with a null starting 
 * state. Such transitions may have a defined list of starting states that 
 * should not respond to the event. </p>
 * <p>A transition may have a null end state. This transition is interpreted 
 * as a loop-back - a state that performs the eventHandler and returns to 
 * the same state. </p>
 * <p>Each statemachine has two optional slots that support a simple persistence
 * scheme that can be used to checkpoint and rollback the system. The first
 * slot contains a persistence id. The second contains function object for logging
 * transitions. <strong>(optional slots not currently implemented)</strong> </p>
 * @author pbaker
 *
 * @param <T> the type of the context object that accompanies each event
 */
@ThreadSafe
public class StateMachine<T> extends Observable {

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
	
	/**
	 * Class Context defines the data for observers of this state machine.
	 * @author pbaker
	 *
	 */
	public class Context {
		public volatile String startState;
		public volatile String endState;
		public volatile String event;
		
		public Context(String start, String end, String e) {
			this.startState = start;
			this.endState = end;
			this.event = e;
		}
		
		public Context(Context c) {
			this.startState = c.startState;
			this.endState = c.endState;
			this.event = c.event;
			
		}
		public String toString() {
			StringBuffer sb = new StringBuffer(" event: ");
			sb.append(event);
			sb.append(" endState: ");
			sb.append(endState);
			return sb.toString();
		}
	}
	

	/**
	 * Function to perform the duties of an observable
	 * n.b. all the callers of this procedure should synchronize on "this"
	 */
	public void notifyObservers() {
		this.setChanged();
		super.notifyObservers(new Context(previousState, currentState, previousEvent));
	}

	public void notifyObservers(Object o) {
		Context c = (Context)o;
//		System.out.printf("called StateMachine.notifyObservers: %s %n", c.toString() );
		this.setChanged();
		super.notifyObservers(o);
	}
	
	
	/**
	 * Internal routine called from a super class to create a copy of context as it is
	 * known to this class. The message field is left blank for completion later. 
	 * @return Context
	 */
	protected Context _getStateContext() {
		return new Context(previousState, currentState, previousEvent);
	}
	
//	/**
//	 * When a state machine becomes the active state machine viewed by an operator,
//	 * the visible components should be notified to take notice of the state by using
//	 * the nowCurrent() call
//	 */
// 	public void nowCurrent() {
//		_notifyObservers();
//	}
	
	/** Assign a state to the state machine. Normally, however, you
	 * should just let that events induce state transitions.  
	 * 
	 * @param state
	 */
	public synchronized void setState(String state) {
		previousState = currentState;
		currentState = state;
		previousEvent = "setState";
		notifyObservers();
	}

	/** Return the current state of the state machine. 
	 * 
	 * @return state
	 */
	public synchronized String getState() {
		return currentState;
	}

	/**
	 * This inner class contains a lookup table for events
	 */
	public class TransitionPt1 {
		// the key for the following table is the event name
		volatile HashMap<String, TransitionPt2> transitionTable;
		
		public TransitionPt1() {
			transitionTable = new HashMap<String, TransitionPt2>();
		}
	}

	/** transition description */
	public class TransitionPt2 {
		volatile String endingState;
		volatile EventListener<T> handler;
	}

	/** alternate transition description for transitions that
	    have many starting states. It adds a list of states
	    that do not respond to the event */
	public class TransitionPt3 extends TransitionPt2 {

		volatile Set<String> insensitiveStates;
	}


	/** The transitionTable is keyed on the starting state name. */
	@GuardedBy("this")
	HashMap<String, TransitionPt1> transitionTable;

	/** The alternate transition table describes another kind of
	transition that can start in all or almost all states. */
	@GuardedBy("this")
	HashMap<String, TransitionPt3>  altTransitionTable;

	/** If no transition can be found for the event, you may specify a default
	 * handler. If this default is null, no action is taken for the unknown event 
	*/
	@GuardedBy("this")
	EventListener<T> defaultHandler;

	/**
	 * 
	 * n.b. all the callers of this procedure should synchronize on "this"
	 * @param startState
	 * @param eventName
	 * @param endState
	 * @param action
	 * @param insensitive
	 */
	private void _addTransition(String startState, String eventName, String endState, 
			EventListener<T> action, Set<String> insensitive) {
//		// the paramater insensitive may not be set - supply empty collection
//		if (insensitive == null) {
//			insensitive = new HashSet<String>();
//		}
		if (startState == null ) {
//			System.out.printf("add entry to table any start to %s via %s %n", endState, eventName);
			// add to alternate table 
			TransitionPt3 pt = new TransitionPt3();
			pt.endingState = endState;
			pt.handler = action;
			pt.insensitiveStates = insensitive;
			altTransitionTable.put(eventName, pt);
		} else {
//			System.out.printf("add transition from %s to %s via %s %n", startState, endState, eventName);
			// check whether there is an entry already in the transitionTable
			if (! transitionTable.containsKey(startState)) {
				// no entry so make one
				transitionTable.put(startState, new TransitionPt1());
			}
			// new entry for the transition
			TransitionPt2 pt = new TransitionPt2();
			pt.endingState = endState;
			pt.handler = action;
			// put it in tables
			transitionTable.get(startState).transitionTable.put(eventName, pt);
						
		}
	}
	
	public StateMachine(String start) {
		currentState = start;
		previousState = "null";
		previousEvent = "BEGIN";
		transitionTable = new HashMap<String, TransitionPt1>();
		altTransitionTable = new HashMap<String, TransitionPt3>();
	}

	/**
	 * Add a transition to the state machine where the transition has multiple start states.
	 * 
	 * @param eventName name of the event
	 * @param endState state after the event or null if this is a loop-back
	 * @param action EventListener that provides actions for the transition
	 * @param insensitive a list of start state names that do not respond to this event
	 */
	public synchronized void addTransition( String eventName, String endState, 
			EventListener<T> action, String[] insensitive) {
		// establish the set of states that don't respond to events
		Set<String> st = new HashSet<String>(); 

        if (insensitive != null) {
            for (String s : insensitive) {
                st.add(s);
            }
        }
		_addTransition(null, eventName, endState, action, st);
	}

	/**
	 * Add a transition to the state machine where the transition has a definite start state.
	 * 
	 * @param startState initial state of transition or null for an event operating on all states.
	 * @param eventName name of the event
	 * @param endState state after the event or null if this is a loop-back
	 * @param action EventListener that provides actions for the transition
	 */
	public synchronized void addTransition(String startState, String eventName, String endState, 
			EventListener<T> action) {
        Set<String> st = new HashSet<String>();
		_addTransition(startState, eventName, endState, action, st);
	}
	
	/**
	 * Receive an event from another object with no context specified. N.b. the success of the 
	 * operation depends on the handlers for the transition invoked by the event. If the handler,
	 * requires a context, then the operation fails. Context free events are usually associated
	 * with user input events, not enterprise messages. 
	 * @param event
	 * @throws MessageSystemException 
	 */
	public synchronized void acceptContextFreeEvent(String event) throws 
            MessageSystemException, InterruptedException, InvocationTargetException {
		_acceptEvent(event, null); 
	}

	/**
	 * Receive an event from another object in the context of a third context object of type T.
	 * @param event
	 * @param context
	 * @throws Exception
	 */
	public synchronized void acceptEvent(String event, T context) throws Exception {
		if (context == null) {
			System.err.printf("Program Error: acceptEvent called with %s and null", event);
			throw new Exception();
		}
			_acceptEvent(event, context); 
	}

	private synchronized void _acceptEvent(String event, T context) throws 
            MessageSystemException, InterruptedException, InvocationTargetException {

//		System.out.printf(" acceptEvent %s%n", event);
		
		
		
		// try the standard selection: starting state -> event -> transition
		if (transitionTable.containsKey(currentState)) { 
			TransitionPt1 pt1 = transitionTable.get(currentState);
			// next look up the event
			if (pt1.transitionTable.containsKey(event)) { 
				// execute the transition
				previousState = currentState;
				previousEvent = event;
				// get transition
				TransitionPt2 pt2 = pt1.transitionTable.get(event);
				// do actions if provided
				if (pt2 != null && pt2.handler != null) {
					pt2.handler.eventHandler(context, event);
				}
				// set resulting state
				if (pt2.endingState != null) {
					currentState = pt2.endingState;										
				}
				// report to any observers				
				notifyObservers();
				return;
			} 
		}
		
		// did not find a standard selection so try the alternate: event -> transition
		if (altTransitionTable.containsKey(event)) { 
			// get the transition
			TransitionPt3 pt3 = altTransitionTable.get(event);
			// check the loop-back states
			if (pt3.insensitiveStates.contains(currentState)) { 
				// do nothing
				previousState = currentState;
				previousEvent = event;
                notifyObservers();
				return;
			} else {
				// execute the transition
				previousState = currentState;
				previousEvent = event;
				// do actions
				if (pt3 != null && pt3.handler != null) {
					pt3.handler.eventHandler(context, event);
				}
				// set resulting state
				if (pt3.endingState != null) {
					currentState = pt3.endingState;
				}
				// report to any observers
				notifyObservers();
				return;
			}
		} 

		// event not found in either table so call the default handler
		// if there is one
		if (defaultHandler != null) {
			defaultHandler.eventHandler(context, event);
			previousEvent = event;
			notifyObservers();
			return;
		}
		
		// unknown event, do nothing
        System.out.println("!!!failed to handle event " + event + " state " +
                currentState + " previous " + previousState);
		notifyObservers();
		return;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// test statemachine: see tests:StateMachineTest
		

	}

//	public void notifyObservers(Object o) {
//		Context c = (Context)o;
//		System.out.printf("called StateMachine.notifyObservers: %s %n", c.toString() );
//		_notifyObservers(o);
//		
//	}


}
