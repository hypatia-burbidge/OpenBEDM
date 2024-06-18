package pygar.communication;

/**
 * The ExternalPmessage is a subtype of Pmessage that is used for message exchange beyond
 * the confines of a single location. This class is a placeholder - code has not been developed
 * and tested yet.
 * <p>
 * In a typical use of this class, the innermost secure portions of the system will
 * work with a Pmessage object which  is converted to a secure External Pmessage before it leaves 
 * the secure portion.
 * <p>
 * The main differences
 * relative to the superclass are these:
 * <nl>
 * <li>There is a new field which is the checksum of the message body. If the message body is 
 *    not present, this field can be blank.
 * </li><li>The brokerId, marketId, subject and sessionId fields are encrypted using the public key of 
 *    the recipient. This prevents unauthorized parties from intercepting and reading the fields. 
 * </li><li>There is a digital signature that is created from the combination of these fields: senderId, 
 *    recipientId, brokerId, marketId, sessionId, subject and checksum. Note that combination uses
 *    the encrypted value of these fields except for the first two. The signature uses the
 *    private key of the sender.This ensures the authenticity of the document. 
 * </li><li>The message body is a binary copy of the Pmessage body formed by encrypting the 
 *    original content with the public key of the recipient. Again, this thwarts eavesdroppers. 
 *    </li></nl>
 * @author pbaker
 *
 */
public class ExternalPmessage extends Pmessage {

}
