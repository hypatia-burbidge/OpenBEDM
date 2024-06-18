/*
 * This small class facilitates the construction of a Profile object from parameters
 * gathered from Swing entry forms. 
 */
package pygar.configuration;

/**
 *
 * @author pbaker
 */
public class ProfileInitParams {
    public enum MessageServiceType { SIMPLE, GLASSFISH }
    public String peerName;
    public boolean isPeer; // set to false when starting the BAN
    public char[] vaultPassword;
    public char[] filePassword;
    public MessageServiceType msgServiceType;
    public String msgServerAddr;
    
    public boolean ready;
    
    public ProfileInitParams(String x, char[] y) {
        peerName = x;
        vaultPassword = y;
        String z = new String(y) + x;
        filePassword = (new String(y) + z).toCharArray();
        msgServiceType = MessageServiceType.SIMPLE;
        msgServerAddr = "";
        isPeer = true;
        ready = true;
    }
    public ProfileInitParams() {
        peerName = "ErrorError - ProfileInitParams not properly initialized";
        vaultPassword = new char[256];
        filePassword = new char[256];
        msgServiceType = MessageServiceType.SIMPLE;
        msgServerAddr = "";
        isPeer = true;
        ready = false;
    }
    
}
