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

import java.io.InputStream;
import java.io.Serializable;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/** The Pmessage class extends the PmessageBase class to provide a message type
 * with various options for bod content. 
 * <p>The body of the message can be empty or nonempty. An empty message is sent
 * solely to convey the subject value. A nonempty message contains a body that will 
 * be present in one of three mutually exclusive forms. The purpose of this complexity
 * is that it supports a lazy evaluation for some functions. E.g., if the body is 
 * present as a file name, then the message can be passed around on the same machine
 * very easily without moving the file. Short messages can be implemented easily with
 * the stringBody alternative.
 * </p>
 * <p>The current version of this class contains a convenience function to
 * determine the type of body content. Note that the addition of a type field
 * might break older software but a field might be added in the future.
 * <p>
 * The current version contains lines of code that have been converted to
 * comments in order to disable a feature for conveying a Stream body. The 
 * reason for this step is that no existing code implements the stream feature.
 * Moreover, it is not compatible with a JMS implementation of a message system.
 * Therefore, we do not expect that the stream feature will ever be enabled. 
 * @author pbaker
 *
 */
@ThreadSafe
public class Pmessage extends PmessageBase implements Serializable {
    
    
    public PmessageBodyType getBodyType() {
        if (getStringBody() != null) return  PmessageBodyType.STRING;
//        if (getStreamBody() != null) return  PmessageBodyType.STREAM;
        if (getFileBodyName() != null) return  PmessageBodyType.FILE;
        return PmessageBodyType.EMPTY;
    }
    /** Upcast a Pmessage to a PmessageBase possibly loosing the body field
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
    

	/** The PmessageBody class contains a message body for the Pmessage class.
	*  The message body, if it exists, is found in exactly one of the following
	*  three mutually exclusive fields of the PmessageBody type. The other fields 
	*  must then be void. It is possible that all three are void because there is 
	*  no message body.
	*/
	public class PmessageBody implements Serializable {
		// if non-void: the actual body of the message as a string.
		@GuardedBy("this")
		private String stringBody;
		// if non-void: an InputStream from which the body can be read
//		@GuardedBy("this")
//		private InputStream streamBody;
		// if non-void: the name of a file from which the body can be read
		@GuardedBy("this")
		private String fileBodyName;
		
		public synchronized void setStringBody(String stringBody) {
			this.stringBody = stringBody;
		}
		public synchronized String getStringBody() {
			return stringBody;
		}
//		public synchronized void setStreamBody(InputStream streamBody) {
//			this.streamBody = streamBody;
//		}
//		public synchronized InputStream getStreamBody() {
//			return streamBody;
//		}
		public synchronized void setFileBodyName(String fileBodyName) {
			this.fileBodyName = fileBodyName;
        }

        public synchronized String getFileBodyName() {
            return fileBodyName;
        }
    }
    private PmessageBody body;

    public synchronized void setStringBody(String stringBody) {
        this.body.stringBody = stringBody;
    }

    public synchronized String getStringBody() {
        return body.stringBody;
    }

//    public synchronized void setStreamBody(InputStream streamBody) {
//        this.body.streamBody = streamBody;
//    }
//
//    public synchronized InputStream getStreamBody() {
//        return body.streamBody;
//    }

    public synchronized void setFileBodyName(String fileBodyName) {
        this.body.fileBodyName = fileBodyName;
    }

    public synchronized String getFileBodyName() {
        return body.fileBodyName;
    }

    public synchronized String getSenderId() {
        return senderId;
    }

    public synchronized void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public synchronized String getRecipientId() {
        return recipientId;
    }

	public synchronized void setRecipientId(String recipientId) {
		this.recipientId = recipientId;
	}

	public synchronized String getBrokerId() {
		return brokerId;
	}

	public synchronized void setBrokerId(String brokerId) {
		this.brokerId = brokerId;
	}

	public synchronized String getMarketId() {
		return marketId;
	}

	public synchronized void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	public synchronized String getSessionId() {
		return sessionId;
	}

	public synchronized void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public synchronized PmessageBody getBody() {
		return body;
	}

	public synchronized void setBody(PmessageBody body) {
		this.body = body;
	}

	public synchronized String getSubject() {
		return subject;
	}

	public synchronized void setSubject(String subject) {
		this.subject = subject;
	}

	/** copy constructor
	 * 
	 * @param msg
	 */
	public Pmessage(Pmessage msg) {
		this.brokerId = msg.brokerId;
		this.marketId = msg.marketId;
		this.sessionId = msg.sessionId;
		this.senderId = msg.senderId;
		this.recipientId = msg.recipientId;
		this.subject = msg.subject;
		this.body = msg.body;
	}
	
	public Pmessage() {
		this.brokerId = "";
		this.marketId = "";
		this.sessionId = "";
		this.senderId = "";
		this.recipientId = "";
		this.subject = "";
		this.body = new PmessageBody();
	}
	
	public Pmessage(String myID, String recipient, String eventName) {
		this.brokerId = "";
		this.marketId = "";
		this.sessionId = "";
		this.senderId = myID;
		this.recipientId = recipient;
		this.subject = eventName;
		this.body = new PmessageBody();
	}
	
	public synchronized void partialPrint() {
		System.out.printf("Pmessage diagnostic, subject: %s to %s from %s session %s%n", this.subject, this.recipientId,
				this.senderId, this.sessionId);
	
	}

}
