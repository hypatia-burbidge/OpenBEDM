package pygar.state;

import java.util.HashMap;

/**
 * OrderedStateMachine is a StateMachine for which the states are defined in a 
 * particular order. The order is useful for some graphical representations of state
 * transition sequences. 
 * 
 * N.B. This class is not used currently anywhere. See SequenceMachine
 * @author pbaker
 *
 * @param <T>
 */
public class OrderedStateMachine<T> extends StateMachine<T> {
	
	HashMap<String, Integer> states;

	public OrderedStateMachine(String start, String[] states) {
		super(start);
		this.states = new HashMap<String, Integer>();
		
		for (int i = 0; i < states.length; ++i) 
			this.states.put(states[i], i);
			
	}
	
	public int compare(String state1, String state2) {
		return this.states.get(state1).compareTo( this.states.get(state2));
	}

}
