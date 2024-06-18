package pygar.state;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import pygar.communication.MessageSystemException;

import pygar.configuration.ConfigurationError;


/**
 * The SequenceDisplayMachine is a helper class used with StateMachine. It is used to visualize and
 * manage a sequence of steps each of which corresponds to a state. The correspondence
 * is based purely on the name, so the connection of sequence steps and state can be either
 * close or loose.
 * 
 * The main purpose of this class is the way it stores configuration information and then 
 * turns that information into two optional display panels - one for pushbuttons and one
 * for a progress display. 
 * 
 * There are three assumed state names: "NotStarted", "Canceled" and "Completed". The first names the 
 * initial state of the display machine before any other state is set. The second state is
 * outside the normal state sequence. When the machine receives a "cancel" call, the "Canceled" 
 * state is set on, all future changes are blocked, and the old state persists until a "restart"
 * call is received. Lastly, the "Completed" state indicates that the final step has been completed. 
 * 
 * @author pbaker
 *
 */
public class SequenceDisplayMachine implements Observer {
	// record whether the state has been determined from a preceding update
	private boolean stateEstablished;
	// the name of the current state
	private String current;
	// order integer of the current state
	private int currentOrder;
	// order integer of canceled state
	final int canceledFlag = -1;
	
	// the ordered list of states - public to allow coupling to session storage
	public Vector<String> states;
	// ordered list of  flags associated with the states
	private Vector<Boolean> displayFlags;
	// lookup the order integer of a state given its name
	private Map<String, Integer> stateOrder;
	// lookup the ActionListener for a state given its order integer
	private Map<Integer, ActionListener> stateListeners;
	
	
	// the text positioned above the list of steps
	private String pText;
	// the text associated with the canceled status line
	private String cText;
	
	// the path to the "images" directory where various icons must be stored.
	private String imagePath;
	
	// The object holding the step sequence display
	private SequenceStepWidget ssw;
	
	// The object holding the button display
	private ButtonPanel bp;
	
	private JPanel buttonPanel;
	private JPanel sequencePanel;
	private JLabel canceledLabel;
	
	// optionally, this object may be connected to the current state machine so that
	// it can send events. 
	private PmessageStateMachine currentStateMachine;
	
	/**
	 * Display a sequence of steps and their status in the progression.
	 * Each step is shown as text plus an icon that show not-started, in-progress,
	 * or completed. The sequence may include invisible states that are not
	 * displayed. When a system is in one of these states, the prior step is
	 * completed and the next step is not-started and none are in-progress. 
	 * 
	 * Lifecycle:
	 * 1) construct the object
	 * 2) add the steps
	 * 3) add the buttons and the button enabled options
	 * 4) call buildPanels to complete the construction of the Swing components. 
	 * 
	 * @param path  file location where the images director must reside
	 * @param progressText  text appearing above the list of steps
	 */
	public SequenceDisplayMachine(String path, String progressText) {
		imagePath = path;
		stateEstablished = false;
		current = "NotStarted";
		currentOrder = 0;
		pText = progressText;
		
		ssw = new SequenceStepWidget(path);
		bp = new ButtonPanel();
		
		states = new Vector<String>();
		states.add(current);
		ssw.addStep(current);
		displayFlags = new Vector<Boolean>();
		displayFlags.add(false);
		
		stateOrder = new HashMap<String, Integer>();
		stateOrder.put(current, 0);
		
		stateListeners = new HashMap<Integer, ActionListener>();
		
	}
	
	/**
	 * Complete the object by building the display components from the 
	 * previously added steps and buttons. 
	 */
	public void buildPanels() {
		sequencePanel = ssw.makeVerticalPanel(new JLabel(pText));
		buttonPanel = bp.getButtonPanel();
		// the state has not be established at this point in the construction
		current = "NotStarted";
		stateEstablished = false;
		currentStateMachine = null;
		// update both panels
		if (EventQueue.isDispatchThread()) {
			ssw.update();
			bp.updateButtonDisplay(current);
		} else {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					ssw.update();
					bp.updateButtonDisplay(current);
				}
			});
		}
		
	}
	
	/** Name the state in which the machine starts. If the first state is not explicitly
	 * named, then it is assumed to be named "NotStarted".
	 * @param name
	 */
	public void addFirstState(String name) {
		if (current.equals(name)) 
			current = name;
		states.set(0, name);
		stateOrder.put(name, 0);
	}
	
	/**
	 * Add the next step in the sequence. When the sequence advances to the step, invoke
	 * the listener. If displayText is set, show this step as a visible milestone in the 
	 * sequence. Otherwise, do not display it. Throw exception on an attempt to add a duplicate
	 * state name.
	 * @param name
	 * @param listener
	 * @param displayText
	 * @throws Exception
	 */
	public void addNextState(String name, ActionListener listener, String displayText) throws Exception {
		int order = states.size();
		if (states.contains(name)) {
			System.err.printf("SequenceDisplayMachine.addNextState duplicate state name: %s%n", name);
			throw new ConfigurationError();
		}
		states.add(name);
		stateOrder.put(name, order);
		if (displayText == null) {
			displayFlags.add(false);
		} else {
			displayFlags.add(true);
		}
		stateListeners.put(order, listener);
		
		int order2 = ssw.addStep(displayText);
		if (order2 != order) {
			System.err.println("PROGRAM ERROR in SequenceDisplayMachine.addNextState");
			throw new Exception();
		}
	}
	
	/**
	 * Set the current sequence state to the given value, call the listener for the new state
	 * if it is not null, and adjust the display widget. If the name does not name a known state,
	 * write an error message but otherwise do nothing.
	 * 
	 * Optionally, the second parameter refers to the current state machine. This reference is 
	 * saved. When the reference is non-null the procedure sendButtonEvent will forward the 
	 * button name as an event to the state machine as a context-free event where the button
	 * name is the event name. If the reference is null, the procedure is a no-op.
	 * 
	 * @param name the state name
	 * @param stateMachine current state machine - may be null and used without state machine connection.
	 */
	public void setState(String name, PmessageStateMachine stateMachine) {
		current = name;
		stateEstablished = true;
		currentStateMachine = stateMachine;
		if (currentStateMachine != null) 
//			System.out.printf("setState to %s in session %s %n", name, currentStateMachine.sessionId)
		// first deal with the possibility of "Completed"
		if (name.equals("Completed")) {
			currentOrder = states.size() + 1;
		} else if (name.equals("Canceled")) {
			currentOrder = canceledFlag;
		} else {
			// next consider the possibility of erroneous invocation
			if ( !stateOrder.containsKey(name)) {
				System.err.printf("Program Error: attemp to setState in SequenceDisplayMachine to" +
						" invalid state: %s%n", name);
				return;
			}
			// finally do the most common operation
			currentOrder = stateOrder.get(current);
			if ( stateListeners.containsKey(current)) {
				stateListeners.get(current).actionPerformed(new ActionEvent(this, currentOrder, name));
			}
		}
		// update both panels
		if (EventQueue.isDispatchThread()) {
			ssw.update();
			bp.updateButtonDisplay(current);
		} else {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					ssw.update();
					bp.updateButtonDisplay(current);
				}
			});
		}
	}

	/**
	 * Declare the step sequence canceled.
	 */
	public void cancel() {
		int question;
		try {
			question = JOptionPane.showConfirmDialog(null, "Do you really want to cancel this session?" +
			"  \nYou can not undo a cancel command.");
			if (question == JOptionPane.YES_OPTION) {
				try {
					currentStateMachine.acceptContextFreeEvent("AbandonSession");
					setState("Canceled", currentStateMachine);
				} catch (Exception ex) {
					Logger.getLogger(SequenceDisplayMachine.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		} catch (HeadlessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Add a Button to the operator's control button panel.
	 * @param displayText
	 * @param listener
	 */
	public void addButton(String displayText, ActionListener listener) {
		bp.addButton(displayText, listener);
	}
	
	/**
	 * Declare that a certain button is activated in a certain state. 
	 * Buttons may be active in several states. 
	 * @param displayText
	 * @param stateName
	 */
	public void buttonEnabledState(String displayText, String stateName) {
		bp.addButtonEnabledState(displayText, stateName);
	}
	
	/**
	 * Declare that several buttons are activated in a certain state. 
	 * Buttons may be active in several states. 
	 * @param buttonDisplayText
	 * @param stateNames
	 */
	public void addButtonEnabledStates(String buttonDisplayText, List<String> stateNames) {
		if (bp == null)
			System.err.println("Whoops - bp null entering addButtonEnabledStates, build panels first");
		Iterator<String> iter = stateNames.iterator();
		String name;
		while (iter.hasNext()) {
			name = iter.next();
			bp.addButtonEnabledState(buttonDisplayText, name);
		}
		System.out.printf("%n");
	}
	
	/**
	 * Print the button names and the states in which each button is enabled. 
	 */
	public void printEnabledButtons() {
		System.out.println("-------- Buttons enabled ----------");
		Iterator<String> iter1 = bp.stateEnabledButtons.keySet().iterator();
		String b;
		while (iter1.hasNext()) {
			b = iter1.next();
			System.out.printf("button %s -> ", b);
			if (bp.stateEnabledButtons.get(b) == null) {
				System.out.println(" all states%n");
			} else {
				Iterator<String> iter2 = bp.stateEnabledButtons.get(b).iterator();
				String s;
				while (iter2.hasNext() ) {
					s = iter2.next();
					System.out.printf(" %s ", s);
				}
				System.out.printf("%n");
			}
		}
	}
	
	/** 
	 * Add a line to the sequence step display that visibly shows when the 
	 * sequence has been canceled. 
	 * @param displayText
	 */
	public void addCanceledState(String displayText) {
		cText = displayText;
		canceledLabel = new JLabel(displayText);
	}
	
	/**
	 * Return the panel containing buttons. 
	 * @return buttonPanel
	 */
	public JPanel getButtonPanel() {
		return buttonPanel;
	}
	
	/** create and return the panel that shows the status of the sequence. The sequence
	 * should be established prior to calling this routine. 
	 * @return JPanel
	 */
	public JPanel getSequencePanel() {
		sequencePanel = ssw.makeVerticalPanel(new JLabel(pText));
		return sequencePanel;
	}

	enum StepState {NotStarted, Working, Completed}
	
	/**
	 * An object of this class will manage one or more representations of sequence steps
	 * in a GUI panel. The purpose of each representation is to name the sequence step
	 * and portray its state. The steps are referred to by a step sequence number. 
	 * This number sequence starts with 0 as in Java rather than the vulgate.
	 * @author pbaker
	 *
	 */
	private class SequenceStepWidget extends JLabel {
		
		
		private Vector<JLabel> steps;
		private Vector<ImageIcon> timerSteps;
		private Vector<StepState> stepWorking;
		private ImageIcon step0;
		private ImageIcon step1;
		private ImageIcon step2;
		private ImageIcon step3;
		private ImageIcon step4;
		private ImageIcon stepx;
		private ImageIcon stepn; // temporary current step, cycles 1->4 via timer
		private int timerCount;
		private Timer timer;
		private JLabel stateName;
		
		
		public SequenceStepWidget(String dirpath) {
			steps = new Vector<JLabel>();
			timerSteps = new Vector<ImageIcon>();
			stepWorking = new Vector<StepState>();
			timerCount = 0;
			stateName = new JLabel();
			
			String imagePath = dirpath + File.separator + "images";
			step0 = new ImageIcon(imagePath + File.separator + "step0.gif");
			step1 = new ImageIcon(imagePath + File.separator + "step1.gif");
			timerSteps.add(step1);
			step2 = new ImageIcon(imagePath + File.separator + "step2.gif");
			timerSteps.add(step2);
			step3 = new ImageIcon(imagePath + File.separator + "step3.gif");
			timerSteps.add(step3);
			step4 = new ImageIcon(imagePath + File.separator + "step4.gif");
			timerSteps.add(step4);
			stepx = new ImageIcon(imagePath + File.separator + "stepx.gif");
			stepn = step1;

			timer = new Timer(500, new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					timerCount += 1;
					if (timerCount == 4) timerCount = 0;

					Icon newIcon = timerSteps.get(timerCount);
					int nsteps = stepWorking.size();
					for (int i = 0; i < nsteps; ++i) {
						if (displayFlags.elementAt(i) &&
								stepWorking.get(i) == StepState.Working) {
							steps.get(i).setIcon(newIcon);
						}
					}

				}

			});
			timer.start();
		}
		private void _updateIcons() {
			SwingUtilities.invokeLater(new Runnable () {
				public void run() {
					int nsteps = stepWorking.size();
					for (int i = 0; i < nsteps; ++i) {
						switch (stepWorking.get(i)) {
						case NotStarted:
							steps.get(i).setIcon(step0);
							break;
						case Working:
							steps.get(i).setIcon(stepn);
							break;
						case Completed:
							steps.get(i).setIcon(stepx);
						}
					}
					if (canceledLabel != null) {
						if (currentOrder == canceledFlag) {
							canceledLabel.setIcon(stepx);
						} else {
							canceledLabel.setIcon(step0);
						}
					}
					stateName.setText(current);
				}
				
			});
		}
		
		public int numberSteps() {
			return steps.size();
		}
		
		public JLabel getStepLabel(int i) {
			return steps.get(i);
		}
		
		/**
		 * Make and return a JPanel object with GridBagLayout that arranges all the steps in
		 * order vertically as sequence step widgets. Each widget shows the status of the step
		 * graphically. Optionally, you can provide a non-null argument that is placed as the 
		 * item at the top of the list. 
		 * @param firstComponent
		 * @return
		 */
		public JPanel makeVerticalPanel(Component firstComponent) {
			JPanel panel = new JPanel();
			GridBagLayout gbag = new GridBagLayout();
			panel.setLayout(gbag);
			panel.setBorder(BorderFactory.createLineBorder(Color.black));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 1;
			
			// add the first component if it is provided
			if (firstComponent != null) {
				gbag.setConstraints(firstComponent, gbc);
				gbc.gridy += 1;
				panel.add(firstComponent);
			}
			
			// add the sequence steps
			int nsteps = numberSteps();
			for (int i = 0; i < nsteps; ++i) {
				if (displayFlags.elementAt(i)) {
					gbag.setConstraints(steps.get(i), gbc);
					gbc.gridy += 1;
					panel.add(steps.get(i));
				}
			}

			// add the canceled step display if it is provided
			if (canceledLabel != null) {
				gbag.setConstraints(canceledLabel, gbc);
				gbc.gridy += 1;
				panel.add(canceledLabel);
			}
			// add state name display
			JLabel label = new JLabel("State Name");
			gbag.setConstraints(label, gbc);
			gbc.gridy += 1;
			panel.add(label);
			stateName.setText("");
			gbag.setConstraints(stateName, gbc);
			gbc.gridy += 1;
			panel.add(stateName);
			
			
			return panel;
		}

		
		/**
		 * Add a new sequence step at the end of the sequence and return its number. 
		 * @param stepText
		 * @return
		 */
		public int addStep(String stepText) {
			int n = steps.size();
			// note that we create a label even if it is not visible.
			// this is done to make it easier to keep track of step order. 
			JLabel newStep = new JLabel(stepText, step0, SwingConstants.LEFT);
			steps.addElement(newStep);
			stepWorking.addElement(StepState.NotStarted);
			return n;
		}
		
		public void update() {
			int nsteps = stepWorking.size();
			for (int i = 0; i < nsteps; ++i) {
				if ( i < currentOrder) {
					stepWorking.set(i, StepState.Completed);
				} else if ( i == currentOrder) {
					stepWorking.set(i, StepState.Working);
				} else {
					stepWorking.set(i, StepState.NotStarted);
				}
			}
			_updateIcons();
			
		}
		
		
	}
	
	private class ButtonPanel {
		// Buttons in order that they appear in the panel
		private Vector<String> buttonNames;
		// lookup table for the position of a given buttonName
		private Map<String, Integer> buttonOrder;
		// the corresponding actual buttons
		private Vector<JButton> buttons;
		// lookup the button names of buttons which are enabled in a state
		// buttonName->list_of_states
		public Map<String, Vector<String>> stateEnabledButtons;
		// the panel
		private JPanel panel;
		// constraints for the panel's layout manager;
		private GridBagLayout gbag;
		private GridBagConstraints gbc;
		
		private String lastState;
		
		public ButtonPanel() {
			stateEnabledButtons = new HashMap<String, Vector<String>>();
			buttonNames = new Vector<String>();
			buttonOrder = new HashMap<String, Integer>();
			buttons = new Vector<JButton>();
			
			panel = new JPanel();
			panel.setBorder(BorderFactory.createLineBorder(Color.black));
			gbag = new GridBagLayout();
			panel.setLayout(gbag);
			gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 1;
		}
		
		public JPanel getButtonPanel() {
			return panel;
		}
		
		public void updateButtonDisplay(String state) {
			// first disable all buttons
			int nbuttons = buttonNames.size();
			for (int i = 0; i < nbuttons; ++i) {
				buttons.elementAt(i).setEnabled(false); 
			}
			// if the sequence has been canceled - then no buttons are active
			if (currentOrder == canceledFlag) return;
			// if the state is currently unknown - then no buttons are active
			if ( !stateEstablished) return;
			// selectively enable some buttons if any
			lastState = state;
			Iterator<String> iter = buttonNames.iterator();
			Iterator<String> iter2;
			int i;
			String bname;
			// loop on button names
			while (iter.hasNext()) {
				bname = iter.next();
				i = buttonOrder.get(bname);
				// first check for the possibility that the button is active
				// in all states
				if ( stateEnabledButtons.get(bname).size() == 0) {
					buttons.elementAt(i).setEnabled(true);
					continue;
				}
				// now consider the selectively enabled buttons
				iter2 = stateEnabledButtons.get(bname).iterator();
				// loop on states for which this button is enabled
				while (iter2.hasNext()) {
					// is it enabled now?
					if (iter2.next().equals(state)) {
						buttons.elementAt(i).setEnabled(true);
					}
				}			
			}
		}
		
		public void addButton(String displayText, ActionListener listener) {
			JButton newButton = new JButton(displayText);
			int order = buttonNames.size();
			buttonNames.add(displayText);
			buttonOrder.put(displayText, order);
			buttons.add(newButton);
			newButton.addActionListener(listener);
			newButton.setEnabled(false);
			stateEnabledButtons.put(displayText, new Vector<String>());
			
			gbag.setConstraints(buttons.elementAt(order), gbc);
			gbc.gridy += 1;
			panel.add(newButton);
			
		}
		
		public void addButtonEnabledState(String buttonDisplayText, String stateName) {
			if (stateEnabledButtons == null) {
				System.err.printf("addButtonEnabledState: null stateEnabledButtons%n");
				return;
			}
			try {
				if ( !stateEnabledButtons.containsKey(buttonDisplayText)) {
					System.err.printf("addButtonEnabledState called for non-existent button %s %n", buttonDisplayText);
					printEnabledButtons();
					stateEnabledButtons.put(buttonDisplayText, new Vector<String>());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stateEnabledButtons.get(buttonDisplayText).add(stateName);
		}
		
	}
	
	void sendButtonEvent(String buttonName) {
        try {
            currentStateMachine.acceptContextFreeEvent(buttonName);
        } catch (Exception ex) {
            Logger.getLogger(SequenceDisplayMachine.class.getName()).log(Level.SEVERE, null, ex);
        }
	}

	
	/**
	 * @param args
	 */
	
	private static SequenceDisplayMachine to;
	
	/**
	 * main program *has not* tracked program changes - needs revision.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		to = new SequenceDisplayMachine("/Users/pbaker/Coding/OpenBEDM/demo1/identities/player1/config",
				"progress steps");
		
		to.addNextState("step1", null, "step 1 step");
		to.addNextState("step2", null, "step 2 step");
		to.addNextState("step3", null, null);
		to.addNextState("step4", null, "step 4 step");
		to.addNextState("step5", null, "step 5 step");
		
		to.addCanceledState("sequence canceled");
		
		to.addButton("1", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.setState("step1", null);
				
			}});
		to.addButton("2", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.setState("step2", null);
				
			}});
		to.addButton("3", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.setState("step3", null);
				
			}});
		to.addButton("4", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.setState("step4", null);
				
			}});
		to.addButton("5", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.setState("step5", null);
				
			}});
		
		to.addButton("End", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.setState("Completed", null);
				
			}});
		
		to.addButton("Abort", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				to.cancel();
				
			}});
		
		to.buttonEnabledState("1", "NotStarted");
		to.buttonEnabledState("2", "step1");
		to.buttonEnabledState("3", "step2");
		to.buttonEnabledState("4", "step3");
		to.buttonEnabledState("Abort", "step3");
		to.buttonEnabledState("5", "step4");
		to.buttonEnabledState("End", "step5");
		

		to.buildPanels();

		try {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					JFrame frame = new JFrame();
					JPanel panel = new JPanel();
					panel.setLayout(new BorderLayout());
					panel.add(to.getButtonPanel(), BorderLayout.WEST);
					panel.add(to.getSequencePanel(), BorderLayout.EAST);
					
					frame.add(panel);
					frame.setVisible(true);					
				

				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void update(Observable sm, Object obj) {
		PmessageStateMachine.Context psmC = (PmessageStateMachine.Context) obj;
//		System.out.printf("SequenceDisplayMachine.update to state: %s %n", psmC.endState);
		setState(psmC.endState, (PmessageStateMachine)sm);
		
	}

}
