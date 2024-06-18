
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import pygar.communication.MessageAgent;
import pygar.communication.MessageSystemException;
import pygar.communication.Pmessage;
import pygar.configuration.ConfigurationError;
import pygar.configuration.DocumentError;
import pygar.configuration.Profile;
import pygar.identity_authority.KeyNotFound;
import pygar.identity_authority.KeyStoreAccess;
import pygar.identity_authority.KeyStoreType;
import pygar.state.EventListener;
import pygar.state.StateMachine;

/** A BAN represents a Blind Agent Negotiator who works with the TeamMember instances to find
 * matches in encrypted negotiating positions presented by each TeamMember. A GUI panel displays
 * status information during the operation of the BAN. 
 * 
 * @author pbaker
 *
 */
public class BAN extends MessageAgent {
	
	private JProgressBar pbar;
	private JLabel progressLabel;
	private JTextArea console;
	private JTextArea matchText;
	private JButton similarityButton;
	private final String ownName;
	private Profile profile;
	private KeyStore keyStore;

	public StateMachine<Pmessage> state;
	public Set<String> sessionGroup;
	// keep track of arriving data files
	public Map<String, Boolean> dataReady;
	// record the names of data files (prior to decryption)
	public Map<String, String> dataFiles;
	// record the names of data files after decryption
	public Map<String, String> matchFiles;
	
	public CompareFiles compareFiles;
	
	
	/** Update the progress bar and progress label in the panel. Use the
	 * state of the system to show the approximate point the system now
	 * occupies in the progression of state in the demonstration session. 
	 * @param state
	 */
	public void updatePBar(final String state) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				int value = 0;
				String label = "";
				if (state.equals("s0")) {
					value = 0;
					label = "ready";
				} 
				if (state.equals("sWaitData")) {
					value = 33;
					label = "waiting";
				} 
				if (state.equals("sWaitResults")) {
					value = 999;
				} 
				if (state.equals("sEnd")) {
					value = 100;
					label = "finished";
					similarityButton.setVisible(true);
				} 
				pbar.setValue(value);
				pbar.setString(label);
				pbar.setIndeterminate(false);
				progressLabel.setText(label);
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
				pbar.setValue(0);
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
				pbar.setIndeterminate(false);
				pbar.setValue(percent);
				pbar.setString(label);
				progressLabel.setText(label);
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
							if (count++ > 200) {
								ta.append(".....listing truncated - use XML reader for full file: " + filename + "\n");
								//System.out.println("readInText appended 200 lines");
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
		updateMatchText(null);
		//
		String f0 = profile.dataDirPath +  File.separator
		+ ownName + File.separator;
		
		readInText(f0 + "BFAmatches.xml", matchText);
	}
	
	public Logger logger;
	
	/** for this proof of concept, the security critical session key is just
	 * sitting at the following location. Obviously that is bad practice.
	 */
	SecretKey tkey;

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
		// n.b. the javadoc process chokes on the following procedure so we need to comment out the body to 
		// create the documentation.
		@SuppressWarnings("unchecked")
		public void update(Observable o,  Object arg) {
			StateMachine<Pmessage>.Context c = (StateMachine.Context) arg;
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

	public void updateConsole(final String message) {
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() {
				console.append(message);
			}
		});
		
	}

	/** BANFrame holds the GUI for the Blind Agent Negotiaton
	 * 
	 * @author pbaker
	 *
	 */
	public class BANFrame extends JFrame {
		private static final long serialVersionUID = -5647495911679980513L;
		public BANFrame() {
			setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		}
		public static final int DEFAULT_WIDTH = 600;
		public static final int DEFAULT_HEIGHT = 400;
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
					matchText.append("Match Results from ban\n" + 
							"--------------------\n");
					
				} else {
					matchText.append(text);
				}
			}
		});
			
	}


	
	public void printLog() {
		logger.printOut();
		
	}
	
	/**
	 * The ActionListener for the button on the panel that asks to display the 
	 * results of the matching process.
	 * @author pbaker
	 *
	 */
	private class SimilarityHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.out.printf("user requests display of similarity %d %n",
					compareFiles.reportedResults);
			try {
				compareFiles.initDisplay();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			compareFiles.displayResults();
		}
	}

	/** Construct the infrastructure for the BAN.
	 * @param profileInstance an instance of profile with the correct subsystem information. 
	 * @param xoffset
	 * @param yoffset
	 * @throws MessageSystemException
	 * @throws ConfigurationError
	 */
	public BAN( final Profile profileInstance, final int xoffset, final int yoffset) 
	throws MessageSystemException, ConfigurationError {
		super(profileInstance, profileInstance.entityAlias);
		ownName = profileInstance.entityAlias;
		profile = profileInstance;
		state = new StateMachine<Pmessage>("xx");
		
		// create an instance of software that compares the encrypted negotiation
		// positions. Then provide that instance to a pointer to ourself. This step
		// is necessary for controlling the progress bar and messages during file matching.
		compareFiles = new CompareFiles(profile.dataDirPath);
		compareFiles.setBAN(this);
		
		// get access to the keystore where the PKE keys are held. 
		try {
			KeyStoreAccess ksa = profile.getKeyStoreAccess();
			keyStore = ksa.getKeyStore(KeyStoreType.PUBLIC);
//			keyStore = profile.getKeyStoreAccess().getKeyStore(KeyStoreType.PUBLIC);
			Enumeration<String> aliases = keyStore.aliases();
			System.out.printf("public keystore for %s has following certificates%n", name);
			while (aliases.hasMoreElements()) {
				System.out.printf("alias:%s%n", aliases.nextElement());
			}
		} catch (Exception e1) {
			System.err.printf("failed to get public keystore for %s %n", name);
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// unlike a real application, this class already knows who is in the 
		// session grouprofileInstance. We set that up here:
		sessionGroup = new HashSet<String>();
		sessionGroup.add("Green Team");
		sessionGroup.add("Blue Team");
		
		// We will keep track of whether the team member sent data with the 
		// following map:
		dataReady = new HashMap<String, Boolean>();
		dataReady.put("GreenTeam", false);
		dataReady.put("BlueTeam", false);
		// initialize storage of file names
		dataFiles = new HashMap<String, String>();
		matchFiles = new HashMap<String, String>();
		
		// Prepare the state machine that governs this classes run-time behavior
		_initStateMachine(state);
		
		// add a place to log progress messages to document the demonstration run
		logger = new Logger(name);
		state.addObserver(logger);

		try {
			EventQueue.invokeAndWait(new Runnable()
			{
				public void run() {
					// create a frame
					BANFrame frame = new BANFrame();
					// create panel
					JPanel panel = new JPanel();
					pbar = new JProgressBar(0, 100);
					pbar.setSize(3 * pbar.getWidth(), pbar.getHeight());
					pbar.setStringPainted(true);
					pbar.setValue(0);
					pbar.setString("started - no reset");
					pbar.setSize(3 * pbar.getWidth(), pbar.getHeight());

					JLabel currentLabel = new JLabel("Current Status");
					panel.add(currentLabel);
					panel.add(pbar);
					progressLabel = new JLabel("");
					panel.add(progressLabel);
					
					console = new JTextArea("console messages\n---\n", 20,70);
					JScrollPane scrollPane = new JScrollPane(console);
					
					matchText = new JTextArea("Match Results from ban\n" + 
							"--------------------\n", 200,100);
					JScrollPane matchPane = new JScrollPane(matchText);
					
					JTabbedPane tabbedPane = new JTabbedPane();
					tabbedPane.addTab("Console", scrollPane);
					tabbedPane.addTab("Match Results", matchPane);
					

					tabbedPane.setSelectedIndex(0);
					
					JPanel southPanel = new JPanel();
					similarityButton = new JButton("Display Similarity Matches");
					similarityButton.addActionListener(new SimilarityHandler());
					similarityButton.setVisible(false);
					southPanel.add(similarityButton);
					
					frame.add(panel, BorderLayout.NORTH);
					frame.add(tabbedPane, BorderLayout.CENTER);
					frame.add(southPanel, BorderLayout.SOUTH);
					
					
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setTitle(profile.entityTitle);
                    
					frame.setLocation(xoffset, yoffset);
					console.setVisible(true);
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
		
		readAllText();
		
		// start a thread that waits for messages that are processed as events
		// by the state machine. 
		t.start();
		
	}

	/** 
	 * Provide a process to run on the BAN's thread, to watch for message
	 * events, and to pass message events to the state machine. 
	 */
	public void run() {
		System.out.printf("BAN named %s starting on threadId %d%n", ownName, Thread.currentThread().getId());
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
	 * during thi procedure - see the StateMachine documentation {@link pygar.state.StateMachine} 
	 */
	private void _initStateMachine(StateMachine<Pmessage> machine) throws MessageSystemException {
		
		
		// set initial state
		state.setState("s0");
		
		// first add a transition that returns the statemachine to the 
		// initial state, s0
		EventListener<Pmessage> eReset = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) throws MessageSystemException {
				// Reset any local storage or variables
				System.out.println("BAN reset invoked");
				updateConsole("BAN has reset and is ready for a session\n");
				Iterator<String> i = dataReady.keySet().iterator();
				String tm;
				while (i.hasNext()) {
					tm = i.next();
					dataReady.put(tm, false);
				}
				// clear results file
				String fName = profile.dataDirPath + File.separator + profile.entityAlias +
				File.separator + "BFAmatches.xml";
				File f1 = new File(fName);
				if (f1.exists()) {
					System.out.printf("   BAN reset deleting %s%n", fName);
					f1.delete();
				} else {
					System.out.printf("    BAN reset does not see %s%n", fName);
				}

					
				// set state
				state.setState("s0");
				// send Reset events to team members
				i = dataReady.keySet().iterator();
				while (i.hasNext()) {
					tm = i.next();
					pms.send(ownName, tm, "Reset");
					updateConsole("     BAN asks " + tm + " to reset.\n");
				}
				readAllText();
			}
		};
		
		state.addTransition(null, "Reset", "s0", eReset);
		
		// Begin session when a Start event arrives from the OpsMgr of the demonstration
		EventListener<Pmessage> eStart = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) throws MessageSystemException {
				// send InitSession to one of team members
				updateConsole("Begin a session\n");
				pms.send(ownName, "BlueTeam", "InitSession");
				updateConsole("BAN asks Blue Team to begin the negotiation\n");
			}
		};
		
		state.addTransition("s0", "Start", "sWaitData", eStart);
		
		// Now we wait around until both team members have sent data 
		// When that occurs we send ourself the DataReady event to 
		// leave the sWaitData state
		EventListener<Pmessage> eData = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) throws MessageSystemException {
				// receive and store data from team member
				logger.writeln("Received negotiation position from: " + msg.getSenderId());
				dataReady.put(msg.getSenderId(), true);
//				System.out.printf("  BAN received data from %s%n", msg.senderId);
				// save the file name
				if (msg.getBody() == null) {
					System.out.println("message body is null!!!");
				} else if (msg.getBody().getFileBodyName() == null) {
					System.out.println("message.body.fileBodyName is null!!!");
				}
				dataFiles.put(msg.getSenderId(), msg.getBody().getFileBodyName());
				updateConsole("Receive encrypted negotiation postion from " + msg.getSenderId() +
						"\n            (protocol demands signature verification - skipped for demo\n");
				
				// check if all data has been received, if so, 
				// send self an event to leave sWaitData state
				Boolean ready = true;
				String tm;
				Iterator<String> i = dataReady.keySet().iterator();
				while (i.hasNext()) {
					tm = i.next();
//					System.out.printf("   test ready for %s find %s%n", tm, dataReady.get(tm));
					ready = ready && dataReady.get(tm);
				}
				if (ready) {
					pms.send(ownName, ownName, "DataReady");
				}
				
			}
		};
		
		// Note that this is a loopback transition
		state.addTransition("sWaitData", "NPosition", "sWaitData", eData);
		
		// handle the DataReady event by removing pke encryption, matching
		// the session key encrypted negotiation positions, PKE encrypting the 
		// results for each party and sending encrypted results to the parties.
		EventListener<Pmessage> eMatchData = new EventListener<Pmessage>()
		{
			public void eventHandler(Pmessage msg, String event) {
				System.out.println("BAN has all the data: ");
				Iterator<String> iNames = dataFiles.keySet().iterator();
				String ename;
				String fname;
				
				// decrypt the pke encryption on the data files.
				// the partial encryption is not affected
				updatePBarIndef("decrypting PKE...");
				while (iNames.hasNext()) {
					ename = iNames.next();
					fname = dataFiles.get(ename);
					System.out.printf("    file:%s%n", fname);
					console.append("Decrypting using public key of " + ename + "\n");
					_pkeDecrypt(fname, ename);
				}
				
				// For the demonstration there are exactly two entries.
				// Raise and error if this is not true.
				// then find their names and match them
				Object[] entities = dataFiles.keySet().toArray();
				if (entities.length != 2) {
					System.out.printf("Error, there are %d entities, should be 2%n", 
							entities.length);
				}
				
				System.out.printf("    entity 1 for datafile %s%n", (String)entities[0]);
				System.out.printf("    entity 2 for datafile %s%n", (String)entities[1]);
				// Find match results
				
				// we obtained an instance of the match engine in the constructor of BAN
				// for use at this point

				// ingest files
				String tm;
				Iterator<String> i = dataReady.keySet().iterator();
				while (i.hasNext()) {
					tm = i.next();
					try {
						showProgressMessage("reading XML data...");
						compareFiles.ingestFile(tm, "ban", "Nposition." + tm + ".xml");
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (DocumentError e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				// sum the occurrence of words in both document files
				compareFiles.sumCounts();
				
				// eliminate the most common words from the sums of the word counts
				compareFiles.trimSums(100);
				
				// compute word frequencies for the remaining words over the text
				// in both documents.
				compareFiles.computeFreqs();
				
				// for each document file, calculate a word frequency profile for each
				// individual document using only the words in that file. 
				compareFiles.computeProfileFreqs();
				
				// compare the documents -- note, for the demo, we hard code the
				// names of the two team members. The general solution involves
				// establishing a session group and comparing each pair of group
				// members. For demonstration 0, this would be overly complex
				compareFiles.compareDocuments(compareFiles.documents.get("GreenTeam"), 
						compareFiles.documents.get("BlueTeam"));
				
				// o.k. look closely. Here is some nasty software engineering. Observe
				// the order of the entities in the preceding call GreenTeam then
				// BlueTeam. We put the same names in the same order into the next call.
				// This won't generalize!! rethink before trying to support session groups.
				
				// write the 5 results with the best similarity index
				// in file BFAmatches named for "Basis for Agreement Matching Results"
				try {
					compareFiles.writeResultsXML("ban", "BFAmatches.xml", 5, "GreenTeam", "BlueTeam");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (XMLStreamException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// we have results for the match so display it locally
				readAllText();
				
				// for each team member: encrypt a copy of the matches with public key
				// of the recipient and then send it in a message.
				showProgressMessage("sending PKE encrypted match data...");				
				i = dataReady.keySet().iterator();
				String toFile;
				Pmessage pmsg;
				while (i.hasNext()) {
					tm = i.next();
					// PKE encrypt match data for intended recipient
					toFile = _pkeEncryptAndSend(tm);
					// send event message and file to recipient
					pmsg = new Pmessage("ban", tm, "MatchResult");
					pmsg.setBody(pmsg.new PmessageBody());
					pmsg.getBody().setFileBodyName(toFile);
					try {
						pms.send(pmsg);
					} catch (MessageSystemException e) {
						System.err.printf("Exception while sending MatchResult from BAN to %s %n", 
								tm);
						e.printStackTrace();
					}
				}

			}
		};
		
		state.addTransition("sWaitData", "DataReady", "sEnd", eMatchData);		
		
	}

	private String _pkeEncryptAndSend(String toName) {
		InputStream in;
		String outName;
		outName = profile.dataDirPath +  File.separator
		+ toName + File.separator
		+ "BFAmatches.blk.bin";
		
		try {
			in = new FileInputStream(profile.dataDirPath +  File.separator 
					+ "ban" + File.separator
					+ "BFAmatches.xml");
			try {
				DataOutputStream out;
				out = new DataOutputStream(
						new FileOutputStream(outName));

				profile.getCrypto().encryptStream(toName, in, out);
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
	
	
	private String _pkeDecrypt(String inFile, String entity) {
		logger.writeln("   Removing PKE encryption");
		DataInputStream in;
		
		String outName;
		outName = profile.dataDirPath +  File.separator 
		+ "ban" + File.separator
		+ "NPosition." + entity + ".xml";
		
		matchFiles.put(entity, outName);
		
//		System.out.printf("BAN decrypts %s to %s%n", inFile, outName);
		updatePBarIndef("decrpyt PKE...");
//		showProgressMessage("removing PKE Encryption from " + entity + "...");
		
			try {
				in = new DataInputStream(
						new FileInputStream(inFile));
				OutputStream out = new FileOutputStream(outName);

				profile.getCrypto().decryptStream(in, out);
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
		
		return outName;
	}
	
	public void showProgressMessage(String s) {
		updatePBarIndef(s);
	}

}
