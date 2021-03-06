/*
 *  This file is a part of port-o-chat.
 * 
 *  port-o-chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lttldrgn.portochat.client;

import java.util.List;
import com.lttldrgn.portochat.common.User;

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
    public void userListReceived(List<User> users, String channel);
    
    /**
     * This method is called when a chat message is received.  If the channel
     * is non-null then it is associated with a channel, otherwise it is a 
     * user to user message.
     * @param fromUser User the message came from
     * @param action true if the message was an action
     * @param message
     * @param channel Which channel the message was sent on
     */
    public void receiveChatMessage(User fromUser, boolean action, String message, String channel);
    
    /**
     * This method is called after a channel list is received from the server
     * @param channels List of created channels on the server
     */
    public void channelListReceived(List<String> channels);
    
    /**
     * This method is called when a channel join/part event occurs
     * @param userId ID of the User joining or parting
     * @param channel Channel that is being joined/parted
     * @param join If true this is a join, otherwise a part event
     */
    public void receiveChannelJoinPart(String userId, String channel,
            boolean join);
    
    /**
     * This method is called when a channel is created or destroyed
     * @param channel Channel name
     * @param created True if channel is being created, otherwise destroyed
     */
    public void channelStatusReceived(String channel, boolean created);
    
    /**
     * This method is called when a server message is received with a connection
     * message.
     * @param username Username sent to server
     * @param success True if connection was successful to the server
     */
    public void handleServerConnection(String username, boolean success);
    
    /**
     * Notification from the server when a message is sent to a user that does not exist
     * @param username Name of the message recipient
     */
    public void userDoesNotExist(String username);
}
