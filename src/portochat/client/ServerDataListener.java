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
     * This method is called when a user list is received from the server.  If
     * channel is non-null then this list is associated with the named channel,
     * otherwise the list is a user list.
     * @param users List of users
     * @param channel The channel that this list of users is from, null if a 
     * server user list.
     */
    public void userListReceived(List<String> users, String channel);
    
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
     * @param fromUser User the message came from
     * @param message
     * @param channel Which channel the message was sent on
     */
    public void receiveChatMessage(String fromUser, String message, String channel);
    
    /**
     * This method is called after a channel list is received from the server
     * @param channels List of created channels on the server
     */
    public void channelListReceived(List<String> channels);
    
    /**
     * This method is called when a channel join/part event occurs
     * @param user User joining or parting
     * @param channel Channel that is being joined/parted
     * @param join If true this is a join, otherwise a part event
     */
    public void receiveChannelJoinPart(String user, String channel, boolean join);
    
    /**
     * This method is called when a channel is created or destroyed
     * @param channel Channel name
     * @param created True if channel is being created, otherwise destroyed
     */
    public void channelStatusReceived(String channel, boolean created);
}
