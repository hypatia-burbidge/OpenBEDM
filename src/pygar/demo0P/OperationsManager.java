package pygar.demo0P;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import pygar.communication.MessageAgent;
import pygar.communication.MessageSystemException;


/** The OperationsManager creates a control panel that an operator can use
 * to start, run, and stop the demonstration. 
 * 
 * @author pbaker
 *
 */
public class OperationsManager {
	/** We need one instance of the TeamFrame for each team.
	 * 
	 * @author pbaker
	 *
	 */
	public class OpsMgrFrame extends JFrame {
		public OpsMgrFrame() {
			setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		}
		public static final int DEFAULT_WIDTH = 600;
		public static final int DEFAULT_HEIGHT = 100;
	}

	private JLabel blankDivider;
	private MessageAgent messageAgent;
	
	public OperationsManager(MessageAgent ma, final int xoffset, final int yoffset) {
		messageAgent = ma;
		EventQueue.invokeLater(new Runnable()
		{
			public void run() {
				// create a frame
				OpsMgrFrame frame = new OpsMgrFrame();
				// create panel
				JPanel panel = new JPanel();
//				panel.setLayout(new GridLayout(6,2));
				// add command buttons
				JButton resetButton = new JButton("Reset");
				resetButton.addActionListener(new ResetHandler());
				panel.add(resetButton);
				
				JButton startButton = new JButton("Start");
				panel.add(startButton);
				startButton.addActionListener(new StartHandler());
				
//				// useful to have a button to push for testing
//				JButton testButton = new JButton("Test");
//				testButton.addActionListener(new TestHandler());
//				panel.add(testButton);

				JButton exitButton = new JButton("Exit");
				exitButton.addActionListener(new ExitHandler());
				panel.add(exitButton);
				
				// display panels
				frame.add(panel);
				
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setTitle("Demonstration Control Panel");
				frame.setLocation(xoffset, yoffset);
				frame.setVisible(true);
				
				
				
			}
		});
		
	}
	
	private class ButtonHandler implements ActionListener {
		public ButtonHandler() {
		// TODO
		}
		public void actionPerformed(ActionEvent e) {
			blankDivider.setText("bingo");
		}
	}

	private class ExitHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			DemoZero.aBAN.printLog();
			DemoZero.aGreen.printLog();
			DemoZero.aBlue.printLog();
			System.exit(0);
		}
	}

	private class StartHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.println("start pushed");
			try {
				DemoZero.aBAN.pms.send("OpsMgr", "BAN", "Start");
			} catch (MessageSystemException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private class ResetHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				DemoZero.aBAN.pms.send("OpsMgr", "BAN", "Reset");
			} catch (MessageSystemException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

//	private class TestHandler implements ActionListener {
//		public void actionPerformed(ActionEvent e) {
//			System.out.println(" pressed - Test");
//			DemoZero.aGreen.sms.send("OpsMgr", "GreenTeam", "Test");
//		}
//	}


}
