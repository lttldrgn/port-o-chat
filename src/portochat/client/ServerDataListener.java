/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package portochat.client;

import java.util.List;

/**
 *
 * @author Brandon
 */
public interface ServerDataListener {
    /**
     * This method is called when a user list is received from the server
     * @param users List of users
     */
    public void userListReceived(List<String> users);
    
    /**
     * This method is called when a user joins or leaves the server or channel
     * @param user Name of the user
     * @param connected true if user has connected, otherwise means disconnected
     */
    public void userConnectionEvent(String user, boolean connected);
    
    /**
     * This method is called when a chat message is received.  If the channel
     * is non-null then it is associated with a channel, otherwise it is a 
     * user to user message.
     * @param user User the message came from
     * @param message
     * @param channel Which channel the message was sent on
     */
    public void receiveChatMessage(String user, String message, String channel);
}