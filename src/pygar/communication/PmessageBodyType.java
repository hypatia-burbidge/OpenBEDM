/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pygar.communication;

/**
 * Enumerate the types of content that might be carried by a Pmessage.
 * Note that EMPTY means there is no content whilst STRING indicates a simple
 * text string. A message with FILE content will contain a suggested file name 
 * as a text string; moreover, the occurrence of the message indicates that a
 * JMS ObjectMessage will follow with file content as a binary payload 
 * @author pbaker
 */
public enum PmessageBodyType {
    EMPTY, STRING, 
    //STREAM, 
    FILE 
 
}
