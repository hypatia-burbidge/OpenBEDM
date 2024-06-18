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

/****** Warning - there are modifications to the SW such that passwords are no longer
 * obtained from a text Java properties file. While this demo0P software has been
 * revised so that it does not load the properties, it is unclear how the software
 * will obtain the passwords. It will be necessary to make additional modifications.
 */

package pygar.demo0P;
import java.awt.*;

import pygar.communication.MessageAgent;
import pygar.communication.MessageSystemException;
import pygar.configuration.ConfigurationError;
import pygar.configuration.Profile;

/**
 * <p>This class implements a demonstration of Blind Encrypted Data matching on 
 * sets of text records. The records are held by two parties called the "Green Team"
 * and the "Blue Team". The two parties desire to keep the records separate but also
 * they need to identify similar records that might be securely exchanged to the 
 * mutual benefit of both parties. They employ the services of a "Blind Agent Negotiator"
 * or BAN who conducts and encrypted matching session. The clear text of the secret records 
 * never leaves the teams; however, the BAN is able to match encrypted records and supply
 * the accession numbers of the matching records to the teams. 
 * </p><p>
 * The demonstration employs separate control threads for all three parties as well as a 
 * fourth party: the user/observer. The observer gets a control panel to reset the system
 * and start a negotiation session. All four parties are represented by a GUI panel that 
 * shows the progress of the negotiation session and allows viewing a sample of the data.
 * </p><p>
 * The matching function is defined in the function xcorrelate.
 * </p>
  
 * @author pbaker
 * @see pygar.demo0P.CompareFiles#xcorrelate
 * 
 * 
 */
public class DemoZero {
	
	/** currently the demonstration requires that the variable dirPath should be set for
	 * the demonstration to the directory path leading to the directory in which are
	 * found the "data" and "config" subdirectories for the demonstration. 
	 * 
	 * Currently the directory path is set by the first argument to the program
	 * on the command line (See function "main" below).
	 */
	static public String dirPath;

	static public MessageAgent aOpsMgr;
	static public TeamMember aGreen;
	static public TeamMember aBlue;
	static public BAN aBAN;
	
	static public Profile profile;
	
	/** We keep an instance of this class available for TBD future use. If it remains
	 * unused, make the variable local to the main procedure. 
	 */
	static public DemoZero instance;
	
	public DemoZero(String dir) throws ConfigurationError {
		profile = new ProfileDemo0("main", dir);
	}
	
	private void _init() throws MessageSystemException, ConfigurationError {
		
	
		// create task and message system for operations manager
		try {
			Profile pOpsMgr = new ProfileDemo0("OpsMgr", dirPath);
			aOpsMgr = new MessageAgent(pOpsMgr, "OpsMgr");
		} catch (MessageSystemException e) {
			e.printStackTrace();
		}
		
		// create team members - each includes a task and message agent
		// green team
		Profile pGreen = new ProfileDemo0("GreenTeam", dirPath);
        pGreen.entityTitle = "Team Green";
		System.out.println("test profile: " + pGreen.entityAlias);
		aGreen = new TeamMember(pGreen, 100, 200, new Color(64, 128, 64));
		
		// blue team
		Profile pBlue = new ProfileDemo0("BlueTeam", dirPath);
        pBlue.entityTitle = "Blue Team";
		aBlue = new TeamMember(pBlue, 200, 275, new Color(64, 64, 128));
		
		// create the Blind Agent Negotiator, BAN
		Profile pBAN = new ProfileDemo0("BAN", dirPath);
        pBAN.entityTitle = "BAN - Blind Agent Negotiator";
		System.out.println("test profile: " + pBAN.entityAlias);
		aBAN = new BAN(pBAN, 300, 350);
		
	}
	

	/**
	 * Function main starts the process threads for each party and opens their associated
	 * GUI interaction panel.
	 * @param args
	 * @throws MessageSystemException 
	 * @throws ConfigurationError 
	 */
	public static void main(String[] args) throws MessageSystemException, ConfigurationError {

//		// set configuration path
//		if (args.length < 1) {
//			System.out.println("Error -- must invoke DemoZero with an argument: the path to data files");
//			throw new ConfigurationError();
//		}
//		dirPath = args[0];
                
                dirPath = "/Users/Paul/Dropbox/Coding/BEDM/demodata/demotrio";
		
		// need an instance of the class for coordination
		instance = new DemoZero(dirPath);
		
		// let demo run under direction of the operations manager panel. 
		@SuppressWarnings("unused")
		OperationsManager opsMgr = new OperationsManager(aOpsMgr, 20, 20);
		
		instance._init();
		
		// sit back and let the OpsMgr GUI panel run the demonstration. The demo
		// ends when the operator hits the Exit button. 
		
		
	}

}

