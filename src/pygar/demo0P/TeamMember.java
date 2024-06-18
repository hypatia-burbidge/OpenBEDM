	/****************************************************************CopyrightNotice
	 * Copyright (c) 2010 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.1
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *    
	 * The blind-agent-mediated negotiation process implemented by this software
	 * is the subject of U.S. Patent 7,685,073. 
	 *******************************************************************************/

package pygar.demo0P;

import java.awt.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.swing.*;
import javax.xml.stream.XMLStreamException;

import pygar.communication.MessageAgent;
import pygar.communication.MessageSystemException;
import pygar.communication.Pmessage;
import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;
import pygar.cryptography.CryptoException;
import pygar.documents.DocumentStoreNotFound;
import pygar.documents.FileStore;
import pygar.identity_authority.KeyNotFound;
import pygar.identity_authority.KeyStoreAccess;
import pygar.identity_authority.KeyStoreType;
import pygar.state.EventListener;
import pygar.state.StateMachine;
import pygar.zoneable.FieldCrypto;
import pygar.zoneable.FieldCryptoXmlTxt0;

/** A TeamMember represents one of the parties in a blind negotiation. 
 * It contains a state machine that performs the functions of the TeamMember
 * according to events received through a messaging system. A GUI panel displays
 * status information during the operation of TeamMember. 
 * 
 * @author pbaker
 *
 */
public class TeamMember extends MessageAgent {
	
//	Declare the elemens of the GUI panel
	private JProgressBar pbar;
	private JLabel progressLabel;
	private JTextArea console;
	private JTextArea npText;
	private JTextArea npBlack;
	private JTextArea matchText;
	private JTextArea matchCryptText;
	
	// save some information about this team member
	private String ownName;
	private Profile profile;
	private FieldCrypto fcrypto;
	private KeyStore keyStore;
	public StateMachine<Pmessage> state;
	public Set<String> sessionGroup;
	public Logger logger;

	/** for this proof of concept, the security critical session key is just
	 * sitting at the following location. Obviously that is bad practice.
	 */
	SecretKey tkey;

	/** TeamFrame holds the GUI for the TeamMember. 
	 * 
	 * @author pbaker
	 *
	 */
	public class TeamFrame extends JFrame {
		private static final long serialVersionUID = 15851401326953785L;
		public TeamFrame(Color color) {
			setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			setBackground(color);
		}
		public static final int DEFAULT_WIDTH = 800;
		public static final int DEFAULT_HEIGHT = 400;
	}

	/**
	 * Update the panel showing the text of the negotiation
	 * position (NP). If the provided text is null, erase the 
	 * panel and write a header; otherwise, append the text.
	 * 
	 * @param text
	 */
	private void updateNpText(final String text) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				if (text == null) {
					npText.setText(null);
					npText.append("Secret Data for this Session\n" + 
							"----------------------------------\n");
					
				} else {
					npText.append(text);
				}
			}
		});
			
	}
	
	/**
	 * Update the panel showing the encrypted version of the negotiation
	 * position (NP). If the provided text is null, erase the 
	 * panel and write a header; otherwise, append the text.
	 * 
	 * @param text
	 */
	private void updateNpBlack(final String text) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				if (text == null) {
					npBlack.setText(null);
					npBlack.append("Encrypted Data for Matching\n" + 
							"----------------------------\n");
					
				} else {
					npBlack.append(text);
				}
			}
		});
			
	}
	
	/**
	 * Update the panel showing the text of the match
	 * results. If the provided text is null, erase the 
	 * panel and write a header; otherwise, append the text.
	 * 
	 * @param text
	 */
	private void updateMatchText(final String text) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				if (text == null) {
					matchText.setText(null);
					matchText.append("Match Results from BAN \n(clear XML text)\n" + 
							"--------------------\n");
					
				} else {
					matchText.append(text);
				}
			}
		});
			
	}
	
	/**
	 * Update the panel showing the encrypted match results.
	 * If the provided text is null, erase the 
	 * panel and write a header; otherwise, append the text.
	 * 
	 * @param text
	 */
	private void updateMatchCryptText(final String text) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				if (text == null) {
					matchCryptText.setText(null);
					matchCryptText.append("Match Results from BAN \n(session key encrypted XML)\n" + 
							"--------------------\n");
					
				} else {
					matchCryptText.append(text);
				}
			}
		});
			
	}
	

	/**
	 * Read text from a file and append it to the text area in a panel.
	 * @param filename
	 * @param ta
	 */
	private void readInText(final String filename, final JTextArea ta) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				String line;
				int count = 0;
				//System.out.printf("readInText from %s%n", filename);
				File f = new File(filename);
				try {
					if (f.canRead()) {
						FileReader fr = new FileReader(f);
						BufferedReader br = new BufferedReader(fr);
						while (br.ready()) {
							line = br.readLine();
							ta.append(line + "\n");
							if (count++ > 1000) {
								ta.append(".....listing truncated - use XML reader for full file: " + filename + "\n");
								//System.out.println("readInText appended 1000 lines");
								return;
							}
						}
					}
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}
	
	/**
	 * Clear all the text panels used by this party's GUI and then try to fill
	 * the panel with available information from a file. If the file is not available
	 * leave only the header visible in the panel. 
	 */
	private void readAllText() {
		// clear text areas and restore the header lines
		updateNpText(null);
		updateNpBlack(null);
		updateMatchText(null);
		updateMatchCryptText(null);
		//
		String f0 = profile.dataDirPath +  File.separator 
		+ ownName + File.separator;
		
		readInText(f0 + "NPosition.xml", npText);
		readInText(f0 + "NPosition.blk.xml", npBlack);
		readInText(f0 + "BFAmatches.xml", matchText);
		readInText(f0 + "BFAmatches.blk.xml", matchCryptText);
	}

	/** Update the progress bar and progress label in the panel. Use the
	 * state of the system to show the approximate point the system now
	 * occupies in the progression of state in the demonstration session. 
	 * @param state
	 */
	private void updatePBar(final String state) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				int value = 0;
				String label = "";
				if (state.equals("s0")) {
					value = 0;
					label = "ready";
				} 
				if (state.equals("sWaitState")) {
					value = 33;
					label = "waiting go ahead";
				} 
				if (state.equals("sWaitResults")) {
					value = 66;
					label = "waiting results";
				} 
				if (state.equals("sEnd")) {
					value = 100;
					label = "finished";
				} 
				pbar.setValue(value);
				pbar.setString(label);
				pbar.setIndeterminate(false);
				progressLabel.setText(label);
				pbar.setIndeterminate(false);
			}
		});
	}
	
	/**
	 * Update the progress label of the panel and set the progress bar
	 * into the indefinite state - a state in which it appears to change
	 * and convey the idea of work in progress. Obviously, the routine should
	 * be called at the start of a step that may require some time before
	 * landing in a longer duration state. 
	 * @param label
	 */
	public void updatePBarIndef(final String label) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				pbar.setIndeterminate(true);
				progressLabel.setText(label);
			}
		});
		
	}
	
	/** 
	 * Update the progress label and the progress bar. This function is most
	 * useful when it is called successively during a long calculation to 
	 * show progress towards the completion of the calculation.
	 * @param percent
	 * @param label
	 */
	public void updatePBarPercent(final int percent, final String label) {

		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				pbar.setValue(percent);
				progressLabel.setText(label);
				pbar.setString(label);
				pbar.setIndeterminate(false);
			}
		});
	}
	
	/** 
	 * Append a message to the console. These message log the progress of the
	 * session for this party.
	 * @param message
	 */
	public void updateConsole(final String message) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				console.append(message);
			}
		});
		
	}


	/**
	 * This inner class provides a log facility for debugging. It keeps messages
	 * in memory until they are printed. Thus, it has two drawbacks: it can fill up,
	 * and a crash may throw away the messages before they are printed. 
	 * 
	 * @author pbaker
	 *
	 */
	public class Logger implements Observer {
		
		StringWriter log;
		@SuppressWarnings("unchecked")
		public void update(Observable o,  Object arg) {
			StateMachine<Pmessage>.Context c = (StateMachine.Context) arg;
			
			System.out.println("New state: " + c.endState + " after event: " + c.event
					+ " prior state: " + c.startState);
			
			
			log.write( "New state: " + c.endState + " after event: " + c.event
					+ " prior state: " + c.startState +  "\n");
			
			
			updatePBar(c.endState);
			
		}
		
		public Logger (String name) {
			log = new StringWriter();
			log.write("Logfile: " + name + "\n");
		}
		
		public void printOut() {
			System.out.println(log.toString());
		}
		
		public void writeln(String s) {
			log.write(">" + s + "\n");
		}
		
	}

	
	public void printLog() {
		logger.printOut();
		
	}
	

	/** 
	 * A single instance of this type is needed to represent one party or team member
	 * in the negotiation session. Each party is associated with a file system directory
	 * where it keeps information. It is governed by a state machine that is created
	 * by the constructor of TeamMember and it communicates by message passing. It maintains
	 * a GUI panel to show progress and data files.
	 * 
	 * @param p an instance of a Profile object which supplies configuration
     *    information. 
	 * @param xoffset upper left corner location of gui panel
	 * @param yoffset upper left corner location of gui panel
	 * @param color color of the frame of the gui panel
	 * @throws MessageSystemException
	 * @throws ConfigurationError
	 */

	public TeamMember( final Profile p, 
			final int xoffset, final int yoffset, final Color color) 
	throws MessageSystemException, ConfigurationError {
		super(p, p.entityAlias);
		ownName = p.entityAlias;
		profile = p;
		state = new StateMachine<Pmessage>("s0");
		// get access to the keystore where the PKE keys are held. 
		try {
			KeyStoreAccess ksa = p.getKeyStoreAccess();
			keyStore = ksa.getKeyStore(KeyStoreType.PUBLIC);
//			keyStore = profile.getKeyStoreAccess().getKeyStore(KeyStoreType.PUBLIC);
			Enumeration<String> aliases = keyStore.aliases();
			System.out.printf("public keystore for %s has following certificates%n", name);
			while (aliases.hasMoreElements()) {
				System.out.printf("alias:%s%n", aliases.nextElement());
			}
		} catch (Exception e1) {
			System.out.printf("failed to get public keystore for %s %n", name);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// initialize the software for encryption
		try {
			_initCrypto(profile.configurationDirPath);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			logger.writeln("***Error: InvalidKeyException");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			logger.writeln("***Error: NoSuchAlgorithmException");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			logger.writeln("***Error: NoSuchPaddingException");
		} catch (ConfigurationError e) {
			e.printStackTrace();
			logger.writeln("***Error: ConfigurationError");
		}

		// unlike a real application, this class already knows who is in the 
		// session group. We set that up here:
		sessionGroup = new HashSet<String>();
		sessionGroup.add("GreenTeam");
		sessionGroup.add("BlueTeam");
		
		// Prepare the state machine that governs this classes run-time behavior
		_initStateMachine();
		
		// add a place to log progress messages to document the demonstration run
		logger = new Logger(name);
		state.addObserver(logger);
		
		// launch the GUI for the team member
		try {
			EventQueue.invokeAndWait(new Runnable()
			{
				public void run() {
					// create a frame
					TeamFrame frame = new TeamFrame(color);
					// create panel
					JPanel panel = new JPanel();
					pbar = new JProgressBar(0, 100);
					pbar.setStringPainted(true);
					pbar.setValue(0);
					pbar.setString("started - no reset");
					
					JLabel currentLabel = new JLabel("Current Status");
					panel.add(currentLabel);
					panel.add(pbar);
					progressLabel = new JLabel("");
					panel.add(progressLabel);
					
					console = new JTextArea("console messages\n---\n", 20,70);
					JScrollPane scrollPane = new JScrollPane(console);

					npText = new JTextArea("Secret Data for Negotiation\n" + 
							"-------------------\n", 200,100);
					JScrollPane npPane = new JScrollPane(npText);

					npBlack = new JTextArea("Encrypted Data for Negotiation\n" + 
							"-------------------\n", 200,100);
					JScrollPane blkPane = new JScrollPane(npBlack);

					matchText = new JTextArea("Match Results from BAN\n" + 
							"--------------------\n", 200,100);
					JScrollPane matchPane = new JScrollPane(matchText);
					
					matchCryptText = new JTextArea("EncryptedMatch Results from BAN\n" + 
							"--------------------\n", 200,100);
					JScrollPane matchCryptPane = new JScrollPane(matchCryptText);
					
					JTabbedPane tabbedPane = new JTabbedPane();
					tabbedPane.addTab("Console", scrollPane);
					tabbedPane.addTab("Secret Data", npPane);
					tabbedPane.addTab("Encrypted Data", blkPane);
					tabbedPane.addTab("Match Results (encrypted)", matchCryptPane);
					tabbedPane.addTab("Match Results (clear)", matchPane);
					
					tabbedPane.setSelectedIndex(0);
										
					frame.add(panel, BorderLayout.NORTH);
					frame.add(tabbedPane, BorderLayout.CENTER);

					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setTitle(profile.entityTitle);

                    frame.setLocation(xoffset, yoffset);
					console.setVisible(true);
					npText.setVisible(true);
					npBlack.setVisible(true);
					matchText.setVisible(true);
					matchCryptText.setVisible(true);
					frame.setVisible(true);
					

				}
			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// add text to displays
		readAllText();

		// start a thread that waits for messages that are processed as events
		// by the state machine. 
		t.start();

	}

	/** 
	 * Provide a process to run on the team member's thread, to watch for message
	 * events, and to pass message events to the state machine. 
	 */
	public void run() {
		System.out.printf("TeamMember named %s starting on threadId %d%n", ownName, Thread.currentThread().getId());
		// wait for events
		while (true) {
			//SimpleMessageSystem.messageLog.printf("try %s %n", ownName);
			if (this.pms.hasNext()) {
				try {
					Pmessage msg = this.pms.next();
					state.acceptEvent(msg.getSubject(), msg);
				} catch (MessageSystemException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				// slow the whole session down, it flashes by too quickly otherwise
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Thread.yield();

		}		
	}
	
	/**
	 * Establish the state transition diagram for the state machine. In compliance with
	 * the state machine design, the code executed for state transitions is also established
	 * during this procedure - see the StateMachine documentation {@link pygar.state.StateMachine} 
	 */
	private void _initStateMachine() {
		// set initial state
		state.setState("s0");

		// next add a transition that returns the statemachine to the 
		// initial state, s0 upon receipt of the Reset event. 
		EventListener<Pmessage> eReset = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) {
				// TODO reset any local storage or variables
				// set state
				state.setState("s0");
				updateConsole("Received reset request - ready for next session.\n");

				String f1Name = profile.dataDirPath + File.separator + profile.entityAlias +
				File.separator + "NPosition.blk.xml";
				File f1 = new File(f1Name);
				if (f1.exists()) {
					System.out.printf("  Team member " + profile.entityAlias + " reset deleting %s%n", f1Name);
					f1.delete();
				} else {
					System.out.printf("  Team member " + profile.entityAlias + " reset does not see %s%n", f1Name);
				}
				
				String f2Name = profile.dataDirPath + File.separator + profile.entityAlias +
				File.separator + "NPosition.blk.bin";
				File f2 = new File(f2Name);
				if (f2.exists()) {
//					System.out.printf("   reset deleting %s%n", f2Name);
					f2.delete();
//				} else {
//					System.out.printf("    reset does not see %s%n", f2Name);
				}
				
				String f3Name = profile.dataDirPath + File.separator + profile.entityAlias +
				File.separator + "BFAmatches.blk.bin";
				File f3 = new File(f3Name);
				if (f3.exists()) {
//					System.out.printf("   reset deleting %s%n", f2Name);
					f3.delete();
//				} else {
//					System.out.printf("    reset does not see %s%n", f2Name);
				}
				
				String f4Name = profile.dataDirPath + File.separator + profile.entityAlias +
				File.separator + "BFAmatches.blk.xml";
				File f4 = new File(f4Name);
				if (f4.exists()) {
//					System.out.printf("   reset deleting %s%n", f2Name);
					f4.delete();
//				} else {
//					System.out.printf("    reset does not see %s%n", f2Name);
				}
				
				String f5Name = profile.dataDirPath + File.separator + profile.entityAlias +
				File.separator + "BFAmatches.xml";
				File f5 = new File(f5Name);
				if (f5.exists()) {
//					System.out.printf("   reset deleting %s%n", f2Name);
					f5.delete();
//				} else {
//					System.out.printf("    reset does not see %s%n", f2Name);
				}
				
				readAllText();
			}
			
		};
		
		state.addTransition(null, "Reset", "s0", eReset);
		
		// The next transition is followed when the BAN asks this
		// TeamMember to lead the session. 
		EventListener<Pmessage> eInit = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) throws MessageSystemException {
				// Protocol: create a session key
				// Protocol: save session key for own use
				// Protocol: send session key to other parties as SKey event
				// Demo0: session key was created and distributed before
				// the demo starts. Simply retrieve send the event. 
				logger.writeln("Create and distribute a session key.");
				String tm;
				updateConsole("Select session key at the request of the BAN\n");
				Iterator<String> i = sessionGroup.iterator();
				while ( i.hasNext()) {
					tm = i.next();
					if ( ! tm.equals(ownName)) {
						pms.send(ownName, tm, "SKey");
						updateConsole("Send session key to " + tm + "\n");
					}
				}
				// send Start event to self
				pms.send(ownName, ownName, "Start");

			}
		};
		
		state.addTransition("s0", "InitSession", "sWaitStart", eInit);

		// The next transition is followed when the team member waits for 
		// another team member to take the lead and send the session key.		
		EventListener<Pmessage> eSKey = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) {
				// Protocol:  accept and store the session key in the SKey event.
				// Demo0: session key was created and distributed before
				// the demo starts. Simply retrieve the key and send the event. 
				updateConsole("Received session key from " + msg.getSenderId() + 
				". Join negotiation session\n");

				logger.writeln("Verify, decrypt and save the session key.");

				// Protocol: select the unencrypted data for negotiation
				// Protocol: encrypt and send data to BAN as event NPosition (for negotiation position)
				// Demo0: the data set is fixed, just encrypt and send to BAN
				logger.writeln("Encrypt negotiation position and send to BAN");
				// Encrypt negotiation position and send to BAN
				_prepareAndSendNPosition();
			}

		};

		state.addTransition("s0", "SKey", "sWaitResults", eSKey);

		// The next transition is only used by the team member who starts the 
		// session. That team member issues a Start to itself which 
		// initiates steps that encrypt and send data to the BAN.
		//  

		EventListener<Pmessage> eStart = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) {
				// Protocol: select the unencrypted data for negotiation
				// Protocol: encrypt and send data to BAN as event NPosition (for negotiation position)
				// Demo0: the data set is fixed, just encrypt and send to BAN
				// Encrypt negotiation position and send to BAN
				logger.writeln("Encrypt negotiation position and send to BAN");
				_prepareAndSendNPosition();

			}
		};
		
		state.addTransition("sWaitStart", "Start", "sWaitResults", eStart);
		
		
		// Finally, the BAN sends negotiation results in the MatchResult event
		// We process the results and the session sequence is finished.
		
		EventListener<Pmessage> eMatch = new EventListener<Pmessage>() {
			public void eventHandler(Pmessage msg, String event) {
				// remove pke decryption from match results
				logger.writeln("Decypt negotiation results from the BAN");
				String inName;
				inName = profile.dataDirPath +  File.separator 
				+ ownName + File.separator + "BFAmatches.blk.bin";
				System.out.printf("    decrypt match results in %s%n", inName);
				String outName = _pkeDecrypt(inName, ownName);

				readInText(outName, matchCryptText);
				
				// remove partial encryption by session key
				String clrName = profile.dataDirPath +  File.separator
				+ ownName + File.separator + "BFAmatches.xml";
				_decryptPartiallyEncrypted(outName, clrName);

				readInText(clrName, matchText);

			}
		};
		
		state.addTransition("sWaitResults", "MatchResult", "sEnd", eMatch);	
		// the following was used for testing only
		state.addTransition("s0", "Test", "sEnd", eMatch);		
		
	}
	
	private void _decryptPartiallyEncrypted(String inName, String outName) {
		FileStore fs = new FileStore();
		InputStream inStream;
		try {
			inStream = fs.InputStoreStream(0, inName, "");

			OutputStream outStream;
			try {
				outStream = fs.OutputStoreStream(0, outName, "");
				try {
					System.out.printf("_decryptPartiallyEncrypted from %s%n" 
							+ "   to %s%n", inName, outName);

					fcrypto.decryptPartiallyEncryptedStream(inStream, outStream, profile.getEncryptedFieldTable());

					try {

						outStream.close();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.writeln("***Error: IOException");
						e.printStackTrace();
					}
				} catch (XMLStreamException e) {
					logger.writeln("***Error: XMLStreamException");
					e.printStackTrace();
				} catch (CryptoException e) {
					logger.writeln("***Error: CryptoException");
					e.printStackTrace();
				} catch (ConfigurationError e) {
					logger.writeln("***Error: ConfigurationError");
					e.printStackTrace();
				}
			} catch (DocumentStoreNotFound e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


		} catch (DocumentStoreNotFound e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

	}
	
        
	public void _initCrypto(String dirPath) throws ConfigurationError, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		/** warning - the code depends on oddities of demo0 */
		// retrieve session key value
		String filePath = dirPath + File.separator
		+ ownName + File.separator;
		// retrieve the key
		tkey = profile.sessionKeyStore.getKey(ProfileDemo0.sessionID);
		// set up the Xml encryption engine
		fcrypto = new FieldCryptoXmlTxt0(profile.getCrypto(), tkey, profile.getEncryptedFieldTable());

	}
	
	private String _pkeEncrypt() {
		logger.writeln("   Applying PKE encryption");
		InputStream in;
		String outName;
		outName = profile.dataDirPath +  File.separator 
		+ ownName + File.separator
		+ "NPosition.blk.bin";
		
		try {
			in = new FileInputStream(profile.dataDirPath +  File.separator
					+ ownName + File.separator
					+ "NPosition.blk.xml");
			try {
				DataOutputStream out;
				out = new DataOutputStream(
						new FileOutputStream(outName));

				profile.getCrypto().encryptStream("BAN", in, out);
				out.close();
			
			} catch (FileNotFoundException e) {
				logger.writeln("***Error: FileNotFoundException");
				e.printStackTrace();
			} catch (KeyNotFound e) {
				logger.writeln("***Error: KeyNotFound");
				e.printStackTrace();
			} catch (ConfigurationError e) {
				logger.writeln("***Error: ConfigurationError");
				e.printStackTrace();
			} catch (Exception e) {
				logger.writeln("***Error: Exception");
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			logger.writeln("***Error: FileNotFoundException");
			e.printStackTrace();
		}
		
		return outName;
	}
	
	private void _partiallyEncrypt() {
		FileStore fs = new FileStore();
		InputStream inStream;
		try {
			inStream = fs.InputStoreStream(0, 
					profile.dataDirPath +  File.separator 
					+ ownName + File.separator
					+ "NPosition.xml",
			"");

			OutputStream outStream;
			try {
				outStream = fs.OutputStoreStream(0,
						profile.dataDirPath +  File.separator
						+ ownName + File.separator
						+ "NPosition.blk.xml",
				"");
				try {

					fcrypto.partiallyEncryptStream(inStream, outStream, profile.getEncryptedFieldTable());

					try {

						outStream.close();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.writeln("***Error: IOException");
						e.printStackTrace();
					}
				} catch (XMLStreamException e) {
					logger.writeln("***Error: XMLStreamException");
					e.printStackTrace();
				} catch (CryptoException e) {
					logger.writeln("***Error: CryptoException");
					e.printStackTrace();
				} catch (ConfigurationError e) {
					logger.writeln("***Error: ConfigurationError");
					e.printStackTrace();
				}
			} catch (DocumentStoreNotFound e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


		} catch (DocumentStoreNotFound e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// display partially encrypted text on the monitor panel
		String f0 = profile.dataDirPath +  File.separator 
		+ ownName + File.separator;
		
		readInText(f0 + "NPosition.blk.xml", npBlack);

	}
	
	private void _prepareAndSendNPosition() {
		updatePBarIndef("encrypting...");
		// first encrypt with session key retaining document structure
		_partiallyEncrypt();
		updateConsole("Partially encrypt negotiation position document with session key\n");
		
		// next encrypt with BAN's public PKE key concealing content and structure
		// the routine returns the binary file where the encrypted content is located
		String binFileName = _pkeEncrypt();
		updateConsole("Fully encrypt the partially-encrypted negotiation-position document \n" + 
				"     with public key of the BAN\n");
		// prepare a message containing the encypted negotiation position
		Pmessage pmsg = new Pmessage(ownName, "BAN", "NPosition");
		pmsg.setBody(pmsg.new PmessageBody());
		pmsg.getBody().setFileBodyName(binFileName);
		
		updateConsole("Send the twice encrypted document to the BAN \n" + 
				"       (protocol requires digital signature but demo SW does not sign it");
		// send the message to the BAN as an event
		try {
			pms.send(pmsg);
		} catch (MessageSystemException e) {
			System.err.printf("Exception while sending NPosition from %s to BAN%n", ownName);
			e.printStackTrace();
		}

	}
	
	private String _pkeDecrypt(String inFile, String entity) {
		logger.writeln("   Removing PKE encryption");
		DataInputStream in;
		
		String outName;
		outName = profile.dataDirPath +  File.separator 
		+ entity + File.separator
		+ "BFAmatches.blk.xml";
		
		System.out.printf("TM %s decrypts %s to %s%n", ownName, inFile, outName);
		updatePBarIndef("decrpyt PKE...");
//		showProgressMessage("removing PKE Encryption from " + entity + "...");
		
			try {
				in = new DataInputStream(
						new FileInputStream(inFile));
				OutputStream out = new FileOutputStream(outName);

				profile.getCrypto().decryptStream(in, out);
				out.close();
				in.close();
			
			} catch (FileNotFoundException e) {
				logger.writeln("***Error: FileNotFoundException");
				e.printStackTrace();
			} catch (KeyNotFound e) {
				logger.writeln("***Error: KeyNotFound");
				e.printStackTrace();
			} catch (ConfigurationError e) {
				logger.writeln("***Error: ConfigurationError");
				e.printStackTrace();
			} catch (Exception e) {
				logger.writeln("***Error: Exception");
				e.printStackTrace();
			}
		
		return outName;
	}


}

