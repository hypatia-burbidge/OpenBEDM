	/****************************************************************CopyrightNotice
	 * Copyright (c) 2013 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.0
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *******************************************************************************/

package pygar.communication;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import pygar.communication.Pmessage.PmessageBody;

/** The PmessageJMSv0 class extends the PmessageBase class to provide a message type
 with additional fields. Instances of this class can be send
 * as a JMS Message with relatively little effort. This implementation is limited to 
 * relatively small files because it allocates a byte buffer to hold the contents of 
 * any file that should be sent with the message. A more robust implementation would
 * send the file in blocks with careful attention to potential network errors.
 * <p>
 * Note that a file body is sent in two fields. A name field holds the name of the
 * file without the path. The path is deleted because it has no general value in 
 * a distributed system. Second, the content of the file is contained in a byte
 * array. 
 * @author pbaker
 *
 */
@ThreadSafe
public class PmessageJMSv0 extends PmessageBase implements Serializable {
    
    public String text;
    public String fileName;
    public byte[] fileContent;
    public int bodyTypeInt;
    
    /** Upcast a PmessageJMSv0 to a PmessageBase possibly discarding fields
     * 
     * @return 
     */
    public PmessageBase makePmessageBase() {
        PmessageBase pmsg = new PmessageBase();
		pmsg.brokerId = this.brokerId;
		pmsg.marketId = this.marketId;
		pmsg.sessionId = this.sessionId;
		pmsg.senderId = this.senderId;
		pmsg.recipientId = this.recipientId;
		pmsg.subject = this.subject;
        return pmsg;
    }
    

    public synchronized void setText(String stringBody) {
        this.text = stringBody;
        this.bodyTypeInt = PmessageBodyType.STRING.ordinal();
    }

    public synchronized String getText() {
        return text;
    }


    public synchronized void setFileName(String name) {
        this.fileName = name;
        this.bodyTypeInt = PmessageBodyType.FILE.ordinal();
    }

    public synchronized String getFileName() {
        return fileName;
    }


    public synchronized void setFileContent(byte[] bytes) {
        this.fileContent = bytes;
        this.bodyTypeInt = PmessageBodyType.FILE.ordinal();
    }

    public synchronized byte[] getFileContent() {
        return fileContent;
    }


	/** copy constructor
	 * 
	 * @param msg
	 */
	public PmessageJMSv0(PmessageJMSv0 msg) {
		this.brokerId = msg.brokerId;
		this.marketId = msg.marketId;
		this.sessionId = msg.sessionId;
		this.senderId = msg.senderId;
		this.recipientId = msg.recipientId;
		this.subject = msg.subject;
        this.text = msg.text;
        this.fileName = msg.fileName;
        this.fileContent = msg.fileContent;
        this.bodyTypeInt = msg.bodyTypeInt;
	}
	
	public PmessageJMSv0() {
		this.brokerId = "";
		this.marketId = "";
		this.sessionId = "";
		this.senderId = "";
		this.recipientId = "";
		this.subject = "";
        this.text = "";
        this.fileName = "";
        this.fileContent = null;
        this.bodyTypeInt = PmessageBodyType.EMPTY.ordinal();
	}
	
	public PmessageJMSv0(String myID, String recipient, String eventName, String s) {
		this.brokerId = "";
		this.marketId = "";
		this.sessionId = "";
		this.senderId = myID;
		this.recipientId = recipient;
		this.subject = eventName;
        this.text = s;
        this.fileName = "";
        this.fileContent = null;
        this.bodyTypeInt = PmessageBodyType.STRING.ordinal();
	}
	/** conversion constructor from the sibling type Pmessage
     * 
     */
    public PmessageJMSv0(Pmessage msg) {
		this.brokerId = msg.brokerId;
		this.marketId = msg.marketId;
		this.sessionId = msg.sessionId;
		this.senderId = msg.senderId;
		this.recipientId = msg.recipientId;
		this.subject = msg.subject;
        // does msg have a string body?
        String s = msg.getStringBody();
        if (s != null) {
            this.text = s;
            this.bodyTypeInt = PmessageBodyType.STRING.ordinal();
        } else {
            // does msg have a file body?
            s = msg.getFileBodyName();
            if (s != null) {
                File f = new File(s);
                long filesize = f.length();
                int ifilesize = (int) filesize;
                this.fileContent = new byte[ifilesize];
                String pathFreeName = f.getName();
                this.fileName = pathFreeName;
                this.bodyTypeInt = PmessageBodyType.FILE.ordinal();
                try {
                    // fill in the content from the file
                    DataInputStream in = new DataInputStream( new FileInputStream(f));
                    in.readFully(this.fileContent);
                    in.close();
                    
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(PmessageJMSv0.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(PmessageJMSv0.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                this.bodyTypeInt = PmessageBodyType.EMPTY.ordinal();
            }
        }
    }
}
